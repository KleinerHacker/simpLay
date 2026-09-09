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

/**
 * The observable selection state of a [PaperSheetView] plus the commands to change it. One instance
 * lives for the whole life of its view and is reached through [PaperSheetView.selectionModel]. The
 * Swing counterpart of the `fx` module's `TextSelectionModel`.
 *
 * All state is read-only; the view's UI is the only writer. Besides the plain [text] the model
 * exposes the character [startIndex] / [endIndex] / [length] in the document's linear text, whether
 * the selection is [isEmpty], its [bounds] in viewport pixels and the styled [runs] (one per covered
 * text part, carrying the font). [selectRange], [selectAll] and [clearSelection] drive the selection
 * from code; a command issued before the view has a UI is applied once the UI is attached.
 *
 * Bound listeners are notified through a [PropertyChangeSupport]; the property names are the
 * `PROP_*` constants.
 */
class TextSelectionModel internal constructor(private val dispatch: (Commands.() -> Unit) -> Unit) {

    /** Sink for the model commands, implemented and registered by the UI delegate. */
    internal interface Commands {
        fun selectRange(start: Int, end: Int)
        fun selectAll()
        fun clearSelection()
    }

    private val pcs = PropertyChangeSupport(this)

    var text: String = ""
        private set

    var startIndex: Int = 0
        private set

    var endIndex: Int = 0
        private set

    val length: Int get() = endIndex - startIndex

    val isEmpty: Boolean get() = startIndex == endIndex

    var bounds: Rectangle? = null
        private set

    private val runsMutable = ArrayList<TextSelectionData>()

    /** The styled runs of the selection, one per covered text part; empty when nothing is selected. */
    val runs: List<TextSelectionData> get() = runsMutable.toList()

    fun addPropertyChangeListener(listener: PropertyChangeListener) = pcs.addPropertyChangeListener(listener)

    fun addPropertyChangeListener(propertyName: String, listener: PropertyChangeListener) =
        pcs.addPropertyChangeListener(propertyName, listener)

    fun removePropertyChangeListener(listener: PropertyChangeListener) = pcs.removePropertyChangeListener(listener)

    fun removePropertyChangeListener(propertyName: String, listener: PropertyChangeListener) =
        pcs.removePropertyChangeListener(propertyName, listener)

    /** Replaces the whole selection state in one step. Called by the UI delegate only. */
    internal fun update(text: String, startIndex: Int, endIndex: Int, bounds: Rectangle?, runs: List<TextSelectionData>) {
        val oldText = this.text
        val oldStart = this.startIndex
        val oldEnd = this.endIndex
        val oldLength = length
        val oldEmpty = isEmpty
        val oldBounds = this.bounds

        this.text = text
        this.startIndex = startIndex
        this.endIndex = endIndex
        this.bounds = bounds
        if (runsMutable != runs) {
            runsMutable.clear()
            runsMutable.addAll(runs)
        }

        pcs.firePropertyChange(PROP_TEXT, oldText, text)
        pcs.firePropertyChange(PROP_START_INDEX, oldStart, startIndex)
        pcs.firePropertyChange(PROP_END_INDEX, oldEnd, endIndex)
        pcs.firePropertyChange(PROP_LENGTH, oldLength, length)
        pcs.firePropertyChange(PROP_EMPTY, oldEmpty, isEmpty)
        pcs.firePropertyChange(PROP_BOUNDS, oldBounds, bounds)
        pcs.firePropertyChange(PROP_RUNS, null, runs)
    }

    /**
     * Selects the characters `[start, end)` of the document's linear text; the order of the two
     * indices does not matter and both are clamped to the document bounds. A no-op without a
     * document.
     */
    fun selectRange(start: Int, end: Int) = dispatch { selectRange(start, end) }

    /** Selects the whole document text. A no-op without a document. */
    fun selectAll() = dispatch { selectAll() }

    /** Clears the selection. */
    fun clearSelection() = dispatch { clearSelection() }

    companion object {
        const val PROP_TEXT = "text"
        const val PROP_START_INDEX = "startIndex"
        const val PROP_END_INDEX = "endIndex"
        const val PROP_LENGTH = "length"
        const val PROP_EMPTY = "empty"
        const val PROP_BOUNDS = "bounds"
        const val PROP_RUNS = "runs"
    }
}
