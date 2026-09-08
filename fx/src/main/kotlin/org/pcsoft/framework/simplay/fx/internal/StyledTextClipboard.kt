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

package org.pcsoft.framework.simplay.fx.internal

import kotlin.math.roundToInt
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight

/**
 * Serialises a run list from [DocumentTextIndex.styledRuns] into the two rich-text flavours other
 * applications read when pasting: `text/html` (inline CSS on `<span>`) and RTF (font table plus
 * `\fN\fsNN\b\i` runs). Font sizes are emitted in points, matching how the document is measured on
 * screen. Both outputs keep the plain characters so a plain-text paste stays identical.
 *
 * JavaFX logical family names (`Serif`, `SansSerif`, `Monospaced`, `System`) are not installed
 * fonts, so a target application would drop them and substitute its default. They are therefore
 * translated to a concrete font name plus a CSS generic ([resolveFamily]); any other family is a
 * real installed face and is passed through unchanged.
 */
internal object StyledTextClipboard {

    /** A JavaFX family name mapped to what HTML and RTF need to actually resolve it on paste. */
    private data class ResolvedFamily(val cssStack: String, val rtfName: String, val rtfCategory: String)

    private fun resolveFamily(family: String): ResolvedFamily = when (family.trim().lowercase()) {
        "", "serif" -> ResolvedFamily("'Times New Roman', serif", "Times New Roman", "\\froman")
        "sansserif", "sans-serif", "system", "dialog" ->
            ResolvedFamily("'Arial', sans-serif", "Arial", "\\fswiss")
        "monospaced", "monospace" -> ResolvedFamily("'Courier New', monospace", "Courier New", "\\fmodern")
        else -> {
            val clean = family.replace("'", "").replace(";", "")
            ResolvedFamily("'$clean'", clean, "\\fnil")
        }
    }

    /** The selected [runs] as an HTML fragment; every styled run carries its font in inline CSS. */
    fun toHtml(runs: List<DocumentTextIndex.StyledRun>): String {
        val sb = StringBuilder()
        sb.append("<div style=\"white-space:pre-wrap;\">")
        for (run in runs) {
            val font = run.font
            if (font == null) {
                sb.append(escapeHtml(run.text).replace("\n", "<br>"))
                continue
            }
            sb.append("<span style=\"")
            sb.append("font-family:").append(resolveFamily(font.family).cssStack).append(';')
            sb.append("font-size:").append(trimNumber(font.size)).append("pt;")
            sb.append("font-weight:").append(if (font.weight == FontWeight.BOLD) "bold" else "normal").append(';')
            sb.append("font-style:").append(if (font.style == FontStyle.ITALIC) "italic" else "normal").append(';')
            sb.append("\">").append(escapeHtml(run.text)).append("</span>")
        }
        sb.append("</div>")
        return sb.toString()
    }

    /**
     * The selected [runs] as a full RTF document: a `\fonttbl` with a `\fcharset0` entry per family
     * and one brace group per run carrying `\fN`, `\fsNN` (half points) and `\b` / `\i`. The
     * paragraph default is set to the first run's font and size so a reader that ignores the run
     * groups still gets the right face; separator runs inherit the surrounding font.
     */
    fun toRtf(runs: List<DocumentTextIndex.StyledRun>): String {
        val families = LinkedHashMap<String, Int>()
        for (run in runs) run.font?.let { families.getOrPut(it.family) { families.size } }
        if (families.isEmpty()) families["Serif"] = 0

        val firstFont = runs.firstNotNullOfOrNull { it.font }
        val baseId = firstFont?.let { families[it.family] } ?: 0
        val baseFs = ((firstFont?.size ?: 12.0) * 2.0).roundToInt()

        val sb = StringBuilder()
        sb.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\uc1")
        sb.append("{\\fonttbl")
        for ((family, id) in families) {
            val resolved = resolveFamily(family)
            sb.append("{\\f").append(id).append(resolved.rtfCategory).append("\\fcharset0 ")
                .append(escapeRtf(resolved.rtfName)).append(";}")
        }
        sb.append('}')
        sb.append("\\pard\\plain\\f").append(baseId).append("\\fs").append(baseFs).append(' ')

        var lastFont = firstFont
        for (run in runs) {
            val font = run.font ?: lastFont
            if (font == null) {
                sb.append(escapeRtf(run.text))
                continue
            }
            lastFont = font
            sb.append("{\\f").append(families[font.family] ?: 0)
            sb.append("\\fs").append((font.size * 2.0).roundToInt())
            if (font.weight == FontWeight.BOLD) sb.append("\\b")
            if (font.style == FontStyle.ITALIC) sb.append("\\i")
            sb.append(' ').append(escapeRtf(run.text)).append('}')
        }
        sb.append("\\par}")
        return sb.toString()
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeRtf(text: String): String = buildString {
        for (ch in text) {
            when {
                ch == '\\' -> append("\\\\")
                ch == '{' -> append("\\{")
                ch == '}' -> append("\\}")
                ch == '\n' -> append("\\line ")
                ch == '\r' -> Unit
                ch == '\t' -> append("\\tab ")
                ch.code in 0x20..0x7E -> append(ch)
                else -> {
                    val code = if (ch.code > 0x7FFF) ch.code - 0x10000 else ch.code
                    append("\\u").append(code).append('?')
                }
            }
        }
    }

    private fun trimNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
