# Swing-Portierung des `fx`-Moduls

## Entscheidungen (mit Nutzer abgestimmt)

- Styling: voller ComponentUI/Look-and-Feel-Weg (`PaperSheetUI` + `BasicPaperSheetUI`, `UIManager`-Defaults).
- Geteilter plattformneutraler Code: neues Modul `:ui-common`, `fx` wird darauf umgestellt, `swing` haengt daran.
- Beobachter: `java.beans.PropertyChangeSupport` auf Komponente und Modellen.
- Struktur: eine `JComponent` mit pluggable `ComponentUI`, nur programmatische API, kein FXML.

## Nicht 1:1 abbildbar - so geloest

- JavaFX-CSS / User-Agent-Stylesheet / Pseudoklassen: ersetzt durch `ComponentUI` + `UIManager`-Keys `PaperSheetView.*`; `:readonly` wird eigener Default-Key.
- FXML fuer `FloatingOverlay`: entfaellt; `content` wird `JComponent`, `FloatingOverlayEvent` erbt von `java.util.EventObject`.
- Observable-Beans / `ObservableList`: ersetzt durch `PropertyChangeSupport` und `List` mit Add/Remove-Methoden plus Change-Events.
- Geometrie: `Bounds`->`java.awt.Rectangle`, `Dimension2D`->`java.awt.Dimension`, `Pos`->neues Enum `OverlayAnchor`, `Paint`/`Color`->`java.awt`-Pendants (Verlaeufe weiter moeglich), `Cursor`->`java.awt.Cursor`.
- `Timeline` (Smooth-Blink): ersetzt durch `javax.swing.Timer`.
- `CanvasDocumentRenderer` (liefert `Canvas`): wird `DocumentImageRenderer` und liefert `BufferedImage`, zusaetzlich `Graphics2D`-Overload.
- Scrollbar: interne vertikale `JScrollBar` im UI-Delegate (verhaltensgleich zur `fx`-Skin). `Scrollable`-Interface vorerst nicht implementiert (kann spaeter ergaenzt werden).
- Schriftmetrik AWT vs. JavaFX: Ausgabe optisch nahezu gleich, nicht pixelgleich - akzeptiert.

## Task 1 - Modul `:ui-common` anlegen

- `settings.gradle.kts`: `include(":ui-common")` ergaenzen.
- `ui-common/build.gradle.kts` neu: `buildsrc.convention.kotlin-jvm`, `dependencies { implementation(project(":engine")); testImplementation(kotlin("test")) }`.
- `licensee`-Block: `allow("Apache-2.0")`, `allow("MIT")`.
- Quellverzeichnisse `ui-common/src/main/kotlin/org/pcsoft/framework/simplay/uicommon/` und `.../src/test/kotlin/...` mit `git add` anlegen.
- Copyright-Header wie in bestehenden `build.gradle.kts` uebernehmen.

## Task 2 - Geteilten Code aus `fx` nach `:ui-common` verschieben

- Per `git mv` verschieben: `internal/DocumentEditor.kt`, `internal/DocumentTextIndex.kt`, `internal/GlyphHitTester.kt`, `internal/SelectionSpan.kt`, `internal/StyledTextClipboard.kt`.
- Zielpaket in den 5 Dateien auf `org.pcsoft.framework.simplay.uicommon` setzen, `internal`-Sichtbarkeit auf `public` anheben.
- `DocumentEditorTest.kt` und `GlyphHitTesterTest.kt` vorerst in `fx` belassen (nur Importe auf `...uicommon` angepasst); Verschiebung nach `:ui-common` mit Stub-`FontMeasureCalculator` erfolgt in Task 10 (benoetigt `testing`-Skill).
- `fx/build.gradle.kts`: `implementation(project(":ui-common"))` ergaenzen.
- In `fx`: Importe der 5 Typen von `...fx.internal` auf `...uicommon` umstellen (`PaperSheetViewSkin`, `ps/PaperSheetCanvasPainter`, `ps/PaperSheetSelection`, `ps/PaperSheetCaret`, `ps/PaperSheetEditor`, `internal/MeasuredCanvasData`, betroffene Tests).
- Gradle-Target `build` fuer `:ui-common` und `:fx` ausfuehren.

## Task 3 - Swing-Grundgeruest: Modul-Setup, Schriftmessung, Enums, Modelle

- `swing/build.gradle.kts`: `implementation(project(":ui-common"))` ergaenzen; kein externer Zusatz-Dependency.
- `SwingFontMeasureCalculator : FontMeasureCalculator` - AWT `Font` + `FontRenderContext`, Cache je `(Font, text)`, Mapping Engine-`Font` -> `java.awt.Font`; Spiegel von `FxFontMeasureCalculator`.
- `PaperSheetMode`-Enum ins Paket `...simplay.swing` kopieren.
- `TextSelectionData` als `data class` kopieren.
- `TextSelectionModel` als einfache Klasse: Zustand + `PropertyChangeSupport`, Kommandos `selectRange`/`selectAll`/`clearSelection` an die View delegiert; `bounds` -> `Rectangle`.
- `CaretModel` als einfache Klasse: Zustand + `PropertyChangeSupport`, lineare/absolute/relative Move-Kommandos an die View delegiert.
- Sink-Interfaces `SelectionCommands` / `CaretCommands` analog `fx` in der View definieren.
- Gradle-Target `build` fuer `:swing` ausfuehren.

## Task 4 - `DocumentImageRenderer` (Bild-Renderer)

- `Graphics2DDocumentRenderer` (intern) - Spiegel von `CanvasRenderer`: Messbaum-Walk, `drawString` je Part auf Baseline, Frame-/Separator-Decorator.
- `ImageRenderConfiguration : RenderConfiguration` mit `unitScale`, `pageGap` - Spiegel von `CanvasRenderConfiguration`.
- `DocumentImageRenderer` mit `of(document) { }`, misst vorab; `pageCount`, `documentImageSize` (`Dimension`), `pageImageSizes[index]`.
- `renderDocument(): BufferedImage` und `renderPage(index): BufferedImage`; zusaetzlich `renderDocument(g2: Graphics2D)` / `renderPage(index, g2)`.
- Gestrichelte Seitentrennlinie in der Gap-Mitte wie im `fx`-Pendant.
- Argumentpruefungen `unitScale > 0`, `pageGap >= 0` uebernehmen.
- Gradle-Target `build` ausfuehren.

## Task 5 - `PaperSheetView` + `PaperSheetUI` (Readonly-Kern)

- `PaperSheetView : JComponent` mit allen `fx`-Properties (document, mode, outerMargin, pageGap, minZoom/maxZoom/zoom, contentSize, Stilwerte, smoothCaretBlink, hovered*), Aenderungen via `firePropertyChange`.
- Zoom-Clamping-Logik und `getUIClassID()` = `"PaperSheetViewUI"`; `updateUI()` registriert Default-UI und `UIManager`-Defaults.
- `PaperSheetUI : ComponentUI` (abstrakt) und `BasicPaperSheetUI` (Default) mit `installUI`/`uninstallUI`, Listener-An/Abmeldung.
- Interne vertikale `JScrollBar` plus Mausrad (im UI-Delegate verwaltet).
- `PaperSheetSwingPainter` - Spiegel von `PaperSheetCanvasPainter`: Viewport-Virtualisierung, Schatten/Fuellung/Rahmen je Blatt, Auswahl-Highlight, Text via `Graphics2DDocumentRenderer`.
- `PaperSheetStyle`-Wertobjekt und `PaperSheetSelection` (Anchor/Focus, Geometrie, styled runs, Command-Sink) portieren.
- Maus: Klick/Drag/Doppelklick -> Auswahl, Hit-Test via `DocumentTextIndex` + `GlyphHitTester`; Textcursor ueber Inhaltsflaeche.
- Clipboard-Copy: `StyledTextTransferable` (`text/html`, `text/rtf`, `stringFlavor`) aus `StyledTextClipboard`; `Ctrl+C` / `Strg+C`.
- Gradle-Target `build` ausfuehren.

## Task 6 - Editiermodus

- `PaperSheetCaret` portieren: Position, Geometrie, Blink via `javax.swing.Timer`, Smooth-Fade, Navigations-Moves.
- `PaperSheetEditor` portieren: Tastenbelegung ueber `InputMap`/`ActionMap` (Tippen, Backspace, Delete, Home/End, Pfeile, `Ctrl+Home/End`, `Ctrl+Left/Right`, je optional Shift).
- `Ctrl+D` Zeilen-/Auswahl-Duplizierung, `Ctrl+X`/`Ctrl+V` ueber `DocumentEditor` und Clipboard.
- Editieren ersetzt `document` durch neue Instanz; altes Dokument unveraendert.
- Drag-and-Drop der Auswahl per Maus-Logik im Delegate (verhaltensgleich zur `fx`-Skin: Press auf Auswahl -> Drop-Preview am Caret, Release -> `editor.dropSelection`; Move bzw. Copy mit Shortcut-Modifier). Kein `TransferHandler`.
- `caretModel` an die UI anbinden; Umschalten `mode` startet/stoppt Caret und Blink.
- Gradle-Target `build` ausfuehren.

## Task 7 - Floating Overlays und Hover

- `FloatingOverlayTrigger`-Enum (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`) kopieren.
- `OverlayAnchor`-Enum (Ersatz fuer `Pos`) mit horizontaler/vertikaler Ausrichtung.
- `FloatingOverlay`: `content: JComponent`, `trigger`, `anchor`, `offsetX/Y`, `autoHide`, Read-only-Aktivzustand via `PropertyChangeSupport`.
- `FloatingOverlayEvent : java.util.EventObject` plus `FloatingOverlayListener` (`onShown`/`onHidden`).
- `PaperSheetView.getFloatingOverlays(): MutableList<FloatingOverlay>` mit Change-Benachrichtigung.
- `PaperSheetHoverTracker` portieren: gehovertes Paragraph/Blatt plus Bounds.
- `PaperSheetOverlays` portieren: Overlay-Ebene als oberste Kind-Komponente (`JLayeredPane`/null-Layout), Ein-/Ausblenden, Positionieren, Clamping an Viewport-Kante, Folgen von Scroll/Zoom.
- Gradle-Target `build` ausfuehren.

## Task 8 - Look-and-Feel-Styling

- `UIManager`-Default-Keys definieren: `PaperSheetView.sheetBackground`, `.sheetBorderColor`, `.sheetBorderWidth`, `.shadowColor`, `.shadowOffset`, `.selectionColor`, `.selectionColorReadonly`, `.caretColor`, `.outerMargin`, `.pageGap`.
- Defaults einmalig registrieren (`UIManager.getDefaults().putIfAbsent(...)`), Werte aus den bestehenden `DEFAULT_*`-Konstanten.
- `BasicPaperSheetUI.installDefaults()` liest Keys via `LookAndFeel.installProperty`; programmatischer Setter gewinnt (Flag "vom Nutzer gesetzt").
- Readonly-Zustand: Auswahlfarbe aus `selectionColorReadonly` beim Moduswechsel setzen.
- `java.awt.Paint` fuer Fuellungen zulassen (Verlaeufe), `caretColor` bleibt `Color`.
- Gradle-Target `build` ausfuehren.

## Task 9 - Demo-Sourceset

- SourceSet `swingDemo` in `swing/build.gradle.kts` analog `demo` in `fx` (nicht publiziert, aus Licensee-Scan ausgenommen).
- `DemoApp` - `JFrame` mit `JTabbedPane`: Tabs `Image`, `Readonly`, `Read/Write`; `main`-Funktion (`...swing.demo.DemoAppKt`).
- `DemoDocuments` und drei Tab-Panels analog zu den `fx`-Demo-Klassen.
- Gradle-Task `run` (`JavaExec`) fuer die Demo registrieren.
- Gradle-Target `build` ausfuehren.

## Task 10 - Tests

- `testing`-Skill vor dem Anlegen jeder Testklasse laden.
- `:ui-common`: verschobene Tests lauffaehig machen, Paketspiegelung pruefen.
- `:swing` Developer-Tests: `SwingFontMeasureCalculator`, `DocumentImageRenderer`, `PaperSheetSwingPainter` (Virtualisierung/Chrome/Caret-Count), `PaperSheetSelection`-Geometrie, `PaperSheetEditor`, `TextSelectionModel`, `CaretModel`, `FloatingOverlay`.
- Headless-AWT (`java.awt.headless=true`) wo moeglich; Komponenten offscreen realisieren.
- Gradle-Target `build` inkl. Tests ausfuehren.

## Task 11 - Doku und CI

- `project-docs`-Skill laden; `docs/docs/swing/implementation.md` fuellen, Guides `paper-sheet-component.md`, `image-rendering.md`, `floating-overlays.md`, `styling.md` analog `fx` anlegen.
- `docs/docs/ui-common/implementation.md` anlegen; `docs/mkdocs.yml`-Navigation erweitern.
- `CHANGELOG.md` ergaenzen.
- `ci-pipeline`-Skill laden; `.github`-Workflows um `:ui-common` und `:swing` (inkl. Demo-/`run`-Task) erweitern.
- Plandatei `.claude/plans/implementation/swing-portierung.md` im selben Change-Set der letzten Task per `git rm` entfernen.

## Verifikation

- `./gradlew build` fuer den gesamten Multi-Projekt-Build gruen (`:engine`, `:ui-common`, `:fx`, `:swing`).
- `./gradlew :fx:build` gruen - Umstellung auf `:ui-common` ohne Regression.
- `./gradlew :swing:test` gruen.
- `./gradlew :swing:run` zeigt die drei Tabs; Readonly-Auswahl + `Strg+C`, Editiermodus mit Caret/Tippen/DnD, ein Floating-Overlay ueber der Auswahl.
- In der Demo `UIManager.put("PaperSheetView.sheetBackground", ...)` vor Erzeugung setzen und Wirkung pruefen; programmatischer Setter ueberschreibt.
- `./gradlew :swing:javadoc`/Dokka und `mkdocs build` ohne Fehler.
