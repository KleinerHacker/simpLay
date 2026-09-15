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

package org.pcsoft.framework.simplay.uicommon

import kotlin.test.Test
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight

/**
 * Tests for [StyledTextClipboard], which serialises a styled run list into the `text/html` and RTF
 * flavours other applications read on paste.
 */
class StyledTextClipboardTest {

    private fun run(text: String, family: String, size: Double, weight: FontWeight = FontWeight.NORMAL) =
        DocumentTextIndex.StyledRun(text, Font(family = family, size = size, weight = weight))

    /**
     * Verifies that the HTML output wraps each run in a `<span>` with inline CSS, maps the JavaFX
     * logical family `Serif` to a concrete stack and emits the font size in points.
     */
    @Test
    fun htmlCarriesInlineFontCss() {
        val html = StyledTextClipboard.toHtml(listOf(run("Title", "Serif", 18.0, FontWeight.BOLD)))
        assertTrue(html.contains("<span"))
        assertTrue(html.contains("font-size:18pt"))
        assertTrue(html.contains("font-weight:bold"))
        assertTrue(html.contains("Times New Roman"))
        assertTrue(html.contains("Title"))
    }

    /**
     * Verifies that the HTML output escapes the markup-significant characters `&`, `<` and `>`.
     */
    @Test
    fun htmlEscapesMarkup() {
        val html = StyledTextClipboard.toHtml(listOf(run("a < b & c > d", "Serif", 12.0)))
        assertTrue(html.contains("a &lt; b &amp; c &gt; d"))
    }

    /**
     * Verifies that the RTF output is a well-formed document with a font table and a run group that
     * carries the half-point size and the italic flag.
     */
    @Test
    fun rtfHasFontTableAndRunGroups() {
        val rtf = StyledTextClipboard.toRtf(listOf(run("hi", "Serif", 12.0, FontWeight.NORMAL).let {
            DocumentTextIndex.StyledRun("hi", Font(family = "Serif", size = 12.0, style = FontStyle.ITALIC))
        }))
        assertTrue(rtf.startsWith("{\\rtf1"))
        assertTrue(rtf.contains("\\fonttbl"))
        assertTrue(rtf.contains("\\fs24"))
        assertTrue(rtf.contains("\\i"))
        assertTrue(rtf.endsWith("}"))
    }
}
