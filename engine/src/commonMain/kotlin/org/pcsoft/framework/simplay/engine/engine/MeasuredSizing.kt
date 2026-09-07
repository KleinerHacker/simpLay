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

package org.pcsoft.framework.simplay.engine.engine

import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage

/**
 * The size a single measured [MeasuredPage] occupies; identical to [MeasuredPage.effectiveSize].
 *
 * Provided as a renderer-agnostic helper so every renderer computes a page box the same way.
 */
fun MeasuredPage.pageSize(): Size = effectiveSize

/**
 * The size of a whole [MeasuredDocument] when its pages are stacked vertically with [gap] layout
 * units between neighbouring pages.
 *
 * Width is the widest page, height is the sum of all page heights plus one [gap] per page boundary
 * (never before the first or after the last page). An empty document has size `0 x 0`.
 *
 * @param gap the vertical space inserted between two consecutive pages, in layout units.
 * @return the stacked document size.
 */
fun MeasuredDocument.documentSize(gap: Double): Size {
    if (pages.isEmpty()) return Size(0.0, 0.0)
    val width = pages.maxOf { it.effectiveSize.width }
    val height = pages.sumOf { it.effectiveSize.height } + gap * (pages.size - 1)
    return Size(width, height)
}
