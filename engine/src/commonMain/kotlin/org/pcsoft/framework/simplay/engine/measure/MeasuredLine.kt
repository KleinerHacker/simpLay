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
import org.pcsoft.framework.simplay.engine.model.TextAlignment

/**
 * One laid-out line of text. An intermediate level that has no raw counterpart.
 *
 * @property parts the measured parts of this line, in reading order.
 * @property lineBox position and extent of the line relative to the page content area.
 * @property baseline distance from the top of [lineBox] to the text baseline, in layout units.
 * @property ascent ascent used for this line, in layout units.
 * @property descent descent used for this line, in layout units.
 * @property alignment horizontal alignment applied to this line.
 * @property lastLine `true` for the final line of its block (relevant for justified alignment).
 */
class MeasuredLine(
    val parts: List<MeasuredTextPart>,
    val lineBox: Rect,
    val baseline: Double,
    val ascent: Double,
    val descent: Double,
    val alignment: TextAlignment,
    val lastLine: Boolean,
)
