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
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

/** A listener for the show / hide transitions of a [FloatingOverlay]. */
fun interface FloatingOverlayListener {
    fun handle(event: FloatingOverlayEvent)
}

/**
 * A caller-supplied component that a [PaperSheetView] shows, positions and hides on its own when a
 * [trigger] condition holds - a copy bar above the text selection, a label above the hovered
 * paragraph, and so on. Register instances through [PaperSheetView.floatingOverlays]. The Swing
 * counterpart of the `fx` module's `FloatingOverlay`; the programmatic API is kept, the FXML support
 * is dropped and [content] is a [JComponent] instead of a JavaFX `Node`.
 *
 * While the trigger holds, the read-only [isActive], [activeBounds], [activeIndex], [activeText],
 * [activeDocumentRange] and [isActivePageDeactivated] fields carry the current context; [onShown] and
 * [onHidden] fire on the show / hide transitions with the same context in a [FloatingOverlayEvent]. A
 * `PropertyChangeEvent` under [PROP_ACTIVE] is fired whenever [isActive] flips.
 */
class FloatingOverlay {

    private val pcs = PropertyChangeSupport(this)

    //region Content / trigger / placement

    /** The component to place into the view while the overlay is active; `null` shows nothing. */
    var content: JComponent? = null

    /** The condition that shows this overlay; defaults to [FloatingOverlayTrigger.SELECTION]. */
    var trigger: FloatingOverlayTrigger = FloatingOverlayTrigger.SELECTION

    /** Where the overlay sits relative to the trigger box; defaults to [OverlayAnchor.TOP_LEFT]. */
    var anchor: OverlayAnchor = OverlayAnchor.TOP_LEFT

    /** Extra horizontal shift in pixels applied after [anchor]. */
    var offsetX: Double = 0.0

    /** Extra vertical shift in pixels applied after [anchor]. */
    var offsetY: Double = 0.0

    /** When `true` (default), the overlay is removed as soon as its trigger stops holding. */
    var autoHide: Boolean = true

    //endregion

    //region Active state (read-only, written by the view)

    var isActive: Boolean = false
        private set

    var activeBounds: Rectangle? = null
        private set

    var activeIndex: Int = -1
        private set

    var activeText: String = ""
        private set

    var activeDocumentRange: IntRange? = null
        private set

    var isActivePageDeactivated: Boolean = false
        private set

    //endregion

    //region Events

    var onShown: FloatingOverlayListener? = null
    var onHidden: FloatingOverlayListener? = null

    fun addPropertyChangeListener(listener: PropertyChangeListener) = pcs.addPropertyChangeListener(listener)

    fun removePropertyChangeListener(listener: PropertyChangeListener) = pcs.removePropertyChangeListener(listener)

    //endregion

    //region View-side updates

    internal fun updateActiveState(
        bounds: Rectangle?,
        index: Int,
        text: String,
        documentRange: IntRange?,
        pageDeactivated: Boolean,
    ) {
        val wasActive = isActive
        isActive = true
        activeBounds = bounds
        activeIndex = index
        activeText = text
        activeDocumentRange = documentRange
        isActivePageDeactivated = pageDeactivated
        if (!wasActive) pcs.firePropertyChange(PROP_ACTIVE, false, true)
    }

    internal fun clearActiveState() {
        val wasActive = isActive
        isActive = false
        activeBounds = null
        activeIndex = -1
        activeText = ""
        activeDocumentRange = null
        isActivePageDeactivated = false
        if (wasActive) pcs.firePropertyChange(PROP_ACTIVE, true, false)
    }

    internal fun fireShown(kind: FloatingOverlayTrigger) {
        onShown?.handle(
            FloatingOverlayEvent(
                this, kind, activeBounds, activeIndex, activeText, activeDocumentRange,
                isActivePageDeactivated, FloatingOverlayEvent.Type.SHOWN,
            ),
        )
    }

    internal fun fireHidden(kind: FloatingOverlayTrigger) {
        onHidden?.handle(
            FloatingOverlayEvent(this, kind, null, -1, "", null, false, FloatingOverlayEvent.Type.HIDDEN),
        )
    }

    //endregion

    companion object {
        const val PROP_ACTIVE = "active"
    }
}
