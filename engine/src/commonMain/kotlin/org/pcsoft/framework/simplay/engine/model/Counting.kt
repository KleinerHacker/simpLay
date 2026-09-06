@file:JvmName("CountingUtil")

package org.pcsoft.framework.simplay.engine.model

import org.pcsoft.framework.simplay.engine.model.charCount
import org.pcsoft.framework.simplay.engine.model.symbolCount
import org.pcsoft.framework.simplay.engine.model.wordCount
import kotlin.jvm.JvmName

/** Number of [org.pcsoft.framework.simplay.engine.model.TextWord] parts in this block. */
fun org.pcsoft.framework.simplay.engine.model.TextBlock.wordCount(): Int = parts.count { it is org.pcsoft.framework.simplay.engine.model.TextWord }

/** Number of [org.pcsoft.framework.simplay.engine.model.TextSymbol] parts in this block. */
fun org.pcsoft.framework.simplay.engine.model.TextBlock.symbolCount(): Int = parts.count { it is org.pcsoft.framework.simplay.engine.model.TextSymbol }

/** Total number of characters across all parts of this block. */
fun org.pcsoft.framework.simplay.engine.model.TextBlock.charCount(): Int = parts.sumOf { it.text.length }

/** Sum of [org.pcsoft.framework.simplay.engine.model.wordCount] over all blocks of this page. */
fun org.pcsoft.framework.simplay.engine.model.Page.wordCount(): Int = blocks.sumOf { it.wordCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.model.symbolCount] over all blocks of this page. */
fun org.pcsoft.framework.simplay.engine.model.Page.symbolCount(): Int = blocks.sumOf { it.symbolCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.model.charCount] over all blocks of this page. */
fun org.pcsoft.framework.simplay.engine.model.Page.charCount(): Int = blocks.sumOf { it.charCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.model.wordCount] over all pages of this document. */
fun org.pcsoft.framework.simplay.engine.model.Document.wordCount(): Int = pages.sumOf { it.wordCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.model.symbolCount] over all pages of this document. */
fun org.pcsoft.framework.simplay.engine.model.Document.symbolCount(): Int = pages.sumOf { it.symbolCount() }

/** Sum of [org.pcsoft.framework.simplay.engine.model.charCount] over all pages of this document. */
fun org.pcsoft.framework.simplay.engine.model.Document.charCount(): Int = pages.sumOf { it.charCount() }
