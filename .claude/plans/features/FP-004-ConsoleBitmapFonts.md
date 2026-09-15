# Feature Plan: Konsolen-Bitmap-Fonts

## 1. Ziel

Die Konsolen-Ausgabe (`ui:console`, siehe FP-003) erhält eigene, mitgelieferte Bitmap-Fonts im
FLF-Format (FIGlet Font Format), die aus typischen Konsolenzeichen bestehen und in Textform
(ASCII-Kunst über mehrere Terminal-Zellen) dargestellt werden. `Font.family` aus dem Engine-Modell
wird dazu nicht mehr ignoriert, sondern auf einen registrierten FLF-Font abgebildet. Zusätzlich zu
den mitgelieferten Fonts können eigene FLF-Font-Dateien von außen nachgeladen werden. `Font.size`
bleibt für die Konsolen-Ausgabe weiterhin ohne Wirkung - jedes Zeichen wird in genau dem festen
Raster dargestellt, das der jeweilige FLF-Font vorgibt.

## 2. Ist-Zustand

* `ui:console` existiert laut FP-003 als Modul mit `DocumentAnsiRenderer`, das reinen
  Klartext/ANSI-Text ausgibt. `Font.family`, `Font.size` und `Font.fingerprint` werden dort
  bewusst ignoriert (siehe FP-003, Abschnitt 3).
* `engine.model.Font` liefert `family: String` und `size: Double` (einheitenlos) als reine
  Datenfelder; die Interpretation beider Felder liegt vollständig beim jeweiligen
  `FontMeasureCalculator` der aufrufenden UI (`fx`, `swing`).
* `FontMeasureCalculator` (engine) ist eine reine Callback-Schnittstelle: `measure(font, text)`
  liefert `TextMetrics` (Breite, Ascent, Descent) für ein `(Font, Text)`-Paar; jedes UI-Modul
  implementiert sie eigenständig (`FxFontMeasureCalculator`, `SwingFontMeasureCalculator`, laut
  FP-003 künftig auch `ConsoleFontMeasureCalculator`).
* Es existiert kein Mechanismus, Fonts als Ressourcen mitzuliefern oder zur Laufzeit aus einer
  externen Datei nachzuladen; `fx`/`swing` nutzen ausschließlich im System installierte Fonts
  (`FxFontProbe`, `SwingFontProbe`).
* Es gibt weder ein Bitmap-Font-Format noch einen FLF-Parser im Projekt.

## 3. Soll-Zustand

* Ein selbst geschriebener FLF-Parser liest Fonts im FIGlet-Font-Format (`.flf`) ein: Header
  (Hardblank-Zeichen, Zeilenhöhe, Baseline, max. Zeichenbreite, Layout-Flags), Kommentarblock und
  je Zeichen einen Block aus Textzeilen mit End-Markierung.
* `ui:console` liefert einen mitgelieferten Satz eingebetteter FLF-Fonts, die die für eine Konsole
  typischen Zeichen abdecken (Latin-Basis, Ziffern, gängige Satzzeichen). Darunter befindet sich
  zwingend ein selbst erstellter FLF-Font (kein Drittanbieter-Font), der als Standard-Font dient.
* Ein Font-Registry-Mechanismus löst `Font.family` auf einen registrierten FLF-Font auf,
  inklusive Fallback-Verhalten bei unbekannter Family; der Fallback ist genau dieser selbst
  erstellte Standard-Font.
* `Font.size` bleibt für die Konsolen-Ausgabe wirkungslos (wie schon in FP-003 festgelegt); die
  Glyphenmaße ergeben sich ausschließlich aus dem FLF-Font selbst (Zeilenhöhe, Zeichenbreite).
* Eigene FLF-Font-Dateien können zur Laufzeit von außen geladen und unter einer Family registriert
  werden, gleichwertig zu den mitgelieferten Fonts.
* `DocumentAnsiRenderer`/`ConsoleRenderConfiguration` (FP-003) nutzen die FLF-Font-Auflösung für
  das Rendering; ein Zeichen wird als mehrzeiliges Glyphen-Raster statt als einzelnes Textzeichen
  ausgegeben.

## 4. Anforderungen

### Funktionale Anforderungen

* Ein Zeichensatz an mitgelieferten FLF-Fonts steht ohne weitere Konfiguration zur Verfügung,
  darunter zwingend ein selbst erstellter Standard-Font.
* `Font.family` wählt einen registrierten FLF-Font aus; unbekannte Familien fallen auf den selbst
  erstellten Standard-Font zurück statt einen Fehler zu werfen.
* `Font.size` hat auf die Konsolen-Ausgabe keine Wirkung; jeder Font wird stets in seinem eigenen,
  im FLF-Header definierten Raster dargestellt.
* Eine externe FLF-Font-Datei kann zur Laufzeit geladen und unter einer selbstgewählten Family
  registriert werden.
* Ein Zeichen, das im gewählten Font nicht abgedeckt ist, erhält ein definiertes
  Ersatz-Raster statt die Ausgabe abzubrechen.

### Technische Anforderungen

* Kotlin, Gradle, Umsetzung ausschließlich innerhalb `ui:console` sowie dem dortigen
  `FontMeasureCalculator` (`engine` selbst bleibt unverändert - `Font.family` ist bereits ein
  generisches Feld, `Font.size` bleibt dort wie in FP-003 unberücksichtigt).
* Keine neue Abhängigkeit ohne Rückfrage beim Nutzer; der FLF-Parser wird als reiner, selbst
  geschriebener Text-Parser umgesetzt (FLF ist ein offenes, textbasiertes Format, keine
  Bibliothek nötig).
* Mitgelieferte Fonts werden als `.flf`-Modul-Ressourcen ausgeliefert. Der Standard-/Fallback-Font
  ist zwingend eine selbst erstellte `.flf`-Datei (kein Drittanbieter-Font, keine Lizenzfrage);
  darüber hinausgehende mitgelieferte Fonts werden vor Aufnahme lizenzrechtlich geprüft.
* Baut auf der Rendering-Pipeline aus FP-003 (IP-02/IP-03) auf, ohne deren SGR-Styling-Logik zu
  verändern.

## 5. Architektur

* Neues FLF-Parser-/Modell-Paket in `ui:console`: Glyphen-Raster je Zeichen (Zeilen eines
  FLF-Zeichenblocks ohne End-Markierungen und Hardblank-Ersetzung), Font-Metadaten aus dem
  FLF-Header (Zeilenhöhe, Baseline, max. Zeichenbreite, Family-Name).
* Font-Registry innerhalb `ui:console`, die mitgelieferte und extern geladene FLF-Fonts unter
  ihrem Family-Namen verwaltet und aus einer Family ein aufgelöstes Glyphen-Raster liefert.
* Ressourcen-Verzeichnis in `ui:console` für die mitgelieferten `.flf`-Dateien.
* Loader-Komponente zum Einlesen einer externen `.flf`-Datei in dasselbe Modell wie die
  mitgelieferten Fonts.
* Erweiterung/Ablösung der in FP-003 (IP-02) skizzierten `ConsoleFontMeasureCalculator`, sodass
  Zellenmaße aus dem aufgelösten FLF-Font statt aus einer festen Konstante stammen.
* Anpassung des internen `AnsiDocumentRenderer` (FP-003) an eine mehrzeilige Glyphen-Ausgabe pro
  Textzeile.

## 6. Übersicht der Implementierungspläne

| ID    | Implementierungsplan                         | Ziel                                                                                  | Abhängigkeiten     |
| ----- | --------------------------------------------- | -------------------------------------------------------------------------------------- | ------------------- |
| IP-01 | FLF-Format-Parser & Modell                    | FLF-Parser (`.flf`), Glyphen-/Font-Modellklassen, Validierungslogik.                     | -                    |
| IP-02 | Mitgelieferter Font-Satz                      | Eingebettete FLF-Fonts für typische Konsolenzeichen als Modul-Ressourcen.                | IP-01                |
| IP-03 | Externes Nachladen eigener Fonts              | Laufzeit-Loader für externe `.flf`-Dateien, Registrierung unter eigener Family.          | IP-01                |
| IP-04 | Glyphen-Rendering-Integration                 | Zellenmaße aus dem Font, mehrzeilige Glyphen-Ausgabe im Renderer, Ersatz-Raster.         | IP-01, FP-003 IP-02  |
| IP-05 | Font-Registry-Integration & öffentliche API   | Family-Auflösung inkl. Fallback, Verdrahtung in `DocumentAnsiRenderer`, Demo, Tests.      | IP-02, IP-03, IP-04  |

## 7. Implementierungspläne

### IP-01: FLF-Format-Parser & Modell

**Ziel**

Implementiert einen selbst geschriebenen Parser für das FLF-Format (FIGlet Font Format) sowie die
Modellklassen für Glyphen und Fonts, inklusive Validierungslogik.

**Umfang**

* Enthalten: FLF-Header-Parsing (Hardblank, Zeilenhöhe, Baseline, max. Zeichenbreite,
  Layout-Flags, Kommentarblock), Glyphen-Modell (Zeichenblock je Zeichen, Hardblank-Ersetzung,
  End-Markierungs-Handling), Font-Modell (Family, Header-Metadaten, Glyphen-Menge), Validierung
  (Vollständigkeit, konsistente Zeilenzahl je Zeichenblock).
* Nicht enthalten: mitgelieferte Font-Inhalte (IP-02), externes Laden (IP-03), Rendering-Integration
  (IP-04).

**Abhängigkeiten**

* Unabhängig.

**Schnittstellen zu anderen Plänen**

* Liefert das Font-/Glyphen-Modell und den FLF-Parser als Basis für IP-02, IP-03 und IP-04.

### IP-02: Mitgelieferter Font-Satz

**Ziel**

Stellt einen eingebetteten Standard-Font-Satz aus FLF-Fonts bereit, der die für eine Konsole
typischen Zeichen (Latin-Basis, Ziffern, gängige Satzzeichen) abdeckt - inklusive eines selbst
erstellten Standard-/Fallback-Fonts.

**Umfang**

* Enthalten: Erstellung eines eigenen FLF-Standard-Fonts (Basis-Zeichensatz, keine
  Drittanbieter-Herkunft), optional Aufnahme weiterer geeigneter `.flf`-Fonts als Modul-Ressourcen
  (inkl. Lizenzprüfung), Laden der mitgelieferten Fonts beim Modul-Start, Tests gegen den Parser
  aus IP-01.
* Nicht enthalten: externes Nachladen (IP-03), Family-Auflösung/Fallback-Verdrahtung (IP-05).

**Abhängigkeiten**

* IP-01 (benötigt FLF-Modell/Parser).

**Schnittstellen zu anderen Plänen**

* Liefert den Satz mitgelieferter Fonts, den IP-05 in die Registry einträgt.

### IP-03: Externes Nachladen eigener Fonts

**Ziel**

Ermöglicht das Laden einer eigenen `.flf`-Font-Datei zur Laufzeit und deren Registrierung unter
einer selbstgewählten Family, gleichwertig zu den mitgelieferten Fonts.

**Umfang**

* Enthalten: Loader-API für externe `.flf`-Dateien, Fehlerbehandlung bei ungültigem/fehlendem
  Font, Tests mit selbst erzeugten Beispiel-Dateien.
* Nicht enthalten: FLF-Parsing selbst (IP-01), Family-Auflösung/Fallback (IP-05).

**Abhängigkeiten**

* IP-01 (benötigt FLF-Modell/Parser).

**Schnittstellen zu anderen Plänen**

* Liefert den externen Loader, den IP-05 neben den mitgelieferten Fonts in die Registry einträgt.

### IP-04: Glyphen-Rendering-Integration

**Ziel**

Integriert die feste Glyphen-Darstellung eines FLF-Fonts als mehrzeilige Ausgabe in die
bestehende Konsolen-Rendering-Pipeline aus FP-003.

**Umfang**

* Enthalten: Ableitung der Zellenmaße für die `ConsoleFontMeasureCalculator` aus FP-003 (IP-02)
  aus dem FLF-Header des gewählten Fonts, Anpassung des `AnsiDocumentRenderer` auf mehrzeilige
  Glyphen-Ausgabe je Textzeile, Ersatz-Raster für nicht abgedeckte Zeichen.
* Nicht enthalten: Family-Auflösung/Fallback auf Registry-Ebene (IP-05), Font-Inhalte selbst
  (IP-01/IP-02/IP-03), jegliche Größen-Skalierung (`Font.size` bleibt wirkungslos).

**Abhängigkeiten**

* IP-01 (benötigt Glyphen-Modell), FP-003 IP-02 (benötigt bestehende Rendering-Pipeline).

**Schnittstellen zu anderen Plänen**

* Konsumiert das Glyphen-Modell aus IP-01 sowie die Rendering-Pipeline aus FP-003 IP-02.
* Liefert die Glyphen-Ausgabe, die IP-05 über die Registry-Auflösung anspricht.

### IP-05: Font-Registry-Integration & öffentliche API

**Ziel**

Verbindet mitgelieferte Fonts (IP-02), externes Nachladen (IP-03) und Glyphen-Rendering (IP-04) zu
einer Family-Auflösung mit Fallback und verdrahtet sie in die öffentliche `ui:console`-API.

**Umfang**

* Enthalten: Font-Registry (Family → Font), Fallback-Regel bei unbekannter Family (auf den selbst
  erstellten Standard-Font aus IP-02), Verdrahtung in
  `DocumentAnsiRenderer`/`ConsoleRenderConfiguration`, Demo, End-to-End-Tests.
* Nicht enthalten: neue fachliche Funktionalität über IP-01 bis IP-04 hinaus.

**Abhängigkeiten**

* IP-02, IP-03, IP-04.

**Schnittstellen zu anderen Plänen**

* Konsumiert die Ergebnisse von IP-02, IP-03 und IP-04; liefert die fertige öffentliche
  Font-Auflösung für `ui:console`.

## 8. Abhängigkeitsgraph

```text
IP-01
├── IP-02
│   └── IP-05
├── IP-03
│   └── IP-05
└── IP-04 (zusätzlich: FP-003 IP-02)
    └── IP-05
```

## 9. Risiken und offene Fragen

* Da `Font.size` keine Wirkung hat, ist die visuelle Größe eines Zeichens allein über die Wahl der
  Family (unterschiedliche FLF-Fonts mit unterschiedlichen festen Rastermaßen) steuerbar; das muss
  den Nutzern des Konsolen-Moduls klar dokumentiert werden.
* FLF ist auf ASCII-Kunst-Zeichensätze ausgelegt; Sonderzeichen/Umlaute sind nicht in jedem
  FLF-Font enthalten - Abdeckung hängt vom jeweils gewählten Font ab (siehe Ersatz-Raster).
* Lizenzbedingungen frei verfügbarer FLF-Fonts (z. B. aus bekannten FIGlet-Sammlungen) müssen vor
  Aufnahme in den mitgelieferten Satz (IP-02) geprüft werden; der Standard-/Fallback-Font selbst
  ist davon nicht betroffen, da er selbst erstellt wird.
* Umfang und Gestaltungsaufwand des selbst zu erstellenden Standard-Fonts (Zeichenabdeckung,
  optische Gestaltung) wird in IP-02 festgelegt.
* Das Verhältnis zur ANSI-Styling-Engine aus FP-003 (IP-03) - insbesondere ob SGR-Attribute
  (Bold/Italic/Dim/Unterstrichen) zusätzlich zur Glyphen-Darstellung angewendet werden oder durch
  das Glyphen-Raster selbst ausgedrückt werden - ist offen und wird in IP-04 geklärt.
* Umfang des mitgelieferten Zeichensatzes (z. B. ob Umlaute/Sonderzeichen enthalten sind) wird in
  IP-02 festgelegt.
* Maximale Datei-/Rastergröße für extern geladene Fonts sowie deren Validierungstiefe wird in
  IP-03 festgelegt.
* FP-003 ist zum Zeitpunkt dieser Planung noch nicht begonnen (Status `NOT_STARTED`); FP-004 setzt
  auf der dort geplanten Rendering-Pipeline auf und kann erst nach deren Fertigstellung
  vollständig abgeschlossen werden (siehe IP-04).

## 10. Abschlusskriterien

* `./gradlew build` ist für den gesamten Workspace grün, inklusive der Bitmap-Font-Erweiterung in
  `ui:console`.
* Der mitgelieferte Font-Satz deckt die typischen Konsolenzeichen ab und wird ohne weitere
  Konfiguration verwendet, wenn keine externe Font-Datei geladen wurde.
* Ein selbst erstellter FLF-Font ist fest als Standard-/Fallback-Font eingebettet.
* Eine externe, selbst erstellte Font-Datei kann geladen, registriert und für die Ausgabe
  verwendet werden.
* Ein Dokument mit unbekannter `Font.family` wird ohne Fehler mit dem selbst erstellten
  Standard-/Fallback-Font ausgegeben.
* `Font.size` hat nachweislich keine Auswirkung auf die Konsolen-Ausgabe.
