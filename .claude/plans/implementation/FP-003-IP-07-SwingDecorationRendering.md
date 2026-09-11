# FP-003 / IP-07: Swing-Anpassung: Darstellung der neuen Dekorationen

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* `ui:swing` stellt die in IP-01 eingeführten Dekorationen (Unterstrichen, Durchgestrichen, Dim)
  beim Zeichnen über `Graphics2D` dar.

## Umfang

### Enthalten

* Anpassung von `Graphics2DDocumentRenderer` (Package
  `org.pcsoft.framework.simplay.swing.internal`), sodass Unterstrichen/Durchgestrichen über
  `java.awt.font.TextAttribute` (`UNDERLINE`, `STRIKETHROUGH`) und Dim über reduzierte
  Deckkraft/Füllfarbe umgesetzt werden.
* Tests für die neue Darstellung.

### Nicht enthalten

* Neue Engine-Modelländerungen (bereits in IP-01 abgeschlossen).
* Farbe.
* Änderungen an `ui:console` oder `ui:fx`.

## Abhängigkeiten

* IP-01. Wird gemäß Nutzervorgabe erst nach Abschluss von IP-01 bis IP-05 eingeplant.

## Schnittstellen zu anderen Plänen

* Konsumiert `Font.decorations` aus IP-01. Keine Abhängigkeit zu IP-02 bis IP-05.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/SwingFontMeasureCalculator.kt` | `toAwtFont` bzw. neue Hilfsfunktion für ein `AttributedCharacterIterator`/`TextAttribute`-Set aus `Font.decorations`. |
| `ui/swing/src/main/kotlin/org/pcsoft/framework/simplay/swing/internal/Graphics2DDocumentRenderer.kt` | `drawString` durch attribuierten Text (Underline/Strikethrough) ersetzen bzw. ergänzen; Dim über `Composite`/Farbe. |
| `ui/swing/src/test/kotlin/org/pcsoft/framework/simplay/swing/internal/Graphics2DDocumentRendererTest.kt` bzw. vorhandene Renderer-Testklasse | Tests für Underline/Strikethrough/Dim ergänzen. |
| `ui/swing/src/demo/kotlin/org/pcsoft/framework/simplay/swing/demo/DemoDocuments.kt` | Beispiel mit Dekorationen ergänzen. |

## Entwurf

* `java.awt.font.TextAttribute.UNDERLINE` (`UNDERLINE_ON`) und `STRIKETHROUGH`
  (`STRIKETHROUGH_ON`) werden nativ von AWT unterstützt; Umsetzung über ein
  `AttributedString`/`Map<TextAttribute, Any>` statt des bisherigen reinen `AwtFont` bei
  `g.drawString`, sofern `Font.decorations` nicht leer ist — bei leerer Menge bleibt der
  bisherige, einfachere Pfad unverändert (keine Regression für den Normalfall).
* Dim wird über eine reduzierte Alpha-Komponente der aktuellen Zeichenfarbe umgesetzt
  (`g.color = SEPARATOR_COLOR`-artiges Muster: temporäre Farbe mit reduziertem Alpha setzen,
  danach ursprüngliche Farbe wiederherstellen), analog zum bestehenden Muster in
  `drawSeparator` (`DocumentImageRenderer.kt`) für Farb-/Stroke-Sicherung und -Wiederherstellung.
* Reihenfolge/Zurücksetzen von `Color`/`Font`/`Composite` an `g` strikt nach dem bestehenden
  Save-Restore-Muster der Klasse.

## Testentwurf

* `testing`-Skill vor Ändern der Testklasse laden.
* `underlineDecorationRendersUnderlinedText` — `TextAttribute.UNDERLINE_ON` im gerenderten Bild
  nachweisbar (z. B. über Pixel-Sampling unterhalb der Baseline).
* `strikethroughDecorationRendersStrikethroughText` — Linie auf halber x-Höhe nachweisbar.
* `dimDecorationReducesOpacity` — reduzierte Alpha-Komponente im gerenderten Bild nachweisbar.
* `combinedDecorationsRenderTogether` — mehrere Dekorationen gleichzeitig ohne Konflikt.
* `noDecorationsRendersUnchangedAsBefore` — Regressionstest gegen bisheriges Rendering (bestehender
  einfacher `drawString`-Pfad bleibt für `decorations.isEmpty()` erhalten).

## Aufgaben

### Aufgabe 1 — Rendering-Erweiterung

* Hilfsfunktion zur Erzeugung eines `TextAttribute`-Sets aus `Font.decorations` ergänzen.
* `Graphics2DDocumentRenderer` für nicht-leere `decorations` auf attribuierten Text umstellen.
* Dim über temporäre Alpha-Reduktion der Zeichenfarbe umsetzen.

### Aufgabe 2 — Tests, Demo, Build, Abschluss

* `testing`-Skill laden; Renderer-Tests gemäß „Testentwurf" erweitern.
* `DemoDocuments` um ein Beispiel mit allen drei Dekorationen ergänzen.
* Build von `:ui:swing` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-07 im Status `COMPLETED`, IP-07 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, `FP-003-Overview.md` aktualisieren, diese Plandatei
  mit `git rm` entfernen (letzter Plan des Features).

## Risiken und offene Punkte

* Exakter Dim-Alphawert ist eine gestalterische Entscheidung; wird als benannte Konstante mit
  Kommentar zur Herleitung festgelegt, konsistent zum in IP-06 gewählten Wert.
* Pixelbasierte Tests für Linien-/Alpha-Nachweis müssen headless (AWT `java.awt.headless=true`,
  bereits in `ui/swing/build.gradle.kts` konfiguriert) stabil funktionieren.
