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

class TokenizerTest {

    private val style: TextStyle =
        TextStyle(
            Font(
                "Serif",
                12.0
            )
        )

    private fun parts(text: String): List<TextPart> = TextBlock.of(text, style).parts

    /**
     * Verifies that a plain sentence is split into one [TextWord] per space-separated token, with a
     * [TextWhitespace] of kind [WhitespaceKind.SPACE] preserved between the words.
     */
    @Test
    fun splitsPlainWords() {
        assertEquals(
            listOf(
                TextWord("The"),
                TextWhitespace(WhitespaceKind.SPACE, " "),
                TextWord("quick"),
                TextWhitespace(WhitespaceKind.SPACE, " "),
                TextWord("fox")
            ),
            parts("The quick fox"),
        )
    }

    /**
     * Verifies that a trailing punctuation mark becomes its own [TextSymbol] separate from the
     * preceding word, and that the space after it is preserved as its own part.
     */
    @Test
    fun splitsTrailingPunctuation() {
        assertEquals(
            listOf(
                TextWord("Hello"),
                TextSymbol(','),
                TextWhitespace(WhitespaceKind.SPACE, " "),
                TextWord("world"),
                TextSymbol('!')
            ),
            parts("Hello, world!"),
        )
    }

    /**
     * Verifies that letters and digits inside one run are kept together in a single [TextWord].
     */
    @Test
    fun keepsLettersAndDigitsTogether() {
        assertEquals(listOf(TextWord("abc123")), parts("abc123"))
    }

    /**
     * Verifies that a hyphen between two words is emitted as a standalone [TextSymbol].
     */
    @Test
    fun splitsHyphen() {
        assertEquals(
            listOf(
                TextWord("state"),
                TextSymbol('-'),
                TextWord("of"),
                TextSymbol('-'),
                TextWord("art")
            ),
            parts("state-of-art"),
        )
    }

    /**
     * Verifies that quotation marks around a word are emitted as individual [TextSymbol]s.
     */
    @Test
    fun splitsQuotes() {
        assertEquals(
            listOf(
                TextSymbol('"'),
                TextWord("hi"),
                TextSymbol('"')
            ),
            parts("\"hi\""),
        )
    }

    /**
     * Verifies that a line break acts as a word separator like a space, and is itself preserved as a
     * [TextWhitespace] of kind [WhitespaceKind.SPACE] so the round trip stays lossless.
     */
    @Test
    fun treatsLineBreakAsSeparator() {
        assertEquals(
            listOf(
                TextWord("first"),
                TextWhitespace(WhitespaceKind.SPACE, "\n"),
                TextWord("second")
            ),
            parts("first\nsecond"),
        )
    }

    /**
     * Verifies that a run of whitespace mixing spaces and a tab splits into one [TextWhitespace] per
     * kind change, each run keeping its exact original characters.
     */
    @Test
    fun mixedWhitespaceSplitsAtKindChange() {
        assertEquals(
            listOf(
                TextWord("a"),
                TextWhitespace(WhitespaceKind.SPACE, "    "),
                TextWhitespace(WhitespaceKind.TAB, "\t"),
                TextWhitespace(WhitespaceKind.SPACE, "  "),
                TextWord("b")
            ),
            parts("a    \t  b"),
        )
    }

    /**
     * Verifies that an empty input yields an empty part list.
     */
    @Test
    fun emptyInputYieldsNoParts() {
        assertEquals(emptyList(), parts(""))
    }

    /**
     * Verifies that a single space between two words becomes one [TextWhitespace] run of kind
     * [WhitespaceKind.SPACE].
     */
    @Test
    fun tokenizePreservesSingleSpaceBetweenWords() {
        assertEquals(
            listOf(
                TextWord("one"),
                TextWhitespace(WhitespaceKind.SPACE, " "),
                TextWord("two")
            ),
            parts("one two"),
        )
    }

    /**
     * Verifies that several consecutive spaces collapse into a single [TextWhitespace] run that
     * keeps every original space character.
     */
    @Test
    fun tokenizePreservesMultipleSpacesAsOneRun() {
        assertEquals(
            listOf(
                TextWord("one"),
                TextWhitespace(WhitespaceKind.SPACE, "   "),
                TextWord("two")
            ),
            parts("one   two"),
        )
    }

    /**
     * Verifies that a run of tab characters is tokenized as [WhitespaceKind.TAB], distinct from a
     * run of spaces.
     */
    @Test
    fun tokenizeDistinguishesTabFromSpace() {
        assertEquals(
            listOf(
                TextWord("one"),
                TextWhitespace(WhitespaceKind.TAB, "\t\t"),
                TextWord("two")
            ),
            parts("one\t\ttwo"),
        )
    }

    /**
     * Verifies that a word directly following a symbol without any whitespace between them produces
     * no [TextWhitespace] part - the root cause of the caret-drift bug this model change fixes.
     */
    @Test
    fun tokenizeSymbolDirectlyFollowedByWordHasNoWhitespacePart() {
        assertEquals(
            listOf(
                TextWord("paragraph"),
                TextSymbol('.'),
                TextWord("X")
            ),
            parts("paragraph.X"),
        )
    }
}
