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
import kotlinx.serialization.json.Json
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

class SerializationTest {

    private val json = Json { prettyPrint = false }
    private val style = TextStyle(
        Font(
            "Serif",
            12.0,
            FontWeight.BOLD,
            FontStyle.ITALIC
        )
    )
    private val layout = PageLayout(
        Size(
            210.0,
            297.0
        ), Margins(20.0, 20.0, 20.0, 20.0)
    )

    /**
     * Verifies that a document containing a [FlowPage] and a [SinglePage] survives a JSON
     * encode/decode round trip without loss.
     */
    @Test
    fun roundTripsDocumentWithBothPageKinds() {
        val document = Document(
            listOf(
                FlowPage(
                    layout,
                    listOf(
                        TextBlock.of(
                            "Hello, world!",
                            style
                        )
                    )
                ),
                SinglePage(
                    layout,
                    listOf(
                        TextBlock.of(
                            "Second page.",
                            style
                        )
                    )
                ),
            ),
        )

        val encoded = json.encodeToString(Document.serializer(), document)
        val decoded = json.decodeFromString(Document.serializer(), encoded)

        assertEquals(document, decoded)
    }

    /**
     * Verifies that an empty document round trips through JSON.
     */
    @Test
    fun roundTripsEmptyDocument() {
        val document = Document()
        val encoded = json.encodeToString(Document.serializer(), document)
        assertEquals(document, json.decodeFromString(Document.serializer(), encoded))
    }
}
