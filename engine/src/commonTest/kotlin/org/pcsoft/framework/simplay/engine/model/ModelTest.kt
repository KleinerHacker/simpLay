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

package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

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
        val layout = PageLayout(
            Size(
                210.0,
                297.0
            ), Margins(20.0, 15.0, 25.0, 30.0)
        )
        assertEquals(165.0, layout.contentWidth)
        assertEquals(252.0, layout.contentHeight)
    }
}
