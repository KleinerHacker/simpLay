# Feature Status: Layout Engine Foundation

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | Persistable Raw Object Model | COMPLETED |
| IP-02 | Measured Decorator Model | COMPLETED |
| IP-03 | Layout Engine | COMPLETED |
| IP-04 | End-to-End Layout & Persistence Tests | NOT_STARTED |
| IP-05 | Documentation Alignment | NOT_STARTED |

## Overall Progress

60%

## Notes

IP-01 completed: raw model, geometry/metric value types, kotlinx.serialization
wiring, tokenizer, TextBlock parser/toString and counting extensions in
`engine/commonMain`, with tokenizer/text-block/counting/serialization/model
tests. Deviation: `Document`, `Font`, `TextStyle` and `TextBlock` are direct
`@Serializable data class` types (no `...Data` impl, no top-level factory); only
`Page` and `TextPart` remain `sealed`. Each of the four data classes implements a
`@PublishedApi internal` contract interface (`IDocument`, `IFont`, `ITextStyle`,
`ITextBlock`) so IP-02 can decorate them with `by` delegation. The tokenizer is a
`private` function in `TextBlock.kt` (no public `tokenize`), tested via `TextBlock.of`.
`TextBlock` has a `private` primary constructor plus `@ConsistentCopyVisibility`,
so `TextBlock.of` is the only way to build one. `TextSymbol`'s public constructor
takes a `Char` and stores it as the single-character `text` (private primary
constructor, `@ConsistentCopyVisibility`, `symbol: Char` accessor). `MeasuredCounting.kt`
uses `@file:JvmName("CountingUtil")` so the counting extensions are `CountingUtil`
static methods on the JVM.

IP-02 completed: non-persistable measured model in `engine/commonMain` package
`...engine.measure` (`MeasuredFont`, `MeasuredTextStyle`, `MeasuredTextPart`,
`MeasuredLine`, `MeasuredTextBlock`, `MeasuredPage` with `MeasuredFlowPage` /
`MeasuredSinglePage`, `MeasuredDocument`), with `DelegationTest`,
`MeasuredAccessorTest`, `SinglePageGrowthTest` and `BuildByHandTest`. No
Kotlin `by` delegation and no `I*` contract interfaces: each measured type holds
a public `raw` handle, an unchanged property is a hand-written forwarder
(`val x get() = raw.x`), a property that becomes a measured type is exposed once
as that type (original via `raw`), nothing is held twice. `MeasuredFont` forwards
`family` / `size` / `weight` / `style` (+ `metrics`); `MeasuredTextStyle` forwards
`lineSpacing` / `alignment`, `font` is a `MeasuredFont`, `resolvedLineHeight` is
derived from `font.metrics` + `lineSpacing`;
`MeasuredFlowPage` / `MeasuredSinglePage` forward `layout` and expose
`List<MeasuredTextBlock>`; `MeasuredTextBlock` / `MeasuredDocument` are plain
wrappers; `MeasuredTextPart` forwards `text`. IP-01 change: `IFont`, `ITextStyle`,
`ITextBlock`, `IDocument` removed again; the raw data classes have no extra
supertype; `Page` / `TextPart` unchanged `@Serializable sealed`. `MeasuredPage` /
`MeasuredTextPart` are not subtypes of `Page` / `TextPart`.
`MeasuredPage.contentArea`, `requiredContentHeight` and `effectiveSize` are
derived on the fly (from `layout` and `blocks`), not stored; the page
constructors take only `raw`, `pageIndex` and `blocks`. `measure/Counting.kt`
mirrors `model/Counting.kt` (`wordCount` / `symbolCount` / `charCount` for
`MeasuredTextBlock` / `MeasuredPage` / `MeasuredDocument`, `CountingUtil` JvmName).

IP-03 completed: layout engine in `engine/commonMain` package `...engine.engine`
(`SimpLayEngine` built via `SimpLayEngine.builder(measurer)....build()`;
`FontMeasureCalculator`,
`WordBreakerStrategy` / `NoOpWordBreakerStrategy`, `LineBreakerStrategy` with
`UnplacedLine` / `UnplacedPart` and the objects `GreedyWordLineBreakerStrategy`,
`CharacterLineBreakerStrategy`, `NoWrapLineBreakerStrategy`), plus the `internal`
measure stages `SimpLayFontEngine`, `SimpLayBlockEngine` and `SimpLayPageEngine`
under `...engine.engine.internal` (shared `SimpLay*Engine` family name, each a
`class` with a private constructor plus a nested `Builder` and
`companion.builder(...)` like `SimpLayEngine`; `SimpLayPageEngine`'s builder
wires in a `SimpLayFontEngine` and a `SimpLayBlockEngine`; "measure" vocabulary
throughout, no "layout" wording).
Tests: `SimpLayFontEngineTest`, `GreedyWordLineBreakerStrategyTest`, `SymbolAttachTest`,
`CharacterLineBreakerStrategyTest`, `NoWrapLineBreakerStrategyTest`,
`LineBreakerStrategySwapTest`, `AlignmentTest`, `FlowPaginationTest`,
`SinglePageTest`, `EmptyDocumentTest`, `WordBreakerStrategyHookTest` with the
shared `EngineTestData` fixture. Deviations vs. the plan: the greedy line filler
became an exchangeable `LineBreakerStrategy` seam with three shipped
implementations, all set on `SimpLayEngine.Builder` and fixed once built; the
measuring callback is named `FontMeasureCalculator` and the word-break seam
`WordBreakerStrategy`; font metrics are derived from a fixed `REFERENCE_GLYPHS`
string (letters, digits, diacritics, punctuation), `leading = 0.0`, measured
fonts cached per `Font` per `SimpLayFontEngine` instance (style wrapper built
fresh); `measure(document)` is a member of the built engine, not a top-level
extension; `FlowPage` pagination is per line, so one raw `TextBlock` may become
several `MeasuredTextBlock` slices and only the block's final line is
`lastLine`; continuation flow pages deep-copy the `PageLayout` and `pageIndex`
is document-wide. Follow-up feature plan `FP-002-AdvancedLineBreaking` created
(balanced / break-opportunity / explicit-break strategies, one IP each).
