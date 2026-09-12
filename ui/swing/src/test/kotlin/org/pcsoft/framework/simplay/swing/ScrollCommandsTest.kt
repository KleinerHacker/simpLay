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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the [PaperSheetView] scroll commands (`scrollToPage`, `scrollToBlock`, `scrollToWord`,
 * `scrollToSymbol`): a pure viewport operation available in every [PaperSheetMode], unlike the
 * [CaretModel] commands. Everything is painted into an off-screen image; no window is opened.
 */
class ScrollCommandsTest {

    private fun ui(view: PaperSheetView): BasicPaperSheetUI = view.getPaperSheetUI() as BasicPaperSheetUI

    private fun paint(ui: BasicPaperSheetUI, width: Int = 500, height: Int = 300) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, width, height)
        } finally {
            g.dispose()
        }
    }

    /** `scrollToPage` produces strictly increasing scroll positions for later pages. */
    @Test
    fun scrollToPageProducesIncreasingPositions() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)
        assertTrue(ui.pageCountForTest > 3, "fixture should span several pages, was ${ui.pageCountForTest}")

        view.scrollToPage(0)
        val p0 = ui.verticalScrollBarForTest.value
        view.scrollToPage(1)
        val p1 = ui.verticalScrollBarForTest.value
        view.scrollToPage(2)
        val p2 = ui.verticalScrollBarForTest.value

        assertTrue(p0 < p1, "page 0 ($p0) should scroll less than page 1 ($p1)")
        assertTrue(p1 < p2, "page 1 ($p1) should scroll less than page 2 ($p2)")
    }

    /** `scrollToBlock` reaches further down for a later block, and moves the viewport at all. */
    @Test
    fun scrollToBlockProducesNonDecreasingPositions() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)
        val lastBlock = view.caretModel.blockCount - 1

        view.scrollToBlock(0)
        val first = ui.verticalScrollBarForTest.value
        view.scrollToBlock(lastBlock)
        val last = ui.verticalScrollBarForTest.value

        assertTrue(first <= last, "first block ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0)
    }

    /** `scrollToWord` reaches further down for a later word, and moves the viewport at all. */
    @Test
    fun scrollToWordProducesNonDecreasingPositions() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)
        val lastWord = view.caretModel.wordCount - 1

        view.scrollToWord(0)
        val first = ui.verticalScrollBarForTest.value
        view.scrollToWord(lastWord)
        val last = ui.verticalScrollBarForTest.value

        assertTrue(first <= last, "first word ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0)
    }

    /** `scrollToSymbol` reaches further down for a later symbol, and moves the viewport at all. */
    @Test
    fun scrollToSymbolProducesNonDecreasingPositions() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)
        val lastSymbol = view.caretModel.symbolCount - 1

        view.scrollToSymbol(0)
        val first = ui.verticalScrollBarForTest.value
        view.scrollToSymbol(lastSymbol)
        val last = ui.verticalScrollBarForTest.value

        assertTrue(first <= last, "first symbol ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0)
    }

    /** An out-of-range page index clamps to the last navigable page instead of throwing. */
    @Test
    fun outOfRangePageClampsInsteadOfThrowing() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)

        view.scrollToPage(ui.pageCountForTest - 1)
        val expected = ui.verticalScrollBarForTest.value
        view.scrollToPage(Int.MAX_VALUE)

        assertEquals(expected, ui.verticalScrollBarForTest.value)
    }

    /** An out-of-range block ordinal clamps to the last block instead of throwing. */
    @Test
    fun outOfRangeBlockClampsInsteadOfThrowing() {
        val view = PaperSheetView().apply { document = TestDocuments.long }
        val ui = ui(view)
        paint(ui)
        val lastBlock = view.caretModel.blockCount - 1

        view.scrollToBlock(lastBlock)
        val expected = ui.verticalScrollBarForTest.value
        view.scrollToBlock(Int.MAX_VALUE)

        assertEquals(expected, ui.verticalScrollBarForTest.value)
    }

    /**
     * The scroll commands move the viewport in [PaperSheetMode.STATIC], which has no caret and no
     * selection at all - the key behavioural difference from the [CaretModel] commands.
     */
    @Test
    fun scrollCommandsWorkInStaticModeWithoutACaret() {
        val view = PaperSheetView().apply {
            mode = PaperSheetMode.STATIC
            document = TestDocuments.long
        }
        val ui = ui(view)
        paint(ui)

        view.scrollToPage(2)

        assertTrue(ui.verticalScrollBarForTest.value > 0)
    }

    /** The scroll commands are a no-op, not a crash, on a view without a document. */
    @Test
    fun scrollCommandsDoNothingWithoutADocument() {
        val view = PaperSheetView()
        val ui = ui(view)
        paint(ui)

        view.scrollToPage(0)
        view.scrollToBlock(0)
        view.scrollToWord(0)
        view.scrollToSymbol(0)

        assertEquals(0, ui.verticalScrollBarForTest.value)
    }

    /** `scrollToPage` on a document that fits entirely in the viewport does not throw and stays at `0`. */
    @Test
    fun scrollToPageOnADocumentThatFitsTheViewportDoesNotThrow() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        val ui = ui(view)
        paint(ui, height = 2000)

        view.scrollToPage(0)

        assertEquals(0, ui.verticalScrollBarForTest.value)
    }

    /**
     * A scroll command issued right after construction - before the delegate has ever painted, so
     * before its scrollbar range reflects real content - still ends up at the same position a command
     * issued after the first paint would, instead of being clamped to a stale (too small) maximum.
     */
    @Test
    fun scrollBeforeFirstPaintIsNotClampedToAStaleMaximum() {
        val early = PaperSheetView().apply { document = TestDocuments.long }
        early.scrollToPage(5)
        val uiEarly = ui(early)
        paint(uiEarly)

        val later = PaperSheetView().apply { document = TestDocuments.long }
        val uiLater = ui(later)
        paint(uiLater)
        later.scrollToPage(5)

        assertEquals(uiLater.verticalScrollBarForTest.value, uiEarly.verticalScrollBarForTest.value)
    }
}
