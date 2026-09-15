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

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextWhitespace

/**
 * One part of an [UnplacedLine]: a raw [part] with its advance [width] and the [spaceBefore] gap
 * that separates it from the previous part. It carries no position; `SimpLayBlockEngine` adds that
 * and produces a [org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart].
 *
 * @property part the raw text part, or a synthetic [org.pcsoft.framework.simplay.engine.model.TextWord]
 *   when a word was split.
 * @property width the advance width of [part], as a unit-less double.
 * @property spaceBefore the width of the gap in front of this part (`0.0` for the first part of a
 *   line and whenever no [TextWhitespace] preceded it in the raw parts).
 */
class UnplacedPart(
    val part: TextPart,
    val width: Double,
    val spaceBefore: Double,
)

/**
 * A line as produced by a [LineBreakerStrategy]: the parts that belong on one line, already
 * measured but not yet positioned. `SimpLayBlockEngine` assigns the positions and turns each
 * `UnplacedLine` into a [org.pcsoft.framework.simplay.engine.measure.MeasuredLine].
 *
 * @property parts the parts of this line, in reading order.
 * @property ascent the largest ascent among the parts, as a unit-less double.
 * @property descent the largest descent among the parts, as a unit-less double.
 */
class UnplacedLine(
    val parts: List<UnplacedPart>,
    val ascent: Double,
    val descent: Double,
) {
    /** The width the parts occupy without any alignment adjustment, gaps included. */
    val naturalWidth: Double
        get() = parts.sumOf { it.spaceBefore + it.width }
}

/**
 * Strategy that turns the [TextPart]s of a block into [UnplacedLine]s for a given content width.
 *
 * The strategy is set on the [org.pcsoft.framework.simplay.engine.SimpLayEngine.Builder] and
 * defaults to [GreedyWordLineBreakerStrategy]. IP-03 also ships [CharacterLineBreakerStrategy],
 * [NoWrapLineBreakerStrategy] and [ExplicitBreakLineBreakerStrategy]; feature plan FP-002 further
 * adds [BalancedLineBreakerStrategy] and [BreakOpportunityLineBreakerStrategy].
 *
 * Every implementation must be deterministic and must not call any platform API.
 */
fun interface LineBreakerStrategy {

    /**
     * Breaks [parts] into lines that each fit into [maxWidth] where the strategy allows it.
     *
     * @param parts the raw parts of one block, in reading order; may contain [TextWhitespace]
     *   parts, which never produce a glyph of their own but determine the [UnplacedPart.spaceBefore]
     *   gap in front of the next non-whitespace part, and may contain a
     *   [org.pcsoft.framework.simplay.engine.model.TextBreak], which every default strategy consumes
     *   as a hard line separator (or ignores for [NoWrapLineBreakerStrategy]) rather than passing on
     *   to [UnplacedLine].
     * @param font the resolved font of the block.
     * @param maxWidth the content width available for a line, as a unit-less double.
     * @param measurer the callback used to measure parts.
     * @param wordBreaker the intra-word break seam; strategies that do not split inside a word
     *   ignore it.
     * @return the lines of the block, in order; an empty list when [parts] is empty.
     */
    fun breakIntoLines(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine>
}
