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
 * Passed to [PaperSheetView.getOnType] right after a character was typed into an editable
 * [PaperSheetView], carrying the typed character together with the document structure it landed in.
 *
 * @property character the character that was typed.
 * @property textPart the raw part [character] was inserted into (or that now overwrites it).
 * @property textBlock the raw block owning [textPart].
 * @property page the raw page owning [textBlock].
 */
class PaperSheetTypeEvent internal constructor(
    val character: Char,
    val textPart: TextPart,
    val textBlock: TextBlock,
    val page: Page,
    eventType: EventType<PaperSheetTypeEvent>,
) : Event(eventType) {

    companion object {

        /** Common supertype of every [PaperSheetTypeEvent] type. */
        @JvmField
        val ANY: EventType<PaperSheetTypeEvent> = EventType(Event.ANY, "PAPER_SHEET_TYPE")

        /** Fired right after a character was applied to the document. */
        @JvmField
        val TYPED: EventType<PaperSheetTypeEvent> = EventType(ANY, "PAPER_SHEET_TYPE_TYPED")
    }
}
