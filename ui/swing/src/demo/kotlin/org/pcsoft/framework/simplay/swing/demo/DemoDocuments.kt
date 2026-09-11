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

package org.pcsoft.framework.simplay.swing.demo

import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontWeight
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Sample [Document]s used across the demo tabs. Defined in the demo source set only; never part of
 * the published artifact.
 */
object DemoDocuments {

    private const val NOVELLA_PARAGRAPHS = 2300

    private val a4 = PageLayout(
        size = Size(width = 595.0, height = 842.0),
        margins = Margins(left = 60.0, top = 60.0, right = 60.0, bottom = 60.0),
    )

    private val body = TextStyle(font = Font(family = "Serif", size = 14.0))
    private val heading = TextStyle(font = Font(family = "Serif", size = 22.0, weight = FontWeight.BOLD))

    private val lorem =
        "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. " +
            "How vexingly quick daft zebras jump. The five boxing wizards jump quickly. " +
            "Sphinx of black quartz, judge my vow. Jackdaws love my big sphinx of quartz."

    /** A short one-page flow document, numbered bottom-center - the default sample shown at startup. */
    val short: Document = Document(
        pages = listOf(
            FlowPage(
                layout = a4,
                blocks = listOf(
                    TextBlock.of("A Short Note", heading),
                    TextBlock.of(lorem, body),
                ),
            ),
        ),
        numbering = PageNumbering(position = PageNumberPosition.BOTTOM_CENTER),
    )

    /** A flow document whose content spills across several pages, numbered bottom-center. */
    val multiPage: Document = Document(
        pages = listOf(
            FlowPage(
                layout = a4,
                blocks = buildList {
                    add(TextBlock.of("A Longer Story", heading))
                    repeat(20) { add(TextBlock.of(lorem, body)) }
                },
            ),
        ),
        numbering = PageNumbering(position = PageNumberPosition.BOTTOM_CENTER),
    )

    /**
     * A novella-length flow document of roughly 150 A4 pages, meant as a rendering performance
     * test. Numbered bottom-center, starting at `1`.
     */
    val novella: Document = Document(
        pages = listOf(
            FlowPage(
                layout = a4,
                blocks = buildList {
                    add(TextBlock.of("A Novella", heading))
                    repeat(NOVELLA_PARAGRAPHS) { add(TextBlock.of(lorem, body)) }
                },
            ),
        ),
        numbering = PageNumbering(position = PageNumberPosition.BOTTOM_CENTER),
    )

    /**
     * A mix of a growing single page followed by a flow page, numbered bottom-outer so the two
     * pages show the binding-aware parity swap (page 1 outer = right, page 2 outer = left).
     */
    val mixed: Document = Document(
        pages = listOf(
            SinglePage(
                layout = a4,
                blocks = buildList {
                    add(TextBlock.of("Single Page", heading))
                    repeat(20) { add(TextBlock.of(lorem, body)) }
                },
            ),
            FlowPage(
                layout = a4,
                blocks = buildList {
                    add(TextBlock.of("Flow Page", heading))
                    repeat(20) { add(TextBlock.of(lorem, body)) }
                },
            ),
        ),
        numbering = PageNumbering(position = PageNumberPosition.BOTTOM_OUTER),
    )

    /** All samples with a display name, in menu order. */
    val all: List<Pair<String, Document>> = listOf(
        "Short" to short,
        "Multi-page" to multiPage,
        "Mixed" to mixed,
        "Novella (~150 pages)" to novella,
    )
}
