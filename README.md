# simPLay

simPLay is a Kotlin framework for building simulations. The core is a Kotlin
Multiplatform engine; a Kotlin Multiplatform module adds console output and
dedicated JVM modules add integrations for JavaFX, Swing, PDF export and
printing.

## Modules

| Module    | Type                   | Artifact           | Purpose                                  |
|-----------|------------------------|--------------------|------------------------------------------|
| `console` | Kotlin Multiplatform   | `simplay-console`  | Console output integration for the engine |
| `engine`  | Kotlin Multiplatform   | `simplay-engine`   | Platform-independent simulation core     |
| `fx`      | Kotlin JVM             | `simplay-fx`       | JavaFX integration for the engine        |
| `j-pdf`   | Kotlin JVM             | `simplay-j-pdf`    | PDF export integration for the engine    |
| `j-print` | Kotlin JVM             | `simplay-j-print`  | Printing integration for the engine      |
| `swing`   | Kotlin JVM             | `simplay-swing`    | Swing integration for the engine         |

The shared build logic is provided by convention plugins in `buildSrc`
(`kotlin-jvm`, `kotlin-multiplatform`). Every module lives in its own top-level
directory; the repository root contains no source code.

The base package is `org.pcsoft.framework.simplay`; each module appends its own
name (`...simplay.console`, `...simplay.engine`, `...simplay.fx`,
`...simplay.jpdf`, `...simplay.jprint`, `...simplay.swing`).

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

* [x] Multi-module project layout (`console`, `engine`, `fx`, `j-pdf`, `j-print`, `swing`)
* [ ] Simulation engine core (`engine`)
    * [x] Persistable raw document model with `kotlinx.serialization` wiring
    * [x] Non-persistable measured model
    * [x] Measure engine (`SimpLayEngine`): font-measuring callback, pluggable line
      breaking, alignment, `FlowPage` continuation, `SinglePage` growth
    * [ ] End-to-end layout and persistence tests
    * [ ] Engine user documentation (MkDocs, KDoc alignment)
* [ ] Console output integration (`console`)
* [ ] JavaFX integration (`fx`)
    * [x] Internal rendering foundation (JavaFX font measuring, measured-tree draw
      walk, size and hit-test helpers)
    * [x] `CanvasDocumentRenderer`: whole-document and single-page rendering onto a
      `Canvas` with dashed page breaks and configurable unit scale / page gap
    * [ ] Scrollable, zoomable paper-sheet component with text selection
    * [ ] In-place editing with standard key bindings
    * [ ] JavaFX CSS styling for the paper-sheet component
    * [ ] `fx` user documentation
* [ ] PDF export integration (`j-pdf`)
* [ ] Printing integration (`j-print`)
* [ ] Swing integration (`swing`)
