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

import org.pcsoft.framework.simplay.fx.PaperSheetMode
import org.pcsoft.framework.simplay.fx.PaperSheetView

import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.uicommon.DocumentEditor
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.EditableRegions
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode

/**
 * The editable-mode input controller of a [PaperSheetView]: the keyboard shortcuts (character
 * typing, `Backspace` / `Delete`, `Ctrl+C` / `Ctrl+V` / `Ctrl+X` / `Ctrl+D`, the caret-navigation
 * keys) and the text mutations they trigger, plus drag-and-drop of the selection. Every mutation
 * runs through [DocumentEditor] and replaces [PaperSheetView.document] with the rebuilt document;
 * [PaperSheetCaret.onEditApplied] then restores the caret.
 *
 * A per-view helper the skin creates once and routes key events and selection drops to. The text
 * index is read through [textIndex]; navigation keys are delegated to [caret], clipboard text to
 * [selection]. The caret-navigation keys need [PaperSheetMode.supportsCaret], every mutating key
 * needs [PaperSheetMode.supportsEditing] and `Ctrl+C` needs [PaperSheetMode.supportsSelection];
 * every other key is ignored.
 *
 * Every mutation is checked against [EditableRegions.isEditRangeAllowed] first: in
 * [PageDeactivationMode.DISABLED] and [PageDeactivationMode.READONLY] a mutation whose range touches a
 * deactivated page is silently dropped (no [DocumentEditor] call, no caret change); `Ctrl+C` is never
 * blocked. [PageDeactivationMode.IGNORE] and [PageDeactivationMode.HIDDEN] never block anything (a
 * `HIDDEN` page is unreachable through the UI anyway).
 *
 * @property view the owning view, for its mode, `document` and re-layout.
 * @property selection the shared selection, for its range and clipboard copy.
 * @property caret the shared caret, for its position, navigation moves and post-edit restore.
 * @property textIndex the current linear text index, or `null` without a document.
 * @property requestRedraw repaints the skin's canvas (used by the no-op drop path).
 * @property markInternalEdit tells the skin that the next `document` change comes from this editor,
 *   so that the re-measure keeps the caret instead of treating the change as a document reload.
 */
internal class PaperSheetEditor(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val caret: PaperSheetCaret,
    private val textIndex: () -> DocumentTextIndex?,
    private val requestRedraw: () -> Unit,
    private val markInternalEdit: () -> Unit,
) {

    private val editable: Boolean get() = view.mode.supportsEditing

    private val caretActive: Boolean get() = view.mode.supportsCaret

    private val selectable: Boolean get() = view.mode.supportsSelection

    //region Key handling

    fun onKeyPressed(event: KeyEvent) {
        if (event.code == KeyCode.C && event.isShortcutDown && !event.isAltDown) {
            if (selectable && selection.putStyledSelectionOnClipboard()) event.consume()
            return
        }
        if (!caretActive) return
        val shift = event.isShiftDown
        val shortcut = event.isShortcutDown
        when (event.code) {
            KeyCode.LEFT -> if (shortcut) caret.moveWordLeft(shift) else caret.moveHorizontal(-1, shift)
            KeyCode.RIGHT -> if (shortcut) caret.moveWordRight(shift) else caret.moveHorizontal(1, shift)
            KeyCode.UP -> caret.moveVertical(-1, shift)
            KeyCode.DOWN -> caret.moveVertical(1, shift)
            KeyCode.HOME -> if (shortcut) caret.moveDocStart(shift) else caret.moveLineStart(shift)
            KeyCode.END -> if (shortcut) caret.moveDocEnd(shift) else caret.moveLineEnd(shift)
            KeyCode.BACK_SPACE -> if (editable) backspace() else return
            KeyCode.DELETE -> if (editable) deleteForward() else return
            KeyCode.V -> if (shortcut && editable) paste() else return
            KeyCode.X -> if (shortcut && editable) cut() else return
            KeyCode.D -> if (shortcut && editable) duplicate() else return
            else -> return
        }
        event.consume()
    }

    fun onKeyTyped(event: KeyEvent) {
        if (!editable) return
        if (event.isShortcutDown || event.isControlDown || event.isAltDown || event.isMetaDown) return
        val ch = event.character
        if (ch.isEmpty()) return
        val c = ch[0]
        if (c.code < 0x20 || c.code == 0x7F) return
        typeText(ch)
        event.consume()
    }

    //endregion

    //region Page-deactivation lock

    /** Whether the mutation range `[lo, hi)` may be applied, per the view's deactivation state. */
    private fun regionsAllow(idx: DocumentTextIndex, doc: Document, lo: Int, hi: Int): Boolean {
        val mode = view.deactivatedPageHandling
        if (mode == PageDeactivationMode.IGNORE || mode == PageDeactivationMode.HIDDEN) return true
        val ids = view.deactivatedPageIds
        if (ids.isEmpty()) return true
        return EditableRegions.of(idx, ids, mode, doc).isEditRangeAllowed(lo, hi)
    }

    //endregion

    //region Mutations

    private fun applyEdit(lo: Int, hi: Int, op: (DocumentTextIndex, Document) -> DocumentEditor.Result) {
        if (!editable) return
        val idx = textIndex() ?: return
        val doc = view.document ?: return
        if (!regionsAllow(idx, doc, lo, hi)) return
        val result = op(idx, doc)
        markInternalEdit()
        view.document = result.document
        caret.onEditApplied(result.caretIndex)
        view.requestLayout()
    }

    /** Inserts [text] at the caret, replacing the selection if there is one. */
    fun typeText(text: String) {
        if (!editable || text.isEmpty()) return
        if (!selection.isEmpty) {
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.replace(i, d, selection.start, selection.end, text) }
        } else {
            applyEdit(caret.position, caret.position) { i, d -> DocumentEditor.insert(i, d, caret.position, text) }
        }
    }

    private fun backspace() {
        if (!selection.isEmpty) {
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.delete(i, d, selection.start, selection.end) }
            return
        }
        if (caret.position <= 0) return
        applyEdit(caret.position - 1, caret.position) { i, d -> DocumentEditor.delete(i, d, caret.position - 1, caret.position) }
    }

    private fun deleteForward() {
        if (!selection.isEmpty) {
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.delete(i, d, selection.start, selection.end) }
            return
        }
        val len = textIndex()?.length ?: 0
        if (caret.position >= len) return
        applyEdit(caret.position, caret.position + 1) { i, d -> DocumentEditor.delete(i, d, caret.position, caret.position + 1) }
    }

    private fun paste() {
        val s = Clipboard.getSystemClipboard().string ?: return
        if (s.isEmpty()) return
        if (!selection.isEmpty) {
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.replace(i, d, selection.start, selection.end, s) }
        } else {
            applyEdit(caret.position, caret.position) { i, d -> DocumentEditor.insert(i, d, caret.position, s) }
        }
    }

    private fun cut() {
        if (selection.isEmpty) return
        if (!selection.putStyledSelectionOnClipboard()) return
        applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.delete(i, d, selection.start, selection.end) }
    }

    private fun duplicate() {
        val idx = textIndex() ?: return
        if (!selection.isEmpty) {
            val text = idx.substring(selection.start, selection.end)
            val at = selection.end
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.insert(i, d, at, text) }
            return
        }
        val (ls, le) = caret.currentLineBounds() ?: return
        val text = idx.substring(ls, le)
        if (text.isEmpty()) return
        applyEdit(ls, le) { i, d -> DocumentEditor.insert(i, d, le, " $text") }
    }

    /** Moves (or, with [copy], copies) the current selection to the character index [target]. */
    fun dropSelection(target: Int, copy: Boolean) {
        val idx = textIndex() ?: return
        val doc = view.document ?: return
        if (selection.isEmpty) return
        val lo = selection.start
        val hi = selection.end
        if (target in lo..hi) {
            requestRedraw()
            return
        }
        if (!regionsAllow(idx, doc, minOf(lo, target), maxOf(hi, target))) {
            requestRedraw()
            return
        }
        val text = idx.substring(lo, hi)
        if (copy) {
            applyEdit(target, target) { i, d -> DocumentEditor.insert(i, d, target, text) }
        } else {
            val insertAt = if (target > hi) target - (hi - lo) else target
            applyEdit(lo, hi) { i, d -> DocumentEditor.delete(i, d, lo, hi) }
            val fresh = textIndex() ?: return
            applyEdit(insertAt, insertAt) { _, d -> DocumentEditor.insert(fresh, d, fresh.clamp(insertAt), text) }
        }
    }

    //endregion
}
