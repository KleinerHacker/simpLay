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
 * Verifies [PageCountingMode.CONTINUOUS] and [PageCountingMode.SKIP_EXCLUDED] in isolation from the
 * planner, and that [PageCountingMode] is itself usable as a [PageCountingStrategy].
 */
class PageCountingStrategyTest {

    /**
     * Use case: each [PageCountingMode] constant is directly usable as a [PageCountingStrategy],
     * with no separate lookup or bridging step.
     */
    @Test
    fun modeIsUsableDirectlyAsAStrategy() {
        val continuous: PageCountingStrategy = PageCountingMode.CONTINUOUS
        val skipExcluded: PageCountingStrategy = PageCountingMode.SKIP_EXCLUDED

        assertTrue(continuous is PageCountingMode)
        assertTrue(skipExcluded is PageCountingMode)
    }

    /**
     * Use case: with nothing excluded, both strategies number every sheet consecutively from
     * `startNumber`.
     */
    @Test
    fun bothStrategiesNumberEverySheetWhenNothingIsExcluded() {
        val flags = List(4) { false }

        assertEquals(listOf(1, 2, 3, 4), PageCountingMode.CONTINUOUS.numbers(4, flags, 1))
        assertEquals(listOf(1, 2, 3, 4), PageCountingMode.SKIP_EXCLUDED.numbers(4, flags, 1))
    }

    /**
     * Use case: [PageCountingMode.CONTINUOUS] hides the label of an excluded sheet but keeps
     * advancing the counter for the sheets that follow it.
     */
    @Test
    fun continuousKeepsCountingAcrossExcluded() {
        val flags = listOf(false, true, false, false)

        assertEquals(listOf(1, null, 3, 4), PageCountingMode.CONTINUOUS.numbers(4, flags, 1))
    }

    /**
     * Use case: [PageCountingMode.SKIP_EXCLUDED] hides the label of an excluded sheet and does not
     * advance the counter, so the following sheet receives the number the excluded one would have
     * had.
     */
    @Test
    fun skipExcludedDoesNotAdvance() {
        val flags = listOf(false, true, false, false)

        assertEquals(listOf(1, null, 2, 3), PageCountingMode.SKIP_EXCLUDED.numbers(4, flags, 1))
    }

    /**
     * Use case: a `startNumber` other than `1` offsets the first counted sheet for both strategies.
     */
    @Test
    fun startNumberOffsetsFirstLabel() {
        val flags = List(3) { false }

        assertEquals(listOf(5, 6, 7), PageCountingMode.CONTINUOUS.numbers(3, flags, 5))
        assertEquals(listOf(5, 6, 7), PageCountingMode.SKIP_EXCLUDED.numbers(3, flags, 5))
    }

    /**
     * Use case: an excluded leading sheet still lets the first counted sheet carry `startNumber`.
     */
    @Test
    fun excludedLeadingSheetLeavesStartNumberOnFirstCountedSheet() {
        val flags = listOf(true, false, false)

        assertEquals(listOf(null, 2, 3), PageCountingMode.CONTINUOUS.numbers(3, flags, 1))
        assertEquals(listOf(null, 1, 2), PageCountingMode.SKIP_EXCLUDED.numbers(3, flags, 1))
    }
}
