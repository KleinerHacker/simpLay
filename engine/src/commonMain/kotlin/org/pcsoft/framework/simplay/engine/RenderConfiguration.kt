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

/**
 * Mutable, renderer-agnostic configuration that every concrete renderer needs before it can measure
 * a [org.pcsoft.framework.simplay.engine.model.Document]: the [lineBreakerStrategy] and the
 * [wordBreakerStrategy] the measure step must use.
 *
 * The class is `open` on purpose so a concrete renderer can derive its own configuration and add
 * renderer-specific values (unit scale, page gap, colours, ...) while keeping the shared measure
 * settings in one place. It is meant to be filled through a Kotlin builder lambda, for example
 * `Configuration().apply { lineBreakerStrategy = ... }`.
 *
 * Both properties default to the same values the [SimpLayEngine] builder uses, so an unconfigured
 * instance measures exactly like `SimpLayEngine.builder(measurer).build()`.
 *
 * @property lineBreakerStrategy the strategy that turns block parts into lines; defaults to
 *   [GreedyWordLineBreakerStrategy].
 * @property wordBreakerStrategy the intra-word (hyphenation) seam; defaults to
 *   [NoOpWordBreakerStrategy].
 */
open class RenderConfiguration {

    var lineBreakerStrategy: LineBreakerStrategy = GreedyWordLineBreakerStrategy

    var wordBreakerStrategy: WordBreakerStrategy = NoOpWordBreakerStrategy
}
