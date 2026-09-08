# FP-002 / IP-01: Balanced Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## Ziel

* `BalancedLineBreakerStrategy` als blockweiter `LineBreakerStrategy` mit minimaler Gesamt-Raggedness.
* Opt-in über `SimpLayEngine.Builder.lineBreakerStrategy(...)`; Default bleibt `GreedyWordLineBreakerStrategy`.

## Umfang

### Enthalten

* Neues `BalancedLineBreakerStrategy`-Objekt in `org.pcsoft.framework.simplay.engine.engine`.
* Interne Badness-/Kostenfunktion über Kandidaten-`UnplacedLine`s.
* Interne dynamische Programmierung über alle legalen Umbruchpunkte, nur Wortgranularität.
* Wiederverwendung von `WordBreakerStrategy` für Wörter breiter als `maxWidth`, gleicher Vertrag wie Greedy.
* Interne Kostenkonstanten (Füll-Strafe-Exponent, Behandlung der letzten Zeile, Überlauf-Strafe).
* Tests, die Raggedness gegen `GreedyWordLineBreakerStrategy` auf einem festen Dokument vergleichen.

### Nicht enthalten

* Silbentrennungslogik selbst; nur die vorhandene `WordBreakerStrategy`-Naht wird konsultiert.
* Jede Änderung an `GreedyWordLineBreakerStrategy` oder den Builder-Defaults.
* Änderungen an Pagination / `SimpLayPageEngine`; Ausgabe bleibt flache `List<UnplacedLine>`.
* Öffentliche Konfiguration der Kostenparameter; vorerst interne Konstanten.
* Änderungen am Rohmodell.

## Abhängigkeiten

* Unabhängig innerhalb FP-002, parallel zu IP-02 und IP-03.
* Externe Voraussetzung: `LineBreakerStrategy`-Naht aus FP-001/IP-03 (`LineBreakerStrategy`,
  `UnplacedLine`, `UnplacedPart`, `WordBreakerStrategy`, `FontMeasureCalculator`).

## Schnittstellen zu anderen Plänen

* Verbraucht `LineBreakerStrategy`, `UnplacedLine`, `UnplacedPart`, `WordBreakerStrategy`,
  `FontMeasureCalculator`, `MeasuredFont` unverändert.
* Liefert keinen neuen geteilten Typ; `SimpLayEngine.Builder` akzeptiert bereits jede `LineBreakerStrategy`.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/BalancedLineBreakerStrategy.kt` | Neu: Objekt, DP, Kostenfunktion, Konstanten, KDoc. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/LineBreakerStrategy.kt` | Nur KDoc: `BalancedLineBreakerStrategy` in der Strategieliste nennen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/engine/BalancedLineBreakerStrategyTest.kt` | Neu: Verhaltens- und Raggedness-Tests. |
| `docs/` (Engine-Strategieseite) | `BalancedLineBreakerStrategy` zur Strategieliste; zuvor `project-docs`-Skill laden. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

### Algorithmus

* Block als geordnete Liste umbrechbarer Items: jeder `TextPart` plus die Pflicht-Glue (Leerzeichen)
  vor jedem `TextWord` außer dem ersten; `TextSymbol` trägt kein führendes Leerzeichen (wie Greedy).
* Legale Umbruchpunkte sind die Lücken vor einem `TextWord`.
* Ein Lauf `symbol* word` wird nicht vor seinen Symbolen getrennt; Symbole bleiben am Zeilenstart
  des folgenden Wortes, wie die Greedy-Anlagerung.
* `cost[i]` = minimale Gesamt-Badness für den Umbruch der Items `0 until i` in Zeilen.
* `cost[0] = 0`; `cost[n]` ist die Antwort; Back-Pointer rekonstruieren die gewählten Umbrüche.
* Natürliche Breite einer Kandidatenzeile `j until i` aus den `measurer.measure(font.raw, part.text)`-Advances
  plus einer Leerzeichenbreite je interner Lücke (Leerzeichenbreite einmal pro Aufruf gemessen).
* Zeilen-Badness: `slack = maxWidth - naturalWidth`.
* Für `slack >= 0` und nicht letzte Zeile des Blocks:
  `badness = (slack / maxWidth).pow(FILL_PENALTY_EXP) * FILL_PENALTY_SCALE`.
* Für `slack < 0` (Überlauf): `badness = (-slack) * OVERFLOW_PENALTY`, endlich gehalten.
* Letzte Zeile des Blocks trägt bei positivem Slack `0` Badness (rechts-ausgefranste Schlusszeile frei).
* Wörter breiter als `maxWidth`: an `wordBreaker.breakOffsets(...)` anbieten; bei Offsets in
  synthetische `TextWord`-Stücke expandieren (wie Greedys `placeHyphenated`), sonst ganz lassen.
* Determinismus: Akkumulation strikt links nach rechts; DP-Gleichstände zugunsten weniger Zeilen,
  dann frühester Umbruch.

### Komplexität

* Naive DP: `O(n^2)` Zeilenkosten-Auswertungen mit `n` = Item-Anzahl eines Blocks; ohne Fensterung.
* Messresultate je `(font, text)` in lokaler `HashMap<String, Metrics>` im Aufruf cachen.

### Kostenkonstanten (intern)

* `FILL_PENALTY_EXP = 2.0`.
* `FILL_PENALTY_SCALE = 100.0`.
* `OVERFLOW_PENALTY = 1000.0`.
* Als `private const val` in der Strategiedatei; als reine Tuning-Werte dokumentiert, nicht API.

### Zeilenaufbau

* Vorhandenes privates `LineAccumulator`-Muster oder einen kleinen lokalen Builder wiederverwenden.
* Gewählte Item-Bereiche zu `UnplacedLine`s mit korrektem `spaceBefore`, `ascent`, `descent`
  (Maximum über Parts, Font-Metrik-Fallback bei Null) — identische Regeln wie Greedy.

## Testentwurf

* `testing`-Skill vor der Testklasse laden; Paketspiegelung; Entwicklertest ohne `IT`-Suffix.
* Deterministische `FontMeasureCalculator`-Doubles aus FP-001 (`EngineTestData` / `MeasureTestData`).
* `emptyPartsProduceNoLines` — leere Eingabe liefert `emptyList()`.
* `singleShortLineStaysOneLine` — passende Parts bleiben eine Zeile, gleich wie Greedy.
* `balancedReducesRaggednessVersusGreedy` — Summe der quadrierten positiven Slacks über Nicht-Schlusszeilen
  ist strikt kleiner als bei `GreedyWordLineBreakerStrategy`.
* `lastLineRaggednessIsFree` — kurze Schlusszeile zwingt frühere Zeilen nicht zur Stauchung.
* `overlongWordWithoutWordBreakerOverflowsSingleLine` — keine Ausnahme; Wort ganz gehalten.
* `overlongWordWithWordBreakerIsSplit` — Offsets eines Stub-`WordBreakerStrategy` werden beachtet.
* `deterministicAcrossRuns` — zwei Aufrufe mit gleicher Eingabe liefern gleiche Strukturen.
* `symbolsStayAttachedToFollowingWord` — `symbol* word` nie vor den Symbolen getrennt.
* `swapViaBuilderProducesBalancedOutput` — Builder routet zur neuen Strategie (wie `LineBreakerStrategySwapTest`).

## Aufgaben

### Aufgabe 1 — Kostenfunktion und DP-Kern

* `BalancedLineBreakerStrategy.kt` mit Objekt-Gerüst und KDoc anlegen.
* Messungs-Cache pro Aufruf und Leerzeichenbreiten-Ermittlung implementieren.
* Item-Liste aus `parts` mit Glue-/Symbol-Regeln konstruieren.
* Natürliche Zeilenbreite und Badness-Funktion mit den internen Konstanten implementieren.
* `O(n^2)`-DP mit Back-Pointern und dokumentierter Gleichstandsauflösung implementieren.
* Gewählte Bereiche über die geteilten Accumulator-Regeln zu `UnplacedLine`s rekonstruieren.

### Aufgabe 2 — Wortbrecher-Integration

* Wörter breiter als `maxWidth` erkennen; `wordBreaker.breakOffsets(...)` aufrufen.
* Akzeptierte Offsets vor der DP in synthetische `TextWord`-Stücke expandieren.
* Ohne Offsets Wort ganz behalten und die Überlauf-Strafe anwenden.

### Aufgabe 3 — Tests

* `testing`-Skill laden.
* `BalancedLineBreakerStrategyTest` gemäß Abschnitt „Testentwurf" anlegen.
* Builder-Swap-Fall analog zu `LineBreakerStrategySwapTest` ergänzen.

### Aufgabe 4 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; Strategie auf der Engine-Strategieseite und in der KDoc-Liste ergänzen.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :engine:build` ausführen und Befunde beheben.
* Im selben Change-Set: IP-01 im Status `COMPLETED`, IP-01 überall in `FP-002-AdvancedLineBreaking.md`
  abhaken, `FP-002-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Endgültige Kostenkonstanten brauchen ggf. Abgleich an echten Dokumenten; Defaults sind Startwerte.
* Ob Balanced-Breaking den `SimpLayFontEngine`-Messcache nutzen soll; vertagt, lokale Map genügt.
* Sehr große Einzelblöcke machen `O(n^2)` spürbar; Fensterung außerhalb des Umfangs, Folgeplan möglich.
* Zusammenspiel mit `JUSTIFY`-Alignment: geänderte Zeilenfüllung ändert Sperrung; per Assertion auf Sample abgedeckt.
