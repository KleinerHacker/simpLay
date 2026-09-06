# Feature Status: Layout Engine Foundation

Status: IN_PROGRESS

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | Persistable Raw Object Model | COMPLETED |
| IP-02 | Measured Decorator Model | NOT_STARTED |
| IP-03 | Layout Engine | NOT_STARTED |
| IP-04 | End-to-End Layout & Persistence Tests | NOT_STARTED |
| IP-05 | Documentation Alignment | NOT_STARTED |

## Overall Progress

20%

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
constructor, `@ConsistentCopyVisibility`, `symbol: Char` accessor). `Counting.kt`
uses `@file:JvmName("CountingUtil")` so the counting extensions are `CountingUtil`
static methods on the JVM.
