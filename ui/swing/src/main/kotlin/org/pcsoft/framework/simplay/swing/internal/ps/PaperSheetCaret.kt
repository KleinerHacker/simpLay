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

package org.pcsoft.framework.simplay.swing.internal.ps

import java.awt.Rectangle
import javax.swing.Timer
import kotlin.math.roundToInt
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredLine
import org.pcsoft.framework.simplay.swing.PaperSheetMode
import org.pcsoft.framework.simplay.swing.PaperSheetView
import org.pcsoft.framework.simplay.swing.CaretModel
import org.pcsoft.framework.simplay.swing.internal.SwingFontMeasureCalculator
import org.pcsoft.framework.simplay.uicommon.DocumentTextIndex
import org.pcsoft.framework.simplay.uicommon.EditableRegions
import org.pcsoft.framework.simplay.uicommon.PageDeactivationMode
import org.pcsoft.framework.simplay.uicommon.hitTest
import org.pcsoft.framework.simplay.uicommon.segmentSpanX

/**
 * Everything the edit caret of a [PaperSheetView] needs: its [position] on the linear document text,
 * the blink (a hard on/off [Timer], or a smooth opacity fade when [PaperSheetView.smoothCaretBlink]
 * is on), the caret geometry, the caret-navigation moves and the [CaretModel.Commands] sink for the
 * public [CaretModel]. The Swing counterpart of the `fx` module's `PaperSheetCaret`.
 *
 * A per-view helper `BasicPaperSheetUI` creates once and forwards mouse, keyboard and lifecycle
 * events to. The caret reads the measured document, the text index, the page tops, the scroll offset
 * and the [PaperSheetView] settings through the supplied accessors, shares the delegate's selection
 * (so `Shift` + navigation can extend it) and asks the delegate to repaint through [requestRedraw]
 * and to keep the caret visible through [scrollCaretIntoView].
 *
 * In [PageDeactivationMode.DISABLED] and [PageDeactivationMode.HIDDEN] every plain (non-`Shift`)
 * [setCaret] move snaps out of a deactivated page in the direction of travel, per
 * [EditableRegions.snapOutOfBlocked], and [moveVertical] skips that page's lines entirely; a `Shift`
 * selection may still span it.
 */
internal class PaperSheetCaret(
    private val view: PaperSheetView,
    private val selection: PaperSheetSelection,
    private val measurer: SwingFontMeasureCalculator,
    private val textIndex: () -> DocumentTextIndex?,
    private val measuredDocument: () -> MeasuredDocument?,
    private val pageTops: () -> DoubleArray,
    private val scrollOffset: () -> Double,
    private val requestRedraw: () -> Unit,
    private val scrollCaretIntoView: () -> Unit,
) : CaretModel.Commands {

    /** Caret offset into the linear document text. */
    var position: Int = 0
        private set

    private var desiredX: Double? = null
    private var shiftAnchor: Int? = null
    private var blinkOn = false
    private var dropPreview: Int? = null

    /** Caret opacity `0..1`, animated by the smooth blink; `1` for the hard blink. */
    private var opacity: Double = 1.0

    private var hardTimer: Timer? = null
    private var smoothTimer: Timer? = null
    private var smoothPhase: Double = 0.0
    private var smoothRising: Boolean = false

    /** Whether [mode] takes the deactivated pages out of plain caret navigation. */
    private fun blocksNavigation(mode: PageDeactivationMode): Boolean =
        mode == PageDeactivationMode.DISABLED || mode == PageDeactivationMode.HIDDEN

    /** Whether the current [PaperSheetMode] shows and navigates a caret at all. */
    private val caretActive: Boolean get() = view.mode.supportsCaret

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
    fun viewportBounds(): Rectangle? {
        val doc = measuredDocument() ?: return null
        val g = geom(position) ?: return null
        if (g.pageIndex !in doc.pages.indices) return null
        val zoom = view.zoom
        val outer = view.outerMargin
        val contentArea = doc.pages[g.pageIndex].contentArea
        val absX = outer + contentArea.x + g.xContent
        val absYTop = outer + pageTops()[g.pageIndex] + contentArea.y + g.yContent
        return Rectangle(
            (absX * zoom).roundToInt(),
            (absYTop * zoom - scrollOffset()).roundToInt(),
            CARET_WIDTH_PX.roundToInt(),
            (g.height * zoom).roundToInt(),
        )
    }

    /** Opacity `0..1` the caret is painted with right now, honouring mode, focus, blink phase and fade. */
    fun currentOpacity(): Double = when {
        dropPreview != null -> 1.0
        !caretActive -> 0.0
        !view.isFocusOwner -> 0.0
        view.smoothCaretBlink -> opacity.coerceIn(0.0, 1.0)
        !blinkOn -> 0.0
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

    private fun stopTimers() {
        hardTimer?.stop(); hardTimer = null
        smoothTimer?.stop(); smoothTimer = null
    }

    /** Restarts the blink, picking the hard or smooth variant and resetting to a fully visible caret. */
    fun restartBlink() {
        stopTimers()
        blinkOn = caretActive && view.isFocusOwner
        opacity = 1.0
        smoothPhase = 1.0
        smoothRising = false
        if (blinkOn) {
            if (view.smoothCaretBlink) {
                smoothTimer = Timer(SMOOTH_STEP_MILLIS) {
                    val delta = SMOOTH_STEP_MILLIS.toDouble() / SMOOTH_BLINK_MILLIS
                    smoothPhase += if (smoothRising) delta else -delta
                    if (smoothPhase <= 0.0) { smoothPhase = 0.0; smoothRising = true }
                    if (smoothPhase >= 1.0) { smoothPhase = 1.0; smoothRising = false }
                    opacity = easeBoth(smoothPhase)
                    requestRedraw()
                }.apply { isRepeats = true; start() }
            } else {
                hardTimer = Timer(HARD_BLINK_MILLIS) {
                    blinkOn = !blinkOn
                    requestRedraw()
                }.apply { isRepeats = true; start() }
            }
        }
        requestRedraw()
    }

    /** Stops the blink; call from the delegate's `uninstallUI`. */
    fun dispose() = stopTimers()

    //endregion

    //region Lifecycle from the delegate

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
     * the new position (and may span a `DISABLED` page); without it the selection collapses and the
     * new position is snapped out of a `DISABLED` page's block in the direction of travel.
     * [keepDesiredX] preserves the wish-x for consecutive vertical moves.
     */
    fun setCaret(index: Int, extend: Boolean, keepDesiredX: Boolean = false) {
        val idx = textIndex() ?: return
        val rawClamped = idx.clamp(index)
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
     * `target` when [PaperSheetView.deactivatedPageHandling] keeps the deactivated pages navigable or
     * there is nothing deactivated; otherwise `target` snapped out of a blocked range in the direction
     * of travel from `from`.
     */
    private fun snapOutOfDeactivated(target: Int, from: Int): Int {
        val mode = view.deactivatedPageHandling
        if (!blocksNavigation(mode)) return target
        val ids = view.deactivatedPageIds
        if (ids.isEmpty()) return target
        val idx = textIndex() ?: return target
        val doc = view.document ?: return target
        val direction = if (target >= from) 1 else -1
        return EditableRegions.of(idx, ids, mode, doc).snapOutOfBlocked(target, direction)
    }

    /** Whether the caret may come to rest on the page with [pageId] under the current mode. */
    private fun isPageNavigable(pageId: String): Boolean =
        !blocksNavigation(view.deactivatedPageHandling) || pageId !in view.deactivatedPageIds

    /** The measured lines a vertical move may land on: every line outside a deactivated page. */
    private fun navigableLines(idx: DocumentTextIndex): List<MeasuredLine> =
        idx.segments.filter { isPageNavigable(it.page.raw.id) }.map { it.line }.distinct()

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
     * The [lines] index a vertical move starts from when the caret sits on a deactivated page: the last
     * navigable line before it for a downward move, the first one after it for an upward move; `-1`
     * when there is none.
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

    //endregion

    //region CaretModel.Commands (public model commands)

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

    //endregion

    private companion object {

        const val HARD_BLINK_MILLIS = 530
        const val SMOOTH_BLINK_MILLIS = 900.0
        const val SMOOTH_STEP_MILLIS = 25
        const val CARET_WIDTH_PX = 2.0

        /** Symmetric ease-in-out over `0..1`. */
        fun easeBoth(t: Double): Double {
            val x = t.coerceIn(0.0, 1.0)
            return if (x < 0.5) 2.0 * x * x else 1.0 - (-2.0 * x + 2.0) * (-2.0 * x + 2.0) / 2.0
        }
    }
}
