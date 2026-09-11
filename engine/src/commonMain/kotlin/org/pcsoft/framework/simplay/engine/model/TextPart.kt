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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * A single atomic piece of text, including whitespace runs (see [TextWhitespace]).
 */
@Serializable
sealed interface TextPart : PlatformSerializable {
    val text: String
}

/**
 * A maximal run of letters and/or digits.
 */
@Serializable
@SerialName("word")
data class TextWord(override val text: String) : TextPart

/**
 * A single non-letter, non-digit, non-whitespace character.
 *
 * The public API takes the [Char]; internally it is kept as its single-character [text].
 */
@Serializable
@SerialName("symbol")
@ConsistentCopyVisibility
data class TextSymbol private constructor(override val text: String) :
    TextPart {

    constructor(symbol: Char) : this(symbol.toString())

    /** The single character of this symbol. */
    val symbol: Char get() = text.first()
}

/**
 * The kind of whitespace character a [TextWhitespace] run is made of.
 */
@Serializable
enum class WhitespaceKind {
    /** A run made of the space character (`' '`). */
    SPACE,

    /** A run made of the tab character (`'\t'`). */
    TAB,
}

/**
 * A maximal run of whitespace characters of a single [kind] (space or tab; a run never mixes the
 * two, nor does it cross a line break).
 *
 * Preserved verbatim for a lossless [TextBlock.Companion.of] / [TextBlock.toString] round trip, but
 * it is not addressable on its own: it produces no glyph in layout and no caret-selectable segment
 * in `DocumentTextIndex`.
 */
@Serializable
@SerialName("whitespace")
data class TextWhitespace(val kind: WhitespaceKind, override val text: String) : TextPart
