@file:JvmName("MeasuredCountingUtil")

package org.pcsoft.framework.playsim.engine.measure

import kotlin.jvm.JvmName
import org.pcsoft.framework.playsim.engine.model.charCount
import org.pcsoft.framework.playsim.engine.model.symbolCount
import org.pcsoft.framework.playsim.engine.model.wordCount

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
