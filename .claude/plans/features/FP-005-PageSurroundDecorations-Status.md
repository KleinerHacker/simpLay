# Feature Status: Seiten-Umrandungs-Dekorationen

Status: COMPLETED

## Implementierungspläne

| ID | Implementierungsplan | Status |
|----|---------------------|--------|
| IP-01 | Gemeinsames Kanten-/Anker-Modell (ui:common) | COMPLETED |
| IP-02 | Umsetzung `PageDecoration` (fx & swing) | COMPLETED |
| IP-03 | Dokumentation & Tests | COMPLETED |

## Gesamtfortschritt

100%

## Notizen

IP-01 umgesetzt: `PageEdge`, `EdgeAlignment` (inkl. `STRETCH` als Standardwert),
`PageDecorationPlacement` und `resolveDecorationBounds` in `ui:common`, samt Unit-Tests.

IP-02 umgesetzt: `PageDecoration`-Klasse (fx mit JavaFX-Properties, FXML-fähig; swing mit
einfachen Feldern) mit flachen Feldern `pageId`, `edge`, `alignment`, `offsetX`, `offsetY` statt
eines eigenen `placement`-Objekts. `pageDecorations`-Property an `PaperSheetView` (fx und swing).
Positionierung ueber neue interne Helferklasse `PaperSheetDecorations` (fx: `fx.internal.ps`,
swing: `swing.internal.ps`), analog zu `PaperSheetOverlays`, verdrahtet in `PaperSheetViewSkin`
bzw. `BasicPaperSheetUI`. Demo-Integration in `PaperSheetDemoTab` (fx) und `PaperSheetDemoPanel`
(swing). Build (`gradlew build`) erfolgreich.

IP-03 umgesetzt: MkDocs-Seiten `page-decorations.md` (fx und swing) analog zu
`floating-overlays.md`, in `docs/mkdocs.yml` unter `Floating overlays` einsortiert.
`PageDecorationTest` je Modul (fx: `JavaFxTestBase`-Fixture; swing: Offscreen-Painting)
deckt Kantenzuordnung, Default-Alignment `STRETCH` vs. `CENTER`, `offsetX`/`offsetY`,
Scroll-/Zoom-Folgeverhalten, Koexistenz mehrerer Dekorationen und Unabhängigkeit von
`PageMode`/`PaperSheetMode` ab. Keine neuen `ui:common`-Tests noetig, da
`PageDecorationLayoutTest` (IP-01) bereits vollständig abdeckt. Kein CHANGELOG-Eintrag,
da laut `project-docs`-Skill reine Doku-/Testaenderungen ausgeschlossen sind. Build
(`gradlew build`) inkl. Kover-Verifikation erfolgreich.
