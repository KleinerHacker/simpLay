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

package org.pcsoft.framework.simplay.uicommon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Tests for [DocumentEditor]: insertion, deletion and replacement on the linear text axis of a
 * [DocumentTextIndex], the merge of two blocks a delete joined across their boundary and the
 * sanitisation of pasted line breaks. Measuring uses [StubFontMeasureCalculator].
 */
class DocumentEditorTest {

    private val style = TextStyle(font = Font(family = "Serif", size = 12.0))

    private val layout = PageLayout(
        size = Size(width = 600.0, height = 800.0),
        margins = Margins(left = 40.0, top = 40.0, right = 40.0, bottom = 40.0),
    )

    private fun document(vararg paragraphs: String): Document =
        Document(pages = listOf(FlowPage(layout = layout, blocks = paragraphs.map { TextBlock.of(it, style) })))

    private fun index(document: Document): DocumentTextIndex =
        DocumentTextIndex(document.measure(StubFontMeasureCalculator()))

    /**
     * Verifies that [DocumentEditor.insert] adds the text at the given linear index and reports the
     * caret index behind the inserted run.
     */
    @Test
    fun insertAddsTextAndAdvancesCaret() {
        val document = document("brown fox")
        val result = DocumentEditor.insert(index(document), document, at = 0, text = "Big ")
        assertEquals("Big brown fox", index(result.document).text)
        assertEquals(4, result.caretIndex)
    }

    /**
     * Verifies that [DocumentEditor.delete] removes the requested half-open range and places the
     * caret at the deletion start.
     */
    @Test
    fun deleteRemovesRange() {
        val document = document("Big brown fox")
        val result = DocumentEditor.delete(index(document), document, from = 0, to = 4)
        assertEquals("brown fox", index(result.document).text)
        assertEquals(0, result.caretIndex)
    }

    /**
     * Verifies that [DocumentEditor.replace] swaps a range for new text in a single step.
     */
    @Test
    fun replaceSwapsRange() {
        val document = document("abc def")
        val result = DocumentEditor.replace(index(document), document, from = 0, to = 3, text = "XYZ")
        assertEquals("XYZ def", index(result.document).text)
    }

    /**
     * Verifies that a delete spanning the boundary of two paragraphs merges them into one block, so
     * the block count drops by one and the joined text has no newline left.
     */
    @Test
    fun deleteAcrossParagraphBoundaryMergesBlocks() {
        val document = document("first line", "second line")
        val idx = index(document)
        val firstLength = "first line".length
        val result = DocumentEditor.delete(idx, document, from = firstLength - 2, to = firstLength + 3)
        val merged = index(result.document)
        assertEquals(1, merged.blockCount)
        assertTrue(!merged.text.contains('\n'))
    }

    /**
     * Verifies that a pasted string containing line breaks is sanitised to spaces, so an insert never
     * raises the block count.
     */
    @Test
    fun insertSanitisesPastedLineBreaks() {
        val document = document("tail")
        val result = DocumentEditor.insert(index(document), document, at = 0, text = "one\ntwo ")
        val idx = index(result.document)
        assertEquals(1, idx.blockCount)
        assertEquals("one two tail", idx.text)
    }
}
