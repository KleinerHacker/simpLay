package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * Multiplicative and additive line spacing. All values are unit-less doubles.
 */
@Serializable
data class LineSpacing(val factor: Double = 1.0, val extraLeading: Double = 0.0)

/**
 * The visual style of a text block. A plain data holder.
 */
@Serializable
data class TextStyle(
    val font: Font,
    val lineSpacing: LineSpacing = LineSpacing(),
    val alignment: TextAlignment = TextAlignment.LEFT,
)
