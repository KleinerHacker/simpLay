package org.pcsoft.framework.simplay.engine.measure

import kotlin.test.Test
import kotlin.test.assertEquals
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Verifies the [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.effectiveSize] rules: a single page grows only in height, a flow page
 * never grows. The required content height is derived from the measured blocks.
 */
class SinglePageGrowthTest {

    /**
     * Use case: when the lowest measured block reaches past the layout height, a
     * [org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage] grows its height to the vertical margins plus the required content
     * height, keeping the layout width.
     */
    @Test
    fun singlePageGrowsInHeightWhenContentOverflows() {
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBlockReaching(
                    400.0
                )
            ),
        )

        assertEquals(400.0, page.requiredContentHeight)
        assertEquals(200.0, page.effectiveSize.width)
        assertEquals(440.0, page.effectiveSize.height)
    }

    /**
     * Use case: when the measured content fits inside the layout, a [org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage] keeps the
     * exact layout size.
     */
    @Test
    fun singlePageKeepsLayoutSizeWhenContentFits() {
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBlockReaching(
                    100.0
                )
            ),
        )

        assertEquals(200.0, page.effectiveSize.width)
        assertEquals(300.0, page.effectiveSize.height)
    }

    /**
     * Use case: an empty [org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage] reports a required content height of zero and keeps
     * the layout size.
     */
    @Test
    fun emptySinglePageReportsZeroRequiredHeight() {
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.SinglePage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = emptyList(),
        )

        assertEquals(0.0, page.requiredContentHeight)
        assertEquals(_root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout.size, page.effectiveSize)
    }

    /**
     * Use case: a [org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage] always reports the raw layout size, regardless of how much
     * content it holds.
     */
    @Test
    fun flowPageNeverGrows() {
        val page = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage(
            raw = _root_ide_package_.org.pcsoft.framework.simplay.engine.model.FlowPage(layout = _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout),
            pageIndex = 0,
            blocks = listOf(
                _root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.measuredBlockReaching(
                    400.0
                )
            ),
        )

        assertEquals(_root_ide_package_.org.pcsoft.framework.simplay.engine.measure.MeasureTestData.pageLayout.size, page.effectiveSize)
    }
}
