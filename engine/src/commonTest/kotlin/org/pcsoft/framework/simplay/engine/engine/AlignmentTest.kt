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

package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.engine.internal.SimpLayBlockEngine
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies the horizontal placement rules of [SimpLayBlockEngine] for every [TextAlignment],
 * including that a justified block does not stretch its last line.
 */
class AlignmentTest {

    private val font = EngineTestData.measuredFont()
    private val blockEngine = SimpLayBlockEngine.builder().build()

    private fun singleLine(alignment: TextAlignment) = blockEngine.measure(
        raw = TextBlock.of("ab cd", EngineTestData.styleWith(alignment)),
        style = EngineTestData.measuredStyle(EngineTestData.styleWith(alignment)),
        unplacedLines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("ab cd"),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        ),
        startY = 0.0,
        contentWidth = 100.0,
        blockEndsHere = true,
    )

    /**
     * Use case: a left-aligned line starts at the content-area origin.
     */
    @Test
    fun leftAlignmentStartsAtZero() {
        val block = singleLine(TextAlignment.LEFT)

        assertEquals(0.0, block.lines[0].parts[0].bounds.x)
        assertEquals(18.0, block.lines[0].parts[1].bounds.x)
    }

    /**
     * Use case: a right-aligned line is pushed by the whole remaining width.
     */
    @Test
    fun rightAlignmentPushesByTheRemainingWidth() {
        val block = singleLine(TextAlignment.RIGHT)

        assertEquals(70.0, block.lines[0].parts[0].bounds.x)
        assertEquals(88.0, block.lines[0].parts[1].bounds.x)
    }

    /**
     * Use case: a centered line is pushed by half of the remaining width.
     */
    @Test
    fun centerAlignmentPushesByHalfTheRemainingWidth() {
        val block = singleLine(TextAlignment.CENTER)

        assertEquals(35.0, block.lines[0].parts[0].bounds.x)
    }

    /**
     * Use case: a justified block spreads the remaining width over the word gaps on every line
     * except its last, which stays left-aligned.
     */
    @Test
    fun justifyStretchesEveryLineButTheLast() {
        val block = blockEngine.measure(
            raw = TextBlock.of("aa bb cc", EngineTestData.styleWith(TextAlignment.JUSTIFY)),
            style = EngineTestData.measuredStyle(EngineTestData.styleWith(TextAlignment.JUSTIFY)),
            unplacedLines = GreedyWordLineBreakerStrategy.breakIntoLines(
                parts = EngineTestData.parts("aa bb cc"),
                font = font,
                maxWidth = 30.0,
                measurer = EngineTestData.measurer,
                wordBreaker = NoOpWordBreakerStrategy,
            ),
            startY = 0.0,
            contentWidth = 100.0,
            blockEndsHere = true,
        )

        assertEquals(2, block.lines.size)
        assertEquals(0.0, block.lines[0].parts[0].bounds.x)
        assertEquals(88.0, block.lines[0].parts[1].bounds.x)
        assertFalse(block.lines[0].lastLine)
        assertEquals(0.0, block.lines[1].parts[0].bounds.x)
        assertTrue(block.lines[1].lastLine)
    }

    /**
     * Use case: the block bounds span the full content width and the stacked line heights.
     */
    @Test
    fun blockBoundsSpanContentWidthAndStackedLines() {
        val block = singleLine(TextAlignment.LEFT)

        assertEquals(0.0, block.bounds.x)
        assertEquals(0.0, block.bounds.y)
        assertEquals(100.0, block.bounds.width)
        assertEquals(EngineTestData.LINE_HEIGHT, block.bounds.height)
    }
}
