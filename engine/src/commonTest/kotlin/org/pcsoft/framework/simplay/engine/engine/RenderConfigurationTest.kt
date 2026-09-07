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

package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Tests for [RenderConfiguration] and the [createEngine] / [measure] extensions.
 */
class RenderConfigurationTest {

    private val document = Document(
        pages = listOf(
            FlowPage(
                layout = EngineTestData.pageLayout(width = 10.0, height = 300.0),
                blocks = listOf(TextBlock.of("aa bb cc", EngineTestData.style)),
            ),
        ),
    )

    private fun lineCount(measured: org.pcsoft.framework.simplay.engine.measure.MeasuredDocument): Int =
        measured.pages.sumOf { page -> page.blocks.sumOf { it.lines.size } }

    /**
     * Use case: a freshly created [RenderConfiguration] carries the same strategy defaults as the
     * [SimpLayEngine] builder.
     */
    @Test
    fun defaultsMatchEngineDefaults() {
        val config = RenderConfiguration()

        assertSame(GreedyWordLineBreakerStrategy, config.lineBreakerStrategy)
        assertSame(NoOpWordBreakerStrategy, config.wordBreakerStrategy)
    }

    /**
     * Use case: the configuration is mutable and can be filled through a builder lambda; the set
     * strategy is kept.
     */
    @Test
    fun builderLambdaOverridesStrategy() {
        val config = RenderConfiguration().apply { lineBreakerStrategy = NoWrapLineBreakerStrategy }

        assertSame(NoWrapLineBreakerStrategy, config.lineBreakerStrategy)
    }

    /**
     * Use case: with the default configuration `Document.measure` breaks greedily, so three words
     * that each exceed the width become three lines.
     */
    @Test
    fun measureWithDefaultConfigBreaksGreedily() {
        val measured = document.measure(EngineTestData.measurer)

        assertEquals(3, lineCount(measured))
    }

    /**
     * Use case: `Document.measure` forwards the configured [NoWrapLineBreakerStrategy] to the
     * engine, so the same document measures to a single line.
     */
    @Test
    fun measurePassesConfiguredStrategyThrough() {
        val config = RenderConfiguration().apply { lineBreakerStrategy = NoWrapLineBreakerStrategy }

        val measured = document.measure(EngineTestData.measurer, config)

        assertEquals(1, lineCount(measured))
    }

    /**
     * Use case: [createEngine] produces an engine that is fixed to the configured strategy.
     */
    @Test
    fun createEngineUsesConfiguredStrategy() {
        val config = RenderConfiguration().apply { lineBreakerStrategy = NoWrapLineBreakerStrategy }

        val engine = config.createEngine(EngineTestData.measurer)

        assertEquals(1, lineCount(engine.measure(document)))
    }
}
