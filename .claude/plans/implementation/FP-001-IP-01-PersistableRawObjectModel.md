# Umsetzungsplan FP-001-IP-01: Persistable Raw Object Model

## Ziel

- Vollständiges, speicherbares Rohdatenmodell des `engine`-Moduls in `commonMain` als reine Datenhalter.

## Abhängigkeiten

- Keine (unabhängiger Plan).

## Entwurfsentscheidungen

- Dekorierte Knoten sind Interfaces (`Document`, `TextBlock`, `TextStyle`, `Font`); Ableitungen bleiben clean benannt.
- Verschachtelte serialisierbare Interfaces sind `sealed` mit genau einer `...Data`-Implementierung.
- `Page` und `TextPart` sind `@Serializable sealed interface` mit Ableitungen `FlowPage`/`SinglePage` bzw. `TextWord`/`TextSymbol`.
- Alle Maße sind `Double` und einheitenlos.
- Tokenizer speichert keinen Leerraum; `toString()` normalisiert Leerraum auf einfache Leerzeichen.
- Ableitungen tragen `@SerialName` (`"flow"`, `"single"`, `"word"`, `"symbol"`).

## Betroffene Dateien

- `engine/build.gradle.kts`
- `engine/src/commonMain/kotlin/org/pcsoft/framework/playsim/engine/geometry/Geometry.kt`
- `.../engine/geometry/Metrics.kt`
- `.../engine/model/Enums.kt`
- `.../engine/model/Font.kt`
- `.../engine/model/TextStyle.kt`
- `.../engine/model/TextPart.kt`
- `.../engine/model/Tokenizer.kt`
- `.../engine/model/TextBlock.kt`
- `.../engine/model/Page.kt`
- `.../engine/model/Document.kt`
- `.../engine/model/Counting.kt`
- `engine/src/commonTest/kotlin/org/pcsoft/framework/playsim/engine/model/` (neue Testklassen)

## Aufgabe 1: Build-Konfiguration

- `alias(libs.plugins.kotlinPluginSerialization)` in `engine/build.gradle.kts` ergänzen.
- `commonMain`-Abhängigkeit `implementation(libs.kotlinxSerialization)` hinzufügen.
- Prüfen, dass `Apache-2.0` in der `licensee`-Allow-List genügt (bereits vorhanden).
- `./gradlew :engine:build` ausführen und grün bestätigen.

## Aufgabe 2: Geometrie- und Metrik-Wertetypen

- Paket `...engine.geometry` anlegen.
- `@Serializable data class Size(width: Double, height: Double)`.
- `@Serializable data class Rect(x: Double, y: Double, width: Double, height: Double)`.
- `@Serializable data class Margins(left: Double, top: Double, right: Double, bottom: Double)`.
- `@Serializable data class FontMetrics(ascent: Double, descent: Double, leading: Double)` mit `lineHeight`-Property.
- `@Serializable data class TextMetrics(width: Double, ascent: Double, descent: Double)`.

## Aufgabe 3: Aufzählungen, Font und TextStyle

- Paket `...engine.model` anlegen.
- `enum class TextAlignment { LEFT, RIGHT, CENTER, JUSTIFY }`.
- `enum class FontWeight { NORMAL, BOLD }`, `enum class FontStyle { NORMAL, ITALIC }`.
- `@Serializable data class LineSpacing(factor: Double = 1.0, extraLeading: Double = 0.0)`.
- `interface Font` plus `@Serializable data class FontData(family, size, weight, style) : Font`.
- `interface TextStyle` plus `@Serializable data class TextStyleData(font, lineSpacing, alignment) : TextStyle`.
- Top-Level-Factories `fun Font(...)` und `fun TextStyle(...)` liefern die Interface-Typen.

## Aufgabe 4: Textteile und Tokenizer

- `@Serializable sealed interface TextPart { val text: String }`.
- `@Serializable @SerialName("word") data class TextWord(text: String) : TextPart`.
- `@Serializable @SerialName("symbol") data class TextSymbol(text: String) : TextPart`.
- `Tokenizer.kt`: Funktion `tokenize(text: String): List<TextPart>`.
- Regel: maximale Läufe aus `Char.isLetterOrDigit()` werden zu `TextWord`.
- Regel: jedes andere Nicht-Leerzeichen wird ein einzelnes `TextSymbol`.
- Regel: Leerraum trennt Teile, wird aber nicht gespeichert.

## Aufgabe 5: TextBlock mit Parser und toString

- `interface TextBlock { val parts: List<TextPart>; val style: TextStyle }` als `@Serializable sealed interface`.
- `@Serializable @SerialName("block") data class TextBlockData(parts, style) : TextBlock`.
- `companion object { fun of(text: String, style: TextStyle): TextBlock }` nutzt den Tokenizer.
- Top-Level-Factory `fun TextBlock(parts: List<TextPart>, style: TextStyle): TextBlock`.
- `TextBlockData.toString()` fügt Teile zusammen: Leerzeichen vor `TextWord`, keins vor `TextSymbol`.
- `of(text).toString()` ergibt den auf einfache Leerzeichen normalisierten Text.

## Aufgabe 6: Seiten und Dokument

- `@Serializable data class PageLayout(size: Size, margins: Margins)` mit `contentWidth`/`contentHeight`.
- `@Serializable sealed interface Page { val layout: PageLayout; val blocks: List<TextBlock> }`.
- `@Serializable @SerialName("flow") data class FlowPage(layout, blocks) : Page`.
- `@Serializable @SerialName("single") data class SinglePage(layout, blocks) : Page`.
- `interface Document { val pages: List<Page> }` plus `@Serializable data class DocumentData(pages) : Document`.
- Top-Level-Factory `fun Document(pages: List<Page> = emptyList()): Document`.
- Leeres Dokument ist zulässig.

## Aufgabe 7: Zähl-Erweiterungsfunktionen

- Datei `Counting.kt` im Paket `...engine.model`.
- `fun TextBlock.wordCount(): Int`, `fun TextBlock.symbolCount(): Int`, `fun TextBlock.charCount(): Int`.
- `fun Page.wordCount()` / `symbolCount()` / `charCount()` aggregiert über Blöcke.
- `fun Document.wordCount()` / `symbolCount()` / `charCount()` aggregiert über Seiten.
- `charCount()` summiert `part.text.length` aller Teile.

## Testkonzept

- `testing`-Skill vor dem Anlegen der Testklassen laden.
- Paketspiegelung: Tests unter `commonTest/.../engine/model/` und `.../engine/geometry/`.
- `TextBlockTest`: `of(text).toString()`-Round-Trip inkl. Satzzeichen und Mehrfach-Leerraum.
- `TokenizerTest`: Wort-/Symbol-Grenzen, Ziffern, Bindestrich, Anführungszeichen, Zeilenumbruch.
- `CountingTest`: Wort-, Symbol-, Zeichenzahl auf Block, Seite, Dokument.
- `SerializationTest`: JSON-`encode`/`decode`-Round-Trip für `DocumentData` mit `FlowPage` und `SinglePage`.
- `ModelTest`: leeres Dokument, `PageLayout.contentWidth`/`contentHeight`.

## Definition of Done

- `./gradlew :engine:build` ist grün, `licensee` meldet keine fehlende Lizenz.
- Rohmodell bildet ein Dokument mit `FlowPage` und `SinglePage` ab; leeres Dokument möglich.
- JSON-Round-Trip eines Rohdokuments ist verlustfrei.
- `...engine.model` enthält keinen Engine- oder Formatcode.
- `TextBlock.of(text).toString()` reproduziert den normalisierten Text.
- Zähl-Erweiterungen liefern korrekte Werte.
- Alle neuen Tests laufen grün.
