@file:JvmName("CountingUtil")

package org.pcsoft.framework.playsim.engine.model

import kotlin.jvm.JvmName

/** Number of [TextWord] parts in this block. */
fun TextBlock.wordCount(): Int = parts.count { it is TextWord }

/** Number of [TextSymbol] parts in this block. */
fun TextBlock.symbolCount(): Int = parts.count { it is TextSymbol }

/** Total number of characters across all parts of this block. */
fun TextBlock.charCount(): Int = parts.sumOf { it.text.length }

/** Sum of [TextBlock.wordCount] over all blocks of this page. */
fun Page.wordCount(): Int = blocks.sumOf { it.wordCount() }

/** Sum of [TextBlock.symbolCount] over all blocks of this page. */
fun Page.symbolCount(): Int = blocks.sumOf { it.symbolCount() }

/** Sum of [TextBlock.charCount] over all blocks of this page. */
fun Page.charCount(): Int = blocks.sumOf { it.charCount() }

/** Sum of [Page.wordCount] over all pages of this document. */
fun Document.wordCount(): Int = pages.sumOf { it.wordCount() }

/** Sum of [Page.symbolCount] over all pages of this document. */
fun Document.symbolCount(): Int = pages.sumOf { it.symbolCount() }

/** Sum of [Page.charCount] over all pages of this document. */
fun Document.charCount(): Int = pages.sumOf { it.charCount() }
