# Feature Plan: Seiten-Umrandungs-Dekorationen

## 1. Ziel

`PaperSheetView` (fx und swing) erhält einen Mechanismus, um eigene Nodes/Components fest an den
vier Kanten jeder einzelnen Seite zu platzieren - im Komponententeil um das Sheet herum, nicht im
Dokumentinhalt selbst. Anders als `FloatingOverlay` sind diese Dekorationen nicht an eine
Interaktion (Hover, Selektion, Caret) gekoppelt, sondern dauerhaft an eine Seite gebunden und
folgen ihr durch Scroll und Zoom.

## 2. Ist-Zustand

* `FloatingOverlay` (fx: `ui/fx/.../FloatingOverlay.kt`, swing: `ui/swing/.../FloatingOverlay.kt`)
  platziert Nodes/Components nur temporär, ausgelöst durch `FloatingOverlayTrigger`
  (`SELECTION`, `PARAGRAPH_HOVER`, `PAGE_HOVER`, `CARET`); es gibt keinen dauerhaft an eine Seite
  gebundenen Anker.
* `PaperSheetView.outerMargin` (fx/swing) reserviert nur einen einheitlichen Leerraum um den
  gesamten Sheet-Stack; er ist rein numerisch und kennt keine Inhalte.
* Seiten werden über die stabile `Page.id` identifiziert (siehe `pageModes`-Mechanismus in
  `PaperSheetView`), was bereits als Muster für Pro-Seite-Overrides dient.
* `ui:common` (`org.pcsoft.framework.simplay.uicommon`) enthält bereits geteilte,
  UI-Toolkit-unabhängige Modelle (`PageMode`, `EditableRegions`, `ZoomDpiScale`), auf denen fx und
  swing jeweils ihre eigene, toolkit-spezifische Klasse aufbauen.
* fx und swing spiegeln sich strukturell (eigene `FloatingOverlay`-, `PaperSheetView`- und
  Skin/UI-Klassen je Modul), Swing dokumentiert sich stets explizit als Pendant zur fx-Klasse.

## 3. Soll-Zustand

* Eine neue, pro Seite wirkende Dekorations-Abstraktion erlaubt es, Nodes/Components an den vier
  Kanten (oben, unten, links, rechts) einer einzelnen Seite zu verankern.
* Die Dekorationen liegen im Komponentenbereich (innerhalb von `outerMargin`/`pageGap`), nicht im
  Seiteninhalt, und werden nicht Teil des `Document`-Modells.
* Die Positionierung folgt Scroll und Zoom wie bei `FloatingOverlay`.
* Registrierung erfolgt analog zu `floatingOverlays`: über eine öffentliche, programmatisch und
  (fx) per FXML befüllbare Liste an `PaperSheetView`.
* Die Funktionalität steht sowohl in `ui:fx` als auch in `ui:swing` zur Verfügung, mit demselben
  Grundmodell wie beim bestehenden `FloatingOverlay`-Pattern.

## 4. Anforderungen

### Funktionale Anforderungen

* Dekorationen werden einer Seite über deren stabile `Page.id` zugeordnet.
* Jede Kante (oben/unten/links/rechts) einer Seite trägt höchstens eine Dekorations-Node.
* Sichtbarkeit/Ausrichtung der Dekoration ist konfigurierbar (analog `anchor`/`offsetX`/`offsetY`
  bei `FloatingOverlay`).
* Dekorationen folgen ihrer Seite bei Scroll und Zoom.
* Dekorationen sind unabhängig vom `PaperSheetMode`/`PageMode` sichtbar, sofern nicht anders
  konfiguriert.

### Technische Anforderungen

* Kotlin- und Gradle-Konventionen des Projekts werden eingehalten.
* Kein neues Drittanbieter-Abhängigkeit ohne Rückfrage beim Nutzer.
* Gemeinsame, toolkit-unabhängige Konzepte (Kanten-Enum, Ausrichtungslogik) liegen in `ui:common`;
  fx und swing implementieren jeweils ihre eigene, toolkit-spezifische Klasse darüber, wie beim
  bestehenden `FloatingOverlay`-Pattern.
* Bestehende `FloatingOverlay`-Mechanik bleibt unverändert und unabhängig von der neuen
  Dekorations-Abstraktion.

## 5. Architektur

* Neue toolkit-unabhängige Bausteine in `ui:common`: ein Kanten-Enum (oben/unten/links/rechts) und
  ggf. eine gemeinsame Ausrichtungs-/Offset-Berechnung, wiederverwendet von fx und swing.
* Neue Klasse `PageDecoration` je UI-Modul (fx: JavaFX `Node`-Property + FXML-Unterstützung; swing:
  `JComponent`-Property), analog zu `FloatingOverlay`.
* Neue Property-Liste an `PaperSheetView` (fx und swing), z. B. `pageDecorations`, mit Zuordnung zu
  `Page.id`.
* Integration in die bestehende Skin- (`PaperSheetViewSkin`, fx) bzw. UI-Delegate-Schicht
  (`BasicPaperSheetUI`/`PaperSheetUI`, swing), die Seiten-Layout, Scroll und Zoom bereits kennt.
* Kein Eingriff in `engine.model.Document`/`Page`; die Zuordnung erfolgt ausschließlich
  view-seitig, wie bei `pageModes`.

## 6. Übersicht der Implementierungspläne

| ID    | Implementierungsplan                          | Ziel                                                                 | Abhängigkeiten |
| ----- | ---------------------------------------------- | --------------------------------------------------------------------| -------------- |
| IP-01 (COMPLETED) | Gemeinsames Kanten-/Anker-Modell (ui:common)   | Toolkit-unabhängiges Kanten-Enum und Ausrichtungslogik               | -              |
| IP-02 | Umsetzung `PageDecoration` (fx & swing)        | `PageDecoration`, Property-Liste, Skin-/UI-Delegate-Integration in beiden Modulen | IP-01 |
| IP-03 | Dokumentation & Tests                          | MkDocs-Seiten, JUnit/TestFX-Tests für fx und swing                   | IP-02          |

## 7. Implementierungspläne

### IP-01 (COMPLETED): Gemeinsames Kanten-/Anker-Modell (ui:common)

**Ziel**

Ein toolkit-unabhängiges Modell für die vier Seitenkanten und deren Ausrichtungs-/Offset-Logik
schaffen, das fx und swing gleichermaßen wiederverwenden können.

**Umfang**

* Enthalten: Kanten-Enum, gemeinsame Positionsberechnung, Unit-Tests in `ui:common`.
* Nicht enthalten: jede toolkit-spezifische Node/Component-Bindung.

**Abhängigkeiten**

Keine - unabhängig.

**Schnittstellen zu anderen Plänen**

Stellt das Kanten-Enum und die Ausrichtungslogik bereit, die IP-02 konsumiert.

**Tatsächliche Umsetzung**

* `EdgeAlignment` erhielt zusätzlich zu `START`/`CENTER`/`END` den Wert `STRETCH`
  (volle Kantenlänge), der als Standardwert von `PageDecorationPlacement.alignment` dient.
* Positionsberechnung nutzt die bereits vorhandenen `engine.geometry`-Typen `Rect`/`Size` statt
  neuer eigener Geometrie-Typen.

### IP-02: Umsetzung `PageDecoration` (fx & swing)

**Ziel**

Die Dekorations-Abstraktion für `ui:fx` und `ui:swing` umsetzen: je Modul eine `PageDecoration`-
Klasse, eine Property-Liste an `PaperSheetView` und die Einbindung in die jeweilige
Skin-/UI-Delegate-Schicht (fx: `PaperSheetViewSkin`, FXML-Unterstützung, Styling; swing:
`BasicPaperSheetUI`/`PaperSheetUI`).

**Umfang**

* Enthalten: `PageDecoration`-Klasse und `pageDecorations`-Property je Modul, Rendering/
  Positionierung in Skin (fx) bzw. UI-Delegate (swing), FXML-Beispiel und CSS-Anpassungen (fx).
* Nicht enthalten: gemeinsames Kanten-Modell (siehe IP-01), Dokumentation und Tests (siehe IP-03).

**Abhängigkeiten**

IP-01.

**Schnittstellen zu anderen Plänen**

Konsumiert das Kanten-Enum aus IP-01; liefert die fertige `PageDecoration`-API beider Module an
IP-03.

### IP-03: Dokumentation & Tests

**Ziel**

Die neue Dekorations-Abstraktion in beiden Modulen dokumentieren und mit Tests absichern.

**Umfang**

* Enthalten: MkDocs-Seiten analog zu `floating-overlays.md` für fx und swing, JUnit-Tests
  (`ui:common`), TestFX-Tests (fx), Swing-Testäquivalent.
* Nicht enthalten: jede Produktivcode-Änderung an `PageDecoration` selbst.

**Abhängigkeiten**

IP-02.

**Schnittstellen zu anderen Plänen**

Verbraucht die in IP-02 fertiggestellten öffentlichen APIs beider Module.

## 8. Abhängigkeitsgraph

```text
IP-01 (COMPLETED)
└── IP-02
    └── IP-03
```

## 9. Risiken und offene Fragen

* Offen: Soll `pageDecorations` global an `PaperSheetView` liegen (mit `pageId`-Feld je Eintrag)
  oder als Map `Page.id -> Map<Edge, PageDecoration>`, analog zu `pageModes`? Wird in IP-02
  entschieden.
* Offen: Verhalten, wenn `outerMargin`/`pageGap` zu klein für den Platzbedarf einer Dekoration ist
  (Clipping vs. automatische Vergrößerung) - wird in IP-02 festgelegt.
* Risiko: Zusätzliche Positionierungslogik in der Skin/UI-Delegate-Schicht kann mit der
  bestehenden `FloatingOverlay`-Clamping-Logik kollidieren; muss in IP-02 geprüft werden.
* Risiko: Da IP-02 beide Module in einem Plan bündelt, ist der Plan größer als ein
  Einzelmodul-Plan; auf konsistente, aber pro Modul klar abgegrenzte Teilaufgaben achten.

## 10. Abschlusskriterien des Features

* An jeder der vier Kanten einer beliebigen Seite kann in fx und in swing mindestens eine
  Dekoration angezeigt werden, die Scroll und Zoom korrekt folgt.
* Die neue API ist in beiden Modulen dokumentiert (MkDocs) und durch Tests abgedeckt.
* Bestehendes `FloatingOverlay`-Verhalten ist unverändert (Regressionstests weiterhin grün).
