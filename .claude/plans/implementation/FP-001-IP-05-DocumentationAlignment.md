# Umsetzungsplan FP-001-IP-05: Documentation Alignment

## Ziel

- MkDocs-Dokumentation und KDoc auf die neue Engine-API des `engine`-Moduls bringen.

## Abhängigkeiten

- IP-03 (dokumentierte API).
- IP-04 (bestätigte Persistenz-Format-Matrix).

## Entwurfsentscheidungen

- Vier eigene Engine-Seiten unter `docs/docs/engine/`.
- Einbindung in den `engine`-Abschnitt der `nav` in `docs/mkdocs.yml`.
- `buildDocs` läuft mit `--strict`; keine gebrochenen Links.
- Dokumentation folgt dem `project-docs`-Skill.
- Rendering-Seite bleibt konzeptionell und impliziert kein Plattformmodul.

## Betroffene Dateien

- `docs/docs/engine/raw-model.md`
- `docs/docs/engine/measured-model.md`
- `docs/docs/engine/simplay-engine.md`
- `docs/docs/engine/rendering.md`
- `docs/docs/engine/implementation.md` (Querverweise)
- `docs/mkdocs.yml` (`nav`)
- `CHANGELOG.md`
- KDoc in den neuen Quelldateien der Pakete `...engine.model`, `...engine.geometry`, `...engine.measure`, `...engine.engine`

## Aufgabe 1: Vorbereitung

- `project-docs`-Skill laden.
- Format-Ergebnis-Matrix aus IP-04 übernehmen.
- Öffentliche Typen und Funktionen der Engine-API auflisten.

## Aufgabe 2: Seite Rohdatenmodell

- `raw-model.md` beschreibt `Document`, `Page`, `FlowPage`, `SinglePage`, `PageLayout`.
- Beschreibt `TextBlock`, `TextPart`, `TextWord`, `TextSymbol`, `TextStyle`, `Font`, `LineSpacing`.
- Beschreibt `TextBlock.of(text)` und die `toString()`-Normalisierungsregel.
- Beschreibt die Zähl-Erweiterungsfunktionen.
- Beschreibt Persistierbarkeit und die Format-Matrix (JSON, YAML, XML, Java Serialization).
- Codebeispiel: Dokument aufbauen und als JSON speichern.

## Aufgabe 3: Seite Measured-Datenmodell

- `measured-model.md` erklärt die `by`-Dekoration je Roh-Interface.
- Beschreibt `MeasuredDocument`, `MeasuredPage`, `MeasuredTextBlock`, `MeasuredTextStyle`, `MeasuredFont`, `MeasuredTextPart`.
- Beschreibt die Zwischenebene `MeasuredLine` mit `lineBox` und `baseline`.
- Erklärt, warum das Modell nicht gespeichert wird.

## Aufgabe 4: Seite SimPLayEngine

- `simplay-engine.md` beschreibt den Builder und den `FontMeasurer`-Vertrag.
- Beschreibt greedy Zeilenumbruch, `WordBreaker`-Naht, Ausrichtung, `LineSpacing`.
- Beschreibt `FlowPage`-Seitennachschub und `SinglePage`-Höhenwachstum.
- Beschreibt Verhalten bei leerem Dokument und Seite ohne Blöcke.
- Durchgerechnetes Beispiel: Rohdokument -> `measure` -> `MeasuredDocument`.

## Aufgabe 5: Seite Rendering

- `rendering.md` erklärt das Ablaufen von Seiten, Zeilen und Teil-Rechtecken.
- Beschreibt die Nutzung von `effectiveSize`, `contentArea`, `bounds`, `baseline`.
- Beschreibt das Mapping der `Double`-Layout-Einheiten in Zieleinheiten.
- Nennt die Stelle, an der spätere Plattformmodule ansetzen.

## Aufgabe 6: Navigation, Querverweise, KDoc, Changelog

- Die vier Seiten in `docs/mkdocs.yml` unter `engine` eintragen.
- `engine/implementation.md` verweist auf die neuen Seiten; veralteten Hinweis entfernen.
- KDoc auf allen neuen öffentlichen Typen und Funktionen ergänzen.
- `CHANGELOG.md` unter `[UNRELEASED]` einen Eintrag zur Layout-Engine ergänzen.

## Testkonzept

- `./gradlew buildDocs` mit `--strict` muss fehlerfrei durchlaufen.
- `./gradlew dokkaGeneratePublicationHtml` erzeugt die API-Doku ohne Warnungen.
- Sichtprüfung der vier Seiten auf Vollständigkeit gegen die Feature Completion Criteria.

## Definition of Done

- Vier Engine-Seiten (Rohmodell, Measured-Modell, SimPLayEngine, Rendering) existieren.
- Die Seiten sind in der `mkdocs.yml`-`nav` verlinkt.
- KDoc deckt die neue öffentliche API ab.
- `buildDocs` (`--strict`) läuft grün.
- `CHANGELOG.md` enthält den Eintrag.
