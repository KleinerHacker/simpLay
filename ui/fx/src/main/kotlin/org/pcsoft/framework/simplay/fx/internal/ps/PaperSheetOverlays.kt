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

package org.pcsoft.framework.simplay.fx.internal.ps

import org.pcsoft.framework.simplay.fx.FloatingOverlay
import org.pcsoft.framework.simplay.fx.FloatingOverlayTrigger
import org.pcsoft.framework.simplay.fx.PaperSheetMode
import org.pcsoft.framework.simplay.fx.PaperSheetView

import javafx.collections.ListChangeListener
import javafx.geometry.Bounds
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode

/**
 * The anchor box and context a satisfied [FloatingOverlayTrigger] hands to [PaperSheetOverlays]:
 * [bounds] in viewport pixels, a trigger-specific [index] (selection start, paragraph ordinal, page
 * index, caret index), the trigger [text] and, for [FloatingOverlayTrigger.SELECTION] /
 * [FloatingOverlayTrigger.CARET], the covered [range].
 */
internal class TriggerGeometry(val bounds: Bounds, val index: Int, val text: String, val range: IntRange?)

/**
 * Drives the registered [FloatingOverlay]s of a [PaperSheetView]. Owns the overlay [layer] stacked on
 * top of the viewport, watches [PaperSheetView.getFloatingOverlays], and on every [refresh] shows,
 * positions (anchor + offsets, clamped to the viewport edge) and hides each overlay's node according
 * to its [FloatingOverlay.trigger] - a text selection ([PaperSheetSelection]), the hovered paragraph
 * or sheet ([PaperSheetHoverTracker]) or, in a mode with [PaperSheetMode.supportsCaret], the caret
 * ([PaperSheetCaret]). [FloatingOverlay.onShown] / [FloatingOverlay.onHidden] fire on the show / hide
 * transition only, and carry whether the page the trigger sits on is currently deactivated.
 *
 * No overlay is shown at all in [PaperSheetMode.STATIC], nor for a trigger sitting on a page locked
 * by [PageDeactivationMode.DISABLED].
 *
 * A per-view helper: [PaperSheetViewSkin] creates it, adds [layer] to its children, sizes it through
 * [layout] on every layout pass, calls [refresh] whenever the geometry may have changed and
 * [dispose]s it when the skin is disposed.
 */
internal class PaperSheetOverlays(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val caret: PaperSheetCaret,
    private val hover: PaperSheetHoverTracker,
    private val textIndex: () -> DocumentTextIndex?,
) {

    private val clipRect = Rectangle()

    /** The overlay layer, to be added on top of the viewport canvas by the skin. */
    val layer: Pane = Pane().apply {
        isManaged = false
        isPickOnBounds = false
        clip = clipRect
    }

    private var viewportWidth = 0.0
    private var viewportHeight = 0.0

    /** Overlays whose node currently sits in [layer]. */
    private val active = HashSet<FloatingOverlay>()

    private val listener = ListChangeListener<FloatingOverlay> { change ->
        while (change.next()) change.removed.forEach { detach(it, fireEvent = false) }
        refresh()
    }

    init {
        view.floatingOverlays.addListener(listener)
    }

    /** Resizes and repositions the overlay layer; called from the skin's `layoutChildren`. */
    fun layout(x: Double, y: Double, width: Double, height: Double) {
        layer.resizeRelocate(x, y, width, height)
        clipRect.width = width
        clipRect.height = height
        viewportWidth = width
        viewportHeight = height
    }

    /** Recomputes every registered overlay's visibility and position. */
    fun refresh() {
        val overlays = view.floatingOverlays
        if (overlays.isEmpty() && active.isEmpty()) return
        if (view.mode == PaperSheetMode.STATIC) {
            active.toList().forEach { detach(it, fireEvent = true) }
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
            if (node !in layer.children) layer.children.add(node)
            node.applyCss()
            node.autosize()
            val placed = placeInViewport(node, geometry.bounds, overlay)
            if (placed == null) {
                detach(overlay, fireEvent = true)
                continue
            }
            node.relocate(placed.first, placed.second)
            overlay.updateActiveState(
                geometry.bounds, geometry.index, geometry.text, geometry.range, isPageDeactivated(pageIndex),
            )
            if (active.add(overlay)) overlay.fireShown(overlay.trigger)
        }
    }

    /** Stops watching the overlay list; called from the skin's `dispose`. */
    fun dispose() {
        view.floatingOverlays.removeListener(listener)
    }

    private fun geometryFor(kind: FloatingOverlayTrigger): TriggerGeometry? = when (kind) {
        FloatingOverlayTrigger.SELECTION -> {
            val bounds = selection.viewportBounds()
            if (bounds == null) null
            else TriggerGeometry(bounds, selection.start, selection.text, selection.range)
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
        overlay.content?.let { layer.children.remove(it) }
        val wasActive = active.remove(overlay)
        overlay.clearActiveState()
        if (wasActive && fireEvent) overlay.fireHidden(overlay.trigger)
    }

    /**
     * Places [node] relative to the trigger [bounds] per the overlay's [FloatingOverlay.anchor] plus
     * its offsets, then clamps the result to the viewport. Returns `null` when [bounds] no longer
     * intersects the viewport at all, so the overlay must be hidden.
     */
    private fun placeInViewport(node: Node, bounds: Bounds, overlay: FloatingOverlay): Pair<Double, Double>? {
        val vpW = viewportWidth
        val vpH = viewportHeight
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

    //region Test hooks

    /** The overlays whose node currently sits in the layer; for tests. */
    internal val activeForTest: Set<FloatingOverlay> get() = active.toSet()

    /** Number of nodes currently in the layer; for tests. */
    internal val nodeCountForTest: Int get() = layer.children.size

    //endregion
}
