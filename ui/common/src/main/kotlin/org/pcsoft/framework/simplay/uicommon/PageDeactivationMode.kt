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
 * How a `PaperSheetView` (fx and swing) treats the pages named in its `deactivatedPageIds` set.
 * A purely view-side switch; nothing here is persisted in the `Document`.
 */
enum class PageDeactivationMode {

    /** `deactivatedPageIds` is fully ignored; behaves as if the set were empty. */
    IGNORE,

    /** The page is not editable, drawn with a special fill and hatch, and the caret skips over it. */
    DISABLED,

    /** The caret reaches and crosses the page normally, selection works, but every mutation touching
     * it is discarded; no special drawing. */
    READONLY,

    /** The page (and its flow overflow sheets) is removed entirely from layout, scroll area and
     * hit-testing; the document itself is unchanged. */
    HIDDEN,
}
