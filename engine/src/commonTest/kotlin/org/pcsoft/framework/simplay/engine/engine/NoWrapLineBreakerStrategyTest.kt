package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [NoWrapLineBreakerStrategy] never breaks: all parts land in a single, possibly
 * over-wide line.
 */
class NoWrapLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: three words that clearly exceed the width still produce exactly one line, with the
     * word gaps preserved and no gap in front of the first word.
     */
    @Test
    fun keepsEverythingOnOneLineDespiteOverflow() {
        val lines = NoWrapLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("alpha beta gamma"),
            font = font,
            maxWidth = 10.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(3, lines[0].parts.size)
        assertEquals(0.0, lines[0].parts[0].spaceBefore)
        assertEquals(EngineTestData.SPACE_WIDTH, lines[0].parts[1].spaceBefore)
        assertEquals(EngineTestData.SPACE_WIDTH, lines[0].parts[2].spaceBefore)
        assertTrue(lines[0].naturalWidth > 10.0)
    }

    /**
     * Use case: no parts produce no lines.
     */
    @Test
    fun emptyInputProducesNoLines() {
        val lines = NoWrapLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 10.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }
}
