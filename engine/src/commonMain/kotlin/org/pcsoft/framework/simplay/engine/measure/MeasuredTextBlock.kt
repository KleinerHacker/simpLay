package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * A raw [TextBlock] broken into laid-out [lines] and placed at its [bounds].
 *
 * `TextBlock` has no unchanged property to forward (`parts` and `style` are both replaced by the
 * measured model): the raw parts are reachable via `raw.parts`, the laid-out parts via [lines].
 * Not persistable.
 *
 * @property raw the wrapped raw block.
 * @property lines the lines this block was broken into, in order.
 * @property bounds position and extent of the whole block relative to the page content area.
 * @property style the measured style of this block.
 */
class MeasuredTextBlock(
    val raw: TextBlock,
    val lines: List<MeasuredLine>,
    val bounds: Rect,
    val style: MeasuredTextStyle,
)
