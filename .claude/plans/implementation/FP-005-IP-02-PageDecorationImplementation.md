# Implementierungsplan: FP-005-IP-02 Umsetzung `PageDecoration` (fx & swing)

## Aufgabe 1: fx `PageDecoration`-Klasse

* Klasse `PageDecoration` in `ui:fx` mit `content: Node?`, `pageId: String`, `edge: PageEdge` anlegen
* Properties `alignment`, `offsetX`, `offsetY` analog zu `FloatingOverlay` ergänzen
* No-Argument-Konstruktor für FXML-Fähigkeit sicherstellen
* KDoc analog zur `FloatingOverlay`-Klasse ergänzen

## Aufgabe 2: fx `pageDecorations`-Property

* `ObservableList<PageDecoration>` als `pageDecorations` an `PaperSheetView` (fx) ergänzen
* FXML-Unterstützung als `<pageDecorations>`-Kindelement sicherstellen
* KDoc mit Registrierungsbeispiel analog `floatingOverlays` ergänzen

## Aufgabe 3: fx Skin-Integration

* Positionierung je Dekoration in `PaperSheetViewSkin` mittels `resolveDecorationBounds` umsetzen
* Bindung an Seiten-Layout, Scroll und Zoom analog zum `FloatingOverlay`-Handling umsetzen
* `pageId` gegen aktuelle Seitenliste auflösen, unbekannte Ids ignorieren
* Dekorations-Nodes bei Listenänderung hinzufügen und entfernen

## Aufgabe 4: fx Styling

* Style-Klasse für den Dekorations-Container vergeben
* `paper-sheet-view.css` bei Bedarf um Layout-relevante Regeln ergänzen

## Aufgabe 5: swing `PageDecoration`-Klasse

* Klasse `PageDecoration` in `ui:swing` mit `content: JComponent?`, `pageId`, `edge` anlegen
* Felder `alignment`, `offsetX`, `offsetY` analog zur swing-`FloatingOverlay` ergänzen
* KDoc als Pendant zur fx-Klasse ergänzen

## Aufgabe 6: swing `pageDecorations`-Property

* Liste `pageDecorations` an `PaperSheetView` (swing) ergänzen
* KDoc mit Registrierungsbeispiel ergänzen

## Aufgabe 7: swing UI-Delegate-Integration

* Positionierung je Dekoration in `BasicPaperSheetUI`/`PaperSheetUI` mittels `resolveDecorationBounds` umsetzen
* Bindung an Seiten-Layout, Scroll und Zoom analog zum swing-`FloatingOverlay`-Handling umsetzen
* `pageId` gegen aktuelle Seitenliste auflösen, unbekannte Ids ignorieren
* Dekorations-Components bei Listenänderung hinzufügen und entfernen

## Aufgabe 8: Demo-Integration

* Beispiel-Dekoration in `PaperSheetDemoTab` (fx) ergänzen
* Beispiel-Dekoration in `PaperSheetDemoPanel` (swing) ergänzen

## Abhängigkeiten

* FP-005 IP-01

## Bezug

* Feature Plan: `.claude/plans/features/FP-005-PageSurroundDecorations.md`, IP-02
