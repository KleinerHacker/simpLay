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

package org.pcsoft.framework.simplay.fx.internal.ps

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.fx.internal.CanvasRenderer
import org.pcsoft.framework.simplay.fx.internal.DocumentTextIndex
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.fx.internal.segmentSpanX

/**
 * The caret rectangle to paint on a page, in that page's content-area coordinates.
 *
 * @property pageIndex the page the caret sits on.
 * @property x left edge of the caret relative to the content-area left edge.
 * @property y top edge of the caret relative to the content-area top edge.
 * @property height caret height (the line height).
 */
internal class CaretPaint(val pageIndex: Int, val x: Double, val y: Double, val height: Double)

/**
 * The pure canvas painting of [PaperSheetViewSkin]: given the measured document, the pre-computed
 * page tops and the current zoom / outer margin / scroll offset, it clears [canvas] and paints only
 * the pages whose vertical band intersects the viewport (simple virtualisation) - drop shadow, white
 * fill and border per sheet, the selection highlight, then the page text through [CanvasRenderer],
 * and finally the edit caret when one is supplied and visible.
 *
 * The painter owns no state beyond the counters it reports for the last [paint]; measuring, layout,
 * scrolling, selection tracking, caret tracking and input handling stay in the skin.
 */
internal class PaperSheetCanvasPainter(private val canvas: Canvas) {

    /** Page indices painted in the last [paint] call. */
    var renderedPageIndices: List<Int> = emptyList()
        private set

    /** Number of sheets (border + shadow) painted in the last [paint] call. */
    var sheetChromeDrawCount: Int = 0
        private set

    /** Number of caret strokes drawn in the last [paint] call (`0` or `1`). */
    var caretDrawCount: Int = 0
        private set

    /**
     * Repaints [canvas]. A `null` / empty [measured] or a zero-sized canvas clears the canvas and
     * resets the counters. The selection highlight is drawn when [index] is set and
     * `selectionEnd > selectionStart`; the [caret] is drawn when it is non-`null`, [caretOpacity] is
     * greater than `0` and its page is in view, stroked with that opacity so a smooth blink can fade
     * it in and out.
     */
    fun paint(
        measured: MeasuredDocument?,
        pageTops: DoubleArray,
        zoom: Double,
        outerMargin: Double,
        scrollOffset: Double,
        index: DocumentTextIndex?,
        selectionStart: Int,
        selectionEnd: Int,
        fonts: FxFontMeasureCalculator,
        caret: CaretPaint? = null,
        caretOpacity: Double = 0.0,
    ) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)

        caretDrawCount = 0

        if (measured == null || measured.pages.isEmpty() || canvas.width <= 0.0 || canvas.height <= 0.0) {
            renderedPageIndices = emptyList()
            sheetChromeDrawCount = 0
            return
        }

        val scrollUnscaled = scrollOffset / zoom
        val viewTop = scrollUnscaled
        val viewBottom = scrollUnscaled + canvas.height / zoom
        val hasSelection = index != null && selectionEnd > selectionStart

        gc.save()
        gc.scale(zoom, zoom)

        val visible = ArrayList<Int>()
        var chrome = 0
        measured.pages.forEachIndexed { i, page ->
            val sheetTop = outerMargin + pageTops[i]
            val sheetBottom = sheetTop + page.effectiveSize.height
            if (sheetBottom >= viewTop && sheetTop <= viewBottom) {
                visible += i
                val originX = outerMargin
                val originY = sheetTop - scrollUnscaled
                drawSheet(gc, page, originX, originY)
                chrome++
                if (hasSelection) {
                    drawSelectionOnPage(gc, index, i, selectionStart, selectionEnd, originX, originY, fonts)
                }
                CanvasRenderer.renderPage(gc, page, originX = originX, originY = originY, fonts = fonts)
                if (caret != null && caretOpacity > 0.0 && caret.pageIndex == i) {
                    drawCaretOnPage(gc, page, caret, originX, originY, caretOpacity.coerceIn(0.0, 1.0))
                    caretDrawCount = 1
                }
            }
        }

        gc.restore()
        renderedPageIndices = visible
        sheetChromeDrawCount = chrome
    }

    private fun drawSheet(gc: GraphicsContext, page: MeasuredPage, originX: Double, originY: Double) {
        val w = page.effectiveSize.width
        val h = page.effectiveSize.height
        gc.save()
        gc.fill = SHADOW_COLOR
        gc.fillRect(originX + SHADOW_OFFSET, originY + SHADOW_OFFSET, w, h)
        gc.fill = SHEET_COLOR
        gc.fillRect(originX, originY, w, h)
        gc.stroke = BORDER_COLOR
        gc.lineWidth = BORDER_WIDTH
        gc.strokeRect(originX + 0.5, originY + 0.5, w - 1.0, h - 1.0)
        gc.restore()
    }

    /**
     * Fills the selection highlight for [pageIndex]. Covered parts are grouped by line and drawn as
     * one rectangle per line, from the start of the first covered part to the end of the last, so the
     * whitespace between selected words is highlighted as well.
     */
    private fun drawSelectionOnPage(
        gc: GraphicsContext,
        index: DocumentTextIndex,
        pageIndex: Int,
        lo: Int,
        hi: Int,
        originX: Double,
        originY: Double,
        fonts: FxFontMeasureCalculator,
    ) {
        val byLine = LinkedHashMap<MeasuredLine, MutableList<DocumentTextIndex.Segment>>()
        for (seg in index.segments) {
            if (seg.pageIndex != pageIndex || seg.end <= lo || seg.start >= hi) continue
            byLine.getOrPut(seg.line) { ArrayList() }.add(seg)
        }
        if (byLine.isEmpty()) return

        gc.save()
        gc.fill = SELECTION_COLOR
        for ((line, segs) in byLine) {
            var x0 = Double.MAX_VALUE
            var x1 = -Double.MAX_VALUE
            for (seg in segs) {
                val (a, b) = segmentSpanX(seg, lo, hi, fonts)
                x0 = min(x0, a)
                x1 = max(x1, b)
            }
            val contentArea = segs.first().page.contentArea
            gc.fillRect(
                originX + contentArea.x + x0,
                originY + contentArea.y + line.lineBox.y,
                (x1 - x0).coerceAtLeast(1.0),
                line.lineBox.height,
            )
        }
        gc.restore()
    }

    private fun drawCaretOnPage(
        gc: GraphicsContext,
        page: MeasuredPage,
        caret: CaretPaint,
        originX: Double,
        originY: Double,
        alpha: Double,
    ) {
        val contentArea = page.contentArea
        val x = originX + contentArea.x + caret.x
        val yTop = originY + contentArea.y + caret.y
        gc.save()
        gc.globalAlpha = alpha
        gc.stroke = CARET_COLOR
        gc.lineWidth = CARET_WIDTH
        gc.strokeLine(x, yTop, x, yTop + caret.height)
        gc.restore()
    }

    private companion object {

        const val SHADOW_OFFSET = 4.0
        const val BORDER_WIDTH = 1.0
        const val CARET_WIDTH = 1.5

        val SHEET_COLOR: Color = Color.WHITE
        val BORDER_COLOR: Color = Color.gray(0.55)
        val SHADOW_COLOR: Color = Color.rgb(0, 0, 0, 0.25)
        val SELECTION_COLOR: Color = Color.rgb(66, 133, 244, 0.35)
        val CARET_COLOR: Color = Color.rgb(20, 20, 20)
    }
}
