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

import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * Builds a [SimpLayEngine] from this [RenderConfiguration] and the given [measurer], applying the
 * configured [lineBreakerStrategy] and [wordBreakerStrategy].
 *
 * Renderers share this so the mapping from a configuration to an engine stays in one place; the
 * platform-specific [FontMeasureCalculator] is the only thing a caller still has to supply.
 *
 * @param measurer the font-measuring callback for the target platform.
 * @return an engine fixed to the strategies of this configuration.
 */
fun RenderConfiguration.createEngine(measurer: FontMeasureCalculator): SimpLayEngine =
    SimpLayEngine.builder(measurer)
        .lineBreakerStrategy(lineBreakerStrategy)
        .wordBreakerStrategy(wordBreakerStrategy)
        .build()

/**
 * Measures this [Document] with the given [measurer] and [config] in one call.
 *
 * Short form of `config.createEngine(measurer).measure(this)`; the result is a [MeasuredDocument]
 * whose line and word breaking follow [config].
 *
 * @param measurer the font-measuring callback for the target platform.
 * @param config the render configuration; an unconfigured [RenderConfiguration] reproduces the
 *   engine defaults.
 * @return the measured document.
 */
fun Document.measure(
    measurer: FontMeasureCalculator,
    config: RenderConfiguration = RenderConfiguration(),
): MeasuredDocument =
    config.createEngine(measurer).measure(this)
