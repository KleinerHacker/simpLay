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

package org.pcsoft.framework.simplay.uicommon

/**
 * The interaction mode of a single page of a `PaperSheetView` (fx and swing).
 *
 * A page without an own mode follows the view-wide mode; a page with an own mode is completely
 * independent of it and may be both more restrictive (a read-only page inside an editable view) and
 * more permissive (an editable page inside a static view). A purely view-side switch; nothing here
 * is persisted in the `Document`.
 *
 * @property supportsSelection whether text of the page can be selected and copied.
 * @property supportsCaret whether the caret may come to rest on the page.
 * @property supportsEditing whether a mutation touching the page is applied.
 * @property laidOut whether the page stays part of layout, scroll area and hit-testing.
 * @property paintedDisabled whether the page is drawn with the special disabled fill and hatch.
 */
enum class PageMode(
    val supportsSelection: Boolean,
    val supportsCaret: Boolean,
    val supportsEditing: Boolean,
    val laidOut: Boolean,
    val paintedDisabled: Boolean,
) {

    /** The page (and its flow overflow sheets) is removed entirely from layout, scroll area and
     * hit-testing; the document itself is unchanged. */
    HIDDEN(
        supportsSelection = false,
        supportsCaret = false,
        supportsEditing = false,
        laidOut = false,
        paintedDisabled = false,
    ),

    /** The page is drawn with a special fill and hatch; no selection, no caret and no mutation. */
    DISABLED(
        supportsSelection = false,
        supportsCaret = false,
        supportsEditing = false,
        laidOut = true,
        paintedDisabled = true,
    ),

    /** The page behaves like an image: no selection, no caret, no mutation, but no special drawing. */
    STATIC(
        supportsSelection = false,
        supportsCaret = false,
        supportsEditing = false,
        laidOut = true,
        paintedDisabled = false,
    ),

    /** Selectable and copyable text, no caret and no mutation. */
    SELECTABLE(
        supportsSelection = true,
        supportsCaret = false,
        supportsEditing = false,
        laidOut = true,
        paintedDisabled = false,
    ),

    /** Everything [SELECTABLE] offers plus a caret that reaches and crosses the page; every mutation
     * touching it is still discarded. */
    NAVIGABLE(
        supportsSelection = true,
        supportsCaret = true,
        supportsEditing = false,
        laidOut = true,
        paintedDisabled = false,
    ),

    /** Everything [NAVIGABLE] offers plus the document mutations. */
    EDITABLE(
        supportsSelection = true,
        supportsCaret = true,
        supportsEditing = true,
        laidOut = true,
        paintedDisabled = false,
    ),
}
