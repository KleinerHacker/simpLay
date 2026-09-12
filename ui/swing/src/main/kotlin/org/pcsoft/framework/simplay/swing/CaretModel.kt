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
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import org.pcsoft.framework.simplay.engine.model.Page
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * The observable caret state of a [PaperSheetView] plus the commands to move it. One instance lives
 * for the whole life of its view and is reached through [PaperSheetView.caretModel]. The Swing
 * counterpart of the `fx` module's `CaretModel`.
 *
 * All state is read-only; the view's UI is the only writer. The [position] is the caret offset in
 * the document's linear text (the same axis [TextSelectionModel] uses), [bounds] is the caret
 * rectangle in viewport pixels while the view is in [PaperSheetMode.EDITABLE] (else `null`) and
 * [isVisible] follows the blink phase. [blockCount], [wordCount] and [symbolCount] report how many
 * of each structural element the current document has. [currentTextPart], [currentTextBlock] and
 * [currentPage] report the raw model elements the caret currently sits in or next to, and
 * [currentCharacter] the character right at [position]; all four are `null` without a document or
 * once [position] is past the last character.
 *
 * The commands come in three groups: linear ([moveTo], [moveToStart], [moveToEnd]); absolute
 * structural, addressing a zero-based ordinal; and relative structural from the current position. A
 * command issued before the view has a UI is applied once the UI is attached.
 */
class CaretModel internal constructor(private val dispatch: (Commands.() -> Unit) -> Unit) {

    /** Sink for the model commands, implemented and registered by the UI delegate. */
    internal interface Commands {
        fun moveTo(index: Int)
        fun moveToStart()
        fun moveToEnd()
        fun moveIntoBlock(block: Int, index: Int)
        fun moveToStartOfBlock(block: Int)
        fun moveToEndOfBlock(block: Int)
        fun moveIntoWord(word: Int, index: Int)
        fun moveToStartOfWord(word: Int)
        fun moveToEndOfWord(word: Int)
        fun moveToSymbol(symbol: Int)
        fun moveToStartOfSymbol(symbol: Int)
        fun moveToEndOfSymbol(symbol: Int)
        fun moveToNextWord()
        fun moveToPrevWord()
        fun moveToNextBlock()
        fun moveToPrevBlock()
        fun moveToNextSymbol()
        fun moveToPrevSymbol()
        fun moveToNextPage()
        fun moveToPrevPage()
    }

    private val pcs = PropertyChangeSupport(this)

    var position: Int = 0
        private set

    var bounds: Rectangle? = null
        private set

    var isVisible: Boolean = false
        private set

    var blockCount: Int = 0
        private set

    var wordCount: Int = 0
        private set

    var symbolCount: Int = 0
        private set

    /** The raw text part the caret currently sits in or next to, or `null` without a document. */
    var currentTextPart: TextPart? = null
        private set

    /** The raw block the caret currently sits in, or `null` without a document. */
    var currentTextBlock: TextBlock? = null
        private set

    /** The raw page the caret currently sits on, or `null` without a document. */
    var currentPage: Page? = null
        private set

    /** The character right at [position], or `null` at the end of the document or without one. */
    var currentCharacter: Char? = null
        private set

    fun addPropertyChangeListener(listener: PropertyChangeListener) = pcs.addPropertyChangeListener(listener)

    fun addPropertyChangeListener(propertyName: String, listener: PropertyChangeListener) =
        pcs.addPropertyChangeListener(propertyName, listener)

    fun removePropertyChangeListener(listener: PropertyChangeListener) = pcs.removePropertyChangeListener(listener)

    fun removePropertyChangeListener(propertyName: String, listener: PropertyChangeListener) =
        pcs.removePropertyChangeListener(propertyName, listener)

    /** Replaces the caret position, bounds, blink state and current structural elements in one step.
     * Called by the UI only. */
    internal fun update(
        position: Int,
        bounds: Rectangle?,
        visible: Boolean,
        textPart: TextPart?,
        textBlock: TextBlock?,
        page: Page?,
        character: Char?,
    ) {
        val oldPos = this.position
        val oldBounds = this.bounds
        val oldVisible = this.isVisible
        val oldTextPart = this.currentTextPart
        val oldTextBlock = this.currentTextBlock
        val oldPage = this.currentPage
        val oldCharacter = this.currentCharacter
        this.position = position
        this.bounds = bounds
        this.isVisible = visible
        this.currentTextPart = textPart
        this.currentTextBlock = textBlock
        this.currentPage = page
        this.currentCharacter = character
        pcs.firePropertyChange(PROP_POSITION, oldPos, position)
        pcs.firePropertyChange(PROP_BOUNDS, oldBounds, bounds)
        pcs.firePropertyChange(PROP_VISIBLE, oldVisible, visible)
        pcs.firePropertyChange(PROP_CURRENT_TEXT_PART, oldTextPart, textPart)
        pcs.firePropertyChange(PROP_CURRENT_TEXT_BLOCK, oldTextBlock, textBlock)
        pcs.firePropertyChange(PROP_CURRENT_PAGE, oldPage, page)
        pcs.firePropertyChange(PROP_CURRENT_CHARACTER, oldCharacter, character)
    }

    /** Replaces the structural element counts. Called by the UI after every re-measure. */
    internal fun updateCounts(blocks: Int, words: Int, symbols: Int) {
        val oldBlocks = blockCount
        val oldWords = wordCount
        val oldSymbols = symbolCount
        blockCount = blocks
        wordCount = words
        symbolCount = symbols
        pcs.firePropertyChange(PROP_BLOCK_COUNT, oldBlocks, blocks)
        pcs.firePropertyChange(PROP_WORD_COUNT, oldWords, words)
        pcs.firePropertyChange(PROP_SYMBOL_COUNT, oldSymbols, symbols)
    }

    /** Moves the caret to [index] in the linear document text; the index is clamped into range. */
    fun moveTo(index: Int) = dispatch { moveTo(index) }

    /** Moves the caret to the start of the document. */
    fun moveToStart() = dispatch { moveToStart() }

    /** Moves the caret to the end of the document. */
    fun moveToEnd() = dispatch { moveToEnd() }

    /** Moves the caret [index] characters into block [block] (both clamped into range). */
    fun moveIntoBlock(block: Int, index: Int) = dispatch { moveIntoBlock(block, index) }

    /** Moves the caret to the first character of block [block] (clamped into range). */
    fun moveToStartOfBlock(block: Int) = dispatch { moveToStartOfBlock(block) }

    /** Moves the caret past the last character of block [block] (clamped into range). */
    fun moveToEndOfBlock(block: Int) = dispatch { moveToEndOfBlock(block) }

    /** Moves the caret [index] characters into word [word] (both clamped into range). */
    fun moveIntoWord(word: Int, index: Int) = dispatch { moveIntoWord(word, index) }

    /** Moves the caret to the first character of word [word] (clamped into range). */
    fun moveToStartOfWord(word: Int) = dispatch { moveToStartOfWord(word) }

    /** Moves the caret past the last character of word [word] (clamped into range). */
    fun moveToEndOfWord(word: Int) = dispatch { moveToEndOfWord(word) }

    /** Moves the caret in front of symbol [symbol] (clamped into range). */
    fun moveToSymbol(symbol: Int) = dispatch { moveToSymbol(symbol) }

    /** Moves the caret in front of symbol [symbol] (clamped into range). */
    fun moveToStartOfSymbol(symbol: Int) = dispatch { moveToStartOfSymbol(symbol) }

    /** Moves the caret past symbol [symbol] (clamped into range). */
    fun moveToEndOfSymbol(symbol: Int) = dispatch { moveToEndOfSymbol(symbol) }

    /** Moves the caret to the start of the next word; stays put at the document end. */
    fun moveToNextWord() = dispatch { moveToNextWord() }

    /** Moves the caret to the start of the previous word; stays put at the document start. */
    fun moveToPrevWord() = dispatch { moveToPrevWord() }

    /** Moves the caret to the start of the next block; stays put at the document end. */
    fun moveToNextBlock() = dispatch { moveToNextBlock() }

    /** Moves the caret to the start of the previous block; stays put at the document start. */
    fun moveToPrevBlock() = dispatch { moveToPrevBlock() }

    /** Moves the caret to the next symbol; stays put at the document end. */
    fun moveToNextSymbol() = dispatch { moveToNextSymbol() }

    /** Moves the caret to the previous symbol; stays put at the document start. */
    fun moveToPrevSymbol() = dispatch { moveToPrevSymbol() }

    /**
     * Moves the caret to the next navigable page, at the same line ordinal it held on the source
     * page (clamped to the target page's last line); stays put on the last navigable page.
     */
    fun moveToNextPage() = dispatch { moveToNextPage() }

    /**
     * Moves the caret to the previous navigable page, at the same line ordinal it held on the source
     * page (clamped to the target page's last line); stays put on the first navigable page.
     */
    fun moveToPrevPage() = dispatch { moveToPrevPage() }

    companion object {
        const val PROP_POSITION = "position"
        const val PROP_BOUNDS = "bounds"
        const val PROP_VISIBLE = "visible"
        const val PROP_BLOCK_COUNT = "blockCount"
        const val PROP_WORD_COUNT = "wordCount"
        const val PROP_SYMBOL_COUNT = "symbolCount"
        const val PROP_CURRENT_TEXT_PART = "currentTextPart"
        const val PROP_CURRENT_TEXT_BLOCK = "currentTextBlock"
        const val PROP_CURRENT_PAGE = "currentPage"
        const val PROP_CURRENT_CHARACTER = "currentCharacter"
    }
}
