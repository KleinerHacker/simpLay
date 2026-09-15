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
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies [LiangPatternParser]: it turns Liang/TeX pattern lines into a trie of per-gap value
 * arrays, one node per letter, skipping blank lines, comment lines and letters with no digit.
 */
class LiangPatternParserTest {

    private fun walk(set: LiangPatternSet, letters: String): PatternTrieNode? {
        var node: PatternTrieNode? = set.root
        for (c in letters) {
            node = node?.children?.get(c)
        }
        return node
    }

    /**
     * Use case: a single pattern with one embedded digit stores the digit at the gap after the
     * letter it follows, and zero at every other gap.
     */
    @Test
    fun singlePatternStoresDigitAtItsGap() {
        val set = LiangPatternParser.parse("a1b")

        val node = walk(set, "ab")

        assertContentEquals(intArrayOf(0, 1, 0), node?.values)
    }

    /**
     * Use case: a leading digit (before any letter) is stored at gap zero.
     */
    @Test
    fun leadingDigitStoresAtGapZero() {
        val set = LiangPatternParser.parse("4ab")

        val node = walk(set, "ab")

        assertContentEquals(intArrayOf(4, 0, 0), node?.values)
    }

    /**
     * Use case: several patterns on separate lines each create their own trie path.
     */
    @Test
    fun multiplePatternLinesAreAllStored() {
        val set = LiangPatternParser.parse("a1b\nc2d")

        assertContentEquals(intArrayOf(0, 1, 0), walk(set, "ab")?.values)
        assertContentEquals(intArrayOf(0, 2, 0), walk(set, "cd")?.values)
    }

    /**
     * Use case: several whitespace-separated patterns on one line are all stored.
     */
    @Test
    fun patternsSeparatedByWhitespaceOnOneLineAreAllStored() {
        val set = LiangPatternParser.parse("a1b c2d")

        assertContentEquals(intArrayOf(0, 1, 0), walk(set, "ab")?.values)
        assertContentEquals(intArrayOf(0, 2, 0), walk(set, "cd")?.values)
    }

    /**
     * Use case: blank lines and `%`-comment lines contribute no pattern.
     */
    @Test
    fun blankAndCommentLinesAreIgnored() {
        val set = LiangPatternParser.parse("\n% comment\na1b\n\n")

        assertTrue(set.root.children.keys == setOf('a'))
    }

    /**
     * Use case: parsing empty input yields an empty trie.
     */
    @Test
    fun emptyInputYieldsEmptyTrie() {
        val set = LiangPatternParser.parse("")

        assertNull(walk(set, "a"))
        assertTrue(set.root.children.isEmpty())
    }
}
