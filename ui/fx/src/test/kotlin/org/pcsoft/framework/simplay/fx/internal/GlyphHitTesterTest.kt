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
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart
import org.pcsoft.framework.simplay.uicommon.hitTest
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Tests for the [hitTest] glyph hit-testing helper.
 */
class GlyphHitTesterTest : JavaFxTestBase() {

    private val font = Font(family = "Serif", size = 16.0)
    private val style = TextStyle(font = font)

    private data class Case(val part: MeasuredTextPart, val calculator: FxFontMeasureCalculator)

    private fun firstPart(): Case {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = PageLayout(
                        size = Size(400.0, 320.0),
                        margins = Margins(left = 30.0, top = 40.0, right = 30.0, bottom = 40.0),
                    ),
                    blocks = listOf(TextBlock.of("Wonderful", style)),
                ),
            ),
        )
        val calculator = FxFontMeasureCalculator()
        val measured = onFxThread { document.measure(calculator) }
        return Case(measured.pages.single().blocks.first().lines.first().parts.first(), calculator)
    }

    /**
     * A position at or left of the part start maps to offset `0`; a position at or right of the part
     * end maps to `text.length`.
     */
    @Test
    fun hitTestReturnsZeroAtPartStartAndLengthAtPartEnd() {
        val (part, calculator) = firstPart()
        val bounds = part.bounds

        assertEquals(0, onFxThread { hitTest(part, font, bounds.x, calculator) })
        assertEquals(0, onFxThread { hitTest(part, font, bounds.x - 5.0, calculator) })
        assertEquals(part.text.length, onFxThread { hitTest(part, font, bounds.x + bounds.width, calculator) })
        assertEquals(part.text.length, onFxThread { hitTest(part, font, bounds.x + bounds.width + 5.0, calculator) })
    }

    /**
     * A position in the middle of the part maps to an interior offset strictly between `0` and
     * `text.length`.
     */
    @Test
    fun hitTestReturnsNearestOffsetInsidePart() {
        val (part, calculator) = firstPart()
        val midX = part.bounds.x + part.bounds.width / 2.0

        val offset = onFxThread { hitTest(part, font, midX, calculator) }

        assertTrue(offset in 1 until part.text.length, "expected an interior offset but was $offset")
    }
}
