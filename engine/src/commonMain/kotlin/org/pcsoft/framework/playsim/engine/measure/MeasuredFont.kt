package org.pcsoft.framework.playsim.engine.measure

import org.pcsoft.framework.playsim.engine.geometry.FontMetrics
import org.pcsoft.framework.playsim.engine.model.Font
import org.pcsoft.framework.playsim.engine.model.FontStyle
import org.pcsoft.framework.playsim.engine.model.FontWeight

/**
 * A raw [Font] enriched with the vertical [metrics] resolved for it.
 *
 * The unchanged font properties are forwarded to [raw] by hand; nothing is stored twice. This type
 * is not persistable.
 *
 * @property raw the wrapped raw font.
 * @property metrics the vertical metrics resolved for [raw], in layout units.
 */
class MeasuredFont(val raw: Font, val metrics: FontMetrics) {
    /** The family of [raw]. */
    val family: String get() = raw.family

    /** The size of [raw], a unit-less double. */
    val size: Double get() = raw.size

    /** The weight of [raw]. */
    val weight: FontWeight get() = raw.weight

    /** The slant of [raw]. */
    val style: FontStyle get() = raw.style
}
