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
 * The kind of whitespace character a [TextWhitespace] run is made of. Each kind carries the [char]
 * every character of such a run consists of.
 */
@Serializable
enum class WhitespaceKind(val char: Char) {
    /** A run made of the space character (`' '`). */
    SPACE(' '),

    /** A run made of the tab character (`'\t'`). */
    TAB('\t'),

    /** A run made of the line feed character (`'\n'`). */
    LINE_BREAK('\n'),
}

/**
 * A maximal run of [count] whitespace characters of a single [kind] (a run never mixes kinds).
 *
 * The run is described by kind and length only; its [text] is derived from those, so the same
 * characters are never stored twice. Preserved for a lossless [TextBlock.Companion.of] /
 * [TextBlock.toString] round trip, but not addressable on its own: it produces no glyph in layout
 * and no caret-selectable segment in `DocumentTextIndex`.
 */
@Serializable
@SerialName("whitespace")
data class TextWhitespace(val kind: WhitespaceKind, val count: Int = 1) : TextPart {

    init {
        require(count >= 1) { "A whitespace run needs at least one character, got $count" }
    }

    /** The [count] characters of this run, derived from [WhitespaceKind.char]. */
    override val text: String
        get() = kind.char.toString().repeat(count)
}

/**
 * An invisible, zero-width navigation marker identified by [id].
 *
 * A [TextAnchor] carries no visible glyph - it is never painted and never contributes to
 * `charCount`/`wordCount`/`symbolCount` - but it still occupies a caret-addressable and
 * scroll-targetable position (a zero-width `MeasuredTextPart`/segment), so callers can jump the
 * caret or the viewport directly to it via its [id]. The [id] must be unique within a document;
 * behaviour for a duplicate [id] is defined by the consumer resolving it (e.g. `DocumentTextIndex`).
 *
 * Written with the `${id}` syntax by [TextBlock.Companion.of] (see [TextBlock.toString] for the
 * inverse), e.g. `${myAnchor}` tokenizes to `TextAnchor("myAnchor")`.
 */
@Serializable
@SerialName("anchor")
data class TextAnchor(val id: String) : TextPart {

    init {
        require(id.isNotEmpty()) { "An anchor needs a non-empty id" }
    }

    /** Always empty: a [TextAnchor] has no visible representation. */
    override val text: String get() = ""
}
