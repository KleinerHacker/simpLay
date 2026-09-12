# swing - Paper sheet component

`PaperSheetView` in `org.pcsoft.framework.simplay.swing` is a scrollable,
zoomable `JComponent` that renders a `Document` as physical-looking sheets
stacked vertically. It is the Swing counterpart of the `fx` module's
`PaperSheetView`.

## Adding it to a UI

```kotlin
val view = PaperSheetView().apply {
    document = myDocument
    mode = PaperSheetMode.SELECTABLE
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
| `mode` | `PaperSheetMode` | `SELECTABLE` | `STATIC` (picture only), `SELECTABLE` (selection only), `NAVIGABLE` (selection + caret) or `EDITABLE` (caret + editing); see [Modes](#modes). |
| `pageModes` | `Map<String, PageMode>` | `emptyMap()` | Per-page `PageMode` overrides, keyed by stable `Page.id`; see [Per-page modes](#per-page-modes). Reset to empty when `document` is reloaded from outside, but not by an edit. |
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

## Modes

`PaperSheetMode` selects what the component does with the document; each mode is
a strict superset of the one above it:

| Mode | Behaviour |
|------|-----------|
| `STATIC` | The document behaves like an image: no selection, no caret, no editing, no floating overlays, the default arrow mouse cursor and no keyboard focus. Zooming, scrolling and the `hoveredParagraph` / `hoveredPage` readouts still work. |
| `SELECTABLE` (default) | Selectable, copyable text, no caret. `document` is never mutated. |
| `NAVIGABLE` | Everything `SELECTABLE` offers plus a blinking caret and the caret-navigation keys. `document` is still never mutated. |
| `EDITABLE` | Everything `NAVIGABLE` offers plus character insertion and removal, clipboard cut / copy / paste, line and selection duplication and drag-and-drop of the selection. An edit replaces `document` with a new instance. |

The capability flags can also be read off the enum constant directly
(`supportsSelection`, `supportsCaret`, `supportsEditing`, `supportsFocus`).
Switching down to a mode without selection drops the current selection.

## Per-page modes

Individual pages can override `mode` with their own `PageMode`, keyed by their
stable `org.pcsoft.framework.simplay.engine.model.Page.id`. A page absent from
`pageModes` simply follows `mode`; an overridden page uses its own `PageMode`
instead, independently of `mode` - both more restrictive (a read-only page in
an editable view) and more permissive (an editable page in a static view) are
possible. `pageModes` is transient view state, never persisted in `document`,
and is reset to empty automatically whenever `document` is reloaded from
outside - an edit, which also replaces `document` with a new instance, leaves
it untouched, since the page ids it is keyed by do not change:

```kotlin
view.setPageMode(0, PageMode.HIDDEN) // resolves index 0 against the current document
```

`PageMode` defines the same capability flags as `PaperSheetMode` plus two
layout-only ones:

| Mode | Meaning |
|------|---------|
| `HIDDEN` | The page (and its flow overflow sheets) is removed entirely from layout, scroll area and hit-testing; the document itself is unchanged. |
| `DISABLED` | The page is not editable and the caret skips over it. It is also drawn with the special disabled fill and hatch, shows no floating overlay and keeps the default arrow mouse cursor. |
| `STATIC` | The page behaves like an image: no selection, no caret, no editing, no special drawing. |
| `SELECTABLE` | Selectable, copyable text, no caret. The page is never mutated. |
| `NAVIGABLE` | Everything `SELECTABLE` offers plus a caret that reaches and crosses the page; every mutation touching it is discarded. |
| `EDITABLE` | Everything `NAVIGABLE` offers plus the document mutations. |

`setPageMode(index, mode)` resolves `index` against the current `document`
into an id immediately, so the override stays attached to that page even as
later edits shift page indices; `mode = null` clears the override.
`FloatingOverlayEvent.pageDeactivated` reports whether the triggering page
currently has a `pageModes` override.

## Selection model

`selectionModel` is a `TextSelectionModel`, the same instance for the life of the
view:

* Read-only state: `text`, `startIndex`, `endIndex`, `length`, `isEmpty`,
  `bounds` (a `Rectangle` in viewport pixels), `runs` (a `List<TextSelectionData>`,
  one per covered text part, carrying family / size / bold / italic).
* Commands: `selectRange(start, end)`, `selectAll()`, `clearSelection()`.
* `addPropertyChangeListener(name, listener)` with the `PROP_*` names (`PROP_TEXT`,
  `PROP_LENGTH`, `PROP_BOUNDS`, ...).

## Caret model (`NAVIGABLE` and `EDITABLE`)

`caretModel` is a `CaretModel`; in the other modes every move command is a no-op:

* Read-only state: `position`, `bounds` (`Rectangle`, `null` without a caret),
  `isVisible` (blink phase), `blockCount`, `wordCount`, `symbolCount`.
* Linear commands: `moveTo(index)`, `moveToStart()`, `moveToEnd()`.
* Absolute structural commands, addressing a zero-based ordinal:
  `moveIntoBlock` / `moveToStartOfBlock` / `moveToEndOfBlock` and the `Word` /
  `Symbol` siblings.
* Relative structural commands from the current position: `moveToNextWord` /
  `moveToPrevWord` and the `Block` / `Symbol` siblings. `moveToNextPage` /
  `moveToPrevPage` jump a whole page, keeping the caret's line ordinal on the
  target page (clamped to its last line) instead of a linear offset.

A command issued before the component has its UI delegate is applied once the
delegate is attached.

## Scrolling

Four commands scroll the viewport directly, addressing the document the same way
the caret model's structural commands do - but, unlike the caret model, they work
in **every** `PaperSheetMode`, including `STATIC` and `SELECTABLE`, since scrolling
never touches the caret or the selection:

* `scrollToPage(page)` - the page's top edge aligns with the viewport top.
* `scrollToBlock(block)` - the top line of block (paragraph) `block`.
* `scrollToWord(word)` - the top line containing word `word`.
* `scrollToSymbol(symbol)` - the top line containing symbol (character) `symbol`.

Every ordinal and page index is clamped into range. A command issued before the
component has its UI delegate is applied once the delegate is attached, exactly
like the caret model's commands.

## Keyboard

| Shortcut | Action |
|----------|--------|
| `Ctrl+C` / `Cmd+C` | Copy the selection as plain text + styled HTML + RTF. Works in both modes. |
| double-click | Select the word under the pointer. |
| `Ctrl+X` | Cut the selection (editable). |
| `Ctrl+V` | Paste plain text at the caret / over the selection (editable). |
| `Ctrl+D` | Duplicate the selection, or the caret line when the selection is empty (editable). |
| arrows, `Home`, `End`, `Ctrl+Home`, `Ctrl+End`, `Ctrl+Left`, `Ctrl+Right` | Caret navigation, optionally with `Shift` to extend the selection (editable). |
| `Page Up` / `Page Down` | Move the caret one page, keeping its line and column, optionally with `Shift` to extend the selection (editable). |
| `Backspace`, `Delete` | Delete backward / forward, or the selection (editable). |
| typing | Insert the character at the caret, replacing the selection (editable). |

Dragging an existing selection with the mouse moves it; holding the copy
modifier (`Ctrl` / `Cmd`) while dropping copies it instead.

## Pluggable delegate

The rendering, measuring, scrolling and input handling live in a `PaperSheetUI`
delegate (`BasicPaperSheetUI` by default), the Swing counterpart of the `fx`
module's skin. `getPaperSheetUI()` returns it; a Look-and-Feel may replace it.
