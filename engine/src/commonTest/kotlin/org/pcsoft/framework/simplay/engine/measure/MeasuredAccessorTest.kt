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
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a replaced property is exposed once, as its measured type, and the raw value stays
 * reachable only through the wrapped `raw` object.
 */
class MeasuredAccessorTest {

    /**
     * Use case: `MeasuredTextStyle.font` is the measured font; the raw font is reachable via
     * `raw.font`.
     */
    @Test
    fun styleExposesMeasuredFontOnly() {
        val style = MeasureTestData.measuredBodyStyle()

        assertEquals(MeasureTestData.bodyMetrics, style.font.metrics)
        assertSame(MeasureTestData.bodyFont, style.font.raw)
        assertSame(MeasureTestData.bodyFont, style.raw.font)
    }

    /**
     * Use case: `MeasuredTextBlock.style` is the measured style; the raw style is reachable via
     * `raw.style`.
     */
    @Test
    fun blockExposesMeasuredStyleOnly() {
        val raw = TextBlock.of("Measured accessors", MeasureTestData.bodyStyle)
        val measuredStyle = MeasureTestData.measuredBodyStyle()
        val block = MeasuredTextBlock(
            raw,
            emptyList(),
            MeasureTestData.someRect(),
            measuredStyle
        )

        assertSame(measuredStyle, block.style)
        assertSame(raw, block.raw)
        assertSame(raw.style, block.raw.style)
    }

    /**
     * Use case: `MeasuredPage.blocks` is the measured block list and `contentArea` is the layout
     * box shrunk by the margins.
     */
    @Test
    fun pageExposesMeasuredBlocksAndContentArea() {
        val raw =
            FlowPage(layout = MeasureTestData.pageLayout)
        val block = MeasuredTextBlock(
            TextBlock.of(
                "Page content",
                MeasureTestData.bodyStyle
            ),
            emptyList(),
            MeasureTestData.someRect(),
            MeasureTestData.measuredBodyStyle(),
        )
        val page = MeasuredFlowPage(
            raw,
            pageIndex = 3,
            blocks = listOf(block)
        )

        assertEquals(listOf(block), page.blocks)
        assertEquals(3, page.pageIndex)
        assertEquals(Rect(20.0, 20.0, 160.0, 260.0), page.contentArea)
        assertSame(raw, page.raw)
    }

    /**
     * Use case: `MeasuredDocument.pages` is the measured page list; the raw pages are reachable via
     * `raw.pages`.
     */
    @Test
    fun documentExposesMeasuredPagesOnly() {
        val rawFlow =
            FlowPage(layout = MeasureTestData.pageLayout)
        val rawSingle =
            SinglePage(layout = MeasureTestData.pageLayout)
        val raw =
            Document(pages = listOf(rawFlow, rawSingle))
        val measuredPages = listOf(
            MeasuredFlowPage(rawFlow, 0, emptyList()),
            MeasuredSinglePage(
                rawSingle,
                1,
                emptyList()
            ),
        )
        val document =
            MeasuredDocument(raw, measuredPages)

        assertEquals(measuredPages, document.pages)
        assertSame(raw, document.raw)
        assertSame(raw.pages, document.raw.pages)
        assertTrue(document.pages[1] is MeasuredSinglePage)
    }
}
