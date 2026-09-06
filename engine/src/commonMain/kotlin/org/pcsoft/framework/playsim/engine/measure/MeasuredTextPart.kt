package org.pcsoft.framework.playsim.engine.measure

import org.pcsoft.framework.playsim.engine.geometry.Rect
import org.pcsoft.framework.playsim.engine.model.TextPart

/**
 * A raw [TextPart] placed at its measured [bounds] relative to the page content area.
 *
 * The unchanged [text] is forwarded to [raw] by hand. Not persistable.
 *
 * @property raw the wrapped raw part.
 * @property bounds position and extent of the part relative to the page content area.
 */
class MeasuredTextPart(val raw: TextPart, val bounds: Rect) {
    /** The text of [raw]. */
    val text: String get() = raw.text
}
