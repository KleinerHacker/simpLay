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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextBlockTest {

    private val style: TextStyle =
        TextStyle(
            Font(
                "Serif",
                12.0
            )
        )

    /**
     * Verifies that [TextBlock.Companion.of] followed by [toString] reproduces a sentence whose words are
     * separated by single spaces.
     */
    @Test
    fun roundTripsPlainSentence() {
        assertEquals("The quick brown fox", TextBlock.of("The quick brown fox", style).toString())
    }

    /**
     * Verifies that punctuation keeps its position without a leading space while following words
     * are still space-separated.
     */
    @Test
    fun roundTripsSentenceWithPunctuation() {
        assertEquals(
            "Hello, world! Are you there?",
            TextBlock.of("Hello, world! Are you there?", style).toString(),
        )
    }

    /**
     * Verifies that a run of multiple whitespace characters is preserved verbatim on the round trip
     * instead of being normalized to a single space.
     */
    @Test
    fun preservesMultipleWhitespaceVerbatim() {
        assertEquals("a   b \t c", TextBlock.of("a   b \t c", style).toString())
    }

    /**
     * Verifies that [TextBlock.Companion.of] stores the tokenized parts in order and keeps the style.
     */
    @Test
    fun ofStoresTokenizedParts() {
        val block = TextBlock.of("one.", style)
        assertEquals(listOf(
            TextWord("one"),
            TextSymbol('.')
        ), block.parts)
        assertEquals(style, block.style)
        assertEquals("one.", block.toString())
    }

    /**
     * Verifies that [TextBlock.Companion.of] carries the given style through to the block.
     */
    @Test
    fun ofCarriesStyle() {
        val block = TextBlock.of("text", style)
        assertTrue(block.style.font.family == "Serif" && block.style.font.size == 12.0)
    }

    /**
     * Verifies that [TextBlock.withStyle] swaps the style while keeping the tokenized parts of the
     * original block untouched.
     */
    @Test
    fun withStyleReplacesStyleAndKeepsParts() {
        val block = TextBlock.of("one two.", style)
        val restyled = block.withStyle(style.copy(alignment = TextAlignment.CENTER))

        assertEquals(block.parts, restyled.parts)
        assertEquals(TextAlignment.CENTER, restyled.style.alignment)
        assertEquals(TextAlignment.LEFT, block.style.alignment)
    }

    /**
     * Verifies the exact bug scenario reported for the caret drift: a word directly following a
     * symbol without any original whitespace between them (`"paragraph.X"`) round trips through
     * [TextBlock.Companion.of] and [toString] without an invented space at the symbol/word boundary.
     */
    @Test
    fun toStringRoundTripsExactOriginalTextIncludingSymbolWordBoundary() {
        assertEquals("paragraph.X", TextBlock.of("paragraph.X", style).toString())
    }

    /**
     * Verifies that alternating runs of several spaces and several tabs - at the start, between
     * words, directly after a symbol and at the end - are reproduced character for character by
     * [toString], with no run normalized, merged or dropped.
     */
    @Test
    fun toStringRoundTripsAlternatingSpaceAndTabRuns() {
        val text = "  \t\tone \t   two.\tthree\t \tfour  "
        assertEquals(text, TextBlock.of(text, style).toString())
    }
}
