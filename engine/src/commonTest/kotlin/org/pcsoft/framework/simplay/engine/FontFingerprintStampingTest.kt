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

package org.pcsoft.framework.simplay.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

/**
 * End-to-end tests for the authoring step [Document.withFontFingerprints] and the measure-pass
 * summary [org.pcsoft.framework.simplay.engine.measure.MeasuredDocument.fingerprintDeviations].
 */
class FontFingerprintStampingTest {

    private val serif = Font(family = "Serif", size = 12.0)
    private val mono = Font(family = "Mono", size = 12.0)

    private val document = Document(
        listOf(
            FlowPage(
                EngineTestData.pageLayout(),
                listOf(
                    TextBlock.of("First block", TextStyle(font = serif)),
                    TextBlock.of("Second block", TextStyle(font = mono)),
                ),
            ),
        ),
    )

    /** Advance depends on the family, so `Serif` and `Mono` produce different fingerprints. */
    private fun measurer(widthFactor: Double) = FontMeasureCalculator { font, text ->
        val familyBump = if (font.family == "Mono") 0.1 else 0.0
        TextMetrics(
            width = text.length * font.size * (widthFactor + familyBump),
            ascent = font.size * 0.8,
            descent = font.size * 0.2,
        )
    }

    /**
     * Use case: stamping fills a fingerprint into every block font while the text parts and the
     * rest of the document stay unchanged.
     */
    @Test
    fun stampingFillsEveryBlockFontFingerprint() {
        val stamped = document.withFontFingerprints(measurer(0.6))

        val fonts = stamped.pages.flatMap { it.blocks }.map { it.style.font }
        assertTrue(fonts.all { it.fingerprint != null }, "every block font carries a fingerprint")
        assertEquals(
            document.pages.flatMap { it.blocks }.map { it.parts },
            stamped.pages.flatMap { it.blocks }.map { it.parts },
        )
    }

    /**
     * Use case: measuring a freshly stamped document with the same text stack reports no deviations.
     */
    @Test
    fun measuringStampedDocumentWithSameStackReportsNoDeviation() {
        val m = measurer(0.6)

        val measured = document.withFontFingerprints(m).measure(m)

        assertEquals(emptyList(), measured.fingerprintDeviations)
    }

    /**
     * Use case: measuring a stamped document with a text stack that resolves the fonts differently
     * reports every affected raw font as a deviation.
     */
    @Test
    fun measuringStampedDocumentWithChangedStackReportsDeviations() {
        val stamped = document.withFontFingerprints(measurer(0.6))

        val measured = stamped.measure(measurer(0.9))

        val deviated = measured.fingerprintDeviations.map { it.family }.toSet()
        assertEquals(setOf("Serif", "Mono"), deviated)
    }

    /**
     * Use case: identical fonts are measured only once during stamping - both blocks over the same
     * font end up with the exact same fingerprint instance value.
     */
    @Test
    fun stampingReusesTheFingerprintForIdenticalFonts() {
        val oneFontDoc = Document(
            listOf(
                FlowPage(
                    EngineTestData.pageLayout(),
                    listOf(
                        TextBlock.of("A", TextStyle(font = serif)),
                        TextBlock.of("B", TextStyle(font = serif)),
                    ),
                ),
            ),
        )

        val stamped = oneFontDoc.withFontFingerprints(measurer(0.6))

        val prints = stamped.pages.flatMap { it.blocks }.map { it.style.font.fingerprint }
        assertNotNull(prints[0])
        assertEquals(prints[0], prints[1])
    }

    /**
     * Use case: with `overwrite = false` a font that already carries a fingerprint keeps it, so a
     * partially stamped document can be completed without touching existing entries.
     */
    @Test
    fun stampingKeepsExistingFingerprintWhenOverwriteIsFalse() {
        val preset = FontFingerprint(normalizedSize = 100.0, ascent = 1.0, descent = 2.0, advances = listOf(3.0))
        val partial = Document(
            listOf(
                FlowPage(
                    EngineTestData.pageLayout(),
                    listOf(
                        TextBlock.of("kept", TextStyle(font = serif.copy(fingerprint = preset))),
                        TextBlock.of("filled", TextStyle(font = mono)),
                    ),
                ),
            ),
        )

        val stamped = partial.withFontFingerprints(measurer(0.6), overwrite = false)

        val fonts = stamped.pages.flatMap { it.blocks }.map { it.style.font }
        assertSame(preset, fonts[0].fingerprint)
        assertNotNull(fonts[1].fingerprint)
        assertTrue(fonts[1].fingerprint != preset)
    }
}
