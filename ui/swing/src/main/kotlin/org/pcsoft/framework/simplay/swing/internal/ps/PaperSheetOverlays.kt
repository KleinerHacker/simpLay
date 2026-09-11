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

package org.pcsoft.framework.simplay.swing.internal.ps

import java.awt.Rectangle
import javax.swing.JComponent
import kotlin.math.roundToInt
import org.pcsoft.framework.simplay.swing.FloatingOverlay
import org.pcsoft.framework.simplay.swing.FloatingOverlayTrigger
import org.pcsoft.framework.simplay.swing.OverlayAnchor
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode

/**
 * The anchor box and context a satisfied [FloatingOverlayTrigger] hands to [PaperSheetOverlays]:
 * [bounds] in viewport pixels, a trigger-specific [index], the trigger [text] and, for
 * [FloatingOverlayTrigger.SELECTION] / [FloatingOverlayTrigger.CARET], the covered [range].
 */
internal class TriggerGeometry(val bounds: Rectangle, val index: Int, val text: String, val range: IntRange?)

/**
 * The overlay layer stacked on top of the viewport. It is transparent and, crucially, only
 * "contains" a point where one of its child components actually sits, so a click on the empty part
 * of the viewport still reaches [PaperSheetView] for text selection.
 */
internal class OverlayLayer : JComponent() {
    init {
        isOpaque = false
        layout = null
        isFocusable = false
    }

    override fun contains(x: Int, y: Int): Boolean =
        components.any { it.isVisible && it.bounds.contains(x, y) }
}

/**
 * Drives the registered [FloatingOverlay]s of a [PaperSheetView]. Owns the overlay [layer] stacked on
 * top of the viewport, and on every [refresh] shows, positions (anchor + offsets, clamped to the
 * viewport edge) and hides each overlay's component according to its [FloatingOverlay.trigger]. The
 * Swing counterpart of the `fx` module's `PaperSheetOverlays`; instead of a list-change listener the
 * [refresh] reconciles against [PaperSheetView.floatingOverlays] on every pass. Also carries whether
 * the page the trigger sits on is currently deactivated.
 *
 * No overlay is shown at all in [PaperSheetMode.STATIC], nor for a trigger sitting on a page locked
 * by [PageDeactivationMode.DISABLED].
 */
internal class PaperSheetOverlays(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val caret: PaperSheetCaret,
    private val hover: PaperSheetHoverTracker,
    private val textIndex: () -> DocumentTextIndex?,
) {

    /** The overlay layer, to be added on top of the viewport by the delegate. */
    val layer: OverlayLayer = OverlayLayer()

    private var viewportWidth = 0
    private var viewportHeight = 0

    /** Overlays whose component currently sits in [layer]. */
    private val active = HashSet<FloatingOverlay>()

    /** Resizes and repositions the overlay layer; called from the delegate's viewport re-layout. */
    fun layout(x: Int, y: Int, width: Int, height: Int) {
        layer.setBounds(x, y, width, height)
        viewportWidth = width
        viewportHeight = height
    }

    /** Recomputes every registered overlay's visibility and position. */
    fun refresh() {
        val overlays = view.floatingOverlays
        // Detach overlays that were removed from the list.
        active.toList().forEach { if (it !in overlays) detach(it, fireEvent = true) }
        if (overlays.isEmpty() && active.isEmpty()) return

        if (view.mode == PaperSheetMode.STATIC) {
            active.toList().forEach { detach(it, fireEvent = true) }
            layer.revalidate()
            layer.repaint()
            return
        }

        for (overlay in overlays) {
            val geometry = geometryFor(overlay.trigger)
            val node = overlay.content
            if (geometry == null || node == null) {
                if (overlay in active && overlay.autoHide) detach(overlay, fireEvent = true)
                continue
            }
            val pageIndex = pageIndexForTrigger(overlay.trigger, geometry)
            if (isPageDisabled(pageIndex)) {
                detach(overlay, fireEvent = true)
                continue
            }
            if (node.parent !== layer) layer.add(node)
            val size = node.preferredSize
            if (size.width > 0 && size.height > 0) node.setSize(size)
            val placed = placeInViewport(node, geometry.bounds, overlay)
            if (placed == null) {
                detach(overlay, fireEvent = true)
                continue
            }
            node.setBounds(placed.first, placed.second, node.width.coerceAtLeast(1), node.height.coerceAtLeast(1))
            overlay.updateActiveState(
                geometry.bounds, geometry.index, geometry.text, geometry.range, isPageDeactivated(pageIndex),
            )
            if (active.add(overlay)) overlay.fireShown(overlay.trigger)
        }
        layer.revalidate()
        layer.repaint()
    }

    /** Detaches every overlay; called from the delegate's `uninstallUI`. */
    fun dispose() {
        active.toList().forEach { detach(it, fireEvent = false) }
    }

    private fun geometryFor(kind: FloatingOverlayTrigger): TriggerGeometry? = when (kind) {
        FloatingOverlayTrigger.SELECTION -> {
            val bounds = selection.viewportBounds()
            if (bounds == null) null else TriggerGeometry(bounds, selection.start, selection.text, selection.range)
        }
        FloatingOverlayTrigger.PARAGRAPH_HOVER -> hover.paragraphGeometry()
        FloatingOverlayTrigger.PAGE_HOVER -> hover.pageGeometry()
        FloatingOverlayTrigger.CARET -> {
            if (!view.mode.supportsCaret) {
                null
            } else {
                val bounds = caret.viewportBounds()
                if (bounds == null) null
                else TriggerGeometry(bounds, caret.position, "", caret.position until caret.position)
            }
        }
    }

    /** The measured-page index the trigger [kind] sits on, or `-1` when it cannot be determined. */
    private fun pageIndexForTrigger(kind: FloatingOverlayTrigger, geometry: TriggerGeometry): Int = when (kind) {
        FloatingOverlayTrigger.SELECTION, FloatingOverlayTrigger.CARET -> textIndex()?.pageIndexAt(geometry.index) ?: -1
        FloatingOverlayTrigger.PARAGRAPH_HOVER, FloatingOverlayTrigger.PAGE_HOVER -> view.hoveredPage
    }

    /** Whether the raw page at [pageIndex] is currently deactivated per the view's state. */
    private fun isPageDeactivated(pageIndex: Int): Boolean {
        if (pageIndex < 0) return false
        if (view.deactivatedPageHandling == PageDeactivationMode.IGNORE) return false
        val id = view.document?.pages?.getOrNull(pageIndex)?.id ?: return false
        return id in view.deactivatedPageIds
    }

    /** Whether the raw page at [pageIndex] is locked by [PageDeactivationMode.DISABLED]. */
    private fun isPageDisabled(pageIndex: Int): Boolean =
        view.deactivatedPageHandling == PageDeactivationMode.DISABLED && isPageDeactivated(pageIndex)

    private fun detach(overlay: FloatingOverlay, fireEvent: Boolean) {
        overlay.content?.let { if (it.parent === layer) layer.remove(it) }
        val wasActive = active.remove(overlay)
        overlay.clearActiveState()
        if (wasActive && fireEvent) overlay.fireHidden(overlay.trigger)
        layer.repaint()
    }

    /**
     * Places [node] relative to the trigger [bounds] per the overlay's [FloatingOverlay.anchor] plus
     * its offsets, then clamps the result to the viewport. Returns `null` when [bounds] no longer
     * intersects the viewport at all, so the overlay must be hidden.
     */
    private fun placeInViewport(node: JComponent, bounds: Rectangle, overlay: FloatingOverlay): Pair<Int, Int>? {
        val vpW = viewportWidth
        val vpH = viewportHeight
        val minX = bounds.x
        val maxX = bounds.x + bounds.width
        val minY = bounds.y
        val maxY = bounds.y + bounds.height
        if (maxX <= 0 || minX >= vpW || maxY <= 0 || minY >= vpH) return null

        val w = node.width.takeIf { it > 0 } ?: node.preferredSize.width
        val h = node.height.takeIf { it > 0 } ?: node.preferredSize.height
        var nx = when (overlay.anchor.hpos) {
            OverlayAnchor.Horizontal.LEFT -> minX.toDouble()
            OverlayAnchor.Horizontal.CENTER -> minX + bounds.width / 2.0 - w / 2.0
            OverlayAnchor.Horizontal.RIGHT -> (maxX - w).toDouble()
        } + overlay.offsetX
        var ny = when (overlay.anchor.vpos) {
            OverlayAnchor.Vertical.TOP -> (minY - h).toDouble()
            OverlayAnchor.Vertical.CENTER -> minY + bounds.height / 2.0 - h / 2.0
            OverlayAnchor.Vertical.BOTTOM -> maxY.toDouble()
        } + overlay.offsetY
        nx = nx.coerceIn(0.0, (vpW - w).coerceAtLeast(0).toDouble())
        ny = ny.coerceIn(0.0, (vpH - h).coerceAtLeast(0).toDouble())
        return nx.roundToInt() to ny.roundToInt()
    }

    //region Test hooks

    internal val activeForTest: Set<FloatingOverlay> get() = active.toSet()

    internal val nodeCountForTest: Int get() = layer.componentCount

    //endregion
}
