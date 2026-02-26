# Fluent Charm API Improvement Plan

## Context

The current Charm closure DSL (`plot(data) { mapping { x = col.month; y = col.sales } ... }`) is
visually elegant, but hard to use in practice because:

1. **Layer params (`size`, `alpha`, `color`, etc.) are invisible to IDEs** — `LayerDsl` captures them
   via `propertyMissing`, so there is zero autocomplete inside `points { }`, `line { }` etc.
2. **Nested sub-closures have the same problem** — inside `theme { legend { position = 'top' } }`,
   neither `legend` nor `position` appear in IDE autocomplete because the delegates are
   rehydrated at runtime.
3. **No typed builder alternative** — there is no fluent method-chain style for users who prefer
   that over closures.

Note: plain string column names (`x = 'month'`) already work inside `mapping {}` —
`Mapping.coerceToColumnExpr()` already handles `CharSequence`. The `col` proxy is optional.

`MappingDsl` already declares all aesthetic fields as explicit typed properties (`x`, `y`,
`color`, `fill`, `size`, `shape`, `group`, `xend`, `yend`, `xmin`, `xmax`, `ymin`, `ymax`,
`alpha`, `linetype`, `label`, `weight`) and handles `colour` as an alias. This means the
closure form `mapping { x = 'month'; y = 'sales' }` is already fully IDE-navigable — the IDE
can autocomplete every aesthetic name — as long as `@DelegatesTo(MappingDsl)` is correctly
declared on `PlotSpec.mapping(Closure)`. The map shorthand `mapping x: 'month', y: 'sales'`
offers **no discoverability advantage** over the closure and should not be the canonical form.

**Long-term goal:** One canonical API. The builder objects (`geomPoint()`, `geomLine()`, etc.)
become the canonical model; the closure DSL becomes convenience syntax that delegates to them.

**Design principle — two closure categories:**

| Category | Examples | Mechanism |
|---|---|---|
| Single-object configuration | `mapping {}`, `labels {}`, `theme {}`, `scale {}`, `coord {}`, `facet {}` | Flat typed setters on the delegate |
| Collection | `layers {}`, `annotate {}` | Factory methods that register items into a list |

**Rule:** Use a flat-setter delegate when configuring a single object; use factory methods that
register items when populating a collection.

Note: `labels {}` configures one `LabelsSpec` object with multiple label fields (title,
subtitle, x-axis label, y-axis label, caption) — the plural name is appropriate because
it sets several labels at once. Static positioned text belongs in
`annotate { text { … } }`, not in `labels {}`. Data-mapped text (one mark per data row)
belongs in `layers { geomText() }` as a proper geom.

Legend control is split across two closures:
- `theme {}` — visual styling and position of the legend box (`legendPosition`, `legendDirection`,
  `legendBackground`, `legendKeySize`, `legendTitle`, `legendText`, `legendMargin`).
- `guides {}` — which guide type each aesthetic gets (`color = legend()`, `fill = colorbar()`,
  `size = none()`) and per-aesthetic legend titles (`color = legend(title: 'Category')`).

`guides {}` is a single-object configuration (one `GuidesDsl` object) even though its setters
are keyed by aesthetic name, so it belongs in the flat-setter category.

---

## Non-goals

This plan does **not** change:
- The rendering pipeline (`CharmRenderer` and its internal geometry/stat/position pipeline)
- Export API (`ChartExport`, `GgExport`, PNG/JPEG/Swing/JavaFX exporters)
- Coordinate systems (`CoordDsl`, `CoordSpec`, cartesian/polar transforms)
- Statistical transformations (stat implementations: `BinStat`, `DensityStat`, etc.)
- Faceting (`FacetDsl`, `FacetSpec`)
- The `matrix-ggplot` public API (`ggplot() + geom_*()` syntax) — only its internal bridge
  (`GgCharmCompiler`) is updated to use builders

---

## Backward compatibility

**Important context:** The charm API has not been published yet — there are no external
users. The phased approach exists to make incremental development and testing possible,
not to maintain backward compatibility with real users. This means:

- **No deprecated methods in the first release.** Where this plan previously suggested
  deprecation windows (e.g. old closure layer methods), those methods should simply be
  removed before the first public release. The transition period is internal only.
- **`Object` types used for flexibility during development** (e.g. `shape` and `linetype`
  in Phase 2, which accept both strings and integers) are pragmatic during the transition
  but should be tightened to specific types before release if possible.
- **`PlotSpec.layer(CharmGeomType, Map)`** — kept during transition for `CharmBridge`;
  removed (not deprecated) before first release once `CharmBridge` is migrated.

**Impact on dependent modules:**
1. **`matrix-ggplot` users:** Unaffected — the public `ggplot() + geom_*()` API does not
   change; only `GgCharmCompiler` internals are migrated.
2. **`matrix-charts` high-level API users:** `CharmBridge` is migrated in Phase 12 — users
   of `AreaChart`, `BarChart`, `LineChart`, etc. are unaffected because `CharmBridge` is
   internal.

---

## Scope clarification: DSL closures not addressed by this plan

The following closures already have explicit typed fields or methods and do **not** need
the same `propertyMissing` → typed-field treatment addressed in Phases 1–3:

| Closure      | Status | Notes |
|--------------|--------|-------|
| `scale {}`   | OK     | `ScaleDsl` has explicit methods (`log10()`, `sqrt()`, etc.) |
| `coord {}`   | OK     | `CoordDsl` has typed setters |
| `facet {}`   | OK     | `FacetDsl` has typed setters |
| `guides {}`  | OK     | `GuidesDsl` has typed setters keyed by aesthetic name |

If any of these are later found to have IDE blind spots (e.g. `propertyMissing` usage),
add a task to the relevant phase.

**`annotate {}` and its sub-closures** (`text {}`, `rect {}`, `segment {}`) use
`AnnotationDsl` with nested closures. Phase 3 task 3.4 audits these for IDE blind spots
and adds explicit typed fields where needed.

---

## `Charts.chart()` / `Charts.plot()` aliases

`Charts.groovy` provides `chart()` as an alias for `plot()`. Both entry points should
return a chainable `PlotSpec`. This plan does not change the alias relationship — both
remain available.

---

## `PlotSpec.layer(CharmGeomType, Map)` — existing programmatic API

`PlotSpec.layer(CharmGeomType, Map)` is the current generic programmatic API for adding
layers. `CharmBridge` uses it (e.g., `spec.layer(geom, [position: position])`).

**Fate:** This method is kept during Phases 4–11 for internal use by `CharmBridge`. In
Phase 12, `CharmBridge` is migrated to use builders, after which `layer(CharmGeomType, Map)`
is removed (no deprecation needed — the charm API has not been published yet).

---

## `col` proxy deprecation

Task 1.6 removes `col.` usage from all internal tests and examples, but the `Cols` proxy
class itself (`MappingDsl.col` field) is not deprecated or removed in this plan. It remains
available for users who prefer it, though plain strings are the canonical form. A future plan
may deprecate `Cols` if adoption of plain strings is universal.

---

## Build lifecycle: `plot()` → `build()` → `render()`

For readers unfamiliar with the Charm lifecycle:

1. **`plot(data) { ... }`** — creates a mutable `PlotSpec` and configures it via closures
2. **`.build()`** — finalizes the spec into an immutable `Chart` object
3. **`chart.render()`** — renders the `Chart` to an SVG (`se.alipsa.groovy.svg.Svg`)
4. **Export** — `SvgWriter.toXml(svg)` for XML string, or use `ChartExport` for
   PNG/JPEG/Swing/JavaFX

All code examples in this document show `.build()` as the terminal operation. Rendering
and export are separate steps not covered by this plan.

---

## Current API (reference)

```groovy
import static se.alipsa.matrix.charm.Charts.plot

def chart = plot(data) {
  mapping {
    x = col.month      // col proxy is optional — x = 'month' already works
    y = col.sales
    color = col.class
  }
  points {
    size = 3           // propertyMissing — invisible to IDE
    alpha = 0.7
  }
  smooth {
    method = 'lm'      // propertyMissing — invisible to IDE
  }
  labels {             // one LabelsSpec — sets title, subtitle, axes, caption
    title    = 'Monthly Sales'
    subtitle = 'January – December 2024'   // renders below title
    x        = 'Month'
    y        = 'Sales (units)'
    caption  = 'Source: internal CRM'      // renders at bottom-right in grey
  }
  annotate {           // static positioned text — not data-driven
    text { x = 3; y = 100; label = 'Peak sales' }
    text { x = 9; y = 30; label = 'Seasonal low' }
  }
  theme {
    legend { position = 'top' } // nested closure, no autocomplete
    axis { lineWidth = 0.75 }
  }
  scale {
    x = log10()        // OK — explicit method in ScaleDsl
    y = sqrt()
  }
  // Also available but not shown here: coord {}, facet {}
  // guides {} shown in the Approach A example below
}.build()
```

Key source files:
- Entry point: `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Charts.groovy`
- Mutable spec: `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`
- Mapping DSL: `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/MappingDsl.groovy`
- Layer DSL: `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/LayerDsl.groovy`
- Inner DSL classes (ThemeDsl, ScaleDsl, FacetDsl, etc.): nested inside `PlotSpec.groovy`
- Model: `Chart.groovy`, `LayerSpec.groovy`, `Mapping.groovy`, `Cols.groovy`

**Dependent modules** — both use charm as their rendering back-end and must remain fully
functional throughout all phases:
- `se.alipsa.matrix.charts` (same module): `CharmBridge.groovy` translates the high-level
  `AreaChart`, `BarChart`, `LineChart`, etc. API into charm `PlotSpec` calls.
- `matrix-ggplot`: `GgCharmCompiler` translates the ggplot DSL (`ggplot() + geom_point()` etc.)
  into charm `LayerSpec` / `PlotSpec` calls.

Test commands:
- `./gradlew :matrix-charts:test -Pheadless=true` — covers charm internals **and** `se.alipsa.matrix.charts`
- `./gradlew :matrix-ggplot:test -Pheadless=true` — covers the ggplot module

---

## Approach A — Fix IDE Support Within the Existing Closure DSL

Keep the same syntax but eliminate the IDE-blind spots.

**Changes:**
1. `MappingDsl` — verify `@DelegatesTo(MappingDsl)` on `PlotSpec.mapping(Closure)` so IDEs
   resolve the already-declared fields. Change field types from `Object` to `String` (column
   names are always strings; `coerceToColumnExpr()` handles conversion downstream). Add `colour`
   as a real declared field (alias for `color`) instead of relying on `propertyMissing`.
   Drop the `col` proxy from all examples — plain strings are the canonical form.
2. `LayerDsl` — add explicit typed property declarations for common layer parameters. Use `Number`
   for `size` and `alpha`; `String` for `color` and `fill`; `Object` for `shape` and `linetype`
   (they accept both strings and integers in current usage). Keep `propertyMissing` as a fallback
   for geom-specific params not covered by the explicit fields.
3. `PlotSpec.ThemeDsl` (inner class) — add single-level property setters (`legendPosition`,
   `axisLineWidth`, `axisColor`, `gridColor`, `baseSize`, `baseFamily`) and remove the
   existing nested closures (`legend {}`, `axis {}`, `text {}`, `grid {}`).

**Result:**
```groovy
plot(data) {
  mapping {
    x = 'month'        // IDE autocompletes: x, y, color, fill, size, shape, group,
    y = 'sales'        //   xend, yend, xmin, xmax, ymin, ymax, alpha, linetype, label, weight
    color = 'category' // 'colour' also accepted as alias
  }
  points {
    size = 3           // IDE autocompletes: size, alpha, color, fill, shape, linetype
    alpha = 0.7
    color = '#3366cc'
  }
  smooth {
    method = 'lm'      // still via propertyMissing — geom-specific param
  }
  labels {                   // one LabelsSpec — sets title, subtitle, axes, caption
    title    = 'Monthly Sales'
    subtitle = 'January – December 2024'   // top, below title
    x        = 'Month'
    y        = 'Sales (units)'
    caption  = 'Source: internal CRM'      // bottom-right, grey
  }
  annotate {
    text { x = 3; y = 100; label = 'Peak sales' }
    text { x = 9; y = 30; label = 'Seasonal low' }
  }
  guides {
    // controls which guide type each aesthetic gets and per-aesthetic legend title
    color = legend(title: 'Category')      // discrete legend (default)
    size  = none()                         // suppress size legend
  }
  theme {
    legendPosition = 'top'   // new: flat setter — no nested closure needed
    axisLineWidth = 0.75
  }
  scale {
    x = log10()        // unchanged
  }
}.build()
```

Three distinct label/text concepts require three distinct mechanisms:

| Concept                       | Example use                              | Mechanism                                                        |
|-------------------------------|------------------------------------------|------------------------------------------------------------------|
| Structural chart labels       | title, subtitle, axis labels, caption    | `labels {}` — flat typed setters on `LabelsSpec`                 |
| Data-mapped text marks        | country name next to each scatter point  | `layers { geomText() }` — proper GoG geom, one mark per data row |
| Static positioned annotations | "outlier" written at coordinates (5, 10) | `annotate { text { x=5; y=10; label='outlier' } }`               |

`labels {}` is plural because it configures multiple label fields (title, subtitle, x-axis,
y-axis, caption) on a single `LabelsSpec` object.

**`LabelsSpec` fields and their rendering positions:**

| Field      | Rendered where                                              |
|------------|-------------------------------------------------------------|
| `title`    | Top, above the plot panel                                   |
| `subtitle` | Top, 4 px below the title                                   |
| `x`        | Below the x-axis                                            |
| `y`        | Left of the y-axis (rotated)                                |
| `caption`  | Bottom-right, grey — use for source citations and footnotes |

There is no mechanism to move `subtitle` to the bottom; use `caption` for bottom text.
`caption` is always bottom-right; `subtitle` is always top (below title).

---

## Approach B — Typed Geom Builder Objects

Introduce a `Geoms` factory class with dedicated builder objects for each geom type.
Factory method names use a `geom` prefix (`geomPoint()`, `geomLine()`, etc.) to avoid name
collisions with the existing `PlotSpec` closure methods (`points()`, `line()`, etc.) when
builders are called inside the `plot { }` closure.

`PlotSpec` gets a `layers(@DelegatesTo(LayersDsl) Closure)` method that collects builders via
`LayersDsl`; a new public `addLayer(LayerBuilder)` overload is added as a programmatic escape
hatch (the existing `addLayer()` is private).

**New package structure:**
```
se/alipsa/matrix/charm/geom/
  Geoms.groovy          — static factories: geomPoint(), geomLine(), geomSmooth(), ...
  LayersDsl.groovy      — delegate for layers {} closure; factory methods register-and-return builders
  LayerBuilder.groovy   — abstract base: mapping(), inheritMapping(), position(), build()
  PointBuilder.groovy   — size(), alpha(), color(), fill(), shape(), linetype()
  LineBuilder.groovy    — color(), alpha(), linetype(), size()
  SmoothBuilder.groovy  — method(), se(), level(), span(), color(), fill()
  AreaBuilder.groovy    — fill(), color(), alpha(), linetype()
  BarBuilder.groovy     — width(), fill(), color(), alpha(), position()
  ColBuilder.groovy     — width(), fill(), color(), alpha(), position()
  TileBuilder.groovy    — fill(), color(), width(), height(), alpha()
  PieBuilder.groovy     — fill(), color(), alpha()
  HistogramBuilder.groovy  — bins(), binwidth(), fill(), color(), alpha(), position()
  BoxplotBuilder.groovy — width(), fill(), color(), alpha(), outlierShape(), notch()
  ViolinBuilder.groovy  — fill(), color(), alpha(), linetype(), drawQuantiles()
  ... (all remaining geom types — see phased build-out below)
```

**Usage inside current closure DSL:**
```groovy
import static se.alipsa.matrix.charm.geom.Geoms.*

plot(data) {
  mapping {
    x = 'month'        // IDE autocompletes all aesthetic names
    y = 'sales'
    color = 'category'
  }
  labels {
    title    = 'Monthly Sales'
    subtitle = 'January – December 2024'
    x        = 'Month'
    y        = 'Sales (units)'
    caption  = 'Source: internal CRM'
  }
  layers {
    geomPoint().size(3).alpha(0.7).fill('#3366cc')
    geomSmooth().method('lm').se(false)
  }
  annotate {
    text { x = 3; y = 100; label = 'Peak sales' }
    text { x = 9; y = 30; label = 'Seasonal low' }
  }
}.build()
```

**Usage as pure method chain (preferred long-term canonical style):**
```groovy
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.charm.geom.Geoms.*

plot(data)
  .mapping { x = 'month'; y = 'sales'; color = 'category' }
  .layers {
    geomPoint().size(3).alpha(0.7).fill('#3366cc')
    geomSmooth().method('lm').se(false)
  }
  .labels {
    title    = 'Monthly Sales'
    subtitle = 'January – December 2024'
    x        = 'Month'; y = 'Sales (units)'
    caption  = 'Source: internal CRM'
  }
  .annotate {
    text { x = 3; y = 100; label = 'Peak sales' }
    text { x = 9; y = 30; label = 'Seasonal low' }
  }
  .build()
```

**Canonical API convergence path:**
Once all builders exist, the existing `PlotSpec` closure convenience methods (`points {}`,
`line {}`, etc.) are removed in Phase 12; `layers {}` replaces them as the declarative syntax
for adding geom layers. Builders remain the canonical model layer; `layers {}` is the canonical
closure syntax over them.

---

## API design notes

### `layers` method vs. `layers` field name collision

`PlotSpec` has a `List<LayerSpec> layers` field. Adding a `PlotSpec.layers(Closure)` method
creates a name overlap (method vs. field). Groovy resolves this correctly — `spec.layers`
accesses the list while `spec.layers { ... }` calls the method — but it may confuse readers.
Document this distinction clearly in examples.

### Static import for `Geoms` factory methods

The examples show `import static se.alipsa.matrix.charm.geom.Geoms.*` — users must remember
this import to use `geomPoint()`, etc. at the top level. Inside `layers {}`, the `LayersDsl`
delegate provides its own `geomPoint()` factory methods (Phase 4, task 4.3), so the static
import from `Geoms` is only needed outside of `layers {}` (e.g. for `addLayer(geomPoint())`).
This distinction should be documented clearly.

### Two parallel ways to set layer aesthetics during transition

During Phases 4–11, both the closure form (`points { size = 3 }`) and the builder form
(`layers { geomPoint().size(3) }`) coexist. They produce equivalent `LayerSpec` objects but
via different code paths. Phase 12 removes the closure forms and unifies on builders.

### `label` field name overlap in `MappingDsl`

`MappingDsl` has a field `label` (for the label aesthetic mapping), and `LabelsSpec` has fields
set via `labels {}`. Inside a `plot {}` closure, these are at different nesting levels so there
is no actual collision — `label` inside `mapping {}` refers to the aesthetic, while `labels {}`
at the plot level configures chart labels. Worth noting in user documentation.

---

## Implementation Plan

### Phase 1 — Make mapping {} fully IDE-navigable

**Goal:** The `mapping {}` closure should have complete IDE autocomplete for all aesthetic
names without any proxy object or map keys. `MappingDsl` already has the right fields; this
phase locks in the types, fixes the `@DelegatesTo` annotation, and removes `col.` from all
examples.

**Tasks:**
- 1.1 [x] Verify that `PlotSpec.mapping(Closure)` is annotated
  `@DelegatesTo(value = MappingDsl, strategy = Closure.DELEGATE_ONLY)`. Add or fix the
  annotation if missing — this is what enables IDE resolution of `x`, `y`, `color`, etc.
  Note: all closure methods in `PlotSpec` consistently use `DELEGATE_ONLY`.
- 1.2 [x] Change all `MappingDsl` field types from `Object` to `String`. Column names are
  always strings; `Mapping.coerceToColumnExpr()` handles the conversion downstream. This
  lets the IDE flag non-string values as errors at authoring time.
- 1.3 [x] Replace the `colour` alias in `propertyMissing` with a real declared
  `String colour` field that sets `color`: `void setColour(String v) { color = v }`.
  Remove `propertyMissing` from `MappingDsl` entirely — all valid aesthetics are now
  explicit, and unknown names should produce a compile-time error under `@CompileStatic`.
- 1.4 [x] Add a test to `CharmApiDesignTest` asserting that `x = 'cty'` inside `mapping {}`
  renders identically to the previous `x = col['cty']` form, using the mpg dataset.
- 1.5 [x] Add a test asserting that `colour = 'drv'` (British spelling) produces the same
  chart as `color = 'drv'`.
- 1.6 [x] Update all internal tests and examples to use plain strings, removing `col.` usage.
  Update `matrix-charts/examples/charm/SimpleCharmChart.groovy` likewise.
- 1.7 [x] Verify that `PlotSpec.labels(Closure)` has `@DelegatesTo(LabelsSpec)` annotation
  so IDE autocomplete works for `title`, `subtitle`, `x`, `y`, `caption` inside `labels {}`.
- 1.8 [x] Fix bug in `PlotSpec.area()`: change stat from `IDENTITY` to `ALIGN` to match
  `GEOM_DEFAULT_STATS[AREA]`. This is a one-line fix with no dependencies — fixing it early
  avoids a stat inconsistency window when `AreaBuilder` (Phase 5) uses `ALIGN` but the old
  `area {}` closure still uses `IDENTITY`. Also fixed `AlignStat` to fall back to identity
  behavior for non-numeric (categorical) x-axis data.
- 1.9 [x] Run `./gradlew :matrix-charts:test -Pheadless=true` — all tests green.

**Success criteria:**
- IntelliJ shows all aesthetic names (`x`, `y`, `color`, `fill`, `size`, `shape`, `group`,
  `xend`, `yend`, `xmin`, `xmax`, `ymin`, `ymax`, `alpha`, `linetype`, `label`, `weight`,
  `colour`) in autocomplete inside `mapping {}` (verify manually in IDE).
- Assigning an unknown name inside `mapping {}` is a compile-time error.
- `PlotSpec.area()` uses `ALIGN` stat (bug fix).
- All tests green.

---

### Phase 2 — Typed properties in LayerDsl

**Goal:** Common aesthetic properties are declared explicitly in `LayerDsl` so IDEs can autocomplete them.

**Tasks:**
- 2.1 [x] Add explicit typed fields to `LayerDsl` (note: `shape` and `linetype` use `Object`
  for now because internal code passes both strings and integers — Phase 12 task 12.5
  tightens these to specific types before the first public release):
  ```groovy
  Number size
  Number alpha
  String color
  String fill
  Object shape     // tightened in Phase 12
  Object linetype  // tightened in Phase 12
  ```
- 2.2 [x] Override `values()` in `LayerDsl` to merge these explicit fields into the map from
  `LayerParams.values()`, so all params reach the renderer. Explicit fields take precedence.
  Unrecognised geom-specific params (e.g. `method`, `se`, `bins`) still fall through to
  `propertyMissing`.
- 2.3 [x] Add tests to `CharmApiDesignTest`:
  - `points { size = 3; alpha = 0.7; color = '#ff0000' }` renders correctly.
  - `line { linetype = 'dashed'; color = 'blue' }` renders correctly.
  - `line { linetype = 2 }` (integer linetype) still works.
  - Assigning an unrecognised property (e.g. `method = 'lm'` inside `smooth {}`) still works.
  Tests verified with `./gradlew :matrix-charts:test -Pheadless=true` — 474 tests passed.
- 2.4 [x] Run `./gradlew :matrix-charts:test -Pheadless=true` — all tests green.

**Success criteria:**
- IntelliJ shows `size`, `alpha`, `color`, `fill`, `shape`, `linetype` in autocomplete inside
  any geom closure block (verify manually in IDE).
- Integer and string values for `shape` and `linetype` both work without errors.
- All tests green.

---

### Phase 3 — Flat property setters in PlotSpec.ThemeDsl

**Goal:** Common theme options can be set as single-level properties inside `theme {}`.
Note: `ThemeDsl` is a static inner class of `PlotSpec` (at `PlotSpec.groovy:733`).

**Tasks:**
- 3.1 [x] Add flat setters to `PlotSpec.ThemeDsl` replacing the nested closures.
  Added setters: `legendPosition`, `legendDirection`, `axisLineWidth`, `axisColor`,
  `axisTickLength`, `textColor`, `textSize`, `titleSize`, `gridColor`, `gridLineWidth`,
  `gridMinor`, `baseFamily`, `baseSize`, `baseLineHeight`.
- 3.2 [x] Remove the existing `legend {}`, `axis {}`, `text {}`, `grid {}` nested closures
  and `configureMap()` helper from `ThemeDsl`. Updated 26 occurrences of nested closure usage
  across 8 test files and 1 example file to use flat setters.
- 3.3 [x] Add `testFlatThemeSettersProduceCorrectThemeState` to `CharmApiDesignTest` —
  verifies all 14 flat setters produce correct theme state.
- 3.4 [x] Audited `AnnotationDsl` — sub-closures delegate directly to typed spec classes
  (no `propertyMissing` or `MapDsl`). Added explicit `color`, `fill`, `alpha` fields with
  write-through to `params` on `TextAnnotationSpec`, `RectAnnotationSpec`, and
  `SegmentAnnotationSpec`. Added `colour` alias. Added
  `testAnnotationSpecExplicitFieldsWriteThroughToParams` test.
- 3.5 [x] Run `./gradlew :matrix-charts:test -Pheadless=true` — 476 tests passed.
  Run `./gradlew :matrix-ggplot:test -Pheadless=true` — all tests passed.

**Success criteria:**
- IntelliJ autocompletes `legendPosition`, `axisLineWidth`, `axisColor`, `gridColor`,
  `baseSize`, `baseFamily` inside `theme {}` (verify manually in IDE).
- No nested closures (`legend {}`, `axis {}`, etc.) remain in `ThemeDsl`.
- `annotate {}` sub-closures have explicit typed fields (no `propertyMissing` for common params).
- All tests green.

---

### Phase 4 — LayerBuilder infrastructure + PointBuilder + LayersDsl + PlotSpec.layers()

**Goal:** Establish the `se.alipsa.matrix.charm.geom` package with the abstract base,
the `Geoms` factory, the `LayersDsl` delegate, and `PlotSpec.layers()`. Include `PointBuilder`
as the first concrete builder so the smoke test uses real working code. Keep
`PlotSpec.addLayer(LayerBuilder)` as a programmatic escape hatch.

**Tasks:**
- 4.1 [x] Create `se/alipsa/matrix/charm/geom/LayerBuilder.groovy` — `@CompileStatic` abstract
  base with `layerMapping`, `inheritMapping`, `positionSpec`, `params`, fluent `mapping()`,
  `inheritMapping()`, `position()`, and `build()` producing `LayerSpec`.
- 4.2 [x] Create `se/alipsa/matrix/charm/geom/PointBuilder.groovy` — concrete builder for
  `POINT / IDENTITY` with fluent setters: `size`, `alpha`, `color`, `colour`, `fill`, `shape`,
  `linetype`. All return `this`.
- 4.3 [x] Create `se/alipsa/matrix/charm/geom/LayersDsl.groovy` — DSL delegate with
  `geomPoint()` factory that registers and returns the builder.
- 4.4 [x] Create `se/alipsa/matrix/charm/geom/Geoms.groovy` — static factory with
  `geomPoint()`. TODO comments for future builder factories (Phases 5–11).
- 4.5 [x] Add `PlotSpec.layers(@DelegatesTo(LayersDsl) Closure)` and public
  `PlotSpec.addLayer(LayerBuilder)` overload. The private `addLayer(geom, stat, closure)`
  method is unchanged.
- 4.6 [x] Add 4 tests to `CharmApiDesignTest`:
  - `testLayersDslClosureStyleRendersPointElements` — closure `layers {}` style
  - `testLayersDslPureChainStyleRendersPointElements` — `.layers {}` chain style
  - `testAddLayerEscapeHatchRendersPointElements` — `addLayer Geoms.geomPoint()` escape hatch
  - `testLayerBuilderMappingAndPositionWork` — verifies mapping, inheritMapping, position
- 4.7 [x] Run `./gradlew :matrix-charts:test -Pheadless=true` — 480 tests passed.
  Run `./gradlew :matrix-ggplot:test -Pheadless=true` — all tests passed.

**Success criteria:**
- `LayerBuilder`, `PointBuilder`, `LayersDsl`, `Geoms.geomPoint()`, `PlotSpec.layers()`, and
  `PlotSpec.addLayer()` compile `@CompileStatic` without errors.
- All three usage styles (closure `layers {}`, pure chain `.layers {}`, escape hatch `addLayer`)
  demonstrated by passing tests.
- All tests green.

---

### Phase 5 — Core line/area builders: LineBuilder, SmoothBuilder, AreaBuilder, RibbonBuilder

**Goal:** The four most common geometry builders alongside `PointBuilder` are all available.

**Tasks:**
- 5.1 [ ] Implement `LineBuilder`: `color(String)`, `alpha(Number)`, `linetype(Object)`,
  `size(Number)`. `build()` → `LINE / IDENTITY`.
- 5.2 [ ] Implement `SmoothBuilder`: `method(String)`, `se(boolean)`, `level(Number)`,
  `span(Number)`, `color(String)`, `fill(String)`, `alpha(Number)`. `build()` → `SMOOTH / SMOOTH`.
- 5.3 [ ] Implement `AreaBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`,
  `linetype(Object)`. `build()` → `AREA / ALIGN`.
- 5.4 [ ] Implement `RibbonBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`,
  `linetype(Object)`. `build()` → `RIBBON / IDENTITY`.
- 5.5 [ ] Add `Geoms.geomLine()`, `geomSmooth()`, `geomArea()`, `geomRibbon()` factory methods.
- 5.6 [ ] Add tests — one render test per builder using both closure and chain styles.
- 5.7 [ ] Run `./gradlew :matrix-charts:test :matrix-ggplot:test -Pheadless=true` — all tests green.

**Success criteria:**
- All four builders compile `@CompileStatic`.
- Each builder has a passing render test.
- `AreaBuilder` uses `AREA / ALIGN`, consistent with `GEOM_DEFAULT_STATS` and `PlotSpec.area()`.

---

### Phase 6 — Bar/stat builders: BarBuilder, ColBuilder, HistogramBuilder, BoxplotBuilder, ViolinBuilder, DotplotBuilder

**Goals:** Builders for the main statistical-summary geoms.

**Tasks:**
- 6.1 [ ] Implement `BarBuilder`: `width(Number)`, `fill(String)`, `color(String)`,
  `alpha(Number)`, `position(Object)`. `build()` → `BAR / COUNT`.
- 6.2 [ ] Implement `ColBuilder`: same params as `BarBuilder`. `build()` → `COL / IDENTITY`.
- 6.3 [ ] Implement `HistogramBuilder`: `bins(Integer)`, `binwidth(Number)`, `fill(String)`,
  `color(String)`, `alpha(Number)`, `position(Object)`. `build()` → `HISTOGRAM / BIN`.
- 6.4 [ ] Implement `BoxplotBuilder`: `width(Number)`, `fill(String)`, `color(String)`,
  `alpha(Number)`, `outlierShape(Object)`, `notch(boolean)`. `build()` → `BOXPLOT / BOXPLOT`.
- 6.5 [ ] Implement `ViolinBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`,
  `linetype(Object)`, `drawQuantiles(List<Number>)`. `build()` → `VIOLIN / YDENSITY`.
- 6.6 [ ] Implement `DotplotBuilder`: `fill(String)`, `color(String)`, `binwidth(Number)`,
  `stackratio(Number)`, `dotsize(Number)`. `build()` → `DOTPLOT / BIN`.
- 6.7 [ ] Add `Geoms.geomBar()`, `geomCol()`, `geomHistogram()`, `geomBoxplot()`,
  `geomViolin()`, `geomDotplot()` factory methods.
- 6.8 [ ] Add tests — one render test per builder.
- 6.9 [ ] Run `./gradlew :matrix-charts:test -Pheadless=true` — all tests green.

**Success criteria:**
- All six builders compile `@CompileStatic`.
- Position-aware builders (`BarBuilder`, `HistogramBuilder`) correctly produce stacked and
  dodged variants when `position('stack')` / `position('dodge')` is used.

---

### Phase 7 — Tile/fill builders: TileBuilder, PieBuilder, RectBuilder, HexBuilder, Bin2dBuilder, RasterBuilder

**Tasks:**
- 7.1 [ ] Implement `TileBuilder`: `fill(String)`, `color(String)`, `width(Number)`,
  `height(Number)`, `alpha(Number)`. `build()` → `TILE / IDENTITY`.
- 7.2 [ ] Implement `PieBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`.
  `build()` → `PIE / IDENTITY`.
- 7.3 [ ] Implement `RectBuilder`: `xmin(Number)`, `xmax(Number)`, `ymin(Number)`,
  `ymax(Number)`, `fill(String)`, `color(String)`, `alpha(Number)`. `build()` → `RECT / IDENTITY`.
- 7.4 [ ] Implement `HexBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`,
  `bins(Integer)`. `build()` → `HEX / BINHEX`.
- 7.5 [ ] Implement `Bin2dBuilder`: `fill(String)`, `color(String)`, `bins(Object)`.
  `build()` → `BIN2D / BIN2D`.
- 7.6 [ ] Implement `RasterBuilder`: `fill(String)`, `alpha(Number)`, `interpolate(boolean)`.
  `build()` → `RASTER / IDENTITY`.
- 7.7 [ ] Add corresponding factory methods to `Geoms`.
- 7.8 [ ] Add tests and run `./gradlew :matrix-charts:test -Pheadless=true`.

**Success criteria:** All six builders compile and each has a passing render test.

---

### Phase 8 — Annotation builders: TextBuilder, LabelBuilder, SegmentBuilder, CurveBuilder, AblineBuilder, HlineBuilder, VlineBuilder

**Tasks:**
- 8.1 [ ] Implement `TextBuilder`: `size(Number)`, `color(String)`, `angle(Number)`,
  `family(String)`, `fontface(Object)`, `hjust(Number)`, `vjust(Number)`.
  `build()` → `TEXT / IDENTITY`.
- 8.2 [ ] Implement `LabelBuilder`: same params as `TextBuilder` plus `fill(String)`.
  `build()` → `LABEL / IDENTITY`.
- 8.3 [ ] Implement `SegmentBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `arrow(Object)`. `build()` → `SEGMENT / IDENTITY`.
- 8.4 [ ] Implement `CurveBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `curvature(Number)`, `angle(Number)`, `ncp(Integer)`, `arrow(Object)`.
  `build()` → `CURVE / IDENTITY`.
- 8.5 [ ] Implement `AblineBuilder`: `intercept(Number)`, `slope(Number)`, `color(String)`,
  `size(Number)`, `linetype(Object)`. `build()` → `ABLINE / IDENTITY`.
- 8.6 [ ] Implement `HlineBuilder`: `yintercept(Number)`, `color(String)`, `size(Number)`,
  `linetype(Object)`. `build()` → `HLINE / IDENTITY`.
- 8.7 [ ] Implement `VlineBuilder`: `xintercept(Number)`, `color(String)`, `size(Number)`,
  `linetype(Object)`. `build()` → `VLINE / IDENTITY`.
- 8.8 [ ] Add corresponding factory methods to `Geoms`.
- 8.9 [ ] Add tests and run `./gradlew :matrix-charts:test -Pheadless=true`.

**Success criteria:** All seven builders compile and each has a passing render test.

---

### Phase 9 — Statistical analysis builders: DensityBuilder, FreqpolyBuilder, QqBuilder, QqLineBuilder, QuantileBuilder, ErrorbarBuilder, CrossbarBuilder, LinerangeBuilder, PointrangeBuilder

**Tasks:**
- 9.1 [ ] Implement `DensityBuilder`: `fill(String)`, `color(String)`, `alpha(Number)`,
  `linetype(Object)`, `adjust(Number)`, `kernel(String)`. `build()` → `DENSITY / DENSITY`.
- 9.2 [ ] Implement `FreqpolyBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `bins(Integer)`, `binwidth(Number)`. `build()` → `FREQPOLY / BIN`.
- 9.3 [ ] Implement `QqBuilder`: `color(String)`, `size(Number)`, `shape(Object)`,
  `alpha(Number)`. `build()` → `QQ / QQ`.
- 9.4 [ ] Implement `QqLineBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`.
  `build()` → `QQ_LINE / QQ_LINE`.
- 9.5 [ ] Implement `QuantileBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `quantiles(List<Number>)`. `build()` → `QUANTILE / QUANTILE`.
- 9.6 [ ] Implement `ErrorbarBuilder`: `width(Number)`, `color(String)`, `size(Number)`,
  `linetype(Object)`. `build()` → `ERRORBAR / IDENTITY`.
- 9.7 [ ] Implement `CrossbarBuilder`: `width(Number)`, `fill(String)`, `color(String)`,
  `fatten(Number)`. `build()` → `CROSSBAR / IDENTITY`.
- 9.8 [ ] Implement `LinerangeBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`.
  `build()` → `LINERANGE / IDENTITY`.
- 9.9 [ ] Implement `PointrangeBuilder`: `color(String)`, `size(Number)`, `shape(Object)`,
  `fill(String)`, `fatten(Number)`. `build()` → `POINTRANGE / IDENTITY`.
- 9.10 [ ] Add corresponding factory methods and tests.
- 9.11 [ ] Run `./gradlew :matrix-charts:test -Pheadless=true`.

**Success criteria:** All nine builders compile and each has a passing render test.

---

### Phase 10 — Path/step/jitter builders: PathBuilder, StepBuilder, JitterBuilder, RugBuilder, CountBuilder, ContourBuilder, FunctionBuilder

**Tasks:**
- 10.1 [ ] Implement `PathBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `alpha(Number)`, `lineend(String)`, `linejoin(String)`. `build()` → `PATH / IDENTITY`.
- 10.2 [ ] Implement `StepBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `direction(String)`. `build()` → `STEP / IDENTITY`.
- 10.3 [ ] Implement `JitterBuilder`: `width(Number)`, `height(Number)`, `color(String)`,
  `size(Number)`, `shape(Object)`, `alpha(Number)`. `build()` → `JITTER / IDENTITY`.
- 10.4 [ ] Implement `RugBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `sides(String)`, `outside(boolean)`. `build()` → `RUG / IDENTITY`.
- 10.5 [ ] Implement `CountBuilder`: `color(String)`, `fill(String)`, `size(Number)`.
  `build()` → `COUNT / COUNT`.
- 10.6 [ ] Implement `ContourBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `bins(Integer)`, `binwidth(Number)`. `build()` → `CONTOUR / CONTOUR`.
- 10.7 [ ] Implement `FunctionBuilder`: `color(String)`, `size(Number)`, `linetype(Object)`,
  `n(Integer)`. `build()` → `FUNCTION / FUNCTION`.
- 10.8 [ ] Add corresponding factory methods and tests.
- 10.9 [ ] Run `./gradlew :matrix-charts:test -Pheadless=true`.

**Success criteria:** All seven builders compile and each has a passing render test.

---

### Phase 11 — Spatial + remaining builders

Remaining geom types: `SF`, `SF_LABEL`, `SF_TEXT`, `POLYGON`, `MAP`, `PARALLEL`, `SPOKE`,
`MAG`, `DENSITY_2D`, `DENSITY_2D_FILLED`, `CONTOUR_FILLED`, `RASTER_ANN`, `LOGTICKS`,
`BLANK`, `CUSTOM`, `ERRORBARH`, `POINTRANGE` (if not done above).

**Tasks:**
- 11.1 [ ] Implement `SfBuilder`, `SfLabelBuilder`, `SfTextBuilder` for spatial geoms.
- 11.2 [ ] Implement `PolygonBuilder`, `MapBuilder` for map/polygon geoms.
- 11.3 [ ] Implement `Density2dBuilder`, `Density2dFilledBuilder`, `ContourFilledBuilder`
  for 2D stat geoms.
- 11.4 [ ] Implement `SpokeBuilder`, `MagBuilder`, `ParallelBuilder` for specialized geoms.
- 11.5 [ ] Implement `LogticksBuilder`, `BlankBuilder`, `RasterAnnBuilder` for annotation geoms.
- 11.6 [ ] Implement `ErrorbarhBuilder` (horizontal error bars).
- 11.7 [ ] `CustomBuilder` — accepts a `Closure` or `Supplier` for user-defined rendering.
- 11.8 [ ] Add all factory methods to `Geoms` (replacing all remaining `// TODO` stubs).
- 11.9 [ ] Add tests for each builder.
- 11.10 [ ] Run `./gradlew :matrix-charts:test -Pheadless=true`.

**Success criteria:**
- `Geoms` has no remaining `// TODO` factory stubs.
- All builders compile `@CompileStatic`.
- Each has a passing render test.

---

### Phase 12 — Convergence: canonical API wiring, closure method cleanup, docs

**Goal:** Wire closure methods to use builders internally (making builders the canonical model),
remove or replace the old closure-only paths, and update documentation.

**Tasks:**
- 12.1 [ ] Wire existing `PlotSpec` closure convenience methods to delegate to their builder
  counterparts internally. Example: `PlotSpec.points(Closure)` constructs a `PointBuilder`,
  applies the closure params to it, then calls `addLayer(builder)`. This unifies the two
  implementations.
- 12.2 [ ] Migrate `CharmBridge.groovy` to use builders. `CharmBridge` currently calls
  `spec.area()`, `spec.line()`, `spec.points()`, `spec.pie()`, and `spec.layer()` — all of
  which are removed in this phase. Rewrite these calls to use `layers {}` / builders.
  `GgCharmCompiler` constructs `LayerSpec` directly (bypasses PlotSpec) and is unaffected.
- 12.3 [ ] Before removing any `PlotSpec` closure method, grep `CharmBridge.groovy` and
  `GgCharmCompiler` for usages of old closure methods AND direct `addLayer()` calls. Verify
  all have been migrated in 12.2; then remove the old closure methods from `PlotSpec`.
  Update all affected tests.
- 12.4 [ ] Remove `PlotSpec.layer(CharmGeomType, Map)` — the generic programmatic API.
  After `CharmBridge` migration in 12.2, this method has no callers. No deprecation needed
  since the charm API has not been published yet.
- 12.5 [ ] Tighten `Object` types for `shape` and `linetype` in `LayerDsl` and all builder
  classes. With all internal code migrated, audit actual usage to determine the correct
  specific types (e.g. `String` if integers are no longer passed, or a dedicated enum/sealed
  type). Update all builders' `shape(Object)` and `linetype(Object)` signatures accordingly.
- 12.6 [ ] Add an integration test in `CharmApiDesignTest` that builds the same chart two ways
  (closure DSL and pure builder chain) and asserts equivalent structure using direct SVG object
  access (element counts and types), not full XML string comparison.
- 12.7 [ ] Update `matrix-charts/examples/charm/SimpleCharmChart.groovy` to demonstrate
  both styles with a comment indicating the builder chain is the canonical form.
- 12.8 [ ] Update `matrix-charts/docs/charm.md` with a "Builder API" section.
- 12.9 [ ] Run `./gradlew :matrix-charts:test :matrix-ggplot:test -Pheadless=true` — all tests green.

**Success criteria:**
- `layers { geomX() }` is the canonical closure syntax for adding geom layers; old `points {}`,
  `line {}`, etc. closure methods are gone. The public `addLayer(LayerBuilder)` overload (added
  in Phase 4) is retained as the programmatic escape hatch only.
- The integration test asserts equivalent SVG structure for closure-DSL-constructed charts
  that were migrated to the builder chain.
- `CharmBridge` fully migrated to builders — no old closure method calls.
- `GgCharmCompiler` unchanged (already bypasses PlotSpec closure methods).
- `PlotSpec.layer(CharmGeomType, Map)` is removed.
- No `Object` types remain for `shape` or `linetype` in the public API.
- Docs reflect the canonical `layers {}` style.
- `./gradlew :matrix-charts:test :matrix-ggplot:test -Pheadless=true` fully green.
