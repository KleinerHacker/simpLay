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
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JScrollBar
import kotlin.math.roundToInt
import org.pcsoft.framework.simplay.engine.RenderConfiguration
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.Page
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetCaret
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetEditor
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetHoverTracker
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetOverlays
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetScroll
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetSelection
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetStyle
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetSwingPainter
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.PageMode
import org.pcsoft.framework.simplay.uicommon.hitTest

/**
 * The default [PaperSheetUI]: owns the measuring, the vertical [JScrollBar], the pointer / mouse
 * routing and the viewport painting (delegated to [PaperSheetSwingPainter], which draws only the
 * pages currently in the viewport - simple page virtualisation - scaled by [PaperSheetView.zoom]).
 * The Swing counterpart of the `fx` module's `PaperSheetViewSkin`.
 *
 * The per-view text concerns live in their own helpers this delegate creates and forwards events to:
 * [PaperSheetSelection] (anchor/focus selection, geometry, the [TextSelectionModel.Commands] sink),
 * [PaperSheetCaret] (caret position, blink, geometry, navigation moves - in
 * [PaperSheetMode.EDITABLE]), [PaperSheetScroll] (the [PaperSheetView.ScrollCommands] sink, scrolling
 * the viewport to a page, block, word or symbol regardless of [PaperSheetMode]), [PaperSheetEditor]
 * (the keyboard shortcuts and the [org.pcsoft.framework.simplay.uicommon.DocumentEditor] mutations
 * they trigger, plus drag-and-drop of the selection), [PaperSheetHoverTracker] (the hovered paragraph
 * / sheet) and [PaperSheetOverlays] (the registered [FloatingOverlay]s and the overlay layer on top of
 * the viewport).
 *
 * Each page's effective [PageMode] ([PaperSheetView.effectivePageMode]) decides whether it is
 * excluded from layout entirely ([PageMode.laidOut]) or drawn specially ([PageMode.paintedDisabled]);
 * both are re-evaluated on every relayout / redraw.
 */
open class BasicPaperSheetUI : PaperSheetUI() {

    private lateinit var view: PaperSheetView
    private lateinit var measurer: SwingFontMeasureCalculator
    private lateinit var renderConfig: RenderConfiguration
    private lateinit var painter: PaperSheetSwingPainter
    private lateinit var selection: PaperSheetSelection
    private lateinit var caret: PaperSheetCaret
    private lateinit var scroll: PaperSheetScroll
    private lateinit var editor: PaperSheetEditor
    private lateinit var hover: PaperSheetHoverTracker
    private lateinit var overlays: PaperSheetOverlays
    private lateinit var scrollBar: JScrollBar

    private var measured: MeasuredDocument? = null
    private var index: DocumentTextIndex? = null
    private var pageTops: DoubleArray = DoubleArray(0)
    private var contentWidthUnscaled = 0.0
    private var contentHeightUnscaled = 0.0

    /** `true` while the mouse extends a selection by dragging. */
    private var dragging = false

    /** `true` while the mouse drags an existing selection to a new drop position. */
    private var draggingSelection = false

    /** `true` while the next `document` change comes from [editor] rather than from outside. */
    private var internalEdit = false

    private lateinit var propertyListener: PropertyChangeListener
    private lateinit var mouseListener: MouseAdapter
    private lateinit var keyListener: KeyAdapter
    private lateinit var focusListener: FocusAdapter
    private lateinit var componentListener: ComponentAdapter

    private val shortcutMask: Int = runCatching { Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx }
        .getOrDefault(java.awt.event.InputEvent.CTRL_DOWN_MASK)

    private val editable: Boolean get() = view.anyEditing

    private val caretActive: Boolean get() = view.anyCaret

    private val selectable: Boolean get() = view.anySelection

    private fun pageMode(page: MeasuredPage): PageMode = view.effectivePageMode(page.raw.id)

    /** Whether [page] is excluded from layout entirely by its effective [PageMode]. */
    private fun isPageHidden(page: MeasuredPage): Boolean = !pageMode(page).laidOut

    /** Whether [page] is drawn specially by its effective [PageMode]. */
    private fun isPageDisabled(page: MeasuredPage): Boolean = pageMode(page).paintedDisabled

    //region install / uninstall

    override fun installUI(c: JComponent) {
        view = c as PaperSheetView
        c.layout = null
        if (c.background == null) c.background = VIEWPORT_BACKGROUND

        measurer = SwingFontMeasureCalculator()
        renderConfig = RenderConfiguration()
        painter = PaperSheetSwingPainter()
        selection = PaperSheetSelection(
            view = view,
            measurer = measurer,
            textIndex = { index },
            pageTops = { pageTops },
            scrollOffset = ::scrollOffset,
            requestRedraw = ::redraw,
            onProgrammaticChange = { dragging = false },
        )
        caret = PaperSheetCaret(
            view = view,
            selection = selection,
            measurer = measurer,
            textIndex = { index },
            measuredDocument = { measured },
            pageTops = { pageTops },
            scrollOffset = ::scrollOffset,
            requestRedraw = ::redraw,
            scrollCaretIntoView = ::scrollCaretIntoView,
        )
        // Clamped against a freshly computed maximum (not the possibly still-default `scrollBar`
        // maximum), since this may run before the first `relayoutViewport` pass has ever set it from
        // real content.
        scroll = PaperSheetScroll(
            view = view,
            textIndex = { index },
            measuredDocument = { measured },
            pageTops = { pageTops },
            scrollTo = { y ->
                val total = (contentHeightUnscaled + 2.0 * view.outerMargin) * view.zoom
                val maxValue = (total - viewportHeight()).coerceAtLeast(0.0)
                scrollBar.value = y.coerceIn(0.0, maxValue).roundToInt()
            },
        )
        editor = PaperSheetEditor(
            view = view,
            selection = selection,
            caret = caret,
            textIndex = { index },
            requestRedraw = ::redraw,
            markInternalEdit = { internalEdit = true },
        )
        hover = PaperSheetHoverTracker(
            view = view,
            measured = { measured },
            pageTops = { pageTops },
            scrollOffset = ::scrollOffset,
            nearestPage = ::nearestPage,
        )
        overlays = PaperSheetOverlays(view, selection, caret, hover, textIndex = { index })

        scrollBar = JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, 0).apply {
            addAdjustmentListener { redraw() }
        }
        c.add(scrollBar)
        c.add(overlays.layer)
        c.setComponentZOrder(overlays.layer, 0)

        view.registerSelectionCommands(selection)
        view.registerCaretCommands(caret)
        view.registerScrollCommands(scroll)
        installListeners()
        remeasure()
    }

    override fun uninstallUI(c: JComponent) {
        view.removePropertyChangeListener(propertyListener)
        view.removeMouseListener(mouseListener)
        view.removeMouseMotionListener(mouseListener)
        view.removeMouseWheelListener(mouseListener)
        view.removeKeyListener(keyListener)
        view.removeFocusListener(focusListener)
        view.removeComponentListener(componentListener)
        view.unregisterSelectionCommands(selection)
        view.unregisterCaretCommands(caret)
        view.unregisterScrollCommands(scroll)
        caret.dispose()
        overlays.dispose()
        c.remove(scrollBar)
        c.remove(overlays.layer)
        measured = null
        index = null
    }

    private fun installListeners() {
        propertyListener = PropertyChangeListener { e ->
            when (e.propertyName) {
                PaperSheetView.PROP_DOCUMENT -> remeasure()
                PaperSheetView.PROP_OUTER_MARGIN,
                PaperSheetView.PROP_PAGE_GAP,
                PaperSheetView.PROP_ZOOM,
                PaperSheetView.PROP_MIN_ZOOM,
                PaperSheetView.PROP_MAX_ZOOM,
                PaperSheetView.PROP_PAGE_MODES,
                -> relayout()
                PaperSheetView.PROP_MODE -> {
                    if (!selectable) selection.clearSelection()
                    caret.onModeChanged()
                    redraw()
                }
                PaperSheetView.PROP_SMOOTH_CARET_BLINK,
                PaperSheetView.PROP_CARET_MODE,
                -> caret.restartBlink()
                PaperSheetView.PROP_SHEET_BACKGROUND,
                PaperSheetView.PROP_SHEET_BORDER_COLOR,
                PaperSheetView.PROP_SHEET_BORDER_WIDTH,
                PaperSheetView.PROP_SHADOW_COLOR,
                PaperSheetView.PROP_SHADOW_OFFSET,
                PaperSheetView.PROP_SELECTION_COLOR,
                PaperSheetView.PROP_CARET_COLOR,
                PaperSheetView.PROP_DEACTIVATED_SHEET_BACKGROUND,
                PaperSheetView.PROP_DEACTIVATED_OVERLAY_COLOR,
                -> redraw()
            }
        }
        mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = onMousePressed(e)
            override fun mouseDragged(e: MouseEvent) = onMouseDragged(e)
            override fun mouseReleased(e: MouseEvent) = onMouseReleased(e)
            override fun mouseClicked(e: MouseEvent) = onMouseClicked(e)
            override fun mouseMoved(e: MouseEvent) {
                view.cursor = cursorFor(e.x.toDouble(), e.y.toDouble())
                hover.update(e.x.toDouble(), e.y.toDouble())
                overlays.refresh()
                fireMouseEvent(PaperSheetMouseEvent.Kind.HOVER, e.x.toDouble(), e.y.toDouble())
            }
            override fun mouseExited(e: MouseEvent) {
                view.cursor = Cursor.getDefaultCursor()
                hover.clear()
                overlays.refresh()
            }
            override fun mouseWheelMoved(e: MouseWheelEvent) = onScroll(e)
        }
        keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) = editor.onKeyPressed(e)
            override fun keyTyped(e: KeyEvent) = editor.onKeyTyped(e)
        }
        focusListener = object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) = caret.restartBlink()
            override fun focusLost(e: FocusEvent) = caret.restartBlink()
        }
        componentListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = relayoutViewport()
        }
        view.addPropertyChangeListener(propertyListener)
        view.addMouseListener(mouseListener)
        view.addMouseMotionListener(mouseListener)
        view.addMouseWheelListener(mouseListener)
        view.addKeyListener(keyListener)
        view.addFocusListener(focusListener)
        view.addComponentListener(componentListener)
    }

    //endregion

    //region measuring / layout

    private fun remeasure() {
        val reload = !internalEdit
        internalEdit = false
        // A page override is tied to a specific page instance; once the document is reloaded from
        // outside (as opposed to replaced by an edit), none of the previous overrides can still be
        // meaningful, so the default state - every page following the global mode - is restored.
        if (reload) view.pageModes = emptyMap()
        measured = view.document?.measure(measurer, renderConfig)
        index = measured?.let { DocumentTextIndex(it) }
        selection.onDocumentChanged(index)
        hover.clear()
        computeLayoutMetrics()
        selection.publish()
        caret.onDocumentRemeasured(reload)
        if (reload) scrollBar.value = 0
        redraw()
    }

    private fun relayout() {
        computeLayoutMetrics()
        redraw()
    }

    private fun computeLayoutMetrics() {
        val doc = measured
        val gap = view.pageGap
        val outer = view.outerMargin
        if (doc == null || doc.pages.isEmpty()) {
            pageTops = DoubleArray(0)
            contentWidthUnscaled = 0.0
            contentHeightUnscaled = 0.0
            view.updateContentSize(Dimension(0, 0))
            view.revalidate()
            relayoutViewport()
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
        view.updateContentSize(
            Dimension((contentWidthUnscaled + 2.0 * outer).toInt(), (contentHeightUnscaled + 2.0 * outer).toInt()),
        )
        view.revalidate()
        relayoutViewport()
    }

    private fun barWidth(): Int = scrollBar.preferredSize.width.coerceAtLeast(DEFAULT_SCROLLBAR_WIDTH)

    private fun viewportWidth(): Double = (view.width - barWidth()).coerceAtLeast(0).toDouble()

    private fun viewportHeight(): Double = view.height.coerceAtLeast(0).toDouble()

    private fun relayoutViewport() {
        val bw = barWidth()
        scrollBar.setBounds(view.width - bw, 0, bw, view.height)
        overlays.layout(0, 0, viewportWidth().toInt(), view.height)
        updateScrollBar(viewportHeight())
        redraw()
    }

    private fun updateScrollBar(viewportHeight: Double) {
        val total = ((contentHeightUnscaled + 2.0 * view.outerMargin) * view.zoom).toInt().coerceAtLeast(0)
        val extent = viewportHeight.toInt().coerceAtLeast(0)
        val maxValue = (total - extent).coerceAtLeast(0)
        scrollBar.setValues(scrollBar.value.coerceIn(0, maxValue), extent, 0, total)
        scrollBar.unitIncrement = LINE_SCROLL_STEP
        scrollBar.blockIncrement = extent
        scrollBar.isEnabled = total > extent
    }

    private fun scrollOffset(): Double = scrollBar.value.toDouble()

    /**
     * Scrolls the viewport by the smallest amount that brings the caret back into it, leaving
     * [CARET_SCROLL_PADDING] of slack above and below; does nothing while there is nothing to scroll
     * or the caret has no geometry.
     */
    private fun scrollCaretIntoView() {
        if (!scrollBar.isEnabled) return
        val height = viewportHeight().toInt()
        if (height <= 0) return
        val bounds = caret.viewportBounds() ?: return
        val top = bounds.y - CARET_SCROLL_PADDING
        val bottom = bounds.y + bounds.height + CARET_SCROLL_PADDING
        val delta = when {
            top < 0 -> top
            bottom > height -> bottom - height
            else -> return
        }
        val maxValue = (scrollBar.maximum - scrollBar.visibleAmount).coerceAtLeast(0)
        scrollBar.value = (scrollBar.value + delta).coerceIn(0, maxValue)
    }

    private fun currentStyle(): PaperSheetStyle {
        val selectionColor = if (
            !editable &&
            PaperSheetView.PROP_SELECTION_COLOR !in view.styleSetByUser
        ) {
            PaperSheetLookAndFeel.nonEditableSelectionColor()
        } else {
            view.selectionColor
        }
        return PaperSheetStyle(
            sheetBackground = view.sheetBackground,
            sheetBorderColor = view.sheetBorderColor,
            sheetBorderWidth = view.sheetBorderWidth,
            shadowColor = view.shadowColor,
            shadowOffset = view.shadowOffset,
            selectionColor = selectionColor,
            caretColor = view.caretColor,
            deactivatedSheetBackground = view.deactivatedSheetBackground,
            deactivatedOverlayColor = view.deactivatedOverlayColor,
        )
    }

    private fun redraw() {
        selection.publish()
        caret.publish()
        hover.publishOutputs()
        overlays.refresh()
        view.repaint()
    }

    //endregion

    //region painting / sizing

    override fun paint(g: Graphics, c: JComponent) {
        val g2 = g.create() as Graphics2D
        try {
            g2.color = c.background
            g2.fillRect(0, 0, c.width, c.height)
            g2.clipRect(0, 0, viewportWidth().toInt(), c.height)
            painter.paint(
                g = g2,
                viewportWidth = viewportWidth(),
                viewportHeight = viewportHeight(),
                measured = measured,
                pageTops = pageTops,
                zoom = view.zoom,
                outerMargin = view.outerMargin,
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
        } finally {
            g2.dispose()
        }
    }

    override fun getPreferredSize(c: JComponent): Dimension {
        val w = (view.contentSize.width * view.zoom).coerceIn(PREF_MIN, PREF_MAX)
        val h = (view.contentSize.height * view.zoom).coerceIn(PREF_MIN, PREF_MAX)
        return Dimension(w.toInt(), h.toInt())
    }

    //endregion

    //region hit testing / pointer

    private fun nearestPage(cyUnscaled: Double): Pair<Int, Double> {
        val doc = measured!!
        val outer = view.outerMargin
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
        if (!selectable) return Cursor.getDefaultCursor()
        val doc = measured ?: return Cursor.getDefaultCursor()
        if (doc.pages.isEmpty()) return Cursor.getDefaultCursor()
        val zoom = view.zoom
        val outer = view.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (pageIndex, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) return Cursor.getDefaultCursor()
        if (!pageMode(doc.pages[pageIndex]).supportsSelection) return Cursor.getDefaultCursor()
        val contentArea = doc.pages[pageIndex].contentArea
        val localX = cx - outer - contentArea.x
        val localY = cy - (outer + pageTops[pageIndex]) - contentArea.y
        return if (localX in 0.0..contentArea.width && localY in 0.0..contentArea.height) {
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
        } else {
            Cursor.getDefaultCursor()
        }
    }

    private fun hitIndexAt(px: Double, py: Double): Int {
        val doc = measured ?: return 0
        val idx = index ?: return 0
        if (doc.pages.isEmpty() || idx.segments.isEmpty()) return 0

        val zoom = view.zoom
        val outer = view.outerMargin
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

    /**
     * The raw part / block / page under viewport point ([px], [py]), or `null` for whichever of them
     * has no hit there - the part and block are `null` over an empty area of a page, all three are
     * `null` outside every page. Feeds [fireMouseEvent].
     */
    private fun resolveMouseHit(px: Double, py: Double): Triple<TextPart?, TextBlock?, Page?> {
        val doc = measured?.takeIf { it.pages.isNotEmpty() } ?: return Triple(null, null, null)
        val zoom = view.zoom
        val outer = view.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (pageIndex, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) return Triple(null, null, null)
        val page = doc.pages[pageIndex]
        val contentArea = page.contentArea
        val localX = cx - outer - contentArea.x
        val localY = cy - (outer + pageTops[pageIndex]) - contentArea.y
        val block = page.blocks.firstOrNull { b ->
            val bounds = b.bounds
            localX >= bounds.x && localX <= bounds.x + bounds.width && localY >= bounds.y && localY <= bounds.y + bounds.height
        } ?: return Triple(null, null, page.raw)
        val part = index?.segments?.filter { it.pageIndex == pageIndex && it.block === block }
            ?.minByOrNull { seg ->
                val bounds = seg.part.bounds
                when {
                    localX < bounds.x -> bounds.x - localX
                    localX > bounds.x + bounds.width -> localX - bounds.x - bounds.width
                    else -> 0.0
                }
            }?.part?.raw
        return Triple(part, block.raw, page.raw)
    }

    private fun fireMouseEvent(kind: PaperSheetMouseEvent.Kind, px: Double, py: Double) {
        val listener = view.onMouseEvent ?: return
        val (part, block, page) = resolveMouseHit(px, py)
        listener.handle(PaperSheetMouseEvent(view, kind, part, block, page))
    }

    //endregion

    //region input

    private fun onScroll(event: MouseWheelEvent) {
        if (!scrollBar.isEnabled) return
        val step = event.unitsToScroll * scrollBar.unitIncrement
        scrollBar.value = (scrollBar.value + step).coerceIn(0, scrollBar.maximum - scrollBar.visibleAmount)
        event.consume()
    }

    private fun onMousePressed(event: MouseEvent) {
        val doc = measured
        val hitPageMode = doc?.pages?.takeIf { it.isNotEmpty() }
            ?.let { pageMode(it[nearestPage((event.y + scrollOffset()) / view.zoom).first]) }
        if (hitPageMode?.supportsSelection != true) return
        view.requestFocusInWindow()
        caret.clearShiftAnchor()
        val i = hitIndexAt(event.x.toDouble(), event.y.toDouble())
        if (hitPageMode.supportsEditing && !selection.isEmpty && selection.contains(event.x.toDouble(), event.y.toDouble())) {
            draggingSelection = true
            dragging = false
            caret.setDropPreview(i)
            return
        }
        selection.beginAt(i)
        dragging = true
        if (hitPageMode.supportsCaret) caret.placeCaret(i) else redraw()
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (draggingSelection) {
            caret.setDropPreview(hitIndexAt(event.x.toDouble(), event.y.toDouble()))
            return
        }
        if (!dragging) return
        selection.dragTo(hitIndexAt(event.x.toDouble(), event.y.toDouble()))
        redraw()
    }

    private fun onMouseReleased(event: MouseEvent) {
        if (draggingSelection) {
            val target = hitIndexAt(event.x.toDouble(), event.y.toDouble())
            val copy = (event.modifiersEx and shortcutMask) == shortcutMask
            draggingSelection = false
            caret.setDropPreview(null)
            editor.dropSelection(target, copy)
            return
        }
        dragging = false
    }

    private fun onMouseClicked(event: MouseEvent) {
        fireMouseEvent(PaperSheetMouseEvent.Kind.CLICK, event.x.toDouble(), event.y.toDouble())
        if (event.clickCount != 2) return
        val doc = measured?.takeIf { it.pages.isNotEmpty() } ?: return
        val hitPageMode = pageMode(doc.pages[nearestPage((event.y + scrollOffset()) / view.zoom).first])
        if (!hitPageMode.supportsSelection) return
        selection.selectWordAt(hitIndexAt(event.x.toDouble(), event.y.toDouble()))
        if (hitPageMode.supportsCaret) caret.placeCaret(selection.end)
        redraw()
    }

    //endregion

    //region test hooks

    internal fun selectByPointsForTest(x0: Double, y0: Double, x1: Double, y1: Double) {
        selection.beginAt(hitIndexAt(x0, y0))
        selection.dragTo(hitIndexAt(x1, y1))
        redraw()
    }

    internal fun cursorAtForTest(x: Double, y: Double): Cursor = cursorFor(x, y)

    internal fun paintForTest(g: Graphics2D, width: Int, height: Int) {
        view.setSize(width, height)
        relayoutViewport()
        paint(g, view)
    }

    internal fun typeTextForTest(text: String) = editor.typeText(text)

    internal fun placeCaretAtForTest(x: Double, y: Double) = caret.placeCaret(hitIndexAt(x, y))

    internal fun pressKeyForTest(keyCode: Int, shift: Boolean = false, shortcut: Boolean = false) {
        var mods = 0
        if (shift) mods = mods or KeyEvent.SHIFT_DOWN_MASK
        if (shortcut) mods = mods or shortcutMask
        editor.onKeyPressed(
            KeyEvent(view, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), mods, keyCode, KeyEvent.CHAR_UNDEFINED),
        )
    }

    internal fun dragSelectionToForTest(x: Double, y: Double, copy: Boolean) =
        editor.dropSelection(hitIndexAt(x, y), copy)

    /** Fires a [PaperSheetMouseEvent] of [kind] at a viewport point, as the real listeners do; for tests. */
    internal fun fireMouseEventForTest(kind: PaperSheetMouseEvent.Kind, x: Double, y: Double) =
        fireMouseEvent(kind, x, y)

    internal fun hoverAtForTest(x: Double, y: Double) {
        hover.update(x, y)
        overlays.refresh()
    }

    internal fun clearHoverForTest() {
        hover.clear()
        overlays.refresh()
    }

    internal fun refreshOverlaysForTest() = overlays.refresh()

    internal val activeOverlaysForTest: Set<FloatingOverlay> get() = overlays.activeForTest
    internal val overlayNodeCountForTest: Int get() = overlays.nodeCountForTest
    internal val renderedPageIndicesForTest: List<Int> get() = painter.renderedPageIndices
    internal val sheetChromeDrawCountForTest: Int get() = painter.sheetChromeDrawCount
    internal val caretDrawCountForTest: Int get() = painter.caretDrawCount
    internal val paintCountForTest: Int get() = painter.paintCount
    internal val pageCountForTest: Int get() = measured?.pages?.size ?: 0
    internal val caretIndexForTest: Int get() = caret.position
    internal fun caretBoundsForTest(): Rectangle? = caret.viewportBounds()

    internal fun caretOpacityForTest(): Double = caret.currentOpacity()
    internal val verticalScrollBarForTest: JScrollBar get() = scrollBar

    //endregion

    companion object {

        private const val DEFAULT_SCROLLBAR_WIDTH = 14
        private const val LINE_SCROLL_STEP = 40

        /** Slack kept above and below the caret when scrolling it back into the viewport. */
        private const val CARET_SCROLL_PADDING = 8
        private const val PREF_MIN = 240.0
        private const val PREF_MAX = 2000.0

        private val VIEWPORT_BACKGROUND = Color(0xF2, 0xF2, 0xF2)
    }
}
