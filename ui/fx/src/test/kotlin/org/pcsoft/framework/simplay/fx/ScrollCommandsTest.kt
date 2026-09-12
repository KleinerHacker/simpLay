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

import javafx.scene.Scene
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Headless tests for the [PaperSheetView] scroll commands (`scrollToPage`, `scrollToBlock`,
 * `scrollToWord`, `scrollToSymbol`): a pure viewport operation available in every [PaperSheetMode],
 * unlike the [CaretModel] commands.
 */
class ScrollCommandsTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(mode: PaperSheetMode = PaperSheetMode.SELECTABLE, paragraphs: Int = 40): Fixture = onFxThread {
        val view = PaperSheetView()
        view.mode = mode
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /** `scrollToPage` produces strictly increasing scroll positions for later pages. */
    @Test
    fun scrollToPageProducesIncreasingPositions() {
        val (view, skin) = fixture()
        assertTrue(skin.pageCount > 3, "fixture should span several pages, was ${skin.pageCount}")

        onFxThread { view.scrollToPage(0) }
        val p0 = skin.verticalScrollBar.value
        onFxThread { view.scrollToPage(1) }
        val p1 = skin.verticalScrollBar.value
        onFxThread { view.scrollToPage(2) }
        val p2 = skin.verticalScrollBar.value

        assertTrue(p0 < p1, "page 0 ($p0) should scroll less than page 1 ($p1)")
        assertTrue(p1 < p2, "page 1 ($p1) should scroll less than page 2 ($p2)")
    }

    /** `scrollToBlock` reaches further down for a later block, and moves the viewport at all. */
    @Test
    fun scrollToBlockProducesNonDecreasingPositions() {
        val (view, skin) = fixture()
        val lastBlock = view.caretModel.blockCount - 1

        onFxThread { view.scrollToBlock(0) }
        val first = skin.verticalScrollBar.value
        onFxThread { view.scrollToBlock(lastBlock) }
        val last = skin.verticalScrollBar.value

        assertTrue(first <= last, "first block ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0.0)
    }

    /** `scrollToWord` reaches further down for a later word, and moves the viewport at all. */
    @Test
    fun scrollToWordProducesNonDecreasingPositions() {
        val (view, skin) = fixture()
        val lastWord = view.caretModel.wordCount - 1

        onFxThread { view.scrollToWord(0) }
        val first = skin.verticalScrollBar.value
        onFxThread { view.scrollToWord(lastWord) }
        val last = skin.verticalScrollBar.value

        assertTrue(first <= last, "first word ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0.0)
    }

    /** `scrollToSymbol` reaches further down for a later symbol, and moves the viewport at all. */
    @Test
    fun scrollToSymbolProducesNonDecreasingPositions() {
        val (view, skin) = fixture()
        val lastSymbol = view.caretModel.symbolCount - 1

        onFxThread { view.scrollToSymbol(0) }
        val first = skin.verticalScrollBar.value
        onFxThread { view.scrollToSymbol(lastSymbol) }
        val last = skin.verticalScrollBar.value

        assertTrue(first <= last, "first symbol ($first) should not scroll further than the last ($last)")
        assertTrue(last > 0.0)
    }

    /**
     * A scroll command issued before the view has a skin is buffered and applied once the skin is
     * attached, exactly like the [CaretModel] commands.
     */
    @Test
    fun commandBeforeSkinIsAppliedOnSkinInit() {
        val view = onFxThread {
            val v = PaperSheetView()
            v.document = PaperSheetTestFixtures.flowDocument(40)
            v.scrollToPage(2)
            v
        }

        onFxThread {
            val stage = Stage()
            stage.scene = Scene(view, 320.0, 260.0)
            stage.show()
            view.applyCss()
            view.layout()
        }

        val skin = view.skin as PaperSheetViewSkin
        assertTrue(skin.verticalScrollBar.value > 0.0)
    }

    /** An out-of-range page index clamps to the last navigable page instead of throwing. */
    @Test
    fun outOfRangePageClampsInsteadOfThrowing() {
        val (view, skin) = fixture()

        onFxThread { view.scrollToPage(skin.pageCount - 1) }
        val expected = skin.verticalScrollBar.value
        onFxThread { view.scrollToPage(Int.MAX_VALUE) }

        assertEquals(expected, skin.verticalScrollBar.value)
    }

    /** An out-of-range block ordinal clamps to the last block instead of throwing. */
    @Test
    fun outOfRangeBlockClampsInsteadOfThrowing() {
        val (view, skin) = fixture()
        val lastBlock = view.caretModel.blockCount - 1

        onFxThread { view.scrollToBlock(lastBlock) }
        val expected = skin.verticalScrollBar.value
        onFxThread { view.scrollToBlock(Int.MAX_VALUE) }

        assertEquals(expected, skin.verticalScrollBar.value)
    }

    /**
     * The scroll commands move the viewport in [PaperSheetMode.STATIC], which has no caret and no
     * selection at all - the key behavioural difference from the [CaretModel] commands.
     */
    @Test
    fun scrollCommandsWorkInStaticModeWithoutACaret() {
        val (view, skin) = fixture(mode = PaperSheetMode.STATIC)

        onFxThread { view.scrollToPage(2) }

        assertTrue(skin.verticalScrollBar.value > 0.0)
    }

    /** The scroll commands are a no-op, not a crash, on a view without a document. */
    @Test
    fun scrollCommandsDoNothingWithoutADocument() {
        val view = onFxThread {
            val v = PaperSheetView()
            val stage = Stage()
            stage.scene = Scene(v, 320.0, 260.0)
            stage.show()
            v.applyCss()
            v.layout()
            v
        }

        onFxThread {
            view.scrollToPage(0)
            view.scrollToBlock(0)
            view.scrollToWord(0)
            view.scrollToSymbol(0)
        }

        val skin = view.skin as PaperSheetViewSkin
        assertEquals(0.0, skin.verticalScrollBar.value)
    }
}
