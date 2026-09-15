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

package org.pcsoft.framework.simplay.engine.strategy

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * The default [WordBreakerStrategy]: never offers a break, so no hyphenation happens and an
 * over-long word simply overflows its line.
 */
object NoOpWordBreakerStrategy : WordBreakerStrategy {

    override fun breakOffsets(
        word: String,
        font: Font,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
    ): List<Int> = emptyList()
}
