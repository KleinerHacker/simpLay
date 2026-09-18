# fx - Page decorations

A page decoration is a node you supply that
[`PaperSheetView`](paper-sheet-component.md) anchors to one edge of a single
page, identified by that page's stable `Page.id` - a page number below the
sheet, a watermark to the side, a stamp above it. Unlike a
[floating overlay](floating-overlays.md), a decoration is not tied to an
interaction (hover, selection, caret); it stays visible for as long as its
page is laid out and follows scroll and zoom.

## Registering a decoration

`PageDecoration` (package `org.pcsoft.framework.simplay.fx`) is a bean with a
no-argument constructor. Add instances to `PaperSheetView.getPageDecorations()`:

```kotlin
import javafx.scene.control.Label
import org.pcsoft.framework.simplay.fx.PageDecoration
import org.pcsoft.framework.simplay.uicommon.EdgeAlignment
import org.pcsoft.framework.simplay.uicommon.PageEdge

val pageLabel = PageDecoration().apply {
    pageId = "page-1"
    edge = PageEdge.BOTTOM
    alignment = EdgeAlignment.CENTER
    offsetY = 4.0
    content = Label("1")
}
view.pageDecorations += pageLabel
```

The same list is populated from FXML as a `<pageDecorations>` child element:

```xml
<?import javafx.scene.control.Label?>
<?import org.pcsoft.framework.simplay.fx.PageDecoration?>
<?import org.pcsoft.framework.simplay.fx.PaperSheetView?>

<PaperSheetView xmlns:fx="http://javafx.com/fxml/1" fx:id="view">
    <pageDecorations>
        <PageDecoration pageId="page-1" edge="TOP" alignment="CENTER" offsetY="4.0">
            <content>
                <Label text="Draft"/>
            </content>
        </PageDecoration>
    </pageDecorations>
</PaperSheetView>
```

## Placement

| Property | Default | Meaning |
|----------|---------|---------|
| `content` | `null` | The node to place while `pageId` resolves to a laid-out page; `null` shows nothing. |
| `pageId` | `""` | The stable `Page.id` this decoration is anchored to. |
| `edge` | `PageEdge.TOP` | Which page edge (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`) the decoration sits against. |
| `alignment` | `EdgeAlignment.STRETCH` | Where the decoration sits along `edge`: `STRETCH` fills the full edge length, `START`/`CENTER`/`END` keep the decoration's own size and position it accordingly. |
| `offsetX` / `offsetY` | `0.0` | Extra pixel shift applied after `edge` and `alignment`; positive `offsetX` moves along the edge, positive `offsetY` moves further away from the page. |

## Behavior

* A decoration is shown for as long as its `pageId` resolves to a page that is
  currently laid out, independent of `mode` or an individual page's
  `PageMode`.
* Its position and size follow scroll and zoom exactly like the page it is
  anchored to.
* Several decorations may target the same page and the same edge; each is
  positioned independently, so overlapping content is the caller's
  responsibility.

## Next

* [Floating overlays](floating-overlays.md) - the interaction-triggered counterpart.
* [Paper sheet component](paper-sheet-component.md) - the host control.
