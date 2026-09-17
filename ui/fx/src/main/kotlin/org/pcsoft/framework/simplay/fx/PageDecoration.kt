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

package org.pcsoft.framework.simplay.fx

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.Node
import org.pcsoft.framework.simplay.uicommon.EdgeAlignment
import org.pcsoft.framework.simplay.uicommon.PageDecorationPlacement
import org.pcsoft.framework.simplay.uicommon.PageEdge

/**
 * A caller-supplied node permanently anchored to one edge of a single page of a [PaperSheetView],
 * identified by the page's stable [pageId]. Unlike [FloatingOverlay], a page decoration is not tied
 * to an interaction (hover, selection, caret); it stays visible for as long as its page is laid out
 * and follows scroll and zoom. Register instances through [PaperSheetView.getPageDecorations].
 *
 * The bean has a no-argument constructor and only JavaFX properties, so it can be declared in FXML:
 *
 * ```xml
 * <PaperSheetView>
 *   <pageDecorations>
 *     <PageDecoration pageId="page-1" edge="TOP" alignment="CENTER" offsetY="4.0">
 *       <content>
 *         <Label text="Draft"/>
 *       </content>
 *     </PageDecoration>
 *   </pageDecorations>
 * </PaperSheetView>
 * ```
 *
 * Every property follows the JavaFX bean convention: the property object through `xxxProperty()`, the
 * value through `getXxx()` / `setXxx()`.
 */
class PageDecoration {

    //region Content

    /** The [content] property, for binding and change listeners. */
    @get:JvmName("contentProperty")
    val contentProperty: ObjectProperty<Node?> = SimpleObjectProperty(this, "content", null)

    /** The node to place into the view while [pageId] resolves to a laid-out page; `null` shows nothing. */
    var content: Node?
        get() = contentProperty.get()
        set(value) = contentProperty.set(value)

    //endregion

    //region Page and placement

    /** The [pageId] property, for binding and change listeners. */
    @get:JvmName("pageIdProperty")
    val pageIdProperty: StringProperty = SimpleStringProperty(this, "pageId", "")

    /** The stable [org.pcsoft.framework.simplay.engine.model.Page.id] this decoration is anchored to. */
    var pageId: String
        get() = pageIdProperty.get()
        set(value) = pageIdProperty.set(value)

    /** The [edge] property, for binding and change listeners. */
    @get:JvmName("edgeProperty")
    val edgeProperty: ObjectProperty<PageEdge> = SimpleObjectProperty(this, "edge", PageEdge.TOP)

    /** The page edge this decoration is anchored to. Defaults to [PageEdge.TOP]. */
    var edge: PageEdge
        get() = edgeProperty.get()
        set(value) = edgeProperty.set(value)

    /** The [alignment] property, for binding and change listeners. */
    @get:JvmName("alignmentProperty")
    val alignmentProperty: ObjectProperty<EdgeAlignment> = SimpleObjectProperty(this, "alignment", EdgeAlignment.STRETCH)

    /** How the decoration is positioned along [edge]. Defaults to [EdgeAlignment.STRETCH]. */
    var alignment: EdgeAlignment
        get() = alignmentProperty.get()
        set(value) = alignmentProperty.set(value)

    /** The [offsetX] property, for binding and change listeners. */
    @get:JvmName("offsetXProperty")
    val offsetXProperty: DoubleProperty = SimpleDoubleProperty(this, "offsetX", 0.0)

    /** Extra shift in layout points along the horizontal axis; see [PageDecorationPlacement.offsetX]. */
    var offsetX: Double
        get() = offsetXProperty.get()
        set(value) = offsetXProperty.set(value)

    /** The [offsetY] property, for binding and change listeners. */
    @get:JvmName("offsetYProperty")
    val offsetYProperty: DoubleProperty = SimpleDoubleProperty(this, "offsetY", 0.0)

    /** Extra shift in layout points along the vertical axis; see [PageDecorationPlacement.offsetY]. */
    var offsetY: Double
        get() = offsetYProperty.get()
        set(value) = offsetYProperty.set(value)

    /** The toolkit-agnostic placement built from [edge], [alignment], [offsetX] and [offsetY]. */
    internal val placement: PageDecorationPlacement get() = PageDecorationPlacement(edge, alignment, offsetX, offsetY)

    //endregion
}
