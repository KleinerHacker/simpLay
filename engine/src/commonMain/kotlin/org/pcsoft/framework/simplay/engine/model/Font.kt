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
 * A font face. A plain data holder; the size is a unit-less double.
 */
@Serializable
data class Font(
    val family: String,
    val size: Double,
    val weight: FontWeight = FontWeight.NORMAL,
    val style: FontStyle = FontStyle.NORMAL,
) : PlatformSerializable
