package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable

/**
 * A font face. A plain data holder; the size is a unit-less double.
 */
@Serializable
data class Font(
    val family: String,
    val size: Double,
    val weight: FontWeight = FontWeight.NORMAL,
    val style: FontStyle = FontStyle.NORMAL,
)
