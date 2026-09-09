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

package org.pcsoft.framework.simplay.swing.internal

import java.awt.font.FontRenderContext
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight
import java.awt.Font as AwtFont

/**
 * A [FontMeasureCalculator] backed by the AWT text stack, the Swing counterpart of the `fx` module's
 * `FxFontMeasureCalculator`.
 *
 * Measurement uses a shared, screen-independent [FontRenderContext] (antialiased, fractional
 * metrics): the advance width is [AwtFont.getStringBounds], the ascent and descent come from
 * [AwtFont.getLineMetrics]. Results are cached per `(Font, text)` pair, which keeps a lookup
 * deterministic as required by [FontMeasureCalculator].
 *
 * No `sun.*` API is used; the calculator works headlessly.
 */
internal open class SwingFontMeasureCalculator : FontMeasureCalculator {

    private val frc = FontRenderContext(null, true, true)
    private val cache = HashMap<CacheKey, TextMetrics>()
    private val fontCache = HashMap<Font, AwtFont>()

    override fun measure(font: Font, text: String): TextMetrics =
        cache.getOrPut(CacheKey(font, text)) {
            val awt = toAwtFont(font)
            val width = awt.getStringBounds(text.ifEmpty { "" }, frc).width
            val lm = awt.getLineMetrics(text.ifEmpty { " " }, frc)
            TextMetrics(
                width = if (text.isEmpty()) 0.0 else width,
                ascent = lm.ascent.toDouble(),
                descent = (lm.descent + lm.leading).toDouble(),
            )
        }

    /** Maps an engine [Font] onto the matching cached AWT [AwtFont]. */
    internal fun toAwtFont(font: Font): AwtFont = fontCache.getOrPut(font) {
        var style = AwtFont.PLAIN
        if (font.weight == FontWeight.BOLD) style = style or AwtFont.BOLD
        if (font.style == FontStyle.ITALIC) style = style or AwtFont.ITALIC
        AwtFont(font.family, style, font.size.toInt().coerceAtLeast(1)).deriveFont(font.size.toFloat())
    }

    private data class CacheKey(val font: Font, val text: String)
}
