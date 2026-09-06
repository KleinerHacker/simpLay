# engine - Raw document model

The raw document model is the input of the layout engine. It lives in the package
`org.pcsoft.framework.simplay.engine.model` and is made of plain, immutable data
holders: no behaviour, no callbacks, no platform types. You build a `Document`,
optionally persist it, and hand it to `SimpLayEngine.measure(document)`.

## Document and pages

| Type | Purpose |
|------|---------|
| `Document` | Ordered list of `Page` objects. May be empty. |
| `Page` | Sealed type; holds a `PageLayout` and a list of `TextBlock`. |
| `FlowPage` | A page whose content flows onto additional pages when it does not fit. |
| `SinglePage` | A page that is never continued; it grows in height instead. |
| `PageLayout` | Physical page frame: outer `Size` plus inner `Margins`. |

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
one `TextStyle`. `TextPart` is a sealed type with two realisations:

* `TextWord` - a maximal run of letters and/or digits.
* `TextSymbol` - a single non-letter, non-digit, non-whitespace character. Its
  public constructor takes a `Char`; the character is also available through
  `symbol`.

Whitespace is never stored as a part.

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

### `toString()` normalisation

`TextBlock.toString()` rejoins the parts with a fixed whitespace rule:

* one space before every `TextWord` except the first part of the block;
* no space before a `TextSymbol`.

So `TextBlock.of("Hello,   world !", style).toString()` yields `"Hello, world !"`.
The rule normalises whitespace runs; it does not preserve the original spacing.

## Style and font

| Type | Fields |
|------|--------|
| `TextStyle` | `font: Font`, `lineSpacing: LineSpacing = LineSpacing()`, `alignment: TextAlignment = LEFT` |
| `Font` | `family: String`, `size: Double`, `weight: FontWeight = NORMAL`, `style: FontStyle = NORMAL` |
| `LineSpacing` | `factor: Double = 1.0` (multiplicative), `extraLeading: Double = 0.0` (additive) |

Enums: `TextAlignment` (`LEFT`, `RIGHT`, `CENTER`, `JUSTIFY`), `FontWeight`
(`NORMAL`, `BOLD`), `FontStyle` (`NORMAL`, `ITALIC`).

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
