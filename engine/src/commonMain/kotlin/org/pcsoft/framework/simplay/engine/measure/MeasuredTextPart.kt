package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * A raw [org.pcsoft.framework.simplay.engine.model.TextPart] placed at its measured [bounds] relative to the page content area.
 *
 * The unchanged [text] is forwarded to [raw] by hand. Not persistable.
 *
 * @property raw the wrapped raw part.
 * @property bounds position and extent of the part relative to the page content area.
 */
class MeasuredTextPart(val raw: org.pcsoft.framework.simplay.engine.model.TextPart, val bounds: org.pcsoft.framework.simplay.engine.geometry.Rect) {
    /** The text of [raw]. */
    val text: String get() = raw.text
}
