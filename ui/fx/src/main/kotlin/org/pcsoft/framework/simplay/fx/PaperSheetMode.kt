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

import org.pcsoft.framework.simplay.uicommon.PageMode

/**
 * The interaction mode of a [PaperSheetView]. Each constant is a strict superset of the previous one:
 * [STATIC] only paints, [SELECTABLE] adds text selection, [NAVIGABLE] adds the caret and
 * [EDITABLE] adds the document mutations.
 *
 * @property supportsSelection whether text can be selected and copied.
 * @property supportsCaret whether a caret is placed, painted and navigated.
 * @property supportsEditing whether input replaces [PaperSheetView.document] with a new instance.
 * @property supportsFocus whether the view takes keyboard focus.
 */
enum class PaperSheetMode(
    val supportsSelection: Boolean,
    val supportsCaret: Boolean,
    val supportsEditing: Boolean,
    val supportsFocus: Boolean,
) {

    /**
     * The document behaves like an image: no selection, no caret, no editing, no floating overlays,
     * the default arrow mouse cursor and no keyboard focus. Zooming, scrolling and the
     * [PaperSheetView.hoveredParagraph] / [PaperSheetView.hoveredPage] readouts still work.
     */
    STATIC(supportsSelection = false, supportsCaret = false, supportsEditing = false, supportsFocus = false),

    /**
     * Selectable and copyable text, no caret. The view never mutates its [PaperSheetView.document].
     */
    SELECTABLE(supportsSelection = true, supportsCaret = false, supportsEditing = false, supportsFocus = true),

    /**
     * Everything [SELECTABLE] offers plus a blinking caret and the standard caret navigation keys
     * (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` / `Ctrl+Right`, each optionally
     * with `Shift`). The view still never mutates its [PaperSheetView.document].
     */
    NAVIGABLE(supportsSelection = true, supportsCaret = true, supportsEditing = false, supportsFocus = true),

    /**
     * Everything [NAVIGABLE] offers plus character insertion and removal, clipboard cut / copy /
     * paste, line duplication and drag-and-drop of the selection. Editing produces a new
     * [PaperSheetView.document]; the previous document instance is not changed.
     */
    EDITABLE(supportsSelection = true, supportsCaret = true, supportsEditing = true, supportsFocus = true),
}

/** The [PageMode] a page without its own override effectively uses when the view is in [this] mode. */
internal fun PaperSheetMode.asPageMode(): PageMode = when (this) {
    PaperSheetMode.STATIC -> PageMode.STATIC
    PaperSheetMode.SELECTABLE -> PageMode.SELECTABLE
    PaperSheetMode.NAVIGABLE -> PageMode.NAVIGABLE
    PaperSheetMode.EDITABLE -> PageMode.EDITABLE
}
