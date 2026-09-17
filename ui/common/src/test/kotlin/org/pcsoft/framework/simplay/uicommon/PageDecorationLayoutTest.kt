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

import org.pcsoft.framework.simplay.engine.geometry.Rect
import org.pcsoft.framework.simplay.engine.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [resolveDecorationBounds], the shared logic every `PaperSheetView` (fx and swing) uses
 * to place a page decoration just outside a page's edge.
 */
class PageDecorationLayoutTest {

    private val page = Rect(x = 100.0, y = 200.0, width = 400.0, height = 600.0)
    private val decoration = Size(width = 40.0, height = 20.0)

    /**
     * Verifies that a [PageEdge.TOP] decoration defaults to [EdgeAlignment.STRETCH], covering the
     * full page width and sitting directly above the page at zoom `1.0`.
     */
    @Test
    fun `top edge stretches to the page width by default`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.TOP),
            decorationSize = decoration,
            zoom = 1.0,
        )
        assertEquals(Rect(x = 100.0, y = 180.0, width = 400.0, height = 20.0), bounds)
    }

    /**
     * Verifies that a [PageEdge.BOTTOM] decoration sits directly below the page.
     */
    @Test
    fun `bottom edge sits below the page`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.BOTTOM),
            decorationSize = decoration,
            zoom = 1.0,
        )
        assertEquals(Rect(x = 100.0, y = 800.0, width = 400.0, height = 20.0), bounds)
    }

    /**
     * Verifies that a [PageEdge.LEFT] decoration stretches to the page height and sits to the left
     * of the page.
     */
    @Test
    fun `left edge stretches to the page height by default`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.LEFT),
            decorationSize = decoration,
            zoom = 1.0,
        )
        assertEquals(Rect(x = 60.0, y = 200.0, width = 40.0, height = 600.0), bounds)
    }

    /**
     * Verifies that a [PageEdge.RIGHT] decoration sits to the right of the page.
     */
    @Test
    fun `right edge sits right of the page`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.RIGHT),
            decorationSize = decoration,
            zoom = 1.0,
        )
        assertEquals(Rect(x = 500.0, y = 200.0, width = 40.0, height = 600.0), bounds)
    }

    /**
     * Verifies that [EdgeAlignment.START]/[EdgeAlignment.CENTER]/[EdgeAlignment.END] keep the
     * decoration's own size along a horizontal edge and position it at the start, center or end.
     */
    @Test
    fun `alignment positions a non-stretched decoration along a horizontal edge`() {
        val start = resolveDecorationBounds(
            page, PageDecorationPlacement(PageEdge.TOP, EdgeAlignment.START), decoration, zoom = 1.0,
        )
        val center = resolveDecorationBounds(
            page, PageDecorationPlacement(PageEdge.TOP, EdgeAlignment.CENTER), decoration, zoom = 1.0,
        )
        val end = resolveDecorationBounds(
            page, PageDecorationPlacement(PageEdge.TOP, EdgeAlignment.END), decoration, zoom = 1.0,
        )

        assertEquals(100.0, start.x)
        assertEquals(280.0, center.x)
        assertEquals(460.0, end.x)
        assertEquals(40.0, start.width)
        assertEquals(40.0, center.width)
        assertEquals(40.0, end.width)
    }

    /**
     * Verifies that [PageDecorationPlacement.offsetX]/[PageDecorationPlacement.offsetY] shift the
     * decoration further along the edge and further away from the page, at zoom `1.0` where layout
     * points map to viewport pixels via [effectiveZoom].
     */
    @Test
    fun `offsets shift the decoration along and away from the edge`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.TOP, offsetX = 10.0, offsetY = 5.0),
            decorationSize = decoration,
            zoom = 1.0,
        )
        val scale = effectiveZoom(1.0)
        assertEquals(100.0 + 10.0 * scale, bounds.x)
        assertEquals(180.0 - 5.0 * scale, bounds.y)
    }

    /**
     * Verifies that a negative offset shifts the decoration in the opposite direction.
     */
    @Test
    fun `negative offset shifts in the opposite direction`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.RIGHT, offsetX = -10.0, offsetY = -5.0),
            decorationSize = decoration,
            zoom = 1.0,
        )
        val scale = effectiveZoom(1.0)
        assertEquals(500.0 - 10.0 * scale, bounds.x)
        assertEquals(200.0 - 5.0 * scale, bounds.y)
    }

    /**
     * Verifies that a non-default zoom factor scales the offset, not the page bounds or the
     * decoration's own size.
     */
    @Test
    fun `zoom factor scales only the offset`() {
        val bounds = resolveDecorationBounds(
            pageBounds = page,
            placement = PageDecorationPlacement(edge = PageEdge.BOTTOM, offsetY = 10.0),
            decorationSize = decoration,
            zoom = 2.0,
        )
        val scale = effectiveZoom(2.0)
        assertEquals(800.0 + 10.0 * scale, bounds.y)
        assertEquals(400.0, bounds.width)
        assertEquals(20.0, bounds.height)
    }
}
