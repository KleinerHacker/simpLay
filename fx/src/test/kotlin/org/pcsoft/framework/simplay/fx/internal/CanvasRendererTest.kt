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
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Tests for the [CanvasRenderer.walkPage] measured-tree walk, recording the emitted draw calls.
 */
class CanvasRendererTest : JavaFxTestBase() {

    private data class Emitted(val text: String, val x: Double, val baselineY: Double)

    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    private fun singlePage(): MeasuredPage {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = PageLayout(
                        size = Size(400.0, 320.0),
                        margins = Margins(left = 30.0, top = 40.0, right = 30.0, bottom = 40.0),
                    ),
                    blocks = listOf(TextBlock.of("The quick brown fox jumps over the lazy dog.", style)),
                ),
            ),
        )
        return onFxThread { document.measure(FxFontMeasureCalculator()) }.pages.single()
    }

    private fun record(page: MeasuredPage, originX: Double = 0.0, originY: Double = 0.0): List<Emitted> {
        val emitted = mutableListOf<Emitted>()
        CanvasRenderer.walkPage(
            page = page,
            originX = originX,
            originY = originY,
            onBlockFont = {},
            onPart = { text, x, baselineY -> emitted += Emitted(text, x, baselineY) },
        )
        return emitted
    }

    /**
     * The walk must emit exactly one draw call per measured part across every block and line of the
     * page.
     */
    @Test
    fun walkEmitsOneFillTextPerMeasuredPart() {
        val page = singlePage()
        val expectedParts = page.blocks.sumOf { block -> block.lines.sumOf { it.parts.size } }

        assertEquals(expectedParts, record(page).size)
    }

    /**
     * A part's baseline y must equal `originY + contentArea.y + line.lineBox.y + line.baseline`, and
     * its x must equal `originX + contentArea.x + part.bounds.x`.
     */
    @Test
    fun walkPlacesPartOnLineBaseline() {
        val page = singlePage()
        val originX = 12.0
        val originY = 7.0

        val firstLine = page.blocks.first().lines.first()
        val firstPart = firstLine.parts.first()
        val expectedX = originX + page.contentArea.x + firstPart.bounds.x
        val expectedBaselineY = originY + page.contentArea.y + firstLine.lineBox.y + firstLine.baseline

        val first = record(page, originX, originY).first()
        assertEquals(expectedX, first.x)
        assertEquals(expectedBaselineY, first.baselineY)
    }
}
