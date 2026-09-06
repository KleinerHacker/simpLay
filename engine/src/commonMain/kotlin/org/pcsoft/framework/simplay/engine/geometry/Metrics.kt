package org.pcsoft.framework.simplay.engine.geometry

import kotlinx.serialization.Serializable

/**
 * Vertical metrics of a font. All values are unit-less doubles.
 */
@Serializable
data class FontMetrics(val ascent: Double, val descent: Double, val leading: Double) {
    /** The distance from one baseline to the next. */
    val lineHeight: Double
        get() = ascent + descent + leading
}

/**
 * Metrics of a measured piece of text. All values are unit-less doubles.
 */
@Serializable
data class TextMetrics(val width: Double, val ascent: Double, val descent: Double)
