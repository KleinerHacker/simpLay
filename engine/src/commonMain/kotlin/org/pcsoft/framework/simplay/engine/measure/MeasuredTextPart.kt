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

package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.TextPart

/**
 * A raw [TextPart] placed at its measured [bounds] relative to the page content area.
 *
 * The unchanged [text] is forwarded to [raw] by hand. Not persistable.
 *
 * @property raw the wrapped raw part.
 * @property bounds position and extent of the part relative to the page content area.
 */
class MeasuredTextPart(val raw: TextPart, val bounds: Rect) {
    /** The text of [raw]. */
    val text: String get() = raw.text
}
