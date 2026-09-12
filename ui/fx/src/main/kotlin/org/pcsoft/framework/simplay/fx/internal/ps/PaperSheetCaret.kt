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

package org.pcsoft.framework.simplay.fx.internal.ps

import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.util.Duration
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.fx.CaretModel
import org.pcsoft.framework.simplay.fx.PaperSheetView
import org.pcsoft.framework.simplay.fx.asPageMode
import org.pcsoft.framework.simplay.fx.internal.FxFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.*

/**
 * Everything the edit caret of a [PaperSheetView] needs: its [position] on the linear document text,
 * the blink (a hard on/off [Timeline], or a smooth opacity fade when [PaperSheetView.smoothCaretBlink]
 * is on), the caret geometry (paint rectangle, viewport bounds), the caret-navigation moves and the
 * [PaperSheetView.CaretCommands] sink for the public [CaretModel].
 *
 * This is a per-view helper, not a singleton: [PaperSheetViewSkin] creates one instance and forwards
 * the mouse, keyboard and lifecycle events to it. The caret reads the measured document, the text
 * index, the page tops, the scroll offset and the [PaperSheetView] settings through the supplied
 * accessors, shares the skin's [TextSelection] (so `Shift` + navigation can extend it) and asks the
 * skin to repaint through [requestRedraw].
 *
 * @property view the owning view, for its mode / zoom / margin / focus / smooth-blink flag / caret model.
 * @property selection the shared [PaperSheetSelection], extended by the `Shift` + navigation moves.
 * @property measurer the shared font measurer for prefix widths and hit-testing.
 * @property textIndex the current linear text index, or `null` without a document.
 * @property measuredDocument the current measured document, or `null` without a document.
 * @property pageTops unscaled top `y` of each page within the stack (without the outer margin).
 * @property scrollOffset the current vertical scroll offset in viewport pixels.
 * @property requestRedraw repaints the skin's canvas.
 * @property scrollCaretIntoView scrolls the viewport so that the caret stays visible after a move.
 */
internal class PaperSheetCaret(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val measurer: FxFontMeasureCalculator,
    private val textIndex: () -> DocumentTextIndex?,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val requestRedraw: () -> Unit,
    private val scrollCaretIntoView: () -> Unit,
) : PaperSheetView.CaretCommands {

    /** Caret offset into the linear document text. */
    var position: Int = 0
        private set

    /** Held wish-x (content-area coordinates) for vertical caret movement; `null` resets it. */
    private var desiredX: Double? = null

    /** Anchor kept while `Shift` + navigation extends the selection. */
    private var shiftAnchor: Int? = null

    /** Hard-blink phase; `true` means the caret is on. */
    private var blinkOn = false

    /** While a selection is being dragged, the linear index the drop caret previews; else `null`. */
    private var dropPreview: Int? = null

    /** Caret opacity `0..1`, animated by the smooth blink; `1` for the hard blink. */
    private val opacity = SimpleDoubleProperty(this, "opacity", 1.0)

    /** The running blink; rebuilt by [restartBlink] as a hard or a smooth blink. */
    private var blink: Timeline = buildHardBlink()

    init {
        opacity.addListener { _, _, _ -> requestRedraw() }
    }

    /** Whether any page currently shows and navigates a caret at all. */
    private val caretActive: Boolean get() = view.anyCaret

    //region Geometry / painting

    private class Geom(val pageIndex: Int, val xContent: Double, val yContent: Double, val height: Double)

    private fun geom(ci: Int): Geom? {
        val idx = textIndex() ?: return null
        if (idx.segments.isEmpty()) return null
        val c = ci.coerceIn(0, idx.length)
        val seg = idx.segments.lastOrNull { it.start <= c && c <= it.end }
            ?: idx.segments.firstOrNull { it.start >= c }
            ?: idx.segments.last()
        val target = seg.start + (c - seg.start).coerceIn(0, seg.part.text.length)
        val (x0, _) = segmentSpanX(seg, target, target, measurer)
        return Geom(seg.pageIndex, x0, seg.line.lineBox.y, seg.line.lineBox.height)
    }

    /** The caret rectangle to paint (in page content-area coordinates), or `null` when hidden. */
    fun caretPaint(): CaretPaint? {
        val preview = dropPreview
        if (preview == null && !caretActive) return null
        val g = geom(preview ?: position) ?: return null
        return CaretPaint(g.pageIndex, g.xContent, g.yContent, g.height)
    }

    /** The caret rectangle in viewport pixels, or `null` when there is no caret geometry. */
    fun viewportBounds(): Bounds? {
        val doc = measuredDocument() ?: return null
        val g = geom(position) ?: return null
        if (g.pageIndex !in doc.pages.indices) return null
        val zoom = view.zoom
        val outer = view.outerMargin
        val contentArea = doc.pages[g.pageIndex].contentArea
        val absX = outer + contentArea.x + g.xContent
        val absYTop = outer + pageTops()[g.pageIndex] + contentArea.y + g.yContent
        return BoundingBox(absX * zoom, absYTop * zoom - scrollOffset(), CARET_WIDTH_PX, g.height * zoom)
    }

    /** Opacity `0..1` the caret is painted with right now, honouring mode, focus, blink phase and fade. */
    fun currentOpacity(): Double = when {
        dropPreview != null -> 1.0
        !caretActive -> 0.0
        !view.isFocused -> 0.0
        !blinkOn -> 0.0
        view.smoothCaretBlink -> opacity.get().coerceIn(0.0, 1.0)
        else -> 1.0
    }

    /** Writes the caret position, bounds, blink state and structural counts onto [CaretModel]. */
    fun publish() {
        val idx = textIndex()
        val model = view.caretModel
        model.updateCounts(idx?.blockCount ?: 0, idx?.wordCount ?: 0, idx?.symbolCount ?: 0)
        val active = caretActive || dropPreview != null
        model.update(
            position.coerceIn(0, idx?.length ?: 0),
            if (active) viewportBounds() else null,
            active && currentOpacity() > 0.05,
        )
    }

    //endregion

    //region Blink

    private fun buildHardBlink(): Timeline = Timeline(
        KeyFrame(Duration.millis(HARD_BLINK_MILLIS), { blinkOn = !blinkOn; requestRedraw() }),
    ).apply { cycleCount = Timeline.INDEFINITE }

    private fun buildSmoothBlink(): Timeline = Timeline(
        KeyFrame(Duration.ZERO, KeyValue(opacity, 1.0)),
        KeyFrame(Duration.millis(SMOOTH_BLINK_MILLIS), KeyValue(opacity, 0.0, Interpolator.EASE_BOTH)),
    ).apply {
        cycleCount = Timeline.INDEFINITE
        isAutoReverse = true
    }

    /** Restarts the blink, picking the hard or smooth variant and resetting to a fully visible caret. */
    fun restartBlink() {
        blink.stop()
        blink = if (view.smoothCaretBlink) buildSmoothBlink() else buildHardBlink()
        blinkOn = caretActive && view.isFocused
        opacity.set(1.0)
        if (blinkOn) blink.playFromStart()
        requestRedraw()
    }

    /** Stops the blink; call from the skin's `dispose`. */
    fun dispose() = blink.stop()

    //endregion

    //region Lifecycle from the skin

    /**
     * Re-clamps the caret after a re-measure and republishes; does not restart the blink. With
     * [reload] - the document was replaced from outside instead of by an edit - the caret always
     * falls back to the document start.
     */
    fun onDocumentRemeasured(reload: Boolean) {
        position = if (reload) 0 else position.coerceIn(0, textIndex()?.length ?: 0)
        desiredX = null
        shiftAnchor = null
        publish()
    }

    /** Re-clamps the caret, drops any drag preview and restarts the blink after a mode change. */
    fun onModeChanged() {
        position = textIndex()?.clamp(position) ?: 0
        shiftAnchor = null
        dropPreview = null
        restartBlink()
    }

    /** Places the caret at the editor-reported index after an edit and restarts the blink. */
    fun onEditApplied(newPosition: Int) {
        position = textIndex()?.clamp(newPosition) ?: 0
        selection.reset()
        shiftAnchor = null
        desiredX = null
        restartBlink()
        scrollCaretIntoView()
    }

    /** Forgets the `Shift` + navigation anchor (mouse press starts a fresh gesture). */
    fun clearShiftAnchor() {
        shiftAnchor = null
    }

    /** Sets or clears the drop-caret preview shown while a selection is dragged. */
    fun setDropPreview(index: Int?) {
        dropPreview = index
        requestRedraw()
    }

    //endregion

    //region Movement

    /** Moves the caret without touching the selection (a mouse click or press). */
    fun placeCaret(index: Int) {
        val idx = textIndex() ?: return
        position = idx.clamp(index)
        desiredX = null
        shiftAnchor = null
        restartBlink()
        scrollCaretIntoView()
    }

    /**
     * Moves the caret to [index]. With [extend] the selection grows from the kept `Shift` anchor to
     * the new position; without it the selection collapses. [keepDesiredX] preserves the wish-x for
     * consecutive vertical moves.
     */
    fun setCaret(index: Int, extend: Boolean, keepDesiredX: Boolean = false) {
        val idx = textIndex() ?: return
        val rawClamped = idx.clamp(index)
        // A `Shift` selection may span a page the caret may not enter; a plain move snaps out of it.
        val clamped = if (extend) rawClamped else snapOutOfDeactivated(rawClamped, position)
        if (extend) {
            if (shiftAnchor == null) shiftAnchor = position
            selection.setAnchorFocus(idx.clamp(shiftAnchor ?: position), clamped)
        } else {
            shiftAnchor = null
            selection.reset()
        }
        position = clamped
        if (!keepDesiredX) desiredX = null
        restartBlink()
        scrollCaretIntoView()
    }

    /**
     * `target` when every page's effective [PageMode] supports the caret; otherwise `target` snapped
     * out of a page it may not enter, in the direction of travel from `from`.
     */
    private fun snapOutOfDeactivated(target: Int, from: Int): Int {
        val idx = textIndex() ?: return target
        val doc = view.document ?: return target
        val direction = if (target >= from) 1 else -1
        return EditableRegions.of(idx, view.pageModes, view.mode.asPageMode(), doc).snapOutOfBlocked(target, direction)
    }

    /** Whether the caret may come to rest on the page with [pageId] under its effective [PageMode]. */
    private fun isPageNavigable(pageId: String): Boolean = view.effectivePageMode(pageId).supportsCaret

    /** The measured lines a vertical move may land on: every line whose page allows the caret. */
    private fun navigableLines(idx: DocumentTextIndex): List<MeasuredLine> =
        idx.segments.filter { isPageNavigable(it.page.raw.id) }.map { it.line }.distinct()

    /** The navigable measured-page indices, in reading order: every page whose effective [PageMode]
     * allows the caret to enter it. */
    private fun navigablePageIndices(idx: DocumentTextIndex): List<Int> =
        idx.segments.filter { isPageNavigable(it.page.raw.id) }.map { it.pageIndex }.distinct().sorted()

    /** The measured lines of page [pageIndex], in reading order. */
    private fun linesOfPage(idx: DocumentTextIndex, pageIndex: Int): List<MeasuredLine> =
        idx.segments.filter { it.pageIndex == pageIndex }.map { it.line }.distinct()

    private fun currentSeg(): DocumentTextIndex.Segment? {
        val idx = textIndex() ?: return null
        if (idx.segments.isEmpty()) return null
        return idx.segments.lastOrNull { it.start <= position && position <= it.end }
            ?: idx.segments.firstOrNull { it.start >= position }
            ?: idx.segments.last()
    }

    private fun lineBounds(seg: DocumentTextIndex.Segment): Pair<Int, Int> {
        val idx = textIndex()!!
        val same = idx.segments.filter { it.line === seg.line }
        return same.minOf { it.start } to same.maxOf { it.end }
    }

    /** First and one-past-last linear index of the line the caret sits on, or `null`. */
    fun currentLineBounds(): Pair<Int, Int>? = currentSeg()?.let { lineBounds(it) }

    private fun boundaryStep(from: Int, dir: Int): Int {
        val idx = textIndex() ?: return from
        var p = from.coerceIn(0, idx.length)
        while (p in 1 until idx.length &&
            idx.segments.none { it.start == p || it.end == p || (it.start < p && p < it.end) }
        ) {
            p += dir
        }
        return p.coerceIn(0, idx.length)
    }

    fun moveHorizontal(dir: Int, extend: Boolean) {
        val idx = textIndex() ?: return
        setCaret(boundaryStep((position + dir).coerceIn(0, idx.length), dir), extend)
    }

    fun moveLineStart(extend: Boolean) {
        val seg = currentSeg() ?: return
        setCaret(lineBounds(seg).first, extend)
    }

    fun moveLineEnd(extend: Boolean) {
        val seg = currentSeg() ?: return
        setCaret(lineBounds(seg).second, extend)
    }

    fun moveDocStart(extend: Boolean) = setCaret(0, extend)

    fun moveDocEnd(extend: Boolean) = setCaret(textIndex()?.length ?: 0, extend)

    fun moveWordLeft(extend: Boolean) = setCaret(textIndex()?.prevWordStart(position) ?: position, extend)

    fun moveWordRight(extend: Boolean) = setCaret(textIndex()?.nextWordStart(position) ?: position, extend)

    fun moveVertical(delta: Int, extend: Boolean) {
        val idx = textIndex() ?: return
        val seg = currentSeg() ?: return
        val lines = navigableLines(idx).ifEmpty { idx.segments.map { it.line }.distinct() }
        val li = lines.indexOf(seg.line).takeIf { it >= 0 } ?: nearestNavigableLine(idx, lines, seg, delta)
        val target = li + delta
        if (li < 0 || target !in lines.indices) return
        if (desiredX == null) desiredX = geom(position)?.xContent ?: 0.0
        val dx = desiredX!!
        val targetLine = lines[target]
        val lineSegs = idx.segments.filter { it.line === targetLine }
        if (lineSegs.isEmpty()) return
        val partSeg = lineSegs.minByOrNull { s ->
            val b = s.part.bounds
            when {
                dx < b.x -> b.x - dx
                dx > b.x + b.width -> dx - b.x - b.width
                else -> 0.0
            }
        } ?: return
        val off = hitTest(partSeg.part, partSeg.font, dx, measurer).coerceIn(0, partSeg.part.text.length)
        setCaret(partSeg.start + off, extend, keepDesiredX = true)
    }

    /**
     * The [lines] index a vertical move starts from when the caret sits on a page it may not enter:
     * the last navigable line before it for a downward move, the first one after it for an upward
     * move; `-1` when there is none.
     */
    private fun nearestNavigableLine(
        idx: DocumentTextIndex,
        lines: List<MeasuredLine>,
        seg: DocumentTextIndex.Segment,
        delta: Int,
    ): Int {
        val all = idx.segments.map { it.line }.distinct()
        val current = all.indexOf(seg.line)
        return if (delta >= 0) {
            lines.indexOfLast { all.indexOf(it) < current }
        } else {
            lines.indexOfFirst { all.indexOf(it) > current }
        }
    }

    /**
     * Moves the caret to the previous ([delta] `< 0`) or next ([delta] `> 0`) navigable page, at the
     * same line ordinal it held on the source page (clamped to the target page's last line) and the
     * same wish-x a vertical move would keep. Stops - does not wrap - at the first/last navigable
     * page, so repeated calls reach every navigable page in order.
     */
    fun movePage(delta: Int, extend: Boolean) {
        val idx = textIndex() ?: return
        val seg = currentSeg() ?: return
        val pages = navigablePageIndices(idx).ifEmpty { idx.segments.map { it.pageIndex }.distinct().sorted() }
        val pi = pages.indexOf(seg.pageIndex).takeIf { it >= 0 } ?: nearestNavigablePage(pages, seg, delta)
        val target = pi + delta
        if (pi < 0 || target !in pages.indices) return
        if (desiredX == null) desiredX = geom(position)?.xContent ?: 0.0
        val dx = desiredX!!

        val sourceLines = linesOfPage(idx, seg.pageIndex)
        val lineOrdinal = sourceLines.indexOf(seg.line).let { if (it >= 0) it else 0 }

        val targetLines = linesOfPage(idx, pages[target])
        if (targetLines.isEmpty()) return
        val targetLine = targetLines[lineOrdinal.coerceIn(0, targetLines.lastIndex)]

        val lineSegs = idx.segments.filter { it.line === targetLine }
        if (lineSegs.isEmpty()) return
        val partSeg = lineSegs.minByOrNull { s ->
            val b = s.part.bounds
            when {
                dx < b.x -> b.x - dx
                dx > b.x + b.width -> dx - b.x - b.width
                else -> 0.0
            }
        } ?: return
        val off = hitTest(partSeg.part, partSeg.font, dx, measurer).coerceIn(0, partSeg.part.text.length)
        setCaret(partSeg.start + off, extend, keepDesiredX = true)
    }

    /**
     * The [pages] index a page move starts from when the caret sits on a page it may not enter: the
     * last navigable page before it for a forward move, the first one after it for a backward move;
     * `-1` when there is none.
     */
    private fun nearestNavigablePage(pages: List<Int>, seg: DocumentTextIndex.Segment, delta: Int): Int {
        val current = seg.pageIndex
        return if (delta >= 0) pages.indexOfLast { it < current } else pages.indexOfFirst { it > current }
    }

    //endregion

    //region CaretCommands (public model commands)

    private fun go(target: Int) = setCaret(target, extend = false)

    override fun moveTo(index: Int) = go(textIndex()?.clamp(index) ?: 0)
    override fun moveToStart() = go(0)
    override fun moveToEnd() = go(textIndex()?.length ?: 0)
    override fun moveIntoBlock(block: Int, index: Int) = go(textIndex()?.offsetInBlock(block, index) ?: 0)
    override fun moveToStartOfBlock(block: Int) = go(textIndex()?.startOfBlock(block) ?: 0)
    override fun moveToEndOfBlock(block: Int) = go(textIndex()?.endOfBlock(block) ?: 0)
    override fun moveIntoWord(word: Int, index: Int) = go(textIndex()?.offsetInWord(word, index) ?: 0)
    override fun moveToStartOfWord(word: Int) = go(textIndex()?.startOfWord(word) ?: 0)
    override fun moveToEndOfWord(word: Int) = go(textIndex()?.endOfWord(word) ?: 0)
    override fun moveToSymbol(symbol: Int) = go(textIndex()?.offsetOfSymbol(symbol) ?: 0)
    override fun moveToStartOfSymbol(symbol: Int) = go(textIndex()?.startOfSymbol(symbol) ?: 0)
    override fun moveToEndOfSymbol(symbol: Int) = go(textIndex()?.endOfSymbol(symbol) ?: 0)
    override fun moveToNextWord() = go(textIndex()?.nextWordStart(position) ?: position)
    override fun moveToPrevWord() = go(textIndex()?.prevWordStart(position) ?: position)
    override fun moveToNextBlock() = go(textIndex()?.nextBlockStart(position) ?: position)
    override fun moveToPrevBlock() = go(textIndex()?.prevBlockStart(position) ?: position)
    override fun moveToNextSymbol() = go(textIndex()?.nextSymbolStart(position) ?: position)
    override fun moveToPrevSymbol() = go(textIndex()?.prevSymbolStart(position) ?: position)
    override fun moveToNextPage() = movePage(1, extend = false)
    override fun moveToPrevPage() = movePage(-1, extend = false)

    //endregion

    private companion object {

        const val HARD_BLINK_MILLIS = 530.0
        const val SMOOTH_BLINK_MILLIS = 900.0
        const val CARET_WIDTH_PX = 2.0
    }
}
