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

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size

/**
 * Resolves the viewport-coordinate target rectangle of a single page decoration of a
 * `PaperSheetView` (fx and swing), shared by both toolkit implementations so the anchoring and
 * scaling behavior stays identical.
 *
 * [pageBounds] is the page's own already zoomed and scrolled rectangle in viewport pixels.
 * [decorationSize] is the decoration node's own size in viewport pixels, unaffected by [zoom].
 * [placement.offsetX]/[placement.offsetY] are given in the engine's point-based layout unit and are
 * scaled to viewport pixels via [effectiveZoom] before being applied.
 *
 * The decoration is placed just outside [pageBounds] on [PageDecorationPlacement.edge], flush
 * against the page along the edge's own axis and shifted further away from the page by the
 * perpendicular offset component. Along the edge, [PageDecorationPlacement.alignment] positions the
 * decoration at the edge's start, center or end, or - the default,
 * [EdgeAlignment.STRETCH] - stretches it to the full length of the edge, in which case
 * [decorationSize]'s extent along the edge is ignored in favor of the edge's own length.
 *
 * No JavaFX or Swing type is referenced here; this function is pure and toolkit-agnostic.
 */
fun resolveDecorationBounds(
    pageBounds: Rect,
    placement: PageDecorationPlacement,
    decorationSize: Size,
    zoom: Double,
): Rect {
    val scale = effectiveZoom(zoom)
    val scaledOffsetX = placement.offsetX * scale
    val scaledOffsetY = placement.offsetY * scale

    return when (placement.edge) {
        PageEdge.TOP, PageEdge.BOTTOM -> {
            val width = if (placement.alignment == EdgeAlignment.STRETCH) pageBounds.width else decorationSize.width
            val height = decorationSize.height
            val alignedX = alongEdgeStart(pageBounds.x, pageBounds.width, width, placement.alignment)
            val x = alignedX + scaledOffsetX
            val y = if (placement.edge == PageEdge.TOP) {
                pageBounds.y - height - scaledOffsetY
            } else {
                pageBounds.y + pageBounds.height + scaledOffsetY
            }
            Rect(x = x, y = y, width = width, height = height)
        }

        PageEdge.LEFT, PageEdge.RIGHT -> {
            val height = if (placement.alignment == EdgeAlignment.STRETCH) pageBounds.height else decorationSize.height
            val width = decorationSize.width
            val alignedY = alongEdgeStart(pageBounds.y, pageBounds.height, height, placement.alignment)
            val y = alignedY + scaledOffsetY
            val x = if (placement.edge == PageEdge.LEFT) {
                pageBounds.x - width - scaledOffsetX
            } else {
                pageBounds.x + pageBounds.width + scaledOffsetX
            }
            Rect(x = x, y = y, width = width, height = height)
        }
    }
}

/** The along-edge start coordinate of a decoration of [extent] on an edge of [edgeExtent] starting
 * at [edgeStart], per [alignment]. */
private fun alongEdgeStart(edgeStart: Double, edgeExtent: Double, extent: Double, alignment: EdgeAlignment): Double =
    when (alignment) {
        EdgeAlignment.STRETCH, EdgeAlignment.START -> edgeStart
        EdgeAlignment.CENTER -> edgeStart + (edgeExtent - extent) / 2.0
        EdgeAlignment.END -> edgeStart + edgeExtent - extent
    }
