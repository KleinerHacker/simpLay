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
import org.pcsoft.framework.simplay.uicommon.effectiveZoom
import org.pcsoft.framework.simplay.uicommon.resolveDecorationBounds

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
 * [org.pcsoft.framework.simplay.swing.PaperSheetMode] / page mode.
 */
internal class PaperSheetDecorations(
    private val view: PaperSheetView,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
) {

    /** The decoration layer, to be added on top of the viewport by the delegate. */
    val layer: DecorationLayer = DecorationLayer()

    /** Decorations whose component currently sits in [layer]. */
    private val active = HashSet<PageDecoration>()

    /** Resizes and repositions the decoration layer; called from the delegate's viewport re-layout. */
    fun layout(x: Int, y: Int, width: Int, height: Int) {
        layer.setBounds(x, y, width, height)
    }

    /** Recomputes every registered decoration's visibility and position. */
    fun refresh() {
        val decorations = view.pageDecorations
        active.toList().forEach { if (it !in decorations) detach(it) }
        if (decorations.isEmpty() && active.isEmpty()) return

        val document = measuredDocument()
        for (decoration in decorations) {
            val component = decoration.content
            val pageIndex = document?.pages?.indexOfFirst { it.raw.id == decoration.pageId } ?: -1
            if (component == null || pageIndex < 0 || !view.effectivePageMode(decoration.pageId).laidOut) {
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

            if (component.parent !== layer) layer.add(component)
            val preferred = component.preferredSize
            val decorationSize = Size(
                width = if (preferred.width > 0) preferred.width.toDouble() else component.width.toDouble(),
                height = if (preferred.height > 0) preferred.height.toDouble() else component.height.toDouble(),
            )
            val bounds = resolveDecorationBounds(pageBounds, decoration.placement, decorationSize, view.zoom)
            component.setBounds(
                bounds.x.toInt(), bounds.y.toInt(), bounds.width.toInt().coerceAtLeast(1), bounds.height.toInt().coerceAtLeast(1),
            )
            active.add(decoration)
        }
        layer.revalidate()
        layer.repaint()
    }

    /** Detaches every decoration; called from the delegate's `uninstallUI`. */
    fun dispose() {
        active.toList().forEach { detach(it) }
    }

    private fun detach(decoration: PageDecoration) {
        decoration.content?.let { if (it.parent === layer) layer.remove(it) }
        active.remove(decoration)
        layer.repaint()
    }

    //region Test hooks

    internal val activeForTest: Set<PageDecoration> get() = active.toSet()

    internal val nodeCountForTest: Int get() = layer.componentCount

    //endregion
}
