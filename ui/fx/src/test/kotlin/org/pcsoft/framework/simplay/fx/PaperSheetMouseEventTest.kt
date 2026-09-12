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

package org.pcsoft.framework.simplay.fx

import javafx.scene.Scene
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Headless tests for [PaperSheetView.onMouseEvent]: the event fired while the mouse hovers or clicks
 * over a [PaperSheetView], carrying the raw text part, block and page under the pointer, nullable
 * over an empty area of a page or outside every page.
 */
class PaperSheetMouseEventTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(): Fixture = onFxThread {
        val view = PaperSheetView()
        view.mode = PaperSheetMode.SELECTABLE
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(1)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * A [PaperSheetMouseEvent.HOVER] over a point covered by text carries a non-`null` text part,
     * block and page.
     */
    @Test
    fun hoverOverTextReportsFullStructure() {
        val (view, skin) = fixture()
        var event: PaperSheetMouseEvent? = null
        onFxThread {
            view.onMouseEvent = javafx.event.EventHandler { e -> event = e }
            skin.fireMouseEventForTest(PaperSheetMouseEvent.HOVER, 60.0, 60.0)
        }

        val fired = assertNotNull(event)
        assertNotNull(fired.textPart)
        assertNotNull(fired.textBlock)
        assertNotNull(fired.page)
        assertEquals(PaperSheetMouseEvent.HOVER, fired.eventType)
    }

    /**
     * A [PaperSheetMouseEvent.CLICK] far outside every page carries `null` for the part, block and
     * page.
     */
    @Test
    fun clickOutsideEveryPageReportsNoStructure() {
        val (view, skin) = fixture()
        var event: PaperSheetMouseEvent? = null
        onFxThread {
            view.onMouseEvent = javafx.event.EventHandler { e -> event = e }
            skin.fireMouseEventForTest(PaperSheetMouseEvent.CLICK, 60.0, 50_000.0)
        }

        val fired = assertNotNull(event)
        assertNull(fired.textPart)
        assertNull(fired.textBlock)
        assertNull(fired.page)
        assertEquals(PaperSheetMouseEvent.CLICK, fired.eventType)
    }

    /** Without a registered [PaperSheetView.onMouseEvent] handler, firing the event is a no-op. */
    @Test
    fun withoutHandlerNothingHappens() {
        val (_, skin) = fixture()
        onFxThread { skin.fireMouseEventForTest(PaperSheetMouseEvent.HOVER, 60.0, 60.0) }
    }
}
