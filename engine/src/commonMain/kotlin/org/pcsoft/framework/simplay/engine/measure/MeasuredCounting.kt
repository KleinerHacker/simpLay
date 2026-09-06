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

@file:JvmName("MeasuredCountingUtil")

package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.model.charCount
import org.pcsoft.framework.simplay.engine.model.symbolCount
import org.pcsoft.framework.simplay.engine.model.wordCount
import kotlin.jvm.JvmName

/** Number of word parts in this block, taken from its raw block. */
fun MeasuredTextBlock.wordCount(): Int = raw.wordCount()

/** Number of symbol parts in this block, taken from its raw block. */
fun MeasuredTextBlock.symbolCount(): Int = raw.symbolCount()

/** Total number of characters across all parts of this block, taken from its raw block. */
fun MeasuredTextBlock.charCount(): Int = raw.charCount()

/** Sum of [MeasuredTextBlock.wordCount] over all measured blocks of this page. */
fun MeasuredPage.wordCount(): Int = blocks.sumOf { it.wordCount() }

/** Sum of [MeasuredTextBlock.symbolCount] over all measured blocks of this page. */
fun MeasuredPage.symbolCount(): Int = blocks.sumOf { it.symbolCount() }

/** Sum of [MeasuredTextBlock.charCount] over all measured blocks of this page. */
fun MeasuredPage.charCount(): Int = blocks.sumOf { it.charCount() }

/** Sum of [MeasuredPage.wordCount] over all measured pages of this document. */
fun MeasuredDocument.wordCount(): Int = pages.sumOf { it.wordCount() }

/** Sum of [MeasuredPage.symbolCount] over all measured pages of this document. */
fun MeasuredDocument.symbolCount(): Int = pages.sumOf { it.symbolCount() }

/** Sum of [MeasuredPage.charCount] over all measured pages of this document. */
fun MeasuredDocument.charCount(): Int = pages.sumOf { it.charCount() }
