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

package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.model.LineSpacing
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * A raw [TextStyle] with its font replaced by a [MeasuredFont] and the [resolvedLineHeight]
 * derived.
 *
 * The unchanged properties (`lineSpacing`, `alignment`) are forwarded to [raw] by hand. The font
 * is exposed only as the measured [font]; the raw one stays reachable via `raw.font`. Not
 * persistable.
 *
 * @property raw the wrapped raw style.
 * @property font the measured font of this style.
 */
class MeasuredTextStyle(
    val raw: TextStyle,
    val font: MeasuredFont,
) {
    /** The line spacing of [raw]. */
    val lineSpacing: LineSpacing get() = raw.lineSpacing

    /** The horizontal alignment of [raw]. */
    val alignment: TextAlignment get() = raw.alignment

    /**
     * The line advance in layout units, derived from [font]'s metrics and [lineSpacing]:
     * `(ascent + descent) * lineSpacing.factor + lineSpacing.extraLeading`.
     */
    val resolvedLineHeight: Double
        get() = (font.metrics.ascent + font.metrics.descent) * lineSpacing.factor + lineSpacing.extraLeading
}
