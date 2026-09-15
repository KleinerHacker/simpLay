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

import org.pcsoft.framework.simplay.fx.PaperSheetView

import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.StyledTextClipboard

/**
 * The text selection of a [PaperSheetView]: an [anchor] (where the drag started) and a [focus] (the
 * moving end), both as character indices into the linear text of [index]. The selected range is
 * always `start until end`; an empty selection has `start == end`.
 *
 * The model is a plain holder driven by the skin's mouse handling; mapping a pointer to an index and
 * painting the highlight happen in the skin, which owns the measured geometry.
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
     * Puts the current selection onto the system clipboard in every flavour at once: plain text,
     * `text/html` and RTF, the rich flavours carrying the per-part font (family, size, weight,
     * slant) so a paste target keeps the text style. The RTF is additionally published under the
     * native Windows clipboard name `Rich Text Format`, the name Word and WordPad actually read,
     * because JavaFX only registers RTF under the `text/rtf` MIME id. Must be called on the JavaFX
     * application thread; a no-op returning `false` when nothing is selected.
     *
     * @return `true` if the clipboard accepted the content.
     */
    fun putStyledSelectionOnClipboard(): Boolean {
        val idx = index ?: return false
        if (isEmpty) return false
        val runs = idx.styledRuns(start, end)
        val rtf = StyledTextClipboard.toRtf(runs)
        return Clipboard.getSystemClipboard().setContent(
            ClipboardContent().apply {
                putString(idx.substring(start, end))
                putHtml(StyledTextClipboard.toHtml(runs))
                putRtf(rtf)
                put(RTF_WINDOWS, rtf)
            },
        )
    }

    companion object {

        /** The native Windows clipboard format name Word and WordPad read RTF from. */
        private val RTF_WINDOWS: DataFormat =
            DataFormat.lookupMimeType(RTF_WINDOWS_ID) ?: DataFormat(RTF_WINDOWS_ID)

        private const val RTF_WINDOWS_ID = "Rich Text Format"

        /**
         * Puts [text] as plain text onto the system [Clipboard]. Must be called on the JavaFX
         * application thread.
         *
         * @return `true` if the clipboard accepted the content.
         */
        fun putPlainTextOnClipboard(text: String): Boolean =
            Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(text) })
    }
}
