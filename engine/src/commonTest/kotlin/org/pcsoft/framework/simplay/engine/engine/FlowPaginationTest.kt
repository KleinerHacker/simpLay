package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a [FlowPage] whose content exceeds the content height is split across several
 * [MeasuredFlowPage]s with a continuous page index and a deep-copied page frame.
 */
class FlowPaginationTest {

    private val document = Document(
        pages = listOf(
            FlowPage(
                layout = EngineTestData.pageLayout(width = 10.0, height = 25.0),
                blocks = listOf(TextBlock.of("aa bb cc dd ee", EngineTestData.style)),
            ),
        ),
    )

    private fun measure() =
        SimpLayEngine.builder(EngineTestData.measurer).build().measure(document)

    /**
     * Use case: five one-word lines with room for two lines per page produce three flow pages with
     * page indices 0, 1 and 2.
     */
    @Test
    fun overflowingContentProducesSeveralFlowPages() {
        val measured = measure()

        assertEquals(3, measured.pages.size)
        assertTrue(measured.pages.all { it is MeasuredFlowPage })
        assertEquals(listOf(0, 1, 2), measured.pages.map { it.pageIndex })
    }

    /**
     * Use case: the first flow page keeps the original raw page while every follow-up page gets a
     * fresh raw page with an equal but not identical frame.
     */
    @Test
    fun followUpPagesCloneThePageFrame() {
        val measured = measure()

        assertSame(document.pages[0], measured.pages[0].raw)
        assertNotSame(document.pages[0], measured.pages[1].raw)
        assertEquals(measured.pages[0].layout, measured.pages[1].layout)
        assertNotSame(measured.pages[0].layout, measured.pages[1].layout)
    }

    /**
     * Use case: no line is lost across the page break; the pages hold two, two and one line and only
     * the very last line is flagged as the block's last line.
     */
    @Test
    fun linesAreDistributedAcrossPagesWithoutLoss() {
        val measured = measure()

        val linesPerPage = measured.pages.map { page -> page.blocks.sumOf { it.lines.size } }
        assertEquals(listOf(2, 2, 1), linesPerPage)

        assertFalse(measured.pages[0].blocks[0].lines[0].lastLine)
        assertTrue(measured.pages[2].blocks[0].lines[0].lastLine)
    }
}
