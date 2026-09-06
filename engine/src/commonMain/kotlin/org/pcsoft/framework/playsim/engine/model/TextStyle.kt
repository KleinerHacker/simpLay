package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * Multiplicative and additive line spacing. All values are unit-less doubles.
 */
@Serializable
data class LineSpacing(val factor: Double = 1.0, val extraLeading: Double = 0.0)

/**
 * Internal structural contract for [TextStyle].
 *
 * It exists only so the measured decorator model (`...engine.measure`) can wrap a raw text style
 * with Kotlin's `by` delegation. It is not part of the public API and carries no serialization.
 */
@PublishedApi
internal interface ITextStyle {
    val font: Font
    val lineSpacing: LineSpacing
    val alignment: TextAlignment
}

/**
 * The visual style of a text block. A plain data holder.
 */
@Serializable
data class TextStyle(
    override val font: Font,
    override val lineSpacing: LineSpacing = LineSpacing(),
    override val alignment: TextAlignment = TextAlignment.LEFT,
) : ITextStyle
