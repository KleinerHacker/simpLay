# FP-003 / IP-05: Integration & öffentliche Console-API

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* Zusammenführen von ANSI-Styling-Engine (IP-03) und Terminal-Erkennung (IP-04) zur
  vollständigen öffentlichen `ui:console`-API.
* Demo-Anwendung und End-to-End-Tests über realistische Konfigurationskombinationen.

## Umfang

### Enthalten

* Verdrahtung: `DocumentAnsiRenderer`/`ConsoleRenderConfiguration` schalten SGR-Ausgabe anhand
  von `TerminalCapabilities` automatisch ab, wenn ANSI nicht unterstützt wird oder kein TTY vorliegt.
* Öffentliches Override-Flag, um Styling unabhängig von der Erkennung zu erzwingen/deaktivieren
  (z. B. für Tests oder bewusste Nutzung bei Umleitung in eine Datei).
* Demo-Anwendung (eigener Gradle-Sourceset `demo`, analog `ui:swing`/`ui:fx`) mit Beispieldokument.
* End-to-End-Tests über Kombinationen aus Paginierung, Margin-Modus, Alignment, Styling-An/Aus.

### Nicht enthalten

* Neue fachliche Funktionalität über IP-01 bis IP-04 hinaus.
* Farbe, Darstellung in `ui:fx`/`ui:swing` (IP-06/IP-07).

## Abhängigkeiten

* IP-03, IP-04.

## Schnittstellen zu anderen Plänen

* Konsumiert die Ergebnisse von IP-03 (Styling-Engine) und IP-04 (Terminal-Erkennung).
* Liefert das fertige öffentliche `ui:console`-API als Abschluss der Konsolen-Umsetzung; IP-06/IP-07
  bauen fachlich nur auf IP-01 auf, nicht auf diesem Plan.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/DocumentAnsiRenderer.kt` | Verdrahtung mit `TerminalCapabilities`, Override-Flag. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/ConsoleRenderConfiguration.kt` | Neues Feld `stylingEnabled: Boolean? = null` (`null` = automatisch erkennen). |
| `ui/console/build.gradle.kts` | Neuer `demo`-Sourceset analog `ui/swing/build.gradle.kts`, `run`-Task. |
| `ui/console/src/demo/kotlin/org/pcsoft/framework/simplay/console/demo/DemoApp.kt` | Neu: Konsolen-Demo, gibt ein Beispieldokument aus. |
| `ui/console/src/demo/kotlin/org/pcsoft/framework/simplay/console/demo/DemoDocuments.kt` | Neu: Beispieldokument mit Weight/Style/Dekorationen. |
| `ui/console/src/commonTest/kotlin/org/pcsoft/framework/simplay/console/DocumentAnsiRendererE2ETest.kt` | Neu: End-to-End-Kombinationstests. |
| `docs/` (Console-Modulseite) | Nach Laden des `project-docs`-Skills: `ui:console`-Nutzung dokumentieren. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

* `stylingEnabled: Boolean? = null` in `ConsoleRenderConfiguration`: `null` bedeutet automatische
  Erkennung über `TerminalCapabilities.supportsAnsi()`/`isTty()`; `true`/`false` erzwingen das
  Verhalten unabhängig von der Erkennung.
* `DocumentAnsiRenderer` reicht den effektiven Styling-Zustand an `AnsiDocumentRenderer` (IP-03)
  durch; bei deaktiviertem Styling wird `AnsiCodes` gar nicht aufgerufen (reiner Klartextpfad).
* Demo-Sourceset folgt exakt dem Muster aus `ui/swing/build.gradle.kts` (eigener `SourceSet`,
  `demoImplementation`/`demoRuntimeOnly` erweitern `implementation`/`runtimeOnly`, `run`-Task).

## Testentwurf

* `testing`-Skill vor Anlegen der Testklasse laden.
* `stylingAutoDetectedFromCapabilitiesWhenNotOverridden` — `null` nutzt `TerminalCapabilities`.
* `stylingCanBeForcedOnDespiteNoTtyDetected` — `true` erzwingt SGR-Ausgabe.
* `stylingCanBeForcedOffDespiteAnsiSupport` — `false` erzwingt Klartext.
* `combinationOfPagingMarginOverrideAndDecorationsRendersConsistently` — Gesamtkombination
  liefert erwartete, stabile Textausgabe.
* `combinationOfScrollingAlignmentAndLineSpacingRendersConsistently` — zweite Gesamtkombination.

## Aufgaben

### Aufgabe 1 — Verdrahtung

* `stylingEnabled`-Feld in `ConsoleRenderConfiguration` ergänzen.
* `DocumentAnsiRenderer`/`AnsiDocumentRenderer` um effektive Styling-Entscheidung erweitern.

### Aufgabe 2 — Demo-Anwendung

* `demo`-Sourceset in `ui/console/build.gradle.kts` analog `ui:swing` anlegen.
* `DemoApp`/`DemoDocuments` mit einem Beispieldokument (Weight/Style/Dekorationen) erstellen.

### Aufgabe 3 — Tests, Doku, Build, Abschluss

* `testing`-Skill laden; `DocumentAnsiRendererE2ETest` gemäß „Testentwurf" anlegen.
* `project-docs`-Skill laden; Console-Modulseite ergänzen; `CHANGELOG.md` ergänzen.
* Build von `:ui:console` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-05 im Status `COMPLETED`, IP-05 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Ob das Override-Flag drei Zustände (`null`/`true`/`false`) oder ein eigenes Enum sein soll,
  wird in Aufgabe 1 final entschieden; `Boolean?` bevorzugt wegen Einfachheit.
* Demo-Anwendung schreibt direkt nach `System.out`; Verhalten bei fehlender Konsole (z. B. in CI)
  wird über die Klartext-Fallback-Logik aus IP-04 abgedeckt.
