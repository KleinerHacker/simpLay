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

- `engine`: page numbering. `Document.numbering: PageNumbering` (default off)
  configures where a page number is drawn (`PageNumberPosition`: `OFF`, plus
  every combination of top/bottom with left, center, right, and the
  binding-aware `INNER` / `OUTER` that alternate side by page parity), the
  first displayed number (`startNumber`), a set of pages excluded from
  numbering by their new stable `Page.id`, and how an excluded page affects the
  running count (`PageCountingMode.CONTINUOUS` / `SKIP_EXCLUDED`).
- `ui/fx` and `ui/swing`: `CanvasDocumentRenderer`, `DocumentImageRenderer` and
  both `PaperSheetView` controls now draw the configured page number on every
  page automatically, using `PageNumbering.textStyle` for its font.
- `ui/common`, `ui/fx` and `ui/swing`: individual pages of a `PaperSheetView`
  can be marked deactivated by their stable `Page.id`. The new
  `deactivatedPageIds` and `deactivatedPageHandling` (`PageDeactivationMode`:
  `IGNORE`, `DISABLED`, `READONLY` (default), `HIDDEN`) properties, plus the
  index-based `setPageDeactivated(index, Boolean)` convenience setter, control
  whether a deactivated page is ignored, shown greyed out and locked with the
  caret skipping over it, kept navigable but not editable, or removed from the
  layout entirely. `FloatingOverlayEvent` now reports `pageDeactivated` so a
  registered overlay can react to it.

### Fixed

- `engine`: `TextBlock` no longer invents a space when reconstructing text
  where a symbol (e.g. `.`, `-`, `(`) was directly followed by a word with no
  whitespace in between. Original spaces and tabs are now preserved as
  explicit `TextWhitespace` parts instead of being discarded during
  tokenizing, which also fixes a caret/character-order drift that could occur
  while typing right after such a symbol.

## [0.2.2]

### Added

- `engine`: `FontMeasureCalculator.measureAdvances(font, text)` returns the advance
  width of every character of a string in one call. It has a default implementation
  that measures each glyph through `measure`, so existing calculators keep working
  unchanged; the `fx` and `swing` calculators override it to reuse a single platform
  metrics object. `FontFingerprint.of` now takes its per-glyph advances through it.

### Changed

- `engine`: `FontFingerprint.decode(text)` now rejects a line whose advance section
  is empty or contains a gap between two separators instead of silently returning a
  fingerprint with a truncated advance list.

## [0.2.1]

### Added

- `engine`: font fingerprinting to detect that a document is being reopened with a
  missing or silently replaced font.
    - `Font` gains an optional `fingerprint: FontFingerprint?`. `FontFingerprint`
      (`org.pcsoft.framework.simplay.engine.model`) is a size-independent signature
      of a resolved font face; it is `@Serializable`, round-trips with the model,
      and has `encode()` / `FontFingerprint.decode(text)` for a one-line text form.
    - `Document.withFontFingerprints(measurer, overwrite = true)` returns a copy of
      the document with a fresh fingerprint stamped into every block font; run it
      before persisting.
    - The measure pass re-checks every fingerprinted font and reports the outcome
      as `MeasuredFont.fingerprintStatus` (`NOT_CHECKED` / `MATCH` / `DEVIATION`),
      aggregated as `MeasuredDocument.fingerprintDeviations`.
    - `FontFingerprint.of(measurer, font)` and `FontFingerprint.matches(other,
      tolerance)` take and compare a single fingerprint by hand.
    - `TextBlock.withStyle(style)` returns a copy of a block with its style
      replaced and its parts kept.
- `ui/swing` and `ui/fx`: new `SwingFontProbe` / `FxFontProbe` classes that
  classify a font family against the concrete text stack - `isFamilyAvailable`,
  `checkAvailability` (returning `FontAvailability` `AVAILABLE` / `SUBSTITUTED` /
  `MISSING`, in `org.pcsoft.framework.simplay.uicommon`), plus `fingerprint`,
  `verify` and `stamp` wrappers around the `engine` fingerprint API.

## [0.2.0]

### Added

- `ui/swing`: new Java Swing integration module (artifact `simplay-swing`,
  package `org.pcsoft.framework.simplay.swing`), mirroring the `ui/fx` module
  within what Swing allows. It exposes two `Document`-only entry points:
    - `DocumentImageRenderer` paints one fixed document onto an AWT
      `BufferedImage` (or a caller-supplied `Graphics2D`), either the whole
      document with a dashed page-break line between pages (`renderDocument`) or
      a single page (`renderPage`). It is created through
      `DocumentImageRenderer.of(document) { ... }`, measuring and laying out the
      document once, up front; `documentImageSize`, `pageImageSizes[pageIndex]`
      and `pageCount` report the geometry, and the configuration lambda runs
      over `ImageRenderConfiguration` (`unitScale`, `pageGap`, and the shared
      `lineBreakerStrategy` / `wordBreakerStrategy`).
    - `PaperSheetView` is a scrollable, zoomable `JComponent` that renders a
      document as physical-looking sheets - each with a border and a drop shadow
      - stacked vertically, drawing only the pages in view. `zoom` is kept
      within `[minZoom, maxZoom]`; layout is controlled by `outerMargin` and
      `pageGap`. Text is selected with the mouse (drag to extend, double-click
      for a word) and copied with `Ctrl+C` / `Cmd+C` as plain text plus styled
      HTML and RTF that carry the font. The selection is exposed through
      `selectionModel` (`TextSelectionModel`: `text`, `startIndex` / `endIndex`
      / `length`, `bounds`, styled `runs`, and the `selectRange` / `selectAll` /
      `clearSelection` commands), with `selectedText` / `selectionBounds` as
      convenience delegates.
    - Setting `mode = PaperSheetMode.EDITABLE` adds a blinking caret (hard or,
      with `smoothCaretBlink`, fading), character insertion and removal,
      clipboard cut / copy / paste, line and selection duplication,
      drag-and-drop of the selection and the standard caret-navigation keys. An
      edit replaces `document` with a new instance. The caret is exposed through
      `caretModel` (`CaretModel`: `position`, `bounds`, blink state, the
      `blockCount` / `wordCount` / `symbolCount` of the document and the linear,
      absolute-structural and relative-structural move commands).
    - `PaperSheetView` accepts floating overlays through `floatingOverlays`: a
      `FloatingOverlay` component the view shows, positions and hides on its own
      while its `trigger` holds (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`,
      and `CARET` while editing), following scroll and zoom and clamped to the
      viewport edge.
    - `PaperSheetView` is styled through the active Look-and-Feel: the
      `PaperSheetView.*` keys (`sheetBackground`, `sheetBorderColor`,
      `sheetBorderWidth`, `shadowColor`, `shadowOffset`, `selectionColor`,
      `selectionColorReadonly`, `caretColor`, `outerMargin`, `pageGap`) seeded
      by `PaperSheetLookAndFeel`, with a programmatic setter always winning.
      Every mutable property fires a `java.beans.PropertyChangeEvent`.
- `ui/common`: new published module (`simplay-common`, package
  `org.pcsoft.framework.simplay.uicommon`) with the toolkit-agnostic building
  blocks shared by `ui/fx` and `ui/swing`: `DocumentTextIndex` (linear document
  text axis), `hitTest`, `segmentSpanX`, `DocumentEditor` and
  `StyledTextClipboard`.

## [0.1.0]

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
