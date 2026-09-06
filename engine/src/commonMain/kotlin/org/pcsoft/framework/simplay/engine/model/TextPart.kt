package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single atomic piece of text. Whitespace is never stored in a part.
 */
@Serializable
sealed interface TextPart {
    val text: String
}

/**
 * A maximal run of letters and/or digits.
 */
@Serializable
@SerialName("word")
data class TextWord(override val text: String) : org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * A single non-letter, non-digit, non-whitespace character.
 *
 * The public API takes the [Char]; internally it is kept as its single-character [text].
 */
@Serializable
@SerialName("symbol")
@ConsistentCopyVisibility
data class TextSymbol private constructor(override val text: String) :
    org.pcsoft.framework.simplay.engine.model.TextPart {

    constructor(symbol: Char) : this(symbol.toString())

    /** The single character of this symbol. */
    val symbol: Char get() = text.first()
}
