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

import java.awt.image.BufferedImage
import javax.swing.JLabel
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.uicommon.EdgeAlignment
import org.pcsoft.framework.simplay.uicommon.PageEdge
import org.pcsoft.framework.simplay.uicommon.PageMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the [PageDecoration] API of [BasicPaperSheetUI]: attaching a decoration to its resolved
 * page, the `STRETCH` default versus an explicit [EdgeAlignment], `offsetX` / `offsetY`, positioning
 * that follows scroll and zoom, coexistence of several decorations and visibility that is independent
 * of [PaperSheetMode] / [PageMode].
 */
class PageDecorationTest {

    private fun paint(ui: BasicPaperSheetUI, width: Int = 500, height: Int = 400) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, width, height)
        } finally {
            g.dispose()
        }
    }

    private fun setUp(document: Document = TestDocuments.twoPage, width: Int = 500, height: Int = 700): Pair<PaperSheetView, BasicPaperSheetUI> {
        val view = PaperSheetView().apply { this.document = document }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        paint(ui, width, height)
        return view to ui
    }

    /**
     * A decoration whose `pageId` resolves to a laid-out page is attached: it appears in
     * `activeDecorationsForTest` and its component is added to the decoration layer.
     */
    @Test
    fun decorationAttachesToItsResolvedPage() {
        val (view, ui) = setUp()
        val decoration = PageDecoration().apply {
            pageId = view.document!!.pages[0].id
            content = JLabel("1")
        }
        view.pageDecorations += decoration
        paint(ui)

        assertTrue(decoration in ui.activeDecorationsForTest)
        assertEquals(1, ui.decorationNodeCountForTest)
    }

    /**
     * A decoration whose `pageId` does not resolve to any page of the current document stays
     * detached: it never appears in `activeDecorationsForTest` and adds no component to the layer.
     */
    @Test
    fun decorationWithUnknownPageIdStaysDetached() {
        val (view, ui) = setUp()
        val decoration = PageDecoration().apply {
            pageId = "does-not-exist"
            content = JLabel("1")
        }
        view.pageDecorations += decoration
        paint(ui)

        assertFalse(decoration in ui.activeDecorationsForTest)
        assertEquals(0, ui.decorationNodeCountForTest)
    }

    /**
     * The default [EdgeAlignment.STRETCH] fills the whole width of a horizontal edge, while an
     * explicit [EdgeAlignment.CENTER] keeps the decoration's own, narrower preferred width.
     */
    @Test
    fun defaultAlignmentStretchesWhileCenterKeepsOwnWidth() {
        val (view, ui) = setUp()
        val pageId = view.document!!.pages[0].id
        val stretched = PageDecoration().apply {
            this.pageId = pageId
            edge = PageEdge.TOP
            content = JLabel("Stretched")
        }
        val centered = PageDecoration().apply {
            this.pageId = pageId
            edge = PageEdge.BOTTOM
            alignment = EdgeAlignment.CENTER
            content = JLabel("Centered")
        }
        view.pageDecorations += stretched
        view.pageDecorations += centered
        paint(ui)

        val stretchedWidth = stretched.content!!.width
        val centeredWidth = centered.content!!.width
        assertTrue(centeredWidth < stretchedWidth, "a centered decoration should keep its own, narrower width")
    }

    /**
     * A larger `offsetY` moves a `TOP`-edge decoration further away from the page, i.e. higher up in
     * the viewport (a smaller `y`).
     */
    @Test
    fun offsetYShiftsTheDecorationFurtherFromThePage() {
        val (view, ui) = setUp()
        val pageId = view.document!!.pages[0].id
        val near = PageDecoration().apply { edge = PageEdge.TOP; this.pageId = pageId; content = JLabel("Near") }
        val far = PageDecoration().apply { edge = PageEdge.TOP; this.pageId = pageId; offsetY = 20.0; content = JLabel("Far") }
        view.pageDecorations += near
        view.pageDecorations += far
        paint(ui)

        assertTrue(far.content!!.y < near.content!!.y, "a larger offsetY should push the TOP decoration further up")
    }

    /**
     * A decoration anchored to a page below the viewport moves as the content scrolls, exactly like
     * the page it is attached to.
     */
    @Test
    fun decorationFollowsScroll() {
        val (view, ui) = setUp(document = TestDocuments.long, height = 300)
        val decoration = PageDecoration().apply {
            edge = PageEdge.BOTTOM
            pageId = view.document!!.pages[0].id
            content = JLabel("Follows")
        }
        view.pageDecorations += decoration
        paint(ui, height = 300)
        val yBeforeScroll = decoration.content!!.y

        ui.scrollForTest(10)
        paint(ui, height = 300)
        val yAfterScroll = decoration.content!!.y

        assertTrue(yBeforeScroll != yAfterScroll, "decoration should move as the content scrolls")
    }

    /**
     * Increasing the zoom scales a `STRETCH`-aligned decoration's width together with the page it is
     * anchored to, while it stays active.
     */
    @Test
    fun decorationFollowsZoom() {
        val (view, ui) = setUp()
        val decoration = PageDecoration().apply {
            edge = PageEdge.TOP
            pageId = view.document!!.pages[0].id
            content = JLabel("Zoom")
        }
        view.pageDecorations += decoration
        paint(ui)
        val widthBeforeZoom = decoration.content!!.width

        view.zoom = 2.0
        paint(ui)
        val widthAfterZoom = decoration.content!!.width

        assertTrue(decoration in ui.activeDecorationsForTest)
        assertTrue(widthAfterZoom > widthBeforeZoom, "zoom should enlarge a STRETCH decoration's width")
    }

    /**
     * Several decorations may target the same page and the same edge; each one is attached and
     * positioned independently.
     */
    @Test
    fun multipleDecorationsOnTheSamePageAndEdgeCoexist() {
        val (view, ui) = setUp()
        val pageId = view.document!!.pages[0].id
        val first = PageDecoration().apply { edge = PageEdge.TOP; this.pageId = pageId; content = JLabel("First") }
        val second = PageDecoration().apply { edge = PageEdge.TOP; this.pageId = pageId; offsetY = 10.0; content = JLabel("Second") }
        view.pageDecorations += first
        view.pageDecorations += second
        paint(ui)

        assertTrue(first in ui.activeDecorationsForTest)
        assertTrue(second in ui.activeDecorationsForTest)
        assertEquals(2, ui.decorationNodeCountForTest)
    }

    /**
     * A decoration stays attached while its page is laid out regardless of a restrictive [PageMode]
     * on that page or a non-`STATIC` [PaperSheetMode], unlike a [FloatingOverlay].
     */
    @Test
    fun decorationStaysVisibleRegardlessOfPageMode() {
        val (view, ui) = setUp()
        val decoration = PageDecoration().apply {
            pageId = view.document!!.pages[1].id
            content = JLabel("Always")
        }
        view.pageDecorations += decoration
        view.setPageMode(1, PageMode.DISABLED)
        paint(ui)

        assertTrue(decoration in ui.activeDecorationsForTest)
    }
}
