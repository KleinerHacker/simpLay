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
 * The interaction mode of a [PaperSheetView].
 */
enum class PaperSheetMode {

    /**
     * Selectable and copyable text, no caret. The view never mutates its [PaperSheetView.document].
     */
    READONLY,

    /**
     * Everything [READONLY] offers plus a blinking caret, character insertion and removal, clipboard
     * cut / copy / paste, line duplication, drag-and-drop of the selection and the standard caret
     * navigation keys. Editing produces a new [PaperSheetView.document]; the previous document
     * instance is not changed.
     */
    EDITABLE,
}
