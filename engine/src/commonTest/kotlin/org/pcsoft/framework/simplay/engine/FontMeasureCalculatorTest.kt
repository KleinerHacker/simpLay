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
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * Tests for the default and overridable [FontMeasureCalculator.measureAdvances] contract.
 */
class FontMeasureCalculatorTest {

    private val font = Font(family = "Serif", size = 10.0)

    /**
     * Use case: the default [FontMeasureCalculator.measureAdvances] returns one entry per `Char` of
     * the input, each equal to the advance width [FontMeasureCalculator.measure] reports for that
     * single character.
     */
    @Test
    fun defaultMeasureAdvancesDelegatesToPerGlyphMeasure() {
        val perChar = mapOf("A" to 7.0, "b" to 5.0, "." to 2.5)
        val calc = FontMeasureCalculator { _, text ->
            TextMetrics(width = perChar.getValue(text), ascent = 8.0, descent = 2.0)
        }

        assertEquals(listOf(7.0, 5.0, 2.5), calc.measureAdvances(font, "Ab."))
    }

    /**
     * Use case: an empty string yields an empty advance list from the default implementation.
     */
    @Test
    fun defaultMeasureAdvancesOnEmptyTextIsEmpty() {
        val calc = FontMeasureCalculator { _, _ -> TextMetrics(width = 1.0, ascent = 1.0, descent = 1.0) }

        assertEquals(emptyList(), calc.measureAdvances(font, ""))
    }

    /**
     * Use case: a backend that overrides [FontMeasureCalculator.measureAdvances] is used by callers
     * such as [org.pcsoft.framework.simplay.engine.model.FontFingerprint.of] instead of the
     * per-glyph default path.
     */
    @Test
    fun overriddenMeasureAdvancesIsHonoured() {
        val calc = object : FontMeasureCalculator {
            override fun measure(font: Font, text: String): TextMetrics =
                TextMetrics(width = 99.0, ascent = 8.0, descent = 2.0)

            override fun measureAdvances(font: Font, text: String): List<Double> =
                text.map { 3.0 }
        }

        assertEquals(listOf(3.0, 3.0, 3.0), calc.measureAdvances(font, "xyz"))
    }
}
