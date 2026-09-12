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

package org.pcsoft.framework.simplay.fx.internal.ps

import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.fx.JavaFxTestBase
import org.pcsoft.framework.simplay.fx.PaperSheetMode
import org.pcsoft.framework.simplay.fx.PaperSheetTestFixtures
import org.pcsoft.framework.simplay.fx.PaperSheetView
import org.pcsoft.framework.simplay.fx.PaperSheetViewSkin
import org.pcsoft.framework.simplay.uicommon.CaretMode
import org.pcsoft.framework.simplay.uicommon.PageMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Headless tests for [PaperSheetCaret.movePage] (`Page Up` / `Page Down` navigation) and
 * [PaperSheetCaret.toggleCaretMode] (the `Insert` insert/overwrite typing-mode toggle and its block
 * cursor), over [PaperSheetTestFixtures.variableLinePagesDocument]: a document whose first two pages
 * have five single-line paragraphs each and whose third page has only two.
 */
class PaperSheetCaretTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(): Fixture = onFxThread {
        val view = PaperSheetView()
        view.mode = PaperSheetMode.EDITABLE
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 900.0)
        stage.show()
        view.document = PaperSheetTestFixtures.variableLinePagesDocument()
        view.applyCss()
        view.layout()
        view.requestFocus()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /** Linear index where global block [block] (0-based, across all pages) starts. */
    private fun Fixture.blockStart(block: Int): Int {
        onFxThread { view.caretModel.moveToStartOfBlock(block) }
        return skin.caretIndexForTest
    }

    /** The document's whole plain text, via a select-all / read / clear round trip. */
    private fun Fixture.text(): String = onFxThread {
        view.selectionModel.selectAll()
        val t = view.selectionModel.text
        view.selectionModel.clearSelection()
        t
    }

    /**
     * `Page Down` from the third line of the first page lands on the third line of the second page:
     * both pages have five lines, so the relative line index is kept exactly.
     */
    @Test
    fun pageDownKeepsRelativeLineIndexWithEqualLineCounts() {
        val fx = fixture()
        val targetStart = fx.blockStart(7)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(2) }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN) }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
    }

    /**
     * `Page Up` from the third line of the second page lands back on the third line of the first
     * page, the inverse of [pageDownKeepsRelativeLineIndexWithEqualLineCounts].
     */
    @Test
    fun pageUpKeepsRelativeLineIndexWithEqualLineCounts() {
        val fx = fixture()
        val targetStart = fx.blockStart(2)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(7) }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_UP) }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
    }

    /**
     * `Page Down` from the last (fifth) line of the second page lands on the last (second) line of
     * the third page, which has fewer lines: the relative line index is clamped instead of failing.
     */
    @Test
    fun pageDownClampsToLastLineOfShorterTargetPage() {
        val fx = fixture()
        val targetStart = fx.blockStart(11)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(9) }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN) }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
    }

    /** `Page Up` on the first navigable page is a no-op: it does not wrap to the last page. */
    @Test
    fun pageUpOnFirstPageDoesNotWrap() {
        val fx = fixture()
        val start = fx.blockStart(2)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_UP) }

        assertEquals(start, fx.skin.caretIndexForTest)
    }

    /** `Page Down` on the last navigable page is a no-op: it does not wrap to the first page. */
    @Test
    fun pageDownOnLastPageDoesNotWrap() {
        val fx = fixture()
        val start = fx.blockStart(11)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN) }

        assertEquals(start, fx.skin.caretIndexForTest)
    }

    /**
     * With the second page set to [PageMode.DISABLED], a single `Page Down` from the first page
     * jumps straight to the third page instead of stopping on the disabled one in between.
     */
    @Test
    fun pageDownSkipsANonNavigablePageInBetween() {
        val fx = fixture()
        val targetStart = fx.blockStart(11)
        onFxThread {
            fx.view.caretModel.moveToStartOfBlock(2)
            fx.view.setPageMode(1, PageMode.DISABLED)
        }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN) }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
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
        onFxThread { fx.view.caretModel.moveTo(sourceStart) }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN) }
        assertEquals(targetStart, fx.skin.caretIndexForTest)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_UP) }
        assertEquals(sourceStart, fx.skin.caretIndexForTest)
    }

    /** `Shift` + `Page Down` extends the selection from the caret to the target line, like `Shift` + `Down`. */
    @Test
    fun shiftPageDownExtendsTheSelection() {
        val fx = fixture()
        val sourceStart = fx.blockStart(2)
        val targetStart = fx.blockStart(7)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(2) }

        onFxThread { fx.skin.pressKeyForTest(KeyCode.PAGE_DOWN, shift = true) }

        assertEquals(sourceStart, fx.view.selectionModel.startIndex)
        assertEquals(targetStart, fx.view.selectionModel.endIndex)
    }

    /** [org.pcsoft.framework.simplay.fx.CaretModel.moveToNextPage] behaves like `Page Down` without `Shift`. */
    @Test
    fun caretModelMoveToNextPageMatchesThePageDownKey() {
        val fx = fixture()
        val targetStart = fx.blockStart(7)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(2) }

        onFxThread { fx.view.caretModel.moveToNextPage() }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
    }

    /** [org.pcsoft.framework.simplay.fx.CaretModel.moveToPrevPage] behaves like `Page Up` without `Shift`. */
    @Test
    fun caretModelMoveToPrevPageMatchesThePageUpKey() {
        val fx = fixture()
        val targetStart = fx.blockStart(2)
        onFxThread { fx.view.caretModel.moveToStartOfBlock(7) }

        onFxThread { fx.view.caretModel.moveToPrevPage() }

        assertEquals(targetStart, fx.skin.caretIndexForTest)
    }

    /** `Insert` toggles [PaperSheetView.caretMode] from [CaretMode.INSERT] to [CaretMode.OVERWRITE] and back. */
    @Test
    fun insertKeyTogglesCaretModeInsertOverwriteInsert() {
        val fx = fixture()
        assertEquals(CaretMode.INSERT, fx.view.caretMode)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.INSERT) }
        assertEquals(CaretMode.OVERWRITE, fx.view.caretMode)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.INSERT) }
        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }

    /** [PaperSheetView.caretMode] is a plain external property: readable and settable without a key press. */
    @Test
    fun caretModeIsDirectlyReadableAndWritable() {
        val fx = fixture()

        onFxThread { fx.view.caretMode = CaretMode.OVERWRITE }
        assertEquals(CaretMode.OVERWRITE, fx.view.caretMode)

        onFxThread { fx.view.caretMode = CaretMode.INSERT }
        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }

    /** In [CaretMode.OVERWRITE], typing replaces the character at the caret instead of inserting. */
    @Test
    fun overwriteReplacesCharacterAtCaretInsteadOfInserting() {
        val fx = fixture()
        val pos = fx.blockStart(0) + 5
        onFxThread {
            fx.view.caretMode = CaretMode.OVERWRITE
            fx.view.caretModel.moveTo(pos)
        }
        val before = fx.text()

        onFxThread { fx.skin.typeTextForTest("X") }

        val after = fx.text()
        assertEquals(before.length, after.length, "overwrite must replace, not grow, the text")
        assertEquals('X', after[pos])
        assertEquals(pos + 1, fx.skin.caretIndexForTest)
    }

    /** In [CaretMode.OVERWRITE], typing at the end of a line falls back to a plain insert. */
    @Test
    fun overwriteAtEndOfLineFallsBackToInsert() {
        val fx = fixture()
        onFxThread {
            fx.view.caretMode = CaretMode.OVERWRITE
            fx.view.caretModel.moveToEndOfBlock(0)
        }
        val before = fx.text()

        onFxThread { fx.skin.typeTextForTest("X") }

        val after = fx.text()
        assertEquals(before.length + 1, after.length)
    }

    /** The painted/public caret bounds widen to a block in [CaretMode.OVERWRITE] and shrink back. */
    @Test
    fun blockCursorWidensInOverwriteAndShrinksBackInInsert() {
        val fx = fixture()
        onFxThread { fx.view.caretModel.moveTo(fx.blockStart(0) + 2) }
        val insertWidth = fx.skin.caretBoundsForTest()!!.width

        onFxThread { fx.skin.pressKeyForTest(KeyCode.INSERT) }
        val overwriteWidth = fx.skin.caretBoundsForTest()!!.width

        onFxThread { fx.skin.pressKeyForTest(KeyCode.INSERT) }
        val insertWidthAgain = fx.skin.caretBoundsForTest()!!.width

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
        onFxThread { fx.view.mode = PaperSheetMode.NAVIGABLE }
        assertEquals(CaretMode.INSERT, fx.view.caretMode)

        onFxThread { fx.skin.pressKeyForTest(KeyCode.INSERT) }

        assertEquals(CaretMode.INSERT, fx.view.caretMode)
    }
}
