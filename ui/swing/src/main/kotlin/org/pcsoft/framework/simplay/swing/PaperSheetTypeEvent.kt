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

/** A listener for the [PaperSheetTypeEvent] fired by [PaperSheetView.onType]. */
fun interface PaperSheetTypeListener {
    fun handle(event: PaperSheetTypeEvent)
}

/**
 * Passed to [PaperSheetView.onType] right after a character was typed into an editable
 * [PaperSheetView], carrying the typed character together with the document structure it landed in.
 * The Swing counterpart of the `fx` module's `PaperSheetTypeEvent`; it extends [EventObject] instead
 * of a JavaFX `Event`.
 *
 * @property character the character that was typed.
 * @property textPart the raw part [character] was inserted into (or that now overwrites it).
 * @property textBlock the raw block owning [textPart].
 * @property page the raw page owning [textBlock].
 */
class PaperSheetTypeEvent internal constructor(
    source: PaperSheetView,
    val character: Char,
    val textPart: TextPart,
    val textBlock: TextBlock,
    val page: Page,
) : EventObject(source)
