# FP-003 / IP-06: FX-Anpassung: Darstellung der neuen Dekorationen

Feature Plan: `.claude/plans/features/FP-003-AnsiConsoleStyling.md`
Status: `.claude/plans/features/FP-003-AnsiConsoleStyling-Status.md`

## Ziel

* `ui:fx` stellt die in IP-01 eingeführten Dekorationen (Unterstrichen, Durchgestrichen, Dim)
  beim Zeichnen auf `Canvas` dar.

## Umfang

### Enthalten

* Anpassung von `CanvasRenderer.renderDocument`/`renderPage` (Package
  `org.pcsoft.framework.simplay.fx.internal`), sodass Unterstrichen/Durchgestrichen über
  JavaFX-`Text`-Eigenschaften und Dim über reduzierte Deckkraft/Füllfarbe umgesetzt werden.
* Tests für die neue Darstellung.

### Nicht enthalten

* Neue Engine-Modelländerungen (bereits in IP-01 abgeschlossen).
* Farbe.
* Änderungen an `ui:console` oder `ui:swing`.

## Abhängigkeiten

* IP-01. Wird gemäß Nutzervorgabe erst nach Abschluss von IP-01 bis IP-05 eingeplant.

## Schnittstellen zu anderen Plänen

* Konsumiert `Font.decorations` aus IP-01. Keine Abhängigkeit zu IP-02 bis IP-05.

## Betroffene Dateien

| Datei | Änderung |
| ----- | -------- |
| `ui/fx/src/main/kotlin/org/pcsoft/framework/simplay/fx/internal/CanvasRenderer.kt` | Dekorationen beim Zeichnen jedes Textteils anwenden. |
| `ui/fx/src/test/kotlin/org/pcsoft/framework/simplay/fx/internal/CanvasRendererTest.kt` | Tests für Underline/Strikethrough/Dim ergänzen. |
| `ui/fx/src/demo/kotlin/org/pcsoft/framework/simplay/fx/demo/DemoDocuments.kt` | Beispiel mit Dekorationen ergänzen, damit die Demo sie zeigt. |

## Entwurf

* `Canvas`/`GraphicsContext` kennt kein natives Underline/Strikethrough wie ein `Text`-Node;
  Umsetzung über manuell gezeichnete Linien auf Höhe der Baseline (Underline: leicht unterhalb
  der Baseline, Strikethrough: auf halber x-Höhe), Linienbreite/-position aus `MeasuredFont`-
  Metriken abgeleitet, analog zur bestehenden Seitentrennlinie in
  `ui/fx/src/main/kotlin/.../internal/ps/PaperSheetOverlays.kt` bzw. dem Swing-Pendant.
* Dim wird über eine reduzierte Deckkraft der Zeichenfarbe umgesetzt (z. B. `gc.globalAlpha`
  temporär auf einen festen, dokumentierten Wert setzen, danach zurücksetzen).
* Reihenfolge beim Zeichnen: Text zuerst, danach Linien für Underline/Strikethrough, damit die
  Linie über dem Text sichtbar bleibt (wie in gängigen Textverarbeitungen).

## Testentwurf

* `testing`-Skill vor Ändern der Testklasse laden.
* `underlineDecorationDrawsLineBelowBaseline` — Pixel-/Pfad-Prüfung der gezeichneten Linie.
* `strikethroughDecorationDrawsLineThroughText` — Linie auf halber x-Höhe.
* `dimDecorationReducesOpacity` — reduzierte Deckkraft im gerenderten Bild nachweisbar.
* `combinedDecorationsRenderTogether` — mehrere Dekorationen gleichzeitig ohne Konflikt.
* `noDecorationsRendersUnchangedAsBefore` — Regressionstest gegen bisheriges Rendering.

## Aufgaben

### Aufgabe 1 — Rendering-Erweiterung

* `CanvasRenderer` um Linienzeichnung für Underline/Strikethrough je Textteil erweitern.
* Dim über temporäre Deckkraft-Reduktion beim Zeichnen umsetzen.
* Zurücksetzen der Zeichenzustände nach jedem Textteil sicherstellen.

### Aufgabe 2 — Tests, Demo, Build, Abschluss

* `testing`-Skill laden; `CanvasRendererTest` gemäß „Testentwurf" erweitern.
* `DemoDocuments` um ein Beispiel mit allen drei Dekorationen ergänzen.
* Build von `:ui:fx` an einen Agenten delegieren (Gradle-Task, kein Inline-Kommando) und Befunde beheben.
* Im selben Change-Set: IP-06 im Status `COMPLETED`, IP-06 überall in
  `FP-003-AnsiConsoleStyling.md` abhaken, `FP-003-Overview.md` aktualisieren, diese Plandatei
  mit `git rm` entfernen.

## Risiken und offene Punkte

* Exakter Dim-Deckkraftwert ist eine gestalterische Entscheidung; wird als benannte Konstante
  mit Kommentar zur Herleitung festgelegt.
* Linienposition/-breite für Underline/Strikethrough muss visuell mit realen Fonts geprüft werden,
  nicht nur rechnerisch aus Metriken.
