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

import java.awt.Cursor
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [BasicPaperSheetUI], the default Look-and-Feel delegate: viewport painting, page
 * virtualisation, mouse text selection and the text pointer. Everything is painted into an off-screen
 * image; no window is opened.
 */
class BasicPaperSheetUITest {

    private fun ui(view: PaperSheetView): BasicPaperSheetUI = view.getPaperSheetUI() as BasicPaperSheetUI

    private fun paint(ui: BasicPaperSheetUI, width: Int = 500, height: Int = 400) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, width, height)
        } finally {
            g.dispose()
        }
    }

    /**
     * Verifies that painting a short document draws the sheet chrome for at least one page and
     * records the rendered page indices.
     */
    @Test
    fun paintsSheetChromeForAShortDocument() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = ui(view)
        paint(ui)
        assertTrue(ui.sheetChromeDrawCountForTest >= 1)
        assertTrue(ui.renderedPageIndicesForTest.isNotEmpty())
    }

    /**
     * Verifies the simple page virtualisation: a long multi-page document paints strictly fewer
     * pages than it measures to, because only the pages intersecting the viewport are drawn.
     */
    @Test
    fun virtualisesPagesOutsideTheViewport() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui, height = 300)
        assertTrue(ui.pageCountForTest > ui.renderedPageIndicesForTest.size)
        assertTrue(ui.renderedPageIndicesForTest.isNotEmpty())
    }

    /**
     * Verifies that dragging the mouse across the first line of text produces a non-empty selection
     * whose model text is non-blank.
     */
    @Test
    fun mouseDragSelectsText() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = ui(view)
        paint(ui)
        ui.selectByPointsForTest(35.0, 45.0, 260.0, 45.0)
        assertTrue(view.selectionModel.length > 0)
        assertTrue(view.selectedText.isNotBlank())
    }

    /**
     * Verifies that the pointer shape over a page's text content area is the text cursor while a
     * point far outside any sheet keeps the default cursor.
     */
    @Test
    fun pointerIsTextCursorOverContent() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = ui(view)
        paint(ui)
        assertEquals(Cursor.TEXT_CURSOR, ui.cursorAtForTest(80.0, 80.0).type)
        assertEquals(Cursor.DEFAULT_CURSOR, ui.cursorAtForTest(5.0, 5.0).type)
    }

    /**
     * Verifies that the internal vertical scroll bar becomes enabled once the scaled content is
     * taller than the viewport.
     */
    @Test
    fun scrollBarEnabledForTallContent() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui, height = 200)
        assertTrue(ui.verticalScrollBarForTest.isEnabled)
    }

    /**
     * Verifies that an empty view (no document) paints without drawing any sheet chrome.
     */
    @Test
    fun emptyViewDrawsNoChrome() {
        val view = PaperSheetView()
        val ui = ui(view)
        paint(ui)
        assertEquals(0, ui.sheetChromeDrawCountForTest)
    }
}
