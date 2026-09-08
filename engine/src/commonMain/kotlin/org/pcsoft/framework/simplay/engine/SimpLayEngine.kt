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

import org.pcsoft.framework.simplay.engine.internal.SimpLayPageEngine
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * Converts a raw [Document] into a [MeasuredDocument] using a font-measuring callback.
 *
 * An engine is created through [builder]; the [FontMeasureCalculator] is passed there, the
 * [LineBreakerStrategy] and the [WordBreakerStrategy] are optional and default to
 * [GreedyWordLineBreakerStrategy] and [NoOpWordBreakerStrategy]. Both strategies are fixed once the
 * engine is built. [measure] is deterministic and calls no platform API of its own.
 */
class SimpLayEngine private constructor(
    private val measurer: FontMeasureCalculator,
    private val lineBreakerStrategy: LineBreakerStrategy,
    private val wordBreakerStrategy: WordBreakerStrategy,
) {

    /**
     * Measures [document] and returns the measured result. Font resolution, line breaking, block
     * measuring and pagination run in sequence; the same input always produces the same output.
     */
    fun measure(document: Document): MeasuredDocument =
        SimpLayPageEngine.builder(measurer)
            .lineBreakerStrategy(lineBreakerStrategy)
            .wordBreakerStrategy(wordBreakerStrategy)
            .build()
            .measure(document)

    /**
     * Collects the parts of a [SimpLayEngine] and creates it. Obtained from [SimpLayEngine.builder];
     * the mandatory [FontMeasureCalculator] is passed there.
     */
    class Builder internal constructor(private val measurer: FontMeasureCalculator) {

        private var lineBreaker: LineBreakerStrategy = GreedyWordLineBreakerStrategy
        private var wordBreaker: WordBreakerStrategy = NoOpWordBreakerStrategy

        /** Sets the [LineBreakerStrategy]; defaults to [GreedyWordLineBreakerStrategy]. */
        fun lineBreakerStrategy(strategy: LineBreakerStrategy): Builder = apply {
            lineBreaker = strategy
        }

        /** Sets the [WordBreakerStrategy]; defaults to [NoOpWordBreakerStrategy]. */
        fun wordBreakerStrategy(strategy: WordBreakerStrategy): Builder = apply {
            wordBreaker = strategy
        }

        /** Builds the engine with the collected strategies. */
        fun build(): SimpLayEngine = SimpLayEngine(measurer, lineBreaker, wordBreaker)
    }

    companion object {

        /** Starts a [Builder] with the mandatory [measurer]. */
        fun builder(measurer: FontMeasureCalculator): Builder = Builder(measurer)
    }
}
