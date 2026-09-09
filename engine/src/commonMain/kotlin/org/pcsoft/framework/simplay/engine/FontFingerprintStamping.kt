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

import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Returns a copy of this [Document] in which every block font carries a
 * [FontFingerprint] taken through [measurer].
 *
 * This is the authoring step for the fingerprint feature: run it once (in a context that has a
 * platform text stack, e.g. a `*FontProbe` in a UI module) before persisting a document, so a later
 * reopen can tell via `MeasuredDocument.fingerprintDeviations` that a font went missing or was
 * silently replaced. A fingerprint is only meaningful against the same toolkit it was taken with.
 *
 * Fonts already carrying a fingerprint are re-stamped. Identical fonts are measured only once.
 *
 * @param measurer the font-measuring callback for the target platform.
 * @param overwrite when `false`, a font that already has a fingerprint is left untouched.
 * @return a document equal to this one except for the block fonts' [Font.fingerprint].
 */
fun Document.withFontFingerprints(
    measurer: FontMeasureCalculator,
    overwrite: Boolean = true,
): Document {
    val cache = HashMap<Font, FontFingerprint>()

    fun stamp(font: Font): Font {
        if (!overwrite && font.fingerprint != null) return font
        val bare = font.copy(fingerprint = null)
        val print = cache.getOrPut(bare) { FontFingerprint.of(measurer, bare) }
        return font.copy(fingerprint = print)
    }

    val pages = pages.map { page ->
        val blocks = page.blocks.map { block -> block.withStyle(block.style.copy(font = stamp(block.style.font))) }
        when (page) {
            is FlowPage -> page.copy(blocks = blocks)
            is SinglePage -> page.copy(blocks = blocks)
        }
    }
    return copy(pages = pages)
}
