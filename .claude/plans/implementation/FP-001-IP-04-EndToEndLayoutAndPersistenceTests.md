# Umsetzungsplan FP-001-IP-04: End-to-End Layout & Persistence Tests

## Ziel

- Komplett-Tests über gemischte Dokumente plus Speicher-/Ladeversuche in JSON, YAML, XML und Java Serialization.

## Abhängigkeiten

- IP-03 (funktionsfähige Engine).

## Entwurfsentscheidungen

- Tests liegen in `commonTest`; Java-Serialization-Test liegt in einem neuen `jvmTest`-Sourceset des `engine`-Moduls.
- Deterministischer Test-`FontMeasurer` wird als gemeinsames Test-Fixture bereitgestellt.
- YAML über `kaml`, XML über `xmlutil`; beide nur `testImplementation`.
- Neue Test-Abhängigkeiten und deren Lizenzen erst nach Freigabe durch den Nutzer aufnehmen.
- Ein fehlschlagender Round-Trip wird als offene Entscheidung mit dem Nutzer geklärt, nicht still umgangen.

## Betroffene Dateien

- `engine/build.gradle.kts` (Sourceset `jvmTest`, Test-Abhängigkeiten, Lizenzen)
- `gradle/libs.versions.toml` (Versionen/Bibliotheken für `kaml`, `xmlutil`)
- `engine/src/commonTest/kotlin/org/pcsoft/framework/playsim/engine/testfixture/TestFontMeasurer.kt`
- `engine/src/commonTest/kotlin/org/pcsoft/framework/playsim/engine/e2e/MixedDocumentLayoutTest.kt`
- `.../engine/e2e/PaginationAndGrowthTest.kt`
- `.../engine/e2e/JsonRoundTripTest.kt`
- `.../engine/e2e/YamlRoundTripTest.kt`
- `.../engine/e2e/XmlRoundTripTest.kt`
- `engine/src/jvmTest/kotlin/org/pcsoft/framework/playsim/engine/e2e/JvmSerializationRoundTripTest.kt`

## Aufgabe 1: Vorbereitung Build und Abhängigkeiten

- Nutzer nach Freigabe von `kaml` und `xmlutil` als Test-Abhängigkeit fragen.
- Nach Freigabe Einträge in `gradle/libs.versions.toml` ergänzen.
- `kaml` und `xmlutil` als `testImplementation` in `engine/build.gradle.kts` aufnehmen.
- Fehlende Lizenzen in die `licensee`-Allow-List des `engine`-Moduls eintragen.
- `jvmTest`-Sourceset im `engine`-Modul aktivieren.
- `./gradlew :engine:build` grün bestätigen.

## Aufgabe 2: Gemeinsames Test-Fixture

- `testing`-Skill vor dem Anlegen der Testklassen laden.
- `TestFontMeasurer` implementiert `FontMeasurer` deterministisch.
- Breite = `text.length * font.size * 0.6`, ascent = `font.size * 0.8`, descent = `font.size * 0.2`.
- Hilfsfunktion baut ein gemischtes Beispiel-Dokument (siehe Aufgabe 3).

## Aufgabe 3: Layout-Komplett-Test

- Dokument mit erster `FlowPage`: zwei Blöcke, verschiedene Fonts, `LEFT` und `JUSTIFY`, verschiedene `LineSpacing`.
- Zweite Seite `SinglePage` mit kleiner Layouthöhe und langem Block.
- Dritte `FlowPage` mit sehr langem Text über mehrere Seiten.
- Engine mit `TestFontMeasurer` ausführen.
- Prüfen: Umbruch in mehrere `MeasuredLine` je Block.
- Prüfen: `FlowPage` erzeugt mehr als eine `MeasuredFlowPage`.
- Prüfen: `SinglePage.effectiveSize.height` größer als Layouthöhe.
- Prüfen: Ausrichtungs-Offsets und letzte Zeile bei `JUSTIFY`.
- Prüfen: Zähl-Erweiterungen auf dem Dokument.

## Aufgabe 4: Leerfall und Sonderfälle

- `EmptyDocument` liefert leeres `MeasuredDocument`.
- Seite ohne Blöcke liefert leere Measured-Seite.
- Überlanges Einzelwort bleibt ungebrochen in eigener Zeile.

## Aufgabe 5: Persistenz-Round-Trips

- `JsonRoundTripTest`: `Json.encodeToString`/`decodeFromString` für das Rohdokument, `assertEquals`.
- `YamlRoundTripTest`: gleicher Round-Trip über `kaml`.
- `XmlRoundTripTest`: gleicher Round-Trip über `xmlutil`, Polymorphie von `Page`/`TextPart` prüfen.
- `JvmSerializationRoundTripTest`: `ObjectOutputStream`/`ObjectInputStream` für das Rohdokument.
- Bei `NotSerializableException` oder Formatfehler: Fall dokumentieren und Nutzerentscheidung einholen.
- Ergebnis-Matrix (Format -> funktioniert/geklärt) für IP-05 festhalten.

## Definition of Done

- `./gradlew :engine:build` ist grün, `licensee` meldet keine fehlende Lizenz.
- Ein oder mehrere Komplett-Tests über gemischte Seiten, Layouts, Blöcke und Stile laufen grün.
- JSON-, YAML- und XML-Round-Trip des Rohdokuments sind verlustfrei oder als Entscheidung dokumentiert.
- Java-Serialization-Round-Trip funktioniert oder die Einschränkung ist mit dem Nutzer geklärt.
- Die Format-Ergebnis-Matrix liegt für IP-05 vor.
