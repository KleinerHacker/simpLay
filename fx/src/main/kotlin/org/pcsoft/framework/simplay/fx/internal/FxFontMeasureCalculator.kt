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

import javafx.geometry.Bounds
import javafx.scene.text.Font as FxFont
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight as FxFontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight

/**
 * A [FontMeasureCalculator] backed by the JavaFX text stack.
 *
 * Measurement uses a throwaway [javafx.scene.text.Text] node with logical bounds: the advance width
 * is the node width, the ascent is [Text.getBaselineOffset] and the descent is the remaining line
 * height. Results are cached per `(Font, text)` pair, which also keeps a lookup deterministic.
 *
 * The JavaFX toolkit must already be initialised when [measure] is first called (an
 * [javafx.application.Application] is running, or a headless toolkit such as Monocle was started for
 * tests). No internal `com.sun.*` API is used.
 */
internal open class FxFontMeasureCalculator : FontMeasureCalculator {

    private val cache = HashMap<CacheKey, TextMetrics>()
    private val fontCache = HashMap<Font, FxFont>()

    override fun measure(font: Font, text: String): TextMetrics =
        cache.getOrPut(CacheKey(font, text)) {
            val node = Text(text).apply {
                this.font = toFxFont(font)
                boundsType = TextBoundsType.LOGICAL
            }
            val bounds: Bounds = node.layoutBounds
            val ascent = node.baselineOffset
            val descent = (bounds.height - ascent).coerceAtLeast(0.0)
            TextMetrics(width = bounds.width, ascent = ascent, descent = descent)
        }

    /** Maps an engine [Font] onto the matching cached JavaFX [FxFont]. */
    internal fun toFxFont(font: Font): FxFont = fontCache.getOrPut(font) {
        FxFont.font(
            font.family,
            if (font.weight == FontWeight.BOLD) FxFontWeight.BOLD else FxFontWeight.NORMAL,
            if (font.style == FontStyle.ITALIC) FontPosture.ITALIC else FontPosture.REGULAR,
            font.size,
        )
    }

    private data class CacheKey(val font: Font, val text: String)
}
