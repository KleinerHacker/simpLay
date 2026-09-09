# simPLay

simPLay is a Kotlin framework for building simulations. The core is a Kotlin
Multiplatform engine; a Kotlin Multiplatform module adds console output and
dedicated JVM modules add integrations for JavaFX, Swing, PDF export and
printing.

This documentation covers how to consume and interact with the public API of the
framework. For checkout, build and contribution instructions see the project
`README.md`.

## What it is

`simPLay` provides a platform-independent simulation core (`engine`) and two
groups of integration modules: user-interface bindings under `ui/`
(`ui/common`, `ui/fx`, `ui/swing`, `ui/console`) and output-format bindings under
`export/` (`export/jvm-pdf`, `export/jvm-print`). Every module is published as a
separate artifact and can be added independently.

## AI disclosure

In accordance with EU transparency requirements, please note that this project -
including its source code, tests, documentation and configuration - was created
entirely with the assistance of artificial intelligence.

## Modules

- **engine** - [Implementation](engine/implementation.md): add the dependency and
  create a simulation with the Kotlin Multiplatform core.
- **ui/common** - [Implementation](common/implementation.md): the toolkit-agnostic
  building blocks shared by the UI bindings.
- **ui/fx** - [Implementation](fx/implementation.md): bind a simulation to a JavaFX
  user interface.
- **ui/swing** - [Implementation](swing/implementation.md): bind a simulation to a
  Swing user interface.
- **ui/console** - [Implementation](console/implementation.md): render simulation
  output to the console with the Kotlin Multiplatform module.
- **export/jvm-pdf** - [Implementation](jvm-pdf/implementation.md): export
  simulation output to PDF (JVM).
- **export/jvm-print** - [Implementation](jvm-print/implementation.md): send
  simulation output to a printer (JVM).

## Further references

- [API Docs](dokka/html/index.html) - the generated KDoc for every module.
- [Licences](licences/index.html) - the third-party licence report.
