package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {

    private val style: org.pcsoft.framework.simplay.engine.model.TextStyle =
        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextStyle(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Font(
                "Serif",
                12.0
            )
        )

    private fun parts(text: String): List<org.pcsoft.framework.simplay.engine.model.TextPart> = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(text, style).parts

    /**
     * Verifies that a plain sentence is split into one [org.pcsoft.framework.simplay.engine.model.TextWord] per space-separated token and
     * that no whitespace is kept.
     */
    @Test
    fun splitsPlainWords() {
        assertEquals(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("The"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("quick"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("fox")
            ),
            parts("The quick fox"),
        )
    }

    /**
     * Verifies that a trailing punctuation mark becomes its own [org.pcsoft.framework.simplay.engine.model.TextSymbol] separate from the
     * preceding word.
     */
    @Test
    fun splitsTrailingPunctuation() {
        assertEquals(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("Hello"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol(','),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("world"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('!')
            ),
            parts("Hello, world!"),
        )
    }

    /**
     * Verifies that letters and digits inside one run are kept together in a single [org.pcsoft.framework.simplay.engine.model.TextWord].
     */
    @Test
    fun keepsLettersAndDigitsTogether() {
        assertEquals(listOf(_root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("abc123")), parts("abc123"))
    }

    /**
     * Verifies that a hyphen between two words is emitted as a standalone [org.pcsoft.framework.simplay.engine.model.TextSymbol].
     */
    @Test
    fun splitsHyphen() {
        assertEquals(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("state"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('-'),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("of"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('-'),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("art")
            ),
            parts("state-of-art"),
        )
    }

    /**
     * Verifies that quotation marks around a word are emitted as individual [org.pcsoft.framework.simplay.engine.model.TextSymbol]s.
     */
    @Test
    fun splitsQuotes() {
        assertEquals(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('"'),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("hi"),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('"')
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
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("first"),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("second")
        ), parts("first\nsecond"))
    }

    /**
     * Verifies that runs of multiple whitespace characters collapse to a single separation.
     */
    @Test
    fun collapsesMultipleWhitespace() {
        assertEquals(listOf(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("a"),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("b")
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
