# simpLay

simpLay is a Kotlin framework for building simulations. The core is a Kotlin
Multiplatform engine; the integration modules are grouped into user-interface
bindings under `ui/`.

## AI disclosure

In accordance with EU transparency requirements, please note that this project -
including its source code, tests, documentation and configuration - was created
entirely with the assistance of artificial intelligence.

## Modules

| Module             | Type                 | Artifact            | Purpose                                       |
|--------------------|----------------------|---------------------|-----------------------------------------------|
| `engine`           | Kotlin Multiplatform | `simplay-engine`    | Platform-independent simulation core          |
| `ui/common`        | Kotlin JVM           | `simplay-common`    | Toolkit-agnostic building blocks for GUI bindings |
| `ui/fx`            | Kotlin JVM           | `simplay-fx`        | JavaFX integration for the engine             |
| `ui/swing`         | Kotlin JVM           | `simplay-swing`     | Swing integration for the engine             |

Console output (`ui/console`), PDF export (`export/jvm-pdf`) and printing
(`export/jvm-print`) are planned; see _Implementation state_ below.

The shared build logic is provided by convention plugins in `buildSrc`
(`kotlin-jvm`, `kotlin-multiplatform`). The Gradle project paths mirror the
directory layout (`:ui:fx`, `:ui:swing`, ...); the repository root contains
no source code.

The base package is `org.pcsoft.framework.simplay`; each module appends its own
name, independent of its directory group (`...simplay.engine`,
`...simplay.uicommon`, `...simplay.fx`, `...simplay.swing`).

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
* `./gradlew :ui:fx:build` / `:ui:swing:run` - address a single nested module.

## Consuming the artifacts

Each module publishes a JAR named `simplay-<module>` under the Maven group
`org.pcsoft.framework` (the `engine` module additionally produces target-specific
artifacts such as `simplay-engine-jvm`). The release artifacts are published to
GitHub Packages (`https://maven.pkg.github.com/KleinerHacker/simPlay`); for local
development use `./gradlew publishToMavenLocal`.

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:0.3.0")
    implementation("org.pcsoft.framework:simplay-fx:0.3.0")
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

* User guide (MkDocs, gh-pages): <https://kleinerhacker.github.io/simpLay/latest/>
* API documentation (KDoc): published within the user guide under _API Docs_
* Licence report: published within the user guide under _Licences_

## Implementation state

* [x] Multi-module project layout: `engine`, `ui/` (`common`, `fx`, `swing`)
* [ ] Simulation engine core (`engine`)
    * [x] Raw and measured document model; serializable raw model (JSON, YAML,
      XML, JVM serialization)
    * [x] Measure engine (`SimpLayEngine`): font-measuring callback, pluggable
      line breaking, alignment, `FlowPage` continuation, `SinglePage` growth,
      shared `RenderConfiguration`
    * [x] Font fingerprinting: `Document.withFontFingerprints`, per-font
      `MeasuredFont.fingerprintStatus` and `MeasuredDocument.fingerprintDeviations`
      to detect a missing or silently replaced font on reopen
    * [x] Page numbering: `Document.numbering` (`PageNumbering`) configures
      position (eleven anchors, including binding-aware `INNER` / `OUTER`),
      start number, per-page exclusion by stable `Page.id` and the counting mode
      (`CONTINUOUS` / `SKIP_EXCLUDED`); `MeasuredDocument.planPageNumbers` lays out the labels,
      drawn by `ui/fx` and `ui/swing`
    * [ ] End-to-end layout and persistence tests
    * [ ] Engine user documentation (MkDocs, KDoc alignment)
* [ ] Console output integration (`ui/console`)
* [x] Toolkit-agnostic GUI building blocks (`ui/common`): linear document text
  index, glyph hit test, selection span, document text editor, styled-text
  clipboard serialisation and per-page interaction modes (`PageMode`),
  shared by `ui/fx` and `ui/swing`
* [x] JavaFX integration (`ui/fx`): `CanvasDocumentRenderer` (whole-document and
  single-page canvas rendering) and `PaperSheetView` - a scrollable, zoomable
  paper-sheet control with four interaction levels (`PaperSheetMode.STATIC` /
  `SELECTABLE` / `NAVIGABLE` / `EDITABLE`, overridable per page through
  `pageModes`), mouse text selection, in-place editing with insert/overwrite
  `caretMode`, `Page Up` / `Page Down` and `scrollTo*` navigation, `Ctrl+A`
  select-all, `onType` and `onMouseEvent` events, automatic page-number
  drawing, FXML-compatible floating overlays and JavaFX CSS styling, plus
  `FxFontProbe` (font availability and fingerprint checks against the JavaFX
  text stack) - plus the `ui/fx` user documentation
* [ ] PDF export integration (`export/jvm-pdf`)
* [ ] Printing integration (`export/jvm-print`)
* [x] Swing integration (`ui/swing`): `DocumentImageRenderer` (whole-document and
  single-page `BufferedImage` rendering) and `PaperSheetView` - a scrollable,
  zoomable paper-sheet `JComponent` with the same four interaction levels,
  per-page `pageModes` overrides, mouse text selection, in-place editing with
  insert/overwrite `caretMode`, `Page Up` / `Page Down` and `scrollTo*`
  navigation, `Ctrl+A` select-all, `onType` and `onMouseEvent` events,
  automatic page-number drawing, floating overlays and Look-and-Feel styling,
  plus `SwingFontProbe` (font availability and fingerprint checks against the
  AWT text stack) - plus the `ui/swing` user documentation
