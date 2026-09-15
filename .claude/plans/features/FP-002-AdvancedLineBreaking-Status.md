# Feature Status: Advanced Line Breaking

Status: COMPLETED

## Implementation Plans

| ID | Implementation Plan | Status |
|----|---------------------|--------|
| IP-01 | Balanced Line Breaking | COMPLETED |
| IP-02 | Break-Opportunity Line Breaking | COMPLETED |
| IP-03 | Explicit Break Line Breaking | COMPLETED |

## Overall Progress

100%

## Notes

Feature Plan created. External precondition for every plan: the
`LineBreakerStrategy` seam from FP-001/IP-03.

IP-03 completed: `TextBreak` raw token, tokenizer wiring, counting/`toString`
pass-through, `ExplicitBreakLineBreakerStrategy` plus the shared `splitAtBreaks()`
helper, and the default greedy/character/no-wrap strategies now handle
`TextBreak`.

IP-01 completed: `BalancedLineBreakerStrategy` ships in `engine`, opt-in via
`SimpLayEngine.Builder.lineBreakerStrategy(...)`. It now also treats `TextBreak`
as a hard break using `splitAtBreaks()`, breaking the block into independent
segments and running the balancing dynamic program per segment.

IP-02 completed: `BreakOpportunityLineBreakerStrategy` ships in
`engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/BreakOpportunityLineBreakerStrategy.kt`,
opt-in via `SimpLayEngine.Builder.lineBreakerStrategy(...)`, default unchanged.
It now also treats `TextBreak` as a hard break, splitting the atom stream at
every `TextBreak` before chunking and filling.

All three implementation plans are complete; feature FP-002 is COMPLETED.
