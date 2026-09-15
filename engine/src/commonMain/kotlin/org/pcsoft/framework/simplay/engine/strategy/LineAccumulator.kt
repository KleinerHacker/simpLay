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

import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * Collects [UnplacedPart]s into [UnplacedLine]s. The ascent and descent of a flushed line fall back
 * to the font metrics when no part contributed a positive value.
 *
 * Shared by [GreedyWordLineBreakerStrategy], [CharacterLineBreakerStrategy] and
 * [NoWrapLineBreakerStrategy].
 */
internal class LineAccumulator(private val font: MeasuredFont) {

    private val lines = mutableListOf<UnplacedLine>()
    private var parts = mutableListOf<UnplacedPart>()
    private var width = 0.0
    private var ascent = 0.0
    private var descent = 0.0

    /**
     * Returns true if the accumulator is empty, false otherwise.
     */
    val isEmpty: Boolean get() = parts.isEmpty()

    /**
     * Returns the current width of the accumulated parts.
     */
    val currentWidth: Double get() = width

    /**
     * Adds a text part with its associated metrics to the current line segment being accumulated.
     * Updates the total width, ascent, and descent properties of the segment.
     *
     * @param part the text part to be added, represented as a [TextPart].
     * @param partWidth the advance width of the text part as a unit-less double.
     * @param spaceBefore the width of the preceding space before this text part.
     * @param partAscent the ascent of the text part, used to update the maximum ascent of the segment.
     * @param partDescent the descent of the text part, used to update the maximum descent of the segment.
     */
    fun add(
        part: TextPart,
        partWidth: Double,
        spaceBefore: Double,
        partAscent: Double,
        partDescent: Double,
    ) {
        parts += UnplacedPart(part, partWidth, spaceBefore)
        width += spaceBefore + partWidth
        ascent = maxOf(ascent, partAscent)
        descent = maxOf(descent, partDescent)
    }

    /**
     * Finalizes the current line segment by converting its accumulated parts into an [UnplacedLine]
     * and adding it to the list of lines.
     *
     * This method calculates the ascent and descent of the line using the font's metrics as a fallback
     * when specific values are not provided. Once the line is created, all associated properties of
     * the current line segment are reset, ready to accumulate the next segment.
     *
     * Ensure that no residual parts or measurements persist after this method is called.
     */
    fun flush() {
        if (parts.isEmpty()) return
        lines += UnplacedLine(
            parts = parts.toList(),
            ascent = if (ascent > 0.0) ascent else font.metrics.ascent,
            descent = if (descent > 0.0) descent else font.metrics.descent,
        )
        parts = mutableListOf()
        width = 0.0
        ascent = 0.0
        descent = 0.0
    }

    /**
     * Retrieves a list of all accumulated lines as [UnplacedLine] instances and resets the internal state.
     *
     * This method finalizes the current line, if any, by calling `flush()` before returning a copy
     * of the list of all lines accumulated so far. Each `UnplacedLine` in the result contains information
     * about the parts, ascent, and descent of the corresponding line, with measurements already processed
     * but positioning still pending.
     *
     * @return the list of accumulated lines as [UnplacedLine] objects, each representing a finalized
     *         but unpositioned line.
     */
    fun result(): List<UnplacedLine> {
        flush()
        return lines.toList()
    }
}
