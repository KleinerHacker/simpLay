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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Headless tests for [CaretModel]: its read-only state (position, bounds, structural counts) and the
 * linear, absolute-structural and relative-structural move commands, including a command issued
 * before the view has a skin.
 */
class CaretModelTest : JavaFxTestBase() {

    private val paragraphLength = PaperSheetTestFixtures.PARAGRAPH.length

    private fun view(paragraphs: Int = 1, mode: PaperSheetMode = PaperSheetMode.NAVIGABLE): PaperSheetView = onFxThread {
        val view = PaperSheetView()
        view.mode = mode
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        view
    }

    /**
     * A fresh view without caret support reports the caret at index `0` with no bounds, and the
     * structural counts of the document it was given.
     */
    @Test
    fun freshModelReportsCaretAtStartAndCounts() {
        val model = view(mode = PaperSheetMode.SELECTABLE).caretModel

        assertEquals(0, model.position)
        assertNull(model.bounds)
        assertEquals(18, model.wordCount)
        assertEquals(1, model.symbolCount)
        assertEquals(1, model.blockCount)
    }

    /**
     * `moveToEnd` places the caret at the last linear index, `moveToStart` back at `0`.
     */
    @Test
    fun moveToStartAndEndSpanTheDocument() {
        val model = view().caretModel

        onFxThread { model.moveToEnd() }
        assertEquals(paragraphLength, model.position)

        onFxThread { model.moveToStart() }
        assertEquals(0, model.position)
    }

    /**
     * `moveTo` clamps an out-of-range index into the document bounds instead of failing.
     */
    @Test
    fun moveToClampsIntoRange() {
        val model = view().caretModel

        onFxThread { model.moveTo(Int.MAX_VALUE) }
        assertEquals(paragraphLength, model.position)

        onFxThread { model.moveTo(-100) }
        assertEquals(0, model.position)
    }

    /**
     * The absolute block commands resolve a block ordinal to the block's linear-text bounds; the
     * word commands resolve a word ordinal likewise.
     */
    @Test
    fun structuralCommandsResolveOrdinals() {
        val model = view(paragraphs = 2).caretModel

        onFxThread { model.moveToEndOfBlock(0) }
        assertEquals(paragraphLength, model.position)

        onFxThread { model.moveToStartOfBlock(1) }
        assertEquals(paragraphLength + 1, model.position)

        onFxThread { model.moveToStartOfWord(1) }
        assertEquals(4, model.position)

        onFxThread { model.moveToEndOfWord(0) }
        assertEquals(3, model.position)
    }

    /**
     * The symbol commands address the paragraph's single trailing full stop.
     */
    @Test
    fun symbolCommandsAddressTheFullStop() {
        val model = view().caretModel

        onFxThread { model.moveToStartOfSymbol(0) }
        assertEquals(paragraphLength - 1, model.position)

        onFxThread { model.moveToEndOfSymbol(0) }
        assertEquals(paragraphLength, model.position)
    }

    /**
     * `moveToNextWord` steps forward to the following word start and `moveToPrevWord` steps back,
     * ending up where it started.
     */
    @Test
    fun nextAndPrevWordNavigationAreInverse() {
        val model = view().caretModel

        onFxThread { model.moveToStart() }
        onFxThread { model.moveToNextWord() }
        assertEquals(4, model.position)

        onFxThread { model.moveToNextWord() }
        assertTrue(model.position > 4)
        val afterTwo = model.position

        onFxThread { model.moveToPrevWord() }
        assertEquals(4, model.position)
        assertTrue(afterTwo > 4)
    }

    /**
     * With caret support the caret bounds become a real viewport rectangle; in a mode without a
     * caret they stay `null`.
     */
    @Test
    fun boundsAreSetOnlyWithCaretSupport() {
        assertNull(view(mode = PaperSheetMode.SELECTABLE).caretModel.bounds)

        val model = view(mode = PaperSheetMode.EDITABLE).caretModel
        onFxThread { model.moveTo(5) }
        val bounds = model.bounds
        assertTrue(bounds != null && bounds.height > 0.0)
    }

    /**
     * A caret command issued before the view has a skin is buffered and applied once the skin is
     * created.
     */
    @Test
    fun commandBeforeSkinIsAppliedOnSkinInit() {
        val view = onFxThread {
            val v = PaperSheetView().apply { mode = PaperSheetMode.NAVIGABLE }
            v.document = PaperSheetTestFixtures.flowDocument(1)
            v.caretModel.moveToEnd()
            v
        }

        assertEquals(0, view.caretModel.position, "no skin yet, so nothing is applied")

        onFxThread {
            val stage = Stage()
            stage.scene = Scene(view, 320.0, 260.0)
            stage.show()
            view.applyCss()
            view.layout()
        }

        assertEquals(paragraphLength, view.caretModel.position)
    }
}
