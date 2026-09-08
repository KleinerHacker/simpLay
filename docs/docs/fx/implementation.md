# fx - Implementation

The `fx` module binds a running simulation from the `engine` core to a JavaFX user
interface.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-fx:<version>")
}
```

JavaFX is exposed transitively by this module, so no separate JavaFX dependency is
required in the consuming build.

## Entry points

The first public entry point is `CanvasDocumentRenderer` in
`org.pcsoft.framework.simplay.fx`. It is created for one fixed document
and measures it up front:

```kotlin
val renderer = CanvasDocumentRenderer.`for`(document) {
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
`lineBreakerStrategy` / `wordBreakerStrategy`).

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

Setting `mode = PaperSheetMode.NORMAL` turns the view into an editor: a blinking
caret, character insertion and removal, clipboard cut / copy / paste, line and
selection duplication, drag-and-drop of the selection and the standard caret
keys (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` /
`Ctrl+Right`, `Backspace`, `Delete`, each optionally with `Shift`). An edit
replaces `document` with a new instance. The caret is exposed through
`caretModel`, a `CaretModel` with the read-only `position` / `bounds` / blink
state, the `blockCount` / `wordCount` / `symbolCount` of the document and the
linear, absolute-structural and relative-structural move commands. Setting
`smoothCaretBlink = true` (off by default) fades the caret instead of blinking it
hard on and off.

`PaperSheetView` also accepts floating overlays through
`getFloatingOverlays()` (or an FXML `<floatingOverlays>` child list): a
`FloatingOverlay` node the view shows, positions and hides on its own while its
`trigger` holds (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, and `CARET` while
editing). Overlays follow scroll and zoom and are clamped to the viewport edge.

`PaperSheetView` is styleable through the standard JavaFX CSS mechanism: the
style class is `paper-sheet-view`, a `:readonly` pseudo-class is active while
`mode` is `PaperSheetMode.READONLY`, and a default user-agent stylesheet ships
with the module. The `-fx-` properties cover the sheet chrome
(`-fx-sheet-background`, `-fx-sheet-border-color`, `-fx-sheet-border-width`), the
drop shadow (`-fx-shadow-color`, `-fx-shadow-offset`), the selection highlight
(`-fx-selection-color`), the caret (`-fx-caret-color`) and the layout
(`-fx-outer-margin`, `-fx-page-gap`). A programmatic setter wins over the
user-agent stylesheet.

!!! note

    A full usage page for the canvas renderer, the paper-sheet component, the
    floating-overlay API and the styling reference follows with the `fx`
    documentation plan (IP-07).
