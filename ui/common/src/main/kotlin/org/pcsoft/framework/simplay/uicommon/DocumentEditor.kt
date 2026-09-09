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

package org.pcsoft.framework.simplay.uicommon

import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Applies a single splice - insert, delete or replace - to a [Document], expressed on the linear text
 * axis of a [DocumentTextIndex].
 *
 * The edit is performed on [DocumentTextIndex.text] (caret indices are indices into that string),
 * the result is cut back into raw blocks along [DocumentTextIndex.blockRanges] remapped through the
 * splice, and every affected block is rebuilt with [TextBlock.of] so it is re-tokenized. Blocks a
 * delete joined across their boundary (no line break left between them) are merged. Pasted line
 * breaks are turned into spaces, so an edit never creates a new block; a delete can only reduce the
 * block count. Unaffected blocks and the page structure (page objects, their layout and type) are
 * kept.
 *
 * Every operation returns the new [Document] together with the caret index it should sit at in the
 * re-measured document.
 */
object DocumentEditor {

    /** Outcome of an edit: the rebuilt [document] and the [caretIndex] for the re-measured text. */
    data class Result(val document: Document, val caretIndex: Int)

    /** Inserts [text] at linear index [at]; line breaks in [text] become spaces. */
    fun insert(index: DocumentTextIndex, document: Document, at: Int, text: String): Result {
        val pos = index.clamp(at)
        if (text.isEmpty()) return Result(document, pos)
        val inserted = sanitize(text)
        return splice(index, document, pos, pos, inserted, caretIndex = pos + inserted.length)
    }

    /** Deletes the linear range `[from, to)` (order-independent). */
    fun delete(index: DocumentTextIndex, document: Document, from: Int, to: Int): Result {
        val lo = index.clamp(minOf(from, to))
        val hi = index.clamp(maxOf(from, to))
        if (lo == hi) return Result(document, lo)
        return splice(index, document, lo, hi, "", caretIndex = lo)
    }

    /** Replaces the linear range `[from, to)` with [text]; line breaks in [text] become spaces. */
    fun replace(index: DocumentTextIndex, document: Document, from: Int, to: Int, text: String): Result {
        val lo = index.clamp(minOf(from, to))
        val hi = index.clamp(maxOf(from, to))
        if (lo == hi) return insert(index, document, lo, text)
        val inserted = sanitize(text)
        return splice(index, document, lo, hi, inserted, caretIndex = lo + inserted.length)
    }

    private fun sanitize(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ')

    private fun splice(
        index: DocumentTextIndex,
        document: Document,
        spliceLo: Int,
        spliceHi: Int,
        inserted: String,
        caretIndex: Int,
    ): Result {
        val oldText = index.text
        val newText = oldText.substring(0, spliceLo) + inserted + oldText.substring(spliceHi)
        val delta = inserted.length - (spliceHi - spliceLo)

        // A block start is left-biased: a start exactly at a pure-insert point stays put, so the
        // inserted text goes to the block on its left. A block end is right-biased: an end exactly
        // at the splice point grows over the inserted text, so typing at the very end of a block
        // keeps the new character in that block instead of losing it in the inter-block gap.
        fun remapStart(x: Int): Int = when {
            x <= spliceLo -> x
            x >= spliceHi -> x + delta
            else -> spliceLo
        }

        fun remapEnd(x: Int): Int = when {
            x < spliceLo -> x
            x <= spliceHi -> spliceLo + inserted.length
            else -> x + delta
        }

        if (index.blockRanges.isEmpty()) {
            return Result(
                rebuildEmpty(document, newText),
                caretIndex.coerceIn(0, newText.length),
            )
        }

        data class Slice(val pageIndex: Int, val blockIndex: Int, val start: Int, val end: Int)

        val remapped = index.blockRanges.map { range ->
            val s = remapStart(range.start).coerceIn(0, newText.length)
            val e = remapEnd(range.end).coerceIn(0, newText.length)
            Slice(range.pageIndex, range.blockIndex, minOf(s, e), maxOf(s, e))
        }

        val merged = ArrayList<Slice>()
        for (slice in remapped) {
            val prev = merged.lastOrNull()
            if (prev != null && prev.end <= slice.start &&
                !newText.substring(prev.end, slice.start).contains('\n')
            ) {
                merged[merged.lastIndex] = prev.copy(end = slice.end)
            } else {
                merged += slice
            }
        }

        val blocks = merged.map { slice ->
            TextBlock.of(newText.substring(slice.start, slice.end), styleFor(document, slice.pageIndex, slice.blockIndex))
        }

        val newPages = document.pages.mapIndexed { pageIndex, page ->
            val pageBlocks = merged.indices
                .filter { merged[it].pageIndex == pageIndex }
                .map { blocks[it] }
            when (page) {
                is FlowPage -> page.copy(blocks = pageBlocks)
                is SinglePage -> page.copy(blocks = pageBlocks)
            }
        }

        return Result(document.copy(pages = newPages), caretIndex.coerceIn(0, newText.length))
    }

    private fun rebuildEmpty(document: Document, newText: String): Document {
        val style = document.pages.firstNotNullOfOrNull { it.blocks.firstOrNull()?.style } ?: DEFAULT_STYLE
        val text = newText.replace('\n', ' ').trim()
        val block = TextBlock.of(text, style)
        if (document.pages.isEmpty()) {
            return document.copy(pages = listOf(FlowPage(layout = FALLBACK_LAYOUT, blocks = listOf(block))))
        }
        val newPages = document.pages.mapIndexed { i, page ->
            val pageBlocks = if (i == 0) listOf(block) else emptyList()
            when (page) {
                is FlowPage -> page.copy(blocks = pageBlocks)
                is SinglePage -> page.copy(blocks = pageBlocks)
            }
        }
        return document.copy(pages = newPages)
    }

    private fun styleFor(document: Document, pageIndex: Int, blockIndex: Int): TextStyle {
        val page = document.pages.getOrNull(pageIndex)
        return page?.blocks?.getOrNull(blockIndex)?.style
            ?: document.pages.firstNotNullOfOrNull { it.blocks.firstOrNull()?.style }
            ?: DEFAULT_STYLE
    }

    private val DEFAULT_STYLE = TextStyle(font = Font(family = "Serif", size = 12.0))

    private val FALLBACK_LAYOUT =
        org.pcsoft.framework.simplay.engine.model.PageLayout(
            size = org.pcsoft.framework.simplay.engine.geometry.Size(width = 595.0, height = 842.0),
            margins = org.pcsoft.framework.simplay.engine.geometry.Margins(
                left = 60.0, top = 60.0, right = 60.0, bottom = 60.0,
            ),
        )
}
