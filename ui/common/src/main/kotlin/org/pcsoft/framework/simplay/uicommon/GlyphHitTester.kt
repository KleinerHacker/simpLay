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

import kotlin.math.abs
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart

/**
 * Maps a horizontal position onto a character offset inside a measured text part.
 *
 * [xInContentArea] is an x coordinate in the page content-area coordinate system (the same frame as
 * [MeasuredTextPart.bounds]). [font] is the raw font of the owning block, [measurer] measures the
 * text prefixes. The result is in `0..part.text.length`: `0` at or left of the part start,
 * `text.length` at or right of the part end, otherwise the offset whose prefix width is closest to
 * the position.
 */
fun hitTest(
    part: MeasuredTextPart,
    font: Font,
    xInContentArea: Double,
    measurer: FontMeasureCalculator,
): Int {
    val text = part.text
    val localX = xInContentArea - part.bounds.x
    if (localX <= 0.0) return 0
    if (localX >= part.bounds.width) return text.length

    var bestOffset = 0
    var bestDistance = Double.MAX_VALUE
    for (offset in 0..text.length) {
        val prefixWidth = if (offset == 0) 0.0 else measurer.measure(font, text.substring(0, offset)).width
        val distance = abs(prefixWidth - localX)
        if (distance < bestDistance) {
            bestDistance = distance
            bestOffset = offset
        }
    }
    return bestOffset
}
