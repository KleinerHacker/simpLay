package org.pcsoft.framework.simplay.engine.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size

class SerializationTest {

    private val json = Json { prettyPrint = false }
    private val style = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextStyle(
        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Font(
            "Serif",
            12.0,
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FontWeight.BOLD,
            _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FontStyle.ITALIC
        )
    )
    private val layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.PageLayout(
        _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Size(
            210.0,
            297.0
        ), _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Margins(20.0, 20.0, 20.0, 20.0)
    )

    /**
     * Verifies that a document containing a [org.pcsoft.framework.simplay.engine.model.FlowPage] and a [org.pcsoft.framework.simplay.engine.model.SinglePage] survives a JSON
     * encode/decode round trip without loss.
     */
    @Test
    fun roundTripsDocumentWithBothPageKinds() {
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document(
            listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(
                    layout,
                    listOf(
                        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                            "Hello, world!",
                            style
                        )
                    )
                ),
                _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(
                    layout,
                    listOf(
                        _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of(
                            "Second page.",
                            style
                        )
                    )
                ),
            ),
        )

        val encoded = json.encodeToString(_root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document.serializer(), document)
        val decoded = json.decodeFromString(_root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document.serializer(), encoded)

        assertEquals(document, decoded)
    }

    /**
     * Verifies that an empty document round trips through JSON.
     */
    @Test
    fun roundTripsEmptyDocument() {
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document()
        val encoded = json.encodeToString(_root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document.serializer(), document)
        assertEquals(document, json.decodeFromString(_root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document.serializer(), encoded))
    }
}
