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

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Tests for the [pageSize] / [documentSize] helpers.
 */
class RenderSizingTest : JavaFxTestBase() {

    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    private fun page(width: Double, height: Double) = FlowPage(
        layout = PageLayout(
            size = Size(width, height),
            margins = Margins(left = 20.0, top = 20.0, right = 20.0, bottom = 20.0),
        ),
        blocks = listOf(TextBlock.of("The quick brown fox jumps over the lazy dog.", style)),
    )

    /**
     * [pageSize] must return exactly the page's [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.effectiveSize].
     */
    @Test
    fun pageSizeMatchesEffectiveSize() {
        val measured = onFxThread { measureDocument(Document(pages = listOf(page(400.0, 300.0)))) }

        val onlyPage = measured.pages.single()
        assertEquals(onlyPage.effectiveSize, pageSize(onlyPage))
    }

    /**
     * For a multi-page document the width is the widest page and the height is the sum of all page
     * heights plus one gap per page boundary (not before the first or after the last).
     */
    @Test
    fun documentSizeIsMaxWidthAndSummedHeightPlusGaps() {
        val gap = 24.0
        val measured = onFxThread {
            measureDocument(Document(pages = listOf(page(400.0, 300.0), page(500.0, 260.0), page(360.0, 280.0))))
        }

        val expectedWidth = measured.pages.maxOf { it.effectiveSize.width }
        val expectedHeight = measured.pages.sumOf { it.effectiveSize.height } + gap * (measured.pages.size - 1)

        assertEquals(Size(expectedWidth, expectedHeight), documentSize(measured, gap))
    }

    /**
     * An empty document (no pages) must have size `0 x 0` regardless of the gap.
     */
    @Test
    fun documentSizeOfEmptyDocumentIsZero() {
        val measured = onFxThread { measureDocument(Document()) }

        assertEquals(Size(0.0, 0.0), documentSize(measured, 42.0))
    }
}
