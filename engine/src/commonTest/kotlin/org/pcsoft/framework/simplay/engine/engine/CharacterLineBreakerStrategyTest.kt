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
