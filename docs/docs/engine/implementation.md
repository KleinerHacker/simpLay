# engine - Implementation

The `engine` module is the platform-independent simulation core, built with Kotlin
Multiplatform.

This page covers integration and the main entry point. Dedicated pages describe
the parts in detail:

* [Raw model](raw-model.md) - the persistable document model you build and store.
* [Measured model](measured-model.md) - the geometry-carrying result of a measure.
* [SimpLayEngine](simplay-engine.md) - the builder, strategies and pagination.
* [Rendering](rendering.md) - how to consume a measured document to draw it.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
}
```

The artifacts are published to GitHub Packages
(`https://maven.pkg.github.com/KleinerHacker/simPlay`). For a Kotlin Multiplatform
consumer add the target-specific artifact (for example `simplay-engine-jvm`) or
rely on Gradle metadata resolution.

## Entry points

### Measuring a document

`SimpLayEngine` is created through a builder. `measure(document)` is a member of
the built engine; it turns a raw `Document` (from the `...engine.model` package)
into a `MeasuredDocument` (from `...engine.measure`). Text measuring is delegated
to a `FontMeasureCalculator` that the caller supplies, so the engine stays free of
any platform text stack:

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

The result is deterministic: the same document and callback always produce the
same `MeasuredDocument`.

`FontMeasureCalculator` has one more method, `measureAdvances(font, text)`, which
returns the advance width of every character of a string. It ships with a default
that calls `measure` once per glyph, so a lambda callback needs nothing extra; a
backend that can build a platform metrics object once should override it (the `fx`
and `swing` calculators do). `FontFingerprint.of` uses it for its per-glyph
advances.

#### Measuring from a RenderConfiguration

A renderer usually keeps its measure settings in a `RenderConfiguration` (an
`open` class in `...engine` holding the `lineBreakerStrategy` and the
`wordBreakerStrategy`, both defaulting to the engine defaults). Two extensions
turn such a configuration and a platform `FontMeasureCalculator` into a result
without touching the builder by hand:

```kotlin
import org.pcsoft.framework.simplay.engine.RenderConfiguration
import org.pcsoft.framework.simplay.engine.createEngine
import org.pcsoft.framework.simplay.engine.measure

val config = RenderConfiguration().apply {
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}

val measured = document.measure(measurer, config)          // one-shot
val engine = config.createEngine(measurer)                 // reusable engine
```

`RenderConfiguration` is meant to be subclassed by a concrete renderer that adds
its own values (unit scale, page gaps, colours) while keeping the shared measure
settings in one place.

#### Document and page size

`MeasuredDocument.documentSize(gap)` and `MeasuredPage.pageSize()` (both in
`...engine`) compute the box a renderer needs: `pageSize()` is the page's
`effectiveSize`; `documentSize(gap)` stacks the pages vertically, taking the
widest page as the width and the summed page heights plus one `gap` per page
boundary as the height (never before the first or after the last page). An empty
document is `0 x 0`.

### Choosing strategies

Line breaking has two independent seams, both chosen on the builder and frozen
once `build()` is called. `lineBreakerStrategy(...)` decides where one line ends
and the next begins; `wordBreakerStrategy(...)` is consulted only when a single
word is too wide to fit on a line by itself. Every implementation must be
deterministic and must not call a platform API - the only text measurements come
from the `FontMeasureCalculator`.

```kotlin
val engine = SimpLayEngine.builder(measurer)
    .lineBreakerStrategy(CharacterLineBreakerStrategy)
    .wordBreakerStrategy(NoOpWordBreakerStrategy)
    .build()
```

#### LineBreakerStrategy

```kotlin
fun interface LineBreakerStrategy {
    fun breakIntoLines(
        parts: List<TextPart>,          // the parts of one block, may contain TextWhitespace
        font: MeasuredFont,             // the resolved block font
        maxWidth: Double,               // the content width available for a line
        measurer: FontMeasureCalculator,
        wordBreaker: WordBreakerStrategy,
    ): List<UnplacedLine>
}
```

A strategy receives the parts of one block and returns `UnplacedLine`s: lines
that are measured but carry no position. `SimpLayBlockEngine` later assigns the
positions and turns each into a `MeasuredLine`. An empty `parts` list yields an
empty result.

* `UnplacedPart` - a raw `TextPart` (or a synthetic `TextWord` when a word was
  split) with its advance `width` and the `spaceBefore` gap in front of it.
  `spaceBefore` is `0.0` for the first part of a line and for every `TextSymbol`.
* `UnplacedLine` - the `parts` of one line plus the largest `ascent` / `descent`
  among them, and `naturalWidth` (the sum of all parts and gaps, before any
  alignment adjustment).

Three implementations ship. All three insert one space before every `TextWord`
except the first part of a line and attach a `TextSymbol` with no leading space;
they differ only in *where* they end a line.

##### GreedyWordLineBreakerStrategy (default)

Fills the current line part by part. Before adding a part it computes
`currentWidth + spaceBefore + partWidth`; if that exceeds `maxWidth` and the line
already holds at least one part, the line is flushed and the part starts the next
line. Words are never split at this level - the smallest unit moved to a new line
is a whole `TextWord` or `TextSymbol`.

If a single part is wider than `maxWidth` and would sit alone on a line, the
strategy offers that word to the `WordBreakerStrategy`:

* offsets returned - the word is cut at the largest offered offset whose prefix
  still fits `maxWidth`, the head is placed as a synthetic `TextWord`, the line
  is flushed and the remainder is processed the same way (so one word can span
  several lines);
* no offsets (the default) - the word is placed whole and overflows its line;
  layout continues on the next line.

Use it for normal prose: it gives the familiar "as many words as fit" wrapping
and only ever breaks inside a word when a `WordBreakerStrategy` explicitly allows
it.

##### CharacterLineBreakerStrategy

Fills lines character by character. For each part it places the largest prefix
that still fits the remaining line width (found by binary search), flushes, and
continues with the rest of the part on the next line. It breaks at *any*
position, including inside a word, and it **never** consults the
`WordBreakerStrategy`. When a line is empty it always places at least one
character, so progress is guaranteed even in a very narrow column.

Use it for hard-wrapped, monospace-style output, for narrow columns where ragged
word wrapping would waste too much width, or wherever a guaranteed fit matters
more than word integrity. Expect words to be cut without a hyphen.

##### NoWrapLineBreakerStrategy

Never breaks. Every part of the block goes onto a single `UnplacedLine`, whose
`naturalWidth` may be far larger than `maxWidth`. The `WordBreakerStrategy` is
ignored.

Use it when the caller handles overflow itself - a single-line label, a clipping
viewport, a horizontally scrolling area, or a measuring pass that only needs the
unwrapped width. Combined with a `SinglePage` this produces one very wide line
rather than extra pages.

A custom strategy implements the `fun interface` directly; it may call `measurer`
as often as needed but must return the same lines for the same input.

#### WordBreakerStrategy

```kotlin
fun interface WordBreakerStrategy {
    fun breakOffsets(
        word: String,                   // the word to inspect, no surrounding whitespace
        font: Font,
        maxWidth: Double,               // the width available for one line
        measurer: FontMeasureCalculator,
    ): List<Int>
}
```

The intra-word (hyphenation) seam. A `LineBreakerStrategy` calls it only for a
word that does not fit on a line on its own. The return value is the list of
character offsets in `1 until word.length` at which a break is allowed, in
ascending order:

* an **empty list** means "do not split this word" - the caller keeps it whole
  (and lets it overflow);
* a **non-empty list** lets the caller pick the last offset whose prefix still
  fits `maxWidth`; offsets outside `1 until word.length`, duplicates and
  out-of-order values are filtered and sorted by the caller.

The engine inserts no hyphen glyph; if a strategy wants a visible hyphen it must
be part of the measured widths it reasons about. Only
`GreedyWordLineBreakerStrategy` consults this seam;
`CharacterLineBreakerStrategy` and `NoWrapLineBreakerStrategy` ignore it.

##### `NoOpWordBreakerStrategy` (default)

Always returns an empty list. No word is ever split, so with the default line
breaker an over-long word overflows its line. This is the only implementation
that ships; hyphenation strategies are planned as a follow-up feature.

A custom strategy might, for example, return the offsets of syllable boundaries
from a hyphenation dictionary, or every _n_-th character as a crude fallback:

```kotlin
val everyFifth = WordBreakerStrategy { word, _, _, _ ->
    (5 until word.length step 5).toList()
}

val engine = SimpLayEngine.builder(measurer)
    .wordBreakerStrategy(everyFifth)
    .build()
```

### Pagination behaviour

* A `FlowPage` whose content exceeds the content height is continued on further
  `MeasuredFlowPage`s; every continuation page carries a deep copy of the page
  frame and page indices run continuously across the document.
* A `SinglePage` never continues onto another page. When its content is taller
  than the page, `MeasuredSinglePage.effectiveSize` reports the grown height.

## Font fingerprint and availability

A persisted `Document` only stores font *families* by name. When it is reopened on
a machine where a family is not installed, the platform text stack silently
substitutes another font and the layout drifts without any signal. Two mechanisms
detect this.

### Fingerprint (font changed or missing)

`FontFingerprint` is a size-independent signature of a resolved font face: the
per-glyph advance widths of a fixed reference string plus the ascent and descent,
all measured at a normalization size. It is taken through a `FontMeasureCalculator`:

```kotlin
import org.pcsoft.framework.simplay.engine.model.FontFingerprint
import org.pcsoft.framework.simplay.engine.withFontFingerprints

// authoring: stamp every block font before persisting
val toPersist = document.withFontFingerprints(measurer)   // Document -> Document
val json = Json.encodeToString(toPersist)

// reopening, elsewhere: the measure pass reports deviations
val measured = restored.measure(measurer)
if (measured.fingerprintDeviations.isNotEmpty()) {
    // at least one font is missing or was silently replaced
}
```

`withFontFingerprints(measurer, overwrite = true)` returns a copy of the document
whose every block font carries a fresh `Font.fingerprint`; identical fonts are
measured once. During `measure`, each fingerprinted font is re-checked and the
result is exposed as `MeasuredFont.fingerprintStatus` (`NOT_CHECKED` / `MATCH` /
`DEVIATION`), aggregated as `MeasuredDocument.fingerprintDeviations`.

A single fingerprint can also be taken and compared by hand with
`FontFingerprint.of(measurer, font)` and `FontFingerprint.matches(other, tolerance)`;
`encode()` / `FontFingerprint.decode(text)` store it as one line of text. A
fingerprint is only meaningful against the same text stack it was taken with - AWT
and JavaFX measure the same font differently.

### Availability (family not installed)

The `engine` module has no font registry of its own. The UI modules add a probe
that classifies a family against the concrete text stack -
`org.pcsoft.framework.simplay.swing.SwingFontProbe` and
`org.pcsoft.framework.simplay.fx.FxFontProbe`, each with `isFamilyAvailable`,
`checkAvailability`, `fingerprint`, `verify` and `stamp`. See the
[Swing](../swing/font-probe.md) and [JavaFX](../fx/font-probe.md) font-probe pages.
