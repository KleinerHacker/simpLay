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
     * Verifies that a plain sentence is split into one [TextWord] per space-separated token and
     * that no whitespace is kept.
     */
    @Test
    fun splitsPlainWords() {
        assertEquals(
            listOf(
                TextWord("The"),
                TextWord("quick"),
                TextWord("fox")
            ),
            parts("The quick fox"),
        )
    }

    /**
     * Verifies that a trailing punctuation mark becomes its own [TextSymbol] separate from the
     * preceding word.
     */
    @Test
    fun splitsTrailingPunctuation() {
        assertEquals(
            listOf(
                TextWord("Hello"),
                TextSymbol(','),
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
     * Verifies that a line break acts as a separator just like a space and is not stored.
     */
    @Test
    fun treatsLineBreakAsSeparator() {
        assertEquals(listOf(
            TextWord("first"),
            TextWord("second")
        ), parts("first\nsecond"))
    }

    /**
     * Verifies that runs of multiple whitespace characters collapse to a single separation.
     */
    @Test
    fun collapsesMultipleWhitespace() {
        assertEquals(listOf(
            TextWord("a"),
            TextWord("b")
        ), parts("a    \t  b"))
    }

    /**
     * Verifies that an empty input yields an empty part list.
     */
    @Test
    fun emptyInputYieldsNoParts() {
        assertEquals(emptyList(), parts(""))
    }
}
