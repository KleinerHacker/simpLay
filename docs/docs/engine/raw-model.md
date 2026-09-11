# engine - Raw document model

The raw document model is the input of the layout engine. It lives in the package
`org.pcsoft.framework.simplay.engine.model` and is made of plain, immutable data
holders: no behaviour, no callbacks, no platform types. You build a `Document`,
optionally persist it, and hand it to `SimpLayEngine.measure(document)`.

## Document and pages

| Type | Purpose |
|------|---------|
| `Document` | Ordered list of `Page` objects, plus `numbering: PageNumbering`. May be empty. |
| `Page` | Sealed type; holds a stable `id: String`, a `PageLayout` and a list of `TextBlock`. |
| `FlowPage` | A page whose content flows onto additional pages when it does not fit. |
| `SinglePage` | A page that is never continued; it grows in height instead. |
| `PageLayout` | Physical page frame: outer `Size` plus inner `Margins`. |

### Page identity

Every `Page` carries an `id: String`, a random UUID (`kotlin.uuid.Uuid`) filled in
by a field default when the page is constructed. It identifies a page
independently of its position in `Document.pages`, so it survives edits that
reorder or splice pages and flow overflow that turns one model page into several
sheets - every overflow sheet of a `FlowPage` shares the same `id`. A document
persisted before this field existed gets a fresh `id` per page on load, through
the same default.

`PageLayout` exposes the derived values `contentWidth` (`size.width - margins.left
- margins.right`) and `contentHeight` (the vertical counterpart). All geometry
values are unit-less `Double`s.

```kotlin
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.PageLayout

val layout = PageLayout(
    size = Size(width = 595.0, height = 842.0),
    margins = Margins(left = 40.0, top = 40.0, right = 40.0, bottom = 40.0),
)
val page = FlowPage(layout = layout, blocks = emptyList())
```

## Text blocks and parts

A `TextBlock` is a run of styled text. It holds an ordered list of `TextPart` and
one `TextStyle`. `TextPart` is a sealed type with three realisations:

* `TextWord` - a maximal run of letters and/or digits.
* `TextSymbol` - a single non-letter, non-digit, non-whitespace character. Its
  public constructor takes a `Char`; the character is also available through
  `symbol`.
* `TextWhitespace` - a maximal run of whitespace of one `WhitespaceKind`
  (`SPACE`, `TAB`, `LINE_BREAK`). It stores only that kind and the run length
  `count`; its `text` is derived from both. A run never mixes kinds, so a space
  followed by a tab yields two parts.

A `TextWhitespace` is not addressable on its own: it produces no glyph in the
layout and no caret-selectable segment, but it keeps the original spacing.

### Building a block

The primary constructor of `TextBlock` is private. Build a block with the factory
`TextBlock.of(text, style)`, which tokenises `text`:

```kotlin
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

val style = TextStyle(font = Font(family = "Serif", size = 12.0))
val block = TextBlock.of("Hello, world!", style)
// parts: TextWord("Hello"), TextSymbol(','), TextWord("world"), TextSymbol('!')
```

### `toString()` round trip

`TextBlock.toString()` rejoins the parts by concatenating their `text` in order.
Because every whitespace run is kept as its own `TextWhitespace` part, the round
trip is lossless - no character is invented or dropped:

```kotlin
TextBlock.of("Hello,   world !", style).toString() // "Hello,   world !"
TextBlock.of("paragraph.X", style).toString()      // "paragraph.X"
```

## Style and font

| Type | Fields |
|------|--------|
| `TextStyle` | `font: Font`, `lineSpacing: LineSpacing = LineSpacing()`, `alignment: TextAlignment = LEFT` |
| `Font` | `family: String`, `size: Double`, `weight: FontWeight = NORMAL`, `style: FontStyle = NORMAL`, `fingerprint: FontFingerprint? = null` |
| `LineSpacing` | `factor: Double = 1.0` (multiplicative), `extraLeading: Double = 0.0` (additive) |

Enums: `TextAlignment` (`LEFT`, `RIGHT`, `CENTER`, `JUSTIFY`), `FontWeight`
(`NORMAL`, `BOLD`), `FontStyle` (`NORMAL`, `ITALIC`).

### Font fingerprint

`Font.fingerprint` is an optional, size-independent signature of the font face the
family resolved to when the document was authored. It is a pure passenger of the
model: only the measure pass reads it, to report via `MeasuredFont.fingerprintStatus`
that a font is missing or was silently replaced when the document is reopened on
another machine. A `null` fingerprint means "not verified".

A fingerprint is taken through a `FontMeasureCalculator`, so it is filled in from a
context that has a platform text stack - either `FontFingerprint.of(measurer, font)`
for a single font, `Document.withFontFingerprints(measurer)` for a whole document,
or a `*FontProbe` in a UI module. See
[Font fingerprint & availability](implementation.md#font-fingerprint-and-availability).

`FontFingerprint` (`normalizedSize`, `ascent`, `descent`, `advances: List<Double>`)
is `@Serializable` and `PlatformSerializable`, so it round-trips with the rest of
the model; `encode()` / `FontFingerprint.decode(text)` give a one-line text form for
storing it outside the model.

## Page numbering

`Document.numbering: PageNumbering` (default `PageNumbering.OFF`) configures the
page numbers a renderer draws:

| Field | Purpose |
|-------|---------|
| `position: PageNumberPosition` | Where the number sits, or `OFF` to draw nothing. |
| `startNumber: Int` | The display value of the first counted sheet (default `1`). |
| `excludedPageIds: Set<String>` | `Page.id`s that show no number, referenced by id, never by index. |
| `counting: PageCountingMode` | How an excluded sheet affects the running counter; also the executable strategy - see below. |
| `textStyle: TextStyle` | Font of the number; defaults to a small serif. |

`PageNumberPosition` has eleven values: `OFF`, and every combination of `TOP_` /
`BOTTOM_` with `LEFT`, `CENTER`, `RIGHT`, `INNER` and `OUTER`. `INNER` / `OUTER`
alternate their horizontal side by sheet parity: an odd sheet is treated as a
right-hand page (inner = left, outer = right), an even sheet as a left-hand page
(inner = right, outer = left).

`PageCountingMode` (package `org.pcsoft.framework.simplay.engine`) selects how
`excludedPageIds` interact with the running counter, and is itself the
`PageCountingStrategy` that carries the behaviour out - a persisted setting and
its executable strategy are the same enum value, with no separate lookup step:

* `CONTINUOUS` (default) - every sheet advances the counter, including an
  excluded one; only its own label is suppressed.
* `SKIP_EXCLUDED` - an excluded sheet shows no label and does not advance the
  counter, so the following sheet gets the number the excluded one would have.

The `planPageNumbers` extension function that lays out the label per sheet from a
`PageNumbering` is engine, not model, logic; see
[SimpLayEngine](simplay-engine.md#page-numbering).

## Counting extensions

Extension functions in the same package keep the data holders clean. Each is
defined for `TextBlock`, `Page` and `Document` and sums over the contained
elements:

* `wordCount()` - number of `TextWord` parts.
* `symbolCount()` - number of `TextSymbol` parts.
* `charCount()` - total characters across all parts.

```kotlin
import org.pcsoft.framework.simplay.engine.model.wordCount

val words = document.wordCount()
```

On the JVM these are static methods of `CountingUtil`.

## Persistence

Every raw type carries `kotlinx.serialization` `@Serializable` and implements the
marker `PlatformSerializable` (`org.pcsoft.framework.simplay.engine`). The marker
maps to `java.io.Serializable` on the JVM and to an empty interface elsewhere, so
the model can also be written with JVM serialization without the common code
depending on a platform type. The sealed `Page` and `TextPart` hierarchies
serialise with a type discriminator (`flow` / `single`, `word` / `symbol`).

The engine owns no format instance - the consumer picks the mechanism. The
following formats are verified for a lossless raw-document round-trip:

| Format | Library | Result |
|--------|---------|--------|
| JSON | `kotlinx-serialization-json` | lossless round-trip |
| YAML | `kaml` | lossless round-trip |
| XML | `xmlutil` (`autoPolymorphic = true`) | lossless round-trip, `Page` / `TextPart` polymorphism kept |
| JVM serialization | `ObjectOutputStream` / `ObjectInputStream` | lossless round-trip via the `PlatformSerializable` marker |

The measured model (see [Measured model](measured-model.md)) is **not**
persistable; only the raw model round-trips.

### Example: build and save as JSON

```kotlin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle

val style = TextStyle(font = Font(family = "Serif", size = 12.0))
val document = Document(
    pages = listOf(
        FlowPage(
            layout = PageLayout(Size(595.0, 842.0), Margins(40.0, 40.0, 40.0, 40.0)),
            blocks = listOf(TextBlock.of("The quick brown fox.", style)),
        ),
    ),
)

val json: String = Json.encodeToString(document)
val restored: Document = Json.decodeFromString(json)
```

## Next

* [Measured model](measured-model.md) - what the engine produces.
* [SimpLayEngine](simplay-engine.md) - turning a raw document into a measured one.
