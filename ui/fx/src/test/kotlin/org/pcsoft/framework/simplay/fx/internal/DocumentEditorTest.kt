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

import org.pcsoft.framework.simplay.engine.RenderConfiguration
import org.pcsoft.framework.simplay.uicommon.DocumentEditor
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Headless tests for [DocumentEditor]: insertion, deletion and replacement on the linear text axis
 * of a [DocumentTextIndex], the merge of two blocks a delete joined across their boundary, the
 * sanitisation of pasted line breaks, the preservation of the page list and type, and editing an
 * empty document.
 */
class DocumentEditorTest : JavaFxTestBase() {

    private val layout = PageLayout(
        size = Size(width = 360.0, height = 260.0),
        margins = Margins(left = 30.0, top = 30.0, right = 30.0, bottom = 30.0),
    )
    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    private fun flow(vararg paragraphs: String): Document =
        Document(pages = listOf(FlowPage(layout = layout, blocks = paragraphs.map { TextBlock.of(it, style) })))

    private fun indexOf(document: Document): DocumentTextIndex =
        onFxThread { DocumentTextIndex(document.measure(FxFontMeasureCalculator(), RenderConfiguration())) }

    private fun Document.plain(): String =
        pages.flatMap { it.blocks }.joinToString("\n") { it.toString() }

    /**
     * Inserting text at a linear index puts it into the document and reports the caret one past the
     * inserted run.
     */
    @Test
    fun insertPutsTextAtIndexAndReportsCaret() {
        val document = flow("The quick brown fox.")
        val index = indexOf(document)

        val result = DocumentEditor.insert(index, document, at = 0, text = "Big ")

        assertTrue(result.document.plain().startsWith("Big The quick brown fox."))
        assertEquals(4, result.caretIndex)
    }

    /**
     * Deleting a linear range removes exactly those characters and reports the caret at the range
     * start.
     */
    @Test
    fun deleteRemovesRangeAndReportsCaretAtStart() {
        val document = flow("The quick brown fox.")
        val index = indexOf(document)

        val result = DocumentEditor.delete(index, document, from = 0, to = 4)

        assertEquals("quick brown fox.", result.document.plain())
        assertEquals(0, result.caretIndex)
    }

    /**
     * A delete that spans the line break between two blocks joins them into a single block on the
     * first block's page.
     */
    @Test
    fun deleteAcrossBlockBoundaryMergesBlocks() {
        val document = flow("First paragraph here", "Second paragraph here")
        val index = indexOf(document)
        val firstLength = "First paragraph here".length

        val result = DocumentEditor.delete(index, document, from = firstLength - 6, to = firstLength + 7)

        assertEquals(1, result.document.pages[0].blocks.size)
        assertFalse(result.document.plain().contains("\n"))
        assertTrue(result.document.plain().startsWith("First"))
        assertTrue(result.document.plain().endsWith("here"))
    }

    /**
     * Replacing a linear range swaps its characters for the new text and reports the caret one past
     * the new run.
     */
    @Test
    fun replaceSwapsRange() {
        val document = flow("The quick brown fox.")
        val index = indexOf(document)

        val result = DocumentEditor.replace(index, document, from = 0, to = 3, text = "XYZ")

        assertTrue(result.document.plain().startsWith("XYZ quick brown fox."))
        assertEquals(3, result.caretIndex)
    }

    /**
     * A line break inside inserted text is turned into a space, so an insertion never creates a new
     * block.
     */
    @Test
    fun insertSanitizesLineBreaks() {
        val document = flow("The quick brown fox.")
        val index = indexOf(document)

        val result = DocumentEditor.insert(index, document, at = 0, text = "one\ntwo ")

        assertEquals(1, result.document.pages[0].blocks.size)
        assertFalse(result.document.plain().contains("\n"))
        assertTrue(result.document.plain().startsWith("one two The"))
    }

    /**
     * An edit inside the first page of a document with different page types keeps both pages and
     * their types.
     */
    @Test
    fun pageListAndTypesArePreserved() {
        val document = Document(
            pages = listOf(
                SinglePage(layout = layout, blocks = listOf(TextBlock.of("Single page content", style))),
                FlowPage(layout = layout, blocks = listOf(TextBlock.of("Flow page content", style))),
            ),
        )
        val index = indexOf(document)

        val result = DocumentEditor.insert(index, document, at = 0, text = "Edited ")

        assertEquals(2, result.document.pages.size)
        assertTrue(result.document.pages[0] is SinglePage)
        assertTrue(result.document.pages[1] is FlowPage)
        assertTrue(result.document.pages[0].blocks.first().toString().startsWith("Edited Single"))
    }

    /**
     * Inserting into a document that has no pages creates a first flow page carrying the text.
     */
    @Test
    fun insertIntoEmptyDocumentCreatesFirstBlock() {
        val document = Document(pages = emptyList())
        val index = indexOf(document)

        val result = DocumentEditor.insert(index, document, at = 0, text = "hello world")

        assertEquals(1, result.document.pages.size)
        assertEquals("hello world", result.document.plain())
    }
}
