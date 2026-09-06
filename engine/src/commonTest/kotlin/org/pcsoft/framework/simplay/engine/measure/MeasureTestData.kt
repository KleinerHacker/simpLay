package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.geometry.FontMetrics
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.LineSpacing
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Shared fixtures for the measured-model tests. All sample data is in English.
 */
internal object MeasureTestData {

    /** A plain body font. */
    val bodyFont = Font(family = "Helvetica", size = 12.0)

    /** Vertical metrics that go with [bodyFont]. */
    val bodyMetrics = FontMetrics(
        ascent = 9.0,
        descent = 3.0,
        leading = 2.0
    )

    /** A left-aligned style over [bodyFont] with 1.5x line spacing and one extra unit of leading. */
    val bodyStyle = TextStyle(
        font = bodyFont,
        lineSpacing = LineSpacing(
            factor = 1.5,
            extraLeading = 1.0
        ),
        alignment = TextAlignment.LEFT,
    )

    /** The expected derived line height for [bodyStyle]: `(9 + 3) * 1.5 + 1`. */
    const val bodyResolvedLineHeight = 19.0

    /** An A-ish page frame with symmetric margins. */
    val pageLayout = PageLayout(
        size = Size(width = 200.0, height = 300.0),
        margins = Margins(
            left = 20.0,
            top = 20.0,
            right = 20.0,
            bottom = 20.0
        ),
    )

    /** Builds the [bodyFont] wrapped as a [MeasuredFont]. */
    fun measuredBodyFont() =
        MeasuredFont(bodyFont, bodyMetrics)

    /** Builds the [bodyStyle] wrapped as a [MeasuredTextStyle]. */
    fun measuredBodyStyle() =
        MeasuredTextStyle(bodyStyle, measuredBodyFont())

    /** A throwaway rectangle used where the concrete value does not matter. */
    fun someRect() = Rect(
        x = 0.0,
        y = 0.0,
        width = 10.0,
        height = 10.0
    )

    /**
     * A measured block anchored at the content-area origin whose bounds reach [bottom] units down,
     * so a page holding just this block reports `requiredContentHeight == bottom`.
     */
    fun measuredBlockReaching(bottom: Double) =
        MeasuredTextBlock(
            raw = TextBlock.of(
                "Block",
                bodyStyle
            ),
            lines = emptyList(),
            bounds = Rect(
                x = 0.0,
                y = 0.0,
                width = 100.0,
                height = bottom
            ),
            style = measuredBodyStyle(),
        )
}
