# Umsetzungsplan FP-001-IP-03: Layout Engine

## Ziel

- `SimPLayEngine`, die ein Rohdokument per Font-Measuring-Callback in ein `MeasuredDocument` umrechnet.

## Abhängigkeiten

- IP-01 (Rohmodell, Wertetypen).
- IP-02 (Measured-Modell, `MeasuredLine`).

## Entwurfsentscheidungen

- `fun interface FontMeasurer { fun measure(font: Font, text: String): TextMetrics }` ist der Callback.
- Font-Zeilenmaße werden über `measure(font, "Xÿ")` als Referenzglyphen bestimmt.
- Erstellung über `SimPLayEngine.builder(measurer).wordBreaker(...).build()`; Measurer ist Pflicht.
- `fun interface WordBreaker` mit No-Op-Default `NoWordBreaker` (keine Trennstellen).
- Umbruch ist greedy Teil für Teil; ein Leerzeichen vor `TextWord`, keins vor `TextSymbol`.
- Ein Wort breiter als der Inhalt bleibt allein in seiner Zeile (Überlauf).
- `JUSTIFY` verteilt Restbreite auf Wortlücken, außer in der letzten Blockzeile.
- `FlowPage` erzeugt 1..n `MeasuredFlowPage`; neue Seiten klonen das `PageLayout` tief.
- `SinglePage` bricht nie um; `requiredContentHeight` steuert das Höhenwachstum.

## Betroffene Dateien

- `engine/src/commonMain/kotlin/org/pcsoft/framework/playsim/engine/engine/FontMeasurer.kt`
- `.../engine/engine/WordBreaker.kt`
- `.../engine/engine/SimPLayEngine.kt`
- `.../engine/engine/internal/FontResolver.kt`
- `.../engine/engine/internal/LineBreaker.kt`
- `.../engine/engine/internal/BlockLayouter.kt`
- `.../engine/engine/internal/Paginator.kt`
- `engine/src/commonTest/kotlin/org/pcsoft/framework/playsim/engine/engine/` (neue Testklassen)

## Aufgabe 1: Callback- und Builder-API

- `FontMeasurer` als `fun interface` mit `measure(font, text): TextMetrics`.
- `WordBreaker` als `fun interface` mit `breakOffsets(word, font, maxWidth, measurer): List<Int>`.
- Objekt `NoWordBreaker : WordBreaker` gibt `emptyList()` zurück.
- `class SimPLayEngine` mit privatem Konstruktor.
- `class SimPLayEngine.Builder` mit `wordBreaker(...)` und `build()`.
- `companion object { fun builder(measurer: FontMeasurer): Builder }`.
- `build()` prüft, dass ein Measurer gesetzt ist.

## Aufgabe 2: Font-Auflösung

- `FontResolver` bestimmt `FontMetrics` je `Font` über `measure(font, "Xÿ")`.
- `ascent`/`descent` aus den Referenzglyphen; `leading` = 0.
- Ergebnis wird pro `measure`-Aufruf zwischengespeichert (Map).
- Baut `MeasuredFont` und `MeasuredTextStyle` samt `resolvedLineHeight`.

## Aufgabe 3: Zeilenumbruch

- `LineBreaker` füllt Zeilen greedy aus `List<TextPart>` bis zur Inhaltsbreite.
- Leerzeichenbreite über `measure(font, " ").width`.
- `TextWord`: passt Breite plus ggf. Leerzeichen davor, sonst neue Zeile.
- `TextSymbol`: ohne führendes Leerzeichen an vorherigen Teil angehängt.
- Überlanges Einzelwort: `WordBreaker` befragen, sonst allein in die Zeile.
- Pro Zeile `ascent`/`descent` aus dem breitesten Teilbeitrag ableiten.
- Ausgabe: Liste roher Zeilen mit Teilbreiten, noch ohne Position.

## Aufgabe 4: Blocklayout und Ausrichtung

- `BlockLayouter` stapelt Zeilen vertikal ab dem aktuellen `y`-Cursor.
- Zeilenhöhe = `resolvedLineHeight`.
- x-Offset je Zeile: `LEFT` 0, `RIGHT` Rest, `CENTER` halber Rest.
- `JUSTIFY`: Restbreite gleichmäßig auf Wortlücken, letzte Blockzeile linksbündig.
- Setzt `MeasuredTextPart.bounds`, `MeasuredLine.lineBox`, `baseline`, `MeasuredTextBlock.bounds`.

## Aufgabe 5: Seitenaufteilung

- `Paginator` verarbeitet Seiten in Dokumentreihenfolge.
- Inhaltsbox aus `PageLayout` (Größe minus Ränder).
- `FlowPage`: bei `y + Zeilenhöhe > Inhaltshöhe` Seite schließen, nächste `MeasuredFlowPage` öffnen.
- Neue Flow-Seite klont `PageLayout` tief; `pageIndex` hochzählen.
- `SinglePage`: alle Zeilen auf eine `MeasuredSinglePage`; `requiredContentHeight` = Cursor-`y`.
- Seite ohne Blöcke ergibt eine leere Measured-Seite.
- Leeres Dokument ergibt `MeasuredDocument` mit leerer Seitenliste.

## Aufgabe 6: Einstiegspunkt

- `fun SimPLayEngine.measure(document: Document): MeasuredDocument`.
- Ruft Font-Auflösung, `LineBreaker`, `BlockLayouter`, `Paginator` in Reihe.
- Ergebnis ist deterministisch und ruft keine Plattform-API auf.

## Testkonzept

- `testing`-Skill vor dem Anlegen der Testklassen laden.
- Deterministischer Test-`FontMeasurer`: Breite = `text.length * size * 0.6`, ascent = `size * 0.8`, descent = `size * 0.2`.
- `LineBreakerTest`: Umbruch bei genau passender und überschreitender Breite.
- `SymbolAttachTest`: Satzzeichen ohne führendes Leerzeichen, Umbruch mit Symbol.
- `AlignmentTest`: `LEFT`/`RIGHT`/`CENTER`/`JUSTIFY`, letzte Zeile nicht ausgetrieben.
- `FlowPaginationTest`: langer Text erzeugt mehrere `MeasuredFlowPage` mit geklontem Layout.
- `SinglePageTest`: Höhe wächst über die Layouthöhe hinaus.
- `EmptyDocumentTest`: leeres Dokument liefert leeres `MeasuredDocument`.
- `WordBreakerHookTest`: No-Op-Default lässt Überlaufwort ungebrochen.

## Definition of Done

- `./gradlew :engine:build` ist grün.
- `SimPLayEngine` wird über Builder mit Font-Measuring-Callback erzeugt.
- Ein `measure`-Aufruf wandelt ein Rohdokument in ein `MeasuredDocument`.
- Zu breiter Blocktext wird in mehrere `MeasuredLine` umgebrochen.
- `FlowPage`-Überlauf erzeugt automatisch weitere Seiten.
- `SinglePage`-Überlauf erzeugt eine höhere Measured-Seite.
- `WordBreaker`-Naht existiert mit No-Op-Default; keine Silbentrennung.
- Alle neuen Tests laufen grün.
