# FP-003 / IP-01: Engine-Modell: Textdekorationen

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* Neuer Typ `TextDecoration` (UNDERLINE, STRIKETHROUGH, DIM) im Engine-Modell.
* Neues, optionales Feld `Font.decorations: Set<TextDecoration>` (Default `emptySet()`).
* Abwärtskompatible Serialisierung (JSON/XML/YAML/JVM) für bestehende Dokumente ohne das Feld.

## Umfang

### Enthalten

* Neue Datei `TextDecoration.kt` (Enum, analog zu `FontWeight`/`FontStyle`).
* Neues Feld `decorations` auf `Font`, mit Default-Wert.
* Durchreichen von `decorations` über `MeasuredFont` (analog `weight`/`style`).
* Anpassung aller betroffenen Serialisierungs-Rundtrip-Tests, sofern sie literale Ausgaben prüfen.
* Neue Tests für das Feld selbst (Default, Kombination mehrerer Werte, Rundtrip).

### Nicht enthalten

* Farbe (bewusst ausgeklammert).
* Darstellung der Dekorationen in `ui:fx`, `ui:swing` oder `ui:console` (eigene Pläne: IP-03, IP-06, IP-07).
* Änderungen an `FontWeight`, `FontStyle` oder deren bestehender Bedeutung.

## Abhängigkeiten

* Unabhängig.

## Schnittstellen zu anderen Plänen

* Liefert `TextDecoration` und `Font.decorations` an IP-03 (Console-Styling), IP-06 (FX),
  IP-07 (Swing).

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/TextDecoration.kt` | Neu: Enum `UNDERLINE`, `STRIKETHROUGH`, `DIM`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/Font.kt` | Neues Feld `decorations: Set<TextDecoration> = emptySet()`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/measure/MeasuredFont.kt` | Neue Property `decorations: Set<TextDecoration> get() = raw.decorations`. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/model/SerializationTest.kt` | Testfälle für `decorations` (Default, gesetzt, Rundtrip) ergänzen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/e2e/E2ETestData.kt` | Ggf. Beispiel-`Font` um Dekorationen ergänzen, falls für E2E-Abdeckung sinnvoll. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/e2e/JsonRoundTripTest.kt` | Prüfen/ergänzen, falls literale JSON-Struktur assertiert wird. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/e2e/XmlRoundTripTest.kt` | Prüfen/ergänzen, falls literale XML-Struktur assertiert wird. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/e2e/YamlRoundTripTest.kt` | Prüfen/ergänzen, falls literale YAML-Struktur assertiert wird. |
| `engine/src/jvmTest/kotlin/org/pcsoft/framework/simplay/engine/e2e/JvmSerializationRoundTripTest.kt` | Prüfen/ergänzen, falls JVM-spezifische Serialisierung betroffen ist. |
| `docs/` (Engine-Modellseite) | Nach Laden des `project-docs`-Skills: `TextDecoration`/`Font.decorations` dokumentieren. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

* `TextDecoration` als einfaches Enum ohne Zusatzdaten, wie `FontWeight`/`FontStyle`.
* `Set<TextDecoration>` statt Einzelfeldern, da Dekorationen frei kombinierbar sind (im Gegensatz
  zu `weight`/`style`, die je eine feste Ausprägung haben).
* Default `emptySet()` stellt sicher, dass bestehende `Font`-Konstruktionsaufrufe ohne den neuen
  Parameter weiter kompilieren und bestehende serialisierte Dokumente ohne das Feld weiterhin
  deserialisierbar bleiben (kotlinx.serialization-Default für fehlende Felder).
* `MeasuredFont.decorations` als reine Weiterleitung, analog zu `weight`/`style`, keine
  Metrik-Auswirkung (Dekorationen ändern keine Breiten/Höhen).

## Testentwurf

* `testing`-Skill vor Anlegen/Ändern von Testklassen laden.
* `SerializationTest`: `Font` mit leerer, einzelner und mehrfacher Dekorationsmenge rundtript
  über JSON/XML/YAML/JVM identisch.
* Bestehendes Dokument ohne `decorations`-Feld (altes Serialisierungsformat simuliert) lädt mit
  `decorations == emptySet()`.
* `MeasuredFont.decorations` spiegelt `Font.decorations` unverändert wider.

## Aufgaben

### Aufgabe 1 — Modell

* `TextDecoration.kt` mit Enum-Werten UNDERLINE, STRIKETHROUGH, DIM anlegen.
* `Font.decorations`-Feld mit Default `emptySet()` ergänzen, KDoc anpassen.
* `MeasuredFont.decorations`-Property als Weiterleitung ergänzen.

### Aufgabe 2 — Serialisierung und Tests

* `testing`-Skill laden.
* Bestehende Rundtrip-Tests (JSON/XML/YAML/JVM) auf literale Assertions prüfen, ggf. anpassen.
* Neue Testfälle für `decorations` in `SerializationTest` ergänzen.
* Abwärtskompatibilitäts-Testfall für Dokumente ohne das neue Feld ergänzen.

### Aufgabe 3 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; Engine-Modellseite um `TextDecoration`/`Font.decorations` ergänzen.
* `CHANGELOG.md`-Eintrag ergänzen.
* Build von `:engine` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-01 im Status `COMPLETED`, IP-01 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, `FP-003-Overview.md` unverändert lassen (Plan bleibt
  bis zum letzten Plan bestehen), diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Ob `Font.decorations` oder ein neues Feld auf `TextStyle` der richtige Ort ist; Entscheidung
  zugunsten `Font` getroffen, da `weight`/`style` als Präzedenzfall dort liegen.
* Literale Serialisierungs-Assertions in bestehenden Tests sind vor Umsetzung nicht abschließend
  bekannt; Umfang von Aufgabe 2 kann sich dadurch geringfügig verschieben.
