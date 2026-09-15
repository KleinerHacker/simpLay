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

/**
 * One contiguous stretch of a [TextSelectionModel]'s selection that shares a single font. The runs of
 * a selection, concatenated, do not necessarily rebuild the selected text verbatim: the whitespace
 * the linear text inserts between two parts is not part of any run.
 *
 * @property text the selected characters of this stretch.
 * @property fontFamily the font family the characters are drawn with.
 * @property fontSize the font size in points.
 * @property bold whether the font weight is bold.
 * @property italic whether the font is italic.
 */
data class TextSelectionData(
    val text: String,
    val fontFamily: String,
    val fontSize: Double,
    val bold: Boolean,
    val italic: Boolean,
)
