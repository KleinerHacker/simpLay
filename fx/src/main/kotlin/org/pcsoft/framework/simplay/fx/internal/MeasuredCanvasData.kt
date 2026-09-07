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

import org.pcsoft.framework.simplay.engine.engine.documentSize
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument

/**
 * The surface-free layout result for drawing a [MeasuredDocument] onto a single canvas. Computed
 * once from the measured tree and the two geometry values, then consumed both by the renderer and
 * by the tests, so no [javafx.scene.canvas.GraphicsContext] has to be mocked.
 *
 * All values are already scaled by `unitScale`.
 *
 * @property canvasSize the total canvas size needed for the whole document.
 * @property documentWidth the scaled width of the widest page; the length of every page-break line.
 * @property pageOriginsY the scaled top `y` of each page, in page order.
 * @property separatorsY the scaled `y` of each dashed page-break line (one per page boundary, so
 *   `pageOriginsY.size - 1` entries for a non-empty document, empty otherwise).
 */
internal class MeasuredCanvasData(
    val canvasSize: Size,
    val documentWidth: Double,
    val pageOriginsY: List<Double>,
    val separatorsY: List<Double>,
)

/**
 * Builds the [MeasuredCanvasData] for [measured] with the given [unitScale] and [pageGap] (both in the
 * same units as the measured geometry). An empty document yields a `0 x 0` canvas and no origins or
 * separators.
 */
internal fun measureForCanvas(
    measured: MeasuredDocument,
    unitScale: Double,
    pageGap: Double,
): MeasuredCanvasData {
    if (measured.pages.isEmpty()) {
        return MeasuredCanvasData(Size(0.0, 0.0), 0.0, emptyList(), emptyList())
    }

    val totalSize = measured.documentSize(pageGap)
    val origins = ArrayList<Double>(measured.pages.size)
    val separators = ArrayList<Double>(measured.pages.size - 1)

    var y = 0.0
    measured.pages.forEachIndexed { index, page ->
        origins += y * unitScale
        y += page.effectiveSize.height
        if (index != measured.pages.lastIndex) {
            separators += (y + pageGap / 2.0) * unitScale
            y += pageGap
        }
    }

    return MeasuredCanvasData(
        canvasSize = Size(totalSize.width * unitScale, totalSize.height * unitScale),
        documentWidth = totalSize.width * unitScale,
        pageOriginsY = origins,
        separatorsY = separators,
    )
}
