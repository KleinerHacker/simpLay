# FP-003 / IP-04: Terminal-Erkennung & Fallback

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* Erkennung der Terminalgröße (Spalten/Zeilen) je Kotlin-Multiplatform-Target.
* Erkennung, ob ANSI/Italic unterstützt wird und ob die Ausgabe an ein TTY geht.
* Klartext-Fallback, wenn keine ANSI-Unterstützung vorliegt oder die Ausgabe umgeleitet wird.

## Umfang

### Enthalten

* `expect`/`actual`-Funktionen für Terminalgröße und TTY-/ANSI-Erkennung (jvm, js, native).
* Auswertung von `NO_COLOR` und `TERM` als Umgebungsvariablen.
* Fallback-Größe, falls Erkennung fehlschlägt (z. B. Umleitung in eine Datei).
* Verdrahtung der automatischen Größen-Erkennung mit dem in IP-02 vorgesehenen
  Größen-Lieferanten in `ConsoleRenderConfiguration`.

### Nicht enthalten

* SGR-Mapping selbst (IP-03).
* Finale Verdrahtung von Fähigkeits-Erkennung und Styling-Engine zu einer Gesamt-API (IP-05).

## Abhängigkeiten

* IP-02 (benötigt den injizierbaren Größen-Lieferanten und die Konfigurationsoberfläche).

## Schnittstellen zu anderen Plänen

* Liefert Größen-/Fähigkeits-Erkennung, die IP-05 mit der Styling-Engine aus IP-03 zusammenführt.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/internal/TerminalCapabilities.kt` | Neu: `expect`-Deklarationen für Größe, TTY-Status, ANSI-Unterstützung. |
| `ui/console/src/jvmMain/kotlin/org/pcsoft/framework/simplay/console/internal/TerminalCapabilities.jvm.kt` | Neu: `actual`-Implementierung (jvm) über Umgebungsvariablen/`System.console()`. |
| `ui/console/src/jsMain/kotlin/org/pcsoft/framework/simplay/console/internal/TerminalCapabilities.js.kt` | Neu: `actual`-Implementierung (js) über Node-`process`. |
| `ui/console/src/<nativeTarget>Main/kotlin/org/pcsoft/framework/simplay/console/internal/TerminalCapabilities.<target>.kt` | Neu: `actual`-Implementierung für das jeweils buildbare Native-Target. |
| `ui/console/src/commonMain/kotlin/org/pcsoft/framework/simplay/console/ConsoleRenderConfiguration.kt` | Verdrahtung: `ConsoleAreaSize.Automatic` nutzt `TerminalCapabilities`. |
| `ui/console/src/jvmTest/kotlin/org/pcsoft/framework/simplay/console/internal/TerminalCapabilitiesJvmTest.kt` | Neu: Tests für die jvm-Implementierung. |

## Entwurf

* `TerminalCapabilities` als `expect object` mit `columns(): Int?`, `rows(): Int?`,
  `isTty(): Boolean`, `supportsAnsi(): Boolean`, `supportsItalic(): Boolean` (best effort; ohne
  belastbares Terminfo je Target wird `supportsItalic` konservativ an `supportsAnsi` gekoppelt).
* jvm-Implementierung: Umgebungsvariablen `COLUMNS`/`LINES` bzw. `stty size` als Fallback,
  `NO_COLOR` deaktiviert ANSI vollständig, `TERM=dumb` deaktiviert ANSI, `System.console() != null`
  als TTY-Indikator.
* js-Implementierung: `process.stdout.columns`/`rows`, `process.stdout.isTTY`, `NO_COLOR`-Konvention.
* Native-Implementierung: plattformspezifische Umgebungsvariablen-Auswertung, gleiche Konvention.
* Fällt keine Größe ermittelbar, liefert `ConsoleAreaSize.Automatic` eine dokumentierte
  Fallback-Größe (z. B. 80x24), niemals `null`/Exception.
* Fehlt ANSI-Unterstützung oder ist die Ausgabe kein TTY, liefert `AnsiDocumentRenderer` (IP-03)
  reinen Klartext ohne Escape-Sequenzen — Umschaltung über ein von IP-05 verdrahtetes Flag.

## Testentwurf

* `testing`-Skill vor Anlegen der Testklassen laden.
* `noColorEnvironmentVariableDisablesAnsi` — `NO_COLOR` gesetzt ergibt `supportsAnsi() == false`.
* `dumbTermDisablesAnsi` — `TERM=dumb` ergibt `supportsAnsi() == false`.
* `missingSizeFallsBackToDocumentedDefault` — keine ermittelbare Größe liefert Fallback-Größe.
* `automaticAreaSizeUsesDetectedColumnsAndRows` — `ConsoleAreaSize.Automatic` nutzt erkannte Werte.

## Aufgaben

### Aufgabe 1 — Erkennungs-Schnittstelle

* `TerminalCapabilities` als `expect object` mit den vier Abfragefunktionen anlegen.
* Fallback-Größe und -Verhalten dokumentieren (KDoc).

### Aufgabe 2 — Plattform-Implementierungen

* jvm-`actual`-Implementierung mit Umgebungsvariablen/`System.console()` umsetzen.
* js-`actual`-Implementierung über Node-`process` umsetzen.
* Native-`actual`-Implementierung für das lokal buildbare Target umsetzen.

### Aufgabe 3 — Verdrahtung mit der Konfiguration

* `ConsoleAreaSize.Automatic` an `TerminalCapabilities` anbinden.
* Fallback-Pfad bei fehlender Erkennung testen und dokumentieren.

### Aufgabe 4 — Tests, Build, Abschluss

* `testing`-Skill laden.
* Tests gemäß „Testentwurf" anlegen, jvm-spezifisch in `jvmTest`.
* Build von `:ui:console` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-04 im Status `COMPLETED`, IP-04 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* `supportsItalic()` lässt sich ohne Terminfo-Datenbank nicht zuverlässig erkennen; die
  konservative Kopplung an `supportsAnsi()` wird als Startpunkt dokumentiert, keine echte
  Terminfo-Auswertung im Umfang dieses Plans.
* Welche Native-Targets lokal buildbar sind, hängt vom Build-Host ab (siehe
  `kotlin-multiplatform.gradle.kts`: mingwX64/macosArm64+macosX64/linuxX64 je Betriebssystem);
  nur das jeweils aktive Target wird umgesetzt.
* js-Erkennung funktioniert nur im Node-Kontext (`browser()`-Target hat kein TTY-Konzept) —
  Verhalten im Browser-Target wird als „kein ANSI, Klartext" festgelegt.
