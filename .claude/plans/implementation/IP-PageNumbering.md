# IP: Seitennummern

Kein Feature-Plan; eigenständiger Umsetzungsplan.
Partnerplan: `.claude/plans/implementation/IP-PageDeactivation.md` (hängt an Aufgabe 1 dieses Plans).

## Ziel

* Seitennummern über das persistente `Document`-Modell konfigurierbar: Position, Startnummer, Ausschlussliste, Zählstrategie.
* Positionen: links, mitte, rechts, wechselnd innen, wechselnd außen — je gekreuzt mit oben und unten — plus aus.
* Ausschluss einzelner Seiten von der Nummernanzeige über stabile Seiten-UUID.
* Anzeige in fx- und swing-View sowie in den Bild-/Canvas-Renderern.

## Umfang

### Enthalten

* Neues Feld `id` (UUID-String) im `Page`-Interface und in `FlowPage` / `SinglePage`, serialisiert, mit generiertem Default.
* Neuer Helfer `PageId.random()` über `kotlin.uuid.Uuid` in `commonMain`.
* Neues `@Serializable` `PageNumbering` im Modell mit `OFF`-Default.
* Neues Enum `PageNumberPosition` (11 Werte inklusive `OFF`).
* Neues Enum `PageCountingMode` (`CONTINUOUS`, `SKIP_EXCLUDED`) im Modell als persistenter Schalter.
* Neues `fun interface` `PageCountingStrategy` in `engine.engine` mit Objekten `ContinuousPageCounting` (Default) und `SkipExcludedPageCounting`.
* Neuer reiner `PageNumberPlanner`, der aus `MeasuredDocument` + `PageNumbering` eine `List<PageNumberLabel?>` erzeugt.
* Feld `numbering: PageNumbering` in `Document` mit Default `PageNumbering.OFF`.
* Zeichnen der Nummer in fx (`CanvasRenderer`, `PaperSheetCanvasPainter`, `CanvasDocumentRenderer`) und swing (`Graphics2DDocumentRenderer` über `PageFrameDecorator`, `DocumentImageRenderer`, `PaperSheetSwingPainter`).

### Nicht enthalten

* Freitext- oder Musterformat der Nummer (`Seite X von Y`, römische Ziffern, Präfix); Folgeplan.
* Kapitel- oder abschnittsbezogene Nummernkreise.
* PDF- und Print-Exporter (`export/jvm-pdf`, `export/jvm-print` sind nur Stubs).
* Bedien-UI (Dialoge, Menüs) zum Setzen der Konfiguration.
* Persistente Deaktivierung von Seiten; siehe `IP-PageDeactivation.md`.

## Abhängigkeiten

* Aufgabe 1 (Seiten-UUID) ist Voraussetzung für `IP-PageDeactivation.md`.
* Baut auf vorhandenen Typen `MeasuredDocument`, `MeasuredPage`, `contentArea`, `layout.margins`, `FxFontMeasureCalculator`, `SwingFontMeasureCalculator` auf; keine Änderung an deren Verträgen.

## Umgang mit Indexverschiebung

* Referenzierung ausgeschlossener Seiten ausschließlich über `Page.id` (UUID), nie über Position.
* `DocumentEditor.splice` baut Seiten mit `page.copy(...)` neu; `id` bleibt dabei erhalten.
* Flow-Überlauf: `SimpLayPageEngine.cloneFlowPage` nutzt `copy`, alle Überlaufblätter einer Modellseite teilen dieselbe `id`.
* Text-Edits ändern die Modell-Seitenanzahl nicht; die Ausschlussliste bleibt gültig.
* Optionaler index-basierter Komfort-Setter löst den Index gegen das aktuelle Dokument in eine `id` auf.
* Alte Persistenzdateien ohne `id` erhalten pro Seite beim Laden eine frische UUID über den Feld-Default.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/PageId.kt` | Neu: `object PageId { fun random(): String }` über `kotlin.uuid.Uuid`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/Page.kt` | `id`-Feld im Interface und in beiden Datenklassen, `@SerialName("id")`, Default `PageId.random()`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/PageNumbering.kt` | Neu: `PageNumbering`, `PageNumberPosition`, `PageCountingMode`, `OFF`-Konstante. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/Document.kt` | Neues Feld `numbering: PageNumbering = PageNumbering.OFF`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/PageCountingStrategy.kt` | Neu: `fun interface` plus `ContinuousPageCounting`, `SkipExcludedPageCounting`, KDoc-Strategieliste. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/PageNumberPlanner.kt` | Neu: `PageNumberLabel` und reine `plan(measured, numbering)`-Funktion. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/CanvasRenderer.kt` | Nummern-Label je Seite in Kopf-/Fußsteg zeichnen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetCanvasPainter.kt` | Planner-Ergebnis je sichtbarer Seite zeichnen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/CanvasDocumentRenderer.kt` | Nummern in die Offscreen-Ausgabe einbeziehen. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/Graphics2DDocumentRenderer.kt` | Aufrufer mit `PageFrameDecorator` aus dem Planner versorgen. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/DocumentImageRenderer.kt` | Decorator aus Planner erzeugen. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetSwingPainter.kt` | Nummer je sichtbarer Seite zeichnen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/PageNumberPlannerTest.kt` | Neu: Position, Ausschluss, Startnummer, Strategiewechsel. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/PageCountingStrategyTest.kt` | Neu: beide Strategien isoliert. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/model/SerializationTest.kt` | `numbering`- und `id`-Round-Trip ergänzen. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetViewSkinTest.kt` | Render-Test: Nummer an erwarteter Kante. |
| `ui/swing/src/test/kotlin/org/pcsoft/framework/simplay/swing/` | Render-Test analog fx. |
| `docs/` | Engine- und UI-Doku (zuvor `project-docs`-Skill laden). |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

### Modell

* `PageNumberPosition { OFF, TOP_LEFT, TOP_CENTER, TOP_RIGHT, TOP_INNER, TOP_OUTER, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_INNER, BOTTOM_OUTER }`.
* `PageNumbering(position = OFF, startNumber = 1, excludedPageIds = emptySet(), counting = CONTINUOUS, textStyle = DEFAULT_NUMBER_STYLE)`.
* `PageNumbering.OFF` = Instanz mit `position = OFF`.
* `Document.numbering` mit Default `PageNumbering.OFF`; Serialisierung abwärtskompatibel über Feld-Default.
* `textStyle: TextStyle` liefert Font und Größe der Nummer; Default: Serif, 10.0.

### Zählung

* `PageCountingStrategy.numbers(sheetCount, excludedSheetFlags, startNumber): List<Int?>` — eine Zuordnung Blattindex zu Anzeigenummer oder `null`.
* `ContinuousPageCounting`: jedes Blatt erhöht den Zähler; ausgeschlossenes Blatt liefert `null`, Zähler läuft weiter.
* `SkipExcludedPageCounting`: ausgeschlossenes Blatt liefert `null`, Zähler bleibt stehen.
* `PageCountingMode` im Modell wird im Planner auf das passende Strategieobjekt abgebildet.
* `startNumber` setzt den ersten gezählten Wert; Titelseiten per Ausschluss oder höherem `startNumber` handhabbar.

### Planer und Zeichnen

* `PageNumberPlanner.plan` iteriert `MeasuredDocument.pages`; Ausschluss über `page.raw.id in numbering.excludedPageIds`.
* Parität für `INNER` / `OUTER` aus der 1-basierten Blattnummer; ungerade = rechte Seite, gerade = linke Seite.
* `TOP_*` platziert im oberen Steg (`layout.margins.top`), `BOTTOM_*` im unteren Steg; horizontale Ausrichtung aus der Position.
* `PageNumberLabel(pageIndex, text, x, y, alignment)` in Seiten-lokalen Koordinaten.
* `position == OFF` liefert leere Liste; Renderer zeichnen nur, keine Layout-Logik im Renderer.
* Renderer lösen den Font über den jeweiligen `FontMeasureCalculator` aus `numbering.textStyle` auf.

## Testentwurf

* `testing`-Skill vor jeder Testklasse laden; Paketspiegelung; Entwicklertests ohne `IT`-Suffix.
* `offPositionProducesNoLabels` — `OFF` liefert keine Labels.
* `eachPositionAnchorsAtExpectedEdge` — je Position korrekte Kante und Ausrichtung.
* `excludedPageShowsNoNumber` — ausgeschlossene Seite ohne Label.
* `continuousKeepsCountingAcrossExcluded` — Folgeblatt behält fortlaufende Nummer.
* `skipExcludedDoesNotAdvance` — Folgeblatt erhält die niedrigere Nummer.
* `startNumberOffsetsFirstLabel` — erstes gezähltes Blatt trägt `startNumber`.
* `innerOuterAlternatesByParity` — gerade/ungerade Blätter spiegeln die horizontale Seite.
* `flowOverflowSheetsShareModelId` — Ausschluss greift für alle Überlaufblätter einer Seite.
* `numberingSurvivesJsonXmlYamlRoundTrip` — Modellfeld unverändert nach Round-Trip.
* fx/swing: Render-Test prüft gezeichnete Nummer an erwarteter Pixelkante.

## Aufgaben

### Aufgabe 1 — Modell: Seiten-UUID

* `PageId.random()` über `kotlin.uuid.Uuid` in `commonMain` anlegen.
* `id`-Feld in `Page`, `FlowPage`, `SinglePage` mit `@SerialName("id")` und Default ergänzen.
* Prüfen, dass `data class`-`copy` und Gleichheit die `id` mitführen.
* Serialisierungstest: Laden ohne `id` erzeugt frische UUID, Round-Trip erhält `id`.

### Aufgabe 2 — Modell: PageNumbering und Document

* `PageNumberPosition` und `PageCountingMode` als Enums anlegen.
* `PageNumbering` als `@Serializable` data class mit Feldern und `OFF`-Konstante anlegen.
* `Document.numbering` mit Default `PageNumbering.OFF` ergänzen.
* Round-Trip-Tests JSON/XML/YAML für `numbering` erweitern.

### Aufgabe 3 — PageCountingStrategy

* `fun interface PageCountingStrategy` in `engine` anlegen.
* `ContinuousPageCounting` und `SkipExcludedPageCounting` implementieren.
* KDoc-Strategieliste analog `LineBreakerStrategy` pflegen.
* Unit-Tests beider Strategien inklusive Startnummer und Ausschluss.

### Aufgabe 4 — PageNumberPlanner

* `PageNumberLabel` anlegen.
* `plan(measured, numbering)` rein implementieren, `PageCountingMode` auf Strategie abbilden.
* Kopf-/Fußsteg-Platzierung und `INNER` / `OUTER`-Parität berechnen.
* `OFF` liefert leere Liste.
* Tests gemäß Abschnitt „Testentwurf" für Planner und Positionen.

### Aufgabe 5 — fx-Rendering

* `PaperSheetCanvasPainter` je sichtbarer Seite das Label zeichnen.
* `CanvasRenderer` und `CanvasDocumentRenderer` für die Offscreen-Ausgabe ergänzen.
* Font aus `numbering.textStyle` über `FxFontMeasureCalculator` auflösen.
* TestFX-Render-Test: Nummer erscheint an erwarteter Kante.

### Aufgabe 6 — swing-Rendering

* `Graphics2DDocumentRenderer`-Aufrufer mit `PageFrameDecorator` aus dem Planner versorgen.
* `DocumentImageRenderer` und `PaperSheetSwingPainter` anbinden.
* Font aus `numbering.textStyle` über `SwingFontMeasureCalculator` auflösen.
* Render-Test analog fx.

### Aufgabe 7 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; Engine- und UI-Doku ergänzen.
* `CHANGELOG.md` unter „Unreleased" ergänzen.
* `./gradlew build` über einen Agent ausführen und Befunde beheben.
* Im selben Change-Set diese Plandatei `IP-PageNumbering.md` mit `git rm` entfernen.

## Risiken und offene Punkte

* `kotlin.uuid.Uuid` je Kotlin-Target: Verfügbarkeit unter `js` / `native` beim Build verifizieren.
* Frische UUID pro Laden bricht externe, dateiübergreifende ID-Referenzen; als Verhalten dokumentieren.
* Sehr schmale Margins: Nummer kann in den Inhalt ragen; Clipping statt Fehler.
* `INNER` / `OUTER`-Semantik (Bundseite) ist dokumentabhängig; Startannahme ungerade = rechts, in Doku festhalten.
* `PageCountingMode`-zu-Strategie-Brücke statt direkt serialisiertem Strategieobjekt; bei späterem Erweiterungsbedarf Registry prüfen.
