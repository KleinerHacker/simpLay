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

package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * A run of styled text made of [TextPart]s. A plain data holder.
 */
@Serializable
@ConsistentCopyVisibility
data class TextBlock private constructor(
    val parts: List<TextPart>,
    val style: TextStyle,
) : PlatformSerializable {
    /**
     * Rejoins the parts by concatenating [TextPart.text] in order. Lossless: whitespace runs are
     * stored as explicit [TextWhitespace] parts, so no character is invented or dropped.
     */
    override fun toString(): String = buildString {
        parts.forEach { part -> append(part.text) }
    }

    /**
     * Returns a copy of this block with [style] replaced and the [parts] kept as they are.
     *
     * Used by the authoring side to re-stamp a block's font (e.g. with a
     * [FontFingerprint][org.pcsoft.framework.simplay.engine.model.FontFingerprint]) without
     * re-tokenizing the text.
     */
    fun withStyle(style: TextStyle): TextBlock = TextBlock(parts, style)

    companion object {
        /**
         * Builds a [TextBlock] by tokenizing [text] with the given [style].
         */
        fun of(text: String, style: TextStyle): TextBlock =
            TextBlock(
                tokenize(text), style
            )
    }
}

/**
 * Splits [text] into [TextPart]s.
 *
 * Maximal runs of [Char.isLetterOrDigit] become a [TextWord], every other non-whitespace
 * character becomes its own [TextSymbol], and a maximal run of whitespace of a single
 * [WhitespaceKind] (space, tab or line break) becomes a [TextWhitespace] carrying that kind and the
 * length of the run; a run splits at a change of kind (e.g. a space directly followed by a tab
 * yields two [TextWhitespace] parts). Any other whitespace character is taken as
 * [WhitespaceKind.SPACE].
 */
private fun tokenize(text: String): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    val word = StringBuilder()
    var whitespaceCount = 0
    var whitespaceKind: WhitespaceKind? = null

    fun flushWord() {
        if (word.isNotEmpty()) {
            parts += TextWord(word.toString())
            word.clear()
        }
    }

    fun flushWhitespace() {
        val kind = whitespaceKind
        if (kind != null && whitespaceCount > 0) {
            parts += TextWhitespace(kind, whitespaceCount)
        }
        whitespaceCount = 0
        whitespaceKind = null
    }

    for (ch in text) {
        when {
            ch.isWhitespace() -> {
                flushWord()
                val kind = when (ch) {
                    '\t' -> WhitespaceKind.TAB
                    '\n' -> WhitespaceKind.LINE_BREAK
                    else -> WhitespaceKind.SPACE
                }
                if (whitespaceKind != null && whitespaceKind != kind) flushWhitespace()
                whitespaceKind = kind
                whitespaceCount++
            }
            ch.isLetterOrDigit() -> {
                flushWhitespace()
                word.append(ch)
            }
            else -> {
                flushWord()
                flushWhitespace()
                parts += TextSymbol(ch)
            }
        }
    }
    flushWord()
    flushWhitespace()
    return parts
}
