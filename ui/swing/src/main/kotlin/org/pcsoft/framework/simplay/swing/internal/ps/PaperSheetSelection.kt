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

import java.awt.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.swing.TextSelectionData
import org.pcsoft.framework.simplay.swing.TextSelectionModel
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.segmentSpanX

/**
 * Everything the text selection of a [PaperSheetView] needs: the mutable anchor/focus holder, the
 * [TextSelectionModel.Commands] sink for the public [TextSelectionModel], the selection geometry
 * (viewport bounds, hit test) and the styled-run / clipboard bookkeeping. The Swing counterpart of
 * the `fx` module's `PaperSheetSelection`.
 *
 * A per-view helper: `BasicPaperSheetUI` creates one instance, forwards its mouse events to it and
 * shares it with `PaperSheetCaret` so `Shift` + navigation can extend the same selection. Geometry
 * is read through the supplied accessors; the delegate is asked to repaint through [requestRedraw],
 * and [onProgrammaticChange] lets it cancel an in-progress mouse drag when a command changes the
 * selection from code.
 */
internal class PaperSheetSelection(
    private val view: PaperSheetView,
    private val measurer: SwingFontMeasureCalculator,
    private val textIndex: () -> DocumentTextIndex?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val requestRedraw: () -> Unit,
    private val onProgrammaticChange: () -> Unit,
) : TextSelectionModel.Commands {

    private val model = TextSelection()

    val start: Int get() = model.start
    val end: Int get() = model.end
    val isEmpty: Boolean get() = model.isEmpty
    val range: IntRange get() = model.start until model.end
    val text: String get() = model.selectedText

    //region Mutation from the delegate's mouse handling

    /** Collapses the selection at [index] (mouse press). */
    fun beginAt(index: Int) {
        model.anchor = index
        model.focus = index
    }

    /** Moves the moving end of the selection to [index] (mouse drag). */
    fun dragTo(index: Int) {
        model.focus = index
    }

    /** Selects the word around [index] (double-click). */
    fun selectWordAt(index: Int) = model.selectWordAt(index)

    /** Sets both ends of the selection; used by `PaperSheetCaret` for `Shift` + navigation. */
    fun setAnchorFocus(anchor: Int, focus: Int) {
        model.anchor = anchor
        model.focus = focus
    }

    /** Collapses the selection back to the document start. */
    fun reset() = model.reset()

    /** Rebinds the selection to [index] and clears it (after a re-measure). */
    fun onDocumentChanged(index: DocumentTextIndex?) {
        model.index = index
        model.reset()
    }

    /** Copies the current selection to the system clipboard as styled HTML + RTF + plain text. */
    fun putStyledSelectionOnClipboard(): Boolean = model.putStyledSelectionOnClipboard()

    //endregion

    //region Geometry

    /** The bounding box of the current selection in viewport pixels, or `null` when empty. */
    fun viewportBounds(): Rectangle? {
        val idx = textIndex() ?: return null
        if (model.isEmpty) return null
        val zoom = view.zoom
        val outer = view.outerMargin
        val scroll = scrollOffset()
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        val lo = model.start
        val hi = model.end
        for (seg in idx.segments) {
            if (seg.end <= lo || seg.start >= hi) continue
            val (x0, x1) = segmentSpanX(seg, lo, hi, measurer)
            val contentArea = seg.page.contentArea
            val absX0 = outer + contentArea.x + x0
            val absX1 = outer + contentArea.x + x1
            val absY0 = outer + pageTops()[seg.pageIndex] + contentArea.y + seg.line.lineBox.y
            val absY1 = absY0 + seg.line.lineBox.height
            minX = min(minX, absX0)
            maxX = max(maxX, absX1)
            minY = min(minY, absY0)
            maxY = max(maxY, absY1)
        }
        if (minX > maxX) return null
        return Rectangle(
            (minX * zoom).roundToInt(),
            (minY * zoom - scroll).roundToInt(),
            ((maxX - minX) * zoom).roundToInt(),
            ((maxY - minY) * zoom).roundToInt(),
        )
    }

    /** Whether a viewport point sits inside the current selection box. */
    fun contains(x: Double, y: Double): Boolean =
        viewportBounds()?.contains(x, y) ?: false

    //endregion

    /** Writes the selected text, range, viewport bounds and styled runs onto [TextSelectionModel]. */
    fun publish() {
        val idx = textIndex()
        if (idx == null || model.isEmpty) {
            view.selectionModel.update("", 0, 0, null, emptyList())
            return
        }
        val lo = model.start
        val hi = model.end
        val runs = idx.styledRuns(lo, hi).mapNotNull { run ->
            val font = run.font ?: return@mapNotNull null
            TextSelectionData(
                text = run.text,
                fontFamily = font.family,
                fontSize = font.size,
                bold = font.weight == FontWeight.BOLD,
                italic = font.style == FontStyle.ITALIC,
            )
        }
        view.selectionModel.update(idx.substring(lo, hi), lo, hi, viewportBounds(), runs)
    }

    //region TextSelectionModel.Commands (public model commands)

    override fun selectRange(start: Int, end: Int) {
        if (textIndex() == null) return
        model.selectRange(start, end)
        onProgrammaticChange()
        requestRedraw()
    }

    override fun selectAll() {
        model.selectAll()
        onProgrammaticChange()
        requestRedraw()
    }

    override fun clearSelection() {
        model.reset()
        onProgrammaticChange()
        requestRedraw()
    }

    //endregion
}
