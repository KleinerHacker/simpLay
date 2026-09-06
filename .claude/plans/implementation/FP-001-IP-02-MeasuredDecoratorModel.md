# Umsetzungsplan FP-001-IP-02: Measured Decorator Model

## Ziel

- Nicht speicherbares Measured-Datenmodell in `commonMain`, das das Rohmodell per `by` dekoriert.

## Abhängigkeiten

- IP-01 (Rohmodell-Interfaces, Geometrie- und Metrik-Wertetypen).

## Entwurfsentscheidungen

- Jeder Measured-Typ delegiert per Kotlin `by` an seine Roh-Interface-Instanz.
- Measured-Typen tragen kein `@Serializable` und keine Persistenz.
- `MeasuredLine` ist eine Zwischenebene ohne Roh-Gegenstück.
- Geometrie wird als `Rect`/`Size`/`Double` in Layout-Einheiten ergänzt.
- Konstruktoren sind `public`, damit Tests das Modell direkt aufbauen können.
- `MeasuredSinglePage.effectiveSize` wächst in der Höhe, Breite bleibt fix.

## Betroffene Dateien

- `engine/src/commonMain/kotlin/org/pcsoft/framework/playsim/engine/measure/MeasuredFont.kt`
- `.../engine/measure/MeasuredTextStyle.kt`
- `.../engine/measure/MeasuredTextPart.kt`
- `.../engine/measure/MeasuredLine.kt`
- `.../engine/measure/MeasuredTextBlock.kt`
- `.../engine/measure/MeasuredPage.kt`
- `.../engine/measure/MeasuredDocument.kt`
- `engine/src/commonTest/kotlin/org/pcsoft/framework/playsim/engine/measure/` (neue Testklassen)

## Aufgabe 1: Font- und Style-Dekoratoren

- Paket `...engine.measure` anlegen.
- `class MeasuredFont(raw: Font, metrics: FontMetrics) : Font by raw`.
- `class MeasuredTextStyle(raw: TextStyle, measuredFont: MeasuredFont, resolvedLineHeight: Double) : TextStyle by raw`.
- `resolvedLineHeight` = `(ascent + descent) * lineSpacing.factor + lineSpacing.extraLeading`.
- `MeasuredTextStyle.font` gibt die `MeasuredFont` zurück.

## Aufgabe 2: TextPart- und Line-Dekoratoren

- `class MeasuredTextPart(raw: TextPart, bounds: Rect) : TextPart by raw`.
- `class MeasuredLine(parts: List<MeasuredTextPart>, lineBox: Rect, baseline: Double, ascent: Double, descent: Double, alignment: TextAlignment, lastLine: Boolean)`.
- `MeasuredLine` delegiert nicht; es ist eigenständig.
- `lineBox` enthält Position und Maße der Zeile relativ zum Seiteninhalt.

## Aufgabe 3: Block- und Seiten-Dekoratoren

- `class MeasuredTextBlock(raw: TextBlock, lines: List<MeasuredLine>, bounds: Rect, style: MeasuredTextStyle) : TextBlock by raw`.
- `sealed interface MeasuredPage : Page { pageIndex: Int; contentArea: Rect; measuredBlocks: List<MeasuredTextBlock>; requiredContentHeight: Double; effectiveSize: Size }`.
- `class MeasuredFlowPage(raw: FlowPage, ...) : Page by raw, MeasuredPage`.
- `class MeasuredSinglePage(raw: SinglePage, ...) : Page by raw, MeasuredPage`.
- `MeasuredFlowPage.effectiveSize` = `layout.size`.
- `MeasuredSinglePage.effectiveSize` = `Size(layout.size.width, max(layout.size.height, requiredContentHeight + Ränder))`.

## Aufgabe 4: Dokument-Dekorator

- `class MeasuredDocument(raw: Document, pages: List<MeasuredPage>) : Document by raw`.
- `MeasuredDocument.pages` überschreibt die Delegation und liefert die `MeasuredPage`-Liste.
- Leeres `MeasuredDocument` mit leerer Seitenliste ist zulässig.

## Testkonzept

- `testing`-Skill vor dem Anlegen der Testklassen laden.
- Paketspiegelung: Tests unter `commonTest/.../engine/measure/`.
- `DelegationTest`: `by`-Durchreichung liefert Rohwerte (z. B. `MeasuredFont.family`).
- `OverrideTest`: `MeasuredDocument.pages` und `MeasuredTextStyle.font` liefern Measured-Typen.
- `SinglePageGrowthTest`: `effectiveSize` wächst nur in der Höhe.
- `BuildByHandTest`: vollständiges Measured-Modell ohne Engine zusammengebaut.

## Definition of Done

- `./gradlew :engine:build` ist grün.
- Alle Measured-Typen delegieren per `by`; `MeasuredLine` existiert als Zwischenebene.
- Kein `@Serializable` und keine Persistenz im Paket `...engine.measure`.
- Ein Test baut ein Measured-Modell direkt auf.
- Alle neuen Tests laufen grün.
