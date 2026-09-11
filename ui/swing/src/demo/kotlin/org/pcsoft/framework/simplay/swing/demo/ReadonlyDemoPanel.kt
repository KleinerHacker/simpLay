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

/**
 * Demo tab for a read-only [PaperSheetView]: a sample selector, a page number position selector, a
 * zoom slider, a live readout of the current selection length and two floating overlays - a `Copy`
 * button above the text selection and a label above the paragraph under the mouse.
 */
class ReadonlyDemoPanel : JPanel(BorderLayout()) {

    private val view = PaperSheetView().apply {
        mode = PaperSheetMode.READONLY
    }
    private val sample = JComboBox(DemoDocuments.all.map { it.first }.toTypedArray())
    private val pageNumber = JComboBox(PageNumberPositions.labels.toTypedArray())
    private val zoom = JSlider(25, 400, 100)
    private val selectionInfo = JLabel("selection: 0")

    init {
        addSelectionCopyOverlay()
        addParagraphHoverOverlay()

        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Sample:"))
            add(sample)
            add(JLabel("Page number:"))
            add(pageNumber)
            add(JLabel("Zoom:"))
            add(zoom)
            add(selectionInfo)
        }
        add(bar, BorderLayout.NORTH)
        add(view, BorderLayout.CENTER)

        sample.addActionListener { selectPageNumberFromSample(); applySample() }
        pageNumber.addActionListener { applySample() }
        zoom.addChangeListener { view.zoom = zoom.value / 100.0 }
        view.selectionModel.addPropertyChangeListener(TextSelectionModel.PROP_LENGTH) {
            selectionInfo.text = "selection: ${view.selectionModel.length}"
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
}
