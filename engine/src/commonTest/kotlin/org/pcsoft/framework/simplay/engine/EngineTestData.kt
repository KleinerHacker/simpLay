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

import org.pcsoft.framework.simplay.engine.internal.SimpLayFontEngine
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextStyle
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Shared fixtures for the measure-engine tests. All sample text is in English.
 *
 * The measurer is fully deterministic: width scales linearly with the character count, ascent and
 * descent scale with the font size. With [FONT_SIZE] `10.0` this yields a character width of `6.0`,
 * an ascent of `8.0`, a descent of `2.0` and, for a default [TextStyle], a resolved line height of
 * `10.0`.
 */
internal object EngineTestData {

    const val FONT_SIZE: Double = 10.0
    const val CHAR_WIDTH: Double = 6.0
    const val SPACE_WIDTH: Double = 6.0
    const val ASCENT: Double = 8.0
    const val DESCENT: Double = 2.0
    const val LINE_HEIGHT: Double = 10.0

    /** A plain test font. */
    val font = Font(family = "Test", size = FONT_SIZE)

    /** A left-aligned style over [font] with default line spacing. */
    val style = TextStyle(font = font)

    /** Deterministic font-measuring callback. */
    val measurer = FontMeasureCalculator { measuredFont, text ->
        TextMetrics(
            width = text.length * measuredFont.size * 0.6,
            ascent = measuredFont.size * 0.8,
            descent = measuredFont.size * 0.2,
        )
    }

    /** The [font] resolved through a fresh [SimpLayFontEngine]. */
    fun measuredFont(): MeasuredFont =
        SimpLayFontEngine.builder(measurer).build().resolveFont(font)

    /** The given [style] resolved through a fresh [SimpLayFontEngine]. */
    fun measuredStyle(style: TextStyle = EngineTestData.style): MeasuredTextStyle =
        SimpLayFontEngine.builder(measurer).build().resolveStyle(style)

    /** A style over [font] with the given [alignment] and default line spacing. */
    fun styleWith(alignment: TextAlignment): TextStyle =
        TextStyle(font = font, alignment = alignment)

    /** Tokenizes [text] with [style] and returns its raw parts. */
    fun parts(text: String, style: TextStyle = EngineTestData.style): List<TextPart> =
        TextBlock.of(text, style).parts

    /** A page frame with no margins, so the content area equals the page size. */
    fun pageLayout(width: Double = 200.0, height: Double = 300.0) = PageLayout(
        size = Size(width = width, height = height),
        margins = Margins(left = 0.0, top = 0.0, right = 0.0, bottom = 0.0),
    )
}
