# simPLay

simPLay is a Kotlin framework for building simulations. The core is a Kotlin
Multiplatform engine; a Kotlin Multiplatform module adds console output and
dedicated JVM modules add integrations for JavaFX, Swing, PDF export and
printing.

## Modules

| Module      | Type                   | Artifact             | Purpose                                       |
|-------------|------------------------|----------------------|-----------------------------------------------|
| `console`   | Kotlin Multiplatform   | `simplay-console`    | Console output integration for the engine     |
| `engine`    | Kotlin Multiplatform   | `simplay-engine`     | Platform-independent simulation core          |
| `ui-common` | Kotlin JVM             | `simplay-ui-common`  | Toolkit-agnostic building blocks for GUI bindings |
| `fx`        | Kotlin JVM             | `simplay-fx`         | JavaFX integration for the engine             |
| `j-pdf`     | Kotlin JVM             | `simplay-j-pdf`      | PDF export integration for the engine         |
| `j-print`   | Kotlin JVM             | `simplay-j-print`    | Printing integration for the engine           |
| `swing`     | Kotlin JVM             | `simplay-swing`      | Swing integration for the engine              |

The shared build logic is provided by convention plugins in `buildSrc`
(`kotlin-jvm`, `kotlin-multiplatform`). Every module lives in its own top-level
directory; the repository root contains no source code.

The base package is `org.pcsoft.framework.simplay`; each module appends its own
name (`...simplay.console`, `...simplay.engine`, `...simplay.uicommon`,
`...simplay.fx`, `...simplay.jpdf`, `...simplay.jprint`, `...simplay.swing`).

## Checkout and build

```
git clone <repository-url>
cd simplay
./gradlew build
```

This project uses the Gradle Wrapper (`./gradlew`), a version catalog
(`gradle/libs.versions.toml`) and both a build and configuration cache
(`gradle.properties`).

* `./gradlew build` - build all modules.
* `./gradlew check` - run all checks, including tests.
* `./gradlew clean` - remove all build outputs.
* `./gradlew projects` - list the modules.

## Consuming the artifacts

Each module publishes a JAR named `simplay-<module>` under the Maven group
`org.pcsoft.framework` (the `engine` module additionally produces target-specific
artifacts such as `simplay-engine-jvm`). The release artifacts are published to
GitHub Packages (`https://maven.pkg.github.com/KleinerHacker/simPlay`); for local
development use `./gradlew publishToMavenLocal`.

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-fx:<version>")
}
```

Build tasks relevant for consumers and maintainers:

* `./gradlew publishToMavenLocal` - publish all modules to the local Maven repository.
* `./gradlew dokkaGeneratePublicationHtml` - generate the aggregated API documentation.
* `./gradlew koverHtmlReport` - generate the aggregated coverage report.
* `./gradlew licensee` - verify all third-party licences against the allow-list.
* `./gradlew generateLicenseReport` - generate the third-party licence report.
* `./gradlew buildDocs` - build the MkDocs site into `build/docs`.

## Documentation

* User guide (MkDocs, gh-pages): <https://kleinerhacker.github.io/simPlay/>
* API documentation (KDoc): published within the user guide under _API Docs_
* Licence report: published within the user guide under _Licences_

## Implementation state

* [x] Multi-module project layout (`console`, `engine`, `ui-common`, `fx`, `j-pdf`, `j-print`, `swing`)
* [ ] Simulation engine core (`engine`)
    * [x] Raw and measured document model; serializable raw model (JSON, YAML,
      XML, JVM serialization)
    * [x] Measure engine (`SimpLayEngine`): font-measuring callback, pluggable
      line breaking, alignment, `FlowPage` continuation, `SinglePage` growth,
      shared `RenderConfiguration`
    * [ ] End-to-end layout and persistence tests
    * [ ] Engine user documentation (MkDocs, KDoc alignment)
* [ ] Console output integration (`console`)
* [x] Toolkit-agnostic GUI building blocks (`ui-common`): linear document text
  index, glyph hit test, selection span, document text editor and styled-text
  clipboard serialisation, shared by `fx` and `swing`
* [x] JavaFX integration (`fx`): `CanvasDocumentRenderer` (whole-document and
  single-page canvas rendering) and `PaperSheetView` - a scrollable, zoomable
  paper-sheet control with mouse text selection, in-place editing, FXML-compatible
  floating overlays and JavaFX CSS styling - plus the `fx` user documentation
* [ ] PDF export integration (`j-pdf`)
* [ ] Printing integration (`j-print`)
* [x] Swing integration (`swing`): `DocumentImageRenderer` (whole-document and
  single-page `BufferedImage` rendering) and `PaperSheetView` - a scrollable,
  zoomable paper-sheet `JComponent` with mouse text selection, in-place editing,
  floating overlays and Look-and-Feel styling - plus the `swing` user
  documentation
