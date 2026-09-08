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

package org.pcsoft.framework.simplay.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a [SinglePage] never wraps onto further pages: it keeps every line and grows in
 * height instead.
 */
class SinglePageTest {

    private val document = Document(
        pages = listOf(
            SinglePage(
                layout = EngineTestData.pageLayout(width = 10.0, height = 30.0),
                blocks = listOf(TextBlock.of("aa bb cc dd ee", EngineTestData.style)),
            ),
        ),
    )

    private fun measure() =
        SimpLayEngine.builder(EngineTestData.measurer).build().measure(document)

    /**
     * Use case: five lines on a page that has room for three still stay on a single measured page,
     * with nothing clipped.
     */
    @Test
    fun contentBeyondThePageHeightStaysOnOnePage() {
        val measured = measure()

        assertEquals(1, measured.pages.size)
        val page = measured.pages[0]
        assertIs<MeasuredSinglePage>(page)
        assertEquals(5, page.blocks[0].lines.size)
        assertTrue(page.blocks[0].lines.last().lastLine)
        assertFalse(page.blocks[0].lines.first().lastLine)
    }

    /**
     * Use case: the measured page reports the content height it actually needs and grows its
     * effective height accordingly while keeping the page width.
     */
    @Test
    fun effectiveSizeGrowsToTheRequiredContentHeight() {
        val page = measure().pages[0]

        assertEquals(50.0, page.requiredContentHeight)
        assertEquals(10.0, page.effectiveSize.width)
        assertEquals(50.0, page.effectiveSize.height)
    }
}
