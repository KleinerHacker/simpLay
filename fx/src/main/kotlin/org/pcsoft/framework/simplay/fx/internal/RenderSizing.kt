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

import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage

/**
 * The size of a single measured [page]; identical to [MeasuredPage.effectiveSize].
 */
internal fun pageSize(page: MeasuredPage): Size = page.effectiveSize

/**
 * The size of a whole [document] when its pages are stacked vertically with [gap] layout units
 * between neighbouring pages.
 *
 * Width is the widest page, height is the sum of all page heights plus one [gap] per page boundary
 * (never before the first or after the last page). An empty document has size `0 x 0`.
 */
internal fun documentSize(document: MeasuredDocument, gap: Double): Size {
    if (document.pages.isEmpty()) return Size(0.0, 0.0)
    val width = document.pages.maxOf { it.effectiveSize.width }
    val height = document.pages.sumOf { it.effectiveSize.height } + gap * (document.pages.size - 1)
    return Size(width, height)
}
