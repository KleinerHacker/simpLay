package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

/**
 * The physical frame of a page: its [size] and its inner [margins].
 */
@Serializable
data class PageLayout(val size: org.pcsoft.framework.simplay.engine.geometry.Size, val margins: org.pcsoft.framework.simplay.engine.geometry.Margins) {
    /** Width available for content between the left and right margins. */
    val contentWidth: Double
        get() = size.width - margins.left - margins.right

    /** Height available for content between the top and bottom margins. */
    val contentHeight: Double
        get() = size.height - margins.top - margins.bottom
}

/**
 * A page holding text blocks. Implementations are plain data holders.
 */
@Serializable
sealed interface Page {
    val layout: org.pcsoft.framework.simplay.engine.model.PageLayout
    val blocks: List<org.pcsoft.framework.simplay.engine.model.TextBlock>
}

/**
 * A page whose content may flow onto following pages.
 */
@Serializable
@SerialName("flow")
data class FlowPage(
    override val layout: org.pcsoft.framework.simplay.engine.model.PageLayout,
    override val blocks: List<org.pcsoft.framework.simplay.engine.model.TextBlock> = emptyList(),
) : org.pcsoft.framework.simplay.engine.model.Page

/**
 * A page whose content is confined to itself.
 */
@Serializable
@SerialName("single")
data class SinglePage(
    override val layout: org.pcsoft.framework.simplay.engine.model.PageLayout,
    override val blocks: List<org.pcsoft.framework.simplay.engine.model.TextBlock> = emptyList(),
) : org.pcsoft.framework.simplay.engine.model.Page
