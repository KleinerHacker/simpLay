# swing - Image rendering

`DocumentImageRenderer` in `org.pcsoft.framework.simplay.swing` paints a fixed
`Document` onto an AWT `BufferedImage` or a caller-supplied `Graphics2D`. It is
the Swing counterpart of the `fx` module's `CanvasDocumentRenderer`.

## Creating a renderer

The document is bound at creation through the `of` factory, which measures it and
computes the whole layout (sizes, page origins, page-break positions) once, up
front:

```kotlin
val renderer = DocumentImageRenderer.of(document) {
    unitScale = 1.5
    pageGap = 16.0
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}
```

The configuration lambda runs over `ImageRenderConfiguration`, which extends the
shared `RenderConfiguration`:

| Property | Type | Default | Effect |
|----------|------|---------|--------|
| `unitScale` | `Double` | `1.0` | Factor applied to every layout coordinate and to the image size; must be `> 0`. |
| `pageGap` | `Double` | `24.0` | Vertical space between two pages, in layout units; must be `>= 0`. The dashed page-break line is drawn in its middle. |
| `lineBreakerStrategy` | from `RenderConfiguration` | greedy word | The line-breaking strategy the measure step uses. |
| `wordBreakerStrategy` | from `RenderConfiguration` | no-op | The intra-word hyphenation seam. |

`of` throws `IllegalArgumentException` if `unitScale <= 0` or `pageGap < 0`.

## Rendering

```kotlin
val whole: BufferedImage = renderer.renderDocument()
val page: BufferedImage = renderer.renderPage(0)
```

* `renderDocument()` returns a new `TYPE_INT_ARGB` image of exactly
  `documentImageSize`, with all pages stacked `pageGap` apart and a dashed line
  in the middle of every gap.
* `renderPage(pageIndex)` returns a new image of exactly
  `pageImageSizes[pageIndex]`, with the page's top at the image origin. An index
  outside `0 until pageCount` raises `IllegalArgumentException`.
* `renderDocument(g)` / `renderPage(pageIndex, g)` draw into an existing
  `Graphics2D` instead; the graphics is scaled by `unitScale` internally, the
  caller only positions the origin.

## Geometry

* `pageCount` - the number of measured pages after flow continuation and growth.
* `documentImageSize: Dimension` - the size `renderDocument()` needs, already
  scaled by `unitScale` and rounded up. `0 x 0` for an empty document.
* `pageImageSizes[pageIndex]: Dimension` - the size `renderPage` needs for one
  page; `pageImageSizes.count` equals `pageCount`.

The instance is immutable and single-document; it is not thread-safe.
