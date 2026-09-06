package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Verifies that empty inputs produce well-formed but empty measured results.
 */
class EmptyDocumentTest {

    private val engine = SimpLayEngine.builder(EngineTestData.measurer).build()

    /**
     * Use case: a document without pages produces a measured document with an empty page list.
     */
    @Test
    fun documentWithoutPagesProducesNoMeasuredPages() {
        val measured = engine.measure(Document())

        assertEquals(0, measured.pages.size)
    }

    /**
     * Use case: a flow page without blocks produces exactly one empty measured flow page.
     */
    @Test
    fun flowPageWithoutBlocksProducesOneEmptyPage() {
        val measured = engine.measure(
            Document(pages = listOf(FlowPage(layout = EngineTestData.pageLayout()))),
        )

        assertEquals(1, measured.pages.size)
        val page = measured.pages[0]
        assertIs<MeasuredFlowPage>(page)
        assertTrue(page.blocks.isEmpty())
    }

    /**
     * Use case: a single page without blocks produces one empty measured single page that needs no
     * content height and keeps the page size.
     */
    @Test
    fun singlePageWithoutBlocksProducesOneEmptyPage() {
        val layout = EngineTestData.pageLayout()
        val measured = engine.measure(Document(pages = listOf(SinglePage(layout = layout))))

        assertEquals(1, measured.pages.size)
        val page = measured.pages[0]
        assertIs<MeasuredSinglePage>(page)
        assertTrue(page.blocks.isEmpty())
        assertEquals(0.0, page.requiredContentHeight)
        assertEquals(layout.size, page.effectiveSize)
    }
}
