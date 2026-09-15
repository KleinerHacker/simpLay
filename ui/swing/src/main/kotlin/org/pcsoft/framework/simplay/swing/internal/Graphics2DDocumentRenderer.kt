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

package org.pcsoft.framework.simplay.swing.internal

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import org.pcsoft.framework.simplay.engine.PageNumberLabel
import org.pcsoft.framework.simplay.engine.planPageNumbers
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Module-internal drawing of a measured tree onto an AWT [Graphics2D]: the surface-free measured-tree
 * walk plus the single-page and whole-document draw entry points. The Swing counterpart of the `fx`
 * module's `CanvasRenderer`; the public `DocumentImageRenderer` is the only consumer.
 */
internal object Graphics2DDocumentRenderer {

    /** Called once per page before its text is drawn, with the page and its top-left origin. */
    fun interface PageFrameDecorator {
        fun decorate(g: Graphics2D, page: MeasuredPage, originX: Double, originY: Double)
    }

    /** Called once for the gap between two stacked pages, with the gap band and the document width. */
    fun interface PageSeparatorDecorator {
        fun decorate(g: Graphics2D, gapTop: Double, gapBottom: Double, width: Double)
    }

    /**
     * The measured-tree walk without a drawing surface: iterates the blocks, lines and parts of
     * [page] and reports each part's absolute pen position `(x, baselineY)` on the target surface
     * (offset by [originX] / [originY]), following `docs/docs/engine/rendering.md`.
     */
    fun walkPage(
        page: MeasuredPage,
        originX: Double = 0.0,
        originY: Double = 0.0,
        onBlockFont: (MeasuredFont) -> Unit,
        onPart: (text: String, x: Double, baselineY: Double) -> Unit,
    ) {
        val contentArea = page.contentArea
        for (block in page.blocks) {
            onBlockFont(block.style.font)
            for (line in block.lines) {
                val baselineY = originY + contentArea.y + line.lineBox.y + line.baseline
                for (part in line.parts) {
                    onPart(part.text, originX + contentArea.x + part.bounds.x, baselineY)
                }
            }
        }
    }

    /**
     * Draws one measured [page] onto [g], offset by ([originX], [originY]). Every part becomes one
     * [Graphics2D.drawString] on its line baseline. [fonts] resolves the AWT font per block.
     * [pageFrame] runs before the text if given. [pageNumberLabel], when given, is drawn last using
     * [numberingStyle] for its font.
     */
    fun renderPage(
        g: Graphics2D,
        page: MeasuredPage,
        originX: Double = 0.0,
        originY: Double = 0.0,
        fonts: SwingFontMeasureCalculator = SwingFontMeasureCalculator(),
        pageFrame: PageFrameDecorator? = null,
        pageNumberLabel: PageNumberLabel? = null,
        numberingStyle: TextStyle = PageNumbering.DEFAULT_NUMBER_STYLE,
    ) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        pageFrame?.decorate(g, page, originX, originY)
        val savedPaint = g.paint
        // The engine model carries no per-run text colour yet, so the document text is always
        // painted black - matching the JavaFX renderer, whose default fill is black.
        g.paint = Color.BLACK
        walkPage(
            page = page,
            originX = originX,
            originY = originY,
            onBlockFont = { g.font = fonts.toAwtFont(it.raw) },
            onPart = { text, x, baselineY -> if (text.isNotEmpty()) g.drawString(text, x.toFloat(), baselineY.toFloat()) },
        )
        g.paint = savedPaint
        if (pageNumberLabel != null) {
            drawPageNumber(g, pageNumberLabel, originX, originY, numberingStyle, fonts)
        }
    }

    /**
     * Draws a whole measured [document] onto [g], pages stacked vertically with [gap] layout units
     * between them. [pageFrame] runs per page, [pageSeparator] runs per inter-page gap; both optional.
     * [numbering] configures the page numbers drawn per page; defaults to [PageNumbering.OFF].
     */
    fun renderDocument(
        g: Graphics2D,
        document: MeasuredDocument,
        gap: Double,
        fonts: SwingFontMeasureCalculator = SwingFontMeasureCalculator(),
        pageFrame: PageFrameDecorator? = null,
        pageSeparator: PageSeparatorDecorator? = null,
        numbering: PageNumbering = PageNumbering.OFF,
    ) {
        val width = if (document.pages.isEmpty()) 0.0 else document.pages.maxOf { it.effectiveSize.width }
        val labels = document.planPageNumbers(numbering)
        var y = 0.0
        document.pages.forEachIndexed { index, page ->
            renderPage(
                g,
                page,
                originX = 0.0,
                originY = y,
                fonts = fonts,
                pageFrame = pageFrame,
                pageNumberLabel = labels.getOrNull(index),
                numberingStyle = numbering.textStyle,
            )
            y += page.effectiveSize.height
            if (index != document.pages.lastIndex) {
                pageSeparator?.decorate(g, y, y + gap, width)
                y += gap
            }
        }
    }

    /**
     * Draws [label] onto [g], offset by ([originX], [originY]), using [textStyle] resolved through
     * [fonts]. AWT has no built-in text alignment, so the pen x is shifted by the label's measured
     * width for [TextAlignment.CENTER] / [TextAlignment.RIGHT], and the baseline is derived from
     * [label]'s vertical anchor plus half the font's ascent/descent spread. The graphics-context font
     * and paint are restored afterwards.
     */
    private fun drawPageNumber(
        g: Graphics2D,
        label: PageNumberLabel,
        originX: Double,
        originY: Double,
        textStyle: TextStyle,
        fonts: SwingFontMeasureCalculator,
    ) {
        val metrics = fonts.measure(textStyle.font, label.text)
        val x = when (label.alignment) {
            TextAlignment.LEFT -> label.x
            TextAlignment.CENTER, TextAlignment.JUSTIFY -> label.x - metrics.width / 2.0
            TextAlignment.RIGHT -> label.x - metrics.width
        }
        val baselineY = label.y + (metrics.ascent - metrics.descent) / 2.0

        val savedFont = g.font
        val savedPaint = g.paint
        g.font = fonts.toAwtFont(textStyle.font)
        g.paint = Color.BLACK
        g.drawString(label.text, (originX + x).toFloat(), (originY + baselineY).toFloat())
        g.font = savedFont
        g.paint = savedPaint
    }
}
