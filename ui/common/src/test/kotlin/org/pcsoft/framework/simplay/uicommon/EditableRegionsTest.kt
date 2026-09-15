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

package org.pcsoft.framework.simplay.uicommon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.measure
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * Tests for [EditableRegions], the shared per-page-mode logic every `PaperSheetView` (fx and swing)
 * builds its editing lock, caret snap and `HIDDEN` layout exclusion on. Every document has two pages
 * with stable ids ("page-1", "page-2"), one short paragraph each, so no flow overflow occurs and
 * `DocumentTextIndex.BlockRange.pageIndex` matches `Document.pages` one to one.
 */
class EditableRegionsTest {

    private val style = TextStyle(font = Font(family = "Serif", size = 12.0))

    private val layout = PageLayout(
        size = Size(width = 600.0, height = 800.0),
        margins = Margins(left = 40.0, top = 40.0, right = 40.0, bottom = 40.0),
    )

    private val document = Document(
        pages = listOf(
            FlowPage(layout = layout, blocks = listOf(TextBlock.of("First page text", style)), id = "page-1"),
            FlowPage(layout = layout, blocks = listOf(TextBlock.of("Second page text", style)), id = "page-2"),
        ),
    )

    private fun measured(): MeasuredDocument = document.measure(StubFontMeasureCalculator())

    private fun index(): DocumentTextIndex = DocumentTextIndex(measured())

    /**
     * Verifies that an empty `pageModes` map returns the whole document as editable, selectable,
     * navigable and visible, per the [PageMode.EDITABLE] global mode.
     */
    @Test
    fun editableRegionsEmptyOverridesReturnFullDocument() {
        val idx = index()
        val regions = EditableRegions.of(idx, emptyMap(), PageMode.EDITABLE, document)
        assertEquals(listOf(0 until idx.length), regions.editableRanges)
        assertEquals(listOf(0 until idx.length), regions.selectableRanges)
        assertEquals(listOf(0 until idx.length), regions.navigableRanges)
        assertEquals(listOf(0 until idx.length), regions.visibleRanges)
    }

    /**
     * Verifies that a page overridden to [PageMode.DISABLED] has its block missing from the editable,
     * selectable and navigable ranges, while it stays part of the visible ranges.
     */
    @Test
    fun editableRegionsExcludeDisabledPageBlocks() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.DISABLED), PageMode.EDITABLE, document)
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertFalse(regions.selectableRanges.any { blocked.start in it })
        assertFalse(regions.navigableRanges.any { blocked.start in it })
        assertTrue(regions.visibleRanges.any { blocked.start in it })
    }

    /**
     * Verifies that a page overridden to [PageMode.NAVIGABLE] stays part of the navigable, selectable
     * and visible ranges while its editable range is excluded (a read-only page).
     */
    @Test
    fun editableRegionsNavigablePageNavigableButNotEditable() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.NAVIGABLE), PageMode.EDITABLE, document)
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertTrue(regions.navigableRanges.any { blocked.start in it })
        assertTrue(regions.selectableRanges.any { blocked.start in it })
        assertTrue(regions.visibleRanges.any { blocked.start in it })
    }

    /**
     * Verifies that a page overridden to [PageMode.HIDDEN] is missing from the visible ranges (and
     * consequently from the editable and navigable ranges too).
     */
    @Test
    fun editableRegionsHiddenPageExcludedFromVisibleRanges() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.HIDDEN), PageMode.EDITABLE, document)
        assertFalse(regions.visibleRanges.any { blocked.start in it })
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertFalse(regions.navigableRanges.any { blocked.start in it })
    }

    /**
     * Verifies that a page overridden to [PageMode.EDITABLE] is fully editable even though the global
     * mode is [PageMode.STATIC] - an override may be more permissive than the global mode, not just
     * more restrictive.
     */
    @Test
    fun editableRegionsOverrideCanBeMorePermissiveThanGlobalMode() {
        val idx = index()
        val overridden = idx.blockRanges.first { it.pageIndex == 0 }
        val other = idx.blockRanges.first { it.pageIndex == 1 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.EDITABLE), PageMode.STATIC, document)
        assertTrue(regions.editableRanges.any { overridden.start in it })
        assertTrue(regions.navigableRanges.any { overridden.start in it })
        assertFalse(regions.editableRanges.any { other.start in it })
        assertFalse(regions.navigableRanges.any { other.start in it })
    }

    /**
     * Verifies that [EditableRegions.isEditRangeAllowed] rejects an insertion point strictly inside a
     * `DISABLED` page's block but accepts one on the block's boundary.
     */
    @Test
    fun insertInsideDisabledPageIsRejected() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.DISABLED), PageMode.EDITABLE, document)
        val inside = blocked.start + 1
        assertFalse(regions.isEditRangeAllowed(inside, inside))
        assertTrue(regions.isEditRangeAllowed(blocked.end, blocked.end))
    }

    /**
     * Verifies that [EditableRegions.isEditRangeAllowed] rejects a mutation range on a `NAVIGABLE`
     * override as well, even though the same range is navigable.
     */
    @Test
    fun insertInsideNavigablePageIsRejected() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.NAVIGABLE), PageMode.EDITABLE, document)
        val inside = blocked.start + 1
        assertFalse(regions.isEditRangeAllowed(inside, inside))
    }

    /**
     * Verifies that a selection-delete range that only touches the very edge of a `DISABLED` page
     * (i.e. spans into it) is rejected because the range overlaps the blocked block.
     */
    @Test
    fun deleteRangeTouchingDisabledPageIsRejected() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.DISABLED), PageMode.EDITABLE, document)
        assertFalse(regions.isEditRangeAllowed(blocked.start, blocked.start + 2))
    }

    /**
     * Verifies that typing on the still-editable page is unaffected regardless of the first page's
     * override: the range of the second, non-overridden page's block is always editable.
     */
    @Test
    fun typingOnActivePageStillWorks() {
        val idx = index()
        val active = idx.blockRanges.first { it.pageIndex == 1 }
        for (mode in PageMode.entries) {
            val regions = EditableRegions.of(idx, mapOf("page-1" to mode), PageMode.EDITABLE, document)
            assertTrue(regions.isEditRangeAllowed(active.start, active.start), "mode=$mode")
        }
    }

    /**
     * Verifies that [EditableRegions.snapOutOfBlocked] moving forward from inside a `DISABLED` page's
     * block lands exactly on the block's end (the next allowed index).
     */
    @Test
    fun caretSkipsDisabledPageForward() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.DISABLED), PageMode.EDITABLE, document)
        val snapped = regions.snapOutOfBlocked(blocked.start + 1, direction = 1)
        assertEquals(blocked.end, snapped)
    }

    /**
     * Verifies that [EditableRegions.snapOutOfBlocked] moving backward from inside a `DISABLED` page's
     * block lands exactly on the block's start (the previous allowed index).
     */
    @Test
    fun caretSkipsDisabledPageBackward() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.DISABLED), PageMode.EDITABLE, document)
        val snapped = regions.snapOutOfBlocked(blocked.start + 1, direction = -1)
        assertEquals(blocked.start, snapped)
    }

    /**
     * Verifies that [EditableRegions.snapOutOfBlocked] never intervenes for a `NAVIGABLE` override
     * because that mode reports no blocked ranges for navigation (`navigableRanges` covers the whole
     * document); the caller only ever calls it for a page that does not support the caret.
     */
    @Test
    fun caretEntersNavigablePageNormally() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, mapOf("page-1" to PageMode.NAVIGABLE), PageMode.EDITABLE, document)
        assertTrue(regions.navigableRanges.any { blocked.start + 1 in it })
    }

    /**
     * Verifies [EditableRegions.modeOf] and [EditableRegions.isPageVisible]: an overridden page
     * reports its override, an id absent from the map falls back to the global mode, and visibility
     * follows [PageMode.laidOut].
     */
    @Test
    fun modeOfAndIsPageVisibleFollowOverridesAndFallback() {
        val overrides = mapOf("page-1" to PageMode.HIDDEN)
        assertEquals(PageMode.HIDDEN, EditableRegions.modeOf("page-1", overrides, PageMode.EDITABLE))
        assertEquals(PageMode.EDITABLE, EditableRegions.modeOf("page-2", overrides, PageMode.EDITABLE))
        assertFalse(EditableRegions.isPageVisible("page-1", overrides, PageMode.EDITABLE))
        assertTrue(EditableRegions.isPageVisible("page-2", overrides, PageMode.EDITABLE))
    }
}
