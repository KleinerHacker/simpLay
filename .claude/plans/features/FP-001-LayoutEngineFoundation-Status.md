# Feature Status: Layout Engine Foundation

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | Persistable Raw Object Model | COMPLETED |
| IP-02 | Measured Decorator Model | COMPLETED |
| IP-03 | Layout Engine | NOT_STARTED |
| IP-04 | End-to-End Layout & Persistence Tests | NOT_STARTED |
| IP-05 | Documentation Alignment | NOT_STARTED |

## Overall Progress

40%

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
