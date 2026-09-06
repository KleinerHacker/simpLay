package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the greedy, word-aware line breaker: a line is filled part by part, a single leading
 * space separates words, and a part that would exceed the width starts a new line.
 */
class GreedyWordLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: two words plus the separating space that together match the width exactly stay on
     * one line.
     */
    @Test
    fun keepsWordsOnOneLineWhenTheyFitExactly() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("ab cd"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(2, lines[0].parts.size)
        assertEquals(0.0, lines[0].parts[0].spaceBefore)
        assertEquals(EngineTestData.SPACE_WIDTH, lines[0].parts[1].spaceBefore)
        assertEquals(30.0, lines[0].naturalWidth)
    }

    /**
     * Use case: when the second word plus its space exceed the width by one unit, that word moves
     * to a new line and starts without a leading space.
     */
    @Test
    fun wrapsWordThatExceedsTheWidth() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("ab cd"),
            font = font,
            maxWidth = 29.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals("ab", lines[0].parts.single().part.text)
        assertEquals("cd", lines[1].parts.single().part.text)
        assertEquals(0.0, lines[1].parts.single().spaceBefore)
    }

    /**
     * Use case: the derived line ascent and descent come from the measured parts.
     */
    @Test
    fun linesCarryTheMeasuredAscentAndDescent() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("hello world"),
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(EngineTestData.ASCENT, lines[0].ascent)
        assertEquals(EngineTestData.DESCENT, lines[0].descent)
    }

    /**
     * Use case: no parts produce no lines.
     */
    @Test
    fun emptyInputProducesNoLines() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }
}
