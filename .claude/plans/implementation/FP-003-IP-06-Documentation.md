# FP-003 / IP-06: Documentation

Feature Plan: `.claude/plans/features/FP-003-JavaFxRendering.md`
Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Ziel

* `fx`-Dokumentation unter `docs/docs/fx/` schreiben: Canvas-Rendering, Paper-Sheet-Nutzung, Paper-Sheet-Styling.
* Nur Dokumentation; kein Produktionscode.

## Umfang

### Enthalten

* Seite „Canvas rendering": direkte Nutzung des Renderers auf einer `Canvas`.
* Diese Seite beschreibt Voll- und Einzelseiten-Rendering, Konfigurationswerte, `Canvas`-Größe,
  Ergebnis des Dynamic-Growth-Spikes, Unit-Scaling.
* Seite „Paper sheet component": alle Properties und Einstellungen, Readonly-/Normal-Modus,
  Einbindung in eine Scene, Laden und Speichern eines `Document` über `engine`-Serialisierung,
  Selektion und Clipboard.
* Diese Seite enthält eine Tabelle der unterstützten Shortcuts (`Strg+C`, `Strg+V`, `Home`, `End`,
  `Strg+Home`, `Strg+Ende`, Pfeile, `Backspace`, `Entf`, `Shift`-Varianten).
* Seite „Styling the paper sheet component": Style-Klasse, `-fx-`-Properties, Pseudo-Klassen,
  Default-Stylesheet, Override-Beispiel.
* `mkdocs.yml`: Navigationseinträge für die drei Seiten.
* Ersetzen bzw. Erweitern des Platzhalters in `docs/docs/fx/implementation.md`.

### Nicht enthalten

* KDoc (jeder Produktionsplan pflegt seine eigene).
* Dokumentation des Demo-Source-Sets.
* Screenshot- oder Bild-Pipeline.
* Änderungen an Produktionscode oder anderen Modulen.

## Abhängigkeiten

* Benötigt IP-02, IP-03, IP-04 und IP-05; dokumentiert deren finale öffentliche Oberfläche.

## Schnittstellen zu anderen Plänen

* Verbraucht die öffentliche API von IP-02 bis IP-05.
* Endpunkt-Plan; liefert nichts zurück.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `docs/docs/fx/canvas-rendering.md` | Neu: Renderer auf `Canvas`, Voll- und Einzelseite, Konfiguration, Größen. |
| `docs/docs/fx/paper-sheet-component.md` | Neu: Properties, Einstellungen, Integration, Laden/Speichern, Shortcut-Tabelle. |
| `docs/docs/fx/styling.md` | Neu: Style-Klasse, `-fx-`-Properties, Pseudo-Klassen, Default-Stylesheet, Override-Beispiel. |
| `docs/docs/fx/implementation.md` | Platzhalterhinweis entfernen, auf die drei neuen Seiten verlinken. |
| `docs/mkdocs.yml` | Navigationseinträge unter dem `fx`-Abschnitt ergänzen. |
| `CHANGELOG.md` | Eintrag unter „Unreleased". |

## Testentwurf

* Kein automatisierter Test; `project-docs`-Skill vor dem Schreiben laden und Vorgaben einhalten.
* `./gradlew :fx:build` bleibt grün (keine Codeänderung).
* MkDocs-Build lokal prüfen, falls verfügbar; alle internen Links auflösbar.
* Codebeispiele auf den Seiten gegen die tatsächliche öffentliche API abgleichen.

## Aufgaben

### Aufgabe 1 — Seite Canvas rendering

* `project-docs`-Skill laden.
* `canvas-rendering.md` mit Minimalbeispiel `Document` → `Canvas` schreiben.
* Voll- und Einzelseiten-Rendering, `CanvasDocumentRenderer.for(document) { ... }`, `CanvasRenderConfiguration`, `documentCanvasSize` und `pageCanvasSizes[pageIndex]` erklären.
* Dynamic-Growth-Ergebnis und Pixel-Limit-Hinweis aufnehmen; Unit-Scaling beschreiben.

### Aufgabe 2 — Seite Paper sheet component

* `paper-sheet-component.md` mit Einbindungsbeispiel in eine Scene schreiben.
* Alle Properties (`document`, `outerMargin`, `pageGap`, `minZoom`, `maxZoom`, `zoom`, `mode`,
  `contentSize`, `selectedText`) tabellarisch beschreiben.
* Readonly- versus Normal-Modus, Selektion und Clipboard erklären.
* Laden/Speichern eines `Document` über die `engine`-Serialisierung zeigen.
* Shortcut-Tabelle mit Taste, Aktion und Modusabhängigkeit ergänzen.

### Aufgabe 3 — Seite Styling

* `styling.md` mit Style-Klasse `paper-sheet-view` und der Liste der `-fx-`-Properties schreiben.
* Pseudo-Klassen und das Default-User-Agent-Stylesheet erklären.
* Ein vollständiges Override-Beispiel (eigenes Stylesheet) aufnehmen.

### Aufgabe 4 — Navigation, Changelog, Build, Abschluss

* `implementation.md`-Platzhalter durch Verlinkung der drei Seiten ersetzen.
* `mkdocs.yml`-Navigation um die drei Seiten erweitern.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :fx:build` ausführen.
* Im selben Change-Set: IP-06 im Status auf `COMPLETED`, im Feature Plan abhaken,
  `FP-003-Overview.md` und diese Plandatei mit `git rm` entfernen sowie
  `FP-003-Overview.md` löschen (letzter Plan des Features).

## Risiken und offene Punkte

* Genauer `mkdocs.yml`-Navigationsabschnitt für `fx` ist zu prüfen.
* Umfang der Lade-/Speicher-Beschreibung, da Serialisierung im `engine`-Modul liegt, nicht in `fx`.
* Abschluss von IP-06 entfernt die Übersichtsdatei; Reihenfolge im letzten Change-Set beachten.
