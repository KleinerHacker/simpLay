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
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition
import org.pcsoft.framework.simplay.engine.model.TextAlignment

/**
 * A page number ready to be drawn: the [pageIndex] it belongs to, the rendered [text] and its
 * anchor ([x], [y]) plus horizontal [alignment], all in page-local coordinates. Renderers only draw
 * it; all layout decisions were already made by [MeasuredDocument.planPageNumbers].
 */
data class PageNumberLabel(
    val pageIndex: Int,
    val text: String,
    val x: Double,
    val y: Double,
    val alignment: TextAlignment,
)

/**
 * Turns this measured document plus [numbering] into the [PageNumberLabel]s to draw, one per sheet
 * that carries a visible number.
 *
 * A pure function: it never touches a platform API and never mutates its receiver or arguments.
 *
 * @param numbering the numbering configuration to apply.
 * @return one entry per sheet of this document, `null` where the sheet shows no number; an empty
 *   list when [PageNumbering.position] is [PageNumberPosition.OFF].
 */
fun MeasuredDocument.planPageNumbers(numbering: PageNumbering): List<PageNumberLabel?> {
    if (numbering.position == PageNumberPosition.OFF) return emptyList()

    val excludedSheetFlags = pages.map { it.raw.id in numbering.excludedPageIds }
    val numbers = numbering.counting.numbers(pages.size, excludedSheetFlags, numbering.startNumber)

    return pages.mapIndexed { index, page ->
        val number = numbers[index] ?: return@mapIndexed null
        pageNumberLabelFor(page, index, number, numbering.position)
    }
}

private fun pageNumberLabelFor(page: MeasuredPage, index: Int, number: Int, position: PageNumberPosition): PageNumberLabel {
    val sheetNumber = index + 1
    val isOddSheet = sheetNumber % 2 != 0
    val alignment = pageNumberAlignment(position, isOddSheet)
    val atTop = isTopPageNumberPosition(position)

    // effectiveSize, not the raw layout size: a MeasuredSinglePage grows taller than its layout
    // when its content overflows, and a bottom-anchored number must follow that grown edge.
    val width = page.effectiveSize.width
    val height = page.effectiveSize.height
    val margins = page.layout.margins

    val x = when (alignment) {
        TextAlignment.LEFT -> margins.left
        TextAlignment.CENTER, TextAlignment.JUSTIFY -> width / 2.0
        TextAlignment.RIGHT -> width - margins.right
    }
    val y = if (atTop) margins.top / 2.0 else height - margins.bottom / 2.0

    return PageNumberLabel(index, number.toString(), x, y, alignment)
}

private fun isTopPageNumberPosition(position: PageNumberPosition): Boolean = when (position) {
    PageNumberPosition.TOP_LEFT, PageNumberPosition.TOP_CENTER, PageNumberPosition.TOP_RIGHT,
    PageNumberPosition.TOP_INNER, PageNumberPosition.TOP_OUTER -> true

    else -> false
}

/**
 * `INNER` / `OUTER` alternate by sheet parity: an odd sheet is a right-hand page whose spine sits on
 * its left (inner = left, outer = right); an even sheet is a left-hand page whose spine sits on its
 * right (inner = right, outer = left).
 */
private fun pageNumberAlignment(position: PageNumberPosition, isOddSheet: Boolean): TextAlignment =
    when (position) {
        PageNumberPosition.TOP_LEFT, PageNumberPosition.BOTTOM_LEFT -> TextAlignment.LEFT
        PageNumberPosition.TOP_CENTER, PageNumberPosition.BOTTOM_CENTER -> TextAlignment.CENTER
        PageNumberPosition.TOP_RIGHT, PageNumberPosition.BOTTOM_RIGHT -> TextAlignment.RIGHT
        PageNumberPosition.TOP_INNER, PageNumberPosition.BOTTOM_INNER ->
            if (isOddSheet) TextAlignment.LEFT else TextAlignment.RIGHT

        PageNumberPosition.TOP_OUTER, PageNumberPosition.BOTTOM_OUTER ->
            if (isOddSheet) TextAlignment.RIGHT else TextAlignment.LEFT

        PageNumberPosition.OFF -> TextAlignment.CENTER
    }
