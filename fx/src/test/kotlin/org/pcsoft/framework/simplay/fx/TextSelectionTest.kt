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

import javafx.scene.input.Clipboard
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.*
import org.pcsoft.framework.simplay.fx.internal.DocumentTextIndex
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.fx.internal.ps.TextSelection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless tests for the [TextSelection] model together with the [DocumentTextIndex] it works on,
 * including the plain-text and the styled (HTML + RTF) clipboard copy.
 */
class TextSelectionTest : JavaFxTestBase() {

    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    private fun index(vararg paragraphs: String): DocumentTextIndex {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = PageLayout(
                        size = Size(width = 480.0, height = 360.0),
                        margins = Margins(left = 24.0, top = 24.0, right = 24.0, bottom = 24.0),
                    ),
                    blocks = paragraphs.map { TextBlock.of(it, style) },
                ),
            ),
        )
        val measured = onFxThread { document.measure(FxFontMeasureCalculator()) }
        return DocumentTextIndex(measured)
    }

    /**
     * For a single block that fits on one line the linear index text equals the block's normalizing
     * `toString()`.
     */
    @Test
    fun indexTextOfSingleShortBlockEqualsBlockToString() {
        val block = TextBlock.of("Hello brave new world", style)
        val idx = index("Hello brave new world")

        assertEquals(block.toString(), idx.text)
    }

    /**
     * An empty selection (anchor equals focus) reports no text and `isEmpty`.
     */
    @Test
    fun emptySelectionHasNoText() {
        val selection = TextSelection().apply { index = index("Alpha beta gamma") }

        selection.anchor = 4
        selection.focus = 4

        assertTrue(selection.isEmpty)
        assertEquals("", selection.selectedText)
    }

    /**
     * `selectAll` covers the whole linear text of the document.
     */
    @Test
    fun selectAllCoversTheWholeDocument() {
        val idx = index("First paragraph here", "Second paragraph here")
        val selection = TextSelection().apply { index = idx }

        selection.selectAll()

        assertFalse(selection.isEmpty)
        assertEquals(idx.text, selection.selectedText)
    }

    /**
     * A selection that starts in the first block and ends in the third yields the joined plain text
     * of the spanned range, including the line break the index inserts between blocks.
     */
    @Test
    fun selectionSpanningBlocksProducesJoinedTextWithSeparators() {
        val idx = index("Block one text", "Block two text", "Block three text")
        val selection = TextSelection().apply { index = idx }

        val from = idx.text.indexOf("one")
        val to = idx.text.indexOf("three") + "three".length
        selection.anchor = to
        selection.focus = from

        assertEquals(idx.text.substring(from, to), selection.selectedText)
        assertTrue(selection.selectedText.contains("\n"))
    }

    /**
     * Double-click word selection expands an interior index to the surrounding letters-and-digits
     * word.
     */
    @Test
    fun selectWordAtExpandsToTheWholeWord() {
        val idx = index("The quick brown fox")
        val selection = TextSelection().apply { index = idx }

        val insideQuick = idx.text.indexOf("quick") + 2
        selection.selectWordAt(insideQuick)

        assertEquals("quick", selection.selectedText)
    }

    /**
     * Copying the selected text puts exactly that plain string on the system clipboard.
     */
    @Test
    fun copyPutsSelectedPlainTextOnClipboard() {
        val idx = index("Copy this sentence please")
        val selection = TextSelection().apply { index = idx }
        val from = idx.text.indexOf("this")
        val to = idx.text.indexOf("please") + "please".length
        selection.anchor = from
        selection.focus = to

        val expected = selection.selectedText
        val onClipboard = onFxThread {
            TextSelection.putPlainTextOnClipboard(expected)
            Clipboard.getSystemClipboard().string
        }

        assertEquals(expected, onClipboard)
    }

    /**
     * The styled copy puts plain text, HTML and RTF on the clipboard at once; the HTML fragment
     * carries the font family and point size as inline CSS and the RTF a font table plus a
     * half-point `\fs` size, so a rich paste target keeps the text style. The JavaFX logical family
     * `Serif` is translated to a concrete, installable font name (`Times New Roman`) plus a generic
     * so the target does not silently substitute it.
     */
    @Test
    fun styledCopyPutsHtmlAndRtfWithFontInfoOnClipboard() {
        val idx = index("Keep this styled text please")
        val selection = TextSelection().apply { index = idx }
        selection.anchor = idx.text.indexOf("this")
        selection.focus = idx.text.indexOf("please") + "please".length
        val expectedPlain = selection.selectedText

        val clipboard = onFxThread {
            selection.putStyledSelectionOnClipboard()
            Clipboard.getSystemClipboard().let {
                Triple(it.string, if (it.hasHtml()) it.html else null, if (it.hasRtf()) it.rtf else null)
            }
        }
        val nativeRtf = onFxThread {
            val format = javafx.scene.input.DataFormat.lookupMimeType("Rich Text Format")
            format != null && Clipboard.getSystemClipboard().hasContent(format)
        }

        assertEquals(expectedPlain, clipboard.first)
        assertTrue(nativeRtf, "RTF should also be published under the native 'Rich Text Format' name")
        val html = assertNotNull(clipboard.second)
        assertTrue(html.contains("font-family:'Times New Roman', serif"), html)
        assertTrue(html.contains("font-size:14pt"), html)
        val rtf = assertNotNull(clipboard.third)
        assertTrue(rtf.contains("\\fonttbl"), rtf)
        assertTrue(rtf.contains("\\froman\\fcharset0 Times New Roman;"), rtf)
        assertTrue(rtf.contains("\\fs28"), rtf)
    }

    /**
     * A real, installed font family is passed through to HTML and RTF unchanged rather than being
     * mapped to one of the logical-family substitutes.
     */
    @Test
    fun styledCopyKeepsRealFontFamilyAsIs() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = PageLayout(
                        size = Size(width = 480.0, height = 360.0),
                        margins = Margins(left = 24.0, top = 24.0, right = 24.0, bottom = 24.0),
                    ),
                    blocks = listOf(TextBlock.of("Verdana styled words", TextStyle(font = Font(family = "Verdana", size = 12.0)))),
                ),
            ),
        )
        val idx = DocumentTextIndex(onFxThread { document.measure(FxFontMeasureCalculator()) })
        val selection = TextSelection().apply { index = idx }
        selection.selectAll()

        val clipboard = onFxThread {
            selection.putStyledSelectionOnClipboard()
            Clipboard.getSystemClipboard().let { (if (it.hasHtml()) it.html else "") to (if (it.hasRtf()) it.rtf else "") }
        }

        assertTrue(clipboard.first.contains("font-family:'Verdana';"), clipboard.first)
        assertTrue(clipboard.second.contains("\\fnil\\fcharset0 Verdana;"), clipboard.second)
    }

    /**
     * `styledRuns` splits a block-spanning selection into font-carrying part runs and `null`-font
     * separator runs whose concatenated text is exactly the selected substring.
     */
    @Test
    fun styledRunsSplitSelectionIntoFontRunsAndSeparators() {
        val idx = index("First block here", "Second block here")
        val from = idx.text.indexOf("block")
        val to = idx.text.indexOf("Second") + "Second".length
        val runs = idx.styledRuns(from, to)

        assertEquals(idx.text.substring(from, to), runs.joinToString("") { it.text })
        assertTrue(runs.any { it.font != null })
        assertTrue(runs.any { it.font == null && it.text.contains("\n") })
    }
}
