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

package org.pcsoft.framework.simplay.fx.internal.ps

import javafx.css.CssMetaData
import javafx.css.Styleable
import javafx.css.StyleConverter
import javafx.css.StyleableProperty
import javafx.scene.control.Control
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.pcsoft.framework.simplay.fx.PaperSheetView

/**
 * The JavaFX CSS metadata of [PaperSheetView]: one [CssMetaData] entry per styleable value the
 * component draws with. Every colour value is a [Paint] (so a gradient or an image pattern works too)
 * except `-fx-caret-color`, which stays a plain [Color]. The two layout values `-fx-outer-margin` and
 * `-fx-page-gap` back the existing `outerMargin` / `pageGap` properties, so a programmatic setter
 * still wins over the user-agent stylesheet.
 *
 * [CLASS_CSS_META_DATA] is the aggregated, unmodifiable list (the [Control] metadata plus the entries
 * below) returned from `PaperSheetView.getClassCssMetaData()` and `getControlCssMetaData()`.
 */
internal object PaperSheetStyleableProperties {

    /** Default sheet fill; mirrors `PaperSheetCanvasPainter.SHEET_COLOR`. */
    val DEFAULT_SHEET_BACKGROUND: Paint = Color.WHITE

    /** Default sheet border colour; mirrors `PaperSheetCanvasPainter.BORDER_COLOR`. */
    val DEFAULT_SHEET_BORDER_COLOR: Paint = Color.gray(0.55)

    /** Default sheet border width in layout units; mirrors `PaperSheetCanvasPainter.BORDER_WIDTH`. */
    const val DEFAULT_SHEET_BORDER_WIDTH: Double = 1.0

    /** Default drop-shadow colour; mirrors `PaperSheetCanvasPainter.SHADOW_COLOR`. */
    val DEFAULT_SHADOW_COLOR: Paint = Color.rgb(0, 0, 0, 0.25)

    /** Default drop-shadow offset in layout units; mirrors `PaperSheetCanvasPainter.SHADOW_OFFSET`. */
    const val DEFAULT_SHADOW_OFFSET: Double = 4.0

    /** Default selection highlight colour; mirrors `PaperSheetCanvasPainter.SELECTION_COLOR`. */
    val DEFAULT_SELECTION_COLOR: Paint = Color.rgb(66, 133, 244, 0.35)

    /** Default caret colour; mirrors `PaperSheetCanvasPainter.CARET_COLOR`. */
    val DEFAULT_CARET_COLOR: Color = Color.rgb(20, 20, 20)

    val SHEET_BACKGROUND: CssMetaData<PaperSheetView, Paint> =
        object : CssMetaData<PaperSheetView, Paint>(
            "-fx-sheet-background", StyleConverter.getPaintConverter(), DEFAULT_SHEET_BACKGROUND,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.sheetBackgroundProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Paint> =
                styleable.sheetBackgroundProperty
        }

    val SHEET_BORDER_COLOR: CssMetaData<PaperSheetView, Paint> =
        object : CssMetaData<PaperSheetView, Paint>(
            "-fx-sheet-border-color", StyleConverter.getPaintConverter(), DEFAULT_SHEET_BORDER_COLOR,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.sheetBorderColorProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Paint> =
                styleable.sheetBorderColorProperty
        }

    val SHEET_BORDER_WIDTH: CssMetaData<PaperSheetView, Number> =
        object : CssMetaData<PaperSheetView, Number>(
            "-fx-sheet-border-width", StyleConverter.getSizeConverter(), DEFAULT_SHEET_BORDER_WIDTH,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.sheetBorderWidthProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Number> =
                styleable.sheetBorderWidthProperty
        }

    val SHADOW_COLOR: CssMetaData<PaperSheetView, Paint> =
        object : CssMetaData<PaperSheetView, Paint>(
            "-fx-shadow-color", StyleConverter.getPaintConverter(), DEFAULT_SHADOW_COLOR,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.shadowColorProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Paint> =
                styleable.shadowColorProperty
        }

    val SHADOW_OFFSET: CssMetaData<PaperSheetView, Number> =
        object : CssMetaData<PaperSheetView, Number>(
            "-fx-shadow-offset", StyleConverter.getSizeConverter(), DEFAULT_SHADOW_OFFSET,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.shadowOffsetProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Number> =
                styleable.shadowOffsetProperty
        }

    val SELECTION_COLOR: CssMetaData<PaperSheetView, Paint> =
        object : CssMetaData<PaperSheetView, Paint>(
            "-fx-selection-color", StyleConverter.getPaintConverter(), DEFAULT_SELECTION_COLOR,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.selectionColorProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Paint> =
                styleable.selectionColorProperty
        }

    val CARET_COLOR: CssMetaData<PaperSheetView, Color> =
        object : CssMetaData<PaperSheetView, Color>(
            "-fx-caret-color", StyleConverter.getColorConverter(), DEFAULT_CARET_COLOR,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.caretColorProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Color> =
                styleable.caretColorProperty
        }

    val OUTER_MARGIN: CssMetaData<PaperSheetView, Number> =
        object : CssMetaData<PaperSheetView, Number>(
            "-fx-outer-margin", StyleConverter.getSizeConverter(), PaperSheetView.DEFAULT_OUTER_MARGIN,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.outerMarginProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Number> =
                styleable.outerMarginProperty
        }

    val PAGE_GAP: CssMetaData<PaperSheetView, Number> =
        object : CssMetaData<PaperSheetView, Number>(
            "-fx-page-gap", StyleConverter.getSizeConverter(), PaperSheetView.DEFAULT_PAGE_GAP,
        ) {
            override fun isSettable(styleable: PaperSheetView) = !styleable.pageGapProperty.isBound
            override fun getStyleableProperty(styleable: PaperSheetView): StyleableProperty<Number> =
                styleable.pageGapProperty
        }

    /** The [Control] metadata plus every entry above, unmodifiable. */
    val CLASS_CSS_META_DATA: List<CssMetaData<out Styleable, *>> = buildList<CssMetaData<out Styleable, *>> {
        addAll(Control.getClassCssMetaData())
        add(SHEET_BACKGROUND)
        add(SHEET_BORDER_COLOR)
        add(SHEET_BORDER_WIDTH)
        add(SHADOW_COLOR)
        add(SHADOW_OFFSET)
        add(SELECTION_COLOR)
        add(CARET_COLOR)
        add(OUTER_MARGIN)
        add(PAGE_GAP)
    }.let(java.util.Collections::unmodifiableList)
}
