# Feature Plan: ANSI-Konsolen-Styling

## 1. Ziel

Bereitstellung eines neuen UI-Moduls `ui:console`, das ein `Document` als reinen Text mit
ANSI-Escape-Sequenzen (SGR) auf einem Terminal ausgibt. Im Gegensatz zu `ui:fx` und `ui:swing` gibt
es keinen Editier-Modus - die Konsole ist reine Ausgabe. Zusätzlich wird das gemeinsame
Engine-Modell um Textdekorationen erweitert, die von allen UI-Modulen genutzt werden können.

## 2. Ist-Zustand

* `ui:console` ist ein leeres Kotlin-Multiplatform-Gerüst (`commonMain`/`commonTest`, nur
  `.gitkeep`-Dateien), noch nicht in `settings.gradle.kts` eingebunden.
* `ui:fx` (`CanvasDocumentRenderer`) und `ui:swing` (`DocumentImageRenderer`) folgen beide dem
  gleichen Muster: privater Konstruktor + `of(document) { configurator }`-Factory, sofortiges
  Messen über eine plattformeigene `FontMeasureCalculator`, Rendern über ein internes
  `*DocumentRenderer`-Objekt.
* Das Engine-Modell (`engine.model.Font`) kennt nur `FontWeight` (NORMAL/BOLD) und `FontStyle`
  (NORMAL/ITALIC). Es gibt keine Textdekorationen (Unterstrichen, Durchgestrichen, Dim) und keine
  Farbe.
* `PageLayout` trägt bereits `size` und `margins`; `TextStyle` trägt `lineSpacing` (Faktor +
  zusätzlicher Vorschub) und `alignment`.

## 3. Soll-Zustand

* Das Engine-Modell besitzt eine neue, optionale Dekorationsmenge auf `Font`
  (Unterstrichen, Durchgestrichen, Dim), abwärtskompatibel serialisierbar (JSON/XML/YAML/JVM).
* `ui:console` ist als Gradle-Modul eingebunden und bietet einen öffentlichen Einstiegspunkt
  `DocumentAnsiRenderer`, der ein `Document` in ANSI-formatierten Text umwandelt.
* Der Einstiegspunkt erlaubt Konfiguration von: Flächengröße (automatische Terminal-Erkennung
  oder explizite Zeichen-/Zeilenzahl), Margin-Handling (übernehmen / überschreiben / mit Faktor
  skalieren), Zeilenabstand-Rundung (mit optionalem Faktor) und Paginierungsmodus
  (`SCROLLING` vs. `PAGING`).
* `FontWeight`/`FontStyle` sowie die neuen Dekorationen werden auf ANSI-SGR-Codes abgebildet;
  `Font.family`, `Font.size` und `Font.fingerprint` werden von der Console-Ausgabe vollständig
  ignoriert.
* Die Ausgabe degradiert automatisch, wenn das Zielterminal ANSI/Farbe/Italic nicht unterstützt
  oder die Ausgabe nicht an ein TTY geht (Klartext-Fallback).
* `ui:fx` und `ui:swing` stellen die neuen Dekorationen ebenfalls dar (jeweils eigener,
  nachgelagerter Implementierungsplan).

## 4. Anforderungen

### Funktionale Anforderungen

* `DocumentAnsiRenderer.of(document) { ... }` liefert Text für das ganze Dokument oder eine
  einzelne Seite.
* `FontWeight.BOLD`, `FontStyle.ITALIC` sowie Unterstrichen/Durchgestrichen/Dim werden als
  ANSI-SGR-Sequenzen ausgegeben, kombinierbar und korrekt zurückgesetzt.
* `TextStyle.alignment` wird über Padding umgesetzt; `TextStyle.lineSpacing` wird auf
  ganzzahlige Leerzeilen gerundet, zusätzlich skalierbar über einen konfigurierbaren Faktor.
* Terminalgröße wird automatisch erkannt, sofern nicht explizit überschrieben.
* Paginierung ist wählbar zwischen fortlaufendem Scrollen und Seiten mit Trennung.
* Nicht unterstützte Fähigkeiten (kein TTY, kein ANSI, kein Italic) führen zu Klartext- bzw.
  reduzierter Ausgabe statt zu fehlerhaften Escape-Sequenzen.

### Technische Anforderungen

* Kotlin, Gradle, `ui:console` als Kotlin-Multiplatform-Modul (jvm/js/native), analog zu `engine`.
* Wiederverwendung von `Document`, `measure(...)`, `MeasuredDocument`, `MeasuredPage`,
  `PageLayout`, `TextStyle`, `Font` aus `engine`.
* Keine neue Abhängigkeit ohne Rückfrage beim Nutzer.
* Bestehende Serialisierungs-Rundtrips (JSON/XML/YAML/JVM) bleiben für Dokumente ohne die neuen
  Dekorationsfelder unverändert kompatibel (Default-Werte).

## 5. Architektur

* Neues Gradle-Modul `ui:console`, eingebunden in `settings.gradle.kts`, mit `commonMain`/
  `commonTest` Quellsätzen (Kotlin-Multiplatform-Konvention wie `engine`).
* Öffentlicher Einstiegspunkt `DocumentAnsiRenderer` im Package
  `org.pcsoft.framework.simplay.console`, analog zu `CanvasDocumentRenderer` (fx) /
  `DocumentImageRenderer` (swing).
* `ConsoleRenderConfiguration` als Konfigurationsobjekt (Flächengröße, Margins, Zeilenabstand,
  Paginierungsmodus), analog zu `CanvasRenderConfiguration` / `ImageRenderConfiguration`.
* Interne `ConsoleFontMeasureCalculator` (feste Zellenbreiten/-höhen statt Font-Metriken) und
  internes `AnsiDocumentRenderer`-Objekt (Text-/SGR-Erzeugung), analog zu
  `Graphics2DDocumentRenderer`.
* Eigenständige Terminal-Erkennung (Größe, ANSI-/Italic-Fähigkeit, TTY) als internes Utility,
  je Kotlin-Multiplatform-Target unterschiedlich implementiert (`expect`/`actual`).
* Engine-Modellerweiterung: neue Dekorationsmenge auf `engine.model.Font`, durchgereicht über
  `measure.MeasuredFont`.
* `ui:fx`/`ui:swing` erhalten in eigenen, nachgelagerten Plänen die Darstellung der neuen
  Dekorationen in ihren jeweiligen internen Renderern.

## 6. Übersicht der Implementierungspläne

| ID    | Implementierungsplan                                   | Ziel                                                                                     | Abhängigkeiten |
| ----- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------- | -------------- |
| IP-01 | Engine-Modell: Textdekorationen                          | Unterstrichen/Durchgestrichen/Dim im Engine-Modell, serialisierbar, abwärtskompatibel.     | -              |
| IP-02 | Console-Grundgerüst & Rendering-Pipeline                 | Gradle-Modul, `DocumentAnsiRenderer`-Einstiegspunkt, Konfiguration, Klartext-Rendering.    | -              |
| IP-03 | ANSI-Styling-Engine                                       | SGR-Mapping für Weight/Style/Dekorationen, Anwendbarkeits-Regeln für Font/TextStyle.       | IP-01, IP-02   |
| IP-04 | Terminal-Erkennung & Fallback                             | Größen-/ANSI-Fähigkeits-/TTY-Erkennung je Target, Klartext-Fallback.                       | IP-02          |
| IP-05 | Integration & öffentliche Console-API                    | Zusammenführen von Styling-Engine und Terminal-Erkennung, Demo, Tests.                     | IP-03, IP-04   |
| IP-06 | FX-Anpassung: Darstellung der neuen Dekorationen          | `ui:fx`-Renderer stellt Unterstrichen/Durchgestrichen/Dim dar.                             | IP-01          |
| IP-07 | Swing-Anpassung: Darstellung der neuen Dekorationen       | `ui:swing`-Renderer stellt Unterstrichen/Durchgestrichen/Dim dar.                          | IP-01          |

## 7. Implementierungspläne

### IP-01: Engine-Modell: Textdekorationen

**Ziel**

Ergänzt das gemeinsame Engine-Modell um eine optionale Menge von Textdekorationen
(Unterstrichen, Durchgestrichen, Dim) auf `Font`, abwärtskompatibel serialisierbar.

**Umfang**

* Enthalten: neuer Typ `TextDecoration`, neues Feld auf `Font`, Durchreichen über `MeasuredFont`,
  Anpassung aller Serialisierungs-Rundtrips.
* Nicht enthalten: Farbe, Darstellung in `ui:fx`/`ui:swing`/`ui:console` (eigene Pläne).

**Abhängigkeiten**

* Unabhängig.

**Schnittstellen zu anderen Plänen**

* Liefert `TextDecoration` und `Font.decorations` an IP-03 (Console), IP-06 (FX), IP-07 (Swing).

### IP-02: Console-Grundgerüst & Rendering-Pipeline

**Ziel**

Legt das Gradle-Modul `ui:console` an und stellt den öffentlichen Einstiegspunkt
`DocumentAnsiRenderer` mit Klartext-Rendering (ohne ANSI-Styling) und der vollständigen
Konfigurationsoberfläche bereit.

**Umfang**

* Enthalten: Gradle-Einbindung, `DocumentAnsiRenderer` (`of`-Factory, `renderDocument`,
  `renderPage`), `ConsoleRenderConfiguration` (Flächengröße, Margin-Handling, Zeilenabstand,
  Paginierungsmodus), `ConsoleFontMeasureCalculator`, internes `AnsiDocumentRenderer`-Gerüst ohne
  SGR-Codes, Alignment als Padding.
* Nicht enthalten: SGR-Erzeugung (IP-03), Terminal-Erkennung (IP-04).

**Abhängigkeiten**

* Unabhängig.

**Schnittstellen zu anderen Plänen**

* Liefert `DocumentAnsiRenderer`, `ConsoleRenderConfiguration` und die Renderer-Pipeline als Basis
  für IP-03, IP-04, IP-05.

### IP-03: ANSI-Styling-Engine

**Ziel**

Bildet `FontWeight`, `FontStyle` und die neuen Textdekorationen auf ANSI-SGR-Sequenzen ab und legt
die Anwendbarkeits-Regeln für nicht übertragbare Font-/TextStyle-Attribute fest.

**Umfang**

* Enthalten: SGR-Mapping (Bold, Italic, Underline, Strikethrough, Dim), Reset-Handling,
  Kombination mehrerer Attribute, Regeln: `Font.family`/`Font.size`/`Font.fingerprint` werden
  ignoriert, `TextStyle.lineSpacing` wird auf Leerzeilen gerundet (mit konfigurierbarem Faktor).
* Nicht enthalten: Terminal-Fähigkeits-Erkennung/Degradierung (IP-04), Farbe.

**Abhängigkeiten**

* IP-01 (benötigt `TextDecoration`), IP-02 (benötigt Renderer-Pipeline).

**Schnittstellen zu anderen Plänen**

* Konsumiert `Font.decorations` aus IP-01 und die Renderer-Pipeline aus IP-02.
* Liefert das SGR-Mapping, das IP-04 bei fehlender Terminal-Unterstützung reduziert.

### IP-04: Terminal-Erkennung & Fallback

**Ziel**

Erkennt Terminalgröße, ANSI-/Italic-Fähigkeit und TTY-Status je Kotlin-Multiplatform-Target und
stellt einen Klartext-Fallback bereit.

**Umfang**

* Enthalten: Größen-Erkennung (Spalten/Zeilen), `NO_COLOR`/`TERM`-Auswertung, TTY-Erkennung,
  `expect`/`actual`-Implementierungen je Target, Klartext-Fallback bei Redirect/fehlendem TTY.
* Nicht enthalten: SGR-Mapping selbst (IP-03).

**Abhängigkeiten**

* IP-02 (benötigt Konfigurationsoberfläche für automatische Größe).

**Schnittstellen zu anderen Plänen**

* Liefert die Fähigkeits-/Größen-Erkennung, die IP-05 mit der Styling-Engine aus IP-03 verbindet.

### IP-05: Integration & öffentliche Console-API

**Ziel**

Verbindet Styling-Engine (IP-03) und Terminal-Erkennung (IP-04) zur vollständigen öffentlichen
Console-API, inklusive Demo und Gesamttests.

**Umfang**

* Enthalten: Verdrahtung in `DocumentAnsiRenderer`/`ConsoleRenderConfiguration`, Demo-Anwendung,
  End-to-End-Tests über Konfigurationskombinationen.
* Nicht enthalten: neue fachliche Funktionalität über IP-01 bis IP-04 hinaus.

**Abhängigkeiten**

* IP-03, IP-04.

**Schnittstellen zu anderen Plänen**

* Konsumiert die Ergebnisse von IP-03 und IP-04; liefert das fertige öffentliche `ui:console`-API.

### IP-06: FX-Anpassung: Darstellung der neuen Dekorationen

**Ziel**

Stellt Unterstrichen, Durchgestrichen und Dim aus dem erweiterten Engine-Modell im
`ui:fx`-Renderer dar.

**Umfang**

* Enthalten: Anpassung von `Graphics2DDocumentRenderer`/`CanvasDocumentRenderer`-Umfeld zur
  Darstellung der neuen Dekorationen, Tests.
* Nicht enthalten: neue Engine-Modelländerungen, Farbe.

**Abhängigkeiten**

* IP-01. Wird gemäß Nutzervorgabe erst nach Abschluss von IP-01 bis IP-05 eingeplant.

**Schnittstellen zu anderen Plänen**

* Konsumiert `Font.decorations` aus IP-01.

### IP-07: Swing-Anpassung: Darstellung der neuen Dekorationen

**Ziel**

Stellt Unterstrichen, Durchgestrichen und Dim aus dem erweiterten Engine-Modell im
`ui:swing`-Renderer dar.

**Umfang**

* Enthalten: Anpassung von `Graphics2DDocumentRenderer`/`DocumentImageRenderer`-Umfeld zur
  Darstellung der neuen Dekorationen, Tests.
* Nicht enthalten: neue Engine-Modelländerungen, Farbe.

**Abhängigkeiten**

* IP-01. Wird gemäß Nutzervorgabe erst nach Abschluss von IP-01 bis IP-05 eingeplant.

**Schnittstellen zu anderen Plänen**

* Konsumiert `Font.decorations` aus IP-01.

## 8. Abhängigkeitsgraph

```text
IP-01
├── IP-03
│   └── IP-05
├── IP-06
└── IP-07
IP-02
├── IP-03
└── IP-04
        └── IP-05
```

## 9. Risiken und offene Fragen

* Terminal-Fähigkeits-Erkennung ist je Kotlin-Multiplatform-Target unterschiedlich zu
  implementieren (jvm: `System.console()`/`stty`, js: Node-`process`, native: Plattform-API);
  Umfang und Grenzen werden in IP-04 detailliert.
* Serialisierungs-Rundtrips mit literal geprüfter Ausgabe (XML/YAML) müssen ggf. um das neue Feld
  ergänzt werden, ohne bestehende Assertions zu brechen.
* Genaues Runden von `lineSpacing` auf Leerzeilen (z. B. bei Faktor < 1.0) wird in IP-03 final
  festgelegt.
* Reihenfolge von IP-06/IP-07 ist bewusst ans Ende gestellt; technisch wären beide bereits nach
  IP-01 startbar.

## 10. Abschlusskriterien

* `./gradlew build` ist für den gesamten Workspace grün, inklusive des neuen Moduls `ui:console`.
* `DocumentAnsiRenderer` erzeugt für ein Beispieldokument korrekt formatierten ANSI-Text und einen
  korrekten Klartext-Fallback bei fehlender Terminal-Unterstützung.
* Automatische Terminalgrößen-Erkennung, Margin-Handling und beide Paginierungsmodi sind nutzbar
  und getestet.
* Bestehende Dokumente ohne die neuen Dekorationsfelder laden weiterhin unverändert
  (Abwärtskompatibilität der Serialisierung).
* `ui:fx` und `ui:swing` stellen die neuen Dekorationen sichtbar dar.
