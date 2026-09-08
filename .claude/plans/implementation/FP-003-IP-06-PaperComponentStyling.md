# FP-003 / IP-06: Paper Component Styling

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* Paper-Sheet-`Control` über die Standard-JavaFX-CSS-Wege stylebar machen.
* Blattoptik, Abstände, Hintergrund und Selektionsfarbe rein per Stylesheet setzbar.

## Umfang

### Enthalten

* `StyleableProperty`/`CssMetaData` für: Blattrand-Farbe, Blattrand-Breite, Blatt-Hintergrund,
  Schattenfarbe, Schattenradius, Seitenabstand, Außenabstand, Selektionsfarbe, Trennlinien-Farbe.
* Standard-Style-Klasse `paper-sheet-view` und passende Pseudo-Klassen (z. B. `:readonly`, `:focused`).
* Default-User-Agent-Stylesheet über `getUserAgentStylesheet()`.
* Skin reagiert auf Stil-Änderungen und zeichnet neu.
* Bestehende Properties `outerMargin`/`pageGap` als styleable ausführen (CSS überschreibbar, API bleibt).
* Style-Klasse `paper-sheet-overlay` für den Overlay-Container aus IP-04, sofern umgesetzt.
* Stylesheet-Umschalter in den Toolbars der Reiter `Readonly` und `Read/Write`.
* Beispiel-Stylesheet(s) im Demo-Source-Set.
* Headless-Tests für CSS-Anwendung.

### Nicht enthalten

* Styling des Canvas-Renderers aus IP-02.
* Theming-API jenseits von JavaFX-CSS; FXML-Anbindung.
* Neue visuelle Effekte über die aufgezählten Werte hinaus.

## Abhängigkeiten

* Benötigt IP-03: `PaperSheetView` und `PaperSheetViewSkin`.
* Abstimmung mit IP-05 über die gemeinsame Skin-Klasse (Caret-/Selektionszeichnung).
* Optional IP-04 für die Overlay-Style-Klasse.

## Schnittstellen zu anderen Plänen

* Verbraucht IP-03; kann sich mit der IP-05-Skin überschneiden, gemeinsame Skin-Klasse.
* Endpunkt-Plan; liefert nichts an spätere Pläne.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetView.kt` | `CssMetaData`-Liste, styleable Properties, `getControlCssMetaData`, Style-Klasse, Pseudo-Klassen. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetViewSkin.kt` | Stilwerte statt interner Konstanten lesen, bei Änderung neu zeichnen. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetStyleableProperties.kt` | Neu: `CssMetaData`-Definitionen und Hilfsfunktionen. |
| `fx/src/main/resources/org/pcsoft/framework/simplay/fx/paper-sheet-view.css` | Neu: Default-User-Agent-Stylesheet. |
| `fx/src/main/resources/.gitkeep` | Entfernen mit `git rm`. |
| `fx/src/demo/resources/org/pcsoft/framework/simplay/fx/demo/demo-dark.css` | Neu: Beispiel-Stylesheet für die Demo. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/ReadonlyDemoTab.kt` | Toolbar: Stylesheet-Umschalter. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/ReadWriteDemoTab.kt` | Toolbar: Stylesheet-Umschalter. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetStylingTest.kt` | Neu: CSS-Anwendungs-Tests. |
| `CHANGELOG.md` | Eintrag unter „Unreleased". |
| `docs/docs/fx/implementation.md` | Nur Verweis: Styling-Seite folgt in IP-07. |

## Testentwurf

* `testing`-Skill vor der Testklasse laden; `JavaFxTestBase` wiederverwenden; kein `IT`-Suffix.
* `defaultUserAgentStylesheetIsApplied` — `getUserAgentStylesheet()` liefert eine ladbare URL.
* `styleClassIsPresentByDefault` — `paper-sheet-view` in den Style-Klassen der Komponente.
* `cssOverridesSheetBorderColor` — Inline-Style setzt die Blattrand-Farbe und wirkt im Skin.
* `cssOverridesPageGapAndOuterMargin` — CSS-Werte verändern das Layout wie die API-Werte.
* `cssOverridesSelectionColor` — Selektionshervorhebung nutzt die per CSS gesetzte Farbe.
* `readonlyPseudoClassTogglesWithMode` — `:readonly` aktiv im Readonly-Modus, sonst nicht.
* `apiSetterStillWinsOverUserAgentStylesheet` — programmatisch gesetzter Wert bleibt gegenüber Default bestehen.
* `styleChangeTriggersRepaint` — Stiländerung löst genau einen Neuzeichenlauf aus.

## Aufgaben

### Aufgabe 1 — CssMetaData-Definitionen

* `PaperSheetStyleableProperties` mit `CssMetaData` für alle aufgezählten Werte anlegen.
* Konverter wählen (`ColorConverter`, `SizeConverter`, `EffectConverter` bzw. Zahl).
* Styleable-Properties in `PaperSheetView` erzeugen und mit den bestehenden Properties verbinden.
* `getControlCssMetaData()` und statische `getClassCssMetaData()` bereitstellen.

### Aufgabe 2 — Style-Klasse, Pseudo-Klassen, Default-CSS

* Style-Klasse `paper-sheet-view` im Konstruktor setzen.
* Pseudo-Klassen `:readonly` und `:focused` an `mode` bzw. Fokus koppeln.
* `paper-sheet-view.css` als Default-Stylesheet schreiben und über `getUserAgentStylesheet()` liefern.

### Aufgabe 3 — Skin auf Stilwerte umstellen

* Interne Farb-/Größenkonstanten in `PaperSheetViewSkin` durch die Styleable-Werte ersetzen.
* Listener auf die Styleable-Properties registrieren, die einen Neuzeichenlauf auslösen.
* Sicherstellen, dass Caret- und Selektionszeichnung (IP-05) dieselben Werte verwenden.

### Aufgabe 4 — Demo-Umschalter

* In beiden Toolbars eine `ChoiceBox` „Stylesheet" mit „Standard" und „Dark".
* Auswahl fügt `demo-dark.css` zur Szene hinzu oder entfernt es.
* `demo-dark.css` mit abweichenden Blatt-, Schatten- und Selektionsfarben anlegen.

### Aufgabe 5 — Tests, Changelog, Build, Abschluss

* `testing`-Skill laden; `PaperSheetStylingTest` gemäß Abschnitt „Testentwurf" anlegen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-06 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Welche Werte CSS-fähig werden und welche einfache Properties bleiben.
* Zusammenspiel mit IP-05 an derselben Skin-Klasse ohne Konflikte; Reihenfolge der Umsetzung.
* Schattenwurf über `DropShadow`-Effekt versus manuelles Zeichnen; Auswirkung auf `EffectConverter`.
* Ob das Default-Stylesheet global oder nur pro Komponente greifen soll.
* Ressourcenpfad des Stylesheets muss im gebauten Jar auflösbar sein.
