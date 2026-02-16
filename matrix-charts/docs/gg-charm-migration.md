# Complete the GG-to-Charm Migration

## Context

The `gg` (Grammar of Graphics) package should be a thin ggplot2-compatible API layer on top of `charm`, which should own all rendering logic. Currently the migration is incomplete: `CharmRenderer` handles only basic cases (10 geom types, 2 stats, 3 positions, Cartesian-only coords), while `GgRenderer` (1,638 lines) retains the full-featured rendering pipeline. The `GgCharmAdapter` attempts to delegate but falls back to `GgRenderer` for most charts.

**Goal:** Move rendering logic out of gg into charm, so gg becomes a thin translation layer that maps ggplot2 API calls to charm specs.

## Current State Summary

| Component | Charm | GG | Coverage |
|---|---|---|---|
| Geom types | 10 (enum) | 22+ (classes with render methods) | ~45% |
| Stat types | IDENTITY, SMOOTH (+ inline histogram/boxplot) | 20+ (GgStat dispatch) | ~20% |
| Positions | IDENTITY, STACK, DODGE | 7 (IDENTITY, DODGE, DODGE2, STACK, FILL, JITTER, NUDGE) | ~43% |
| Coord systems | CARTESIAN only | 9 (Cartesian, Flip, Fixed, Polar, Radial, Trans, Quickmap, Map, SF) | ~11% |
| Scale training | ScaleModel (auto continuous/discrete, transforms) | Full system (expansion, user scales, coord transforms, 10+ scale types) | ~30% |
| Aesthetics | x, y, color, fill | x, y, color, fill, size, shape, alpha, linetype, group, label, weight | ~36% |
| Expression eval | None | Factor, CutWidth, Expression, AfterStat, AfterScale, Identity | 0% |
| Faceting | NONE, WRAP, GRID (basic) | WRAP, GRID with free/free_x/free_y scales, strip styling | ~50% |
| Legend | Simple discrete | Discrete + continuous colorbars, guides, shape merging | ~20% |
| Axis rendering | Simple Cartesian (73 lines) | Polar, Radial, stacked, log ticks, secondary axes (999 lines) | ~7% |
| Theme system | Simple maps | Element-based (ElementLine, ElementRect, etc.) | ~15% |
| Labels | title, x, y | title, subtitle, caption, x, y, legend title | ~60% |

## Key Files

**Charm (target)**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy` (744 lines) - main renderer
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/ScaleModel.groovy` (222 lines) - scale training
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/AxisRenderer.groovy` (73 lines)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/GridRenderer.groovy` (39 lines)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/LegendRenderer.groovy` (59 lines)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/FacetRenderer.groovy` (116 lines)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Geom.groovy` (enum, 10 values)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Stat.groovy` (enum, 2 values)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Position.groovy` (enum, 3 values)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Chart.groovy` (296 lines) - immutable compiled model
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Scale.groovy` (235 lines) - scale spec
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Aes.groovy` - aesthetic mappings

**GG (source to thin out)**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/GgRenderer.groovy` (1,638 lines) - to be emptied
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/AxisRenderer.groovy` (999 lines)
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/LegendRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/adapter/GgCharmAdapter.groovy` (495 lines) - adapter to expand
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/geom/Geom*.groovy` - 22+ geom classes with render()
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/stat/GgStat.groovy` - stat dispatch

## Migration Plan (Incremental Phases)

Each phase is self-contained: compile, all tests pass, no regressions. Each phase expands how many gg charts can be delegated to charm, shrinking the GgRenderer fallback path.

---

### Phase 1: Expand Geom Enum and Rendering in CharmRenderer

Add the most commonly used geom types that are missing from charm.

**Add to `Geom.groovy` enum:**
- [ ] `DENSITY` - kernel density curves
- [ ] `VIOLIN` - violin plots
- [ ] `RUG` - rug marks
- [ ] `SEGMENT` - line segments
- [ ] `HLINE` - horizontal reference lines
- [ ] `VLINE` - vertical reference lines
- [ ] `ABLINE` - arbitrary reference lines
- [ ] `TEXT` - text annotations
- [ ] `LABEL` - text with background
- [ ] `ERRORBAR` - error bars
- [ ] `RIBBON` - filled area between ymin/ymax
- [ ] `STEP` - step function lines
- [ ] `CROSSBAR` - crossbar (box + middle line)

**Add rendering methods to `CharmRenderer.groovy`** for each new geom. Port the core SVG construction logic from the corresponding `GeomXxx.groovy` classes, but adapted to work with charm's `LayerData` pipeline (not gg's Matrix-based approach).

- [ ] Add `renderDensity()` to CharmRenderer
- [ ] Add `renderViolin()` to CharmRenderer
- [ ] Add `renderRug()` to CharmRenderer
- [ ] Add `renderSegment()` to CharmRenderer
- [ ] Add `renderHline()` to CharmRenderer
- [ ] Add `renderVline()` to CharmRenderer
- [ ] Add `renderAbline()` to CharmRenderer
- [ ] Add `renderText()` to CharmRenderer
- [ ] Add `renderLabel()` to CharmRenderer
- [ ] Add `renderErrorbar()` to CharmRenderer
- [ ] Add `renderRibbon()` to CharmRenderer
- [ ] Add `renderStep()` to CharmRenderer
- [ ] Add `renderCrossbar()` to CharmRenderer
- [ ] Update `renderLayer()` switch to dispatch new geom types

**Update adapter:**
- [ ] Expand `GgCharmAdapter` geom mapping
- [ ] Update `GgCharmMappingRegistry` to register new geoms

**Tests:**
- [ ] Add tests for new geom types; verify SVG output is non-empty and structurally correct

**Files to modify:**
- `charm/Geom.groovy` - add enum values
- `charm/render/CharmRenderer.groovy` - add render methods and update switch
- `gg/adapter/GgCharmAdapter.groovy` - expand geom mapping
- `gg/adapter/GgCharmMappingRegistry.groovy` - register new geoms

---

### Phase 2: Expand Stat Types in Charm

Move stat computation logic from inline `CharmRenderer` methods and `GgStat` into charm.

**Add to `Stat.groovy` enum:**
- [ ] `COUNT` - aggregate counts by category
- [ ] `BIN` - histogram binning (already inline as `histogramStat`, formalize it)
- [ ] `BOXPLOT` - quartile computation (already inline as `boxplotStat`, formalize it)
- [ ] `DENSITY` - kernel density estimation
- [ ] `SUMMARY` - summary statistics (mean, median, etc.)
- [ ] `ECDF` - empirical CDF
- [ ] `FUNCTION` - evaluate a function over x range
- [ ] `UNIQUE` - deduplicate rows

**Create StatEngine:**
- [ ] Create `charm/render/StatEngine.groovy` - extract `smoothStat()`, `histogramStat()`, `boxplotStat()` from CharmRenderer
- [ ] Port `count` stat from `GgStat.groovy`
- [ ] Port `density` stat from `GgStat.groovy`
- [ ] Port `summary` stat from `GgStat.groovy`
- [ ] Port `ecdf` stat from `GgStat.groovy`
- [ ] Port `function` stat from `GgStat.groovy`
- [ ] Port `unique` stat from `GgStat.groovy`

**Integration:**
- [ ] Refactor `CharmRenderer.applyStat()` to delegate to StatEngine
- [ ] Update `GgCharmAdapter` to recognize more stat types as delegatable

**Files to modify:**
- `charm/Stat.groovy` - add enum values
- `charm/render/StatEngine.groovy` - **new file**, extract + port stat logic
- `charm/render/CharmRenderer.groovy` - refactor `applyStat()` to delegate to StatEngine
- `gg/adapter/GgCharmAdapter.groovy` - expand stat mapping

---

### Phase 3: Expand Position Adjustments

**Add to `Position.groovy` enum:**
- [ ] `DODGE2` - dodge variant with padding
- [ ] `FILL` - stack and normalize to 100%
- [ ] `JITTER` - random jittering
- [ ] `NUDGE` - fixed offset

**Create PositionEngine:**
- [ ] Create `charm/render/PositionEngine.groovy` - extract `applyPosition()` from CharmRenderer
- [ ] Port `dodge2` position from `GgPosition`
- [ ] Port `fill` position from `GgPosition`
- [ ] Port `jitter` position from `GgPosition`
- [ ] Port `nudge` position from `GgPosition`

**Integration:**
- [ ] Refactor `CharmRenderer` to delegate to PositionEngine
- [ ] Update `GgCharmAdapter` to expand position mapping

**Files to modify:**
- `charm/Position.groovy` - add enum values
- `charm/render/PositionEngine.groovy` - **new file**, position logic
- `charm/render/CharmRenderer.groovy` - delegate to PositionEngine
- `gg/adapter/GgCharmAdapter.groovy` - expand position mapping

---

### Phase 4: Add Non-Positional Aesthetic Channels

Charm currently only maps `x`, `y`, `color`, `fill`. Geom rendering needs `size`, `shape`, `alpha`, `linetype`, `group`, `label` to match gg capabilities.

- [ ] Expand `Aes` mappings to support size, shape, alpha, linetype, group, label, weight
- [ ] Expand `LayerData` to carry size, shape, alpha, linetype, group, label values
- [ ] Add `trainSize()` to `ScaleModel`
- [ ] Add `trainShape()` to `ScaleModel`
- [ ] Add `trainAlpha()` to `ScaleModel`
- [ ] Add `trainLinetype()` to `ScaleModel`
- [ ] Update `CharmRenderer.mapData()` to read new aesthetic channels
- [ ] Update `CharmRenderer.trainScales()` to train new scales
- [ ] Update `renderPoints()` to vary radius by size, change marker shape
- [ ] Update `renderLines()` to apply linetype (dash pattern) and alpha
- [ ] Update other geom renderers to apply aesthetic channels
- [ ] Remove "mapped aesthetics" fallback from `GgCharmAdapter`

**Files to modify:**
- `charm/Aes.groovy` - add mapping support for new channels
- `charm/render/LayerData.groovy` - add fields
- `charm/render/CharmRenderer.groovy` - update `mapData()`, `trainScales()`, and geom renderers
- `charm/render/ScaleModel.groovy` - add `trainSize()`, `trainShape()`, etc.
- `gg/adapter/GgCharmAdapter.groovy` - remove fallback for mapped aesthetics

---

### Phase 5: Coordinate System Support

**Add coordinate types to `CoordType.groovy`:**
- [ ] `FLIP` - swap x/y axes
- [ ] `FIXED` - fixed aspect ratio
- [ ] `TRANS` - axis transformations (log, sqrt, reverse)

**Implement in `CharmRenderer`:**
- [ ] `FLIP`: swap x/y scale ranges during training, swap axis rendering
- [ ] `FIXED`: compute aspect-ratio-adjusted dimensions before rendering
- [ ] `TRANS`: apply transform to data values before scale training (port from GgRenderer lines 550-574)

**Integration:**
- [ ] Update `GgCharmAdapter` to delegate non-Cartesian coords

**Note:** Polar/Radial coords are complex and rarely used; defer to Phase 12.

**Files to modify:**
- `charm/CoordType.groovy` - add enum values
- `charm/Coord.groovy` - add params for each coord type
- `charm/render/CharmRenderer.groovy` - coord-aware rendering
- `gg/adapter/GgCharmAdapter.groovy` - expand coord mapping

---

### Phase 6: Enhanced Scale Training

Port the scale computation logic from `GgRenderer.computeScales()` (lines 543-764) into charm.

**Enhancements to `ScaleModel`:**
- [ ] Scale expansion (multiplicative + additive padding around domain)
- [ ] User-specified breaks and labels (from `Scale.breaks` and `Scale.labels`)
- [ ] Bar baseline inclusion (ensure y=0 is in domain for bar/col/histogram)
- [ ] Handle user-specified `ScaleSpec` overrides per aesthetic

**Integration:**
- [ ] Port `createAutoScale()` logic for proper discrete/continuous auto-detection
- [ ] Update `CharmRenderer` to use enhanced scale training
- [ ] Remove "explicit scales" fallback from `GgCharmAdapter`

**Files to modify:**
- `charm/render/ScaleModel.groovy` - add expansion, breaks, labels support
- `charm/Scale.groovy` - ensure breaks/labels/params are propagated
- `charm/render/CharmRenderer.groovy` - use enhanced scale training
- `gg/adapter/GgCharmAdapter.groovy` - remove "explicit scales" fallback

---

### Phase 7: Enhanced Faceting

Port free scales support from `GgRenderer.renderFaceted()`.

- [ ] Add `scales` field to `FacetSpec`: `'fixed'`, `'free'`, `'free_x'`, `'free_y'`
- [ ] Implement per-panel scale training when scales are free
- [ ] Add strip label styling from theme
- [ ] Add FacetGrid row strips (rotated)
- [ ] Remove faceting fallback from `GgCharmAdapter`

**Files to modify:**
- `charm/Facet.groovy` / `charm/FacetSpec.groovy` - add `scales` field
- `charm/render/FacetRenderer.groovy` - strip rendering improvements
- `charm/render/CharmRenderer.groovy` - per-panel scale training
- `gg/adapter/GgCharmAdapter.groovy` - remove faceting fallback

---

### Phase 8: Enhanced Labels, Legends, and Axes

**Labels:**
- [ ] Add subtitle rendering to `CharmRenderer.renderLabels()`
- [ ] Add caption rendering to `CharmRenderer.renderLabels()`

**Legends:**
- [ ] Continuous color bar rendering (gradient)
- [ ] Multiple legend merging (when color and shape map to same variable)
- [ ] Legend positioning (top, bottom, left, right, none, custom coordinates)
- [ ] Guide type resolution ('legend', 'colorbar', 'none')

**Axes:**
- [ ] Log-scale tick generation
- [ ] Axis label overlap prevention
- [ ] Custom breaks/labels display

**Files to modify:**
- `charm/Labels.groovy` / `charm/LabelsSpec.groovy` - add subtitle, caption
- `charm/render/CharmRenderer.groovy` - renderLabels() expansion
- `charm/render/LegendRenderer.groovy` - continuous colorbars, positioning, merging
- `charm/render/AxisRenderer.groovy` - log ticks, overlap prevention, custom breaks

---

### Phase 9: Theme System Migration

Port the element-based theme system. Currently charm themes are simple maps; gg themes use `ElementLine`, `ElementRect`, `ElementText`, `ElementBlank` objects.

**Approach:** Keep charm's map-based theme but expand it to cover all the properties that the gg element system provides. The `GgCharmAdapter` flattens gg theme elements into charm theme maps.

**Key properties to support:**
- [ ] `panel.background` (fill, color)
- [ ] `panel.grid.major` / `panel.grid.minor` (color, size, linetype)
- [ ] `axis.text` / `axis.title` (size, color, angle, hjust)
- [ ] `axis.ticks` (color, size)
- [ ] `axis.line` (color, size)
- [ ] `legend.position`, `legend.key.size`, `legend.text`, `legend.title`
- [ ] `plot.title`, `plot.subtitle`, `plot.caption` (size, hjust)
- [ ] `strip.background`, `strip.text`
- [ ] Theme flattening in `GgCharmAdapter`
- [ ] Remove non-default theme fallback from `GgCharmAdapter`

**Files to modify:**
- `charm/Theme.groovy` / `charm/ThemeSpec.groovy` - expand supported properties
- `charm/render/CharmRenderer.groovy` - read theme properties in rendering
- `charm/render/AxisRenderer.groovy` - theme-aware axis rendering
- `charm/render/GridRenderer.groovy` - theme-aware grid rendering
- `charm/render/LegendRenderer.groovy` - theme-aware legend rendering
- `gg/adapter/GgCharmAdapter.groovy` - theme flattening, remove non-default theme fallback

---

### Phase 10: Expression Evaluation

Expressions allow closures and computed aesthetics in gg (Factor, CutWidth, Expression, AfterStat, AfterScale, Identity).

**Approach:** Expression evaluation is a **translation concern**, not a rendering concern. The gg adapter evaluates expressions to produce a modified Matrix with computed columns, then passes plain column-name-based aesthetics to charm. This keeps charm clean.

- [ ] Move `evaluateExpressions()` logic from `GgRenderer` to `GgCharmAdapter`
- [ ] Handle `Factor` evaluation in adapter
- [ ] Handle `CutWidth` evaluation in adapter
- [ ] Handle `Expression` (closures) evaluation in adapter
- [ ] Handle `AfterStat` evaluation in adapter
- [ ] Remove "expression-based aesthetics" fallback from `GgCharmAdapter`

**Files to modify:**
- `gg/adapter/GgCharmAdapter.groovy` - add expression evaluation during adaptation, remove fallback

---

### Phase 11: Widen GgCharmAdapter and Thin GgRenderer

At this point, charm handles most rendering scenarios. Systematically remove fallback reasons from `GgCharmAdapter.adapt()`.

- [ ] Remove fallback for each feature that charm now supports
- [ ] Move any remaining translation logic from `GgRenderer` to the adapter
- [ ] Reduce `GgRenderer` to only handle truly unsupported edge cases (Polar/Radial coords, SF/Map geoms)
- [ ] Document intentionally unsupported features that remain in GgRenderer
- [ ] Audit: all gg render code has either been ported to charm or documented as deferred

**Files to modify:**
- `gg/adapter/GgCharmAdapter.groovy` - expand `adapt()` to delegate more cases
- `gg/render/GgRenderer.groovy` - remove rendering code that charm now handles

---

### Phase 12: Advanced Coordinate Systems (Optional)

Add Polar and Radial coordinate support to charm. These require fundamentally different rendering (arc paths instead of rectangles, angular axes, etc.).

- [ ] Add `POLAR` to `CoordType` enum
- [ ] Add `RADIAL` to `CoordType` enum
- [ ] Implement polar rendering (arc paths, angular axes)
- [ ] Implement radial rendering
- [ ] Port polar/radial geom variants (GeomBar radial/donut paths)

**This phase is optional** because polar/radial charts are relatively rare. The GgRenderer fallback can continue to handle these.

---

## Verification Strategy

After each phase:
1. `./gradlew :matrix-charts:compileGroovy` - compiles
2. `./gradlew :matrix-charts:test -Pheadless=true` - all existing tests pass
3. `./gradlew test -Pheadless=true` - full suite passes
4. Verify that the GgCharmAdapter successfully delegates more chart types by checking that fewer fallback reasons are triggered for the test charts

## Backwards Compatibility

- The gg public API (`GgPlot.ggplot()`, `geom_point()`, etc.) remains unchanged throughout
- `GgChart.render()` continues to work; it calls `GgCharmAdapter.render()` which either delegates to charm or falls back to gg
- Charm's own API (`Charts.create()`) is unaffected
- Each phase is additive: no existing behavior is removed until charm fully handles it
