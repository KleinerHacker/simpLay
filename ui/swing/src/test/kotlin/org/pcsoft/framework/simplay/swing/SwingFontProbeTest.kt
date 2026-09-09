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

package org.pcsoft.framework.simplay.swing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.FontAvailability

/**
 * Tests for [SwingFontProbe]: family availability against the AWT text stack, deterministic
 * fingerprinting and stamping a whole document.
 */
class SwingFontProbeTest {

    private val probe = SwingFontProbe()
    private val serif = Font(family = "Serif", size = 13.0)
    private val fictional = Font(family = "Totally-Made-Up-Font-XYZ", size = 13.0)

    /**
     * Use case: an AWT logical family is always reported as available without touching the installed
     * font list.
     */
    @Test
    fun isFamilyAvailableTrueForLogicalFamily() {
        assertTrue(probe.isFamilyAvailable("Serif"))
        assertTrue(probe.isFamilyAvailable("Monospaced"))
    }

    /**
     * Use case: a family name that is not installed is reported as unavailable.
     */
    @Test
    fun isFamilyAvailableFalseForFictionalFamily() {
        assertFalse(probe.isFamilyAvailable("Totally-Made-Up-Font-XYZ"))
    }

    /**
     * Use case: a logical family resolves to itself, so it is classified as
     * [FontAvailability.AVAILABLE].
     */
    @Test
    fun checkAvailabilityIsAvailableForLogicalFamily() {
        assertEquals(FontAvailability.AVAILABLE, probe.checkAvailability(serif))
    }

    /**
     * Use case: a fictional family cannot resolve to itself, so it is classified as missing or
     * substituted - never [FontAvailability.AVAILABLE].
     */
    @Test
    fun checkAvailabilityIsNotAvailableForFictionalFamily() {
        assertNotEquals(FontAvailability.AVAILABLE, probe.checkAvailability(fictional))
    }

    /**
     * Use case: a fingerprint carries one advance per reference glyph and is identical across
     * repeated calls for the same font.
     */
    @Test
    fun fingerprintIsDeterministic() {
        val first = probe.fingerprint(serif)
        val second = probe.fingerprint(serif)

        assertEquals(FontFingerprint.REFERENCE_GLYPHS.length, first.advances.size)
        assertEquals(first, second)
    }

    /**
     * Use case: [SwingFontProbe.verify] accepts a font against its own fingerprint and rejects it
     * against the fingerprint of a visibly different family.
     */
    @Test
    fun verifyAcceptsOwnFingerprintAndRejectsForeignOne() {
        val serifPrint = probe.fingerprint(serif)
        val monoPrint = probe.fingerprint(Font(family = "Monospaced", size = 13.0))

        assertTrue(probe.verify(serif, serifPrint))
        assertFalse(probe.verify(serif, monoPrint))
    }

    /**
     * Use case: [SwingFontProbe.stamp] fills a fingerprint into every block font, and re-measuring
     * the stamped document with a fresh AWT measurer reports no deviations.
     */
    @Test
    fun stampFillsDocumentAndSurvivesRemeasure() {
        val stamped = probe.stamp(TestDocuments.short)

        val fonts = stamped.pages.flatMap { it.blocks }.map { it.style.font }
        assertTrue(fonts.all { it.fingerprint != null })

        val measured = stamped.measure(SwingFontMeasureCalculator())
        assertEquals(emptyList(), measured.fingerprintDeviations)
    }
}
