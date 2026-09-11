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
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    //region Page deactivation

    /**
     * A `DISABLED` second page is still drawn (part of [BasicPaperSheetUI.renderedPageIndicesForTest])
     * but with the deactivated fill instead of the normal sheet background, and the caret does not
     * render even while it sits inside that page.
     */
    @Test
    fun disabledPageIsPaintedWithOverlayAndNoCaret() {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.twoPage
        }
        val ui = ui(view)
        paint(ui, height = 800)
        view.caretModel.moveToStartOfBlock(1)
        view.deactivatedPageHandling = PageDeactivationMode.DISABLED
        view.setPageDeactivated(1, true)
        paint(ui, height = 800)

        assertTrue(ui.renderedPageIndicesForTest.contains(1), "the DISABLED page is still drawn")
        assertEquals(0, ui.caretDrawCountForTest, "no caret should be drawn on a DISABLED page")
    }

    /**
     * A `HIDDEN` second page is excluded from layout entirely: it is missing from
     * [BasicPaperSheetUI.renderedPageIndicesForTest] and no longer contributes to
     * [PaperSheetView.contentSize].
     */
    @Test
    fun hiddenPageIsExcludedFromLayoutAndNotPainted() {
        val viewBoth = PaperSheetView().apply { document = TestDocuments.twoPage }
        paint(ui(viewBoth), height = 800)
        val heightWithBothPages = viewBoth.contentSize.height

        val view = PaperSheetView().apply { document = TestDocuments.twoPage }
        val ui = ui(view)
        paint(ui, height = 800)
        view.deactivatedPageHandling = PageDeactivationMode.HIDDEN
        view.setPageDeactivated(1, true)
        paint(ui, height = 800)

        assertFalse(ui.renderedPageIndicesForTest.contains(1), "the HIDDEN page must not be painted")
        assertTrue(
            view.contentSize.height < heightWithBothPages,
            "hiding a page should shrink the content height (${view.contentSize.height} vs $heightWithBothPages)",
        )
    }

    /**
     * A `READONLY` second page is painted exactly like a normal one, and it still counts in the
     * rendered pages even though every mutation on it is rejected. The caret is placed strictly
     * inside the blocked block, because an insertion point on its boundary is allowed by contract.
     */
    @Test
    fun readonlyPageIsPaintedNormallyButNotEditable() {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.twoPage
        }
        val ui = ui(view)
        paint(ui, height = 800)
        view.caretModel.moveIntoBlock(1, 2)
        view.deactivatedPageHandling = PageDeactivationMode.READONLY
        view.setPageDeactivated(1, true)
        paint(ui, height = 800)

        assertTrue(ui.renderedPageIndicesForTest.contains(1))
        val before = view.document
        ui.typeTextForTest("Z")
        assertEquals(before, view.document, "READONLY must reject the mutation")
    }

    //endregion
}
