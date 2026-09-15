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

import java.awt.Color
import java.awt.Paint
import javax.swing.UIManager
import org.pcsoft.framework.simplay.swing.internal.ps.PaperSheetStyle

/**
 * The Look-and-Feel integration of [PaperSheetView], the Swing replacement for the `fx` module's
 * default user-agent stylesheet. It seeds the `PaperSheetView.*` keys of the active Look-and-Feel
 * with the built-in defaults and copies them onto a fresh view unless the caller already set a value
 * programmatically.
 *
 * Every key maps to one visual value the sheet chrome, the drop shadow, the selection highlight, the
 * caret or the layout is drawn with. A Look-and-Feel (or the caller, before creating the view) may
 * override any of them with `UIManager.put("PaperSheetView.sheetBackground", ...)`; a programmatic
 * setter on the view still wins.
 */
object PaperSheetLookAndFeel {

    const val KEY_SHEET_BACKGROUND = "PaperSheetView.sheetBackground"
    const val KEY_SHEET_BORDER_COLOR = "PaperSheetView.sheetBorderColor"
    const val KEY_SHEET_BORDER_WIDTH = "PaperSheetView.sheetBorderWidth"
    const val KEY_SHADOW_COLOR = "PaperSheetView.shadowColor"
    const val KEY_SHADOW_OFFSET = "PaperSheetView.shadowOffset"
    const val KEY_SELECTION_COLOR = "PaperSheetView.selectionColor"
    const val KEY_SELECTION_COLOR_NON_EDITABLE = "PaperSheetView.selectionColorNonEditable"
    const val KEY_CARET_COLOR = "PaperSheetView.caretColor"
    const val KEY_DEACTIVATED_SHEET_BACKGROUND = "PaperSheetView.deactivatedSheetBackground"
    const val KEY_DEACTIVATED_OVERLAY_COLOR = "PaperSheetView.deactivatedOverlayColor"
    const val KEY_OUTER_MARGIN = "PaperSheetView.outerMargin"
    const val KEY_PAGE_GAP = "PaperSheetView.pageGap"

    /** Seeds any missing `PaperSheetView.*` key in the active Look-and-Feel with its built-in default. */
    fun installDefaults() {
        val d = UIManager.getDefaults()
        d.putIfAbsent(KEY_SHEET_BACKGROUND, PaperSheetStyle.DEFAULT_SHEET_BACKGROUND)
        d.putIfAbsent(KEY_SHEET_BORDER_COLOR, PaperSheetStyle.DEFAULT_SHEET_BORDER_COLOR)
        d.putIfAbsent(KEY_SHEET_BORDER_WIDTH, PaperSheetStyle.DEFAULT_SHEET_BORDER_WIDTH)
        d.putIfAbsent(KEY_SHADOW_COLOR, PaperSheetStyle.DEFAULT_SHADOW_COLOR)
        d.putIfAbsent(KEY_SHADOW_OFFSET, PaperSheetStyle.DEFAULT_SHADOW_OFFSET)
        d.putIfAbsent(KEY_SELECTION_COLOR, PaperSheetStyle.DEFAULT_SELECTION_COLOR)
        d.putIfAbsent(KEY_SELECTION_COLOR_NON_EDITABLE, PaperSheetStyle.DEFAULT_SELECTION_COLOR_NON_EDITABLE)
        d.putIfAbsent(KEY_CARET_COLOR, PaperSheetStyle.DEFAULT_CARET_COLOR)
        d.putIfAbsent(KEY_DEACTIVATED_SHEET_BACKGROUND, PaperSheetStyle.DEFAULT_DEACTIVATED_SHEET_BACKGROUND)
        d.putIfAbsent(KEY_DEACTIVATED_OVERLAY_COLOR, PaperSheetStyle.DEFAULT_DEACTIVATED_OVERLAY_COLOR)
        d.putIfAbsent(KEY_OUTER_MARGIN, PaperSheetStyle.DEFAULT_OUTER_MARGIN)
        d.putIfAbsent(KEY_PAGE_GAP, PaperSheetStyle.DEFAULT_PAGE_GAP)
    }

    /** Copies every `PaperSheetView.*` value the caller did not set programmatically onto [view]. */
    fun applyTo(view: PaperSheetView) {
        installDefaults()
        view.applyLafDefaults {
            fun unset(prop: String) = prop !in view.styleSetByUser
            if (unset(PaperSheetView.PROP_SHEET_BACKGROUND)) paint(KEY_SHEET_BACKGROUND)?.let { view.sheetBackground = it }
            if (unset(PaperSheetView.PROP_SHEET_BORDER_COLOR)) paint(KEY_SHEET_BORDER_COLOR)?.let { view.sheetBorderColor = it }
            if (unset(PaperSheetView.PROP_SHEET_BORDER_WIDTH)) number(KEY_SHEET_BORDER_WIDTH)?.let { view.sheetBorderWidth = it }
            if (unset(PaperSheetView.PROP_SHADOW_COLOR)) paint(KEY_SHADOW_COLOR)?.let { view.shadowColor = it }
            if (unset(PaperSheetView.PROP_SHADOW_OFFSET)) number(KEY_SHADOW_OFFSET)?.let { view.shadowOffset = it }
            if (unset(PaperSheetView.PROP_SELECTION_COLOR)) paint(KEY_SELECTION_COLOR)?.let { view.selectionColor = it }
            if (unset(PaperSheetView.PROP_CARET_COLOR)) color(KEY_CARET_COLOR)?.let { view.caretColor = it }
            if (unset(PaperSheetView.PROP_DEACTIVATED_SHEET_BACKGROUND)) {
                paint(KEY_DEACTIVATED_SHEET_BACKGROUND)?.let { view.deactivatedSheetBackground = it }
            }
            if (unset(PaperSheetView.PROP_DEACTIVATED_OVERLAY_COLOR)) {
                paint(KEY_DEACTIVATED_OVERLAY_COLOR)?.let { view.deactivatedOverlayColor = it }
            }
            if (unset(PaperSheetView.PROP_OUTER_MARGIN)) number(KEY_OUTER_MARGIN)?.let { view.outerMargin = it }
            if (unset(PaperSheetView.PROP_PAGE_GAP)) number(KEY_PAGE_GAP)?.let { view.pageGap = it }
        }
    }

    /** The `PaperSheetView.selectionColorNonEditable` value, or the built-in default. */
    fun nonEditableSelectionColor(): Paint =
        paint(KEY_SELECTION_COLOR_NON_EDITABLE) ?: PaperSheetStyle.DEFAULT_SELECTION_COLOR_NON_EDITABLE

    private fun paint(key: String): Paint? = UIManager.get(key) as? Paint

    private fun color(key: String): Color? = UIManager.get(key) as? Color

    private fun number(key: String): Double? = (UIManager.get(key) as? Number)?.toDouble()
}
