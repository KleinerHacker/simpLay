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

import kotlin.math.max
import kotlin.math.min

/**
 * The `[x0, x1]` horizontal span, in page content-area coordinates, that the character range
 * `[lo, hi)` covers inside [segment]: `x0` is the left edge of the first covered glyph, `x1` the
 * right edge of the last, both clamped to the segment's own part bounds. [measurer] measures the
 * prefix widths. Shared by the selection highlight and the selection bounding box.
 */
internal fun segmentSpanX(
    segment: DocumentTextIndex.Segment,
    lo: Int,
    hi: Int,
    measurer: FxFontMeasureCalculator = FxFontMeasureCalculator(),
): Pair<Double, Double> {
    val text = segment.part.text
    val startOff = (max(lo, segment.start) - segment.start).coerceIn(0, text.length)
    val endOff = (min(hi, segment.end) - segment.start).coerceIn(0, text.length)
    val x0 = segment.part.bounds.x +
        if (startOff <= 0) 0.0 else measurer.measure(segment.font, text.substring(0, startOff)).width
    val x1 = segment.part.bounds.x +
        if (endOff >= text.length) {
            segment.part.bounds.width
        } else {
            measurer.measure(segment.font, text.substring(0, endOff)).width
        }
    return x0 to x1
}
