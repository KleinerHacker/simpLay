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

package org.pcsoft.framework.simplay.engine.geometry

import kotlinx.serialization.Serializable
import org.pcsoft.framework.simplay.engine.PlatformSerializable

/**
 * Vertical metrics of a font. All values are unit-less doubles.
 */
@Serializable
data class FontMetrics(val ascent: Double, val descent: Double, val leading: Double) : PlatformSerializable {
    /** The distance from one baseline to the next. */
    val lineHeight: Double
        get() = ascent + descent + leading
}

/**
 * Metrics of a measured piece of text. All values are unit-less doubles.
 */
@Serializable
data class TextMetrics(val width: Double, val ascent: Double, val descent: Double) : PlatformSerializable
