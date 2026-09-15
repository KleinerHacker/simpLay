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
 * A [LineBreakerStrategy] that never breaks: all parts land in a single [UnplacedLine] that may be
 * wider than [maxWidth]. The [WordBreakerStrategy] is ignored. A [TextWhitespace] part produces no
 * glyph and only marks the next part to receive a leading space, following the same rule as the
 * greedy strategy. A [TextBreak] is likewise ignored - it produces no glyph and does not mark a
 * pending space - since this strategy never breaks a line in the first place.
 */
object NoWrapLineBreakerStrategy : LineBreakerStrategy {

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
            acc.add(part, metrics.width, space, metrics.ascent, metrics.descent)
        }

        return acc.result()
    }
}
