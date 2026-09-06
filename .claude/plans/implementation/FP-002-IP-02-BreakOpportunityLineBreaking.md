# FP-002 / IP-02: Break-Opportunity Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status file: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## 1. Objective

Add `BreakOpportunityLineBreakerStrategy`, a greedy `LineBreakerStrategy` that fills lines like
the default but only breaks at positions allowed by a small, self-contained subset of the
UAX #14 line-break classes. It is opt-in via `SimpLayEngine.Builder`; the default is unchanged.

## 2. Scope

### In scope

* New `BreakOpportunityLineBreakerStrategy` object in
  `org.pcsoft.framework.simplay.engine.engine`.
* Internal mapping from a character to a small `BreakClass` enum.
* Internal derivation of break opportunities between and inside `TextPart`s from those classes.
* Greedy line fill restricted to allowed break points; overflow when no opportunity exists.
* Tests with punctuation, non-breaking runs, and CJK-like input.

### Out of scope

* Full Unicode line-break property tables and pair-table; only a curated subset.
* Locale tailoring, dictionary-based breaking (Thai, Khmer, ...).
* Hyphenation; `WordBreakerStrategy` is accepted in the signature but not consulted.
* Raw-model changes; balanced/optimal breaking (IP-01).

## 3. Dependencies

* Independent within FP-002 (parallel to IP-01 and IP-03).
* External precondition: FP-001/IP-03 seam (`LineBreakerStrategy`, `UnplacedLine`,
  `UnplacedPart`, `FontMeasureCalculator`) and the deterministic test `FontMeasureCalculator`.

## 4. Interfaces to Other Plans

* Consumes the FP-001 seam types unchanged.
* Provides no new shared type. `BreakClass` is `internal` (see open question in the feature
  plan); if a later plan needs it public that is a separate change.

## 5. Design

### 5.1 Break classes (internal subset)

Internal enum `BreakClass` with the minimal set needed for Latin + basic CJK behaviour:

| Class | Meaning | Example characters |
| ----- | ------- | ------------------ |
| `MANDATORY` | forced break | (only via explicit tokens — not produced here; reserved) |
| `SPACE` | breakable glue | U+0020, tab already stripped by tokenizer |
| `BEFORE` | break allowed before, not after | opening brackets `([{`, `¡`, `¿` |
| `AFTER` | break allowed after, not before | `)]}`, `!`, `?`, `,`, `;`, `:`, `.`, `%`, `/`, `-`, `—`, `…` |
| `BOTH` | break allowed before and after | ideographic range, `–` where treated as separator |
| `NONBREAK` | never break adjacent | default for letters/digits, `&`, non-breaking punctuation, digits around `.` and `,` (numeric guard) |

* Classification is a `when` over code point ranges/sets, no external table. Documented as an
  approximation of UAX #14, not a conformant implementation.
* CJK detection: code points in CJK Unified Ideographs, Hiragana, Katakana ranges → `BOTH`.

### 5.2 From parts to opportunities

* The tokenizer already removed whitespace and split symbols into single-char `TextSymbol`s and
  runs into `TextWord`s. So opportunities are:
  * before every `TextWord` after the first — the implicit inter-token space (class `SPACE`),
    unless the preceding `TextSymbol` is class `BEFORE` (then the break goes before the symbol)
    or the following/preceding context is `NONBREAK` (e.g. numeric `1,000` — but tokenizer
    keeps `,` as its own symbol, so apply a numeric guard: digit `TextWord` + `,`/`.` +
    digit `TextWord` ⇒ no opportunity around that symbol).
  * around a `TextSymbol` according to its class (`BEFORE` / `AFTER` / `BOTH` / `NONBREAK`).
  * inside a `TextWord` only when it contains CJK code points: an opportunity after every CJK
    code point (class `BOTH`), none between two Latin letters.
* Each opportunity is a cut index over a flattened `(part, charOffset)` stream; a cut may fall
  at a part boundary or inside a CJK `TextWord`.

### 5.3 Greedy fill

* Walk parts left to right accumulating width like `GreedyWordLineBreakerStrategy`
  (`spaceWidth` measured once; `spaceBefore` = space width before a `TextWord` that is not
  first on the line, `0` for `TextSymbol`).
* When the next unit would exceed `maxWidth`, retreat to the last break opportunity at or
  before the overflow point and flush there.
* If there is no opportunity on the current line, place the offending unit anyway and flush
  (line overflows) — never break inside a `NONBREAK` run.
* A CJK `TextWord` longer than `maxWidth` is split at CJK opportunities into synthetic
  `TextWord` pieces (like greedy's hyphenation path, but driven by break class, not
  `WordBreakerStrategy`).

### 5.4 Determinism

* Pure function of input; classification and greedy retreat are deterministic. No platform API.

## 6. Affected Files

| File | Change |
| ---- | ------ |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/BreakOpportunityLineBreakerStrategy.kt` | New: strategy object, `BreakClass` enum, classifier, opportunity derivation, greedy fill, KDoc. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/LineBreakerStrategy.kt` | KDoc only: add to strategy list. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/engine/BreakOpportunityLineBreakerStrategyTest.kt` | New: behaviour tests. |
| `docs/` (engine strategy page) | Add strategy; load `project-docs` skill first. |
| `CHANGELOG.md` | New unreleased entry. |

## 7. Test Design

Load the `testing` skill first. Developer test, package-mirrored, deterministic
`FontMeasureCalculator` double.

* `emptyPartsProduceNoLines`.
* `breaksAtSpaceLikeGreedyForPlainText` — for Latin words + spaces, output equals
  `GreedyWordLineBreakerStrategy` on the same input/width.
* `neverBreaksInsideNonBreakingRun` — a long run classified `NONBREAK` stays whole and
  overflows, no cut inside it.
* `breaksAfterTrailingPunctuation` — `word,` allows a break after `,`, not before it.
* `breaksBeforeOpeningBracket` — `word(` allows a break before `(`.
* `numericGuardKeepsThousandsSeparatorTogether` — `1 , 000` token stream yields no break at the
  separator.
* `cjkTextBreaksBetweenIdeographs` — a CJK-only `TextWord` wider than `maxWidth` is split at
  ideograph boundaries into multiple lines.
* `cjkAndLatinMixedRespectsBothRules` — no break between Latin letters, break between ideographs.
* `deterministicAcrossRuns`.
* `swapViaBuilderRoutesToStrategy` — analogous to `LineBreakerStrategySwapTest`.

## 8. Task Breakdown

### Task 1 — Break-class model

* Add `BreakOpportunityLineBreakerStrategy.kt` skeleton with KDoc.
* Add `internal enum class BreakClass` and the code-point classifier (`when` over ranges/sets).
* Document the subset as a non-conformant UAX #14 approximation.

### Task 2 — Opportunity derivation

* Flatten `parts` into a `(part, charOffset)` stream.
* Emit opportunities from inter-token spaces and per-symbol classes.
* Apply the numeric guard for digit + separator + digit.
* Emit intra-word opportunities for CJK code points only.

### Task 3 — Greedy fill restricted to opportunities

* Accumulate width with greedy's space rules.
* Retreat to the last opportunity at/left of the overflow; flush there.
* Overflow when no opportunity on the line; never cut a `NONBREAK` run.
* Split over-long CJK `TextWord`s at ideograph opportunities into synthetic `TextWord` pieces.
* Build `UnplacedLine`s with the shared `spaceBefore` / ascent / descent rules.

### Task 4 — Tests

* Load `testing` skill.
* Add `BreakOpportunityLineBreakerStrategyTest` covering section 7.

### Task 5 — Docs, changelog, build, close-out

* Load `project-docs` skill; update the engine strategy page and KDoc list.
* Add `CHANGELOG.md` entry.
* Run `./gradlew :engine:build`; fix findings.
* Same change set: status file IP-02 `COMPLETED`, tick IP-02 across
  `FP-002-AdvancedLineBreaking.md`, update `FP-002-Overview.md`, `git rm` this plan file.

## 9. Risks and Open Questions

* The curated break-class set will not match ICU on edge cases; documented as approximate.
* Whether `BreakClass` should be public — kept `internal` per the feature plan open question.
* Interaction of the numeric guard with the tokenizer (which already splits `,`/`.` into
  symbols) needs care; covered by an explicit test.
* CJK range list must be pinned to specific Unicode blocks to stay deterministic across
  Kotlin/JDK versions; ranges are hard-coded, not derived from `Character` APIs where those
  differ per platform.
