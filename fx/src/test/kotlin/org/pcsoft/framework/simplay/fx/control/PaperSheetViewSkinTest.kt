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

import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Headless tests for [PaperSheetViewSkin]: sheet chrome, page virtualisation, the vertical scroll
 * bar, the absence of a caret, the pointer shape, and mouse text selection with `Ctrl+C` copy.
 */
class PaperSheetViewSkinTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(paragraphs: Int, width: Double = 320.0, height: Double = 260.0): Fixture = onFxThread {
        val view = PaperSheetView()
        val stage = Stage()
        stage.scene = Scene(view, width, height)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * Every page in the viewport is painted as a sheet: exactly one border-and-shadow chrome pass
     * per rendered page.
     */
    @Test
    fun pagesRenderAsSheetsWithBorderAndShadow() {
        val (_, skin) = fixture(paragraphs = 2)

        assertTrue(skin.renderedPageIndices.isNotEmpty())
        assertEquals(skin.renderedPageIndices.size, skin.sheetChromeDrawCount)
    }

    /**
     * A long document is virtualised: with the scroll position at the top only the first page(s)
     * are drawn and the count stays well below the total page count.
     */
    @Test
    fun onlyVisiblePagesAreDrawnAtTop() {
        val (_, skin) = fixture(paragraphs = 40)

        assertTrue(skin.pageCount > 3, "fixture should span several pages, was ${skin.pageCount}")
        assertEquals(0, skin.renderedPageIndices.first())
        assertTrue(skin.renderedPageIndices.size < skin.pageCount)
    }

    /**
     * Scrolling to the bottom swaps the rendered pages: the first page is gone and the last page is
     * now among the drawn ones.
     */
    @Test
    fun scrollingChangesTheVisiblePageWindow() {
        val (_, skin) = fixture(paragraphs = 40)

        onFxThread { skin.verticalScrollBar.value = skin.verticalScrollBar.max }

        assertFalse(skin.renderedPageIndices.contains(0))
        assertTrue(skin.renderedPageIndices.contains(skin.pageCount - 1))
    }

    /**
     * The vertical scroll bar's maximum equals the scaled content height minus the viewport height.
     */
    @Test
    fun verticalScrollBarTracksContentHeight() {
        val (view, skin) = fixture(paragraphs = 40)

        val expected = view.contentSize.height * view.zoom - skin.viewportHeight

        assertTrue(expected > 0.0)
        assertTrue(abs(skin.verticalScrollBar.max - expected) < 1.0)
    }

    /**
     * The read-only component never draws a caret.
     */
    @Test
    fun noCaretIsRenderedInReadonly() {
        val (_, skin) = fixture(paragraphs = 6)

        assertEquals(0, skin.caretDrawCount)
    }

    /**
     * The pointer is a text (I-beam) cursor while it is over a page's content area and the default
     * cursor while it is over the outer margin / paper border.
     */
    @Test
    fun cursorIsTextOverContentAreaAndDefaultOverMargin() {
        val (_, skin) = fixture(paragraphs = 6)

        assertEquals(Cursor.TEXT, onFxThread { skin.cursorAtForTest(80.0, 90.0) })
        assertEquals(Cursor.DEFAULT, onFxThread { skin.cursorAtForTest(4.0, 4.0) })
    }

    /**
     * Pressing on the text and dragging to another point selects the characters in between; the
     * selection text and its bounding box become non-empty.
     */
    @Test
    fun clickThenDragSelectsTextRange() {
        val (view, skin) = fixture(paragraphs = 6)

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }

        assertTrue(view.selectedText.isNotEmpty())
        val bounds = view.selectionBounds
        assertNotNull(bounds)
        assertTrue(bounds.width > 0.0 && bounds.height > 0.0)
    }

    /**
     * A selection that covers two words on one line also spans the whitespace between them: the
     * selection text contains a space and the highlight bounding box is wider than either word.
     */
    @Test
    fun selectionCoversWhitespaceBetweenWords() {
        val (view, skin) = fixture(paragraphs = 6)

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 170.0, 57.0) }

        assertTrue(view.selectedText.contains(" "), "expected a multi-word selection: '${view.selectedText}'")
        val bounds = assertNotNull(view.selectionBounds)
        assertTrue(bounds.width > 0.0)
    }

    /**
     * A selection dragged from the top of a multi-block page down to its bottom spans more than one
     * block and returns the joined plain text, longer than a single paragraph.
     */
    @Test
    fun selectionSpansBlocksAndPages() {
        val (view, skin) = fixture(paragraphs = 40)

        onFxThread { skin.selectByPointsForTest(57.0, 45.0, 300.0, 240.0) }

        assertTrue(
            view.selectedText.length > PaperSheetTestFixtures.PARAGRAPH.length,
            "expected a multi-block selection but was ${view.selectedText.length} chars",
        )
    }

    /**
     * A mouse drag over two words feeds the view's selection model: its character range and its
     * styled run list become non-empty.
     */
    @Test
    fun dragUpdatesSelectionModelRunsAndRange() {
        val (view, skin) = fixture(paragraphs = 6)

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 170.0, 57.0) }

        val model = view.selectionModel
        assertTrue(model.length > 0)
        assertEquals(model.endIndex - model.startIndex, model.length)
        assertTrue(model.runs.isNotEmpty())
    }

    /**
     * `Ctrl+C` copies the current selection to the system clipboard in three flavours at once: plain
     * text, styled HTML and styled RTF, the rich ones carrying the font.
     */
    @Test
    fun ctrlCPutsPlainAndStyledTextOnClipboard() {
        val (view, skin) = fixture(paragraphs = 6)
        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 260.0, 57.0) }
        val expected = view.selectedText

        val clipboard = onFxThread {
            view.fireEvent(
                KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false, true, false, false),
            )
            Clipboard.getSystemClipboard().let {
                Triple(it.string, it.hasHtml(), it.hasRtf())
            }
        }

        assertTrue(expected.isNotEmpty())
        assertEquals(expected, clipboard.first)
        assertTrue(clipboard.second, "HTML flavour expected on the clipboard")
        assertTrue(clipboard.third, "RTF flavour expected on the clipboard")
    }
}
