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

package org.pcsoft.framework.simplay.swing.internal.ps

import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import org.pcsoft.framework.simplay.swing.BasicPaperSheetUI
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.swing.TestDocuments
import org.pcsoft.framework.simplay.uicommon.CaretMode
import org.pcsoft.framework.simplay.uicommon.PageMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [PaperSheetCaret.movePage] (`Page Up` / `Page Down` navigation) and
 * [PaperSheetCaret.toggleCaretMode] (the `Insert` insert/overwrite typing-mode toggle and its block
 * cursor), over [TestDocuments.variableLinePages]: a document whose first two pages have five
 * single-line paragraphs each and whose third page has only two.
 */
class PaperSheetCaretTest {

    private data class Fixture(val view: PaperSheetView, val ui: BasicPaperSheetUI)

    private fun fixture(): Fixture {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.EDITABLE
            document = TestDocuments.variableLinePages
        }
        val ui = view.getPaperSheetUI() as BasicPaperSheetUI
        val image = BufferedImage(500, 1200, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, 500, 1200)
        } finally {
            g.dispose()
        }
        return Fixture(view, ui)
    }

    /** Linear index where global block [block] (0-based, across all pages) starts. */
    private fun Fixture.blockStart(block: Int): Int {
        view.caretModel.moveToStartOfBlock(block)
        return ui.caretIndexForTest
    }

    /** The document's whole plain text, via a select-all / read / clear round trip. */
    private fun Fixture.text(): String {
        view.selectionModel.selectAll()
        val t = view.selectionModel.text
        view.selectionModel.clearSelection()
        return t
    }

    /**
     * `Page Down` from the third line of the first page lands on the third line of the second page:
     * both pages have five lines, so the relative line index is kept exactly.
     */
    @Test
    fun pageDownKeepsRelativeLineIndexWithEqualLineCounts() {
        val fx = fixture()
        val targetStart = fx.blockStart(7)
        fx.view.caretModel.moveToStartOfBlock(2)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN)

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /**
     * `Page Up` from the third line of the second page lands back on the third line of the first
     * page, the inverse of [pageDownKeepsRelativeLineIndexWithEqualLineCounts].
     */
    @Test
    fun pageUpKeepsRelativeLineIndexWithEqualLineCounts() {
        val fx = fixture()
        val targetStart = fx.blockStart(2)
        fx.view.caretModel.moveToStartOfBlock(7)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_UP)

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /**
     * `Page Down` from the last (fifth) line of the second page lands on the last (second) line of
     * the third page, which has fewer lines: the relative line index is clamped instead of failing.
     */
    @Test
    fun pageDownClampsToLastLineOfShorterTargetPage() {
        val fx = fixture()
        val targetStart = fx.blockStart(11)
        fx.view.caretModel.moveToStartOfBlock(9)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN)

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /** `Page Up` on the first navigable page is a no-op: it does not wrap to the last page. */
    @Test
    fun pageUpOnFirstPageDoesNotWrap() {
        val fx = fixture()
        val start = fx.blockStart(2)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_UP)

        assertEquals(start, fx.ui.caretIndexForTest)
    }

    /** `Page Down` on the last navigable page is a no-op: it does not wrap to the first page. */
    @Test
    fun pageDownOnLastPageDoesNotWrap() {
        val fx = fixture()
        val start = fx.blockStart(11)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN)

        assertEquals(start, fx.ui.caretIndexForTest)
    }

    /**
     * With the second page set to [PageMode.DISABLED], a single `Page Down` from the first page
     * jumps straight to the third page instead of stopping on the disabled one in between.
     */
    @Test
    fun pageDownSkipsANonNavigablePageInBetween() {
        val fx = fixture()
        val targetStart = fx.blockStart(11)
        fx.view.caretModel.moveToStartOfBlock(2)
        fx.view.setPageMode(1, PageMode.DISABLED)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN)

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /**
     * The wish-x column is preserved across a page jump and its inverse: starting four characters
     * into the third line (still inside the "Page" prefix both pages' text share), `Page Down` then
     * `Page Up` return the caret to the exact starting offset.
     */
    @Test
    fun columnIsPreservedAcrossThePageJumpAndBack() {
        val fx = fixture()
        val sourceStart = fx.blockStart(2) + 4
        val targetStart = fx.blockStart(7) + 4
        fx.view.caretModel.moveTo(sourceStart)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN)
        assertEquals(targetStart, fx.ui.caretIndexForTest)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_UP)
        assertEquals(sourceStart, fx.ui.caretIndexForTest)
    }

    /** `Shift` + `Page Down` extends the selection from the caret to the target line, like `Shift` + `Down`. */
    @Test
    fun shiftPageDownExtendsTheSelection() {
        val fx = fixture()
        val sourceStart = fx.blockStart(2)
        val targetStart = fx.blockStart(7)
        fx.view.caretModel.moveToStartOfBlock(2)

        fx.ui.pressKeyForTest(KeyEvent.VK_PAGE_DOWN, shift = true)

        assertEquals(sourceStart, fx.view.selectionModel.startIndex)
        assertEquals(targetStart, fx.view.selectionModel.endIndex)
    }

    /** [org.pcsoft.framework.simplay.swing.CaretModel.moveToNextPage] behaves like `Page Down` without `Shift`. */
    @Test
    fun caretModelMoveToNextPageMatchesThePageDownKey() {
        val fx = fixture()
        val targetStart = fx.blockStart(7)
        fx.view.caretModel.moveToStartOfBlock(2)

        fx.view.caretModel.moveToNextPage()

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /** [org.pcsoft.framework.simplay.swing.CaretModel.moveToPrevPage] behaves like `Page Up` without `Shift`. */
    @Test
    fun caretModelMoveToPrevPageMatchesThePageUpKey() {
        val fx = fixture()
        val targetStart = fx.blockStart(2)
        fx.view.caretModel.moveToStartOfBlock(7)

        fx.view.caretModel.moveToPrevPage()

        assertEquals(targetStart, fx.ui.caretIndexForTest)
    }

    /** `Insert` toggles [PaperSheetView.caretMode] from [CaretMode.INSERT] to [CaretMode.OVERWRITE] and back. */
    @Test
    fun insertKeyTogglesCaretModeInsertOverwriteInsert() {
        val fx = fixture()
        assertEquals(CaretMode.INSERT, fx.view.caretMode)

        fx.ui.pressKeyForTest(KeyEvent.VK_INSERT)
        assertEquals(CaretMode.OVERWRITE, fx.view.caretMode)

        fx.ui.pressKeyForTest(KeyEvent.VK_INSERT)
        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }

    /** [PaperSheetView.caretMode] is a plain external property: readable and settable without a key press. */
    @Test
    fun caretModeIsDirectlyReadableAndWritable() {
        val fx = fixture()

        fx.view.caretMode = CaretMode.OVERWRITE
        assertEquals(CaretMode.OVERWRITE, fx.view.caretMode)

        fx.view.caretMode = CaretMode.INSERT
        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }

    /** In [CaretMode.OVERWRITE], typing replaces the character at the caret instead of inserting. */
    @Test
    fun overwriteReplacesCharacterAtCaretInsteadOfInserting() {
        val fx = fixture()
        val pos = fx.blockStart(0) + 5
        fx.view.caretMode = CaretMode.OVERWRITE
        fx.view.caretModel.moveTo(pos)
        val before = fx.text()

        fx.ui.typeTextForTest("X")

        val after = fx.text()
        assertEquals(before.length, after.length, "overwrite must replace, not grow, the text")
        assertEquals('X', after[pos])
        assertEquals(pos + 1, fx.ui.caretIndexForTest)
    }

    /** In [CaretMode.OVERWRITE], typing at the end of a line falls back to a plain insert. */
    @Test
    fun overwriteAtEndOfLineFallsBackToInsert() {
        val fx = fixture()
        fx.view.caretMode = CaretMode.OVERWRITE
        fx.view.caretModel.moveToEndOfBlock(0)
        val before = fx.text()

        fx.ui.typeTextForTest("X")

        val after = fx.text()
        assertEquals(before.length + 1, after.length)
    }

    /** The painted/public caret bounds widen to a block in [CaretMode.OVERWRITE] and shrink back. */
    @Test
    fun blockCursorWidensInOverwriteAndShrinksBackInInsert() {
        val fx = fixture()
        fx.view.caretModel.moveTo(fx.blockStart(0) + 2)
        val insertWidth = fx.ui.caretBoundsForTest()!!.width

        fx.ui.pressKeyForTest(KeyEvent.VK_INSERT)
        val overwriteWidth = fx.ui.caretBoundsForTest()!!.width

        fx.ui.pressKeyForTest(KeyEvent.VK_INSERT)
        val insertWidthAgain = fx.ui.caretBoundsForTest()!!.width

        assertTrue(
            overwriteWidth > insertWidth,
            "block width ($overwriteWidth) should exceed the thin line width ($insertWidth)",
        )
        assertEquals(insertWidth, insertWidthAgain)
    }

    /** `Insert` does not toggle [PaperSheetView.caretMode] in a non-editable mode. */
    @Test
    fun insertKeyIsNoOpInNonEditableMode() {
        val fx = fixture()
        fx.view.mode = PaperSheetMode.NAVIGABLE
        assertEquals(CaretMode.INSERT, fx.view.caretMode)

        fx.ui.pressKeyForTest(KeyEvent.VK_INSERT)

        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }
}
