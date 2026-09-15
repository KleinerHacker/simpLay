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

package org.pcsoft.framework.simplay.fx.demo

import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition

/**
 * Human-readable labels for [PageNumberPosition], shared by every demo tab that lets the page
 * number position be picked from a combo box.
 */
internal object PageNumberPositions {

    /** Every [PageNumberPosition] label, in declaration order, ready for a combo box's items. */
    val labels: List<String> = PageNumberPosition.entries.map { labelOf(it) }

    /** The label shown for [position]. */
    fun labelOf(position: PageNumberPosition): String = when (position) {
        PageNumberPosition.OFF -> "Off"
        PageNumberPosition.TOP_LEFT -> "Top Left"
        PageNumberPosition.TOP_CENTER -> "Top Center"
        PageNumberPosition.TOP_RIGHT -> "Top Right"
        PageNumberPosition.TOP_INNER -> "Top Inner"
        PageNumberPosition.TOP_OUTER -> "Top Outer"
        PageNumberPosition.BOTTOM_LEFT -> "Bottom Left"
        PageNumberPosition.BOTTOM_CENTER -> "Bottom Center"
        PageNumberPosition.BOTTOM_RIGHT -> "Bottom Right"
        PageNumberPosition.BOTTOM_INNER -> "Bottom Inner"
        PageNumberPosition.BOTTOM_OUTER -> "Bottom Outer"
    }

    /** The [PageNumberPosition] whose [labelOf] equals [label]. */
    fun positionOf(label: String): PageNumberPosition = PageNumberPosition.entries.first { labelOf(it) == label }
}

/** Returns a copy of this document with its numbering position overridden to [position]. */
internal fun Document.withPageNumberPosition(position: PageNumberPosition): Document =
    copy(numbering = numbering.copy(position = position))
