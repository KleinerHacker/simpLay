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

import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * Callback that measures a piece of text in a given [Font].
 *
 * The engine never talks to a platform text stack itself; the caller supplies this callback so the
 * same document can be measured against AWT, Skia, a headless stub or any other backend. A given
 * ([Font], text) pair must always yield the same [TextMetrics] so the result stays deterministic.
 */
fun interface FontMeasureCalculator {

    /**
     * Returns the [TextMetrics] of [text] rendered in [font].
     *
     * @param font the font the text would be rendered in.
     * @param text the exact string to measure; may be a single glyph, a word or a whole reference
     *   string.
     * @return the advance width plus the ascent and descent of [text] in [font], as unit-less
     *   doubles.
     */
    fun measure(font: Font, text: String): TextMetrics
}
