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
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
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
import org.pcsoft.framework.simplay.fx.PaperSheetMode
import org.pcsoft.framework.simplay.fx.PaperSheetView

/**
 * Content of the demo's `Read/Write` tab: a [PaperSheetView] in [PaperSheetMode.EDITABLE], driven by a
 * [ToolBar] that exposes the mode, the smooth-caret-blink switch and every value the `Readonly` tab
 * exposes (sample document, font family, outer margin, page gap, min/max/current zoom), and reads
 * back the current zoom, the caret position from [PaperSheetView.caretModel] and the size of the
 * (edited) document (characters and pages). `Go to start` / `Go to end` drive the caret model
 * directly.
 */
class ReadWriteDemoTab : BorderPane() {

    private val view = PaperSheetView().apply { mode = PaperSheetMode.EDITABLE }

    private val modeBox = ChoiceBox<PaperSheetMode>().apply {
        items.setAll(PaperSheetMode.entries)
        value = PaperSheetMode.EDITABLE
    }

    private val smoothCaretBox = CheckBox("Smooth caret").apply { isSelected = view.smoothCaretBlink }

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

    private val goStartButton = Button("Go to start").apply { setOnAction { view.caretModel.moveToStart() } }
    private val goEndButton = Button("Go to end").apply { setOnAction { view.caretModel.moveToEnd() } }

    private val zoomLabel = Label()
    private val caretLabel = Label()
    private val documentLabel = Label()

    init {
        top = ToolBar(
            Label("Mode:"), modeBox, smoothCaretBox,
            Separator(),
            Label("Document:"), sampleBox,
            Label("Font:"), fontFamilyBox,
            Separator(),
            Label("Outer margin:"), outerMarginSpinner,
            Label("Page gap:"), pageGapSpinner,
            Separator(),
            Label("Min zoom:"), minZoomSpinner,
            Label("Max zoom:"), maxZoomSpinner,
            Label("Zoom:"), zoomSpinner,
            zoomLabel,
            Separator(),
            goStartButton, goEndButton, caretLabel,
            Separator(),
            documentLabel,
        )
        center = view

        modeBox.valueProperty().addListener { _, _, v -> if (v != null) view.mode = v }
        smoothCaretBox.selectedProperty().addListener { _, _, v -> view.smoothCaretBlink = v }
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
        view.caretModel.positionProperty.addListener { _, _, _ -> updateCaretLabel() }
        view.documentProperty.addListener { _, _, _ -> updateDocumentLabel() }

        applySample()
        zoomLabel.text = "Zoom: ${format(view.zoom)}"
        updateCaretLabel()
        updateDocumentLabel()
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

    private fun updateCaretLabel() {
        val caret = view.caretModel
        caretLabel.text =
            "Caret: ${caret.position} (blocks ${caret.blockCount}, words ${caret.wordCount}, symbols ${caret.symbolCount})"
    }

    private fun updateDocumentLabel() {
        val doc = view.document
        val chars = doc?.pages?.sumOf { page -> page.blocks.sumOf { it.toString().length } } ?: 0
        val pages = doc?.pages?.size ?: 0
        documentLabel.text = "Document: $chars chars in $pages page(s)"
    }

    private fun format(value: Double): String = ((value * 100.0).toInt() / 100.0).toString()

    private companion object {

        const val FONT_DEFAULT = "Default (document)"
    }
}
