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

import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.PageCountingMode
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * Anchor of a page number on a sheet. `INNER` / `OUTER` alternate their horizontal side by sheet
 * parity (binding-aware layout); `OFF` disables numbering entirely.
 */
enum class PageNumberPosition {
    OFF,
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, TOP_INNER, TOP_OUTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_INNER, BOTTOM_OUTER,
}

/**
 * Page numbering configuration of a [Document]. A plain data holder, except [counting]: a
 * [PageCountingMode] is both the persisted setting and, through [PageCountingMode.numbers], the
 * strategy that executes it - see [org.pcsoft.framework.simplay.engine.PageCountingStrategy].
 *
 * [excludedPageIds] references pages by their stable [Page.id], never by index, so it survives
 * reordering, splicing and flow-overflow pagination.
 */
@Serializable
data class PageNumbering(
    val position: PageNumberPosition = PageNumberPosition.OFF,
    val startNumber: Int = 1,
    val excludedPageIds: Set<String> = emptySet(),
    val counting: PageCountingMode = PageCountingMode.CONTINUOUS,
    val textStyle: TextStyle = DEFAULT_NUMBER_STYLE,
) : PlatformSerializable {
    companion object {
        /** Default text style of a page number: a small serif font. */
        val DEFAULT_NUMBER_STYLE = TextStyle(Font("Serif", 10.0))

        /** Numbering disabled. */
        val OFF = PageNumbering(position = PageNumberPosition.OFF)
    }
}
