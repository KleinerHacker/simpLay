# FP-003 / IP-03: ANSI-Styling-Engine

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* SGR-Mapping für `FontWeight`, `FontStyle` und `Font.decorations` (Underline/Strikethrough/Dim).
* Festlegung der Anwendbarkeits-Regeln für Font-/TextStyle-Attribute, die auf der Konsole nicht
  übertragbar sind.

## Umfang

### Enthalten

* Internes SGR-Code-Mapping (Objekt `AnsiCodes` o. ä.): Bold=1, Dim=2, Italic=3, Underline=4,
  Strikethrough=9, Reset=0.
* Kombination mehrerer aktiver Attribute in einer Escape-Sequenz sowie korrektes Zurücksetzen
  am Ende eines Textteils/Blocks.
* Anwendung der Regeln: `Font.family`, `Font.size`, `Font.fingerprint` werden ignoriert;
  `TextStyle.lineSpacing` wird (in IP-02 bereits strukturell vorgesehen) auf ganzzahlige
  Leerzeilen gerundet, hier inhaltlich mit dem SGR-Rendering zusammengeführt.
* Integration in `AnsiDocumentRenderer` aus IP-02.

### Nicht enthalten

* Terminal-Fähigkeits-Erkennung/Degradierung bei fehlender Unterstützung (IP-04).
* Farbe.

## Abhängigkeiten

* IP-01 (benötigt `TextDecoration`/`Font.decorations`).
* IP-02 (benötigt `AnsiDocumentRenderer`-Gerüst und `ConsoleFontMeasureCalculator`).

## Schnittstellen zu anderen Plänen

* Konsumiert `Font.decorations` (IP-01) und die Rendering-Pipeline (IP-02).
* Liefert das SGR-Mapping, das IP-04 bei fehlender Terminal-Unterstützung selektiv deaktiviert,
  und das IP-05 final verdrahtet.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/internal/AnsiCodes.kt` | Neu: SGR-Code-Konstanten und Kombinationslogik. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/internal/AnsiDocumentRenderer.kt` | Erweiterung: SGR-Erzeugung je Textteil, Reset-Handling. |
| `ui/console/src/commonTest/kotlin/org/pcsoft/framework/simplay/console/internal/AnsiCodesTest.kt` | Neu: Tests für Code-Mapping und Kombination. |
| `ui/console/src/commonTest/kotlin/org/pcsoft/framework/simplay/console/DocumentAnsiRendererTest.kt` | Erweiterung um Styling-Testfälle. |

## Entwurf

* `AnsiCodes` bildet `FontWeight`/`FontStyle`/`Set<TextDecoration>` auf eine geordnete Liste von
  SGR-Parametern ab; Erzeugung der Sequenz `ESC[<params>m`.
* Attribute werden pro Textteil (`MeasuredTextPart`) ausgewertet; wechselt die Attributkombination
  zwischen zwei Teilen, wird die Sequenz neu ausgegeben, sonst wiederverwendet (keine redundanten
  Escape-Codes je Zeichen).
* Am Ende jeder Zeile bzw. jedes Blocks mit aktivem Styling wird `ESC[0m` ausgegeben, um ein
  „Auslaufen" der Formatierung in Folgezeilen zu verhindern.
* `Font.family`, `Font.size`, `Font.fingerprint` fließen nicht in `AnsiCodes` ein — bewusst
  keine Heuristik (siehe Feature-Plan-Entscheidung).
* `TextStyle.lineSpacing`: `resolvedLineHeight`-Analogon für die Konsole rundet
  `factor`/`extraLeading` auf die nächste ganze Zahl zusätzlicher Leerzeilen, multipliziert mit
  `ConsoleRenderConfiguration.lineSpacingFactor`, minimal `0`.

## Testentwurf

* `testing`-Skill vor Anlegen der Testklassen laden.
* `boldWeightProducesSgrCode1` — `FontWeight.BOLD` erzeugt Code 1.
* `italicStyleProducesSgrCode3` — `FontStyle.ITALIC` erzeugt Code 3.
* `eachDecorationProducesExpectedSgrCode` — Underline=4, Strikethrough=9, Dim=2.
* `combinedAttributesProduceSingleSequence` — Bold+Italic+Underline in einer Sequenz.
* `unchangedAttributesBetweenPartsEmitNoRedundantSequence` — keine doppelte Sequenz bei gleichem Stil.
* `endOfStyledLineEmitsReset` — `ESC[0m` nach formatiertem Text.
* `fontFamilySizeFingerprintDoNotAffectOutput` — Variation dieser Felder ändert die SGR-Ausgabe nicht.
* `lineSpacingRoundsAndAppliesConfiguredFactor` — Rundung plus `lineSpacingFactor` korrekt kombiniert.

## Aufgaben

### Aufgabe 1 — SGR-Mapping

* `AnsiCodes` mit Konstanten und Kombinationsfunktion für Weight/Style/Dekorationen anlegen.
* Reset-Sequenz-Erzeugung implementieren.
* KDoc mit den verwendeten SGR-Nummern ergänzen.

### Aufgabe 2 — Integration in die Rendering-Pipeline

* `AnsiDocumentRenderer` um Sequenz-Erzeugung je Textteil erweitern.
* Redundanzvermeidung bei unveränderter Attributkombination implementieren.
* Zeilenabstand-Rundung mit `lineSpacingFactor` final verdrahten.

### Aufgabe 3 — Tests, Build, Abschluss

* `testing`-Skill laden.
* `AnsiCodesTest` und Erweiterung von `DocumentAnsiRendererTest` gemäß „Testentwurf" anlegen.
* Build von `:ui:console` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-03 im Status `COMPLETED`, IP-03 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Exaktes Rundungsverhalten bei `lineSpacing`-Werten kleiner 1.0 (z. B. `factor = 0.5`) wird in
  Aufgabe 1/2 final festgelegt (Vorschlag: Untergrenze 0 zusätzliche Leerzeilen).
* Reihenfolge der SGR-Parameter in einer kombinierten Sequenz hat keine funktionale Bedeutung,
  wird aber für deterministische Tests fest sortiert.
