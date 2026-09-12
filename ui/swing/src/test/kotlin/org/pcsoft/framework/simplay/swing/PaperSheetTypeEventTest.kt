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

package org.pcsoft.framework.simplay.swing

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Headless tests for [PaperSheetView.onType]: the listener fired right after a character was typed
 * into an editable view, carrying the character plus the raw text part, block and page it landed in.
 */
class PaperSheetTypeEventTest {

    private fun ui(view: PaperSheetView): BasicPaperSheetUI = view.getPaperSheetUI() as BasicPaperSheetUI

    private fun paint(ui: BasicPaperSheetUI, width: Int = 500, height: Int = 300) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            ui.paintForTest(g, width, height)
        } finally {
            g.dispose()
        }
    }

    /**
     * Typing a single character fires [PaperSheetView.onType] once with that character and a
     * non-`null` text part, block and page.
     */
    @Test
    fun typingFiresOnTypeWithStructuralContext() {
        val view = PaperSheetView().apply { mode = PaperSheetMode.EDITABLE; document = TestDocuments.short }
        val delegate = ui(view)
        paint(delegate)

        var event: PaperSheetTypeEvent? = null
        var callCount = 0
        view.onType = PaperSheetTypeListener { e -> event = e; callCount++ }
        delegate.typeTextForTest("X")

        assertEquals(1, callCount)
        val fired = assertNotNull(event)
        assertEquals('X', fired.character)
        assertNotNull(fired.textPart)
        assertNotNull(fired.textBlock)
        assertNotNull(fired.page)
    }

    /** A read-only view (no editing support) never fires [PaperSheetView.onType]. */
    @Test
    fun readOnlyViewNeverFiresOnType() {
        val view = PaperSheetView().apply { mode = PaperSheetMode.NAVIGABLE; document = TestDocuments.short }
        val delegate = ui(view)
        paint(delegate)

        var callCount = 0
        view.onType = PaperSheetTypeListener { callCount++ }
        delegate.typeTextForTest("Z")

        assertEquals(0, callCount)
    }
}
