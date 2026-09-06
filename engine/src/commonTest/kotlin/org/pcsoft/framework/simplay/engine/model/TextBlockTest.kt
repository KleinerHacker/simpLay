package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextBlockTest {

    private val style: org.pcsoft.framework.simplay.engine.model.TextStyle =
        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextStyle(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Font(
                "Serif",
                12.0
            )
        )

    /**
     * Verifies that [org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of] followed by [toString] reproduces a sentence whose words are
     * separated by single spaces.
     */
    @Test
    fun roundTripsPlainSentence() {
        assertEquals("The quick brown fox", _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("The quick brown fox", style).toString())
    }

    /**
     * Verifies that punctuation keeps its position without a leading space while following words
     * are still space-separated.
     */
    @Test
    fun roundTripsSentenceWithPunctuation() {
        assertEquals(
            "Hello, world! Are you there?",
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("Hello, world! Are you there?", style).toString(),
        )
    }

    /**
     * Verifies that runs of multiple whitespace characters are normalized to single spaces on the
     * round trip.
     */
    @Test
    fun normalizesMultipleWhitespace() {
        assertEquals("a b c", _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("a   b \t c", style).toString())
    }

    /**
     * Verifies that [org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of] stores the tokenized parts in order and keeps the style.
     */
    @Test
    fun ofStoresTokenizedParts() {
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("one.", style)
        assertEquals(listOf(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextWord("one"),
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextSymbol('.')
        ), block.parts)
        assertEquals(style, block.style)
        assertEquals("one.", block.toString())
    }

    /**
     * Verifies that [org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of] carries the given style through to the block.
     */
    @Test
    fun ofCarriesStyle() {
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("text", style)
        assertTrue(block.style.font.family == "Serif" && block.style.font.size == 12.0)
    }
}
