# FP-003 / IP-02: Canvas Renderer

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* Öffentlichen Renderer bereitstellen, der ein `Document` komplett oder seitenweise auf eine `Canvas` zeichnet.
* Seitenübergänge als gestrichelte Linien; `Canvas` korrekt dimensioniert.

## Umfang

### Enthalten

* Öffentlicher Typ `CanvasDocumentRenderer` im Paket `org.pcsoft.framework.simplay.fx.canvas`.
* Eingabe ausschließlich `Document`; internes Messen über die IP-01-Fassade.
* `renderDocument(document, canvas?)`: erzeugt bzw. füllt eine `Canvas` mit allen Seiten untereinander.
* `renderPage(document, pageIndex, canvas?)`: zeichnet genau eine Seite.
* Konfiguration: Unit-Scale und Seitenabstand als Konstruktor- bzw. Parameterwerte mit Defaults.
* Gestrichelte Trennlinie zwischen aufeinanderfolgenden Seiten.
* `documentCanvasSize(document)` und `pageCanvasSize(document, pageIndex)` als öffentliche Helfer.
* Spike zur dynamischen `Canvas`-Vergrößerung; Ergebnis als Kommentar und Changelog-Notiz.
* `Canvas`-Reiter der Demo mit Toolbar für alle setzbaren Werte und Rückanzeige der Canvas-Größe.
* Headless-Tests.

### Nicht enthalten

* Zoom, Scroll-Container, Blattoptik, Schattenwurf, Selektion, Caret, Editieren.
* Virtualisierung sehr großer Dokumente über einen dokumentierten Größen-Hinweis hinaus.
* Styling über JavaFX-CSS.

## Abhängigkeiten

* Benötigt IP-01: Measurer, Zeichen-Walk, `pageSize`, `documentSize`, `DocumentMeasuring`, Demo-Hülle.

## Schnittstellen zu anderen Plänen

* Stellt `renderPage` bereit, das IP-03 optional zum Zeichnen eines einzelnen Blatts wiederverwendet.
* Stellt `documentCanvasSize` / `pageCanvasSize` als wiederverwendbare Größenberechnung bereit.
* Verbraucht nur IP-01.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/canvas/CanvasDocumentRenderer.kt` | Neu: öffentlicher Renderer, Voll- und Einzelseiten-Pfad, Trennlinien. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/canvas/CanvasRenderConfig.kt` | Neu: Datenklasse für Unit-Scale und Seitenabstand mit Defaults. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/RenderWalk.kt` | Trennlinien-/Rahmen-Callback anbinden (in IP-01 vorbereitet). |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/CanvasDemoTab.kt` | Neu: Reiterinhalt mit `Canvas` in `ScrollPane` und Toolbar. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/DemoApp.kt` | `Canvas`-Reiter mit `CanvasDemoTab` füllen. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/canvas/CanvasDocumentRendererTest.kt` | Neu: Größen-, Trennlinien-, Einzelseiten-Tests. |
| `CHANGELOG.md` | Eintrag unter „Unreleased" inkl. Spike-Ergebnis. |
| `docs/docs/fx/implementation.md` | Nur Verweis: vollständige Seite folgt in IP-06. |

## Testentwurf

* `testing`-Skill vor der Testklasse laden; `JavaFxTestBase` wiederverwenden; kein `IT`-Suffix.
* `documentCanvasSizeMatchesSummedPageHeights` — Canvas-Höhe = Summe der Seitenhöhen plus Abstände.
* `renderDocumentCreatesCanvasOfComputedSize` — ohne übergebene `Canvas` entsteht eine passend große.
* `renderDocumentReusesPassedCanvasWhenLargeEnough` — vorhandene `Canvas` wird gefüllt, nicht ersetzt.
* `renderDrawsDashedLineBetweenPages` — je Seitenübergang ein gestrichelter `strokeLine`-Aufruf.
* `singlePageRenderMatchesSlifeOfDocumentRender` — `renderPage` erzeugt dieselbe Seitenausgabe.
* `renderPageRejectsOutOfRangeIndex` — ungültiger `pageIndex` wirft `IllegalArgumentException`.
* `unitScaleScalesAllCoordinates` — doppelter Unit-Scale verdoppelt Positionen und Größe.
* `emptyDocumentProducesMinimalCanvas` — leeres `Document` ergibt eine `Canvas` ohne Zeichenaufrufe.

## Aufgaben

### Aufgabe 1 — Renderer-Kern

* `CanvasRenderConfig` mit `unitScale` und `pageGap` und Defaults anlegen.
* `CanvasDocumentRenderer` mit `renderDocument(document, canvas?)` implementieren.
* Intern `DocumentMeasuring` aufrufen, dann `documentCanvasSize` bestimmen.
* `Canvas` erzeugen oder übergebene prüfen und Größe setzen.
* Seiten mit dem IP-01-Walk untereinander zeichnen, Unit-Scale anwenden.

### Aufgabe 2 — Trennlinien und Einzelseite

* Nach jeder Seite außer der letzten eine gestrichelte Linie über die Breite zeichnen.
* `strokeLine` mit `setLineDashes`; Farbe und Strichmuster als interne Konstanten.
* `renderPage(document, pageIndex, canvas?)` mit Bereichsprüfung implementieren.
* `pageCanvasSize` und `documentCanvasSize` öffentlich bereitstellen.

### Aufgabe 3 — Spike dynamische Canvas

* Prüfen, ob `Canvas` nach erstem Layout zuverlässig via `width`/`height` wachsen kann.
* Grenzfall sehr hoher Dokumente und Pixel-Limit notieren.
* Ergebnis als KDoc-Absatz und `CHANGELOG.md`-Notiz festhalten; keine Tiling-Implementierung.

### Aufgabe 4 — Demo-Reiter

* `CanvasDemoTab` mit `Canvas` in `ScrollPane` anlegen.
* Toolbar: Auswahl Beispiel-`Document`, Unit-Scale, Seitenabstand, Modus ganzes Dokument/Seite.
* Bei Seitenmodus Spinner für `pageIndex`; Canvas-Größe als Label zurückanzeigen.
* Änderungen in der Toolbar lösen Neuzeichnen aus.
* `DemoApp` bindet `CanvasDemoTab` in den `Canvas`-Reiter ein.

### Aufgabe 5 — Tests, Changelog, Build, Abschluss

* `testing`-Skill laden; `CanvasDocumentRendererTest` gemäß Abschnitt „Testentwurf" anlegen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-02 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Ob `Canvas` dynamisch wachsen darf, klärt erst der Spike; Fallback ist Vorab-Dimensionierung.
* Sehr hohe Dokumente können das `Canvas`-Pixel-Limit überschreiten; Tiling bleibt möglicher Folgeplan.
* Aufgezeichneter `GraphicsContext` für Tests: ggf. dünner Wrapper oder Verifikation über Mock.
* Genaue Optik der Trennlinie (Abstand zur Seitenkante, Strichlänge) noch festzulegen.
