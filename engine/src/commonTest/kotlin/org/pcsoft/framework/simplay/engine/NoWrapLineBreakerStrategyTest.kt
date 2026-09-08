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
