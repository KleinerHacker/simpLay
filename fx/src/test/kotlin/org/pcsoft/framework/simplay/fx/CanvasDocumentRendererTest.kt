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

package org.pcsoft.framework.simplay.fx

import javafx.scene.canvas.Canvas
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.engine.NoWrapLineBreakerStrategy
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.*
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.fx.internal.measureForCanvas
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Headless tests for the public [CanvasDocumentRenderer].
 */
class CanvasDocumentRendererTest : JavaFxTestBase() {

    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    private val lorem =
        "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. " +
            "How vexingly quick daft zebras jump. The five boxing wizards jump quickly."

    private fun page() = SinglePage(
        layout = PageLayout(
            size = Size(360.0, 240.0),
            margins = Margins(left = 24.0, top = 24.0, right = 24.0, bottom = 24.0),
        ),
        blocks = listOf(TextBlock.of(lorem, style)),
    )

    private fun multiPageDocument() = Document(pages = listOf(page(), page(), page()))

    private fun renderer(
        document: Document,
        configurator: CanvasRenderConfiguration.() -> Unit = {},
    ): CanvasDocumentRenderer = onFxThread { CanvasDocumentRenderer.`for`(document, configurator) }

    /**
     * The document canvas height must equal the sum of the measured page heights plus one page gap
     * per page boundary, and its width the widest page.
     */
    @Test
    fun documentCanvasSizeMatchesSummedPageHeights() {
        val document = multiPageDocument()
        val gap = 20.0

        val measured = onFxThread { document.measure(FxFontMeasureCalculator(), CanvasRenderConfiguration().apply { pageGap = gap }) }
        val expectedHeight = measured.pages.sumOf { it.effectiveSize.height } + gap * (measured.pages.size - 1)
        val expectedWidth = measured.pages.maxOf { it.effectiveSize.width }

        val size = renderer(document) { pageGap = gap }.documentCanvasSize
        assertEquals(expectedWidth, size.width)
        assertEquals(expectedHeight, size.height)
    }

    /**
     * Rendering without a passed canvas must create a new canvas whose size is exactly the computed
     * document canvas size.
     */
    @Test
    fun renderDocumentCreatesCanvasOfComputedSize() {
        val renderer = renderer(multiPageDocument())

        val (canvas, expected) = onFxThread { renderer.renderDocument() to renderer.documentCanvasSize }
        assertEquals(expected.width, canvas.width)
        assertEquals(expected.height, canvas.height)
    }

    /**
     * A passed canvas that is already large enough must be filled in place - the same instance is
     * returned and its size is left untouched.
     */
    @Test
    fun renderDocumentReusesPassedCanvasWhenLargeEnough() {
        val renderer = renderer(multiPageDocument())

        val result = onFxThread {
            val size = renderer.documentCanvasSize
            val canvas = Canvas(size.width + 50.0, size.height + 50.0)
            val returned = renderer.renderDocument(canvas)
            Triple(canvas, returned, size)
        }
        assertSame(result.first, result.second)
        assertEquals(result.third.width + 50.0, result.first.width)
        assertEquals(result.third.height + 50.0, result.first.height)
    }

    /**
     * The measured canvas data must place exactly one dashed page-break line per page boundary,
     * each in the middle of the gap band.
     */
    @Test
    fun renderDrawsDashedLineBetweenPages() {
        val document = multiPageDocument()
        val gap = 20.0
        val measured = onFxThread { document.measure(FxFontMeasureCalculator(), CanvasRenderConfiguration().apply { pageGap = gap }) }

        val canvasData = measureForCanvas(measured, unitScale = 1.0, pageGap = gap)

        assertEquals(measured.pages.size - 1, canvasData.separatorsY.size)
        val firstBoundary = measured.pages[0].effectiveSize.height
        assertEquals(firstBoundary + gap / 2.0, canvasData.separatorsY.first())
    }

    /**
     * The single-page render must produce a canvas of exactly the page's canvas size, which in turn
     * is the page's effective size scaled by the unit scale.
     */
    @Test
    fun singlePageRenderMatchesSliceOfDocumentRender() {
        val document = multiPageDocument()
        val renderer = renderer(document) { unitScale = 1.5 }

        val (canvas, pageSize) = onFxThread { renderer.renderPage(1) to renderer.pageCanvasSizes[1] }
        val measured = onFxThread { document.measure(FxFontMeasureCalculator()) }
        assertEquals(measured.pages[1].effectiveSize.width * 1.5, pageSize.width)
        assertEquals(measured.pages[1].effectiveSize.height * 1.5, pageSize.height)
        assertEquals(pageSize.width, canvas.width)
        assertEquals(pageSize.height, canvas.height)
    }

    /**
     * An out-of-range page index must be rejected with an [IllegalArgumentException], both for
     * rendering and for the size helper.
     */
    @Test
    fun renderPageRejectsOutOfRangeIndex() {
        val renderer = renderer(multiPageDocument())

        onFxThread { assertFailsWith<IllegalArgumentException> { renderer.renderPage(99) } }
        onFxThread { assertFailsWith<IllegalArgumentException> { renderer.pageCanvasSizes[-1] } }
    }

    /**
     * An invalid configuration must be rejected by the [CanvasDocumentRenderer.for] factory.
     */
    @Test
    fun factoryRejectsInvalidConfiguration() {
        onFxThread {
            assertFailsWith<IllegalArgumentException> {
                CanvasDocumentRenderer.`for`(multiPageDocument()) { unitScale = 0.0 }
            }
            assertFailsWith<IllegalArgumentException> {
                CanvasDocumentRenderer.`for`(multiPageDocument()) { pageGap = -1.0 }
            }
        }
    }

    /**
     * The `pageCanvasSize` accessor must be addressable by page index and report the same size as a
     * single-page render, and its `count` must equal `pageCount`.
     */
    @Test
    fun pageCanvasSizeIsIndexAddressable() {
        val document = multiPageDocument()
        val renderer = renderer(document) { unitScale = 2.0 }

        val measured = onFxThread { document.measure(FxFontMeasureCalculator()) }
        onFxThread {
            assertEquals(renderer.pageCount, renderer.pageCanvasSizes.count)
            for (index in 0 until renderer.pageCount) {
                val size = renderer.pageCanvasSizes[index]
                assertEquals(measured.pages[index].effectiveSize.width * 2.0, size.width)
                assertEquals(measured.pages[index].effectiveSize.height * 2.0, size.height)
            }
            assertFailsWith<IllegalArgumentException> { renderer.pageCanvasSizes[renderer.pageCount] }
        }
    }

    /**
     * Doubling the unit scale must double both the position span and the resulting canvas size.
     */
    @Test
    fun unitScaleScalesAllCoordinates() {
        val document = multiPageDocument()

        val plain = renderer(document) { unitScale = 1.0 }.documentCanvasSize
        val doubled = renderer(document) { unitScale = 2.0 }.documentCanvasSize

        assertEquals(plain.width * 2.0, doubled.width)
        assertEquals(plain.height * 2.0, doubled.height)
    }

    /**
     * An empty document must yield a `0 x 0` canvas and no page-break lines.
     */
    @Test
    fun emptyDocumentProducesMinimalCanvas() {
        val renderer = renderer(Document())

        val (canvas, size) = onFxThread { renderer.renderDocument() to renderer.documentCanvasSize }
        assertEquals(0.0, size.width)
        assertEquals(0.0, size.height)
        assertEquals(0.0, canvas.width)
        assertEquals(0.0, canvas.height)

        val measured = onFxThread { Document().measure(FxFontMeasureCalculator()) }
        assertTrue(measureForCanvas(measured, 1.0, 24.0).separatorsY.isEmpty())
    }

    /**
     * Selecting [NoWrapLineBreakerStrategy] through the configuration lambda must change the
     * measured line breaking, which shows up as a smaller document height than the greedy default.
     */
    @Test
    fun noWrapStrategyChangesLineBreaking() {
        val document = Document(
            pages = listOf(
                SinglePage(
                    layout = PageLayout(
                        size = Size(360.0, 60.0),
                        margins = Margins(left = 12.0, top = 12.0, right = 12.0, bottom = 12.0),
                    ),
                    blocks = listOf(TextBlock.of(lorem, style)),
                ),
            ),
        )

        val greedy = renderer(document).documentCanvasSize
        val noWrap = renderer(document) { lineBreakerStrategy = NoWrapLineBreakerStrategy }.documentCanvasSize

        assertTrue(noWrap.height < greedy.height, "no-wrap height ${noWrap.height} !< greedy ${greedy.height}")
    }

    /**
     * The document must be measured exactly once, in the [CanvasDocumentRenderer.for] factory:
     * every later render or size call reuses that result and triggers no further measuring.
     */
    @Test
    fun documentIsMeasuredOnceAtCreation() {
        val counter = CountingMeasurer()
        val renderer = onFxThread { CanvasDocumentRenderer.create(multiPageDocument(), counter) }

        val afterCreation = counter.calls
        assertTrue(afterCreation > 0)

        onFxThread {
            renderer.renderDocument()
            renderer.documentCanvasSize
            renderer.renderPage(0)
            renderer.pageCanvasSizes[0]
        }
        assertEquals(afterCreation, counter.calls)
    }

    /** A [FxFontMeasureCalculator] that counts how often [measure] is invoked. */
    private class CountingMeasurer : FxFontMeasureCalculator() {

        var calls: Int = 0

        override fun measure(font: Font, text: String) =
            super.measure(font, text).also { calls++ }
    }
}
