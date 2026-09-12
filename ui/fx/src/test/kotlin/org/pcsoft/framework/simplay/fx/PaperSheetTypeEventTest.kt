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

/**
 * Headless tests for [PaperSheetView.onType]: the event fired right after a character was typed into
 * an editable view, carrying the character plus the raw text part, block and page it landed in.
 */
class PaperSheetTypeEventTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(mode: PaperSheetMode = PaperSheetMode.EDITABLE): Fixture = onFxThread {
        val view = PaperSheetView()
        view.mode = mode
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(1)
        view.applyCss()
        view.layout()
        view.requestFocus()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * Typing a single character fires [PaperSheetView.onType] once with that character and a
     * non-`null` text part, block and page.
     */
    @Test
    fun typingFiresOnTypeWithStructuralContext() {
        val (view, skin) = fixture()
        var event: PaperSheetTypeEvent? = null
        var callCount = 0
        onFxThread {
            view.onType = javafx.event.EventHandler { e -> event = e; callCount++ }
            skin.typeTextForTest("X")
        }

        assertEquals(1, callCount)
        val fired = assertNotNull(event)
        assertEquals('X', fired.character)
        assertNotNull(fired.textPart)
        assertNotNull(fired.textBlock)
        assertNotNull(fired.page)
        assertEquals(PaperSheetTypeEvent.TYPED, fired.eventType)
    }

    /**
     * Without a registered [PaperSheetView.onType] handler, typing still succeeds - the caret still
     * advances and no exception is thrown.
     */
    @Test
    fun typingWithoutHandlerDoesNotFail() {
        val (_, skin) = fixture()
        val before = skin.caretIndexForTest
        onFxThread { skin.typeTextForTest("Y") }
        assertEquals(before + 1, skin.caretIndexForTest)
    }

    /**
     * A read-only view (no editing support) never fires [PaperSheetView.onType].
     */
    @Test
    fun readOnlyViewNeverFiresOnType() {
        val (view, skin) = fixture(mode = PaperSheetMode.NAVIGABLE)
        var callCount = 0
        onFxThread {
            view.onType = javafx.event.EventHandler { callCount++ }
            skin.typeTextForTest("Z")
        }
        assertEquals(0, callCount)
    }
}
