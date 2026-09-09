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

package org.pcsoft.framework.simplay.fx

import java.io.File
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Headless tests for the JavaFX CSS styling of [PaperSheetView]: the default user-agent stylesheet,
 * the `paper-sheet-view` style class, the `-fx-` properties for sheet chrome / selection / layout,
 * the `:readonly` pseudo-class, the precedence of a programmatic setter over the user-agent
 * stylesheet, and the repaint triggered by a style change.
 */
class PaperSheetStylingTest : JavaFxTestBase() {

    private data class Fixture(val view: PaperSheetView, val skin: PaperSheetViewSkin)

    /**
     * Builds a shown [PaperSheetView] with a sample document, optionally an inline style, a scene
     * stylesheet and an initial mode, then applies CSS and lays it out once.
     */
    private fun fixture(
        inlineStyle: String? = null,
        stylesheet: String? = null,
        mode: PaperSheetMode = PaperSheetMode.READONLY,
        configure: PaperSheetView.() -> Unit = {},
    ): Fixture = onFxThread {
        val view = PaperSheetView()
        view.mode = mode
        if (inlineStyle != null) view.style = inlineStyle
        view.configure()
        val stage = Stage()
        val scene = Scene(view, 320.0, 260.0)
        if (stylesheet != null) scene.stylesheets.add(stylesheet)
        stage.scene = scene
        stage.show()
        view.document = PaperSheetTestFixtures.flowDocument(4)
        view.applyCss()
        view.layout()
        Fixture(view, view.skin as PaperSheetViewSkin)
    }

    /** Writes [css] to a temporary file and returns its `file:` URL for a scene stylesheet. */
    private fun stylesheetFile(css: String): String {
        val file = File.createTempFile("paper-sheet-styling-", ".css")
        file.deleteOnExit()
        file.writeText(css)
        return file.toURI().toURL().toExternalForm()
    }

    /**
     * [PaperSheetView.getUserAgentStylesheet] returns a non-null, loadable URL that points at the
     * bundled `paper-sheet-view.css` resource.
     */
    @Test
    fun defaultUserAgentStylesheetIsApplied() {
        val url = onFxThread { PaperSheetView().userAgentStylesheet }

        assertNotNull(url)
        assertTrue(url.endsWith("paper-sheet-view.css"), "unexpected stylesheet URL: $url")
        assertNotNull(PaperSheetView::class.java.getResource("paper-sheet-view.css"))
    }

    /** Every [PaperSheetView] carries the `paper-sheet-view` style class from construction. */
    @Test
    fun styleClassIsPresentByDefault() {
        assertTrue("paper-sheet-view" in onFxThread { PaperSheetView().styleClass })
    }

    /** An inline `-fx-sheet-border-color` reaches the styleable property and drives the skin repaint. */
    @Test
    fun cssOverridesSheetBorderColor() {
        val (view, _) = fixture(inlineStyle = "-fx-sheet-border-color: #ff0000;")

        assertEquals(Color.web("#ff0000"), view.sheetBorderColor)
    }

    /**
     * Inline `-fx-page-gap` and `-fx-outer-margin` change the property values and the resulting
     * laid-out content size just as the Kotlin setters would.
     */
    @Test
    fun cssOverridesPageGapAndOuterMargin() {
        val defaultWidth = fixture().view.contentSize.width
        val (view, _) = fixture(inlineStyle = "-fx-page-gap: 40; -fx-outer-margin: 50;")

        assertEquals(40.0, view.pageGap)
        assertEquals(50.0, view.outerMargin)
        assertTrue(
            view.contentSize.width > defaultWidth,
            "a wider outer margin should widen the content size (${view.contentSize.width} vs $defaultWidth)",
        )
    }

    /** An inline `-fx-selection-color` reaches the styleable property. */
    @Test
    fun cssOverridesSelectionColor() {
        val (view, _) = fixture(inlineStyle = "-fx-selection-color: #00ff00;")

        assertEquals(Color.web("#00ff00"), view.selectionColor)
    }

    /**
     * The `:readonly` pseudo-class is active in [PaperSheetMode.READONLY] and gone in
     * [PaperSheetMode.EDITABLE], so a `.paper-sheet-view:readonly` rule wins only while read-only.
     */
    @Test
    fun readonlyPseudoClassTogglesWithMode() {
        val sheet = stylesheetFile(
            """
            .paper-sheet-view { -fx-sheet-border-width: 2; }
            .paper-sheet-view:readonly { -fx-sheet-border-width: 9; }
            """.trimIndent(),
        )
        val (view, _) = fixture(stylesheet = sheet, mode = PaperSheetMode.READONLY)

        assertTrue(view.pseudoClassStates.any { it.pseudoClassName == "readonly" })
        assertEquals(9.0, view.sheetBorderWidth)

        onFxThread {
            view.mode = PaperSheetMode.EDITABLE
            view.applyCss()
        }

        assertTrue(view.pseudoClassStates.none { it.pseudoClassName == "readonly" })
        assertEquals(2.0, view.sheetBorderWidth)
    }

    /**
     * A value set through the Kotlin setter keeps its value after CSS is applied: the programmatic
     * origin wins over the user-agent stylesheet.
     */
    @Test
    fun apiSetterWinsOverUserAgentStylesheet() {
        val (view, _) = fixture(configure = { outerMargin = 99.0 })

        assertEquals(99.0, view.outerMargin)
    }

    /** Changing a styleable value triggers exactly one repaint of the skin canvas. */
    @Test
    fun styleChangeTriggersRepaint() {
        val (_, skin) = fixture()

        val delta = onFxThread {
            val before = skin.paintCountForTest
            skin.skinnable.selectionColor = Color.web("#12345680")
            skin.paintCountForTest - before
        }

        assertEquals(1, delta)
    }
}
