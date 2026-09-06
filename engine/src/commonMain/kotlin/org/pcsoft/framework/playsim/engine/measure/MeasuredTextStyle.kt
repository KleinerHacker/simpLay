package org.pcsoft.framework.playsim.engine.measure

import org.pcsoft.framework.playsim.engine.model.LineSpacing
import org.pcsoft.framework.playsim.engine.model.TextAlignment
import org.pcsoft.framework.playsim.engine.model.TextStyle

/**
 * A raw [TextStyle] with its font replaced by a [MeasuredFont] and the [resolvedLineHeight]
 * derived.
 *
 * The unchanged properties (`lineSpacing`, `alignment`) are forwarded to [raw] by hand. The font
 * is exposed only as the measured [font]; the raw one stays reachable via `raw.font`. Not
 * persistable.
 *
 * @property raw the wrapped raw style.
 * @property font the measured font of this style.
 */
class MeasuredTextStyle(
    val raw: TextStyle,
    val font: MeasuredFont,
) {
    /** The line spacing of [raw]. */
    val lineSpacing: LineSpacing get() = raw.lineSpacing

    /** The horizontal alignment of [raw]. */
    val alignment: TextAlignment get() = raw.alignment

    /**
     * The line advance in layout units, derived from [font]'s metrics and [lineSpacing]:
     * `(ascent + descent) * lineSpacing.factor + lineSpacing.extraLeading`.
     */
    val resolvedLineHeight: Double
        get() = (font.metrics.ascent + font.metrics.descent) * lineSpacing.factor + lineSpacing.extraLeading
}
