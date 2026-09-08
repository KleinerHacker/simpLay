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

import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock

/**
 * Tracks which paragraph (a measured block) and which sheet the mouse currently hovers in a
 * [PaperSheetView] and mirrors that onto the view's read-only `hoveredParagraph` / `hoveredPage`
 * (+ bounds) properties. [PaperSheetViewSkin] feeds it pointer moves and uses [paragraphGeometry] /
 * [pageGeometry] to drive the [FloatingOverlayTrigger.PARAGRAPH_HOVER] and
 * [FloatingOverlayTrigger.PAGE_HOVER] overlay triggers.
 *
 * The tracker holds only the hovered indices, no geometry: [measured], [pageTops], [scrollOffset]
 * and [nearestPage] are read back from the skin on every call, so a scroll, a zoom or a re-measure
 * is reflected automatically on the next [publishOutputs].
 */
internal class PaperSheetHoverTracker(
    private val view: PaperSheetView,
    private val measured: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val nearestPage: (cyUnscaled: Double) -> Pair<Int, Double>,
) {

    private var paragraphPage = -1
    private var paragraphBlock: MeasuredTextBlock? = null
    private var paragraphOrdinal = -1
    private var pageIndex = -1

    /** The measured block currently under the mouse, or `null`. */
    val hoveredBlock: MeasuredTextBlock? get() = paragraphBlock

    /** Recomputes the hovered paragraph and sheet from a viewport point, then republishes. */
    fun update(px: Double, py: Double) {
        val doc = measured()
        if (doc == null || doc.pages.isEmpty()) {
            clear()
            return
        }
        val zoom = view.zoom
        val outer = view.outerMargin
        val cx = px / zoom
        val cy = (py + scrollOffset()) / zoom
        val (page, bandDistance) = nearestPage(cy)
        if (bandDistance > 0.0) {
            reset()
            publishOutputs()
            return
        }
        pageIndex = page
        val measuredPage = doc.pages[page]
        val contentArea = measuredPage.contentArea
        val localX = cx - outer - contentArea.x
        val localY = cy - (outer + pageTops()[page]) - contentArea.y
        val blockIndex = measuredPage.blocks.indexOfFirst { block ->
            val b = block.bounds
            localX >= b.x && localX <= b.x + b.width && localY >= b.y && localY <= b.y + b.height
        }
        if (blockIndex < 0) {
            paragraphPage = -1
            paragraphBlock = null
            paragraphOrdinal = -1
        } else {
            paragraphPage = page
            paragraphBlock = measuredPage.blocks[blockIndex]
            paragraphOrdinal = ordinalOf(doc, page, blockIndex)
        }
        publishOutputs()
    }

    /** Drops the hover state (the mouse left the viewport), then republishes. */
    fun clear() {
        reset()
        publishOutputs()
    }

    /**
     * Recomputes the viewport bounds of the currently hovered paragraph / sheet from the live scroll
     * offset and zoom and writes them onto [view]. Call after every scroll, zoom or repaint.
     */
    fun publishOutputs() {
        val block = paragraphBlock
        val paragraphBounds = if (block != null && paragraphPage >= 0) {
            contentRectToViewport(
                paragraphPage,
                block.bounds.x, block.bounds.y, block.bounds.width, block.bounds.height,
            )
        } else {
            null
        }
        view.updateHoveredParagraph(paragraphOrdinal, paragraphBounds)
        view.updateHoveredPage(pageIndex, if (pageIndex >= 0) pageRectToViewport(pageIndex) else null)
    }

    /** Anchor box, paragraph ordinal and text for the `PARAGRAPH_HOVER` trigger, or `null`. */
    fun paragraphGeometry(): TriggerGeometry? {
        val block = paragraphBlock ?: return null
        if (paragraphPage < 0) return null
        val b = block.bounds
        return TriggerGeometry(
            contentRectToViewport(paragraphPage, b.x, b.y, b.width, b.height),
            paragraphOrdinal,
            block.raw.toString(),
            null,
        )
    }

    /** Anchor box and page index for the `PAGE_HOVER` trigger, or `null`. */
    fun pageGeometry(): TriggerGeometry? {
        if (pageIndex < 0) return null
        return TriggerGeometry(pageRectToViewport(pageIndex), pageIndex, "", null)
    }

    private fun reset() {
        paragraphPage = -1
        paragraphBlock = null
        paragraphOrdinal = -1
        pageIndex = -1
    }

    private fun ordinalOf(doc: MeasuredDocument, page: Int, blockIndex: Int): Int {
        var ordinal = 0
        for (i in 0 until page) ordinal += doc.pages[i].blocks.size
        return ordinal + blockIndex
    }

    private fun contentRectToViewport(page: Int, rx: Double, ry: Double, rw: Double, rh: Double): Bounds {
        val zoom = view.zoom
        val outer = view.outerMargin
        val contentArea = measured()!!.pages[page].contentArea
        val absX = outer + contentArea.x + rx
        val absY = outer + pageTops()[page] + contentArea.y + ry
        return BoundingBox(absX * zoom, absY * zoom - scrollOffset(), rw * zoom, rh * zoom)
    }

    private fun pageRectToViewport(page: Int): Bounds {
        val zoom = view.zoom
        val outer = view.outerMargin
        val measuredPage = measured()!!.pages[page]
        val absX = outer
        val absY = outer + pageTops()[page]
        return BoundingBox(
            absX * zoom, absY * zoom - scrollOffset(),
            measuredPage.effectiveSize.width * zoom, measuredPage.effectiveSize.height * zoom,
        )
    }
}
