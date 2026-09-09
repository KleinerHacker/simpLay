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

package org.pcsoft.framework.simplay.swing.internal

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.StyledTextClipboard

/**
 * A [Transferable] that publishes a selected styled run list in the three flavours other
 * applications read when pasting: plain text ([DataFlavor.stringFlavor]), `text/html` (inline CSS on
 * `<span>`) and `text/rtf` (font table plus `\fN\fsNN\b\i` runs). The Swing counterpart of the
 * `fx` module's `ClipboardContent` fill in `TextSelection`.
 *
 * The HTML and RTF payloads are produced by the shared [StyledTextClipboard]; the plain text keeps
 * the characters verbatim so a plain-text paste stays identical.
 */
internal class StyledTextTransferable private constructor(
    private val plain: String,
    private val html: String,
    private val rtf: String,
) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(HTML_FLAVOR, RTF_FLAVOR, DataFlavor.stringFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        transferDataFlavors.any { it.equals(flavor) }

    override fun getTransferData(flavor: DataFlavor): Any = when {
        flavor.equals(DataFlavor.stringFlavor) -> plain
        flavor.equals(HTML_FLAVOR) -> html
        flavor.equals(RTF_FLAVOR) -> ByteArrayInputStream(rtf.toByteArray(Charsets.US_ASCII))
        else -> throw UnsupportedFlavorException(flavor)
    }

    companion object {

        val HTML_FLAVOR: DataFlavor = DataFlavor("text/html;class=java.lang.String")
        val RTF_FLAVOR: DataFlavor = DataFlavor("text/rtf;class=java.io.InputStream")

        /** Builds a transferable for the character range `[start, end)` of [index]. */
        fun of(index: DocumentTextIndex, start: Int, end: Int): StyledTextTransferable {
            val runs = index.styledRuns(start, end)
            return StyledTextTransferable(
                plain = index.substring(start, end),
                html = StyledTextClipboard.toHtml(runs),
                rtf = StyledTextClipboard.toRtf(runs),
            )
        }
    }
}
