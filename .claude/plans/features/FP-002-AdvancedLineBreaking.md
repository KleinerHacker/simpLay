# Feature Plan: Advanced Line Breaking

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

| ID    | Implementation Plan             | Objective                                                                                          | Dependencies |
| ----- | ------------------------------- | ------------------------------------------------------------------------------------------------- | ------------ |
| IP-01 | Balanced Line Breaking          | Provide `BalancedLineBreakerStrategy` with a Knuth-Plass-style minimal-raggedness line breaker.    | -            |
| IP-02 | Break-Opportunity Line Breaking | Provide `BreakOpportunityLineBreakerStrategy` following UAX #14 break classes.                     | -            |
| IP-03 | Explicit Break Line Breaking    | Add a raw line-break token and `ExplicitBreakLineBreakerStrategy` that breaks only at those tokens. | -            |

All three plans require the FP-001/IP-03 `LineBreakerStrategy` seam as an
external precondition; among themselves they are independent and parallelizable.

## 7. Implementation Plans

### IP-01: Balanced Line Breaking

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

### IP-02: Break-Opportunity Line Breaking

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

### IP-03: Explicit Break Line Breaking

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

## 8. Dependency Graph

```text
FP-001/IP-03 (external precondition)
├── FP-002/IP-01
├── FP-002/IP-02
└── FP-002/IP-03
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
