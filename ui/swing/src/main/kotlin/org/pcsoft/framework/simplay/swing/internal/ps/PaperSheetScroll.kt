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

package org.pcsoft.framework.simplay.swing.internal.ps

import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex

/**
 * The [PaperSheetView.ScrollCommands] sink for the public `scrollToPage` / `scrollToBlock` /
 * `scrollToWord` / `scrollToSymbol` methods of a [PaperSheetView]: a pure viewport operation, never
 * touching the caret or the selection, and never gated by [PaperSheetView.mode] the way the caret
 * commands are.
 *
 * Only vertical positioning is needed - the view never scrolls horizontally - so this resolves a
 * target directly to the owning `MeasuredPage` index and its line's `y`, instead of reusing
 * [PaperSheetCaret]'s `geom`, which also resolves an `x` nothing here needs.
 *
 * @property view the owning view, for its `outerMargin` and `zoom`.
 * @property textIndex the current linear text index, or `null` without a document.
 * @property measuredDocument the current measured document, or `null` without a document.
 * @property pageTops unscaled top `y` of each page within the stack (without the outer margin).
 * @property scrollTo applies an already zoom-scaled absolute `y` as the new scroll position.
 */
internal class PaperSheetScroll(
    private val view: PaperSheetView,
    private val textIndex: () -> DocumentTextIndex?,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollTo: (Double) -> Unit,
) : PaperSheetView.ScrollCommands {

    override fun scrollToPage(page: Int) {
        val tops = pageTops()
        if (tops.isEmpty()) return
        val p = page.coerceIn(0, tops.lastIndex)
        scrollTo((view.outerMargin + tops[p]) * view.zoom)
    }

    override fun scrollToBlock(block: Int) = scrollToLinear(textIndex()?.startOfBlock(block))

    override fun scrollToWord(word: Int) = scrollToLinear(textIndex()?.startOfWord(word))

    override fun scrollToSymbol(symbol: Int) = scrollToLinear(textIndex()?.startOfSymbol(symbol))

    /** Scrolls so the line owning linear text index [linearIndex] is at the viewport top. */
    private fun scrollToLinear(linearIndex: Int?) {
        val idx = textIndex() ?: return
        val doc = measuredDocument() ?: return
        val tops = pageTops()
        if (linearIndex == null || idx.segments.isEmpty() || tops.isEmpty()) return
        val ci = linearIndex.coerceIn(0, idx.length)
        val seg = idx.segments.lastOrNull { it.start <= ci && ci <= it.end }
            ?: idx.segments.firstOrNull { it.start >= ci }
            ?: idx.segments.last()
        val pageIndex = seg.pageIndex
        if (pageIndex !in tops.indices || pageIndex !in doc.pages.indices) return
        val y = view.outerMargin + tops[pageIndex] + doc.pages[pageIndex].contentArea.y + seg.line.lineBox.y
        scrollTo(y * view.zoom)
    }
}
