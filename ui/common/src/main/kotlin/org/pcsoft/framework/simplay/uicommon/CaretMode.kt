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
 * The typing behaviour of a `PaperSheetView` (fx and swing) edit caret.
 *
 * @property INSERT typed characters are inserted before the caret; the default, painted as a thin line.
 * @property OVERWRITE typed characters replace the one at the caret instead, up to the end of its
 *   line; painted as a filled block the width of the character about to be overwritten.
 */
enum class CaretMode {
    INSERT,
    OVERWRITE,
}

/** The other [CaretMode]: [CaretMode.INSERT] toggles to [CaretMode.OVERWRITE] and back. */
fun CaretMode.toggled(): CaretMode = when (this) {
    CaretMode.INSERT -> CaretMode.OVERWRITE
    CaretMode.OVERWRITE -> CaretMode.INSERT
}
