# FP-003 / IP-02: Console-Grundgerüst & Rendering-Pipeline

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* Gradle-Modul `ui:console` einbinden (Kotlin-Multiplatform, analog `engine`).
* Öffentlicher Einstiegspunkt `DocumentAnsiRenderer` mit vollständiger Konfigurationsoberfläche.
* Klartext-Rendering (ohne SGR-Codes) als Basis für IP-03/IP-04.

## Umfang

### Enthalten

* `settings.gradle.kts`: `include(":ui:console")`.
* `ui/console/build.gradle.kts` nach Vorbild `engine/build.gradle.kts` (Kotlin-Multiplatform-
  Konvention, Abhängigkeit auf `:engine`).
* Öffentliche Klasse `DocumentAnsiRenderer` (privater Konstruktor + `of(document) { configurator }`).
* `ConsoleRenderConfiguration`: Flächengröße (Auto-Erkennung/explizit), Margin-Handling
  (übernehmen/überschreiben/Faktor), `lineSpacingFactor`, Paginierungsmodus
  (`ConsolePaginationMode.SCROLLING`/`PAGING`).
* Interne `ConsoleFontMeasureCalculator` (feste Zellenbreite/-höhe = 1 Zeichen).
* Internes `AnsiDocumentRenderer`-Objekt: Layout, Alignment als Padding, Seitentrennung bei
  `PAGING`, fortlaufende Ausgabe bei `SCROLLING` — noch ohne SGR-Codes.
* Grundlegende Tests der Rendering-Pipeline mit einer Stub-Terminalgröße.

### Nicht enthalten

* SGR-Code-Erzeugung für Weight/Style/Dekorationen (IP-03).
* Echte Terminal-Erkennung (IP-04) — in diesem Plan wird die Flächengröße über einen
  austauschbaren, injizierbaren Größen-Lieferanten (Default: feste Fallback-Größe) abstrahiert.

## Abhängigkeiten

* Unabhängig.

## Schnittstellen zu anderen Plänen

* Liefert `DocumentAnsiRenderer`, `ConsoleRenderConfiguration`, `ConsoleFontMeasureCalculator`
  und `AnsiDocumentRenderer`-Gerüst an IP-03, IP-04, IP-05.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `settings.gradle.kts` | `include(":ui:console")` ergänzen. |
| `ui/console/build.gradle.kts` | Kotlin-Multiplatform-Konvention, Abhängigkeit `:engine`, `licensee`-Allowlist. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/DocumentAnsiRenderer.kt` | Neu: öffentlicher Einstiegspunkt. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/ConsoleRenderConfiguration.kt` | Neu: Konfiguration (Größe, Margins, Zeilenabstand, Paginierung). |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/ConsolePaginationMode.kt` | Neu: Enum `SCROLLING`, `PAGING`. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/internal/ConsoleFontMeasureCalculator.kt` | Neu: `FontMeasureCalculator`-Implementierung mit fixen Zellmaßen. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/internal/AnsiDocumentRenderer.kt` | Neu: Layout-/Text-Erzeugung ohne SGR. |
| `ui/console/src/commonTest/kotlin/org/pcsoft/framework/simplay/console/DocumentAnsiRendererTest.kt` | Neu: Tests der Rendering-Pipeline. |
| `.gitkeep`-Dateien unter `ui/console` | Entfernen (`git rm`), sobald reale Quellen vorhanden sind. |

## Entwurf

* `DocumentAnsiRenderer` folgt exakt dem Muster von `DocumentImageRenderer`
  (`ui/swing/src/main/kotlin/.../DocumentImageRenderer.kt`): `document.measure(measurer, config)`
  im Konstruktor, `pageCount`, `renderDocument()`, `renderPage(pageIndex)`.
* Zusätzlich `renderDocument(out: Appendable)`/`renderPage(pageIndex, out: Appendable)` für
  direktes Schreiben (z. B. nach `System.out`), da Konsolenausgabe meist gestreamt statt als
  Rückgabewert verarbeitet wird.
* `ConsoleRenderConfiguration`:
  * `areaSize: ConsoleAreaSize` — `Automatic` (nutzt injizierbaren Größen-Lieferanten) oder
    `Fixed(columns, rows)`.
  * `margins: ConsoleMarginMode` — `FromDocument`, `Override(Margins)`, `Factor(Double)`.
  * `lineSpacingFactor: Double = 1.0` — skaliert die aus `TextStyle.lineSpacing` gerundeten
    Leerzeilen zusätzlich.
  * `paginationMode: ConsolePaginationMode = PAGING`.
* Größen-Lieferant als Funktionstyp `() -> ConsoleSize` im Konstruktor/Konfiguration, damit
  IP-04 die echte Erkennung ohne Änderung an `DocumentAnsiRenderer` selbst nachrüsten kann.
* `ConsoleFontMeasureCalculator` liefert für jedes Zeichen Breite `1.0`, Zeilenhöhe `1.0`,
  unabhängig von `Font.family`/`Font.size` (werden ignoriert, siehe IP-03).
* Alignment (`LEFT`/`CENTER`/`RIGHT`/`JUSTIFY`) wird über Leerzeichen-Padding auf Basis der
  ermittelten Spaltenbreite umgesetzt.

## Testentwurf

* `testing`-Skill vor Anlegen der Testklasse laden; Paketspiegelung; Entwicklertest ohne
  `IT`-Suffix.
* `emptyDocumentRendersEmptyString` — leeres Dokument liefert leeren Text.
* `singlePageRendersWithinFixedAreaSize` — `Fixed(columns, rows)` erzeugt Zeilen exakt dieser Breite.
* `alignmentLeftCenterRightProduceCorrectPadding` — Padding je Ausrichtung geprüft.
* `marginOverrideAppliesGivenMargins` — `Override(Margins)` überschreibt Dokument-Margins sichtbar.
* `marginFactorScalesDocumentMargins` — `Factor(0.5)` halbiert die Innenabstände.
* `paginationModePagingInsertsPageSeparator` — `PAGING` fügt Seitentrennung ein.
* `paginationModeScrollingProducesContinuousText` — `SCROLLING` liefert keine Seitentrennung.
* `lineSpacingRoundsToWholeBlankLines` — `lineSpacing`-Faktor rundet auf ganzzahlige Leerzeilen.

## Aufgaben

### Aufgabe 1 — Gradle-Modul

* `settings.gradle.kts` um `include(":ui:console")` ergänzen.
* `ui/console/build.gradle.kts` nach Vorbild `engine/build.gradle.kts` anlegen.
* `.gitkeep`-Dateien unter `ui/console` per `git rm` entfernen, sobald echte Dateien existieren.

### Aufgabe 2 — Konfiguration und Einstiegspunkt

* `ConsolePaginationMode`, `ConsoleMarginMode`, `ConsoleAreaSize` als Typen anlegen.
* `ConsoleRenderConfiguration` mit allen Konfigurationsfeldern und KDoc anlegen.
* `DocumentAnsiRenderer` mit `of`-Factory, `renderDocument`, `renderPage`, `Appendable`-Varianten anlegen.

### Aufgabe 3 — Interne Rendering-Pipeline

* `ConsoleFontMeasureCalculator` mit fixen Zellmaßen implementieren.
* `AnsiDocumentRenderer` mit Layout, Padding-Alignment, Seitentrennung/Scrolling ohne SGR implementieren.
* Zeilenabstand-Rundung inklusive `lineSpacingFactor` implementieren.

### Aufgabe 4 — Tests, Build, Abschluss

* `testing`-Skill laden.
* `DocumentAnsiRendererTest` gemäß Abschnitt „Testentwurf" anlegen.
* Build von `:ui:console` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-02 im Status `COMPLETED`, IP-02 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Exakte Signatur des injizierbaren Größen-Lieferanten wird mit IP-04 abgestimmt, um spätere
  Bruchänderungen zu vermeiden.
* Verhalten bei `Fixed(columns, rows)` kleiner als die Dokument-Mindestbreite (z. B. durch
  Margins) ist zu definieren (Fehler vs. Clipping) — wird in Aufgabe 2 entschieden und dokumentiert.
