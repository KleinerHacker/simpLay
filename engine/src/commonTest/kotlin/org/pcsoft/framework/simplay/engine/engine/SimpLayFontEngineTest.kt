package org.pcsoft.framework.simplay.engine.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.pcsoft.framework.simplay.engine.engine.internal.SimpLayFontEngine

/**
 * Verifies that [SimpLayFontEngine] derives vertical font metrics from the reference glyphs, sets
 * the leading to zero and caches resolved fonts per instance.
 */
class SimpLayFontEngineTest {

    private fun fontEngine() = SimpLayFontEngine.builder(EngineTestData.measurer).build()

    /**
     * Use case: resolving a font measures the reference glyph string once and takes the ascent and
     * descent from it, with a leading of zero.
     */
    @Test
    fun resolvesFontMetricsFromReferenceGlyphs() {
        val measured = fontEngine().resolveFont(EngineTestData.font)

        assertEquals(EngineTestData.ASCENT, measured.metrics.ascent)
        assertEquals(EngineTestData.DESCENT, measured.metrics.descent)
        assertEquals(0.0, measured.metrics.leading)
    }

    /**
     * Use case: the reference glyph string covers the Latin alphabet, digits, common diacritics and
     * common punctuation so ascenders, descenders and accents are all represented.
     */
    @Test
    fun referenceGlyphsCoverLettersDigitsDiacriticsAndPunctuation() {
        val glyphs = SimpLayFontEngine.REFERENCE_GLYPHS

        assertEquals(true, glyphs.any { it in 'A'..'Z' })
        assertEquals(true, glyphs.any { it in 'a'..'z' })
        assertEquals(true, glyphs.any { it in '0'..'9' })
        assertEquals(true, glyphs.contains('ä'))
        assertEquals(true, glyphs.contains('ç'))
        assertEquals(true, glyphs.contains('ł'))
        assertEquals(true, glyphs.contains('.'))
    }

    /**
     * Use case: one instance caches a resolved font, while a separate instance resolves its own.
     */
    @Test
    fun cachesResolvedFontPerInstance() {
        val engine = fontEngine()

        val first = engine.resolveFont(EngineTestData.font)
        val second = engine.resolveFont(EngineTestData.font)
        val fromOtherInstance = fontEngine().resolveFont(EngineTestData.font)

        assertSame(first, second)
        assertNotSame(first, fromOtherInstance)
    }

    /**
     * Use case: a resolved style exposes a measured style whose line height is derived from the
     * resolved font metrics and the style line spacing, and whose font is the instance's cached one.
     */
    @Test
    fun resolvesStyleWithDerivedLineHeightAndCachedFont() {
        val engine = fontEngine()

        val style = engine.resolveStyle(EngineTestData.style)

        assertEquals(EngineTestData.LINE_HEIGHT, style.resolvedLineHeight)
        assertSame(engine.resolveFont(EngineTestData.font), style.font)
    }
}
