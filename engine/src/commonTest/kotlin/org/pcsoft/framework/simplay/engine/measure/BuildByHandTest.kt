package org.pcsoft.framework.simplay.engine.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextBlock

/**
 * Verifies that a complete measured model can be assembled by hand, without any engine, and that
 * every level is reachable from the resulting [org.pcsoft.framework.simplay.engine.measure.MeasuredDocument].
 */
class BuildByHandTest {

    /**
     * Use case: a test builds font, style, parts, line, block, page and document directly and the
     * whole tree can be traversed from the top.
     */
    @Test
    fun fullMeasuredModelAssembledByHand() {
        val measuredStyle = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBodyStyle()

        val rawBlock = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.TextBlock.Companion.of("Engine core", _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyStyle)
        val parts = rawBlock.parts.mapIndexed { index, part ->
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextPart(
                part,
                _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(
                    x = index * 30.0,
                    y = 0.0,
                    width = 28.0,
                    height = 12.0
                )
            )
        }
        val line = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredLine(
            parts = parts,
            lineBox = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(
                x = 0.0,
                y = 0.0,
                width = 160.0,
                height = 19.0
            ),
            baseline = 9.0,
            ascent = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyMetrics.ascent,
            descent = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyMetrics.descent,
            alignment = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyStyle.alignment,
            lastLine = true,
        )
        val block = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock(
            raw = rawBlock,
            lines = listOf(line),
            bounds = _root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(
                x = 0.0,
                y = 0.0,
                width = 160.0,
                height = 19.0
            ),
            style = measuredStyle,
        )

        val rawPage = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(
            layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout,
            blocks = listOf(rawBlock)
        )
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(
            raw = rawPage,
            pageIndex = 0,
            blocks = listOf(block),
        )

        val rawDocument = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document(pages = listOf(rawPage))
        val document = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredDocument(
            rawDocument,
            pages = listOf(page)
        )

        val reachedPart = document.pages
            .single()
            .blocks
            .single()
            .lines
            .single()
            .parts
            .first()

        assertEquals("Engine", reachedPart.text)
        assertEquals(_root_ide_package_.org.pcsoft.framework.simplay.engine.geometry.Rect(0.0, 0.0, 28.0, 12.0), reachedPart.bounds)
        assertSame(measuredStyle, document.pages.single().blocks.single().style)
        assertEquals(
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.bodyResolvedLineHeight,
            document.pages.single().blocks.single().style.resolvedLineHeight,
        )
        assertTrue(document.pages.single() is org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage)
        assertSame(rawDocument, document.raw)
        assertEquals(rawBlock.parts.size, page.blocks.single().lines.single().parts.size)
    }

    /**
     * Use case: an empty measured document with no pages is a valid hand-built model, and the raw
     * document stays reachable.
     */
    @Test
    fun emptyMeasuredDocumentIsAllowed() {
        val raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.Document()
        val document =
            _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredDocument(raw, pages = emptyList())

        assertTrue(document.pages.isEmpty())
        assertSame(raw, document.raw)
        assertTrue(document.raw.pages.isEmpty())
    }
}
