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

package org.pcsoft.framework.simplay.swing

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

/** Sample [Document]s shared by the `swing` module tests. All text is English. */
internal object TestDocuments {

    /** The page layout [short] and [long] use; exposed so tests can compute expected geometry. */
    val layout = PageLayout(
        size = Size(width = 400.0, height = 300.0),
        margins = Margins(left = 30.0, top = 30.0, right = 30.0, bottom = 30.0),
    )

    private val body = TextStyle(font = Font(family = "SansSerif", size = 12.0))

    private val lorem =
        "The quick brown fox jumps over the lazy dog while five boxing wizards jump quickly."

    /** A one-page document with two short paragraphs. */
    val short: Document = Document(
        pages = listOf(
            FlowPage(
                layout = layout,
                blocks = listOf(TextBlock.of("Heading", body), TextBlock.of(lorem, body)),
            ),
        ),
    )

    /** A flow document long enough to span many small pages, for the virtualisation tests. */
    val long: Document = Document(
        pages = listOf(
            FlowPage(
                layout = layout,
                blocks = buildList {
                    add(TextBlock.of("A Long Story", body))
                    repeat(120) { add(TextBlock.of(lorem, body)) }
                },
            ),
        ),
    )

    /** [short] with page numbering turned on at [position] (top-center by default). */
    fun numberedShort(position: PageNumberPosition = PageNumberPosition.TOP_CENTER): Document =
        short.copy(numbering = PageNumbering(position = position))
}
