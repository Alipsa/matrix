# Charm Improvements Plan

Prioritized plan addressing common ggplot2 pain points where Charm can improve or differentiate.

## Scope and guardrails

- Charm enhancements are the primary design target.
- `matrix-charts` and `matrix-ggplot` changes are compatibility work to preserve existing behavior and keep ggplot API semantics close to ggplot2.
- No external backward-compatibility constraints yet (Charm not released), but internal module compatibility must be maintained.

## Locked design decision

- `plotGrid` v1 will use **nested `<svg>` composition per subplot** (not flattened `<g>` merging).
  - Rationale: lower implementation and regression risk, explicit subplot encapsulation, and faster delivery.
  - `PlotGridRenderer` must rewrite/prefix subplot-local SVG IDs (`clipPath`, `mask`, `gradient`, `pattern`, etc.) and corresponding references during composition (`url(#...)`, `href="#..."`, and `xlink:href="#..."`), since SVG IDs are document-global even with nested `<svg>`.
  - A flattened mode can be added later if a specific SVG consumer requires it.
  - Legends remain **per subplot** in v1; no global legend merge in the initial implementation.
- Date/time scales will use **one canonical internal unit: epoch milliseconds**.
  - Applies to `date`, `datetime`, and `time` transform handling inside Charm runtime scale training/ticks/labels.
  - Default normalization uses **UTC/GMT**.
  - For pure `time` scales, canonical values are **milliseconds since midnight UTC** (`00:00:00.000` to `23:59:59.999`), still represented in the same epoch-millis numeric domain.
  - Users can override timezone via a scale param (for example `zoneId: 'Europe/Stockholm'`) for parsing, break generation, and label formatting.
  - For gg compatibility, **do not add new timezone arguments to gg scale APIs** unless required for ggplot2 parity. Instead, gg->Charm delegation sets `zoneId` internally so delegated rendering preserves gg runtime behavior.
  - Axis formatters may still render date-only or time-only labels based on transform mode.

---

## Current status of investigated areas

| Area | Status | Action needed |
|------|--------|---------------|
| Categorical axis ordering | Preserves encounter order (`LinkedHashSet`) | Document clearly (see 1) |
| Axis limits (`xlim`/`ylim`) | Clamp/squish behavior (coord-cartesian-like) | Document clearly (see 1) |
| Date/time axes in Charm core | Basic transforms exist, but date/time label/break ergonomics are incomplete | Implement (see 2) |
| Scale `breaks`/`labels` in Charm core | Model fields exist but are not consistently honored at runtime | Implement (see 2) |
| Legend customization | Strong guide support already | API polish + guide chaining wiring (see 3) |
| Facet label formatting | `Labeller` class exists with instance `label()` methods; static convenience factories (`value()`, `both()`, `label(Closure)`) are absent | Add factories + DSL wiring (see 4) |
| Error messages | Structured exception hierarchy exists | Improve actionability (see 5) |
| Multi-plot layouts | Faceting exists; independent plot composition missing | Add `plotGrid()` (see 6) |
| Per-layer scales | Not supported; global scales only | Add architecture + compatibility glue (see 7) |
| SVG interactivity | CSS classes/IDs only; no tooltips | Add tooltip support (see 8) |
| Large dataset helpers | Stats exist but no explicit sampling stat in Charm DSL | Add stat + DSL support (see 9) |
| Animation helpers | Not implemented | Add optional CSS animation injection (see 10) |

---

## 1. Document existing advantages

**Priority:** Low | **Effort:** Small

### 1.1 [x] Add "Differences from ggplot2" section to `charm.md`

**File:** `matrix-charts/docs/charm.md`

Document and exemplify:
- Categorical axes preserve encounter order (not forced alphabetical).
- `xlim`/`ylim` clamp values to boundaries instead of dropping rows.
- Legend and guide APIs available in Charm core.

### 1.2 [x] Add a "Current limitations" note for date/time ergonomics

**File:** `matrix-charts/docs/charm.md`

Document that:
- Date/time transforms are available.
- Human-friendly date/time break and label controls are being implemented in section 2.

### Verification

Verified in:
- `matrix-charts/docs/charm.md`

---

## 2. Date/time and scale break/label completeness

**Priority:** High | **Effort:** Medium

### 2.0 [x] Normalize temporal values to canonical epoch-millis unit in Charm runtime

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ScaleEngine.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ContinuousCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/BinnedCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ColorCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/TemporalScaleUtil.groovy` (new helper)

Ensure all temporal inputs (including date/time/datetime objects and parseable strings) are normalized to epoch milliseconds before domain training, break generation, and label formatting.
Use UTC/GMT by default, with optional per-scale timezone override via `Scale.params.zoneId`.
For pure `time` transforms, normalize to milliseconds since midnight UTC.
For calendar-unit stepping (`day/week/month/year`), iterate in timezone-aware temporal types (`ZonedDateTime`/`LocalDate`) and convert each generated breakpoint to epoch millis; do not advance calendar intervals via fixed millisecond deltas.

### 2.0.1 [x] Add explicit `datetime` transform support in Charm core

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/ScaleTransform.groovy` (add `DatetimeScaleTransform` + registry key)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Scale.groovy` (add `datetime()` factory)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (`ScaleDsl` convenience for `datetime()`)

### 2.1 [x] Honor configured `Scale.breaks` and `Scale.labels` in runtime scales

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ContinuousCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/BinnedCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/DiscreteCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ColorCharmScale.groovy`

Use configured breaks/labels when present, with sane fallbacks when absent.

### 2.2 [x] Add date/time/datetime-aware axis label formatting in Charm core

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/AxisRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ContinuousCharmScale.groovy`

When transform strategy is `date`, `time`, or `datetime`, format ticks using scale params (for example `dateFormat`/`timeFormat`) and sensible defaults.
Formatting should use UTC/GMT unless a `zoneId` override is provided on the scale.

### 2.3 [x] Add simple string break spec support for date/time/datetime scales

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ContinuousCharmScale.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/TemporalScaleUtil.groovy` (new helper)

Support strings such as:
- `'1 day'`, `'2 weeks'`, `'3 months'`, `'1 year'`
- `'30 seconds'`, `'30 minutes'`, `'1 hour'`
- `'6 hours'` (for datetime ranges)

**Parsing approach:** Parse and resolve string break specs at runtime inside `ContinuousCharmScale` (not at DSL build time, so that the trained domain min/max is already available). Use `java.time.Period` for calendar-unit specs (`day`, `week`, `month`, `year`) and `java.time.Duration` for fixed-duration specs (`second`, `minute`, `hour`). For calendar units, step with temporal arithmetic in the configured/default zone; for fixed units, step with duration arithmetic. Store raw interval specs in `Scale.params` (for example `dateBreaks`/`timeBreaks`), not in `Scale.breaks`. Precedence: explicit `Scale.breaks` list wins; interval specs are fallback when `breaks` is unset.

### 2.4 [x] Audit existing scale-param propagation in bridge, then add date/time/datetime params and explicit mapping

**File:**
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/bridge/GgCharmCompiler.groovy`

The existing propagation mechanism lives in `enrichScale()` (lines ~701–720 of `GgCharmCompiler`). It uses explicit field-by-field conditional copies: `name`, `limits`, `expand`, and `guide` are copied into `charmScale.params`, while `breaks` and `labels` are copied to top-level `charmScale.breaks`/`charmScale.labels` fields (as new `ArrayList` copies). Extend this same `enrichScale()` method rather than creating a parallel propagation path.

Add conditional copies for temporal parameters:
- `dateFormat` / `timeFormat` → `charmScale.params['dateFormat']` / `charmScale.params['timeFormat']`
- `dateBreaks` / `timeBreaks` → `charmScale.params['dateBreaks']` / `charmScale.params['timeBreaks']`
- For gg temporal scales (`ScaleX/YDate`, `ScaleX/YTime`, `ScaleX/YDatetime`), set `charmScale.params['zoneId']` internally to the gg runtime default timezone (for example `ZoneId.systemDefault().id`) so delegated Charm rendering preserves gg behavior while Charm itself keeps UTC as default.

**Compatibility rule:** Keep gg API close to ggplot2 — do **not** add new user-facing timezone parameters to gg scales in this phase unless ggplot2 parity requires it.

Keep explicit gg `breaks` lists mapped to `Scale.breaks`; only interval strings should map to params keys. This ensures the Charm runtime date/time formatting and break logic applies when gg charts delegate to Charm.

Also update gg→Charm scale mapping logic so datetime is explicit (not inferred by class-name substring fallback order):
- `GgCharmMappingRegistry.mapPositionalScale()` should map:
  - `ScaleXDate`/`ScaleYDate` → `CharmScale.date()`
  - `ScaleXTime`/`ScaleYTime` → `CharmScale.time()`
  - `ScaleXDatetime`/`ScaleYDatetime` → `CharmScale.datetime()`
- In `GgCharmCompiler.fallbackScale()`, check `datetime`/`time` before `date` for class-name fallback safety.

### 2.5 [x] Add focused tests for date/time breaks, labels, and gg-bridge propagation

**Files:**
- `matrix-charts/src/test/groovy/charm/render/scale/DateTimeScaleTest.groovy` (new)
- `matrix-charts/src/test/groovy/charm/render/scale/ScaleBreakLabelRuntimeTest.groovy` (new; includes `ContinuousCharmScale`, `BinnedCharmScale`, `DiscreteCharmScale`, and `ColorCharmScale`)
- `matrix-ggplot/src/test/groovy/gg/DateTimeScaleBridgeTest.groovy` (new)

Include assertions for:
- default UTC behavior
- explicit `zoneId` override behavior
- gg->Charm delegation sets `zoneId` for temporal gg scales without introducing new gg timezone API
- configured breaks/labels are honored for discrete and non-discrete runtime scales
- consistent break/label output across environments

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
./gradlew test
```

---

## 3. Legend API polish and guide chaining

**Priority:** Medium | **Effort:** Medium

### 3.0 [x] Audit existing guide factory methods before adding new ones

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/GuideSpec.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (`GuidesDsl`)

Check whether `legend()`, `colorbar()`, `colorsteps()`, and `none()` are already available and reuse/delegate those methods rather than duplicating logic in `ScaleDsl`.

### 3.1 [x] Add `colorBrewer()` and `colorGradient()` convenience methods to `ScaleDsl`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (`ScaleDsl`)

These are thin wrappers around the existing `Scale.brewer(paletteName)` and `Scale.gradient(from, to)` static factories — both confirmed to exist in `Scale.groovy` — making them available inside a `scale { ... }` closure under `DELEGATE_ONLY` without a `Scale.` prefix.

### 3.2 [x] Add guide factory methods to `ScaleDsl` for closure-local chaining

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (`ScaleDsl`)

`GuideSpec` already has static factories `legend()`, `colorbar()`, `colorsteps()`, and `none()` (the same ones that `GuidesDsl` calls). Call those `GuideSpec` static factories directly from `ScaleDsl` — do **not** try to delegate to `GuidesDsl` methods (`GuidesDsl` is a static inner class of `PlotSpec` with no accessible instance from `ScaleDsl`). Expose them in `ScaleDsl` so this works under `DELEGATE_ONLY`:

```groovy
scale {
  fill = colorBrewer('Set1').guide(none())
  color = colorGradient('#fff', '#f00').guide(colorbar())
}
```

### 3.3 [x] Add `guide(Object)` fluent method to `Scale`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Scale.groovy`

Store normalized guide configuration in scale params and return `this`.

### 3.4 [x] Materialize scale-attached guides into `GuidesSpec` during build

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

On `build()`, promote scale-level guide metadata into `guides` so `LegendRenderer` sees it.
Define precedence: explicit `guides {}` entries override scale-attached guide metadata.

### 3.5 [x] Add `legendPosition()` shorthand to `PlotSpec`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

Add a top-level convenience method that forwards to `theme.legendPosition`:

```groovy
PlotSpec legendPosition(Object value) {
  theme.legendPosition = value
  this
}
```

Legend position is already settable via `theme { legendPosition = ... }` (through `ThemeDsl`). This shorthand is a convenience only. Precedence: the last write wins — if both are called, the one called last takes effect. Document this in `charm.md` (see 3.7).

### 3.6 [x] Add tests for legend convenience methods and guide chaining

**File:** `matrix-charts/src/test/groovy/charm/render/LegendConvenienceTest.groovy` (new)

### 3.7 [x] Document legend and guide chaining API in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add examples for `colorBrewer()`/`colorGradient()` + `.guide()` chaining and `legendPosition()` shorthand.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew test -Pheadless=true
```

---

## 4. Facet labeller convenience

**Priority:** Low | **Effort:** Small

### 4.1 [ ] Add built-in labeller factory methods

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/facet/Labeller.groovy`

Add:
- `static Labeller value()` — formats each strip as the facet value only (e.g. `"setosa"`); this matches the current fallback behavior and makes it explicit.
- `static Labeller both()` — formats each strip as `"variable: value"` (e.g. `"Species: setosa"`).
- `static Labeller label(Closure<String> fn)` — wraps an arbitrary labelling closure.

### 4.2 [ ] Add `labeller` support in Charm facet DSL

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (`FacetDsl` / `WrapDsl`)

Accept a `Labeller` instance on both `FacetDsl` and `WrapDsl`. The `Labeller` factories from 4.1 should be exposed in the closure scope via delegation so callers can write `labeller = both()` without an import.

### 4.3 [ ] Carry `labeller` through the compiled spec and into the renderer

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` — persist `labeller` in `facet.params['labeller']` for both wrap and grid DSL paths
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/FacetRenderer.groovy` — continue reading `params['labeller']` and apply it when formatting strip labels

Keep `labeller` in facet params for v1 (no new typed field on `FacetSpec`).

### 4.4 [ ] Add tests for labeller convenience

**File:** `matrix-charts/src/test/groovy/charm/render/FacetLabellerTest.groovy` (new)

### 4.5 [ ] Document labeller convenience in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add examples showing `value()`, `both()`, and custom closure labellers.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## 5. Improve error message actionability

**Priority:** High | **Effort:** Medium

### 5.0 [x] Add explicit `commons-text` dependency for string distance

**File:** `matrix-charts/build.gradle`

Add `org.apache.commons:commons-text` explicitly and use `LevenshteinDistance` directly in validation error suggestions. Do not rely on transitive availability and do not create a custom string-distance utility.

### 5.1 [x] Add "did you mean?" suggestions for unknown columns

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`
- `matrix-charts/build.gradle` (dependency added in 5.0)

### 5.2 [x] Add contextual hints for common semantic mistakes

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

Examples:
- Missing required `y` on point/line geoms.
- `geomBar()` with explicit `y` mapping (recommend `geomCol()`).
- Literal color accidentally placed in mapping instead of layer param.

### 5.3 [x] Add tests for improved error messages

**File:** `matrix-charts/src/test/groovy/charm/core/ErrorMessageTest.groovy` (new)

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew test
```

---

## 6. Multi-plot composition (`plotGrid`)

**Priority:** High | **Effort:** Large

### 6.1 [ ] Create `PlotGrid` specification class

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotGrid.groovy` (new)

Key fields:
- `List<se.alipsa.matrix.charm.Chart> charts = []`
- `int ncol = 1`
- `Integer nrow` (nullable; auto-compute when null)
- `List<BigDecimal> widths` — fractional column weights (e.g. `[1.0, 2.0]` means second column is twice as wide); null means equal widths
- `List<BigDecimal> heights` — fractional row weights; null means equal heights
- `String title`
- `int spacing = 10`

### 6.2 [ ] Create `PlotGridRenderer` using nested `<svg>` composition

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/PlotGridRenderer.groovy` (new)

`RenderConfig` (holding `width` and `height`) is confirmed to exist at `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/RenderConfig.groovy` — use it directly.

`Chart` already provides convenience `render()` methods, but `PlotGridRenderer` should call `CharmRenderer.render(chart, config)` directly to guarantee per-cell render sizing and avoid hidden defaults. Rendering steps:
1. Compute grid cell dimensions from total width/height and relative weights.
2. For each subplot `Chart`, construct a per-cell `RenderConfig(cellW, cellH)` and call `CharmRenderer.render(chart, config)` to obtain the cell `Svg`.
3. Rewrite/prefix all subplot-local SVG IDs and corresponding references (`url(#...)`, `href="#..."`, and `xlink:href="#..."`) with a per-cell prefix (for example `grid-r1c2-...`) before embedding.
4. Embed each cell `Svg` as a nested `<svg x=… y=…>` positioned at its grid slot in the outer canvas.
5. Add optional overall title above the grid.

Legend behavior for v1: render legends independently per subplot. Do not attempt automatic cross-subplot legend merging in the initial implementation.

`PlotGrid.render(int width, int height)` is the public convenience entry point; it constructs a `PlotGridRenderer` and delegates to it.

### 6.3 [ ] Create `PlotGridDsl` and add `plotGrid()` factory to `Charts`

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotGridDsl.groovy` (new)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Charts.groovy`

`PlotGrid` is the immutable compiled spec (holding `List<Chart>`). `PlotGridDsl` is the mutable closure delegate used inside `plotGrid { ... }` blocks — it mirrors the pattern of `PlotSpec` / `ScaleDsl`. Callers must have a compiled `Chart` (from `PlotSpec.build()` or `Charts.plot(...).build()`) before adding it to a grid. `PlotGrid.render(int width, int height)` is a convenience wrapper delegating to `PlotGridRenderer`.

### 6.4 [ ] Add `plot_grid()` compatibility API to ggplot layer

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

Compile each `GgChart` to Charm `Chart`, then delegate to Charm `PlotGrid`.
Requirement for v1: all `GgChart` inputs are Charm-delegable. Any adaptation failure is a bridge bug and `plot_grid()` should fail fast (no fallback rendering path inside `plot_grid()`).

### 6.5 [ ] Keep existing exporters unchanged; use `SvgWriter.toXml()` for SVG output

Use:

```groovy
import se.alipsa.groovy.svg.io.SvgWriter

Svg svg = grid.render(1200, 800)
ChartToPng.export(svg, new File('dashboard.png'))
new File('dashboard.svg').text = SvgWriter.toXml(svg)
```

### 6.6 [ ] Add tests for PlotGrid behavior and isolation

**File:** `matrix-charts/src/test/groovy/charm/render/PlotGridTest.groovy` (new)

Test:
- 2x2 and uneven layouts.
- Relative widths/heights.
- Title rendering.
- Per-subplot legends/clips/defs remain correct.
- No SVG ID collisions across subplots (including clip paths and all reference forms: `url(#...)`, `href="#..."`, and `xlink:href="#..."`).

### 6.7 [ ] Document `plotGrid` and `plot_grid` API in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add a "Multi-plot composition" section with a `plotGrid { }` DSL example and notes on the nested `<svg>` approach.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## 7. Per-layer scale overrides (including positional scales)

**Priority:** Medium | **Effort:** Large

### 7.1 [ ] Add layer-level scale DSL and model storage

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Layer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/LayerSpec.groovy`

Add `scale { ... }` support and persist as `Map<String, Scale>` by aesthetic.

### 7.2 [ ] Add render-context storage for per-layer trained scales

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/RenderContext.groovy`

Add structure such as:
- `Map<Integer, TrainedScales> layerScales`
- helpers for selecting active scales per layer during rendering

### 7.3 [ ] Train global and per-layer scales in `CharmRenderer`/`ScaleEngine`

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ScaleEngine.groovy`

Train overrides for all aesthetics, including `x`/`y`, not only non-positional channels.

### 7.4 [ ] Resolve mapped aesthetics and positional transforms with per-layer-first fallback logic

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy`
- geom renderers that directly use `context.xScale`/`context.yScale`

Resolution order:
1. Style callback/layer literal params
2. Per-layer trained scale (if present)
3. Global trained scale
4. Defaults

### 7.5 [ ] Implement v1 axis behavior when per-layer `x`/`y` scales diverge

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/AxisRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy`

**v1 decision (locked):** Always render a single set of global axes, computed from the global trained `x`/`y` scale domain (i.e., the union of all layers' data ranges). Per-layer positional scales control only how that layer's data points are mapped to pixel coordinates — they do not produce separate axis ticks or labels. Divergence warnings are based on pixel-space, not raw-domain percentages: emit `log.warn()` when a layer’s mapped span or boundary offset differs from the global axis span by more than 10% of the panel span. This keeps warning behavior meaningful across mixed transforms (for example log vs. linear).

**Documentation note:** When per-layer positional scales use divergent transforms (e.g., log on one layer vs. linear on another), the single global axis may be misleading since tick labels reflect the global scale while individual layers are mapped differently. Document this caveat in `charm.md` (section 7.9) with a recommendation to use `plotGrid` (section 6) instead when layers need truly independent axes.

### 7.6 [ ] Render separate legend entries for split scales

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/LegendRenderer.groovy`

### 7.7 [ ] Add gg compatibility path (`new_scale_color()` / `new_scale_fill()`)

**Files:**
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy`
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/bridge/GgCharmCompiler.groovy`

Use marker components to reset scale scope for subsequent layers while keeping API close to ggplot2 semantics.

### 7.8 [ ] Add tests for per-layer scales in Charm and gg compatibility

**Files:**
- `matrix-charts/src/test/groovy/charm/render/scale/PerLayerScaleTest.groovy` (new)
- `matrix-ggplot/src/test/groovy/gg/PerLayerScaleCompatibilityTest.groovy` (new)

### 7.9 [ ] Document per-layer scale limitations and divergent-transform caveat

**File:** `matrix-charts/docs/charm.md`

Add a "Per-layer scales" section documenting:
- How per-layer scale overrides work (including positional `x`/`y`)
- The v1 single-global-axis behavior
- **Caveat:** when per-layer positional scales use divergent transforms (e.g., log vs. linear), the global axis tick labels may not accurately represent all layers' data. Recommend using `plotGrid` (section 6) when layers need truly independent axes.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
./gradlew test
```

---

## 8. SVG tooltip support

**Priority:** High | **Effort:** Medium

### 8.1 [x] Add `tooltip` aesthetic to Charm mapping and pipeline data

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Mapping.groovy` — add `ColumnExpr tooltip` field **and** update all four of:
  - `setTooltip(Object)` / `getTooltip()` property accessors
  - `apply(Map)` — add `case 'tooltip' ->` to the switch statement (missing this means `aes(tooltip: 'col')` throws `CharmMappingException`)
  - `copy()` — copy the `tooltip` field (the method copies each field explicitly)
  - `mappings()` — include `tooltip` in the returned map (method enumerates fields explicitly)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/MappingDsl.groovy` — add a `tooltip` property setter so `mapping { tooltip = 'colName' }` works in closure scope
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/RenderContext.groovy` — add `String tooltip` to `LayerData` inner class (explicit typed field preferred over the existing `meta` map for type safety and clarity)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/LayerDataUtil.groovy` — include `tooltip` in `copyDatum()`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy` — populate `tooltip` in `mapData()`

### 8.2 [x] Add layer tooltip configuration API

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy`

Add:
- `tooltip(String template)`
- `tooltip(boolean enabled)`

Default behavior: tooltip rendering is **off** unless explicitly enabled by layer API or explicit tooltip mapping.

### 8.3 [x] Add tooltip resolver with explicit precedence

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy`

Resolution order:
1. Disabled -> `null`
2. Template
3. Mapped tooltip value
4. Auto-generated text (only when enabled)

### 8.4 [x] Add `addTooltip()` helper to attach `<title>` to SVG elements

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy`

### 8.5 [x] Apply tooltip emission to high-usage renderers first

**Files:**
- `BarRenderer.groovy`
- `PointRenderer.groovy`
- `LineRenderer.groovy`
- `AreaRenderer.groovy`

### 8.6 [x] Add gg compatibility support for tooltip aesthetic

**Files:**
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/aes/Aes.groovy`
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/bridge/GgCharmCompiler.groovy`

### 8.7 [x] Add tests for tooltip rendering and default-off behavior

**File:** `matrix-charts/src/test/groovy/charm/render/geom/TooltipTest.groovy` (new)

### 8.8 [x] Document tooltip API in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add a "Tooltips" section covering: mapping `tooltip`, template strings, `tooltip(true/false)` on a layer, and the SVG `<title>` mechanism.

### 8.9 [x] Address PR review feedback for tooltip bridging and placeholder precedence

**Files:**
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/bridge/GgCharmCompiler.groovy` — include `tooltip` in `PARAM_COLUMN_AESTHETICS` so `geom_* (tooltip: 'col')` layer params can flow into Charm mapping.
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy` — prevent row-map keys from overwriting mapped aesthetic placeholders in tooltip templates.
- `matrix-charts/src/test/groovy/charm/render/geom/TooltipTest.groovy` — add regression test for `{x}`/`{y}` placeholder stability when data contains `x`/`y` columns not used for mapped axes.
- `matrix-ggplot/src/test/groovy/gg/TooltipBridgeTest.groovy` — add regression test for layer-param tooltip column propagation through gg -> Charm bridge.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

Executed on this implementation:
- `./gradlew :matrix-charts:test -Pheadless=true`
- `./gradlew :matrix-ggplot:test -Pheadless=true`
- `./gradlew :matrix-charts:test --tests "charm.render.geom.TooltipTest" -Pheadless=true`
- `./gradlew :matrix-ggplot:test --tests "gg.TooltipBridgeTest" -Pheadless=true`

---

## 9. Large dataset sampling helpers

**Priority:** Medium | **Effort:** Medium

### 9.1 [x] Add explicit layer stat override API in Charm DSL

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy`

Use explicit default-vs-selected naming:

1. Rename abstract `statType()` in `LayerBuilder` to `defaultStatType()`.
2. Add `protected CharmStatType statType` field to `LayerBuilder` to hold a caller-selected stat.
3. Add fluent `stat(Object stat)` method that parses the value and sets the `statType` field.
4. In `LayerBuilder.build()`, resolve the effective stat as:
   ```groovy
   CharmStatType effectiveStat = statType ?: defaultStatType()
   ```
5. Update concrete geom builders to rename their overrides from `statType()` to `defaultStatType()` (mechanical rename; logic unchanged).

Also add:
- `params(Map<String, Object> values)` convenience in addition to `param(key, value)`

No existing behavior changes: when `stat()` is not called, `statType` is null and `defaultStatType()` is used exactly as before.

### 9.2 [x] Add `SAMPLE` stat and implementation

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/CharmStatType.groovy` — add `SAMPLE` enum value
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/stat/StatEngine.groovy` — add `SAMPLE` case routing to `SampleStat.compute()`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/stat/SampleStat.groovy` (new)

Parameters:
- `n` (default 10000)
- `seed` (optional)
- `method` in `random|systematic` — **omit `stratified` from v1**; stratified sampling requires a `stratifyBy` column parameter that adds meaningful API surface; add in a follow-up once the basic stat is stable

### 9.3 [x] Add Charm convenience constructors for sampled points

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/Geoms.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayersDsl.groovy`

Example target API:
- `geomPointSampled(Map params = [:])`

### 9.4 [x] Add gg `SAMPLE` stat semantics (not helper-only)

**Files:**
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/layer/Layer.groovy` (`StatType`) — add `SAMPLE`
- `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/bridge/GgCharmMappingRegistry.groovy` — map gg `SAMPLE` -> Charm `SAMPLE`

Implement sampling as a first-class gg stat so it composes with existing layer/stat behavior and is not tied to a single helper API.

### 9.5 [x] Add gg convenience helper preserving ggplot-like style

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

Example:
- `geom_point_sampled(Map params = [:])` as a convenience that configures point + sample stat.

### 9.6 [x] Add tests for sampling correctness and reproducibility

**File:** `matrix-charts/src/test/groovy/charm/render/stat/SampleStatTest.groovy` (new)

### 9.7 [x] Document sampling API in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add a "Large dataset helpers" section covering `geomPointSampled()`, the `stat()` override API, and the `n`/`seed`/`method` parameters.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

Executed on this implementation:
- `./gradlew :matrix-charts:test -Pheadless=true`
- `./gradlew :matrix-ggplot:test -Pheadless=true`
- `./gradlew test`

---

## 10. CSS animation helpers

**Priority:** Low | **Effort:** Small

### 10.1 [x] Create `AnimationSpec` model

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/AnimationSpec.groovy` (new)

### 10.2 [x] Add animation field wiring through PlotSpec -> Chart

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Chart.groovy`

### 10.3 [x] Add `animation {}` DSL block to PlotSpec

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

### 10.4 [x] Inject optional `<style>`/`@keyframes` at render time

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy`

### 10.5 [x] Add tests for animation CSS injection and no-op default

**File:** `matrix-charts/src/test/groovy/charm/render/AnimationTest.groovy` (new)

### 10.6 [x] Document animation API and limitations in `charm.md`

**File:** `matrix-charts/docs/charm.md`

Add an "Animations" section covering the `animation {}` DSL, available properties, and the following explicit limitation:

> **Note:** CSS `@keyframes` animations are embedded in the SVG `<style>` block and are visible only when the SVG is rendered in a browser or SVG viewer that supports CSS animations. Animations are **silently stripped** when the chart is exported to PNG or JPEG via `ChartToPng`/`ChartToJpeg`, because rasterization captures a static frame. Use the `animation {}` block only when SVG is the final output format.

### Verification

```bash
./gradlew :matrix-charts:test -Pheadless=true
```

Executed on this implementation:
- `./gradlew :matrix-charts:test -Pheadless=true`
- `./gradlew :matrix-ggplot:test -Pheadless=true`
- `./gradlew test`

---

## Implementation order

Recommended sequence based on impact and dependencies:

| Phase | Items | Rationale |
|------|-------|-----------|
| **Phase 1** | 1 (docs), 5 (errors), 2 (date/time + breaks/labels) | Correctness and usability foundation |
| **Phase 2** | 3 (legend/guide chaining), 4 (facet labeller) | API polish on stable core |
| **Phase 3** | 6 (`plotGrid`, nested `<svg>`) | Major feature with locked design |
| **Phase 4** | 8 (tooltips) | Differentiating capability on stable renderer |
| **Phase 5** | 9 (sampling), 10 (animation) | Performance + polish |
| **Phase 6** | 7 (per-layer scales) | Largest architectural change; do last |

---

## Verification strategy

After each phase, run:

```bash
# Charm core and pict API
./gradlew :matrix-charts:test -Pheadless=true

# ggplot compatibility layer
./gradlew :matrix-ggplot:test -Pheadless=true

# Full project regression suite
./gradlew test
```
