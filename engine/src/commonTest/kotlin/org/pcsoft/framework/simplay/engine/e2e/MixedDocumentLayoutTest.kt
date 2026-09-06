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
import org.pcsoft.framework.simplay.engine.measure.charCount
import org.pcsoft.framework.simplay.engine.measure.symbolCount
import org.pcsoft.framework.simplay.engine.measure.wordCount
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.charCount as rawCharCount
import org.pcsoft.framework.simplay.engine.model.symbolCount as rawSymbolCount
import org.pcsoft.framework.simplay.engine.model.wordCount as rawWordCount

/**
 * End-to-end check that a document mixing page kinds, page layouts, blocks and text styles is
 * measured into a consistent [org.pcsoft.framework.simplay.engine.measure.MeasuredDocument].
 */
class MixedDocumentLayoutTest {

    private val document = E2ETestData.mixedDocument()
    private val measured = E2ETestData.engine().measure(document)

    /**
     * Use case: the first flow page keeps both of its blocks and each block is wrapped into several
     * measured lines according to its own content width.
     */
    @Test
    fun firstFlowPageWrapsEachBlockIntoSeveralLines() {
        val page = measured.pages[0]
        assertTrue(page is MeasuredFlowPage)
        assertEquals(0, page.pageIndex)
        assertEquals(2, page.blocks.size)
        assertEquals(3, page.blocks[0].lines.size)
        assertEquals(5, page.blocks[1].lines.size)
    }

    /**
     * Use case: each block on the first page carries its own resolved style, so alignment and the
     * resolved line height follow the block's [org.pcsoft.framework.simplay.engine.model.TextStyle].
     */
    @Test
    fun firstFlowPageBlocksKeepTheirOwnStyle() {
        val page = measured.pages[0]

        assertEquals(TextAlignment.LEFT, page.blocks[0].style.alignment)
        assertEquals(10.0, page.blocks[0].style.resolvedLineHeight)

        assertEquals(TextAlignment.JUSTIFY, page.blocks[1].style.alignment)
        assertEquals(18.0, page.blocks[1].style.resolvedLineHeight, absoluteTolerance = 1e-9)
    }

    /**
     * Use case: a justified block stretches every line but its last one to the full content width,
     * and the last line stays flush left.
     */
    @Test
    fun justifiedBlockStretchesEveryLineButTheLast() {
        val justified = measured.pages[0].blocks[1]
        val contentWidth = measured.pages[0].contentArea.width

        justified.lines.dropLast(1).forEach { line ->
            assertFalse(line.lastLine)
            assertEquals(0.0, line.parts.first().bounds.x)
            val rightEdge = line.parts.last().let { it.bounds.x + it.bounds.width }
            assertEquals(contentWidth, rightEdge, absoluteTolerance = 1e-9)
        }

        val last = justified.lines.last()
        assertTrue(last.lastLine)
        assertEquals(0.0, last.parts.first().bounds.x)
    }

    /**
     * Use case: the second page is a single page whose block is taller than its layout height, so
     * the measured page grows in height while keeping the layout width.
     */
    @Test
    fun singlePageGrowsPastItsLayoutHeight() {
        val page = measured.pages[1]
        assertTrue(page is MeasuredSinglePage)

        assertEquals(120.0, page.effectiveSize.width)
        assertTrue(page.effectiveSize.height > page.layout.size.height)
        assertEquals(page.requiredContentHeight, page.effectiveSize.height, absoluteTolerance = 1e-9)
    }

    /**
     * Use case: the third page's single block does not fit one flow page, so it is spread over
     * several measured flow pages with a document-wide continuous page index.
     */
    @Test
    fun longFlowBlockSpansSeveralPages() {
        val flowSlices = measured.pages.drop(2)

        assertTrue(flowSlices.size >= 2)
        assertTrue(flowSlices.all { it is MeasuredFlowPage })
        assertEquals(measured.pages.indices.toList(), measured.pages.map { it.pageIndex })
    }

    /**
     * Use case: only the very last measured line of the split third block is flagged as the block's
     * last line; every earlier line across every slice is not.
     */
    @Test
    fun onlyTheFinalLineOfTheSplitBlockIsFlaggedLast() {
        val allLines = measured.pages.drop(2).flatMap { page -> page.blocks.flatMap { it.lines } }

        assertTrue(allLines.dropLast(1).none { it.lastLine })
        assertTrue(allLines.last().lastLine)
    }

    /**
     * Use case: the counting extensions on the first (non-split) measured page reproduce the word,
     * symbol and character totals of the corresponding raw page.
     */
    @Test
    fun countingOnTheFirstPageMatchesTheRawPage() {
        val rawPage = document.pages[0]

        assertEquals(rawPage.rawWordCount(), measured.pages[0].wordCount())
        assertEquals(rawPage.rawSymbolCount(), measured.pages[0].symbolCount())
        assertEquals(rawPage.rawCharCount(), measured.pages[0].charCount())
    }
}
