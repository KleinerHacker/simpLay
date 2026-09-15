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

package org.pcsoft.framework.simplay.swing

import org.pcsoft.framework.simplay.engine.RenderConfiguration

/**
 * Mutable configuration for [DocumentImageRenderer]. Extends the shared, renderer-agnostic
 * [RenderConfiguration] (line- and word-breaking) with the two values the image renderer adds on top.
 * The Swing counterpart of the `fx` module's `CanvasRenderConfiguration`.
 *
 * Filled through the builder lambda passed to [DocumentImageRenderer.of], for example
 * `DocumentImageRenderer.of(document) { unitScale = 2.0; pageGap = 16.0 }`.
 *
 * @property unitScale factor applied to every layout coordinate and to the resulting image size;
 *   must be greater than `0.0`. `1.0` renders one layout unit per pixel.
 * @property pageGap vertical space between two consecutive pages, in layout units (before scaling);
 *   must not be negative. The dashed page-break line is drawn in the middle of this band.
 */
class ImageRenderConfiguration : RenderConfiguration() {

    var unitScale: Double = 1.0

    var pageGap: Double = 24.0
}
