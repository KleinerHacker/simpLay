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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
import org.pcsoft.framework.simplay.swing.FloatingOverlay
import org.pcsoft.framework.simplay.swing.FloatingOverlayListener
import org.pcsoft.framework.simplay.swing.FloatingOverlayTrigger
import org.pcsoft.framework.simplay.swing.OverlayAnchor
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.swing.TextSelectionModel
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode

/**
 * Demo tab for the [PaperSheetView]: a [PaperSheetMode] selector, a sample selector, a page number
 * position selector, a zoom slider, a live readout of the current selection length and two floating
 * overlays - a `Copy` button above the text selection and a label above the paragraph under the
 * mouse.
 *
 * Switching the mode selector is the way to compare the four interaction levels on the same
 * document: [PaperSheetMode.STATIC] has no selection, no caret and the default arrow cursor,
 * [PaperSheetMode.SELECTABLE] selects without a caret, [PaperSheetMode.NAVIGABLE] adds the caret
 * without mutating the document and [PaperSheetMode.EDITABLE] edits.
 *
 * Page deactivation is demonstrated by the `Deactivate` check boxes - one per page of the current
 * document, up to [MAX_DEACTIVATION_PAGES] - together with the [PageDeactivationMode] selector in
 * the same tool bar, which can be switched at any time to compare the modes on the same marked
 * pages. The "Four pages" sample is the one to pick here. Marks are dropped whenever the document is
 * replaced, because the page ids change with it.
 */
class PaperSheetDemoPanel : JPanel(BorderLayout()) {

    private val view = PaperSheetView().apply {
        mode = PaperSheetMode.EDITABLE
        smoothCaretBlink = true
    }
    private val mode = JComboBox(PaperSheetMode.entries.toTypedArray()).apply {
        selectedItem = view.mode
    }
    private val sample = JComboBox(DemoDocuments.all.map { it.first }.toTypedArray())
    private val pageNumber = JComboBox(PageNumberPositions.labels.toTypedArray())
    private val zoom = JSlider(25, 400, 100)
    private val selectionInfo = JLabel("selection: 0")
    private val deactivationMode = JComboBox(PageDeactivationMode.entries.toTypedArray()).apply {
        selectedItem = view.deactivatedPageHandling
    }
    private val deactivatedPageBoxes: List<JCheckBox> =
        (0 until MAX_DEACTIVATION_PAGES).map { index -> JCheckBox("P${index + 1}") }
    private val deactivatedLabel = JLabel()

    init {
        addSelectionCopyOverlay()
        addParagraphHoverOverlay()

        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Mode:"))
            add(mode)
            add(JLabel("Sample:"))
            add(sample)
            add(JLabel("Page number:"))
            add(pageNumber)
            add(JLabel("Zoom:"))
            add(zoom)
            add(selectionInfo)
            add(JLabel("Deactivation:"))
            add(deactivationMode)
            add(JLabel("Deactivate:"))
            deactivatedPageBoxes.forEach { add(it) }
            add(deactivatedLabel)
        }
        add(bar, BorderLayout.NORTH)
        add(view, BorderLayout.CENTER)

        mode.addActionListener { view.mode = mode.selectedItem as PaperSheetMode }
        sample.addActionListener { selectPageNumberFromSample(); applySample() }
        pageNumber.addActionListener { applySample() }
        zoom.addChangeListener { view.zoom = zoom.value / 100.0 }
        view.selectionModel.addPropertyChangeListener(TextSelectionModel.PROP_LENGTH) {
            selectionInfo.text = "selection: ${view.selectionModel.length}"
        }
        deactivationMode.addActionListener {
            view.deactivatedPageHandling = deactivationMode.selectedItem as PageDeactivationMode
        }
        deactivatedPageBoxes.forEachIndexed { index, box ->
            box.addActionListener {
                view.setPageDeactivated(index, box.isSelected)
                updateDeactivatedLabel()
            }
        }

        selectPageNumberFromSample()
        applySample()
    }

    /** Seeds [pageNumber] from the currently selected sample's own numbering position. */
    private fun selectPageNumberFromSample() {
        val base = DemoDocuments.all[sample.selectedIndex].second
        pageNumber.selectedItem = PageNumberPositions.labelOf(base.numbering.position)
    }

    private fun applySample() {
        val base = DemoDocuments.all[sample.selectedIndex].second
        val position = PageNumberPositions.positionOf(pageNumber.selectedItem as String)
        view.document = base.withPageNumberPosition(position)
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
            box.isEnabled = index < pages
        }
        updateDeactivatedLabel()
    }

    private fun updateDeactivatedLabel() {
        val pages = deactivatedPageBoxes.withIndex().filter { it.value.isSelected }.map { it.index + 1 }
        deactivatedLabel.text = if (pages.isEmpty()) "none" else "pages ${pages.joinToString(", ")}"
    }

    /** A `Copy` button that floats above the current text selection. */
    private fun addSelectionCopyOverlay() {
        val copyButton = JButton("Copy").apply {
            isFocusable = false
            addActionListener {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(view.selectedText), null)
            }
        }
        view.floatingOverlays += FloatingOverlay().apply {
            content = copyButton
            trigger = FloatingOverlayTrigger.SELECTION
            anchor = OverlayAnchor.TOP_LEFT
            offsetY = -4.0
        }
    }

    /** A label that floats above the paragraph currently under the mouse and shows its ordinal. */
    private fun addParagraphHoverOverlay() {
        val badge = JLabel("Paragraph").apply {
            isOpaque = true
            background = Color(0x33, 0x33, 0x33)
            foreground = Color.WHITE
            border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
        }
        view.floatingOverlays += FloatingOverlay().apply {
            content = badge
            trigger = FloatingOverlayTrigger.PARAGRAPH_HOVER
            anchor = OverlayAnchor.TOP_LEFT
            offsetY = -4.0
            onShown = FloatingOverlayListener { event -> badge.text = "Paragraph #${event.index}" }
        }
    }

    private companion object {

        /** Number of `Deactivate` check boxes; enough for the "Four pages" sample. */
        const val MAX_DEACTIVATION_PAGES = 4
    }
}
