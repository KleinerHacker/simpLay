# Feature Status: JavaFX Rendering

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | FX Rendering Foundation | COMPLETED |
| IP-02 | Canvas Renderer | COMPLETED |
| IP-03 | Paper Sheet Component | COMPLETED |
| IP-04 | Floating Overlays | NOT_STARTED |
| IP-05 | Editing And Key Commands | NOT_STARTED |
| IP-06 | Paper Component Styling | NOT_STARTED |
| IP-07 | Documentation | NOT_STARTED |

## Overall Progress

43%

## Notes

IP-01 is done: the `fx` module now has the internal JavaFX binding
(`FxFontMeasureCalculator`, the `walkPage` / `renderPage` / `renderDocument`
draw walk, `pageSize` / `documentSize`, the `hitTest` glyph helper, the
`measureDocument` engine facade) plus the `fx/src/demo` source set with a `run`
task and the empty three-tab shell (`Canvas`, `Readonly`, `Read/Write`), each
tab a `BorderPane` with an empty `ToolBar`. Headless TestFX/Monocle tests cover
the measurer, walk, sizing and hit-test. `./gradlew :fx:build` and `:fx:run`
are green. Everything stays in the `fx` module.

IP-02 is done: the `fx` module now exposes the public `CanvasDocumentRenderer`
(`...fx.canvas`), created for one fixed document via
`CanvasDocumentRenderer.`for`(document) { ... }` which measures and lays it out
once, up front. It paints that document onto a `Canvas`, whole
(`renderDocument`, dashed page-break lines) or per page (`renderPage`), with
`documentCanvasSize` / `pageCanvasSizes` / `pageCount` and a builder-lambda
`CanvasRenderConfiguration` (`unitScale`, `pageGap`, shared
`lineBreakerStrategy` / `wordBreakerStrategy`). The shared render configuration and the measure/size
helpers were lifted into `engine` (`RenderConfiguration`, `createEngine`,
`Document.measure`, `MeasuredDocument.documentSize`, `MeasuredPage.pageSize`) as
an agreed special case - the only `engine` change in FP-003. The demo `Canvas`
tab is filled with `CanvasDemoTab` (sample, unit scale, page gap, line-/word-
break strategy, whole/single-page mode, canvas-size read-back).
`./gradlew :engine:build :fx:build` is green.

IP-03 is done: the `fx` module now exposes the public `PaperSheetView` (package
`...fx.control`), a read-only `Control` that stacks a `Document`'s pages as sheets
(white fill, grey border, drop-shadow rectangle) in a viewport-sized `Canvas`,
scaled by `zoom` (clamped to `[minZoom, maxZoom]`) via `GraphicsContext.scale`,
drawing only the pages in view (simple virtualisation) and driving a vertical
`ScrollBar` whose `max` is `contentHeight*zoom - viewport`. Mouse drag selects
text over the measured geometry (`DocumentTextIndex` + the IP-01 `hitTest`),
double-click selects a word, `Ctrl+C` copies styled HTML + RTF + plain text;
`contentSize` is a read-only output. The demo `Readonly` tab is filled with
`ReadonlyDemoTab` (sample, outer margin, page gap, min/max/current zoom, zoom and
selection read-back). `./gradlew :fx:build` is green. The internal `TextSelection`
and `PaperSheetViewSkin` stay module-private; the canvas painting (virtualisation
loop, sheet chrome, selection highlight, page text) sits in the internal
`PaperSheetCanvasPainter` and the `renderPage` frame hook is left null;
`contentSize` is `0 x 0` for a null document.

Post-IP-03 follow-up (plan `FP-003-TextSelectionModel.md`, done): the selection is
exposed through a public `TextSelectionModel` reached via `PaperSheetView.selectionModel`
- read-only `text`, `startIndex` / `endIndex` / `length` / `empty`, `bounds` and
  styled `runs` (`TextSelectionData`: family, size, bold, italic) - plus the
`selectRange` / `selectAll` / `clearSelection` commands (a command issued before
  the skin exists is buffered and replayed on skin init). `selectedText` and
`selectionBounds` remain as delegates on the view. The `Readonly` demo tab gained
`Select all` / `Clear` buttons and a range/run-count read-out.

IP-04 (floating overlays, FXML-compatible), IP-05 (editing) and IP-06 (JavaFX CSS
styling) all depend on IP-03 and are parallelizable with each other; IP-05
optionally wires the `CARET` overlay trigger from IP-04. IP-07 comes last and
writes the four `docs/docs/fx/` pages (canvas rendering, paper-sheet usage,
floating overlays, styling).
