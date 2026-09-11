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

package org.pcsoft.framework.simplay.engine.e2e

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.PageCountingMode
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight
import org.pcsoft.framework.simplay.engine.model.LineSpacing
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.PageNumbering
import org.pcsoft.framework.simplay.engine.model.PageNumberPosition
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextAlignment
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Shared fixtures for the end-to-end tests. All sample text is in English.
 *
 * The measurer is fully deterministic: the advance width is `text.length * font.size * 0.6`, the
 * ascent is `font.size * 0.8` and the descent is `font.size * 0.2`. A blank has the same advance as
 * any other character. With a font size of `10.0` this gives a character width of `6.0`, an ascent
 * of `8.0`, a descent of `2.0` and, for a default [TextStyle], a resolved line height of `10.0`.
 */
internal object E2ETestData {

    /** Deterministic font-measuring callback shared by every end-to-end test. */
    val measurer = FontMeasureCalculator { font, text ->
        TextMetrics(
            width = text.length * font.size * 0.6,
            ascent = font.size * 0.8,
            descent = font.size * 0.2,
        )
    }

    /** A fresh engine over [measurer] with the default strategies. */
    fun engine(): SimpLayEngine = SimpLayEngine.builder(measurer).build()

    /** A page frame of the given size with no margins, so the content area equals the page size. */
    fun layout(width: Double, height: Double) = PageLayout(
        size = Size(width = width, height = height),
        margins = Margins(left = 0.0, top = 0.0, right = 0.0, bottom = 0.0),
    )

    private val bodyFont = Font(family = "Body", size = 10.0)
    private val headingFont = Font(family = "Heading", size = 12.0, weight = FontWeight.BOLD)

    /** Left-aligned body style, default line spacing. */
    val bodyStyle = TextStyle(font = bodyFont, alignment = TextAlignment.LEFT)

    /** Justified style over a larger font with 1.5x line spacing. */
    val justifiedStyle = TextStyle(
        font = headingFont,
        lineSpacing = LineSpacing(factor = 1.5),
        alignment = TextAlignment.JUSTIFY,
    )

    /**
     * A document that mixes a [FlowPage] holding two differently styled blocks, a [SinglePage] with
     * a small layout height and a long block, and a second [FlowPage] whose single block spans
     * several pages.
     *
     * The first flow page is large enough to hold both of its blocks without a page break.
     */
    fun mixedDocument(): Document = Document(
        pages = listOf(
            FlowPage(
                layout = layout(width = 120.0, height = 400.0),
                blocks = listOf(
                    TextBlock.of("alpha bravo charlie delta echo foxtrot golf", bodyStyle),
                    TextBlock.of(
                        "one two three four five six seven eight nine ten eleven twelve",
                        justifiedStyle,
                    ),
                ),
            ),
            SinglePage(
                layout = layout(width = 120.0, height = 30.0),
                blocks = listOf(
                    TextBlock.of(
                        "This single page block is long enough to grow the page far past its layout height.",
                        bodyStyle,
                    ),
                ),
            ),
            FlowPage(
                layout = layout(width = 120.0, height = 40.0),
                blocks = listOf(
                    TextBlock.of(
                        "The quick brown fox jumps over the lazy dog while the sleepy cat " +
                            "watches every single move from the warm and quiet windowsill nearby",
                        bodyStyle,
                    ),
                ),
            ),
        ),
    )

    /**
     * A small document that exercises every raw model type: both page kinds, both [FontWeight]s and
     * [FontStyle]s, several [TextAlignment]s, a custom [LineSpacing] and blocks holding words and
     * symbols. Kept small so a serialization round-trip compares cleanly.
     */
    fun rawSampleDocument(): Document = Document(
        pages = listOf(
            FlowPage(
                layout = layout(width = 200.0, height = 300.0),
                blocks = listOf(
                    TextBlock.of(
                        "Hello, brave new world!",
                        TextStyle(
                            font = Font(
                                family = "Serif",
                                size = 11.0,
                                weight = FontWeight.NORMAL,
                                style = FontStyle.ITALIC,
                            ),
                            lineSpacing = LineSpacing(factor = 1.25, extraLeading = 2.0),
                            alignment = TextAlignment.CENTER,
                        ),
                    ),
                    TextBlock.of(
                        "Numbers 1 2 3 and symbols # & * stay intact.",
                        TextStyle(
                            font = Font(family = "Mono", size = 9.0, weight = FontWeight.BOLD),
                            alignment = TextAlignment.JUSTIFY,
                        ),
                    ),
                ),
            ),
            SinglePage(
                layout = layout(width = 150.0, height = 100.0),
                blocks = listOf(
                    TextBlock.of(
                        "A confined page.",
                        TextStyle(font = Font(family = "Sans", size = 10.0), alignment = TextAlignment.RIGHT),
                    ),
                ),
            ),
        ),
    )

    /**
     * [rawSampleDocument] with a non-default [PageNumbering] configured: bottom-center position,
     * a custom start number and its second page excluded from numbering.
     */
    fun numberedSampleDocument(): Document {
        val base = rawSampleDocument()
        return base.copy(
            numbering = PageNumbering(
                position = PageNumberPosition.BOTTOM_CENTER,
                startNumber = 3,
                excludedPageIds = setOf(base.pages[1].id),
                counting = PageCountingMode.SKIP_EXCLUDED,
            ),
        )
    }
}
