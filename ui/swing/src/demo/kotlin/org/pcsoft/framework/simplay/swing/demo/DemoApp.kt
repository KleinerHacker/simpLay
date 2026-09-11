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

import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Demo application for the `swing` module. Hosts a [JTabbedPane] with the tabs `Image` and
 * `Paper Sheet`: the `Image` tab shows [org.pcsoft.framework.simplay.swing.DocumentImageRenderer]
 * via [ImageDemoPanel]; the `Paper Sheet` tab shows the
 * [org.pcsoft.framework.simplay.swing.PaperSheetView] via [PaperSheetDemoPanel], whose own mode
 * selector switches between the [org.pcsoft.framework.simplay.swing.PaperSheetMode] levels.
 */
fun main() {
    SwingUtilities.invokeLater {
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

        val tabs = JTabbedPane().apply {
            addTab("Image", ImageDemoPanel())
            addTab("Paper Sheet", PaperSheetDemoPanel())
        }

        JFrame("SimpLay - swing demo").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            contentPane = tabs
            preferredSize = Dimension(1024, 768)
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}
