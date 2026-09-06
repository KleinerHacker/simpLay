package org.pcsoft.framework.simplay.engine.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies the counting extensions on the measured model mirror those on the raw model.
 */
class CountingTest {

    private fun block(text: String) = MeasuredTextBlock(
        raw = TextBlock.of(
            text,
            MeasureTestData.bodyStyle
        ),
        lines = emptyList(),
        bounds = MeasureTestData.someRect(),
        style = MeasureTestData.measuredBodyStyle(),
    )

    /**
     * Use case: word, symbol and character counts on a single measured block containing words and
     * punctuation.
     */
    @Test
    fun countsOnMeasuredBlock() {
        val b = block("Hello, world!")

        assertEquals(2, b.wordCount())
        assertEquals(2, b.symbolCount())
        assertEquals("Helloworld".length + 2, b.charCount())
    }

    /**
     * Use case: measured page counts aggregate the counts of all contained measured blocks.
     */
    @Test
    fun countsOnMeasuredPage() {
        val page = MeasuredFlowPage(
            raw = FlowPage(layout = MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(block("one two"), block("three.")),
        )

        assertEquals(3, page.wordCount())
        assertEquals(1, page.symbolCount())
        assertEquals("onetwothree".length + 1, page.charCount())
    }

    /**
     * Use case: measured document counts aggregate the counts of all contained measured pages.
     */
    @Test
    fun countsOnMeasuredDocument() {
        val document = MeasuredDocument(
            raw = Document(),
            pages = listOf(
                MeasuredFlowPage(
                    FlowPage(
                        layout = MeasureTestData.pageLayout
                    ), 0, listOf(block("a b"))
                ),
                MeasuredSinglePage(
                    SinglePage(
                        layout = MeasureTestData.pageLayout
                    ), 1, listOf(block("c d!"))
                ),
            ),
        )

        assertEquals(4, document.wordCount())
        assertEquals(1, document.symbolCount())
        assertEquals(5, document.charCount())
    }
}
