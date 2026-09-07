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

package org.pcsoft.framework.simplay.fx.control

import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.pcsoft.framework.simplay.fx.JavaFxTestBase

/**
 * Headless tests for [TextSelectionModel]: the read-only selection state it reports (text, character
 * range, bounds, styled runs) and its `selectRange` / `selectAll` / `clearSelection` commands,
 * including a command issued before the view has a skin.
 */
class TextSelectionModelTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    private fun fixture(paragraphs: Int = 6): Fixture = onFxThread {
        val view = PaperSheetView()
        val stage = Stage()
        stage.scene = Scene(view, 320.0, 260.0)
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(paragraphs)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /**
     * With a document but no selection every field of the model reports the empty state.
     */
    @Test
    fun modelIsEmptyWithoutSelection() {
        val model = fixture().view.selectionModel

        assertTrue(model.isEmpty)
        assertEquals("", model.text)
        assertEquals(0, model.length)
        assertEquals(0, model.startIndex)
        assertEquals(0, model.endIndex)
        assertNull(model.bounds)
        assertTrue(model.runs.isEmpty())
    }

    /**
     * A mouse drag over one line fills the plain text, a consistent character range and at least one
     * styled run.
     */
    @Test
    fun dragSelectionFillsTextRangeAndRuns() {
        val (view, skin) = fixture()

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }
        val model = view.selectionModel

        assertFalse(model.isEmpty)
        assertTrue(model.text.isNotEmpty())
        assertTrue(model.startIndex < model.endIndex)
        assertEquals(model.endIndex - model.startIndex, model.length)
        assertTrue(model.runs.isNotEmpty())
    }

    /**
     * Each styled run carries the font of the block it belongs to: the fixture's `Serif` 14 pt,
     * neither bold nor italic.
     */
    @Test
    fun runsCarryFontFamilySizeBoldItalic() {
        val (view, skin) = fixture()

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }
        val run = view.selectionModel.runs.first()

        assertEquals("Serif", run.fontFamily)
        assertEquals(14.0, run.fontSize, 1e-9)
        assertFalse(run.bold)
        assertFalse(run.italic)
    }

    /**
     * The model's `bounds` are the same box the view exposes through its `selectionBounds` delegate.
     */
    @Test
    fun boundsEqualViewSelectionBounds() {
        val (view, skin) = fixture()

        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }

        val bounds = assertNotNull(view.selectionModel.bounds)
        assertTrue(bounds.width > 0.0 && bounds.height > 0.0)
        assertEquals(view.selectionBounds, view.selectionModel.bounds)
    }

    /**
     * `selectAll` selects the whole linear document text, starting at index `0`.
     */
    @Test
    fun selectAllSelectsWholeDocument() {
        val model = fixture(paragraphs = 3).view.selectionModel

        onFxThread { model.selectAll() }

        assertEquals(0, model.startIndex)
        assertEquals(model.text.length, model.length)
        assertTrue(model.text.length > PaperSheetTestFixtures.PARAGRAPH.length)
        assertTrue(model.runs.isNotEmpty())
    }

    /**
     * `selectRange` with out-of-range indices is clamped to the document bounds instead of failing,
     * ending up equal to a full `selectAll`.
     */
    @Test
    fun selectRangeClampsToDocumentBounds() {
        val model = fixture(paragraphs = 3).view.selectionModel
        onFxThread { model.selectAll() }
        val whole = model.text

        onFxThread { model.selectRange(-100, Int.MAX_VALUE) }

        assertEquals(0, model.startIndex)
        assertEquals(whole.length, model.endIndex)
        assertEquals(whole, model.text)
    }

    /**
     * `clearSelection` empties the model and, through the delegates, the view's `selectedText` and
     * `selectionBounds`.
     */
    @Test
    fun clearSelectionEmptiesModelAndView() {
        val (view, skin) = fixture()
        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }

        onFxThread { view.selectionModel.clearSelection() }

        assertTrue(view.selectionModel.isEmpty)
        assertTrue(view.selectionModel.runs.isEmpty())
        assertEquals("", view.selectedText)
        assertNull(view.selectionBounds)
    }

    /**
     * Replacing the document drops the current selection.
     */
    @Test
    fun documentChangeResetsModel() {
        val (view, skin) = fixture()
        onFxThread { skin.selectByPointsForTest(57.0, 57.0, 250.0, 57.0) }
        assertFalse(view.selectionModel.isEmpty)

        onFxThread { view.document = PaperSheetTestFixtures.flowDocument(2) }

        assertTrue(view.selectionModel.isEmpty)
        assertTrue(view.selectionModel.runs.isEmpty())
    }

    /**
     * A selection command issued before the view has a skin is buffered and applied once the skin is
     * created.
     */
    @Test
    fun commandBeforeSkinIsAppliedOnSkinInit() {
        val view = onFxThread {
            val v = PaperSheetView()
            v.document = PaperSheetTestFixtures.flowDocument(2)
            v.selectionModel.selectAll()
            v
        }

        assertTrue(view.selectionModel.isEmpty, "no skin yet, so nothing is applied")

        onFxThread {
            val stage = Stage()
            stage.scene = Scene(view, 320.0, 260.0)
            stage.show()
            view.applyCss()
            view.layout()
        }

        assertFalse(view.selectionModel.isEmpty)
        assertTrue(view.selectionModel.text.isNotEmpty())
    }
}
