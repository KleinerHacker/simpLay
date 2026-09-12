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

import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Dimension2D
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.control.ScrollBar
import javafx.scene.control.SkinBase
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import org.pcsoft.framework.simplay.engine.RenderConfiguration
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.PageMode
import org.pcsoft.framework.simplay.uicommon.hitTest
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetCanvasPainter
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetCaret
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetEditor
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetHoverTracker
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetOverlays
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetScroll
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetSelection
import org.pcsoft.framework.simplay.fx.internal.ps.PaperSheetStyle

/**
 * Skin of [PaperSheetView]. Owns the measuring, the vertical [ScrollBar], the pointer / mouse
 * routing and the canvas painting (delegated to [PaperSheetCanvasPainter], which draws only the pages
 * currently in the viewport - simple page virtualisation - scaled by [PaperSheetView.zoom]).
 *
 * The concerns of a text control live in their own per-view helpers the skin creates and forwards
 * events to:
 *
 * * [PaperSheetSelection] - the anchor/focus selection, its geometry, the styled runs and the
 *   [PaperSheetView.SelectionCommands] sink;
 * * [PaperSheetCaret] - the caret position, blink, geometry and navigation moves (in
 *   [PaperSheetMode.EDITABLE]);
 * * [PaperSheetScroll] - the [PaperSheetView.ScrollCommands] sink, scrolling the viewport to a page,
 *   block, word or symbol regardless of [PaperSheetMode];
 * * [PaperSheetEditor] - the keyboard shortcuts (typing, `Backspace` / `Delete`, `Ctrl+C` / `V` /
 *   `X` / `D`, caret navigation) and the [org.pcsoft.framework.simplay.uicommon.DocumentEditor]
 *   mutations they trigger, plus drag-and-drop of the selection;
 * * [PaperSheetHoverTracker] - the hovered paragraph / sheet;
 * * [PaperSheetOverlays] - the registered [FloatingOverlay]s and the overlay layer on top of the
 *   viewport.
 *
 * The sheet chrome, the selection highlight and the caret are painted with the values from the
 * styleable [PaperSheetView] properties (`-fx-sheet-background` and friends); a change to any of them
 * triggers a repaint. Each page's effective [PageMode] ([PaperSheetView.effectivePageMode]) decides
 * whether it is excluded from layout entirely ([PageMode.laidOut]) or drawn specially
 * ([PageMode.paintedDisabled]); both are re-evaluated on every relayout / redraw, so a change to
 * [PaperSheetView.mode] or [PaperSheetView.pageModes] takes effect immediately.
 */
internal class PaperSheetViewSkin(control: PaperSheetView) : SkinBase<PaperSheetView>(control) {

    //region Nodes and state

    private val canvas = Canvas()
    private val scrollBar = ScrollBar().apply {
        orientation = Orientation.VERTICAL
        min = 0.0
        value = 0.0
    }

    private val measurer = FxFontMeasureCalculator()
    private val renderConfig = RenderConfiguration()
    private val painter = PaperSheetCanvasPainter(canvas)

    private var measured: MeasuredDocument? = null
    private var index: DocumentTextIndex? = null

    /** Unscaled top `y` of each page within the stack, excluding [PaperSheetView.outerMargin]. */
    private var pageTops: DoubleArray = DoubleArray(0)
    private var contentWidthUnscaled = 0.0
    private var contentHeightUnscaled = 0.0

    /** `true` while the mouse extends a selection by dragging. */
    private var dragging = false

    /** `true` while the mouse drags an existing selection to a new drop position. */
    private var draggingSelection = false

    /** `true` while the next `document` change comes from [editor] rather than from outside. */
    private var internalEdit = false

    /** The text selection: anchor/focus, geometry, styled runs and the model command sink. */
    private val selection = PaperSheetSelection(
        view = control,
        measurer = measurer,
        textIndex = { index },
        pageTops = { pageTops },
        scrollOffset = ::scrollOffset,
        requestRedraw = ::redraw,
        onProgrammaticChange = { dragging = false },
    )

    /** The edit caret: position, blink, geometry and the navigation moves. */
    private val caret = PaperSheetCaret(
        view = control,
        selection = selection,
        measurer = measurer,
        textIndex = { index },
        measuredDocument = { measured },
        pageTops = { pageTops },
        scrollOffset = ::scrollOffset,
        requestRedraw = ::redraw,
        scrollCaretIntoView = ::scrollCaretIntoView,
    )

    /**
     * Scrolls the viewport to an absolute page / block / word / symbol position. Clamped against a
     * freshly computed maximum (not the possibly still-default [scrollBar] `max`), since this may run
     * before the first [layoutChildren] pass has ever set it from real content.
     */
    private val scroll = PaperSheetScroll(
        view = control,
        textIndex = { index },
        measuredDocument = { measured },
        pageTops = { pageTops },
        scrollTo = { y ->
            val maxValue = ((contentHeightUnscaled + 2.0 * skinnable.outerMargin) * skinnable.zoom - canvas.height)
                .coerceAtLeast(0.0)
            scrollBar.value = y.coerceIn(0.0, maxValue)
        },
    )

    /** The keyboard shortcuts, the text mutations they trigger and the selection drop. */
    private val editor = PaperSheetEditor(
        view = control,
        selection = selection,
        caret = caret,
        textIndex = { index },
        requestRedraw = ::redraw,
        markInternalEdit = { internalEdit = true },
    )

    /** Tracks the paragraph and sheet under the mouse for the hover overlay triggers. */
    private val hover = PaperSheetHoverTracker(
        view = control,
        measured = { measured },
        pageTops = { pageTops },
        scrollOffset = ::scrollOffset,
        nearestPage = ::nearestPage,
    )

    /** The registered floating overlays and the overlay layer on top of the viewport. */
    private val overlays = PaperSheetOverlays(
        view = control,
        selection = selection,
        caret = caret,
        hover = hover,
        textIndex = { index },
    )

    private val keyHandler = EventHandler<KeyEvent> { editor.onKeyPressed(it) }
    private val keyTypedHandler = EventHandler<KeyEvent> { editor.onKeyTyped(it) }

    /** Page indices drawn in the last [redraw]; for tests. */
    internal var renderedPageIndices: List<Int> = emptyList()
        private set

    /** Number of sheets (border + shadow) painted in the last [redraw]; for tests. */
    internal var sheetChromeDrawCount: Int = 0
        private set

    /** Number of caret strokes drawn in the last [redraw] (`0` in read-only mode); for tests. */
    internal val caretDrawCount: Int get() = painter.caretDrawCount

    /** Number of completed paint passes; for tests. */
    internal val paintCountForTest: Int get() = painter.paintCount

    /** Number of measured pages of the current document; for tests. */
    internal val pageCount: Int get() = measured?.pages?.size ?: 0

    /** The vertical scroll bar; for tests. */
    internal val verticalScrollBar: ScrollBar get() = scrollBar

    /** The current viewport height in pixels; for tests. */
    internal val viewportHeight: Double get() = canvas.height

    /** The painted canvas; for tests that snapshot pixels. */
    internal val canvasForTest: Canvas get() = canvas

    private val mode: PaperSheetMode get() = skinnable.mode

    private fun pageMode(page: MeasuredPage): PageMode = skinnable.effectivePageMode(page.raw.id)

    /** Whether [page] is excluded from layout entirely by its effective [PageMode]. */
    private fun isPageHidden(page: MeasuredPage): Boolean = !pageMode(page).laidOut

    /** Whether [page] is drawn specially by its effective [PageMode]. */
    private fun isPageDisabled(page: MeasuredPage): Boolean = pageMode(page).paintedDisabled

    //endregion

    //region Wiring

    init {
        children.addAll(canvas, scrollBar, overlays.layer)

        remeasure()
        skinnable.registerSelectionCommands(selection)
        skinnable.registerCaretCommands(caret)
        skinnable.registerScrollCommands(scroll)

        registerChangeListener(control.documentProperty) { remeasure(); control.requestLayout() }
        registerChangeListener(control.outerMarginProperty) { relayout() }
        registerChangeListener(control.pageGapProperty) { relayout() }
        registerChangeListener(control.zoomProperty) { relayout() }
        registerChangeListener(control.modeProperty) {
            if (!control.anySelection) selection.clearSelection()
            caret.onModeChanged()
        }
        registerChangeListener(control.smoothCaretBlinkProperty) { caret.restartBlink() }
        registerChangeListener(control.focusedProperty()) { caret.restartBlink() }

        registerChangeListener(control.pageModesProperty) {
            if (!control.anySelection) selection.clearSelection()
            caret.onModeChanged()
            relayout()
        }

        registerChangeListener(control.sheetBackgroundProperty) { redraw() }
        registerChangeListener(control.sheetBorderColorProperty) { redraw() }
        registerChangeListener(control.sheetBorderWidthProperty) { redraw() }
        registerChangeListener(control.shadowColorProperty) { redraw() }
        registerChangeListener(control.shadowOffsetProperty) { redraw() }
        registerChangeListener(control.selectionColorProperty) { redraw() }
        registerChangeListener(control.caretColorProperty) { redraw() }
        registerChangeListener(control.deactivatedSheetBackgroundProperty) { redraw() }
        registerChangeListener(control.deactivatedOverlayColorProperty) { redraw() }

        scrollBar.valueProperty().addListener { _, _, _ -> redraw() }

        canvas.addEventHandler(ScrollEvent.SCROLL, ::onScroll)
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, ::onMousePressed)
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, ::onMouseDragged)
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, ::onMouseReleased)
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, ::onMouseClicked)
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) {
            canvas.cursor = cursorFor(it.x, it.y)
            hover.update(it.x, it.y)
            overlays.refresh()
        }
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED) {
            canvas.cursor = Cursor.DEFAULT
            hover.clear()
            overlays.refresh()
        }
        control.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler)
        control.addEventHandler(KeyEvent.KEY_TYPED, keyTypedHandler)
    }

    //endregion

    //region Measuring / layout

    private fun remeasure() {
        val reload = !internalEdit
        internalEdit = false
        // A page override is tied to a specific page instance; once the document is reloaded from
        // outside (as opposed to replaced by an edit), none of the previous overrides can still be
        // meaningful, so the default state - every page following the global mode - is restored.
        if (reload) skinnable.pageModes = emptyMap()
        val document = skinnable.document
        measured = document?.measure(measurer, renderConfig)
        index = measured?.let { DocumentTextIndex(it) }
        selection.onDocumentChanged(index)
        hover.clear()
        computeLayoutMetrics()
        selection.publish()
        caret.onDocumentRemeasured(reload)
        if (reload) scrollBar.value = 0.0
    }

    private fun relayout() {
        computeLayoutMetrics()
        skinnable.requestLayout()
    }

    private fun computeLayoutMetrics() {
        val doc = measured
        val gap = skinnable.pageGap
        val outer = skinnable.outerMargin
        if (doc == null || doc.pages.isEmpty()) {
            pageTops = DoubleArray(0)
            contentWidthUnscaled = 0.0
            contentHeightUnscaled = 0.0
            skinnable.updateContentSize(Dimension2D(0.0, 0.0))
            return
        }
        pageTops = DoubleArray(doc.pages.size)
        var y = 0.0
        doc.pages.forEachIndexed { i, page ->
            pageTops[i] = y
            if (!isPageHidden(page)) {
                y += page.effectiveSize.height
                if (i != doc.pages.lastIndex) y += gap
            }
        }
        contentHeightUnscaled = y
        contentWidthUnscaled = doc.pages.filterNot(::isPageHidden).maxOfOrNull { it.effectiveSize.width } ?: 0.0
        skinnable.updateContentSize(
            Dimension2D(contentWidthUnscaled + 2.0 * outer, contentHeightUnscaled + 2.0 * outer),
        )
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        val barWidth = scrollBar.prefWidth(-1.0).let { if (it <= 0.0) DEFAULT_SCROLLBAR_WIDTH else it }
        val viewportWidth = (w - barWidth).coerceAtLeast(0.0)

        canvas.width = viewportWidth
        canvas.height = h
        canvas.relocate(x, y)
        scrollBar.resizeRelocate(x + viewportWidth, y, barWidth, h)

        overlays.layout(x, y, viewportWidth, h)

        updateScrollBar(h)
        redraw()
    }

    private fun updateScrollBar(viewportHeight: Double) {
        val totalScaled = (contentHeightUnscaled + 2.0 * skinnable.outerMargin) * skinnable.zoom
        val maxValue = (totalScaled - viewportHeight).coerceAtLeast(0.0)
        scrollBar.max = maxValue
        scrollBar.visibleAmount = if (maxValue <= 0.0) 0.0 else viewportHeight
        scrollBar.unitIncrement = LINE_SCROLL_STEP
        scrollBar.blockIncrement = viewportHeight
        if (scrollBar.value > maxValue) scrollBar.value = maxValue
        scrollBar.isDisable = maxValue <= 0.0
    }

    override fun computePrefWidth(height: Double, top: Double, right: Double, bottom: Double, left: Double): Double =
        left + right + (skinnable.contentSize.width * skinnable.zoom).coerceIn(PREF_MIN, PREF_MAX)

    override fun computePrefHeight(width: Double, top: Double, right: Double, bottom: Double, left: Double): Double =
        top + bottom + (skinnable.contentSize.height * skinnable.zoom).coerceIn(PREF_MIN, PREF_MAX)

    override fun dispose() {
        caret.dispose()
        overlays.dispose()
        skinnable?.removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler)
        skinnable?.removeEventHandler(KeyEvent.KEY_TYPED, keyTypedHandler)
        skinnable?.unregisterSelectionCommands(selection)
        skinnable?.unregisterCaretCommands(caret)
        skinnable?.unregisterScrollCommands(scroll)
        super.dispose()
    }

    //endregion

    //region Drawing

    private fun scrollOffset(): Double = scrollBar.value.coerceIn(0.0, scrollBar.max)

    /**
     * Scrolls the viewport by the smallest amount that brings the caret back into it, leaving
     * [CARET_SCROLL_PADDING] of slack above and below; does nothing while there is nothing to scroll
     * or the caret has no geometry.
     */
    private fun scrollCaretIntoView() {
        if (scrollBar.max <= 0.0) return
        val viewportHeight = canvas.height
        if (viewportHeight <= 0.0) return
        val bounds = caret.viewportBounds() ?: return
        val top = bounds.minY - CARET_SCROLL_PADDING
        val bottom = bounds.maxY + CARET_SCROLL_PADDING
        val delta = when {
            top < 0.0 -> top
            bottom > viewportHeight -> bottom - viewportHeight
            else -> return
        }
        scrollBar.value = (scrollBar.value + delta).coerceIn(0.0, scrollBar.max)
    }

    private fun currentStyle(): PaperSheetStyle = PaperSheetStyle(
        sheetBackground = skinnable.sheetBackground,
        sheetBorderColor = skinnable.sheetBorderColor,
        sheetBorderWidth = skinnable.sheetBorderWidth,
        shadowColor = skinnable.shadowColor,
        shadowOffset = skinnable.shadowOffset,
        selectionColor = skinnable.selectionColor,
        caretColor = skinnable.caretColor,
        deactivatedSheetBackground = skinnable.deactivatedSheetBackground,
        deactivatedOverlayColor = skinnable.deactivatedOverlayColor,
    )

    private fun redraw() {
        painter.paint(
            measured = measured,
            pageTops = pageTops,
            zoom = skinnable.zoom,
            outerMargin = skinnable.outerMargin,
            scrollOffset = scrollOffset(),
            index = index,
            selectionStart = selection.start,
            selectionEnd = selection.end,
            fonts = measurer,
            style = currentStyle(),
            caret = caret.caretPaint(),
            caretOpacity = caret.currentOpacity(),
            isHidden = ::isPageHidden,
            isDisabled = ::isPageDisabled,
        )
        renderedPageIndices = painter.renderedPageIndices
        sheetChromeDrawCount = painter.sheetChromeDrawCount
        selection.publish()
        hover.publishOutputs()
        overlays.refresh()
        caret.publish()
    }

    //endregion

    //region Hit testing / pointer

    /**
     * The page whose vertical band is closest to [cyUnscaled] (0 distance when inside it), with the
     * signed distance; used for hit-testing, the pointer shape and the hover tracker. Pages excluded
     * from layout by their effective [PageMode] are skipped.
     */
    private fun nearestPage(cyUnscaled: Double): Pair<Int, Double> {
        val doc = measured!!
        val outer = skinnable.outerMargin
        var pageIndex = 0
        var bestDist = Double.MAX_VALUE
        doc.pages.forEachIndexed { i, page ->
            if (isPageHidden(page)) return@forEachIndexed
            val top = outer + pageTops[i]
            val bottom = top + page.effectiveSize.height
            val dist = when {
                cyUnscaled < top -> top - cyUnscaled
                cyUnscaled > bottom -> cyUnscaled - bottom
                else -> 0.0
            }
            if (dist < bestDist) {
                bestDist = dist
                pageIndex = i
            }
        }
        return pageIndex to bestDist
    }

    /**
     * The pointer shape at a viewport point: a text cursor over a page content area, else default.
     * No page currently supporting selection, or a page whose own effective [PageMode] does not
     * support selection, always keeps the default arrow.
     */
    private fun cursorFor(px: Double, py: Double): Cursor {
        if (!skinnable.anySelection) return Cursor.DEFAULT
        val doc = measured ?: return Cursor.DEFAULT
        if (doc.pages.isEmpty()) return Cursor.DEFAULT
        val zoom = skinnable.zoom
        val outer = skinnable.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (pageIndex, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) return Cursor.DEFAULT
        if (!pageMode(doc.pages[pageIndex]).supportsSelection) return Cursor.DEFAULT
        val contentArea = doc.pages[pageIndex].contentArea
        val localX = cx - outer - contentArea.x
        val localY = cy - (outer + pageTops[pageIndex]) - contentArea.y
        return if (localX in 0.0..contentArea.width && localY in 0.0..contentArea.height) Cursor.TEXT else Cursor.DEFAULT
    }

    private fun hitIndexAt(px: Double, py: Double): Int {
        val doc = measured ?: return 0
        val idx = index ?: return 0
        if (doc.pages.isEmpty() || idx.segments.isEmpty()) return 0

        val zoom = skinnable.zoom
        val outer = skinnable.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom

        val (pageIndex, _) = nearestPage(cy)
        val page = doc.pages[pageIndex]
        val contentArea = page.contentArea
        val localX = cx - outer - contentArea.x
        val localY = cy - (outer + pageTops[pageIndex]) - contentArea.y

        val pageSegments = idx.segments.filter { it.pageIndex == pageIndex }
        if (pageSegments.isEmpty()) {
            return idx.segments.firstOrNull { it.pageIndex >= pageIndex }?.start ?: idx.length
        }

        val lineSegment = pageSegments.minByOrNull { seg ->
            val box = seg.line.lineBox
            when {
                localY < box.y -> box.y - localY
                localY > box.y + box.height -> localY - box.y - box.height
                else -> 0.0
            }
        }!!
        val lineSegments = pageSegments.filter { it.line === lineSegment.line }
        val partSegment = lineSegments.minByOrNull { seg ->
            val bounds = seg.part.bounds
            when {
                localX < bounds.x -> bounds.x - localX
                localX > bounds.x + bounds.width -> localX - bounds.x - bounds.width
                else -> 0.0
            }
        }!!

        val offset = hitTest(partSegment.part, partSegment.font, localX, measurer)
            .coerceIn(0, partSegment.part.text.length)
        return partSegment.start + offset
    }

    //endregion

    //region Input

    private fun onScroll(event: ScrollEvent) {
        if (scrollBar.max <= 0.0) return
        scrollBar.value = (scrollBar.value - event.deltaY).coerceIn(0.0, scrollBar.max)
        event.consume()
    }

    private fun onMousePressed(event: MouseEvent) {
        val doc = measured
        val hitPageMode = doc?.pages?.takeIf { it.isNotEmpty() }
            ?.let { pageMode(it[nearestPage((event.y + scrollOffset()) / skinnable.zoom).first]) }
        if (hitPageMode?.supportsSelection != true) return
        if (skinnable.anyFocus) skinnable.requestFocus()
        caret.clearShiftAnchor()
        val i = hitIndexAt(event.x, event.y)
        if (hitPageMode.supportsEditing && !selection.isEmpty && selection.contains(event.x, event.y)) {
            draggingSelection = true
            dragging = false
            caret.setDropPreview(i)
            event.consume()
            return
        }
        selection.beginAt(i)
        dragging = true
        if (hitPageMode.supportsCaret) {
            caret.placeCaret(i)
        } else {
            redraw()
        }
        event.consume()
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (draggingSelection) {
            caret.setDropPreview(hitIndexAt(event.x, event.y))
            event.consume()
            return
        }
        if (!dragging) return
        selection.dragTo(hitIndexAt(event.x, event.y))
        redraw()
        event.consume()
    }

    private fun onMouseReleased(event: MouseEvent) {
        if (draggingSelection) {
            val target = hitIndexAt(event.x, event.y)
            val copy = event.isShortcutDown
            draggingSelection = false
            caret.setDropPreview(null)
            editor.dropSelection(target, copy)
            event.consume()
            return
        }
        dragging = false
    }

    private fun onMouseClicked(event: MouseEvent) {
        if (event.clickCount != 2) return
        val doc = measured?.takeIf { it.pages.isNotEmpty() } ?: return
        val hitPageMode = pageMode(doc.pages[nearestPage((event.y + scrollOffset()) / skinnable.zoom).first])
        if (!hitPageMode.supportsSelection) return
        selection.selectWordAt(hitIndexAt(event.x, event.y))
        if (hitPageMode.supportsCaret) caret.placeCaret(selection.end)
        redraw()
        event.consume()
    }

    //endregion

    //region Test hooks

    /** Selects the text between two viewport points and repaints; for tests. */
    internal fun selectByPointsForTest(x0: Double, y0: Double, x1: Double, y1: Double) {
        selection.beginAt(hitIndexAt(x0, y0))
        selection.dragTo(hitIndexAt(x1, y1))
        redraw()
    }

    /** The pointer shape the skin would show at a viewport point; for tests. */
    internal fun cursorAtForTest(x: Double, y: Double): Cursor = cursorFor(x, y)

    /** Simulates the mouse hovering a viewport point and refreshes the overlays; for tests. */
    internal fun hoverAtForTest(x: Double, y: Double) {
        hover.update(x, y)
        overlays.refresh()
    }

    /** Simulates the mouse leaving the viewport and refreshes the overlays; for tests. */
    internal fun clearHoverForTest() {
        hover.clear()
        overlays.refresh()
    }

    /** Recomputes overlay visibility and position; for tests. */
    internal fun refreshOverlaysForTest() = overlays.refresh()

    /** The overlays whose node currently sits in the overlay layer; for tests. */
    internal val activeOverlaysForTest: Set<FloatingOverlay> get() = overlays.activeForTest

    /** Number of nodes currently in the overlay layer; for tests. */
    internal val overlayNodeCountForTest: Int get() = overlays.nodeCountForTest

    /** The current caret index; for tests. */
    internal val caretIndexForTest: Int get() = caret.position

    /** Whether the caret would currently be painted; for tests. */
    internal val caretRenderedForTest: Boolean get() = caret.caretPaint() != null

    /** The opacity the caret is painted with right now; for tests. */
    internal fun caretOpacityForTest(): Double = caret.currentOpacity()

    /** The current caret rectangle in viewport pixels, or `null`; for tests. */
    internal fun caretBoundsForTest(): Bounds? = caret.viewportBounds()

    /** Places the caret at the character nearest a viewport point; for tests. */
    internal fun placeCaretAtForTest(x: Double, y: Double) = caret.setCaret(hitIndexAt(x, y), extend = false)

    /** Types [text] at the caret (replacing any selection); for tests. */
    internal fun typeTextForTest(text: String) = editor.typeText(text)

    /** Fires a `KEY_PRESSED` through the editor's handler; for tests. */
    internal fun pressKeyForTest(code: KeyCode, shift: Boolean = false, shortcut: Boolean = false) {
        val mac = System.getProperty("os.name", "").lowercase().contains("mac")
        val control = shortcut && !mac
        val meta = shortcut && mac
        editor.onKeyPressed(KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, control, false, meta))
    }

    /** Drops the current selection at the character nearest a viewport point; for tests. */
    internal fun dragSelectionToForTest(x: Double, y: Double, copy: Boolean) =
        editor.dropSelection(hitIndexAt(x, y), copy)

    //endregion

    private companion object {

        const val DEFAULT_SCROLLBAR_WIDTH = 14.0
        const val LINE_SCROLL_STEP = 40.0

        /** Slack kept above and below the caret when scrolling it back into the viewport. */
        const val CARET_SCROLL_PADDING = 8.0
        const val PREF_MIN = 240.0
        const val PREF_MAX = 2000.0
    }
}
