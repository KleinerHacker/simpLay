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
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Verifies the [MeasuredPage.effectiveSize] rules: a single page grows only in height, a flow page
 * never grows. The required content height is derived from the measured blocks.
 */
class SinglePageGrowthTest {

    /**
     * Use case: when the lowest measured block reaches past the layout height, a
     * [MeasuredSinglePage] grows its height to the vertical margins plus the required content
     * height, keeping the layout width.
     */
    @Test
    fun singlePageGrowsInHeightWhenContentOverflows() {
        val page = MeasuredSinglePage(
            raw = SinglePage(layout = MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                MeasureTestData.measuredBlockReaching(
                    400.0
                )
            ),
        )

        assertEquals(400.0, page.requiredContentHeight)
        assertEquals(200.0, page.effectiveSize.width)
        assertEquals(440.0, page.effectiveSize.height)
    }

    /**
     * Use case: when the measured content fits inside the layout, a [MeasuredSinglePage] keeps the
     * exact layout size.
     */
    @Test
    fun singlePageKeepsLayoutSizeWhenContentFits() {
        val page = MeasuredSinglePage(
            raw = SinglePage(layout = MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                MeasureTestData.measuredBlockReaching(
                    100.0
                )
            ),
        )

        assertEquals(200.0, page.effectiveSize.width)
        assertEquals(300.0, page.effectiveSize.height)
    }

    /**
     * Use case: an empty [MeasuredSinglePage] reports a required content height of zero and keeps
     * the layout size.
     */
    @Test
    fun emptySinglePageReportsZeroRequiredHeight() {
        val page = MeasuredSinglePage(
            raw = SinglePage(layout = MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = emptyList(),
        )

        assertEquals(0.0, page.requiredContentHeight)
        assertEquals(MeasureTestData.pageLayout.size, page.effectiveSize)
    }

    /**
     * Use case: a [MeasuredFlowPage] always reports the raw layout size, regardless of how much
     * content it holds.
     */
    @Test
    fun flowPageNeverGrows() {
        val page = MeasuredFlowPage(
            raw = FlowPage(layout = MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                MeasureTestData.measuredBlockReaching(
                    400.0
                )
            ),
        )

        assertEquals(MeasureTestData.pageLayout.size, page.effectiveSize)
    }
}
