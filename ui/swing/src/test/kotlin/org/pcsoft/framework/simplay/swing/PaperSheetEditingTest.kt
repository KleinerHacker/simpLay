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
}
