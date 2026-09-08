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

package org.pcsoft.framework.simplay.engine.internal

import org.pcsoft.framework.simplay.engine.UnplacedLine
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextStyle
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Measure stage that places the [UnplacedLine]s of one block slice at absolute positions, in
 * coordinates relative to the page content area.
 *
 * One of the internal `SimpLay*Engine` stages driven by
 * [org.pcsoft.framework.simplay.engine.SimpLayEngine], invoked by [SimpLayPageEngine];
 * created through [builder]. It carries no configuration. Lines are stacked from `startY`
 * downwards, one [MeasuredTextStyle.resolvedLineHeight] apart. The horizontal offset follows the
 * block alignment: `LEFT` at `0`, `RIGHT` at the remaining width, `CENTER` at half of it.
 * `JUSTIFY` spreads the remaining width evenly over the word gaps, except on the final line of the
 * block.
 */
internal class SimpLayBlockEngine private constructor() {

    /**
     * Measures one block slice into a [MeasuredTextBlock].
     *
     * @param raw the raw block this slice belongs to.
     * @param style the resolved style of [raw].
     * @param unplacedLines the lines placed by this call; may be empty.
     * @param startY the vertical cursor, relative to the content area, where the first line starts.
     * @param contentWidth the content width available for the block.
     * @param blockEndsHere `true` when [unplacedLines] contains the last line of the block, so its
     *   final line is treated as the block's last line.
     */
    fun measure(
        raw: TextBlock,
        style: MeasuredTextStyle,
        unplacedLines: List<UnplacedLine>,
        startY: Double,
        contentWidth: Double,
        blockEndsHere: Boolean,
    ): MeasuredTextBlock {
        val lineHeight = style.resolvedLineHeight
        val alignment = style.alignment
        val measuredLines = ArrayList<MeasuredLine>(unplacedLines.size)
        var y = startY

        unplacedLines.forEachIndexed { index, line ->
            val lastLine = blockEndsHere && index == unplacedLines.lastIndex
            val slack = (contentWidth - line.naturalWidth).coerceAtLeast(0.0)
            val justify = alignment == TextAlignment.JUSTIFY && !lastLine
            val gapCount = line.parts.count { it.spaceBefore > 0.0 }
            val extraPerGap = if (justify && gapCount > 0) slack / gapCount else 0.0
            val startX = when (alignment) {
                TextAlignment.LEFT, TextAlignment.JUSTIFY -> 0.0
                TextAlignment.RIGHT -> slack
                TextAlignment.CENTER -> slack / 2.0
            }

            var x = startX
            val partHeight = line.ascent + line.descent
            val parts = line.parts.map { unplacedPart ->
                x += unplacedPart.spaceBefore
                if (unplacedPart.spaceBefore > 0.0) x += extraPerGap
                val bounds = Rect(x, y, unplacedPart.width, partHeight)
                x += unplacedPart.width
                MeasuredTextPart(unplacedPart.part, bounds)
            }

            measuredLines += MeasuredLine(
                parts = parts,
                lineBox = Rect(0.0, y, contentWidth, lineHeight),
                baseline = line.ascent,
                ascent = line.ascent,
                descent = line.descent,
                alignment = alignment,
                lastLine = lastLine,
            )
            y += lineHeight
        }

        return MeasuredTextBlock(
            raw = raw,
            lines = measuredLines,
            bounds = Rect(0.0, startY, contentWidth, y - startY),
            style = style,
        )
    }

    /** Collects the parts of a [SimpLayBlockEngine] and creates it. Obtained from [builder]. */
    class Builder internal constructor() {

        /** Builds the block engine. */
        fun build(): SimpLayBlockEngine = SimpLayBlockEngine()
    }

    companion object {

        /** Starts a [Builder]. */
        fun builder(): Builder = Builder()
    }
}
