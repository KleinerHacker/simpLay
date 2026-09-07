# FP-002 / IP-03: Explicit Break Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## Ziel

* Explizites Umbruch-Token `TextBreak` (`TextPart`) im Rohmodell; Tokenizer erzeugt es für `\n`.
* `ExplicitBreakLineBreakerStrategy`, die nur an diesen Tokens umbricht, nie auf Breite; Opt-in über Builder.
* Neues Token übersteht alle vorhandenen Serialisierungs-Round-Trips.

## Umfang

### Enthalten

* Neuer versiegelter Untertyp `TextBreak` von `TextPart` in `engine.model`, mit `@SerialName("break")`.
* Tokenizer-Änderung: Newline (`\n`, `\r\n` zu einem zusammengefasst) erzeugt ein `TextBreak`;
  übriges Whitespace-Verhalten unverändert.
* `kotlinx.serialization`-Verdrahtung für JSON / YAML / XML plus JVM-(Java-)Serialisierung.
* Zählung (`wordCount`, `symbolCount`, `charCount`) und `TextBlock.toString()`-Behandlung.
* Measured-Modell-Durchreichung: `TextBreak` wird nicht gemessen, erzeugt kein `MeasuredTextPart`.
* `ExplicitBreakLineBreakerStrategy` in `engine.engine`, builder-wählbar.
* Tests: Tokenizer, Zählung, `toString`, JSON/YAML/XML/JVM-Round-Trips, Strategieverhalten, Builder-Swap.

### Nicht enthalten

* Breitenbasiertes Umbrechen in `ExplicitBreakLineBreakerStrategy` (bricht nie auf Breite).
* Silbentrennung, balanciertes Umbrechen, Break-Opportunity-Klassen.
* Öffentliche API an `TextBlock.of`, um Umbrüche anders als über `\n` im Quelltext einzufügen.
* Renderseitige Änderungen über „kein Measured-Part erzeugt" hinaus.

## Abhängigkeiten

* Unabhängig innerhalb FP-002, parallel zu IP-01 und IP-02.
* Externe Voraussetzung: FP-001/IP-03-Naht.
* Berührt `engine.model` (versiegelte Hierarchie, FP-001/IP-01) — mit den Rohmodell-Eignern abstimmen;
  einziger FP-002-Plan, der das Rohmodell ändert.

## Schnittstellen zu anderen Plänen

* Liefert das rohe `TextBreak`-Token an jeden `engine.model`-Konsumenten (Zählung, `toString`,
  Serialisierung, Measure-Pipeline).
* Andere FP-002-Strategien müssen `TextBreak` in ihrer `parts`-Eingabe tolerieren:
  IP-01 / IP-02 behandeln `TextBreak` als harten Umbruchpunkt (aktuelle Zeile umbrechen, Token verwerfen).
* Dieser Plan ergänzt einen geteilten `internal`-Helfer `List<TextPart>.splitAtBreaks()` in `engine.engine`,
  von allen drei Strategien genutzt; landen IP-01/IP-02 zuerst, setzen sie ein TODO und dieser Plan verdrahtet.
* Verbraucht die FP-001-Naht unverändert.

## Entwurf

### `TextBreak`-Token

```kotlin
@Serializable
@SerialName("break")
data object TextBreak : TextPart {
    override val text: String get() = "\n"
}
```

* `data object` — es gibt nur eine Art Umbruch; hält `equals`/`hashCode` frei und Serialisierung als bloßen Diskriminator.
* `text == "\n"`, damit `charCount` und `toString` wohldefiniert bleiben.
* Alternative `text == ""` verworfen: `charCount` würde es ignorieren, ein Umbruch ist ein Quellzeichen.

### Tokenizer

* In `tokenize(text)`: bei `\n` (nach Zusammenfassen eines vorangehenden `\r`) `flushWord()`, dann `parts += TextBreak`.
* Läufe von Newlines erzeugen ein `TextBreak` je Newline (Leerzeile ⇒ zwei).
* Alle anderen `Char.isWhitespace()` trennen weiterhin nur Parts, ohne gespeichert zu werden.
* `TextBlock.of` bleibt in der Signatur unverändert.

### `toString()`

* Aktuell setzt `TextBlock.toString()` ein Leerzeichen vor jedes nicht-erste `TextWord`.
* Neue Regel: ein `TextWord` direkt nach einem `TextBreak` erhält kein führendes Leerzeichen;
  ein `TextBreak` hängt sein `"\n"` an. Symbole unverändert.

### Zählung

* `wordCount` / `symbolCount` unverändert (filtern nach konkretem Typ; `TextBreak` zählt als keines).
* `charCount` zählt `TextBreak.text.length == 1`; KDoc-Hinweis ergänzen.
* Bestehende `CountingTest`-Erwartungen ohne Umbrüche bleiben gültig.

### Serialisierung

* JSON (`kotlinx.serialization`): `TextBreak` über den versiegelten `TextPart`-Serializer; Diskriminator `"break"`.
* `data object` serialisiert als `{"type":"break"}`.
* YAML / XML Round-Trip (`e2e/YamlRoundTripTest`, `e2e/XmlRoundTripTest`): Dokument mit `\n` ergänzen,
  Gleichheit nach Decode prüfen; XML-Umgang mit reinem Diskriminator-Objekt bestätigen.
* JVM-Java-Serialisierung (`PlatformSerializable`, `jvmTest/e2e/JvmSerializationRoundTripTest`):
  `data object` muss nach Deserialisierung auf dasselbe Singleton auflösen; Fall ergänzen.

### Measured-Modell-Durchreichung

* Line-Breaker konsumieren `TextBreak` und geben es nie in eine `UnplacedLine`;
  `SimpLayBlockEngine` und `MeasuredTextPart` brauchen keine Änderung.
* Guard/Assert im Block-Engine-Pfad nur, falls ein verirrtes `TextBreak` ihn erreichen könnte.
* Default-Strategien greedy/character/no-wrap müssen `TextBreak` überspringen (harter Umbruch für
  greedy/character, ignorieren für no-wrap) — kleine Änderung an `LineBreakerStrategy.kt`, per Regressionstests abgedeckt.

### `ExplicitBreakLineBreakerStrategy`

* `parts` an jedem `TextBreak` in Segmente teilen (Helfer `splitAtBreaks()`).
* Jedes Segment wird genau eine `UnplacedLine` unabhängig von `maxWidth` (kein Umbruch),
  gemessen mit denselben `spaceBefore`-/Ascent-/Descent-Regeln wie `NoWrapLineBreakerStrategy`.
* Leeres Segment (aufeinanderfolgende Umbrüche) erzeugt eine leere Zeile mit Font-Metrik-Ascent/Descent,
  damit Leerzeilen vertikalen Raum einnehmen.
* `WordBreakerStrategy` wird akzeptiert, aber ignoriert.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/.../engine/model/TextPart.kt` | `TextBreak` `data object`, KDoc. Ein `Write`. |
| `engine/.../engine/model/TextBlock.kt` | Tokenizer erzeugt `TextBreak` für `\n`; `toString()`-Abstandsregel. Ein `Write`. |
| `engine/.../engine/model/Counting.kt` | KDoc-Hinweis an `charCount`; keine Logikänderung (verifizieren). |
| `engine/.../engine/engine/LineBreakerStrategy.kt` | Default-Strategien überspringen/harter Umbruch bei `TextBreak`; KDoc-Liste nennt neue Strategie. |
| `engine/.../engine/engine/ExplicitBreakLineBreakerStrategy.kt` | Neu: Strategie plus `splitAtBreaks()`-Helfer. |
| `engine/.../engine/PlatformSerializable.kt` (jvm) | Nur falls `data object` ein explizites `readResolve` braucht; zuerst bestätigen. |
| `engine/src/commonTest/.../model/TokenizerTest.kt` | Fälle für `\n`, `\r\n`, Leerzeilen. |
| `engine/src/commonTest/.../model/CountingTest.kt` | `charCount` mit Umbrüchen. |
| `engine/src/commonTest/.../model/TextBlockTest.kt` | `toString()` mit Umbrüchen. |
| `engine/src/commonTest/.../model/SerializationTest.kt` | JSON-Round-Trip mit einem Umbruch. |
| `engine/src/commonTest/.../e2e/YamlRoundTripTest.kt`, `XmlRoundTripTest.kt`, `JsonRoundTripTest.kt` | Dokument mit `\n`. |
| `engine/src/jvmTest/.../e2e/JvmSerializationRoundTripTest.kt` | `data object`-Singleton-Round-Trip. |
| `engine/src/commonTest/.../engine/ExplicitBreakLineBreakerStrategyTest.kt` | Neu: Verhalten plus Builder-Swap. |
| `docs/` (Rohmodell- plus Engine-Strategieseite) | `TextBreak` und Strategie dokumentieren; `project-docs`-Skill laden. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Testentwurf

* `testing`-Skill zuerst laden; Entwicklertests, paketgespiegelt, deterministischer Measurer-Double.
* Tokenizer: `newlineBecomesSingleTextBreak`, `crlfBecomesSingleTextBreak`,
  `blankLineBecomesTwoTextBreaks`, `spacesStillNotStored`.
* Zählung: `charCountIncludesTextBreak`, `wordAndSymbolCountIgnoreTextBreak`.
* `toString`: `breakRendersAsNewlineWithoutExtraSpace`.
* Serialisierung: `textBreakRoundTripsJson`, `...Yaml`, `...Xml`, `...JvmSerialization` (Singleton-Identität erhalten).
* Strategie: `emptyPartsProduceNoLines`, `oneLinePerSegment`, `neverWrapsOnWidthEvenWhenOverflowing`,
  `consecutiveBreaksProduceEmptyLine`, `wordBreakerIsIgnored`, `deterministicAcrossRuns`, `swapViaBuilderRoutesToStrategy`.
* Regression: `greedyStrategyTreatsTextBreakAsHardBreak`, `noWrapStrategyIgnoresTextBreakGracefully`,
  bestehende FP-001-Tests unverändert.

## Aufgaben

### Aufgabe 1 — Rohmodell-Token

* `TextBreak` `data object` in `TextPart.kt` mit KDoc ergänzen (ein `Write`).
* `tokenize` in `TextBlock.kt` anpassen: `\n` / `\r\n` ⇒ ein `TextBreak`; `toString()`-Abstandsregel anpassen (ein `Write`).
* KDoc-Hinweis an `charCount` in `Counting.kt`; prüfen, dass keine Logikänderung nötig ist.

### Aufgabe 2 — Serialisierungs-Verdrahtung und Verifikation

* Bestätigen, dass der versiegelte `TextPart`-JSON-Diskriminator `TextBreak` erfasst (`@SerialName("break")`).
* Prüfen, ob YAML- und XML-Format ein reines Diskriminator-Objekt handhaben; Konfiguration bei Bedarf anpassen.
* Prüfen, ob JVM-`data object` zum Singleton deserialisiert; `readResolve` nur bei Bedarf ergänzen.

### Aufgabe 3 — Line-Breaker-Änderungen

* `ExplicitBreakLineBreakerStrategy.kt` mit `splitAtBreaks()`-Helfer und no-wrap-artigem Segment-zu-Zeile-Aufbau
  inklusive Leerzeilen-Behandlung anlegen.
* Default-Strategien in `LineBreakerStrategy.kt` `TextBreak` als harten Umbruch (greedy, character) behandeln
  oder überspringen (no-wrap) lassen; KDoc-Strategieliste aktualisieren.

### Aufgabe 4 — Tests

* `testing`-Skill laden.
* Tokenizer-, Zählungs-, `toString`-, JSON/YAML/XML/JVM-Round-Trip-Tests ergänzen bzw. erweitern.
* `ExplicitBreakLineBreakerStrategyTest` und die Regressionsfälle aus Abschnitt „Testentwurf" ergänzen.

### Aufgabe 5 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; `TextBreak` auf der Rohmodellseite und die Strategie auf der Engine-Strategieseite dokumentieren.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :engine:build` ausführen; Befunde beheben.
* Im selben Change-Set: Status IP-03 `COMPLETED` und Feature `COMPLETED`, wenn letzter Plan;
  IP-03 in `FP-002-AdvancedLineBreaking.md` abhaken, diese Plandatei mit `git rm` entfernen und,
  wenn letzter verbliebener Plan, `git rm FP-002-Overview.md`.

## Risiken und offene Punkte

* `text`-Wert für `TextBreak` (`"\n"` vs `""`) steuert `charCount` und `toString`;
  `"\n"` gewählt, bei Doppelzählung eines Konsumenten neu bewerten.
* XML-Format kann ein reines Diskriminator-Element ablehnen oder verstümmeln — spiegelt die FP-001/IP-01-Sorge;
  Fallback ist ein Dummy-Body-Feld für `TextBreak`, in Aufgabe 2 entschieden.
* JVM-`data object`-Singleton-Identität nach Java-Deserialisierung ist Kotlin-versionsabhängig;
  ein explizites `readResolve` kann nötig sein.
* Reihenfolge mit IP-01/IP-02: landen sie zuerst, müssen `splitAtBreaks()` und das „harter Umbruch"-Verhalten
  hier nachgerüstet werden; landet dieser Plan zuerst, ist der Helfer schon vorhanden.
* Vertikaler Abstand von Leerzeilen hängt vom korrekten Font-Metrik-Ascent/Descent-Fallback im
  `LineAccumulator` ab; per `consecutiveBreaksProduceEmptyLine` abgedeckt.
