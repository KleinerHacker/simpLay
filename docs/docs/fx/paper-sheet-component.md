# fx - Paper sheet component

`PaperSheetView` (package `org.pcsoft.framework.simplay.fx`) is a JavaFX
`Control` that renders a [`Document`](../engine/raw-model.md) as physical-looking
sheets - each with a border and a drop shadow - stacked vertically in a
scrollable, zoomable viewport. Only the pages currently in view are drawn (simple
page virtualisation).

Its [`mode`](#modes) switches between read-only viewing and full in-place
editing. Text is always selectable with the mouse and copyable with `Ctrl+C`.

## Adding it to a scene

`PaperSheetView` is a normal control - put it anywhere a `Node` goes:

```kotlin
import org.pcsoft.framework.simplay.fx.PaperSheetView

val view = PaperSheetView().apply {
    document = myDocument
    outerMargin = 24.0
    pageGap = 16.0
    minZoom = 0.5
    maxZoom = 3.0
    zoom = 1.0
}

val scene = Scene(BorderPane(view), 900.0, 700.0)
```

In FXML:

```xml
<?import org.pcsoft.framework.simplay.fx.PaperSheetView?>

<BorderPane xmlns:fx="http://javafx.com/fxml/1">
    <center>
        <PaperSheetView fx:id="view" outerMargin="24.0" pageGap="16.0"/>
    </center>
</BorderPane>
```

The default user-agent stylesheet is applied automatically; see
[Styling the paper sheet component](styling.md).

## Properties

Every property follows the JavaFX bean convention: a `xxxProperty()` accessor for
the property object and `getXxx()` / `setXxx()` (or `isXxx()`) for the value.

| Property | Type | Access | Meaning |
|----------|------|--------|---------|
| `document` | `Document?` | read/write | The document to render; `null` shows an empty view. An edit replaces it with a new instance. |
| `mode` | `PaperSheetMode` | read/write | `READONLY` (default) or `EDITABLE`; see [Modes](#modes). |
| `deactivatedPageIds` | `Set<String>` | read/write | Stable `Page.id` values of the pages currently marked deactivated; see [Deactivating pages](#deactivating-pages). |
| `deactivatedPageHandling` | `PageDeactivationMode` | read/write | How `deactivatedPageIds` is honoured. Default `READONLY`. |
| `outerMargin` | `Double` | read/write, styleable | Space in layout units around the whole sheet stack. Default `24.0`. |
| `pageGap` | `Double` | read/write, styleable | Vertical space in layout units between two sheets. Default `16.0`. |
| `minZoom` | `Double` | read/write | Lower bound for `zoom`. Default `0.25`. |
| `maxZoom` | `Double` | read/write | Upper bound for `zoom`. Default `4.0`. |
| `zoom` | `Double` | read/write | Current scale factor for the whole view, always kept within `[minZoom, maxZoom]`; assigning outside the range, or narrowing the range, re-clamps it. Default `1.0`. |
| `smoothCaretBlink` | `Boolean` | read/write | When `true`, the caret fades in and out instead of blinking hard. Off by default; only effective in `EDITABLE`. |
| `contentSize` | `Dimension2D` | read-only | Unscaled size of the whole sheet stack including `outerMargin` on every side; `0 x 0` for a `null` document. |
| `selectionModel` | `TextSelectionModel` | read-only | The selection state and commands; see [Selection and clipboard](#selection-and-clipboard). |
| `caretModel` | `CaretModel` | read-only | The caret state and move commands; see [The caret model](#the-caret-model). |
| `selectedText` | `String` | read-only | Convenience delegate for `selectionModel.text`. |
| `selectionBounds` | `Bounds?` | read-only | Convenience delegate for `selectionModel.bounds`. |
| `floatingOverlays` | `ObservableList<FloatingOverlay>` | read-only list | Registered overlays; see [Floating overlays](floating-overlays.md). |
| `hoveredParagraph` / `hoveredParagraphBounds` | `Int` / `Bounds?` | read-only | Zero-based ordinal and viewport box of the paragraph under the mouse, or `-1` / `null`. |
| `hoveredPage` / `hoveredPageBounds` | `Int` / `Bounds?` | read-only | Zero-based index and viewport box of the sheet under the mouse, or `-1` / `null`. |

The styling-only colour and shadow properties are listed on the
[styling page](styling.md#properties).

## Modes

`PaperSheetMode` selects what the view does with the document:

| Mode | Behaviour |
|------|-----------|
| `READONLY` (default) | Selectable, copyable text, no caret. `document` is never mutated. |
| `EDITABLE` | Everything `READONLY` offers plus a blinking caret, character insertion and removal, clipboard cut / copy / paste, line and selection duplication, drag-and-drop of the selection and the caret-navigation keys. An edit replaces `document` with a new instance; the previous instance is untouched. |

```kotlin
view.mode = PaperSheetMode.EDITABLE
```

Switching to `READONLY` returns the component to exactly its read-only behaviour.

## Deactivating pages

Individual pages can be marked deactivated by their stable
`org.pcsoft.framework.simplay.engine.model.Page.id`, independently of `mode`.
Both properties are transient view state and are never persisted in `document`:

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

## Selection and clipboard

The mouse always selects: drag to extend, double-click to take a word. The
highlight also covers the whitespace between selected words. `Ctrl+C` copies the
selection to the system clipboard as plain text plus styled HTML and RTF that
carry the font (family, size, weight, slant), so a rich paste target keeps the
text style.

`selectionModel` (`TextSelectionModel`) exposes the state and the commands:

| Member | Meaning |
|--------|---------|
| `text` | Selected text as plain text; `""` when empty. |
| `startIndex` / `endIndex` / `length` | Character range in the document's linear text. |
| `isEmpty` | `true` when nothing is selected. |
| `bounds` | Bounding box in viewport pixels, or `null`. |
| `runs` | `List<TextSelectionData>`, one styled run per covered text part (`text`, `fontFamily`, `fontSize`, `bold`, `italic`). |
| `selectRange(start, end)` | Selects `[start, end)`; index order does not matter, both are clamped. |
| `selectAll()` | Selects the whole document text. |
| `clearSelection()` | Clears the selection. |

```kotlin
view.selectionModel.selectRange(0, 25)
val families = view.selectionModel.runs.map { it.fontFamily }.distinct()
```

## The caret model

In `EDITABLE` mode `caretModel` (`CaretModel`) reports the caret and moves it:

* **State** (read-only): `position` (offset in the linear text), `bounds`
  (viewport rectangle, `null` in read-only mode), `isVisible` (blink phase),
  and `blockCount` / `wordCount` / `symbolCount` of the current document.
* **Linear commands**: `moveTo(index)`, `moveToStart()`, `moveToEnd()`.
* **Absolute structural commands**, addressing a zero-based ordinal:
  `moveIntoBlock(block, index)`, `moveToStartOfBlock(block)`,
  `moveToEndOfBlock(block)` and the `...Word` / `...Symbol` siblings. Every
  ordinal and offset is clamped into range.
* **Relative structural commands** from the current position:
  `moveToNextWord()` / `moveToPrevWord()` and the block / symbol siblings; they
  stop at the document bounds.

A command issued before the view has rendered once is applied as soon as its skin
is attached.

## Shortcuts

`Ctrl` is the shortcut key on Windows and Linux, `Cmd` on macOS. Hold `Shift`
with any caret-navigation key to extend the selection instead of moving.

| Key | Action | Mode |
|-----|--------|------|
| `Ctrl+C` | Copy selection (plain text + styled HTML + RTF) | both |
| `Ctrl+X` | Cut selection | `EDITABLE` |
| `Ctrl+V` | Paste clipboard text at the caret | `EDITABLE` |
| `Ctrl+D` | Duplicate the selection, or the current line when nothing is selected | `EDITABLE` |
| `Left` / `Right` | Move caret one character | `EDITABLE` |
| `Ctrl+Left` / `Ctrl+Right` | Move caret one word | `EDITABLE` |
| `Up` / `Down` | Move caret one line | `EDITABLE` |
| `Home` / `End` | Move caret to line start / end | `EDITABLE` |
| `Ctrl+Home` / `Ctrl+End` | Move caret to document start / end | `EDITABLE` |
| `Backspace` | Delete the character before the caret, or the selection | `EDITABLE` |
| `Delete` | Delete the character after the caret, or the selection | `EDITABLE` |
| Printable key | Insert the character at the caret | `EDITABLE` |

Dragging with the mouse inside an existing selection box moves the selected text;
holding `Ctrl` while releasing copies it instead of moving it.

## Loading and saving a document

`PaperSheetView` only ever consumes and produces an `engine`
[`Document`](../engine/raw-model.md). The raw model is serializable, so
persistence is a plain `engine` concern:

```kotlin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pcsoft.framework.simplay.engine.model.Document

// load
val document: Document = Json.decodeFromString(jsonText)
view.document = document

// save the current (possibly edited) state
val currentJson: String = Json.encodeToString(view.document)
```

YAML (`kaml`) and JVM serialization work the same way; see
[Raw document model](../engine/raw-model.md#persistence). After an edit,
`view.document` is a new `Document` instance that reflects every change.

## Next

* [Floating overlays](floating-overlays.md) - attaching your own nodes to
  selection, hover and caret.
* [Styling the paper sheet component](styling.md) - the JavaFX CSS reference.
* [Canvas rendering](canvas-rendering.md) - drawing without a control.
