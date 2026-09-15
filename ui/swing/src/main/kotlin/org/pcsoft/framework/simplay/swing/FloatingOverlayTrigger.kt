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

/**
 * What makes a [FloatingOverlay] of a [PaperSheetView] show up. The value is chosen per overlay and
 * never changes at runtime; the UI delegate watches the matching state and shows, positions and
 * hides the overlay's component accordingly.
 */
enum class FloatingOverlayTrigger {

    /** Active while the view has a non-empty text selection; anchored to the selection bounding box. */
    SELECTION,

    /** Active while the mouse hovers a paragraph (a measured block); anchored to that block's box. */
    PARAGRAPH_HOVER,

    /** Active while the mouse hovers a sheet; anchored to that sheet's box. */
    PAGE_HOVER,

    /** Active while an edit caret is placed; anchored to the caret rectangle. Inert outside [PaperSheetMode.EDITABLE]. */
    CARET,
}
