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

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * A compact, size-independent signature of a resolved font face.
 *
 * It is taken by measuring a fixed set of reference glyphs ([REFERENCE_GLYPHS]) at a fixed
 * normalization size ([NORMALIZED_SIZE]) through a [FontMeasureCalculator]. Two fingerprints taken
 * with the *same* text stack compare equal (within [matches]' tolerance) as long as the underlying
 * font file is unchanged; a missing or replaced family shifts the advances and the vertical metrics.
 *
 * Stored on a [Font] by the authoring side ([of], or a `*FontProbe` in a UI module), it lets the
 * measure pass flag - via `MeasuredFont.fingerprintStatus` - that a font was silently substituted or
 * updated on a machine that reopens the document. A fingerprint only compares against another one
 * taken with the same toolkit; AWT and JavaFX measure the same font differently.
 *
 * @property normalizedSize the size the reference glyphs were measured at.
 * @property ascent the ascent reported for the reference glyphs, at [normalizedSize].
 * @property descent the descent reported for the reference glyphs, at [normalizedSize].
 * @property advances the per-glyph advance widths of [REFERENCE_GLYPHS], in order.
 */
@Serializable
data class FontFingerprint(
    val normalizedSize: Double,
    val ascent: Double,
    val descent: Double,
    val advances: List<Double>,
) : PlatformSerializable {

    /**
     * Whether [other] describes the same font face as this one, allowing a per-value absolute
     * difference of [tolerance] to absorb the sub-pixel rounding of a text stack.
     *
     * Fingerprints of different lengths (a changed reference set) never match.
     */
    fun matches(other: FontFingerprint, tolerance: Double = DEFAULT_TOLERANCE): Boolean {
        if (advances.size != other.advances.size) return false
        if (abs(normalizedSize - other.normalizedSize) > tolerance) return false
        if (abs(ascent - other.ascent) > tolerance) return false
        if (abs(descent - other.descent) > tolerance) return false
        for (i in advances.indices) {
            if (abs(advances[i] - other.advances[i]) > tolerance) return false
        }
        return true
    }

    /**
     * Encodes this fingerprint into a single line of text for storage outside the model:
     * `v1|<size>|<ascent>|<descent>|<adv0>,<adv1>,...`.
     */
    fun encode(): String = buildString {
        append(FORMAT_TAG)
        append(SEP).append(normalizedSize)
        append(SEP).append(ascent)
        append(SEP).append(descent)
        append(SEP).append(advances.joinToString(LIST_SEP))
    }

    companion object {

        /**
         * Reference glyphs a fingerprint is taken from: the Latin alphabet, digits, common
         * diacritics and common punctuation, so advances, ascenders and descenders are all
         * represented.
         */
        const val REFERENCE_GLYPHS: String =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "0123456789" +
                "ÀÂÄÅÉÈÊËÇÑÖØÜ" +
                "àâäåéèêëçñöøü" +
                ".,;:!?'\"()[]{}<>-/\\|@#%&*+=~^"

        /** Size the reference glyphs are measured at, decoupling a fingerprint from the font size. */
        const val NORMALIZED_SIZE: Double = 100.0

        /**
         * Default per-value absolute tolerance for [matches]. Wide enough to absorb the sub-pixel
         * rounding a text stack applies between JDK builds, tight enough that a real family
         * substitution still stands out.
         */
        const val DEFAULT_TOLERANCE: Double = 0.5

        private const val FORMAT_TAG = "v1"
        private const val SEP = "|"
        private const val LIST_SEP = ","
        private const val QUANT_STEP = 1_000.0

        /**
         * Takes a [FontFingerprint] of [font] by measuring [REFERENCE_GLYPHS] at [NORMALIZED_SIZE]
         * through [measurer].
         *
         * Every reference glyph is measured on its own (via [FontMeasureCalculator.measureAdvances])
         * so a single changed glyph is visible; every value is quantized to three decimals to drop
         * noise below the fingerprint's resolution.
         */
        fun of(measurer: FontMeasureCalculator, font: Font): FontFingerprint {
            val probe = font.copy(size = NORMALIZED_SIZE, fingerprint = null)
            val whole = measurer.measure(probe, REFERENCE_GLYPHS)
            val advances = measurer.measureAdvances(probe, REFERENCE_GLYPHS).map { quantize(it) }
            return FontFingerprint(
                normalizedSize = NORMALIZED_SIZE,
                ascent = quantize(whole.ascent),
                descent = quantize(whole.descent),
                advances = advances,
            )
        }

        /**
         * Parses a string produced by [encode] back into a [FontFingerprint].
         *
         * @throws IllegalArgumentException if [text] is not a valid `v1` fingerprint line.
         */
        fun decode(text: String): FontFingerprint {
            val parts = text.trim().split(SEP)
            require(parts.size == 5 && parts[0] == FORMAT_TAG) { "not a v1 font fingerprint: '$text'" }
            require(parts[4].isNotEmpty()) { "font fingerprint has no advances: '$text'" }
            val advances = parts[4].split(LIST_SEP).map { token ->
                token.toDoubleOrNull() ?: throw IllegalArgumentException("invalid advance '$token' in '$text'")
            }
            return FontFingerprint(
                normalizedSize = parts[1].toDoubleOrNull() ?: parseError("size", text),
                ascent = parts[2].toDoubleOrNull() ?: parseError("ascent", text),
                descent = parts[3].toDoubleOrNull() ?: parseError("descent", text),
                advances = advances,
            )
        }

        private fun parseError(field: String, text: String): Nothing =
            throw IllegalArgumentException("invalid $field in font fingerprint '$text'")

        private fun quantize(value: Double): Double = (value * QUANT_STEP).roundToLong() / QUANT_STEP
    }
}
