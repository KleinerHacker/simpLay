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
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JScrollBar
import org.pcsoft.framework.simplay.engine.RenderConfiguration
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetStyle
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetSelection
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetSwingPainter
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.hitTest

/**
 * The default [PaperSheetUI]: owns the measuring, the vertical [JScrollBar], the pointer / mouse
 * routing and the viewport painting (delegated to [PaperSheetSwingPainter], which draws only the
 * pages currently in the viewport - simple page virtualisation - scaled by [PaperSheetView.zoom]).
 * The Swing counterpart of the `fx` module's `PaperSheetViewSkin`.
 *
 * This first cut covers the read-only behaviour: sheet chrome, mouse text selection, double-click
 * word selection, wheel scrolling, the text pointer and `Ctrl+C` / `Cmd+C` styled copy. The caret,
 * the editor shortcuts, the hover tracker and the floating overlays are wired by the later
 * implementation steps.
 */
open class BasicPaperSheetUI : PaperSheetUI() {

    private lateinit var view: PaperSheetView
    private lateinit var measurer: SwingFontMeasureCalculator
    private lateinit var renderConfig: RenderConfiguration
    private lateinit var painter: PaperSheetSwingPainter
    private lateinit var selection: PaperSheetSelection
    private lateinit var scrollBar: JScrollBar

    private var measured: MeasuredDocument? = null
    private var index: DocumentTextIndex? = null
    private var pageTops: DoubleArray = DoubleArray(0)
    private var contentWidthUnscaled = 0.0
    private var contentHeightUnscaled = 0.0

    private var dragging = false

    private lateinit var propertyListener: PropertyChangeListener
    private lateinit var mouseListener: MouseAdapter
    private lateinit var keyListener: KeyAdapter
    private lateinit var componentListener: ComponentAdapter

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

        scrollBar = JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, 0).apply {
            addAdjustmentListener { redraw() }
        }
        c.add(scrollBar)

        view.registerSelectionCommands(selection)
        installListeners()
        remeasure()
    }

    override fun uninstallUI(c: JComponent) {
        view.removePropertyChangeListener(propertyListener)
        view.removeMouseListener(mouseListener)
        view.removeMouseMotionListener(mouseListener)
        view.removeMouseWheelListener(mouseListener)
        view.removeKeyListener(keyListener)
        view.removeComponentListener(componentListener)
        view.unregisterSelectionCommands(selection)
        c.remove(scrollBar)
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
                -> relayout()
                PaperSheetView.PROP_MODE,
                PaperSheetView.PROP_SHEET_BACKGROUND,
                PaperSheetView.PROP_SHEET_BORDER_COLOR,
                PaperSheetView.PROP_SHEET_BORDER_WIDTH,
                PaperSheetView.PROP_SHADOW_COLOR,
                PaperSheetView.PROP_SHADOW_OFFSET,
                PaperSheetView.PROP_SELECTION_COLOR,
                PaperSheetView.PROP_CARET_COLOR,
                -> redraw()
            }
        }
        mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = onMousePressed(e)
            override fun mouseDragged(e: MouseEvent) = onMouseDragged(e)
            override fun mouseReleased(e: MouseEvent) { dragging = false }
            override fun mouseClicked(e: MouseEvent) = onMouseClicked(e)
            override fun mouseMoved(e: MouseEvent) { view.cursor = cursorFor(e.x.toDouble(), e.y.toDouble()) }
            override fun mouseExited(e: MouseEvent) { view.cursor = Cursor.getDefaultCursor() }
            override fun mouseWheelMoved(e: MouseWheelEvent) = onScroll(e)
        }
        keyListener = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val shortcut = e.isControlDown || e.isMetaDown
                if (shortcut && e.keyCode == KeyEvent.VK_C) {
                    selection.putStyledSelectionOnClipboard()
                    e.consume()
                }
            }
        }
        componentListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = relayoutViewport()
        }
        view.addPropertyChangeListener(propertyListener)
        view.addMouseListener(mouseListener)
        view.addMouseMotionListener(mouseListener)
        view.addMouseWheelListener(mouseListener)
        view.addKeyListener(keyListener)
        view.addComponentListener(componentListener)
    }

    //endregion

    //region measuring / layout

    private fun remeasure() {
        measured = view.document?.measure(measurer, renderConfig)
        index = measured?.let { DocumentTextIndex(it) }
        selection.onDocumentChanged(index)
        computeLayoutMetrics()
        val idx = index
        view.caretModel.updateCounts(idx?.blockCount ?: 0, idx?.wordCount ?: 0, idx?.symbolCount ?: 0)
        selection.publish()
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
            y += page.effectiveSize.height
            if (i != doc.pages.lastIndex) y += gap
        }
        contentHeightUnscaled = y
        contentWidthUnscaled = doc.pages.maxOf { it.effectiveSize.width }
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

    private fun currentStyle(): PaperSheetStyle {
        val selectionColor = if (
            view.mode == PaperSheetMode.READONLY &&
            PaperSheetView.PROP_SELECTION_COLOR !in view.styleSetByUser
        ) {
            PaperSheetStyle.DEFAULT_SELECTION_COLOR_READONLY
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
        )
    }

    private fun redraw() {
        selection.publish()
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

    private fun cursorFor(px: Double, py: Double): Cursor {
        val doc = measured ?: return Cursor.getDefaultCursor()
        if (doc.pages.isEmpty()) return Cursor.getDefaultCursor()
        val zoom = view.zoom
        val outer = view.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (pageIndex, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) return Cursor.getDefaultCursor()
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

    //endregion

    //region input

    private fun onScroll(event: MouseWheelEvent) {
        if (!scrollBar.isEnabled) return
        val step = event.unitsToScroll * scrollBar.unitIncrement
        scrollBar.value = (scrollBar.value + step).coerceIn(0, scrollBar.maximum - scrollBar.visibleAmount)
        event.consume()
    }

    private fun onMousePressed(event: MouseEvent) {
        view.requestFocusInWindow()
        val i = hitIndexAt(event.x.toDouble(), event.y.toDouble())
        selection.beginAt(i)
        dragging = true
        redraw()
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (!dragging) return
        selection.dragTo(hitIndexAt(event.x.toDouble(), event.y.toDouble()))
        redraw()
    }

    private fun onMouseClicked(event: MouseEvent) {
        if (event.clickCount != 2) return
        selection.selectWordAt(hitIndexAt(event.x.toDouble(), event.y.toDouble()))
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

    internal val renderedPageIndicesForTest: List<Int> get() = painter.renderedPageIndices
    internal val sheetChromeDrawCountForTest: Int get() = painter.sheetChromeDrawCount
    internal val paintCountForTest: Int get() = painter.paintCount
    internal val pageCountForTest: Int get() = measured?.pages?.size ?: 0
    internal val verticalScrollBarForTest: JScrollBar get() = scrollBar

    //endregion

    companion object {

        private const val DEFAULT_SCROLLBAR_WIDTH = 14
        private const val LINE_SCROLL_STEP = 40
        private const val PREF_MIN = 240.0
        private const val PREF_MAX = 2000.0

        private val VIEWPORT_BACKGROUND = Color(0xF2, 0xF2, 0xF2)
    }
}
