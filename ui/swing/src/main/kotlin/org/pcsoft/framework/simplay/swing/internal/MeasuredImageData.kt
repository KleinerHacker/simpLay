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

package org.pcsoft.framework.simplay.swing.internal

import org.pcsoft.framework.simplay.engine.documentSize
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument

/**
 * The surface-free layout result for drawing a [MeasuredDocument] onto a single image. Computed once
 * from the measured tree and the two geometry values, then consumed both by [DocumentImageRenderer]
 * and by the tests, so no [java.awt.Graphics2D] has to be mocked. The Swing counterpart of the `fx`
 * module's `MeasuredCanvasData`.
 *
 * All values are already scaled by `unitScale`.
 *
 * @property imageSize the total image size needed for the whole document.
 * @property documentWidth the scaled width of the widest page; the length of every page-break line.
 * @property pageOriginsY the scaled top `y` of each page, in page order.
 * @property separatorsY the scaled `y` of each dashed page-break line (`pageOriginsY.size - 1`
 *   entries for a non-empty document, empty otherwise).
 */
internal class MeasuredImageData(
    val imageSize: Size,
    val documentWidth: Double,
    val pageOriginsY: List<Double>,
    val separatorsY: List<Double>,
)

/**
 * Builds the [MeasuredImageData] for [measured] with the given [unitScale] and [pageGap] (both in the
 * same units as the measured geometry). An empty document yields a `0 x 0` image and no origins or
 * separators.
 */
internal fun measureForImage(
    measured: MeasuredDocument,
    unitScale: Double,
    pageGap: Double,
): MeasuredImageData {
    if (measured.pages.isEmpty()) {
        return MeasuredImageData(Size(0.0, 0.0), 0.0, emptyList(), emptyList())
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

    return MeasuredImageData(
        imageSize = Size(totalSize.width * unitScale, totalSize.height * unitScale),
        documentWidth = totalSize.width * unitScale,
        pageOriginsY = origins,
        separatorsY = separators,
    )
}
