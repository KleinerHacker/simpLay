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

import org.pcsoft.framework.simplay.engine.model.Font

/**
 * Seam for intra-word breaking (hyphenation). A [LineBreakerStrategy] asks a [WordBreakerStrategy]
 * where a single word that is wider than the available width may be split.
 *
 * IP-03 ships only the no-op [NoOpWordBreakerStrategy]; real hyphenation strategies are added later
 * (see feature plan FP-002). The strategy is set on the [SimpLayEngine.Builder] and defaults to
 * [NoOpWordBreakerStrategy].
 */
fun interface WordBreakerStrategy {

    /**
     * Returns the character offsets inside [word] at which a line break is allowed, in ascending
     * order. An empty list means the word must not be split.
     *
     * @param word the word to inspect, without surrounding whitespace.
     * @param font the font the word is rendered in.
     * @param maxWidth the width available for a single line, as a unit-less double.
     * @param measurer the callback used to measure candidate parts.
     * @return allowed break offsets in `1 until word.length`, ascending; empty to forbid splitting.
     */
    fun breakOffsets(
        word: String,
        font: Font,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
    ): List<Int>
}

/**
 * The default [WordBreakerStrategy]: never offers a break, so no hyphenation happens and an
 * over-long word simply overflows its line.
 */
object NoOpWordBreakerStrategy : WordBreakerStrategy {

    override fun breakOffsets(
        word: String,
        font: Font,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
    ): List<Int> = emptyList()
}
