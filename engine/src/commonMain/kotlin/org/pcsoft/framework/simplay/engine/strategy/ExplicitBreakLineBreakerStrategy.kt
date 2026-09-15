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

/**
 * Splits this list of [TextPart]s into segments at every [TextBreak], discarding the [TextBreak]
 * tokens themselves.
 *
 * Two consecutive [TextBreak]s, or a [TextBreak] at the very start or end of the list, yield an
 * empty segment. A list without any [TextBreak] yields a single segment equal to the whole list.
 *
 * Shared by every [LineBreakerStrategy] that must treat a [TextBreak] as a hard line separator; used
 * directly by [ExplicitBreakLineBreakerStrategy] and equivalent to the per-part handling the default
 * strategies apply inline.
 */
internal fun List<TextPart>.splitAtBreaks(): List<List<TextPart>> {
    val segments = mutableListOf<List<TextPart>>()
    var current = mutableListOf<TextPart>()
    for (part in this) {
        if (part is TextBreak) {
            segments += current
            current = mutableListOf()
        } else {
            current += part
        }
    }
    segments += current
    return segments
}

/**
 * A [LineBreakerStrategy] that only breaks at an explicit [TextBreak] token and never on width.
 *
 * [parts] is split into segments with [splitAtBreaks]; each segment becomes exactly one
 * [UnplacedLine] regardless of [maxWidth] - a segment wider than [maxWidth] simply overflows its
 * line. An empty segment (two consecutive breaks, or a break at the very start or end) still
 * produces an empty line, using the font's metric ascent/descent as fallback, so a blank line takes
 * up vertical space like any other. Within a segment, a [TextWhitespace] part produces no glyph and
 * only marks the next part to receive a leading space, and a [TextAnchor] is kept as a zero-width
 * part - the same rules the default strategies apply. The [WordBreakerStrategy] is accepted but
 * ignored, matching [NoWrapLineBreakerStrategy].
 */
object ExplicitBreakLineBreakerStrategy : LineBreakerStrategy {

    override fun breakIntoLines(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine> {
        if (parts.isEmpty()) return emptyList()

        val spaceWidth = measurer.measure(font.raw, " ").width
        return parts.splitAtBreaks().map { segment -> buildLine(segment, font, spaceWidth, measurer) }
    }

    private fun buildLine(
        segment: List<TextPart>,
        font: MeasuredFont,
        spaceWidth: Double,
        measurer: FontMeasureCalculator,
    ): UnplacedLine {
        val unplacedParts = mutableListOf<UnplacedPart>()
        var pendingSpace = false
        var ascent = 0.0
        var descent = 0.0

        for (part in segment) {
            if (part is TextWhitespace) {
                pendingSpace = true
                continue
            }

            if (part is TextAnchor) {
                unplacedParts += UnplacedPart(part, 0.0, 0.0)
                continue
            }

            val metrics = measurer.measure(font.raw, part.text)
            val space = if (unplacedParts.isEmpty() || !pendingSpace) 0.0 else spaceWidth
            pendingSpace = false
            unplacedParts += UnplacedPart(part, metrics.width, space)
            ascent = maxOf(ascent, metrics.ascent)
            descent = maxOf(descent, metrics.descent)
        }

        return UnplacedLine(
            parts = unplacedParts,
            ascent = if (ascent > 0.0) ascent else font.metrics.ascent,
            descent = if (descent > 0.0) descent else font.metrics.descent,
        )
    }
}
