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

package org.pcsoft.framework.simplay.swing.internal

import java.awt.Font as AwtFont
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight

/**
 * Tests for [SwingFontMeasureCalculator], the AWT-backed font measurer of the `swing` module.
 */
class SwingFontMeasureCalculatorTest {

    private val calc = SwingFontMeasureCalculator()

    /**
     * Verifies that measuring the same `(Font, text)` pair twice returns the identical cached
     * [org.pcsoft.framework.simplay.engine.geometry.TextMetrics] instance, keeping results
     * deterministic as [org.pcsoft.framework.simplay.engine.FontMeasureCalculator] requires.
     */
    @Test
    fun cachesMetricsPerFontAndText() {
        val font = Font(family = "SansSerif", size = 14.0)
        val first = calc.measure(font, "hello")
        val second = calc.measure(font, "hello")
        assertSame(first, second)
    }

    /**
     * Verifies that an empty string measures to zero advance width while still yielding positive
     * ascent and descent.
     */
    @Test
    fun emptyStringHasZeroWidth() {
        val metrics = calc.measure(Font(family = "SansSerif", size = 14.0), "")
        assertEquals(0.0, metrics.width)
        assertTrue(metrics.ascent > 0.0)
        assertTrue(metrics.descent > 0.0)
    }

    /**
     * Verifies that a longer string is measured wider than a shorter prefix of it in the same font.
     */
    @Test
    fun widthGrowsWithText() {
        val font = Font(family = "SansSerif", size = 14.0)
        assertTrue(calc.measure(font, "wwwww").width > calc.measure(font, "w").width)
    }

    /**
     * Verifies that [SwingFontMeasureCalculator.measureAdvances] - which reads all advances from one
     * resolved AWT font - returns exactly the per-glyph widths that calling
     * [SwingFontMeasureCalculator.measure] on each single character yields, so a font fingerprint
     * stays comparable regardless of which path filled it.
     */
    @Test
    fun measureAdvancesMatchesPerGlyphMeasure() {
        val font = Font(family = "SansSerif", size = 14.0)
        val text = "Ag.7"

        val batch = calc.measureAdvances(font, text)
        val perGlyph = text.map { calc.measure(font, it.toString()).width }

        assertEquals(perGlyph, batch)
    }

    /**
     * Verifies that the engine font weight and style are mapped onto the matching AWT font style
     * bits.
     */
    @Test
    fun mapsWeightAndStyleToAwtFont() {
        val awt = calc.toAwtFont(
            Font(family = "Serif", size = 12.0, weight = FontWeight.BOLD, style = FontStyle.ITALIC),
        )
        assertEquals(AwtFont.BOLD or AwtFont.ITALIC, awt.style)
        assertEquals(12, awt.size)
    }
}
