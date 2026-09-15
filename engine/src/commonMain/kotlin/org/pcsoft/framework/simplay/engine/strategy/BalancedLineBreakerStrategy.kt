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

import kotlin.math.pow
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.measure.MeasuredFont
import org.pcsoft.framework.simplay.engine.model.TextAnchor
import org.pcsoft.framework.simplay.engine.model.TextPart
import org.pcsoft.framework.simplay.engine.model.TextWhitespace
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * A [LineBreakerStrategy] that breaks a whole block at once, minimising the total raggedness of the
 * non-final lines instead of greedily filling every line.
 *
 * A [org.pcsoft.framework.simplay.engine.model.TextBreak] first splits [parts] into independent
 * segments with [splitAtBreaks]; the balancing dynamic program below runs separately per segment, so
 * a hard break can never be smoothed away by the raggedness optimisation - each segment always ends
 * its own line, even an empty one (two consecutive breaks, or a break at the very start or end),
 * which still produces an empty line using the font's metric ascent/descent.
 *
 * Within one segment, legal break points sit before a [TextWord]; a run of `symbol* word` (a
 * [org.pcsoft.framework.simplay.engine.model.TextSymbol] run directly followed by a word, without
 * an intervening [TextWhitespace]) is never split apart, so the symbols always stay attached to the
 * front of the following word. A dynamic program then picks, among all legal partitions of the
 * segment into lines, the one with the lowest total badness: a non-final line is penalised the more
 * slack it leaves, an overflowing line is penalised in proportion to the overflow, and the final line
 * of the segment (which is also the final line of the block only if it is the last segment) is free
 * to be short. Ties are resolved in favour of fewer lines, then the earliest break.
 *
 * A word wider than [maxWidth] on its own is offered to the [WordBreakerStrategy], exactly like
 * [GreedyWordLineBreakerStrategy]; if that returns no offsets the word stays whole and its line
 * overflows.
 *
 * The cost constants below are internal tuning values, not public configuration.
 */
object BalancedLineBreakerStrategy : LineBreakerStrategy {

    /** Exponent applied to the normalised slack of a non-final line. */
    private const val FILL_PENALTY_EXP: Double = 2.0

    /** Scale applied to the exponentiated slack of a non-final line. */
    private const val FILL_PENALTY_SCALE: Double = 100.0

    /** Scale applied to the absolute overflow of a line that does not fit [maxWidth]. */
    private const val OVERFLOW_PENALTY: Double = 1000.0

    override fun breakIntoLines(
        parts: List<TextPart>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine> {
        if (parts.isEmpty()) return emptyList()

        val cache = HashMap<String, TextMetrics>()
        fun measure(text: String): TextMetrics = cache.getOrPut(text) { measurer.measure(font.raw, text) }

        val spaceWidth = measure(" ").width

        val lines = mutableListOf<UnplacedLine>()
        for (segment in parts.splitAtBreaks()) {
            if (segment.isEmpty()) {
                lines += UnplacedLine(
                    parts = emptyList(),
                    ascent = font.metrics.ascent,
                    descent = font.metrics.descent,
                )
                continue
            }

            val runs = buildRuns(segment, spaceWidth, ::measure)
            val expandedRuns = expandOverlongWords(runs, font, maxWidth, measurer, wordBreaker, ::measure)
            if (expandedRuns.isEmpty()) continue

            lines += breakRuns(expandedRuns, font, maxWidth)
        }

        return lines
    }

    /**
     * One indivisible group of [SubPart]s: zero or more symbols/anchors directly followed by a word,
     * with no [TextWhitespace] between them. [spaceBefore] is the width of the gap in front of this
     * run (`0.0` for the very first run of the block).
     */
    private class Run(val subParts: MutableList<SubPart>, val spaceBefore: Double) {
        val width: Double get() = subParts.sumOf { it.width }
    }

    /** One measured [part] inside a [Run]. */
    private class SubPart(val part: TextPart, val width: Double, val ascent: Double, val descent: Double)

    /**
     * Groups [parts] into [Run]s: a new run starts whenever a [TextWhitespace] preceded the next
     * part, or at the very start of the block. A [TextAnchor] never starts a run on its own and does
     * not consume a pending space, matching [GreedyWordLineBreakerStrategy].
     */
    private fun buildRuns(
        parts: List<TextPart>,
        spaceWidth: Double,
        measure: (String) -> TextMetrics,
    ): List<Run> {
        val runs = mutableListOf<Run>()
        var pendingSpace = false

        for (part in parts) {
            when (part) {
                is TextWhitespace -> pendingSpace = true

                is TextAnchor -> {
                    val subPart = SubPart(part, 0.0, 0.0, 0.0)
                    if (runs.isEmpty()) {
                        runs += Run(mutableListOf(subPart), 0.0)
                    } else {
                        runs.last().subParts += subPart
                    }
                }

                else -> {
                    val metrics = measure(part.text)
                    val subPart = SubPart(part, metrics.width, metrics.ascent, metrics.descent)
                    if (runs.isEmpty() || pendingSpace) {
                        val spaceBefore = if (runs.isEmpty()) 0.0 else spaceWidth
                        runs += Run(mutableListOf(subPart), spaceBefore)
                    } else {
                        runs.last().subParts += subPart
                    }
                    pendingSpace = false
                }
            }
        }

        return runs
    }

    /**
     * Expands every run that consists of a single [TextWord] wider than [maxWidth] into several
     * synthetic-word runs, using the offsets [wordBreaker] allows. A run left whole simply overflows
     * its eventual line, same as [GreedyWordLineBreakerStrategy].
     */
    private fun expandOverlongWords(
        runs: List<Run>,
        font: MeasuredFont,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
        measure: (String) -> TextMetrics,
    ): List<Run> {
        val result = mutableListOf<Run>()

        for (run in runs) {
            val onlySubPart = run.subParts.singleOrNull()
            val word = onlySubPart?.part as? TextWord
            if (word == null || run.width <= maxWidth) {
                result += run
                continue
            }

            val offsets = wordBreaker
                .breakOffsets(word.text, font.raw, maxWidth, measurer)
                .filter { it in 1 until word.text.length }
                .distinct()
                .sorted()

            if (offsets.isEmpty()) {
                result += run
                continue
            }

            result += splitOverlongWord(word.text, offsets, run.spaceBefore, maxWidth, measure)
        }

        return result
    }

    /** Splits [text] into synthetic-word runs at [offsets], mirroring `placeHyphenated`. */
    private fun splitOverlongWord(
        text: String,
        offsets: List<Int>,
        spaceBefore: Double,
        maxWidth: Double,
        measure: (String) -> TextMetrics,
    ): List<Run> {
        val pieces = mutableListOf<Run>()
        var start = 0
        var first = true

        while (start < text.length) {
            val candidates = offsets.filter { it > start }
            var cut = -1
            for (candidate in candidates) {
                if (measure(text.substring(start, candidate)).width <= maxWidth) {
                    cut = candidate
                } else {
                    break
                }
            }
            val end = if (cut != -1) cut else (candidates.firstOrNull() ?: text.length)
            val piece = text.substring(start, end)
            val metrics = measure(piece)
            pieces += Run(
                mutableListOf(SubPart(TextWord(piece), metrics.width, metrics.ascent, metrics.descent)),
                if (first) spaceBefore else 0.0,
            )
            start = end
            first = false
        }

        return pieces
    }

    /**
     * Runs the `O(n^2)` dynamic program over [runs] and reconstructs the chosen lines. Accumulation
     * is strictly left to right; among equal-cost partitions the one with fewer lines - equivalently,
     * the earliest (smallest) predecessor index - wins, since candidates are evaluated in ascending
     * predecessor order and only a strictly lower cost replaces the current best.
     */
    private fun breakRuns(runs: List<Run>, font: MeasuredFont, maxWidth: Double): List<UnplacedLine> {
        val n = runs.size

        val prefixWidth = DoubleArray(n + 1)
        val prefixSpace = DoubleArray(n + 1)
        for (index in 0 until n) {
            prefixWidth[index + 1] = prefixWidth[index] + runs[index].width
            prefixSpace[index + 1] = prefixSpace[index] + runs[index].spaceBefore
        }

        fun naturalWidth(from: Int, to: Int): Double {
            val widthSum = prefixWidth[to] - prefixWidth[from]
            val spaceSum = if (to > from + 1) prefixSpace[to] - prefixSpace[from + 1] else 0.0
            return widthSum + spaceSum
        }

        val cost = DoubleArray(n + 1)
        val back = IntArray(n + 1)

        for (i in 1..n) {
            var bestCost = Double.POSITIVE_INFINITY
            var bestFrom = -1
            val isLastLineOfBlock = i == n

            for (j in 0 until i) {
                val width = naturalWidth(j, i)
                val slack = maxWidth - width
                val badness = when {
                    slack >= 0.0 && isLastLineOfBlock -> 0.0
                    slack >= 0.0 -> (slack / maxWidth).pow(FILL_PENALTY_EXP) * FILL_PENALTY_SCALE
                    else -> (-slack) * OVERFLOW_PENALTY
                }

                val candidateCost = cost[j] + badness
                if (candidateCost < bestCost) {
                    bestCost = candidateCost
                    bestFrom = j
                }
            }

            cost[i] = bestCost
            back[i] = bestFrom
        }

        val breakpoints = mutableListOf<Int>()
        var index = n
        while (index > 0) {
            breakpoints += index
            index = back[index]
        }
        breakpoints += 0
        breakpoints.reverse()

        val lines = mutableListOf<UnplacedLine>()
        for (lineIndex in 0 until breakpoints.size - 1) {
            val from = breakpoints[lineIndex]
            val to = breakpoints[lineIndex + 1]
            lines += buildLine(runs, from, to, font)
        }

        return lines
    }

    /** Turns the runs `runs[from until to)` into one [UnplacedLine]. */
    private fun buildLine(runs: List<Run>, from: Int, to: Int, font: MeasuredFont): UnplacedLine {
        val lineParts = mutableListOf<UnplacedPart>()
        var ascent = 0.0
        var descent = 0.0

        for (runIndex in from until to) {
            val run = runs[runIndex]
            for ((subIndex, subPart) in run.subParts.withIndex()) {
                val spaceBefore = if (runIndex == from && subIndex == 0) {
                    0.0
                } else if (subIndex == 0) {
                    run.spaceBefore
                } else {
                    0.0
                }
                lineParts += UnplacedPart(subPart.part, subPart.width, spaceBefore)
                ascent = maxOf(ascent, subPart.ascent)
                descent = maxOf(descent, subPart.descent)
            }
        }

        return UnplacedLine(
            parts = lineParts,
            ascent = if (ascent > 0.0) ascent else font.metrics.ascent,
            descent = if (descent > 0.0) descent else font.metrics.descent,
        )
    }
}
