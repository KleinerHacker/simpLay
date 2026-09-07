# FP-003 / IP-04: Floating Overlays

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* FXML-kompatible API, um von außen eigene schwebende Komponenten zu registrieren.
* Die `PaperSheetView` blendet sie ein, positioniert sie und blendet sie aus, wenn ein Trigger feuert.

## Umfang

### Enthalten

* `FloatingOverlayTrigger`-Enum im Paket `org.pcsoft.framework.simplay.fx.control`:
  `SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`.
* `CARET` bleibt in IP-04 inert; aktiviert wird er in IP-05, sobald das Caret existiert.
* `FloatingOverlay`-Bean mit parameterlosem Konstruktor; Properties `content: ObjectProperty<Node?>`,
  `trigger: ObjectProperty<FloatingOverlayTrigger>`, `anchor: ObjectProperty<Pos>`,
  `offsetX`/`offsetY: DoubleProperty`, `autoHide: BooleanProperty` (Default `true`).
* Read-only-Felder je Overlay, vom Skin bei Trigger befüllt, in FXML per `${overlayId.prop}` bindbar:
  `active`, `activeBounds`, `activeIndex`, `activeText`, `activeDocumentRange`.
* `PaperSheetView.getFloatingOverlays(): ObservableList<FloatingOverlay>`, von FXML als
  gleichnamiges Kindelement befüllbar; kein `@DefaultProperty`.
* Read-only-Properties an `PaperSheetView`: `hoveredParagraph`, `hoveredParagraphBounds`,
  `hoveredPage`, `hoveredPageBounds` (Viewport-Koordinaten, folgen Scroll und Zoom).
* `onShown`/`onHidden` als `EventHandler`-Properties je Overlay; Event trägt dieselben Felder.
* Skin: interner Overlay-`Pane` über dem Viewport, Trigger-Erkennung für Selektion, Absatz-Hover
  (Block-Ebene) und Blatt-Hover, Positionierung an `anchor` plus Offset.
* Skin: Nachführen der Overlays bei Scroll und Zoom; Ausblenden bei `autoHide`, wenn der Trigger endet.
* Rand-Verhalten: Overlay am Viewport-Rand festklemmen, bis der Anker ganz aus dem Viewport ist,
  dann ausblenden.
* `Readonly`-Demo-Reiter um Beispiel-Overlays erweitern (Kopieren-Leiste über der Selektion,
  Hover-Label über dem Absatz).
* Headless-Tests inkl. `FXMLLoader`-Ladetest mit deklarierten Overlays.

### Nicht enthalten

* `CARET`-Trigger-Logik (IP-05).
* CSS-Styling der Overlays (IP-06).
* Layout-Manager für mehrere gleichzeitige Overlays am selben Anker über einfache Stapelung hinaus.
* Editieren, Caret, Tastenkommandos.

## Abhängigkeiten

* Benötigt IP-03: `PaperSheetView`, `PaperSheetViewSkin`, `TextSelection`, `selectionBounds`,
  Positions-Mapping.
* Nutzt aus IP-01 den Zeichen-Walk (`walkPage`) und den Hit-Test (`hitTest`).

## Schnittstellen zu anderen Plänen

* IP-05 aktiviert den `CARET`-Trigger an derselben API und speist ihn mit der Caret-Geometrie.
* IP-06 macht Overlay-Container und -Optik CSS-fähig.
* IP-07 dokumentiert die Overlay-API.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/FloatingOverlayTrigger.kt` | Neu: Enum `SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/FloatingOverlay.kt` | Neu: FXML-taugliche Bean mit Properties, Read-only-Feldern, `onShown`/`onHidden`. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetView.kt` | `floatingOverlays`-Liste, Hover-Read-only-Properties; Bean-Konformität sichern. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewSkin.kt` | Overlay-`Pane`, Trigger-Erkennung, Positionierung, Scroll-/Zoom-Nachführung, Rand-Klemmung. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/ReadonlyDemoTab.kt` | Beispiel-Overlays und Rückanzeige. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/FloatingOverlayTest.kt` | Neu: Trigger-, Positionierungs-, FXML-Ladetests. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewSkinTest.kt` | Overlay-Fälle ergänzen. |
| `fx/src/test/resources/org/pcsoft/framework/simplay/fx/control/paper-sheet-overlays.fxml` | Neu: Test-FXML mit `<PaperSheetView>` und `<floatingOverlays>`. |
| `CHANGELOG.md` | Eintrag unter „Unreleased". |
| `docs/docs/fx/implementation.md` | Nur Verweis: vollständige Seite folgt in IP-07. |

## Testentwurf

* `testing`-Skill vor den Testklassen laden; `JavaFxTestBase` wiederverwenden; kein `IT`-Suffix.
* `selectionOverlayAppearsAndTracksSelection` — Overlay erscheint bei Selektion, folgt ihrer Box.
* `selectionOverlayHidesWhenSelectionCleared` — Aufheben der Selektion blendet das Overlay aus.
* `paragraphHoverOverlayCarriesParagraphIndex` — `activeIndex` entspricht dem Absatz unter der Maus.
* `pageHoverOverlayAnchorsToSheet` — Overlay steht am erwarteten Blatt-Rand.
* `overlayFollowsScrollAndZoom` — nach Scroll und Zoom bleibt die Position am Anker.
* `overlayClampsAtViewportEdgeThenHides` — Overlay klemmt am Rand, verschwindet bei ganz weggescrolltem Anker.
* `fxmlDeclaredOverlaysLoadAndActivate` — per FXML deklarierte Overlays laden und reagieren wie im Code-Pfad.
* `caretTriggerIsInertWithoutEditing` — ein `CARET`-Overlay bleibt in IP-04 unsichtbar.
* `onShownAndOnHiddenFireWithContext` — Handler erhalten `activeBounds`/`activeIndex`/`activeText`.

## Aufgaben

### Aufgabe 1 — Overlay-Typen

* `FloatingOverlayTrigger`-Enum mit den vier Werten anlegen.
* `FloatingOverlay`-Bean mit parameterlosem Konstruktor und allen Properties.
* Read-only-Felder `active`, `activeBounds`, `activeIndex`, `activeText`, `activeDocumentRange`.
* `onShown`/`onHidden` als `ObjectProperty<EventHandler<…>>`; passendes Event-Objekt.
* KDoc für alle Properties und Felder; FXML-Nutzung im Klassen-KDoc zeigen.

### Aufgabe 2 — PaperSheetView-Anbindung

* `floatingOverlays` als `ObservableList<FloatingOverlay>` mit reinem Getter bereitstellen.
* Hover-Read-only-Properties `hoveredParagraph(+Bounds)` und `hoveredPage(+Bounds)` ergänzen.
* Alle neuen und bestehenden Properties auf das JavaFX-Bean-Muster prüfen (FXML/`@NamedArg`).
* Liste an das Skin durchreichen; Änderungen der Liste beobachten.

### Aufgabe 3 — Skin: Trigger-Erkennung

* Overlay-`Pane` über dem Viewport aufbauen, Kinder aus `content` der aktiven Overlays.
* Selektionswechsel aus `TextSelection`/`selectionBounds` als `SELECTION`-Trigger auswerten.
* Maus-Hover über Blöcke via `walkPage`-Blockgeometrie als `PARAGRAPH_HOVER` auswerten.
* Maus-Hover über Blätter als `PAGE_HOVER` auswerten.
* Read-only-Felder des Overlays füllen; `onShown`/`onHidden` auslösen.

### Aufgabe 4 — Skin: Positionierung

* Overlay an `anchor` der Trigger-Bounding-Box ausrichten, `offsetX`/`offsetY` addieren.
* Bei Scroll, Zoom und Größenänderung alle sichtbaren Overlays neu positionieren.
* Anker aus dem Viewport: Overlay am Rand klemmen, dann bei vollständigem Verlust ausblenden.
* Entprellung bei schnellem Hover-Wechsel, um Flackern zu vermeiden.

### Aufgabe 5 — Demo und Test-FXML

* `ReadonlyDemoTab`: Kopieren-Leiste (`SELECTION`) und Hover-Label (`PARAGRAPH_HOVER`) hinzufügen.
* Rückanzeige des zuletzt getriggerten Absatz-/Seitenindex.
* `paper-sheet-overlays.fxml` mit `<PaperSheetView>` und `<floatingOverlays>` anlegen.

### Aufgabe 6 — Tests, Changelog, Build, Abschluss

* `testing`-Skill laden; Testklassen gemäß Abschnitt „Testentwurf" anlegen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-04 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Genauigkeit des Block-Hit-Tests bei mehrspaltigen oder verschachtelten Blöcken.
* Mehrere aktive Overlays am selben Anker; Stapel- oder Kollisionsverhalten.
* Flackern bei schnellem Hover-Wechsel; passende Entprellungsdauer.
* Headless-Hover-Simulation mit TestFX/Monocle für die Positionierungstests.
* Ob `activeText` bei `PARAGRAPH_HOVER` den `TextBlock.toString()`-Text oder die gerenderten Zeilen abbildet.
