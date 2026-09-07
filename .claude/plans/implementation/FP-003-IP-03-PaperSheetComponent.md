# FP-003 / IP-03: Paper Sheet Component

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* Öffentliches `Control`, das ein `Document` als Blätter mit Kanten und Schatten darstellt.
* Vertikal scrollbar, zoombar, Text markierbar und mit Strg+C kopierbar; Readonly-Modus (kein Caret).

## Umfang

### Enthalten

* Öffentliches `PaperSheetView : Control` im Paket `org.pcsoft.framework.simplay.fx.control`.
* Zugehöriges `PaperSheetViewSkin : SkinBase<PaperSheetView>`.
* Eingabe ausschließlich `document`-Property (`ObjectProperty<Document?>`).
* Properties: `outerMargin`, `pageGap`, `minZoom`, `maxZoom`, `zoom` (auf `[minZoom, maxZoom]` geklemmt).
* Nur-Lese-Property `contentSize` und `selectedText`.
* Blattoptik: Rand und Schattenwurf je Seite, konfigurierbarer Außenabstand und Seitenabstand.
* Vertikaler `ScrollBar`; Seiten werden bei Bedarf virtualisiert gezeichnet.
* Selektionsmodell über gemessene Geometrie mit dem IP-01-Hit-Test, blockübergreifend.
* Maus: Klick setzt Anker, Ziehen erweitert Selektion; `Strg+C` kopiert Klartext ins System-Clipboard.
* Wiederverwendung von `renderPage` aus IP-02 zum Zeichnen eines einzelnen Blatts, sofern sinnvoll.
* `Readonly`-Reiter der Demo: Komponente im Readonly-Modus plus Toolbar für alle Properties.
* Headless-Tests.

### Nicht enthalten

* Caret, Editieren, Tastenkommandos außer `Strg+C` (IP-04).
* JavaFX-CSS-Styling der visuellen Werte (IP-05).
* Horizontales Scrollen, nicht-vertikale Anordnung, Druck/PDF.
* Formatierte Selektion; kopiert wird ausschließlich Klartext.

## Abhängigkeiten

* Benötigt IP-01: Measurer, Zeichen-Walk, Größenhelfer, Hit-Test, Demo-Hülle.
* Optional: `renderPage` aus IP-02.

## Schnittstellen zu anderen Plänen

* Liefert an IP-04 und IP-05: `PaperSheetView`, `PaperSheetViewSkin`, Selektionsmodell,
  Abbildung gemessene Position ↔ `Document`-Position.
* IP-04 ergänzt Editieren, IP-05 ergänzt CSS-fähige Properties an derselben Klasse.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetView.kt` | Neu: `Control` mit allen Properties. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewSkin.kt` | Neu: Skin mit Viewport, Scroll, Zoom, Blattoptik, Zeichnen. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/TextSelection.kt` | Neu: Selektionsmodell und Position-Abbildung. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/DocumentTextIndex.kt` | Neu: linearer Textindex über Blöcke für blockübergreifende Selektion. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/ReadonlyDemoTab.kt` | Neu: Reiterinhalt mit `PaperSheetView` und Toolbar. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/DemoApp.kt` | `Readonly`-Reiter mit `ReadonlyDemoTab` füllen. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewTest.kt` | Neu: Property- und Klemm-Tests. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/TextSelectionTest.kt` | Neu: Selektions- und Kopier-Tests. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewSkinTest.kt` | Neu: Layout-, Scroll-, Zoom-Tests. |
| `CHANGELOG.md` | Eintrag unter „Unreleased". |
| `docs/docs/fx/implementation.md` | Nur Verweis: vollständige Seiten folgen in IP-06. |

## Testentwurf

* `testing`-Skill vor den Testklassen laden; `JavaFxTestBase` wiederverwenden; kein `IT`-Suffix.
* `zoomIsClampedToMinAndMax` — Setzen außerhalb der Grenzen klemmt auf `minZoom`/`maxZoom`.
* `settingDocumentUpdatesContentSize` — neues `Document` aktualisiert `contentSize`.
* `nullDocumentShowsEmptyViewport` — `null` ergibt leere, fehlerfreie Ansicht.
* `outerMarginAndPageGapAffectLayout` — Änderungen verschieben die Blattpositionen erwartungsgemäß.
* `pagesRenderAsSheetsWithBorderAndShadow` — je Seite ein Rahmen- und ein Schatten-Zeichenaufruf.
* `onlyVisiblePagesAreDrawn` — bei langem Dokument werden nicht sichtbare Seiten übersprungen.
* `clickThenDragSelectsTextRange` — Selektion umfasst die erwarteten Zeichen.
* `selectionSpansBlocksAndPages` — blockübergreifende Selektion liefert zusammenhängenden Text.
* `ctrlCPutsPlainTextOnClipboard` — `Clipboard` enthält den `selectedText`.
* `noCaretIsRenderedInReadonly` — kein Caret-Zeichenaufruf im Skin.
* `verticalScrollBarTracksContentHeight` — `ScrollBar`-Max entspricht Inhaltshöhe minus Viewport.

## Aufgaben

### Aufgabe 1 — Control und Properties

* `PaperSheetView : Control` mit `document`, `outerMargin`, `pageGap`, `minZoom`, `maxZoom`, `zoom`.
* `zoom` beim Setzen und bei Grenzänderung auf `[minZoom, maxZoom]` klemmen.
* Nur-Lese-Properties `contentSize` und `selectedText`.
* `createDefaultSkin()` liefert `PaperSheetViewSkin`.
* KDoc für alle Properties; Defaults sinnvoll wählen.

### Aufgabe 2 — Skin: Layout, Scroll, Zoom

* Viewport mit `Canvas` oder `Region` plus vertikalem `ScrollBar` aufbauen.
* Blattpositionen aus `pageSize`, `outerMargin`, `pageGap` und `zoom` berechnen.
* Bei Änderung von `document`, `zoom` oder Größe neu messen und neu zeichnen.
* Nur im Viewport sichtbare Seiten zeichnen (einfache Virtualisierung).
* Mausrad und `ScrollBar` an die vertikale Verschiebung koppeln.

### Aufgabe 3 — Blattoptik

* Je Seite Rechteck mit Rand und Schlagschatten zeichnen.
* Seiteninhalt über den IP-01-Walk bzw. `renderPage` aus IP-02 einzeichnen.
* Farben, Randstärke, Schattenparameter als interne Konstanten (in IP-05 CSS-fähig).

### Aufgabe 4 — Selektion und Kopieren

* `DocumentTextIndex` als lineare Abbildung Block/Offset ↔ globaler Textindex.
* `TextSelection` mit Anker und Fokus; Trefferbestimmung über den IP-01-Hit-Test.
* Maus-Handler: Klick setzt Anker, Ziehen setzt Fokus, Doppelklick wählt Wort.
* Selektion im Skin hervorheben; `selectedText` fortlaufend aktualisieren.
* `Strg+C` kopiert `selectedText` als Klartext ins `Clipboard`.

### Aufgabe 5 — Demo-Reiter

* `ReadonlyDemoTab` mit `PaperSheetView` im Readonly-Modus.
* Toolbar: Beispiel-`Document`, `outerMargin`, `pageGap`, `minZoom`, `maxZoom`, `zoom`.
* Rückanzeige von `zoom` und `selectedText` als Labels.
* `DemoApp` bindet `ReadonlyDemoTab` in den `Readonly`-Reiter ein.

### Aufgabe 6 — Tests, Changelog, Build, Abschluss

* `testing`-Skill laden; Testklassen gemäß Abschnitt „Testentwurf" anlegen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-03 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Ob die Skin-Zeichnung auf einer großen `Canvas` oder pro Blatt-`Canvas` erfolgt — Auswirkung auf IP-05.
* `TextBlock` speichert keine Whitespace; Selektions-Offsets orientieren sich an `TextBlock.toString()`.
* Reihenfolge des kopierten Klartexts bei blockübergreifender Selektion (Zeilenumbruch zwischen Blöcken).
* Virtualisierung versus einfache Sichtbarkeitsprüfung bei sehr vielen Seiten.
* Zoom-Implementierung über `Scale`-Transform oder über Neumessung; Auswahl beeinflusst Schärfe.
