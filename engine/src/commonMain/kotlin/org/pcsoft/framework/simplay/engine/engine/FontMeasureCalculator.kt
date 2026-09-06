package org.pcsoft.framework.simplay.engine.engine

import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * Callback that measures a piece of text in a given [Font].
 *
 * The engine never talks to a platform text stack itself; the caller supplies this callback so the
 * same document can be measured against AWT, Skia, a headless stub or any other backend. A given
 * ([Font], text) pair must always yield the same [TextMetrics] so the result stays deterministic.
 */
fun interface FontMeasureCalculator {

    /**
     * Returns the [TextMetrics] of [text] rendered in [font].
     *
     * @param font the font the text would be rendered in.
     * @param text the exact string to measure; may be a single glyph, a word or a whole reference
     *   string.
     * @return the advance width plus the ascent and descent of [text] in [font], as unit-less
     *   doubles.
     */
    fun measure(font: Font, text: String): TextMetrics
}
