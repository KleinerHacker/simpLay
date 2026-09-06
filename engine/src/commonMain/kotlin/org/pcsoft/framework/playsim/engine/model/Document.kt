package org.pcsoft.framework.playsim.engine.model

import kotlinx.serialization.Serializable

/**
 * A whole document made of pages. A plain data holder; an empty document is allowed.
 */
@Serializable
data class Document(val pages: List<Page> = emptyList())
