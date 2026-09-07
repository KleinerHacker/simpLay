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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Bounds
import javafx.geometry.Dimension2D
import javafx.scene.control.Control
import javafx.scene.control.Skin
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * A read-only, scrollable and zoomable view that renders a [Document] as physical-looking sheets -
 * each with a border and a drop shadow - stacked vertically. Text can be selected with the mouse and
 * copied to the system clipboard with `Ctrl+C` as styled HTML, RTF and plain text. There is no
 * caret; editing is added by a later implementation plan on this same class.
 *
 * The only input is [document]. Layout is controlled by [outerMargin] (space around the sheet stack)
 * and [pageGap] (space between two sheets). [zoom] scales the whole view and is always kept within
 * `[minZoom, maxZoom]`; assigning a value outside that range, or narrowing the range, clamps it. The
 * read-only [contentSize] reports the laid-out size; the selection is exposed through [selectionModel]
 * (with [selectedText] and [selectionBounds] as convenience delegates).
 *
 * Every property follows the JavaFX bean convention: the property object is exposed through a
 * `xxxProperty()` accessor (the Kotlin property is named `xxxProperty`, its JVM getter renamed with
 * [JvmName]) and the plain value through `getXxx()` / `setXxx()`.
 */
class PaperSheetView : Control() {

    //region Document

    /** The [document] property, for binding and change listeners. */
    @get:JvmName("documentProperty")
    val documentProperty: ObjectProperty<Document?> = SimpleObjectProperty(this, "document", null)

    /** The document to render, or `null` for an empty view. */
    var document: Document?
        get() = documentProperty.get()
        set(value) {
            documentProperty.set(value)
        }

    //endregion

    //region Layout

    /** The [outerMargin] property, for binding and change listeners. */
    @get:JvmName("outerMarginProperty")
    val outerMarginProperty: DoubleProperty = SimpleDoubleProperty(this, "outerMargin", DEFAULT_OUTER_MARGIN)

    /** Space in layout units kept around the whole sheet stack. */
    var outerMargin: Double
        get() = outerMarginProperty.get()
        set(value) {
            outerMarginProperty.set(value)
        }

    /** The [pageGap] property, for binding and change listeners. */
    @get:JvmName("pageGapProperty")
    val pageGapProperty: DoubleProperty = SimpleDoubleProperty(this, "pageGap", DEFAULT_PAGE_GAP)

    /** Vertical space in layout units between two consecutive sheets. */
    var pageGap: Double
        get() = pageGapProperty.get()
        set(value) {
            pageGapProperty.set(value)
        }

    //endregion

    //region Zoom

    /** The [minZoom] property, for binding and change listeners. */
    @get:JvmName("minZoomProperty")
    val minZoomProperty: DoubleProperty = SimpleDoubleProperty(this, "minZoom", DEFAULT_MIN_ZOOM)

    /** Lower bound for [zoom]; changing it re-clamps [zoom]. */
    var minZoom: Double
        get() = minZoomProperty.get()
        set(value) {
            minZoomProperty.set(value)
        }

    /** The [maxZoom] property, for binding and change listeners. */
    @get:JvmName("maxZoomProperty")
    val maxZoomProperty: DoubleProperty = SimpleDoubleProperty(this, "maxZoom", DEFAULT_MAX_ZOOM)

    /** Upper bound for [zoom]; changing it re-clamps [zoom]. */
    var maxZoom: Double
        get() = maxZoomProperty.get()
        set(value) {
            maxZoomProperty.set(value)
        }

    /** The [zoom] property, for binding and change listeners. */
    @get:JvmName("zoomProperty")
    val zoomProperty: DoubleProperty = SimpleDoubleProperty(this, "zoom", DEFAULT_ZOOM)

    /** Current scale factor for the whole view, always kept within `[minZoom, maxZoom]`. */
    var zoom: Double
        get() = zoomProperty.get()
        set(value) {
            zoomProperty.set(value)
        }

    private var clamping = false

    private fun clampZoom() {
        if (clamping) return
        val lo = minZoomProperty.get()
        val hi = maxZoomProperty.get().coerceAtLeast(lo)
        val current = zoomProperty.get()
        val clamped = current.coerceIn(lo, hi)
        if (clamped != current) {
            clamping = true
            zoomProperty.set(clamped)
            clamping = false
        }
    }

    //endregion

    //region Content size

    private val contentSizeWrapper = ReadOnlyObjectWrapper(this, "contentSize", Dimension2D(0.0, 0.0))

    /** The [contentSize] property, read-only. */
    @get:JvmName("contentSizeProperty")
    val contentSizeProperty: ReadOnlyObjectProperty<Dimension2D>
        get() = contentSizeWrapper.readOnlyProperty

    /** The unscaled size of the whole sheet stack including [outerMargin] on every side. */
    val contentSize: Dimension2D get() = contentSizeWrapper.get()

    internal fun updateContentSize(size: Dimension2D) = contentSizeWrapper.set(size)

    //endregion

    //region Selection

    private val selectionModelInstance = TextSelectionModel(this)

    /** The [selectionModel] property, read-only; the same instance for the whole life of the view. */
    @get:JvmName("selectionModelProperty")
    val selectionModelProperty: ReadOnlyObjectProperty<TextSelectionModel> =
        ReadOnlyObjectWrapper(this, "selectionModel", selectionModelInstance).readOnlyProperty

    /**
     * The selection model: selected text, character range, viewport bounds and styled runs, plus the
     * `selectRange` / `selectAll` / `clearSelection` commands.
     */
    val selectionModel: TextSelectionModel get() = selectionModelInstance

    /** The [selectedText] property (delegates to [selectionModel]). */
    @get:JvmName("selectedTextProperty")
    val selectedTextProperty: ReadOnlyStringProperty
        get() = selectionModelInstance.textProperty

    /** The currently selected text as plain text; empty when nothing is selected. */
    val selectedText: String get() = selectionModelInstance.text

    /** The [selectionBounds] property (delegates to [selectionModel]). */
    @get:JvmName("selectionBoundsProperty")
    val selectionBoundsProperty: ReadOnlyObjectProperty<Bounds?>
        get() = selectionModelInstance.boundsProperty

    /** The bounding box of the current selection in viewport pixels, or `null` when empty. */
    val selectionBounds: Bounds? get() = selectionModelInstance.bounds

    /** Sink for the [selectionModel] commands, implemented and registered by the skin. */
    internal interface SelectionCommands {
        fun selectRange(start: Int, end: Int)
        fun selectAll()
        fun clearSelection()
    }

    private var selectionCommands: SelectionCommands? = null
    private var pendingSelectionCommand: (SelectionCommands.() -> Unit)? = null

    internal fun registerSelectionCommands(commands: SelectionCommands) {
        selectionCommands = commands
        pendingSelectionCommand?.let { pending ->
            pendingSelectionCommand = null
            commands.pending()
        }
    }

    internal fun unregisterSelectionCommands(commands: SelectionCommands) {
        if (selectionCommands === commands) selectionCommands = null
    }

    private fun runSelectionCommand(block: SelectionCommands.() -> Unit) {
        val commands = selectionCommands
        if (commands != null) commands.block() else pendingSelectionCommand = block
    }

    internal fun requestSelectRange(start: Int, end: Int) = runSelectionCommand { selectRange(start, end) }

    internal fun requestSelectAll() = runSelectionCommand { selectAll() }

    internal fun requestClearSelection() = runSelectionCommand { clearSelection() }

    //endregion

    //region Wiring

    init {
        styleClass.add(DEFAULT_STYLE_CLASS)
        isFocusTraversable = true
        zoomProperty.addListener { _, _, _ -> clampZoom() }
        minZoomProperty.addListener { _, _, _ -> clampZoom() }
        maxZoomProperty.addListener { _, _, _ -> clampZoom() }
    }

    override fun createDefaultSkin(): Skin<*> = PaperSheetViewSkin(this)

    //endregion

    companion object {

        /** Style class added to every instance. */
        const val DEFAULT_STYLE_CLASS = "paper-sheet-view"

        const val DEFAULT_OUTER_MARGIN = 24.0
        const val DEFAULT_PAGE_GAP = 16.0
        const val DEFAULT_MIN_ZOOM = 0.25
        const val DEFAULT_MAX_ZOOM = 4.0
        const val DEFAULT_ZOOM = 1.0
    }
}
