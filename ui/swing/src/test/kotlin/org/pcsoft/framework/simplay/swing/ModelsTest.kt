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

import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * Tests for the observable [TextSelectionModel] and [CaretModel] plus their command dispatch.
 */
class ModelsTest {

    /**
     * Verifies that [TextSelectionModel.update] recomputes the derived `length` and `empty` fields
     * and fires a `PropertyChangeEvent` for the text property.
     */
    @Test
    fun selectionModelUpdateRecomputesDerivedFieldsAndNotifies() {
        val model = TextSelectionModel { }
        var textEvents = 0
        model.addPropertyChangeListener(TextSelectionModel.PROP_TEXT) { textEvents++ }
        model.update("hello", 2, 7, Rectangle(1, 2, 3, 4), emptyList())
        assertEquals(5, model.length)
        assertFalse(model.isEmpty)
        assertEquals(1, textEvents)
    }

    /**
     * Verifies that the [TextSelectionModel] command methods are forwarded to the registered
     * [TextSelectionModel.Commands] sink.
     */
    @Test
    fun selectionModelCommandsReachTheSink() {
        val calls = mutableListOf<String>()
        val sink = object : TextSelectionModel.Commands {
            override fun selectRange(start: Int, end: Int) { calls += "range $start $end" }
            override fun selectAll() { calls += "all" }
            override fun clearSelection() { calls += "clear" }
        }
        val model = TextSelectionModel { block -> sink.block() }
        model.selectRange(1, 4)
        model.selectAll()
        model.clearSelection()
        assertEquals(listOf("range 1 4", "all", "clear"), calls)
    }

    /**
     * Verifies that [CaretModel.update] and [CaretModel.updateCounts] publish their new values and
     * notify listeners for the position and block-count properties.
     */
    @Test
    fun caretModelPublishesPositionAndCounts() {
        val model = CaretModel { }
        var positionEvents = 0
        model.addPropertyChangeListener(CaretModel.PROP_POSITION) { positionEvents++ }
        model.update(9, Rectangle(0, 0, 2, 10), true, null, null, null, null)
        model.updateCounts(3, 12, 4)
        assertEquals(9, model.position)
        assertTrue(model.isVisible)
        assertEquals(3, model.blockCount)
        assertEquals(1, positionEvents)
    }

    /**
     * Verifies that [CaretModel.update] also publishes the current text part, block, page and
     * character, and notifies a listener registered on [CaretModel.PROP_CURRENT_CHARACTER].
     */
    @Test
    fun caretModelPublishesCurrentStructuralElements() {
        val style = TextStyle(font = Font(family = "SansSerif", size = 12.0))
        val block = TextBlock.of("hello", style)
        val page = FlowPage(layout = TestDocuments.layout, blocks = listOf(block))
        val part = TextWord("hello")

        val model = CaretModel { }
        var characterEvents = 0
        model.addPropertyChangeListener(CaretModel.PROP_CURRENT_CHARACTER) { characterEvents++ }
        model.update(0, null, false, part, block, page, 'h')

        assertEquals(part, model.currentTextPart)
        assertEquals(block, model.currentTextBlock)
        assertEquals(page, model.currentPage)
        assertEquals('h', model.currentCharacter)
        assertEquals(1, characterEvents)

        model.update(5, null, false, null, null, null, null)
        assertNull(model.currentTextPart)
        assertNull(model.currentCharacter)
    }

    /**
     * Verifies that a [CaretModel] navigation command is forwarded to the registered
     * [CaretModel.Commands] sink.
     */
    @Test
    fun caretModelCommandsReachTheSink() {
        var moved = -1
        val sink = object : CaretModel.Commands {
            override fun moveTo(index: Int) { moved = index }
            override fun moveToStart() {}
            override fun moveToEnd() {}
            override fun moveIntoBlock(block: Int, index: Int) {}
            override fun moveToStartOfBlock(block: Int) {}
            override fun moveToEndOfBlock(block: Int) {}
            override fun moveIntoWord(word: Int, index: Int) {}
            override fun moveToStartOfWord(word: Int) {}
            override fun moveToEndOfWord(word: Int) {}
            override fun moveToSymbol(symbol: Int) {}
            override fun moveToStartOfSymbol(symbol: Int) {}
            override fun moveToEndOfSymbol(symbol: Int) {}
            override fun moveToNextWord() {}
            override fun moveToPrevWord() {}
            override fun moveToNextBlock() {}
            override fun moveToPrevBlock() {}
            override fun moveToNextSymbol() {}
            override fun moveToPrevSymbol() {}
            override fun moveToNextPage() {}
            override fun moveToPrevPage() {}
        }
        val model = CaretModel { block -> sink.block() }
        model.moveTo(42)
        assertEquals(42, moved)
    }
}
