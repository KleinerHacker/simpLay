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
 * Tests for [EditableRegions], the shared page-deactivation logic every `PaperSheetView` (fx and
 * swing) builds its editing lock, caret snap and `HIDDEN` layout exclusion on. Every document has two
 * pages with stable ids ("page-1", "page-2"), one short paragraph each, so no flow overflow occurs and
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
     * Verifies that [PageDeactivationMode.IGNORE] returns the whole document as editable, navigable
     * and visible, even though a page id is marked deactivated.
     */
    @Test
    fun editableRegionsIgnoreModeReturnsFullDocument() {
        val idx = index()
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.IGNORE, document)
        assertEquals(listOf(0 until idx.length), regions.editableRanges)
        assertEquals(listOf(0 until idx.length), regions.navigableRanges)
        assertEquals(listOf(0 until idx.length), regions.visibleRanges)
    }

    /**
     * Verifies that in [PageDeactivationMode.DISABLED] the linear range of the deactivated page's
     * block is missing from both the editable and the navigable ranges.
     */
    @Test
    fun editableRegionsExcludeDisabledPageBlocks() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.DISABLED, document)
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertFalse(regions.navigableRanges.any { blocked.start in it })
        assertTrue(regions.visibleRanges.any { blocked.start in it })
    }

    /**
     * Verifies that in [PageDeactivationMode.READONLY] the deactivated page stays part of the
     * navigable (and visible) ranges while its editable range is excluded.
     */
    @Test
    fun editableRegionsReadonlyPageNavigableButNotEditable() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.READONLY, document)
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertTrue(regions.navigableRanges.any { blocked.start in it })
        assertTrue(regions.visibleRanges.any { blocked.start in it })
    }

    /**
     * Verifies that in [PageDeactivationMode.HIDDEN] the deactivated page is missing from the visible
     * ranges (and consequently from the editable and navigable ranges too).
     */
    @Test
    fun editableRegionsHiddenPageExcludedFromVisibleRanges() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.HIDDEN, document)
        assertFalse(regions.visibleRanges.any { blocked.start in it })
        assertFalse(regions.editableRanges.any { blocked.start in it })
        assertFalse(regions.navigableRanges.any { blocked.start in it })
    }

    /**
     * Verifies that [EditableRegions.isEditRangeAllowed] rejects an insertion point strictly inside a
     * `DISABLED` page's block but accepts one on the block's boundary.
     */
    @Test
    fun insertInsideDisabledPageIsRejected() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.DISABLED, document)
        val inside = blocked.start + 1
        assertFalse(regions.isEditRangeAllowed(inside, inside))
        assertTrue(regions.isEditRangeAllowed(blocked.end, blocked.end))
    }

    /**
     * Verifies that [EditableRegions.isEditRangeAllowed] rejects a mutation range in
     * [PageDeactivationMode.READONLY] as well, even though the same range is navigable.
     */
    @Test
    fun insertInsideReadonlyPageIsRejected() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.READONLY, document)
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
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.DISABLED, document)
        assertFalse(regions.isEditRangeAllowed(blocked.start, blocked.start + 2))
    }

    /**
     * Verifies that typing on the still-active page is unaffected in every mode: the range of the
     * second, non-deactivated page's block is always editable.
     */
    @Test
    fun typingOnActivePageStillWorks() {
        val idx = index()
        val active = idx.blockRanges.first { it.pageIndex == 1 }
        for (mode in PageDeactivationMode.entries) {
            val regions = EditableRegions.of(idx, setOf("page-1"), mode, document)
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
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.DISABLED, document)
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
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.DISABLED, document)
        val snapped = regions.snapOutOfBlocked(blocked.start + 1, direction = -1)
        assertEquals(blocked.start, snapped)
    }

    /**
     * Verifies that [EditableRegions.snapOutOfBlocked] never intervenes in
     * [PageDeactivationMode.READONLY] because that mode reports no blocked ranges for navigation
     * (`navigableRanges` covers the whole document); the caller only ever calls it for `DISABLED`.
     */
    @Test
    fun caretEntersReadonlyPageNormally() {
        val idx = index()
        val blocked = idx.blockRanges.first { it.pageIndex == 0 }
        val regions = EditableRegions.of(idx, setOf("page-1"), PageDeactivationMode.READONLY, document)
        assertTrue(regions.navigableRanges.any { blocked.start + 1 in it })
    }
}
