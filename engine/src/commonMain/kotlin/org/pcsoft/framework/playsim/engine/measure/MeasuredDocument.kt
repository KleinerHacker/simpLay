package org.pcsoft.framework.playsim.engine.measure

import org.pcsoft.framework.playsim.engine.model.Document

/**
 * A raw [Document] with its pages replaced by measured pages.
 *
 * `Document` has no pass-through properties to delegate (`pages` is replaced), so this is a plain
 * wrapper: the raw pages stay reachable via `raw.pages`. An empty document with no pages is
 * allowed. Not persistable.
 *
 * @property raw the wrapped raw document.
 * @property pages the measured pages of this document, in order.
 */
class MeasuredDocument(
    val raw: Document,
    val pages: List<MeasuredPage>,
)
