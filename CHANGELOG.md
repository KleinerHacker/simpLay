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

- `engine`: raw document model in `...engine.model` - `Document`, `FlowPage` /
  `SinglePage`, `PageLayout`, `TextBlock` with `TextBlock.of(text, style)` and a
  normalizing `toString()`, `TextPart` (`TextWord` / `TextSymbol`), `TextStyle`,
  `Font`, `LineSpacing`, plus `wordCount()` / `symbolCount()` / `charCount()`
  extensions. The model is serializable (JSON, YAML, XML) and, via the
  `PlatformSerializable` marker, usable with JVM serialization.
- `engine`: measured result model in `...engine.measure` - `MeasuredDocument` and
  `MeasuredPage` / `MeasuredTextBlock` / `MeasuredLine` / `MeasuredTextPart` with
  resolved font metrics and absolute geometry (`contentArea`, `effectiveSize`,
  `lineBox`, `baseline`).
- `engine`: `SimpLayEngine`, built through `SimpLayEngine.builder(measurer)`,
  turns a raw `Document` into a `MeasuredDocument` using a caller-supplied
  `FontMeasureCalculator`. Greedy word- and symbol-aware line breaking with line
  spacing and `LEFT` / `RIGHT` / `CENTER` / `JUSTIFY` alignment, automatic
  `FlowPage` continuation and automatic `SinglePage` height growth.
- `engine`: pluggable `LineBreakerStrategy` (default `GreedyWordLineBreakerStrategy`,
  plus `CharacterLineBreakerStrategy` and `NoWrapLineBreakerStrategy`) and a
  `WordBreakerStrategy` hyphenation seam (default `NoOpWordBreakerStrategy`),
  both fixed on the engine builder.
