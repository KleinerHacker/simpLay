/*
 * Copyright (c) KleinerHacker alias Pfeiffer C Soft 2026.
 * This work is licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, this software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations.
 */

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
