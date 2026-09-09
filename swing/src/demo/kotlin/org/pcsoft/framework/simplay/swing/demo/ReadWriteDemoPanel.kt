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
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
import org.pcsoft.framework.simplay.swing.FloatingOverlay
import org.pcsoft.framework.simplay.swing.FloatingOverlayTrigger
import org.pcsoft.framework.simplay.swing.OverlayAnchor
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView

/**
 * Demo tab for an editable [PaperSheetView]: a sample selector, a zoom slider and a `Copy` floating
 * overlay that appears above the current text selection.
 */
class ReadWriteDemoPanel : JPanel(BorderLayout()) {

    private val view = PaperSheetView().apply {
        mode = PaperSheetMode.EDITABLE
        document = DemoDocuments.short
        smoothCaretBlink = true
    }
    private val sample = JComboBox(DemoDocuments.all.map { it.first }.toTypedArray())
    private val zoom = JSlider(25, 400, 100)

    init {
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

        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Sample:"))
            add(sample)
            add(JLabel("Zoom:"))
            add(zoom)
        }
        add(bar, BorderLayout.NORTH)
        add(view, BorderLayout.CENTER)

        sample.addActionListener { view.document = DemoDocuments.all[sample.selectedIndex].second }
        zoom.addChangeListener { view.zoom = zoom.value / 100.0 }
    }
}
