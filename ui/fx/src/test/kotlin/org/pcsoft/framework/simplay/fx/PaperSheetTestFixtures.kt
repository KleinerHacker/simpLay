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

package org.pcsoft.framework.simplay.fx

import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Shared sample documents for the [PaperSheetView] tests. All text is English.
 */
object PaperSheetTestFixtures {

    /** One short paragraph used to fill the sample blocks. */
    const val PARAGRAPH: String =
        "The quick brown fox jumps over the lazy dog and then quietly rests beneath the old oak tree."

    /** The page layout every fixture document uses; exposed so tests can compute expected geometry. */
    val layout = PageLayout(
        size = Size(width = 360.0, height = 260.0),
        margins = Margins(left = 30.0, top = 30.0, right = 30.0, bottom = 30.0),
    )

    private val style = TextStyle(font = Font(family = "Serif", size = 14.0))

    /** A flow document with [paragraphs] paragraphs; large values spill across several pages. */
    fun flowDocument(paragraphs: Int): Document =
        Document(
            pages = listOf(
                FlowPage(
                    layout = layout,
                    blocks = buildList { repeat(paragraphs) { add(TextBlock.of(PARAGRAPH, style)) } },
                ),
            ),
        )

    /** [flowDocument] with page numbering turned on at [position] (top-center by default). */
    fun numberedFlowDocument(
        paragraphs: Int,
        position: PageNumberPosition = PageNumberPosition.TOP_CENTER,
    ): Document = flowDocument(paragraphs).copy(numbering = PageNumbering(position = position))

    /**
     * A two-raw-page document, each page a single [FlowPage] with distinct text and a distinct stable
     * id, for the page-deactivation tests. Each page's content is short and fits on one sheet, so page
     * two starts right at the first block boundary.
     */
    fun twoPageDocument(): Document =
        Document(
            pages = listOf(
                FlowPage(layout = layout, blocks = listOf(TextBlock.of("Page one content here.", style))),
                FlowPage(layout = layout, blocks = listOf(TextBlock.of("Page two content here.", style))),
            ),
        )
}
