# fx - Implementation

The `fx` module binds a running simulation from the `engine` core to a JavaFX user
interface.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:0.3.0")
    implementation("org.pcsoft.framework:simplay-fx:0.3.0")
}
```

JavaFX is exposed transitively by this module, so no separate JavaFX dependency is
required in the consuming build.

## Entry points

The first public entry point is `CanvasDocumentRenderer` in
`org.pcsoft.framework.simplay.fx`. It is created for one fixed document
and measures it up front:

```kotlin
val renderer = CanvasDocumentRenderer.of(document) {
    unitScale = 1.5
    pageGap = 16.0
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}
val canvas = renderer.renderDocument()
val firstPageSize = renderer.pageCanvasSizes[0]
```

It paints that document onto a JavaFX `Canvas`, whole (`renderDocument`) or one
page at a time (`renderPage`), and reports the required canvas size through
`documentCanvasSize` and the index-addressable `pageCanvasSizes[pageIndex]` (and
the page count through `pageCount`). The configuration lambda runs over
`CanvasRenderConfiguration` (`unitScale`, `pageGap`, and the shared
`lineBreakerStrategy` / `wordBreakerStrategy`). See
[Canvas rendering](canvas-rendering.md) for the full reference.

For a ready-made, scrollable and zoomable view, `PaperSheetView` in
`org.pcsoft.framework.simplay.fx` takes a `Document` and shows its pages
as sheets with a border and a drop shadow:

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
mouse and copied with `Ctrl+C` as styled HTML, RTF and plain text. The read-only
`contentSize` property reports the laid-out size; the selection is exposed
through `selectionModel` (a `TextSelectionModel` with `text`, `startIndex` /
`endIndex` / `length`, `bounds`, the styled `runs`, and the `selectRange` /
`selectAll` / `clearSelection` commands), with `selectedText` and
`selectionBounds` kept as convenience delegates.

`mode` picks one of four interaction levels - `STATIC` (a plain picture),
`SELECTABLE` (the default), `NAVIGABLE` (adds a caret without mutating the
document) and `EDITABLE`. Setting `mode = PaperSheetMode.EDITABLE` turns the view into an editor: a blinking
caret, character insertion and removal, clipboard cut / copy / paste, line and
selection duplication, drag-and-drop of the selection and the standard caret
keys (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` /
`Ctrl+Right`, `Backspace`, `Delete`, each optionally with `Shift`). An edit
replaces `document` with a new instance. The caret is exposed through
`caretModel`, a `CaretModel` with the read-only `position` / `bounds` / blink
state, the `blockCount` / `wordCount` / `symbolCount` of the document, the
`currentTextPart` / `currentTextBlock` / `currentPage` / `currentCharacter` the
caret currently sits in or next to, and the linear, absolute-structural and
relative-structural move commands. Setting `smoothCaretBlink = true` (off by
default) fades the caret instead of blinking it hard on and off.

`onType` fires a `PaperSheetTypeEvent` right after a character was typed into
an editable view, carrying the character plus the raw text part, block and
page it landed in. `onMouseEvent` fires a `PaperSheetMouseEvent` while the
mouse hovers or clicks over the view, carrying the raw text part, block and
page under the pointer - `null` over an empty area of a page, or outright for
part and block outside every page.

`PaperSheetView` also accepts floating overlays through
`getFloatingOverlays()` (or an FXML `<floatingOverlays>` child list): a
`FloatingOverlay` node the view shows, positions and hides on its own while its
`trigger` holds (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, and `CARET` while
editing). Overlays follow scroll and zoom and are clamped to the viewport edge.

`PaperSheetView` is styleable through the standard JavaFX CSS mechanism: the
style class is `paper-sheet-view`, exactly one of the `:static`, `:selectable`,
`:navigable` and `:editable` pseudo-classes is active for the current
`PaperSheetMode`, and a default user-agent stylesheet ships
with the module. The `-fx-` properties cover the sheet chrome
(`-fx-sheet-background`, `-fx-sheet-border-color`, `-fx-sheet-border-width`), the
drop shadow (`-fx-shadow-color`, `-fx-shadow-offset`), the selection highlight
(`-fx-selection-color`), the caret (`-fx-caret-color`) and the layout
(`-fx-outer-margin`, `-fx-page-gap`). A programmatic setter wins over the
user-agent stylesheet.

## Usage guides

* [Canvas rendering](canvas-rendering.md) - drawing a `Document` directly onto a
  `Canvas`, whole or per page.
* [Paper sheet component](paper-sheet-component.md) - all `PaperSheetView`
  properties, the read-only and editable modes, scene integration, selection and
  clipboard, and the shortcut table.
* [Floating overlays](floating-overlays.md) - the FXML-compatible overlay API,
  its triggers, binding fields and events.
* [Styling the paper sheet component](styling.md) - the style class, the `-fx-`
  properties, the pseudo-classes, the default stylesheet and an override example.
