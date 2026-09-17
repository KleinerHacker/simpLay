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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [effectiveZoom], the shared logic every `PaperSheetView` (fx and swing) applies to convert
 * its logical zoom factor from the engine's point-based layout unit ([LAYOUT_DPI]) into the toolkit's
 * device-independent pixel unit ([TOOLKIT_DPI]), so that a zoom of `1.0` renders content at true
 * physical size.
 */
class ZoomDpiScaleTest {

    /**
     * Verifies that a logical zoom of `1.0` is scaled up by exactly `96 / 72`, the fixed conversion
     * from PostScript/PDF points to the toolkit's `96` DPI device-independent pixel.
     */
    @Test
    fun `default zoom is scaled by the point to toolkit pixel ratio`() {
        assertEquals(96.0 / 72.0, effectiveZoom(zoom = 1.0))
    }

    /**
     * Verifies that the conversion composes multiplicatively with a non-default logical zoom.
     */
    @Test
    fun `conversion composes multiplicatively with logical zoom`() {
        assertEquals(0.25 * 96.0 / 72.0, effectiveZoom(zoom = 0.25))
        assertEquals(4.0 * 96.0 / 72.0, effectiveZoom(zoom = 4.0))
    }

    /**
     * Verifies that a zoom of `0.0` stays `0.0`.
     */
    @Test
    fun `zero zoom stays zero`() {
        assertEquals(0.0, effectiveZoom(zoom = 0.0))
    }
}
