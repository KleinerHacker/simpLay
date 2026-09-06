package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable
import kotlin.collections.plusAssign
import kotlin.text.iterator

/**
 * A run of styled text made of [org.pcsoft.framework.simplay.engine.model.TextPart]s. A plain data holder.
 */
@Serializable
@ConsistentCopyVisibility
data class TextBlock private constructor(
    val parts: List<org.pcsoft.framework.simplay.engine.model.TextPart>,
    val style: org.pcsoft.framework.simplay.engine.model.TextStyle,
) {
    /**
     * Rejoins the parts, putting a single space before every [org.pcsoft.framework.simplay.engine.model.TextWord] except the first and no
     * space before a [org.pcsoft.framework.simplay.engine.model.TextSymbol].
     */
    override fun toString(): String = buildString {
        parts.forEachIndexed { index, part ->
            if (part is org.pcsoft.framework.simplay.engine.model.TextWord && index > 0) append(' ')
            append(part.text)
        }
    }

    companion object {
        /**
         * Builds a [TextBlock] by tokenizing [text] with the given [style].
         */
        fun of(text: String, style: org.pcsoft.framework.simplay.engine.model.TextStyle): org.pcsoft.framework.simplay.engine.model.TextBlock =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.tokenize(text), style
            )
    }
}

/**
 * Splits [text] into [org.pcsoft.framework.simplay.engine.model.TextPart]s.
 *
 * Maximal runs of [Char.isLetterOrDigit] become a [org.pcsoft.framework.simplay.engine.model.TextWord], every other non-whitespace
 * character becomes its own [org.pcsoft.framework.simplay.engine.model.TextSymbol], and whitespace separates parts without being stored.
 */
private fun tokenize(text: String): List<org.pcsoft.framework.simplay.engine.model.TextPart> {
    val parts = mutableListOf<org.pcsoft.framework.simplay.engine.model.TextPart>()
    val word = StringBuilder()

    fun flushWord() {
        if (word.isNotEmpty()) {
            parts += _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord(word.toString())
            word.clear()
        }
    }

    for (ch in text) {
        when {
            ch.isWhitespace() -> flushWord()
            ch.isLetterOrDigit() -> word.append(ch)
            else -> {
                flushWord()
                parts += _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol(ch)
            }
        }
    }
    flushWord()
    return parts
}
