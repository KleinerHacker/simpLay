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
import java.awt.datatransfer.StringSelection
import org.pcsoft.framework.simplay.swing.internal.StyledTextTransferable
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex

/**
 * The text selection of a `PaperSheetView`: an [anchor] (where the drag started) and a [focus] (the
 * moving end), both as character indices into the linear text of [index]. The selected range is
 * always `start until end`; an empty selection has `start == end`. The Swing counterpart of the `fx`
 * module's `TextSelection`.
 *
 * The model is a plain holder driven by the UI delegate's mouse handling; mapping a pointer to an
 * index and painting the highlight happen in the delegate, which owns the measured geometry.
 */
internal class TextSelection {

    /** The linear text index the selection refers to, or `null` when there is no document. */
    var index: DocumentTextIndex? = null

    /** Character index where the current selection gesture started. */
    var anchor: Int = 0

    /** Character index of the moving end of the selection. */
    var focus: Int = 0

    /** Lower bound of the selected range. */
    val start: Int get() = minOf(anchor, focus)

    /** Upper bound of the selected range. */
    val end: Int get() = maxOf(anchor, focus)

    /** `true` when nothing is selected. */
    val isEmpty: Boolean get() = start == end

    /** The selected text as plain text, including the separators inside the range; `""` when empty. */
    val selectedText: String
        get() = index?.substring(start, end).orEmpty()

    /** Collapses the selection back to the document start. */
    fun reset() {
        anchor = 0
        focus = 0
    }

    /** Selects everything in [index]. */
    fun selectAll() {
        anchor = 0
        focus = index?.length ?: 0
    }

    /** Selects `[start, end)` of [index], order-independent, with both ends clamped; no-op without an [index]. */
    fun selectRange(start: Int, end: Int) {
        val idx = index ?: return
        anchor = idx.clamp(minOf(start, end))
        focus = idx.clamp(maxOf(start, end))
    }

    /** Selects the letters-and-digits word around [charIndex]; no-op without an [index]. */
    fun selectWordAt(charIndex: Int) {
        val range = index?.wordRangeAt(charIndex) ?: return
        anchor = range.first
        focus = range.last
    }

    /**
     * Puts the current selection onto the system clipboard as plain text, `text/html` and RTF at
     * once, the rich flavours carrying the per-part font. A no-op returning `false` when nothing is
     * selected.
     */
    fun putStyledSelectionOnClipboard(): Boolean {
        val idx = index ?: return false
        if (isEmpty) return false
        val transferable = StyledTextTransferable.of(idx, start, end)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
        return true
    }

    companion object {

        /** Puts [text] as plain text onto the system clipboard. */
        fun putPlainTextOnClipboard(text: String): Boolean {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            return true
        }
    }
}
