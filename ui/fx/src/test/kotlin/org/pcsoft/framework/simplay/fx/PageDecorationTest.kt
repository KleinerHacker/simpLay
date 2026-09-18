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

package org.pcsoft.framework.simplay.fx

import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.uicommon.EdgeAlignment
import org.pcsoft.framework.simplay.uicommon.PageEdge
import org.pcsoft.framework.simplay.uicommon.PageMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless tests for the [PageDecoration] API of [PaperSheetView]: attaching a decoration to its
 * resolved page, the `STRETCH` default versus an explicit [EdgeAlignment], `offsetX` / `offsetY`,
 * positioning that follows scroll and zoom, coexistence of several decorations and visibility that is
 * independent of [PaperSheetMode] / [PageMode].
 */
class PageDecorationTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(
        paragraphs: Int = 8,
        width: Double = 360.0,
        height: Double = 300.0,
        vararg decorations: PageDecoration,
    ): Fixture = onFxThread {
        val view = PaperSheetView()
        view.pageDecorations.addAll(decorations)
        val stage = Stage()
        stage.scene = Scene(view, width, height)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * A decoration whose `pageId` resolves to a laid-out page is attached: it appears in
     * `activeDecorationsForTest` and its node is added to the decoration layer.
     */
    @Test
    fun decorationAttachesToItsResolvedPage() {
        val decoration = PageDecoration().apply { content = Label("1") }
        val (view, skin) = fixture(decorations = arrayOf(decoration))
        onFxThread { decoration.pageId = view.document!!.pages[0].id }

        onFxThread { skin.refreshDecorationsForTest() }

        assertTrue(skin.activeDecorationsForTest.contains(decoration))
        assertEquals(1, skin.decorationNodeCountForTest)
    }

    /**
     * A decoration whose `pageId` does not resolve to any page of the current document stays
     * detached: it never appears in `activeDecorationsForTest` and adds no node to the layer.
     */
    @Test
    fun decorationWithUnknownPageIdStaysDetached() {
        val decoration = PageDecoration().apply {
            pageId = "does-not-exist"
            content = Label("1")
        }
        val (_, skin) = fixture(decorations = arrayOf(decoration))

        assertFalse(skin.activeDecorationsForTest.contains(decoration))
        assertEquals(0, skin.decorationNodeCountForTest)
    }

    /**
     * The default [EdgeAlignment.STRETCH] fills the whole width of a horizontal edge, while an
     * explicit [EdgeAlignment.CENTER] keeps the decoration's own, narrower preferred width.
     */
    @Test
    fun defaultAlignmentStretchesWhileCenterKeepsOwnWidth() {
        val stretched = PageDecoration().apply { edge = PageEdge.TOP; content = Label("Stretched") }
        val centered = PageDecoration().apply {
            edge = PageEdge.BOTTOM
            alignment = EdgeAlignment.CENTER
            content = Label("Centered")
        }
        val (view, skin) = fixture(decorations = arrayOf(stretched, centered))
        onFxThread {
            val pageId = view.document!!.pages[0].id
            stretched.pageId = pageId
            centered.pageId = pageId
            skin.refreshDecorationsForTest()
        }

        val stretchedWidth = onFxThread { stretched.content!!.layoutBounds.width }
        val centeredWidth = onFxThread { centered.content!!.layoutBounds.width }
        assertTrue(centeredWidth < stretchedWidth, "a centered decoration should keep its own, narrower width")
    }

    /**
     * A larger `offsetY` moves a `TOP`-edge decoration further away from the page, i.e. higher up in
     * the viewport (a smaller layout `y`).
     */
    @Test
    fun offsetYShiftsTheDecorationFurtherFromThePage() {
        val near = PageDecoration().apply { edge = PageEdge.TOP; content = Label("Near") }
        val far = PageDecoration().apply { edge = PageEdge.TOP; offsetY = 20.0; content = Label("Far") }
        val (view, skin) = fixture(decorations = arrayOf(near, far))
        onFxThread {
            val pageId = view.document!!.pages[0].id
            near.pageId = pageId
            far.pageId = pageId
            skin.refreshDecorationsForTest()
        }

        val nearY = onFxThread { near.content!!.layoutY }
        val farY = onFxThread { far.content!!.layoutY }
        assertTrue(farY < nearY, "a larger offsetY should push the TOP decoration further up")
    }

    /**
     * A decoration anchored to a page below the viewport moves as the content scrolls, exactly like
     * the page it is attached to.
     */
    @Test
    fun decorationFollowsScroll() {
        val decoration = PageDecoration().apply { edge = PageEdge.BOTTOM; content = Label("Follows") }
        val (view, skin) = fixture(paragraphs = 40, decorations = arrayOf(decoration))
        onFxThread {
            decoration.pageId = view.document!!.pages[0].id
            skin.refreshDecorationsForTest()
        }
        val yBeforeScroll = onFxThread { decoration.content!!.layoutY }

        onFxThread {
            skin.verticalScrollBar.value = 45.0
            skin.refreshDecorationsForTest()
        }
        val yAfterScroll = onFxThread { decoration.content!!.layoutY }

        assertTrue(yBeforeScroll - yAfterScroll > 20.0, "decoration should move up as the content scrolls")
    }

    /**
     * Increasing the zoom scales a `STRETCH`-aligned decoration's width together with the page it is
     * anchored to, while it stays active.
     */
    @Test
    fun decorationFollowsZoom() {
        val decoration = PageDecoration().apply { edge = PageEdge.TOP; content = Label("Zoom") }
        val (view, skin) = fixture(decorations = arrayOf(decoration))
        onFxThread {
            decoration.pageId = view.document!!.pages[0].id
            skin.refreshDecorationsForTest()
        }
        val widthBeforeZoom = onFxThread { decoration.content!!.layoutBounds.width }

        onFxThread {
            view.zoom = 2.0
            view.layout()
            skin.refreshDecorationsForTest()
        }
        val widthAfterZoom = onFxThread { decoration.content!!.layoutBounds.width }

        assertTrue(skin.activeDecorationsForTest.contains(decoration))
        assertTrue(widthAfterZoom > widthBeforeZoom, "zoom should enlarge a STRETCH decoration's width")
    }

    /**
     * Several decorations may target the same page and the same edge; each one is attached and
     * positioned independently.
     */
    @Test
    fun multipleDecorationsOnTheSamePageAndEdgeCoexist() {
        val first = PageDecoration().apply { edge = PageEdge.TOP; content = Label("First") }
        val second = PageDecoration().apply { edge = PageEdge.TOP; offsetY = 10.0; content = Label("Second") }
        val (view, skin) = fixture(decorations = arrayOf(first, second))
        onFxThread {
            val pageId = view.document!!.pages[0].id
            first.pageId = pageId
            second.pageId = pageId
            skin.refreshDecorationsForTest()
        }

        assertTrue(skin.activeDecorationsForTest.contains(first))
        assertTrue(skin.activeDecorationsForTest.contains(second))
        assertEquals(2, skin.decorationNodeCountForTest)
    }

    /**
     * A decoration whose node is larger than [PaperSheetView.outerMargin] grows the reserved layout
     * space on its edge: the view's [PaperSheetView.contentSize] height ends up taller than it would
     * be for a document with no decoration at all.
     */
    @Test
    fun oversizedDecorationGrowsReservedSpace() {
        val decoration = PageDecoration().apply {
            edge = PageEdge.TOP
            content = Label("Tall").apply { minHeight = 500.0 }
        }
        val plain = fixture()
        val (view, skin) = fixture(decorations = arrayOf(decoration))
        onFxThread {
            decoration.pageId = view.document!!.pages[0].id
            skin.refreshDecorationsForTest()
            view.layout()
        }

        assertTrue(
            view.contentSize.height > plain.view.contentSize.height + 400.0,
            "a decoration taller than outerMargin should grow the reserved space",
        )
    }

    /**
     * A decoration whose node is explicitly marked `isManaged = false` (JavaFX's own flag, reused by
     * this module as the reservation opt-out) stays a plain overlay: it never grows the reserved
     * space, even when it is larger than [PaperSheetView.outerMargin].
     */
    @Test
    fun unmanagedDecorationDoesNotGrowReservedSpace() {
        val decoration = PageDecoration().apply {
            edge = PageEdge.TOP
            content = Label("Tall").apply { minHeight = 500.0; isManaged = false }
        }
        val plain = fixture()
        val (view, skin) = fixture(decorations = arrayOf(decoration))
        onFxThread {
            decoration.pageId = view.document!!.pages[0].id
            skin.refreshDecorationsForTest()
            view.layout()
        }

        assertEquals(plain.view.contentSize.height, view.contentSize.height, 0.5)
    }

    /**
     * A decoration stays attached while its page is laid out regardless of a restrictive
     * [PageMode] on that page or a non-`STATIC` [PaperSheetMode], unlike a [FloatingOverlay].
     */
    @Test
    fun decorationStaysVisibleRegardlessOfPageMode() {
        val decoration = PageDecoration().apply { content = Label("Always") }
        val skin = onFxThread {
            val view = PaperSheetView()
            view.pageDecorations.add(decoration)
            val stage = Stage()
            stage.scene = Scene(view, 400.0, 700.0)
            stage.show()
            view.document = PaperSheetTestFixtures.twoPageDocument()
            decoration.pageId = view.document!!.pages[1].id
            view.setPageMode(1, PageMode.DISABLED)
            view.applyCss()
            view.layout()
            (view.skin as PaperSheetViewSkin).also { it.refreshDecorationsForTest() }
        }

        assertNotNull(decoration.content)
        assertTrue(skin.activeDecorationsForTest.contains(decoration))
    }
}
