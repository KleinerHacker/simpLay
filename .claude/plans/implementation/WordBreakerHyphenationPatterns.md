# Implementierungsplan: Silbengenauer WordBreakerStrategy (Liang-Trennmuster)

## Aufgabe 1: Resource-Infrastruktur im Engine-Modul

* `kotlin.sourceSets.commonMain.resources` im Kotlin-Multiplatform-Plugin aktivieren, kein Drittanbieter-Dependency
* Generierten `Res`-Zugriff (Kotlin-eigene Resource-API) fuer jvm-, js- und nativeMain-Targets pruefen
* Verzeichnis `engine/src/commonMain/resources/org/pcsoft/framework/simplay/engine/hyphenation/` anlegen
* `licensee`-Konfiguration in `engine/build.gradle.kts` um noetige Lizenz der Pattern-Quelle ergaenzen

## Aufgabe 2: Sprachspezifische Trennmuster-Dateien beschaffen

* Quelle: github.com/hyphenation/tex-hyphen, Verzeichnis `hyph-utf8/tex/generic/hyph-utf8/patterns/txt/`
* Zielsprachen: alle dort vorhandenen Sprachcodes aus der vom Nutzer genannten Locale-Liste
* Je Sprache zuerst zugehoerige `.tex`-Loader-Datei abrufen, `licence:`-Kopf pruefen (Lizenz steht NICHT in der `.txt`-Datei)
* Nur bei permissiver Lizenz uebernehmen: MIT, BSD, Public Domain, LPPL 1.2+/1.3+
* Bei Copyleft-Lizenz (z. B. GPL, AGPL) oder unklarer Lizenz: Datei auslassen, Nutzer sofort waehrend der Umsetzung informieren
* Uebernommene `.pat.txt`-Dateien unveraendert in das Resource-Verzeichnis legen, benannt `hyph-<code>.pat.txt`
* Lizenz- und Quellenhinweis je Datei in einer begleitenden `LICENSES.md` im Resource-Verzeichnis dokumentieren (englisch)
* Format je Datei: eine Zeile pro Pattern, Ziffern zwischen Buchstaben kodieren Trennwahrscheinlichkeit

## Aufgabe 3: Liang-Hyphenator implementieren

* Neue Datei `engine/.../hyphenation/LiangPatternParser.kt`: liest Pattern-Textzeilen, baut Trie-Struktur
* Neue Datei `engine/.../hyphenation/LiangHyphenator.kt`: berechnet Trennstellen fuer ein Wort per Trie-Lookup
* Mindest-Trennabstand vom Wortanfang/-ende beruecksichtigen (Standard: 2 Zeichen, wie im TeX-Algorithmus ueblich)
* Kein Caching-Zwang, aber Parser-Ergebnis (Trie) pro Sprache wiederverwendbar halten

## Aufgabe 4: WordBreakerStrategy-Implementierung

* Neue Datei `engine/.../strategy/PatternWordBreakerStrategy.kt`, implementiert `WordBreakerStrategy`
* Konstruktor nimmt Locale/Sprachcode entgegen, laedt zugehoerige Resource-Pattern-Datei ueber `Res`
* `breakOffsets(...)` liefert Trie-Ergebnis von `LiangHyphenator`, gefiltert auf `1 until word.length`
* Unbekannter Sprachcode: leere Liste liefern (Verhalten wie `NoOpWordBreakerStrategy`), keine Exception
* Bestehendes Seam-Verhalten (`GreedyWordLineBreakerStrategy.placeHyphenated`) bleibt unveraendert nutzbar

## Aufgabe 5: Tests

* Vor Testerstellung: `testing`-Skill laden
* Parser-Test: bekannte Trennmuster-Zeilen ergeben erwartete Trie-Eintraege
* Hyphenator-Test: bekannte deutsche und englische Woerter ergeben erwartete Trennstellen
* Strategy-Test: `PatternWordBreakerStrategy` liefert stabile Offsets, unbekannte Sprache liefert leere Liste
* Integrationstest: `GreedyWordLineBreakerStrategy` mit `PatternWordBreakerStrategy` trennt ein langes Wort silbengenau

## Aufgabe 6: Dokumentation

* Vor Aenderung: `project-docs`-Skill laden
* KDoc fuer `PatternWordBreakerStrategy`, `LiangHyphenator`, `LiangPatternParser` ergaenzen (englisch)
* CHANGELOG.md um neuen WordBreakerStrategy-Eintrag ergaenzen
* README/MkDocs-Seite zur Silbentrennung ergaenzen, falls WordBreakerStrategy dort bereits dokumentiert ist

## Aufgabe 7: Build

* `./gradlew :engine:build` ueber Agent ausfuehren (langlaufend, nicht inline)
* Bei fehlender Lizenzfreigabe: Rueckfrage an Nutzer gemaess Dependency-Regel
