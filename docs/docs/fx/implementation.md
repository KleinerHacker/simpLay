# fx - Implementation

The `fx` module binds a running simulation from the `engine` core to a JavaFX user
interface.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-fx:<version>")
}
```

JavaFX is exposed transitively by this module, so no separate JavaFX dependency is
required in the consuming build.

## Entry points

The first public entry point is `CanvasDocumentRenderer` in
`org.pcsoft.framework.simplay.fx.canvas`. It is created for one fixed document
and measures it up front:

```kotlin
val renderer = CanvasDocumentRenderer.`for`(document) {
    unitScale = 1.5
    pageGap = 16.0
    lineBreakerStrategy = NoWrapLineBreakerStrategy
}
val canvas = renderer.renderDocument()
val firstPageSize = renderer.pageCanvasSizes[0]
```

It paints that document onto a JavaFX `Canvas`, whole (`renderDocument`) or one
page at a time (`renderPage`), and reports the required canvas size through
`documentCanvasSize` and the index-addressable `pageCanvasSizes[pageIndex]` (and
the page count through `pageCount`). The configuration lambda runs over
`CanvasRenderConfiguration` (`unitScale`, `pageGap`, and the shared
`lineBreakerStrategy` / `wordBreakerStrategy`).

!!! note

    A full usage page for the canvas renderer and the paper-sheet component
    follows with the `fx` documentation plan (IP-06).
