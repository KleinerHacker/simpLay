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

package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics

/**
 * Tests for [FontFingerprint]: taking one via [FontFingerprint.of], comparing two with
 * [FontFingerprint.matches] and the [FontFingerprint.encode] / [FontFingerprint.decode] text form.
 */
class FontFingerprintTest {

    /** Deterministic measurer: advance scales with the character count, metrics with the size. */
    private val measurer = FontMeasureCalculator { font, text ->
        TextMetrics(width = text.length * font.size * 0.6, ascent = font.size * 0.8, descent = font.size * 0.2)
    }

    private val font = Font(family = "Serif", size = 12.0)

    /**
     * Use case: a fingerprint holds one advance per reference glyph and reports the ascent and
     * descent measured at the normalization size, not at the font's own size.
     */
    @Test
    fun ofCapturesNormalizedReferenceMetrics() {
        val print = FontFingerprint.of(measurer, font)

        assertEquals(FontFingerprint.REFERENCE_GLYPHS.length, print.advances.size)
        assertEquals(FontFingerprint.NORMALIZED_SIZE, print.normalizedSize)
        assertEquals(80.0, print.ascent, "0.8 * 100")
        assertEquals(20.0, print.descent, "0.2 * 100")
        assertTrue(print.advances.all { it == 60.0 }, "0.6 * 100 per glyph")
    }

    /**
     * Use case: the concrete font size does not influence the fingerprint, so the same face at two
     * sizes yields an equal fingerprint.
     */
    @Test
    fun ofIsIndependentOfConcreteFontSize() {
        assertEquals(
            FontFingerprint.of(measurer, font.copy(size = 8.0)),
            FontFingerprint.of(measurer, font.copy(size = 72.0)),
        )
    }

    /**
     * Use case: an already stored fingerprint on the font is ignored while a new one is taken, so
     * re-stamping is idempotent.
     */
    @Test
    fun ofIgnoresAnExistingFingerprintOnTheFont() {
        val once = FontFingerprint.of(measurer, font)
        val twice = FontFingerprint.of(measurer, font.copy(fingerprint = once))

        assertEquals(once, twice)
    }

    /**
     * Use case: two fingerprints whose every value stays within the tolerance count as the same
     * face; an advance drifting past the tolerance breaks the match.
     */
    @Test
    fun matchesAcceptsWithinToleranceAndRejectsBeyond() {
        val base = FontFingerprint.of(measurer, font)
        val nudged = base.copy(advances = base.advances.mapIndexed { i, a -> if (i == 0) a + 0.4 else a })
        val shifted = base.copy(advances = base.advances.mapIndexed { i, a -> if (i == 0) a + 5.0 else a })

        assertTrue(base.matches(base))
        assertTrue(base.matches(nudged, tolerance = 0.5))
        assertFalse(base.matches(shifted, tolerance = 0.5))
    }

    /**
     * Use case: fingerprints of different reference lengths never match, even if the shared values
     * agree.
     */
    @Test
    fun matchesRejectsDifferentAdvanceCount() {
        val base = FontFingerprint.of(measurer, font)

        assertFalse(base.matches(base.copy(advances = base.advances.dropLast(1))))
    }

    /**
     * Use case: a vertical-metric difference past the tolerance breaks the match even when all
     * advances agree.
     */
    @Test
    fun matchesRejectsDivergingVerticalMetrics() {
        val base = FontFingerprint.of(measurer, font)

        assertFalse(base.matches(base.copy(ascent = base.ascent + 3.0)))
        assertFalse(base.matches(base.copy(descent = base.descent + 3.0)))
    }

    /**
     * Use case: [FontFingerprint.encode] followed by [FontFingerprint.decode] reproduces an equal
     * fingerprint, so a caller can persist the single line next to a document.
     */
    @Test
    fun encodeDecodeRoundTrips() {
        val print = FontFingerprint.of(measurer, font)

        assertEquals(print, FontFingerprint.decode(print.encode()))
    }

    /**
     * Use case: [FontFingerprint.decode] rejects a string that is not a valid `v1` fingerprint line.
     */
    @Test
    fun decodeRejectsMalformedInput() {
        assertFailsWith<IllegalArgumentException> { FontFingerprint.decode("not-a-fingerprint") }
        assertFailsWith<IllegalArgumentException> { FontFingerprint.decode("v1|100.0|80.0|20.0") }
        assertFailsWith<IllegalArgumentException> { FontFingerprint.decode("v1|100.0|x|20.0|60.0") }
    }

    /**
     * Use case: the reference glyph set covers the Latin alphabet, digits, common diacritics and
     * common punctuation so advances, ascenders and descenders are all represented.
     */
    @Test
    fun referenceGlyphsCoverLettersDigitsDiacriticsAndPunctuation() {
        val glyphs = FontFingerprint.REFERENCE_GLYPHS

        assertTrue(glyphs.any { it in 'A'..'Z' })
        assertTrue(glyphs.any { it in 'a'..'z' })
        assertTrue(glyphs.any { it in '0'..'9' })
        assertTrue(glyphs.contains('ä'))
        assertTrue(glyphs.contains('ç'))
        assertTrue(glyphs.contains('.'))
    }
}
