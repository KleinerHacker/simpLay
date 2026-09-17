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

package org.pcsoft.framework.simplay.engine.geometry

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class PaperFormatTest {

    /**
     * Verifies that [PaperFormat.DIN_A4] converts to the well-known ISO 216 point size of 595x842.
     */
    @Test
    fun convertsDinA4ToKnownPointSize() {
        val size = PaperFormat.DIN_A4.size
        assertEquals(595, size.width.roundToInt())
        assertEquals(842, size.height.roundToInt())
    }

    /**
     * Verifies that [PaperFormat.DIN_B4] converts to its expected ISO 216 point size.
     */
    @Test
    fun convertsDinB4ToKnownPointSize() {
        val size = PaperFormat.DIN_B4.size
        assertEquals(709, size.width.roundToInt())
        assertEquals(1001, size.height.roundToInt())
    }

    /**
     * Verifies that [PaperFormat.DIN_C4] converts to its expected ISO 269 point size.
     */
    @Test
    fun convertsDinC4ToKnownPointSize() {
        val size = PaperFormat.DIN_C4.size
        assertEquals(649, size.width.roundToInt())
        assertEquals(918, size.height.roundToInt())
    }

    /**
     * Verifies that [PaperFormat.LETTER] converts to the well-known US Letter point size of 612x792.
     */
    @Test
    fun convertsLetterToKnownPointSize() {
        val size = PaperFormat.LETTER.size
        assertEquals(612, size.width.roundToInt())
        assertEquals(792, size.height.roundToInt())
    }

    /**
     * Verifies that the `withMargin` infix call combines a format's [PaperFormat.size] with the given
     * [Margins] into a [org.pcsoft.framework.simplay.engine.model.PageLayout].
     */
    @Test
    fun withMarginCombinesSizeAndMargins() {
        val margins = Margins(left = 10.0, top = 20.0, right = 10.0, bottom = 20.0)

        val layout = PaperFormat.DIN_A4 withMargin margins

        assertEquals(PaperFormat.DIN_A4.size, layout.size)
        assertEquals(margins, layout.margins)
    }
}
