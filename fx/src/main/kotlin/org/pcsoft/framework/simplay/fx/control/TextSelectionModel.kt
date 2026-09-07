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

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyListProperty
import javafx.beans.property.ReadOnlyListWrapper
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Bounds

/**
 * The observable selection state of a [PaperSheetView] plus the commands to change it. One instance
 * lives for the whole life of its view and is reached through [PaperSheetView.getSelectionModel].
 *
 * All state is read-only; the view's skin is the only writer. Besides the plain [text] the model
 * exposes the character [startIndex] / [endIndex] / [length] in the document's linear text, whether
 * the selection is [isEmpty], its [bounds] in viewport pixels and the styled [runs] (one per covered
 * text part, carrying the font). [selectRange], [selectAll] and [clearSelection] drive the selection
 * from code; a command issued before the view has a skin is applied once the skin is attached.
 *
 * Not to be confused with the module-internal `TextSelection`, which is the mutable anchor/focus
 * holder the skin manipulates on mouse input.
 */
class TextSelectionModel internal constructor(private val view: PaperSheetView) {

    //region Read-only state

    private val textWrapper = ReadOnlyStringWrapper(this, "text", "")

    /** The [text] property. */
    @get:JvmName("textProperty")
    val textProperty: ReadOnlyStringProperty
        get() = textWrapper.readOnlyProperty

    /** The selected text as plain text, including the separators inside the range; `""` when empty. */
    val text: String get() = textWrapper.get()

    private val startIndexWrapper = ReadOnlyIntegerWrapper(this, "startIndex", 0)

    /** The [startIndex] property. */
    @get:JvmName("startIndexProperty")
    val startIndexProperty: ReadOnlyIntegerProperty
        get() = startIndexWrapper.readOnlyProperty

    /** The offset of the first selected character in the document's linear text. */
    val startIndex: Int get() = startIndexWrapper.get()

    private val endIndexWrapper = ReadOnlyIntegerWrapper(this, "endIndex", 0)

    /** The [endIndex] property. */
    @get:JvmName("endIndexProperty")
    val endIndexProperty: ReadOnlyIntegerProperty
        get() = endIndexWrapper.readOnlyProperty

    /** The offset one past the last selected character in the document's linear text. */
    val endIndex: Int get() = endIndexWrapper.get()

    private val lengthWrapper = ReadOnlyIntegerWrapper(this, "length", 0)

    /** The [length] property. */
    @get:JvmName("lengthProperty")
    val lengthProperty: ReadOnlyIntegerProperty
        get() = lengthWrapper.readOnlyProperty

    /** The number of selected characters, `endIndex - startIndex`. */
    val length: Int get() = lengthWrapper.get()

    private val emptyWrapper = ReadOnlyBooleanWrapper(this, "empty", true)

    /** The [isEmpty] property. */
    @get:JvmName("emptyProperty")
    val emptyProperty: ReadOnlyBooleanProperty
        get() = emptyWrapper.readOnlyProperty

    /** `true` when nothing is selected (`startIndex == endIndex`). */
    val isEmpty: Boolean get() = emptyWrapper.get()

    private val boundsWrapper = ReadOnlyObjectWrapper<Bounds?>(this, "bounds", null)

    /** The [bounds] property. */
    @get:JvmName("boundsProperty")
    val boundsProperty: ReadOnlyObjectProperty<Bounds?>
        get() = boundsWrapper.readOnlyProperty

    /** The bounding box of the current selection in viewport pixels, or `null` when empty. */
    val bounds: Bounds? get() = boundsWrapper.get()

    private val runsWrapper =
        ReadOnlyListWrapper<TextSelectionData>(this, "runs", FXCollections.observableArrayList())

    /** The [runs] property. */
    @get:JvmName("runsProperty")
    val runsProperty: ReadOnlyListProperty<TextSelectionData>
        get() = runsWrapper.readOnlyProperty

    /** The styled runs of the selection, one per covered text part; empty when nothing is selected. */
    val runs: ObservableList<TextSelectionData> get() = runsWrapper.get()

    //endregion

    //region Skin-only writer

    /** Replaces the whole selection state in one step. Called by [PaperSheetViewSkin] only. */
    internal fun update(text: String, startIndex: Int, endIndex: Int, bounds: Bounds?, runs: List<TextSelectionData>) {
        textWrapper.set(text)
        startIndexWrapper.set(startIndex)
        endIndexWrapper.set(endIndex)
        lengthWrapper.set(endIndex - startIndex)
        emptyWrapper.set(startIndex == endIndex)
        boundsWrapper.set(bounds)
        if (runsWrapper.get() != runs) runsWrapper.get().setAll(runs)
    }

    //endregion

    //region Commands

    /**
     * Selects the characters `[start, end)` of the document's linear text; the order of the two
     * indices does not matter and both are clamped to the document bounds. A no-op without a
     * document.
     */
    fun selectRange(start: Int, end: Int) = view.requestSelectRange(start, end)

    /** Selects the whole document text. A no-op without a document. */
    fun selectAll() = view.requestSelectAll()

    /** Clears the selection. */
    fun clearSelection() = view.requestClearSelection()

    //endregion
}
