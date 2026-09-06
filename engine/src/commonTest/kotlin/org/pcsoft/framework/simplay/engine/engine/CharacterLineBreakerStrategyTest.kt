package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [CharacterLineBreakerStrategy] fills lines character by character and breaks inside
 * a word, unlike the greedy strategy.
 */
class CharacterLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: a single word wider than the line is split at the character boundary that fills the
     * width, and every character is kept.
     */
    @Test
    fun splitsAWordInTheMiddle() {
        val lines = CharacterLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdefghij"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals("abcde", lines[0].parts.single().part.text)
        assertEquals("fghij", lines[1].parts.single().part.text)
        assertTrue(lines[0].parts.single().width <= 30.0)
    }

    /**
     * Use case: the greedy strategy keeps the same over-long word whole, which is the behaviour the
     * character strategy deliberately differs from.
     */
    @Test
    fun greedyStrategyKeepsTheSameWordWhole() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdefghij"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals("abcdefghij", lines[0].parts.single().part.text)
    }

    /**
     * Use case: no parts produce no lines.
     */
    @Test
    fun emptyInputProducesNoLines() {
        val lines = CharacterLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }
}
