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

package org.pcsoft.framework.simplay.fx.control

import javafx.event.Event
import javafx.event.EventType
import javafx.geometry.Bounds

/**
 * Passed to the [FloatingOverlay.getOnShown] and [FloatingOverlay.getOnHidden] handlers when the
 * owning [PaperSheetView] shows or hides an overlay. Carries the same context the read-only overlay
 * fields hold at that moment.
 *
 * @property overlay the overlay that was shown or hidden.
 * @property triggerKind the trigger that drives [overlay].
 * @property triggerBounds the anchor box in viewport pixels at the time of the event, or `null` on hide.
 * @property index trigger-specific index: selection start, paragraph ordinal, page index or caret
 *   index; `-1` when not applicable.
 * @property text the trigger text: the selected text or the hovered paragraph text; `""` otherwise.
 * @property documentRange the covered character range for [FloatingOverlayTrigger.SELECTION], else `null`.
 */
class FloatingOverlayEvent internal constructor(
    val overlay: FloatingOverlay,
    val triggerKind: FloatingOverlayTrigger,
    val triggerBounds: Bounds?,
    val index: Int,
    val text: String,
    val documentRange: IntRange?,
    eventType: EventType<FloatingOverlayEvent>,
) : Event(eventType) {

    companion object {

        /** Common supertype of every [FloatingOverlayEvent] type. */
        @JvmField
        val ANY: EventType<FloatingOverlayEvent> = EventType(Event.ANY, "FLOATING_OVERLAY")

        /** Fired right after an overlay's node has been added to the view. */
        @JvmField
        val SHOWN: EventType<FloatingOverlayEvent> = EventType(ANY, "FLOATING_OVERLAY_SHOWN")

        /** Fired right after an overlay's node has been removed from the view. */
        @JvmField
        val HIDDEN: EventType<FloatingOverlayEvent> = EventType(ANY, "FLOATING_OVERLAY_HIDDEN")
    }
}
