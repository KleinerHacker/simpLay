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
import org.pcsoft.framework.simplay.engine.model.TextAnchor
import org.pcsoft.framework.simplay.engine.model.TextBreak
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextWhitespace
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * A [LineBreakerStrategy] that fills lines character by character and breaks at any position, even
 * inside a word. It never consults the [WordBreakerStrategy]. A [TextWhitespace] part is skipped
 * entirely (no glyph, no character offered to the character-fitting loop) and only marks the next
 * non-whitespace part's first chunk to receive a leading space, following the same rule as the
 * greedy strategy. A [TextBreak] is a hard break: the current line is flushed (even if empty) and no
 * leading space carries over to the next line.
 */
object CharacterLineBreakerStrategy : LineBreakerStrategy {

    override fun breakIntoLines(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine> {
        if (parts.isEmpty()) return emptyList()

        val spaceWidth = measurer.measure(font.raw, " ").width
        val acc = LineAccumulator(font)
        var pendingSpace = false

        for (part in parts) {
            if (part is TextBreak) {
                acc.flush()
                pendingSpace = false
                continue
            }

            if (part is TextWhitespace) {
                pendingSpace = true
                continue
            }

            if (part is TextAnchor) {
                acc.add(part, 0.0, 0.0, 0.0, 0.0)
                continue
            }

            var text = part.text
            var firstChunk = true
            while (text.isNotEmpty()) {
                val space = if (acc.isEmpty || !pendingSpace || !firstChunk) 0.0 else spaceWidth
                val remaining = maxWidth - acc.currentWidth - space
                val whole = measurer.measure(font.raw, text)

                val fit = if (whole.width <= remaining) {
                    text.length
                } else {
                    largestPrefix(text, font, remaining, measurer, atLeastOne = acc.isEmpty)
                }

                if (fit == 0) {
                    acc.flush()
                    continue
                }

                val head = if (fit == text.length) text else text.substring(0, fit)
                val headMetrics = if (fit == text.length) whole else measurer.measure(font.raw, head)
                val headPart = if (head == part.text) part else TextWord(head)
                acc.add(
                    headPart,
                    headMetrics.width,
                    if (acc.isEmpty) 0.0 else space,
                    headMetrics.ascent,
                    headMetrics.descent,
                )
                text = text.substring(fit)
                firstChunk = false
                if (text.isNotEmpty()) acc.flush()
            }
            pendingSpace = false
        }

        return acc.result()
    }

    /**
     * Returns the largest `n` in `1..text.length` whose first `n` characters measure no wider than
     * [limit]. Returns `1` when nothing fits and [atLeastOne] is set, otherwise `0`.
     */
    private fun largestPrefix(
        text: String,
        font: MeasuredFont,
        limit: Double,
        measurer: FontMeasureCalculator,
        atLeastOne: Boolean,
    ): Int {
        var low = 1
        var high = text.length
        var best = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (measurer.measure(font.raw, text.substring(0, mid)).width <= limit) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return if (best == 0 && atLeastOne) 1 else best
    }
}
