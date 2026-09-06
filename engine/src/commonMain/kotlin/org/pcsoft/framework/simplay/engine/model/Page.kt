package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

/**
 * The physical frame of a page: its [size] and its inner [margins].
 */
@Serializable
data class PageLayout(val size: Size, val margins: Margins) {
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
    val layout: PageLayout
    val blocks: List<TextBlock>
}

/**
 * A page whose content may flow onto following pages.
 */
@Serializable
@SerialName("flow")
data class FlowPage(
    override val layout: PageLayout,
    override val blocks: List<TextBlock> = emptyList(),
) : Page

/**
 * A page whose content is confined to itself.
 */
@Serializable
@SerialName("single")
data class SinglePage(
    override val layout: PageLayout,
    override val blocks: List<TextBlock> = emptyList(),
) : Page
