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

import java.awt.Color
import java.awt.Dimension
import java.awt.Paint
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.UIManager
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetStyle
import org.pcsoft.framework.simplay.uicommon.PageMode

/**
 * A scrollable and zoomable Swing component that renders a [Document] as physical-looking sheets -
 * each with a border and a drop shadow - stacked vertically. Text can be selected with the mouse and
 * copied to the system clipboard with `Ctrl+C` (`Cmd+C` on macOS) as styled HTML, RTF and plain
 * text. The Swing counterpart of the `fx` module's `PaperSheetView`.
 *
 * The [mode] picks one of four interaction levels: [PaperSheetMode.STATIC] (a plain picture - no
 * selection, no caret, the default arrow cursor and no keyboard focus), [PaperSheetMode.SELECTABLE]
 * (selecting and copying, no caret), [PaperSheetMode.NAVIGABLE] (adds a blinking caret and the
 * standard caret-navigation keys, still without mutating the document) and [PaperSheetMode.EDITABLE]
 * (adds character insertion / removal, clipboard cut / copy / paste, line duplication and
 * drag-and-drop of the selection). Editing replaces [document] with a new instance; the previous
 * document is not mutated.
 *
 * [pageModes] (keyed by the stable [org.pcsoft.framework.simplay.engine.model.Page.id]) overrides
 * [mode] for individual pages: a page absent from the map, or mapped to `null`, follows [mode]; a page
 * mapped to a [PageMode] uses that mode instead, independent of [mode]. Purely transient view state,
 * never persisted in [document] and reset to empty whenever [document] is reloaded from outside - an
 * edit (which also replaces [document] with a new instance) leaves it untouched, since the page ids
 * it is keyed by do not change. Use [setPageMode] to override a page by its current index.
 *
 * Layout is controlled by [outerMargin] (space around the sheet stack) and [pageGap] (space between
 * two sheets). [zoom] scales the whole view and is always kept within `[minZoom, maxZoom]`. The
 * read-only [contentSize] reports the laid-out size; the selection is exposed through
 * [selectionModel] (with [selectedText] and [selectionBounds] as convenience delegates) and the
 * caret through [caretModel].
 *
 * Every mutable property fires a `java.beans.PropertyChangeEvent` under the matching `PROP_*` name.
 * The visual and layout values (sheet chrome, drop shadow, selection highlight, caret, outer margin,
 * page gap) are also read from the `PaperSheetView.*` keys of the active Look-and-Feel via
 * [PaperSheetLookAndFeel] unless set programmatically; the pluggable delegate type is [PaperSheetUI],
 * the default [BasicPaperSheetUI].
 */
open class PaperSheetView : JComponent() {

    /** Names of the style / layout properties a caller assigned explicitly; the L&F leaves those alone. */
    internal val styleSetByUser: MutableSet<String> = HashSet()

    private var applyingLaf = false

    private fun markSet(name: String) {
        if (!applyingLaf) styleSetByUser.add(name)
    }

    /** Runs [block] while property assignments count as Look-and-Feel defaults, not explicit sets. */
    internal fun applyLafDefaults(block: () -> Unit) {
        applyingLaf = true
        try {
            block()
        } finally {
            applyingLaf = false
        }
    }

    //region Document

    var document: Document? = null
        set(value) {
            val old = field
            field = value
            firePropertyChange(PROP_DOCUMENT, old, value)
        }

    //endregion

    //region Mode

    /** How much interaction the view offers; defaults to [PaperSheetMode.SELECTABLE]. */
    var mode: PaperSheetMode = PaperSheetMode.SELECTABLE
        set(value) {
            val old = field
            field = value
            isFocusable = anyFocus
            firePropertyChange(PROP_MODE, old, value)
        }

    //endregion

    //region Page mode

    /**
     * Per-page [PageMode] overrides, keyed by the stable
     * [org.pcsoft.framework.simplay.engine.model.Page.id]. A page absent from the map follows [mode].
     * Purely transient view state, never persisted in [document]; reset to empty whenever [document]
     * is reloaded from outside (not by an edit). Ids no longer present in [document] are simply ignored.
     */
    var pageModes: Map<String, PageMode> = emptyMap()
        set(value) {
            val old = field
            field = value
            isFocusable = anyFocus
            firePropertyChange(PROP_PAGE_MODES, old, value)
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

    var outerMargin: Double = PaperSheetStyle.DEFAULT_OUTER_MARGIN
        set(value) {
            val old = field
            field = value
            markSet(PROP_OUTER_MARGIN)
            firePropertyChange(PROP_OUTER_MARGIN, old, value)
        }

    var pageGap: Double = PaperSheetStyle.DEFAULT_PAGE_GAP
        set(value) {
            val old = field
            field = value
            markSet(PROP_PAGE_GAP)
            firePropertyChange(PROP_PAGE_GAP, old, value)
        }

    //endregion

    //region Zoom

    var minZoom: Double = DEFAULT_MIN_ZOOM
        set(value) {
            val old = field
            field = value
            firePropertyChange(PROP_MIN_ZOOM, old, value)
            clampZoom()
        }

    var maxZoom: Double = DEFAULT_MAX_ZOOM
        set(value) {
            val old = field
            field = value
            firePropertyChange(PROP_MAX_ZOOM, old, value)
            clampZoom()
        }

    var zoom: Double = DEFAULT_ZOOM
        set(value) {
            val old = field
            field = value
            firePropertyChange(PROP_ZOOM, old, value)
            clampZoom()
        }

    private var clamping = false

    private fun clampZoom() {
        if (clamping) return
        val lo = minZoom
        val hi = maxZoom.coerceAtLeast(lo)
        val clamped = zoom.coerceIn(lo, hi)
        if (clamped != zoom) {
            clamping = true
            zoom = clamped
            clamping = false
        }
    }

    //endregion

    //region Styling

    var sheetBackground: Paint = PaperSheetStyle.DEFAULT_SHEET_BACKGROUND
        set(value) {
            val old = field
            field = value
            markSet(PROP_SHEET_BACKGROUND)
            firePropertyChange(PROP_SHEET_BACKGROUND, old, value)
        }

    var sheetBorderColor: Paint = PaperSheetStyle.DEFAULT_SHEET_BORDER_COLOR
        set(value) {
            val old = field
            field = value
            markSet(PROP_SHEET_BORDER_COLOR)
            firePropertyChange(PROP_SHEET_BORDER_COLOR, old, value)
        }

    var sheetBorderWidth: Double = PaperSheetStyle.DEFAULT_SHEET_BORDER_WIDTH
        set(value) {
            val old = field
            field = value
            markSet(PROP_SHEET_BORDER_WIDTH)
            firePropertyChange(PROP_SHEET_BORDER_WIDTH, old, value)
        }

    var shadowColor: Paint = PaperSheetStyle.DEFAULT_SHADOW_COLOR
        set(value) {
            val old = field
            field = value
            markSet(PROP_SHADOW_COLOR)
            firePropertyChange(PROP_SHADOW_COLOR, old, value)
        }

    var shadowOffset: Double = PaperSheetStyle.DEFAULT_SHADOW_OFFSET
        set(value) {
            val old = field
            field = value
            markSet(PROP_SHADOW_OFFSET)
            firePropertyChange(PROP_SHADOW_OFFSET, old, value)
        }

    var selectionColor: Paint = PaperSheetStyle.DEFAULT_SELECTION_COLOR
        set(value) {
            val old = field
            field = value
            markSet(PROP_SELECTION_COLOR)
            firePropertyChange(PROP_SELECTION_COLOR, old, value)
        }

    var caretColor: Color = PaperSheetStyle.DEFAULT_CARET_COLOR
        set(value) {
            val old = field
            field = value
            markSet(PROP_CARET_COLOR)
            firePropertyChange(PROP_CARET_COLOR, old, value)
        }

    var deactivatedSheetBackground: Paint = PaperSheetStyle.DEFAULT_DEACTIVATED_SHEET_BACKGROUND
        set(value) {
            val old = field
            field = value
            markSet(PROP_DEACTIVATED_SHEET_BACKGROUND)
            firePropertyChange(PROP_DEACTIVATED_SHEET_BACKGROUND, old, value)
        }

    var deactivatedOverlayColor: Paint = PaperSheetStyle.DEFAULT_DEACTIVATED_OVERLAY_COLOR
        set(value) {
            val old = field
            field = value
            markSet(PROP_DEACTIVATED_OVERLAY_COLOR)
            firePropertyChange(PROP_DEACTIVATED_OVERLAY_COLOR, old, value)
        }

    //endregion

    //region Caret blink

    var smoothCaretBlink: Boolean = false
        set(value) {
            val old = field
            field = value
            firePropertyChange(PROP_SMOOTH_CARET_BLINK, old, value)
        }

    //endregion

    //region Content size (read-only)

    var contentSize: Dimension = Dimension(0, 0)
        private set

    internal fun updateContentSize(size: Dimension) {
        val old = contentSize
        contentSize = size
        firePropertyChange(PROP_CONTENT_SIZE, old, size)
    }

    //endregion

    //region Selection

    private var selectionCommands: TextSelectionModel.Commands? = null
    private var pendingSelectionCommand: (TextSelectionModel.Commands.() -> Unit)? = null

    val selectionModel: TextSelectionModel = TextSelectionModel { block -> runSelectionCommand(block) }

    /** The currently selected text as plain text; empty when nothing is selected. */
    val selectedText: String get() = selectionModel.text

    /** The bounding box of the current selection in viewport pixels, or `null` when empty. */
    val selectionBounds: Rectangle? get() = selectionModel.bounds

    internal fun registerSelectionCommands(commands: TextSelectionModel.Commands) {
        selectionCommands = commands
        pendingSelectionCommand?.let { pending ->
            pendingSelectionCommand = null
            commands.pending()
        }
    }

    internal fun unregisterSelectionCommands(commands: TextSelectionModel.Commands) {
        if (selectionCommands === commands) selectionCommands = null
    }

    private fun runSelectionCommand(block: TextSelectionModel.Commands.() -> Unit) {
        if (!anySelection) return
        val commands = selectionCommands
        if (commands != null) commands.block() else pendingSelectionCommand = block
    }

    //endregion

    //region Caret

    private var caretCommands: CaretModel.Commands? = null
    private var pendingCaretCommand: (CaretModel.Commands.() -> Unit)? = null

    val caretModel: CaretModel = CaretModel { block -> runCaretCommand(block) }

    internal fun registerCaretCommands(commands: CaretModel.Commands) {
        caretCommands = commands
        pendingCaretCommand?.let { pending ->
            pendingCaretCommand = null
            commands.pending()
        }
    }

    internal fun unregisterCaretCommands(commands: CaretModel.Commands) {
        if (caretCommands === commands) caretCommands = null
    }

    private fun runCaretCommand(block: CaretModel.Commands.() -> Unit) {
        if (!anyCaret) return
        val commands = caretCommands
        if (commands != null) commands.block() else pendingCaretCommand = block
    }

    //endregion

    //region Hover (read-only)

    var hoveredParagraph: Int = -1
        private set

    var hoveredParagraphBounds: Rectangle? = null
        private set

    var hoveredPage: Int = -1
        private set

    var hoveredPageBounds: Rectangle? = null
        private set

    internal fun updateHoveredParagraph(index: Int, bounds: Rectangle?) {
        val oldIndex = hoveredParagraph
        hoveredParagraph = index
        hoveredParagraphBounds = bounds
        firePropertyChange(PROP_HOVERED_PARAGRAPH, oldIndex, index)
    }

    internal fun updateHoveredPage(index: Int, bounds: Rectangle?) {
        val oldIndex = hoveredPage
        hoveredPage = index
        hoveredPageBounds = bounds
        firePropertyChange(PROP_HOVERED_PAGE, oldIndex, index)
    }

    //endregion

    //region Floating overlays

    private val floatingOverlaysList = ArrayList<FloatingOverlay>()

    /**
     * The registered floating overlays: caller-supplied components the view shows, positions and
     * hides on its own when their [FloatingOverlay.trigger] holds. Mutable; the same list instance
     * for the whole life of the view. A newly added overlay is picked up on the next repaint.
     */
    val floatingOverlays: MutableList<FloatingOverlay> get() = floatingOverlaysList

    //endregion

    //region UI wiring

    init {
        isFocusable = anyFocus
        isOpaque = true
        PaperSheetLookAndFeel.applyTo(this)
        updateUI()
    }

    override fun getUIClassID(): String = "PaperSheetViewUI"

    final override fun updateUI() {
        val fromLaf = if (UIManager.get(uiClassID) != null) UIManager.getUI(this) as? PaperSheetUI else null
        setUI(fromLaf ?: BasicPaperSheetUI())
    }

    /** The active Look-and-Feel delegate. */
    fun getPaperSheetUI(): PaperSheetUI? = ui as? PaperSheetUI

    //endregion

    companion object {

        const val DEFAULT_MIN_ZOOM = 0.25
        const val DEFAULT_MAX_ZOOM = 4.0
        const val DEFAULT_ZOOM = 1.0

        const val PROP_DOCUMENT = "document"
        const val PROP_MODE = "mode"
        const val PROP_PAGE_MODES = "pageModes"
        const val PROP_OUTER_MARGIN = "outerMargin"
        const val PROP_PAGE_GAP = "pageGap"
        const val PROP_MIN_ZOOM = "minZoom"
        const val PROP_MAX_ZOOM = "maxZoom"
        const val PROP_ZOOM = "zoom"
        const val PROP_SHEET_BACKGROUND = "sheetBackground"
        const val PROP_SHEET_BORDER_COLOR = "sheetBorderColor"
        const val PROP_SHEET_BORDER_WIDTH = "sheetBorderWidth"
        const val PROP_SHADOW_COLOR = "shadowColor"
        const val PROP_SHADOW_OFFSET = "shadowOffset"
        const val PROP_SELECTION_COLOR = "selectionColor"
        const val PROP_CARET_COLOR = "caretColor"
        const val PROP_DEACTIVATED_SHEET_BACKGROUND = "deactivatedSheetBackground"
        const val PROP_DEACTIVATED_OVERLAY_COLOR = "deactivatedOverlayColor"
        const val PROP_SMOOTH_CARET_BLINK = "smoothCaretBlink"
        const val PROP_CONTENT_SIZE = "contentSize"
        const val PROP_HOVERED_PARAGRAPH = "hoveredParagraph"
        const val PROP_HOVERED_PAGE = "hoveredPage"
    }
}
