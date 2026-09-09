# ui-common - Implementation

The `ui-common` module holds the toolkit-agnostic building blocks that every
interactive GUI binding of the engine needs. It has no dependency on a concrete
UI toolkit; text measuring is taken through the `engine` `FontMeasureCalculator`.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-ui-common:<version>")
}
```

The `fx` and `swing` modules depend on it transitively; a consumer of one of
those integrations does not add `ui-common` explicitly.

## What it provides

The package `org.pcsoft.framework.simplay.uicommon` exposes:

* `DocumentTextIndex` - a linear text axis over a `MeasuredDocument`. It joins
  every block into one string (paragraphs separated by a newline), keeps the
  per-character segment, line and part mapping, and offers the structural
  navigation helpers (`startOfBlock` / `endOfBlock`, `nextWordStart`,
  `offsetOfSymbol`, ...) plus `substring` and `styledRuns` for a range.
* `hitTest` - maps a horizontal position inside a `MeasuredTextPart` onto the
  nearest character offset, using a supplied `FontMeasureCalculator` for the
  prefix widths.
* `segmentSpanX` - the `[x0, x1]` span a character range covers inside one
  segment; shared by the selection highlight and the selection bounding box.
* `DocumentEditor` - applies a single insert / delete / replace on the linear
  text axis and rebuilds the affected blocks, returning the new `Document` and
  the caret index for the re-measured text. Pasted line breaks become spaces, so
  an edit never adds a block.
* `StyledTextClipboard` - serialises a styled run list into `text/html` (inline
  CSS) and RTF (font table plus `\fN\fsNN\b\i` runs) for a rich-text paste.

These are the primitives the `fx` `PaperSheetView` skin and the `swing`
`PaperSheetView` UI delegate build their selection, caret, editing and clipboard
behaviour on.
