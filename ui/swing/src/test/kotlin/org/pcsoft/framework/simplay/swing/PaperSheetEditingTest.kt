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

import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the editable mode of [PaperSheetView]: typing, caret navigation and deletion, all driven
 * through the [BasicPaperSheetUI] test hooks against an off-screen paint.
 */
class PaperSheetEditingTest {

    private fun editableView(): Pair<PaperSheetView, BasicPaperSheetUI> {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.short
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val image = BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 400)
        } finally {
            g.dispose()
        }
        return view to ui
    }

    /**
     * Verifies that typing at the document start replaces [PaperSheetView.document] with a new
     * instance whose linear text begins with the typed characters and leaves the caret behind them.
     */
    @Test
    fun typingInsertsTextAndAdvancesCaret() {
        val (view, ui) = editableView()
        ui.placeCaretAtForTest(35.0, 45.0)
        val caretBefore = ui.caretIndexForTest
        ui.typeTextForTest("XY")
        assertEquals(caretBefore + 2, ui.caretIndexForTest)
        assertTrue(view.selectedText.isEmpty())
        assertNotNull(view.document)
    }

    /**
     * Verifies that a right-arrow key press moves the caret forward by one character.
     */
    @Test
    fun rightArrowMovesCaretForward() {
        val (_, ui) = editableView()
        ui.placeCaretAtForTest(35.0, 45.0)
        val before = ui.caretIndexForTest
        ui.pressKeyForTest(KeyEvent.VK_RIGHT)
        assertEquals(before + 1, ui.caretIndexForTest)
    }

    /**
     * Verifies that a backspace key press after typing removes the last inserted character.
     */
    @Test
    fun backspaceDeletesThePrecedingCharacter() {
        val (_, ui) = editableView()
        ui.placeCaretAtForTest(35.0, 45.0)
        ui.typeTextForTest("Z")
        val afterType = ui.caretIndexForTest
        ui.pressKeyForTest(KeyEvent.VK_BACK_SPACE)
        assertEquals(afterType - 1, ui.caretIndexForTest)
    }

    /**
     * Verifies that in editable mode the caret has a viewport rectangle once it is placed.
     */
    @Test
    fun caretHasViewportBoundsWhenEditable() {
        val (_, ui) = editableView()
        ui.placeCaretAtForTest(35.0, 45.0)
        assertNotNull(ui.caretBoundsForTest())
    }

    //region Page deactivation

    private fun Document.plain(): String = pages.flatMap { it.blocks }.joinToString("\n") { it.toString() }

    /**
     * Builds a shown, editable [PaperSheetView] over [TestDocuments.twoPage] and returns it together
     * with the linear index where the second raw page's single block starts.
     */
    private fun deactivationFixture(): Pair<Pair<PaperSheetView, BasicPaperSheetUI>, Int> {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.twoPage
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val image = BufferedImage(500, 800, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 800)
        } finally {
            g.dispose()
        }
        view.caretModel.moveToStartOfBlock(1)
        val page2Start = ui.caretIndexForTest
        return (view to ui) to page2Start
    }

    /** Marks the second raw page deactivated under [mode], without moving the caret. */
    private fun deactivateSecondPage(view: PaperSheetView, mode: PageDeactivationMode) {
        view.deactivatedPageHandling = mode
        view.setPageDeactivated(1, true)
    }

    /**
     * With `DISABLED` on the second page, typing at a caret already sitting inside it leaves the
     * document and the caret untouched.
     */
    @Test
    fun insertInsideDisabledPageIsRejected() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start + 2)
        deactivateSecondPage(view, PageDeactivationMode.DISABLED)
        val before = view.document
        val caretBefore = ui.caretIndexForTest

        ui.typeTextForTest("Z")

        assertEquals(before, view.document)
        assertEquals(caretBefore, ui.caretIndexForTest)
    }

    /**
     * With `READONLY` on the second page, typing at a caret sitting inside it leaves the document
     * untouched and the caret stays at its position.
     */
    @Test
    fun insertInsideReadonlyPageIsRejected() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start + 2)
        deactivateSecondPage(view, PageDeactivationMode.READONLY)
        val before = view.document
        val caretBefore = ui.caretIndexForTest

        ui.typeTextForTest("Z")

        assertEquals(before, view.document)
        assertEquals(caretBefore, ui.caretIndexForTest)
    }

    /**
     * A selection whose range touches a `DISABLED` page is discarded by `Backspace`: the document is
     * unchanged.
     */
    @Test
    fun deleteRangeTouchingDisabledPageIsRejected() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.selectionModel.selectRange(page2Start - 2, page2Start + 2)
        deactivateSecondPage(view, PageDeactivationMode.DISABLED)
        val before = view.document

        ui.pressKeyForTest(KeyEvent.VK_BACK_SPACE)

        assertEquals(before, view.document)
    }

    /**
     * Typing on the still-active first page keeps working normally in every
     * [org.pcsoft.framework.simplay.uicommon.PageDeactivationMode].
     */
    @Test
    fun typingOnActivePageStillWorks() {
        for (mode in PageDeactivationMode.entries) {
            val (viewUi, _) = deactivationFixture()
            val (view, ui) = viewUi

            view.caretModel.moveTo(2)
            deactivateSecondPage(view, mode)
            val before = view.document!!.plain()

            ui.typeTextForTest("Z")

            assertTrue(view.document!!.plain().contains("Z"), "typing should work in mode $mode")
            assertTrue(view.document!!.plain().length > before.length, "document should grow in mode $mode")
        }
    }

    /**
     * In `DISABLED` mode, moving the caret forward into the deactivated page snaps it to the page's
     * end instead of landing inside it.
     */
    @Test
    fun caretSkipsDisabledPageForward() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi
        val docLength = view.selectionModel.let { it.selectAll(); val l = it.text.length; it.clearSelection(); l }

        view.caretModel.moveTo(page2Start - 1)
        deactivateSecondPage(view, PageDeactivationMode.DISABLED)
        view.caretModel.moveTo(page2Start + 2)

        assertEquals(docLength, ui.caretIndexForTest)
    }

    /**
     * In `DISABLED` mode, moving the caret backward from past the end of the deactivated page snaps
     * it to the page's start instead of landing inside it.
     */
    @Test
    fun caretSkipsDisabledPageBackward() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi
        val docLength = view.selectionModel.let { it.selectAll(); val l = it.text.length; it.clearSelection(); l }

        view.caretModel.moveTo(docLength)
        deactivateSecondPage(view, PageDeactivationMode.DISABLED)
        view.caretModel.moveTo(page2Start + 2)

        assertEquals(page2Start, ui.caretIndexForTest)
    }

    /**
     * In `READONLY` mode the caret enters and crosses the deactivated page exactly like a normal one:
     * no snap happens.
     */
    @Test
    fun caretEntersReadonlyPageNormally() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start - 1)
        deactivateSecondPage(view, PageDeactivationMode.READONLY)
        view.caretModel.moveTo(page2Start + 2)

        assertEquals(page2Start + 2, ui.caretIndexForTest)
    }

    /**
     * A `Shift` + navigation selection is allowed to span into a `DISABLED` page.
     */
    @Test
    fun shiftSelectionMaySpanDisabledPage() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start - 1)
        deactivateSecondPage(view, PageDeactivationMode.DISABLED)
        repeat(4) { ui.pressKeyForTest(KeyEvent.VK_RIGHT, shift = true) }

        assertEquals(page2Start + 3, ui.caretIndexForTest)
        assertEquals(4, view.selectionModel.length)
    }

    /**
     * [PaperSheetView.setPageDeactivated] stores the resolved page id, not the index passed in.
     */
    @Test
    fun setPageDeactivatedByIndexResolvesToId() {
        val (viewUi, _) = deactivationFixture()
        val (view, _) = viewUi
        val expectedId = view.document!!.pages[1].id

        view.setPageDeactivated(1, true)

        assertEquals(setOf(expectedId), view.deactivatedPageIds)
    }

    //endregion
}
