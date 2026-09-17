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

package org.pcsoft.framework.simplay.engine.geometry

import org.pcsoft.framework.simplay.engine.model.PageLayout

/**
 * A standard page format, described by its width and height in millimeters.
 * [size] converts those millimeters into the engine's point-based layout unit (see [MM_TO_POINT]).
 */
enum class PaperFormat(val widthMm: Double, val heightMm: Double) {
    DIN_A0(841.0, 1189.0),
    DIN_A1(594.0, 841.0),
    DIN_A2(420.0, 594.0),
    DIN_A3(297.0, 420.0),
    DIN_A4(210.0, 297.0),
    DIN_A5(148.0, 210.0),
    DIN_A6(105.0, 148.0),
    DIN_A7(74.0, 105.0),
    DIN_A8(52.0, 74.0),
    DIN_A9(37.0, 52.0),
    DIN_A10(26.0, 37.0),

    DIN_B0(1000.0, 1414.0),
    DIN_B1(707.0, 1000.0),
    DIN_B2(500.0, 707.0),
    DIN_B3(353.0, 500.0),
    DIN_B4(250.0, 353.0),
    DIN_B5(176.0, 250.0),
    DIN_B6(125.0, 176.0),
    DIN_B7(88.0, 125.0),
    DIN_B8(62.0, 88.0),
    DIN_B9(44.0, 62.0),
    DIN_B10(31.0, 44.0),

    DIN_C0(917.0, 1297.0),
    DIN_C1(648.0, 917.0),
    DIN_C2(458.0, 648.0),
    DIN_C3(324.0, 458.0),
    DIN_C4(229.0, 324.0),
    DIN_C5(162.0, 229.0),
    DIN_C6(114.0, 162.0),
    DIN_C7(81.0, 114.0),
    DIN_C8(57.0, 81.0),
    DIN_C9(40.0, 57.0),
    DIN_C10(28.0, 40.0),

    LETTER(215.9, 279.4),
    LEGAL(215.9, 355.6),
    TABLOID(279.4, 431.8),

    PHOTO_9X13(90.0, 130.0),
    PHOTO_10X15(100.0, 150.0),
    PHOTO_13X18(130.0, 180.0),
    PHOTO_15X20(150.0, 200.0),
    PHOTO_20X25(200.0, 250.0),
    PHOTO_20X30(200.0, 300.0),
    PHOTO_30X40(300.0, 400.0),
    ;

    /** This format's extent in the engine's point-based layout unit. */
    val size: Size
        get() = Size(width = widthMm * MM_TO_POINT, height = heightMm * MM_TO_POINT)

    /**
     * Combines this format's [size] with the given [margins] into a [PageLayout].
     */
    infix fun withMargin(margins: Margins): PageLayout = PageLayout(size = size, margins = margins)

    companion object {
        /** Conversion factor from millimeters to the engine's point-based layout unit (72 pt = 1 in = 25.4 mm). */
        const val MM_TO_POINT: Double = 72.0 / 25.4
    }
}
