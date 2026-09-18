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

import javax.swing.JComponent
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.swing.PageDecoration
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.uicommon.EdgeReservation
import org.pcsoft.framework.simplay.uicommon.computePageDecorationReservation
import org.pcsoft.framework.simplay.uicommon.effectiveZoom
import org.pcsoft.framework.simplay.uicommon.resolveDecorationBounds
import kotlin.math.abs

/**
 * The decoration layer stacked on top of the viewport. Transparent and, like the overlay layer, only
 * "contains" a point where one of its child components actually sits, so a click on the empty part of
 * the viewport still reaches [PaperSheetView] for text selection.
 */
internal class DecorationLayer : JComponent() {
    init {
        isOpaque = false
        layout = null
        isFocusable = false
    }

    override fun contains(x: Int, y: Int): Boolean =
        components.any { it.isVisible && it.bounds.contains(x, y) }
}

/**
 * Drives the registered [PageDecoration]s of a [PaperSheetView]. Owns the decoration [layer] stacked
 * on top of the viewport, and on every [refresh] resolves each decoration's [PageDecoration.pageId]
 * against the current [MeasuredDocument], positions its component with [resolveDecorationBounds] and
 * shows or hides it accordingly. The Swing counterpart of the `fx` module's `PaperSheetDecorations`;
 * instead of a list-change listener the [refresh] reconciles against
 * [PaperSheetView.pageDecorations] on every pass. Unlike [PaperSheetOverlays], a decoration is not
 * tied to a trigger; it is shown for as long as its page resolves and is laid out, regardless of
 * [org.pcsoft.framework.simplay.swing.PaperSheetMode] / page mode. [reservation] reports the per-edge
 * extra space the currently measured decorations of a page (or of every page, with a `null` id) need,
 * so the delegate can grow its layout metrics accordingly; [onReservationChanged] is called at most
 * once per [refresh] pass, after every decoration has been measured, so the callback never re-enters
 * the still-running [refresh] loop.
 */
internal class PaperSheetDecorations(
    private val view: PaperSheetView,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val reservedMargins: () -> EdgeReservation = { EdgeReservation(view.outerMargin, view.outerMargin, view.outerMargin, view.outerMargin) },
    private val onReservationChanged: () -> Unit = {},
) {

    /** The decoration layer, to be added on top of the viewport by the delegate. */
    val layer: DecorationLayer = DecorationLayer()

    /** Decorations whose component currently sits in [layer]. */
    private val active = HashSet<PageDecoration>()

    /** The last measured natural size of each decoration's component; feeds [reservation]. */
    private val measuredSizes = HashMap<PageDecoration, Size>()

    /** Resizes and repositions the decoration layer; called from the delegate's viewport re-layout. */
    fun layout(x: Int, y: Int, width: Int, height: Int) {
        layer.setBounds(x, y, width, height)
    }

    /**
     * The per-edge extra space the currently measured decorations of [pageId] need (every page's, with
     * a `null` [pageId]), considering only decorations with [PageDecoration.reserveSpace] `true` (the
     * default); a decoration with `reserveSpace = false` stays a plain overlay and is excluded. Pages
     * stack vertically, so only a single page's TOP/BOTTOM decorations ever share a given inter-page gap
     * or the document's own top/bottom edge - the caller resolves [pageIndex] per boundary; LEFT/RIGHT
     * space is shared by the whole page stack, so the caller passes `null` for those. [pageIndex] is a
     * position in [MeasuredDocument.pages], not a raw page id: a `FlowPage` can spill across several
     * measured sheets that all share one raw id, but a decoration always resolves to only the first of
     * them (see [refresh]), so filtering by raw id here would wrongly attribute it to every sheet of
     * that raw page instead of just that one.
     */
    fun reservation(pageIndex: Int? = null): EdgeReservation {
        val document = if (pageIndex != null) measuredDocument() else null
        val entries = view.pageDecorations.mapNotNull { deco ->
            if (pageIndex != null) {
                val resolvedIndex = document?.pages?.indexOfFirst { it.raw.id == deco.pageId } ?: -1
                if (resolvedIndex != pageIndex) return@mapNotNull null
            }
            val component = deco.content ?: return@mapNotNull null
            val size = measuredSizes[deco] ?: return@mapNotNull null
            Triple(deco.edge, size, deco.reserveSpace)
        }
        return computePageDecorationReservation(entries)
    }

    /** Recomputes every registered decoration's visibility and position. */
    fun refresh() {
        val decorations = view.pageDecorations
        var changed = false
        active.toList().forEach { if (it !in decorations && detach(it)) changed = true }
        if (decorations.isEmpty() && active.isEmpty()) return

        val document = measuredDocument()
        for (decoration in decorations) {
            val component = decoration.content
            val pageIndex = document?.pages?.indexOfFirst { it.raw.id == decoration.pageId } ?: -1
            if (component == null || pageIndex < 0 || !view.effectivePageMode(decoration.pageId).laidOut) {
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

            if (component.parent !== layer) layer.add(component)
            val preferred = component.preferredSize
            val decorationSize = Size(
                width = if (preferred.width > 0) preferred.width.toDouble() else component.width.toDouble(),
                height = if (preferred.height > 0) preferred.height.toDouble() else component.height.toDouble(),
            )
            val previous = measuredSizes[decoration]
            measuredSizes[decoration] = decorationSize
            if (previous == null || abs(previous.width - decorationSize.width) > 0.5 || abs(previous.height - decorationSize.height) > 0.5) {
                changed = true
            }
            val bounds = resolveDecorationBounds(pageBounds, decoration.placement, decorationSize, view.zoom)
            component.setBounds(
                bounds.x.toInt(), bounds.y.toInt(), bounds.width.toInt().coerceAtLeast(1), bounds.height.toInt().coerceAtLeast(1),
            )
            active.add(decoration)
        }
        layer.revalidate()
        layer.repaint()
        if (changed) onReservationChanged()
    }

    /** Detaches every decoration; called from the delegate's `uninstallUI`. */
    fun dispose() {
        active.toList().forEach { detach(it) }
    }

    /** Detaches [decoration]'s component from [layer]; returns whether a cached measured size was removed. */
    private fun detach(decoration: PageDecoration): Boolean {
        decoration.content?.let { if (it.parent === layer) layer.remove(it) }
        active.remove(decoration)
        val removed = measuredSizes.remove(decoration) != null
        layer.repaint()
        return removed
    }

    //region Test hooks

    internal val activeForTest: Set<PageDecoration> get() = active.toSet()

    internal val nodeCountForTest: Int get() = layer.componentCount

    //endregion
}
