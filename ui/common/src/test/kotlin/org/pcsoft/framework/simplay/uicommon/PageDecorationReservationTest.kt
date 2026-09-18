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

import org.pcsoft.framework.simplay.engine.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [computePageDecorationReservation] and [resolveReservedMargins], the shared logic every
 * `PaperSheetView` (fx and swing) uses to grow the `outerMargin`/`pageGap` band to fit a page
 * decoration's natural size, when that decoration opts into reservation.
 */
class PageDecorationReservationTest {

    @Test
    fun `empty list yields an all-zero reservation`() {
        val reservation = computePageDecorationReservation(emptyList())
        assertEquals(EdgeReservation(), reservation)
    }

    @Test
    fun `reservation is the max size per edge across multiple decorations`() {
        val entries = listOf(
            Triple(PageEdge.TOP, Size(width = 100.0, height = 20.0), true),
            Triple(PageEdge.TOP, Size(width = 100.0, height = 50.0), true),
            Triple(PageEdge.LEFT, Size(width = 30.0, height = 100.0), true),
            Triple(PageEdge.LEFT, Size(width = 10.0, height = 100.0), true),
        )
        val reservation = computePageDecorationReservation(entries)
        assertEquals(50.0, reservation.top)
        assertEquals(0.0, reservation.bottom)
        assertEquals(30.0, reservation.left)
        assertEquals(0.0, reservation.right)
    }

    @Test
    fun `decorations that opt out of reservation are excluded`() {
        val entries = listOf(
            Triple(PageEdge.TOP, Size(width = 100.0, height = 999.0), false),
            Triple(PageEdge.TOP, Size(width = 100.0, height = 20.0), true),
        )
        val reservation = computePageDecorationReservation(entries)
        assertEquals(20.0, reservation.top)
    }

    @Test
    fun `resolved margin equals outerMargin when reservation is below it`() {
        val margins = resolveReservedMargins(24.0, EdgeReservation(top = 10.0, left = 5.0))
        assertEquals(24.0, margins.top)
        assertEquals(24.0, margins.left)
        assertEquals(24.0, margins.bottom)
        assertEquals(24.0, margins.right)
    }

    @Test
    fun `resolved margin equals reservation when it exceeds outerMargin`() {
        val margins = resolveReservedMargins(24.0, EdgeReservation(top = 80.0, bottom = 60.0))
        assertEquals(80.0, margins.top)
        assertEquals(60.0, margins.bottom)
        assertEquals(24.0, margins.left)
        assertEquals(24.0, margins.right)
    }
}
