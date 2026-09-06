@file:JvmName("MeasuredCountingUtil")

package org.pcsoft.framework.simplay.engine.measure

import org.pcsoft.framework.simplay.engine.measure.charCount
import org.pcsoft.framework.simplay.engine.measure.symbolCount
import org.pcsoft.framework.simplay.engine.measure.wordCount
import kotlin.jvm.JvmName
import org.pcsoft.framework.simplay.engine.model.charCount
import org.pcsoft.framework.simplay.engine.model.symbolCount
import org.pcsoft.framework.simplay.engine.model.wordCount

/** Number of word parts in this block, taken from its raw block. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.wordCount(): Int = raw.wordCount()

/** Number of symbol parts in this block, taken from its raw block. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.symbolCount(): Int = raw.symbolCount()

/** Total number of characters across all parts of this block, taken from its raw block. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.charCount(): Int = raw.charCount()

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.MeasuredTextBlock.wordCount] over all measured blocks of this page. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredPage.wordCount(): Int = blocks.sumOf { it.wordCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.MeasuredTextBlock.symbolCount] over all measured blocks of this page. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredPage.symbolCount(): Int = blocks.sumOf { it.symbolCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredTextBlock.MeasuredTextBlock.charCount] over all measured blocks of this page. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredPage.charCount(): Int = blocks.sumOf { it.charCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.MeasuredPage.wordCount] over all measured pages of this document. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredDocument.wordCount(): Int = pages.sumOf { it.wordCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.MeasuredPage.symbolCount] over all measured pages of this document. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredDocument.symbolCount(): Int = pages.sumOf { it.symbolCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.measure.MeasuredPage.MeasuredPage.charCount] over all measured pages of this document. */
fun org.pcsoft.framework.simplay.engine.measure.MeasuredDocument.charCount(): Int = pages.sumOf { it.charCount() }
