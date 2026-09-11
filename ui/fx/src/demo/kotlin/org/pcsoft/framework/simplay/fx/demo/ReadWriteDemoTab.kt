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
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode

/**
 * Content of the demo's `Read/Write` tab: a [PaperSheetView] in [PaperSheetMode.EDITABLE], driven by a
 * [ToolBar] that exposes the mode, the smooth-caret-blink switch and every value the `Readonly` tab
 * exposes (sample document, font family, page number position, stylesheet, outer margin, page gap,
 * min/max/current zoom), and reads back the current zoom, the caret position from
 * [PaperSheetView.caretModel] and the size of the (edited) document (characters and pages). `Go to
 * start` / `Go to end` drive the caret model directly.
 *
 * The "Stylesheet" selector switches between the built-in look ("Standard") and the bundled
 * `demo-dark.css` example ("Dark"). The "Page number" selector overrides the position of the
 * sample's [org.pcsoft.framework.simplay.engine.model.PageNumbering]; "Off" hides it.
 *
 * Page deactivation is demonstrated by the "Deactivate" check boxes - one per page of the current
 * document, up to [MAX_DEACTIVATION_PAGES] - together with the [PageDeactivationMode] selector next
 * to them, which can be switched at any time to compare the modes on the same marked pages. The
 * "Four pages" sample is the one to pick here. Marks are dropped whenever the document is replaced,
 * because the page ids change with it.
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

    private val pageNumberBox = ComboBox<String>().apply {
        items.setAll(PageNumberPositions.labels)
    }

    private val stylesheetBox = ChoiceBox<String>().apply {
        items.setAll(STYLE_STANDARD, STYLE_DARK)
        value = STYLE_STANDARD
    }

    private val deactivationModeBox = ChoiceBox<PageDeactivationMode>().apply {
        items.setAll(PageDeactivationMode.entries)
        value = view.deactivatedPageHandling
    }

    private val deactivatedPageBoxes: List<CheckBox> =
        (0 until MAX_DEACTIVATION_PAGES).map { index -> CheckBox("P${index + 1}") }

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
    private val deactivatedLabel = Label()

    init {
        top = ToolBar(
            *buildList {
                addAll(listOf(Label("Mode:"), modeBox, smoothCaretBox))
                add(Separator())
                addAll(listOf(Label("Document:"), sampleBox))
                addAll(listOf(Label("Font:"), fontFamilyBox))
                addAll(listOf(Label("Page number:"), pageNumberBox))
                addAll(listOf(Label("Stylesheet:"), stylesheetBox))
                add(Separator())
                addAll(listOf(Label("Deactivation:"), deactivationModeBox, Label("Deactivate:")))
                addAll(deactivatedPageBoxes)
                add(deactivatedLabel)
                add(Separator())
                addAll(listOf(Label("Outer margin:"), outerMarginSpinner))
                addAll(listOf(Label("Page gap:"), pageGapSpinner))
                add(Separator())
                addAll(listOf(Label("Min zoom:"), minZoomSpinner))
                addAll(listOf(Label("Max zoom:"), maxZoomSpinner))
                addAll(listOf(Label("Zoom:"), zoomSpinner, zoomLabel))
                add(Separator())
                addAll(listOf(goStartButton, goEndButton, caretLabel))
                add(Separator())
                add(documentLabel)
            }.toTypedArray(),
        )
        center = view

        modeBox.valueProperty().addListener { _, _, v -> if (v != null) view.mode = v }
        smoothCaretBox.selectedProperty().addListener { _, _, v -> view.smoothCaretBlink = v }
        sampleBox.valueProperty().addListener { _, _, _ -> selectPageNumberBoxFromSample(); applySample() }
        fontFamilyBox.valueProperty().addListener { _, _, _ -> applySample() }
        pageNumberBox.valueProperty().addListener { _, _, _ -> applySample() }
        stylesheetBox.valueProperty().addListener { _, _, v -> applyStylesheet(v) }
        deactivationModeBox.valueProperty().addListener { _, _, v -> if (v != null) view.deactivatedPageHandling = v }
        deactivatedPageBoxes.forEachIndexed { index, box ->
            box.selectedProperty().addListener { _, _, selected ->
                view.setPageDeactivated(index, selected)
                updateDeactivatedLabel()
            }
        }
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

        selectPageNumberBoxFromSample()
        applySample()
        zoomLabel.text = "Zoom: ${format(view.zoom)}"
        updateCaretLabel()
        updateDocumentLabel()
    }

    /** Seeds [pageNumberBox] from the currently selected sample's own numbering position. */
    private fun selectPageNumberBoxFromSample() {
        val base = DemoDocuments.all.first { it.first == sampleBox.value }.second
        pageNumberBox.value = PageNumberPositions.labelOf(base.numbering.position)
    }

    private fun applySample() {
        val base = DemoDocuments.all.first { it.first == sampleBox.value }.second
        val family = fontFamilyBox.value
        val withFont = if (family == null || family == FONT_DEFAULT) base else base.withFontFamily(family)
        view.document = withFont.withPageNumberPosition(PageNumberPositions.positionOf(pageNumberBox.value))
        resetDeactivation()
    }

    /**
     * Drops every deactivation mark and re-enables one check box per page of the current document:
     * the ids of the previous document no longer exist, so keeping the marks would be misleading.
     */
    private fun resetDeactivation() {
        view.deactivatedPageIds = emptySet()
        val pages = view.document?.pages?.size ?: 0
        deactivatedPageBoxes.forEachIndexed { index, box ->
            box.isSelected = false
            box.isDisable = index >= pages
        }
        updateDeactivatedLabel()
    }

    /** Adds or removes the bundled `demo-dark.css` on this tab so it cascades to [view]. */
    private fun applyStylesheet(choice: String?) {
        stylesheets.remove(DARK_STYLESHEET)
        if (choice == STYLE_DARK) stylesheets.add(DARK_STYLESHEET)
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

    private fun updateDeactivatedLabel() {
        val pages = deactivatedPageBoxes.withIndex().filter { it.value.isSelected }.map { it.index + 1 }
        deactivatedLabel.text = if (pages.isEmpty()) "none" else "pages ${pages.joinToString(", ")}"
    }

    private fun format(value: Double): String = ((value * 100.0).toInt() / 100.0).toString()

    private companion object {

        const val FONT_DEFAULT = "Default (document)"

        const val STYLE_STANDARD = "Standard"
        const val STYLE_DARK = "Dark"

        /** Number of `Deactivate` check boxes; enough for the "Four pages" sample. */
        const val MAX_DEACTIVATION_PAGES = 4

        val DARK_STYLESHEET: String =
            ReadWriteDemoTab::class.java.getResource("demo-dark.css")!!.toExternalForm()
    }
}
