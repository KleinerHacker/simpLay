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
import org.pcsoft.framework.simplay.uicommon.EdgeReservation
import org.pcsoft.framework.simplay.uicommon.computePageDecorationReservation
import org.pcsoft.framework.simplay.uicommon.effectiveZoom
import org.pcsoft.framework.simplay.uicommon.resolveDecorationBounds
import kotlin.math.abs

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
 * may have changed and [dispose]s it when the skin is disposed. [reservation] reports the per-edge
 * extra space the currently measured decorations of a page (or of every page, with a `null` id) need,
 * so the skin can grow its layout metrics accordingly; [onReservationChanged] is called at most once per
 * [refresh] pass, after every decoration has been measured, whenever a size change may affect that
 * reservation, so the skin can trigger a relayout without the callback re-entering the still-running
 * [refresh] loop.
 */
internal class PaperSheetDecorations(
    private val view: PaperSheetView,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val reservedMargins: () -> EdgeReservation = { EdgeReservation(view.outerMargin, view.outerMargin, view.outerMargin, view.outerMargin) },
    private val onReservationChanged: () -> Unit = {},
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

    /** The last measured natural size of each decoration's node; feeds [reservation]. */
    private val measuredSizes = HashMap<PageDecoration, Size>()

    private val listener = ListChangeListener<PageDecoration> { change ->
        var changed = false
        while (change.next()) change.removed.forEach { if (detach(it)) changed = true }
        refresh()
        if (changed) onReservationChanged()
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

    /**
     * The per-edge extra space the currently measured decorations of [pageId] need (every page's, with
     * a `null` [pageId]), considering only decorations whose node opts into reservation via JavaFX's own
     * `Node.isManaged` (default `true`); a decoration whose node was explicitly set `isManaged = false`
     * by the caller stays a plain overlay and is excluded. [PaperSheetView.outerMargin] stacks pages
     * vertically, so only a single page's TOP/BOTTOM decorations ever share a given inter-page gap or
     * the document's own top/bottom edge - the caller resolves [pageIndex] per boundary; LEFT/RIGHT
     * space is shared by the whole page stack, so the caller passes `null` for those. [pageIndex] is a
     * position in [MeasuredDocument.pages], not a raw page id: a [FlowPage][org.pcsoft.framework.simplay.engine.model.FlowPage]
     * can spill across several measured sheets that all share one raw id, but a decoration always
     * resolves to only the first of them (see [refresh]), so filtering by raw id here would wrongly
     * attribute it to every sheet of that raw page instead of just that one.
     */
    fun reservation(pageIndex: Int? = null): EdgeReservation {
        val document = if (pageIndex != null) measuredDocument() else null
        val entries = view.pageDecorations.mapNotNull { deco ->
            if (pageIndex != null) {
                val resolvedIndex = document?.pages?.indexOfFirst { it.raw.id == deco.pageId } ?: -1
                if (resolvedIndex != pageIndex) return@mapNotNull null
            }
            val node = deco.content ?: return@mapNotNull null
            val size = measuredSizes[deco] ?: return@mapNotNull null
            Triple(deco.edge, size, node.isManaged)
        }
        return computePageDecorationReservation(entries)
    }

    /** Recomputes every registered decoration's visibility and position. */
    fun refresh() {
        val decorations = view.pageDecorations
        if (decorations.isEmpty() && active.isEmpty()) return

        var changed = false
        val document = measuredDocument()
        for (decoration in decorations) {
            val node = decoration.content
            val pageIndex = document?.pages?.indexOfFirst { it.raw.id == decoration.pageId } ?: -1
            if (node == null || pageIndex < 0 || !view.effectivePageMode(decoration.pageId).laidOut) {
                if (detach(decoration)) changed = true
                continue
            }

            val page = document!!.pages[pageIndex]
            val zoom = effectiveZoom(view.zoom)
            val margins = reservedMargins()
            val pageBounds = Rect(
                x = margins.left * zoom,
                y = (margins.top + pageTops()[pageIndex]) * zoom - scrollOffset(),
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
            val previous = measuredSizes[decoration]
            measuredSizes[decoration] = decorationSize
            if (previous == null || abs(previous.width - decorationSize.width) > 0.5 || abs(previous.height - decorationSize.height) > 0.5) {
                changed = true
            }
            val bounds = resolveDecorationBounds(pageBounds, decoration.placement, decorationSize, view.zoom)
            node.resizeRelocate(bounds.x, bounds.y, bounds.width, bounds.height)
            active.add(decoration)
        }
        if (changed) onReservationChanged()
    }

    /** Stops watching the decoration list and detaches every node; called from the skin's `dispose`. */
    fun dispose() {
        view.pageDecorations.removeListener(listener)
        active.toList().forEach { detach(it) }
    }

    /** Detaches [decoration]'s node from [layer]; returns whether a cached measured size was removed. */
    private fun detach(decoration: PageDecoration): Boolean {
        decoration.content?.let { layer.children.remove(it) }
        active.remove(decoration)
        return measuredSizes.remove(decoration) != null
    }

    //region Test hooks

    /** The decorations whose node currently sits in the layer; for tests. */
    internal val activeForTest: Set<PageDecoration> get() = active.toSet()

    /** Number of nodes currently in the layer; for tests. */
    internal val nodeCountForTest: Int get() = layer.children.size

    //endregion
}
