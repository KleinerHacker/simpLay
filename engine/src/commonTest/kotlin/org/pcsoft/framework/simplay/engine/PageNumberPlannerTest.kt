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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasureTestData
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextAlignment

/**
 * Verifies [MeasuredDocument.planPageNumbers]: positions, exclusion, start number and the counting strategy
 * switch.
 */
class PageNumberPlannerTest {

    private val layout = PageLayout(Size(200.0, 300.0), Margins(20.0, 30.0, 20.0, 40.0))

    private fun measuredDocument(sheetCount: Int): MeasuredDocument {
        val rawPages = List(sheetCount) { FlowPage(layout, emptyList()) }
        val measuredPages: List<MeasuredPage> = rawPages.mapIndexed { index, raw ->
            MeasuredFlowPage(raw, index, emptyList())
        }
        return MeasuredDocument(Document(rawPages), measuredPages)
    }

    /**
     * Use case: `OFF` yields no labels at all, not even null placeholders.
     */
    @Test
    fun offPositionProducesNoLabels() {
        val measured = measuredDocument(3)

        assertTrue(measured.planPageNumbers(PageNumbering.OFF).isEmpty())
    }

    /**
     * Use case: each non-alternating position anchors the label at the expected edge and horizontal
     * alignment.
     */
    @Test
    fun eachPositionAnchorsAtExpectedEdge() {
        val measured = measuredDocument(1)

        val topLeft = measured.planPageNumbers(PageNumbering(position = PageNumberPosition.TOP_LEFT))[0]!!
        assertEquals(TextAlignment.LEFT, topLeft.alignment)
        assertEquals(layout.margins.top / 2.0, topLeft.y)

        val bottomRight =
            measured.planPageNumbers(PageNumbering(position = PageNumberPosition.BOTTOM_RIGHT))[0]!!
        assertEquals(TextAlignment.RIGHT, bottomRight.alignment)
        assertEquals(layout.size.height - layout.margins.bottom / 2.0, bottomRight.y)

        val topCenter = measured.planPageNumbers(PageNumbering(position = PageNumberPosition.TOP_CENTER))[0]!!
        assertEquals(TextAlignment.CENTER, topCenter.alignment)
        assertEquals(layout.size.width / 2.0, topCenter.x)
    }

    /**
     * Use case: a page whose id is listed in `excludedPageIds` shows no number. The default
     * `CONTINUOUS` counting still advances the counter for the excluded sheet, so the next sheet
     * carries `2`, not `1`.
     */
    @Test
    fun excludedPageShowsNoNumber() {
        val measured = measuredDocument(2)
        val excludedId = measured.pages[0].raw.id
        val numbering = PageNumbering(position = PageNumberPosition.TOP_CENTER, excludedPageIds = setOf(excludedId))

        val labels = measured.planPageNumbers(numbering)

        assertNull(labels[0])
        assertEquals("2", labels[1]!!.text)
    }

    /**
     * Use case: with [PageCountingMode.CONTINUOUS] an excluded sheet is skipped but the following
     * sheet keeps the number it would have had anyway.
     */
    @Test
    fun continuousKeepsCountingAcrossExcluded() {
        val measured = measuredDocument(3)
        val excludedId = measured.pages[1].raw.id
        val numbering = PageNumbering(
            position = PageNumberPosition.TOP_CENTER,
            excludedPageIds = setOf(excludedId),
            counting = PageCountingMode.CONTINUOUS,
        )

        val labels = measured.planPageNumbers(numbering)

        assertEquals("1", labels[0]!!.text)
        assertNull(labels[1])
        assertEquals("3", labels[2]!!.text)
    }

    /**
     * Use case: with [PageCountingMode.SKIP_EXCLUDED] the sheet following an excluded one receives
     * the lower number the excluded sheet would have taken.
     */
    @Test
    fun skipExcludedDoesNotAdvance() {
        val measured = measuredDocument(3)
        val excludedId = measured.pages[1].raw.id
        val numbering = PageNumbering(
            position = PageNumberPosition.TOP_CENTER,
            excludedPageIds = setOf(excludedId),
            counting = PageCountingMode.SKIP_EXCLUDED,
        )

        val labels = measured.planPageNumbers(numbering)

        assertEquals("1", labels[0]!!.text)
        assertNull(labels[1])
        assertEquals("2", labels[2]!!.text)
    }

    /**
     * Use case: the first counted sheet carries `startNumber` instead of `1`.
     */
    @Test
    fun startNumberOffsetsFirstLabel() {
        val measured = measuredDocument(2)
        val numbering = PageNumbering(position = PageNumberPosition.TOP_CENTER, startNumber = 7)

        val labels = measured.planPageNumbers(numbering)

        assertEquals("7", labels[0]!!.text)
        assertEquals("8", labels[1]!!.text)
    }

    /**
     * Use case: `INNER` / `OUTER` mirror their horizontal side by sheet parity - an odd sheet is a
     * right-hand page (inner = left, outer = right), an even sheet is a left-hand page (inner =
     * right, outer = left).
     */
    @Test
    fun innerOuterAlternatesByParity() {
        val measured = measuredDocument(2)

        val inner = measured.planPageNumbers(PageNumbering(position = PageNumberPosition.TOP_INNER))
        assertEquals(TextAlignment.LEFT, inner[0]!!.alignment)
        assertEquals(TextAlignment.RIGHT, inner[1]!!.alignment)

        val outer = measured.planPageNumbers(PageNumbering(position = PageNumberPosition.TOP_OUTER))
        assertEquals(TextAlignment.RIGHT, outer[0]!!.alignment)
        assertEquals(TextAlignment.LEFT, outer[1]!!.alignment)
    }

    /**
     * Use case: every overflow sheet of a flow page shares the same model id, so excluding that id
     * hides the number on all of them.
     */
    @Test
    fun flowOverflowSheetsShareModelId() {
        val rawPage = FlowPage(layout, emptyList())
        val measured = MeasuredDocument(
            Document(listOf(rawPage)),
            listOf(
                MeasuredFlowPage(rawPage, 0, emptyList()),
                MeasuredFlowPage(rawPage, 1, emptyList()),
            ),
        )
        val numbering = PageNumbering(position = PageNumberPosition.TOP_CENTER, excludedPageIds = setOf(rawPage.id))

        val labels = measured.planPageNumbers(numbering)

        assertNull(labels[0])
        assertNull(labels[1])
    }

    /**
     * Use case: a `BOTTOM_*` position follows a grown `MeasuredSinglePage`'s `effectiveSize`, not
     * its raw layout height - otherwise the label would land near the top of an overflowed page
     * instead of its actual bottom edge.
     */
    @Test
    fun bottomPositionFollowsGrownSinglePageHeight() {
        val rawPage = SinglePage(layout, emptyList())
        val block = MeasureTestData.measuredBlockReaching(bottom = 500.0)
        val measuredPage = MeasuredSinglePage(rawPage, 0, listOf(block))
        val expectedHeight = layout.margins.top + 500.0 + layout.margins.bottom
        check(expectedHeight > layout.size.height) { "fixture must actually overflow the raw layout" }

        val measured = MeasuredDocument(Document(listOf(rawPage)), listOf(measuredPage))
        val numbering = PageNumbering(position = PageNumberPosition.BOTTOM_CENTER)

        val label = measured.planPageNumbers(numbering)[0]!!

        assertEquals(expectedHeight - layout.margins.bottom / 2.0, label.y)
    }
}
