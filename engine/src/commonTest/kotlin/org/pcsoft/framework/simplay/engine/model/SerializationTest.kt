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
