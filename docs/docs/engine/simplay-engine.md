# engine - SimpLayEngine

`SimpLayEngine` (package `org.pcsoft.framework.simplay.engine`) converts a
raw [`Document`](raw-model.md) into a [`MeasuredDocument`](measured-model.md) in
one explicit call. It never touches a platform text stack: all text measuring is
delegated to a caller-supplied callback. `measure` is deterministic - the same
document and callback always produce the same result.

## Building the engine

```kotlin
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics

val engine = SimpLayEngine.builder(
    FontMeasureCalculator { font, text ->
        // delegate to AWT, Skia, a headless stub, ...
        TextMetrics(width = /* ... */, ascent = /* ... */, descent = /* ... */)
    },
).build()

val measured = engine.measure(document)
```

`SimpLayEngine.builder(measurer)` takes the mandatory `FontMeasureCalculator` and
returns a `Builder`. The strategies are fixed once `build()` is called.

A renderer that keeps its measure settings in a `RenderConfiguration` can skip
the builder and use `config.createEngine(measurer)` or the one-shot
`document.measure(measurer, config)` instead; see
[Implementation](implementation.md#measuring-from-a-renderconfiguration).

## The FontMeasureCalculator contract

`FontMeasureCalculator` is a `fun interface` with one method:

```kotlin
fun measure(font: Font, text: String): TextMetrics
```

* It returns the advance `width`, `ascent` and `descent` of `text` in `font`.
* A given `(font, text)` pair must always return the same metrics, or `measure`
  stops being deterministic.
* Font line metrics are derived by the engine from a single call over a fixed
  reference-glyph string, with `leading` forced to `0.0`. Resolved fonts are
  cached per `measure` run.

## Line breaking

### LineBreakerStrategy

`lineBreakerStrategy(...)` on the builder selects how the parts of a block become
lines. Every implementation is deterministic and platform-free and produces
positionless `UnplacedLine` / `UnplacedPart` values. The three shipped
strategies are described in detail on the
[Implementation](implementation.md#choosing-strategies) page.

| Strategy | Behaviour |
|----------|-----------|
| `GreedyWordLineBreakerStrategy` (default) | Adds parts until the next no longer fits, then starts a new line. Word- and symbol-aware: one space before every `TextWord` except the first of a line, `TextSymbol` attached with no leading space. A word wider than the content width is offered to the `WordBreakerStrategy`; if it declines, the word is kept whole and overflows. |
| `CharacterLineBreakerStrategy` | Fills lines character by character, breaking at any position, even mid-word. Never consults the `WordBreakerStrategy`. |
| `NoWrapLineBreakerStrategy` | Never breaks; all parts land in one line that may exceed the content width. |

```kotlin
val engine = SimpLayEngine.builder(measurer)
    .lineBreakerStrategy(NoWrapLineBreakerStrategy)
    .build()
```

### WordBreakerStrategy

`wordBreakerStrategy(...)` selects the intra-word (hyphenation) seam. It is asked
where an over-wide single word may split and returns ascending break offsets in
`1 until word.length`; an empty list means "do not split". Only
`GreedyWordLineBreakerStrategy` consults it. See the
[Implementation](implementation.md#choosing-strategies) page for the full
contract.

The default `NoOpWordBreakerStrategy` always returns an empty list, so an
over-long word overflows its line. No hyphenation ships with the engine.

## Alignment and line spacing

`SimpLayBlockEngine` stacks the lines of a block from the block top downward, one
`MeasuredTextStyle.resolvedLineHeight` apart, and offsets each line horizontally
by the block alignment:

| Alignment | Horizontal placement |
|-----------|----------------------|
| `LEFT` | offset `0` |
| `RIGHT` | offset by the full slack (content width minus natural line width) |
| `CENTER` | offset by half the slack |
| `JUSTIFY` | remaining width spread evenly over the word gaps, except on the block's last line (`lastLine == true`) |

`resolvedLineHeight` is `(ascent + descent) * lineSpacing.factor +
lineSpacing.extraLeading`. Each line's `baseline` is set to its `ascent`.

## Pagination

* **`FlowPage`** is filled line by line. When the next line would cross the
  content height, a new `MeasuredFlowPage` is opened with a deep copy of the page
  frame. A single raw `TextBlock` may therefore be split into several
  `MeasuredTextBlock` slices across pages; only the final line of the block
  carries `lastLine == true`. `pageIndex` runs continuously across the document.
* **`SinglePage`** is never continued. Every line is placed without clipping; the
  extra height is reported through `MeasuredSinglePage.effectiveSize` and
  `requiredContentHeight` (see [Measured model](measured-model.md)).

## Edge cases

* An empty `Document` (no pages) yields `MeasuredDocument(document, emptyList())`.
* A page with no blocks yields a `MeasuredPage` with an empty `blocks` list;
  `requiredContentHeight` is `0.0` and a `SinglePage` keeps its layout height.

## Worked example

```kotlin
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import org.pcsoft.framework.simplay.engine.measure.wordCount

// A deterministic stub: every glyph is 6 units wide, 8 up, 2 down.
val measurer = FontMeasureCalculator { _, text ->
    TextMetrics(width = text.length * 6.0, ascent = 8.0, descent = 2.0)
}

val style = TextStyle(font = Font(family = "Serif", size = 12.0))
val document = Document(
    pages = listOf(
        FlowPage(
            layout = PageLayout(Size(200.0, 120.0), Margins(10.0, 10.0, 10.0, 10.0)),
            blocks = listOf(
                TextBlock.of("The quick brown fox jumps over the lazy dog.", style),
            ),
        ),
    ),
)

val engine = SimpLayEngine.builder(measurer).build()
val measured: org.pcsoft.framework.simplay.engine.measure.MeasuredDocument =
    engine.measure(document)

val firstPage = measured.pages.first()
val lines = firstPage.blocks.first().lines          // block wrapped into several MeasuredLine
val totalWords = measured.wordCount()               // 9
```

The content width here is `200 - 10 - 10 = 180`, so at 6 units per glyph the
block wraps into multiple `MeasuredLine` objects, each with a `lineBox` inside the
page content area.

## Next

* [Rendering](rendering.md) - walking the `MeasuredDocument` to draw it.
