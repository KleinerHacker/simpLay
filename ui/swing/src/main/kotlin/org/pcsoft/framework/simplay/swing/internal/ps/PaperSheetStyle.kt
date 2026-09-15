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

package org.pcsoft.framework.simplay.swing.internal.ps

import java.awt.Color
import java.awt.Paint

/**
 * The visual values a [PaperSheetSwingPainter] pass draws the sheet chrome, the selection highlight
 * and the caret with. The Swing counterpart of the `fx` module's `PaperSheetStyle`; every field
 * defaults to the built-in look, `BasicPaperSheetUI` fills it from the view's style properties so a
 * Look-and-Feel or a programmatic setter can override it.
 *
 * @property sheetBackground fill of every sheet.
 * @property sheetBorderColor stroke of every sheet border.
 * @property sheetBorderWidth stroke width of every sheet border.
 * @property shadowColor fill of the drop shadow.
 * @property shadowOffset offset of the drop shadow to the lower right.
 * @property selectionColor fill of the text selection highlight.
 * @property caretColor stroke of the edit caret.
 * @property deactivatedSheetBackground fill of a `DISABLED` sheet, instead of [sheetBackground].
 * @property deactivatedOverlayColor colour of the diagonal hatch drawn over a `DISABLED` sheet.
 */
class PaperSheetStyle(
    val sheetBackground: Paint = DEFAULT_SHEET_BACKGROUND,
    val sheetBorderColor: Paint = DEFAULT_SHEET_BORDER_COLOR,
    val sheetBorderWidth: Double = DEFAULT_SHEET_BORDER_WIDTH,
    val shadowColor: Paint = DEFAULT_SHADOW_COLOR,
    val shadowOffset: Double = DEFAULT_SHADOW_OFFSET,
    val selectionColor: Paint = DEFAULT_SELECTION_COLOR,
    val caretColor: Color = DEFAULT_CARET_COLOR,
    val deactivatedSheetBackground: Paint = DEFAULT_DEACTIVATED_SHEET_BACKGROUND,
    val deactivatedOverlayColor: Paint = DEFAULT_DEACTIVATED_OVERLAY_COLOR,
) {

    companion object {

        val DEFAULT_SHEET_BACKGROUND: Paint = Color.WHITE
        val DEFAULT_SHEET_BORDER_COLOR: Paint = Color(140, 140, 140)
        const val DEFAULT_SHEET_BORDER_WIDTH: Double = 1.0
        val DEFAULT_SHADOW_COLOR: Paint = Color(0, 0, 0, 64)
        const val DEFAULT_SHADOW_OFFSET: Double = 4.0
        val DEFAULT_SELECTION_COLOR: Paint = Color(66, 133, 244, 89)
        val DEFAULT_SELECTION_COLOR_NON_EDITABLE: Paint = Color(120, 120, 120, 77)
        val DEFAULT_CARET_COLOR: Color = Color(20, 20, 20)
        val DEFAULT_DEACTIVATED_SHEET_BACKGROUND: Paint = Color(237, 237, 237)
        val DEFAULT_DEACTIVATED_OVERLAY_COLOR: Paint = Color(0, 0, 0, 31)

        const val DEFAULT_OUTER_MARGIN: Double = 24.0
        const val DEFAULT_PAGE_GAP: Double = 16.0
        const val CARET_WIDTH: Float = 1.5f
    }
}
