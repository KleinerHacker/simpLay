# IP: Seiten-Deaktivierung

Kein Feature-Plan; eigenständiger Umsetzungsplan.
Partnerplan: `.claude/plans/implementation/IP-PageNumbering.md`.

## Ziel

* Einzelne Seiten in der View als „deaktiviert" markierbar; Umgang damit über einen View-Schalter steuerbar.
* Neues Enum `PageDeactivationMode` mit `IGNORE`, `DISABLED`, `READONLY`, `HIDDEN` in `ui/common`.
* Markierung und Modus sind reine UI-/Ansichtssache, nicht persistent im `Document`.
* Referenzierung über stabile Seiten-UUID, damit die Zuordnung beim Schreiben stabil bleibt.
* Umsetzung in fx- und swing-`PaperSheetView` mit geteilter Logik in `ui/common`.

## Modus-Bedeutung

* `IGNORE` — `deactivatedPageIds` wird vollständig ignoriert; Verhalten wie leere Menge.
* `DISABLED` — Seite nicht editierbar, optisch ausgegraut/schraffiert, Caret springt beim Navigieren darüber hinweg.
* `READONLY` — Caret erreicht und durchquert den Text normal, Auswahl möglich, aber jede Mutation wird verworfen; keine Sonderdarstellung (Text bleibt normal lesbar).
* `HIDDEN` — Seite (und ihre Flow-Überlaufblätter) wird komplett aus Layout, Scroll-Bereich und Hit-Testing entfernt; im Dokument bleibt sie unverändert bestehen.

## Umfang

### Enthalten

* Neue transiente View-Eigenschaft `deactivatedPageIds: Set<String>` in fx- und swing-`PaperSheetView`.
* Neue View-Eigenschaft `deactivatedPageHandling: PageDeactivationMode` (Default `READONLY`) in fx- und swing-`PaperSheetView`.
* Index-basierter Komfort-Setter `setPageDeactivated(index: Int, deactivated: Boolean)`, der gegen das aktuelle Dokument in eine `id` auflöst.
* Geteilter Helfer in `ui/common`, der aus `DocumentTextIndex`, der Id-Menge und dem Modus die editierbaren/sichtbaren Linearbereiche berechnet.
* Editier-Sperre (Modus `DISABLED`/`READONLY`): jede Mutation, die einen deaktivierten Bereich berührt, wird vor `DocumentEditor` verworfen.
* Caret-Navigation: überspringt deaktivierte Seiten nur im Modus `DISABLED`; bei `READONLY` normale Navigation, bei `HIDDEN` durch Layout-Entfernung automatisch ausgeschlossen.
* Layout-Ausschluss (Modus `HIDDEN`): betroffene Blätter erhalten keine Zeilenhöhe/Scroll-Fläche und werden nicht gezeichnet.
* fx: neue styleable Paints für die `DISABLED`-Darstellung plus Zeichnen in `PaperSheetCanvasPainter`.
* swing: neue L&F-Schlüssel und `PaperSheetStyle`-Felder plus Zeichnen in `PaperSheetSwingPainter`.
* Änderungsbenachrichtigung: fx über Property, swing über `PropertyChangeEvent` (`PROP_DEACTIVATED_PAGE_IDS`, `PROP_DEACTIVATED_PAGE_HANDLING`).
* Erweiterung von `FloatingOverlayEvent` (fx und swing) um `pageDeactivated: Boolean`, damit ein Overlay erkennt, dass die zugehörige Seite deaktiviert ist.

### Nicht enthalten

* Persistenz der Deaktivierung/des Modus im Modell oder in Serialisierungsformaten.
* Deaktivierung auf Block- oder Absatzebene.
* Bedien-UI (Kontextmenü, Buttons) zum Umschalten.
* Änderung an der Seiten-Nummerierung durch deaktivierte/versteckte Seiten (siehe `IP-PageNumbering.md`).
* Read-only-Modus-Verhalten der gesamten View (dort ist ohnehin nichts editierbar).
* Sonderdarstellung für `READONLY` (bewusst wie normale Seite dargestellt).

## Abhängigkeiten

* Setzt `Page.id` (UUID) aus `IP-PageNumbering.md`, Aufgabe 1, voraus.
* Baut auf `DocumentTextIndex.BlockRange.pageIndex` (Index in `MeasuredDocument.raw.pages`) und `DocumentEditor` auf; deren Verträge bleiben unverändert.
* Baut auf der Sheet-Layout-Berechnung in `PaperSheetViewSkin` (fx) bzw. dem analogen Layout in `ui/swing` auf.

## Umgang mit Indexverschiebung

* `deactivatedPageIds` speichert UUIDs, nie Positionen.
* Beim Setzen per Index wird sofort gegen `document.pages[index].id` aufgelöst und die `id` gespeichert.
* Text-Edits ändern die Modell-Seitenanzahl nicht; `DocumentEditor.splice` erhält `id` über `page.copy(...)`.
* Wird eine Seite mit gespeicherter `id` aus dem Dokument entfernt, bleibt die `id` folgenlos in der Menge (kein Fehler).
* Beim Dokumentwechsel bleibt die Menge bestehen; nicht mehr vorhandene `id`s wirken einfach nicht.
* Flow-Überlaufblätter teilen die `id` ihrer Modellseite; Modus wirkt auf alle zugehörigen Blätter.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/PageDeactivationMode.kt` | Neu: Enum `IGNORE`, `DISABLED`, `READONLY`, `HIDDEN`. |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/EditableRegions.kt` | Neu: `of(index, deactivatedPageIds, mode, document)` liefert editierbare, navigierbare und sichtbare Linearbereiche; `isEditRangeAllowed`, `snapOutOfBlocked`, `isPageVisible`. |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/DocumentTextIndex.kt` | Optional: Helfer `pageIdAt(linearIndex)` bzw. `blockRangePageId` ergänzen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetView.kt` | Neue Properties `deactivatedPageIds`, `deactivatedPageHandling`, `setPageDeactivated`, styleable `DISABLED`-Paints. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetStyleableProperties.kt` | Neue `CssMetaData` für `-fx-deactivated-sheet-background`, `-fx-deactivated-overlay-color`. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetViewSkin.kt` | `relayout`/Blatt-Aufbau überspringt `HIDDEN`-Blätter (keine Höhe, kein Scroll-Platz); Neuberechnung bei Property-Änderung. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetCanvasPainter.kt` | `DISABLED`-Seite abweichend füllen, Schraffur-Overlay, Caret/Selektion dort unterdrücken; `HIDDEN`-Blätter nicht zeichnen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetEditor.kt` | Vor `applyEdit` gegen `EditableRegions` prüfen (Modus `DISABLED`/`READONLY`) und abbrechen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetCaret.kt` | Navigation im Modus `DISABLED` über deaktivierte Seiten hinweg springen lassen; `READONLY`/`HIDDEN` unverändert bzw. durch Layout ausgeschlossen. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/FloatingOverlayEvent.kt` | Neues Feld `pageDeactivated: Boolean`. |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/ps/PaperSheetOverlays.kt` | Beim Feuern von `SHOWN`/`HIDDEN` den Deaktiviert-Status der betroffenen Seite ermitteln und mitgeben. |
| `ui/fx/src/main/resources/org/pcsoft/framework/simplay/fx/paper-sheet-view.css` | Default-Werte der neuen Paints. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/PaperSheetView.kt` | Neue Felder `deactivatedPageIds`, `deactivatedPageHandling`, `setPageDeactivated`, `PROP_DEACTIVATED_PAGE_IDS`, `PROP_DEACTIVATED_PAGE_HANDLING`. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetStyle.kt` | Neue Felder für `DISABLED`-Hintergrund und Overlay. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/BasicPaperSheetUI.kt` | Neue L&F-Schlüssel lesen und in `PaperSheetStyle` füllen; Blatt-Layout überspringt `HIDDEN`-Blätter. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetSwingPainter.kt` | `DISABLED`-Seite abweichend zeichnen, ohne Caret/Selektion; `HIDDEN`-Blätter nicht zeichnen. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetEditor.kt` | Editier-Sperre analog fx. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetCaret.kt` | Navigations-Sprung analog fx. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/FloatingOverlayEvent.kt` | Neues Feld `pageDeactivated: Boolean`. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/ps/PaperSheetOverlays.kt` | Deaktiviert-Status der betroffenen Seite ermitteln und mitgeben, analog fx. |
| `ui/common/src/test/kotlin/org/pcsoft/framework/simplay/uicommon/EditableRegionsTest.kt` | Neu: Bereichs-, Sicht- und Snap-Logik je Modus. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetEditingTest.kt` | Sperre und Caret-Sprung je Modus. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/PaperSheetStylingTest.kt` | `DISABLED`-Darstellung, `HIDDEN`-Layout-Ausschluss. |
| `ui/swing/src/test/kotlin/org/pcsoft/framework/simplay/swing/` | Tests analog fx. |
| `docs/` | UI-Doku (zuvor `project-docs`-Skill laden). |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

### Datenfluss

* View hält `deactivatedPageIds` und `deactivatedPageHandling` als transiente Werte; Änderung an einem von beiden löst Neuzeichnen, Neuberechnung der editierbaren Bereiche und ggf. Relayout aus.
* `EditableRegions.of(index, deactivatedPageIds, mode, document)` liefert: erlaubte Editierbereiche, erlaubte Caret-Snap-Bereiche, sichtbare Bereiche (für `HIDDEN`).
* Ein betroffener Bereich entspricht allen `BlockRange`s, deren `raw.pages[pageIndex].id` in der Menge liegt, plus umschließender Trennzeichen.
* Bei `IGNORE` liefert `EditableRegions` durchgehend das ganze Dokument als erlaubt/sichtbar zurück (keine Sonderpfade in Editor/Caret/Layout).

### Editier-Sperre (`DISABLED`, `READONLY`)

* `PaperSheetEditor.applyEdit` ermittelt den betroffenen Linearbereich (`from`..`to` bzw. Einfügeposition).
* Liegt ein Teil davon außerhalb der erlaubten Bereiche, wird die Mutation verworfen (kein `DocumentEditor`-Aufruf, kein Caret-Wechsel).
* Betrifft Tippen, `Backspace` / `Delete`, Einfügen, Ausschneiden, Duplizieren und Drag-and-Drop-Ziel.
* `Ctrl+C` bleibt in beiden Modi erlaubt.
* Bei `IGNORE` und `HIDDEN` (dort ohnehin unerreichbar) greift keine Sperre.

### Caret

* `DISABLED`: nach jeder linearen Bewegung, die in einem gesperrten Bereich landet, springt der Caret in Bewegungsrichtung zum nächsten erlaubten Index; am Dokumentanfang/-ende ohne Ziel bleibt er an der Bereichsgrenze stehen. Auswahl mit `Shift` darf den Bereich überspannen.
* `READONLY`: keine Sonderbehandlung; Caret bewegt und platziert sich wie auf einer normalen Seite, Mutationen bleiben durch die Editier-Sperre blockiert.
* `HIDDEN`: die Seite ist nicht Teil des Layouts, daher für Caret/Klick/Scroll nicht erreichbar; keine zusätzliche Snap-Logik nötig, `EditableRegions` liefert sie einfach nicht als navigierbaren Bereich.
* `IGNORE`: keine Sonderbehandlung.

### Layout / Sichtbarkeit (`HIDDEN`)

* `PaperSheetViewSkin.relayout` (fx) und das analoge Layout in `BasicPaperSheetUI` (swing) filtern Blätter, deren Modell-Seiten-`id` in `deactivatedPageIds` liegt und `deactivatedPageHandling == HIDDEN`, komplett aus der Blatt-Liste heraus.
* Ausgeblendete Blätter erhalten keine Höhe im Stack, keinen Scroll-Anteil und werden im Painter nicht gezeichnet.
* Seitenzahl-Anzeige (falls über `IP-PageNumbering.md` aktiv) zählt weiterhin nach Modell-Reihenfolge; keine Lücken-Kompensation in diesem Plan.
* Modus-Wechsel zu/von `HIDDEN` löst immer ein volles `relayout` aus.

### Overlay-Status

* `FloatingOverlayEvent` (fx und swing) erhält `pageDeactivated: Boolean`, ermittelt anhand der Seiten-`id` von `index`/`triggerBounds`-Kontext gegen `deactivatedPageIds` und `deactivatedPageHandling` (bei `IGNORE` immer `false`).
* `PaperSheetOverlays` löst die Seiten-`id` zum jeweiligen Trigger (Selektion, Hover, Caret) auf und reicht den Status beim Feuern von `SHOWN`/`HIDDEN` durch.
* Ein Overlay über einer `HIDDEN`-Seite wird nicht gezeigt, da der Trigger dort ohnehin nicht entsteht (Layout-Ausschluss).

### Darstellung (`DISABLED`)

* fx: deaktivierte Seite mit `deactivatedSheetBackground` füllen, danach diagonales Schraffur-Overlay in `deactivatedOverlayColor`, Rahmen unverändert.
* fx: Programmatischer Setter schlägt User-Agent-Stylesheet, analog den bestehenden Paints.
* swing: gleiche zwei Werte über `PaperSheetStyle`, aus L&F-Schlüsseln `PaperSheetView.deactivatedSheetBackground` / `.deactivatedOverlayColor`, sofern nicht programmatisch gesetzt.
* Selektions-Highlight und Caret werden auf `DISABLED`-Seiten nicht gezeichnet.
* `READONLY` und `IGNORE` erhalten keine Sonderdarstellung; `HIDDEN` wird gar nicht gezeichnet.

## Testentwurf

* `testing`-Skill vor jeder Testklasse laden; Paketspiegelung; Entwicklertests ohne `IT`-Suffix.
* `editableRegionsIgnoreModeReturnsFullDocument` — `IGNORE` liefert unveränderte Bereiche.
* `editableRegionsExcludeDisabledPageBlocks` — Bereiche der `DISABLED`-Seite fehlen in Editierbereichen und Caret-Snap-Bereichen.
* `editableRegionsReadonlyPageNavigableButNotEditable` — Seite bleibt navigierbar, Editierbereich schließt sie aus.
* `editableRegionsHiddenPageExcludedFromVisibleBereichen` — `HIDDEN`-Seite fehlt im Sichtbarkeits-Bereich.
* `insertInsideDisabledPageIsRejected` — Dokument unverändert, Caret unverändert.
* `insertInsideReadonlyPageIsRejected` — Dokument unverändert, Caret bleibt auf Position.
* `deleteRangeTouchingDisabledPageIsRejected` — Auswahl-Löschung über die Grenze wird verworfen.
* `typingOnActivePageStillWorks` — normale Bearbeitung unbeeinflusst, in allen vier Modi.
* `caretSkipsDisabledPageForward` / `...Backward` — nur im Modus `DISABLED`.
* `caretEntersReadonlyPageNormally` — kein Sprung im Modus `READONLY`.
* `shiftSelectionMaySpanDisabledPage` — Auswahl erlaubt, Kopie funktioniert.
* `setPageDeactivatedByIndexResolvesToId` — gespeichert wird die `id`, nicht der Index.
* `deactivationSurvivesTextEditAbovePage` — nach Edit oberhalb bleibt dieselbe Seite gesperrt.
* `changingHandlingModeTriggersRelayoutAndRedraw` — Property-Wechsel löst Neuberechnung aus.
* fx/swing: `disabledPageIsPaintedWithOverlayAndNoCaret`, `hiddenPageIsExcludedFromLayoutAndNotPainted`, `readonlyPageIsPaintedNormallyButNotEditable`.
* fx/swing: `floatingOverlayEventReportsPageDeactivatedTrue` / `...False` — Status je nach Modus und Seiten-Zugehörigkeit.

## Aufgaben

### Aufgabe 1 — Geteilte Bereichslogik in ui/common

* `PageDeactivationMode`-Enum (`IGNORE`, `DISABLED`, `READONLY`, `HIDDEN`) anlegen.
* `EditableRegions` mit `of(index, deactivatedPageIds, mode, document)` anlegen; liefert Editier-, Snap- und Sicht-Bereiche getrennt.
* `isEditRangeAllowed(lo, hi)` und `snapOutOfBlocked(index, direction)` modusabhängig umsetzen.
* Bei `IGNORE` bzw. leerer Id-Menge das ganze Dokument als erlaubt/sichtbar zurückgeben.
* `EditableRegionsTest` gemäß „Testentwurf" anlegen.

### Aufgabe 2 — fx: View-API und Editier-Sperre

* `deactivatedPageIds`- und `deactivatedPageHandling`-Property sowie `setPageDeactivated(index, Boolean)` in `PaperSheetView` ergänzen.
* `PaperSheetEditor.applyEdit` und alle Mutationspfade gegen `EditableRegions` absichern (Modi `DISABLED`/`READONLY`).
* `PaperSheetCaret`-Bewegungen im Modus `DISABLED` durch `snapOutOfBlocked` führen; `READONLY`/`IGNORE` unverändert lassen.
* Neuberechnung bei Änderung von `deactivatedPageIds` oder `deactivatedPageHandling` anstoßen.
* `FloatingOverlayEvent` um `pageDeactivated` erweitern; `PaperSheetOverlays` ermittelt den Status je Trigger und reicht ihn durch.

### Aufgabe 3 — fx: Darstellung und Layout

* `PaperSheetStyleableProperties` um zwei `CssMetaData` für `DISABLED` erweitern.
* `PaperSheetView` um die zwei styleable Paint-Properties erweitern.
* `PaperSheetCanvasPainter.drawSheet` für `DISABLED`-Seiten mit Sonderfüllung und Schraffur; `HIDDEN`-Blätter überspringen.
* Caret und Selektion auf `DISABLED`-Seiten im Painter unterdrücken.
* `PaperSheetViewSkin.relayout` um Ausschluss von `HIDDEN`-Blättern aus Blatt-Liste, Höhe und Scroll-Fläche erweitern.
* Default-Werte in `paper-sheet-view.css` ergänzen.

### Aufgabe 4 — swing: View-API und Editier-Sperre

* Felder `deactivatedPageIds`, `deactivatedPageHandling`, `setPageDeactivated`, `PROP_DEACTIVATED_PAGE_IDS`, `PROP_DEACTIVATED_PAGE_HANDLING` in `PaperSheetView` ergänzen.
* `internal/ps/PaperSheetEditor` und `PaperSheetCaret` analog fx absichern.
* Neuzeichnen und Bereichs-Neuberechnung bei Property-Änderung anstoßen.
* `FloatingOverlayEvent` (swing) um `pageDeactivated` erweitern; `PaperSheetOverlays` (swing) ermittelt den Status analog fx.

### Aufgabe 5 — swing: Darstellung und Layout

* `PaperSheetStyle` (swing) um `DISABLED`-Hintergrund und Overlay-Farbe erweitern.
* `BasicPaperSheetUI` liest die neuen L&F-Schlüssel, sofern nicht programmatisch gesetzt.
* `BasicPaperSheetUI`-Blatt-Layout um Ausschluss von `HIDDEN`-Blättern erweitern.
* `PaperSheetSwingPainter` zeichnet `DISABLED`-Seiten abweichend, ohne Caret/Selektion; `HIDDEN`-Blätter überspringen.

### Aufgabe 6 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; UI-Doku beider Toolkits um `PageDeactivationMode` ergänzen.
* `CHANGELOG.md` unter „Unreleased" ergänzen.
* `./gradlew build` über einen Agent ausführen und Befunde beheben.
* Im selben Change-Set diese Plandatei `IP-PageDeactivation.md` mit `git rm` entfernen.

## Risiken und offene Punkte

* Reihenfolge zu `IP-PageNumbering.md`: ohne `Page.id` nicht umsetzbar; Aufgabe 1 dort zuerst.
* `HIDDEN` und Seitennummerierung: Zählung folgt weiterhin der Modell-Reihenfolge, keine Lücken-Kompensation — spätere Abstimmung mit `IP-PageNumbering.md` ggf. nötig.
* Auswahl über eine `DISABLED`-Seite hinweg: Kopierinhalt schließt gesperrten Text ein; als gewolltes Verhalten dokumentieren.
* Schraffur-Overlay-Performance bei vielen sichtbaren `DISABLED`-Seiten; einfache Linien statt Muster-Paint.
* Caret-Ruhepunkt, wenn alle sichtbaren/navigierbaren Seiten `DISABLED` oder `HIDDEN` sind: definierter Ruhepunkt am Anfang nötig.
* Laufzeit-Wechsel des Modus (z. B. `DISABLED` → `HIDDEN`) muss Caret/Selektion, die auf der betroffenen Seite steht, sauber auflösen (Caret verwerfen/neu platzieren).
* Drag-and-Drop-Ziel auf `DISABLED`-Seite muss auch den No-op-Redraw-Pfad sauber bedienen.
