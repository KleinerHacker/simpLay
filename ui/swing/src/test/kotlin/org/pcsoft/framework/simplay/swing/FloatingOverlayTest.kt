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
import javax.swing.JButton
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [FloatingOverlay] handling by [BasicPaperSheetUI]: a `SELECTION`-triggered overlay is
 * shown while text is selected, fires its `onShown` listener and is auto-hidden when the selection
 * clears.
 */
class FloatingOverlayTest {

    private fun setUp(): Triple<PaperSheetView, BasicPaperSheetUI, FloatingOverlay> {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val overlay = FloatingOverlay().apply {
            content = JButton("Copy")
            trigger = FloatingOverlayTrigger.SELECTION
        }
        view.floatingOverlays += overlay
        val image = BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 400)
        } finally {
            g.dispose()
        }
        return Triple(view, ui, overlay)
    }

    /**
     * Verifies that selecting text activates a `SELECTION`-triggered overlay: its component is placed
     * in the overlay layer, [FloatingOverlay.isActive] is set and the `onShown` listener fires once.
     */
    @Test
    fun selectionShowsTheOverlayAndFiresOnShown() {
        val (_, ui, overlay) = setUp()
        var shown = 0
        overlay.onShown = FloatingOverlayListener { shown++ }
        ui.selectByPointsForTest(35.0, 45.0, 260.0, 45.0)
        assertTrue(overlay in ui.activeOverlaysForTest)
        assertEquals(1, ui.overlayNodeCountForTest)
        assertTrue(overlay.isActive)
        assertEquals(1, shown)
    }

    /**
     * Verifies that clearing the selection auto-hides the overlay again: it leaves the layer and
     * [FloatingOverlay.isActive] is cleared.
     */
    @Test
    fun clearingSelectionAutoHidesTheOverlay() {
        val (view, ui, overlay) = setUp()
        ui.selectByPointsForTest(35.0, 45.0, 260.0, 45.0)
        view.selectionModel.clearSelection()
        ui.refreshOverlaysForTest()
        assertFalse(overlay in ui.activeOverlaysForTest)
        assertEquals(0, ui.overlayNodeCountForTest)
        assertFalse(overlay.isActive)
    }

    /**
     * Verifies that a `PAGE_HOVER` overlay is shown while the mouse hovers a sheet and hidden once
     * the mouse leaves the viewport.
     */
    @Test
    fun pageHoverOverlayFollowsTheMouse() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val overlay = FloatingOverlay().apply {
            content = JButton("Page")
            trigger = FloatingOverlayTrigger.PAGE_HOVER
        }
        view.floatingOverlays += overlay
        val image = BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { g -> ui.paintForTest(g, 500, 400); g.dispose() }

        ui.hoverAtForTest(80.0, 80.0)
        assertTrue(overlay.isActive)
        ui.clearHoverForTest()
        assertFalse(overlay.isActive)
    }

    /**
     * A `PAGE_HOVER` overlay's `onShown` event reports `pageDeactivated = true` while hovering a page
     * that is marked deactivated under a non-`IGNORE` mode.
     */
    @Test
    fun floatingOverlayEventReportsPageDeactivatedTrue() {
        val view = PaperSheetView().apply {
            document = TestDocuments.twoPage
            deactivatedPageHandling = PageDeactivationMode.READONLY
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val overlay = FloatingOverlay().apply {
            content = JButton("Page")
            trigger = FloatingOverlayTrigger.PAGE_HOVER
        }
        view.floatingOverlays += overlay
        val image = BufferedImage(500, 800, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { g -> ui.paintForTest(g, 500, 800); g.dispose() }
        view.setPageDeactivated(1, true)
        image.createGraphics().let { g -> ui.paintForTest(g, 500, 800); g.dispose() }

        val shown = ArrayList<FloatingOverlayEvent>()
        overlay.onShown = FloatingOverlayListener { shown.add(it) }
        ui.hoverAtForTest(80.0, 396.0)

        assertTrue(overlay.isActive)
        assertEquals(1, shown.size)
        assertTrue(shown.first().pageDeactivated)
    }

    /**
     * The same `PAGE_HOVER` overlay reports `pageDeactivated = false` while hovering the still-active
     * first page, even though the second page is deactivated.
     */
    @Test
    fun floatingOverlayEventReportsPageDeactivatedFalse() {
        val view = PaperSheetView().apply {
            document = TestDocuments.twoPage
            deactivatedPageHandling = PageDeactivationMode.READONLY
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val overlay = FloatingOverlay().apply {
            content = JButton("Page")
            trigger = FloatingOverlayTrigger.PAGE_HOVER
        }
        view.floatingOverlays += overlay
        val image = BufferedImage(500, 800, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { g -> ui.paintForTest(g, 500, 800); g.dispose() }
        view.setPageDeactivated(1, true)
        image.createGraphics().let { g -> ui.paintForTest(g, 500, 800); g.dispose() }

        val shown = ArrayList<FloatingOverlayEvent>()
        overlay.onShown = FloatingOverlayListener { shown.add(it) }
        ui.hoverAtForTest(80.0, 80.0)

        assertTrue(overlay.isActive)
        assertEquals(1, shown.size)
        assertFalse(shown.first().pageDeactivated)
    }
}
