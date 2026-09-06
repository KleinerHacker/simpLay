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

package org.pcsoft.framework.simplay.engine.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * End-to-end checks for the degenerate inputs: an empty document, a page without blocks and a word
 * wider than the content area.
 */
class SpecialCasesTest {

    private val engine = E2ETestData.engine()

    /**
     * Use case: a document without pages is measured into a measured document that also has no
     * pages.
     */
    @Test
    fun emptyDocumentYieldsEmptyMeasuredDocument() {
        val measured = engine.measure(Document())

        assertTrue(measured.pages.isEmpty())
        assertEquals(Document(), measured.raw)
    }

    /**
     * Use case: a flow page that carries no blocks is measured into exactly one measured flow page
     * with an empty block list.
     */
    @Test
    fun pageWithoutBlocksYieldsEmptyMeasuredPage() {
        val measured = engine.measure(
            Document(pages = listOf(FlowPage(layout = E2ETestData.layout(width = 100.0, height = 100.0)))),
        )

        assertEquals(1, measured.pages.size)
        assertIs<MeasuredFlowPage>(measured.pages[0])
        assertTrue(measured.pages[0].blocks.isEmpty())
    }

    /**
     * Use case: a single word wider than the content area is not broken; it stays on its own line
     * and overflows the line box.
     */
    @Test
    fun overlongWordStaysUnbrokenOnItsOwnLine() {
        val contentWidth = 30.0
        val measured = engine.measure(
            Document(
                pages = listOf(
                    SinglePage(
                        layout = E2ETestData.layout(width = contentWidth, height = 200.0),
                        blocks = listOf(TextBlock.of("supercalifragilistic", E2ETestData.bodyStyle)),
                    ),
                ),
            ),
        )

        val block = measured.pages[0].blocks[0]
        assertEquals(1, block.lines.size)
        assertEquals(1, block.lines[0].parts.size)
        assertTrue(block.lines[0].parts[0].bounds.width > contentWidth)
    }
}
