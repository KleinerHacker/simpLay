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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.EngineTestData
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies the balanced, whole-block line breaker: a dynamic program picks the partition into lines
 * with the lowest total raggedness instead of greedily filling every line, while keeping the same
 * word-granularity contract as [GreedyWordLineBreakerStrategy] (word-breaker seam, symbol attachment,
 * deterministic output).
 */
class BalancedLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    /** Sum of the squared positive slack over every line but the last of [lines]. */
    private fun raggedness(lines: List<UnplacedLine>, maxWidth: Double): Double =
        lines.dropLast(1).sumOf { line ->
            val slack = maxWidth - line.naturalWidth
            if (slack > 0.0) slack * slack else 0.0
        }

    /**
     * Use case: no parts produce no lines, same contract as every other [LineBreakerStrategy].
     */
    @Test
    fun emptyPartsProduceNoLines() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }

    /**
     * Use case: two words plus the separating space that together match the width exactly stay on
     * one line, matching the greedy strategy on the trivial single-line case.
     */
    @Test
    fun singleShortLineStaysOneLine() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("ab cd"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(2, lines[0].parts.size)
        assertEquals(0.0, lines[0].parts[0].spaceBefore)
        assertEquals(EngineTestData.SPACE_WIDTH, lines[0].parts[1].spaceBefore)
    }

    /**
     * Use case: a block whose greedy partition packs the first of two non-final lines to the brim
     * (leaving the second one very ragged) is broken more evenly by the balanced strategy, so the
     * total squared slack over its non-final lines is strictly lower than greedy's.
     */
    @Test
    fun balancedReducesRaggednessVersusGreedy() {
        val parts = EngineTestData.parts("aa bb cc dd ee ff GGGGGGGGGG")
        val maxWidth = 66.0

        val greedyLines = GreedyWordLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = maxWidth,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )
        val balancedLines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = maxWidth,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        // Greedy packs four short words onto the first line, leaving the second one very ragged.
        assertEquals(listOf("aa", "bb", "cc", "dd"), greedyLines[0].parts.map { it.part.text })
        // The balanced strategy trades a little slack on the first line for much less on the second.
        assertEquals(listOf("aa", "bb", "cc"), balancedLines[0].parts.map { it.part.text })

        val greedyRaggedness = raggedness(greedyLines, maxWidth)
        val balancedRaggedness = raggedness(balancedLines, maxWidth)
        assertTrue(
            balancedRaggedness < greedyRaggedness,
            "expected balanced raggedness $balancedRaggedness to be lower than greedy's $greedyRaggedness",
        )
    }

    /**
     * Use case: a short final line does not force the earlier lines to be squeezed tighter than
     * necessary; the first line still uses the largest word group that fits.
     */
    @Test
    fun lastLineRaggednessIsFree() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("aaaa b c d"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals(listOf("aaaa"), lines[0].parts.map { it.part.text })
        assertEquals(listOf("b", "c", "d"), lines[1].parts.map { it.part.text })
        assertEquals(30.0, lines[1].naturalWidth)
    }

    /**
     * Use case: a word wider than the available width with no [WordBreakerStrategy] offer stays whole
     * and simply overflows its (single) line, no exception thrown.
     */
    @Test
    fun overlongWordWithoutWordBreakerOverflowsSingleLine() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdefghij"),
            font = font,
            maxWidth = 18.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals("abcdefghij", lines[0].parts.single().part.text)
        assertTrue(lines[0].naturalWidth > 18.0)
    }

    /**
     * Use case: a word wider than the available width is split at the offsets a stub
     * [WordBreakerStrategy] allows, same contract as greedy's `placeHyphenated`.
     */
    @Test
    fun overlongWordWithWordBreakerIsSplit() {
        val stubWordBreaker = WordBreakerStrategy { _, _, _, _ -> listOf(3) }

        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("abcdef"),
            font = font,
            maxWidth = 18.0,
            measurer = EngineTestData.measurer,
            wordBreaker = stubWordBreaker,
        )

        assertEquals(2, lines.size)
        assertEquals("abc", lines[0].parts.single().part.text)
        assertEquals("def", lines[1].parts.single().part.text)
        assertEquals(0.0, lines[0].parts.single().spaceBefore)
        assertEquals(0.0, lines[1].parts.single().spaceBefore)
    }

    /**
     * Use case: breaking the same input twice yields the exact same line structure, so the strategy
     * is deterministic.
     */
    @Test
    fun deterministicAcrossRuns() {
        val parts = EngineTestData.parts("aa bb cc dd ee ff GGGGGGGGGG")

        fun snapshot(): List<List<Pair<String, Double>>> = BalancedLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = 66.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        ).map { line -> line.parts.map { it.part.text to it.spaceBefore } }

        assertEquals(snapshot(), snapshot())
    }

    /**
     * Use case: a symbol directly followed by a word (no whitespace between them) is never split from
     * that word - when the whole run does not fit on the current line, it moves to the next line as a
     * unit, and the leading space of the run is dropped since it now starts the line.
     */
    @Test
    fun symbolsStayAttachedToFollowingWord() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("aaaa b,cc"),
            font = font,
            maxWidth = 26.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals(listOf("aaaa"), lines[0].parts.map { it.part.text })
        assertEquals(listOf("b", ",", "cc"), lines[1].parts.map { it.part.text })
        assertEquals(0.0, lines[1].parts[0].spaceBefore)
        assertEquals(0.0, lines[1].parts[1].spaceBefore)
        assertEquals(0.0, lines[1].parts[2].spaceBefore)
    }

    /**
     * Use case: an explicit [org.pcsoft.framework.simplay.engine.model.TextBreak] forces a hard line
     * end regardless of the raggedness optimisation - even though the two segments would fit
     * comfortably combined, each ends up on its own line.
     */
    @Test
    fun textBreakForcesHardLineBreak() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("aa bb\ncc dd"),
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(2, lines.size)
        assertEquals(listOf("aa", "bb"), lines[0].parts.map { it.part.text })
        assertEquals(listOf("cc", "dd"), lines[1].parts.map { it.part.text })
    }

    /**
     * Use case: two consecutive explicit breaks (a blank source line) produce an empty line in
     * between, using the font's metric ascent/descent as fallback so it still takes up vertical
     * space.
     */
    @Test
    fun consecutiveTextBreaksProduceEmptyLine() {
        val lines = BalancedLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("first\n\nsecond"),
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(3, lines.size)
        assertEquals(0, lines[1].parts.size)
        assertEquals(EngineTestData.ASCENT, lines[1].ascent)
        assertEquals(EngineTestData.DESCENT, lines[1].descent)
    }

    /**
     * Use case: setting [BalancedLineBreakerStrategy] on the [SimpLayEngine.Builder] is the strategy
     * that actually measures the document, producing its more evenly filled first line instead of the
     * greedy default's maximal one.
     */
    @Test
    fun swapViaBuilderProducesBalancedOutput() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = EngineTestData.pageLayout(width = 66.0, height = 300.0),
                    blocks = listOf(TextBlock.of("aa bb cc dd ee ff GGGGGGGGGG", EngineTestData.style)),
                ),
            ),
        )
        val engine = SimpLayEngine.builder(EngineTestData.measurer)
            .lineBreakerStrategy(BalancedLineBreakerStrategy)
            .build()

        val firstLineWords = engine.measure(document).pages.single().blocks.single()
            .lines[0].parts.map { it.text }

        assertEquals(listOf("aa", "bb", "cc"), firstLineWords)
    }
}
