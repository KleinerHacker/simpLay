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

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.geometry.Bounds

/**
 * The observable caret state of a [PaperSheetView] plus the commands to move it. One instance lives
 * for the whole life of its view and is reached through [PaperSheetView.getCaretModel].
 *
 * All state is read-only; the view's skin is the only writer. The [position] is the caret offset in
 * the document's linear text (the same axis [TextSelectionModel] uses), [bounds] is the caret
 * rectangle in viewport pixels while the view is in [PaperSheetMode.EDITABLE] (else `null`) and
 * [isVisible] follows the blink phase. [blockCount], [wordCount] and [symbolCount] report how many
 * of each structural element the current document has, so a caller can pick a valid ordinal for the
 * structural move commands.
 *
 * The commands come in three groups: linear ([moveTo], [moveToStart], [moveToEnd]); absolute
 * structural, addressing a zero-based ordinal ([moveIntoBlock] / [moveToStartOfBlock] /
 * [moveToEndOfBlock] and the [moveIntoWord] / [moveToSymbol] siblings), with every ordinal and
 * offset clamped into range; and relative structural from the current position ([moveToNextWord] /
 * [moveToPrevWord] and the block / symbol siblings), which stop at the document bounds. A command
 * issued before the view has a skin is applied once the skin is attached.
 *
 * Not to be confused with the module-internal caret bookkeeping the skin keeps for rendering and
 * navigation; this model is the public surface.
 */
class CaretModel internal constructor(private val view: PaperSheetView) {

    //region Read-only state

    private val positionWrapper = ReadOnlyIntegerWrapper(this, "position", 0)

    /** The [position] property. */
    @get:JvmName("positionProperty")
    val positionProperty: ReadOnlyIntegerProperty
        get() = positionWrapper.readOnlyProperty

    /** The caret offset in the document's linear text. */
    val position: Int get() = positionWrapper.get()

    private val boundsWrapper = ReadOnlyObjectWrapper<Bounds?>(this, "bounds", null)

    /** The [bounds] property. */
    @get:JvmName("boundsProperty")
    val boundsProperty: ReadOnlyObjectProperty<Bounds?>
        get() = boundsWrapper.readOnlyProperty

    /** The caret rectangle in viewport pixels while the view edits, or `null` in read-only mode. */
    val bounds: Bounds? get() = boundsWrapper.get()

    private val visibleWrapper = ReadOnlyBooleanWrapper(this, "visible", false)

    /** The [isVisible] property. */
    @get:JvmName("visibleProperty")
    val visibleProperty: ReadOnlyBooleanProperty
        get() = visibleWrapper.readOnlyProperty

    /** `true` while the caret is currently painted (blink on and the view is in normal mode). */
    val isVisible: Boolean get() = visibleWrapper.get()

    private val blockCountWrapper = ReadOnlyIntegerWrapper(this, "blockCount", 0)

    /** The [blockCount] property. */
    @get:JvmName("blockCountProperty")
    val blockCountProperty: ReadOnlyIntegerProperty
        get() = blockCountWrapper.readOnlyProperty

    /** Number of addressable blocks (paragraphs) in the current document. */
    val blockCount: Int get() = blockCountWrapper.get()

    private val wordCountWrapper = ReadOnlyIntegerWrapper(this, "wordCount", 0)

    /** The [wordCount] property. */
    @get:JvmName("wordCountProperty")
    val wordCountProperty: ReadOnlyIntegerProperty
        get() = wordCountWrapper.readOnlyProperty

    /** Number of addressable words in the current document. */
    val wordCount: Int get() = wordCountWrapper.get()

    private val symbolCountWrapper = ReadOnlyIntegerWrapper(this, "symbolCount", 0)

    /** The [symbolCount] property. */
    @get:JvmName("symbolCountProperty")
    val symbolCountProperty: ReadOnlyIntegerProperty
        get() = symbolCountWrapper.readOnlyProperty

    /** Number of addressable symbols (single non-word characters) in the current document. */
    val symbolCount: Int get() = symbolCountWrapper.get()

    //endregion

    //region Skin-only writer

    /** Replaces the caret position, bounds and blink state in one step. Called by the skin only. */
    internal fun update(position: Int, bounds: Bounds?, visible: Boolean) {
        positionWrapper.set(position)
        boundsWrapper.set(bounds)
        visibleWrapper.set(visible)
    }

    /** Replaces the structural element counts. Called by the skin after every re-measure. */
    internal fun updateCounts(blocks: Int, words: Int, symbols: Int) {
        blockCountWrapper.set(blocks)
        wordCountWrapper.set(words)
        symbolCountWrapper.set(symbols)
    }

    //endregion

    //region Linear commands

    /** Moves the caret to [index] in the linear document text; the index is clamped into range. */
    fun moveTo(index: Int) = view.requestCaret { moveTo(index) }

    /** Moves the caret to the start of the document. */
    fun moveToStart() = view.requestCaret { moveToStart() }

    /** Moves the caret to the end of the document. */
    fun moveToEnd() = view.requestCaret { moveToEnd() }

    //endregion

    //region Absolute structural commands

    /** Moves the caret [index] characters into block [block] (both clamped into range). */
    fun moveIntoBlock(block: Int, index: Int) = view.requestCaret { moveIntoBlock(block, index) }

    /** Moves the caret to the first character of block [block] (clamped into range). */
    fun moveToStartOfBlock(block: Int) = view.requestCaret { moveToStartOfBlock(block) }

    /** Moves the caret past the last character of block [block] (clamped into range). */
    fun moveToEndOfBlock(block: Int) = view.requestCaret { moveToEndOfBlock(block) }

    /** Moves the caret [index] characters into word [word] (both clamped into range). */
    fun moveIntoWord(word: Int, index: Int) = view.requestCaret { moveIntoWord(word, index) }

    /** Moves the caret to the first character of word [word] (clamped into range). */
    fun moveToStartOfWord(word: Int) = view.requestCaret { moveToStartOfWord(word) }

    /** Moves the caret past the last character of word [word] (clamped into range). */
    fun moveToEndOfWord(word: Int) = view.requestCaret { moveToEndOfWord(word) }

    /** Moves the caret in front of symbol [symbol] (clamped into range). */
    fun moveToSymbol(symbol: Int) = view.requestCaret { moveToSymbol(symbol) }

    /** Moves the caret in front of symbol [symbol] (clamped into range). */
    fun moveToStartOfSymbol(symbol: Int) = view.requestCaret { moveToStartOfSymbol(symbol) }

    /** Moves the caret past symbol [symbol] (clamped into range). */
    fun moveToEndOfSymbol(symbol: Int) = view.requestCaret { moveToEndOfSymbol(symbol) }

    //endregion

    //region Relative structural commands

    /** Moves the caret to the start of the next word; stays put at the document end. */
    fun moveToNextWord() = view.requestCaret { moveToNextWord() }

    /** Moves the caret to the start of the previous word; stays put at the document start. */
    fun moveToPrevWord() = view.requestCaret { moveToPrevWord() }

    /** Moves the caret to the start of the next block; stays put at the document end. */
    fun moveToNextBlock() = view.requestCaret { moveToNextBlock() }

    /** Moves the caret to the start of the previous block; stays put at the document start. */
    fun moveToPrevBlock() = view.requestCaret { moveToPrevBlock() }

    /** Moves the caret to the next symbol; stays put at the document end. */
    fun moveToNextSymbol() = view.requestCaret { moveToNextSymbol() }

    /** Moves the caret to the previous symbol; stays put at the document start. */
    fun moveToPrevSymbol() = view.requestCaret { moveToPrevSymbol() }

    /**
     * Moves the caret to the next navigable page, at the same line ordinal it held on the source
     * page (clamped to the target page's last line); stays put on the last navigable page.
     */
    fun moveToNextPage() = view.requestCaret { moveToNextPage() }

    /**
     * Moves the caret to the previous navigable page, at the same line ordinal it held on the source
     * page (clamped to the target page's last line); stays put on the first navigable page.
     */
    fun moveToPrevPage() = view.requestCaret { moveToPrevPage() }

    //endregion
}
