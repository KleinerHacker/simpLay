# Implementierungsplan: FP-004-IP-05 Font-Registry-Integration & öffentliche API

## Aufgabe 1: Registry

* Font-Registry (Family → `FlfFont`) implementieren
* Mitgelieferte Fonts (IP-02) und extern geladene Fonts (IP-03) eintragen können
* Fallback-Regel: unbekannte Family liefert Standard-Font aus IP-02

## Aufgabe 2: Verdrahtung

* `DocumentAnsiRenderer`/`ConsoleRenderConfiguration` (FP-003) an Registry anbinden
* `Font.family` bei jedem Zeichen über Registry auflösen

## Aufgabe 3: Demo & Tests

* Demo-Anwendung: Dokument mit mehreren Familien inkl. unbekannter Family rendern
* End-to-End-Test: bekannte Family, unbekannte Family (Fallback), extern geladene Family
* Build-Verifikation: `./gradlew build` grün für Gesamt-Workspace

## Abhängigkeiten

* FP-004-IP-02, FP-004-IP-03, FP-004-IP-04

## Bezug

* Feature Plan: `.claude/plans/features/FP-004-ConsoleBitmapFonts.md`, IP-05
