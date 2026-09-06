package org.pcsoft.framework.simplay.engine.geometry

import kotlinx.serialization.Serializable

/**
 * A two-dimensional extent. All values are unit-less doubles.
 */
@Serializable
data class Size(val width: Double, val height: Double)

/**
 * An axis-aligned rectangle described by its top-left corner and its extent.
 * All values are unit-less doubles.
 */
@Serializable
data class Rect(val x: Double, val y: Double, val width: Double, val height: Double)

/**
 * The four inner offsets of a box. All values are unit-less doubles.
 */
@Serializable
data class Margins(val left: Double, val top: Double, val right: Double, val bottom: Double)
