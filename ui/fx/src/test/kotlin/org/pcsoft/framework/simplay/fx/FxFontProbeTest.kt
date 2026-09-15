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

package org.pcsoft.framework.simplay.fx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator

/**
 * Tests for [FxFontProbe]: family availability against the JavaFX text stack, deterministic
 * fingerprinting and stamping a whole document. Runs on the headless JavaFX toolkit.
 */
class FxFontProbeTest : JavaFxTestBase() {

    private val serif = Font(family = "Serif", size = 13.0)
    private val fictional = Font(family = "Totally-Made-Up-Font-XYZ", size = 13.0)

    private val document = Document(
        listOf(
            FlowPage(
                PageLayout(Size(400.0, 300.0), Margins(20.0, 20.0, 20.0, 20.0)),
                listOf(
                    TextBlock.of("First block", TextStyle(font = serif)),
                    TextBlock.of("Second block", TextStyle(font = Font(family = "Monospaced", size = 13.0))),
                ),
            ),
        ),
    )

    /**
     * Use case: a JavaFX standard family is always reported as available.
     */
    @Test
    fun isFamilyAvailableTrueForStandardFamily() {
        assertTrue(onFxThread { FxFontProbe().isFamilyAvailable("Serif") })
    }

    /**
     * Use case: a family name that is not installed is reported as unavailable.
     */
    @Test
    fun isFamilyAvailableFalseForFictionalFamily() {
        assertFalse(onFxThread { FxFontProbe().isFamilyAvailable("Totally-Made-Up-Font-XYZ") })
    }

    /**
     * Use case: a fictional family cannot resolve to itself, so it is never classified as
     * [org.pcsoft.framework.simplay.uicommon.FontAvailability.AVAILABLE].
     */
    @Test
    fun checkAvailabilityIsNotAvailableForFictionalFamily() {
        val availability = onFxThread { FxFontProbe().checkAvailability(fictional) }

        assertNotEquals(org.pcsoft.framework.simplay.uicommon.FontAvailability.AVAILABLE, availability)
    }

    /**
     * Use case: a fingerprint carries one advance per reference glyph and is identical across
     * repeated calls for the same font.
     */
    @Test
    fun fingerprintIsDeterministic() {
        val (first, second) = onFxThread {
            val probe = FxFontProbe()
            probe.fingerprint(serif) to probe.fingerprint(serif)
        }

        assertEquals(FontFingerprint.REFERENCE_GLYPHS.length, first.advances.size)
        assertEquals(first, second)
    }

    /**
     * Use case: [FxFontProbe.verify] accepts a font against its own fingerprint and rejects it
     * against the fingerprint of a visibly different family.
     */
    @Test
    fun verifyAcceptsOwnFingerprintAndRejectsForeignOne() {
        val result = onFxThread {
            val probe = FxFontProbe()
            val serifPrint = probe.fingerprint(serif)
            val monoPrint = probe.fingerprint(Font(family = "Monospaced", size = 13.0))
            probe.verify(serif, serifPrint) to probe.verify(serif, monoPrint)
        }

        assertTrue(result.first)
        assertFalse(result.second)
    }

    /**
     * Use case: [FxFontProbe.stamp] fills a fingerprint into every block font, and re-measuring the
     * stamped document with a fresh JavaFX measurer reports no deviations.
     */
    @Test
    fun stampFillsDocumentAndSurvivesRemeasure() {
        val deviations = onFxThread {
            val stamped = FxFontProbe().stamp(document)
            assertTrue(stamped.pages.flatMap { it.blocks }.all { it.style.font.fingerprint != null })
            stamped.measure(FxFontMeasureCalculator()).fingerprintDeviations
        }

        assertEquals(emptyList(), deviations)
    }
}
