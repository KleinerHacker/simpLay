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
 * The default [LineBreakerStrategy]: greedy, word- and symbol-aware.
 *
 * Parts are added to the current line until the next part no longer fits, then a new line starts.
 * A [TextWhitespace] part produces no glyph of its own; instead it marks the very next part to
 * receive a single [FontMeasureCalculator]-measured space width in front of it (unless that part
 * starts a new line). A part with no preceding [TextWhitespace] - e.g. a [TextWord] directly after a
 * symbol - is attached without a leading space. A word wider than [maxWidth] on its own is offered
 * to the [WordBreakerStrategy]; if that returns no offsets the word stays whole and overflows its
 * line. A [TextBreak] is a hard break: the current line is flushed (even if empty, so a blank line
 * still takes up vertical space) and no leading space carries over to the next line.
 */
object GreedyWordLineBreakerStrategy : LineBreakerStrategy {

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

            val metrics = measurer.measure(font.raw, part.text)
            val space = if (acc.isEmpty || !pendingSpace) 0.0 else spaceWidth
            pendingSpace = false

            if (!acc.isEmpty && acc.currentWidth + space + metrics.width > maxWidth) {
                acc.flush()
            }

            if (acc.isEmpty && metrics.width > maxWidth) {
                val offsets = wordBreaker
                    .breakOffsets(part.text, font.raw, maxWidth, measurer)
                    .filter { it in 1 until part.text.length }
                    .distinct()
                    .sorted()
                if (offsets.isNotEmpty()) {
                    placeHyphenated(part.text, offsets, font, maxWidth, measurer, acc)
                } else {
                    acc.add(part, metrics.width, 0.0, metrics.ascent, metrics.descent)
                    acc.flush()
                }
                continue
            }

            val effectiveSpace = if (acc.isEmpty) 0.0 else space
            acc.add(part, metrics.width, effectiveSpace, metrics.ascent, metrics.descent)
        }

        return acc.result()
    }

    private fun placeHyphenated(
        text: String,
        offsets: List<Int>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        acc: LineAccumulator,
    ) {
        var start = 0
        while (start < text.length) {
            val candidates = offsets.filter { it > start }
            var cut = -1
            for (candidate in candidates) {
                if (measurer.measure(font.raw, text.substring(start, candidate)).width <= maxWidth) {
                    cut = candidate
                } else {
                    break
                }
            }
            val end = if (cut != -1) cut else (candidates.firstOrNull() ?: text.length)
            val piece = text.substring(start, end)
            val pieceMetrics = measurer.measure(font.raw, piece)
            acc.add(TextWord(piece), pieceMetrics.width, 0.0, pieceMetrics.ascent, pieceMetrics.descent)
            start = end
            if (start < text.length) acc.flush()
        }
    }
}
