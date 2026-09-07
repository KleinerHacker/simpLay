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
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Tests for [FxFontMeasureCalculator], the JavaFX-backed font measurer.
 */
class FxFontMeasureCalculatorTest : JavaFxTestBase() {

    private val font = Font(family = "Serif", size = 16.0)

    /**
     * A non-empty string must yield a strictly positive advance width and a positive line height
     * (ascent + descent), proving the JavaFX text stack was queried.
     */
    @Test
    fun measureReturnsPositiveAdvanceForNonEmptyText() {
        val metrics = onFxThread { FxFontMeasureCalculator().measure(font, "Hello world") }

        assertTrue(metrics.width > 0.0, "width must be positive")
        assertTrue(metrics.ascent + metrics.descent > 0.0, "line height must be positive")
    }

    /**
     * Measuring the same font/text pair twice must return equal metrics, so the layout stays
     * deterministic regardless of the internal cache.
     */
    @Test
    fun measureIsDeterministicForSameFontAndText() {
        val calculator = FxFontMeasureCalculator()

        val first = onFxThread { calculator.measure(font, "Deterministic") }
        val second = onFxThread { calculator.measure(font, "Deterministic") }

        assertEquals(first, second)
    }

    /**
     * A larger font size must produce a larger advance width and a larger line height for the same
     * text.
     */
    @Test
    fun measureScalesWithFontSize() {
        val calculator = FxFontMeasureCalculator()

        val small = onFxThread { calculator.measure(Font(family = "Serif", size = 12.0), "Scaling") }
        val large = onFxThread { calculator.measure(Font(family = "Serif", size = 36.0), "Scaling") }

        assertTrue(large.width > small.width, "width must grow with font size")
        assertTrue(large.ascent + large.descent > small.ascent + small.descent, "height must grow with font size")
    }
}
