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

package org.pcsoft.framework.simplay.engine.e2e

import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Verifies that the raw document survives an XML save / load round-trip through `xmlutil` without
 * loss, and that the sealed [org.pcsoft.framework.simplay.engine.model.Page] and
 * [org.pcsoft.framework.simplay.engine.model.TextPart] polymorphism is kept.
 */
class XmlRoundTripTest {

    private val xml = XML { autoPolymorphic = true }
    private val document = E2ETestData.rawSampleDocument()

    /**
     * Use case: encoding the sample document to XML and decoding it again yields an equal document.
     */
    @Test
    fun xmlRoundTripPreservesTheDocument() {
        val text = xml.encodeToString(Document.serializer(), document)
        val restored = xml.decodeFromString(Document.serializer(), text)

        assertEquals(document, restored)
    }

    /**
     * Use case: the concrete page subtypes survive the XML round-trip, so a flow page stays a flow
     * page and a single page stays a single page.
     */
    @Test
    fun xmlRoundTripKeepsThePagePolymorphism() {
        val text = xml.encodeToString(Document.serializer(), document)
        val restored = xml.decodeFromString(Document.serializer(), text)

        assertTrue(restored.pages[0] is FlowPage)
        assertTrue(restored.pages[1] is SinglePage)
        assertEquals(
            document.pages.map { it::class },
            restored.pages.map { it::class },
        )
    }

    /**
     * Use case: every text block reproduces its original string after the XML round-trip.
     */
    @Test
    fun xmlRoundTripPreservesEveryBlockText() {
        val text = xml.encodeToString(Document.serializer(), document)
        val restored = xml.decodeFromString(Document.serializer(), text)

        assertEquals(
            document.pages.flatMap { it.blocks }.map { it.toString() },
            restored.pages.flatMap { it.blocks }.map { it.toString() },
        )
    }
}
