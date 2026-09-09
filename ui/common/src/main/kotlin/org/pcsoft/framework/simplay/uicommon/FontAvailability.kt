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

package org.pcsoft.framework.simplay.uicommon

/**
 * Outcome of checking whether a requested font family can still be resolved by the platform text
 * stack the way it was when a document was authored.
 *
 * The distinction between [MISSING] and [SUBSTITUTED] depends on how much the concrete toolkit
 * reveals: some stacks answer an unknown request with a well-known default family (detectable as
 * [MISSING]), others silently answer with a different real family ([SUBSTITUTED], only distinguishable
 * from [AVAILABLE] against a stored font fingerprint).
 */
enum class FontAvailability {
    /** The requested family resolves to itself; the font is present. */
    AVAILABLE,

    /** The requested family is unknown but the stack answered with another real family. */
    SUBSTITUTED,

    /** The requested family is unknown and the stack fell back to a default family. */
    MISSING,
}
