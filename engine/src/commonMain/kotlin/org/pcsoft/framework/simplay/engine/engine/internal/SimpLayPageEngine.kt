package org.pcsoft.framework.simplay.engine.engine.internal

import org.pcsoft.framework.simplay.engine.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.engine.GreedyWordLineBreakerStrategy
import org.pcsoft.framework.simplay.engine.engine.LineBreakerStrategy
import org.pcsoft.framework.simplay.engine.engine.NoOpWordBreakerStrategy
import org.pcsoft.framework.simplay.engine.engine.UnplacedLine
import org.pcsoft.framework.simplay.engine.engine.WordBreakerStrategy
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredFlowPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.measure.MeasuredSinglePage
import org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.SinglePage

/**
 * Measure stage that turns the pages of a raw [Document] into measured pages.
 *
 * The outermost of the internal `SimpLay*Engine` stages driven by
 * [org.pcsoft.framework.simplay.engine.engine.SimpLayEngine]; created through [builder], which
 * wires in a [SimpLayFontEngine] and a [SimpLayBlockEngine]. A [SinglePage] keeps all its blocks;
 * nothing is clipped and its height grows through the measured model. A [FlowPage] is filled line
 * by line: when the next line would cross the content height the page is closed and a new
 * [MeasuredFlowPage] is opened with a deep copy of the page frame. Page indices run continuously
 * across the whole document.
 */
internal class SimpLayPageEngine private constructor(
    private val fontEngine: SimpLayFontEngine,
    private val blockEngine: SimpLayBlockEngine,
    private val lineBreaker: LineBreakerStrategy,
    private val wordBreaker: WordBreakerStrategy,
    private val measurer: FontMeasureCalculator,
) {

    fun measure(document: Document): MeasuredDocument {
        val pages = mutableListOf<MeasuredPage>()
        var pageIndex = 0

        for (rawPage in document.pages) {
            when (rawPage) {
                is SinglePage -> {
                    pages += MeasuredSinglePage(rawPage, pageIndex++, measureSingle(rawPage))
                }

                is FlowPage -> {
                    measureFlow(rawPage).forEachIndexed { sliceIndex, blocks ->
                        val pageRaw = if (sliceIndex == 0) rawPage else cloneFlowPage(rawPage)
                        pages += MeasuredFlowPage(pageRaw, pageIndex++, blocks)
                    }
                }
            }
        }

        return MeasuredDocument(document, pages)
    }

    private fun measureSingle(page: SinglePage): List<MeasuredTextBlock> {
        val contentWidth = page.layout.contentWidth
        val blocks = mutableListOf<MeasuredTextBlock>()
        var y = 0.0

        for (rawBlock in page.blocks) {
            val style = fontEngine.resolveStyle(rawBlock.style)
            val unplacedLines = lineBreaker.breakIntoLines(
                rawBlock.parts, style.font, contentWidth, measurer, wordBreaker,
            )
            val block = blockEngine.measure(
                rawBlock, style, unplacedLines, y, contentWidth, blockEndsHere = true,
            )
            blocks += block
            y += block.bounds.height
        }

        return blocks
    }

    private fun measureFlow(page: FlowPage): List<List<MeasuredTextBlock>> {
        val contentWidth = page.layout.contentWidth
        val contentHeight = page.layout.contentHeight
        val result = mutableListOf<MutableList<MeasuredTextBlock>>()
        var current = mutableListOf<MeasuredTextBlock>()
        var y = 0.0

        fun closePage() {
            result += current
            current = mutableListOf()
            y = 0.0
        }

        for (rawBlock in page.blocks) {
            val style = fontEngine.resolveStyle(rawBlock.style)
            val lineHeight = style.resolvedLineHeight
            val unplacedLines = lineBreaker.breakIntoLines(
                rawBlock.parts, style.font, contentWidth, measurer, wordBreaker,
            )

            if (unplacedLines.isEmpty()) {
                current += blockEngine.measure(
                    rawBlock, style, emptyList(), y, contentWidth, blockEndsHere = true,
                )
                continue
            }

            var index = 0
            while (index < unplacedLines.size) {
                if (y > 0.0 && y + lineHeight > contentHeight) {
                    closePage()
                }

                val sliceStart = y
                val sliceLines = mutableListOf<UnplacedLine>()
                while (
                    index < unplacedLines.size &&
                    (y + lineHeight <= contentHeight || sliceLines.isEmpty())
                ) {
                    sliceLines += unplacedLines[index]
                    y += lineHeight
                    index++
                }

                val endsHere = index == unplacedLines.size
                current += blockEngine.measure(
                    rawBlock, style, sliceLines, sliceStart, contentWidth, blockEndsHere = endsHere,
                )
                if (!endsHere) {
                    closePage()
                }
            }
        }

        result += current
        return result
    }

    private fun cloneFlowPage(page: FlowPage): FlowPage = page.copy(
        layout = page.layout.copy(
            size = page.layout.size.copy(),
            margins = page.layout.margins.copy(),
        ),
        blocks = emptyList(),
    )

    /**
     * Collects the parts of a [SimpLayPageEngine] and creates it. Obtained from [builder]; the
     * mandatory [FontMeasureCalculator] is passed there. The [SimpLayFontEngine], the
     * [SimpLayBlockEngine] and the two strategies default to fresh / greedy / no-op values.
     */
    class Builder internal constructor(private val measurer: FontMeasureCalculator) {

        private var fontEngine: SimpLayFontEngine? = null
        private var blockEngine: SimpLayBlockEngine? = null
        private var lineBreaker: LineBreakerStrategy = GreedyWordLineBreakerStrategy
        private var wordBreaker: WordBreakerStrategy = NoOpWordBreakerStrategy

        /** Sets the [SimpLayFontEngine]; defaults to a fresh one built from the measurer. */
        fun fontEngine(engine: SimpLayFontEngine): Builder = apply { fontEngine = engine }

        /** Sets the [SimpLayBlockEngine]; defaults to a fresh one. */
        fun blockEngine(engine: SimpLayBlockEngine): Builder = apply { blockEngine = engine }

        /** Sets the [LineBreakerStrategy]; defaults to [GreedyWordLineBreakerStrategy]. */
        fun lineBreakerStrategy(strategy: LineBreakerStrategy): Builder = apply {
            lineBreaker = strategy
        }

        /** Sets the [WordBreakerStrategy]; defaults to [NoOpWordBreakerStrategy]. */
        fun wordBreakerStrategy(strategy: WordBreakerStrategy): Builder = apply {
            wordBreaker = strategy
        }

        /** Builds the page engine, filling in defaults for anything not set. */
        fun build(): SimpLayPageEngine = SimpLayPageEngine(
            fontEngine = fontEngine ?: SimpLayFontEngine.builder(measurer).build(),
            blockEngine = blockEngine ?: SimpLayBlockEngine.builder().build(),
            lineBreaker = lineBreaker,
            wordBreaker = wordBreaker,
            measurer = measurer,
        )
    }

    companion object {

        /** Starts a [Builder] with the mandatory [measurer]. */
        fun builder(measurer: FontMeasureCalculator): Builder = Builder(measurer)
    }
}
