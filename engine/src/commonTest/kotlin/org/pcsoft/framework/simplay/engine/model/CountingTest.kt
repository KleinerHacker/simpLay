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

package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

class CountingTest {

    private val style: TextStyle =
        TextStyle(
            Font(
                "Serif",
                12.0
            )
        )
    private val layout = PageLayout(
        Size(
            100.0,
            200.0
        ), Margins(10.0, 10.0, 10.0, 10.0)
    )

    /**
     * Verifies word, symbol and character counts on a single block containing words and
     * punctuation.
     */
    @Test
    fun countsOnBlock() {
        val block = TextBlock.of("Hello, world!", style)
        assertEquals(2, block.wordCount())
        assertEquals(2, block.symbolCount())
        assertEquals("Helloworld".length + 2, block.charCount())
    }

    /**
     * Verifies that page counts aggregate the counts of all contained blocks.
     */
    @Test
    fun countsOnPage() {
        val page = FlowPage(
            layout,
            listOf(
                TextBlock.of(
                    "one two",
                    style
                ), TextBlock.of("three.", style)
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
        val document = Document(
            listOf(
                FlowPage(
                    layout,
                    listOf(
                        TextBlock.of(
                            "a b",
                            style
                        )
                    )
                ),
                SinglePage(
                    layout,
                    listOf(
                        TextBlock.of(
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
