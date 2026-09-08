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

- `fx`: editing for `PaperSheetView`. A new `mode` property switches between
  `PaperSheetMode.READONLY` (the previous behaviour: selectable, copyable text,
  no caret) and `PaperSheetMode.NORMAL`, which adds a blinking caret, character
  insertion and removal, clipboard cut / copy / paste (`Ctrl+X` / `Ctrl+C` /
  `Ctrl+V`), line and selection duplication (`Ctrl+D`), drag-and-drop of the
  selection (hold `Ctrl` to copy instead of move) and the standard caret keys
  (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows, `Ctrl+Left` / `Ctrl+Right`,
  `Backspace`, `Delete`, each optionally with `Shift` to extend the selection).
  Editing replaces `document` with a new instance; the previous document is not
  mutated. The caret is exposed through the new `caretModel`, a `CaretModel` with
  the read-only `position`, viewport `bounds`, blink `visible` state and the
  `blockCount` / `wordCount` / `symbolCount` of the document, plus linear
  (`moveTo`, `moveToStart`, `moveToEnd`), absolute-structural (`moveIntoBlock` /
  `moveToStartOfBlock` / `moveToEndOfBlock` and the word / symbol siblings) and
  relative-structural (`moveToNextWord` / `moveToPrevWord` and the block / symbol
  siblings) move commands. A `smoothCaretBlink` property (off by default) fades
  the caret in and out instead of blinking it hard on and off. The `CARET`
  floating-overlay trigger is now live and anchors an overlay to the caret
  rectangle. The demo gains a `Read/Write` tab
  hosting the control in normal mode with a mode selector, the `Readonly`
  toolbar's settings and a live caret / document read-out.
- `fx`: `PaperSheetView` (package `org.pcsoft.framework.simplay.fx`), a
  read-only JavaFX `Control` that renders a `Document` as physical-looking sheets
  - each with a border and a drop shadow - stacked vertically in a scrollable,
  zoomable viewport. Only the pages currently in the viewport are drawn (simple
  page virtualisation), and `zoom` is always kept within `[minZoom, maxZoom]`.
  Layout is controlled by `outerMargin` and `pageGap`. Text is selected with the
  mouse (drag to extend, double-click for a word), the highlight also covering
  the whitespace between selected words, and copied to the system clipboard with
  `Ctrl+C` as plain text plus styled HTML and RTF that carry the font (family,
  size, weight, slant) so a rich paste target keeps the text style; there is no
  caret. The pointer shows a text (I-beam)
  cursor while it is over a page's content area. The read-only `contentSize`
  property reports the laid-out size; the selection is exposed through
  `selectionModel`, a `TextSelectionModel` carrying the selected `text`, the
  character range (`startIndex` / `endIndex` / `length`, `empty`), the viewport
  `bounds` and the styled `runs` (one per covered text part, with font family,
  size, bold and italic), and offering the `selectRange`, `selectAll` and
  `clearSelection` commands. `selectedText` and `selectionBounds` remain as
  convenience delegates. The demo's `Readonly` tab hosts the control with a
  toolbar for every setting plus `Select all` / `Clear` buttons and a live
  read-out of the selection range and run count.
- `fx`: floating overlays for `PaperSheetView`. A `FloatingOverlay` (package
  `org.pcsoft.framework.simplay.fx`) is a caller-supplied node the view
  shows, positions and hides on its own when its `trigger` holds - a non-empty
  text selection (`SELECTION`), the mouse over a paragraph (`PARAGRAPH_HOVER`) or
  over a sheet (`PAGE_HOVER`); a `CARET` trigger constant exists but stays inert
  until editing is added. Each overlay carries a `content` node, an `anchor`
  (`Pos`) with `offsetX` / `offsetY`, an `autoHide` flag, the read-only fields
  `active`, `activeBounds`, `activeIndex`, `activeText` and `activeDocumentRange`,
  and `onShown` / `onHidden` handlers that receive a `FloatingOverlayEvent` with
  the same context. Overlays are registered through
  `PaperSheetView.getFloatingOverlays()` and can equally be declared in FXML as
  `<floatingOverlays>` children with their fields bound via `${id.activeText}`
  and friends. The view also exposes read-only `hoveredParagraph` /
  `hoveredParagraphBounds` and `hoveredPage` / `hoveredPageBounds`. Active
  overlays follow scroll and zoom, are clamped to the viewport edge while their
  anchor is partly out of view and hidden once it leaves entirely. The demo's
  `Readonly` tab shows a `Copy` bar over the selection and a badge over the
  hovered paragraph.
- `fx`: `CanvasDocumentRenderer` (package `org.pcsoft.framework.simplay.fx`)
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
- `engine`: `RenderConfiguration` (package `org.pcsoft.framework.simplay.engine`),
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
