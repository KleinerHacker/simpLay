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

package org.pcsoft.framework.simplay.engine

import kotlinx.serialization.Serializable

/**
 * Strategy that turns a per-sheet exclusion mask into displayed page numbers.
 *
 * Implemented directly by [PageCountingMode], so the persisted setting on
 * [org.pcsoft.framework.simplay.engine.model.PageNumbering.counting] doubles as the strategy
 * [MeasuredDocument.planPageNumbers][org.pcsoft.framework.simplay.engine.planPageNumbers] executes -
 * no separate lookup or bridging step between the two.
 *
 * Every implementation must be deterministic and must not call any platform API.
 */
fun interface PageCountingStrategy {

    /**
     * Maps each sheet to its displayed number, or `null` when the sheet is excluded.
     *
     * @param sheetCount the total number of sheets.
     * @param excludedSheetFlags `true` at the index of every excluded sheet, same size as [sheetCount].
     * @param startNumber the display value of the first counted sheet.
     * @return one entry per sheet, in order; `null` for an excluded sheet.
     */
    fun numbers(sheetCount: Int, excludedSheetFlags: List<Boolean>, startNumber: Int): List<Int?>
}

/**
 * How excluded sheets affect the running page counter - a persistable enum that is at the same time
 * a [PageCountingStrategy], so storing a mode and executing it are the same value.
 */
@Serializable
enum class PageCountingMode : PageCountingStrategy {

    /**
     * Every sheet advances the counter, including an excluded one; only its own label is
     * suppressed.
     */
    CONTINUOUS {
        override fun numbers(sheetCount: Int, excludedSheetFlags: List<Boolean>, startNumber: Int): List<Int?> {
            var next = startNumber
            return List(sheetCount) { index ->
                val current = next
                next += 1
                if (excludedSheetFlags[index]) null else current
            }
        }
    },

    /**
     * An excluded sheet is skipped entirely: it shows no label and does not advance the counter.
     */
    SKIP_EXCLUDED {
        override fun numbers(sheetCount: Int, excludedSheetFlags: List<Boolean>, startNumber: Int): List<Int?> {
            var next = startNumber
            return List(sheetCount) { index ->
                if (excludedSheetFlags[index]) {
                    null
                } else {
                    val current = next
                    next += 1
                    current
                }
            }
        }
    },
}
