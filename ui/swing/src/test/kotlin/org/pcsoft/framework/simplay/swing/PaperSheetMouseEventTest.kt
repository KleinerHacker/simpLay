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
import kotlin.test.assertNull

/**
 * Headless tests for [PaperSheetView.onMouseEvent]: the listener fired while the mouse hovers or
 * clicks over a [PaperSheetView], carrying the raw text part, block and page under the pointer,
 * nullable over an empty area of a page or outside every page.
 */
class PaperSheetMouseEventTest {

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
     * A [PaperSheetMouseEvent.Kind.HOVER] over a point covered by text carries a non-`null` text
     * part, block and page.
     */
    @Test
    fun hoverOverTextReportsFullStructure() {
        val view = PaperSheetView().apply { mode = PaperSheetMode.SELECTABLE; document = TestDocuments.short }
        val delegate = ui(view)
        paint(delegate)

        var event: PaperSheetMouseEvent? = null
        view.onMouseEvent = PaperSheetMouseListener { e -> event = e }
        delegate.fireMouseEventForTest(PaperSheetMouseEvent.Kind.HOVER, 60.0, 60.0)

        val fired = assertNotNull(event)
        assertNotNull(fired.textPart)
        assertNotNull(fired.textBlock)
        assertNotNull(fired.page)
        assertEquals(PaperSheetMouseEvent.Kind.HOVER, fired.kind)
    }

    /**
     * A [PaperSheetMouseEvent.Kind.CLICK] far outside every page carries `null` for the part, block
     * and page.
     */
    @Test
    fun clickOutsideEveryPageReportsNoStructure() {
        val view = PaperSheetView().apply { mode = PaperSheetMode.SELECTABLE; document = TestDocuments.short }
        val delegate = ui(view)
        paint(delegate)

        var event: PaperSheetMouseEvent? = null
        view.onMouseEvent = PaperSheetMouseListener { e -> event = e }
        delegate.fireMouseEventForTest(PaperSheetMouseEvent.Kind.CLICK, 60.0, 50_000.0)

        val fired = assertNotNull(event)
        assertNull(fired.textPart)
        assertNull(fired.textBlock)
        assertNull(fired.page)
        assertEquals(PaperSheetMouseEvent.Kind.CLICK, fired.kind)
    }
}
