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

import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.Spinner
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font as FxFont
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.fx.control.PaperSheetView

/**
 * Content of the demo's `Readonly` tab: a [PaperSheetView] in its read-only mode, driven by a
 * [ToolBar] that exposes every externally settable value (sample document, font family, outer
 * margin, page gap, min/max/current zoom) and reads back the current zoom and the current text
 * selection from [PaperSheetView.getSelectionModel] (range, length, run count). Two buttons drive
 * the selection model with `selectAll` and `clearSelection`.
 *
 * The font selector lists every font family installed on the system ([FxFont.getFamilies]); picking
 * one rebuilds the sample document with that family. "Default (document)" keeps the family the
 * sample was built with.
 */
class ReadonlyDemoTab : BorderPane() {

    private val view = PaperSheetView()

    private val sampleBox = ComboBox<String>().apply {
        items.setAll(DemoDocuments.all.map { it.first })
        selectionModel.selectFirst()
    }

    private val fontFamilyBox = ComboBox<String>().apply {
        items.add(FONT_DEFAULT)
        items.addAll(FxFont.getFamilies())
        selectionModel.selectFirst()
    }

    private val outerMarginSpinner = Spinner<Double>(0.0, 200.0, view.outerMargin, 4.0)
    private val pageGapSpinner = Spinner<Double>(0.0, 200.0, view.pageGap, 4.0)
    private val minZoomSpinner = Spinner<Double>(0.1, 2.0, view.minZoom, 0.05)
    private val maxZoomSpinner = Spinner<Double>(1.0, 8.0, view.maxZoom, 0.5)
    private val zoomSpinner = Spinner<Double>(0.1, 8.0, view.zoom, 0.1)

    private val selectAllButton = Button("Select all").apply { setOnAction { view.selectionModel.selectAll() } }
    private val clearButton = Button("Clear").apply { setOnAction { view.selectionModel.clearSelection() } }

    private val zoomLabel = Label()
    private val selectionLabel = Label()

    init {
        top = ToolBar(
            Label("Document:"), sampleBox,
            Label("Font:"), fontFamilyBox,
            Separator(),
            Label("Outer margin:"), outerMarginSpinner,
            Label("Page gap:"), pageGapSpinner,
            Separator(),
            Label("Min zoom:"), minZoomSpinner,
            Label("Max zoom:"), maxZoomSpinner,
            Label("Zoom:"), zoomSpinner,
            Separator(),
            zoomLabel, Separator(),
            selectAllButton, clearButton, selectionLabel,
        )
        center = view

        sampleBox.valueProperty().addListener { _, _, _ -> applySample() }
        fontFamilyBox.valueProperty().addListener { _, _, _ -> applySample() }
        outerMarginSpinner.valueProperty().addListener { _, _, v -> view.outerMargin = v }
        pageGapSpinner.valueProperty().addListener { _, _, v -> view.pageGap = v }
        minZoomSpinner.valueProperty().addListener { _, _, v -> view.minZoom = v }
        maxZoomSpinner.valueProperty().addListener { _, _, v -> view.maxZoom = v }
        zoomSpinner.valueProperty().addListener { _, _, v -> view.zoom = v }

        view.zoomProperty.addListener { _, _, v ->
            zoomLabel.text = "Zoom: ${format(v.toDouble())}"
            if (zoomSpinner.value != v.toDouble()) zoomSpinner.valueFactory.value = v.toDouble()
        }
        view.selectionModel.textProperty.addListener { _, _, _ -> updateSelectionLabel() }

        applySample()
        zoomLabel.text = "Zoom: ${format(view.zoom)}"
        updateSelectionLabel()
    }

    private fun applySample() {
        val base = DemoDocuments.all.first { it.first == sampleBox.value }.second
        val family = fontFamilyBox.value
        view.document = if (family == null || family == FONT_DEFAULT) base else base.withFontFamily(family)
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

    private fun updateSelectionLabel() {
        val model = view.selectionModel
        selectionLabel.text = if (model.isEmpty) {
            "Selection: -"
        } else {
            "Selection: ${model.length} chars [${model.startIndex}–${model.endIndex}], ${model.runs.size} run(s)"
        }
    }

    private fun format(value: Double): String = ((value * 100.0).toInt() / 100.0).toString()

    private companion object {

        const val FONT_DEFAULT = "Default (document)"
    }
}
