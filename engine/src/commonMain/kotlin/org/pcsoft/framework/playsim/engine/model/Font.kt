package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * Internal structural contract for [Font].
 *
 * It exists only so the measured decorator model (`...engine.measure`) can wrap a raw font with
 * Kotlin's `by` delegation. It is not part of the public API and carries no serialization.
 */
@PublishedApi
internal interface IFont {
    val family: String
    val size: Double
    val weight: FontWeight
    val style: FontStyle
}

/**
 * A font face. A plain data holder; the size is a unit-less double.
 */
@Serializable
data class Font(
    override val family: String,
    override val size: Double,
    override val weight: FontWeight = FontWeight.NORMAL,
    override val style: FontStyle = FontStyle.NORMAL,
) : IFont
