# Implementierungsplan: FP-004-IP-02 Mitgelieferter Font-Satz

## Aufgabe 1: Standard-Font

* Eigenen FLF-Standard-Font entwerfen (Basis-Zeichensatz: Latin, Ziffern, Satzzeichen)
* Als `.flf`-Ressource in `ui:console` ablegen
* Als Standard-/Fallback-Font eindeutig kennzeichnen

## Aufgabe 2: Laden & Tests

* Ressourcen-Verzeichnis für mitgelieferte Fonts anlegen
* Ladefunktion für alle mitgelieferten `.flf`-Ressourcen beim Modul-Start
* Test: Standard-Font lädt fehlerfrei und deckt Basis-Zeichensatz ab
* Test: Parser aus IP-01 verarbeitet den Standard-Font korrekt

## Abhängigkeiten

* FP-004-IP-01 (benötigt FLF-Modell/Parser)

## Bezug

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`, IP-02
