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

package org.pcsoft.framework.simplay.uicommon

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * A deterministic, platform-free [FontMeasureCalculator] for the `ui-common` tests: every glyph is
 * `0.6 * fontSize` wide, the ascent is `0.8 * fontSize` and the descent `0.2 * fontSize`. It lets the
 * text index, the editor, the hit test and the span helper be exercised without an AWT or JavaFX
 * text stack.
 */
class StubFontMeasureCalculator : FontMeasureCalculator {

    override fun measure(font: Font, text: String): TextMetrics =
        TextMetrics(
            width = text.length * font.size * GLYPH_WIDTH_FACTOR,
            ascent = font.size * ASCENT_FACTOR,
            descent = font.size * DESCENT_FACTOR,
        )

    private companion object {
        const val GLYPH_WIDTH_FACTOR = 0.6
        const val ASCENT_FACTOR = 0.8
        const val DESCENT_FACTOR = 0.2
    }
}
