# swing - Page decorations

A `PageDecoration` is a caller-supplied `JComponent` that a `PaperSheetView`
anchors to one edge of a single page, identified by that page's stable
`Page.id` - a page number below the sheet, a watermark to the side, a stamp
above it. It is the Swing counterpart of the `fx` module's `PageDecoration`,
with the FXML support dropped and `content` typed as a `JComponent`. Unlike a
[floating overlay](floating-overlays.md), a decoration is not tied to an
interaction (hover, selection, caret); it stays visible for as long as its
page is laid out and follows scroll and zoom.

## Registering a decoration

```kotlin
val pageLabel = JLabel("1")

view.pageDecorations += PageDecoration().apply {
    content = pageLabel
    pageId = "page-1"
    edge = PageEdge.BOTTOM
    alignment = EdgeAlignment.CENTER
    offsetY = 4.0
}
```

`pageDecorations` is a plain `MutableList`; a newly added decoration is picked
up on the next repaint, and removing a decoration from the list hides it.

## Configuration

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `content` | `JComponent?` | `null` | The component to place while `pageId` resolves to a laid-out page; `null` shows nothing. |
| `pageId` | `String` | `""` | The stable `Page.id` this decoration is anchored to. |
| `edge` | `PageEdge` | `TOP` | Which page edge (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`) the decoration sits against. |
| `alignment` | `EdgeAlignment` | `STRETCH` | Where the decoration sits along `edge`: `STRETCH` fills the full edge length, `START`/`CENTER`/`END` keep the component's own size and position it accordingly. |
| `offsetX` / `offsetY` | `Double` | `0.0` | Extra shift in pixels applied after `edge` and `alignment`; positive `offsetX` moves along the edge, positive `offsetY` moves further away from the page. |

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
