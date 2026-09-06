# Feature Plan: Layout Engine Foundation

## 1. Objective

Build the foundational layout system inside the `engine` module. A persistable
raw document object model (document -> pages -> text blocks -> text parts, with a
per-block style) is paired with a non-persistable measured decorator model, and
an engine converts the former into the latter. The engine measures text through
a caller-supplied font-measuring callback and performs greedy line breaking plus
pagination. The result of the explicit engine call is a fully measured document
that carries lines and geometry and provides enough pages for the content,
depending on the text style of each text block and the page layout. The feature
also brings the MkDocs documentation in line with the new engine API.

## 2. Current State

* `engine` is a Kotlin Multiplatform module; the `commonMain` and `commonTest`
  source sets contain only `.gitkeep` files.
* No object model, no engine and no layout logic exist yet.
* Base package is `org.pcsoft.framework.playsim.engine`.
* MkDocs has one page per module (`docs/docs/engine/implementation.md`) wired
  into `docs/mkdocs.yml`; `buildDocs` runs with `--strict`.
* The other modules (`fx`, `swing`, `j-pdf`, `j-print`) are out of scope and are
  not touched by this feature.

## 3. Target State

* Persistable raw data model in `commonMain`, kept as plain data holders
  (POJO-style `data class` / `sealed` hierarchies, no behaviour, no callbacks,
  no platform types) so the consumer can choose the storage mechanism (JSON,
  YAML, XML, JVM serialization, or another):
  * `Document` holds an ordered list of `Page` objects and may be empty.
  * `Page` is an interface / sealed type holding a list of `TextBlock` and a
    `PageLayout` (width, height, margins). It is realised as `FlowPage` (further
    required pages are computed) and `SinglePage` (the layout height is only a
    minimum; the page grows when more text arrives).
  * `PageLayout` holds width, height and margins as `Double` values.
  * `TextBlock` holds a list of `TextPart` and a reference to a `TextStyle`;
    `TextBlock.of(text)` parses a string into words and symbols and `toString()`
    reassembles the original text.
  * `TextPart` is an interface / sealed type realised as `TextWord` and
    `TextSymbol`.
  * `TextStyle` holds line spacing, alignment and a `Font`.
  * The types carry `kotlinx.serialization` `@Serializable` annotations only;
    annotations do not add a base class or change the shape, so other mechanisms
    stay usable.
  * Counting of words, symbols and characters is provided through extension
    functions, keeping the data holders clean.
* Non-persistable measured decorator model in `commonMain`, explicitly not meant
  for storage, built with Kotlin `by` delegation: `MeasuredDocument`,
  `MeasuredPage`, `MeasuredTextBlock`, `MeasuredTextStyle` and `MeasuredFont`,
  `MeasuredTextPart`, plus a new intermediate `MeasuredLine` level that does not
  exist in the raw model.
* `SimPLayEngine`, created through a builder that receives a font-measuring
  callback; one explicit call converts a raw `Document` into a
  `MeasuredDocument` using greedy word / symbol line breaking with an empty
  `WordBreaker` hook, line spacing and alignment applied, page filling,
  `FlowPage` page supply by cloning the last flow page's `PageLayout`, and
  `SinglePage` vertical growth.
* MkDocs carries four dedicated engine pages: the raw data model, the measured
  data model, the SimPLayEngine (detailed function and usage) and a rendering
  guide, all wired into `mkdocs.yml` and passing `buildDocs --strict`.

## 4. Requirements

### Functional Requirements

* Construct a `Document` with zero or more pages.
* The raw model is persistable: it can be serialised and restored without loss.
* The raw model stays POJO-style so the consumer, not the engine, picks the
  storage format.
* `TextBlock.of(text)` splits text into `TextWord` and `TextSymbol`; `toString()`
  round-trips the original text.
* Extension functions count words, symbols and characters on `TextBlock` and
  `Document`.
* `TextStyle` carries line spacing, alignment and font.
* The measured decorator model mirrors the raw model via `by` delegation and
  adds geometry plus the `MeasuredLine` level; it is not persisted.
* A builder produces a `SimPLayEngine` from a font-measuring callback.
* One engine call converts a raw `Document` into a `MeasuredDocument`.
* Text in a `TextBlock` wraps automatically to the page content width according
  to its `TextStyle`.
* Enough pages are produced for the content: `FlowPage` computes additional
  pages, `SinglePage` extends its height.
* An empty document yields an empty `MeasuredDocument`.
* The documentation explains the raw model, the measured model, the engine and
  how to render the measured result.

### Technical Requirements

* Kotlin only; `commonMain` and `commonTest` of the `engine` module only, plus a
  `jvmTest` source set of the `engine` module if JVM serialization has to be
  exercised. No platform main source sets, no other module. This is a hard
  boundary. Documentation under `docs/` is the only non-code change.
* `kotlinx.serialization` (Kotlin serialization plugin plus
  `kotlinx-serialization-json`, Apache-2.0) is added to the `engine` module and
  its licence allow-list; it is the only new main dependency of this feature.
* Additional serialisation-format libraries needed only by IP-04 (YAML, XML) are
  test-scoped and require the user's approval before IP-04 adds them; each new
  licence is added to the allow-list.
* The raw model carries `@Serializable` annotations only and otherwise stays
  plain `data class` / `sealed` types with no functions and no platform types,
  so YAML, XML or JVM serialization remain possible for the consumer.
* All geometry values are `Double` and unit-agnostic; conversion into fixed
  units is left to later concrete implementations.
* The measured model uses `by` delegation; raw objects stay pure data holders.
* Line breaking is greedy; a `WordBreaker` seam is present but not implemented
  (no hyphenation).
* A build with the Gradle `build` target is run after every change; tests follow
  the `testing` skill.
* Documentation work follows the `project-docs` skill; `buildDocs` (`--strict`)
  must pass.
* No further third-party dependency; the user is asked first if one becomes
  necessary.

## 5. Architecture

* `...engine.model` - the persistable POJO raw model and its unit-agnostic value
  types (`Size`, `Rect`, `Margins`, `FontMetrics`, `TextMetrics`), the
  `TextBlock` parser and the counting extension functions. `@Serializable` is
  the only serialisation coupling; no format instance is owned here.
* `...engine.measure` - the non-persistable measured decorator model: the
  `MeasuredXxx` types delegating to their raw type via `by`, and `MeasuredLine`.
* `...engine.engine` - `SimPLayEngine`, its builder, the font-measuring callback
  type, the greedy line breaker, the `WordBreaker` seam and the pagination /
  page-supply logic.
* Data flow: the caller builds and optionally persists a raw `Document` with a
  format of its choice, then calls `SimPLayEngine.measure(document)`; the engine
  measures each `TextBlock` through the callback, groups parts into
  `MeasuredLine` objects, fills pages, supplies pages for a `FlowPage` and grows
  a `SinglePage`, and returns a transient `MeasuredDocument` that delegates to
  the raw document. A renderer then walks the `MeasuredDocument` geometry.
* Documentation: new pages under `docs/docs/engine/`, referenced from the
  `engine` section of `docs/mkdocs.yml`; KDoc on the new public API.
* No persistence implementation and no configuration in this feature; the raw
  model is only made persistable in shape and verified through IP-04.

## 6. Implementation Plan Overview

| ID    | Implementation Plan                    | Objective                                                                                                    | Dependencies |
| ----- | ------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------ |
| IP-01 | Persistable Raw Object Model           | Provide the full POJO raw data model, value types, `kotlinx.serialization` wiring, the `TextBlock` parser and counting extensions. | -            |
| IP-02 | Measured Decorator Model               | Provide the full non-persistable `by`-delegating measured model including the `MeasuredLine` level.          | IP-01        |
| IP-03 | Layout Engine                          | Provide `SimPLayEngine`, its builder, the font-measuring callback and the line-breaking / pagination logic.  | IP-01, IP-02 |
| IP-04 | End-to-End Layout & Persistence Tests  | Provide full end-to-end tests over mixed pages / styles plus save / load round-trips in JSON, YAML, XML and JVM serialization. | IP-03        |
| IP-05 | Documentation Alignment                | Provide dedicated MkDocs pages for the raw model, the measured model, the SimPLayEngine and rendering, plus KDoc. | IP-03, IP-04 |

## 7. Implementation Plans

### IP-01: Persistable Raw Object Model

**Objective**

Implement the complete raw data model as plain, persistable POJO data holders:
`Document`, `Page` with `FlowPage` and `SinglePage`, `PageLayout`, `TextBlock`
with `of(text)` / `toString()`, `TextPart` with `TextWord` and `TextSymbol`,
`TextStyle` with `Font`, line spacing and alignment, the unit-agnostic value
types, the `kotlinx.serialization` wiring and the counting extension functions.

**Scope**

* In: all raw model types and enums, geometry value types (`Size`, `Rect`,
  `Margins`), `FontMetrics` / `TextMetrics` value types.
* In: adding the Kotlin serialization plugin and `kotlinx-serialization-json` to
  the `engine` build and its licence allow-list; `@Serializable` on the model
  types; a `sealed` hierarchy for `Page` and `TextPart` so polymorphism
  serialises with a type discriminator.
* In: `TextBlock.of(text)` tokenizer, `TextBlock.toString()` round-trip and the
  whitespace reconstruction rule.
* In: extension functions counting words, symbols and characters.
* Out: any measuring, decorator or engine logic.
* Out: owning a configured format instance or a persistence API.
* Out: YAML / XML / JVM-serialization wiring (verified in IP-04).
* Out: platform-specific font handling.

**Dependencies**

* Independent.

**Interfaces to Other Plans**

* Provides the raw types decorated by IP-02 and consumed by IP-03, IP-04 and
  documented by IP-05.
* Provides `FontMetrics` / `TextMetrics` as the result shape of the IP-03
  callback and the input of the IP-03 algorithm.

### IP-02: Measured Decorator Model

**Objective**

Implement the complete non-persistable measured model with Kotlin `by`
delegation (`MeasuredDocument`, `MeasuredPage`, `MeasuredTextBlock`,
`MeasuredTextStyle`, `MeasuredFont`, `MeasuredTextPart`) and the intermediate
`MeasuredLine` level that has no raw counterpart. No engine logic.

**Scope**

* In: all `MeasuredXxx` types delegating to their raw type via `by`, plus the
  geometry they add (position, width, height, line box, part rectangles).
* In: `MeasuredLine` holding its parts, its line box and its baseline / spacing
  metrics.
* In: `MeasuredFont` / `MeasuredTextStyle` adding the resolved font metrics.
* In: constructors / factories that let a test build a measured model directly.
* Out: `SimPLayEngine` and any code that populates the model from a raw one.
* Out: any persistence or serialisation of these types.

**Dependencies**

* IP-01 (the raw types being delegated to and the geometry value types).

**Interfaces to Other Plans**

* Provides the measured model that IP-03 populates and returns, that IP-04
  asserts against and that IP-05 documents.

### IP-03: Layout Engine

**Objective**

Implement `SimPLayEngine` and a builder that receives a font-measuring callback,
fix the callback contract, and implement greedy word / symbol line breaking with
the empty `WordBreaker` hook, line spacing and alignment, page filling,
`FlowPage` page supply by cloning the last flow page's `PageLayout`, `SinglePage`
vertical growth and empty-document handling. The engine converts a raw
`Document` into a `MeasuredDocument`.

**Scope**

* In: `SimPLayEngine`, its builder, the font-measuring callback type and input
  validation.
* In: the greedy line filler over `TextPart`, the `WordBreaker` interface with a
  no-op default, alignment handling (including the last-line seam).
* In: page filling, `FlowPage` page supply, `SinglePage` growth,
  empty-document handling, assembly of the `MeasuredDocument`.
* Out: hyphenation logic (seam only).
* Out: any change to the raw or measured model shapes.

**Dependencies**

* IP-01 (raw model, value types).
* IP-02 (measured model, `MeasuredLine`).

**Interfaces to Other Plans**

* Consumes the IP-01 raw model and the IP-02 measured model.
* Provides the `SimPLayEngine.measure` entry point that IP-04 exercises and
  IP-05 documents.

### IP-04: End-to-End Layout & Persistence Tests

**Objective**

Provide one or more complete end-to-end tests that build a document mixing
`FlowPage` and `SinglePage` with different page layouts, multiple text blocks
and different text styles, run it through `SimPLayEngine` with a deterministic
font-measuring callback and assert the resulting `MeasuredDocument`; and verify
that the raw document survives a save / load round-trip in JSON, YAML, XML and
JVM serialization.

**Scope**

* In: a deterministic test font-measuring callback.
* In: full-document scenarios mixing page types, layouts, blocks and styles.
* In: assertions on wrapping into `MeasuredLine`, `FlowPage` page supply,
  `SinglePage` growth, alignment and line spacing, and the empty document.
* In: raw-document save / load round-trip tests for JSON, YAML and XML in
  `commonTest`, and for JVM serialization in a `jvmTest` source set.
* In: adding the test-scoped YAML and XML format libraries (and their licences)
  after the user approves them.
* Out: unit tests owned by IP-01 to IP-03 (those ship with their own plans).
* Out: production code changes; a round-trip failure is escalated and fixed in
  the owning plan (usually IP-01) as a follow-up decision with the user.

**Dependencies**

* IP-03 (the working engine).

**Interfaces to Other Plans**

* Consumes IP-01 to IP-03; feeds any model-shape corrections back into IP-01 if
  a format round-trip fails, and hands the confirmed format matrix to IP-05.

### IP-05: Documentation Alignment

**Objective**

Bring the MkDocs documentation in line with the new engine API: four dedicated
engine pages plus KDoc on the new public types, all passing `buildDocs`
(`--strict`).

**Scope**

* In: a raw data model page (`Document`, `Page` / `FlowPage` / `SinglePage`,
  `PageLayout`, `TextBlock`, `TextPart` / `TextWord` / `TextSymbol`, `TextStyle`,
  counting extensions, the confirmed persistence-format matrix from IP-04).
* In: a measured data model page (`by` delegation, `MeasuredLine`, geometry,
  why it is not persisted).
* In: a detailed SimPLayEngine page (builder, font-measuring callback contract,
  greedy line breaking, `WordBreaker` seam, `FlowPage` supply, `SinglePage`
  growth, empty document, worked example).
* In: a rendering page explaining how to consume a `MeasuredDocument` to render
  the content (walking pages, lines and part rectangles; mapping `Double` units;
  where platform modules plug in).
* In: wiring the four pages into the `engine` section of `docs/mkdocs.yml` and
  adjusting the existing `engine/implementation.md` cross-links.
* In: KDoc on every new public type / function per the `project-docs` skill;
  CHANGELOG entry.
* Out: any production code change.
* Out: documentation for other modules.

**Dependencies**

* IP-03 (the API being documented).
* IP-04 (the confirmed persistence-format outcomes referenced on the raw model
  page).

**Interfaces to Other Plans**

* Consumes the final API and test outcomes of IP-01 to IP-04; it is the terminal
  plan of the feature.

## 8. Dependency Graph

```text
IP-01
└── IP-02
    └── IP-03
        └── IP-04
            └── IP-05
```

## 9. Risks and Open Questions

* Serialisation coupling: the raw model is annotated with `@Serializable` only
  and stays POJO-style; the consumer supplies the format.
* JSON is native to `kotlinx.serialization`. YAML and XML need extra libraries
  (e.g. a `kotlinx.serialization` YAML and XML format); they are test-scoped in
  IP-04 and need the user's approval and a licence-allow-list entry.
* JVM serialization is JVM-only: it cannot run in `commonMain` / `commonTest`.
  IP-04 needs a `jvmTest` source set, and every raw type must be
  `java.io.Serializable` on the JVM - which a plain common `data class` is not by
  default. Making the common model implement `Serializable` from `commonMain` is
  not directly possible; options (a JVM-only `actual` marker, `@JvmRecord`, or
  documenting JVM serialization as consumer responsibility) must be decided with
  the user if the round-trip fails.
* XML round-trip of the `sealed` `Page` / `TextPart` polymorphism may need a
  format-specific discriminator configuration; behaviour to be checked in IP-04.
* Persisting the `Page` / `TextPart` polymorphism relies on a `sealed`
  hierarchy plus a type discriminator; the discriminator naming must be pinned
  in IP-01.
* Tokenizer rules: what is a symbol versus part of a word (digits, hyphen inside
  a word, quotes, whitespace runs, line breaks); to be pinned in IP-01.
* `toString()` round-trip fidelity: exact whitespace reconstruction between
  parts must be defined (single space versus preserved original spacing).
* Font-measuring callback contract: exact input (font plus string or single
  part) and output (width, ascent, descent, line height) must be fixed in IP-03.
* Line spacing semantics: multiplier versus absolute value, leading above or
  below the line.
* Alignment of the last line of a block, especially for justified text -
  deferred, but the seam must allow it.
* `FlowPage` page supply: whether the cloned `PageLayout` is deep-copied and
  whether the supplied pages are `FlowPage` as well (assumed yes).
* `SinglePage` growth: whether the width stays fixed (assumed yes) and how
  margins apply to the grown region.
* Whether `MeasuredLine` exposes per-part positions or only the line box
  (assumed both).
* Justified text without hyphenation can leave large gaps; acceptable for the
  foundation.
* The rendering page describes a consumption pattern for a renderer that does
  not exist yet in this feature; it must stay conceptual and not imply a
  platform module.
* Cross-target `toString()` / `equals` conventions for the data holders in a
  Kotlin Multiplatform module.

## 10. Feature Completion Criteria

* The `engine` module builds via `./gradlew build` with the new code and every
  added dependency passes the licence check.
* The raw model can represent a document containing a `FlowPage` and a
  `SinglePage`; an empty document is allowed.
* A raw document survives save / load round-trips in JSON, YAML, XML and JVM
  serialization unchanged, or any failing format is written up as a resolved
  decision with the user.
* The model types stay POJO-style (no engine or format code in `...model`).
* `TextBlock.of(text).toString()` reproduces the input according to the fixed
  whitespace rule.
* The counting extensions return correct word, symbol and character totals.
* The measured decorator model mirrors the raw model via `by` delegation and can
  be built directly in a test.
* `SimPLayEngine` is built from a font-measuring callback and converts a
  document in one explicit call.
* A block whose text exceeds the content width is wrapped into multiple
  `MeasuredLine` objects.
* A `FlowPage` document whose content exceeds one page produces additional pages
  automatically.
* A `SinglePage` whose content exceeds its layout height produces a taller
  measured page.
* The `WordBreaker` seam exists with a no-op default and no hyphenation occurs.
* One or more end-to-end tests over a mixed document (page types, layouts,
  blocks, styles) pass.
* MkDocs has the four engine pages (raw model, measured model, SimPLayEngine,
  rendering), they are in the `mkdocs.yml` nav, KDoc covers the new public API
  and `buildDocs` (`--strict`) passes.
