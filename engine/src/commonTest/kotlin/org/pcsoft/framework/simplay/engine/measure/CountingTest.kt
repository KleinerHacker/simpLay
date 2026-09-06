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
