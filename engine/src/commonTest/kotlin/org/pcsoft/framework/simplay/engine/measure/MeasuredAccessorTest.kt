package org.pcsoft.framework.simplay.engine.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a replaced property is exposed once, as its measured type, and the raw value stays
 * reachable only through the wrapped `raw` object.
 */
class MeasuredAccessorTest {

    /**
     * Use case: `MeasuredTextStyle.font` is the measured font; the raw font is reachable via
     * `raw.font`.
     */
    @Test
    fun styleExposesMeasuredFontOnly() {
        val style = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBodyStyle()

        assertEquals(_root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyMetrics, style.font.metrics)
        assertSame(_root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyFont, style.font.raw)
        assertSame(_root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyFont, style.raw.font)
    }

    /**
     * Use case: `MeasuredTextBlock.style` is the measured style; the raw style is reachable via
     * `raw.style`.
     */
    @Test
    fun blockExposesMeasuredStyleOnly() {
        val raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("Measured accessors", _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyStyle)
        val measuredStyle = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBodyStyle()
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock(
            raw,
            emptyList(),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.someRect(),
            measuredStyle
        )

        assertSame(measuredStyle, block.style)
        assertSame(raw, block.raw)
        assertSame(raw.style, block.raw.style)
    }

    /**
     * Use case: `MeasuredPage.blocks` is the measured block list and `contentArea` is the layout
     * box shrunk by the margins.
     */
    @Test
    fun pageExposesMeasuredBlocksAndContentArea() {
        val raw =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout)
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                "Page content",
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyStyle
            ),
            emptyList(),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.someRect(),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBodyStyle(),
        )
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(
            raw,
            pageIndex = 3,
            blocks = listOf(block)
        )

        assertEquals(listOf(block), page.blocks)
        assertEquals(3, page.pageIndex)
        assertEquals(_root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(20.0, 20.0, 160.0, 260.0), page.contentArea)
        assertSame(raw, page.raw)
    }

    /**
     * Use case: `MeasuredDocument.pages` is the measured page list; the raw pages are reachable via
     * `raw.pages`.
     */
    @Test
    fun documentExposesMeasuredPagesOnly() {
        val rawFlow =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout)
        val rawSingle =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout)
        val raw =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document(pages = listOf(rawFlow, rawSingle))
        val measuredPages = listOf(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(rawFlow, 0, emptyList()),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage(
                rawSingle,
                1,
                emptyList()
            ),
        )
        val document =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredDocument(raw, measuredPages)

        assertEquals(measuredPages, document.pages)
        assertSame(raw, document.raw)
        assertSame(raw.pages, document.raw.pages)
        assertTrue(document.pages[1] is org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage)
    }
}
