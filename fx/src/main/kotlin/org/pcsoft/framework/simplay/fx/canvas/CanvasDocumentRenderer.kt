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

package org.pcsoft.framework.simplay.fx.canvas

import javafx.geometry.Dimension2D
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.pcsoft.framework.simplay.engine.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.fx.internal.CanvasRenderer
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.fx.internal.MeasuredCanvasData
import org.pcsoft.framework.simplay.fx.internal.measureForCanvas

/**
 * Public renderer that paints one fixed [Document] - whole, or a single page - onto a JavaFX
 * [Canvas].
 *
 * The document is bound at creation through the [for] factory, so measuring and the whole canvas
 * layout (sizes, page origins, page-break positions) are computed once, up front:
 *
 * ```kotlin
 * val renderer = CanvasDocumentRenderer.`for`(document) {
 *     unitScale = 1.5
 *     pageGap = 16.0
 *     lineBreakerStrategy = NoWrapLineBreakerStrategy
 * }
 * val canvas = renderer.renderDocument()
 * val firstPageSize = renderer.pageCanvasSizes[0]
 * ```
 *
 * Pages are stacked vertically, one [CanvasRenderConfiguration.pageGap] apart, and every page
 * boundary is marked with a dashed line drawn in the middle of the gap. Every layout coordinate and
 * the resulting canvas size are multiplied by [CanvasRenderConfiguration.unitScale].
 *
 * The instance is immutable and single-document; it is not thread-safe. Because the document is
 * measured in the [for] factory, it must be called with the JavaFX toolkit already initialised
 * (an `Application` is running, or a headless toolkit was started).
 *
 * **Canvas sizing.** A JavaFX `Canvas` can be resized through its `width` / `height` properties at
 * any time, but very tall documents run into the backend's maximum texture size (commonly a few
 * thousand to `16384` pixels per axis) and into plain heap pressure, and a resize clears the canvas.
 * This renderer therefore sizes the canvas up front from [documentCanvasSize] and does no tiling; a
 * document taller than the platform limit is out of scope and would need a tiling follow-up.
 */
class CanvasDocumentRenderer private constructor(
    document: Document,
    private val config: CanvasRenderConfiguration,
    private val measurer: FxFontMeasureCalculator,
) {

    private val measured: MeasuredDocument = document.measure(measurer, config)

    private val canvasData: MeasuredCanvasData = measureForCanvas(measured, config.unitScale, config.pageGap)

    /** The number of pages the bound document measures to, after flow continuation and growth. */
    val pageCount: Int = measured.pages.size

    /**
     * The per-page canvas sizes, addressed by page index: `renderer.pageCanvasSizes[pageIndex]`.
     */
    val pageCanvasSizes: PageCanvasSizes = PageCanvasSizes()

    /**
     * Draws every page of the bound document onto a [Canvas].
     *
     * @param canvas an existing canvas to draw into; it is grown if it is smaller than the required
     *   size and never shrunk or replaced. When `null` a new canvas of exactly the required size is
     *   created.
     * @return the canvas that was drawn into (the passed one, or a freshly created one).
     */
    fun renderDocument(canvas: Canvas? = null): Canvas {
        val target = resolveCanvas(canvas, canvasData.canvasSize)

        val gc = target.graphicsContext2D
        gc.clearRect(0.0, 0.0, target.width, target.height)
        gc.save()
        gc.scale(config.unitScale, config.unitScale)
        CanvasRenderer.renderDocument(
            gc = gc,
            document = measured,
            gap = config.pageGap,
            fonts = measurer,
            pageSeparator = { g, gapTop, gapBottom, width -> drawSeparator(g, gapTop, gapBottom, width) },
        )
        gc.restore()
        return target
    }

    /**
     * Draws exactly the page at [pageIndex] of the bound document onto a [Canvas], with its top at
     * the canvas origin.
     *
     * @param pageIndex the zero-based page to draw.
     * @param canvas an existing canvas to draw into; grown if too small, never shrunk or replaced.
     *   When `null` a new canvas of exactly the page size is created.
     * @return the canvas that was drawn into.
     * @throws IllegalArgumentException if [pageIndex] is outside the document's page range.
     */
    fun renderPage(pageIndex: Int, canvas: Canvas? = null): Canvas {
        val page = pageAt(pageIndex)
        val target = resolveCanvas(canvas, scaledPageSize(page))

        val gc = target.graphicsContext2D
        gc.clearRect(0.0, 0.0, target.width, target.height)
        gc.save()
        gc.scale(config.unitScale, config.unitScale)
        CanvasRenderer.renderPage(gc = gc, page = page, fonts = measurer)
        gc.restore()
        return target
    }

    /**
     * The canvas size [renderDocument] needs, already scaled by
     * [CanvasRenderConfiguration.unitScale]. An empty document yields `0 x 0`.
     */
    val documentCanvasSize: Dimension2D
        get() = Dimension2D(canvasData.canvasSize.width, canvasData.canvasSize.height)

    private fun pageAt(pageIndex: Int): MeasuredPage {
        require(pageIndex in measured.pages.indices) {
            "pageIndex $pageIndex is out of range 0..${measured.pages.size - 1}"
        }
        return measured.pages[pageIndex]
    }

    private fun scaledPageSize(page: MeasuredPage): Size =
        Size(page.effectiveSize.width * config.unitScale, page.effectiveSize.height * config.unitScale)

    private fun resolveCanvas(canvas: Canvas?, size: Size): Canvas {
        if (canvas == null) return Canvas(size.width, size.height)
        if (canvas.width < size.width) canvas.width = size.width
        if (canvas.height < size.height) canvas.height = size.height
        return canvas
    }

    private fun drawSeparator(
        gc: GraphicsContext,
        gapTop: Double,
        gapBottom: Double,
        width: Double,
    ) {
        val midY = (gapTop + gapBottom) / 2.0
        gc.save()
        gc.stroke = SEPARATOR_COLOR
        gc.lineWidth = SEPARATOR_LINE_WIDTH
        gc.setLineDashes(*SEPARATOR_DASHES)
        gc.strokeLine(0.0, midY, width, midY)
        gc.restore()
    }

    /**
     * Index-addressable view of the per-page canvas sizes. Obtained from
     * [CanvasDocumentRenderer.pageCanvasSizes], used as `pageCanvasSizes[pageIndex]`.
     */
    inner class PageCanvasSizes internal constructor() {

        /** The number of addressable pages; identical to [CanvasDocumentRenderer.pageCount]. */
        val count: Int get() = pageCount

        /**
         * The canvas size [renderPage] needs for the page at [pageIndex], already scaled by
         * [CanvasRenderConfiguration.unitScale].
         *
         * @throws IllegalArgumentException if [pageIndex] is outside the document's page range.
         */
        operator fun get(pageIndex: Int): Dimension2D {
            val size = scaledPageSize(pageAt(pageIndex))
            return Dimension2D(size.width, size.height)
        }
    }

    companion object {

        private val SEPARATOR_COLOR: Color = Color.gray(0.55)
        private const val SEPARATOR_LINE_WIDTH: Double = 1.0
        private val SEPARATOR_DASHES: DoubleArray = doubleArrayOf(6.0, 4.0)

        /**
         * Creates a renderer for [document], configured through [configurator] (a builder lambda
         * over [CanvasRenderConfiguration]). The document is measured and its canvas layout computed
         * immediately, so the JavaFX toolkit must already be initialised.
         *
         * @throws IllegalArgumentException if `unitScale <= 0` or `pageGap < 0`.
         */
        fun `for`(
            document: Document,
            configurator: CanvasRenderConfiguration.() -> Unit = {},
        ): CanvasDocumentRenderer = create(document, FxFontMeasureCalculator(), configurator)

        internal fun create(
            document: Document,
            measurer: FxFontMeasureCalculator,
            configurator: CanvasRenderConfiguration.() -> Unit = {},
        ): CanvasDocumentRenderer {
            val config = CanvasRenderConfiguration().apply(configurator)
            require(config.unitScale > 0.0) { "unitScale must be greater than 0, was ${config.unitScale}" }
            require(config.pageGap >= 0.0) { "pageGap must not be negative, was ${config.pageGap}" }
            return CanvasDocumentRenderer(document, config, measurer)
        }
    }
}
