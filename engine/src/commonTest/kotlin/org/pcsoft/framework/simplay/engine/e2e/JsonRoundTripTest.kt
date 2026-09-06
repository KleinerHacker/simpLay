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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * Verifies that the raw document survives a JSON save / load round-trip through
 * `kotlinx-serialization-json` without loss, including the sealed page and text-part polymorphism.
 */
class JsonRoundTripTest {

    private val json = Json { prettyPrint = false }
    private val document = E2ETestData.rawSampleDocument()

    /**
     * Use case: encoding the sample document to JSON and decoding it again yields an equal
     * document.
     */
    @Test
    fun jsonRoundTripPreservesTheDocument() {
        val text = json.encodeToString(Document.serializer(), document)
        val restored = json.decodeFromString(Document.serializer(), text)

        assertEquals(document, restored)
    }

    /**
     * Use case: every text block reproduces its original string after the JSON round-trip.
     */
    @Test
    fun jsonRoundTripPreservesEveryBlockText() {
        val text = json.encodeToString(Document.serializer(), document)
        val restored = json.decodeFromString(Document.serializer(), text)

        assertEquals(
            document.pages.flatMap { it.blocks }.map { it.toString() },
            restored.pages.flatMap { it.blocks }.map { it.toString() },
        )
    }
}
