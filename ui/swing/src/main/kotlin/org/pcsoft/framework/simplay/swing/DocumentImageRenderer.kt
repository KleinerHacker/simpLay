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

package org.pcsoft.framework.simplay.swing

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import org.pcsoft.framework.simplay.engine.planPageNumbers
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.swing.internal.Graphics2DDocumentRenderer
import org.pcsoft.framework.simplay.swing.internal.MeasuredImageData
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.swing.internal.measureForImage

/**
 * Public renderer that paints one fixed [Document] - whole, or a single page - onto an AWT
 * [BufferedImage] (or a caller-supplied [Graphics2D]). The Swing counterpart of the `fx` module's
 * `CanvasDocumentRenderer`.
 *
 * The document is bound at creation through the [of] factory, so measuring and the whole layout
 * (sizes, page origins, page-break positions) are computed once, up front:
 *
 * ```kotlin
 * val renderer = DocumentImageRenderer.of(document) {
 *     unitScale = 1.5
 *     pageGap = 16.0
 *     lineBreakerStrategy = NoWrapLineBreakerStrategy
 * }
 * val image = renderer.renderDocument()
 * val firstPageSize = renderer.pageImageSizes[0]
 * ```
 *
 * Pages are stacked vertically, one [ImageRenderConfiguration.pageGap] apart, and every page
 * boundary is marked with a dashed line drawn in the middle of the gap. Every layout coordinate and
 * the resulting image size are multiplied by [ImageRenderConfiguration.unitScale].
 *
 * The instance is immutable and single-document; it is not thread-safe. The produced images are
 * `TYPE_INT_ARGB` with a transparent background.
 */
class DocumentImageRenderer private constructor(
    document: Document,
    private val config: ImageRenderConfiguration,
    private val measurer: SwingFontMeasureCalculator,
) {

    private val measured: MeasuredDocument = document.measure(measurer, config)

    private val imageData: MeasuredImageData = measureForImage(measured, config.unitScale, config.pageGap)

    /** The number of pages the bound document measures to, after flow continuation and growth. */
    val pageCount: Int = measured.pages.size

    /** The per-page image sizes, addressed by page index: `renderer.pageImageSizes[pageIndex]`. */
    val pageImageSizes: PageImageSizes = PageImageSizes()

    /**
     * Draws every page of the bound document onto a new [BufferedImage] of exactly the required size
     * ([documentImageSize]).
     */
    fun renderDocument(): BufferedImage {
        val image = newImage(imageData.imageSize)
        val g = image.createGraphics()
        try {
            renderDocument(g)
        } finally {
            g.dispose()
        }
        return image
    }

    /**
     * Draws every page of the bound document onto [g]. [g] is scaled by
     * [ImageRenderConfiguration.unitScale] internally; the caller only positions the origin.
     */
    fun renderDocument(g: Graphics2D) {
        val saved = g.transform
        g.scale(config.unitScale, config.unitScale)
        Graphics2DDocumentRenderer.renderDocument(
            g = g,
            document = measured,
            gap = config.pageGap,
            fonts = measurer,
            pageSeparator = { gg, gapTop, gapBottom, width -> drawSeparator(gg, gapTop, gapBottom, width) },
            numbering = measured.raw.numbering,
        )
        g.transform = saved
    }

    /**
     * Draws exactly the page at [pageIndex] onto a new [BufferedImage], with its top at the image
     * origin.
     *
     * @throws IllegalArgumentException if [pageIndex] is outside the document's page range.
     */
    fun renderPage(pageIndex: Int): BufferedImage {
        val page = pageAt(pageIndex)
        val image = newImage(scaledPageSize(page))
        val g = image.createGraphics()
        try {
            renderPage(pageIndex, g)
        } finally {
            g.dispose()
        }
        return image
    }

    /**
     * Draws exactly the page at [pageIndex] onto [g], with its top at the current origin. [g] is
     * scaled by [ImageRenderConfiguration.unitScale] internally.
     *
     * @throws IllegalArgumentException if [pageIndex] is outside the document's page range.
     */
    fun renderPage(pageIndex: Int, g: Graphics2D) {
        val page = pageAt(pageIndex)
        val saved = g.transform
        g.scale(config.unitScale, config.unitScale)
        val label = measured.planPageNumbers(measured.raw.numbering).getOrNull(pageIndex)
        Graphics2DDocumentRenderer.renderPage(
            g = g,
            page = page,
            fonts = measurer,
            pageNumberLabel = label,
            numberingStyle = measured.raw.numbering.textStyle,
        )
        g.transform = saved
    }

    /**
     * The image size [renderDocument] needs, already scaled by [ImageRenderConfiguration.unitScale]
     * and rounded up. An empty document yields `0 x 0`.
     */
    val documentImageSize: Dimension
        get() = toDimension(imageData.imageSize)

    private fun pageAt(pageIndex: Int): MeasuredPage {
        require(pageIndex in measured.pages.indices) {
            "pageIndex $pageIndex is out of range 0..${measured.pages.size - 1}"
        }
        return measured.pages[pageIndex]
    }

    private fun scaledPageSize(page: MeasuredPage): Size =
        Size(page.effectiveSize.width * config.unitScale, page.effectiveSize.height * config.unitScale)

    private fun newImage(size: Size): BufferedImage {
        val w = ceil(size.width).toInt().coerceAtLeast(1)
        val h = ceil(size.height).toInt().coerceAtLeast(1)
        return BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }

    private fun drawSeparator(g: Graphics2D, gapTop: Double, gapBottom: Double, width: Double) {
        val midY = ((gapTop + gapBottom) / 2.0).toFloat()
        val savedStroke = g.stroke
        val savedColor = g.color
        g.color = SEPARATOR_COLOR
        g.stroke = BasicStroke(SEPARATOR_LINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, SEPARATOR_DASHES, 0f)
        g.drawLine(0, midY.toInt(), width.toInt(), midY.toInt())
        g.stroke = savedStroke
        g.color = savedColor
    }

    /**
     * Index-addressable view of the per-page image sizes. Obtained from
     * [DocumentImageRenderer.pageImageSizes], used as `pageImageSizes[pageIndex]`.
     */
    inner class PageImageSizes internal constructor() {

        /** The number of addressable pages; identical to [DocumentImageRenderer.pageCount]. */
        val count: Int get() = pageCount

        /**
         * The image size [renderPage] needs for the page at [pageIndex], already scaled by
         * [ImageRenderConfiguration.unitScale] and rounded up.
         *
         * @throws IllegalArgumentException if [pageIndex] is outside the document's page range.
         */
        operator fun get(pageIndex: Int): Dimension = toDimension(scaledPageSize(pageAt(pageIndex)))
    }

    companion object {

        private val SEPARATOR_COLOR: Color = Color(140, 140, 140)
        private const val SEPARATOR_LINE_WIDTH: Float = 1.0f
        private val SEPARATOR_DASHES: FloatArray = floatArrayOf(6.0f, 4.0f)

        private fun toDimension(size: Size): Dimension =
            Dimension(ceil(size.width).toInt().coerceAtLeast(0), ceil(size.height).toInt().coerceAtLeast(0))

        /**
         * Creates a renderer for [document], configured through [configurator] (a builder lambda
         * over [ImageRenderConfiguration]). The document is measured and its layout computed
         * immediately.
         *
         * @throws IllegalArgumentException if `unitScale <= 0` or `pageGap < 0`.
         */
        fun of(
            document: Document,
            configurator: ImageRenderConfiguration.() -> Unit = {},
        ): DocumentImageRenderer = create(document, SwingFontMeasureCalculator(), configurator)

        internal fun create(
            document: Document,
            measurer: SwingFontMeasureCalculator,
            configurator: ImageRenderConfiguration.() -> Unit = {},
        ): DocumentImageRenderer {
            val config = ImageRenderConfiguration().apply(configurator)
            require(config.unitScale > 0.0) { "unitScale must be greater than 0, was ${config.unitScale}" }
            require(config.pageGap >= 0.0) { "pageGap must not be negative, was ${config.pageGap}" }
            return DocumentImageRenderer(document, config, measurer)
        }
    }
}
