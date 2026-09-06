package org.pcsoft.framework.simplay.engine.measure

import kotlin.math.max
import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Page
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * A raw [Page] with its blocks replaced by measured blocks.
 *
 * The unchanged [layout] is forwarded to [raw] by hand. The block list is exposed only as the
 * measured [blocks]; the raw blocks stay reachable via `raw.blocks`. [contentArea],
 * [requiredContentHeight] and [effectiveSize] are derived on the fly. Not persistable.
 *
 * @property raw the wrapped raw page.
 * @property layout the raw page frame (forwarded from [raw]).
 * @property pageIndex zero-based position of this page in its document.
 * @property blocks the measured blocks placed on this page, in order.
 * @property contentArea the rectangle available for content: the layout box shrunk by the margins,
 *   relative to the page origin. Purely a function of [layout].
 * @property requiredContentHeight the content height actually needed, i.e. the bottom edge of the
 *   lowest measured block in [contentArea] coordinates (`0.0` when the page holds no blocks).
 * @property effectiveSize the size the page occupies after layout: for a flow page always the
 *   layout size, for a single page the layout width with a height grown to the content.
 */
sealed interface MeasuredPage {
    val raw: Page
    val pageIndex: Int
    val blocks: List<MeasuredTextBlock>

    val layout: PageLayout
        get() = raw.layout

    val contentArea: Rect
        get() = Rect(
            layout.margins.left,
            layout.margins.top,
            layout.contentWidth,
            layout.contentHeight,
        )

    val requiredContentHeight: Double
        get() = blocks.maxOfOrNull { it.bounds.y + it.bounds.height } ?: 0.0

    val effectiveSize: Size
}

/**
 * A measured [FlowPage]. Its [effectiveSize] is always the raw layout size.
 */
class MeasuredFlowPage(
    override val raw: FlowPage,
    override val pageIndex: Int,
    override val blocks: List<MeasuredTextBlock>,
) : MeasuredPage {

    override val effectiveSize: Size
        get() = layout.size
}

/**
 * A measured [SinglePage]. Its [effectiveSize] keeps the layout width but grows in height to fit
 * [requiredContentHeight] plus the vertical margins.
 */
class MeasuredSinglePage(
    override val raw: SinglePage,
    override val pageIndex: Int,
    override val blocks: List<MeasuredTextBlock>,
) : MeasuredPage {

    override val effectiveSize: Size
        get() = Size(
            layout.size.width,
            max(layout.size.height, layout.margins.top + requiredContentHeight + layout.margins.bottom),
        )
}
