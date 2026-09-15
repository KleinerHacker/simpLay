# Implementierungsplan: FP-004-IP-01 FLF-Format-Parser & Modell

## Aufgabe 1: Modell

* Paket `console.font` in `ui:console` (`org.pcsoft.framework.simplay.console.font`) anlegen
* Datenklasse `FlfHeader` (Hardblank, Höhe, Baseline, max. Breite, Layout-Flags)
* Datenklasse `FlfGlyph` (Zeichen, Zeilen-Raster)
* Datenklasse `FlfFont` (Family, Header, Glyphen-Map)

## Aufgabe 2: Parser

* Funktion zum Einlesen der Header-Zeile inkl. Signatur-Prüfung (`flf2a`)
* Überspringen des Kommentarblocks laut Header-Angabe
* Extraktion je Zeichenblock, Entfernen der Endmarkierungen
* Ersetzen des Hardblank-Zeichens durch Leerzeichen im Raster

## Aufgabe 3: Validierung & Fehler

* Prüfung auf konsistente Zeilenzahl je Zeichenblock
* Eigene Exception-Klasse für ungültige/unvollständige FLF-Dateien
* Tests mit gültigen und fehlerhaften Beispiel-Dateien
* Tests für Hardblank-Ersetzung und Endmarkierungs-Handling

## Abhängigkeiten

* Unabhängig

## Bezug

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`, IP-01
