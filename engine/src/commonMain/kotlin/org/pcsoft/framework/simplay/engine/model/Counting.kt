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

@file:JvmName("CountingUtil")

package org.pcsoft.framework.simplay.engine.model

import kotlin.jvm.JvmName

/** Number of [TextWord] parts in this block. */
fun TextBlock.wordCount(): Int = parts.count { it is TextWord }

/** Number of [TextSymbol] parts in this block. */
fun TextBlock.symbolCount(): Int = parts.count { it is TextSymbol }

/** Total number of characters across all non-whitespace parts of this block; [TextWhitespace] runs
 * are not counted (as before whitespace was preserved as its own part). */
fun TextBlock.charCount(): Int = parts.filterNot { it is TextWhitespace }.sumOf { it.text.length }

/** Sum of [wordCount] over all blocks of this page. */
fun Page.wordCount(): Int = blocks.sumOf { it.wordCount() }

/** Sum of [symbolCount] over all blocks of this page. */
fun Page.symbolCount(): Int = blocks.sumOf { it.symbolCount() }

/** Sum of [charCount] over all blocks of this page. */
fun Page.charCount(): Int = blocks.sumOf { it.charCount() }

/** Sum of [wordCount] over all pages of this document. */
fun Document.wordCount(): Int = pages.sumOf { it.wordCount() }

/** Sum of [symbolCount] over all pages of this document. */
fun Document.symbolCount(): Int = pages.sumOf { it.symbolCount() }

/** Sum of [charCount] over all pages of this document. */
fun Document.charCount(): Int = pages.sumOf { it.charCount() }
