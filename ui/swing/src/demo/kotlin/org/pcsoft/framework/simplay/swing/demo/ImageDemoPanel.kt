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
import java.awt.image.BaseMultiResolutionImage
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import org.pcsoft.framework.simplay.swing.DocumentImageRenderer

/**
 * Demo tab for [DocumentImageRenderer]: renders the selected sample document to a `BufferedImage` at
 * a chosen unit scale and shows it in a scroll pane. The "Page number" selector overrides the
 * position of the sample's [org.pcsoft.framework.simplay.engine.model.PageNumbering]; "Off" hides it.
 *
 * The bitmap is rendered at the display's own device scale (`GraphicsConfiguration.defaultTransform`)
 * and wrapped in a [BaseMultiResolutionImage] alongside a logically-sized variant, so a HiDPI-scaled
 * Windows desktop (125% / 150% / ...) does not stretch a 1x-resolution raster - which is what made the
 * text look blurry / pixelated on such a desktop.
 */
class ImageDemoPanel : JPanel(BorderLayout()) {

    private val sample = JComboBox(DemoDocuments.all.map { it.first }.toTypedArray())
    private val pageNumber = JComboBox(PageNumberPositions.labels.toTypedArray())
    private val scale = JSpinner(SpinnerNumberModel(1.0, 0.25, 4.0, 0.25))
    private val imageLabel = JLabel()

    init {
        val bar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Sample:"))
            add(sample)
            add(JLabel("Page number:"))
            add(pageNumber)
            add(JLabel("Scale:"))
            add(scale)
        }
        add(bar, BorderLayout.NORTH)
        add(JScrollPane(imageLabel), BorderLayout.CENTER)

        sample.addActionListener { selectPageNumberFromSample(); rerender() }
        pageNumber.addActionListener { rerender() }
        scale.addChangeListener { rerender() }

        selectPageNumberFromSample()
        rerender()
    }

    /**
     * The panel has no [java.awt.GraphicsConfiguration] - and thus no known device scale - until it
     * is added to a displayable window, which happens only after the constructor's own [rerender]
     * ran. Re-rendering once more here picks up the real scale for the initial view.
     */
    override fun addNotify() {
        super.addNotify()
        SwingUtilities.invokeLater(::rerender)
    }

    /** Seeds [pageNumber] from the currently selected sample's own numbering position. */
    private fun selectPageNumberFromSample() {
        val base = DemoDocuments.all[sample.selectedIndex].second
        pageNumber.selectedItem = PageNumberPositions.labelOf(base.numbering.position)
    }

    private fun rerender() {
        val base = DemoDocuments.all[sample.selectedIndex].second
        val position = PageNumberPositions.positionOf(pageNumber.selectedItem as String)
        val document = base.withPageNumberPosition(position)
        val unit = (scale.value as Number).toDouble()
        val deviceScale = graphicsConfiguration?.defaultTransform?.scaleX ?: 1.0
        val renderer = DocumentImageRenderer.of(document) { unitScale = unit * deviceScale; pageGap = 16.0 * deviceScale }
        imageLabel.icon = ImageIcon(toResolutionAwareImage(renderer.renderDocument(), deviceScale))
        imageLabel.revalidate()
        imageLabel.repaint()
    }

    /**
     * Wraps [deviceImage] - rendered at [deviceScale]x device pixels - into a [BaseMultiResolutionImage]
     * whose logical size is [deviceImage] shrunk back by [deviceScale], so a HiDPI-aware toolkit paints
     * the full-resolution bitmap instead of stretching a 1x one. At [deviceScale] `1.0` this is a no-op.
     */
    private fun toResolutionAwareImage(deviceImage: BufferedImage, deviceScale: Double): java.awt.Image {
        if (deviceScale <= 1.0) return deviceImage
        val logicalWidth = (deviceImage.width / deviceScale).toInt().coerceAtLeast(1)
        val logicalHeight = (deviceImage.height / deviceScale).toInt().coerceAtLeast(1)
        val logicalPlaceholder = BufferedImage(logicalWidth, logicalHeight, BufferedImage.TYPE_INT_ARGB)
        return BaseMultiResolutionImage(logicalPlaceholder, deviceImage)
    }
}
