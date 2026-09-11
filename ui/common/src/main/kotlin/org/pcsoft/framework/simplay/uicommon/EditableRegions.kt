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
 * The editable, navigable and visible linear-text regions of a document once a set of pages has been
 * deactivated, shared by the `fx` and `swing` `PaperSheetView` implementations.
 *
 * Built once per [of] call from a [DocumentTextIndex], the set of deactivated page ids and a
 * [PageDeactivationMode]. A raw block is "blocked" when the id of the raw page it lives on
 * (`DocumentTextIndex.BlockRange.pageIndex` into `document.pages`) is in `deactivatedPageIds`. All
 * blocked blocks of one page form a single blocked span, from the first block's start to the last
 * block's end, so the separators between them are blocked as well and the caret cannot come to rest
 * between two blocks of a deactivated page.
 *
 * * [editableRanges] - where a mutation may land ([DISABLED][PageDeactivationMode.DISABLED] and
 *   [READONLY][PageDeactivationMode.READONLY] both exclude the blocked ranges).
 * * [navigableRanges] - where the caret may come to rest after a plain (non-`Shift`) navigation move
 *   ([DISABLED][PageDeactivationMode.DISABLED] and [HIDDEN][PageDeactivationMode.HIDDEN] exclude the
 *   blocked ranges, every other mode is the full document).
 * * [visibleRanges] - what stays part of layout and hit-testing ([HIDDEN][PageDeactivationMode.HIDDEN]
 *   excludes the blocked ranges, every other mode is the full document).
 *
 * At [PageDeactivationMode.IGNORE] or with an empty `deactivatedPageIds` every region is simply the
 * whole document and [isEditRangeAllowed] / [snapOutOfBlocked] never intervene.
 */
class EditableRegions private constructor(
    val editableRanges: List<IntRange>,
    val navigableRanges: List<IntRange>,
    val visibleRanges: List<IntRange>,
    private val blockedPairs: List<Pair<Int, Int>>,
) {

    /**
     * Whether the half-open mutation range `[lo, hi)` (order-independent; `lo == hi` for an insertion
     * point) may be applied. A zero-length range is rejected only when it sits strictly inside a
     * blocked range; its boundaries (the edges of the active text) are always allowed.
     */
    fun isEditRangeAllowed(lo: Int, hi: Int): Boolean {
        val a = minOf(lo, hi)
        val b = maxOf(lo, hi)
        return blockedPairs.none { (s, e) -> if (a == b) a in (s + 1) until e else a < e && s < b }
    }

    /**
     * `index` when it does not sit strictly inside a blocked range; otherwise the nearest allowed
     * index in movement [direction] (`>= 0` forward, `< 0` backward) - the blocked range's end for a
     * forward move, its start for a backward move.
     */
    fun snapOutOfBlocked(index: Int, direction: Int): Int {
        val hit = blockedPairs.firstOrNull { (s, e) -> index > s && index < e } ?: return index
        return if (direction >= 0) hit.second else hit.first
    }

    companion object {

        /** Builds the [EditableRegions] for [index] once [deactivatedPageIds] and [mode] are known. */
        fun of(
            index: DocumentTextIndex,
            deactivatedPageIds: Set<String>,
            mode: PageDeactivationMode,
            document: Document,
        ): EditableRegions {
            val length = index.length
            val full = if (length > 0) listOf(0 until length) else emptyList()

            if (mode == PageDeactivationMode.IGNORE || deactivatedPageIds.isEmpty()) {
                return EditableRegions(full, full, full, emptyList())
            }

            val blockedPairs = mergeIntervals(
                index.blockRanges
                    .filter { document.pages.getOrNull(it.pageIndex)?.id in deactivatedPageIds }
                    .groupBy { it.pageIndex }
                    .map { (_, ranges) -> ranges.minOf { it.start } to ranges.maxOf { it.end } },
            )
            if (blockedPairs.isEmpty()) {
                return EditableRegions(full, full, full, emptyList())
            }

            val subtracted = toRanges(subtract(length, blockedPairs))
            return when (mode) {
                PageDeactivationMode.DISABLED -> EditableRegions(subtracted, subtracted, full, blockedPairs)
                PageDeactivationMode.READONLY -> EditableRegions(subtracted, full, full, blockedPairs)
                PageDeactivationMode.HIDDEN -> EditableRegions(subtracted, subtracted, subtracted, blockedPairs)
                PageDeactivationMode.IGNORE -> EditableRegions(full, full, full, emptyList())
            }
        }

        /** Whether the page with [pageId] stays part of layout / scroll / hit-testing. */
        fun isPageVisible(pageId: String, deactivatedPageIds: Set<String>, mode: PageDeactivationMode): Boolean =
            !(mode == PageDeactivationMode.HIDDEN && pageId in deactivatedPageIds)

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
