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

/**
 * How a page decoration is positioned along the [PageEdge] it is anchored to.
 */
enum class EdgeAlignment {

    /** Flush with the start of the edge (the left end of a horizontal edge, the top end of a
     * vertical edge), unaffected by the decoration's extent along the edge. */
    START,

    /** Centered along the edge. */
    CENTER,

    /** Flush with the end of the edge (the right end of a horizontal edge, the bottom end of a
     * vertical edge). */
    END,

    /** Stretched to cover the full length of the edge; the decoration's own extent along the edge
     * is ignored in favor of the page edge's length. */
    STRETCH,
}

/**
 * Where a single page decoration of a `PaperSheetView` (fx and swing) is anchored, in the
 * component area around a page (inside `outerMargin`/`pageGap`), not in the page content itself.
 * That band is no longer a fixed `outerMargin`: when the decoration opts into reservation, it grows
 * via [resolveReservedMargins]/[EdgeReservation] to fit the decoration's natural size.
 *
 * @property edge the page edge the decoration is attached to.
 * @property alignment how the decoration is positioned along [edge]; [EdgeAlignment.STRETCH] by
 * default, covering the full edge length.
 * @property offsetX additional shift in layout points along the horizontal axis - along the edge
 * for [PageEdge.TOP]/[PageEdge.BOTTOM], away from the page for [PageEdge.LEFT]/[PageEdge.RIGHT].
 * @property offsetY additional shift in layout points along the vertical axis - away from the page
 * for [PageEdge.TOP]/[PageEdge.BOTTOM], along the edge for [PageEdge.LEFT]/[PageEdge.RIGHT].
 */
data class PageDecorationPlacement(
    val edge: PageEdge,
    val alignment: EdgeAlignment = EdgeAlignment.STRETCH,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
)
