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
import org.pcsoft.framework.simplay.uicommon.PageMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    /** Paints [view] off-screen once and returns it together with its UI delegate. */
    private fun paintedView(mode: PaperSheetMode): Pair<PaperSheetView, BasicPaperSheetUI> {
        val view = PaperSheetView().apply {
            this.mode = mode
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
     * Verifies that [PaperSheetMode.NAVIGABLE] moves the caret with the navigation keys but drops
     * every mutating key, so the document instance stays untouched.
     */
    @Test
    fun navigableModeMovesTheCaretButRejectsEdits() {
        val (view, ui) = paintedView(PaperSheetMode.NAVIGABLE)
        val before = view.document

        ui.pressKeyForTest(KeyEvent.VK_RIGHT)
        ui.typeTextForTest("Z")
        ui.pressKeyForTest(KeyEvent.VK_BACK_SPACE)
        ui.pressKeyForTest(KeyEvent.VK_DELETE)

        assertEquals(before, view.document)
        assertEquals(1, ui.caretIndexForTest)
    }

    /**
     * Verifies that [PaperSheetMode.STATIC] ignores every key, keeps the caret at the document start
     * and refuses a programmatic `selectAll`, so the document is shown like a plain picture.
     */
    @Test
    fun staticModeRejectsKeysCaretAndSelection() {
        val (view, ui) = paintedView(PaperSheetMode.STATIC)
        val before = view.document

        ui.pressKeyForTest(KeyEvent.VK_RIGHT)
        ui.typeTextForTest("Z")
        view.selectionModel.selectAll()
        view.caretModel.moveToEnd()

        assertEquals(before, view.document)
        assertEquals(0, ui.caretIndexForTest)
        assertTrue(view.selectionModel.isEmpty)
    }

    /**
     * Verifies that replacing the document from outside resets the caret to the document start and
     * scrolls the viewport back to the top, no matter where the caret stood before.
     */
    @Test
    fun replacingTheDocumentResetsCaretAndScroll() {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.long
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val image = BufferedImage(500, 200, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 200)
        } finally {
            g.dispose()
        }
        view.caretModel.moveToEnd()
        assertTrue(ui.caretIndexForTest > 0)
        assertTrue(ui.verticalScrollBarForTest.value > 0)

        view.document = TestDocuments.short

        assertEquals(0, ui.caretIndexForTest)
        assertEquals(0, ui.verticalScrollBarForTest.value)
    }

    /**
     * Verifies that typing replaces the document instance as well, but is not treated as a reload:
     * the caret stays behind the typed character instead of jumping back to the document start.
     */
    @Test
    fun typingKeepsTheCaretDespiteTheDocumentChange() {
        val (_, ui) = editableView()

        ui.placeCaretAtForTest(35.0, 45.0)
        val before = ui.caretIndexForTest
        ui.typeTextForTest("X")

        assertEquals(before + 1, ui.caretIndexForTest)
    }

    /**
     * Verifies that moving the caret to the end of a document taller than the viewport scrolls the
     * view down, so that the caret stays inside the visible area.
     */
    @Test
    fun caretMoveScrollsTheViewportToTheCaret() {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.long
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val image = BufferedImage(500, 200, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 200)
        } finally {
            g.dispose()
        }
        assertTrue(ui.verticalScrollBarForTest.isEnabled)

        view.caretModel.moveToEnd()

        assertTrue(ui.verticalScrollBarForTest.value > 0)
        val bounds = assertNotNull(ui.caretBoundsForTest())
        assertTrue(bounds.y >= 0)
        assertTrue(bounds.y + bounds.height <= 200)
    }

    /**
     * Verifies that an editable view which is not the focus owner paints no caret at all: the caret
     * opacity stays at zero, so an unfocused sheet does not show a misleading caret.
     */
    @Test
    fun unfocusedEditableViewHidesCaret() {
        val (view, ui) = editableView()

        assertFalse(view.isFocusOwner)
        assertEquals(0.0, ui.caretOpacityForTest(), 1e-9)
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

    /** Gives the second raw page its own [mode], without moving the caret. */
    private fun overrideSecondPage(view: PaperSheetView, mode: PageMode) {
        view.setPageMode(1, mode)
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
        overrideSecondPage(view, PageMode.DISABLED)
        val before = view.document
        val caretBefore = ui.caretIndexForTest

        ui.typeTextForTest("Z")

        assertEquals(before, view.document)
        assertEquals(caretBefore, ui.caretIndexForTest)
    }

    /**
     * With `NAVIGABLE` on the second page, typing at a caret sitting inside it leaves the document
     * untouched and the caret stays at its position.
     */
    @Test
    fun insertInsideNavigablePageIsRejected() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start + 2)
        overrideSecondPage(view, PageMode.NAVIGABLE)
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
        overrideSecondPage(view, PageMode.DISABLED)
        val before = view.document

        ui.pressKeyForTest(KeyEvent.VK_BACK_SPACE)

        assertEquals(before, view.document)
    }

    /**
     * Typing on the first page, which has no own mode, keeps working normally for every
     * [org.pcsoft.framework.simplay.uicommon.PageMode] of the second page.
     */
    @Test
    fun typingOnActivePageStillWorks() {
        for (mode in PageMode.entries) {
            val (viewUi, _) = deactivationFixture()
            val (view, ui) = viewUi

            view.caretModel.moveTo(2)
            overrideSecondPage(view, mode)
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
        overrideSecondPage(view, PageMode.DISABLED)
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
        overrideSecondPage(view, PageMode.DISABLED)
        view.caretModel.moveTo(page2Start + 2)

        assertEquals(page2Start, ui.caretIndexForTest)
    }

    /**
     * In `NAVIGABLE` mode the caret enters and crosses the overridden page exactly like a normal one:
     * no snap happens.
     */
    @Test
    fun caretEntersNavigablePageNormally() {
        val (viewUi, page2Start) = deactivationFixture()
        val (view, ui) = viewUi

        view.caretModel.moveTo(page2Start - 1)
        overrideSecondPage(view, PageMode.NAVIGABLE)
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
        overrideSecondPage(view, PageMode.DISABLED)
        repeat(4) { ui.pressKeyForTest(KeyEvent.VK_RIGHT, shift = true) }

        assertEquals(page2Start + 3, ui.caretIndexForTest)
        assertEquals(4, view.selectionModel.length)
    }

    /**
     * [PaperSheetView.setPageMode] stores the resolved page id, not the index passed in.
     */
    @Test
    fun setPageModeByIndexResolvesToId() {
        val (viewUi, _) = deactivationFixture()
        val (view, _) = viewUi
        val expectedId = view.document!!.pages[1].id

        view.setPageMode(1, PageMode.DISABLED)

        assertEquals(mapOf(expectedId to PageMode.DISABLED), view.pageModes)
    }

    /**
     * An edit replaces [PaperSheetView.document] with a new instance, but that is not a reload from
     * outside, so an existing [PaperSheetView.pageModes] override must survive it untouched.
     */
    @Test
    fun editingDocumentDoesNotResetPageModes() {
        val (viewUi, _) = deactivationFixture()
        val (view, ui) = viewUi
        view.setPageMode(0, PageMode.DISABLED)

        view.caretModel.moveTo(1)
        ui.typeTextForTest("Z")

        assertEquals(mapOf(view.document!!.pages[0].id to PageMode.DISABLED), view.pageModes)
    }

    /**
     * Replacing [PaperSheetView.document] with an entirely different document - a reload from outside,
     * not an edit - drops every [PaperSheetView.pageModes] override because the previous page ids no
     * longer apply.
     */
    @Test
    fun reloadingDocumentResetsPageModes() {
        val (viewUi, _) = deactivationFixture()
        val (view, _) = viewUi
        view.setPageMode(0, PageMode.DISABLED)
        assertTrue(view.pageModes.isNotEmpty())

        view.document = TestDocuments.short

        assertEquals(emptyMap(), view.pageModes)
    }

    //endregion
}
