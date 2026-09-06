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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * End-to-end checks for flow-page pagination continuity and single-page vertical growth.
 */
class PaginationAndGrowthTest {

    private val engine = E2ETestData.engine()

    /**
     * Use case: five one-word lines with room for two lines per page are spread over three flow
     * pages that carry a continuous, document-wide page index.
     */
    @Test
    fun flowContentIsSplitAcrossPagesWithContinuousIndex() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = E2ETestData.layout(width = 10.0, height = 25.0),
                    blocks = listOf(TextBlock.of("aa bb cc dd ee", E2ETestData.bodyStyle)),
                ),
            ),
        )

        val measured = engine.measure(document)

        assertEquals(3, measured.pages.size)
        assertTrue(measured.pages.all { it is MeasuredFlowPage })
        assertEquals(listOf(0, 1, 2), measured.pages.map { it.pageIndex })
        assertEquals(listOf(2, 2, 1), measured.pages.map { page -> page.blocks.sumOf { it.lines.size } })
    }

    /**
     * Use case: across the whole split block only the last line on the last page is the block's
     * last line.
     */
    @Test
    fun onlyTheLastLineOfTheSplitBlockIsFlaggedLast() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = E2ETestData.layout(width = 10.0, height = 25.0),
                    blocks = listOf(TextBlock.of("aa bb cc dd ee", E2ETestData.bodyStyle)),
                ),
            ),
        )

        val measured = engine.measure(document)
        val lines = measured.pages.flatMap { page -> page.blocks.flatMap { it.lines } }

        assertEquals(5, lines.size)
        assertTrue(lines.dropLast(1).none { it.lastLine })
        assertTrue(lines.last().lastLine)
    }

    /**
     * Use case: a single page whose block needs four lines but whose layout height fits only two
     * grows to the required content height and keeps its layout width; nothing is clipped.
     */
    @Test
    fun singlePageGrowsToFitAllLines() {
        val document = Document(
            pages = listOf(
                SinglePage(
                    layout = E2ETestData.layout(width = 10.0, height = 20.0),
                    blocks = listOf(TextBlock.of("aa bb cc dd", E2ETestData.bodyStyle)),
                ),
            ),
        )

        val measured = engine.measure(document)
        val page = measured.pages[0]
        assertTrue(page is MeasuredSinglePage)

        assertEquals(4, page.blocks[0].lines.size)
        assertEquals(10.0, page.effectiveSize.width)
        assertEquals(40.0, page.effectiveSize.height)
        assertEquals(40.0, page.requiredContentHeight)
    }

    /**
     * Use case: a single page whose content already fits inside its layout height keeps that height
     * unchanged.
     */
    @Test
    fun singlePageKeepsLayoutHeightWhenContentFits() {
        val document = Document(
            pages = listOf(
                SinglePage(
                    layout = E2ETestData.layout(width = 10.0, height = 100.0),
                    blocks = listOf(TextBlock.of("aa bb", E2ETestData.bodyStyle)),
                ),
            ),
        )

        val measured = engine.measure(document)
        val page = measured.pages[0]

        assertEquals(100.0, page.effectiveSize.height)
        assertFalse(page.effectiveSize.height < page.requiredContentHeight)
    }
}
