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

package org.pcsoft.framework.simplay.engine.internal

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.FontMetrics
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextStyle
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Measure stage that resolves [Font] and [TextStyle] into their measured counterparts.
 *
 * One of the internal `SimpLay*Engine` stages driven by
 * [org.pcsoft.framework.simplay.engine.SimpLayEngine]; created through [builder]. The
 * vertical metrics of a font come from a single [FontMeasureCalculator] call over
 * [REFERENCE_GLYPHS], a fixed string covering the Latin alphabet, digits, common diacritics and
 * common punctuation, so ascenders, descenders and accents are all represented. Resolved fonts are
 * cached for the lifetime of the instance.
 */
internal class SimpLayFontEngine private constructor(
    private val measurer: FontMeasureCalculator,
) {

    private val fontCache = mutableMapOf<Font, MeasuredFont>()

    fun resolveFont(font: Font): MeasuredFont = fontCache.getOrPut(font) {
        val metrics = measurer.measure(font, REFERENCE_GLYPHS)
        MeasuredFont(
            raw = font,
            metrics = FontMetrics(
                ascent = metrics.ascent,
                descent = metrics.descent,
                leading = 0.0,
            ),
        )
    }

    fun resolveStyle(style: TextStyle): MeasuredTextStyle =
        MeasuredTextStyle(raw = style, font = resolveFont(style.font))

    /** Collects the parts of a [SimpLayFontEngine] and creates it. Obtained from [builder]. */
    class Builder internal constructor(private val measurer: FontMeasureCalculator) {

        /** Builds the font engine. */
        fun build(): SimpLayFontEngine = SimpLayFontEngine(measurer)
    }

    companion object {

        /** Reference glyphs used to derive a font's ascent and descent. */
        const val REFERENCE_GLYPHS: String =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789" +
                    "ÅÄÖØåäöø" +
                    "ÀÂÉÈÊËÇàâéèêëç" +
                    "ÑÁÍÓÚñáíóú" +
                    "ĄĆĘŁŃŚŹŻ" +
                    "ąćęłńśźż" +
                    "ČŠŽčšžĂÎăî" +
                    ".,;:!?'\"()[]{}<>-/\\|@#%&*+=~^"

        /** Starts a [Builder] with the mandatory [measurer]. */
        fun builder(measurer: FontMeasureCalculator): Builder = Builder(measurer)
    }
}
