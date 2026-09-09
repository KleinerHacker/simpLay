# swing - Floating overlays

A `FloatingOverlay` is a caller-supplied `JComponent` that a `PaperSheetView`
shows, positions and hides on its own while a trigger condition holds - a copy
bar above the text selection, a label above the hovered paragraph, and so on. It
is the Swing counterpart of the `fx` module's `FloatingOverlay`, with the FXML
support dropped and `content` typed as a `JComponent`.

## Registering an overlay

```kotlin
val copyBar = JButton("Copy").apply {
    isFocusable = false
    addActionListener { copySelection() }
}

view.floatingOverlays += FloatingOverlay().apply {
    content = copyBar
    trigger = FloatingOverlayTrigger.SELECTION
    anchor = OverlayAnchor.TOP_LEFT
    offsetY = -4.0
}
```

`floatingOverlays` is a plain `MutableList`; a newly added overlay is picked up on
the next repaint, and removing an overlay from the list hides it.

## Configuration

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `content` | `JComponent?` | `null` | The component to place while active; `null` shows nothing. |
| `trigger` | `FloatingOverlayTrigger` | `SELECTION` | What activates the overlay (see below). |
| `anchor` | `OverlayAnchor` | `TOP_LEFT` | Where the component sits relative to the trigger box. |
| `offsetX` / `offsetY` | `Double` | `0.0` | Extra shift in pixels applied after the anchor. |
| `autoHide` | `Boolean` | `true` | Remove the component as soon as the trigger stops holding. |

`OverlayAnchor` combines a horizontal part (`LEFT` / `CENTER` / `RIGHT`, aligning
the component's left edge, centre or right edge to the box) and a vertical part
(`TOP` / `CENTER` / `BOTTOM`, placing the component fully above, centred on, or
fully below the box). The result is clamped to the viewport edge.

## Triggers

| Trigger | Active while | Anchor box |
|---------|--------------|------------|
| `SELECTION` | there is a non-empty text selection | the selection bounding box |
| `PARAGRAPH_HOVER` | the mouse hovers a paragraph (a measured block) | that block's box |
| `PAGE_HOVER` | the mouse hovers a sheet | that sheet's box |
| `CARET` | an edit caret is placed (`EDITABLE` mode only) | the caret rectangle |

## Reading the context

While the overlay is active, these read-only fields carry the current context:

* `isActive` - whether the component is currently placed.
* `activeBounds` - the trigger box in viewport pixels (a `Rectangle`).
* `activeIndex` - selection start, paragraph ordinal, page index or caret index;
  `-1` when not applicable.
* `activeText` - the selected text or the hovered paragraph text; `""` otherwise.
* `activeDocumentRange` - the covered character range for `SELECTION` / `CARET`,
  else `null`.

A `PropertyChangeEvent` under `FloatingOverlay.PROP_ACTIVE` fires whenever
`isActive` flips.

## Events

`onShown` and `onHidden` take a `FloatingOverlayListener` and fire on the
show / hide transition with a `FloatingOverlayEvent` (a `java.util.EventObject`)
carrying the same context plus a `type` of `SHOWN` or `HIDDEN`:

```kotlin
overlay.onShown = FloatingOverlayListener { event ->
    println("shown for '${event.text}' at ${event.triggerBounds}")
}
```
