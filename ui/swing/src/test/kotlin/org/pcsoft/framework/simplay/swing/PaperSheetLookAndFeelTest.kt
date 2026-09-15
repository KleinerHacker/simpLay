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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import javax.swing.UIManager

/**
 * Tests for [PaperSheetLookAndFeel], the Swing replacement for the `fx` module's user-agent
 * stylesheet: the `PaperSheetView.*` keys seed a fresh view unless the caller set a value first.
 */
class PaperSheetLookAndFeelTest {

    @AfterTest
    fun restoreDefaults() {
        UIManager.getDefaults().remove(PaperSheetLookAndFeel.KEY_SHEET_BACKGROUND)
        UIManager.getDefaults().remove(PaperSheetLookAndFeel.KEY_CARET_COLOR)
        PaperSheetLookAndFeel.installDefaults()
    }

    /**
     * Verifies that a `PaperSheetView.sheetBackground` value put into the Look-and-Feel before a view
     * is created is copied onto that view.
     */
    @Test
    fun lookAndFeelValueSeedsANewView() {
        UIManager.put(PaperSheetLookAndFeel.KEY_SHEET_BACKGROUND, Color.PINK)
        val view = PaperSheetView()
        assertEquals(Color.PINK, view.sheetBackground)
    }

    /**
     * Verifies that a value the caller set programmatically after construction is not overwritten by
     * [PaperSheetLookAndFeel.applyTo].
     */
    @Test
    fun programmaticSetterWinsOverLookAndFeel() {
        val view = PaperSheetView()
        view.caretColor = Color.GREEN
        UIManager.put(PaperSheetLookAndFeel.KEY_CARET_COLOR, Color.RED)
        PaperSheetLookAndFeel.applyTo(view)
        assertEquals(Color.GREEN, view.caretColor)
    }

    /**
     * Verifies that the non-editable selection colour resolves to the dedicated
     * `PaperSheetView.selectionColorNonEditable` key and differs from the editable selection colour.
     */
    @Test
    fun nonEditableSelectionColourComesFromItsOwnKey() {
        val nonEditable = PaperSheetLookAndFeel.nonEditableSelectionColor()
        assertNotEquals(UIManager.get(PaperSheetLookAndFeel.KEY_SELECTION_COLOR), nonEditable)
    }
}
