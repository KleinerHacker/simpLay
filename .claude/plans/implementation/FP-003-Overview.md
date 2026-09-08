# Übersicht FP-003: JavaFX Rendering

## Feature Plan

- `.claude/plans/features/FP-003-JavaFxRendering.md`
- Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Implementierungspläne

- IP-01 FX Rendering Foundation — COMPLETED (plan file removed)
- IP-02 Canvas Renderer — COMPLETED (plan file removed)
- IP-03 Paper Sheet Component — COMPLETED (plan file removed)
- IP-03 Folgeplan: TextSelectionModel — COMPLETED (plan file removed)
- IP-04 Floating Overlays — COMPLETED (plan file removed)
- IP-05 Editing And Key Commands — `FP-003-IP-05-EditingAndKeyCommands.md`
- IP-06 Paper Component Styling — `FP-003-IP-06-PaperComponentStyling.md`
- IP-07 Documentation — `FP-003-IP-07-Documentation.md`

## Reihenfolge und Voraussetzungen

- IP-01 — ABGESCHLOSSEN; keine Voraussetzung; lieferte internen Measurer, Zeichen-Walk, Größen- und Hit-Test-Helfer sowie das `fx/src/demo`-Source-Set mit leerer Drei-Reiter-Hülle (`Canvas`, `Readonly`, `Read/Write`).
- IP-02 — ABGESCHLOSSEN; benötigt IP-01; lieferte `CanvasDocumentRenderer` (per `CanvasDocumentRenderer.`for`(document) { ... }` an ein Dokument gebunden, Messen und Layout einmalig vorberechnet; ganzes Dokument oder einzelne Seite auf eine `Canvas`, gestrichelte Trennlinien), die Builder-Lambda-`CanvasRenderConfiguration`, den gefüllten `Canvas`-Reiter samt Toolbar (inkl. Blattrand-Andeutung nur in der Demo) sowie die nach `engine` gehobene `RenderConfiguration` inkl. Mess-/Größen-Erweiterungen (abgestimmter Sonderfall).
- IP-03 — ABGESCHLOSSEN; benötigt IP-01; lieferte das öffentliche `PaperSheetView : Control` (`...fx.control`): gestapelte Blätter mit Rand und Schatten in einer viewport-großen `Canvas`, `zoom` auf `[minZoom, maxZoom]` geklemmt, nur sichtbare Seiten gezeichnet, vertikaler `ScrollBar`, Maus-Textselektion über `DocumentTextIndex` + IP-01-`hitTest`, `Strg+C` kopiert Klartext, Nur-Lese `contentSize`/`selectedText`/`selectionBounds`; `TextSelection` und `PaperSheetViewSkin` bleiben `internal`; gefüllter `Readonly`-Reiter (`ReadonlyDemoTab`).
- IP-03 Folgeplan TextSelectionModel — ABGESCHLOSSEN; benötigt IP-03; lieferte das öffentliche `TextSelectionModel` (`PaperSheetView.selectionModel`) mit `text`/`startIndex`/`endIndex`/`length`/`empty`/`bounds`/`runs` (`TextSelectionData`) plus `selectRange`/`selectAll`/`clearSelection`; `selectedText`/`selectionBounds` bleiben als Delegates; `Strg+C` kopiert jetzt HTML + RTF + Klartext; `Readonly`-Reiter um „Select all"/„Clear" und Range-/Run-Anzeige erweitert.
- IP-04 — ABGESCHLOSSEN; benötigt IP-03; lieferte die FXML-kompatible Overlay-API: `FloatingOverlay`-Bean (`content`/`trigger`/`anchor`/`offsetX`/`offsetY`/`autoHide`, Nur-Lese `active`/`activeBounds`/`activeIndex`/`activeText`/`activeDocumentRange`, `onShown`/`onHidden`), `FloatingOverlayTrigger`-Enum (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET` — inert bis IP-05), `PaperSheetView.getFloatingOverlays()` (Code + FXML `<floatingOverlays>`), Nur-Lese `hoveredParagraph`/`hoveredPage` (+ `Bounds`); interne Overlay-`Pane` im Skin mit Trigger-Erkennung, Anker-/Offset-Positionierung, Scroll-/Zoom-Nachführung und Rand-Klemmung; `Readonly`-Reiter um `Copy`-Leiste und Absatz-Badge erweitert; Headless-Tests inkl. `FXMLLoader`-Test.
- IP-05 — benötigt IP-03; liefert den Normal-Modus (Readonly plus Editieren) inkl. Modus-Property und ergänzt den `Read/Write`-Reiter samt Toolbar; aktiviert den `CARET`-Trigger der Overlay-API, sofern IP-04 umgesetzt ist.
- IP-06 — benötigt IP-03 (optional IP-04 für die Overlay-Style-Klasse); macht die Paper-Sheet-Komponente über die Standard-JavaFX-CSS-Wege stylebar.
- IP-07 — benötigt IP-02, IP-03, IP-04, IP-05, IP-06; schreibt die vier `docs/docs/fx/`-Seiten (Canvas-Rendering inkl. Einzelseite, Paper-Sheet-Nutzung + Shortcuts, Floating Overlays, Paper-Sheet-Styling).
- IP-03 ist unabhängig von IP-02 und parallelisierbar.
- IP-05 und IP-06 bauen auf IP-03 auf und sind untereinander parallelisierbar.
- IP-07 kommt zuletzt.
- Alles ausschließlich im Modul `fx`.
