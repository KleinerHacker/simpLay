# Übersicht FP-003: JavaFX Rendering

## Feature Plan

- `.claude/plans/features/FP-003-JavaFxRendering.md`
- Status: `.claude/plans/features/FP-003-JavaFxRendering-Status.md`

## Implementierungspläne

- IP-01 FX Rendering Foundation — COMPLETED (plan file removed)
- IP-02 Canvas Renderer — `FP-003-IP-02-CanvasRenderer.md`
- IP-03 Paper Sheet Component — `FP-003-IP-03-PaperSheetComponent.md`
- IP-04 Editing And Key Commands — `FP-003-IP-04-EditingAndKeyCommands.md`
- IP-05 Paper Component Styling — `FP-003-IP-05-PaperComponentStyling.md`
- IP-06 Documentation — `FP-003-IP-06-Documentation.md`

## Reihenfolge und Voraussetzungen

- IP-01 — ABGESCHLOSSEN; keine Voraussetzung; lieferte internen Measurer, Zeichen-Walk, Größen- und Hit-Test-Helfer sowie das `fx/src/demo`-Source-Set mit leerer Drei-Reiter-Hülle (`Canvas`, `Readonly`, `Read/Write`).
- IP-02 — benötigt IP-01; ganzes Dokument oder einzelne Seite auf eine `Canvas`; ergänzt den `Canvas`-Reiter samt Toolbar.
- IP-03 — benötigt IP-01; liefert den Readonly-Modus (kein Caret, selektierbarer Text) und ergänzt den `Readonly`-Reiter samt Toolbar.
- IP-04 — benötigt IP-03; liefert den Normal-Modus (Readonly plus Editieren) inkl. Modus-Property und ergänzt den `Read/Write`-Reiter samt Toolbar.
- IP-05 — benötigt IP-03; macht die Paper-Sheet-Komponente über die Standard-JavaFX-CSS-Wege stylebar.
- IP-06 — benötigt IP-02, IP-03, IP-04, IP-05; schreibt die drei `docs/docs/fx/`-Seiten (Canvas-Rendering inkl. Einzelseite, Paper-Sheet-Nutzung + Shortcuts, Paper-Sheet-Styling).
- IP-02 und IP-03 sind untereinander unabhängig und parallelisierbar.
- IP-04 und IP-05 bauen beide auf IP-03 auf und sind untereinander parallelisierbar.
- IP-06 kommt zuletzt.
- Alles ausschließlich im Modul `fx`.
