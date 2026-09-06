# FP-002 / IP-03: Explicit Break Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status file: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## 1. Objective

Introduce an explicit line-break token in the raw model (`TextBreak`, a `TextPart`) that the
tokenizer produces for `\n`, and add `ExplicitBreakLineBreakerStrategy` that breaks lines only at
those tokens and never on width. Opt-in via `SimpLayEngine.Builder`; default unchanged. The new
token must survive all existing serialization round-trips.

## 2. Scope

### In scope

* New sealed subtype `TextBreak` of `TextPart` in `engine.model`, with `@SerialName("break")`.
* Tokenizer change: a newline (`\n`, and `\r\n` collapsed to one) emits one `TextBreak`; other
  whitespace behaviour unchanged.
* `kotlinx.serialization` wiring for JSON / YAML / XML plus JVM (Java) serialization.
* Counting (`wordCount`, `symbolCount`, `charCount`) and `TextBlock.toString()` handling.
* Measured-model pass-through: `TextBreak` is not measured, produces no `MeasuredTextPart`.
* `ExplicitBreakLineBreakerStrategy` in `engine.engine`, builder-selectable.
* Tests: tokenizer, counting, `toString`, JSON/YAML/XML/JVM round-trips, strategy behaviour,
  builder swap.

### Out of scope

* Width-based wrapping inside `ExplicitBreakLineBreakerStrategy` (it never wraps on width).
* Hyphenation, balanced breaking, break-opportunity classes.
* A public API on `TextBlock.of` to inject breaks other than via `\n` in the source text.
* Rendering-side changes beyond "no measured part emitted".

## 3. Dependencies

* Independent within FP-002 (parallel to IP-01 and IP-02).
* External precondition: FP-001/IP-03 seam.
* Touches `engine.model` (a sealed hierarchy owned by FP-001/IP-01) — coordinate with the
  raw-model owners; this is the only FP-002 plan that changes the raw model.

## 4. Interfaces to Other Plans

* Provides the raw `TextBreak` token to every `engine.model` consumer (counting, `toString`,
  serialization, the measure pipeline). Other FP-002 strategies must tolerate `TextBreak` in
  their `parts` input:
  * IP-01 / IP-02 treat `TextBreak` as a hard break point (flush current line, drop the token).
  * This plan adds a shared private/internal helper `List<TextPart>.splitAtBreaks()` in
    `engine.engine` reused by all three strategies; if IP-01/IP-02 land first they add a TODO
    and this plan wires them up.
* Consumes the FP-001 seam unchanged.

## 5. Design

### 5.1 `TextBreak` token

```kotlin
@Serializable
@SerialName("break")
data object TextBreak : TextPart {
    override val text: String get() = "\n"
}
```

* `data object` — there is only one kind of break; keeps equality/`hashCode` free and
  serialization a bare discriminator.
* `text == "\n"` so `charCount` and `toString` stay well-defined; alternatives considered:
  `text == ""` (would make `charCount` ignore it) — rejected, a break is one source character.

### 5.2 Tokenizer

* In `tokenize(text)`: on `\n` (after collapsing a preceding `\r`), `flushWord()` then
  `parts += TextBreak`. Runs of newlines emit one `TextBreak` per newline (blank line ⇒ two).
* All other `Char.isWhitespace()` still just separate parts without being stored.
* `TextBlock.of` is unchanged in signature.

### 5.3 `toString()`

* `TextBlock.toString()` currently prefixes a space before every non-first `TextWord`.
  New rule: a `TextWord` that immediately follows a `TextBreak` gets **no** leading space;
  a `TextBreak` appends its `"\n"`. Symbols unchanged.

### 5.4 Counting

* `wordCount` / `symbolCount` unchanged (they filter by concrete type; `TextBreak` counts as
  neither).
* `charCount` counts `TextBreak.text.length == 1`. Add a KDoc note. Existing `CountingTest`
  expectations without breaks stay valid.

### 5.5 Serialization

* JSON (`kotlinx.serialization`): `TextBreak` participates via the sealed `TextPart` serializer;
  discriminator `"break"`. `data object` serializes as `{"type":"break"}`.
* YAML / XML round-trip tests (`e2e/YamlRoundTripTest`, `e2e/XmlRoundTripTest`): add a document
  containing `\n` and assert equality after decode. Confirm the XML format handles a
  discriminator-only object (mirrors FP-001/IP-01 concern in the feature plan risks).
* JVM Java serialization (`PlatformSerializable`, `jvmTest/e2e/JvmSerializationRoundTripTest`):
  `data object` must resolve to the same singleton after deserialize; add a case.

### 5.6 Measured-model pass-through

* Line breakers consume `TextBreak` and never emit it into an `UnplacedLine`, so
  `SimpLayBlockEngine` and `MeasuredTextPart` need no change.
* Add a guard/assert in the block engine path only if a stray `TextBreak` could reach it; the
  default greedy/character/no-wrap strategies must also skip `TextBreak` (treat as a hard
  break for greedy/character, ignore for no-wrap) — small change to
  `LineBreakerStrategy.kt` implementations, covered by regression tests.

### 5.7 `ExplicitBreakLineBreakerStrategy`

* Split `parts` at every `TextBreak` into segments (helper `splitAtBreaks()`).
* Each segment becomes exactly one `UnplacedLine` regardless of `maxWidth` (no wrapping),
  measured with the same `spaceBefore` / ascent / descent rules as `NoWrapLineBreakerStrategy`.
* An empty segment (consecutive breaks) produces an empty line using font-metric ascent/descent
  so blank lines take vertical space.
* `WordBreakerStrategy` is accepted but ignored.

## 6. Affected Files

| File | Change |
| ---- | ------ |
| `engine/.../engine/model/TextPart.kt` | Add `TextBreak` `data object`, KDoc. Single `Write`. |
| `engine/.../engine/model/TextBlock.kt` | Tokenizer emits `TextBreak` for `\n`; `toString()` spacing rule. Single `Write`. |
| `engine/.../engine/model/Counting.kt` | KDoc note on `charCount`; no logic change (verify). |
| `engine/.../engine/engine/LineBreakerStrategy.kt` | Default strategies skip/hard-break on `TextBreak`; KDoc list mentions new strategy. |
| `engine/.../engine/engine/ExplicitBreakLineBreakerStrategy.kt` | New: strategy + `splitAtBreaks()` helper. |
| `engine/.../engine/PlatformSerializable.kt` (jvm) | Only if `data object` needs an explicit `readResolve`; confirm first. |
| `engine/src/commonTest/.../model/TokenizerTest.kt` | Cases for `\n`, `\r\n`, blank lines. |
| `engine/src/commonTest/.../model/CountingTest.kt` | `charCount` with breaks. |
| `engine/src/commonTest/.../model/TextBlockTest.kt` | `toString()` with breaks. |
| `engine/src/commonTest/.../model/SerializationTest.kt` | JSON round-trip with a break. |
| `engine/src/commonTest/.../e2e/YamlRoundTripTest.kt`, `XmlRoundTripTest.kt`, `JsonRoundTripTest.kt` | Document containing `\n`. |
| `engine/src/jvmTest/.../e2e/JvmSerializationRoundTripTest.kt` | `data object` singleton round-trip. |
| `engine/src/commonTest/.../engine/ExplicitBreakLineBreakerStrategyTest.kt` | New: behaviour + builder swap. |
| `docs/` (raw model + engine strategy pages) | Document `TextBreak` and the strategy; load `project-docs` skill. |
| `CHANGELOG.md` | New unreleased entry. |

## 7. Test Design

Load the `testing` skill first. Developer tests, package-mirrored, deterministic measurer double.

* Tokenizer: `newlineBecomesSingleTextBreak`, `crlfBecomesSingleTextBreak`,
  `blankLineBecomesTwoTextBreaks`, `spacesStillNotStored`.
* Counting: `charCountIncludesTextBreak`, `wordAndSymbolCountIgnoreTextBreak`.
* `toString`: `breakRendersAsNewlineWithoutExtraSpace`.
* Serialization: `textBreakRoundTripsJson`, `...Yaml`, `...Xml`, `...JvmSerialization`
  (singleton identity preserved).
* Strategy: `emptyPartsProduceNoLines`, `oneLinePerSegment`,
  `neverWrapsOnWidthEvenWhenOverflowing`, `consecutiveBreaksProduceEmptyLine`,
  `wordBreakerIsIgnored`, `deterministicAcrossRuns`, `swapViaBuilderRoutesToStrategy`.
* Regression: `greedyStrategyTreatsTextBreakAsHardBreak`,
  `noWrapStrategyIgnoresTextBreakGracefully`, existing FP-001 tests unchanged.

## 8. Task Breakdown

### Task 1 — Raw model token

* Add `TextBreak` `data object` to `TextPart.kt` with KDoc (single `Write`).
* Update `tokenize` in `TextBlock.kt`: `\n` / `\r\n` ⇒ one `TextBreak`; adjust `toString()`
  spacing rule (single `Write`).
* Add KDoc note to `charCount` in `Counting.kt`; verify no logic change needed.

### Task 2 — Serialization wiring and verification

* Confirm sealed `TextPart` JSON discriminator picks up `TextBreak` (`@SerialName("break")`).
* Check YAML and XML formats handle a discriminator-only object; adjust config if required.
* Check JVM `data object` deserializes to the singleton; add `readResolve` only if needed.

### Task 3 — Line-breaker changes

* Add `ExplicitBreakLineBreakerStrategy.kt` with `splitAtBreaks()` helper and the no-wrap-style
  segment-to-line assembly, including empty-line handling.
* Update default strategies in `LineBreakerStrategy.kt` to treat `TextBreak` as a hard break
  (greedy, character) or skip it (no-wrap); update KDoc strategy list.

### Task 4 — Tests

* Load `testing` skill.
* Add/extend tokenizer, counting, `toString`, JSON/YAML/XML/JVM round-trip tests.
* Add `ExplicitBreakLineBreakerStrategyTest` and the regression cases from section 7.

### Task 5 — Docs, changelog, build, close-out

* Load `project-docs` skill; document `TextBreak` in the raw-model page and the strategy in the
  engine strategy page.
* Add `CHANGELOG.md` entry.
* Run `./gradlew :engine:build`; fix findings.
* Same change set: status file IP-03 `COMPLETED` and feature `COMPLETED` when it is the last
  plan, tick IP-03 across `FP-002-AdvancedLineBreaking.md`, `git rm` this plan file and, when
  it is the last remaining plan, `git rm FP-002-Overview.md`.

## 9. Risks and Open Questions

* `text` value for `TextBreak` (`"\n"` vs `""`) drives `charCount` and `toString`; chosen
  `"\n"`, revisit if a consumer double-counts.
* XML format may reject or mangle a discriminator-only element — mirrors the FP-001/IP-01
  concern; fallback is giving `TextBreak` a dummy body field, decided during Task 2.
* JVM `data object` singleton identity after Java deserialization is Kotlin-version dependent;
  an explicit `readResolve` may be required.
* Ordering with IP-01/IP-02: if they land first, the shared `splitAtBreaks()` helper and the
  "treat `TextBreak` as hard break" behaviour must be retrofitted here; if this lands first the
  helper is already in place.
* Blank-line vertical spacing depends on font-metric ascent/descent fallback being correct in
  `LineAccumulator`; covered by `consecutiveBreaksProduceEmptyLine`.
