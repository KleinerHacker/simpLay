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

import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint

/**
 * Result of matching a [Font]'s stored [FontFingerprint] against a fresh measurement during the
 * measure pass.
 */
enum class FontFingerprintStatus {
    /** The font carried no fingerprint, so nothing was verified. */
    NOT_CHECKED,

    /** The stored fingerprint matches the current measurement; the same face is in use. */
    MATCH,

    /** The stored fingerprint does not match; the font is missing or was silently replaced. */
    DEVIATION,
}
