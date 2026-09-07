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

package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Tests for the [pageSize] / [documentSize] helpers on the measured model.
 */
class MeasuredSizingTest {

    private fun page(width: Double, height: Double) = FlowPage(
        layout = EngineTestData.pageLayout(width = width, height = height),
        blocks = listOf(TextBlock.of("a", EngineTestData.style)),
    )

    private fun measure(vararg pages: FlowPage) =
        Document(pages = pages.toList()).measure(EngineTestData.measurer)

    /**
     * Use case: [pageSize] returns exactly the page's
     * [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.effectiveSize].
     */
    @Test
    fun pageSizeMatchesEffectiveSize() {
        val onlyPage = measure(page(400.0, 300.0)).pages.single()

        assertEquals(onlyPage.effectiveSize, onlyPage.pageSize())
    }

    /**
     * Use case: for a multi-page document the width is the widest page and the height is the sum of
     * all page heights plus one gap per page boundary, never before the first or after the last.
     */
    @Test
    fun documentSizeIsMaxWidthAndSummedHeightPlusGaps() {
        val gap = 24.0
        val measured = measure(page(400.0, 300.0), page(500.0, 260.0), page(360.0, 280.0))

        assertEquals(Size(500.0, 300.0 + 260.0 + 280.0 + gap * 2), measured.documentSize(gap))
    }

    /**
     * Use case: a single-page document adds no gap at all.
     */
    @Test
    fun documentSizeOfSinglePageHasNoGap() {
        val measured = measure(page(400.0, 300.0))

        assertEquals(Size(400.0, 300.0), measured.documentSize(24.0))
    }

    /**
     * Use case: an empty document (no pages) has size `0 x 0` regardless of the gap.
     */
    @Test
    fun documentSizeOfEmptyDocumentIsZero() {
        val measured = Document().measure(EngineTestData.measurer)

        assertEquals(Size(0.0, 0.0), measured.documentSize(42.0))
    }
}
