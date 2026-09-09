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
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Tests for [DocumentTextIndex], the linear text axis every interactive GUI binding builds selection,
 * caret and hit-testing on. All measuring goes through [StubFontMeasureCalculator] so the assertions
 * stay platform-independent.
 */
class DocumentTextIndexTest {

    private val style = TextStyle(font = Font(family = "Serif", size = 12.0))

    private val layout = PageLayout(
        size = Size(width = 600.0, height = 800.0),
        margins = Margins(left = 40.0, top = 40.0, right = 40.0, bottom = 40.0),
    )

    private fun measure(vararg paragraphs: String): MeasuredDocument =
        Document(pages = listOf(FlowPage(layout = layout, blocks = paragraphs.map { TextBlock.of(it, style) })))
            .measure(StubFontMeasureCalculator())

    /**
     * Verifies that the linear text of a two-paragraph document joins the paragraphs with a single
     * newline separator and that [DocumentTextIndex.length] equals that joined string's length.
     */
    @Test
    fun linearTextJoinsParagraphsWithNewline() {
        val index = DocumentTextIndex(measure("Hello world", "Second line"))
        assertEquals("Hello world\nSecond line", index.text)
        assertEquals(index.text.length, index.length)
    }

    /**
     * Verifies that [DocumentTextIndex.blockCount] reports one entry per raw block of the document.
     */
    @Test
    fun blockCountMatchesParagraphCount() {
        val index = DocumentTextIndex(measure("One", "Two", "Three"))
        assertEquals(3, index.blockCount)
    }

    /**
     * Verifies that [DocumentTextIndex.substring] returns exactly the requested slice of the linear
     * text and that out-of-range indices are clamped rather than throwing.
     */
    @Test
    fun substringReturnsClampedSlice() {
        val index = DocumentTextIndex(measure("Hello world"))
        assertEquals("Hello", index.substring(0, 5))
        assertEquals("Hello world", index.substring(-10, 999))
    }

    /**
     * Verifies that [DocumentTextIndex.styledRuns] over the first word yields a single run whose text
     * is that word and whose font is the block font.
     */
    @Test
    fun styledRunsCarryTheBlockFont() {
        val index = DocumentTextIndex(measure("Hello world"))
        val runs = index.styledRuns(0, 5)
        assertEquals(1, runs.size)
        assertEquals("Hello", runs.first().text)
        assertEquals("Serif", runs.first().font?.family)
    }

    /**
     * Verifies that [DocumentTextIndex.wordRangeAt] expands an index inside a word to that word's
     * full half-open character range.
     */
    @Test
    fun wordRangeAtExpandsToWholeWord() {
        val index = DocumentTextIndex(measure("Hello world"))
        val range = index.wordRangeAt(2)
        assertEquals(0, range.first)
        assertEquals(5, range.last)
    }

    /**
     * Verifies that [DocumentTextIndex.startOfBlock] and [DocumentTextIndex.endOfBlock] bracket the
     * second paragraph, and that [DocumentTextIndex.nextWordStart] steps forward to the next word.
     */
    @Test
    fun structuralNavigationHelpersAddressBlocksAndWords() {
        val index = DocumentTextIndex(measure("alpha beta", "gamma delta"))
        val secondStart = index.startOfBlock(1)
        assertEquals("gamma delta", index.substring(secondStart, index.endOfBlock(1)))
        assertTrue(index.nextWordStart(0) > 0)
    }
}
