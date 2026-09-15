# fx - Canvas rendering

`CanvasDocumentRenderer` (package `org.pcsoft.framework.simplay.fx`) is the
lowest-level public entry point of the module. It measures one fixed
[`Document`](../engine/raw-model.md) up front and paints it - whole, or a single
page - onto a plain JavaFX `Canvas`. There is no scrolling, no zoom and no sheet
chrome; use [`PaperSheetView`](paper-sheet-component.md) when you need those.

The JavaFX toolkit must already be running when the renderer is created, because
measuring happens in the factory.

## Creating a renderer

The document is bound through the `of` factory, which takes a builder lambda over
[`CanvasRenderConfiguration`](#configuration):

```kotlin
import org.pcsoft.framework.simplay.engine.NoWrapLineBreakerStrategy
import org.pcsoft.framework.simplay.fx.CanvasDocumentRenderer

val renderer = CanvasDocumentRenderer.of(document) {
    unitScale = 1.5
    pageGap = 16.0
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}
```

The instance is immutable and bound to that one document; it is not thread-safe.
Create a new renderer for a new document.

## Rendering the whole document

`renderDocument()` stacks every page vertically, one `pageGap` apart, and draws a
dashed separator line in the middle of each gap band:

```kotlin
val canvas: Canvas = renderer.renderDocument()          // new canvas, exact size
myPane.children += canvas
```

Pass an existing canvas to draw into it instead; it is grown when it is too small
and never shrunk or replaced:

```kotlin
renderer.renderDocument(existingCanvas)
```

## Rendering a single page

`renderPage(pageIndex)` draws exactly one page, its top at the canvas origin and
no separator line. `pageIndex` is zero-based and validated against the measured
page count:

```kotlin
val firstPage: Canvas = renderer.renderPage(0)
```

`pageCount` reports how many pages the document measures to, after `FlowPage`
continuation and `SinglePage` growth.

## Canvas sizes

The renderer reports the sizes it needs as `javafx.geometry.Dimension2D`, already
multiplied by `unitScale`:

| Accessor | Meaning |
|----------|---------|
| `documentCanvasSize` | Size `renderDocument()` needs; `0 x 0` for an empty document. |
| `pageCanvasSizes[pageIndex]` | Size `renderPage(pageIndex)` needs for that page. |
| `pageCanvasSizes.count` | Number of addressable pages; identical to `pageCount`. |

```kotlin
val full = renderer.documentCanvasSize
val page0 = renderer.pageCanvasSizes[0]
```

## Configuration

`CanvasRenderConfiguration` extends the shared
[`RenderConfiguration`](../engine/simplay-engine.md#building-the-engine) and adds
two values on top:

| Property | Default | Meaning |
|----------|---------|---------|
| `unitScale` | `1.0` | Factor applied to every layout coordinate and to the resulting canvas size. `1.0` draws one layout unit per pixel. Must be `> 0`. |
| `pageGap` | `24.0` | Vertical space between two pages, in layout units before scaling. The dashed page-break line sits in the middle of this band. Must be `>= 0`. |
| `lineBreakerStrategy` | `GreedyWordLineBreakerStrategy` | Inherited; how block parts become lines. |
| `wordBreakerStrategy` | `NoOpWordBreakerStrategy` | Inherited; intra-word (hyphenation) seam. |

An out-of-range `unitScale` or `pageGap` makes the factory throw
`IllegalArgumentException`.

### Unit scaling

`unitScale` multiplies both the drawing coordinates and the canvas size, so text
stays sharp (it is re-rasterised at the scaled size, not stretched). It is the
right knob for a fixed high-resolution export; for an interactive zoom that
changes at runtime use `PaperSheetView.zoom` instead.

## A note on canvas size

A JavaFX `Canvas` can be resized through its `width` / `height` at any time, but
very tall documents run into the graphics backend's maximum texture size
(commonly a few thousand up to `16384` pixels per axis) and into heap pressure,
and a resize clears the canvas. `CanvasDocumentRenderer` therefore sizes the
canvas once, up front, from `documentCanvasSize` and does no tiling. A document
taller than the platform limit is out of scope for this renderer.

## Page numbers

When the bound document has `document.numbering` configured (anything other than
`PageNumbering.OFF`), every `renderDocument` / `renderPage` call also draws the
page numbers, resolved through `MeasuredDocument.planPageNumbers` from the engine (see
[engine - Rendering: Page numbering](../engine/rendering.md#page-numbering)) and
drawn with `numbering.textStyle` through the same `FxFontMeasureCalculator` used
for the document text. No extra call or configuration is needed on the renderer
itself - the numbering lives on the document.

## Next

* [Paper sheet component](paper-sheet-component.md) - the scrollable, zoomable
  sheet control.
* [Rendering](../engine/rendering.md) - the measured-tree walk this renderer performs.
