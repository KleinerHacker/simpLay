# IP: Seiten-Deaktivierung

Kein Feature-Plan; eigenständiger Umsetzungsplan.
Partnerplan: `.claude/plans/implementation/IP-PageNumbering.md`.

## Ziel

* Einzelne Seiten in der View als „deaktiviert" markierbar: nicht editierbar und optisch abgesetzt (Skin / Style).
* Markierung ist reine UI-/Ansichtssache, nicht persistent im `Document`.
* Referenzierung über stabile Seiten-UUID, damit die Zuordnung beim Schreiben stabil bleibt.
* Umsetzung in fx- und swing-`PaperSheetView` mit geteilter Logik in `ui/common`.

## Umfang

### Enthalten

* Neue transiente View-Eigenschaft `deactivatedPageIds: Set<String>` in fx- und swing-`PaperSheetView`.
* Index-basierter Komfort-Setter `setPageDeactivated(index: Int, deactivated: Boolean)`, der gegen das aktuelle Dokument in eine `id` auflöst.
* Geteilter Helfer in `ui/common`, der aus `DocumentTextIndex` und der Id-Menge die editierbaren Linearbereiche berechnet.
* Editier-Sperre: jede Mutation, die einen deaktivierten Bereich berührt, wird vor `DocumentEditor` verworfen.
* Caret-Navigation überspringt deaktivierte Seiten; Auswahl darüber erlaubt.
* fx: neue styleable Paints für die Deaktiviert-Darstellung plus Zeichnen in `PaperSheetCanvasPainter`.
* swing: neue L&F-Schlüssel und `PaperSheetStyle`-Felder plus Zeichnen in `PaperSheetSwingPainter`.
* Änderungsbenachrichtigung: fx über Property, swing über `PropertyChangeEvent` (`PROP_DEACTIVATED_PAGE_IDS`).

### Nicht enthalten

* Persistenz der Deaktivierung im Modell oder in Serialisierungsformaten.
* Deaktivierung auf Block- oder Absatzebene.
* Bedien-UI (Kontextmenü, Buttons) zum Umschalten.
* Änderung an der Seiten-Nummerierung durch deaktivierte Seiten (siehe `IP-PageNumbering.md`).
* Read-only-Modus-Verhalten (dort ist ohnehin nichts editierbar).

## Abhängigkeiten

* Setzt `Page.id` (UUID) aus `IP-PageNumbering.md`, Aufgabe 1, voraus.
* Baut auf `DocumentTextIndex.BlockRange.pageIndex` (Index in `MeasuredDocument.raw.pages`) und `DocumentEditor` auf; deren Verträge bleiben unverändert.

## Umgang mit Indexverschiebung

* `deactivatedPageIds` speichert UUIDs, nie Positionen.
* Beim Setzen per Index wird sofort gegen `document.pages[index].id` aufgelöst und die `id` gespeichert.
* Text-Edits ändern die Modell-Seitenanzahl nicht; `DocumentEditor.splice` erhält `id` über `page.copy(...)`.
* Wird eine Seite mit gespeicherter `id` aus dem Dokument entfernt, bleibt die `id` folgenlos in der Menge (kein Fehler).
* Beim Dokumentwechsel bleibt die Menge bestehen; nicht mehr vorhandene `id`s wirken einfach nicht.
* Flow-Überlaufblätter teilen die `id` ihrer Modellseite; Deaktivierung wirkt auf alle zugehörigen Blätter.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/EditableRegions.kt` | Neu: berechnet editierbare Linearbereiche aus `DocumentTextIndex` und der Id-Menge; `isEditRangeAllowed`, `snapOutOfBlocked`. |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/DocumentTextIndex.kt` | Optional: Helfer `pageIdAt(linearIndex)` bzw. `blockRangePageId` ergänzen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetView.kt` | Neue Property `deactivatedPageIds`, `setPageDeactivated`, styleable Deaktiviert-Paints. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetStyleableProperties.kt` | Neue `CssMetaData` für `-fx-deactivated-sheet-background`, `-fx-deactivated-overlay-color`. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetCanvasPainter.kt` | Deaktivierte Seite abweichend füllen, Schraffur-Overlay, Caret/Selektion dort unterdrücken. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetEditor.kt` | Vor `applyEdit` gegen `EditableRegions` prüfen und abbrechen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetCaret.kt` | Navigation über deaktivierte Seiten hinweg springen lassen. |
| `ui/fx/src/main/resources/org/pcsoft/framework/simplay/fx/paper-sheet-view.css` | Default-Werte der neuen Paints. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/PaperSheetView.kt` | Neues Feld `deactivatedPageIds`, `setPageDeactivated`, `PROP_DEACTIVATED_PAGE_IDS`. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetStyle.kt` | Neue Felder für Deaktiviert-Hintergrund und Overlay. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/BasicPaperSheetUI.kt` | Neue L&F-Schlüssel lesen und in `PaperSheetStyle` füllen. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetSwingPainter.kt` | Deaktivierte Seite abweichend zeichnen, Caret/Selektion dort unterdrücken. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetEditor.kt` | Editier-Sperre analog fx. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetCaret.kt` | Navigations-Sprung analog fx. |
| `ui/common/src/test/kotlin/org/pcsoft/framework/simplay/uicommon/EditableRegionsTest.kt` | Neu: Bereichs- und Snap-Logik. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetEditingTest.kt` | Sperre und Caret-Sprung. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetStylingTest.kt` | Deaktiviert-Darstellung. |
| `ui/swing/src/test/kotlin/org/pcsoft/framework/simplay/swing/` | Tests analog fx. |
| `docs/` | UI-Doku (zuvor `project-docs`-Skill laden). |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

### Datenfluss

* View hält `deactivatedPageIds` als transiente Menge; Änderung löst Neuzeichnen und Neuberechnung der editierbaren Bereiche aus.
* `EditableRegions.of(index, deactivatedPageIds, document)` liefert die Liste erlaubter `IntRange` auf der Linearachse.
* Ein deaktivierter Bereich entspricht allen `BlockRange`s, deren `raw.pages[pageIndex].id` in der Menge liegt, plus umschließender Trennzeichen.

### Editier-Sperre

* `PaperSheetEditor.applyEdit` ermittelt den betroffenen Linearbereich (`from`..`to` bzw. Einfügeposition).
* Liegt ein Teil davon außerhalb der erlaubten Bereiche, wird die Mutation verworfen (kein `DocumentEditor`-Aufruf, kein Caret-Wechsel).
* Betrifft Tippen, `Backspace` / `Delete`, Einfügen, Ausschneiden, Duplizieren und Drag-and-Drop-Ziel.
* `Ctrl+C` bleibt erlaubt.

### Caret

* Nach jeder linearen Bewegung, die in einem gesperrten Bereich landet, springt der Caret in Bewegungsrichtung zum nächsten erlaubten Index.
* Am Dokumentanfang/-ende ohne erlaubtes Ziel bleibt der Caret an der Bereichsgrenze stehen.
* Auswahl mit `Shift` darf einen gesperrten Bereich überspannen; nur Mutationen sind blockiert.

### Darstellung

* fx: deaktivierte Seite mit `deactivatedSheetBackground` füllen, danach diagonales Schraffur-Overlay in `deactivatedOverlayColor`, Rahmen unverändert.
* fx: Programmatischer Setter schlägt User-Agent-Stylesheet, analog den bestehenden Paints.
* swing: gleiche zwei Werte über `PaperSheetStyle`, aus L&F-Schlüsseln `PaperSheetView.deactivatedSheetBackground` / `.deactivatedOverlayColor`, sofern nicht programmatisch gesetzt.
* Selektions-Highlight und Caret werden auf deaktivierten Seiten nicht gezeichnet.

## Testentwurf

* `testing`-Skill vor jeder Testklasse laden; Paketspiegelung; Entwicklertests ohne `IT`-Suffix.
* `editableRegionsExcludeDeactivatedPageBlocks` — Bereiche der deaktivierten Seite fehlen.
* `insertInsideDeactivatedPageIsRejected` — Dokument unverändert, Caret unverändert.
* `deleteRangeTouchingDeactivatedPageIsRejected` — Auswahl-Löschung über die Grenze wird verworfen.
* `typingOnActivePageStillWorks` — normale Bearbeitung unbeeinflusst.
* `caretSkipsDeactivatedPageForward` / `...Backward` — Caret landet auf der nächsten erlaubten Position.
* `shiftSelectionMaySpanDeactivatedPage` — Auswahl erlaubt, Kopie funktioniert.
* `setPageDeactivatedByIndexResolvesToId` — gespeichert wird die `id`, nicht der Index.
* `deactivationSurvivesTextEditAbovePage` — nach Edit oberhalb bleibt dieselbe Seite gesperrt.
* fx/swing: `deactivatedPageIsPaintedWithOverlayAndNoCaret`.

## Aufgaben

### Aufgabe 1 — Geteilte Bereichslogik in ui/common

* `EditableRegions` mit `of(index, deactivatedPageIds, document)` und `isEditRangeAllowed(lo, hi)` anlegen.
* `snapOutOfBlocked(index, direction)` für die Caret-Korrektur ergänzen.
* Bei leerer Id-Menge das ganze Dokument als erlaubt zurückgeben.
* `EditableRegionsTest` gemäß „Testentwurf" anlegen.

### Aufgabe 2 — fx: View-API und Editier-Sperre

* `deactivatedPageIds`-Property und `setPageDeactivated(index, Boolean)` in `PaperSheetView` ergänzen.
* `PaperSheetEditor.applyEdit` und alle Mutationspfade gegen `EditableRegions` absichern.
* `PaperSheetCaret`-Bewegungen nach dem Schritt durch `snapOutOfBlocked` führen.
* Neuberechnung bei Änderung von `deactivatedPageIds` oder `document` anstoßen.

### Aufgabe 3 — fx: Darstellung

* `PaperSheetStyleableProperties` um zwei `CssMetaData` erweitern.
* `PaperSheetView` um die zwei styleable Paint-Properties erweitern.
* `PaperSheetCanvasPainter.drawSheet` für deaktivierte Seiten mit Sonderfüllung und Schraffur.
* Caret und Selektion auf deaktivierten Seiten im Painter unterdrücken.
* Default-Werte in `paper-sheet-view.css` ergänzen.

### Aufgabe 4 — swing: View-API und Editier-Sperre

* Feld `deactivatedPageIds`, `setPageDeactivated`, `PROP_DEACTIVATED_PAGE_IDS` in `PaperSheetView` ergänzen.
* `internal/ps/PaperSheetEditor` und `PaperSheetCaret` analog fx absichern.
* Neuzeichnen und Bereichs-Neuberechnung bei Property-Änderung anstoßen.

### Aufgabe 5 — swing: Darstellung

* `PaperSheetStyle` (swing) um Deaktiviert-Hintergrund und Overlay-Farbe erweitern.
* `BasicPaperSheetUI` liest die neuen L&F-Schlüssel, sofern nicht programmatisch gesetzt.
* `PaperSheetSwingPainter` zeichnet deaktivierte Seiten abweichend, ohne Caret/Selektion.

### Aufgabe 6 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; UI-Doku beider Toolkits ergänzen.
* `CHANGELOG.md` unter „Unreleased" ergänzen.
* `./gradlew build` über einen Agent ausführen und Befunde beheben.
* Im selben Change-Set diese Plandatei `IP-PageDeactivation.md` mit `git rm` entfernen.

## Risiken und offene Punkte

* Reihenfolge zu `IP-PageNumbering.md`: ohne `Page.id` nicht umsetzbar; Aufgabe 1 dort zuerst.
* Auswahl über eine deaktivierte Seite hinweg: Kopierinhalt schließt gesperrten Text ein; als gewolltes Verhalten dokumentieren.
* Schraffur-Overlay-Performance bei vielen sichtbaren Seiten; einfache Linien statt Muster-Paint.
* Caret-Sprung bei komplett deaktiviertem Dokument: definierter Ruhepunkt am Anfang nötig.
* Drag-and-Drop-Ziel auf deaktivierter Seite muss auch den No-op-Redraw-Pfad sauber bedienen.
