package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * Internal structural contract for [TextBlock].
 *
 * It exists only so the measured decorator model (`...engine.measure`) can wrap a raw text block
 * with Kotlin's `by` delegation. It is not part of the public API and carries no serialization.
 */
@PublishedApi
internal interface ITextBlock {
    val parts: List<TextPart>
    val style: TextStyle
}

/**
 * A run of styled text made of [TextPart]s. A plain data holder.
 */
@Serializable
@ConsistentCopyVisibility
data class TextBlock private constructor(
    override val parts: List<TextPart>,
    override val style: TextStyle,
) : ITextBlock {
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
        fun of(text: String, style: TextStyle): TextBlock = TextBlock(tokenize(text), style)
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
