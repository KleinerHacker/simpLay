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

package org.pcsoft.framework.simplay.fx.internal

import org.pcsoft.framework.simplay.engine.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * Module-internal facade that turns a raw [Document] into a [MeasuredDocument] using a JavaFX-backed
 * [SimpLayEngine]. The [measurer] doubles as the font resolver for the render walk, so callers that
 * also render should pass the same instance into [renderPage] / [renderDocument].
 */
internal fun measureDocument(
    document: Document,
    measurer: FxFontMeasureCalculator = FxFontMeasureCalculator(),
): MeasuredDocument =
    SimpLayEngine.builder(measurer).build().measure(document)
