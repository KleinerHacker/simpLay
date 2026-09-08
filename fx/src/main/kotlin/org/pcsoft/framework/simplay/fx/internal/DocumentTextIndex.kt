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

package org.pcsoft.framework.simplay.fx.internal

import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.TextSymbol
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * A flat, linear view of the text in a [MeasuredDocument] for block- and page-spanning selection and
 * for editing.
 *
 * The measured tree (`pages -> blocks -> lines -> parts`) is walked once and turned into a single
 * [text] string plus one [Segment] per [MeasuredTextPart]. Each segment records where that part's
 * characters sit in [text] (`start` until `end`) together with the measured geometry needed to draw
 * a selection highlight and to hit-test a pointer position.
 *
 * The separators inserted between parts mirror [org.pcsoft.framework.simplay.engine.model.TextBlock.toString]:
 * a single space before every [TextWord] that is not the first part of its line, a space between the
 * soft-wrapped lines of one block, and a line break between blocks and between pages. Separators are
 * part of [text] (so a copied range reads naturally) but never belong to a [Segment] range, so a
 * selection can only ever cover real glyphs.
 *
 * On top of that flat axis the index also exposes the structural elements a caret can address:
 * [blockRanges] (one entry per raw block of the document, in document order), [wordRanges] and
 * [symbolRanges] (one entry per measured part of that kind). The `*Of*` helpers map a structural
 * ordinal plus an in-element offset onto a linear text index, and the `next*` / `prev*` helpers step
 * from a linear index to the next / previous element start.
 */
internal class DocumentTextIndex(measured: MeasuredDocument) {

    /**
     * One measured text part placed on the linear [text] axis.
     *
     * @property pageIndex zero-based index of the owning page.
     * @property page the owning measured page.
     * @property block the owning measured block.
     * @property line the owning measured line.
     * @property part the measured part itself; [MeasuredTextPart.bounds] is in page-content-area coordinates.
     * @property font the raw font of [block], used for prefix measuring and hit-testing.
     * @property start index of this part's first character in [text].
     * @property end index one past this part's last character in [text].
     */
    data class Segment(
        val pageIndex: Int,
        val page: MeasuredPage,
        val block: MeasuredTextBlock,
        val line: MeasuredLine,
        val part: MeasuredTextPart,
        val font: Font,
        val start: Int,
        val end: Int,
    )

    /**
     * The half-open linear-text span `[start, end)` of one raw block of the document.
     *
     * @property pageIndex index of the raw page in `MeasuredDocument.raw.pages` this block lives on.
     * @property blockIndex index of the block within that raw page's `blocks` list.
     * @property start index of the block's first character in [text].
     * @property end index one past the block's last character in [text].
     */
    data class BlockRange(val pageIndex: Int, val blockIndex: Int, val start: Int, val end: Int)

    val segments: List<Segment>

    /** The whole document text, real glyphs plus the readable separators between parts. */
    val text: String

    /** One entry per raw block that produced glyphs, in document order. */
    val blockRanges: List<BlockRange>

    /** One `[start, end)` linear-text range per measured word part, in document order. */
    val wordRanges: List<IntRange>

    /** One `[start, end)` linear-text range per measured symbol part, in document order. */
    val symbolRanges: List<IntRange>

    init {
        val builder = StringBuilder()
        val collected = ArrayList<Segment>()
        var first = true
        var prevPage: MeasuredPage? = null
        var prevBlock: MeasuredTextBlock? = null
        var prevLine: MeasuredLine? = null

        measured.pages.forEachIndexed { pageIndex, page ->
            for (block in page.blocks) {
                val font = block.style.font.raw
                for (line in block.lines) {
                    line.parts.forEach { part ->
                        val separator = when {
                            first -> ""
                            page !== prevPage -> "\n"
                            block !== prevBlock -> "\n"
                            line !== prevLine -> " "
                            part.raw is TextWord -> " "
                            else -> ""
                        }
                        builder.append(separator)
                        val start = builder.length
                        builder.append(part.text)
                        val end = builder.length
                        collected += Segment(pageIndex, page, block, line, part, font, start, end)

                        first = false
                        prevPage = page
                        prevBlock = block
                        prevLine = line
                    }
                }
            }
        }

        text = builder.toString()
        segments = collected

        blockRanges = buildBlockRanges(measured, collected)

        val words = ArrayList<IntRange>()
        val symbols = ArrayList<IntRange>()
        for (segment in collected) {
            when (segment.part.raw) {
                is TextWord -> words += segment.start until segment.end
                is TextSymbol -> symbols += segment.start until segment.end
            }
        }
        wordRanges = words
        symbolRanges = symbols
    }

    /** The total character count of [text]. */
    val length: Int get() = text.length

    /** Number of addressable raw blocks. */
    val blockCount: Int get() = blockRanges.size

    /** Number of addressable words. */
    val wordCount: Int get() = wordRanges.size

    /** Number of addressable symbols. */
    val symbolCount: Int get() = symbolRanges.size

    /** Clamps [index] to the valid `0..length` range. */
    fun clamp(index: Int): Int = index.coerceIn(0, length)

    /**
     * The plain text between [a] and [b] (order-independent), clamped to the document bounds.
     * Includes the separators that fall inside the range.
     */
    fun substring(a: Int, b: Int): String {
        val lo = clamp(minOf(a, b))
        val hi = clamp(maxOf(a, b))
        return text.substring(lo, hi)
    }

    /**
     * One run of the linear [text] for a selection range: either a slice of a single measured part
     * (then [font] is that part's font) or a stretch of the separators between parts (then [font] is
     * `null`). Concatenating the [StyledRun.text] of consecutive runs rebuilds the selected substring.
     *
     * @property text the literal characters of this run.
     * @property font the font the characters are drawn with, or `null` for separator text.
     */
    data class StyledRun(val text: String, val font: Font?)

    /**
     * Splits the selection `[a, b)` (order-independent, clamped) into [StyledRun]s so a copy can carry
     * the per-part font (family, size, weight, slant). Separator characters that fall between parts
     * become runs with a `null` [StyledRun.font].
     */
    fun styledRuns(a: Int, b: Int): List<StyledRun> {
        val lo = clamp(minOf(a, b))
        val hi = clamp(maxOf(a, b))
        if (lo >= hi) return emptyList()
        val runs = ArrayList<StyledRun>()
        var cursor = lo
        for (segment in segments) {
            if (segment.end <= lo || segment.start >= hi) continue
            val partStart = maxOf(segment.start, lo)
            if (cursor < partStart) runs += StyledRun(text.substring(cursor, partStart), null)
            val partEnd = minOf(segment.end, hi)
            runs += StyledRun(text.substring(partStart, partEnd), segment.font)
            cursor = partEnd
        }
        if (cursor < hi) runs += StyledRun(text.substring(cursor, hi), null)
        return runs
    }

    /**
     * The half-open range of the letters-and-digits word around [index], for double-click selection.
     * Returns `index..index` when [index] does not sit on a word character.
     */
    fun wordRangeAt(index: Int): IntRange {
        val i = clamp(index)
        if (i >= length || !text[i].isLetterOrDigit()) {
            if (i == 0 || !text[i - 1].isLetterOrDigit()) return i..i
        }
        var lo = i
        while (lo > 0 && text[lo - 1].isLetterOrDigit()) lo--
        var hi = i
        while (hi < length && text[hi].isLetterOrDigit()) hi++
        return lo..hi
    }

    //region Structural addressing

    /** Linear index [index] characters into block [block]; ordinal and offset are clamped. */
    fun offsetInBlock(block: Int, index: Int): Int {
        if (blockRanges.isEmpty()) return 0
        val range = blockRanges[block.coerceIn(0, blockRanges.lastIndex)]
        return (range.start + index).coerceIn(range.start, range.end)
    }

    /** Linear index of the first character of block [block]; the ordinal is clamped. */
    fun startOfBlock(block: Int): Int =
        if (blockRanges.isEmpty()) 0 else blockRanges[block.coerceIn(0, blockRanges.lastIndex)].start

    /** Linear index one past the last character of block [block]; the ordinal is clamped. */
    fun endOfBlock(block: Int): Int =
        if (blockRanges.isEmpty()) 0 else blockRanges[block.coerceIn(0, blockRanges.lastIndex)].end

    /** Linear index [index] characters into word [word]; ordinal and offset are clamped. */
    fun offsetInWord(word: Int, index: Int): Int {
        if (wordRanges.isEmpty()) return 0
        val range = wordRanges[word.coerceIn(0, wordRanges.lastIndex)]
        return (range.first + index).coerceIn(range.first, range.last + 1)
    }

    /** Linear index of the first character of word [word]; the ordinal is clamped. */
    fun startOfWord(word: Int): Int =
        if (wordRanges.isEmpty()) 0 else wordRanges[word.coerceIn(0, wordRanges.lastIndex)].first

    /** Linear index one past the last character of word [word]; the ordinal is clamped. */
    fun endOfWord(word: Int): Int =
        if (wordRanges.isEmpty()) 0 else wordRanges[word.coerceIn(0, wordRanges.lastIndex)].last + 1

    /** Linear index in front of symbol [symbol]; the ordinal is clamped. */
    fun offsetOfSymbol(symbol: Int): Int =
        if (symbolRanges.isEmpty()) 0 else symbolRanges[symbol.coerceIn(0, symbolRanges.lastIndex)].first

    /** Linear index in front of symbol [symbol]; the ordinal is clamped. */
    fun startOfSymbol(symbol: Int): Int = offsetOfSymbol(symbol)

    /** Linear index past symbol [symbol]; the ordinal is clamped. */
    fun endOfSymbol(symbol: Int): Int =
        if (symbolRanges.isEmpty()) 0 else symbolRanges[symbol.coerceIn(0, symbolRanges.lastIndex)].last + 1

    /** Start of the first word that begins after [from]; [length] when there is none. */
    fun nextWordStart(from: Int): Int = wordRanges.firstOrNull { it.first > from }?.first ?: length

    /** Start of the last word that begins before [from]; `0` when there is none. */
    fun prevWordStart(from: Int): Int = wordRanges.lastOrNull { it.first < from }?.first ?: 0

    /** Start of the first block that begins after [from]; [length] when there is none. */
    fun nextBlockStart(from: Int): Int = blockRanges.firstOrNull { it.start > from }?.start ?: length

    /** Start of the last block that begins before [from]; `0` when there is none. */
    fun prevBlockStart(from: Int): Int = blockRanges.lastOrNull { it.start < from }?.start ?: 0

    /** Position of the first symbol that starts after [from]; [length] when there is none. */
    fun nextSymbolStart(from: Int): Int = symbolRanges.firstOrNull { it.first > from }?.first ?: length

    /** Position of the last symbol that starts before [from]; `0` when there is none. */
    fun prevSymbolStart(from: Int): Int = symbolRanges.lastOrNull { it.first < from }?.first ?: 0

    //endregion

    private companion object {

        /**
         * Groups the [segments] by the raw [org.pcsoft.framework.simplay.engine.model.TextBlock] they
         * belong to (consecutive segments sharing the same raw block instance, so a raw block split
         * across a page break stays one group) and matches each group to its position in
         * `measured.raw.pages[*].blocks[*]`. Raw blocks that produced no glyph are skipped.
         */
        fun buildBlockRanges(measured: MeasuredDocument, segments: List<Segment>): List<BlockRange> {
            if (segments.isEmpty()) return emptyList()

            val rawFlat = ArrayList<Triple<Int, Int, Any>>()
            measured.raw.pages.forEachIndexed { pageIndex, page ->
                page.blocks.forEachIndexed { blockIndex, block ->
                    rawFlat += Triple(pageIndex, blockIndex, block as Any)
                }
            }

            val ranges = ArrayList<BlockRange>()
            var pointer = 0
            var i = 0
            while (i < segments.size) {
                val rawBlock: Any = segments[i].block.raw
                var j = i
                while (j < segments.size && segments[j].block.raw === rawBlock) j++
                val start = segments[i].start
                val end = segments[j - 1].end

                while (pointer < rawFlat.size && rawFlat[pointer].third !== rawBlock) pointer++
                if (pointer < rawFlat.size) {
                    ranges += BlockRange(rawFlat[pointer].first, rawFlat[pointer].second, start, end)
                    pointer++
                } else {
                    val lastPage = ranges.lastOrNull()?.pageIndex ?: 0
                    val lastBlock = (ranges.lastOrNull()?.blockIndex ?: -1) + 1
                    ranges += BlockRange(lastPage, lastBlock, start, end)
                }
                i = j
            }
            return ranges
        }
    }
}
