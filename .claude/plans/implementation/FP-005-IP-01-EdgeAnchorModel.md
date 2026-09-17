# Implementierungsplan: FP-005-IP-01 Gemeinsames Kanten-/Anker-Modell (ui:common)

## Aufgabe 1: Kanten-Enum

* Enum `PageEdge` mit `TOP`, `BOTTOM`, `LEFT`, `RIGHT` in `ui:common` anlegen
* Paket `org.pcsoft.framework.simplay.uicommon` verwenden
* Kurze KDoc je Konstante ergänzen

## Aufgabe 2: Ausrichtung entlang der Kante

* Enum `EdgeAlignment` mit `START`, `CENTER`, `END` anlegen
* Datenklasse `PageDecorationPlacement` mit Kante, Ausrichtung, Offset-X, Offset-Y anlegen
* KDoc analog zu `PageMode`/`EditableRegions` ergänzen

## Aufgabe 3: Positionsberechnung

* Reine Funktion `resolveDecorationBounds` in `ui:common` implementieren
* Eingabe: Seiten-Box, `PageDecorationPlacement`, Dekorationsgröße, Zoom-Faktor
* Ausgabe: Ziel-Rechteck der Dekoration in Viewport-Koordinaten
* Kein Bezug zu JavaFX- oder Swing-Typen in der Signatur

## Aufgabe 4: Tests

* Unit-Tests je Kante und Ausrichtung anlegen, Paketstruktur spiegeln
* Randfalltests für negative/positive Offsets und unterschiedliche Zoom-Faktoren ergänzen
* Testdaten und KDoc je Testmethode auf Englisch gemäß Testing-Skill verfassen

## Abhängigkeiten

* Unabhängig

## Bezug

* Feature Plan: `.claude/plans/features/FP-005-PageSurroundDecorations.md`, IP-01
