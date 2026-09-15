# Feature Plan: Advanced Line Breaking (COMPLETED)

## 1. Objective

Add advanced, opt-in `LineBreakerStrategy` implementations on top of the seam
introduced in FP-001/IP-03. The default layout behaviour stays unchanged; each
new strategy is set on `SimpLayEngine.Builder`.

## 2. Current State

FP-001/IP-03 provides the `LineBreakerStrategy` seam in
`org.pcsoft.framework.simplay.engine.engine` with three implementations:
`GreedyWordLineBreakerStrategy` (default), `CharacterLineBreakerStrategy` and
`NoWrapLineBreakerStrategy`. A strategy turns a `List<TextPart>` plus a
`maxWidth` into positionless `UnplacedLine`s and may consult a `WordBreakerStrategy`
(no-op default). The raw model in `engine.model` has no explicit line-break
token; the tokenizer drops `\n` as whitespace.

## 3. Target State

Three further strategies are available and set on `SimpLayEngine.Builder`:

* a balanced strategy that minimises overall raggedness across a block,
* a break-opportunity strategy that breaks only at UAX #14 allowed positions,
* an explicit-break strategy backed by a new raw line-break token.

No change to the default strategy or to existing engine output.

## 4. Requirements

### Functional Requirements

* `BalancedLineBreakerStrategy` produces fewer/short-line artefacts than greedy
  for the same input and width.
* `BreakOpportunityLineBreakerStrategy` never breaks inside a non-breaking run
  and honours common punctuation break classes.
* `ExplicitBreakLineBreakerStrategy` breaks only at explicit break tokens and
  never on width.
* All strategies remain deterministic and free of platform APIs.
* All strategies are chosen on `SimpLayEngine.Builder` and fixed once built.

### Technical Requirements

* Kotlin, Gradle, `commonMain` / `commonTest`.
* Reuse `LineBreakerStrategy`, `UnplacedLine`, `WordBreakerStrategy` and the
  deterministic test `FontMeasureCalculator` from FP-001.
* The raw-model change for explicit breaks keeps `kotlinx.serialization`
  round-trips intact and updates counting / `toString` consistently.
* No new runtime dependency without the user's approval.

## 5. Architecture

* New strategy types live next to the existing ones in
  `...engine.engine` (implementation detail may sit under `...engine.engine.internal`).
* `BalancedLineBreakerStrategy` adds an internal cost function over candidate
  `UnplacedLine`s and a dynamic-programming optimum; input and output types are the
  seam types.
* `BreakOpportunityLineBreakerStrategy` adds an internal mapping from characters
  / `TextPart`s to break classes, then a greedy fill restricted to allowed
  break points.
* `ExplicitBreakLineBreakerStrategy` requires a raw-model addition in
  `engine.model` (e.g. a `TextBreak` marker `TextPart`), tokenizer and
  serialization wiring, and measured-model pass-through.
* `SimpLayEngine.Builder` already accepts any `LineBreakerStrategy`; no new
  builder method is needed.

## 6. Implementation Plan Overview

| ID    | Implementation Plan             | Objective                                                                                          | Dependencies | Status |
| ----- | ------------------------------- | ------------------------------------------------------------------------------------------------- | ------------ | ------ |
| IP-01 | Balanced Line Breaking (COMPLETED)          | Provide `BalancedLineBreakerStrategy` with a Knuth-Plass-style minimal-raggedness line breaker.    | -            | COMPLETED |
| IP-02 | Break-Opportunity Line Breaking (COMPLETED) | Provide `BreakOpportunityLineBreakerStrategy` following UAX #14 break classes.                     | -            | COMPLETED |
| IP-03 | Explicit Break Line Breaking (COMPLETED)    | Add a raw line-break token and `ExplicitBreakLineBreakerStrategy` that breaks only at those tokens. | -            | COMPLETED |

All three plans require the FP-001/IP-03 `LineBreakerStrategy` seam as an
external precondition; among themselves they are independent and parallelizable.

### Completed Plans

* IP-01: Balanced Line Breaking - COMPLETED.
* IP-02: Break-Opportunity Line Breaking - COMPLETED.
* IP-03: Explicit Break Line Breaking - COMPLETED.

## 7. Implementation Plans

### IP-01: Balanced Line Breaking (COMPLETED)

**What was actually built**

* `BalancedLineBreakerStrategy` lives in `org.pcsoft.framework.simplay.engine`
  (next to `LineBreakerStrategy`), not under a nested `...engine.engine`
  package as sections 2/5 above assumed - the engine module has no such nested
  package.
* Everything else matches the plan: internal cost constants, an `O(n^2)`
  dynamic program over word-granular break points, `WordBreakerStrategy` reuse
  for over-wide words, and tests comparing raggedness against
  `GreedyWordLineBreakerStrategy`.
* Follow-up integration (done as part of finishing the feature): the strategy
  now splits `parts` into segments with the `splitAtBreaks()` helper shared
  with `ExplicitBreakLineBreakerStrategy` and runs the balancing dynamic
  program independently per segment, so a `TextBreak` always forces a hard
  line end and can never be smoothed away by the raggedness optimisation. Two
  consecutive breaks (or a break at the very start/end) yield an empty segment,
  which still produces an empty line.

**Objective**

Add `BalancedLineBreakerStrategy`, a whole-block line breaker that minimises the
total badness (raggedness) instead of filling each line greedily.

**Scope**

* In: badness/cost function over `UnplacedLine` candidates, dynamic-programming
  optimum, `BalancedLineBreakerStrategy`, builder wiring, tests comparing
  raggedness against the greedy default.
* In: default cost parameters exposed as internal constants.
* Out: hyphenation, changes to the default strategy, pagination changes.

**Dependencies**

* Independent. External precondition: FP-001/IP-03 seam.

**Interfaces to Other Plans**

* Consumes `LineBreakerStrategy`, `UnplacedLine`, `WordBreakerStrategy` from
  FP-001. Provides no new shared type.

### IP-02: Break-Opportunity Line Breaking (COMPLETED)

**Objective**

Add `BreakOpportunityLineBreakerStrategy` that fills lines greedily but only
breaks at positions allowed by UAX #14 break classes.

**Scope**

* In: mapping of characters / `TextPart`s to a small set of break classes,
  greedy fill restricted to allowed break points, `BreakOpportunityLineBreakerStrategy`,
  builder wiring, tests with punctuation and CJK-like input.
* Out: full Unicode line-break tables, locale tailoring, dictionary breaking.

**Dependencies**

* Independent. External precondition: FP-001/IP-03 seam.

**Interfaces to Other Plans**

* Consumes the FP-001 seam and the deterministic test `FontMeasureCalculator`.
  Provides no new shared type.

**Built**

* `BreakOpportunityLineBreakerStrategy`, the internal `BreakClass` enum and the
  classifier live in
  `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/BreakOpportunityLineBreakerStrategy.kt`
  - the actual engine package is `org.pcsoft.framework.simplay.engine`
  (single-level), not the `...engine.engine` path named in section 2/5; the
  other shipped strategies (`GreedyWordLineBreakerStrategy`,
  `CharacterLineBreakerStrategy`, `NoWrapLineBreakerStrategy`) already live
  there too.
* Break opportunities are derived by classifying `TextPart`s into atoms, then
  grouping atoms into maximal non-breakable chunks; the greedy fill operates on
  chunks exactly like `GreedyWordLineBreakerStrategy` operates on parts. An
  over-wide CJK word needs no dedicated splitting path: each CJK character is
  already its own chunk, so it wraps through the same greedy fill.
* Follow-up integration (done as part of finishing the feature): a `TextBreak`
  is now classified as a hard break independent of the curated break-opportunity
  table - the atom stream is cut at every `TextBreak` before chunking, so it can
  never be absorbed into a chunk or overridden by the break-class rules.

### IP-03: Explicit Break Line Breaking (COMPLETED)

**Objective**

Introduce an explicit line-break token in the raw model and add
`ExplicitBreakLineBreakerStrategy` that breaks only at those tokens.

**Scope**

* In: new raw marker `TextPart` (e.g. `TextBreak`), tokenizer keeps `\n` as that
  marker, `kotlinx.serialization` wiring, counting / `toString` handling,
  measured-model pass-through, `ExplicitBreakLineBreakerStrategy`, builder
  wiring, tests including save / load round-trips.
* Out: width-based wrapping in this strategy, hyphenation.

**Dependencies**

* Independent. External precondition: FP-001/IP-03 seam. Touches `engine.model`,
  so it must be coordinated with the FP-001 raw-model owners.

**Interfaces to Other Plans**

* Provides the raw `TextBreak` token to `engine.model` consumers. Consumes the
  FP-001 seam.

**What was actually built**

* The `LineBreakerStrategy` seam already lives directly in package
  `org.pcsoft.framework.simplay.engine` (not a nested `...engine.engine`
  sub-package as this plan's Architecture section assumed);
  `ExplicitBreakLineBreakerStrategy` and the shared `splitAtBreaks()` helper
  were placed there, next to `LineBreakerStrategy.kt`.
  * `TextBreak` is a `data object` with a manual JVM `readResolve`, so it
    resolves to the same singleton after `java.io.Serializable` round trips.
  * `TextBreak.text` is `"\n"` (not empty), so it counts as one source
    character in `charCount()`, matching the risk noted in section 9.
  * The default `GreedyWordLineBreakerStrategy` and `CharacterLineBreakerStrategy`
    were changed to flush the current line as a hard break at a `TextBreak`;
    `NoWrapLineBreakerStrategy` ignores it.
* `splitAtBreaks()` is now also reused by `BalancedLineBreakerStrategy` (IP-01),
  and `BreakOpportunityLineBreakerStrategy` (IP-02) applies the equivalent
  hard-break handling directly on its atom stream, so all three FP-002
  strategies treat `TextBreak` consistently as a hard line separator.

## 8. Dependency Graph

```text
FP-001/IP-03 (external precondition)
├── FP-002/IP-01 (COMPLETED)
├── FP-002/IP-02 (COMPLETED)
└── FP-002/IP-03 (COMPLETED)
```

## 9. Risks and Open Questions

* Exact shape of the `TextBreak` token and its effect on `wordCount` /
  `symbolCount` / `charCount` and `TextBlock.toString()`.
* Whether balanced breaking needs its own measurement cache or can reuse the
  engine's `SimpLayFontEngine` cache.
* Default badness parameters for the balanced strategy (line-fill penalty,
  last-line handling, overflow penalty).
* Whether break-opportunity handling should be a public enum or stay internal.
* XML / YAML round-trip of the new sealed `TextPart` subtype (discriminator
  naming), mirroring the FP-001/IP-01 concerns.

## 10. Feature Completion Criteria

* `./gradlew :engine:build` is green.
* Each of the three strategies can be set on `SimpLayEngine.Builder`.
* `BalancedLineBreakerStrategy` measurably reduces raggedness versus greedy on a
  fixed test document.
* `BreakOpportunityLineBreakerStrategy` never breaks inside a non-breaking run.
* `ExplicitBreakLineBreakerStrategy` breaks exactly at the explicit tokens and
  survives a JSON save / load round-trip.
* The default engine behaviour and all FP-001 tests are unchanged.
* Each strategy ships with its own tests; all tests are green.
