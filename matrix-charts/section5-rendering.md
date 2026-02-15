# Section 5 Charm Rendering Pipeline (SVG First)

This document records implementation outcomes for Section 5 in `matrix-charts/charm-plan.md`.

## Section 1 Constraint Alignment

- `1.1` (`gg` API stability): Section 5 changes are isolated to `se.alipsa.matrix.charm` render internals and `charm` tests; no `se.alipsa.matrix.gg` public API signature changes were introduced.
- `1.4` (SVG-first standardization): Charm rendering now produces `se.alipsa.groovy.svg.Svg` through a dedicated Charm renderer pipeline.
- `1.5` (DRY): shared render primitives were split into focused classes (`AxisRenderer`, `GridRenderer`, `LegendRenderer`, `FacetRenderer`) and reused by `CharmRenderer`.
- `1.9` (spec conformance): renderer consumes immutable compiled `Chart` objects produced by `PlotSpec.build()` and follows Charm model semantics from `charm-specification.md`.

## 5.1, 5.5, 5.6, 5.8, 5.11 Implemented Charm Renderer Pipeline

Added Charm renderer package:

- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/CharmRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/RenderConfig.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/RenderContext.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/ScaleModel.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/AxisRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/GridRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/LegendRenderer.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/render/FacetRenderer.groovy`

`Chart.render()` now delegates to `CharmRenderer`.

Implemented data-flow stages (explicitly named in renderer):

- `mapData(...)`
- `applyStat(...)`
- `applyPosition(...)`
- `trainScales(...)`
- geom render (`renderPoints`, `renderLines`, `renderBars`, `renderTiles`)
- label/legend/theme application
- final SVG assembly

Pipeline contract:

`data + mappings -> stat -> position -> scale -> coord -> geom -> theme -> svg`

Renderer entrypoints accept immutable compiled `Chart`, not mutable `PlotSpec`.

## 5.2 Migration Assessment (`gg.render`)

Class-by-class classification of `se.alipsa.matrix.gg.render`:

| Class | Classification | Notes |
|---|---|---|
| `GgRenderer` | redesign required | Hardcoded margin/layout constants, direct `GgChart`/`Layer` coupling, and broad orchestration responsibilities. |
| `AxisRenderer` | portable with refactor | Cartesian axis/tick/label generation is portable; polar/radial/secondary-axis logic remains gg-specific and should be adapter-driven before Charm reuse. |
| `GridRenderer` | portable with refactor | Core major/minor grid primitives are portable; gg scale subtype checks and polar/radial coupling need Charm-scale abstraction. |
| `FacetRenderer` | portable with refactor | Strip/axis drawing primitives are reusable, but current signatures depend on gg `Scale`/`Theme` types. |
| `LegendRenderer` | redesign required | Heavy gg-specific guide/type branching, custom guide closures, and legacy constants; reimplemented in Charm with Charm scale/context models. |
| `RenderContext` | redesign required | Mutable cross-layer state (`panelRow`, `panelCol`, `layerIndex`) and gg-specific utility behavior. |

## 5.3, 5.4 Portable Primitive Port + Non-portable Redesign

Portable primitives ported into Charm renderers:

- axis ticks/labels
- grid line rendering
- basic discrete legend keys
- facet panel layout
- clip-path per panel
- theme-driven colors/sizes for axis/grid/text/panel

Redesigned non-portable concerns:

- replaced hardcoded gg renderer constants with `RenderConfig`
- removed gg-domain render signatures in favor of Charm-native context/models
- removed mutable global context behavior from core render flow

## 5.7 `SvgBarChart` Historical Evaluation

Reviewed:

- `matrix-charts/src/main/groovy/se/alipsa/matrix/charts/svg/SvgBarChart.groovy`

Useful reference retained conceptually:

- explicit draw order (canvas -> axes/ticks -> labels -> data)
- imperative plotting flow is easy to trace for debugging

Anti-patterns intentionally not carried into Charm:

- hardcoded styles/colors (for example fixed `'navy'`)
- inline `println` debugging in render path
- incomplete legend positioning implementation
- chart-type-specific renderer without shared GoG abstractions

## 5.9, 5.10 Rendering Tests

Added deterministic Charm renderer tests:

- `matrix-charts/src/test/groovy/charm/render/CharmRendererTest.groovy`

Coverage includes:

- point render primitives (`Circle`, `Line`)
- line render primitives
- bar/histogram rectangle rendering
- facet wrap clip-path count per panel
- deterministic repeated render shape counts
- explicit `RenderConfig` size application

Added gg fixture comparison tests (structural parity, non-fragile):

- `matrix-charts/src/test/groovy/charm/render/CharmRendererFixtureParityTest.groovy`

Coverage compares selected Charm vs gg outputs for:

- point
- line
- histogram

Assertions focus on element-type counts and structural presence, not full XML equality.

## Command Log

- `./gradlew :matrix-charts:compileGroovy`
  - Result: SUCCESS
- `./gradlew :matrix-charts:test --tests "charm.render.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 8 tests, 8 passed, 0 failed, 0 skipped
- `./gradlew :matrix-charts:test -Pheadless=true`
  - Result: SUCCESS
  - Summary: 1731 tests, 1731 passed, 0 failed, 0 skipped
