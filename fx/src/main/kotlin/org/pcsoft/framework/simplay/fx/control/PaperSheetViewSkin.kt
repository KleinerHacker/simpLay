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

import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Dimension2D
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import javafx.scene.control.ScrollBar
import javafx.scene.control.SkinBase
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import kotlin.math.max
import kotlin.math.min
import org.pcsoft.framework.simplay.engine.engine.RenderConfiguration
import org.pcsoft.framework.simplay.engine.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight
import org.pcsoft.framework.simplay.fx.internal.DocumentTextIndex
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.fx.internal.hitTest
import org.pcsoft.framework.simplay.fx.internal.segmentSpanX

/**
 * The anchor box and context a satisfied [FloatingOverlayTrigger] hands to the skin: [bounds] in
 * viewport pixels, a trigger-specific [index] (selection start, paragraph ordinal, page index), the
 * trigger [text] and, for [FloatingOverlayTrigger.SELECTION], the covered [range].
 */
internal class TriggerGeometry(val bounds: Bounds, val index: Int, val text: String, val range: IntRange?)

/**
 * Skin of [PaperSheetView]. Owns the measuring, the vertical [ScrollBar], the mouse and keyboard
 * handling and the selection model; the actual canvas painting is delegated to
 * [PaperSheetCanvasPainter], which draws only the pages currently in the viewport (simple page
 * virtualisation) scaled by [PaperSheetView.zoom]. Mouse dragging selects text over the measured
 * geometry, `Ctrl+C` copies it as styled HTML, RTF and plain text at once so a paste target keeps
 * the text style. The pointer turns into a text (I-beam) cursor while it is over a page's content
 * area.
 *
 * The skin is the only writer of [PaperSheetView.selectionModel] and the sink for its commands. It
 * also drives the registered [FloatingOverlay]s: the text selection is tracked here, the paragraph
 * and sheet under the mouse in [PaperSheetHoverTracker], and each overlay's node is shown, positioned
 * (following scroll and zoom, clamped to the viewport edge) and hidden in an internal overlay [Pane]
 * on top of the viewport.
 */
internal class PaperSheetViewSkin(control: PaperSheetView) : SkinBase<PaperSheetView>(control) {

    //region Nodes and state

    private val canvas = Canvas()
    private val scrollBar = ScrollBar().apply {
        orientation = Orientation.VERTICAL
        min = 0.0
        value = 0.0
    }

    private val overlayClip = Rectangle()
    private val overlayPane = Pane().apply {
        isManaged = false
        isPickOnBounds = false
        clip = overlayClip
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

    private val selection = TextSelection()
    private var dragging = false

    /** Tracks the paragraph and sheet under the mouse for the hover overlay triggers. */
    private val hover = PaperSheetHoverTracker(
        view = control,
        measured = { measured },
        pageTops = { pageTops },
        scrollOffset = ::scrollOffset,
        nearestPage = ::nearestPage,
    )

    private val keyHandler = EventHandler<KeyEvent> { onKeyPressed(it) }

    /** Overlays whose node currently sits in [overlayPane]. */
    private val activeOverlays = HashSet<FloatingOverlay>()

    private val overlaysListener = ListChangeListener<FloatingOverlay> { change ->
        while (change.next()) change.removed.forEach { detachOverlay(it, fireEvent = false) }
        refreshOverlays()
    }

    /** Sink for the programmatic selection commands of [PaperSheetView.selectionModel]. */
    private val selectionCommands = object : PaperSheetView.SelectionCommands {
        override fun selectRange(start: Int, end: Int) {
            if (index == null) return
            selection.selectRange(start, end)
            dragging = false
            redraw()
        }

        override fun selectAll() {
            selection.selectAll()
            dragging = false
            redraw()
        }

        override fun clearSelection() {
            selection.reset()
            dragging = false
            redraw()
        }
    }

    /** Page indices drawn in the last [redraw]; for tests. */
    internal var renderedPageIndices: List<Int> = emptyList()
        private set

    /** Number of sheets (border + shadow) painted in the last [redraw]; for tests. */
    internal var sheetChromeDrawCount: Int = 0
        private set

    /** No caret is ever drawn in the read-only component; for tests. */
    internal val caretDrawCount: Int = 0

    /** Number of measured pages of the current document; for tests. */
    internal val pageCount: Int get() = measured?.pages?.size ?: 0

    /** The vertical scroll bar; for tests. */
    internal val verticalScrollBar: ScrollBar get() = scrollBar

    /** The current viewport height in pixels; for tests. */
    internal val viewportHeight: Double get() = canvas.height

    //endregion

    //region Wiring

    init {
        children.addAll(canvas, scrollBar, overlayPane)

        remeasure()
        skinnable.registerSelectionCommands(selectionCommands)
        skinnable.floatingOverlays.addListener(overlaysListener)

        registerChangeListener(control.documentProperty) { remeasure(); control.requestLayout() }
        registerChangeListener(control.outerMarginProperty) { relayout() }
        registerChangeListener(control.pageGapProperty) { relayout() }
        registerChangeListener(control.zoomProperty) { relayout() }

        scrollBar.valueProperty().addListener { _, _, _ -> redraw() }

        canvas.addEventHandler(ScrollEvent.SCROLL, ::onScroll)
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, ::onMousePressed)
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, ::onMouseDragged)
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { dragging = false }
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, ::onMouseClicked)
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) {
            canvas.cursor = cursorFor(it.x, it.y)
            hover.update(it.x, it.y)
            refreshOverlays()
        }
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED) {
            canvas.cursor = Cursor.DEFAULT
            hover.clear()
            refreshOverlays()
        }
        control.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler)
    }

    //endregion

    //region Measuring / layout

    private fun remeasure() {
        val document = skinnable.document
        measured = document?.measure(measurer, renderConfig)
        index = measured?.let { DocumentTextIndex(it) }
        selection.index = index
        selection.reset()
        hover.clear()
        computeLayoutMetrics()
        updateSelectionOutputs()
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
            y += page.effectiveSize.height
            if (i != doc.pages.lastIndex) y += gap
        }
        contentHeightUnscaled = y
        contentWidthUnscaled = doc.pages.maxOf { it.effectiveSize.width }
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

        overlayPane.resizeRelocate(x, y, viewportWidth, h)
        overlayClip.width = viewportWidth
        overlayClip.height = h

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
        skinnable?.removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler)
        skinnable?.unregisterSelectionCommands(selectionCommands)
        skinnable?.floatingOverlays?.removeListener(overlaysListener)
        super.dispose()
    }

    //endregion

    //region Drawing

    private fun scrollOffset(): Double = scrollBar.value.coerceIn(0.0, scrollBar.max)

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
        )
        renderedPageIndices = painter.renderedPageIndices
        sheetChromeDrawCount = painter.sheetChromeDrawCount
        updateSelectionOutputs()
        hover.publishOutputs()
        refreshOverlays()
    }

    //endregion

    //region Selection outputs

    private fun updateSelectionOutputs() {
        val idx = index
        if (idx == null || selection.isEmpty) {
            skinnable.selectionModel.update("", 0, 0, null, emptyList())
            return
        }
        val start = selection.start
        val end = selection.end
        val runs = idx.styledRuns(start, end).mapNotNull { run ->
            val font = run.font ?: return@mapNotNull null
            TextSelectionData(
                text = run.text,
                fontFamily = font.family,
                fontSize = font.size,
                bold = font.weight == FontWeight.BOLD,
                italic = font.style == FontStyle.ITALIC,
            )
        }
        skinnable.selectionModel.update(idx.substring(start, end), start, end, computeSelectionBounds(), runs)
    }

    private fun computeSelectionBounds(): Bounds? {
        val idx = index ?: return null
        if (selection.isEmpty) return null
        val zoom = skinnable.zoom
        val outer = skinnable.outerMargin
        val scroll = scrollOffset()
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        val lo = selection.start
        val hi = selection.end
        for (seg in idx.segments) {
            if (seg.end <= lo || seg.start >= hi) continue
            val (x0, x1) = segmentSpanX(seg, lo, hi, measurer)
            val contentArea = seg.page.contentArea
            val absX0 = outer + contentArea.x + x0
            val absX1 = outer + contentArea.x + x1
            val absY0 = outer + pageTops[seg.pageIndex] + contentArea.y + seg.line.lineBox.y
            val absY1 = absY0 + seg.line.lineBox.height
            minX = min(minX, absX0)
            maxX = max(maxX, absX1)
            minY = min(minY, absY0)
            maxY = max(maxY, absY1)
        }
        if (minX > maxX) return null
        return BoundingBox(minX * zoom, minY * zoom - scroll, (maxX - minX) * zoom, (maxY - minY) * zoom)
    }

    //endregion

    //region Hit testing / pointer

    /**
     * The page whose vertical band is closest to [cyUnscaled] (0 distance when inside it), with the
     * signed distance; used for hit-testing, the pointer shape and the hover tracker.
     */
    private fun nearestPage(cyUnscaled: Double): Pair<Int, Double> {
        val doc = measured!!
        val outer = skinnable.outerMargin
        var pageIndex = 0
        var bestDist = Double.MAX_VALUE
        doc.pages.forEachIndexed { i, page ->
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

    /** The pointer shape at a viewport point: a text cursor over a page content area, else default. */
    private fun cursorFor(px: Double, py: Double): Cursor {
        val doc = measured ?: return Cursor.DEFAULT
        if (doc.pages.isEmpty()) return Cursor.DEFAULT
        val zoom = skinnable.zoom
        val outer = skinnable.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (pageIndex, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) return Cursor.DEFAULT
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

    //region Floating overlays

    private fun geometryFor(kind: FloatingOverlayTrigger): TriggerGeometry? = when (kind) {
        FloatingOverlayTrigger.SELECTION -> {
            val bounds = computeSelectionBounds()
            if (bounds == null) null
            else TriggerGeometry(bounds, selection.start, skinnable.selectedText, selection.start until selection.end)
        }
        FloatingOverlayTrigger.PARAGRAPH_HOVER -> hover.paragraphGeometry()
        FloatingOverlayTrigger.PAGE_HOVER -> hover.pageGeometry()
        // Inert until the editing implementation plan wires a caret.
        FloatingOverlayTrigger.CARET -> null
    }

    private fun refreshOverlays() {
        val overlays = skinnable.floatingOverlays
        if (overlays.isEmpty() && activeOverlays.isEmpty()) return
        for (overlay in overlays) {
            val geometry = geometryFor(overlay.trigger)
            val node = overlay.content
            if (geometry == null || node == null) {
                if (overlay in activeOverlays && overlay.autoHide) detachOverlay(overlay, fireEvent = true)
                continue
            }
            if (node !in overlayPane.children) overlayPane.children.add(node)
            node.applyCss()
            node.autosize()
            val placed = placeInViewport(node, geometry.bounds, overlay)
            if (placed == null) {
                detachOverlay(overlay, fireEvent = true)
                continue
            }
            node.relocate(placed.first, placed.second)
            overlay.updateActiveState(geometry.bounds, geometry.index, geometry.text, geometry.range)
            if (activeOverlays.add(overlay)) overlay.fireShown(overlay.trigger)
        }
    }

    private fun detachOverlay(overlay: FloatingOverlay, fireEvent: Boolean) {
        overlay.content?.let { overlayPane.children.remove(it) }
        val wasActive = activeOverlays.remove(overlay)
        overlay.clearActiveState()
        if (wasActive && fireEvent) overlay.fireHidden(overlay.trigger)
    }

    /**
     * Places [node] relative to the trigger [bounds] per the overlay's [FloatingOverlay.anchor] plus
     * its offsets, then clamps the result to the viewport. Returns `null` when [bounds] no longer
     * intersects the viewport at all, so the overlay must be hidden.
     */
    private fun placeInViewport(node: Node, bounds: Bounds, overlay: FloatingOverlay): Pair<Double, Double>? {
        val vpW = canvas.width
        val vpH = canvas.height
        if (bounds.maxX <= 0.0 || bounds.minX >= vpW || bounds.maxY <= 0.0 || bounds.minY >= vpH) return null

        val w = node.layoutBounds.width.takeIf { it > 0.0 } ?: node.prefWidth(-1.0)
        val h = node.layoutBounds.height.takeIf { it > 0.0 } ?: node.prefHeight(-1.0)
        val anchor = overlay.anchor
        var nx = when (anchor.hpos) {
            HPos.LEFT -> bounds.minX
            HPos.CENTER -> bounds.minX + bounds.width / 2.0 - w / 2.0
            HPos.RIGHT -> bounds.maxX - w
        } + overlay.offsetX
        var ny = when (anchor.vpos) {
            VPos.TOP -> bounds.minY - h
            VPos.CENTER -> bounds.minY + bounds.height / 2.0 - h / 2.0
            VPos.BASELINE, VPos.BOTTOM -> bounds.maxY
        } + overlay.offsetY
        nx = nx.coerceIn(0.0, (vpW - w).coerceAtLeast(0.0))
        ny = ny.coerceIn(0.0, (vpH - h).coerceAtLeast(0.0))
        return nx to ny
    }

    //endregion

    //region Input

    private fun onScroll(event: ScrollEvent) {
        if (scrollBar.max <= 0.0) return
        scrollBar.value = (scrollBar.value - event.deltaY).coerceIn(0.0, scrollBar.max)
        event.consume()
    }

    private fun onMousePressed(event: MouseEvent) {
        skinnable.requestFocus()
        val i = hitIndexAt(event.x, event.y)
        selection.anchor = i
        selection.focus = i
        dragging = true
        redraw()
        event.consume()
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (!dragging) return
        selection.focus = hitIndexAt(event.x, event.y)
        redraw()
        event.consume()
    }

    private fun onMouseClicked(event: MouseEvent) {
        if (event.clickCount != 2) return
        selection.selectWordAt(hitIndexAt(event.x, event.y))
        redraw()
        event.consume()
    }

    private fun onKeyPressed(event: KeyEvent) {
        if (event.code == KeyCode.C && event.isShortcutDown) {
            if (selection.putStyledSelectionOnClipboard()) {
                event.consume()
            }
        }
    }

    //endregion

    //region Test hooks

    /** Selects the text between two viewport points and repaints; for tests. */
    internal fun selectByPointsForTest(x0: Double, y0: Double, x1: Double, y1: Double) {
        selection.anchor = hitIndexAt(x0, y0)
        selection.focus = hitIndexAt(x1, y1)
        redraw()
    }

    /** The pointer shape the skin would show at a viewport point; for tests. */
    internal fun cursorAtForTest(x: Double, y: Double): Cursor = cursorFor(x, y)

    /** Simulates the mouse hovering a viewport point and refreshes the overlays; for tests. */
    internal fun hoverAtForTest(x: Double, y: Double) {
        hover.update(x, y)
        refreshOverlays()
    }

    /** Simulates the mouse leaving the viewport and refreshes the overlays; for tests. */
    internal fun clearHoverForTest() {
        hover.clear()
        refreshOverlays()
    }

    /** Recomputes overlay visibility and position; for tests. */
    internal fun refreshOverlaysForTest() = refreshOverlays()

    /** The overlays whose node currently sits in the overlay pane; for tests. */
    internal val activeOverlaysForTest: Set<FloatingOverlay> get() = activeOverlays.toSet()

    /** Number of nodes currently in the overlay pane; for tests. */
    internal val overlayNodeCountForTest: Int get() = overlayPane.children.size

    //endregion

    private companion object {

        const val DEFAULT_SCROLLBAR_WIDTH = 14.0
        const val LINE_SCROLL_STEP = 40.0
        const val PREF_MIN = 240.0
        const val PREF_MAX = 2000.0
    }
}
