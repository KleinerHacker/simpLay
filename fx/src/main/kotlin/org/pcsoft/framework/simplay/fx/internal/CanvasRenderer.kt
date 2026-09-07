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

import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage

/**
 * Module-internal drawing of a measured tree onto a JavaFX [GraphicsContext]: the surface-free
 * measured-tree walk plus the single-page and whole-document draw entry points. The public
 * `org.pcsoft.framework.simplay.fx.canvas.CanvasDocumentRenderer` is the only consumer.
 */
internal object CanvasRenderer {

    /**
     * Called once per page before its text is drawn, with the page and its top-left origin on the
     * target surface. Used to paint the sheet frame; the canvas renderer leaves it `null`.
     */
    fun interface PageFrameDecorator {
        fun decorate(gc: GraphicsContext, page: MeasuredPage, originX: Double, originY: Double)
    }

    /**
     * Called once for the gap between two stacked pages, with the gap band `[gapTop, gapBottom]` and
     * the document width. Used to paint the dashed page-break line.
     */
    fun interface PageSeparatorDecorator {
        fun decorate(gc: GraphicsContext, gapTop: Double, gapBottom: Double, width: Double)
    }

    /**
     * The measured-tree walk without a drawing surface: iterates the blocks, lines and parts of
     * [page] and reports each part's absolute position on the target surface (offset by [originX] /
     * [originY]).
     *
     * [onBlockFont] fires once per block before its parts, [onPart] fires once per
     * [org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart] with the text and its pen
     * position `(x, baselineY)` following `docs/docs/engine/rendering.md`. [renderPage] is the
     * drawing consumer of this walk.
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
     * Draws one measured [page] onto [gc], offset by ([originX], [originY]) on the target surface.
     * Every part becomes one [GraphicsContext.fillText] on its line baseline. [fonts] resolves the
     * JavaFX font per block. [pageFrame] runs before the text if given.
     */
    fun renderPage(
        gc: GraphicsContext,
        page: MeasuredPage,
        originX: Double = 0.0,
        originY: Double = 0.0,
        fonts: FxFontMeasureCalculator = FxFontMeasureCalculator(),
        pageFrame: PageFrameDecorator? = null,
    ) {
        pageFrame?.decorate(gc, page, originX, originY)
        gc.textBaseline = VPos.BASELINE
        walkPage(
            page = page,
            originX = originX,
            originY = originY,
            onBlockFont = { gc.font = fonts.toFxFont(it.raw) },
            onPart = { text, x, baselineY -> gc.fillText(text, x, baselineY) },
        )
    }

    /**
     * Draws a whole measured [document] onto [gc], pages stacked vertically with [gap] layout units
     * between them. [pageFrame] runs per page, [pageSeparator] runs per inter-page gap; both
     * optional.
     */
    fun renderDocument(
        gc: GraphicsContext,
        document: MeasuredDocument,
        gap: Double,
        fonts: FxFontMeasureCalculator = FxFontMeasureCalculator(),
        pageFrame: PageFrameDecorator? = null,
        pageSeparator: PageSeparatorDecorator? = null,
    ) {
        val width = if (document.pages.isEmpty()) 0.0 else document.pages.maxOf { it.effectiveSize.width }
        var y = 0.0
        document.pages.forEachIndexed { index, page ->
            renderPage(gc, page, originX = 0.0, originY = y, fonts = fonts, pageFrame = pageFrame)
            y += page.effectiveSize.height
            if (index != document.pages.lastIndex) {
                pageSeparator?.decorate(gc, y, y + gap, width)
                y += gap
            }
        }
    }
}
