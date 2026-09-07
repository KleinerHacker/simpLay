# Feature Status: JavaFX Rendering

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | FX Rendering Foundation | COMPLETED |
| IP-02 | Canvas Renderer | COMPLETED |
| IP-03 | Paper Sheet Component | NOT_STARTED |
| IP-04 | Editing And Key Commands | NOT_STARTED |
| IP-05 | Paper Component Styling | NOT_STARTED |
| IP-06 | Documentation | NOT_STARTED |

## Overall Progress

33%

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

IP-03 depends on IP-01 and is unblocked and parallelizable with IP-02; IP-04
(editing) and IP-05 (JavaFX CSS styling) both depend on IP-03 and are
parallelizable with each other. IP-06 comes last and writes the three
`docs/docs/fx/` pages.
