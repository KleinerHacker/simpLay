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
 * Verifies [ExplicitBreakLineBreakerStrategy]: it only breaks at an explicit
 * [org.pcsoft.framework.simplay.engine.model.TextBreak] token and never on width.
 */
class ExplicitBreakLineBreakerStrategyTest {

    private val font = EngineTestData.measuredFont()

    /**
     * Use case: no parts produce no lines.
     */
    @Test
    fun emptyPartsProduceNoLines() {
        val lines = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = emptyList(),
            font = font,
            maxWidth = 100.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(0, lines.size)
    }

    /**
     * Use case: text with two explicit breaks produces exactly three lines, one per segment, each
     * carrying only the words of its own segment.
     */
    @Test
    fun oneLinePerSegment() {
        val lines = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("alpha beta\ngamma\ndelta epsilon"),
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(3, lines.size)
        assertEquals(listOf("alpha", "beta"), lines[0].parts.map { it.part.text })
        assertEquals(listOf("gamma"), lines[1].parts.map { it.part.text })
        assertEquals(listOf("delta", "epsilon"), lines[2].parts.map { it.part.text })
    }

    /**
     * Use case: a segment far wider than [maxWidth] still lands on a single line - this strategy
     * never wraps on width, only on an explicit break.
     */
    @Test
    fun neverWrapsOnWidthEvenWhenOverflowing() {
        val lines = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("alpha beta gamma delta epsilon"),
            font = font,
            maxWidth = 1.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(1, lines.size)
        assertEquals(5, lines[0].parts.size)
        assertTrue(lines[0].naturalWidth > 1.0)
    }

    /**
     * Use case: two consecutive breaks (a blank line) produce an empty line in between, using the
     * font's metric ascent/descent as fallback so it still takes up vertical space.
     */
    @Test
    fun consecutiveBreaksProduceEmptyLine() {
        val lines = ExplicitBreakLineBreakerStrategy.breakIntoLines(
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
     * Use case: a word wider than [maxWidth] is not offered to the [WordBreakerStrategy] - it stays
     * whole and simply overflows its line, matching [NoWrapLineBreakerStrategy].
     */
    @Test
    fun wordBreakerIsIgnored() {
        var wasCalled = false
        val trackingWordBreaker = WordBreakerStrategy { word, font, maxWidth, measurer ->
            wasCalled = true
            listOf(word.length / 2)
        }

        val lines = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = EngineTestData.parts("supercalifragilisticexpialidocious"),
            font = font,
            maxWidth = 1.0,
            measurer = EngineTestData.measurer,
            wordBreaker = trackingWordBreaker,
        )

        assertEquals(1, lines.size)
        assertEquals(1, lines[0].parts.size)
        assertEquals("supercalifragilisticexpialidocious", lines[0].parts.single().part.text)
        assertTrue(!wasCalled)
    }

    /**
     * Use case: breaking the same parts twice yields the same result both times.
     */
    @Test
    fun deterministicAcrossRuns() {
        val parts = EngineTestData.parts("alpha\nbeta")

        val first = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )
        val second = ExplicitBreakLineBreakerStrategy.breakIntoLines(
            parts = parts,
            font = font,
            maxWidth = 1_000.0,
            measurer = EngineTestData.measurer,
            wordBreaker = NoOpWordBreakerStrategy,
        )

        assertEquals(first.map { line -> line.parts.map { it.part.text } }, second.map { line -> line.parts.map { it.part.text } })
    }

    /**
     * Use case: setting [ExplicitBreakLineBreakerStrategy] on the [SimpLayEngine.Builder] routes
     * measurement through it, so a block with one explicit break and ample width still becomes two
     * lines.
     */
    @Test
    fun swapViaBuilderRoutesToStrategy() {
        val document = Document(
            pages = listOf(
                FlowPage(
                    layout = EngineTestData.pageLayout(width = 1_000.0, height = 300.0),
                    blocks = listOf(TextBlock.of("first line\nsecond line", EngineTestData.style)),
                ),
            ),
        )
        val engine = SimpLayEngine.builder(EngineTestData.measurer)
            .lineBreakerStrategy(ExplicitBreakLineBreakerStrategy)
            .build()

        val measured = engine.measure(document)

        assertEquals(2, measured.pages.single().blocks.single().lines.size)
    }
}
