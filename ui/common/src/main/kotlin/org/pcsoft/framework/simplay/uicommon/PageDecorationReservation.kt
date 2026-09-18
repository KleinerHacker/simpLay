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

import org.pcsoft.framework.simplay.engine.geometry.Size
import kotlin.math.max

/**
 * The extra space (in unscaled layout points) that must be reserved on each [PageEdge] beyond the
 * page stack's own bounds, because at least one page decoration on that edge needs more room than
 * the existing `outerMargin`/`pageGap` band.
 */
data class EdgeReservation(
    val top: Double = 0.0,
    val bottom: Double = 0.0,
    val left: Double = 0.0,
    val right: Double = 0.0,
)

/**
 * Computes the per-edge [EdgeReservation] from a set of page decorations, given as triples of the
 * [PageEdge] the decoration is anchored to, its natural size in unscaled layout points, and whether
 * it opts into reservation at all. Decorations with `reserveSpace == false` are ignored entirely, so
 * a plain overlay decoration never grows the layout. For every edge, the reservation is the maximum
 * of the relevant dimension - height for [PageEdge.TOP]/[PageEdge.BOTTOM], width for
 * [PageEdge.LEFT]/[PageEdge.RIGHT] - across the decorations reserving space on that edge, `0.0` if
 * none do. Pure function, no JavaFX or Swing type is referenced here.
 */
fun computePageDecorationReservation(entries: List<Triple<PageEdge, Size, Boolean>>): EdgeReservation {
    val reserving = entries.filter { it.third }
    fun maxFor(edge: PageEdge, dimension: (Size) -> Double): Double =
        reserving.filter { it.first == edge }.maxOfOrNull { dimension(it.second) } ?: 0.0

    return EdgeReservation(
        top = maxFor(PageEdge.TOP) { it.height },
        bottom = maxFor(PageEdge.BOTTOM) { it.height },
        left = maxFor(PageEdge.LEFT) { it.width },
        right = maxFor(PageEdge.RIGHT) { it.width },
    )
}

/**
 * Grows the uniform [outerMargin] per edge to fit [reservation], never shrinking it and never adding
 * on top of it - the result is `max(outerMargin, reservation.<edge>)` for each edge.
 */
fun resolveReservedMargins(outerMargin: Double, reservation: EdgeReservation): EdgeReservation =
    EdgeReservation(
        top = max(outerMargin, reservation.top),
        bottom = max(outerMargin, reservation.bottom),
        left = max(outerMargin, reservation.left),
        right = max(outerMargin, reservation.right),
    )
