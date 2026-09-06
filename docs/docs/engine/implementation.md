# engine - Implementation

The `engine` module is the platform-independent simulation core, built with Kotlin
Multiplatform.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
}
```

The artifacts are published to GitHub Packages
(`https://maven.pkg.github.com/KleinerHacker/simPlay`). For a Kotlin Multiplatform
consumer add the target-specific artifact (for example `simplay-engine-jvm`) or
rely on Gradle metadata resolution.

## Entry points

### Measuring a document

`SimpLayEngine` is created through a builder. `SimpLayEngine.measure(document)`
turns a raw `Document` (from the `...engine.model` package) into a
`MeasuredDocument` (from `...engine.measure`). Text measuring is delegated to a
`FontMeasureCalculator` that the caller supplies, so the engine stays free of any
platform text stack:

```kotlin
import org.pcsoft.framework.simplay.engine.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics

val engine = SimpLayEngine.builder(
    FontMeasureCalculator { font, text ->
        // delegate to AWT, Skia, a headless stub, ...
        TextMetrics(width = /* ... */, ascent = /* ... */, descent = /* ... */)
    },
).build()

val measured = engine.measure(document)
```

The result is deterministic: the same document and callback always produce the
same `MeasuredDocument`.

### Choosing strategies

Two aspects of line breaking are pluggable and are fixed on the builder:

* `lineBreakerStrategy(...)` - how parts become lines. Defaults to
  `GreedyWordLineBreakerStrategy` (word- and symbol-aware). Also available:
  `CharacterLineBreakerStrategy` (breaks inside a word) and
  `NoWrapLineBreakerStrategy` (never breaks).
* `wordBreakerStrategy(...)` - where an over-long single word may be split.
  Defaults to `NoOpWordBreakerStrategy`, which never hyphenates.

```kotlin
val engine = SimpLayEngine.builder(measurer)
    .lineBreakerStrategy(NoWrapLineBreakerStrategy)
    .build()
```

### Pagination behaviour

* A `FlowPage` whose content exceeds the content height is continued on further
  `MeasuredFlowPage`s; every continuation page carries a deep copy of the page
  frame and page indices run continuously across the document.
* A `SinglePage` never continues onto another page. When its content is taller
  than the page, `MeasuredSinglePage.effectiveSize` reports the grown height.
