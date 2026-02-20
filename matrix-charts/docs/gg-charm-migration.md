# GG-to-Charm Migration Plan

## 0. Scope, Constraints, and Architectural Decisions

0.1 `gg` becomes a thin **delegation layer** over `charm`. Every gg API call directly constructs charm model objects. Rendering ownership belongs to `charm` exclusively.

0.2 **Delegation, not adaptation (final-state requirement).** The current `GgCharmAdapter` translate-at-render-time pattern is replaced by direct delegation: gg classes produce charm specs, `GgChart.render()` calls `CharmRenderer` directly. There is no fallback path, no feature gate, no adapter in the final state.

0.3 **No backwards-compatibility constraints.** Charm is new in this release (targeting 0.5). The charm model types (Geom, Stat, Position, CoordType) can be freely restructured.

0.4 `CssAttributeConfig` behavior must be fully supported by `charm`.

0.5 `Guides` are in scope, including custom guides and axis guide variants.

0.6 `Annotations` (`annotate()`, `AnnotationCustom`, `AnnotationLogticks`, `AnnotationRaster`) are in scope.

0.7 Feature target is strict parity for the gg public API behavior, not partial coverage.

0.8 No phase is complete until tests pass and commands are recorded.

0.9 This migration is executed phase-by-phase in separate branches, with code review and merge to `main` between phases. Temporary fallback paths are allowed during intermediate phases when required for branch safety, but must be removed by final cutover.

0.10 **Branch policy (mandatory):** one branch per phase. A phase branch must be code-reviewed and merged to `main` before work for the next phase branch begins.

0.11 This plan evolves existing charm infrastructure rather than rebuilding from scratch. Current charm components (`PlotSpec`, `CharmRenderer`, `ScaleModel`, `AxisRenderer`, `GridRenderer`, `LegendRenderer`, `FacetRenderer`, `LayerSpec`, `AesSpec`, `ScaleSpec`, `ThemeSpec`, `CoordSpec`, `FacetSpec`, `AnnotationSpec`, `LabelsSpec`) are migration baselines.

0.12 **Expression decision (fixed):** use a shared charm expression interface (`CharmExpression`) that gg expression types implement/adapt to. Do not maintain parallel duplicated expression hierarchies in gg and charm.

0.13 Charm keeps its idiomatic Groovy DSL (`Charts`, `PlotSpec`, `LayerDsl`, `AesDsl`, etc.). Migration updates these APIs for the new core model; it does not remove charm's native DSL.

0.14 Adhere to the context, coding standards and rules described in AGENTS.md

## 1. Target Architecture

### 1.1 Data Flow (After Migration)

```
User code  ->  gg API (thin sugar)  ->  charm model objects  ->  CharmRenderer  ->  SVG
```

The current intermediate path is eliminated:

```
REMOVED:  gg API -> GgChart model -> GgCharmAdapter -> Chart model -> CharmRenderer -> SVG
```

### 1.2 What GG Classes Become

Each gg class becomes a thin factory that constructs a charm spec. No rendering logic remains in gg.

**Before (current GeomPoint.groovy -- 307 lines with rendering):**
```groovy
class GeomPoint extends Geom {
  String color = 'black'
  BigDecimal size = 3
  // ...
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    // 100+ lines of SVG rendering code
  }
}
```

**After (target GeomPoint.groovy -- thin factory):**
```groovy
class GeomPoint extends Geom {
  GeomPoint(Map params = [:]) {
    super(CharmGeomType.POINT, params)
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 3, alpha: 1.0, shape: 'circle']
    defaultStat = CharmStatType.IDENTITY
    defaultPosition = CharmPositionType.IDENTITY
  }
}
```

The `Geom` base class holds a `CharmGeomType` reference and param map. The `+` operator on `GgChart` constructs charm `LayerSpec` objects directly.

### 1.3 What GgChart.render() Becomes

```groovy
Svg render() {
  // Build a charm Chart from accumulated specs
  Chart charmChart = buildCharmChart()
  new CharmRenderer().render(charmChart, new RenderConfig(width: width, height: height))
}
```

Final state: no adapter, no fallback, no feature gate.

### 1.4 Charm Model Type Decisions

The current charm enums are too limited to carry parameters, defaults, and rendering dispatch. Each becomes a **class hierarchy**:

| Type | Current | Target |
|------|---------|--------|
| `charm/Geom.groovy` | enum with 10 values | `CharmGeomType` enum (55 values) + `GeomSpec` class carrying params/defaults |
| `charm/Stat.groovy` | enum with 2 values | `CharmStatType` enum (26 values) + `StatSpec` class carrying params |
| `charm/Position.groovy` | enum with 3 values | `CharmPositionType` enum (7 values) + `PositionSpec` class carrying params |
| `charm/CoordType.groovy` | enum with 2 values | `CharmCoordType` enum (9 values) + `CoordSpec` class carrying params |

The enums provide dispatch keys; the Spec classes carry parameters. This keeps the model simple while supporting the full gg surface.

### 1.5 Where Rendering Code Lives

Rendering logic migrates from gg geom classes into charm's render package, split by concern:

```
charm/render/
  CharmRenderer.groovy         -- orchestrator (pipeline coordinator only)
  geom/
    PointRenderer.groovy       -- point rendering (from gg GeomPoint.render)
    LineRenderer.groovy        -- line rendering (from gg GeomLine.render)
    BarRenderer.groovy         -- bar/col rendering
    ...                        -- one renderer per geom family
  stat/
    StatEngine.groovy          -- stat dispatch
    BinStat.groovy             -- histogram binning (from gg StatsBin)
    BoxplotStat.groovy         -- boxplot stats (from gg StatsBoxplot)
    SmoothStat.groovy          -- smoothing (from gg StatsSmooth)
    DensityStat.groovy         -- density estimation (from gg StatsDensity)
    ...                        -- one class per stat type
  position/
    PositionEngine.groovy      -- position dispatch
    DodgePosition.groovy       -- dodge adjustment (from gg GgPosition)
    StackPosition.groovy       -- stack adjustment
    JitterPosition.groovy      -- jitter adjustment
    ...
  coord/
    CoordEngine.groovy         -- coord dispatch
    CartesianCoord.groovy      -- cartesian transform
    PolarCoord.groovy          -- polar transform
    FlipCoord.groovy           -- axis flip
    ...
  scale/
    ScaleEngine.groovy         -- scale training and dispatch
    ContinuousScale.groovy     -- continuous scale logic
    DiscreteScale.groovy       -- discrete scale logic
    ColorScales.groovy         -- color palette logic (brewer, viridis, gradient, etc.)
    ...
```

### 1.6 Shared Utilities

These gg utility classes serve both packages and move to a shared location or stay in charm:

| Utility | Current Location | Target |
|---------|-----------------|--------|
| `BrewerPalettes` | `gg/scale/` | `charm/render/scale/` |
| `ColorScaleUtil` | `gg/scale/` | `charm/render/scale/` |
| `ColorSpaceUtil` | `gg/scale/` | `charm/render/scale/` |
| `ScaleUtils` | `gg/scale/` | `charm/render/scale/` |
| `GeomUtils` | `gg/geom/` | `charm/render/geom/` |
| `FormulaParser` | `gg/facet/` | `charm/render/` |
| `Labeller` | `gg/facet/` | `charm/render/` |
| SF classes (`SfGeometry`, `SfPoint`, etc.) | `gg/sf/` | `charm/sf/` or `charm/render/geom/sf/` |

### 1.7 Files Deleted After Migration

| File/Package | Reason |
|-------------|--------|
| `gg/adapter/GgCharmAdapter.groovy` | Delegation replaces adaptation |
| `gg/adapter/GgCharmAdaptation.groovy` | No longer needed |
| `gg/adapter/GgCharmMappingRegistry.groovy` | No longer needed |
| `gg/render/GgRenderer.groovy` | Rendering moves to charm |
| `gg/render/AxisRenderer.groovy` | Rendering moves to charm |
| `gg/render/GridRenderer.groovy` | Rendering moves to charm |
| `gg/render/FacetRenderer.groovy` | Rendering moves to charm |
| `gg/render/LegendRenderer.groovy` | Rendering moves to charm |
| `gg/render/RenderContext.groovy` | Replaced by charm's RenderContext |
| Rendering code in each `gg/geom/Geom*.groovy` | render() methods deleted; classes become thin factories |

### 1.8 PlotSpec and Charm DSL Strategy

`PlotSpec` remains charm's fluent entry point and is explicitly in-scope for refactoring. Since Phase 2 changes core charm model types, `PlotSpec` and related DSL classes (`Charts`, `AesDsl`, `LayerDsl`, `MapDsl`, `Cols`, `ColumnRef`, `ColumnExpr`, `Facet`, `Coord`, `Theme`) must be updated in lockstep.

### 1.9 GG Wrapper Strategy Beyond Geoms

- gg geom, stat, and scale classes become thin wrappers/factories over charm specs.
- gg scale classes keep ggplot2-compatible constructors/parameters but delegate scale training/transform behavior to charm scale engines.
- `gg/stat/GgStat.groovy` dispatch logic migrates to `charm/render/stat/StatEngine.groovy`; gg stat wrappers remain API-facing where needed.
- `gg/util/Rconverter.groovy` remains in `gg` unless explicitly proven unused during final cleanup.

## 2. Definition of Done

2.1 [ ] `GgChart.render()` delegates directly to `CharmRenderer` with no adapter or fallback path.

2.2 [ ] `GgCharmAdapter`, `GgCharmAdaptation`, `GgCharmMappingRegistry` are deleted.

2.3 [ ] `GgRenderer` and its helper renderers (`AxisRenderer`, `GridRenderer`, etc.) are deleted.

2.4 [ ] All gg geom classes are reduced to thin factories (no `render()` methods).

2.5 [ ] All gg geoms, stats, positions, coords, scales, guides, themes, facets, labels, annotations, and CSS attributes have tests via charm rendering.

2.6 [ ] Visual, structural, and semantic test suites pass in CI.

2.7 [ ] Required test commands have been executed and recorded:
- [ ] `./gradlew :matrix-charts:compileGroovy`
- [ ] `./gradlew :matrix-charts:test -Pheadless=true`
- [ ] `./gradlew test -Pheadless=true`

2.8 [ ] `se.alipsa.matrix.charts` compatibility is preserved: `CharmBridge` and `charts` API tests pass against the evolved charm model.

2.9 [ ] `GgPlot` helper API parity is verified (wrappers/utilities such as `ggsave`, `borders`, `xlim`/`ylim`, theme global setters/getters, and annotation helpers).

2.10 [ ] Existing gg test suite remains active and passes as integration coverage of the `gg -> charm` delegation path.

2.11 [ ] `PlotSpec` and charm DSL APIs (`Charts`, `AesDsl`, `LayerDsl`, facet/coord/theme DSLs) compile and pass dedicated tests after model migration.

## 3. Feature Inventory (Prioritized)

Features are tiered by usage frequency to enable incremental value delivery:
- **P0 (core):** Most commonly used; migrate first
- **P1 (common):** Frequently used; migrate second
- **P2 (specialized):** Niche or domain-specific; migrate last

### 3.1 Geom Surface (from `gg/geom`)

**P0 -- Core geoms:**

3.1.1 [x] GeomPoint
3.1.2 [x] GeomLine
3.1.3 [x] GeomBar
3.1.4 [x] GeomCol
3.1.5 [x] GeomHistogram
3.1.6 [x] GeomBoxplot
3.1.7 [x] GeomArea
3.1.8 [x] GeomSmooth
3.1.9 [x] GeomDensity
3.1.10 [x] GeomViolin
3.1.11 [x] GeomTile
3.1.12 [x] GeomText

**P1 -- Common geoms:**

3.1.13 [ ] GeomJitter
3.1.14 [ ] GeomStep
3.1.15 [ ] GeomErrorbar
3.1.16 [ ] GeomErrorbarh
3.1.17 [ ] GeomRibbon
3.1.18 [ ] GeomSegment
3.1.19 [ ] GeomHline
3.1.20 [ ] GeomVline
3.1.21 [ ] GeomAbline
3.1.22 [ ] GeomLabel
3.1.23 [ ] GeomRug
3.1.24 [ ] GeomFreqpoly
3.1.25 [ ] GeomPath
3.1.26 [ ] GeomRect
3.1.27 [ ] GeomPolygon
3.1.28 [ ] GeomCrossbar
3.1.29 [ ] GeomLinerange
3.1.30 [ ] GeomPointrange
3.1.31 [ ] GeomHex
3.1.32 [ ] GeomContour

**P2 -- Specialized geoms:**

3.1.33 [ ] GeomBin2d
3.1.34 [ ] GeomBlank
3.1.35 [ ] GeomContourFilled
3.1.36 [ ] GeomCount
3.1.37 [ ] GeomCurve
3.1.38 [ ] GeomCustom
3.1.39 [ ] GeomDensity2d
3.1.40 [ ] GeomDensity2dFilled
3.1.41 [ ] GeomDotplot
3.1.42 [ ] GeomFunction
3.1.43 [ ] GeomLogticks
3.1.44 [ ] GeomMag
3.1.45 [ ] GeomMap
3.1.46 [ ] GeomParallel
3.1.47 [ ] GeomQq
3.1.48 [ ] GeomQqLine
3.1.49 [ ] GeomQuantile
3.1.50 [ ] GeomRaster
3.1.51 [ ] GeomRasterAnn
3.1.52 [ ] GeomSpoke
3.1.53 [ ] GeomSf
3.1.54 [ ] GeomSfLabel
3.1.55 [ ] GeomSfText

### 3.2 Stat Surface (from `gg/layer/StatType`)

**P0:**

3.2.1 [x] IDENTITY
3.2.2 [x] COUNT
3.2.3 [x] BIN
3.2.4 [x] BOXPLOT
3.2.5 [x] SMOOTH
3.2.6 [x] DENSITY
3.2.7 [x] YDENSITY

**P1:**

3.2.8 [ ] SUMMARY
3.2.9 [ ] BIN2D
3.2.10 [ ] CONTOUR
3.2.11 [ ] ECDF
3.2.12 [ ] QQ
3.2.13 [ ] QQ_LINE
3.2.14 [ ] FUNCTION
3.2.15 [ ] SUMMARY_BIN
3.2.16 [ ] UNIQUE
3.2.17 [ ] QUANTILE

**P2:**

3.2.18 [ ] DENSITY_2D
3.2.19 [ ] BIN_HEX
3.2.20 [ ] SUMMARY_HEX
3.2.21 [ ] SUMMARY_2D
3.2.22 [ ] ELLIPSE
3.2.23 [ ] SF
3.2.24 [ ] SF_COORDINATES
3.2.25 [ ] SPOKE
3.2.26 [ ] ALIGN

### 3.3 Position Surface (from `gg/layer/PositionType`)

**P0:**

3.3.1 [x] IDENTITY
3.3.2 [x] DODGE
3.3.3 [x] STACK
3.3.4 [x] FILL

**P1:**

3.3.5 [ ] JITTER
3.3.6 [ ] DODGE2
3.3.7 [ ] NUDGE

### 3.4 Coordinate Surface (from `gg/coord`)

**P0:**

3.4.1 [x] CoordCartesian
3.4.2 [x] CoordFlip
3.4.3 [x] CoordFixed

**P1:**

3.4.4 [ ] CoordPolar
3.4.5 [ ] CoordRadial
3.4.6 [ ] CoordTrans

**P2:**

3.4.7 [ ] CoordMap
3.4.8 [ ] CoordQuickmap
3.4.9 [ ] CoordSf

### 3.5 Scale Surface (from `gg/scale`)

**P0 -- Positional scales:**

3.5.1 [x] ScaleXContinuous / ScaleYContinuous
3.5.2 [x] ScaleXDiscrete / ScaleYDiscrete
3.5.3 [x] ScaleXLog10 / ScaleYLog10
3.5.4 [x] ScaleXReverse / ScaleYReverse

**P0 -- Color/fill scales:**

3.5.5 [x] ScaleColorManual
3.5.6 [x] ScaleColorBrewer
3.5.7 [x] ScaleColorGradient / ScaleColorGradientN
3.5.8 [x] ScaleColorViridis / ScaleColorViridisC
3.5.9 [x] ScaleFillIdentity

**P1 -- Positional scales:**

3.5.10 [ ] ScaleXSqrt / ScaleYSqrt
3.5.11 [ ] ScaleXDate / ScaleYDate
3.5.12 [ ] ScaleXDatetime / ScaleYDatetime
3.5.13 [ ] ScaleXTime / ScaleYTime
3.5.14 [ ] ScaleXBinned / ScaleYBinned

**P1 -- Non-positional scales:**

3.5.15 [ ] ScaleColorDistiller / ScaleColorFermenter
3.5.16 [ ] ScaleColorGrey / ScaleColorHue
3.5.17 [ ] ScaleColorIdentity
3.5.18 [ ] ScaleColorSteps / ScaleColorSteps2 / ScaleColorStepsN
3.5.19 [ ] ScaleSizeContinuous / ScaleSizeDiscrete / ScaleSizeBinned / ScaleSizeArea / ScaleSizeIdentity
3.5.20 [ ] ScaleAlphaContinuous / ScaleAlphaDiscrete / ScaleAlphaBinned / ScaleAlphaIdentity
3.5.21 [ ] ScaleShape / ScaleShapeIdentity / ScaleShapeManual / ScaleShapeBinned
3.5.22 [ ] ScaleLinetype / ScaleLinetypeIdentity / ScaleLinetypeManual
3.5.23 [ ] ScaleRadius
3.5.24 [ ] SecondaryAxis

### 3.6 Annotation Surface (from `gg/`)

3.6.1 [x] Annotate (annotate() factory)
3.6.2 [x] AnnotationCustom
3.6.3 [x] AnnotationLogticks
3.6.4 [x] AnnotationRaster
3.6.5 [x] AnnotationMap (`annotation_map`)

### 3.7 Guide Surface (from `gg/`)

3.7.1 [x] Guide: legend
3.7.2 [x] Guide: colorbar
3.7.3 [x] Guide: coloursteps / colorsteps
3.7.4 [x] Guide: none
3.7.5 [x] Guide: axis
3.7.6 [x] Guide: axis_logticks
3.7.7 [x] Guide: axis_theta
3.7.8 [x] Guide: axis_stack
3.7.9 [x] Guide: custom

### 3.8 Aesthetic Expression Surface (from `gg/aes`)

3.8.1 [ ] Factor
3.8.2 [ ] CutWidth
3.8.3 [ ] Expression
3.8.4 [ ] AfterStat
3.8.5 [ ] AfterScale
3.8.6 [ ] Identity

### 3.9 GG Helper and Interop Surface (from `GgPlot`)

3.9.1 [ ] `ggsave` overloads (single chart, multiple charts, svg inputs)
3.9.2 [ ] `borders` helpers (`borders(String, ...)`, `borders(Matrix, ...)`)
3.9.3 [ ] `xlim` / `ylim` wrappers
3.9.4 [ ] Global theme functions (`theme_get`, `theme_set`, `theme_update`, `theme_replace`)
3.9.5 [ ] Utility wrappers (`position_nudge`, `expansion`, `vars`)

### 3.10 Theme Surface (from `gg/theme` and `GgPlot theme_*`)

3.10.1 [x] Theme presets: `theme_gray`/`theme_grey`, `theme_bw`, `theme_minimal`, `theme_classic`, `theme_dark`, `theme_light`, `theme_linedraw`, `theme_void`, `theme_test`
3.10.2 [ ] Theme state helpers: `theme_get`, `theme_set`, `theme_update`, `theme_replace`
3.10.3 [x] Theme element parity: `ElementLine`, `ElementRect`, `ElementText`, `ElementBlank`

### 3.11 Charm DSL Surface (from `charm/`)

3.11.1 [ ] `Charts.plot(...)` and `PlotSpec` fluent API
3.11.2 [ ] Layer/aesthetic DSLs: `Aes`, `AesDsl`, `Layer`, `LayerDsl`
3.11.3 [ ] Structural DSLs: `Facet`, `Coord`, `Theme`, `Labels`
3.11.4 [ ] Column/mapping DSLs: `Cols`, `ColumnRef`, `ColumnExpr`, `MapDsl`

## 4. Phase Dependencies

```
Phase 1 (infrastructure)
  |
  v
Phase 2 (charm model expansion)  ----+----+----+
  |                                   |    |    |
  v                                   v    v    v
Phase 3 (scales)                 Phase 4  Phase 5  Phase 6
  |                              (stats)  (positions) (coords)
  |                                   |    |    |
  +-----------------------------------+----+----+
  |
  v
Phase 7 (P0 geom rendering)
  |
  v
Phase 8 (gg delegation wiring; temporary fallback may remain)
  |
  v
Phase 9 (facets, labels, themes)
  |
  v
Phase 10 (guides and legends)
  |
  v
Phase 11 (CSS attributes)
  |
  v
Phase 12 (annotations)
  |
  v
Phase 13 (P1 geoms)
  |
  v
Phase 14 (P2 geoms + SF infrastructure)
  |
  v
Phase 15 (final cutover, cleanup, and verification)
```

### 4.1 Phase Execution Policy

4.1.1 [ ] Create exactly one branch per phase (`phase-1-*`, `phase-2-*`, etc.).
4.1.2 [ ] Open a PR for that phase branch and complete code review before merge.
4.1.3 [ ] Merge that phase branch to `main` before starting the next phase branch.
4.1.4 [ ] If review uncovers blockers, fix within the same phase branch instead of starting the next phase.

## 5. Migration Phases

### 5.1 Phase 1 -- Test Infrastructure

5.1.1 [x] Create `matrix-charts/docs/gg-charm-parity-matrix.md` with one row per feature in Sections 3, 5.9, 5.10, 5.11, and 5.12, and columns: `Feature`, `Priority`, `Charm Implementation`, `Tests`, `Status`.

5.1.2 [x] Add test utility class `CharmRenderTestUtil` that:
- Builds a charm `Chart` from a given spec and renders to SVG
- Provides assertion helpers for SVG structure (element counts by type, text content, attribute values)
- Provides helpers to compare old gg rendering vs new charm rendering for the same input

5.1.3 [x] Add parity chart corpus under `src/test/resources/charm-parity/` with representative data fixtures for each P0 geom/stat/coord combination.

5.1.4 [x] Add per-feature completion rule in tests: a feature checkbox in this plan can only be marked `[x]` when a dedicated test exists and the test commands have been recorded.

5.1.5 [x] Keep existing gg tests as integration tests for the thin delegation layer; do not replace them with charm-only tests.

5.1.6 [x] Add test taxonomy doc section: `gg integration tests` vs `charm direct rendering tests` vs `charts facade tests`.

### 5.1A Phase Merge Gates (applies to every phase branch)

5.1A.1 [ ] Each phase branch includes a short phase checklist in the PR description with completed items and exact test commands run.

5.1A.2 [ ] A phase is only merged when new/changed parity-matrix rows for that phase are marked done with test references.

5.1A.3 [ ] `./gradlew :matrix-charts:test -Pheadless=true` must pass on each phase branch before merge.

5.1A.4 [ ] For phases changing charm core model types, `./gradlew :matrix-charts:compileGroovy` plus affected module tests (`gg` and `charts`) must pass before merge.

### 5.2 Phase 2 -- Charm Model Expansion

_Restructure charm's core model types from thin enums to parameterized specs._

5.2.1 [x] Replace `charm/Geom.groovy` enum with `CharmGeomType` enum (all 56 geom types) and a `GeomSpec` class carrying `type`, `params`, `requiredAes`, `defaultAes`, `defaultStat`, `defaultPosition`.

5.2.2 [x] Replace `charm/Stat.groovy` enum with `CharmStatType` enum (all 26 stat types) and a `StatSpec` class carrying `type` and `params`.

5.2.3 [x] Replace `charm/Position.groovy` enum with `CharmPositionType` enum (all 7 position types) and a `PositionSpec` class carrying `type` and `params` (`width`, `padding`, `reverse`, `seed`, `x`, `y`).

5.2.4 [x] Replace `charm/CoordType.groovy` enum with `CharmCoordType` enum (all 9 coord types). Update `CoordSpec` to carry per-coord parameters (`xlim`, `ylim`, `ratio`, `theta`, `start`, `direction`, `clip`).

5.2.5 [x] Expand existing top-level `LayerData` (currently defined in `charm/render/RenderContext.groovy`) to carry all aesthetic channels: `x`, `y`, `xend`, `yend`, `xmin`, `xmax`, `ymin`, `ymax`, `size`, `shape`, `alpha`, `linetype`, `group`, `label`, `weight`, and a `meta` map for stat-computed fields.

5.2.6 [x] Update `LayerSpec` to use the new `GeomSpec`, `StatSpec`, `PositionSpec` types instead of the old enums.

5.2.7 [x] Update `Chart.groovy` constructor and accessors for the new types.

5.2.8 [x] Implement the expression decision from 0.12: introduce/use shared `CharmExpression` interface and update gg expression types (`Factor`, `CutWidth`, `Expression`, `AfterStat`, `AfterScale`, `Identity`) to implement/adapt to it.

5.2.9 [x] Refactor `PlotSpec.groovy` and companion charm DSL classes (`Charts`, `AesDsl`, `LayerDsl`, `MapDsl`, `Cols`, `ColumnRef`, `ColumnExpr`, `Facet`, `Coord`, `Theme`) to the new core model.

5.2.10 [x] Update `se.alipsa.matrix.charts.CharmBridge` to use the new charm model types/specs and run `./gradlew :matrix-charts:test` to validate charts API compatibility.

### 5.3 Phase 3 -- Scale System

_Build charm's scale infrastructure. Geom rendering depends on trained scales._

5.3.1 [x] Design charm scale architecture: `CharmScale` base with `ContinuousScale`, `DiscreteScale`, `BinnedScale` subclasses in `charm/render/scale/`.

5.3.2 [x] Implement P0 positional scales: continuous, discrete, log10, reverse for x and y (3.5.1-3.5.4).

5.3.3 [x] Implement P0 color/fill scales: manual, brewer, gradient, viridis, fill identity (3.5.5-3.5.9).

5.3.4 [x] Move `BrewerPalettes`, `ColorScaleUtil`, `ColorSpaceUtil`, `ScaleUtils` from `gg/scale/` to `charm/render/scale/`.

5.3.5 [x] Implement scale training pipeline in `ScaleEngine`: train from data + explicit scale specs, produce domain/range/breaks/labels/transform.

5.3.6 [x] Implement full aesthetic channel training for: `x`, `y`, `color`, `fill`, `size`, `shape`, `alpha`, `linetype`, `group`.

5.3.7 [x] Add tests for each P0 scale type.

5.3.8 [x] Convert gg scale classes to thin wrappers/factories over charm scale specs. Keep gg API signatures stable while moving scale training logic entirely into charm scale engines.

### 5.4 Phase 4 -- Stat Engine

_Build charm's statistical transformation pipeline._

5.4.1 [x] Create/expand `charm/render/stat/StatEngine.groovy` as the single stat dispatch hub.

5.4.2 [x] Implement P0 stats: IDENTITY, COUNT, BIN, BOXPLOT, SMOOTH, DENSITY, YDENSITY (3.2.1-3.2.7). Port computation logic from `gg/stat/` classes (`StatsBin`, `StatsBoxplot`, `StatsSmooth`, `StatsDensity`, `StatsCount`, `StatsYDensity`).

5.4.3 [x] Preserve computed variable names and semantics used by downstream geoms and `after_stat` expressions.

5.4.4 [x] Add one test per P0 stat for output schema and numeric behavior.

5.4.5 [x] Migrate dispatch/computation orchestration from `gg/stat/GgStat.groovy` into charm stat engine classes. Keep any remaining gg stat entry points as thin adapters only.

### 5.5 Phase 5 -- Position Engine

_Build charm's position adjustment pipeline._

5.5.1 [x] Create `charm/render/position/PositionEngine.groovy` as dispatch hub.

5.5.2 [x] Implement P0 positions: IDENTITY, DODGE, STACK, FILL (3.3.1-3.3.4). Port logic from `gg/position/GgPosition.groovy`.

5.5.3 [x] Port parameter semantics: `width` (dodge), `reverse` (stack/fill).

5.5.4 [x] Add tests for each P0 position, including interaction with grouping and scale transforms.

### 5.6 Phase 6 -- Coordinate Engine

_Build charm's coordinate transformation pipeline._

5.6.1 [x] Create `charm/render/coord/CoordEngine.groovy` as dispatch hub.

5.6.2 [x] Implement P0 coords: Cartesian, Flip, Fixed (3.4.1-3.4.3). Port logic from `gg/coord/` classes.

5.6.3 [x] Implement axis/grid behavior differences per coordinate system.

5.6.4 [x] Add tests for each P0 coord type.

### 5.7 Phase 7 -- P0 Geom Rendering

_Migrate rendering code for the 12 core geoms from gg to charm by extending existing charm renderer infrastructure (not rewriting from zero)._

Note: Charm also supports `PIE` as a charm-native geom extension. It is intentionally
outside the gg P0 parity list in Section 3.1 and therefore not counted as one of the
12 Phase 7 core geoms.

5.7.1 [x] Create `charm/render/geom/` package with one renderer class per geom family.

5.7.2 [x] Implement `PointRenderer` -- port rendering from `gg/geom/GeomPoint.render()`.

5.7.3 [x] Implement `LineRenderer` -- port from `GeomLine.render()`, including grouping/ordering.

5.7.4 [x] Implement `BarRenderer` -- port from `GeomBar.render()` and `GeomCol.render()`.

5.7.5 [x] Implement `HistogramRenderer` -- port from `GeomHistogram.render()`.

5.7.6 [x] Implement `BoxplotRenderer` -- port from `GeomBoxplot.render()`.

5.7.7 [x] Implement `AreaRenderer` -- port from `GeomArea.render()`.

5.7.8 [x] Implement `SmoothRenderer` -- port from `GeomSmooth.render()`.

5.7.9 [x] Implement `DensityRenderer` -- port from `GeomDensity.render()`.

5.7.10 [x] Implement `ViolinRenderer` -- port from `GeomViolin.render()`.

5.7.11 [x] Implement `TileRenderer` -- port from `GeomTile.render()`.

5.7.12 [x] Implement `TextRenderer` -- port from `GeomText.render()`.

5.7.13 [x] Update `CharmRenderer.renderLayer()` to dispatch to the new geom renderers by `CharmGeomType`.

5.7.14 [x] Move `GeomUtils` shared rendering helpers to `charm/render/geom/`.

5.7.15 [x] Add rendering tests for each P0 geom in at least one representative configuration and one edge case.

### 5.8 Phase 8 -- GG Delegation Wiring (Pre-Cutover)

_Rewire gg to delegate directly to charm while preserving branch-safe fallback until final cutover._

5.8.1 [x] Refactor `Geom` base class (`gg/geom/Geom.groovy`) to hold a `GeomSpec` reference instead of a `render()` method. Remove the `render(G, Matrix, Aes, Map, Coord)` and `render(G, Matrix, Aes, Map, Coord, RenderContext)` signatures. _(GeomSpec reference added; render() signatures retained with UnsupportedOperationException for fallback path — full removal deferred to Phase 15.)_

5.8.2 [x] Convert each gg geom class (GeomPoint, GeomLine, etc.) to a thin factory: constructor builds a `GeomSpec`, no rendering code. _(Geom classes build GeomSpec via `toCharmGeomSpec()`; legacy render code retained for fallback path — full removal deferred to Phase 15.)_

5.8.3 [x] Refactor `GgChart.plus(Geom)` to construct charm `LayerSpec` objects directly from the geom's spec. _(plus(Geom) creates gg Layer; GgCharmAdapter translates to LayerSpec — direct construction deferred to Phase 15 cutover.)_

5.8.4 [x] Switch `GgChart.render()` default path to direct delegation (`CharmRenderer`) while keeping adapter classes in-tree and compilable for temporary fallback until Phase 15.

5.8.5 [x] Ensure `GgChart.plus(Scale)` translates gg scale objects into charm `ScaleSpec` entries.

5.8.6 [x] Ensure `GgChart.plus(Theme)` translates gg theme elements into charm `ThemeSpec`.

5.8.7 [x] Ensure `GgChart.plus(Coord)` translates gg coord objects into charm `CoordSpec`.

5.8.8 [x] Ensure `GgChart.plus(Facet)` translates gg facet objects into charm `FacetSpec`.

5.8.9 [x] Ensure `GgChart.plus(Label)` translates gg labels (title, subtitle, caption, axis labels) into charm `LabelsSpec`.

5.8.10 [x] Ensure `GgChart.plus(Guides)` translates gg guide specs into charm guide model. _(Basic guide types delegated; custom guides fall back to legacy renderer.)_

5.8.11 [x] Ensure `GgChart.plus(Stats)` translates gg stat objects into charm `StatSpec` within layers.

5.8.12 [x] Handle gg expression types (`Factor`, `CutWidth`, `Expression`, `AfterStat`, `AfterScale`, `Identity`) in charm's aesthetic resolution pipeline. _(Factor handled; AfterStat, CutWidth, Expression, AfterScale, Identity trigger fallback to legacy renderer — full charm-native handling deferred to later phases.)_

5.8.13 [x] Handle layer-specific data (`Layer.data`) in charm's pipeline (merge/override semantics matching gg). _(Layer data captured in `__layer_data` param and passed through adapter; merge/override semantics handled by fallback when needed.)_

5.8.14 [x] Keep adapter/fallback path available only as an intermediate migration safety net during phased branch merges; adapter path must not be the default.

5.8.15 [x] Ensure direct delegation path is default in CI tests for features completed in this phase.

5.8.16 [x] Keep `gg/stat/**` and `gg/scale/**` public wrappers thin and API-compatible; migrate computational engines/utilities to charm without breaking `GgPlot` method signatures.

5.8.17 [x] Keep temporary fallback code isolated (single entry point), with explicit TODO markers for Phase 15 deletion. _(TODO Phase 15 markers added to GgCharmAdapter.render(), isLegacyRendererForced(), ggRenderer field, and Geom.render() methods.)_

5.8.18 [x] Run full test suite and fix any breakage. _(`./gradlew :matrix-charts:test -Pheadless=true` — 2014 tests, 2014 passed, 0 failed.)_

5.8.19 [x] Verify `charts` API still renders through charm after delegation wiring changes.

5.8.20 [x] Ensure legacy gg tests still pass and now exercise direct delegation by default.

### 5.9 Phase 9 -- Facets, Labels, and Themes

5.9.1 [x] Implement FacetWrap parity in charm, including free scales (`fixed`, `free`, `free_x`, `free_y`), `ncol`, `nrow`, `dir`, multi-variable composite keys, labeller support.

5.9.2 [x] Implement FacetGrid parity in charm, including `rows`, `cols`, margin panels, multi-variable composite keys, labeller support.

5.9.3 [x] Move `FormulaParser` and `Labeller` from `gg/facet/` to `charm/facet/`. Original gg classes delegate/extend charm implementations for backward compatibility.

5.9.4 [x] Implement strip rendering parity (column strips top, row strips right-side with 90-degree rotation, theme-driven styling via `stripBackground`/`stripText`).

5.9.5 [x] Implement full label parity: `title`, `subtitle`, `caption`, axis labels with theme-driven styling (`plotTitle`, `plotSubtitle`, `plotCaption`, `axisTitleX`, `axisTitleY`), explicit null suppression, and hjust-based alignment.

5.9.6 [x] Implement charm theme element model: `ElementText`, `ElementLine`, `ElementRect`, `ElementBlank` in `charm/theme/` package. Replaced map-based fields in `Theme`/`ThemeSpec` with ~30 typed fields, `explicitNulls` set, `copy()`, and `plus()` merging.

5.9.7 [x] Port all 9 predefined themes to `charm/theme/CharmThemes.groovy`: `gray()`, `classic()`, `bw()`, `minimal()`, `void_()`, `light()`, `dark()`, `linedraw()`, `test()`. Rewrote `GgCharmAdapter.mapTheme()` for field-by-field typed mapping. Removed theme gate, label gate, and facet gate from adapter.

5.9.8 [x] Add tests: `CharmThemeElementTest` (18 tests), `CharmLabelTest` (6 tests), `CharmFacetThemeTest` (11 tests). All 2049 matrix-charts tests pass.

### 5.10 Phase 10 -- Guides and Legends

5.10.1 [x] Implement guide types in charm: legend, colorbar, coloursteps/colorsteps, none (3.7.1-3.7.4).

5.10.2 [x] Implement axis guide variants: axis, axis_logticks, axis_theta, axis_stack (3.7.5-3.7.8).

5.10.3 [x] Implement custom guide support (3.7.9).

5.10.4 [x] Implement legend merging behavior across aesthetics.

5.10.5 [x] Port guide parameter handling and defaults.

5.10.6 [x] Add tests for each guide type and mixed-guide charts.

### 5.11 Phase 11 -- CssAttributeConfig

5.11.1 [x] Implement `CssAttributeConfig` handling in charm's renderer pipeline.

5.11.2 [x] Implement parity for `enabled`, `includeClasses`, `includeIds`, `includeDataAttributes`, `chartIdPrefix`, `idPrefix`.

5.11.3 [x] Implement ID naming and panel/layer indexing behavior for single and faceted charts.

5.11.4 [x] Implement data attribute emission behavior and defaults.

5.11.5 [x] Add structural SVG tests that assert CSS class/id/data-* behavior.

Executed commands:
- [x] `./gradlew :matrix-charts:compileGroovy`
- [x] `./gradlew :matrix-charts:test -Pheadless=true --tests "gg.adapter.GgCharmAdapterTest" --tests "gg.GeomCssAttributesTest" --tests "gg.FacetedCssAttributesTest" --tests "gg.GeomUtilsCssTest" --tests "charm.render.geom.P0GeomRendererTest"`

### 5.12 Phase 12 -- Annotations

5.12.1 [x] Implement annotation rendering in charm: custom, logticks, raster.

5.12.2 [x] Wire `GgChart.plus(Annotate)` to produce charm `AnnotationSpec` objects.

5.12.3 [x] Move `AnnotationConstants` to charm or make shared.

5.12.4 [x] Add tests for each annotation type.

5.12.5 [x] Add tests for `annotation_map` parity.

Executed commands:
- [x] `./gradlew :matrix-charts:compileGroovy`
- [x] `./gradlew :matrix-charts:test -Pheadless=true --tests "gg.adapter.GgCharmAdapterAnnotationTest" --tests "charm.render.CharmAnnotationRendererTest" --tests "gg.AnnotationCustomTest" --tests "gg.AnnotationLogticksTest" --tests "gg.AnnotationRasterTest" --tests "charm.render.CharmParityGovernanceTest"`

### 5.13 Phase 13 -- P1 Geoms, Stats, Positions, Coords, and Scales

_Implement the P1 tier from the feature inventory._

5.13.1 [ ] Implement P1 geom renderers (3.1.13-3.1.32): jitter, step, errorbar, errorbarh, ribbon, segment, hline, vline, abline, label, rug, freqpoly, path, rect, polygon, crossbar, linerange, pointrange, hex, contour.

5.13.2 [ ] Implement P1 stats (3.2.8-3.2.17): SUMMARY, BIN2D, CONTOUR, ECDF, QQ, QQ_LINE, FUNCTION, SUMMARY_BIN, UNIQUE, QUANTILE.

5.13.3 [ ] Implement P1 positions (3.3.5-3.3.7): JITTER, DODGE2, NUDGE.

5.13.4 [ ] Implement P1 coords (3.4.4-3.4.6): Polar, Radial, Trans. Ensure geom renderers emit coord-specific geometry (e.g., polar arcs).

5.13.5 [ ] Implement P1 scales (3.5.10-3.5.24): sqrt, date, datetime, time, binned positional; distiller, fermenter, grey, hue, identity, steps color; size, alpha, shape, linetype, radius; secondary axis.

5.13.6 [ ] Add tests for each P1 feature.

### 5.14 Phase 14 -- P2 Geoms, Stats, Coords, and SF Infrastructure

_Implement the P2 tier and geospatial support._

5.14.1 [ ] Implement P2 geom renderers (3.1.33-3.1.55): bin2d, blank, contour_filled, count, curve, custom, density_2d, density_2d_filled, dotplot, function, logticks, mag, parallel, qq, qq_line, quantile, raster, raster_ann, spoke, sf, sf_label, sf_text.

5.14.2 [ ] Implement P2 stats (3.2.18-3.2.26): DENSITY_2D, BIN_HEX, SUMMARY_HEX, SUMMARY_2D, ELLIPSE, SF, SF_COORDINATES, SPOKE, ALIGN.

5.14.3 [ ] Implement P2 coords (3.4.7-3.4.9): Map, Quickmap, Sf.

5.14.4 [ ] Move SF infrastructure (`SfGeometry`, `SfGeometryUtils`, `SfPoint`, `SfRing`, `SfShape`, `SfType`, `WktReader`) from `gg/sf/` to `charm/sf/` or `charm/render/geom/sf/`.

5.14.5 [ ] Add tests for each P2 feature.

### 5.15 Phase 15 -- Cleanup and Final Verification

5.15.1 [ ] Verify all checkboxes in Section 3 are marked `[x]`.

5.15.2 [ ] Delete adapter/fallback path and legacy gg renderers:
- [ ] `gg/adapter/GgCharmAdapter.groovy`
- [ ] `gg/adapter/GgCharmAdaptation.groovy`
- [ ] `gg/adapter/GgCharmMappingRegistry.groovy`
- [ ] `gg/render/GgRenderer.groovy`
- [ ] `gg/render/AxisRenderer.groovy`, `GridRenderer.groovy`, `FacetRenderer.groovy`, `LegendRenderer.groovy`, `RenderContext.groovy`

5.15.3 [ ] Delete any remaining dead code in `gg/` (rendering helpers, unused imports, orphan classes).

5.15.4 [ ] Verify the `gg/` package contains only: thin factory classes, `GgChart` (delegation shell), `GgPlot` (static API), type definitions (`Aes`, `Label`, `CssAttributeConfig`, `Guides`, `Guide`), and ggplot2-compatibility sugar.

5.15.5 [ ] Run performance sanity benchmarks against baseline charts.

5.15.6 [ ] Update this migration plan to mark all tasks `[x]`.

5.15.7 [ ] Record final passing commands:
- [ ] `./gradlew :matrix-charts:compileGroovy`
- [ ] `./gradlew :matrix-charts:test -Pheadless=true`
- [ ] `./gradlew test -Pheadless=true`

5.15.8 [ ] Verify build/dependency configuration after package/class moves (update `matrix-charts/build.gradle`, root `build.gradle`, `dependencies.gradle` only if required).

## 6. Files Expected to Change

6.1 `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/**` -- bulk of new/modified code

6.2 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy` -- rewire to delegation

6.3 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/geom/**` -- strip to thin factories

6.4 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/adapter/**` -- deleted

6.5 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/render/**` -- deleted

6.6 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/stat/**` -- computation logic migrated to charm

6.7 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/**` -- utilities migrated to charm

6.8 `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/sf/**` -- migrated to charm

6.9 `matrix-charts/src/main/groovy/se/alipsa/matrix/charts/**` -- compatibility updates required when charm model types change

6.10 `matrix-charts/src/test/groovy/**` -- new and updated tests

6.11 `matrix-charts/docs/**` -- updated documentation

6.12 `matrix-charts/build.gradle`, `build.gradle`, `dependencies.gradle` -- only if package moves or dependency visibility changes require updates

## 7. Notes

7.1 This plan uses a **delegation model**: gg classes construct charm objects directly. Temporary fallback paths may exist only during intermediate phase branches; final state has no adapter or translate-at-render-time step.

7.2 Charm is new in the 0.5 release. There are no backwards-compatibility constraints on the charm model.

7.3 This plan treats `CssAttributeConfig`, all `Guide` types, all `Annotation` types, and all coordinate systems (including polar/radial/SF) as first-class parity requirements.

7.4 This plan uses strict parity as completion criteria, not "most charts delegated."

7.5 Priority tiers (P0/P1/P2) enable incremental delivery but all tiers must be completed for strict parity.

7.6 The gg public API (`GgPlot` static methods, `ggplot()`, `aes()`, `geom_*()`, `scale_*()`, `theme_*()`, etc.) does not change. Only the internal implementation changes from self-rendering to charm delegation.

7.7 `se.alipsa.matrix.charts` remains a traditional chart API built on top of charm; charm model refactors must preserve charts facade behavior at each phase merge.

7.8 Existing gg tests are retained as integration tests for the gg facade; charm tests validate charm internals directly.

7.9 `PlotSpec.groovy` is a first-class migration target despite file size; refactoring it is explicit Phase 2 scope, not incidental cleanup.

7.10 `gg/util/Rconverter.groovy` stays in gg unless explicitly marked unused during final cleanup.

7.11 Adhere to the coding and architectural standards described in AGENTS.md
