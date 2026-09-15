# Implementierungsplan: FP-004-IP-04 Glyphen-Rendering-Integration

## Aufgabe 1: Zellenmaße

* `ConsoleFontMeasureCalculator` (FP-003) um FLF-Header-Ableitung erweitern
* Zeilenhöhe/Zeichenbreite aus `FlfFont`-Header statt fester Konstante

## Aufgabe 2: Rendering

* `AnsiDocumentRenderer` (FP-003) auf mehrzeilige Glyphen-Ausgabe je Textzeile anpassen
* Zusammensetzen der Glyphen-Zeilen mehrerer Zeichen zu Ausgabezeilen
* Ersatz-Raster für nicht abgedeckte Zeichen definieren und einsetzen

## Aufgabe 3: Tests

* Test: einzelnes Zeichen wird als korrektes mehrzeiliges Raster ausgegeben
* Test: nicht abgedecktes Zeichen nutzt Ersatz-Raster
* Test: Zellenmaße stimmen mit FLF-Header überein

## Abhängigkeiten

* FP-004-IP-01 (benötigt Glyphen-Modell)
* FP-003 IP-02 (benötigt bestehende Rendering-Pipeline)

## Bezug

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`, IP-04
