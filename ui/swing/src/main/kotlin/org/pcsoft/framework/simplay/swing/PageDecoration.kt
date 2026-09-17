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

package org.pcsoft.framework.simplay.swing

import javax.swing.JComponent
import org.pcsoft.framework.simplay.uicommon.EdgeAlignment
import org.pcsoft.framework.simplay.uicommon.PageDecorationPlacement
import org.pcsoft.framework.simplay.uicommon.PageEdge

/**
 * A caller-supplied component permanently anchored to one edge of a single page of a
 * [PaperSheetView], identified by the page's stable [pageId]. Unlike [FloatingOverlay], a page
 * decoration is not tied to an interaction (hover, selection, caret); it stays visible for as long
 * as its page is laid out and follows scroll and zoom. Register instances through
 * [PaperSheetView.pageDecorations]. The Swing counterpart of the `fx` module's `PageDecoration`; the
 * programmatic API is kept, the FXML support is dropped and [content] is a [JComponent] instead of a
 * JavaFX `Node`.
 */
class PageDecoration {

    /** The component to place into the view while [pageId] resolves to a laid-out page; `null` shows nothing. */
    var content: JComponent? = null

    /** The stable [org.pcsoft.framework.simplay.engine.model.Page.id] this decoration is anchored to. */
    var pageId: String = ""

    /** The page edge this decoration is anchored to. Defaults to [PageEdge.TOP]. */
    var edge: PageEdge = PageEdge.TOP

    /** How the decoration is positioned along [edge]. Defaults to [EdgeAlignment.STRETCH]. */
    var alignment: EdgeAlignment = EdgeAlignment.STRETCH

    /** Extra shift in layout points along the horizontal axis; see [PageDecorationPlacement.offsetX]. */
    var offsetX: Double = 0.0

    /** Extra shift in layout points along the vertical axis; see [PageDecorationPlacement.offsetY]. */
    var offsetY: Double = 0.0

    /** The toolkit-agnostic placement built from [edge], [alignment], [offsetX] and [offsetY]. */
    internal val placement: PageDecorationPlacement get() = PageDecorationPlacement(edge, alignment, offsetX, offsetY)
}
