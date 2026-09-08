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

package org.pcsoft.framework.simplay.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.model.TextSymbol

/**
 * Verifies that the greedy line breaker attaches a [TextSymbol] without a leading space and still
 * wraps it onto a new line when it no longer fits.
 */
class SymbolAttachTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: a trailing punctuation symbol follows its word on the same line with no gap in
     * front of it.
     */
    @Test
    fun symbolFollowsWordWithoutLeadingSpace() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("word!"),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(2, lines[0].parts.size)
        assertEquals("!", lines[0].parts[1].part.text)
        assertEquals(0.0, lines[0].parts[1].spaceBefore)
    }

    /**
     * Use case: when the word fills the line, the following symbol wraps onto the next line on its
     * own, still without a leading space.
     */
    @Test
    fun symbolWrapsOntoNextLineWhenItNoLongerFits() {
        val lines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("word!"),
            font = font,
            maxWidth = 28.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals("word", lines[0].parts.single().part.text)
        val wrapped = lines[1].parts.single()
        assertTrue(wrapped.part is TextSymbol)
        assertEquals(0.0, wrapped.spaceBefore)
    }
}
