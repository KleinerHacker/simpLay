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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import kotlin.math.max
import kotlin.math.min
import org.pcsoft.framework.simplay.engine.planPageNumbers
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.swing.internal.Graphics2DDocumentRenderer
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.segmentSpanX

/**
 * The caret rectangle to paint on a page, in that page's content-area coordinates. Swing counterpart
 * of the `fx` module's `CaretPaint`.
 */
internal class CaretPaint(val pageIndex: Int, val x: Double, val y: Double, val height: Double)

/**
 * The pure viewport painting of `BasicPaperSheetUI`: given the measured document, the pre-computed
 * page tops and the current zoom / outer margin / scroll offset, it paints only the pages whose
 * vertical band intersects the viewport (simple virtualisation) - drop shadow, fill and border per
 * sheet, the selection highlight, then the page text through [Graphics2DDocumentRenderer], and
 * finally the edit caret when one is supplied and visible. The Swing counterpart of the `fx` module's
 * `PaperSheetCanvasPainter`.
 *
 * [isHidden] marks pages the `HIDDEN` page-deactivation mode removes from layout entirely - they are
 * skipped and never counted as rendered. [isDisabled] marks pages the `DISABLED` mode still lays out
 * but draws with [PaperSheetStyle.deactivatedSheetBackground] plus a diagonal hatch, and on which the
 * selection highlight and the caret are suppressed.
 */
internal class PaperSheetSwingPainter {

    var renderedPageIndices: List<Int> = emptyList()
        private set

    var sheetChromeDrawCount: Int = 0
        private set

    var caretDrawCount: Int = 0
        private set

    var paintCount: Int = 0
        private set

    /**
     * Paints into [g], clipped to `0, 0, viewportWidth x viewportHeight` (the caller clears the
     * background). A `null` / empty [measured] or a zero-sized viewport just resets the counters.
     */
    fun paint(
        g: Graphics2D,
        viewportWidth: Double,
        viewportHeight: Double,
        measured: MeasuredDocument?,
        pageTops: DoubleArray,
        zoom: Double,
        outerMargin: Double,
        scrollOffset: Double,
        index: DocumentTextIndex?,
        selectionStart: Int,
        selectionEnd: Int,
        fonts: SwingFontMeasureCalculator,
        style: PaperSheetStyle = PaperSheetStyle(),
        caret: CaretPaint? = null,
        caretOpacity: Double = 0.0,
        isHidden: (MeasuredPage) -> Boolean = { false },
        isDisabled: (MeasuredPage) -> Boolean = { false },
    ) {
        caretDrawCount = 0

        if (measured == null || measured.pages.isEmpty() || viewportWidth <= 0.0 || viewportHeight <= 0.0) {
            renderedPageIndices = emptyList()
            sheetChromeDrawCount = 0
            paintCount++
            return
        }

        val scrollUnscaled = scrollOffset / zoom
        val viewTop = scrollUnscaled
        val viewBottom = scrollUnscaled + viewportHeight / zoom
        val hasSelection = index != null && selectionEnd > selectionStart

        val saved = g.transform
        g.scale(zoom, zoom)

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
                drawSheet(g, page, originX, originY, style, disabled)
                chrome++
                if (hasSelection && !disabled) {
                    drawSelectionOnPage(
                        g, index, i, selectionStart, selectionEnd, originX, originY, fonts, style.selectionColor,
                    )
                }
                Graphics2DDocumentRenderer.renderPage(
                    g, page, originX = originX, originY = originY, fonts = fonts,
                    pageNumberLabel = numberLabels.getOrNull(i),
                    numberingStyle = measured.raw.numbering.textStyle,
                )
                if (caret != null && caretOpacity > 0.0 && caret.pageIndex == i && !disabled) {
                    drawCaretOnPage(g, page, caret, originX, originY, caretOpacity.coerceIn(0.0, 1.0), style.caretColor)
                    caretDrawCount = 1
                }
            }
        }

        g.transform = saved
        renderedPageIndices = visible
        sheetChromeDrawCount = chrome
        paintCount++
    }

    private fun drawSheet(
        g: Graphics2D,
        page: MeasuredPage,
        originX: Double,
        originY: Double,
        style: PaperSheetStyle,
        disabled: Boolean,
    ) {
        val w = page.effectiveSize.width
        val h = page.effectiveSize.height
        val savedPaint = g.paint
        val savedStroke = g.stroke
        g.paint = style.shadowColor
        g.fill(Rectangle2D.Double(originX + style.shadowOffset, originY + style.shadowOffset, w, h))
        g.paint = if (disabled) style.deactivatedSheetBackground else style.sheetBackground
        g.fill(Rectangle2D.Double(originX, originY, w, h))
        if (disabled) drawDeactivatedHatch(g, originX, originY, w, h, style.deactivatedOverlayColor)
        g.paint = style.sheetBorderColor
        g.stroke = BasicStroke(style.sheetBorderWidth.toFloat())
        g.draw(Rectangle2D.Double(originX + 0.5, originY + 0.5, w - 1.0, h - 1.0))
        g.paint = savedPaint
        g.stroke = savedStroke
    }

    /** Diagonal hatch lines spaced [HATCH_SPACING] apart, clipped to the sheet rectangle. */
    private fun drawDeactivatedHatch(g: Graphics2D, x: Double, y: Double, w: Double, h: Double, color: Paint) {
        val savedPaint = g.paint
        val savedStroke = g.stroke
        val savedClip = g.clip
        g.clip(Rectangle2D.Double(x, y, w, h))
        g.paint = color
        g.stroke = BasicStroke(1.0f)
        var offset = -h
        while (offset < w) {
            g.draw(Line2D.Double(x + offset, y + h, x + offset + h, y))
            offset += HATCH_SPACING
        }
        g.clip = savedClip
        g.paint = savedPaint
        g.stroke = savedStroke
    }

    /**
     * Fills the selection highlight for [pageIndex]. Covered parts are grouped by line and drawn as
     * one rectangle per line, from the start of the first covered part to the end of the last.
     */
    private fun drawSelectionOnPage(
        g: Graphics2D,
        index: DocumentTextIndex,
        pageIndex: Int,
        lo: Int,
        hi: Int,
        originX: Double,
        originY: Double,
        fonts: SwingFontMeasureCalculator,
        selectionColor: Paint,
    ) {
        val byLine = LinkedHashMap<MeasuredLine, MutableList<DocumentTextIndex.Segment>>()
        for (seg in index.segments) {
            if (seg.pageIndex != pageIndex || seg.end <= lo || seg.start >= hi) continue
            byLine.getOrPut(seg.line) { ArrayList() }.add(seg)
        }
        if (byLine.isEmpty()) return

        val savedPaint = g.paint
        g.paint = selectionColor
        for ((line, segs) in byLine) {
            var x0 = Double.MAX_VALUE
            var x1 = -Double.MAX_VALUE
            for (seg in segs) {
                val (a, b) = segmentSpanX(seg, lo, hi, fonts)
                x0 = min(x0, a)
                x1 = max(x1, b)
            }
            val contentArea = segs.first().page.contentArea
            g.fill(
                Rectangle2D.Double(
                    originX + contentArea.x + x0,
                    originY + contentArea.y + line.lineBox.y,
                    (x1 - x0).coerceAtLeast(1.0),
                    line.lineBox.height,
                ),
            )
        }
        g.paint = savedPaint
    }

    private fun drawCaretOnPage(
        g: Graphics2D,
        page: MeasuredPage,
        caret: CaretPaint,
        originX: Double,
        originY: Double,
        alpha: Double,
        caretColor: Color,
    ) {
        val contentArea = page.contentArea
        val x = originX + contentArea.x + caret.x
        val yTop = originY + contentArea.y + caret.y
        val savedPaint = g.paint
        val savedStroke = g.stroke
        val savedComposite = g.composite
        g.composite = java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha.toFloat())
        g.paint = caretColor
        g.stroke = BasicStroke(PaperSheetStyle.CARET_WIDTH)
        g.draw(Line2D.Double(x, yTop, x, yTop + caret.height))
        g.composite = savedComposite
        g.paint = savedPaint
        g.stroke = savedStroke
    }

    private companion object {
        const val HATCH_SPACING = 10.0
    }
}
