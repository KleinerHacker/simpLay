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

package org.pcsoft.framework.simplay.uicommon

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Tests for the [hitTest] glyph hit-testing helper, driven by [StubFontMeasureCalculator] so glyph
 * widths are exactly `0.6 * fontSize` and the expected offsets are known.
 */
class GlyphHitTesterTest {

    private val font = Font(family = "Serif", size = 10.0)
    private val calculator = StubFontMeasureCalculator()

    private fun firstPart(): MeasuredTextPart {
        val layout = PageLayout(
            size = Size(width = 600.0, height = 800.0),
            margins = Margins(left = 40.0, top = 40.0, right = 40.0, bottom = 40.0),
        )
        val measured = Document(
            pages = listOf(
                FlowPage(layout = layout, blocks = listOf(TextBlock.of("abcdef", TextStyle(font = font)))),
            ),
        ).measure(calculator)
        return measured.pages.first().blocks.first().lines.first().parts.first()
    }

    /**
     * Verifies that a position at or left of the part start maps to offset `0` and a position at or
     * right of the part end maps to `text.length`.
     */
    @Test
    fun clampsToPartBounds() {
        val part = firstPart()
        assertEquals(0, hitTest(part, font, part.bounds.x, calculator))
        assertEquals(0, hitTest(part, font, part.bounds.x - 5.0, calculator))
        assertEquals(part.text.length, hitTest(part, font, part.bounds.x + part.bounds.width, calculator))
        assertEquals(part.text.length, hitTest(part, font, part.bounds.x + part.bounds.width + 20.0, calculator))
    }

    /**
     * Verifies that a position roughly in the middle of the part maps to the character offset whose
     * prefix width is closest, i.e. about half of the six glyphs.
     */
    @Test
    fun returnsNearestInteriorOffset() {
        val part = firstPart()
        val midX = part.bounds.x + part.bounds.width / 2.0
        assertEquals(3, hitTest(part, font, midX, calculator))
    }
}
