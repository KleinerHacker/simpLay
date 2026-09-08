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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Bounds
import javafx.geometry.Dimension2D
import javafx.scene.control.Control
import javafx.scene.control.Skin
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * A scrollable and zoomable view that renders a [Document] as physical-looking sheets - each with a
 * border and a drop shadow - stacked vertically. Text can be selected with the mouse and copied to
 * the system clipboard with `Ctrl+C` as styled HTML, RTF and plain text.
 *
 * The [mode] switches between [PaperSheetMode.READONLY] (no caret, exactly the read-only behaviour)
 * and [PaperSheetMode.EDITABLE], which adds a blinking caret, character insertion / removal, clipboard
 * cut / copy / paste (`Ctrl+X` / `Ctrl+C` / `Ctrl+V`), line duplication (`Ctrl+D`), drag-and-drop of
 * the selection and the standard caret-navigation keys (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`,
 * arrows, `Ctrl+Left` / `Ctrl+Right`, `Backspace`, `Delete`, each optionally with `Shift`). Editing
 * replaces [document] with a new instance; the previous document is not mutated.
 *
 * The only input is [document]. Layout is controlled by [outerMargin] (space around the sheet stack)
 * and [pageGap] (space between two sheets). [zoom] scales the whole view and is always kept within
 * `[minZoom, maxZoom]`; assigning a value outside that range, or narrowing the range, clamps it. The
 * read-only [contentSize] reports the laid-out size; the selection is exposed through
 * [selectionModel] (with [selectedText] and [selectionBounds] as convenience delegates) and the
 * caret through [caretModel].
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

    //region Mode

    /** The [mode] property, for binding and change listeners. */
    @get:JvmName("modeProperty")
    val modeProperty: ObjectProperty<PaperSheetMode> =
        SimpleObjectProperty(this, "mode", PaperSheetMode.READONLY)

    /** Whether the view only shows text or also edits it; defaults to [PaperSheetMode.READONLY]. */
    var mode: PaperSheetMode
        get() = modeProperty.get()
        set(value) {
            modeProperty.set(value)
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

    //region Caret

    private val caretModelInstance = CaretModel(this)

    /** The [caretModel] property, read-only; the same instance for the whole life of the view. */
    @get:JvmName("caretModelProperty")
    val caretModelProperty: ReadOnlyObjectProperty<CaretModel> =
        ReadOnlyObjectWrapper(this, "caretModel", caretModelInstance).readOnlyProperty

    /**
     * The caret model: the caret [CaretModel.position], its viewport [CaretModel.bounds], the blink
     * state and the linear, absolute-structural and relative-structural move commands.
     */
    val caretModel: CaretModel get() = caretModelInstance

    /** The [smoothCaretBlink] property, for binding and change listeners. */
    @get:JvmName("smoothCaretBlinkProperty")
    val smoothCaretBlinkProperty: javafx.beans.property.BooleanProperty =
        javafx.beans.property.SimpleBooleanProperty(this, "smoothCaretBlink", false)

    /**
     * When `true`, the caret fades in and out instead of blinking hard on and off. Off by default.
     * Only takes effect in [PaperSheetMode.EDITABLE].
     */
    var smoothCaretBlink: Boolean
        get() = smoothCaretBlinkProperty.get()
        set(value) = smoothCaretBlinkProperty.set(value)

    /** Sink for the [caretModel] commands, implemented and registered by the skin. */
    internal interface CaretCommands {
        fun moveTo(index: Int)
        fun moveToStart()
        fun moveToEnd()
        fun moveIntoBlock(block: Int, index: Int)
        fun moveToStartOfBlock(block: Int)
        fun moveToEndOfBlock(block: Int)
        fun moveIntoWord(word: Int, index: Int)
        fun moveToStartOfWord(word: Int)
        fun moveToEndOfWord(word: Int)
        fun moveToSymbol(symbol: Int)
        fun moveToStartOfSymbol(symbol: Int)
        fun moveToEndOfSymbol(symbol: Int)
        fun moveToNextWord()
        fun moveToPrevWord()
        fun moveToNextBlock()
        fun moveToPrevBlock()
        fun moveToNextSymbol()
        fun moveToPrevSymbol()
    }

    private var caretCommands: CaretCommands? = null
    private var pendingCaretCommand: (CaretCommands.() -> Unit)? = null

    internal fun registerCaretCommands(commands: CaretCommands) {
        caretCommands = commands
        pendingCaretCommand?.let { pending ->
            pendingCaretCommand = null
            commands.pending()
        }
    }

    internal fun unregisterCaretCommands(commands: CaretCommands) {
        if (caretCommands === commands) caretCommands = null
    }

    internal fun requestCaret(block: CaretCommands.() -> Unit) {
        val commands = caretCommands
        if (commands != null) commands.block() else pendingCaretCommand = block
    }

    //endregion

    //region Floating overlays

    private val floatingOverlaysList: ObservableList<FloatingOverlay> = FXCollections.observableArrayList()

    /**
     * The registered floating overlays: caller-supplied nodes the view shows, positions and hides on
     * its own when their [FloatingOverlay.trigger] holds. Mutable; also populated from FXML as a
     * `<floatingOverlays>` child element. The same list instance for the whole life of the view.
     */
    val floatingOverlays: ObservableList<FloatingOverlay> get() = floatingOverlaysList

    private val hoveredParagraphWrapper = ReadOnlyIntegerWrapper(this, "hoveredParagraph", -1)
    private val hoveredParagraphBoundsWrapper = ReadOnlyObjectWrapper<Bounds?>(this, "hoveredParagraphBounds", null)
    private val hoveredPageWrapper = ReadOnlyIntegerWrapper(this, "hoveredPage", -1)
    private val hoveredPageBoundsWrapper = ReadOnlyObjectWrapper<Bounds?>(this, "hoveredPageBounds", null)

    /** The [hoveredParagraph] property, read-only. */
    @get:JvmName("hoveredParagraphProperty")
    val hoveredParagraphProperty: ReadOnlyIntegerProperty get() = hoveredParagraphWrapper.readOnlyProperty

    /** Zero-based ordinal of the paragraph (measured block) under the mouse, or `-1` when none. */
    val hoveredParagraph: Int get() = hoveredParagraphWrapper.get()

    /** The [hoveredParagraphBounds] property, read-only. */
    @get:JvmName("hoveredParagraphBoundsProperty")
    val hoveredParagraphBoundsProperty: ReadOnlyObjectProperty<Bounds?>
        get() = hoveredParagraphBoundsWrapper.readOnlyProperty

    /** Box of the hovered paragraph in viewport pixels (follows scroll and zoom), or `null`. */
    val hoveredParagraphBounds: Bounds? get() = hoveredParagraphBoundsWrapper.get()

    /** The [hoveredPage] property, read-only. */
    @get:JvmName("hoveredPageProperty")
    val hoveredPageProperty: ReadOnlyIntegerProperty get() = hoveredPageWrapper.readOnlyProperty

    /** Zero-based index of the sheet under the mouse, or `-1` when none. */
    val hoveredPage: Int get() = hoveredPageWrapper.get()

    /** The [hoveredPageBounds] property, read-only. */
    @get:JvmName("hoveredPageBoundsProperty")
    val hoveredPageBoundsProperty: ReadOnlyObjectProperty<Bounds?>
        get() = hoveredPageBoundsWrapper.readOnlyProperty

    /** Box of the hovered sheet in viewport pixels (follows scroll and zoom), or `null`. */
    val hoveredPageBounds: Bounds? get() = hoveredPageBoundsWrapper.get()

    internal fun updateHoveredParagraph(index: Int, bounds: Bounds?) {
        hoveredParagraphWrapper.set(index)
        hoveredParagraphBoundsWrapper.set(bounds)
    }

    internal fun updateHoveredPage(index: Int, bounds: Bounds?) {
        hoveredPageWrapper.set(index)
        hoveredPageBoundsWrapper.set(bounds)
    }

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
