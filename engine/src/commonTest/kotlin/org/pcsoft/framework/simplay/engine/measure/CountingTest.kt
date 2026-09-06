package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.measure.charCount
import org.pcsoft.framework.simplay.engine.measure.symbolCount
import org.pcsoft.framework.simplay.engine.measure.wordCount
import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies the counting extensions on the measured model mirror those on the raw model.
 */
class CountingTest {

    private fun block(text: String) = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock(
        raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
            text,
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyStyle
        ),
        lines = emptyList(),
        bounds = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.someRect(),
        style = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBodyStyle(),
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
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout),
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
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredDocument(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document(),
            pages = listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(
                    _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(
                        layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout
                    ), 0, listOf(block("a b"))
                ),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage(
                    _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(
                        layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout
                    ), 1, listOf(block("c d!"))
                ),
            ),
        )

        assertEquals(4, document.wordCount())
        assertEquals(1, document.symbolCount())
        assertEquals(5, document.charCount())
    }
}
