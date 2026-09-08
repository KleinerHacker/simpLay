# FP-002 / IP-02: Break-Opportunity Line Breaking

Feature Plan: `.claude/plans/features/FP-002-AdvancedLineBreaking.md`
Status: `.claude/plans/features/FP-002-AdvancedLineBreaking-Status.md`

## Ziel

* `BreakOpportunityLineBreakerStrategy` als Greedy-Strategie, die nur an erlaubten UAX-#14-Positionen umbricht.
* Opt-in über `SimpLayEngine.Builder`; Default unverändert.

## Umfang

### Enthalten

* Neues `BreakOpportunityLineBreakerStrategy`-Objekt in `org.pcsoft.framework.simplay.engine.engine`.
* Interne Abbildung von Zeichen auf ein kleines `BreakClass`-Enum.
* Interne Herleitung von Umbruchgelegenheiten zwischen und innerhalb von `TextPart`s aus diesen Klassen.
* Greedy-Zeilenfüllung, auf erlaubte Umbruchpunkte beschränkt; Überlauf, wenn keine Gelegenheit besteht.
* Tests mit Interpunktion, nicht umbrechbaren Läufen und CJK-artiger Eingabe.

### Nicht enthalten

* Vollständige Unicode-Line-Break-Tabellen und Paartabelle; nur eine kuratierte Teilmenge.
* Locale-Anpassung, wörterbuchbasiertes Umbrechen (Thai, Khmer, ...).
* Silbentrennung; `WordBreakerStrategy` ist in der Signatur, wird aber nicht konsultiert.
* Rohmodell-Änderungen; balanciertes/optimales Umbrechen (IP-01).

## Abhängigkeiten

* Unabhängig innerhalb FP-002, parallel zu IP-01 und IP-03.
* Externe Voraussetzung: FP-001/IP-03-Naht (`LineBreakerStrategy`, `UnplacedLine`, `UnplacedPart`,
  `FontMeasureCalculator`) und der deterministische Test-`FontMeasureCalculator`.

## Schnittstellen zu anderen Plänen

* Verbraucht die FP-001-Nahttypen unverändert.
* Liefert keinen neuen geteilten Typ; `BreakClass` ist `internal`.
* Braucht ein späterer Plan `BreakClass` öffentlich, ist das eine separate Änderung.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/BreakOpportunityLineBreakerStrategy.kt` | Neu: Strategieobjekt, `BreakClass`-Enum, Klassifizierer, Gelegenheitsherleitung, Greedy-Füllung, KDoc. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/engine/LineBreakerStrategy.kt` | Nur KDoc: zur Strategieliste ergänzen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/engine/BreakOpportunityLineBreakerStrategyTest.kt` | Neu: Verhaltenstests. |
| `docs/` (Engine-Strategieseite) | Strategie ergänzen; zuvor `project-docs`-Skill laden. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |

## Entwurf

### Break-Klassen (interne Teilmenge)

* Internes `enum class BreakClass` mit der Minimalmenge für Latein plus einfaches CJK-Verhalten.

| Klasse | Bedeutung | Beispielzeichen |
| ------ | --------- | --------------- |
| `MANDATORY` | erzwungener Umbruch | nur über explizite Tokens — hier nicht erzeugt; reserviert |
| `SPACE` | umbrechbare Glue | U+0020, Tab bereits vom Tokenizer entfernt |
| `BEFORE` | Umbruch davor erlaubt, danach nicht | öffnende Klammern `([{`, `¡`, `¿` |
| `AFTER` | Umbruch danach erlaubt, davor nicht | `)]}`, `!`, `?`, `,`, `;`, `:`, `.`, `%`, `/`, `-`, `—`, `…` |
| `BOTH` | Umbruch davor und danach erlaubt | ideografischer Bereich, `–` als Trenner |
| `NONBREAK` | nie benachbart umbrechen | Default für Buchstaben/Ziffern, `&`, nicht umbrechende Interpunktion, Ziffern um `.` und `,` |

* Klassifizierung als `when` über Codepunkt-Bereiche/-Mengen, ohne externe Tabelle.
* Als Näherung an UAX #14 dokumentiert, keine konforme Implementierung.
* CJK-Erkennung: Codepunkte in CJK Unified Ideographs, Hiragana, Katakana → `BOTH`.

### Von Parts zu Gelegenheiten

* Tokenizer hat Whitespace entfernt und Symbole in Einzelzeichen-`TextSymbol`s, Läufe in `TextWord`s zerlegt.
* Gelegenheit vor jedem `TextWord` nach dem ersten — der implizite Inter-Token-Space (Klasse `SPACE`) —
  außer das vorangehende `TextSymbol` ist `BEFORE` (Umbruch vor das Symbol) oder Kontext ist `NONBREAK`.
* Numerische Absicherung: Ziffern-`TextWord` + `,`/`.` + Ziffern-`TextWord` ⇒ keine Gelegenheit um das Symbol.
* Gelegenheit um ein `TextSymbol` gemäß seiner Klasse (`BEFORE` / `AFTER` / `BOTH` / `NONBREAK`).
* Innerhalb eines `TextWord` nur bei CJK-Codepunkten: Gelegenheit nach jedem CJK-Codepunkt (`BOTH`),
  keine zwischen zwei lateinischen Buchstaben.
* Jede Gelegenheit ist ein Schnittindex über einen abgeflachten `(part, charOffset)`-Strom;
  ein Schnitt kann an einer Part-Grenze oder in einem CJK-`TextWord` liegen.

### Greedy-Füllung

* Parts links nach rechts durchlaufen und Breite akkumulieren wie `GreedyWordLineBreakerStrategy`
  (`spaceWidth` einmal gemessen; `spaceBefore` = Leerzeichenbreite vor nicht erstem `TextWord`, `0` für `TextSymbol`).
* Würde die nächste Einheit `maxWidth` überschreiten, zur letzten Gelegenheit an/vor dem Überlaufpunkt
  zurückgehen und dort umbrechen.
* Gibt es auf der aktuellen Zeile keine Gelegenheit, die Einheit trotzdem setzen und umbrechen
  (Zeile läuft über) — nie in einem `NONBREAK`-Lauf schneiden.
* Ein CJK-`TextWord` länger als `maxWidth` an CJK-Gelegenheiten in synthetische `TextWord`-Stücke
  aufteilen (wie Greedys Silbentrennungspfad, aber von der Break-Klasse getrieben).

### Determinismus

* Reine Funktion der Eingabe; Klassifizierung und Greedy-Rückzug sind deterministisch; keine Plattform-API.

## Testentwurf

* `testing`-Skill zuerst laden; Entwicklertest, paketgespiegelt, deterministischer `FontMeasureCalculator`-Double.
* `emptyPartsProduceNoLines`.
* `breaksAtSpaceLikeGreedyForPlainText` — für lateinische Wörter plus Leerzeichen gleich `GreedyWordLineBreakerStrategy`.
* `neverBreaksInsideNonBreakingRun` — langer `NONBREAK`-Lauf bleibt ganz und läuft über, kein Schnitt darin.
* `breaksAfterTrailingPunctuation` — `word,` erlaubt Umbruch nach `,`, nicht davor.
* `breaksBeforeOpeningBracket` — `word(` erlaubt Umbruch vor `(`.
* `numericGuardKeepsThousandsSeparatorTogether` — `1 , 000`-Token-Strom ergibt keinen Umbruch am Trenner.
* `cjkTextBreaksBetweenIdeographs` — CJK-only-`TextWord` breiter als `maxWidth` wird an Ideograph-Grenzen umbrochen.
* `cjkAndLatinMixedRespectsBothRules` — kein Umbruch zwischen lateinischen Buchstaben, Umbruch zwischen Ideographen.
* `deterministicAcrossRuns`.
* `swapViaBuilderRoutesToStrategy` — analog zu `LineBreakerStrategySwapTest`.

## Aufgaben

### Aufgabe 1 — Break-Klassen-Modell

* `BreakOpportunityLineBreakerStrategy.kt`-Gerüst mit KDoc anlegen.
* `internal enum class BreakClass` und den Codepunkt-Klassifizierer (`when` über Bereiche/Mengen) ergänzen.
* Die Teilmenge als nicht konforme UAX-#14-Näherung dokumentieren.

### Aufgabe 2 — Gelegenheitsherleitung

* `parts` in einen `(part, charOffset)`-Strom abflachen.
* Gelegenheiten aus Inter-Token-Spaces und den Symbolklassen erzeugen.
* Numerische Absicherung für Ziffer + Trenner + Ziffer anwenden.
* Wortinterne Gelegenheiten nur für CJK-Codepunkte erzeugen.

### Aufgabe 3 — Greedy-Füllung, auf Gelegenheiten beschränkt

* Breite mit Greedys Leerzeichenregeln akkumulieren.
* Zur letzten Gelegenheit an/links vom Überlauf zurückgehen und dort umbrechen.
* Überlauf, wenn keine Gelegenheit auf der Zeile; nie einen `NONBREAK`-Lauf schneiden.
* Überlange CJK-`TextWord`s an Ideograph-Gelegenheiten in synthetische `TextWord`-Stücke teilen.
* `UnplacedLine`s mit den geteilten `spaceBefore`-/Ascent-/Descent-Regeln bauen.

### Aufgabe 4 — Tests

* `testing`-Skill laden.
* `BreakOpportunityLineBreakerStrategyTest` gemäß Abschnitt „Testentwurf" anlegen.

### Aufgabe 5 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; Engine-Strategieseite und KDoc-Liste aktualisieren.
* `CHANGELOG.md`-Eintrag ergänzen.
* `./gradlew :engine:build` ausführen; Befunde beheben.
* Im selben Change-Set: Status IP-02 `COMPLETED`, IP-02 in `FP-002-AdvancedLineBreaking.md` abhaken,
  `FP-002-Overview.md` aktualisieren, diese Plandatei mit `git rm` entfernen.

## Risiken und offene Punkte

* Die kuratierte Break-Klassen-Menge trifft ICU in Randfällen nicht; als Näherung dokumentiert.
* Ob `BreakClass` öffentlich sein soll — laut Feature-Plan-Frage `internal` gehalten.
* Zusammenspiel der numerischen Absicherung mit dem Tokenizer (der `,`/`.` bereits in Symbole teilt);
  per expliziten Test abgedeckt.
* CJK-Bereichsliste an feste Unicode-Blöcke binden, um über Kotlin-/JDK-Versionen deterministisch zu bleiben;
  Bereiche hart kodiert, nicht aus plattformabhängigen `Character`-APIs abgeleitet.
