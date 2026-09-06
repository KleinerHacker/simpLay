package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that the [LineBreakerStrategy] set on the [SimpLayEngine.Builder] is the one that is
 * used.
 */
class LineBreakerStrategySwapTest {

    private val document = Document(
        pages = listOf(
            FlowPage(
                layout = EngineTestData.pageLayout(width = 10.0, height = 300.0),
                blocks = listOf(TextBlock.of("aa bb cc", EngineTestData.style)),
            ),
        ),
    )

    private fun lineCount(engine: SimpLayEngine): Int =
        engine.measure(document).pages.sumOf { page -> page.blocks.sumOf { it.lines.size } }

    /**
     * Use case: without an explicit strategy the greedy default is used, so three words that each
     * exceed the width become three lines.
     */
    @Test
    fun defaultStrategyIsGreedy() {
        val engine = SimpLayEngine.builder(EngineTestData.measurer).build()

        assertEquals(3, lineCount(engine))
    }

    /**
     * Use case: setting [NoWrapLineBreakerStrategy] on the builder makes the same document measure
     * to a single line.
     */
    @Test
    fun configuredNoWrapStrategyIsUsed() {
        val engine = SimpLayEngine.builder(EngineTestData.measurer)
            .lineBreakerStrategy(NoWrapLineBreakerStrategy)
            .build()

        assertEquals(1, lineCount(engine))
    }
}
