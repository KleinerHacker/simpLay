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
import javafx.scene.paint.Paint
import kotlin.math.max
import kotlin.math.min
import org.pcsoft.framework.simplay.engine.planPageNumbers
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.fx.internal.CanvasRenderer
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.segmentSpanX

/** How the caret is painted: a thin line before its character, or a filled block over it. */
internal enum class CaretShape {
    LINE,
    BLOCK,
}

/**
 * The caret rectangle to paint on a page, in that page's content-area coordinates.
 *
 * @property pageIndex the page the caret sits on.
 * @property x left edge of the caret relative to the content-area left edge.
 * @property y top edge of the caret relative to the content-area top edge.
 * @property height caret height (the line height).
 * @property position the caret's linear text index; used by [CaretShape.BLOCK] to measure the
 *   character about to be overwritten.
 * @property shape [CaretShape.LINE] or [CaretShape.BLOCK] (overwrite mode).
 */
internal class CaretPaint(
    val pageIndex: Int,
    val x: Double,
    val y: Double,
    val height: Double,
    val position: Int,
    val shape: CaretShape,
)

/**
 * The visual values a [PaperSheetCanvasPainter.paint] pass draws the sheet chrome, the selection
 * highlight and the caret with. Every field defaults to the built-in look; [PaperSheetViewSkin] fills
 * it from the styleable [org.pcsoft.framework.simplay.fx.PaperSheetView] properties so a stylesheet
 * can override it.
 *
 * @property sheetBackground fill of every sheet.
 * @property sheetBorderColor stroke of every sheet border.
 * @property sheetBorderWidth stroke width of every sheet border.
 * @property shadowColor fill of the drop shadow.
 * @property shadowOffset offset of the drop shadow to the lower right.
 * @property selectionColor fill of the text selection highlight.
 * @property caretColor stroke of the edit caret.
 * @property deactivatedSheetBackground fill of a `DISABLED` sheet, instead of [sheetBackground].
 * @property deactivatedOverlayColor colour of the diagonal hatch drawn over a `DISABLED` sheet.
 */
internal class PaperSheetStyle(
    val sheetBackground: Paint = PaperSheetCanvasPainter.SHEET_COLOR,
    val sheetBorderColor: Paint = PaperSheetCanvasPainter.BORDER_COLOR,
    val sheetBorderWidth: Double = PaperSheetCanvasPainter.BORDER_WIDTH,
    val shadowColor: Paint = PaperSheetCanvasPainter.SHADOW_COLOR,
    val shadowOffset: Double = PaperSheetCanvasPainter.SHADOW_OFFSET,
    val selectionColor: Paint = PaperSheetCanvasPainter.SELECTION_COLOR,
    val caretColor: Color = PaperSheetCanvasPainter.CARET_COLOR,
    val deactivatedSheetBackground: Paint = PaperSheetCanvasPainter.DEACTIVATED_SHEET_COLOR,
    val deactivatedOverlayColor: Paint = PaperSheetCanvasPainter.DEACTIVATED_OVERLAY_COLOR,
)

/**
 * The pure canvas painting of [PaperSheetViewSkin]: given the measured document, the pre-computed
 * page tops and the current zoom / outer margin / scroll offset, it clears [canvas] and paints only
 * the pages whose vertical band intersects the viewport (simple virtualisation) - drop shadow, white
 * fill and border per sheet, the selection highlight, then the page text through [CanvasRenderer], and
 * finally the edit caret when one is supplied and visible. The colours and sizes come from the
 * supplied [PaperSheetStyle].
 *
 * [isHidden] marks pages the `HIDDEN` page-deactivation mode removes from layout entirely - they are
 * skipped and never counted as rendered. [isDisabled] marks pages the `DISABLED` mode still lays out
 * but draws with [PaperSheetStyle.deactivatedSheetBackground] plus a diagonal hatch, and on which the
 * selection highlight and the caret are suppressed.
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

    /** Number of completed [paint] calls; lets a test assert that a style change repainted once. */
    var paintCount: Int = 0
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
        style: PaperSheetStyle = PaperSheetStyle(),
        caret: CaretPaint? = null,
        caretOpacity: Double = 0.0,
        isHidden: (MeasuredPage) -> Boolean = { false },
        isDisabled: (MeasuredPage) -> Boolean = { false },
    ) {
        val gc = canvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, canvas.width, canvas.height)

        caretDrawCount = 0

        if (measured == null || measured.pages.isEmpty() || canvas.width <= 0.0 || canvas.height <= 0.0) {
            renderedPageIndices = emptyList()
            sheetChromeDrawCount = 0
            paintCount++
            return
        }

        val scrollUnscaled = scrollOffset / zoom
        val viewTop = scrollUnscaled
        val viewBottom = scrollUnscaled + canvas.height / zoom
        val hasSelection = index != null && selectionEnd > selectionStart

        gc.save()
        gc.scale(zoom, zoom)

        val numberLabels = measured.planPageNumbers(measured.raw.numbering)
        val visible = ArrayList<Int>()
        var chrome = 0
        measured.pages.forEachIndexed { i, page ->
            if (isHidden(page)) return@forEachIndexed
            val sheetTop = outerMargin + pageTops[i]
            val sheetBottom = sheetTop + page.effectiveSize.height
            if (sheetBottom >= viewTop && sheetTop <= viewBottom) {
                visible += i
                val originX = outerMargin
                val originY = sheetTop - scrollUnscaled
                val disabled = isDisabled(page)
                drawSheet(gc, page, originX, originY, style, disabled)
                chrome++
                if (hasSelection && !disabled) {
                    drawSelectionOnPage(
                        gc, index, i, selectionStart, selectionEnd, originX, originY, fonts, style.selectionColor,
                    )
                }
                CanvasRenderer.renderPage(
                    gc, page, originX = originX, originY = originY, fonts = fonts,
                    pageNumberLabel = numberLabels.getOrNull(i),
                    numberingStyle = measured.raw.numbering.textStyle,
                )
                if (caret != null && caretOpacity > 0.0 && caret.pageIndex == i && !disabled) {
                    drawCaretOnPage(
                        gc, page, caret, originX, originY, caretOpacity.coerceIn(0.0, 1.0), style.caretColor,
                        index, fonts,
                    )
                    caretDrawCount = 1
                }
            }
        }

        gc.restore()
        renderedPageIndices = visible
        sheetChromeDrawCount = chrome
        paintCount++
    }

    private fun drawSheet(
        gc: GraphicsContext,
        page: MeasuredPage,
        originX: Double,
        originY: Double,
        style: PaperSheetStyle,
        disabled: Boolean,
    ) {
        val w = page.effectiveSize.width
        val h = page.effectiveSize.height
        gc.save()
        gc.fill = style.shadowColor
        gc.fillRect(originX + style.shadowOffset, originY + style.shadowOffset, w, h)
        gc.fill = if (disabled) style.deactivatedSheetBackground else style.sheetBackground
        gc.fillRect(originX, originY, w, h)
        if (disabled) drawDeactivatedHatch(gc, originX, originY, w, h, style.deactivatedOverlayColor)
        gc.stroke = style.sheetBorderColor
        gc.lineWidth = style.sheetBorderWidth
        gc.strokeRect(originX + 0.5, originY + 0.5, w - 1.0, h - 1.0)
        gc.restore()
    }

    /** Diagonal hatch lines spaced [HATCH_SPACING] apart, clipped to the sheet rectangle. */
    private fun drawDeactivatedHatch(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double, color: Paint) {
        gc.save()
        gc.beginPath()
        gc.rect(x, y, w, h)
        gc.clip()
        gc.stroke = color
        gc.lineWidth = 1.0
        var offset = -h
        while (offset < w) {
            gc.strokeLine(x + offset, y + h, x + offset + h, y)
            offset += HATCH_SPACING
        }
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
        selectionColor: Paint,
    ) {
        val byLine = LinkedHashMap<MeasuredLine, MutableList<DocumentTextIndex.Segment>>()
        for (seg in index.segments) {
            if (seg.pageIndex != pageIndex || seg.end <= lo || seg.start >= hi) continue
            byLine.getOrPut(seg.line) { ArrayList() }.add(seg)
        }
        if (byLine.isEmpty()) return

        gc.save()
        gc.fill = selectionColor
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

    /**
     * Draws [caret]: a thin stroked line for [CaretShape.LINE], or a filled block the width of the
     * character at [CaretPaint.position] for [CaretShape.BLOCK] (falling back to the line when that
     * character can't be measured, e.g. the caret sits at the very end of its line).
     */
    private fun drawCaretOnPage(
        gc: GraphicsContext,
        page: MeasuredPage,
        caret: CaretPaint,
        originX: Double,
        originY: Double,
        alpha: Double,
        caretColor: Color,
        index: DocumentTextIndex?,
        fonts: FxFontMeasureCalculator,
    ) {
        val contentArea = page.contentArea
        val x = originX + contentArea.x + caret.x
        val yTop = originY + contentArea.y + caret.y
        val blockWidth = if (caret.shape == CaretShape.BLOCK) blockWidthAt(index, caret.position, fonts) else null
        gc.save()
        gc.globalAlpha = alpha
        if (blockWidth != null) {
            gc.fill = caretColor
            gc.fillRect(x, yTop, blockWidth, caret.height)
        } else {
            gc.stroke = caretColor
            gc.lineWidth = CARET_WIDTH
            gc.strokeLine(x, yTop, x, yTop + caret.height)
        }
        gc.restore()
    }

    /** Width of the character at [ci], or `null` when there is none there (e.g. end of the line). */
    private fun blockWidthAt(index: DocumentTextIndex?, ci: Int, fonts: FxFontMeasureCalculator): Double? {
        val idx = index ?: return null
        if (idx.segments.isEmpty()) return null
        val c = ci.coerceIn(0, idx.length)
        val seg = idx.segments.lastOrNull { it.start <= c && c <= it.end }
            ?: idx.segments.firstOrNull { it.start >= c }
            ?: idx.segments.last()
        val (x0, x1) = segmentSpanX(seg, c, c + 1, fonts)
        return (x1 - x0).takeIf { it > 0.0 }
    }

    internal companion object {

        const val SHADOW_OFFSET = 4.0
        const val BORDER_WIDTH = 1.0
        const val CARET_WIDTH = 1.5
        const val HATCH_SPACING = 10.0

        val SHEET_COLOR: Color = Color.WHITE
        val BORDER_COLOR: Color = Color.gray(0.55)
        val SHADOW_COLOR: Color = Color.rgb(0, 0, 0, 0.25)
        val SELECTION_COLOR: Color = Color.rgb(66, 133, 244, 0.35)
        val CARET_COLOR: Color = Color.rgb(20, 20, 20)
        val DEACTIVATED_SHEET_COLOR: Color = Color.gray(0.93)
        val DEACTIVATED_OVERLAY_COLOR: Color = Color.rgb(0, 0, 0, 0.12)
    }
}
