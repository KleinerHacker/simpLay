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

import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * A raw [Document] with its pages replaced by measured pages.
 *
 * `Document` has no pass-through properties to delegate (`pages` is replaced), so this is a plain
 * wrapper: the raw pages stay reachable via `raw.pages`. An empty document with no pages is
 * allowed. Not persistable.
 *
 * @property raw the wrapped raw document.
 * @property pages the measured pages of this document, in order.
 */
class MeasuredDocument(
    val raw: Document,
    val pages: List<MeasuredPage>,
) {
    /**
     * The distinct raw fonts whose stored [org.pcsoft.framework.simplay.engine.model.FontFingerprint]
     * did not match the current measurement ([FontFingerprintStatus.DEVIATION]).
     *
     * Empty when no block font carried a fingerprint or every one still matched. A non-empty list
     * means the document is being rendered with at least one substituted or updated font.
     */
    val fingerprintDeviations: List<Font>
        get() = pages.asSequence()
            .flatMap { it.blocks.asSequence() }
            .map { it.style.font }
            .filter { it.fingerprintStatus == FontFingerprintStatus.DEVIATION }
            .map { it.raw }
            .distinct()
            .toList()
}
