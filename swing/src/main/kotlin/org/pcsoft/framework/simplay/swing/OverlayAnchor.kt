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

package org.pcsoft.framework.simplay.swing

/**
 * Where a [FloatingOverlay] sits relative to its trigger box, the Swing replacement for JavaFX's
 * `Pos`. The [hpos] part aligns the component's left edge ([Horizontal.LEFT]), centre
 * ([Horizontal.CENTER]) or right edge ([Horizontal.RIGHT]) to the box; the [vpos] part places the
 * component fully above the box ([Vertical.TOP]), centred on it ([Vertical.CENTER]) or fully below
 * it ([Vertical.BOTTOM]). The overlay offsets are added afterwards.
 */
enum class OverlayAnchor(val hpos: Horizontal, val vpos: Vertical) {
    TOP_LEFT(Horizontal.LEFT, Vertical.TOP),
    TOP_CENTER(Horizontal.CENTER, Vertical.TOP),
    TOP_RIGHT(Horizontal.RIGHT, Vertical.TOP),
    CENTER_LEFT(Horizontal.LEFT, Vertical.CENTER),
    CENTER(Horizontal.CENTER, Vertical.CENTER),
    CENTER_RIGHT(Horizontal.RIGHT, Vertical.CENTER),
    BOTTOM_LEFT(Horizontal.LEFT, Vertical.BOTTOM),
    BOTTOM_CENTER(Horizontal.CENTER, Vertical.BOTTOM),
    BOTTOM_RIGHT(Horizontal.RIGHT, Vertical.BOTTOM);

    enum class Horizontal { LEFT, CENTER, RIGHT }

    enum class Vertical { TOP, CENTER, BOTTOM }
}
