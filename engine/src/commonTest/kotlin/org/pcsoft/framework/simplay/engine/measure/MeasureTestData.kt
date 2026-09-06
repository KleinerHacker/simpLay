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
    val bodyFont = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Font(family = "Helvetica", size = 12.0)

    /** Vertical metrics that go with [bodyFont]. */
    val bodyMetrics = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.FontMetrics(
        ascent = 9.0,
        descent = 3.0,
        leading = 2.0
    )

    /** A left-aligned style over [bodyFont] with 1.5x line spacing and one extra unit of leading. */
    val bodyStyle = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextStyle(
        font = bodyFont,
        lineSpacing = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.LineSpacing(
            factor = 1.5,
            extraLeading = 1.0
        ),
        alignment = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextAlignment.LEFT,
    )

    /** The expected derived line height for [bodyStyle]: `(9 + 3) * 1.5 + 1`. */
    const val bodyResolvedLineHeight = 19.0

    /** An A-ish page frame with symmetric margins. */
    val pageLayout = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.PageLayout(
        size = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Size(width = 200.0, height = 300.0),
        margins = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Margins(
            left = 20.0,
            top = 20.0,
            right = 20.0,
            bottom = 20.0
        ),
    )

    /** Builds the [bodyFont] wrapped as a [org.pcsoft.framework.simplay.engine.measure.MeasuredFont]. */
    fun measuredBodyFont() =
        _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFont(bodyFont, bodyMetrics)

    /** Builds the [bodyStyle] wrapped as a [org.pcsoft.framework.simplay.engine.measure.MeasuredTextStyle]. */
    fun measuredBodyStyle() =
        _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextStyle(bodyStyle, measuredBodyFont())

    /** A throwaway rectangle used where the concrete value does not matter. */
    fun someRect() = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(
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
        _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                "Block",
                bodyStyle
            ),
            lines = emptyList(),
            bounds = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(
                x = 0.0,
                y = 0.0,
                width = 100.0,
                height = bottom
            ),
            style = measuredBodyStyle(),
        )
}
