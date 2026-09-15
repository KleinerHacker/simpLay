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

package org.pcsoft.framework.simplay.swing

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [DocumentImageRenderer], the fixed-document image renderer of the `swing` module.
 */
class DocumentImageRendererTest {

    /**
     * Verifies that [DocumentImageRenderer.pageCount] reflects the measured page count and that
     * [DocumentImageRenderer.documentImageSize] scales with [ImageRenderConfiguration.unitScale].
     */
    @Test
    fun reportsPageCountAndScaledSize() {
        val atOne = DocumentImageRenderer.of(TestDocuments.short) { unitScale = 1.0 }
        val atTwo = DocumentImageRenderer.of(TestDocuments.short) { unitScale = 2.0 }
        assertTrue(atOne.pageCount >= 1)
        assertEquals(atOne.documentImageSize.width * 2, atTwo.documentImageSize.width)
        assertEquals(atOne.documentImageSize.height * 2, atTwo.documentImageSize.height)
    }

    /**
     * Verifies that [DocumentImageRenderer.renderDocument] returns an image of exactly the reported
     * document image size.
     */
    @Test
    fun renderDocumentMatchesReportedSize() {
        val renderer = DocumentImageRenderer.of(TestDocuments.short)
        val image = renderer.renderDocument()
        assertEquals(renderer.documentImageSize.width, image.width)
        assertEquals(renderer.documentImageSize.height, image.height)
    }

    /**
     * Verifies that [DocumentImageRenderer.renderPage] draws a single page at the size reported by
     * [DocumentImageRenderer.pageImageSizes].
     */
    @Test
    fun renderPageMatchesPageSize() {
        val renderer = DocumentImageRenderer.of(TestDocuments.short) { unitScale = 1.5 }
        val expected = renderer.pageImageSizes[0]
        val image = renderer.renderPage(0)
        assertEquals(expected.width, image.width)
        assertEquals(expected.height, image.height)
    }

    /**
     * Verifies that an out-of-range page index raises [IllegalArgumentException].
     */
    @Test
    fun renderPageRejectsBadIndex() {
        val renderer = DocumentImageRenderer.of(TestDocuments.short)
        assertFailsWith<IllegalArgumentException> { renderer.renderPage(renderer.pageCount) }
    }

    /**
     * Verifies that a non-positive unit scale is rejected by the [DocumentImageRenderer.of] factory.
     */
    @Test
    fun factoryRejectsNonPositiveUnitScale() {
        assertFailsWith<IllegalArgumentException> {
            DocumentImageRenderer.of(TestDocuments.short) { unitScale = 0.0 }
        }
    }

    /**
     * Verifies that top-center page numbering paints a dark pixel near its planned anchor in the top
     * margin band, which the plain (unnumbered) document never draws there.
     */
    @Test
    fun renderPagePaintsPageNumberAtExpectedEdge() {
        val plainImage = DocumentImageRenderer.of(TestDocuments.short).renderPage(0)
        val numberedImage = DocumentImageRenderer.of(TestDocuments.numberedShort()).renderPage(0)

        val expectedX = TestDocuments.layout.size.width / 2.0
        val expectedY = TestDocuments.layout.margins.top / 2.0

        assertFalse(
            hasDarkPixelNear(plainImage, expectedX, expectedY),
            "unnumbered document should not paint anything near the numbering anchor",
        )
        assertTrue(
            hasDarkPixelNear(numberedImage, expectedX, expectedY),
            "numbered document should paint the page number near its planned anchor",
        )
    }

    /**
     * Scans a small box around ([cx], [cy]) on [image] for a pixel darker than mid-gray, the
     * signature of drawn black text over the image's transparent/white background.
     */
    private fun hasDarkPixelNear(image: java.awt.image.BufferedImage, cx: Double, cy: Double): Boolean {
        for (dx in -10..10) {
            for (dy in -7..7) {
                val x = (cx + dx).toInt()
                val y = (cy + dy).toInt()
                if (x < 0 || y < 0 || x >= image.width || y >= image.height) continue
                val color = Color(image.getRGB(x, y), true)
                val alpha = color.alpha / 255.0
                val brightness = (color.red + color.green + color.blue) / (3.0 * 255.0)
                if (alpha > 0.1 && brightness < 0.5) return true
            }
        }
        return false
    }
}
