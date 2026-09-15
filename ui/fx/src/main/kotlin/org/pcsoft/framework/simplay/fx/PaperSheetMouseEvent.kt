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

import javafx.event.Event
import javafx.event.EventType
import org.pcsoft.framework.simplay.engine.model.Page
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * Passed to [PaperSheetView.getOnMouseEvent] while the mouse hovers ([HOVER]) or clicks ([CLICK])
 * over a [PaperSheetView], carrying the document structure under the pointer.
 *
 * @property textPart the raw part under the pointer, or `null` over an empty area of a page or
 *   outside every page.
 * @property textBlock the raw block under the pointer, or `null` over an empty area of a page or
 *   outside every page.
 * @property page the raw page under the pointer, or `null` outside every page.
 */
class PaperSheetMouseEvent internal constructor(
    val textPart: TextPart?,
    val textBlock: TextBlock?,
    val page: Page?,
    eventType: EventType<PaperSheetMouseEvent>,
) : Event(eventType) {

    companion object {

        /** Common supertype of every [PaperSheetMouseEvent] type. */
        @JvmField
        val ANY: EventType<PaperSheetMouseEvent> = EventType(Event.ANY, "PAPER_SHEET_MOUSE")

        /** Fired while the mouse hovers over the view (on every pointer move). */
        @JvmField
        val HOVER: EventType<PaperSheetMouseEvent> = EventType(ANY, "PAPER_SHEET_MOUSE_HOVER")

        /** Fired when the mouse clicks the view. */
        @JvmField
        val CLICK: EventType<PaperSheetMouseEvent> = EventType(ANY, "PAPER_SHEET_MOUSE_CLICK")
    }
}
