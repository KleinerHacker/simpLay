# Changelog

All notable end-user visible changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Only changes an end user of the framework can see or notice belong here.
Tests, refactorings, renamings, moved code, build and CI changes, changes to the
rules under `.claude` and changes to the documentation itself are intentionally
excluded.

## [UNRELEASED]

### Added

- `engine`: `SimpLayEngine`, built through `SimpLayEngine.builder(measurer)`,
  turns a raw `Document` into a `MeasuredDocument` using a caller-supplied
  `FontMeasureCalculator`. Greedy word- and symbol-aware line breaking with line
  spacing and `LEFT` / `RIGHT` / `CENTER` / `JUSTIFY` alignment, automatic
  `FlowPage` continuation and automatic `SinglePage` height growth.
- `engine`: pluggable `LineBreakerStrategy` (default `GreedyWordLineBreakerStrategy`,
  plus `CharacterLineBreakerStrategy` and `NoWrapLineBreakerStrategy`) and a
  `WordBreakerStrategy` hyphenation seam (default `NoOpWordBreakerStrategy`),
  both fixed on the engine builder.
