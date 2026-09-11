# swing - Paper sheet component

`PaperSheetView` in `org.pcsoft.framework.simplay.swing` is a scrollable,
zoomable `JComponent` that renders a `Document` as physical-looking sheets
stacked vertically. It is the Swing counterpart of the `fx` module's
`PaperSheetView`.

## Adding it to a UI

```kotlin
val view = PaperSheetView().apply {
    document = myDocument
    mode = PaperSheetMode.READONLY
}
someContainer.add(view, BorderLayout.CENTER)
```

The component manages its own vertical scroll bar and virtualises the page
painting, so it can be added directly to any container - it does not need to be
wrapped in a `JScrollPane`.

## Properties

All properties fire a `java.beans.PropertyChangeEvent` under the `PROP_*` name
shown; register a `java.beans.PropertyChangeListener` to react.

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `document` | `Document?` | `null` | The document to render. |
| `mode` | `PaperSheetMode` | `READONLY` | `READONLY` (selection only) or `EDITABLE` (caret + editing). |
| `deactivatedPageIds` | `Set<String>` | `emptySet()` | Stable `Page.id` values of the pages currently marked deactivated; see [Deactivating pages](#deactivating-pages). |
| `deactivatedPageHandling` | `PageDeactivationMode` | `READONLY` | How `deactivatedPageIds` is honoured. |
| `outerMargin` | `Double` | `24.0` | Space around the whole sheet stack, in layout units. |
| `pageGap` | `Double` | `16.0` | Vertical space between two sheets, in layout units. |
| `minZoom` / `maxZoom` / `zoom` | `Double` | `0.25` / `4.0` / `1.0` | `zoom` is always clamped into `[minZoom, maxZoom]`. |
| `smoothCaretBlink` | `Boolean` | `false` | Fade the caret instead of blinking it hard on and off. |
| `contentSize` | `Dimension` (read-only) | `0 x 0` | The unscaled size of the sheet stack including `outerMargin` on every side. |
| `selectedText` | `String` (read-only) | `""` | Convenience delegate for `selectionModel.text`. |
| `selectionBounds` | `Rectangle?` (read-only) | `null` | Convenience delegate for `selectionModel.bounds`. |
| `hoveredParagraph` / `hoveredPage` | `Int` (read-only) | `-1` | Ordinal of the paragraph / index of the sheet under the mouse, or `-1`. |
| `hoveredParagraphBounds` / `hoveredPageBounds` | `Rectangle?` (read-only) | `null` | Their boxes in viewport pixels. |

The sheet chrome, drop shadow, selection highlight and caret are styled through
the Look-and-Feel - see [Styling](styling.md).

## Deactivating pages

Individual pages can be marked deactivated by their stable
`org.pcsoft.framework.simplay.engine.model.Page.id`, independently of `mode`.
Both properties are transient view state and are never persisted in
`document`:

```kotlin
view.setPageDeactivated(0, true) // resolves index 0 against the current document
view.deactivatedPageHandling = PageDeactivationMode.HIDDEN
```

`PageDeactivationMode` controls how `deactivatedPageIds` is honoured:

| Mode | Meaning |
|------|---------|
| `IGNORE` | `deactivatedPageIds` is fully ignored; behaves as an empty set. |
| `DISABLED` | The page is not editable and the caret skips over it. |
| `READONLY` (default) | The caret reaches and crosses the page normally, selection works, but every mutation touching it is discarded. |
| `HIDDEN` | The page (and its flow overflow sheets) is removed entirely from layout, scroll area and hit-testing; the document itself is unchanged. |

`setPageDeactivated(index, deactivated)` resolves `index` against the current
`document` into an id immediately, so the marker stays attached to that page
even as later edits shift page indices. `FloatingOverlayEvent.pageDeactivated`
reports whether the triggering page is currently deactivated.

## Selection model

`selectionModel` is a `TextSelectionModel`, the same instance for the life of the
view:

* Read-only state: `text`, `startIndex`, `endIndex`, `length`, `isEmpty`,
  `bounds` (a `Rectangle` in viewport pixels), `runs` (a `List<TextSelectionData>`,
  one per covered text part, carrying family / size / bold / italic).
* Commands: `selectRange(start, end)`, `selectAll()`, `clearSelection()`.
* `addPropertyChangeListener(name, listener)` with the `PROP_*` names (`PROP_TEXT`,
  `PROP_LENGTH`, `PROP_BOUNDS`, ...).

## Caret model (editable mode)

`caretModel` is a `CaretModel`:

* Read-only state: `position`, `bounds` (`Rectangle`, `null` in read-only mode),
  `isVisible` (blink phase), `blockCount`, `wordCount`, `symbolCount`.
* Linear commands: `moveTo(index)`, `moveToStart()`, `moveToEnd()`.
* Absolute structural commands, addressing a zero-based ordinal:
  `moveIntoBlock` / `moveToStartOfBlock` / `moveToEndOfBlock` and the `Word` /
  `Symbol` siblings.
* Relative structural commands from the current position: `moveToNextWord` /
  `moveToPrevWord` and the `Block` / `Symbol` siblings.

A command issued before the component has its UI delegate is applied once the
delegate is attached.

## Keyboard

| Shortcut | Action |
|----------|--------|
| `Ctrl+C` / `Cmd+C` | Copy the selection as plain text + styled HTML + RTF. Works in both modes. |
| double-click | Select the word under the pointer. |
| `Ctrl+X` | Cut the selection (editable). |
| `Ctrl+V` | Paste plain text at the caret / over the selection (editable). |
| `Ctrl+D` | Duplicate the selection, or the caret line when the selection is empty (editable). |
| arrows, `Home`, `End`, `Ctrl+Home`, `Ctrl+End`, `Ctrl+Left`, `Ctrl+Right` | Caret navigation, optionally with `Shift` to extend the selection (editable). |
| `Backspace`, `Delete` | Delete backward / forward, or the selection (editable). |
| typing | Insert the character at the caret, replacing the selection (editable). |

Dragging an existing selection with the mouse moves it; holding the copy
modifier (`Ctrl` / `Cmd`) while dropping copies it instead.

## Pluggable delegate

The rendering, measuring, scrolling and input handling live in a `PaperSheetUI`
delegate (`BasicPaperSheetUI` by default), the Swing counterpart of the `fx`
module's skin. `getPaperSheetUI()` returns it; a Look-and-Feel may replace it.
