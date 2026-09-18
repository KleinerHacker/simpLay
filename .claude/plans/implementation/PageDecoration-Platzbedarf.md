# PageDecoration Platzbedarf beruecksichtigen

## Neue Datei ui-common: PageDecorationReservation.kt
* Datenklasse `EdgeReservation(top, bottom, left, right: Double)` anlegen
* Funktion `computePageDecorationReservation(placements: List<Triple<PageEdge, Size, Boolean>>): EdgeReservation`
* Nur Dekorationen mit Reservierungs-Flag `true` in Aggregation einbeziehen
* Aggregation je Kante als Maximum ueber alle beruecksichtigten Dekorationen
* Funktion `resolveReservedMargins(outerMargin, gap, reservation): EdgeReservation`
* Effektive Kante = `max(outerMargin, reservationWert)`, kein additives Verhalten
* KDoc auf Englisch gemaess Regel fuer Datei-Inhalte

## Anpassung ui-common: PageDecorationPlacement.kt
* KDoc aktualisieren: Band waechst bei Bedarf statt fixem outerMargin
* Keine Feldaenderung an `PageDecorationPlacement` selbst

## Anpassung FX: PaperSheetDecorations.kt
* Vorhandenes `node.isManaged` als Reservierungs-Flag auswerten, nicht ueberschreiben
* `isManaged = true` (Default) reserviert Platz, `isManaged = false` nur Overlay
* Cache `Node -> Size` fuer gemessene Dekorationsgroessen einfuehren
* Methode `reservation(): EdgeReservation` ergaenzen, nutzt `computePageDecorationReservation`
* Messung ueber `prefWidth`/`prefHeight` vor Layout auffrischen
* Callback `onReservationChanged` bei Groessenaenderung ueber Epsilon-Schwelle ausloesen
* ClipRect an reservierte Bandbreite statt reinem Viewport anpassen
* `layout(...)` Signatur um reservierte Raender erweitern

## Anpassung FX: PaperSheetViewSkin.kt
* `computeLayoutMetrics()`: `decorations.reservation()` vor Metrikberechnung abfragen
* Effektive Raender ueber `resolveReservedMargins` bestimmen und zwischenspeichern
* `updateContentSize(...)` mit effektiven Top/Bottom/Left/Right Werten aufrufen
* Interne Seitenabstaende (pageGap) global um Top/Bottom Reservierung erweitern
* `layoutChildren()`: reservierte Raender an `decorations.layout(...)` uebergeben
* `updateScrollBar()`: `totalScaled` mit effektiven Raendern statt `2*outerMargin` berechnen
* Hit-Testing-Stellen (~Zeile 477-579): `outerMargin` durch effektive Werte ersetzen
* `onReservationChanged` an `skinnable.requestLayout()` binden

## Anpassung Swing: PageDecoration.kt
* Neues Property `var reserveSpace: Boolean = true` ergaenzen
* KDoc: Aequivalent zu FX `node.isManaged` fuer Platzreservierung

## Anpassung Swing: PaperSheetDecorations.kt (internal.ps)
* Cache `Component -> Dimension` fuer gemessene Dekorationsgroessen einfuehren
* Nur Dekorationen mit `reserveSpace = true` in Reservierung einbeziehen
* Methode `reservation(): EdgeReservation` analog zu FX ergaenzen
* Invalidierungs-Callback an `view.revalidate()` binden
* `DecorationLayer` Bounds/Clip an reservierte Bandbreite anpassen
* `layout(...)` Signatur um reservierte Raender erweitern

## Anpassung Swing: BasicPaperSheetUI.kt
* `computeLayoutMetrics()`: `decorations.reservation()` abfragen und effektive Raender bestimmen
* `view.updateContentSize(...)` mit effektiven Raendern aufrufen
* `relayoutViewport()`: reservierte Raender an `decorations.layout(...)` uebergeben
* `updateScrollBar()`: `total` mit effektiven Raendern statt `2*outerMargin` berechnen
* Hit-Testing-Stellen (~Zeile 496-602): `outerMargin` durch effektive Werte ersetzen

## Pruefung abhaengiger Klassen FX und Swing
* `PaperSheetSelection`, `PaperSheetCaret`, `PaperSheetHoverTracker`, `PaperSheetScroll` pruefen
* Falls `outerMargin` direkt gelesen wird, auf effektive Randwerte umstellen
* Nur anpassen, wenn eigenstaendiger Zugriff auf `outerMargin` vorliegt

## Demo FX: PaperSheetDemoTab.kt
* Bestehende TOP-Dekoration mit Inhalt groesser als `outerMargin` versehen
* Zusaetzliche LEFT- und BOTTOM-Dekoration zur Demonstration ergaenzen
* Erlaeuternden Hinweistext in der Demo-UI ergaenzen

## Demo Swing: PaperSheetDemoPanel.kt
* Bestehende TOP-Dekoration mit Inhalt groesser als `outerMargin` versehen
* Zusaetzliche LEFT- und BOTTOM-Dekoration zur Demonstration ergaenzen
* Erlaeuternden Hinweistext in der Demo-UI ergaenzen

## Tests ui-common: PageDecorationLayoutTest.kt
* Test fuer `computePageDecorationReservation` mit leerer Liste
* Test fuer Maximum-Aggregation mehrerer Dekorationen je Kante
* Test fuer `resolveReservedMargins` bei Wert unter und ueber `outerMargin`

## Tests FX: PageDecorationTest.kt
* Test: uebergrosse TOP-Dekoration wird nicht mehr geclippt
* Test: uebergrosse LEFT- und BOTTOM-Dekoration analog pruefen
* Test: Seitenposition verschiebt sich passend zur reservierten Flaeche
* Test: `isManaged = false` verhindert Platzreservierung, reines Overlay

## Tests Swing: PageDecorationTest.kt
* Gleiche Szenarien wie FX fuer Swing-Modul spiegeln
* Test: `reserveSpace = false` verhindert Platzreservierung, reines Overlay

## Tests FX: PaperSheetViewSkinTest.kt und PaperSheetViewTest.kt
* Test: `contentSize`/Scrollbar-Maximum waechst bei grosser Dekoration
* Test: Werte kehren nach Entfernen der Dekoration zurueck
* Regressionstest: unveraendertes Layout ohne Dekorationen sicherstellen

## Build und Abschluss
* Gradle-Build ueber Agent im Hintergrund ausfuehren (`build` Target)
* Plan-Datei nach Abschluss mit `git rm` aus `.claude/plans/implementation` entfernen
