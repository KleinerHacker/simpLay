# FP-003 / IP-05: Editing And Key Commands

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* Normal-Modus der Paper-Sheet-Komponente ergänzen: Caret, Texteingabe, Strg+V, Navigationskommandos.
* Bearbeitung erzeugt ein aktualisiertes `Document`; Readonly bleibt exakt das IP-03-Verhalten.

## Umfang

### Enthalten

* `mode`-Property an `PaperSheetView` mit Werten `READONLY` und `NORMAL` (Default `READONLY`).
* Caret-Modell: Position als globaler Textindex, Blinken, Zeichnen im Skin nur im Normal-Modus.
* Zeicheneingabe über `onKeyTyped` an der Caret-Position; Selektion wird ersetzt.
* `Strg+V` fügt Klartext aus dem `Clipboard` an der Caret-Position ein.
* Navigations- und Editierkommandos: `Home`, `End`, `Strg+Home`, `Strg+Ende`, Pfeile links/rechts/oben/unten,
  `Backspace`, `Entf`, jeweils optional mit `Shift` zum Erweitern der Selektion.
* Neumessung nach jeder Änderung; `document`-Property spiegelt den neuen Stand.
* Änderungs-Event bzw. beobachtbare `document`-Property als Ausgabe.
* Strategie zum Neuaufbau eines `Document` aus Editieroperationen.
* Aktivierung des `CARET`-Triggers der Overlay-API (Caret-Position als Trigger-Geometrie), sofern IP-04 umgesetzt ist.
* `Read/Write`-Reiter der Demo: Komponente im Normal-Modus plus Toolbar inkl. Moduswahl.
* Headless-Tests.

### Nicht enthalten

* Undo/Redo, Rich-Text-Bearbeitung, Formatierungs-Toolbar, kollaboratives Editieren.
* JavaFX-CSS-Styling (IP-06).
* Laden/Speichern von Dateien; die Demo nutzt dafür `engine`-Serialisierung.

## Abhängigkeiten

* Benötigt IP-03: `PaperSheetView`, `PaperSheetViewSkin`, `TextSelection`, `DocumentTextIndex`.
* Nutzt aus IP-01 den Hit-Test und die Größenhelfer.
* Optional IP-04: Overlay-API für den `CARET`-Trigger.

## Schnittstellen zu anderen Plänen

* Verbraucht IP-03 und IP-01; optional IP-04.
* Teilt sich die eine Skin-Klasse mit IP-04 und IP-06; Abstimmung über die Skin-Struktur.
* Endpunkt-Plan; liefert nichts an spätere Pläne.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetView.kt` | `mode`-Property, `PaperSheetMode`-Enum, Editier-Ausgabe. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetViewSkin.kt` | Caret zeichnen und blinken, Key-Handler, Selektion ersetzen, `CARET`-Trigger auslösen. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetMode.kt` | Neu: Enum `READONLY`, `NORMAL`. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/control/CaretModel.kt` | Neu: Caret-Position, Bewegungslogik, Blinktimer. |
| `fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/DocumentEditor.kt` | Neu: Einfügen/Löschen am Textindex, `Document`-Neuaufbau über `TextBlock.of`. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/ReadWriteDemoTab.kt` | Neu: Reiterinhalt mit editierbarer Komponente und Toolbar. |
| `fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/DemoApp.kt` | `Read/Write`-Reiter mit `ReadWriteDemoTab` füllen. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/CaretModelTest.kt` | Neu: Bewegungs- und Grenzfall-Tests. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/internal/DocumentEditorTest.kt` | Neu: Einfüge-/Lösch-/Neuaufbau-Tests. |
| `fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/control/PaperSheetEditingTest.kt` | Neu: Tastenkommando- und Integrationstests. |
| `CHANGELOG.md` | Eintrag unter „Unreleased". |
| `docs/docs/fx/implementation.md` | Nur Verweis: vollständige Seiten folgen in IP-07. |

## Testentwurf

* `testing`-Skill vor den Testklassen laden; `JavaFxTestBase` wiederverwenden; kein `IT`-Suffix.
* `readonlyModeShowsNoCaretAndRejectsInput` — Eingabe im Readonly-Modus ändert `document` nicht.
* `switchingToNormalShowsCaret` — Moduswechsel erzeugt einen Caret-Zeichenaufruf.
* `typingCharacterInsertsAtCaret` — `document`-Text enthält das Zeichen an der erwarteten Stelle.
* `typingReplacesActiveSelection` — bei aktiver Selektion ersetzt das Zeichen den markierten Bereich.
* `backspaceAndDeleteRemoveExpectedCharacter` — je ein Zeichen vor bzw. nach dem Caret entfällt.
* `ctrlVPastesClipboardTextAtCaret` — Clipboard-Klartext wird an der Caret-Position eingefügt.
* `homeEndMoveWithinLine` — Caret springt an Zeilenanfang bzw. Zeilenende.
* `ctrlHomeCtrlEndMoveToDocumentBounds` — Caret springt an Dokumentanfang bzw. -ende.
* `arrowKeysMoveByCharacterAndLine` — Links/Rechts je ein Zeichen, Oben/Unten je eine Zeile.
* `shiftWithNavigationExtendsSelection` — `Shift` plus Navigation erweitert die Selektion.
* `editingReMeasuresAndUpdatesContentSize` — nach Umbruch ändert sich `contentSize`.
* `documentPropertyReflectsEditedState` — Listener auf `document` erhält den neuen Wert.
* `caretTriggerOverlayFollowsCaret` — ein `CARET`-Overlay steht an der Caret-Position.

## Aufgaben

### Aufgabe 1 — Modus und Caret-Modell

* `PaperSheetMode`-Enum und `mode`-Property mit Default `READONLY`.
* `CaretModel` mit Position als globaler Textindex und Bewegungsmethoden.
* Blinktimer über `Timeline`; pausiert im Readonly-Modus.
* Caret-Rechteck aus der gemessenen Zeilen-/Part-Geometrie berechnen.

### Aufgabe 2 — DocumentEditor

* `DocumentEditor` mit `insert(index, text)` und `delete(from, to)` auf dem Textindex.
* Betroffene `TextBlock`s über `TextBlock.of(neuerText, style)` neu erzeugen.
* Unveränderte Seiten und Blöcke per `copy` übernehmen; Seiten-/Blockstruktur beibehalten.
* Abbildung globaler Index → Block/Zeichen über `DocumentTextIndex`.

### Aufgabe 3 — Key-Handling im Skin

* `onKeyTyped` für druckbare Zeichen; Selektion vorher löschen.
* `onKeyPressed` für `Home`/`End`/`Strg+Home`/`Strg+Ende`/Pfeile/`Backspace`/`Entf`.
* `Strg+V` liest `Clipboard`-Klartext und ruft `DocumentEditor.insert`.
* `Shift`-Varianten erweitern die Selektion statt sie zu löschen.
* Nach jeder Änderung neu messen, `document` setzen, Caret nachführen.
* `CARET`-Trigger der Overlay-API mit der aktuellen Caret-Geometrie speisen.

### Aufgabe 4 — Demo-Reiter

* `ReadWriteDemoTab` mit `PaperSheetView` im Normal-Modus.
* Toolbar: Moduswahl plus alle Werte des `Readonly`-Reiters.
* Rückanzeige von Caret-Position und aktuellem `Document` (z. B. Zeichenzahl, Seitenzahl).
* `DemoApp` bindet `ReadWriteDemoTab` in den `Read/Write`-Reiter ein.

### Aufgabe 5 — Tests, Changelog, Build, Abschluss

* `testing`-Skill laden; Testklassen gemäß Abschnitt „Testentwurf" anlegen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-05 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* `TextBlock` hat privaten Konstruktor; Bearbeitung nur über `TextBlock.of` mit `toString`-Text möglich.
* Symbol-versus-Wort-Tokenisierung: Einfügen kann Parts neu aufteilen; Caret-Offset muss stabil bleiben.
* Ob ein eigenes Änderungs-Event nötig ist oder die beobachtbare `document`-Property genügt.
* Zeilenweise Auf/Ab-Navigation braucht eine gehaltene Wunsch-x-Position.
* Verhalten bei leeren Blöcken oder leerem `Document` während der Bearbeitung.
* Abstimmung mit IP-06, damit Caret- und Selektionszeichnung stylebare Werte nutzen kann.
