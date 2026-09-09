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

import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless tests for the [FloatingOverlay] API of [PaperSheetView]: the selection, paragraph-hover
 * and page-hover triggers, positioning that follows scroll and zoom, viewport-edge clamping, the
 * inert `CARET` trigger, the `onShown` / `onHidden` events and loading overlays from FXML.
 */
class FloatingOverlayTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(
        paragraphs: Int = 8,
        width: Double = 360.0,
        height: Double = 300.0,
        vararg overlays: FloatingOverlay,
    ): Fixture = onFxThread {
        val view = PaperSheetView()
        view.floatingOverlays.addAll(overlays)
        val stage = Stage()
        stage.scene = Scene(view, width, height)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * A `SELECTION` overlay is shown as soon as text is selected and its anchor box grows together
     * with the selection when it is extended.
     */
    @Test
    fun selectionOverlayAppearsAndTracksSelection() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.SELECTION
            content = Label("Copy")
        }
        val (_, skin) = fixture(overlays = arrayOf(overlay))

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 200.0, 57.0) }
        assertTrue(overlay.isActive)
        val firstBounds = assertNotNull(overlay.activeBounds)

        onFxThread { skin.selectByPointsForTest(57.0, 45.0, 300.0, 140.0) }
        val grownBounds = assertNotNull(overlay.activeBounds)

        assertTrue(skin.activeOverlaysForTest.contains(overlay))
        assertTrue(grownBounds.height > firstBounds.height, "anchor box should grow with the selection")
    }

    /**
     * Clearing the selection hides the `SELECTION` overlay again and removes its node from the view.
     */
    @Test
    fun selectionOverlayHidesWhenSelectionCleared() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.SELECTION
            content = Label("Copy")
        }
        val (view, skin) = fixture(overlays = arrayOf(overlay))
        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 200.0, 57.0) }
        assertTrue(overlay.isActive)

        onFxThread { view.selectionModel.clearSelection() }

        assertFalse(overlay.isActive)
        assertEquals(0, skin.overlayNodeCountForTest)
    }

    /**
     * A `PARAGRAPH_HOVER` overlay becomes active while the mouse is over a paragraph and its
     * `activeIndex` equals the view's `hoveredParagraph` ordinal.
     */
    @Test
    fun paragraphHoverOverlayCarriesParagraphIndex() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.PARAGRAPH_HOVER
            content = Label("Paragraph")
        }
        val (view, skin) = fixture(paragraphs = 6, overlays = arrayOf(overlay))

        onFxThread { skin.hoverAtForTest(60.0, 60.0) }

        assertTrue(overlay.isActive)
        assertTrue(overlay.activeIndex >= 0)
        assertEquals(view.hoveredParagraph, overlay.activeIndex)
    }

    /**
     * A `PAGE_HOVER` overlay anchors to the sheet under the mouse: its anchor box shares the left
     * edge with the view's `hoveredPageBounds`.
     */
    @Test
    fun pageHoverOverlayAnchorsToSheet() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.PAGE_HOVER
            anchor = Pos.TOP_LEFT
            content = Label("Page")
        }
        val (view, skin) = fixture(overlays = arrayOf(overlay))

        onFxThread { skin.hoverAtForTest(60.0, 60.0) }

        assertTrue(overlay.isActive)
        val pageBounds = assertNotNull(view.hoveredPageBounds)
        val overlayBounds = assertNotNull(overlay.activeBounds)
        assertEquals(pageBounds.minX, overlayBounds.minX, 0.5)
        assertEquals(pageBounds.minY, overlayBounds.minY, 0.5)
    }

    /**
     * The overlay node follows the content: scrolling down moves it up, and increasing the zoom
     * scales its anchor box while keeping it active.
     */
    @Test
    fun overlayFollowsScrollAndZoom() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.SELECTION
            anchor = Pos.BOTTOM_LEFT
            content = Label("Copy")
        }
        val (view, skin) = fixture(paragraphs = 40, overlays = arrayOf(overlay))
        onFxThread { skin.selectByPointsForTest(57.0, 130.0, 250.0, 150.0) }
        val node = assertNotNull(overlay.content)

        val yBeforeScroll = onFxThread { node.layoutY }
        val widthBeforeZoom = assertNotNull(overlay.activeBounds).width

        onFxThread { skin.verticalScrollBar.value = 45.0 }
        val yAfterScroll = onFxThread { node.layoutY }
        assertTrue(yBeforeScroll - yAfterScroll > 20.0, "overlay should move up as the content scrolls")

        onFxThread {
            skin.verticalScrollBar.value = 0.0
            view.zoom = 2.0
            view.layout()
            skin.refreshOverlaysForTest()
        }
        assertTrue(overlay.isActive)
        assertTrue(assertNotNull(overlay.activeBounds).width > widthBeforeZoom, "zoom should enlarge the anchor box")
    }

    /**
     * While the anchor is only partly scrolled out the overlay node stays clamped inside the
     * viewport; once the anchor is completely out of view the overlay is hidden.
     */
    @Test
    fun overlayClampsAtViewportEdgeThenHides() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.SELECTION
            anchor = Pos.BOTTOM_LEFT
            content = Label("Copy")
        }
        val (_, skin) = fixture(paragraphs = 40, overlays = arrayOf(overlay))
        onFxThread { skin.selectByPointsForTest(57.0, 42.0, 250.0, 120.0) }
        val node = assertNotNull(overlay.content)
        val nodeHeight = onFxThread { node.layoutBounds.height.takeIf { it > 0.0 } ?: node.prefHeight(-1.0) }

        onFxThread { skin.verticalScrollBar.value = 55.0 }
        if (overlay.isActive) {
            val y = onFxThread { node.layoutY }
            assertTrue(y in 0.0..(skin.viewportHeight - nodeHeight), "clamped inside the viewport, was $y")
        }

        onFxThread { skin.verticalScrollBar.value = skin.verticalScrollBar.max }
        assertFalse(overlay.isActive)
        assertEquals(0, skin.overlayNodeCountForTest)
    }

    /**
     * Overlays declared in FXML as `<floatingOverlays>` children load into the control and react to
     * the selection and paragraph-hover triggers exactly like code-registered ones.
     */
    @Test
    fun fxmlDeclaredOverlaysLoadAndActivate() {
        val resource = javaClass.getResource("/org/pcsoft/framework/simplay/fx/paper-sheet-overlays.fxml")
        assertNotNull(resource, "test FXML must be on the classpath")

        val view = onFxThread { FXMLLoader(resource).load<PaperSheetView>() }
        assertEquals(2, view.floatingOverlays.size)

        val skin = onFxThread {
            val stage = Stage()
            stage.scene = Scene(view, 360.0, 300.0)
            stage.show()
            view.document = PaperSheetTestFixtures.flowDocument(8)
            view.applyCss()
            view.layout()
            view.skin as PaperSheetViewSkin
        }

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 200.0, 57.0) }
        val selectionOverlay = view.floatingOverlays.first { it.trigger == FloatingOverlayTrigger.SELECTION }
        assertTrue(selectionOverlay.isActive)

        onFxThread { skin.hoverAtForTest(60.0, 60.0) }
        val paragraphOverlay = view.floatingOverlays.first { it.trigger == FloatingOverlayTrigger.PARAGRAPH_HOVER }
        assertTrue(paragraphOverlay.isActive)
    }

    /**
     * A `CARET` overlay never shows in the read-only component: no selection or hover activates it.
     */
    @Test
    fun caretTriggerIsInertWithoutEditing() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.CARET
            content = Label("Caret")
        }
        val (_, skin) = fixture(overlays = arrayOf(overlay))

        onFxThread {
            skin.selectByPointsForTest(57.0, 57.0, 200.0, 57.0)
            skin.hoverAtForTest(60.0, 60.0)
            skin.refreshOverlaysForTest()
        }

        assertFalse(overlay.isActive)
        assertEquals(0, skin.overlayNodeCountForTest)
    }

    /**
     * `onShown` fires once with the trigger context (bounds, text, document range) when the overlay
     * appears; `onHidden` fires once when it disappears; a redundant refresh fires neither again.
     */
    @Test
    fun onShownAndOnHiddenFireWithContext() {
        val shown = ArrayList<FloatingOverlayEvent>()
        val hidden = ArrayList<FloatingOverlayEvent>()
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.SELECTION
            content = Label("Copy")
            onShown = EventHandler { shown.add(it) }
            onHidden = EventHandler { hidden.add(it) }
        }
        val (view, skin) = fixture(overlays = arrayOf(overlay))

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 220.0, 57.0) }
        assertEquals(1, shown.size)
        val event = shown.first()
        assertEquals(FloatingOverlayTrigger.SELECTION, event.triggerKind)
        assertNotNull(event.triggerBounds)
        assertTrue(event.text.isNotEmpty())
        assertNotNull(event.documentRange)

        onFxThread { skin.refreshOverlaysForTest() }
        assertEquals(1, shown.size, "no duplicate onShown on a redundant refresh")

        onFxThread { view.selectionModel.clearSelection() }
        assertEquals(1, hidden.size)
        assertEquals(FloatingOverlayTrigger.SELECTION, hidden.first().triggerKind)
    }
}
