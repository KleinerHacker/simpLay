package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies the [WordBreakerStrategy] seam: the no-op default leaves an over-long word whole, while a
 * custom strategy is consulted by the greedy line breaker and by the engine.
 */
class WordBreakerStrategyHookTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: with [NoOpWordBreakerStrategy] a word wider than the line stays on a single,
     * over-wide line.
     */
    @Test
    fun noOpDefaultLeavesTheOverLongWordUnbroken() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdefghij"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(60.0, lines[0].parts.single().width)
    }

    /**
     * Use case: a custom strategy that offers a break offset makes the greedy line breaker split
     * the word at that offset.
     */
    @Test
    fun customStrategyOffsetSplitsTheWord() {
        val breakAtFive = WordBreakerStrategy { _, _, _, _ -> listOf(5) }

        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdefghij"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = breakAtFive,
        )

        assertEquals(2, lines.size)
        assertEquals("abcde", lines[0].parts.single().part.text)
        assertEquals("fghij", lines[1].parts.single().part.text)
    }

    /**
     * Use case: the strategy set on the [SimpLayEngine.Builder] reaches the line breaker.
     */
    @Test
    fun engineForwardsTheConfiguredStrategyToTheLineBreaker() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = EngineTestData.pageLayout(width = 30.0, height = 300.0),
                    blocks = listOf(TextBlock.of("abcdefghij", EngineTestData.style)),
                ),
            ),
        )

        val measured = SimpLayEngine.builder(EngineTestData.measurer)
            .wordBreakerStrategy { _, _, _, _ -> listOf(5) }
            .build()
            .measure(document)

        val lines = measured.pages[0].blocks[0].lines
        assertEquals(2, lines.size)
    }
}
