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
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * A flat, linear view of the text in a [MeasuredDocument] for block- and page-spanning selection.
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

    val segments: List<Segment>

    /** The whole document text, real glyphs plus the readable separators between parts. */
    val text: String

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
    }

    /** The total character count of [text]. */
    val length: Int get() = text.length

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
}
