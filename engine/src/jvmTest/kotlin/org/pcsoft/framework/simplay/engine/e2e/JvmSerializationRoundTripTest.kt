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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.Document

/**
 * Verifies that the raw document survives a JVM-serialization save / load round-trip through
 * `ObjectOutputStream` / `ObjectInputStream` without loss. The raw model is `java.io.Serializable`
 * on the JVM through the `PlatformSerializable` marker.
 */
class JvmSerializationRoundTripTest {

    private val document = E2ETestData.rawSampleDocument()

    private fun roundTrip(value: Document): Document {
        val bytes = ByteArrayOutputStream().use { out ->
            ObjectOutputStream(out).use { it.writeObject(value) }
            out.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Document }
    }

    /**
     * Use case: writing the sample document to an object stream and reading it back yields an equal
     * document.
     */
    @Test
    fun jvmSerializationRoundTripPreservesTheDocument() {
        assertEquals(document, roundTrip(document))
    }

    /**
     * Use case: every text block reproduces its original string after the JVM-serialization
     * round-trip.
     */
    @Test
    fun jvmSerializationRoundTripPreservesEveryBlockText() {
        val restored = roundTrip(document)

        assertEquals(
            document.pages.flatMap { it.blocks }.map { it.toString() },
            restored.pages.flatMap { it.blocks }.map { it.toString() },
        )
    }
}
