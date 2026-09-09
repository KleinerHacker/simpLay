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
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.pcsoft.framework.simplay.engine.internal.SimpLayFontEngine
import org.pcsoft.framework.simplay.engine.measure.FontFingerprintStatus
import org.pcsoft.framework.simplay.engine.model.FontFingerprint

/**
 * Verifies that [SimpLayFontEngine] derives vertical font metrics from the reference glyphs, sets
 * the leading to zero, caches resolved fonts per instance and reports the fingerprint status.
 */
class SimpLayFontEngineTest {

    private fun fontEngine() = SimpLayFontEngine.builder(EngineTestData.measurer).build()

    /**
     * Use case: resolving a font measures the reference glyph string once and takes the ascent and
     * descent from it, with a leading of zero.
     */
    @Test
    fun resolvesFontMetricsFromReferenceGlyphs() {
        val measured = fontEngine().resolveFont(EngineTestData.font)

        assertEquals(EngineTestData.ASCENT, measured.metrics.ascent)
        assertEquals(EngineTestData.DESCENT, measured.metrics.descent)
        assertEquals(0.0, measured.metrics.leading)
    }

    /**
     * Use case: the reference glyph string covers the Latin alphabet, digits, common diacritics and
     * common punctuation so ascenders, descenders and accents are all represented.
     */
    @Test
    fun referenceGlyphsCoverLettersDigitsDiacriticsAndPunctuation() {
        val glyphs = SimpLayFontEngine.REFERENCE_GLYPHS

        assertEquals(true, glyphs.any { it in 'A'..'Z' })
        assertEquals(true, glyphs.any { it in 'a'..'z' })
        assertEquals(true, glyphs.any { it in '0'..'9' })
        assertEquals(true, glyphs.contains('ä'))
        assertEquals(true, glyphs.contains('ç'))
        assertEquals(true, glyphs.contains('ł'))
        assertEquals(true, glyphs.contains('.'))
    }

    /**
     * Use case: one instance caches a resolved font, while a separate instance resolves its own.
     */
    @Test
    fun cachesResolvedFontPerInstance() {
        val engine = fontEngine()

        val first = engine.resolveFont(EngineTestData.font)
        val second = engine.resolveFont(EngineTestData.font)
        val fromOtherInstance = fontEngine().resolveFont(EngineTestData.font)

        assertSame(first, second)
        assertNotSame(first, fromOtherInstance)
    }

    /**
     * Use case: a resolved style exposes a measured style whose line height is derived from the
     * resolved font metrics and the style line spacing, and whose font is the instance's cached one.
     */
    @Test
    fun resolvesStyleWithDerivedLineHeightAndCachedFont() {
        val engine = fontEngine()

        val style = engine.resolveStyle(EngineTestData.style)

        assertEquals(EngineTestData.LINE_HEIGHT, style.resolvedLineHeight)
        assertSame(engine.resolveFont(EngineTestData.font), style.font)
    }

    /**
     * Use case: a font without a stored fingerprint is resolved with status
     * [FontFingerprintStatus.NOT_CHECKED].
     */
    @Test
    fun reportsNotCheckedWhenFontCarriesNoFingerprint() {
        val measured = fontEngine().resolveFont(EngineTestData.font)

        assertEquals(FontFingerprintStatus.NOT_CHECKED, measured.fingerprintStatus)
    }

    /**
     * Use case: a font whose stored fingerprint still matches the current measurement is resolved
     * with status [FontFingerprintStatus.MATCH].
     */
    @Test
    fun reportsMatchWhenStoredFingerprintStillFits() {
        val stored = FontFingerprint.of(EngineTestData.measurer, EngineTestData.font)

        val measured = fontEngine().resolveFont(EngineTestData.font.copy(fingerprint = stored))

        assertEquals(FontFingerprintStatus.MATCH, measured.fingerprintStatus)
    }

    /**
     * Use case: a font whose stored fingerprint no longer matches the current measurement is
     * resolved with status [FontFingerprintStatus.DEVIATION].
     */
    @Test
    fun reportsDeviationWhenStoredFingerprintDiffers() {
        val stale = FontFingerprint.of(EngineTestData.measurer, EngineTestData.font)
            .let { it.copy(advances = it.advances.map { a -> a + 10.0 }) }

        val measured = fontEngine().resolveFont(EngineTestData.font.copy(fingerprint = stale))

        assertEquals(FontFingerprintStatus.DEVIATION, measured.fingerprintStatus)
    }
}
