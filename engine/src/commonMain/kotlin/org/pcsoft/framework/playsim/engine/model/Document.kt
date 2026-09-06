package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * Internal structural contract for [Document].
 *
 * It exists only so the measured decorator model (`...engine.measure`) can wrap a raw document
 * with Kotlin's `by` delegation. It is not part of the public API and carries no serialization.
 */
@PublishedApi
internal interface IDocument {
    val pages: List<Page>
}

/**
 * A whole document made of pages. A plain data holder; an empty document is allowed.
 */
@Serializable
data class Document(override val pages: List<Page> = emptyList()) : IDocument
