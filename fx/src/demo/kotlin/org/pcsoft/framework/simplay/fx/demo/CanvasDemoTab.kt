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

import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Separator
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Font as FxFont
import org.pcsoft.framework.simplay.engine.CharacterLineBreakerStrategy
import org.pcsoft.framework.simplay.engine.GreedyWordLineBreakerStrategy
import org.pcsoft.framework.simplay.engine.LineBreakerStrategy
import org.pcsoft.framework.simplay.engine.NoOpWordBreakerStrategy
import org.pcsoft.framework.simplay.engine.NoWrapLineBreakerStrategy
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.fx.CanvasDocumentRenderer

/**
 * Content of the demo's `Canvas` tab: a [CanvasDocumentRenderer] output shown in a [ScrollPane],
 * driven by a [ToolBar] that exposes every renderer setting (sample document, font family, unit
 * scale, page gap, line-/word-break strategy, whole-document vs. single-page mode) and reads back
 * the resulting canvas size.
 *
 * The font selector lists every font family installed on the system ([FxFont.getFamilies]); picking
 * one rebuilds the sample document with that family so the measure and render path can be tried
 * with real system fonts. "Default (document)" keeps the family the sample was built with.
 *
 * As a demo-only aid the sheet edges are outlined with a thin grey stroke drawn on top of the
 * renderer output. This border is not part of `CanvasDocumentRenderer`; it lives entirely in the
 * demo.
 */
class CanvasDemoTab : BorderPane() {

    private val sampleBox = ComboBox<String>().apply {
        items.setAll(DemoDocuments.all.map { it.first })
        selectionModel.selectFirst()
    }

    private val fontFamilyBox = ComboBox<String>().apply {
        items.add(FONT_DEFAULT)
        items.addAll(FxFont.getFamilies())
        selectionModel.selectFirst()
    }

    private val unitScaleSpinner = Spinner<Double>(0.25, 4.0, 1.0, 0.25)

    private val pageGapSpinner = Spinner<Double>(0.0, 120.0, 24.0, 4.0)

    private val modeBox = ComboBox<String>().apply {
        items.setAll(MODE_WHOLE, MODE_SINGLE)
        selectionModel.selectFirst()
    }

    private val pageIndexSpinner = Spinner<Int>().apply {
        valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0)
        isDisable = true
    }

    private val lineBreakBox = ComboBox<String>().apply {
        items.setAll(LINE_GREEDY, LINE_CHARACTER, LINE_NOWRAP)
        selectionModel.selectFirst()
    }

    private val wordBreakBox = ComboBox<String>().apply {
        items.setAll(WORD_NOOP)
        selectionModel.selectFirst()
    }

    private val sizeLabel = Label()

    private val canvasHolder = Pane()

    init {
        top = ToolBar(
            Label("Document:"), sampleBox,
            Label("Font:"), fontFamilyBox,
            Separator(),
            Label("Unit scale:"), unitScaleSpinner,
            Label("Page gap:"), pageGapSpinner,
            Separator(),
            Label("Line break:"), lineBreakBox,
            Label("Word break:"), wordBreakBox,
            Separator(),
            Label("Mode:"), modeBox,
            Label("Page:"), pageIndexSpinner,
            Separator(),
            sizeLabel,
        )
        center = ScrollPane(canvasHolder).apply { padding = Insets(12.0) }

        listOf(sampleBox, fontFamilyBox, modeBox, lineBreakBox, wordBreakBox).forEach {
            it.valueProperty().addListener { _, _, _ -> redraw() }
        }
        listOf(unitScaleSpinner, pageGapSpinner).forEach {
            it.valueProperty().addListener { _, _, _ -> redraw() }
        }
        pageIndexSpinner.valueProperty().addListener { _, _, _ -> redraw() }

        redraw()
    }

    private fun redraw() {
        val renderer = CanvasDocumentRenderer.of(selectedDocument()) {
            unitScale = unitScaleSpinner.value
            pageGap = pageGapSpinner.value
            lineBreakerStrategy = selectedLineBreaker()
            wordBreakerStrategy = NoOpWordBreakerStrategy
        }

        val singlePage = modeBox.value == MODE_SINGLE
        pageIndexSpinner.isDisable = !singlePage
        val lastPage = (renderer.pageCount - 1).coerceAtLeast(0)
        (pageIndexSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory).apply {
            max = lastPage
            if (value > lastPage) value = lastPage
        }

        if (singlePage) {
            val index = pageIndexSpinner.value
            val canvas = renderer.renderPage(index)
            val size = renderer.pageCanvasSizes[index]
            outlineSheets(canvas, listOf(0.0), listOf(size.width to size.height))
            canvasHolder.children.setAll(canvas)
            sizeLabel.text = "Canvas: ${format(size.width)} x ${format(size.height)}"
            return
        }

        val size = renderer.documentCanvasSize
        sizeLabel.text = "Canvas: ${format(size.width)} x ${format(size.height)}"
        if (size.width > MAX_CANVAS_DIMENSION || size.height > MAX_CANVAS_DIMENSION) {
            // A single JavaFX Canvas that large cannot allocate its render texture (RTTexture is
            // null). Whole-document rendering of a very tall document is out of scope for the
            // canvas renderer; use single-page mode instead. Demo-only guard.
            canvasHolder.children.setAll(
                Label(
                    "The whole-document canvas would be ${format(size.width)} x ${format(size.height)}, " +
                        "beyond the JavaFX texture limit of ${MAX_CANVAS_DIMENSION.toInt()} px. " +
                        "Switch \"Mode\" to \"$MODE_SINGLE\" to page through it.",
                ).apply {
                    isWrapText = true
                    padding = Insets(16.0)
                },
            )
            return
        }

        val canvas = renderer.renderDocument()
        outlineSheets(canvas, pageOriginsY(renderer), pageSizes(renderer))
        canvasHolder.children.setAll(canvas)
    }

    /**
     * Draws a thin grey rectangle around every sheet, on top of the already rendered [canvas]. Each
     * page starts at the matching entry of [originsY] and has the matching `width to height` from
     * [sizes]; both are in scaled canvas units.
     */
    private fun outlineSheets(canvas: Canvas, originsY: List<Double>, sizes: List<Pair<Double, Double>>) {
        val gc: GraphicsContext = canvas.graphicsContext2D
        gc.save()
        gc.stroke = SHEET_BORDER_COLOR
        gc.lineWidth = 1.0
        gc.setLineDashes()
        originsY.forEachIndexed { index, y ->
            val (w, h) = sizes[index]
            if (w > 1.0 && h > 1.0) {
                gc.strokeRect(0.5, y + 0.5, w - 1.0, h - 1.0)
            }
        }
        gc.restore()
    }

    private fun pageSizes(renderer: CanvasDocumentRenderer): List<Pair<Double, Double>> =
        (0 until renderer.pageCount).map {
            val size = renderer.pageCanvasSizes[it]
            size.width to size.height
        }

    private fun pageOriginsY(renderer: CanvasDocumentRenderer): List<Double> {
        val scaledGap = pageGapSpinner.value * unitScaleSpinner.value
        var y = 0.0
        return (0 until renderer.pageCount).map { index ->
            val origin = y
            y += renderer.pageCanvasSizes[index].height + scaledGap
            origin
        }
    }

    private fun selectedDocument(): Document {
        val base = DemoDocuments.all.first { it.first == sampleBox.value }.second
        val family = fontFamilyBox.value
        return if (family == null || family == FONT_DEFAULT) base else base.withFontFamily(family)
    }

    /** Rebuilds every block of [this] document with [family] as the font family, keeping all text. */
    private fun Document.withFontFamily(family: String): Document = copy(
        pages = pages.map { page ->
            val blocks = page.blocks.map { block ->
                TextBlock.of(block.toString(), block.style.copy(font = block.style.font.copy(family = family)))
            }
            when (page) {
                is FlowPage -> page.copy(blocks = blocks)
                is SinglePage -> page.copy(blocks = blocks)
            }
        },
    )

    private fun selectedLineBreaker(): LineBreakerStrategy = when (lineBreakBox.value) {
        LINE_CHARACTER -> CharacterLineBreakerStrategy
        LINE_NOWRAP -> NoWrapLineBreakerStrategy
        else -> GreedyWordLineBreakerStrategy
    }

    private fun format(value: Double): String = ((value * 10.0).toInt() / 10.0).toString()

    private companion object {

        const val MODE_WHOLE = "Whole document"
        const val MODE_SINGLE = "Single page"
        const val LINE_GREEDY = "Greedy (word)"
        const val LINE_CHARACTER = "Character"
        const val LINE_NOWRAP = "No wrap"
        const val WORD_NOOP = "No-op"
        const val FONT_DEFAULT = "Default (document)"

        /** Safe upper bound for a single JavaFX `Canvas` edge before texture allocation fails. */
        const val MAX_CANVAS_DIMENSION = 16_384.0

        val SHEET_BORDER_COLOR: Color = Color.gray(0.75)
    }
}
