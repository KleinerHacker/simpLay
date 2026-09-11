# IP: Erhalt von Whitespace in TextBlock

Kein Feature-Plan; eigenständiger Umsetzungsplan (Bugfix mit Modelländerung).

## Ziel

* Whitespace (Leerzeichen, Tab) wird beim Tokenisieren nicht mehr verworfen, sondern als eigener
  `TextPart`-Typ `TextWhitespace` (mit `WhitespaceKind.SPACE` / `WhitespaceKind.TAB`) gespeichert.
* `TextBlock.toString()` wird dadurch verlustfrei: kein Erfinden eines Leerzeichens mehr an
  Symbol→Wort-Übergängen ohne Original-Whitespace.
* Der gemeldete Caret-Drift-Bug in `DocumentEditor.splice()` (Tippen `X`,`Y`,`Z` direkt hinter einem
  `TextSymbol` ergibt `YZX` statt `XYZ`) verschwindet dadurch an der Wurzel, ohne `splice()` selbst
  anzufassen.

## Ursache (bestätigt)

* `TextBlock.kt`: `tokenize()` verwirft jeden Whitespace-Charakter ersatzlos; `toString()` fügt
  danach pauschal ein Leerzeichen vor jedem `TextWord` außer dem ersten ein - unabhängig davon, ob im
  Original wirklich eines stand.
* `"paragraph.X"` und `"paragraph. X"` erzeugen identische Parts (`TextWord`, `TextSymbol('.')`,
  `TextWord`); beim Wiederaufbau wird daraus immer `"paragraph. X"`.
* `DocumentEditor.splice()` berechnet `caretIndex` gegen den rohen Splice-Text, nicht gegen den nach
  `TextBlock.of`-Retokenisierung tatsächlich rekonstruierten Text - jedes erfundene Leerzeichen lässt
  den Caret um ein Zeichen zurückfallen; jedes weitere getippte Zeichen landet dadurch vor statt nach
  dem vorherigen.

## Entwurf

* `TextWhitespace` ist ein vollwertiger, serialisierbarer `TextPart` (wie `TextWord`/`TextSymbol`),
  fasst einen maximalen Lauf gleichartigen Whitespaces (nur Leerzeichen oder nur Tabs) zusammen.
* Layout/Messung (`LineBreakerStrategy`): `TextWhitespace`-Parts erzeugen kein eigenes
  `UnplacedPart`/keinen Glyph, sondern setzen ein "war Whitespace davor"-Flag für den nächsten
  Wort-/Symbol-Part. Die bisherige Regel `space = if (part is TextSymbol) 0.0 else spaceWidth` wird
  durch dieses Flag ersetzt (Space nur bei echtem Original-Whitespace, exakt eine `spaceWidth`
  unabhängig von der Lauflänge oder `TAB` vs. `SPACE` - Breitenberechnung bleibt bewusst einfach).
* `LineBreakerStrategy.breakIntoLines`: Doc-Kommentar wird angepasst („`parts` kann jetzt
  `TextWhitespace` enthalten, das keinen Glyph erzeugt"); alle drei mitgelieferten Strategien
  (`GreedyWordLineBreakerStrategy`, `CharacterLineBreakerStrategy`, `NoWrapLineBreakerStrategy`)
  werden entsprechend angepasst. Das ist eine dokumentierte Verhaltensänderung der Schnittstelle für
  künftige externe Implementierungen (siehe Risiken).
* `DocumentTextIndex`: die Separator-Regel zwischen zwei Segmenten nutzt statt der pauschalen
  "ist nächstes Teil ein Wort?"-Annahme den tatsächlich im rohen Block vorhandenen
  `TextWhitespace`-Part (falls vorhanden) bzw. `""`, wenn keiner vorhanden war. `TextWhitespace`
  selbst bleibt nicht einzeln selektierbar/caret-adressierbar (wie bisher kein Whitespace
  adressierbar war).
* `DocumentEditor.splice()` bleibt unverändert - der Bug ist durch die verlustfreie
  Tokenisierung/Reassemblierung behoben, da `caretIndex` und der neu gemessene Index jetzt wieder
  übereinstimmen.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/TextPart.kt` | Neu: `WhitespaceKind`-Enum (`SPACE`, `TAB`), Datenklasse `TextWhitespace(kind, text)`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/model/TextBlock.kt` | `tokenize()` erzeugt `TextWhitespace`-Läufe statt Whitespace zu verwerfen; `toString()` vereinfacht auf reines Concat aller `part.text`. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/LineBreakerStrategy.kt` | Doc-Kommentar der `parts`-Parameter aktualisieren; `TextWhitespace`-Behandlung (kein Glyph, setzt Pending-Space-Flag) in allen drei Strategien; `is TextSymbol`-Regel durch das Flag ersetzen. |
| `engine/src/commonMain/kotlin/org/pcsoft/framework/simplay/engine/internal/SimpLayBlockEngine.kt` | Prüfen/verifizieren, dass Gap-Zählung für `JUSTIFY` mit der neuen `spaceBefore`-Herkunft weiterhin korrekt ist (voraussichtlich keine Code-Änderung nötig). |
| `ui/common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/DocumentTextIndex.kt` | Separator-Bestimmung zwischen zwei Segmenten auf echten `TextWhitespace`-Part statt pauschaler Wort-Regel umstellen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/model/TokenizerTest.kt` | Neue Fälle: Whitespace-Erhalt, mehrere Leerzeichen, Tab, Symbol-Wort ohne Leerzeichen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/model/TextBlockTest.kt` | `toString()`-Round-Trip verlustfrei, insbesondere Symbol→Wort ohne Leerzeichen. |
| `engine/src/commonTest/kotlin/org/pcsoft/framework/simplay/engine/model/SerializationTest.kt` | JSON- und XML-Round-Trip für `TextWhitespace`/`WhitespaceKind`. |
| Test(s) zu `LineBreakerStrategy` (Datei beim Umsetzen ermitteln, testing-Skill vorher laden) | Space nur bei echtem Whitespace, kein Space bei Symbol-Wort-Übergang ohne Leerzeichen, je Strategie. |
| `ui/common/src/test/kotlin/org/pcsoft/framework/simplay/uicommon/DocumentTextIndexTest.kt` | Segment-/Text-Erhalt bei Symbol-Wort ohne Leerzeichen. |
| `ui/common/src/test/kotlin/org/pcsoft/framework/simplay/uicommon/DocumentEditorTest.kt` | Regressionstest exakt für den gemeldeten Fall: Caret ans Blockende, `X`,`Y`,`Z` tippen, Ergebnis `"...paragraph.XYZ"`, Caret nach jedem Zeichen an der jeweils korrekten Position. |
| `CHANGELOG.md` | Neuer Eintrag unter „Unreleased". |
| `docs/` | Bei Bedarf Anpassung, falls Whitespace-Verhalten dokumentiert ist (`project-docs`-Skill vorher laden). |

## Testentwurf

* `testing`-Skill vor jeder Testklassen-Änderung laden; Paketspiegelung; Entwicklertests ohne
  `IT`-Suffix.
* `tokenizePreservesSingleSpaceBetweenWords`, `tokenizePreservesMultipleSpacesAsOneRun`,
  `tokenizeDistinguishesTabFromSpace`, `tokenizeSymbolDirectlyFollowedByWordHasNoWhitespacePart`.
* `toStringRoundTripsExactOriginalTextIncludingSymbolWordBoundary` (das konkrete `"...paragraph.X"`-
  Beispiel aus der Fehlermeldung).
* `serializationRoundTripsTextWhitespaceViaJson` / `...ViaXml`.
* Je Strategie: `noSpaceBeforeWordDirectlyAfterSymbol`, `spaceBeforeWordAfterExplicitWhitespace`.
* `documentTextIndexSegmentTextMatchesOriginalAtSymbolWordBoundary`.
* `typingThreeCharsAfterSymbolAtBlockEndKeepsOrderAndCaretPosition` (Regressionstest 1:1 zum
  gemeldeten Bug).

## Aufgaben

### Aufgabe 1 — Modell (engine)

* `WhitespaceKind`-Enum und `TextWhitespace`-Datenklasse in `TextPart.kt` anlegen.
* `tokenize()` in `TextBlock.kt` um Whitespace-Lauf-Erkennung erweitern (Leerzeichen vs. Tab
  getrennt, gemischte Läufe an der Kind-Grenze aufteilen).
* `toString()` in `TextBlock.kt` auf reines Concat aller `part.text` vereinfachen.
* `TokenizerTest.kt` und `TextBlockTest.kt` gemäß Testentwurf ergänzen.

### Aufgabe 2 — Layout/Messung (engine)

* Doc-Kommentar von `LineBreakerStrategy.breakIntoLines` aktualisieren.
* `TextWhitespace`-Behandlung (Pending-Space-Flag statt `is TextSymbol`-Regel) in
  `GreedyWordLineBreakerStrategy`, `CharacterLineBreakerStrategy`, `NoWrapLineBreakerStrategy`
  umsetzen.
* `SimpLayBlockEngine.kt` auf Korrektheit der `JUSTIFY`-Gap-Zählung prüfen, bei Bedarf anpassen.
* Zugehörige Strategie-Tests gemäß Testentwurf ergänzen.

### Aufgabe 3 — ui/common

* `DocumentTextIndex.kt`: Separator-Logik auf echten `TextWhitespace`-Part umstellen.
* `DocumentTextIndexTest.kt` und `DocumentEditorTest.kt` gemäß Testentwurf ergänzen, insbesondere den
  1:1-Regressionstest zum gemeldeten Bug.

### Aufgabe 4 — Serialisierung

* `SerializationTest.kt` um JSON-/XML-Round-Trip für `TextWhitespace`/`WhitespaceKind` ergänzen.

### Aufgabe 5 — Doku, Changelog, Build, Abschluss

* `project-docs`-Skill laden; Doku bei Bedarf ergänzen.
* `CHANGELOG.md` unter „Unreleased" ergänzen.
* `./gradlew build` über einen Agenten ausführen und Befunde beheben.
* Im selben Change-Set diese Plandatei `IP-WhitespacePreservation.md` mit `git rm` entfernen.

## Risiken und offene Punkte

* `LineBreakerStrategy.breakIntoLines` ändert seinen Vertrag (`parts` darf jetzt `TextWhitespace`
  enthalten); jede künftige externe Implementierung muss das berücksichtigen - dokumentiert im
  Interface-Kommentar.
* Tab-Breite wird weiterhin wie ein einzelnes Leerzeichen behandelt (kein echtes Tab-Stop-Raster);
  `WhitespaceKind.TAB` dient nur dem Text-/Round-Trip-Erhalt, nicht der Breitenberechnung.
* Mehrfache aufeinanderfolgende Leerzeichen bleiben visuell eine `spaceWidth` breit (Textinhalt aber
  exakt erhalten) - bewusste Scope-Reduktion gegenüber einer vollen Multi-Space-Breitenberechnung.
* `StyledTextClipboard.kt` (Copy/Paste) sollte während der Umsetzung auf `TextPart`-Verzweigungen
  geprüft werden, auch wenn die Recherche keine fand.
* `Counting.kt`/`MeasuredCounting.kt` (`wordCount`/`symbolCount`) bleiben unverändert - kein
  `whitespaceCount()` im Umfang dieses Plans.
