# fx - Floating overlays

A floating overlay is a node you supply that
[`PaperSheetView`](paper-sheet-component.md) shows, positions and hides on its own
while a trigger condition holds - a copy bar above the text selection, a label
above the hovered paragraph, an action button next to the caret. The view keeps
the overlay glued to its anchor through scroll and zoom and clamps it to the
viewport edge.

## Registering an overlay

`FloatingOverlay` (package `org.pcsoft.framework.simplay.fx`) is a bean with a
no-argument constructor. Add instances to `PaperSheetView.getFloatingOverlays()`:

```kotlin
import javafx.geometry.Pos
import javafx.scene.control.Button
import org.pcsoft.framework.simplay.fx.FloatingOverlay
import org.pcsoft.framework.simplay.fx.FloatingOverlayTrigger

val copyBar = FloatingOverlay().apply {
    trigger = FloatingOverlayTrigger.SELECTION
    anchor = Pos.TOP_LEFT
    offsetY = -6.0
    content = Button("Copy").apply { setOnAction { view.selectionModel /* ... */ } }
}
view.floatingOverlays += copyBar
```

The same list is populated from FXML as a `<floatingOverlays>` child element:

```xml
<?import javafx.scene.control.Button?>
<?import org.pcsoft.framework.simplay.fx.FloatingOverlay?>
<?import org.pcsoft.framework.simplay.fx.PaperSheetView?>

<PaperSheetView xmlns:fx="http://javafx.com/fxml/1" fx:id="view">
    <floatingOverlays>
        <FloatingOverlay fx:id="copyBar" trigger="SELECTION" anchor="TOP_LEFT" offsetY="-6.0">
            <content>
                <Button text="Copy" onAction="#copySelection"/>
            </content>
        </FloatingOverlay>
    </floatingOverlays>
</PaperSheetView>
```

## Triggers

`FloatingOverlayTrigger` is fixed per overlay and never changes at runtime:

| Trigger | Active while... | Anchored to |
|---------|-----------------|-------------|
| `SELECTION` (default) | the view has a non-empty text selection | the selection bounding box |
| `PARAGRAPH_HOVER` | the mouse hovers a paragraph (a measured block) | that block's box |
| `PAGE_HOVER` | the mouse hovers a sheet | that sheet's box |
| `CARET` | an edit caret is placed (`EDITABLE` mode only) | the caret rectangle |

A `CARET` overlay never appears while `mode` is `READONLY`.

## Placement

| Property | Default | Meaning |
|----------|---------|---------|
| `content` | `null` | The node to place while active; `null` shows nothing. |
| `anchor` | `Pos.TOP_LEFT` | Where the overlay sits relative to the trigger box. Horizontal part aligns the node's left edge (`LEFT`), centre (`CENTER`) or right edge (`RIGHT`); vertical part places it fully above (`TOP`), centred (`CENTER`) or fully below (`BOTTOM` / `BASELINE`). |
| `offsetX` / `offsetY` | `0.0` | Extra pixel shift applied after `anchor`. |
| `autoHide` | `true` | When `true`, the overlay is removed as soon as its trigger stops holding. |

Regardless of `autoHide`, an overlay is hidden once its trigger box scrolls fully
out of the viewport; while the box is only partly out of view the overlay is
clamped to the viewport edge.

## Reading the active context

While the trigger holds, the view writes the current context into read-only
fields that can be bound from FXML with `${copyBar.activeText}` and friends:

| Field | Meaning |
|-------|---------|
| `isActive` | `true` while the overlay's node is placed in the view. |
| `activeBounds` | The trigger box in viewport pixels, or `null`. |
| `activeIndex` | Trigger-specific index: selection start, paragraph ordinal, page index or caret index; `-1` when not applicable. |
| `activeText` | The selected text or the hovered paragraph text; `""` otherwise. |
| `activeDocumentRange` | The covered character range (`IntRange`) for `SELECTION`, else `null`. |

## Events

`onShown` and `onHidden` (`EventHandler<FloatingOverlayEvent>`) fire on the
show / hide transitions only - repositioning on scroll, zoom or hover does not
re-fire them. `FloatingOverlayEvent` carries the same context: `overlay`,
`triggerKind`, `triggerBounds`, `index`, `text`, `documentRange`.

```kotlin
copyBar.onShown = EventHandler { e -> println("copy bar over: ${e.text}") }
copyBar.onHidden = EventHandler { println("copy bar gone") }
```

## Hover properties on the view

Independently of any overlay, `PaperSheetView` exposes the current hover target
as read-only properties: `hoveredParagraph` / `hoveredParagraphBounds` and
`hoveredPage` / `hoveredPageBounds` (ordinal / index and viewport box, or `-1` /
`null`). They follow scroll and zoom.

## Next

* [Paper sheet component](paper-sheet-component.md) - the host control.
* [Styling the paper sheet component](styling.md) - the JavaFX CSS reference.
