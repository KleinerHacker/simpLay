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

import java.awt.GraphicsEnvironment
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.engine.withFontFingerprints
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.FontAvailability

/**
 * Public probe for the AWT text stack: tells whether a [Font] family is still installed, stamps a
 * [Document]'s fonts with a size-independent [FontFingerprint] before it is persisted, and re-checks
 * a single font against a stored fingerprint. The Swing counterpart of the `fx` module's
 * `FxFontProbe`.
 *
 * ```kotlin
 * val probe = SwingFontProbe()
 *
 * // on save: stamp every block font
 * val toPersist = probe.stamp(document)
 *
 * // on load, elsewhere: the measure pass reports deviations
 * val measured = toPersist.measure(SwingFontMeasureCalculator())
 * if (measured.fingerprintDeviations.isNotEmpty()) warnFontsChanged(measured.fingerprintDeviations)
 *
 * // ad-hoc, for a single family
 * when (probe.checkAvailability(style.font)) {
 *     FontAvailability.AVAILABLE   -> Unit
 *     FontAvailability.SUBSTITUTED,
 *     FontAvailability.MISSING     -> warnFontMissing()
 * }
 * ```
 *
 * Measurement runs headlessly and is cached per `(Font, text)` pair. A fingerprint taken here only
 * compares against another one taken here - AWT and JavaFX measure the same font differently.
 */
class SwingFontProbe {

    private val measurer = SwingFontMeasureCalculator()

    /**
     * Whether [family] resolves to a real installed family (case-insensitive), or is one of the AWT
     * logical families, which are always available.
     */
    fun isFamilyAvailable(family: String): Boolean {
        if (family.trim().lowercase() in LOGICAL_FAMILIES) return true
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .any { it.equals(family, ignoreCase = true) }
    }

    /**
     * Classifies how the platform resolves [font]'s family: to itself ([FontAvailability.AVAILABLE]),
     * to a default fallback ([FontAvailability.MISSING]) or to a different real family
     * ([FontAvailability.SUBSTITUTED]).
     */
    fun checkAvailability(font: Font): FontAvailability {
        val requested = font.family.trim()
        if (requested.lowercase() in LOGICAL_FAMILIES) return FontAvailability.AVAILABLE
        val resolved = measurer.toAwtFont(font).family
        if (resolved.equals(requested, ignoreCase = true)) return FontAvailability.AVAILABLE
        if (isFamilyAvailable(requested)) return FontAvailability.AVAILABLE
        return if (resolved.lowercase() in FALLBACK_FAMILIES) FontAvailability.MISSING else FontAvailability.SUBSTITUTED
    }

    /** Takes a [FontFingerprint] of [font] as the AWT text stack currently resolves it. */
    fun fingerprint(font: Font): FontFingerprint = FontFingerprint.of(measurer, font)

    /**
     * Returns a copy of [document] whose every block font carries a fresh [FontFingerprint]; run
     * before persisting so a later reopen can detect a missing or replaced font.
     *
     * @param overwrite when `false`, a font that already has a fingerprint keeps it.
     */
    fun stamp(document: Document, overwrite: Boolean = true): Document =
        document.withFontFingerprints(measurer, overwrite)

    /**
     * Whether [font] still resolves to the face [expected] was taken from, within [tolerance].
     *
     * @param expected a fingerprint previously produced by [fingerprint] on this toolkit.
     */
    fun verify(
        font: Font,
        expected: FontFingerprint,
        tolerance: Double = FontFingerprint.DEFAULT_TOLERANCE,
    ): Boolean = fingerprint(font).matches(expected, tolerance)

    private companion object {

        val LOGICAL_FAMILIES = setOf("dialog", "dialoginput", "serif", "sansserif", "monospaced", "default")
        val FALLBACK_FAMILIES = setOf("dialog", "default")
    }
}
