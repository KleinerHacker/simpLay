package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.model.LineSpacing
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * A raw [org.pcsoft.framework.simplay.engine.model.TextStyle] with its font replaced by a [org.pcsoft.framework.simplay.engine.measure.MeasuredFont] and the [resolvedLineHeight]
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
    val raw: org.pcsoft.framework.simplay.engine.model.TextStyle,
    val font: org.pcsoft.framework.simplay.engine.measure.MeasuredFont,
) {
    /** The line spacing of [raw]. */
    val lineSpacing: org.pcsoft.framework.simplay.engine.model.LineSpacing get() = raw.lineSpacing

    /** The horizontal alignment of [raw]. */
    val alignment: org.pcsoft.framework.simplay.engine.model.TextAlignment get() = raw.alignment

    /**
     * The line advance in layout units, derived from [font]'s metrics and [lineSpacing]:
     * `(ascent + descent) * lineSpacing.factor + lineSpacing.extraLeading`.
     */
    val resolvedLineHeight: Double
        get() = (font.metrics.ascent + font.metrics.descent) * lineSpacing.factor + lineSpacing.extraLeading
}
