# Feature Status: JavaFX Rendering

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | FX Rendering Foundation | COMPLETED |
| IP-02 | Canvas Renderer | NOT_STARTED |
| IP-03 | Paper Sheet Component | NOT_STARTED |
| IP-04 | Editing And Key Commands | NOT_STARTED |
| IP-05 | Paper Component Styling | NOT_STARTED |
| IP-06 | Documentation | NOT_STARTED |

## Overall Progress

17%

## Notes

IP-01 is done: the `fx` module now has the internal JavaFX binding
(`FxFontMeasureCalculator`, the `walkPage` / `renderPage` / `renderDocument`
draw walk, `pageSize` / `documentSize`, the `hitTest` glyph helper, the
`measureDocument` engine facade) plus the `fx/src/demo` source set with a `run`
task and the empty three-tab shell (`Canvas`, `Readonly`, `Read/Write`), each
tab a `BorderPane` with an empty `ToolBar`. Headless TestFX/Monocle tests cover
the measurer, walk, sizing and hit-test. `./gradlew :fx:build` and `:fx:run`
are green. Everything stays in the `fx` module.

IP-02 and IP-03 depend on IP-01 and are now unblocked and parallelizable; IP-04
(editing) and IP-05 (JavaFX CSS styling) both depend on IP-03 and are
parallelizable with each other. IP-06 comes last and writes the three
`docs/docs/fx/` pages.
