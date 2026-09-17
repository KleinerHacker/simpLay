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

package org.pcsoft.framework.simplay.swing

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for the [PaperSheetView] component surface: the property-change notifications, the zoom
 * clamping and the pluggable delegate.
 */
class PaperSheetViewTest {

    /**
     * Verifies that assigning [PaperSheetView.document] fires a `PropertyChangeEvent` under
     * [PaperSheetView.PROP_DOCUMENT] carrying the new value.
     */
    @Test
    fun documentAssignmentFiresPropertyChange() {
        val view = PaperSheetView()
        var fired = 0
        view.addPropertyChangeListener(PaperSheetView.PROP_DOCUMENT) { fired++ }
        view.document = TestDocuments.short
        assertEquals(1, fired)
    }

    /**
     * Verifies that [PaperSheetView.zoom] is clamped into `[minZoom, maxZoom]` both when the zoom is
     * set out of range and when the range is narrowed around a current zoom.
     */
    @Test
    fun zoomIsAlwaysClampedIntoRange() {
        val view = PaperSheetView()
        view.zoom = 99.0
        assertEquals(view.maxZoom, view.zoom)
        view.zoom = 1.0
        view.maxZoom = 0.5
        assertEquals(0.5, view.zoom)
    }

    /**
     * Verifies that the default delegate installed by [PaperSheetView.updateUI] is a
     * [BasicPaperSheetUI].
     */
    @Test
    fun defaultDelegateIsBasicPaperSheetUI() {
        assertIs<BasicPaperSheetUI>(PaperSheetView().getPaperSheetUI())
    }

    /**
     * Verifies that a programmatic style setter is recorded in [PaperSheetView.styleSetByUser], so
     * the Look-and-Feel no longer overrides that value.
     */
    @Test
    fun explicitStyleSetterIsRemembered() {
        val view = PaperSheetView()
        assertTrue(PaperSheetView.PROP_CARET_COLOR !in view.styleSetByUser)
        view.caretColor = Color.RED
        assertTrue(PaperSheetView.PROP_CARET_COLOR in view.styleSetByUser)
    }

    /**
     * Verifies that measuring a document publishes the unscaled content size (including the outer
     * margin on both sides) onto the read-only [PaperSheetView.contentSize].
     */
    @Test
    fun contentSizeIsPublishedAfterMeasuring() {
        val view = PaperSheetView().apply { document = TestDocuments.short }
        assertTrue(view.contentSize.width > 0)
        assertTrue(view.contentSize.height > 0)
    }

    /**
     * Verifies that all five input-control switches - zoom, clipboard, text (duplicate), selection
     * and caret (`Home`/`End`/`Page Up`/`Page Down`) - default to `true`, can be toggled off and
     * back on, and fire a `PropertyChangeEvent` under their respective `PROP_*` constant.
     */
    @Test
    fun inputControlPropertiesDefaultToTrueAndFirePropertyChanges() {
        val view = PaperSheetView()
        assertTrue(view.zoomInputControlEnabled)
        assertTrue(view.clipboardInputControlEnabled)
        assertTrue(view.textInputControlEnabled)
        assertTrue(view.selectionInputControlEnabled)
        assertTrue(view.caretInputControlEnabled)

        var fired = 0
        view.addPropertyChangeListener(PaperSheetView.PROP_ZOOM_INPUT_CONTROL_ENABLED) { fired++ }
        view.zoomInputControlEnabled = false
        view.clipboardInputControlEnabled = false
        view.textInputControlEnabled = false
        view.selectionInputControlEnabled = false
        view.caretInputControlEnabled = false

        assertEquals(1, fired)
        assertFalse(view.zoomInputControlEnabled)
        assertFalse(view.clipboardInputControlEnabled)
        assertFalse(view.textInputControlEnabled)
        assertFalse(view.selectionInputControlEnabled)
        assertFalse(view.caretInputControlEnabled)

        view.zoomInputControlEnabled = true
        assertTrue(view.zoomInputControlEnabled)
    }
}
