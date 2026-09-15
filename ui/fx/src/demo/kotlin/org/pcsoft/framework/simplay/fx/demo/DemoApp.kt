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

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.stage.Stage

/**
 * Demo application for the `fx` module. Hosts a [TabPane] with the tabs `Canvas` and `Paper Sheet`.
 * The `Canvas` tab shows the [org.pcsoft.framework.simplay.fx.CanvasDocumentRenderer] via
 * [CanvasDemoTab]; the `Paper Sheet` tab shows the
 * [org.pcsoft.framework.simplay.fx.PaperSheetView] via [PaperSheetDemoTab], whose own mode selector
 * switches between the [org.pcsoft.framework.simplay.fx.PaperSheetMode] levels.
 */
class DemoApp : Application() {

    override fun start(stage: Stage) {
        val tabs = TabPane().apply {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabs.addAll(
                Tab("Canvas", CanvasDemoTab()),
                Tab("Paper Sheet", PaperSheetDemoTab()),
            )
        }

        stage.title = "SimpLay - fx demo"
        stage.scene = Scene(tabs, 1024.0, 768.0)
        stage.show()
    }
}

/** Entry point. The main class is this file, not [DemoApp], so JavaFX starts from the classpath. */
fun main(args: Array<String>) {
    Application.launch(DemoApp::class.java, *args)
}
