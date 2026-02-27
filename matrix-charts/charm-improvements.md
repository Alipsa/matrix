# Charm Improvements Plan

Prioritized plan addressing common ggplot2 pain points where Charm can improve or differentiate.

Note: Charm has not been released yet, so there are no external backward-compatibility
constraints. However, both the chart-type API in `matrix-charts` and the ggplot API in
`matrix-ggplot` depend on Charm internals, so changes to Charm may require corresponding
updates in those modules. The existing test suites catch these — always run all three
after Charm changes.

---

## Current status of investigated areas

| Area | Status | Action needed |
|------|--------|---------------|
| Categorical axis ordering | Already preserves data order (`LinkedHashSet`) | None — document advantage over ggplot2 |
| Axis limits (xlim/ylim) | Already clamp/squish (like `coord_cartesian`) | None — document advantage over ggplot2 |
| Legend customization | Comprehensive (position, direction, key size, custom guides) | Minor API polish (see 3) |
| Facet label formatting | `Labeller` class with custom closures, `'both'` mode | Minor API polish (see 4) |
| Date/time axes | Three scale types, auto-breaks, simple string specs | None — already better than ggplot2 |
| Error messages | Structured hierarchy, compile-time validation | Improve actionability (see 5) |
| Multi-plot layouts | Faceting only; no independent plot composition | Add `plotGrid()` (see 6) |
| Per-layer scales | Not supported; global scales only | Add per-layer scale overrides (see 7) |
| SVG interactivity | CSS classes/IDs only; no tooltips or events | Add tooltip support (see 8) |
| Large dataset performance | Binning/aggregation stats exist; no explicit sampling | Add data-reduction helpers (see 9) |
| Animation | Not implemented | Add CSS animation helpers (see 10) |

---

## 1. Document existing advantages

**Priority:** Low | **Effort:** Small

Charm already solves two of the most common ggplot2 complaints. Document these clearly so
users migrating from ggplot2 understand the difference.

### 1.1 [ ] Add "Differences from ggplot2" section to charm.md

**File:** `matrix-charts/docs/charm.md`

Document:
- Categorical axes preserve data encounter order (not alphabetical)
- `xlim`/`ylim` clamp values to boundaries (like `coord_cartesian`), never drop data
- Date axis scales use simple string specs (`'3 months'`) instead of R's `scales::date_breaks()`

### Verification
No code changes — documentation only.

---

## 2. (Reserved — no work needed)

Categorical ordering and axis limits already work correctly.

---

## 3. Legend API polish

**Priority:** Medium | **Effort:** Medium

The legend system is feature-complete but some common operations are more verbose than necessary.

### 3.1 [ ] Add `guide()` convenience method to `ScaleDsl`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (ScaleDsl inner class)

Allow setting guide directly on the scale in Charm DSL:
```groovy
scale {
  fill = colorBrewer('Set1').guide('none')    // suppress legend
  color = colorGradient('#fff', '#f00').guide(colorbar())
}
```

Currently users must use a separate `guides {}` block. The `guide()` method on the scale
object is a shorthand that sets the guide spec on the scale's params.

### 3.2 [ ] Add `legendPosition()` shorthand to `PlotSpec`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

```groovy
plot(data) {
  legendPosition('bottom')  // shorthand for theme { legendPosition = 'bottom' }
}
```

### 3.3 [ ] Add tests for legend convenience methods

**File:** `matrix-charts/src/test/groovy/charm/render/LegendConvenienceTest.groovy` (new)

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## 4. Facet label convenience

**Priority:** Low | **Effort:** Small

The `Labeller` class exists but requires importing and constructing manually. Add DSL shortcuts.

### 4.1 [ ] Add built-in labeller constants

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/facet/Labeller.groovy`

Add static factory methods:
```groovy
static Labeller value()      // default: just the value
static Labeller both()       // "variable: value"
static Labeller label(Closure<String> fn)  // custom closure
```

### 4.2 [ ] Allow labeller in Charm DSL facet block

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy` (FacetDsl)

```groovy
facet {
  wrap {
    vars = ['category']
    labeller = Labeller.both()
  }
}
```

### 4.3 [ ] Add tests for labeller convenience

**File:** `matrix-charts/src/test/groovy/charm/render/FacetLabellerTest.groovy` (new)

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## 5. Improve error messages

**Priority:** High | **Effort:** Medium

Charm already has structured exceptions (`CharmValidationException`, `CharmMappingException`, etc.)
and compile-time validation. Improvements focus on making messages more actionable.

### 5.1 [ ] Add "did you mean?" suggestions for misspelled column names

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

In `validateColumn()` (line ~424), when a column is not found, compute Levenshtein distance
to available columns and suggest the closest match:

```groovy
// Current:
"Unknown column 'hwy_mpg' for plot mapping.x. Available columns: cty, hwy, class, drv"

// Improved:
"Unknown column 'hwy_mpg' for plot mapping.x. Did you mean 'hwy'? Available columns: cty, hwy, class, drv"
```

### 5.2 [ ] Add contextual hints for common mistakes

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

Detect and hint for common issues:
- Missing `y` mapping on `geomPoint()` → "geomPoint requires both x and y mappings"
- Using `geomBar()` with explicit y → "geomBar uses stat=COUNT; use geomCol() for pre-computed y values"
- `fill` set to a color string in mapping → "To use a literal color, set fill on the layer: `geomBar().fill('#ff0000')`. To map a column, use the column name."

### 5.3 [ ] Create `LevenshteinDistance` utility

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/util/StringDistance.groovy` (new)

Simple Levenshtein implementation for "did you mean?" suggestions. Keep in charm module
(not matrix-core) since it's only used for developer-facing error messages.

### 5.4 [ ] Add tests for improved error messages

**File:** `matrix-charts/src/test/groovy/charm/core/ErrorMessageTest.groovy` (new)

Test that error messages contain helpful suggestions for common mistakes.

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## 6. Multi-plot composition (`plotGrid`)

**Priority:** High | **Effort:** Large

ggplot2 lacks built-in multi-plot layout, requiring external packages (`patchwork`, `cowplot`).
Charm can provide this natively.

### 6.1 [ ] Create `PlotGrid` specification class

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotGrid.groovy` (new)

```groovy
@CompileStatic
class PlotGrid {
  List<Chart> charts = []
  int ncol = 1
  int nrow                    // auto-computed if null
  List<BigDecimal> widths     // relative column widths (e.g., [2, 1])
  List<BigDecimal> heights    // relative row heights
  String title                // overall title
  int spacing = 10            // gap between subplots in pixels
}
```

### 6.2 [ ] Create `PlotGridRenderer`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/PlotGridRenderer.groovy` (new)

Renders multiple `Chart` objects into a single SVG. Each chart is rendered independently
into its own `<g>` element positioned within the grid. Steps:
1. Compute cell dimensions from total width/height and relative sizes
2. Render each chart via `CharmRenderer` at its cell dimensions
3. Position rendered SVG groups in the grid
4. Add optional overall title

### 6.3 [ ] Add `plotGrid()` factory to `Charts`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Charts.groovy`

```groovy
static PlotGrid plotGrid(Map params = [:], @DelegatesTo(PlotGridDsl) Closure configure) { ... }
```

DSL usage:
```groovy
def grid = Charts.plotGrid(ncol: 2) {
  add(chart1)
  add(chart2)
  add(chart3)
  title('My Dashboard')
}
Svg svg = grid.render(800, 600)
```

### 6.4 [ ] Add `plot_grid()` to ggplot API

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

```groovy
static PlotGrid plot_grid(Map params = [:], GgChart... charts) { ... }
```

### 6.5 [ ] Add export support for PlotGrid

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/chartexport/` (update existing exporters)

Ensure `ChartToSvg`, `ChartToPng`, etc. can accept `PlotGrid` objects or their rendered SVG.

### 6.6 [ ] Add tests for PlotGrid

**File:** `matrix-charts/src/test/groovy/charm/render/PlotGridTest.groovy` (new)

Test cases:
- 2x2 grid renders 4 charts
- Uneven grid (3 charts, ncol=2) leaves one cell empty
- Custom relative widths
- Overall title rendering
- Each subplot renders correctly (axes, geoms, legends)

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## 7. Per-layer color scale overrides

**Priority:** Medium | **Effort:** Large

ggplot2 cannot have two different color/fill scales on the same plot (a top-3 complaint).
Charm's architecture could support this since layers already carry per-layer mappings.

### 7.1 [ ] Add optional `scales` block to `LayerBuilder`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy`

```groovy
LayerBuilder scale(@DelegatesTo(LayerScaleDsl) Closure configure) { ... }
```

Allow layers to specify their own color/fill scales:
```groovy
layers {
  geomPoint().scale { fill = colorBrewer('Set1') }
  geomLine().scale { color = colorGradient('#000', '#f00') }
}
```

### 7.2 [ ] Add `layerScales` field to `Layer` / `LayerSpec`

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Layer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/LayerSpec.groovy`

Store per-layer scale specs as a `Map<String, Scale>` keyed by aesthetic name.

### 7.3 [ ] Train per-layer scales in `ScaleEngine`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/scale/ScaleEngine.groovy`

When a layer has its own scale spec for an aesthetic, train a separate `ColorCharmScale` for
that layer's data only. Store in a `Map<Integer, TrainedScales>` keyed by layer index.

### 7.4 [ ] Use per-layer scales in `GeomUtils` resolve methods

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy`

When resolving color/fill, check if the current layer has a per-layer trained scale before
falling back to the global scale from `RenderContext`.

### 7.5 [ ] Render separate legends for per-layer scales

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/LegendRenderer.groovy`

When multiple layers have different scales for the same aesthetic, render separate legend
entries for each.

### 7.6 [ ] Add `ggnewscale` equivalent to ggplot API

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

```groovy
// Usage:
ggplot(data, aes(x: 'x', y: 'y')) +
  geom_point(aes(color: 'group1')) +
  scale_color_brewer(palette: 'Set1') +
  new_scale_color() +                     // reset color scale for subsequent layers
  geom_line(aes(color: 'group2')) +
  scale_color_gradient(low: '#000', high: '#f00')
```

### 7.7 [ ] Add tests for per-layer scales

**File:** `matrix-charts/src/test/groovy/charm/render/scale/PerLayerScaleTest.groovy` (new)

Test cases:
- Two layers with different fill scales render distinct colors
- Per-layer scale produces separate legend entry
- Layers without per-layer scale use global scale
- Mixed: one layer with per-layer scale, others with global scale

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
./gradlew test
```

---

## 8. SVG tooltip support

**Priority:** High | **Effort:** Medium

ggplot2 is static-only and requires `plotly::ggplotly()` for interactivity. Charm renders
to SVG, which natively supports `<title>` elements for browser tooltips.

### 8.1 [ ] Add `tooltip` aesthetic to `Mapping`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Mapping.groovy`

Add `tooltip` as a recognized aesthetic that maps to a column name or expression.

### 8.2 [ ] Add `tooltip()` method to `LayerBuilder`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy`

```groovy
LayerBuilder tooltip(String template)  // e.g., "x: {x}, y: {y}, group: {color}"
LayerBuilder tooltip(boolean enabled)  // true = auto-generate from mapped aesthetics
```

### 8.3 [ ] Render `<title>` elements in geom renderers

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/geom/GeomUtils.groovy`

Add a `resolveTooltip()` method that:
1. Checks for tooltip template on the layer
2. Checks for tooltip mapping (column reference)
3. Falls back to auto-generated text from x/y/color/fill values
4. Returns null if tooltips are disabled

Then add a helper that wraps a geom element in a `<g>` with a `<title>` child:
```groovy
static void addTooltip(SvgElement element, String tooltipText) {
  if (tooltipText != null) {
    element.addTitle(tooltipText)
  }
}
```

### 8.4 [ ] Update key geom renderers to emit tooltips

**Files:** Update the most commonly used renderers first:
- `BarRenderer.groovy`
- `PointRenderer.groovy`
- `LineRenderer.groovy`
- `AreaRenderer.groovy`

Each renderer calls `GeomUtils.addTooltip()` when tooltip content is available.
Other renderers can be updated incrementally.

### 8.5 [ ] Add `tooltip()` to ggplot API

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

Support tooltip in `aes()`:
```groovy
ggplot(data, aes(x: 'year', y: 'sales', tooltip: 'description')) + geom_point()
```

### 8.6 [ ] Add tests for tooltip rendering

**File:** `matrix-charts/src/test/groovy/charm/render/geom/TooltipTest.groovy` (new)

Test cases:
- Tooltip from mapped column appears as `<title>` in SVG
- Tooltip template with placeholders renders correctly
- Auto-generated tooltip includes x/y values
- No tooltip by default
- Tooltip works with bar, point, and line geoms

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
./gradlew :matrix-ggplot:test -Pheadless=true
```

---

## 9. Large dataset performance helpers

**Priority:** Medium | **Effort:** Medium

ggplot2 struggles with >100k points. Charm on JVM can leverage better data structures, and
existing stat transformations (binning, density) already reduce data. Add explicit helpers.

### 9.1 [ ] Add `statSample` stat type

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/stat/SampleStat.groovy` (new)

Random sampling stat that reduces large datasets before rendering:
```groovy
layers {
  geomPoint().stat('sample').params(n: 5000, seed: 42)
}
```

Parameters:
- `n` — maximum number of points to keep (default: 10000)
- `seed` — random seed for reproducibility (default: null)
- `method` — 'random' (default), 'systematic' (every nth), 'stratified' (by group)

### 9.2 [ ] Register `SAMPLE` in `CharmStatType` and `StatEngine`

**Files:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/CharmStatType.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/stat/StatEngine.groovy`

### 9.3 [ ] Add `geomPointSampled()` convenience

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/` (update relevant builder)

Shorthand for `geomPoint().stat('sample')` with sensible defaults.

### 9.4 [ ] Add `geom_point_sampled()` to ggplot API

**File:** `matrix-ggplot/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy`

### 9.5 [ ] Add tests for sampling stat

**File:** `matrix-charts/src/test/groovy/charm/render/stat/SampleStatTest.groovy` (new)

Test cases:
- Dataset with 50k rows sampled to 5k produces ~5k LayerData entries
- Small dataset (< n) passes through unchanged
- Stratified sampling preserves group proportions
- Seed produces reproducible results

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## 10. CSS animation helpers

**Priority:** Low | **Effort:** Small

Charm SVG output supports CSS classes. Add optional animation helpers that inject
`<style>` blocks with CSS animations.

### 10.1 [ ] Create `AnimationSpec` class

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/AnimationSpec.groovy` (new)

```groovy
@CompileStatic
class AnimationSpec {
  boolean enabled = false
  String entrance = 'fade-in'      // 'fade-in', 'slide-up', 'grow', 'none'
  BigDecimal duration = 0.5        // seconds
  BigDecimal stagger = 0.05        // delay between elements
  String easing = 'ease-out'       // CSS timing function
}
```

### 10.2 [ ] Inject CSS animation `<style>` during render

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy`

When animation is enabled, add a `<style>` element to the SVG root containing
`@keyframes` and animation rules targeting `.charm-*` classes.

### 10.3 [ ] Add `animation {}` block to `PlotSpec`

**File:** `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/PlotSpec.groovy`

```groovy
plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers { geomBar() }
  animation {
    entrance = 'grow'
    duration = 0.8
    stagger = 0.1
  }
}
```

### 10.4 [ ] Add tests for animation CSS injection

**File:** `matrix-charts/src/test/groovy/charm/render/AnimationTest.groovy` (new)

Test cases:
- Animation disabled by default — no `<style>` element
- Animation enabled — `<style>` element with `@keyframes` present
- Stagger produces incremental `animation-delay` values
- SVG structure unchanged apart from `<style>` addition

### Verification
```bash
./gradlew :matrix-charts:test -Pheadless=true
```

---

## Implementation order

Recommended sequence based on impact and dependencies:

| Phase | Items | Rationale |
|-------|-------|-----------|
| **Phase 1** | 1 (docs), 5 (error messages) | Low effort, high user impact |
| **Phase 2** | 8 (tooltips), 3 (legend polish) | Medium effort, high differentiation |
| **Phase 3** | 6 (plotGrid), 4 (facet labels) | Multi-plot is most requested feature |
| **Phase 4** | 9 (sampling), 10 (animation) | Performance and polish |
| **Phase 5** | 7 (per-layer scales) | Largest effort; requires careful architectural work |

## Verification strategy

After each phase, run the full test suite to guard against regressions:

```bash
# Charts module (Charm DSL)
./gradlew :matrix-charts:test -Pheadless=true

# ggplot module (ggplot-compatible API)
./gradlew :matrix-ggplot:test -Pheadless=true

# Full project (all modules)
./gradlew test
```
