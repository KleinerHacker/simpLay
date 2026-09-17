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

/**
 * The DPI a `PaperSheetView` (fx and swing) layout unit is authored in: a PostScript/PDF point, the
 * convention the engine's page geometry already uses (e.g. ISO A4 is `595 x 842`, the standard point
 * size). `72` points make one inch.
 */
const val LAYOUT_DPI: Double = 72.0

/**
 * The DPI Swing's and JavaFX's own device-independent pixel is defined against at `100%` OS display
 * scaling. Both toolkits have scaled this unit to the real screen automatically since Java 9, so a
 * `PaperSheetView` never needs to query the actual screen itself - only the fixed conversion from the
 * engine's point-based layout unit to this toolkit unit.
 */
const val TOOLKIT_DPI: Double = 96.0

/**
 * Combines a `PaperSheetView` (fx and swing) logical [zoom] factor with the fixed conversion from the
 * engine's point-based layout unit ([LAYOUT_DPI]) to the toolkit's own device-independent pixel unit
 * ([TOOLKIT_DPI]), so that a `zoom` of `1.0` renders content at true physical size.
 *
 * @param zoom the logical zoom factor as exposed by the view's `zoom` property (`1.0` = "100%").
 * @return the scale factor to apply when converting layout units to toolkit (screen) pixels.
 */
fun effectiveZoom(zoom: Double): Double = zoom * (TOOLKIT_DPI / LAYOUT_DPI)
