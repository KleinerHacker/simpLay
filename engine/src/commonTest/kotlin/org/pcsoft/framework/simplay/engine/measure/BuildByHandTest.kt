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
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a complete measured model can be assembled by hand, without any engine, and that
 * every level is reachable from the resulting [MeasuredDocument].
 */
class BuildByHandTest {

    /**
     * Use case: a test builds font, style, parts, line, block, page and document directly and the
     * whole tree can be traversed from the top.
     */
    @Test
    fun fullMeasuredModelAssembledByHand() {
        val measuredStyle = MeasureTestData.measuredBodyStyle()

        val rawBlock = TextBlock.of("Engine core", MeasureTestData.bodyStyle)
        val parts = rawBlock.parts.mapIndexed { index, part ->
            MeasuredTextPart(
                part,
                Rect(
                    x = index * 30.0,
                    y = 0.0,
                    width = 28.0,
                    height = 12.0
                )
            )
        }
        val line = MeasuredLine(
            parts = parts,
            lineBox = Rect(
                x = 0.0,
                y = 0.0,
                width = 160.0,
                height = 19.0
            ),
            baseline = 9.0,
            ascent = MeasureTestData.bodyMetrics.ascent,
            descent = MeasureTestData.bodyMetrics.descent,
            alignment = MeasureTestData.bodyStyle.alignment,
            lastLine = true,
        )
        val block = MeasuredTextBlock(
            raw = rawBlock,
            lines = listOf(line),
            bounds = Rect(
                x = 0.0,
                y = 0.0,
                width = 160.0,
                height = 19.0
            ),
            style = measuredStyle,
        )

        val rawPage = FlowPage(
            layout = MeasureTestData.pageLayout,
            blocks = listOf(rawBlock)
        )
        val page = MeasuredFlowPage(
            raw = rawPage,
            pageIndex = 0,
            blocks = listOf(block),
        )

        val rawDocument = Document(pages = listOf(rawPage))
        val document = MeasuredDocument(
            rawDocument,
            pages = listOf(page)
        )

        val reachedPart = document.pages
            .single()
            .blocks
            .single()
            .lines
            .single()
            .parts
            .first()

        assertEquals("Engine", reachedPart.text)
        assertEquals(Rect(0.0, 0.0, 28.0, 12.0), reachedPart.bounds)
        assertSame(measuredStyle, document.pages.single().blocks.single().style)
        assertEquals(
            MeasureTestData.bodyResolvedLineHeight,
            document.pages.single().blocks.single().style.resolvedLineHeight,
        )
        assertTrue(document.pages.single() is MeasuredFlowPage)
        assertSame(rawDocument, document.raw)
        assertEquals(rawBlock.parts.size, page.blocks.single().lines.single().parts.size)
    }

    /**
     * Use case: an empty measured document with no pages is a valid hand-built model, and the raw
     * document stays reachable.
     */
    @Test
    fun emptyMeasuredDocumentIsAllowed() {
        val raw = Document()
        val document =
            MeasuredDocument(raw, pages = emptyList())

        assertTrue(document.pages.isEmpty())
        assertSame(raw, document.raw)
        assertTrue(document.raw.pages.isEmpty())
    }
}
