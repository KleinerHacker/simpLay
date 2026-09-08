# Feature Status: JavaFX Rendering

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | FX Rendering Foundation | COMPLETED |
| IP-02 | Canvas Renderer | COMPLETED |
| IP-03 | Paper Sheet Component | COMPLETED |
| IP-04 | Floating Overlays | COMPLETED |
| IP-05 | Editing And Key Commands | COMPLETED |
| IP-06 | Paper Component Styling | COMPLETED |
| IP-07 | Documentation | NOT_STARTED |

## Overall Progress

86%

## Notes

IP-01 is done: the `fx` module now has the internal JavaFX binding
(`FxFontMeasureCalculator`, the `walkPage` / `renderPage` / `renderDocument` draw
walk, `pageSize` / `documentSize`, the `hitTest` glyph helper, the
`measureDocument` engine facade) plus the `fx/src/demo` source set with a `run`
task and the empty three-tab shell (`Canvas`, `Readonly`, `Read/Write`), each tab
a `BorderPane` with an empty `ToolBar`. Headless TestFX/Monocle tests cover the
measurer, walk, sizing and hit-test. `./gradlew :fx:build` and `:fx:run` are
green. Everything stays in the `fx` module.

IP-02 is done: the `fx` module now exposes the public `CanvasDocumentRenderer`
(`...fx`), created for one fixed document via
`CanvasDocumentRenderer.`for`(document) { ... }` which measures and lays it out
once, up front. It paints that document onto a `Canvas`, whole (`renderDocument`,
dashed page-break lines) or per page (`renderPage`), with `documentCanvasSize` /
`pageCanvasSizes` / `pageCount` and a builder-lambda `CanvasRenderConfiguration`
(`unitScale`, `pageGap`, shared `lineBreakerStrategy` / `wordBreakerStrategy`).
The shared render configuration and the measure/size helpers were lifted into
`engine` (`RenderConfiguration`, `createEngine`, `Document.measure`,
`MeasuredDocument.documentSize`, `MeasuredPage.pageSize`) as an agreed special
case - the only `engine` change in FP-003. The demo `Canvas` tab is filled with
`CanvasDemoTab`. `./gradlew :engine:build :fx:build` is green.

IP-03 is done: the `fx` module now exposes the public `PaperSheetView` (package
`...fx`), a read-only `Control` that stacks a `Document`'s pages as sheets
(white fill, grey border, drop-shadow rectangle) in a viewport-sized `Canvas`,
scaled by `zoom` (clamped to `[minZoom, maxZoom]`), drawing only the pages in
view (simple virtualisation) and driving a vertical `ScrollBar`. Mouse drag
selects text over the measured geometry (`DocumentTextIndex` + the IP-01
`hitTest`), double-click selects a word, `Ctrl+C` copies styled HTML + RTF +
plain text; `contentSize` is a read-only output. The demo `Readonly` tab is
filled with `ReadonlyDemoTab`. `./gradlew :fx:build` is green. The internal
`TextSelection` and `PaperSheetViewSkin` stay module-private; the canvas painting
sits in the internal `PaperSheetCanvasPainter`; `contentSize` is `0 x 0` for a
null document.

Post-IP-03 follow-up (plan `FP-003-TextSelectionModel.md`, done): the selection is
exposed through a public `TextSelectionModel` reached via
`PaperSheetView.selectionModel` - read-only `text`, `startIndex` / `endIndex` /
`length` / `empty`, `bounds` and styled `runs` (`TextSelectionData`) - plus the
`selectRange` / `selectAll` / `clearSelection` commands. `selectedText` and
`selectionBounds` remain as delegates on the view. The `Readonly` demo tab gained
`Select all` / `Clear` buttons and a range/run-count read-out.

IP-04 is done: `PaperSheetView` gained the FXML-compatible floating-overlay API.
`FloatingOverlay` (`...fx`) is a no-arg bean with `content`, `trigger`
(`FloatingOverlayTrigger`: `SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`),
`anchor` (`Pos`), `offsetX` / `offsetY`, `autoHide`, the read-only fields
`active` / `activeBounds` / `activeIndex` / `activeText` / `activeDocumentRange`
(bindable from FXML via `${id.prop}`) and `onShown` / `onHidden`
(`EventHandler<FloatingOverlayEvent>`). `PaperSheetView.getFloatingOverlays()`
returns an `ObservableList` fillable in code and from FXML as a
`<floatingOverlays>` read-only list; the view also exposes read-only
`hoveredParagraph` / `hoveredPage` (+ `Bounds`). The skin owns an internal
overlay `Pane` on top of the viewport, detects the selection, paragraph-hover and
page-hover triggers, positions each overlay by `anchor` + offsets, tracks scroll
and zoom, clamps to the viewport edge and hides once the anchor leaves it. The
`CARET` trigger is inert until IP-05. The `Readonly` demo tab shows a `Copy` bar
on the selection and a badge on the hovered paragraph. Headless tests
(`FloatingOverlayTest`, extra `PaperSheetViewSkinTest` cases) cover the triggers,
scroll/zoom tracking, edge clamping, the inert `CARET`, the `onShown` / `onHidden`
context and an `FXMLLoader` round-trip. `./gradlew :fx:build` is green.

IP-05 is done: `PaperSheetView` gained a `mode` property
(`PaperSheetMode.READONLY` / `NORMAL`). In normal mode the internal
`PaperSheetViewSkin` runs a blinking caret (a `Timeline`), character typing via
`onKeyTyped`, the navigation / editing keys via `onKeyPressed`
(`Home` / `End` / `Ctrl+Home` / `Ctrl+End` / arrows / `Ctrl+Left` /
`Ctrl+Right` / `Backspace` / `Delete`, each with optional `Shift`), clipboard
`Ctrl+V` / `Ctrl+X` / `Ctrl+D`, and drag-and-drop of the selection (mouse press
inside the selection box starts a move; `Ctrl` on release copies). Every edit
goes through the new internal `DocumentEditor` object, which splices
`DocumentTextIndex.text`, cuts it back into raw blocks along the block ranges
remapped through the splice (merging blocks a delete joined across their
boundary), rebuilds affected blocks with `TextBlock.of` and keeps the page list
and types; it returns the new `Document` plus the caret index, and the skin
restores the caret after the re-measure the `document` assignment triggers. The
caret is exposed publicly through `caretModel` (`CaretModel`, built like
`TextSelectionModel`): read-only `position` / `bounds` / `visible` /
`blockCount` / `wordCount` / `symbolCount` and the linear, absolute-structural
(block / word / symbol) and relative-structural (`moveToNext*` / `moveToPrev*`)
move commands, routed through a `PaperSheetView.CaretCommands` sink with the same
pre-skin buffering as the selection commands. `DocumentTextIndex` gained
`blockRanges` / `wordRanges` / `symbolRanges` and the ordinal resolution helpers.
The `CARET` floating-overlay trigger is now live (`geometryFor` returns the caret
box in normal mode). The demo's `Read/Write` tab is filled with
`ReadWriteDemoTab`. Headless tests: `DocumentEditorTest`, `CaretModelTest`,
`PaperSheetEditingTest`. `./gradlew build` is green.

Known limitation: an intentionally empty raw block (a block whose text is empty)
produces no measured glyph and no `blockRanges` entry, so the first edit drops
it. None of the sample or fixture documents contain empty blocks.

IP-06 is done: `PaperSheetView` is styleable through the standard JavaFX CSS
mechanism. New `internal object PaperSheetStyleableProperties` holds the
`CssMetaData` for `-fx-sheet-background` / `-fx-sheet-border-color` /
`-fx-sheet-border-width` / `-fx-shadow-color` / `-fx-shadow-offset` /
`-fx-selection-color` / `-fx-caret-color` / `-fx-outer-margin` / `-fx-page-gap`;
every colour is a `Paint` except `-fx-caret-color` (`Color`). `outerMarginProperty`
/ `pageGapProperty` became `StyleableDoubleProperty`, the new colour/size
properties are `SimpleStyleableObjectProperty` / `SimpleStyleableDoubleProperty`
with public bean accessors, and `getControlCssMetaData()` / `getUserAgentStylesheet()`
are overridden (the latter loads the bundled
`fx/src/main/resources/.../paper-sheet-view.css`). A `:readonly` pseudo-class is
wired to `modeProperty`; `:focused` is left to the JavaFX `Node` default (no own
wiring). The skin builds a `PaperSheetStyle` value from the properties and passes
it to `PaperSheetCanvasPainter.paint`, and a change listener on every styleable
property repaints; the painter keeps its former constants as the `PaperSheetStyle`
defaults and gained a `paintCount` test hook. A planned separate
`PaperSheetStyleableProperties.kt` under `fx` was placed under `fx.internal.ps`
next to the painter. The static `getClassCssMetaData()` accessor was dropped -
Kotlin cannot hide the inherited `Control` static - `getControlCssMetaData()` is
the entry point. The demo `Readonly` / `Read/Write` tabs gained a `Stylesheet`
`ChoiceBox` (Standard / Dark) toggling the bundled `demo-dark.css`. Headless
tests: `PaperSheetStylingTest`. `./gradlew :fx:build` is green.

IP-07 comes last and writes the four `docs/docs/fx/` pages.
