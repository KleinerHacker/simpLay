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

import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextSymbol
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * One part of an [UnplacedLine]: a raw [part] with its advance [width] and the [spaceBefore] gap
 * that separates it from the previous part. It carries no position; [SimpLayBlockEngine] adds that
 * and produces a [org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart].
 *
 * @property part the raw text part, or a synthetic [TextWord] when a word was split.
 * @property width the advance width of [part], as a unit-less double.
 * @property spaceBefore the width of the gap in front of this part (`0.0` for the first part of a
 *   line and for every [TextSymbol]).
 */
class UnplacedPart(
    val part: TextPart,
    val width: Double,
    val spaceBefore: Double,
)

/**
 * A line as produced by a [LineBreakerStrategy]: the parts that belong on one line, already
 * measured but not yet positioned. [SimpLayBlockEngine] assigns the positions and turns each
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
 * The strategy is set on the [SimpLayEngine.Builder] and defaults to
 * [GreedyWordLineBreakerStrategy]. IP-03 also ships [CharacterLineBreakerStrategy] and
 * [NoWrapLineBreakerStrategy]; further strategies are added by feature plan FP-002.
 *
 * Every implementation must be deterministic and must not call any platform API.
 */
fun interface LineBreakerStrategy {

    /**
     * Breaks [parts] into lines that each fit into [maxWidth] where the strategy allows it.
     *
     * @param parts the raw parts of one block, in reading order, without whitespace parts.
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

/**
 * The default [LineBreakerStrategy]: greedy, word- and symbol-aware.
 *
 * Parts are added to the current line until the next part no longer fits, then a new line starts.
 * A single space precedes every [TextWord] except the first of a line; a [TextSymbol] is attached
 * without a leading space. A word wider than [maxWidth] on its own is offered to the
 * [WordBreakerStrategy]; if that returns no offsets the word stays whole and overflows its line.
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

        for (part in parts) {
            val metrics = measurer.measure(font.raw, part.text)
            val space = if (acc.isEmpty || part is TextSymbol) 0.0 else spaceWidth

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

/**
 * A [LineBreakerStrategy] that fills lines character by character and breaks at any position, even
 * inside a word. It never consults the [WordBreakerStrategy]. Word gaps follow the same rule as the
 * greedy strategy.
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

        for (part in parts) {
            var text = part.text
            var firstChunk = true
            while (text.isNotEmpty()) {
                val space = if (acc.isEmpty || part is TextSymbol || !firstChunk) 0.0 else spaceWidth
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
        }

        return acc.result()
    }
}

/**
 * A [LineBreakerStrategy] that never breaks: all parts land in a single [UnplacedLine] that may be
 * wider than [maxWidth]. The [WordBreakerStrategy] is ignored.
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

        for (part in parts) {
            val metrics = measurer.measure(font.raw, part.text)
            val space = if (acc.isEmpty || part is TextSymbol) 0.0 else spaceWidth
            acc.add(part, metrics.width, space, metrics.ascent, metrics.descent)
        }

        return acc.result()
    }
}

/**
 * Collects [UnplacedPart]s into [UnplacedLine]s. The ascent and descent of a flushed line fall back
 * to the font metrics when no part contributed a positive value.
 */
private class LineAccumulator(private val font: MeasuredFont) {

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
