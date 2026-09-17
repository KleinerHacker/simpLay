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

import javafx.collections.ListChangeListener
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.fx.PageDecoration
import org.pcsoft.framework.simplay.fx.PaperSheetView
import org.pcsoft.framework.simplay.uicommon.effectiveZoom
import org.pcsoft.framework.simplay.uicommon.resolveDecorationBounds

/**
 * Drives the registered [PageDecoration]s of a [PaperSheetView]. Owns the decoration [layer] stacked
 * on top of the viewport, watches [PaperSheetView.getPageDecorations], and on every [refresh] resolves
 * each decoration's [PageDecoration.pageId] against the current [MeasuredDocument], positions its node
 * with [resolveDecorationBounds] and shows or hides it accordingly. Unlike [PaperSheetOverlays], a
 * decoration is not tied to a trigger; it is shown for as long as its page resolves and is laid out,
 * regardless of [org.pcsoft.framework.simplay.fx.PaperSheetMode] / page mode.
 *
 * A per-view helper: [org.pcsoft.framework.simplay.fx.PaperSheetViewSkin] creates it, adds [layer] to
 * its children, sizes it through [layout] on every layout pass, calls [refresh] whenever the geometry
 * may have changed and [dispose]s it when the skin is disposed.
 */
internal class PaperSheetDecorations(
    private val view: PaperSheetView,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
) {

    private val clipRect = Rectangle()

    /** The decoration layer, to be added on top of the viewport canvas by the skin. */
    val layer: Pane = Pane().apply {
        isManaged = false
        isPickOnBounds = false
        clip = clipRect
        styleClass.add("page-decoration-layer")
    }

    /** Decorations whose node currently sits in [layer]. */
    private val active = HashSet<PageDecoration>()

    private val listener = ListChangeListener<PageDecoration> { change ->
        while (change.next()) change.removed.forEach { detach(it) }
        refresh()
    }

    init {
        view.pageDecorations.addListener(listener)
    }

    /** Resizes and repositions the decoration layer; called from the skin's `layoutChildren`. */
    fun layout(x: Double, y: Double, width: Double, height: Double) {
        layer.resizeRelocate(x, y, width, height)
        clipRect.width = width
        clipRect.height = height
    }

    /** Recomputes every registered decoration's visibility and position. */
    fun refresh() {
        val decorations = view.pageDecorations
        if (decorations.isEmpty() && active.isEmpty()) return

        val document = measuredDocument()
        for (decoration in decorations) {
            val node = decoration.content
            val pageIndex = document?.pages?.indexOfFirst { it.raw.id == decoration.pageId } ?: -1
            if (node == null || pageIndex < 0 || !view.effectivePageMode(decoration.pageId).laidOut) {
                detach(decoration)
                continue
            }

            val page = document!!.pages[pageIndex]
            val zoom = effectiveZoom(view.zoom)
            val pageBounds = Rect(
                x = view.outerMargin * zoom,
                y = (view.outerMargin + pageTops()[pageIndex]) * zoom - scrollOffset(),
                width = page.effectiveSize.width * zoom,
                height = page.effectiveSize.height * zoom,
            )

            if (node !in layer.children) layer.children.add(node)
            node.applyCss()
            node.autosize()
            val decorationSize = Size(
                width = node.layoutBounds.width.takeIf { it > 0.0 } ?: node.prefWidth(-1.0),
                height = node.layoutBounds.height.takeIf { it > 0.0 } ?: node.prefHeight(-1.0),
            )
            val bounds = resolveDecorationBounds(pageBounds, decoration.placement, decorationSize, view.zoom)
            node.resizeRelocate(bounds.x, bounds.y, bounds.width, bounds.height)
            active.add(decoration)
        }
    }

    /** Stops watching the decoration list and detaches every node; called from the skin's `dispose`. */
    fun dispose() {
        view.pageDecorations.removeListener(listener)
        active.toList().forEach { detach(it) }
    }

    private fun detach(decoration: PageDecoration) {
        decoration.content?.let { layer.children.remove(it) }
        active.remove(decoration)
    }

    //region Test hooks

    /** The decorations whose node currently sits in the layer; for tests. */
    internal val activeForTest: Set<PageDecoration> get() = active.toSet()

    /** Number of nodes currently in the layer; for tests. */
    internal val nodeCountForTest: Int get() = layer.children.size

    //endregion
}
