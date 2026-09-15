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

/**
 * Computes syllable-accurate hyphenation points for a word using Liang's algorithm: the word is
 * padded with a `.` boundary marker on both ends, every pattern in a [LiangPatternSet] that occurs
 * as a substring of the padded word contributes its per-gap values, the highest value per gap wins,
 * and a gap with an odd value is a permissible break.
 */
object LiangHyphenator {

    /** No break is offered closer than this many characters to either end of the word. */
    private const val MIN_LEFT = 2
    private const val MIN_RIGHT = 2

    /**
     * Returns the character offsets inside [word] at which [patterns] allow a hyphenation break, in
     * ascending order. Matching is case-insensitive; offsets are into the original [word].
     */
    fun breakOffsets(word: String, patterns: LiangPatternSet): List<Int> {
        if (word.length < MIN_LEFT + MIN_RIGHT) return emptyList()

        val padded = ".${word.lowercase()}."
        val gapValues = IntArray(padded.length + 1)

        for (start in padded.indices) {
            var node = patterns.root
            for (end in start until padded.length) {
                node = node.children[padded[end]] ?: break
                val patternValues = node.values ?: continue
                for (k in patternValues.indices) {
                    val gap = start + k
                    if (patternValues[k] > gapValues[gap]) gapValues[gap] = patternValues[k]
                }
            }
        }

        return (MIN_LEFT..(word.length - MIN_RIGHT)).filter { split -> gapValues[split + 1] % 2 == 1 }
    }
}
