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

package org.pcsoft.framework.simplay.fx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Pos
import javafx.scene.Node

/**
 * A caller-supplied node that a [PaperSheetView] shows, positions and hides on its own when a
 * [trigger] condition holds - a copy bar above the text selection, a label above the hovered
 * paragraph, and so on. Register instances through [PaperSheetView.getFloatingOverlays].
 *
 * The bean has a no-argument constructor and only JavaFX properties, so it can be declared in FXML:
 *
 * ```xml
 * <PaperSheetView>
 *   <floatingOverlays>
 *     <FloatingOverlay fx:id="copyBar" trigger="SELECTION" anchor="TOP_LEFT" offsetY="-6.0">
 *       <content>
 *         <Button text="Copy" onAction="#copySelection"/>
 *       </content>
 *     </FloatingOverlay>
 *   </floatingOverlays>
 * </PaperSheetView>
 * ```
 *
 * While the trigger holds, the read-only [active], [activeBounds], [activeIndex], [activeText] and
 * [activeDocumentRange] fields carry the current context and can be bound from FXML with
 * `${copyBar.activeText}` and friends. [onShown] and [onHidden] fire on the show / hide transitions
 * with the same context in a [FloatingOverlayEvent].
 *
 * Every property follows the JavaFX bean convention: the property object through `xxxProperty()`, the
 * value through `getXxx()` / `setXxx()` (or `isXxx()` for booleans).
 */
class FloatingOverlay {

    //region Content

    /** The [content] property, for binding and change listeners. */
    @get:JvmName("contentProperty")
    val contentProperty: ObjectProperty<Node?> = SimpleObjectProperty(this, "content", null)

    /** The node to place into the view while the overlay is active; `null` shows nothing. */
    var content: Node?
        get() = contentProperty.get()
        set(value) = contentProperty.set(value)

    //endregion

    //region Trigger

    /** The [trigger] property, for binding and change listeners. */
    @get:JvmName("triggerProperty")
    val triggerProperty: ObjectProperty<FloatingOverlayTrigger> =
        SimpleObjectProperty(this, "trigger", FloatingOverlayTrigger.SELECTION)

    /** The condition that shows this overlay; defaults to [FloatingOverlayTrigger.SELECTION]. */
    var trigger: FloatingOverlayTrigger
        get() = triggerProperty.get()
        set(value) = triggerProperty.set(value)

    //endregion

    //region Placement

    /** The [anchor] property, for binding and change listeners. */
    @get:JvmName("anchorProperty")
    val anchorProperty: ObjectProperty<Pos> = SimpleObjectProperty(this, "anchor", Pos.TOP_LEFT)

    /**
     * Where the overlay sits relative to the trigger box. The horizontal part aligns the node's left
     * edge (`LEFT`), centre (`CENTER`) or right edge (`RIGHT`) to the box; the vertical part places
     * the node fully above the box (`TOP`), centred on it (`CENTER`) or fully below it
     * (`BOTTOM` / `BASELINE`). [offsetX] / [offsetY] are added afterwards. Defaults to [Pos.TOP_LEFT].
     */
    var anchor: Pos
        get() = anchorProperty.get()
        set(value) = anchorProperty.set(value)

    /** The [offsetX] property, for binding and change listeners. */
    @get:JvmName("offsetXProperty")
    val offsetXProperty: DoubleProperty = SimpleDoubleProperty(this, "offsetX", 0.0)

    /** Extra horizontal shift in pixels applied after [anchor]. */
    var offsetX: Double
        get() = offsetXProperty.get()
        set(value) = offsetXProperty.set(value)

    /** The [offsetY] property, for binding and change listeners. */
    @get:JvmName("offsetYProperty")
    val offsetYProperty: DoubleProperty = SimpleDoubleProperty(this, "offsetY", 0.0)

    /** Extra vertical shift in pixels applied after [anchor]. */
    var offsetY: Double
        get() = offsetYProperty.get()
        set(value) = offsetYProperty.set(value)

    /** The [autoHide] property, for binding and change listeners. */
    @get:JvmName("autoHideProperty")
    val autoHideProperty: BooleanProperty = SimpleBooleanProperty(this, "autoHide", true)

    /** When `true` (default), the overlay is removed as soon as its trigger stops holding. */
    var autoHide: Boolean
        get() = autoHideProperty.get()
        set(value) = autoHideProperty.set(value)

    //endregion

    //region Active state (read-only, written by the view)

    private val activeWrapper = ReadOnlyBooleanWrapper(this, "active", false)
    private val activeBoundsWrapper = ReadOnlyObjectWrapper<Bounds?>(this, "activeBounds", null)
    private val activeIndexWrapper = ReadOnlyIntegerWrapper(this, "activeIndex", -1)
    private val activeTextWrapper = ReadOnlyStringWrapper(this, "activeText", "")
    private val activeDocumentRangeWrapper = ReadOnlyObjectWrapper<IntRange?>(this, "activeDocumentRange", null)

    /** The [active] property, read-only. */
    @get:JvmName("activeProperty")
    val activeProperty: ReadOnlyBooleanProperty get() = activeWrapper.readOnlyProperty

    /** `true` while the overlay's node is currently placed in the view. */
    val isActive: Boolean get() = activeWrapper.get()

    /** The [activeBounds] property, read-only. */
    @get:JvmName("activeBoundsProperty")
    val activeBoundsProperty: ReadOnlyObjectProperty<Bounds?> get() = activeBoundsWrapper.readOnlyProperty

    /** The trigger box in viewport pixels while [isActive], else `null`. */
    val activeBounds: Bounds? get() = activeBoundsWrapper.get()

    /** The [activeIndex] property, read-only. */
    @get:JvmName("activeIndexProperty")
    val activeIndexProperty: ReadOnlyIntegerProperty get() = activeIndexWrapper.readOnlyProperty

    /**
     * Trigger-specific index while [isActive]: selection start index, paragraph ordinal, page index
     * or caret index; `-1` when not applicable.
     */
    val activeIndex: Int get() = activeIndexWrapper.get()

    /** The [activeText] property, read-only. */
    @get:JvmName("activeTextProperty")
    val activeTextProperty: ReadOnlyStringProperty get() = activeTextWrapper.readOnlyProperty

    /** The selected text or the hovered paragraph text while [isActive]; `""` otherwise. */
    val activeText: String get() = activeTextWrapper.get()

    /** The [activeDocumentRange] property, read-only. */
    @get:JvmName("activeDocumentRangeProperty")
    val activeDocumentRangeProperty: ReadOnlyObjectProperty<IntRange?>
        get() = activeDocumentRangeWrapper.readOnlyProperty

    /** The covered character range for [FloatingOverlayTrigger.SELECTION] while [isActive], else `null`. */
    val activeDocumentRange: IntRange? get() = activeDocumentRangeWrapper.get()

    //endregion

    //region Events

    /** The [onShown] property, for binding and change listeners. */
    @get:JvmName("onShownProperty")
    val onShownProperty: ObjectProperty<EventHandler<FloatingOverlayEvent>?> =
        SimpleObjectProperty(this, "onShown", null)

    /** Handler invoked right after the overlay's node was added to the view. */
    var onShown: EventHandler<FloatingOverlayEvent>?
        get() = onShownProperty.get()
        set(value) = onShownProperty.set(value)

    /** The [onHidden] property, for binding and change listeners. */
    @get:JvmName("onHiddenProperty")
    val onHiddenProperty: ObjectProperty<EventHandler<FloatingOverlayEvent>?> =
        SimpleObjectProperty(this, "onHidden", null)

    /** Handler invoked right after the overlay's node was removed from the view. */
    var onHidden: EventHandler<FloatingOverlayEvent>?
        get() = onHiddenProperty.get()
        set(value) = onHiddenProperty.set(value)

    //endregion

    //region View-side updates

    /** Replaces the active-state fields; called by the skin on every refresh while the trigger holds. */
    internal fun updateActiveState(bounds: Bounds?, index: Int, text: String, documentRange: IntRange?) {
        activeWrapper.set(true)
        activeBoundsWrapper.set(bounds)
        activeIndexWrapper.set(index)
        activeTextWrapper.set(text)
        activeDocumentRangeWrapper.set(documentRange)
    }

    /** Clears the active-state fields; called by the skin when the overlay is hidden. */
    internal fun clearActiveState() {
        activeWrapper.set(false)
        activeBoundsWrapper.set(null)
        activeIndexWrapper.set(-1)
        activeTextWrapper.set("")
        activeDocumentRangeWrapper.set(null)
    }

    internal fun fireShown(kind: FloatingOverlayTrigger) {
        onShown?.handle(
            FloatingOverlayEvent(
                this, kind, activeBounds, activeIndex, activeText, activeDocumentRange, FloatingOverlayEvent.SHOWN,
            ),
        )
    }

    internal fun fireHidden(kind: FloatingOverlayTrigger) {
        onHidden?.handle(
            FloatingOverlayEvent(this, kind, null, -1, "", null, FloatingOverlayEvent.HIDDEN),
        )
    }

    //endregion
}
