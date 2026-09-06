# Übersicht FP-002: Advanced Line Breaking

## Feature Plan

- `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
- Status: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## Implementierungspläne

- IP-01 Balanced Line Breaking — `FP-002-IP-01-BalancedLineBreaking.md` (noch anzulegen)
- IP-02 Break-Opportunity Line Breaking — `FP-002-IP-02-BreakOpportunityLineBreaking.md` (noch anzulegen)
- IP-03 Explicit Break Line Breaking — `FP-002-IP-03-ExplicitBreakLineBreaking.md` (noch anzulegen)

## Reihenfolge und Voraussetzungen

- Externe Voraussetzung für alle Pläne: `LineBreakerStrategy`-Naht aus FP-001/IP-03.
- IP-01 — keine Voraussetzung innerhalb FP-002
- IP-02 — keine Voraussetzung innerhalb FP-002
- IP-03 — keine Voraussetzung innerhalb FP-002; berührt `engine.model` (Abstimmung mit FP-001/IP-01)
- IP-01, IP-02, IP-03 sind untereinander unabhängig und parallelisierbar.
