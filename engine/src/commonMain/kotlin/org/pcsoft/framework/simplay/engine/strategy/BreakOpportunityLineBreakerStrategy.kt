/*
 * Copyright (c) KleinerHacker alias Pfeiffer C Soft 2026.
 * This work is licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, this software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations.
 */

package org.pcsoft.framework.simplay.engine.strategy

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.model.TextAnchor
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextSymbol
import org.pcsoft.framework.simplay.engine.model.TextWhitespace
import org.pcsoft.framework.simplay.engine.model.TextWord



/**
 * A curated approximation of the UAX #14 line-break classes used by
 * [BreakOpportunityLineBreakerStrategy] to decide where a break is allowed.
 *
 * This is not a conformant UAX #14 implementation: it covers only a small, hard-coded subset of
 * Unicode (Latin punctuation plus a simple CJK rule) and ignores locale, dictionaries and the full
 * pair table. [MANDATORY] is reserved for a future explicit break token and is never produced today.
 */
internal enum class BreakClass {
    /** Forced break. Reserved; no character is classified this way today. */
    MANDATORY,

    /** Breakable glue, produced only by an explicit [TextWhitespace] run. */
    SPACE,

    /** A break is allowed immediately before the character, not after it. */
    BEFORE,

    /** A break is allowed immediately after the character, not before it. */
    AFTER,

    /** A break is allowed both before and after the character. */
    BOTH,

    /** No break is allowed directly adjacent to the character. The default. */
    NONBREAK,
}

/** Characters that allow a break immediately before them (opening brackets and inverted marks). */
private val BREAK_BEFORE_CHARS = setOf('(', '[', '{', '¡', '¿')

/** Characters that allow a break immediately after them (closing brackets and terminal punctuation). */
private val BREAK_AFTER_CHARS =
    setOf(')', ']', '}', '!', '?', ',', ';', ':', '.', '%', '/', '-', '—', '…')

/** Characters that allow a break both before and after them. */
private val BREAK_BOTH_CHARS = setOf('–')

/** Hard-coded Unicode ranges treated as CJK for the purpose of intra-word breaking. */
private val CJK_RANGES = listOf(
    0x3040..0x309F, // Hiragana
    0x30A0..0x30FF, // Katakana
    0x3400..0x4DBF, // CJK Unified Ideographs Extension A
    0x4E00..0x9FFF, // CJK Unified Ideographs
)

/**
 * Classifies a single symbol character (never a letter or digit, see [TextSymbol]) into its
 * [BreakClass], following the curated table documented on [BreakClass].
 */
internal fun classifySymbol(char: Char): BreakClass = when (char) {
    in BREAK_BEFORE_CHARS -> BreakClass.BEFORE
    in BREAK_AFTER_CHARS -> BreakClass.AFTER
    in BREAK_BOTH_CHARS -> BreakClass.BOTH
    else -> BreakClass.NONBREAK
}

/**
 * Returns true when [char] falls into one of the hard-coded [CJK_RANGES], the curated approximation
 * of "behaves like a CJK ideograph" used to allow a break after every such character inside a
 * [TextWord].
 */
internal fun isCjk(char: Char): Boolean {
    val codePoint = char.code
    return CJK_RANGES.any { codePoint in it }
}

/**
 * A single indivisible unit derived from one [TextPart] (or a slice of a [TextWord]): its own
 * measured [width]/[ascent]/[descent], whether an explicit [TextWhitespace] preceded it
 * ([spaceBefore]), and whether a break is allowed immediately before ([breakBefore]) or after
 * ([breakAfter]) it.
 */
private class Atom(
    val part: TextPart,
    val width: Double,
    val ascent: Double,
    val descent: Double,
    val breakBefore: Boolean,
    val breakAfter: Boolean,
    val spaceBefore: Boolean,
) {
    fun withBreaks(breakBefore: Boolean = this.breakBefore, breakAfter: Boolean = this.breakAfter): Atom =
        Atom(part, width, ascent, descent, breakBefore, breakAfter, spaceBefore)
}

/**
 * A maximal run of [atoms] with no allowed break between them; the smallest group
 * [BreakOpportunityLineBreakerStrategy] may place on a line. [spaceBefore] mirrors the leading
 * atom's [Atom.spaceBefore].
 */
private class Chunk(val atoms: List<Atom>) {
    val spaceBefore: Boolean get() = atoms.first().spaceBefore
    val width: Double get() = atoms.sumOf { it.width }
}

/**
 * A [LineBreakerStrategy] that greedily fills lines like [GreedyWordLineBreakerStrategy], but only
 * ever breaks at a position [classifySymbol]/[isCjk] allow, following a curated, non-conformant
 * approximation of UAX #14 (see [BreakClass]).
 *
 * Parts are first classified into [Atom]s, then grouped into [Chunk]s - maximal runs with no allowed
 * break between them - so a symbol like a closing bracket stays glued to the word in front of it
 * while still allowing a break right after it. Chunks are then filled onto lines exactly like
 * [GreedyWordLineBreakerStrategy] fills words: a chunk that alone exceeds [breakIntoLines]'s
 * `maxWidth` is still placed and overflows its line, since cutting inside it would violate a
 * disallowed position. A [TextWord] made only of CJK-classified characters (see [isCjk]) is split
 * into one single-character chunk per character, so it naturally wraps between ideographs through
 * the same greedy fill - no separate splitting path is needed. The [WordBreakerStrategy] is never
 * consulted.
 *
 * A digit run, a `.` or `,` and another digit run with nothing else between them (e.g. the
 * `1`/`,`/`000` tokens of `"1,000"`) are kept in one chunk regardless of their own classes, so a
 * thousands/decimal separator never gets picked as a break position.
 *
 * A [org.pcsoft.framework.simplay.engine.model.TextBreak] is always a hard break, independent of the
 * curated break-opportunity table: [parts] is first split into segments with [splitAtBreaks] and each
 * segment is filled independently, so a hard break can never be absorbed into a chunk or overridden by
 * the break-class rules. An empty segment (two consecutive breaks, or a break at the very start or
 * end) still produces an empty line, using the font's metric ascent/descent.
 *
 * Deterministic and free of platform APIs, like every [LineBreakerStrategy].
 */
object BreakOpportunityLineBreakerStrategy : LineBreakerStrategy {

    override fun breakIntoLines(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine> {
        if (parts.isEmpty()) return emptyList()

        val lines = mutableListOf<UnplacedLine>()
        for (segment in parts.splitAtBreaks()) {
            if (segment.isEmpty()) {
                lines += UnplacedLine(
                    parts = emptyList(),
                    ascent = font.metrics.ascent,
                    descent = font.metrics.descent,
                )
                continue
            }

            lines += fillSegment(segment, font, maxWidth, measurer)
        }

        return lines
    }

    /** Fills one [splitAtBreaks] segment (never containing a `TextBreak`) into lines. */
    private fun fillSegment(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
    ): List<UnplacedLine> {
        val spaceWidth = measurer.measure(font.raw, " ").width
        val atoms = applyNumericGuard(buildAtoms(parts, font, measurer))
        val chunks = groupIntoChunks(atoms)

        val lines = mutableListOf<UnplacedLine>()
        var lineParts = mutableListOf<UnplacedPart>()
        var lineWidth = 0.0
        var lineAscent = 0.0
        var lineDescent = 0.0

        fun flush() {
            if (lineParts.isEmpty()) return
            lines += UnplacedLine(
                parts = lineParts.toList(),
                ascent = if (lineAscent > 0.0) lineAscent else font.metrics.ascent,
                descent = if (lineDescent > 0.0) lineDescent else font.metrics.descent,
            )
            lineParts = mutableListOf()
            lineWidth = 0.0
            lineAscent = 0.0
            lineDescent = 0.0
        }

        for (chunk in chunks) {
            val space = if (lineParts.isEmpty() || !chunk.spaceBefore) 0.0 else spaceWidth

            if (lineParts.isNotEmpty() && lineWidth + space + chunk.width > maxWidth) {
                flush()
            }

            var first = true
            for (atom in chunk.atoms) {
                val atomSpace = if (first && lineParts.isNotEmpty()) space else 0.0
                lineParts += UnplacedPart(atom.part, atom.width, atomSpace)
                lineWidth += atomSpace + atom.width
                lineAscent = maxOf(lineAscent, atom.ascent)
                lineDescent = maxOf(lineDescent, atom.descent)
                first = false
            }
        }

        flush()
        return lines
    }

    /** Flattens [parts] into [Atom]s, splitting a [TextWord] at every CJK-classified character. */
    private fun buildAtoms(
        parts: List<TextPart>,
        font: MeasuredFont,
        measurer: FontMeasureCalculator,
    ): List<Atom> {
        val atoms = mutableListOf<Atom>()
        var pendingSpace = false

        for (part in parts) {
            when (part) {
                is org.pcsoft.framework.simplay.engine.model.TextBreak ->
                    error("TextBreak must be removed by splitAtBreaks() before buildAtoms() runs")

                is TextWhitespace -> pendingSpace = true

                is TextAnchor -> {
                    // Zero-width and transparent to spacing, like every other LineBreakerStrategy.
                    atoms += Atom(part, 0.0, 0.0, 0.0, breakBefore = true, breakAfter = true, spaceBefore = false)
                }

                is TextSymbol -> {
                    val metrics = measurer.measure(font.raw, part.text)
                    val breakClass = classifySymbol(part.symbol)
                    atoms += Atom(
                        part = part,
                        width = metrics.width,
                        ascent = metrics.ascent,
                        descent = metrics.descent,
                        breakBefore = breakClass == BreakClass.BEFORE || breakClass == BreakClass.BOTH,
                        breakAfter = breakClass == BreakClass.AFTER || breakClass == BreakClass.BOTH,
                        spaceBefore = pendingSpace,
                    )
                    pendingSpace = false
                }

                is TextWord -> {
                    atoms += buildWordAtoms(part, font, measurer, pendingSpace)
                    pendingSpace = false
                }
            }
        }

        return atoms
    }

    /**
     * Splits [word] into [Atom]s: every CJK-classified character (see [isCjk]) becomes its own
     * `BOTH`-classified atom, while a maximal run of non-CJK characters in between stays one
     * `NONBREAK` atom. Only the very first atom can carry [firstSpaceBefore].
     */
    private fun buildWordAtoms(
        word: TextWord,
        font: MeasuredFont,
        measurer: FontMeasureCalculator,
        firstSpaceBefore: Boolean,
    ): List<Atom> {
        val text = word.text
        val result = mutableListOf<Atom>()
        var index = 0
        var isFirst = true

        fun piecePart(piece: String): TextPart = if (piece == text) word else TextWord(piece)

        while (index < text.length) {
            val start = index
            val cjk = isCjk(text[index])
            index = if (cjk) {
                index + 1
            } else {
                var end = index + 1
                while (end < text.length && !isCjk(text[end])) end++
                end
            }
            val piece = text.substring(start, index)
            val metrics = measurer.measure(font.raw, piece)
            result += Atom(
                part = piecePart(piece),
                width = metrics.width,
                ascent = metrics.ascent,
                descent = metrics.descent,
                breakBefore = cjk,
                breakAfter = cjk,
                spaceBefore = isFirst && firstSpaceBefore,
            )
            isFirst = false
        }

        return result
    }

    /**
     * Forces the boundaries around a `.`/`,` between two all-digit [TextWord]s - with nothing else,
     * not even whitespace, in between - to disallow a break, so a thousands or decimal separator is
     * never chosen as a break position.
     */
    private fun applyNumericGuard(atoms: List<Atom>): List<Atom> {
        if (atoms.size < 3) return atoms

        val result = atoms.toMutableList()
        for (i in 1 until result.size - 1) {
            val separator = result[i]
            val separatorPart = separator.part
            val previous = result[i - 1]
            val next = result[i + 1]

            if (separatorPart !is TextSymbol) continue
            if (separatorPart.symbol != '.' && separatorPart.symbol != ',') continue
            if (separator.spaceBefore || next.spaceBefore) continue
            if (!isAllDigits(previous.part)) continue
            if (!isAllDigits(next.part)) continue

            result[i - 1] = previous.withBreaks(breakAfter = false)
            result[i] = separator.withBreaks(breakBefore = false, breakAfter = false)
            result[i + 1] = next.withBreaks(breakBefore = false)
        }
        return result
    }

    private fun isAllDigits(part: TextPart): Boolean =
        part is TextWord && part.text.isNotEmpty() && part.text.all { it.isDigit() }

    /** Groups [atoms] into [Chunk]s at every position a break is allowed. */
    private fun groupIntoChunks(atoms: List<Atom>): List<Chunk> {
        if (atoms.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var current = mutableListOf(atoms[0])
        for (i in 1 until atoms.size) {
            val previous = atoms[i - 1]
            val atom = atoms[i]
            val canBreak = atom.spaceBefore || previous.breakAfter || atom.breakBefore
            if (canBreak) {
                chunks += Chunk(current.toList())
                current = mutableListOf(atom)
            } else {
                current += atom
            }
        }
        chunks += Chunk(current.toList())
        return chunks
    }
}
