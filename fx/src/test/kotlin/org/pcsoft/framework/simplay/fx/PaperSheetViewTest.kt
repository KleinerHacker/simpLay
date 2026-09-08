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
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Headless tests for the public [PaperSheetView] control: its properties, the zoom clamping and the
 * read-only outputs.
 */
class PaperSheetViewTest : JavaFxTestBase() {

    private fun realizedView(): PaperSheetView = onFxThread {
        val view = PaperSheetView()
        val stage = Stage()
        stage.scene = Scene(view, 400.0, 320.0)
        stage.show()
        view.applyCss()
        view.layout()
        view
    }

    /**
     * The default zoom sits inside the default zoom range and the default range itself is valid
     * (`minZoom < maxZoom`).
     */
    @Test
    fun defaultsFormAValidZoomRange() {
        val view = onFxThread { PaperSheetView() }

        assertTrue(view.minZoom < view.maxZoom)
        assertTrue(view.zoom in view.minZoom..view.maxZoom)
    }

    /**
     * Assigning a zoom above `maxZoom` or below `minZoom` snaps it back onto the nearest bound.
     */
    @Test
    fun zoomIsClampedToMinAndMax() {
        val view = onFxThread { PaperSheetView() }

        onFxThread { view.zoom = 999.0 }
        assertEquals(view.maxZoom, view.zoom, 1e-9)

        onFxThread { view.zoom = 0.0001 }
        assertEquals(view.minZoom, view.zoom, 1e-9)
    }

    /**
     * Narrowing `maxZoom` below the current zoom pulls the current zoom down to the new maximum.
     */
    @Test
    fun zoomIsReclampedWhenMaxZoomNarrows() {
        val view = onFxThread { PaperSheetView() }
        onFxThread { view.zoom = 3.0 }

        onFxThread { view.maxZoom = 2.0 }

        assertEquals(2.0, view.zoom, 1e-9)
    }

    /**
     * Setting a document updates `contentSize` to a non-empty extent; clearing it back to `null`
     * returns `contentSize` to zero without raising an error.
     */
    @Test
    fun settingAndClearingDocumentUpdatesContentSize() {
        val view = realizedView()

        assertEquals(0.0, view.contentSize.width, 1e-9)
        assertEquals(0.0, view.contentSize.height, 1e-9)

        onFxThread { view.document = PaperSheetTestFixtures.flowDocument(6) }
        assertTrue(view.contentSize.width > 0.0)
        assertTrue(view.contentSize.height > 0.0)

        onFxThread { view.document = null }
        assertEquals(0.0, view.contentSize.width, 1e-9)
        assertEquals(0.0, view.contentSize.height, 1e-9)
    }

    /**
     * A larger page gap makes the stacked content taller; the width stays unchanged.
     */
    @Test
    fun pageGapAffectsContentSize() {
        val view = realizedView()
        onFxThread { view.document = PaperSheetTestFixtures.flowDocument(30) }

        val before = view.contentSize
        onFxThread { view.pageGap = view.pageGap + 40.0 }
        val after = view.contentSize

        assertTrue(after.height > before.height)
        assertEquals(before.width, after.width, 1e-9)
    }

    /**
     * A `null` document yields an empty, error-free view: no rendered pages and no selection bounds.
     */
    @Test
    fun nullDocumentShowsEmptyViewport() {
        val view = realizedView()

        val skin = view.skin as PaperSheetViewSkin
        assertTrue(skin.renderedPageIndices.isEmpty())
        assertEquals(0, skin.sheetChromeDrawCount)
        assertNull(view.selectionBounds)
        assertEquals("", view.selectedText)
    }

    /**
     * The view's `selectedText` mirrors the `text` of its selection model.
     */
    @Test
    fun selectedTextDelegatesToSelectionModel() {
        val view = realizedView()
        onFxThread { view.document = PaperSheetTestFixtures.flowDocument(3) }

        onFxThread { view.selectionModel.selectAll() }

        assertTrue(view.selectedText.isNotEmpty())
        assertEquals(view.selectionModel.text, view.selectedText)
    }

    /**
     * The selection model is the same instance for the whole life of the view, across document
     * changes.
     */
    @Test
    fun selectionModelInstanceIsStable() {
        val view = realizedView()
        val model = view.selectionModel

        onFxThread { view.document = PaperSheetTestFixtures.flowDocument(2) }

        assertSame(model, view.selectionModel)
    }
}
