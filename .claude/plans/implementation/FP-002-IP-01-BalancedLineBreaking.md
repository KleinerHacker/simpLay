# FP-002 / IP-01: Balanced Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status file: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## 1. Objective

Add `BalancedLineBreakerStrategy`, a whole-block `LineBreakerStrategy` that minimises the total
raggedness of a block instead of filling each line greedily. It is opt-in via
`SimpLayEngine.Builder.lineBreakerStrategy(...)`; the default stays `GreedyWordLineBreakerStrategy`.

## 2. Scope

### In scope

* New `BalancedLineBreakerStrategy` object in `org.pcsoft.framework.simplay.engine.engine`.
* Internal badness/cost function over candidate `UnplacedLine`s.
* Internal dynamic-programming optimum over all legal break points (Knuth-Plass-style, word
  granularity only).
* Reuse of `WordBreakerStrategy` for words wider than `maxWidth` (same contract as greedy).
* Internal cost constants (line-fill penalty exponent, last-line handling, overflow penalty).
* Tests comparing raggedness against `GreedyWordLineBreakerStrategy` on a fixed document.

### Out of scope

* Hyphenation logic itself (only the existing `WordBreakerStrategy` seam is consulted).
* Any change to `GreedyWordLineBreakerStrategy` or the builder defaults.
* Pagination / `SimpLayPageEngine` changes; output stays a flat `List<UnplacedLine>`.
* Public configuration of cost parameters (kept as internal constants for now).
* Raw-model changes.

## 3. Dependencies

* Independent within FP-002 (parallel to IP-02 and IP-03).
* External precondition: the `LineBreakerStrategy` seam from FP-001/IP-03
  (`LineBreakerStrategy`, `UnplacedLine`, `UnplacedPart`, `WordBreakerStrategy`,
  `FontMeasureCalculator`).

## 4. Interfaces to Other Plans

* Consumes `LineBreakerStrategy`, `UnplacedLine`, `UnplacedPart`, `WordBreakerStrategy`,
  `FontMeasureCalculator`, `MeasuredFont` unchanged.
* Provides no new shared type; `SimpLayEngine.Builder` already accepts any `LineBreakerStrategy`.

## 5. Design

### 5.1 Algorithm

* Treat the block as an ordered list of breakable items: each `TextPart` plus the mandatory
  glue (space) that precedes every `TextWord` after the first; `TextSymbol` carries no leading
  space (mirrors greedy).
* Legal break points are the gaps before a `TextWord`. A run `symbol* word` cannot be split
  before its symbols; symbols stay attached to the following word's line segment start, matching
  greedy attachment.
* `cost[i]` = minimum total badness for breaking items `0 until i` into lines.
  `cost[0] = 0`; `cost[n]` is the answer. Back-pointers reconstruct the chosen breaks.
* For a candidate line covering items `j until i`, compute its natural width from
  `measurer.measure(font.raw, part.text)` advances plus one space width per internal gap
  (space width measured once per call, like greedy).
* Line badness:
  * `slack = maxWidth - naturalWidth`
  * if `slack >= 0` and the line is not the last line of the block:
    `badness = (slack / maxWidth).pow(FILL_PENALTY_EXP) * FILL_PENALTY_SCALE`
  * if `slack < 0` (overflow): `badness = (-slack) * OVERFLOW_PENALTY` (kept finite so an
    unbreakable over-long word still yields a result).
  * the last line of the block contributes `0` badness for positive slack (ragged-right bottom
    line is free), matching typographic convention.
* Words wider than `maxWidth` on their own: offer to `wordBreaker.breakOffsets(...)`; if it
  returns offsets, expand that word into synthetic `TextWord` pieces (as greedy's
  `placeHyphenated` does) before running the DP; if empty, keep the word whole and let the
  overflow penalty apply.
* Determinism: no floating-point-order ambiguity beyond left-to-right accumulation; ties in the
  DP are resolved by preferring the smaller number of lines, then the earliest break.

### 5.2 Complexity

* Naive DP is `O(n^2)` line-cost evaluations with `n` = item count of one block. Acceptable for
  document blocks; no windowing needed in this plan. Measurement results for a
  `(font, text)` pair are cached in a local `HashMap<String, Metrics>` inside the call to avoid
  repeated `measurer` hits.

### 5.3 Cost constants (internal)

* `FILL_PENALTY_EXP = 2.0`
* `FILL_PENALTY_SCALE = 100.0`
* `OVERFLOW_PENALTY = 1000.0`
* Declared `private const val` in the strategy file; documented as tuning-only, not API.

### 5.4 Line assembly

* Reuse the existing private `LineAccumulator` pattern, or a small local builder, to turn the
  chosen item ranges into `UnplacedLine`s with correct `spaceBefore`, `ascent`, `descent`
  (max over parts, font-metric fallback when zero) — identical rules to greedy.

## 6. Affected Files

| File | Change |
| ---- | ------ |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/BalancedLineBreakerStrategy.kt` | New: `BalancedLineBreakerStrategy` object, DP, cost function, constants, KDoc. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/LineBreakerStrategy.kt` | KDoc only: mention `BalancedLineBreakerStrategy` in the strategy list. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/engine/BalancedLineBreakerStrategyTest.kt` | New: behaviour and raggedness tests. |
| `docs/` (engine strategy page) | Add `BalancedLineBreakerStrategy` to the strategy list; load `project-docs` skill first. |
| `CHANGELOG.md` | New entry under the unreleased section. |

## 7. Test Design

Load the `testing` skill before writing the test class. Package mirrors the production package;
developer test (no `IT` suffix). Uses the deterministic `FontMeasureCalculator` /
`FontMeasureCalculator` test double from FP-001 (`EngineTestData` / `MeasureTestData`).

* `emptyPartsProduceNoLines` — empty input returns `emptyList()`.
* `singleShortLineStaysOneLine` — parts that fit stay on one line, equal to greedy.
* `balancedReducesRaggednessVersusGreedy` — on a fixed paragraph and width, the sum of squared
  positive slack over non-last lines is strictly smaller than for `GreedyWordLineBreakerStrategy`.
* `lastLineRaggednessIsFree` — a short final line does not force earlier lines to compress.
* `overlongWordWithoutWordBreakerOverflowsSingleLine` — no exception; word kept whole.
* `overlongWordWithWordBreakerIsSplit` — offsets from a stub `WordBreakerStrategy` are honoured.
* `deterministicAcrossRuns` — two calls with identical input return equal structures.
* `symbolsStayAttachedToFollowingWord` — `symbol* word` never split before the symbols.
* `swapViaBuilderProducesBalancedOutput` — `SimpLayEngine.Builder.lineBreakerStrategy(...)`
  routes through to the new strategy (mirror `LineBreakerStrategySwapTest`).

## 8. Task Breakdown

### Task 1 — Cost function and DP core

* Add `BalancedLineBreakerStrategy.kt` with the object skeleton and KDoc.
* Implement per-call measurement cache and space-width lookup.
* Implement item list construction from `parts` with glue/symbol rules.
* Implement line natural-width and badness function with the internal constants.
* Implement `O(n^2)` DP with back-pointers and documented tie-breaking.
* Reconstruct chosen ranges into `UnplacedLine`s via the shared accumulator rules.

### Task 2 — Word-breaker integration

* Detect words wider than `maxWidth`; call `wordBreaker.breakOffsets(...)`.
* Expand accepted offsets into synthetic `TextWord` pieces before the DP.
* Keep whole word with overflow penalty when no offsets are returned.

### Task 3 — Tests

* Load `testing` skill.
* Add `BalancedLineBreakerStrategyTest` covering section 7.
* Add a builder-swap case analogous to `LineBreakerStrategySwapTest`.

### Task 4 — Docs, changelog, build

* Load `project-docs` skill; add the strategy to the engine strategy page and KDoc list.
* Add a `CHANGELOG.md` entry.
* Run `./gradlew :engine:build` and fix findings.
* In the same change set: mark IP-01 `COMPLETED` in the status file, tick IP-01 everywhere in
  `FP-002-AdvancedLineBreaking.md`, update `FP-002-Overview.md`, and `git rm` this plan file.

## 9. Risks and Open Questions

* Final cost constants may need tuning against real documents; defaults are a starting point.
* Whether balanced breaking should reuse the engine's `SimpLayFontEngine` measurement cache
  instead of a local map — deferred; local map is sufficient and side-effect free.
* Very large single blocks make `O(n^2)` noticeable; windowing is out of scope and can be a
  follow-up plan if a real document shows a problem.
* Interaction with `JUSTIFY` alignment: balancing changes line fill, which changes justification
  spread; acceptable and covered by an assertion on a justified sample.
