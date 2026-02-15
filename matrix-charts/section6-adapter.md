# Section 6 GG -> Charm Adapter Layer

This document records implementation outcomes for Section 6 in `matrix-charts/charm-plan.md`.

## Section 1 Constraint Alignment

- `1.1` API stability: `se.alipsa.matrix.gg` signatures remain unchanged; delegation is internal and fallback-safe.
- `1.4` SVG-first rendering: adapter can render delegated gg charts through Charm SVG renderer; runtime delegation is currently opt-in to preserve parity safety.
- `1.5` DRY: `GgChart.render()` now routes through one adapter bridge instead of directly instantiating gg renderer.
- `1.9` spec conformance: adapter target is Charm compiled `Chart` model, with explicit fallback when semantics are not yet parity-safe.

## 6.1, 6.2 Adapter Package and Architecture

Added adapter package under `gg`:

- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/adapter/GgCharmAdapter.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/adapter/GgCharmMappingRegistry.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/adapter/GgCharmAdaptation.groovy`

Architecture:

- Registry/strategy mapping (`GgCharmMappingRegistry`) converts gg type families into canonical parameterized Charm targets.
- Adapter (`GgCharmAdapter`) performs compatibility checks and either:
  - builds a Charm `Chart` and renders with `CharmRenderer`, or
  - falls back to legacy `GgRenderer` with explicit reasons.

## 6.3, 6.4 Canonical Targets and Mapping Matrix

| Surface | gg input | Charm target | Status |
|---|---|---|---|
| Geom | `GeomPoint` | `Geom.POINT` | delegated |
| Geom | `GeomLine` | `Geom.LINE` | delegated |
| Geom | `GeomSmooth` | `Geom.SMOOTH` | delegated (method=`lm` only) |
| Geom | `GeomTile` | `Geom.TILE` | mapped, fallback currently |
| Geom | `GeomBar` | `Geom.BAR` | delegated for stat=`identity`; fallback for default stat=`count` |
| Geom | `GeomCol` | `Geom.COL` | delegated |
| Geom | `GeomHistogram` | `Geom.HISTOGRAM` | delegated (x-only mapping) |
| Geom | `GeomBoxplot` | `Geom.BOXPLOT` | mapped, fallback currently |
| Stat | `StatType.IDENTITY` | `Stat.IDENTITY` | supported |
| Stat | `StatType.SMOOTH` | `Stat.SMOOTH` | delegated for smooth layer |
| Position | `IDENTITY` | `Position.IDENTITY` | supported |
| Position | `STACK`, `DODGE` | `Position.STACK`, `Position.DODGE` | stack delegated for `BAR/COL`; dodge fallback |
| Coord | `CoordCartesian` | `CoordType.CARTESIAN` | supported |
| Coord | `CoordPolar` | `CoordType.POLAR` | mapped, fallback currently |
| Scale (x/y) | continuous/discrete/log10/sqrt/reverse/date/time families | `Scale` (`CONTINUOUS`/`DISCRETE`/`TRANSFORM`/`DATE`) | mapped in registry |
| Scale (color/fill) | discrete families | `Scale.discrete()` | mapped in registry |
| Theme | gg theme model | Charm `Theme` sections (`legend/axis/text/grid/raw`) | default-gray delegated; non-default themes fallback |

Compatibility policy in v1 adapter:

- Delegate parity-safe subset (point/line/smooth(`lm`)/histogram/col and bar identity cases) with default gray theme and no gg-specific extras.
- Fallback for unsupported or not-yet-parity-safe constructs (guides, facets, explicit scales, non-default themes, advanced geoms/stats/coords).

## 6.5, 6.6 Render Entrypoint Delegation with Stable GG API

Updated:

- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy`

`GgChart.render()` now delegates through `GgCharmAdapter.render(this)` while preserving existing method signature and default coord initialization behavior.

Runtime policy:

- Default behavior uses legacy gg rendering unless delegation is explicitly enabled.
- Enable delegated rendering with either:
  - JVM property: `-Dmatrix.charts.gg.delegateToCharm=true`
  - Env var: `MATRIX_GG_DELEGATE_TO_CHARM=true`

`GgPlot` static factories were left unchanged.

## 6.7 Modernized Switch Syntax

Modernized to Groovy 5 switch expressions in:

- `parseStatType()` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy`
- `parsePositionType()` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy`

## 6.8, 6.9, 6.10 DRY + Parity Validation

Added adapter tests:

- `matrix-charts/src/test/groovy/gg/adapter/GgCharmAdapterTest.groovy`

Coverage includes:

- registry canonical mapping behavior,
- delegated point/line/smooth/histogram/col behavior,
- documented fallback behavior for non-parity-safe constructs (`geom_bar` count stat, unsupported smooth methods, non-default theme),
- gg adapter vs native Charm model parity for equivalent point semantics.

## Command Log

- `./gradlew :matrix-charts:compileGroovy`
  - Result: SUCCESS
- `./gradlew :matrix-charts:test --tests "gg.adapter.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 5 tests, 5 passed, 0 failed, 0 skipped
- `./gradlew :matrix-charts:test --tests "gg.GgPlotTest.testHistogram" --tests "gg.GgPlotTest.testScatterWithSmooth" -Pheadless=true`
  - Result: SUCCESS
  - Verified via `matrix-charts/build/test-results/test/TEST-gg.GgPlotTest.xml`:
    - tests=2, failures=0, errors=0, skipped=0
- `./gradlew :matrix-charts:test -Pheadless=true`
  - Result: started, then manually terminated in this environment after prolonged no-output executor run (process remained active, no XML summary produced).
