# Changelog

All notable end-user visible changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Only changes an end user of the framework can see or notice belong here.
Tests, refactorings, renamings, moved code, build and CI changes, changes to the
rules under `.claude` and changes to the documentation itself are intentionally
excluded.

## [UNRELEASED]

### Added

- `fx`: `CanvasDocumentRenderer` (package `org.pcsoft.framework.simplay.fx.canvas`)
  - the first public entry point of the module. Created for one fixed document
  through `CanvasDocumentRenderer.for(document) { /* configuration */ }`, which
  measures the document and computes the whole canvas layout once, up front. It
  paints that document onto a JavaFX `Canvas`, either the whole document with a
  dashed page-break line between pages (`renderDocument`) or a single page
  (`renderPage`); `documentCanvasSize`, the index-addressable
  `pageCanvasSizes[pageIndex]` and `pageCount` report the geometry. The
  configuration lambda runs over `CanvasRenderConfiguration`
  (`unitScale`, `pageGap`, plus the shared `lineBreakerStrategy` /
  `wordBreakerStrategy`). A `Canvas` can be resized through its `width` /
  `height` at any time, but very tall documents hit the backend texture limit
  and a resize clears the canvas, so the renderer sizes the canvas up front and
  does no tiling.
- `engine`: `RenderConfiguration` (package `org.pcsoft.framework.simplay.engine.engine`),
  a mutable, renderer-agnostic base configuration carrying the
  `lineBreakerStrategy` and `wordBreakerStrategy` a measure step needs, plus the
  `RenderConfiguration.createEngine(measurer)`, `Document.measure(measurer, config)`,
  `MeasuredDocument.documentSize(gap)` and `MeasuredPage.pageSize()` extensions
  so any renderer builds the engine and computes page/document boxes the same
  way.

- `fx`: module-internal JavaFX rendering foundation - `FxFontMeasureCalculator`
  (a `FontMeasureCalculator` backed by the JavaFX text stack), the measured-tree
  draw walk onto a `GraphicsContext` and a glyph-level hit-testing helper. No
  public entry point yet.

- `engine`: raw document model in `...engine.model` - `Document`, `FlowPage` /
  `SinglePage`, `PageLayout`, `TextBlock` with `TextBlock.of(text, style)` and a
  normalizing `toString()`, `TextPart` (`TextWord` / `TextSymbol`), `TextStyle`,
  `Font`, `LineSpacing`, plus `wordCount()` / `symbolCount()` / `charCount()`
  extensions. The model is serializable (JSON, YAML, XML) and, via the
  `PlatformSerializable` marker, usable with JVM serialization.
- `engine`: measured result model in `...engine.measure` - `MeasuredDocument` and
  `MeasuredPage` / `MeasuredTextBlock` / `MeasuredLine` / `MeasuredTextPart` with
  resolved font metrics and absolute geometry (`contentArea`, `effectiveSize`,
  `lineBox`, `baseline`).
- `engine`: `SimpLayEngine`, built through `SimpLayEngine.builder(measurer)`,
  turns a raw `Document` into a `MeasuredDocument` using a caller-supplied
  `FontMeasureCalculator`. Greedy word- and symbol-aware line breaking with line
  spacing and `LEFT` / `RIGHT` / `CENTER` / `JUSTIFY` alignment, automatic
  `FlowPage` continuation and automatic `SinglePage` height growth.
- `engine`: pluggable `LineBreakerStrategy` (default `GreedyWordLineBreakerStrategy`,
  plus `CharacterLineBreakerStrategy` and `NoWrapLineBreakerStrategy`) and a
  `WordBreakerStrategy` hyphenation seam (default `NoOpWordBreakerStrategy`),
  both fixed on the engine builder.
