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

/**
 * The editable, selectable, navigable and visible linear-text regions of a document once every page
 * has been resolved to its [PageMode], shared by the `fx` and `swing` `PaperSheetView`
 * implementations.
 *
 * Built once per [of] call from a [DocumentTextIndex], the per-page mode overrides and the view-wide
 * fallback mode. The mode of a raw page is `pageModes[page.id] ?: globalMode`; a raw block belongs to
 * the raw page its `DocumentTextIndex.BlockRange.pageIndex` points at. All blocks of one page form a
 * single span, from the first block's start to the last block's end, so the separators between them
 * are treated like the blocks themselves and the caret cannot come to rest between two blocks of a
 * page it may not enter.
 *
 * * [editableRanges] - where a mutation may land (pages without [PageMode.supportsEditing] are cut out).
 * * [selectableRanges] - what may be selected (pages without [PageMode.supportsSelection] are cut out).
 * * [navigableRanges] - where the caret may come to rest after a plain (non-`Shift`) navigation move
 *   (pages without [PageMode.supportsCaret] are cut out).
 * * [visibleRanges] - what stays part of layout and hit-testing (pages without [PageMode.laidOut] are
 *   cut out).
 *
 * [isEditRangeAllowed] judges a mutation against the editable pages, [snapOutOfBlocked] pushes the
 * caret out of a page it may not enter.
 */
class EditableRegions private constructor(
    val editableRanges: List<IntRange>,
    val selectableRanges: List<IntRange>,
    val navigableRanges: List<IntRange>,
    val visibleRanges: List<IntRange>,
    private val editBlockedPairs: List<Pair<Int, Int>>,
    private val caretBlockedPairs: List<Pair<Int, Int>>,
) {

    /**
     * Whether the half-open mutation range `[lo, hi)` (order-independent; `lo == hi` for an insertion
     * point) may be applied. A zero-length range is rejected only when it sits strictly inside a
     * non-editable page; its boundaries (the edges of the editable text) are always allowed.
     */
    fun isEditRangeAllowed(lo: Int, hi: Int): Boolean {
        val a = minOf(lo, hi)
        val b = maxOf(lo, hi)
        return editBlockedPairs.none { (s, e) -> if (a == b) a in (s + 1) until e else a < e && s < b }
    }

    /**
     * `index` when it does not sit strictly inside a page the caret may not enter; otherwise the
     * nearest allowed index in movement [direction] (`>= 0` forward, `< 0` backward) - the page span's
     * end for a forward move, its start for a backward move.
     */
    fun snapOutOfBlocked(index: Int, direction: Int): Int {
        val hit = caretBlockedPairs.firstOrNull { (s, e) -> index > s && index < e } ?: return index
        return if (direction >= 0) hit.second else hit.first
    }

    companion object {

        /**
         * Builds the [EditableRegions] for [index] once the per-page overrides [pageModes] and the
         * view-wide fallback [globalMode] are known. Ids in [pageModes] that are no longer present in
         * [document] are ignored.
         */
        fun of(
            index: DocumentTextIndex,
            pageModes: Map<String, PageMode>,
            globalMode: PageMode,
            document: Document,
        ): EditableRegions {
            val length = index.length
            val full = if (length > 0) listOf(0 until length) else emptyList()

            val spans = index.blockRanges
                .groupBy { it.pageIndex }
                .mapNotNull { (pageIndex, ranges) ->
                    val id = document.pages.getOrNull(pageIndex)?.id ?: return@mapNotNull null
                    modeOf(id, pageModes, globalMode) to (ranges.minOf { it.start } to ranges.maxOf { it.end })
                }

            fun blocked(allowed: (PageMode) -> Boolean): List<Pair<Int, Int>> =
                mergeIntervals(spans.filterNot { allowed(it.first) }.map { it.second })

            fun ranges(blocked: List<Pair<Int, Int>>): List<IntRange> =
                if (blocked.isEmpty()) full else toRanges(subtract(length, blocked))

            val editBlocked = blocked { it.supportsEditing }
            val selectBlocked = blocked { it.supportsSelection }
            val caretBlocked = blocked { it.supportsCaret }
            val hiddenBlocked = blocked { it.laidOut }

            return EditableRegions(
                editableRanges = ranges(editBlocked),
                selectableRanges = ranges(selectBlocked),
                navigableRanges = ranges(caretBlocked),
                visibleRanges = ranges(hiddenBlocked),
                editBlockedPairs = editBlocked,
                caretBlockedPairs = caretBlocked,
            )
        }

        /** The mode page [pageId] is shown with: its own override, or [globalMode] when it has none. */
        fun modeOf(pageId: String, pageModes: Map<String, PageMode>, globalMode: PageMode): PageMode =
            pageModes[pageId] ?: globalMode

        /** Whether the page with [pageId] stays part of layout / scroll / hit-testing. */
        fun isPageVisible(pageId: String, pageModes: Map<String, PageMode>, globalMode: PageMode): Boolean =
            modeOf(pageId, pageModes, globalMode).laidOut

        private fun mergeIntervals(intervals: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
            if (intervals.isEmpty()) return emptyList()
            val sorted = intervals.sortedBy { it.first }
            val out = ArrayList<Pair<Int, Int>>()
            var (cs, ce) = sorted.first()
            for (i in 1 until sorted.size) {
                val (s, e) = sorted[i]
                if (s <= ce) {
                    ce = maxOf(ce, e)
                } else {
                    out += cs to ce
                    cs = s
                    ce = e
                }
            }
            out += cs to ce
            return out
        }

        private fun subtract(length: Int, blocked: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
            val out = ArrayList<Pair<Int, Int>>()
            var cursor = 0
            for ((s, e) in blocked) {
                if (s > cursor) out += cursor to s
                cursor = maxOf(cursor, e)
            }
            if (cursor < length) out += cursor to length
            return out
        }

        private fun toRanges(pairs: List<Pair<Int, Int>>): List<IntRange> = pairs.map { it.first until it.second }
    }
}
