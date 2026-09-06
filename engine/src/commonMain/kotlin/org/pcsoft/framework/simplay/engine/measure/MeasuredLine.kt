package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.TextAlignment

/**
 * One laid-out line of text. An intermediate level that has no raw counterpart.
 *
 * @property parts the measured parts of this line, in reading order.
 * @property lineBox position and extent of the line relative to the page content area.
 * @property baseline distance from the top of [lineBox] to the text baseline, in layout units.
 * @property ascent ascent used for this line, in layout units.
 * @property descent descent used for this line, in layout units.
 * @property alignment horizontal alignment applied to this line.
 * @property lastLine `true` for the final line of its block (relevant for justified alignment).
 */
class MeasuredLine(
    val parts: List<MeasuredTextPart>,
    val lineBox: Rect,
    val baseline: Double,
    val ascent: Double,
    val descent: Double,
    val alignment: TextAlignment,
    val lastLine: Boolean,
)
