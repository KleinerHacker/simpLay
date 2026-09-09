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
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.engine.model.Document
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless integration tests for editing in [PaperSheetView] / [PaperSheetViewSkin]: the mode
 * property gating the caret and input, character typing, selection replacement, backspace / delete,
 * clipboard paste / cut, line and selection duplication, caret navigation keys, caret survival
 * across the re-measure an edit triggers, the observable document output and drag-and-drop of the
 * selection.
 */
class PaperSheetEditingTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(paragraphs: Int = 1, normal: Boolean = true): Fixture = onFxThread {
        val view = PaperSheetView()
        if (normal) view.mode = PaperSheetMode.EDITABLE
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    private fun Document.plain(): String =
        pages.flatMap { it.blocks }.joinToString("\n") { it.toString() }

    private fun documentLength(view: PaperSheetView): Int = onFxThread {
        view.selectionModel.selectAll()
        val length = view.selectionModel.text.length
        view.selectionModel.clearSelection()
        length
    }

    /**
     * In read-only mode no caret is drawn and typing or pressing an editing key leaves the document
     * untouched.
     */
    @Test
    fun readonlyModeShowsNoCaretAndRejectsInput() {
        val (view, skin) = fixture(normal = false)
        val before = view.document

        onFxThread {
            skin.typeTextForTest("Z")
            skin.pressKeyForTest(KeyCode.BACK_SPACE)
        }

        assertEquals(before, view.document)
        assertEquals(0, skin.caretDrawCount)
        assertFalse(skin.caretRenderedForTest)
    }

    /**
     * Switching the mode to normal makes the skin draw exactly one caret stroke.
     */
    @Test
    fun switchingToNormalRendersCaret() {
        val (view, skin) = fixture(normal = false)

        onFxThread { view.mode = PaperSheetMode.EDITABLE }

        assertTrue(skin.caretRenderedForTest)
        assertEquals(1, skin.caretDrawCount)
    }

    /**
     * Typing a character in normal mode inserts it into the document at the caret and grows the text
     * by one.
     */
    @Test
    fun typingInsertsCharacterAtCaret() {
        val (view, skin) = fixture()
        val before = documentLength(view)

        onFxThread {
            view.caretModel.moveTo(10)
            skin.typeTextForTest("Z")
        }

        assertTrue(view.document!!.plain().contains("Z"))
        assertEquals(before + 1, documentLength(view))
    }

    /**
     * Typing while a selection is active replaces the selected range and clears the selection.
     */
    @Test
    fun typingReplacesActiveSelection() {
        val (view, skin) = fixture()
        val before = documentLength(view)

        onFxThread {
            view.selectionModel.selectRange(4, 15)
            skin.typeTextForTest("Z")
        }

        assertTrue(view.selectionModel.isEmpty)
        assertTrue(view.document!!.plain().contains("Z"))
        assertTrue(documentLength(view) < before)
    }

    /**
     * `Backspace` removes the character before the caret; `Delete` the one after it.
     */
    @Test
    fun backspaceAndDeleteRemoveExpectedCharacter() {
        val (view, skin) = fixture()
        val before = documentLength(view)

        onFxThread {
            view.caretModel.moveTo(6)
            skin.pressKeyForTest(KeyCode.BACK_SPACE)
        }
        assertEquals(before - 1, documentLength(view))

        onFxThread {
            view.caretModel.moveTo(6)
            skin.pressKeyForTest(KeyCode.DELETE)
        }
        assertEquals(before - 2, documentLength(view))
    }

    /**
     * `Ctrl+V` inserts the clipboard's plain text at the caret.
     */
    @Test
    fun ctrlVPastesClipboardTextAtCaret() {
        val (view, skin) = fixture()

        onFxThread {
            Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString("PASTED") })
            view.caretModel.moveTo(0)
            skin.pressKeyForTest(KeyCode.V, shortcut = true)
        }

        assertTrue(view.document!!.plain().contains("PASTED"))
    }

    /**
     * `Ctrl+X` copies the selection to the clipboard and removes it from the document.
     */
    @Test
    fun ctrlXCutsSelectionToClipboardAndDocument() {
        val (view, skin) = fixture()
        val before = documentLength(view)

        val (cut, clipboard) = onFxThread {
            view.selectionModel.selectRange(0, 9)
            val text = view.selectedText
            skin.pressKeyForTest(KeyCode.X, shortcut = true)
            text to Clipboard.getSystemClipboard().string
        }

        assertEquals(cut, clipboard)
        assertTrue(view.selectionModel.isEmpty)
        assertTrue(documentLength(view) < before)
    }

    /**
     * `Ctrl+D` with a selection inserts a copy of it right after the selection, growing the document.
     */
    @Test
    fun ctrlDDuplicatesSelection() {
        val (view, skin) = fixture()
        val before = documentLength(view)

        onFxThread {
            view.selectionModel.selectRange(0, 9)
            skin.pressKeyForTest(KeyCode.D, shortcut = true)
        }

        assertTrue(documentLength(view) > before)
    }

    /**
     * `Home` / `End` move within the current line; `Ctrl+Home` / `Ctrl+End` to the document bounds.
     */
    @Test
    fun homeEndAndDocumentBoundNavigation() {
        val (view, skin) = fixture(paragraphs = 2)
        val length = documentLength(view)

        onFxThread {
            view.caretModel.moveTo(1)
            skin.pressKeyForTest(KeyCode.HOME)
        }
        assertEquals(0, skin.caretIndexForTest)

        onFxThread { skin.pressKeyForTest(KeyCode.END) }
        assertTrue(skin.caretIndexForTest > 0)

        onFxThread { skin.pressKeyForTest(KeyCode.END, shortcut = true) }
        assertEquals(length, skin.caretIndexForTest)

        onFxThread { skin.pressKeyForTest(KeyCode.HOME, shortcut = true) }
        assertEquals(0, skin.caretIndexForTest)
    }

    /**
     * The left / right arrows move the caret by one character; `Ctrl+Left` / `Ctrl+Right` by one
     * word.
     */
    @Test
    fun arrowAndWordNavigation() {
        val (view, skin) = fixture()

        onFxThread {
            view.caretModel.moveTo(5)
            skin.pressKeyForTest(KeyCode.RIGHT)
        }
        assertEquals(6, skin.caretIndexForTest)

        onFxThread { skin.pressKeyForTest(KeyCode.LEFT) }
        assertEquals(5, skin.caretIndexForTest)

        onFxThread {
            view.caretModel.moveTo(0)
            skin.pressKeyForTest(KeyCode.RIGHT, shortcut = true)
        }
        assertEquals(4, skin.caretIndexForTest)

        onFxThread { skin.pressKeyForTest(KeyCode.LEFT, shortcut = true) }
        assertEquals(0, skin.caretIndexForTest)
    }

    /**
     * `Shift` with a navigation key extends the selection instead of collapsing it.
     */
    @Test
    fun shiftWithNavigationExtendsSelection() {
        val (view, skin) = fixture()

        onFxThread {
            view.caretModel.moveTo(5)
            repeat(3) { skin.pressKeyForTest(KeyCode.RIGHT, shift = true) }
        }

        assertEquals(5, view.selectionModel.startIndex)
        assertEquals(3, view.selectionModel.length)
    }

    /**
     * After an edit re-measures the view the caret sits one past the inserted character, not back at
     * the document start.
     */
    @Test
    fun caretSurvivesReMeasureAfterEdit() {
        val (view, skin) = fixture()

        onFxThread {
            view.caretModel.moveTo(10)
            skin.typeTextForTest("Z")
        }

        assertEquals(11, skin.caretIndexForTest)
        assertTrue(view.document!!.plain().contains("Z"))
    }

    /**
     * Every edit publishes a new value on the observable `document` property.
     */
    @Test
    fun editingUpdatesDocumentProperty() {
        val (view, skin) = fixture()
        val seen = ArrayList<Document?>()
        onFxThread { view.documentProperty.addListener { _, _, value -> seen.add(value) } }

        onFxThread {
            view.caretModel.moveTo(0)
            skin.typeTextForTest("Z")
        }

        assertEquals(1, seen.size)
        assertNotNull(seen.first())
    }

    /**
     * Dragging the selection to a point outside it moves the text there: the marker word leaves the
     * front of the document but is still present.
     */
    @Test
    fun dragDropMovesSelection() {
        val (view, skin) = fixture()

        onFxThread {
            view.caretModel.moveTo(0)
            skin.typeTextForTest("ZZZ ")
            view.selectionModel.selectRange(0, 3)
            skin.dragSelectionToForTest(250.0, 200.0, copy = false)
        }

        val text = view.document!!.plain()
        assertFalse(text.startsWith("ZZZ"))
        assertTrue(text.contains("ZZZ"))
    }

    /**
     * Dragging the selection with `Ctrl` held copies it: the marker word stays at the front and a
     * second copy appears at the drop point.
     */
    @Test
    fun dragDropWithCtrlCopiesSelection() {
        val (view, skin) = fixture()

        onFxThread {
            view.caretModel.moveTo(0)
            skin.typeTextForTest("ZZZ ")
            view.selectionModel.selectRange(0, 3)
            skin.dragSelectionToForTest(250.0, 200.0, copy = true)
        }

        val text = view.document!!.plain()
        assertTrue(text.startsWith("ZZZ"))
        assertTrue(text.indexOf("ZZZ") != text.lastIndexOf("ZZZ"))
    }

    /**
     * Typing at the very end of a block - the caret sitting right before the block separator - keeps
     * the new character in that block instead of dropping it into the inter-block gap.
     */
    @Test
    fun typingAtBlockEndKeepsCharacterInThatBlock() {
        val (view, skin) = fixture(paragraphs = 2)

        onFxThread {
            view.caretModel.moveToEndOfBlock(0)
            skin.typeTextForTest("Z")
        }

        val blocks = view.document!!.pages.flatMap { it.blocks }
        assertEquals(2, blocks.size)
        assertTrue(blocks[0].toString().endsWith("Z"), "first block should end with the typed char: '${blocks[0]}'")
        assertFalse(blocks[1].toString().contains("Z"))
    }

    /**
     * A `CARET`-triggered floating overlay becomes active while the view edits and anchors to the
     * caret rectangle, carrying the caret index.
     */
    @Test
    fun caretOverlayActivatesInNormalMode() {
        val overlay = FloatingOverlay().apply {
            trigger = FloatingOverlayTrigger.CARET
            content = javafx.scene.control.Label("caret")
        }
        val (view, _) = fixture()

        onFxThread {
            view.floatingOverlays.add(overlay)
            view.caretModel.moveTo(5)
        }

        assertTrue(overlay.isActive)
        assertEquals(5, overlay.activeIndex)
        assertNotNull(overlay.activeBounds)
    }

    /**
     * The smooth-caret-blink switch is off by default and can be toggled at runtime.
     */
    @Test
    fun smoothCaretBlinkDefaultsOffAndIsSettable() {
        val (view, _) = fixture(normal = false)

        assertFalse(view.smoothCaretBlink)

        onFxThread { view.smoothCaretBlink = true }
        assertTrue(view.smoothCaretBlink)
    }

    /**
     * With the smooth blink enabled the caret is still rendered and starts fully opaque right after a
     * caret move.
     */
    @Test
    fun smoothCaretBlinkKeepsCaretRenderedAtFullOpacityAfterMove() {
        val (view, skin) = fixture()

        onFxThread {
            view.smoothCaretBlink = true
            view.caretModel.moveTo(4)
        }

        assertTrue(skin.caretRenderedForTest)
        assertEquals(1.0, skin.caretOpacityForTest(), 1e-9)
    }
}
