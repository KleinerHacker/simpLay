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

package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * Multiplicative and additive line spacing. All values are unit-less doubles.
 */
@Serializable
data class LineSpacing(val factor: Double = 1.0, val extraLeading: Double = 0.0) : PlatformSerializable

/**
 * The visual style of a text block. A plain data holder.
 */
@Serializable
data class TextStyle(
    val font: Font,
    val lineSpacing: LineSpacing = LineSpacing(),
    val alignment: TextAlignment = TextAlignment.LEFT,
) : PlatformSerializable
