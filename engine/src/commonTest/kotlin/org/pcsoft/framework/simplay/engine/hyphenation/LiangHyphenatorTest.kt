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

package org.pcsoft.framework.simplay.engine.hyphenation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [LiangHyphenator]: it applies a [LiangPatternSet] to a word using Liang's algorithm -
 * highest pattern value per gap wins, an odd value is a permissible break - while respecting the
 * minimum distance from either end of the word.
 */
class LiangHyphenatorTest {

    /**
     * Use case: a single matching pattern with an odd gap value produces exactly the break it
     * encodes, once the surrounding word is long enough to clear the minimum edge distance.
     */
    @Test
    fun matchingPatternWithOddValueProducesABreak() {
        val patterns = LiangPatternParser.parse("a1b")

        val offsets = LiangHyphenator.breakOffsets("xaabby", patterns)

        assertEquals(listOf(3), offsets)
    }

    /**
     * Use case: a matching pattern with an even gap value forbids a break at that gap.
     */
    @Test
    fun matchingPatternWithEvenValueProducesNoBreak() {
        val patterns = LiangPatternParser.parse("a2b")

        val offsets = LiangHyphenator.breakOffsets("xaabby", patterns)

        assertEquals(emptyList(), offsets)
    }

    /**
     * Use case: when two overlapping patterns disagree on a gap, the higher value wins, so an odd
     * value from a more specific pattern can override an even value from a shorter one.
     */
    @Test
    fun higherPatternValueWinsOnOverlap() {
        val patterns = LiangPatternParser.parse("a2b\nxa3ab")

        val offsets = LiangHyphenator.breakOffsets("xaabby", patterns)

        assertEquals(listOf(2), offsets)
    }

    /**
     * Use case: no pattern in the set matches anywhere in the word, so no break is offered.
     */
    @Test
    fun noMatchingPatternProducesNoBreak() {
        val patterns = LiangPatternParser.parse("q1z")

        val offsets = LiangHyphenator.breakOffsets("xaabby", patterns)

        assertEquals(emptyList(), offsets)
    }

    /**
     * Use case: a word shorter than the combined minimum left/right edge distance never offers a
     * break, even if a pattern would otherwise match.
     */
    @Test
    fun tooShortWordProducesNoBreak() {
        val patterns = LiangPatternParser.parse("a1b")

        val offsets = LiangHyphenator.breakOffsets("ab", patterns)

        assertEquals(emptyList(), offsets)
    }

    /**
     * Use case: a break that a pattern would place closer to either edge than the minimum distance
     * allows is suppressed, even though the gap value itself is odd.
     */
    @Test
    fun breakTooCloseToWordEdgeIsSuppressed() {
        val patterns = LiangPatternParser.parse("a1b")

        val offsets = LiangHyphenator.breakOffsets("abaabb", patterns)

        assertEquals(listOf(4), offsets)
    }
}
