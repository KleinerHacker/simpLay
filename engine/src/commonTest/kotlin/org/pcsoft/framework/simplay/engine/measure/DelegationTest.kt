package org.pcsoft.framework.simplay.engine.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.TextSymbol
import org.pcsoft.framework.simplay.engine.model.TextWord

/**
 * Verifies that the measured decorators forward their pass-through properties to the wrapped raw
 * object via Kotlin `by` delegation, without adding a second accessor for a replaced property.
 */
class DelegationTest {

    /**
     * Use case: a [MeasuredFont] must expose the wrapped font's family, size, weight and style
     * unchanged (all delegated) while adding its own metrics.
     */
    @Test
    fun measuredFontDelegatesEveryRawProperty() {
        val raw = MeasureTestData.bodyFont
        val measured = MeasuredFont(
            raw,
            MeasureTestData.bodyMetrics
        )

        assertEquals(raw.family, measured.family)
        assertEquals(raw.size, measured.size)
        assertEquals(raw.weight, measured.weight)
        assertEquals(raw.style, measured.style)
        assertSame(raw, measured.raw)
        assertEquals(MeasureTestData.bodyMetrics, measured.metrics)
    }

    /**
     * Use case: a [MeasuredTextStyle] must delegate the pass-through properties `lineSpacing` and
     * `alignment`; `font` is not delegated but replaced by the measured font.
     */
    @Test
    fun measuredTextStyleDelegatesPassThroughProperties() {
        val raw = MeasureTestData.bodyStyle
        val measured = MeasureTestData.measuredBodyStyle()

        assertEquals(raw.lineSpacing, measured.lineSpacing)
        assertEquals(raw.alignment, measured.alignment)
        assertSame(raw, measured.raw)
    }

    /**
     * Use case: a [MeasuredTextPart] forwards the wrapped part's text (a word and a symbol) and
     * keeps the raw part reachable.
     */
    @Test
    fun measuredTextPartForwardsText() {
        val rawWord = TextWord("Layout")
        val word = MeasuredTextPart(
            rawWord,
            MeasureTestData.someRect()
        )
        val symbol = MeasuredTextPart(
            TextSymbol('!'),
            MeasureTestData.someRect()
        )

        assertEquals("Layout", word.text)
        assertSame(rawWord, word.raw)
        assertEquals("!", symbol.text)
    }

    /**
     * Use case: a [MeasuredFlowPage] delegates the pass-through `layout` and keeps the raw page
     * reachable; it exposes no raw block list.
     */
    @Test
    fun measuredFlowPageDelegatesLayout() {
        val raw =
            FlowPage(layout = MeasureTestData.pageLayout)
        val measured = MeasuredFlowPage(
            raw = raw,
            pageIndex = 0,
            blocks = emptyList(),
        )

        assertSame(raw.layout, measured.layout)
        assertSame(raw, measured.raw)
    }

    /**
     * Use case: `MeasuredLine` is a standalone level with no raw counterpart; its members are
     * exactly the values passed in.
     */
    @Test
    fun measuredLineStoresItsOwnValues() {
        val box = Rect(
            x = 1.0,
            y = 2.0,
            width = 30.0,
            height = 12.0
        )
        val line = MeasuredLine(
            parts = emptyList(),
            lineBox = box,
            baseline = 9.0,
            ascent = 9.0,
            descent = 3.0,
            alignment = MeasureTestData.bodyStyle.alignment,
            lastLine = true,
        )

        assertSame(box, line.lineBox)
        assertEquals(9.0, line.baseline)
        assertEquals(true, line.lastLine)
    }
}
