# engine - Measured model

`SimpLayEngine.measure(document)` returns a `MeasuredDocument`: the raw model with
resolved font metrics, broken lines and absolute geometry attached. The measured
types live in the package `org.pcsoft.framework.simplay.engine.measure` (the raw
types stay in `org.pcsoft.framework.simplay.engine.model`).

## Decoration pattern

The measured model does not use Kotlin `by` delegation. Every measured type holds
a public `raw` handle and follows one rule:

* an **unchanged** property is a hand-written forwarder (`val family get() = raw.family`);
* a property that **becomes** a measured type is exposed once, as that measured
  type, with the original reachable only through `raw`;
* nothing is stored twice; derived geometry is computed on access.

| Measured type | `raw` | Adds |
|---------------|-------|------|
| `MeasuredDocument` | `Document` | `pages: List<MeasuredPage>` |
| `MeasuredPage` (`MeasuredFlowPage` / `MeasuredSinglePage`) | `Page` | `pageIndex`, `blocks`, `contentArea`, `requiredContentHeight`, `effectiveSize` |
| `MeasuredTextBlock` | `TextBlock` | `lines: List<MeasuredLine>`, `bounds: Rect`, `style: MeasuredTextStyle` |
| `MeasuredTextStyle` | `TextStyle` | `font: MeasuredFont`, `resolvedLineHeight` |
| `MeasuredFont` | `Font` | `metrics: FontMetrics` |
| `MeasuredTextPart` | `TextPart` | `bounds: Rect` |

`MeasuredPage` / `MeasuredTextPart` are **not** subtypes of `Page` / `TextPart`.

## Geometry

All coordinates are unit-less `Double`s. Part and line boxes are relative to the
page content area; the content area is relative to the page origin.

| Value | On | Meaning |
|-------|----|---------|
| `contentArea: Rect` | `MeasuredPage` | Layout box shrunk by the margins; pure function of `layout`. |
| `requiredContentHeight: Double` | `MeasuredPage` | Bottom edge of the lowest measured block in content-area coordinates; `0.0` when the page has no blocks. |
| `effectiveSize: Size` | `MeasuredPage` | Size the page occupies after layout. `MeasuredFlowPage` always reports the raw layout size; `MeasuredSinglePage` keeps the width but grows the height to `margins.top + requiredContentHeight + margins.bottom` (never below the layout height). |
| `resolvedLineHeight: Double` | `MeasuredTextStyle` | Line advance: `(metrics.ascent + metrics.descent) * lineSpacing.factor + lineSpacing.extraLeading`. |
| `pageIndex: Int` | `MeasuredPage` | Zero-based position of the page, continuous across the whole document (a continued `FlowPage` keeps counting). |

`FontMetrics` carries `ascent`, `descent`, `leading` and the derived
`lineHeight`. `TextMetrics` (the result of the measuring callback) carries
`width`, `ascent`, `descent`.

## MeasuredLine

`MeasuredLine` is an intermediate level with no raw counterpart. One block breaks
into one or more lines.

| Field | Meaning |
|-------|---------|
| `parts: List<MeasuredTextPart>` | Measured parts in reading order. |
| `lineBox: Rect` | Position and extent relative to the page content area. |
| `baseline: Double` | Distance from the top of `lineBox` to the text baseline (equals `ascent`). |
| `ascent` / `descent` | Values used for this line. |
| `alignment: TextAlignment` | Alignment applied to this line. |
| `lastLine: Boolean` | `true` for the final line of its block; relevant for `JUSTIFY`. |

## Counting extensions

`wordCount()`, `symbolCount()` and `charCount()` are also defined for
`MeasuredTextBlock`, `MeasuredPage` and `MeasuredDocument`; they delegate to the
raw counts. On the JVM they are static methods of `MeasuredCountingUtil`.

## Why it is not persisted

The measured model is a transient layout result: it holds absolute geometry that
depends on the page layout, the text style and the font-measuring callback of one
specific `measure` call. Persisting a document means persisting the raw model (see
[Raw model](raw-model.md)) and measuring again. None of the `Measured*` types are
`@Serializable` or implement `PlatformSerializable`.

## Next

* [SimpLayEngine](simplay-engine.md) - how the measured model is produced.
* [Rendering](rendering.md) - how to consume it.
