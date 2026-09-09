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

import java.awt.Rectangle
import java.util.EventObject

/**
 * Passed to the [FloatingOverlay.onShown] and [FloatingOverlay.onHidden] listeners when the owning
 * [PaperSheetView] shows or hides an overlay. Carries the same context the read-only overlay fields
 * hold at that moment. The Swing counterpart of the `fx` module's `FloatingOverlayEvent`; it extends
 * [EventObject] instead of a JavaFX `Event`.
 *
 * @property overlay the overlay that was shown or hidden (also the [EventObject.getSource]).
 * @property triggerKind the trigger that drives [overlay].
 * @property triggerBounds the anchor box in viewport pixels at the time of the event, or `null` on hide.
 * @property index trigger-specific index: selection start, paragraph ordinal, page index or caret
 *   index; `-1` when not applicable.
 * @property text the trigger text: the selected text or the hovered paragraph text; `""` otherwise.
 * @property documentRange the covered character range for [FloatingOverlayTrigger.SELECTION] /
 *   [FloatingOverlayTrigger.CARET], else `null`.
 * @property type whether the overlay was [Type.SHOWN] or [Type.HIDDEN].
 */
class FloatingOverlayEvent internal constructor(
    val overlay: FloatingOverlay,
    val triggerKind: FloatingOverlayTrigger,
    val triggerBounds: Rectangle?,
    val index: Int,
    val text: String,
    val documentRange: IntRange?,
    val type: Type,
) : EventObject(overlay) {

    enum class Type { SHOWN, HIDDEN }
}
