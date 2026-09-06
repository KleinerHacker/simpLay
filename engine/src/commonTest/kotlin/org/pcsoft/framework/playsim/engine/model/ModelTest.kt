package org.pcsoft.framework.playsim.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.playsim.engine.geometry.Margins
import org.pcsoft.framework.playsim.engine.geometry.Size

class ModelTest {

    /**
     * Verifies that a document without pages is allowed and reports empty counts.
     */
    @Test
    fun emptyDocumentIsAllowed() {
        val document = Document()
        assertTrue(document.pages.isEmpty())
        assertEquals(0, document.wordCount())
        assertEquals(0, document.charCount())
    }

    /**
     * Verifies that [PageLayout.contentWidth] and [PageLayout.contentHeight] subtract the margins
     * from the page size.
     */
    @Test
    fun pageLayoutComputesContentBox() {
        val layout = PageLayout(Size(210.0, 297.0), Margins(20.0, 15.0, 25.0, 30.0))
        assertEquals(165.0, layout.contentWidth)
        assertEquals(252.0, layout.contentHeight)
    }
}
