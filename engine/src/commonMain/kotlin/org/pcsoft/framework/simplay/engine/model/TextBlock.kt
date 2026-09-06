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

import kotlin.collections.plusAssign
import kotlin.text.iterator
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
     * Rejoins the parts, putting a single space before every [TextWord] except the first and no
     * space before a [TextSymbol].
     */
    override fun toString(): String = buildString {
        parts.forEachIndexed { index, part ->
            if (part is TextWord && index > 0) append(' ')
            append(part.text)
        }
    }

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
 * character becomes its own [TextSymbol], and whitespace separates parts without being stored.
 */
private fun tokenize(text: String): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    val word = StringBuilder()

    fun flushWord() {
        if (word.isNotEmpty()) {
            parts += TextWord(word.toString())
            word.clear()
        }
    }

    for (ch in text) {
        when {
            ch.isWhitespace() -> flushWord()
            ch.isLetterOrDigit() -> word.append(ch)
            else -> {
                flushWord()
                parts += TextSymbol(ch)
            }
        }
    }
    flushWord()
    return parts
}
