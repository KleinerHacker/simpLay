# simPLay

simPLay is a Kotlin framework for building simulations. The core is a Kotlin
Multiplatform engine; a Kotlin Multiplatform module adds console output and
dedicated JVM modules add integrations for JavaFX, Swing, PDF export and
printing.

This documentation covers how to consume and interact with the public API of the
framework. For checkout, build and contribution instructions see the project
`README.md`.

## What it is

`simPLay` provides a platform-independent simulation core (`engine`), a
platform-independent console output module (`console`) and a set of JVM
integration modules that connect a running simulation to a user interface
(`fx`, `swing`) or to an output pipeline (`j-pdf`, `j-print`). Every module is
published as a separate artifact and can be added independently.

## AI disclosure

In accordance with EU transparency requirements, please note that this project -
including its source code, tests, documentation and configuration - was created
entirely with the assistance of artificial intelligence.

## Modules

- **console** - [Implementation](console/implementation.md): render simulation
  output to the console with the Kotlin Multiplatform module.
- **engine** - [Implementation](engine/implementation.md): add the dependency and
  create a simulation with the Kotlin Multiplatform core.
- **fx** - [Implementation](fx/implementation.md): bind a simulation to a JavaFX
  user interface.
- **j-pdf** - [Implementation](j-pdf/implementation.md): export simulation output
  to PDF.
- **j-print** - [Implementation](j-print/implementation.md): send simulation
  output to a printer.
- **swing** - [Implementation](swing/implementation.md): bind a simulation to a
  Swing user interface.

## Further references

- [API Docs](dokka/html/index.html) - the generated KDoc for every module.
- [Licences](licences/index.html) - the third-party licence report.
