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
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.css.CssMetaData
import javafx.css.PseudoClass
import javafx.css.SimpleStyleableDoubleProperty
import javafx.css.SimpleStyleableObjectProperty
import javafx.css.Styleable
import javafx.css.StyleableDoubleProperty
import javafx.css.StyleableObjectProperty
import javafx.geometry.Bounds
import javafx.geometry.Dimension2D
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetStyleableProperties
import org.pcsoft.framework.simplay.uicommon.PageMode

/**
 * A scrollable and zoomable view that renders a [Document] as physical-looking sheets - each with a
 * border and a drop shadow - stacked vertically. Text can be selected with the mouse and copied to
 * the system clipboard with `Ctrl+C` as styled HTML, RTF and plain text.
 *
 * The [mode] picks one of four interaction levels: [PaperSheetMode.STATIC] (a plain picture - no
 * selection, no caret, the default arrow cursor and no keyboard focus), [PaperSheetMode.SELECTABLE]
 * (selecting and copying, no caret), [PaperSheetMode.NAVIGABLE] (adds a blinking caret and the
 * standard caret-navigation keys, still without mutating the document) and [PaperSheetMode.EDITABLE]
 * (adds character insertion / removal, clipboard cut / copy / paste (`Ctrl+X` / `Ctrl+C` / `Ctrl+V`),
 * line duplication (`Ctrl+D`) and drag-and-drop of the selection). Editing replaces [document] with a
 * new instance; the previous document is not mutated.
 *
 * [pageModes] (keyed by the stable [org.pcsoft.framework.simplay.engine.model.Page.id]) overrides
 * [mode] for individual pages: a page absent from the map, or mapped to `null`, simply follows [mode];
 * a page mapped to a [PageMode] uses that mode instead, independent of [mode] - both more restrictive
 * (a read-only page inside an editable view) and more permissive (an editable page inside a static
 * view) are possible. Purely transient view state, never persisted in [document] and cleared back to
 * empty whenever [document] is reloaded from outside - an edit (which also replaces [document] with a
 * new instance) leaves it untouched, since the page ids it is keyed by do not change. Use
 * [setPageMode] to override a page by its current index - it resolves the index against [document]
 * into an id immediately, so the override stays attached to that page even as later edits shift
 * indices.
 *
 * The only input is [document]. Layout is controlled by [outerMargin] (space around the sheet stack)
 * and [pageGap] (space between two sheets). [zoom] scales the whole view and is always kept within
 * `[minZoom, maxZoom]`; assigning a value outside that range, or narrowing the range, clamps it. The
 * read-only [contentSize] reports the laid-out size; the selection is exposed through
 * [selectionModel] (with [selectedText] and [selectionBounds] as convenience delegates) and the
 * caret through [caretModel].
 *
 * The sheet chrome, the drop shadow, the selection highlight, the caret and the two layout values are
 * styleable through the standard JavaFX CSS mechanism. The style class is `paper-sheet-view`, and
 * exactly one of the pseudo-classes `:static`, `:selectable`, `:navigable` and `:editable` is active,
 * matching the current [mode] (the inherited `:focused` pseudo-class works as usual);
 * [getUserAgentStylesheet] ships the default look. The
 * `-fx-` properties are `-fx-sheet-background`, `-fx-sheet-border-color`, `-fx-sheet-border-width`,
 * `-fx-shadow-color`, `-fx-shadow-offset`, `-fx-selection-color`, `-fx-caret-color`,
 * `-fx-deactivated-sheet-background`, `-fx-deactivated-overlay-color`, `-fx-outer-margin` and
 * `-fx-page-gap`. Every colour value is a [Paint] (a gradient works too) except `-fx-caret-color`,
 * which is a plain [Color]. A programmatic setter still wins over the user-agent stylesheet.
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
        SimpleObjectProperty(this, "mode", PaperSheetMode.SELECTABLE)

    /** How much interaction the view offers; defaults to [PaperSheetMode.SELECTABLE]. */
    var mode: PaperSheetMode
        get() = modeProperty.get()
        set(value) {
            modeProperty.set(value)
        }

    //endregion

    //region Page mode

    /** The [pageModes] property, for binding and change listeners. */
    @get:JvmName("pageModesProperty")
    val pageModesProperty: ObjectProperty<Map<String, PageMode>> =
        SimpleObjectProperty(this, "pageModes", emptyMap())

    /**
     * Per-page [PageMode] overrides, keyed by the stable
     * [org.pcsoft.framework.simplay.engine.model.Page.id]. A page absent from the map follows [mode].
     * Purely transient view state, never persisted in [document]; reset to empty whenever [document]
     * is replaced with a different instance. Ids no longer present in [document] are simply ignored.
     */
    var pageModes: Map<String, PageMode>
        get() = pageModesProperty.get()
        set(value) {
            pageModesProperty.set(value)
        }

    /**
     * Overrides (or clears, for `mode == null`) the [PageMode] of the page currently at [index] of
     * [document]. Resolves [index] against the current [document] into that page's stable id
     * immediately, so the override stays attached to the same page even as later edits shift page
     * indices. A no-op without a [document] or for an out-of-range [index].
     */
    fun setPageMode(index: Int, mode: PageMode?) {
        val id = document?.pages?.getOrNull(index)?.id ?: return
        pageModes = if (mode != null) pageModes + (id to mode) else pageModes - id
    }

    /** The effective [PageMode] of page [pageId]: its [pageModes] override, or [mode] otherwise. */
    fun effectivePageMode(pageId: String): PageMode = pageModes[pageId] ?: mode.asPageMode()

    /** Whether any page (via [mode] or a [pageModes] override) currently supports selection. */
    internal val anySelection: Boolean
        get() = mode.supportsSelection || pageModes.values.any { it.supportsSelection }

    /** Whether any page (via [mode] or a [pageModes] override) currently supports the caret. */
    internal val anyCaret: Boolean
        get() = mode.supportsCaret || pageModes.values.any { it.supportsCaret }

    /** Whether any page (via [mode] or a [pageModes] override) currently supports editing. */
    internal val anyEditing: Boolean
        get() = mode.supportsEditing || pageModes.values.any { it.supportsEditing }

    /** Whether the view should take keyboard focus: [mode] does, or a [pageModes] override needs it. */
    internal val anyFocus: Boolean
        get() = mode.supportsFocus || anySelection || anyCaret || anyEditing

    //endregion

    //region Layout

    /** The [outerMargin] property, for binding and change listeners; styleable as `-fx-outer-margin`. */
    @get:JvmName("outerMarginProperty")
    val outerMarginProperty: StyleableDoubleProperty =
        SimpleStyleableDoubleProperty(
            PaperSheetStyleableProperties.OUTER_MARGIN, this, "outerMargin", DEFAULT_OUTER_MARGIN,
        )

    /** Space in layout units kept around the whole sheet stack. */
    var outerMargin: Double
        get() = outerMarginProperty.get()
        set(value) {
            outerMarginProperty.set(value)
        }

    /** The [pageGap] property, for binding and change listeners; styleable as `-fx-page-gap`. */
    @get:JvmName("pageGapProperty")
    val pageGapProperty: StyleableDoubleProperty =
        SimpleStyleableDoubleProperty(
            PaperSheetStyleableProperties.PAGE_GAP, this, "pageGap", DEFAULT_PAGE_GAP,
        )

    /** Vertical space in layout units between two consecutive sheets. */
    var pageGap: Double
        get() = pageGapProperty.get()
        set(value) {
            pageGapProperty.set(value)
        }

    //endregion

    //region Styling

    /** The [sheetBackground] property; styleable as `-fx-sheet-background`. */
    @get:JvmName("sheetBackgroundProperty")
    val sheetBackgroundProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.SHEET_BACKGROUND, this, "sheetBackground",
            PaperSheetStyleableProperties.DEFAULT_SHEET_BACKGROUND,
        )

    /** Fill of every sheet; a [Paint], so a gradient or image pattern works. */
    var sheetBackground: Paint
        get() = sheetBackgroundProperty.get()
        set(value) {
            sheetBackgroundProperty.set(value)
        }

    /** The [sheetBorderColor] property; styleable as `-fx-sheet-border-color`. */
    @get:JvmName("sheetBorderColorProperty")
    val sheetBorderColorProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.SHEET_BORDER_COLOR, this, "sheetBorderColor",
            PaperSheetStyleableProperties.DEFAULT_SHEET_BORDER_COLOR,
        )

    /** Stroke colour of every sheet border; a [Paint]. */
    var sheetBorderColor: Paint
        get() = sheetBorderColorProperty.get()
        set(value) {
            sheetBorderColorProperty.set(value)
        }

    /** The [sheetBorderWidth] property; styleable as `-fx-sheet-border-width`. */
    @get:JvmName("sheetBorderWidthProperty")
    val sheetBorderWidthProperty: StyleableDoubleProperty =
        SimpleStyleableDoubleProperty(
            PaperSheetStyleableProperties.SHEET_BORDER_WIDTH, this, "sheetBorderWidth",
            PaperSheetStyleableProperties.DEFAULT_SHEET_BORDER_WIDTH,
        )

    /** Stroke width of every sheet border, in layout units. */
    var sheetBorderWidth: Double
        get() = sheetBorderWidthProperty.get()
        set(value) {
            sheetBorderWidthProperty.set(value)
        }

    /** The [shadowColor] property; styleable as `-fx-shadow-color`. */
    @get:JvmName("shadowColorProperty")
    val shadowColorProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.SHADOW_COLOR, this, "shadowColor",
            PaperSheetStyleableProperties.DEFAULT_SHADOW_COLOR,
        )

    /** Fill of the drop shadow behind every sheet; a [Paint]. */
    var shadowColor: Paint
        get() = shadowColorProperty.get()
        set(value) {
            shadowColorProperty.set(value)
        }

    /** The [shadowOffset] property; styleable as `-fx-shadow-offset`. */
    @get:JvmName("shadowOffsetProperty")
    val shadowOffsetProperty: StyleableDoubleProperty =
        SimpleStyleableDoubleProperty(
            PaperSheetStyleableProperties.SHADOW_OFFSET, this, "shadowOffset",
            PaperSheetStyleableProperties.DEFAULT_SHADOW_OFFSET,
        )

    /** Offset of the drop shadow to the lower right of every sheet, in layout units. */
    var shadowOffset: Double
        get() = shadowOffsetProperty.get()
        set(value) {
            shadowOffsetProperty.set(value)
        }

    /** The [selectionColor] property; styleable as `-fx-selection-color`. */
    @get:JvmName("selectionColorProperty")
    val selectionColorProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.SELECTION_COLOR, this, "selectionColor",
            PaperSheetStyleableProperties.DEFAULT_SELECTION_COLOR,
        )

    /** Fill of the text selection highlight; a [Paint], usually semi-transparent. */
    var selectionColor: Paint
        get() = selectionColorProperty.get()
        set(value) {
            selectionColorProperty.set(value)
        }

    /** The [caretColor] property; styleable as `-fx-caret-color`. */
    @get:JvmName("caretColorProperty")
    val caretColorProperty: StyleableObjectProperty<Color> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.CARET_COLOR, this, "caretColor",
            PaperSheetStyleableProperties.DEFAULT_CARET_COLOR,
        )

    /** Stroke colour of the edit caret; a plain [Color]. */
    var caretColor: Color
        get() = caretColorProperty.get()
        set(value) {
            caretColorProperty.set(value)
        }

    /** The [deactivatedSheetBackground] property; styleable as `-fx-deactivated-sheet-background`. */
    @get:JvmName("deactivatedSheetBackgroundProperty")
    val deactivatedSheetBackgroundProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.DEACTIVATED_SHEET_BACKGROUND, this, "deactivatedSheetBackground",
            PaperSheetStyleableProperties.DEFAULT_DEACTIVATED_SHEET_BACKGROUND,
        )

    /** Fill of a [PageMode.DISABLED] sheet, instead of [sheetBackground]. */
    var deactivatedSheetBackground: Paint
        get() = deactivatedSheetBackgroundProperty.get()
        set(value) {
            deactivatedSheetBackgroundProperty.set(value)
        }

    /** The [deactivatedOverlayColor] property; styleable as `-fx-deactivated-overlay-color`. */
    @get:JvmName("deactivatedOverlayColorProperty")
    val deactivatedOverlayColorProperty: StyleableObjectProperty<Paint> =
        SimpleStyleableObjectProperty(
            PaperSheetStyleableProperties.DEACTIVATED_OVERLAY_COLOR, this, "deactivatedOverlayColor",
            PaperSheetStyleableProperties.DEFAULT_DEACTIVATED_OVERLAY_COLOR,
        )

    /** Colour of the diagonal hatch drawn over a [PageMode.DISABLED] sheet. */
    var deactivatedOverlayColor: Paint
        get() = deactivatedOverlayColorProperty.get()
        set(value) {
            deactivatedOverlayColorProperty.set(value)
        }

    override fun getControlCssMetaData(): MutableList<CssMetaData<out Styleable, *>> =
        ArrayList(PaperSheetStyleableProperties.CLASS_CSS_META_DATA)

    override fun getUserAgentStylesheet(): String = USER_AGENT_STYLESHEET

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

    internal fun requestSelectRange(start: Int, end: Int) {
        if (!anySelection) return
        runSelectionCommand { selectRange(start, end) }
    }

    internal fun requestSelectAll() {
        if (!anySelection) return
        runSelectionCommand { selectAll() }
    }

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
    val smoothCaretBlinkProperty: BooleanProperty =
        SimpleBooleanProperty(this, "smoothCaretBlink", false)

    /**
     * When `true`, the caret fades in and out instead of blinking hard on and off. Off by default.
     * Only takes effect in a [mode] with [PaperSheetMode.supportsCaret].
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
        fun moveToNextPage()
        fun moveToPrevPage()
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
        if (!anyCaret) return
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
        zoomProperty.addListener { _, _, _ -> clampZoom() }
        minZoomProperty.addListener { _, _, _ -> clampZoom() }
        maxZoomProperty.addListener { _, _, _ -> clampZoom() }

        applyMode(mode)
        modeProperty.addListener { _, _, value -> applyMode(value) }
    }

    private fun applyMode(value: PaperSheetMode) {
        isFocusTraversable = value.supportsFocus
        if (!value.supportsFocus && isFocused) parent?.requestFocus()
        for ((candidate, pseudoClass) in MODE_PSEUDO_CLASSES) {
            pseudoClassStateChanged(pseudoClass, candidate == value)
        }
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

        /** The pseudo-class activated for each [PaperSheetMode]; exactly one is active at a time. */
        private val MODE_PSEUDO_CLASSES: Map<PaperSheetMode, PseudoClass> =
            PaperSheetMode.entries.associateWith { PseudoClass.getPseudoClass(it.name.lowercase()) }

        private val USER_AGENT_STYLESHEET: String =
            PaperSheetView::class.java.getResource("paper-sheet-view.css")!!.toExternalForm()
    }
}
