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
import org.pcsoft.framework.simplay.engine.EngineTestData
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextSymbol
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * Verifies the break-opportunity line breaker: a greedy filler that only ever cuts at a position
 * [classifySymbol]/[isCjk] allow, never inside a non-breaking run.
 */
class BreakOpportunityLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    private fun texts(lines: List<UnplacedLine>): List<List<String>> =
        lines.map { line -> line.parts.map { it.part.text } }

    /**
     * Use case: no parts produce no lines.
     */
    @Test
    fun emptyPartsProduceNoLines() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }

    /**
     * Use case: plain Latin words separated by a single space break exactly like
     * [GreedyWordLineBreakerStrategy] - one line when everything fits, a leading space on the second
     * word.
     */
    @Test
    fun breaksAtSpaceLikeGreedyForPlainText() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
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
        assertEquals(30.0, lines[0].naturalWidth)
    }

    /**
     * Use case: a single long run of letters with no punctuation is a non-breaking run; it never
     * gets cut, even though it is far wider than the available width.
     */
    @Test
    fun neverBreaksInsideNonBreakingRun() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = listOf(TextWord("abcdefghij")),
            font = font,
            maxWidth = 10.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(1, lines[0].parts.size)
        assertEquals("abcdefghij", lines[0].parts[0].part.text)
        assertEquals(60.0, lines[0].naturalWidth)
    }

    /**
     * Use case: `word,` allows a break right after the trailing comma, so `next` moves to a new line
     * while the comma stays attached to `word`.
     */
    @Test
    fun breaksAfterTrailingPunctuation() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("word,next"),
            font = font,
            maxWidth = 30.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("word", ","), listOf("next")), texts(lines))
        assertEquals(0.0, lines[1].parts[0].spaceBefore)
    }

    /**
     * Use case: `word(next)` allows a break before the opening bracket, but the bracket, the word
     * inside and the closing bracket stay glued together on the next line.
     */
    @Test
    fun breaksBeforeOpeningBracket() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("word(next)"),
            font = font,
            maxWidth = 24.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("word"), listOf("(", "next", ")")), texts(lines))
        assertEquals(0.0, lines[1].parts[0].spaceBefore)
    }

    /**
     * Use case: a `1`/`,`/`000` token stream - a thousands separator between two digit runs - never
     * gets a break at the separator, even when the whole group overflows the line.
     */
    @Test
    fun numericGuardKeepsThousandsSeparatorTogether() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = listOf(TextWord("1"), TextSymbol(','), TextWord("000")),
            font = font,
            maxWidth = 12.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("1", ",", "000")), texts(lines))
        assertEquals(30.0, lines[0].naturalWidth)
    }

    /**
     * Use case: a CJK-only word wider than the available width is broken between ideographs, one per
     * line when only two fit.
     */
    @Test
    fun cjkTextBreaksBetweenIdeographs() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("你好世界"),
            font = font,
            maxWidth = 12.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("你", "好"), listOf("世", "界")), texts(lines))
    }

    /**
     * Use case: a word mixing Latin letters and ideographs never breaks between two Latin letters,
     * but does break between ideographs.
     */
    @Test
    fun cjkAndLatinMixedRespectsBothRules() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("ab你好世"),
            font = font,
            maxWidth = 12.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("ab"), listOf("你", "好"), listOf("世")), texts(lines))
    }

    /**
     * Use case: breaking the same parts twice yields the exact same lines, since the strategy is a
     * pure function of its input.
     */
    @Test
    fun deterministicAcrossRuns() {
        val parts = EngineTestData.parts("word,next(more) 1,000 你好世")

        val first = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = 18.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )
        val second = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = 18.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(texts(first), texts(second))
    }

    /**
     * Use case: an explicit [org.pcsoft.framework.simplay.engine.model.TextBreak] forces a hard line
     * end regardless of the curated break-opportunity table - even though the two segments would fit
     * comfortably combined, each ends up on its own line.
     */
    @Test
    fun textBreakForcesHardLineBreak() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("aa bb\ncc dd"),
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(listOf(listOf("aa", "bb"), listOf("cc", "dd")), texts(lines))
    }

    /**
     * Use case: two consecutive explicit breaks (a blank source line) produce an empty line in
     * between, using the font's metric ascent/descent as fallback so it still takes up vertical
     * space.
     */
    @Test
    fun consecutiveTextBreaksProduceEmptyLine() {
        val lines = BreakOpportunityLineBreakerStrategy.breakIntoLines(
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
     * Use case: setting [BreakOpportunityLineBreakerStrategy] on the [SimpLayEngine.Builder] routes
     * measuring through it instead of the default [GreedyWordLineBreakerStrategy].
     */
    @Test
    fun swapViaBuilderRoutesToStrategy() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = EngineTestData.pageLayout(width = 10.0, height = 300.0),
                    blocks = listOf(TextBlock.of("aa bb cc", EngineTestData.style)),
                ),
            ),
        )
        val engine = SimpLayEngine.builder(EngineTestData.measurer)
            .lineBreakerStrategy(BreakOpportunityLineBreakerStrategy)
            .build()

        val lineCount = engine.measure(document).pages.sumOf { page -> page.blocks.sumOf { it.lines.size } }

        assertEquals(3, lineCount)
    }
}
