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

/**
 * A two-dimensional extent. All values are unit-less doubles.
 */
@Serializable
data class Size(val width: Double, val height: Double)

/**
 * An axis-aligned rectangle described by its top-left corner and its extent.
 * All values are unit-less doubles.
 */
@Serializable
data class Rect(val x: Double, val y: Double, val width: Double, val height: Double)

/**
 * The four inner offsets of a box. All values are unit-less doubles.
 */
@Serializable
data class Margins(val left: Double, val top: Double, val right: Double, val bottom: Double)
