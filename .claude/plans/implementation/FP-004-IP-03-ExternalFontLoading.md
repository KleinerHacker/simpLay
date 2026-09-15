# Implementierungsplan: FP-004-IP-03 Externes Nachladen eigener Fonts

## Aufgabe 1: Loader-API

* Funktion zum Laden einer `.flf`-Datei von einem Dateipfad/Stream
* Rückgabe als `FlfFont`-Modell aus IP-01
* Fehlerbehandlung bei fehlender/ungültiger Datei (eigene Exception)

## Aufgabe 2: Tests

* Test: gültige externe Beispiel-Datei wird korrekt geladen
* Test: fehlende Datei löst definierten Fehler aus
* Test: ungültiges FLF-Format löst definierten Fehler aus

## Abhängigkeiten

* FP-004-IP-01 (benötigt FLF-Modell/Parser)

## Bezug

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`, IP-03
