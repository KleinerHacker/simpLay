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

import org.pcsoft.framework.simplay.engine.geometry.FontMetrics
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontStyle
import org.pcsoft.framework.simplay.engine.model.FontWeight

/**
 * A raw [Font] enriched with the vertical [metrics] resolved for it.
 *
 * The unchanged font properties are forwarded to [raw] by hand; nothing is stored twice. This type
 * is not persistable.
 *
 * @property raw the wrapped raw font.
 * @property metrics the vertical metrics resolved for [raw], in layout units.
 */
class MeasuredFont(val raw: Font, val metrics: FontMetrics) {
    /** The family of [raw]. */
    val family: String get() = raw.family

    /** The size of [raw], a unit-less double. */
    val size: Double get() = raw.size

    /** The weight of [raw]. */
    val weight: FontWeight get() = raw.weight

    /** The slant of [raw]. */
    val style: FontStyle get() = raw.style
}
