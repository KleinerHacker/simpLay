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
import javax.swing.ImageIcon
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import org.pcsoft.framework.simplay.swing.DocumentImageRenderer

/**
 * Demo tab for [DocumentImageRenderer]: renders the selected sample document to a `BufferedImage` at
 * a chosen unit scale and shows it in a scroll pane.
 */
class ImageDemoPanel : JPanel(BorderLayout()) {

    private val sample = JComboBox(DemoDocuments.all.map { it.first }.toTypedArray())
    private val scale = JSpinner(SpinnerNumberModel(1.0, 0.25, 4.0, 0.25))
    private val imageLabel = JLabel()

    init {
        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Sample:"))
            add(sample)
            add(JLabel("Scale:"))
            add(scale)
        }
        add(bar, BorderLayout.NORTH)
        add(JScrollPane(imageLabel), BorderLayout.CENTER)

        sample.addActionListener { rerender() }
        scale.addChangeListener { rerender() }
        rerender()
    }

    private fun rerender() {
        val document = DemoDocuments.all[sample.selectedIndex].second
        val unit = (scale.value as Number).toDouble()
        val renderer = DocumentImageRenderer.of(document) { unitScale = unit; pageGap = 16.0 }
        imageLabel.icon = ImageIcon(renderer.renderDocument())
        imageLabel.revalidate()
        imageLabel.repaint()
    }
}
