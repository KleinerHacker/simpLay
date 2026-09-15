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

package org.pcsoft.framework.simplay.engine.strategy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.EngineTestData

/**
 * Verifies [PatternWordBreakerStrategy]: it builds itself from bundled hyph-utf8 patterns by locale
 * and offers hyphenation offsets computed from them, falling back to `null` for a locale with no
 * bundled patterns.
 */
class PatternWordBreakerStrategyTest {

    private val font = EngineTestData.measuredFont().raw

    /**
     * Use case: an unknown locale, for which no patterns are bundled, yields no strategy instead of
     * throwing.
     */
    @Test
    fun unknownLocaleYieldsNoStrategy() {
        val strategy = PatternWordBreakerStrategy.forLocale("xx-unknown")

        assertNull(strategy)
    }

    /**
     * Use case: the bundled English patterns hyphenate a long, common English word at plausible,
     * strictly ascending, in-bounds offsets.
     */
    @Test
    fun englishLocaleHyphenatesALongWord() {
        val strategy = assertNotNull(PatternWordBreakerStrategy.forLocale("en"))

        val word = "information"
        val offsets = strategy.breakOffsets(word, font, maxWidth = 1000.0, measurer = EngineTestData.measurer)

        assertTrue(offsets.isNotEmpty())
        assertEquals(offsets.sorted().distinct(), offsets)
        assertTrue(offsets.all { it in 1 until word.length })
    }

    /**
     * Use case: the bundled German patterns hyphenate a long, common German word at plausible,
     * strictly ascending, in-bounds offsets.
     */
    @Test
    fun germanLocaleHyphenatesALongWord() {
        val strategy = assertNotNull(PatternWordBreakerStrategy.forLocale("de"))

        val word = "Silbentrennung"
        val offsets = strategy.breakOffsets(word, font, maxWidth = 1000.0, measurer = EngineTestData.measurer)

        assertTrue(offsets.isNotEmpty())
        assertEquals(offsets.sorted().distinct(), offsets)
        assertTrue(offsets.all { it in 1 until word.length })
    }
}
