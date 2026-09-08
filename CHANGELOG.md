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

- `fx`: new JavaFX integration module (package `org.pcsoft.framework.simplay.fx`).
  It exposes two `Document`-only entry points:
    - `CanvasDocumentRenderer` paints one fixed document onto a JavaFX `Canvas`,
      either the whole document with a dashed page-break line between pages
      (`renderDocument`) or a single page (`renderPage`). It is created through
      `CanvasDocumentRenderer.of(document) { ... }`, measuring and laying out
      the document once, up front; `documentCanvasSize`,
      `pageCanvasSizes[pageIndex]` and `pageCount` report the geometry, and the
      configuration lambda runs over `CanvasRenderConfiguration` (`unitScale`,
      `pageGap`, and the shared `lineBreakerStrategy` / `wordBreakerStrategy`).
      The canvas is sized up front, without tiling, because very tall documents
      hit the graphics backend's texture limit.
    - `PaperSheetView` is a scrollable, zoomable `Control` that renders a
      document as physical-looking sheets - each with a border and a drop shadow
      - stacked vertically, drawing only the pages in view. `zoom` is kept within
      `[minZoom, maxZoom]`; layout is controlled by `outerMargin` and `pageGap`.
      Text is selected with the mouse (drag to extend, double-click for a word)
      and copied with `Ctrl+C` as plain text plus styled HTML and RTF that carry
      the font. The selection is exposed through `selectionModel`
      (`TextSelectionModel`: `text`, `startIndex` / `endIndex` / `length`,
      `bounds`, styled `runs`, and the `selectRange` / `selectAll` /
      `clearSelection` commands), with `selectedText` / `selectionBounds` as
      convenience delegates, and `contentSize` reports the laid-out size.
  - `PaperSheetView` has a `mode` property switching between
    `PaperSheetMode.READONLY` (selectable, copyable text, no caret) and
    `PaperSheetMode.EDITABLE`, which adds a blinking caret, character insertion
    and removal, clipboard cut / copy / paste (`Ctrl+X` / `Ctrl+C` / `Ctrl+V`),
    line and selection duplication (`Ctrl+D`), drag-and-drop of the selection
    (hold `Ctrl` to copy instead of move) and the standard caret keys (`Home`,
    `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` / `Ctrl+Right`,
    `Backspace`, `Delete`, each optionally with `Shift`). An edit replaces
    `document` with a new instance; the previous one is not mutated. The caret is
    exposed through `caretModel` (`CaretModel`: read-only `position`, `bounds`,
    blink `visible`, `blockCount` / `wordCount` / `symbolCount`, plus linear,
    absolute-structural and relative-structural move commands), and
    `smoothCaretBlink` (off by default) fades the caret instead of blinking it.
  - `PaperSheetView` accepts floating overlays through `getFloatingOverlays()`
    (or an FXML `<floatingOverlays>` child list): a `FloatingOverlay` node the
    view shows, positions and hides on its own while its `trigger` holds
    (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`). Each overlay carries
    `content`, `anchor` (`Pos`) with `offsetX` / `offsetY`, `autoHide`, the
    read-only `active` / `activeBounds` / `activeIndex` / `activeText` /
    `activeDocumentRange` fields (bindable from FXML via `${id.activeText}`) and
    `onShown` / `onHidden` handlers receiving a `FloatingOverlayEvent`. Overlays
    follow scroll and zoom and are clamped to the viewport edge; the view also
    exposes read-only `hoveredParagraph` / `hoveredPage` (+ bounds).
  - `PaperSheetView` is styleable through the standard JavaFX CSS mechanism: the
    `paper-sheet-view` style class, a `:readonly` pseudo-class (active while
    `mode` is `PaperSheetMode.READONLY`), a bundled default user-agent stylesheet
    and the `-fx-` properties `-fx-sheet-background`, `-fx-sheet-border-color`,
    `-fx-sheet-border-width`, `-fx-shadow-color`, `-fx-shadow-offset`,
    `-fx-selection-color`, `-fx-caret-color`, `-fx-outer-margin` and
    `-fx-page-gap`, mirrored by Kotlin properties. Every colour is a `Paint`
    (a gradient works too) except `-fx-caret-color` (`Color`); a programmatic
    setter wins over the user-agent stylesheet.
- `engine`: raw document model in `...engine.model` - `Document`, `FlowPage` /
  `SinglePage`, `PageLayout`, `TextBlock` with `TextBlock.of(text, style)` and a
  normalizing `toString()`, `TextPart` (`TextWord` / `TextSymbol`), `TextStyle`,
  `Font`, `LineSpacing`, plus `wordCount()` / `symbolCount()` / `charCount()`
  extensions - and the measured result model in `...engine.measure` -
  `MeasuredDocument` and `MeasuredPage` / `MeasuredTextBlock` / `MeasuredLine` /
  `MeasuredTextPart` with resolved font metrics and absolute geometry
  (`contentArea`, `effectiveSize`, `lineBox`, `baseline`). The raw model is
  serializable (JSON, YAML, XML) and, via the `PlatformSerializable` marker,
  usable with JVM serialization.
- `engine`: `SimpLayEngine`, built through `SimpLayEngine.builder(measurer)`,
  turns a raw `Document` into a `MeasuredDocument` using a caller-supplied
  `FontMeasureCalculator`. Word- and symbol-aware greedy line breaking with line
  spacing and `LEFT` / `RIGHT` / `CENTER` / `JUSTIFY` alignment, automatic
  `FlowPage` continuation and automatic `SinglePage` height growth. Line breaking
  is pluggable through `LineBreakerStrategy` (default
  `GreedyWordLineBreakerStrategy`, plus `CharacterLineBreakerStrategy` and
  `NoWrapLineBreakerStrategy`) and a `WordBreakerStrategy` hyphenation seam
  (default `NoOpWordBreakerStrategy`), both fixed on the builder.
- `engine`: `RenderConfiguration` (package `org.pcsoft.framework.simplay.engine`),
  a mutable, renderer-agnostic base configuration carrying the
  `lineBreakerStrategy` and `wordBreakerStrategy` a measure step needs, plus the
  `RenderConfiguration.createEngine(measurer)`, `Document.measure(measurer, config)`,
  `MeasuredDocument.documentSize(gap)` and `MeasuredPage.pageSize()` extensions
  so any renderer builds the engine and computes page/document boxes the same
  way.
