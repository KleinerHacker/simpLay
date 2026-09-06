package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.charCount
import org.pcsoft.framework.simplay.engine.model.symbolCount
import org.pcsoft.framework.simplay.engine.model.wordCount

class CountingTest {

    private val style: org.pcsoft.framework.simplay.engine.model.TextStyle =
        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextStyle(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Font(
                "Serif",
                12.0
            )
        )
    private val layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.PageLayout(
        _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Size(
            100.0,
            200.0
        ), _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Margins(10.0, 10.0, 10.0, 10.0)
    )

    /**
     * Verifies word, symbol and character counts on a single block containing words and
     * punctuation.
     */
    @Test
    fun countsOnBlock() {
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("Hello, world!", style)
        assertEquals(2, block.wordCount())
        assertEquals(2, block.symbolCount())
        assertEquals("Helloworld".length + 2, block.charCount())
    }

    /**
     * Verifies that page counts aggregate the counts of all contained blocks.
     */
    @Test
    fun countsOnPage() {
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(
            layout,
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                    "one two",
                    style
                ), _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("three.", style)
            ),
        )
        assertEquals(3, page.wordCount())
        assertEquals(1, page.symbolCount())
        assertEquals("onetwothree".length + 1, page.charCount())
    }

    /**
     * Verifies that document counts aggregate the counts of all contained pages.
     */
    @Test
    fun countsOnDocument() {
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(
                    layout,
                    listOf(
                        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                            "a b",
                            style
                        )
                    )
                ),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(
                    layout,
                    listOf(
                        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                            "c d!",
                            style
                        )
                    )
                ),
            ),
        )
        assertEquals(4, document.wordCount())
        assertEquals(1, document.symbolCount())
        assertEquals(5, document.charCount())
    }
}
