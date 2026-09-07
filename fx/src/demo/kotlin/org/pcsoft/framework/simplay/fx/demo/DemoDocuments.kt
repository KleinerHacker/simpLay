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

package org.pcsoft.framework.simplay.fx.demo

import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontWeight
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Sample [Document]s used across the demo tabs. Defined in the demo source set only; never part of
 * the published artifact.
 */
object DemoDocuments {

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

    /** A short one-page flow document. */
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
    )

    /** A flow document whose content spills across several pages. */
    val multiPage: Document = Document(
        pages = listOf(
            FlowPage(
                layout = a4,
                blocks = buildList {
                    add(TextBlock.of("A Longer Story", heading))
                    repeat(12) { add(TextBlock.of(lorem, body)) }
                },
            ),
        ),
    )

    /** A mix of a growing single page followed by a flow page. */
    val mixed: Document = Document(
        pages = listOf(
            SinglePage(
                layout = a4,
                blocks = listOf(
                    TextBlock.of("Single Page", heading),
                    TextBlock.of(lorem, body),
                    TextBlock.of(lorem, body),
                ),
            ),
            FlowPage(
                layout = a4,
                blocks = listOf(
                    TextBlock.of("Flow Page", heading),
                    TextBlock.of(lorem, body),
                ),
            ),
        ),
    )

    /** All samples with a display name, in menu order. */
    val all: List<Pair<String, Document>> = listOf(
        "Short" to short,
        "Multi-page" to multiPage,
        "Mixed" to mixed,
    )
}
