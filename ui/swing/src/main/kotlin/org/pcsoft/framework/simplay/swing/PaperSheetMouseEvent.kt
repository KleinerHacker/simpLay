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

import java.util.EventObject
import org.pcsoft.framework.simplay.engine.model.Page
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextPart

/** A listener for the [PaperSheetMouseEvent] fired by [PaperSheetView.onMouseEvent]. */
fun interface PaperSheetMouseListener {
    fun handle(event: PaperSheetMouseEvent)
}

/**
 * Passed to [PaperSheetView.onMouseEvent] while the mouse hovers ([Kind.HOVER]) or clicks
 * ([Kind.CLICK]) over a [PaperSheetView], carrying the document structure under the pointer. The
 * Swing counterpart of the `fx` module's `PaperSheetMouseEvent`; it extends [EventObject] instead of
 * a JavaFX `Event`.
 *
 * @property kind whether the mouse hovered or clicked.
 * @property textPart the raw part under the pointer, or `null` over an empty area of a page or
 *   outside every page.
 * @property textBlock the raw block under the pointer, or `null` over an empty area of a page or
 *   outside every page.
 * @property page the raw page under the pointer, or `null` outside every page.
 */
class PaperSheetMouseEvent internal constructor(
    source: PaperSheetView,
    val kind: Kind,
    val textPart: TextPart?,
    val textBlock: TextBlock?,
    val page: Page?,
) : EventObject(source) {

    enum class Kind { HOVER, CLICK }
}
