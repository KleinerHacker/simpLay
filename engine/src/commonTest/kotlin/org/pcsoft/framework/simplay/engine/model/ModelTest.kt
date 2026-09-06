package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.charCount
import org.pcsoft.framework.simplay.engine.model.wordCount

class ModelTest {

    /**
     * Verifies that a document without pages is allowed and reports empty counts.
     */
    @Test
    fun emptyDocumentIsAllowed() {
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document()
        assertTrue(document.pages.isEmpty())
        assertEquals(0, document.wordCount())
        assertEquals(0, document.charCount())
    }

    /**
     * Verifies that [org.pcsoft.framework.simplay.engine.model.PageLayout.contentWidth] and [org.pcsoft.framework.simplay.engine.model.PageLayout.contentHeight] subtract the margins
     * from the page size.
     */
    @Test
    fun pageLayoutComputesContentBox() {
        val layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.PageLayout(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Size(
                210.0,
                297.0
            ), _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Margins(20.0, 15.0, 25.0, 30.0)
        )
        assertEquals(165.0, layout.contentWidth)
        assertEquals(252.0, layout.contentHeight)
    }
}
