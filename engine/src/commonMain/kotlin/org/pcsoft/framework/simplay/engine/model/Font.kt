package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable

/**
 * A font face. A plain data holder; the size is a unit-less double.
 */
@Serializable
data class Font(
    val family: String,
    val size: Double,
    val weight: org.pcsoft.framework.simplay.engine.model.FontWeight = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FontWeight.NORMAL,
    val style: org.pcsoft.framework.simplay.engine.model.FontStyle = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FontStyle.NORMAL,
)
