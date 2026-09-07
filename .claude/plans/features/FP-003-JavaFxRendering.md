# Feature Plan: JavaFX Rendering

## 1. Objective

Give the `fx` module a public API that turns a raw `Document` into JavaFX output.
Delivered in rising complexity: first a plain `Canvas` renderer, then a scrollable
paper-sheet component with zoom and text selection, finally in-place editing with
standard key bindings. The public API only ever accepts a `Document`; all
`Measured*` types stay hidden inside the module. Everything lives exclusively in
the `fx` module; no other module is touched. A runnable demo application exists
from the first plan on for visual verification.

## 2. Current State

`fx` is an empty JVM module with the JavaFX Gradle plugin applied
(`javafx.controls`, `javafx.fxml`, version 25) and a dependency on `:engine`. It
has only `.gitkeep` files. `engine` produces a `MeasuredDocument`
(`pages -> blocks -> lines -> parts`, all with absolute `Rect` bounds and
`effectiveSize` per page) via `SimpLayEngine.builder(FontMeasureCalculator)`. No
platform binding, renderer or `FontMeasureCalculator` exists for JavaFX.
`docs/docs/engine/rendering.md` describes the measured-tree walk a renderer must
perform.

## 3. Target State

The `fx` module exposes three cooperating, `Document`-only entry points:

* a `Canvas` renderer that draws the whole document onto one correctly sized
  `Canvas`, page transitions shown as dashed lines;
* a `Control` that shows the pages as real sheets (edges, drop shadow), with
  configurable outer margin and inter-page gap, configurable min/max zoom,
  vertical scrolling, and selectable/copyable text (Ctrl+C);
* the same `Control` with editing enabled, understanding standard commands
  (Ctrl+V, `End`, `Ctrl+End`, arrows, `Home`, `Backspace`, `Delete`, ...).

A shared internal layer provides the JavaFX-backed `FontMeasureCalculator`, the
measured-tree draw walk and the size computation; no measured type appears in any
public signature.

A demo application in `fx/src/demo` with a `TabPane` of three tabs - `Canvas`,
`Readonly`, `Read/Write` - hosts the matching component per tab. Each tab has its
own toolbar that exposes and displays every value the hosted component accepts
from outside. The demo grows tab by tab with the plans and is never a released
artifact.

## 4. Requirements

### Functional Requirements

* All code, tests and the demo stay inside the `fx` module; no other module
  changes.
* Public API takes a `Document` only; measuring happens inside the module.
* Canvas renderer: one `Canvas` for the entire document, pages stacked
  vertically, dashed line between pages, canvas pre-sized to the full content.
* Investigate whether the `Canvas` can instead grow dynamically and record the
  outcome.
* The canvas renderer can draw either the whole document or a single selected
  page onto a `Canvas`; the paper component reuses the per-page path.
* Paper component: sheet visuals (border, shadow), configurable outer margin and
  page spacing, min/max zoom all configurable, vertical scroll only.
* Text in the paper component is selectable with the mouse and copyable via
  Ctrl+C (plain text).
* The paper component has a readonly mode and a normal (editable) mode, switched
  by a mode property. Readonly = no caret but selectable/copyable text; it is
  exactly the IP-03 behaviour. Normal = the readonly behaviour plus editing; it
  is exactly the IP-04 behaviour.
* Editing mode: caret, text insertion/removal, clipboard paste (Ctrl+V), and
  caret navigation commands (`Home`, `End`, `Ctrl+Home`, `Ctrl+End`, arrows).
* Editing produces an updated `Document`; the source `Document` is not mutated in
  place unless its model already allows it.
* A demo application resides in `fx/src/demo` with a `TabPane`: tabs `Canvas`,
  `Readonly`, `Read/Write`, each hosting its component.
* Each demo tab has its own toolbar; every externally settable value of the
  hosted component is editable there and reflects the component's current state.
* Each plan that introduces a component also adds its demo tab and toolbar.
* The `fx` documentation under `docs/docs/fx/` has a page for direct canvas
  rendering (incl. single-page rendering), a paper-sheet usage page (properties,
  settings, integration, load/save) with the supported shortcuts, and a
  paper-sheet styling page.

### Technical Requirements

* Kotlin, Gradle, JVM `src/main` / `src/test`; base package
  `org.pcsoft.framework.simplay.fx`.
* The demo is a dedicated `src/demo` Gradle source set in the `fx` module with a
  `run` task; it depends on `main` and is excluded from the published artifact.
* Reuse `SimpLayEngine`, `MeasuredDocument` and the rendering walk from `engine`;
  no change to `engine` public API without the user's approval.
* JavaFX 25 (`javafx.controls`); headless UI tests per the `testing` skill
  (TestFX).
* No new runtime dependency without the user's approval.
* Documentation updated per the `project-docs` skill (`docs/docs/fx/`).

## 5. Architecture

* New package tree under `org.pcsoft.framework.simplay.fx`:
  * `...fx.internal` - `FxFontMeasureCalculator` (JavaFX `Text`/`Font` metrics),
    the `MeasuredDocument` draw walk against a `GraphicsContext`, page-size and
    document-size helpers, glyph-level hit-testing helper.
  * `...fx.canvas` - the public `Canvas` renderer entry point.
  * `...fx.control` - the public paper-sheet `Control` (skin, viewport, zoom,
    scroll, selection; editing added later on the same class).
* Data flow: `Document` -> internal `SimpLayEngine.measure` ->
  `MeasuredDocument` (module-private) -> draw walk -> JavaFX surface. Selection
  and caret work on measured geometry internally and map back to `Document`
  positions.
* No persistence. Clipboard via `javafx.scene.input.Clipboard`.
* `fx/src/demo` - own Gradle source set (`demo`), `Application` subclass building
  the three-tab `TabPane`, one `<Component>DemoTab` per tab bundling the
  component with its toolbar; sample `Document`s built in the demo only.
* Detailed design decisions belong in the derived implementation plans.

## 6. Implementation Plan Overview

| ID    | Implementation Plan            | Objective                                                                                             | Dependencies |
| ----- | ------------------------------ | ---------------------------------------------------------------------------------------------------- | ------------ |
| IP-01 ✅ | FX Rendering Foundation (COMPLETED) | JavaFX `FontMeasureCalculator`, measured-tree draw walk, size computation, hit-testing helper; `src/demo` source set with an empty three-tab shell. | -            |
| IP-02 | Canvas Renderer                | Public `Document`-only renderer drawing the whole document on one `Canvas` with dashed page breaks; `Canvas` demo tab with toolbar.  | IP-01        |
| IP-03 | Paper Sheet Component          | Public scrollable, zoomable sheet `Control` with margins, page gaps and selectable/copyable text; `Readonly` demo tab with toolbar.    | IP-01        |
| IP-04 | Editing And Key Commands       | Add caret, text editing, Ctrl+V and standard navigation commands to the paper component; `Read/Write` demo tab with toolbar.             | IP-03        |
| IP-05 | Paper Component Styling         | Make the paper `Control` styleable through the standard JavaFX CSS mechanism (`-fx-` properties, default stylesheet, pseudo-classes).    | IP-03        |
| IP-06 | Documentation                  | `docs/docs/fx/` pages: direct canvas rendering (incl. single-page), paper-sheet usage + shortcuts, paper-sheet styling.                 | IP-02, IP-03, IP-04, IP-05 |

IP-02 and IP-03 are independent of each other and parallelizable once IP-01 is
done; IP-04 and IP-05 both build on IP-03 and are parallelizable with each
other. IP-06 comes last and consolidates the documentation for all components.

## 7. Implementation Plans

### IP-01: FX Rendering Foundation ✅ (COMPLETED)

**As built (deviations from plan)**

* The measured-tree walk is split into an internal surface-free `walkPage(...)`
  function plus `renderPage` / `renderDocument` on a `GraphicsContext`, so the
  walk is unit-testable without mocking the final `GraphicsContext` class.
* No separate launcher class: the demo entry point is a top-level `main` in
  `DemoApp.kt` (main class `DemoAppKt`), which lets JavaFX start from the plain
  classpath. The `run` task uses a non-modular classpath launch; the JavaFX
  plugin's "Unsupported JavaFX configuration" warning is expected.
* `DemoLauncher.kt` was not created (not needed); `DemoDocuments.kt` holds the
  sample documents as planned.
* `documentSize` counts the inter-page gap only between pages, never before the
  first or after the last.

**Objective**

Provide the module-internal JavaFX binding: a `FontMeasureCalculator` backed by
the JavaFX text stack, the reusable `MeasuredDocument` draw walk onto a
`GraphicsContext`, document/page size computation, and a helper mapping a point
inside a measured part to a character offset.

**Scope**

* In: `FxFontMeasureCalculator`, draw-walk function(s), `documentSize` /
  `pageSize` helpers, glyph hit-testing helper, unit tests with a headless
  JavaFX toolkit.
* In: `fx/src/demo` Gradle source set, `run` task, demo `Application` with a
  three-tab `TabPane` (`Canvas`, `Readonly`, `Read/Write`) with empty tab bodies
  and a per-tab toolbar placeholder.
* Out: any public entry point, `Canvas` sizing policy, scrolling, zoom,
  selection UI, editing, actual tab content.

**Dependencies**

* Independent within FP-003. Consumes `engine` measured model and rendering doc.

**Interfaces to Other Plans**

* Provides to IP-02/IP-03/IP-04: the internal measurer, the draw walk, size
  helpers and the hit-testing helper. These signatures are the contract for the
  rest of the feature.
* Provides the `src/demo` source set, `run` task and the three-tab shell each
  later plan fills with its tab and toolbar.

### IP-02: Canvas Renderer

**Objective**

Expose a public entry point that measures a `Document` and paints the entire
document onto a single `Canvas`, drawing a dashed separator where one page ends
and the next begins.

**Scope**

* In: public renderer type/function taking `Document` (+ optional config for
  unit scale and page-gap), full-document `Canvas` sizing, dashed page-break
  lines, a per-page render entry point (one `Document` page onto a `Canvas`),
  spike + decision note on dynamic `Canvas` growth, tests.
* In: `Canvas` demo tab - the `Canvas` in a `ScrollPane` plus a toolbar with a
  control for every renderer config value (unit scale, page-gap, sample
  document) and read-back of the resulting canvas size.
* Out: zoom, scrolling container, sheet visuals, selection, editing.

**Dependencies**

* Requires IP-01 (measurer, draw walk, size helpers).

**Interfaces to Other Plans**

* Consumes IP-01. Provides the per-page render entry point that IP-03 may reuse
  for drawing a single sheet.

### IP-03: Paper Sheet Component

**Objective**

Provide a public JavaFX `Control` that takes a `Document` and renders its pages
as physical-looking sheets - borders and drop shadow - inside a vertically
scrolling, zoomable viewport, with mouse text selection and Ctrl+C copy. This is
the component's readonly mode: selectable text, no caret.

**Scope**

* In: `Control` + skin, readonly mode (no caret, selectable text), configurable
  outer margin and inter-page gap, configurable min/max zoom with a current-zoom
  property, vertical `ScrollBar`/virtualization, sheet chrome, selection model
  over measured geometry, Ctrl+C to system clipboard as plain text, tests.
* In: `Readonly` demo tab - the `Control` in readonly mode plus a toolbar exposing outer margin,
  page gap, min zoom, max zoom, current zoom and sample document, with live
  read-back of current zoom and selection.
* Out: editing, caret, horizontal scrolling, non-vertical layouts, print/PDF.

**Dependencies**

* Requires IP-01 (measurer, draw walk, size helpers, hit-testing). Optionally
  reuses the IP-02 per-page render entry point for drawing a single sheet.

**Interfaces to Other Plans**

* Provides to IP-04 and IP-05: the `Control` class, its skin, the selection
  model and the measured-position <-> `Document`-position mapping. IP-04 extends
  it with editing, IP-05 with CSS-styleable properties.

### IP-04: Editing And Key Commands

**Objective**

Add the normal (editable) mode to the paper component: a mode property toggling
readonly/normal, a blinking caret, character insert/delete, clipboard paste, and
the standard caret-navigation key bindings, emitting an updated `Document`.
Readonly stays exactly the IP-03 behaviour.

**Scope**

* In: mode property (readonly/normal), caret model and rendering, key handling for
  `Home`/`End`/`Ctrl+Home`/`Ctrl+End`/arrows/`Backspace`/`Delete`, Ctrl+V
  paste, re-measure on change, `Document` change output/event, tests.
* In: decision on how a `Document` is mutated/rebuilt (record if `engine`
  support is missing).
* In: `Read/Write` demo tab - the `Control` in normal mode plus a toolbar
  exposing the mode property and every value from the `Readonly` toolbar, plus
  live read-back of caret position and the current `Document` state.
* Out: undo/redo, rich-text editing UI, styling toolbar, collaborative editing.

**Dependencies**

* Requires IP-03 (component, skin, selection model, position mapping).

**Interfaces to Other Plans**

* Consumes IP-03 and IP-01. Provides nothing to later plans in this feature.

### IP-05: Paper Component Styling

**Objective**

Make the paper sheet `Control` fully styleable the standard JavaFX way, so
sheet chrome, gaps, background and selection colours can be set from a
stylesheet without touching Kotlin.

**Scope**

* In: `StyleableProperty`/`CssMetaData` for the visual values (sheet border,
  shadow, sheet background, page gap, outer margin, selection colour, page-break
  line), a default stylesheet returned by `getUserAgentStylesheet`, a style
  class and relevant pseudo-classes, skin reacting to style changes, tests.
* In: a stylesheet switcher in the `Readonly`/`Read/Write` demo toolbars.
* Out: styling the `Canvas` renderer, theming API beyond JavaFX CSS, FXML.

**Dependencies**

* Requires IP-03 (the `Control` and its skin).

**Interfaces to Other Plans**

* Consumes IP-03. May overlap the IP-04 skin; the two coordinate on the skin
  class.

### IP-06: Documentation

**Objective**

Write the `fx` module documentation under `docs/docs/fx/`: how to use the
renderer directly on a `Canvas`, how to use the paper sheet component, and how
to style it.

**Scope**

* In: a canvas-rendering page (full-document and single-page rendering, config
  values, `Canvas` sizing, dynamic-growth outcome, unit scaling); a paper-sheet
  usage page (all properties and settings, readonly/normal mode, integration
  into a scene, loading and saving a `Document`, selection/clipboard) with a
  table of supported shortcuts; a paper-sheet styling page (style class,
  `-fx-` properties, pseudo-classes, default stylesheet, override example);
  `mkdocs.yml` navigation entries.
* In: keeping the pages consistent with the `project-docs` skill.
* Out: KDoc (each production plan handles its own), demo documentation,
  screenshots pipeline.

**Dependencies**

* Requires IP-02, IP-03, IP-04 and IP-05 - it documents their final surface.

**Interfaces to Other Plans**

* Consumes the public API of IP-02 through IP-05. Provides nothing back.
  Terminal plan.

## 8. Dependency Graph

```text
IP-01 ✅
├── IP-02 ─────────────┐
└── IP-03              │
    ├── IP-04 ─────────┤
    └── IP-05 ─────────┤
                       └── IP-06
```

## 9. Risks and Open Questions

* Whether a `Canvas` can grow dynamically after first layout, or must be sized
  up front from `documentSize` (IP-02 spike).
* Very tall documents may exceed the practical `Canvas` pixel limit; whether
  IP-02 needs tiling or an explicit size cap.
* `engine` raw model may be immutable with no edit API; IP-04 must define how an
  edited `Document` is produced and may need user approval for an `engine`
  change.
* Character-offset hit-testing accuracy depends on measuring sub-word glyph
  runs; cost and caching strategy for `FxFontMeasureCalculator`.
* Selection across blocks/pages and its plain-text serialization order.
* Whether the paper `Control` should virtualize pages for large documents or
  render all sheets eagerly.
* Which visual values become CSS-styleable vs. stay plain properties, and how
  IP-04 and IP-05 share the single skin class without conflicts.
* TestFX headless setup on the CI runners (Monocle) - confirm with the
  `ci-pipeline` skill.
* Wiring a `src/demo` source set with the JavaFX Gradle plugin and a `run` task,
  and keeping it out of the published jar, licensee scan and CI `build`.

## 10. Feature Completion Criteria

* `./gradlew :fx:build` is green, including headless UI tests; only the `fx`
  module changed.
* No public signature in `fx` exposes a `Measured*` type.
* `./gradlew :fx:run` starts the demo with the three tabs `Canvas`, `Readonly`,
  `Read/Write`, each showing its component and a toolbar for every externally
  settable value.
* The demo source set is not part of the published `simplay-fx` artifact.
* Given only a `Document`, the canvas renderer yields one `Canvas` showing every
  page with dashed page-break lines, correctly sized.
* Given only a `Document`, the paper component shows sheets with edges and
  shadow, scrolls vertically, zooms within the configured min/max, and its outer
  margin and page gap are configurable.
* Text in the paper component can be selected with the mouse and copied with
  Ctrl+C.
* In readonly mode the component shows no caret; switching to normal mode adds
  the caret and editing, and readonly then behaves exactly as after IP-03.
* With editing enabled, the component accepts typing, Ctrl+V, and
  `Home`/`End`/`Ctrl+Home`/`Ctrl+End`/arrow navigation, and reports an updated
  `Document`.
* The paper component's sheet chrome, gaps and selection colour can be
  restyled purely through a JavaFX stylesheet, with a working default user-agent
  stylesheet.
* The canvas renderer can draw a single selected `Document` page, and the paper
  component reuses that per-page path.
* `docs/docs/fx/` has three pages - direct canvas rendering (incl. single-page),
  paper-sheet usage with a shortcut table, paper-sheet styling - linked in
  `mkdocs.yml`.
* Each implementation plan ships with its own tests; all are green.
