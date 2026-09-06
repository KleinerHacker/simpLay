package org.pcsoft.framework.simplay.engine.model

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
    val font: org.pcsoft.framework.simplay.engine.model.Font,
    val lineSpacing: org.pcsoft.framework.simplay.engine.model.LineSpacing = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.LineSpacing(),
    val alignment: org.pcsoft.framework.simplay.engine.model.TextAlignment = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextAlignment.LEFT,
)
