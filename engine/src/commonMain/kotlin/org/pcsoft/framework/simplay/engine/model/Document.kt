package org.pcsoft.framework.simplay.engine.model

import kotlinx.serialization.Serializable

/**
 * A whole document made of pages. A plain data holder; an empty document is allowed.
 */
@Serializable
data class Document(val pages: List<org.pcsoft.framework.simplay.engine.model.Page> = emptyList())
