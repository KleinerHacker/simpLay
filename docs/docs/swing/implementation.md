# swing - Implementation

The `swing` module binds a measured `Document` from the `engine` core to a Java
Swing user interface. It mirrors the `fx` module one to one, within what Swing
allows: the styling is not as open as JavaFX CSS, but everything a Look-and-Feel
can express is exposed.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-swing:<version>")
}
```

Swing is part of the JDK, so no toolkit dependency is added. The
`ui/common` module (the toolkit-agnostic text index, editor and clipboard
helpers) is pulled in transitively.

## Entry points

### DocumentImageRenderer

`DocumentImageRenderer` in `org.pcsoft.framework.simplay.swing` paints one fixed
`Document` - whole, or a single page - onto an AWT `BufferedImage` (or a
caller-supplied `Graphics2D`). It is the Swing counterpart of the `fx` module's
`CanvasDocumentRenderer`.

```kotlin
val renderer = DocumentImageRenderer.of(document) {
    unitScale = 1.5
    pageGap = 16.0
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}
val image = renderer.renderDocument()
val firstPageSize = renderer.pageImageSizes[0]
```

It measures and lays out the document once, up front; `documentImageSize`,
`pageImageSizes[pageIndex]` and `pageCount` report the geometry, and the
configuration lambda runs over `ImageRenderConfiguration` (`unitScale`,
`pageGap`, and the shared `lineBreakerStrategy` / `wordBreakerStrategy`). See
[Image rendering](image-rendering.md) for the full reference.

### PaperSheetView

`PaperSheetView` is a scrollable, zoomable `JComponent` that renders a
`Document` as physical-looking sheets - each with a border and a drop shadow -
stacked vertically, drawing only the pages in view.

```kotlin
val view = PaperSheetView().apply {
    document = myDocument
    outerMargin = 24.0
    pageGap = 16.0
    minZoom = 0.5
    maxZoom = 3.0
    zoom = 1.0
}
```

`zoom` is always kept within `[minZoom, maxZoom]`. Text is selected with the
mouse (drag to extend, double-click for a word) and copied with `Ctrl+C`
(`Cmd+C` on macOS) as plain text plus styled HTML and RTF that carry the font.
The read-only `contentSize` reports the laid-out size; the selection is exposed
through `selectionModel` (a `TextSelectionModel` with `text`, `startIndex` /
`endIndex` / `length`, `bounds`, the styled `runs`, and the `selectRange` /
`selectAll` / `clearSelection` commands), with `selectedText` and
`selectionBounds` kept as convenience delegates.

Setting `mode = PaperSheetMode.EDITABLE` turns the view into an editor: a
blinking caret, character insertion and removal, clipboard cut / copy / paste,
line and selection duplication, drag-and-drop of the selection and the standard
caret keys (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` /
`Ctrl+Right`, `Backspace`, `Delete`, each optionally with `Shift`). An edit
replaces `document` with a new instance. The caret is exposed through
`caretModel`, a `CaretModel` with the read-only `position` / `bounds` / blink
state, the `blockCount` / `wordCount` / `symbolCount` of the document and the
linear, absolute-structural and relative-structural move commands. Setting
`smoothCaretBlink = true` (off by default) fades the caret instead of blinking
it hard on and off.

`PaperSheetView` also accepts floating overlays through `floatingOverlays`: a
`FloatingOverlay` component the view shows, positions and hides on its own while
its `trigger` holds (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, and `CARET`
while editing). Overlays follow scroll and zoom and are clamped to the viewport
edge. See [Floating overlays](floating-overlays.md).

Every mutable property fires a `java.beans.PropertyChangeEvent` under a `PROP_*`
name, so a listener can react with `addPropertyChangeListener`.

## Differences from the `fx` module

Swing has no equivalent of JavaFX CSS or FXML, so:

* Styling is done through the Look-and-Feel: the `PaperSheetView.*` keys of the
  active Look-and-Feel plus a programmatic setter, not a stylesheet. See
  [Styling](styling.md).
* `FloatingOverlay` is a programmatic API only; there is no FXML equivalent. Its
  `content` is a `JComponent` and `FloatingOverlayEvent` extends
  `java.util.EventObject`.
* Observable state is exposed through `java.beans.PropertyChangeSupport`, not
  JavaFX properties and bindings.
* `DocumentImageRenderer` returns a `BufferedImage` instead of a live `Canvas`
  node.

Because the AWT and JavaFX text stacks measure slightly differently, the visual
output is very close to the `fx` module but not pixel-identical.

## Usage guides

* [Image rendering](image-rendering.md) - drawing a `Document` directly onto a
  `BufferedImage` or `Graphics2D`, whole or per page.
* [Paper sheet component](paper-sheet-component.md) - all `PaperSheetView`
  properties, the read-only and editable modes, selection and clipboard, and the
  shortcut table.
* [Floating overlays](floating-overlays.md) - the overlay API, its triggers,
  fields and events.
* [Styling the paper sheet component](styling.md) - the `PaperSheetView.*`
  Look-and-Feel keys, the default values and an override example.
