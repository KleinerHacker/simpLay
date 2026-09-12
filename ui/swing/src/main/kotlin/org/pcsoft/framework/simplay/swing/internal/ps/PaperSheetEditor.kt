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

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.swing.asPageMode
import org.pcsoft.framework.simplay.uicommon.DocumentEditor
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.EditableRegions

/**
 * The editable-mode input controller of a [PaperSheetView]: the keyboard shortcuts (character
 * typing, `Backspace` / `Delete`, `Ctrl+C` / `Ctrl+V` / `Ctrl+X` / `Ctrl+D`, the caret-navigation
 * keys) and the text mutations they trigger, plus drag-and-drop of the selection. Every mutation
 * runs through [DocumentEditor] and replaces [PaperSheetView.document] with the rebuilt document;
 * [PaperSheetCaret.onEditApplied] then restores the caret. The Swing counterpart of the `fx` module's
 * `PaperSheetEditor`.
 *
 * A per-view helper the delegate creates once and routes key events and selection drops to. The
 * caret-navigation keys need [PaperSheetMode.supportsCaret], every mutating key needs
 * [PaperSheetMode.supportsEditing] and `Ctrl+C` / `Cmd+C` needs [PaperSheetMode.supportsSelection];
 * every other key is ignored.
 *
 * Every mutation is checked against [EditableRegions.isEditRangeAllowed] first: a mutation whose range
 * touches a page whose effective mode does not support editing is silently dropped; `Ctrl+C` is never
 * blocked by it.
 */
internal class PaperSheetEditor(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val caret: PaperSheetCaret,
    private val textIndex: () -> DocumentTextIndex?,
    private val requestRedraw: () -> Unit,
    private val markInternalEdit: () -> Unit,
) {

    private val editable: Boolean get() = view.anyEditing

    private val caretActive: Boolean get() = view.anyCaret

    private val selectable: Boolean get() = view.anySelection

    private val shortcutMask: Int = runCatching { Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx }
        .getOrDefault(java.awt.event.InputEvent.CTRL_DOWN_MASK)

    private fun KeyEvent.isShortcutDown(): Boolean = (modifiersEx and shortcutMask) == shortcutMask

    //region Key handling

    fun onKeyPressed(event: KeyEvent) {
        if (event.keyCode == KeyEvent.VK_C && event.isShortcutDown() && !event.isAltDown) {
            if (selectable && selection.putStyledSelectionOnClipboard()) event.consume()
            return
        }
        if (!caretActive) return
        val shift = event.isShiftDown
        val shortcut = event.isShortcutDown()
        when (event.keyCode) {
            KeyEvent.VK_LEFT -> if (shortcut) caret.moveWordLeft(shift) else caret.moveHorizontal(-1, shift)
            KeyEvent.VK_RIGHT -> if (shortcut) caret.moveWordRight(shift) else caret.moveHorizontal(1, shift)
            KeyEvent.VK_UP -> caret.moveVertical(-1, shift)
            KeyEvent.VK_DOWN -> caret.moveVertical(1, shift)
            KeyEvent.VK_PAGE_UP -> caret.movePage(-1, shift)
            KeyEvent.VK_PAGE_DOWN -> caret.movePage(1, shift)
            KeyEvent.VK_HOME -> if (shortcut) caret.moveDocStart(shift) else caret.moveLineStart(shift)
            KeyEvent.VK_END -> if (shortcut) caret.moveDocEnd(shift) else caret.moveLineEnd(shift)
            KeyEvent.VK_BACK_SPACE -> if (editable) backspace() else return
            KeyEvent.VK_DELETE -> if (editable) deleteForward() else return
            KeyEvent.VK_V -> if (shortcut && editable) paste() else return
            KeyEvent.VK_X -> if (shortcut && editable) cut() else return
            KeyEvent.VK_D -> if (shortcut && editable) duplicate() else return
            else -> return
        }
        event.consume()
    }

    fun onKeyTyped(event: KeyEvent) {
        if (!editable) return
        if (event.isShortcutDown() || event.isControlDown || event.isAltDown || event.isMetaDown) return
        val c = event.keyChar
        if (c == KeyEvent.CHAR_UNDEFINED) return
        if (c.code < 0x20 || c.code == 0x7F) return
        typeText(c.toString())
        event.consume()
    }

    //endregion

    //region Page-mode lock

    /** Whether the mutation range `[lo, hi)` may be applied, per every page's effective mode. */
    private fun regionsAllow(idx: DocumentTextIndex, doc: Document, lo: Int, hi: Int): Boolean {
        if (view.pageModes.isEmpty()) return true
        return EditableRegions.of(idx, view.pageModes, view.mode.asPageMode(), doc).isEditRangeAllowed(lo, hi)
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
        val s = readClipboardString() ?: return
        if (s.isEmpty()) return
        if (!selection.isEmpty) {
            applyEdit(selection.start, selection.end) { i, d -> DocumentEditor.replace(i, d, selection.start, selection.end, s) }
        } else {
            applyEdit(caret.position, caret.position) { i, d -> DocumentEditor.insert(i, d, caret.position, s) }
        }
    }

    private fun readClipboardString(): String? = try {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) cb.getData(DataFlavor.stringFlavor) as? String else null
    } catch (_: Exception) {
        null
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
