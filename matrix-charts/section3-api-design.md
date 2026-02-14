# Section 3 Charm API Specification Implementation

This document records implementation artifacts for Section 3 in `matrix-charts/charm-plan.md`.

## 3.1 Naming Strategy

Charm public API now uses concise domain names in `se.alipsa.matrix.charm`:

- `Chart`
- `PlotSpec`
- `Layer`
- `Geom`
- `Scale` / `ScaleSpec`
- `Coord`
- `Facet`
- `Theme`
- `Aes`
- `Stat`
- `Position`

Internal and typed-spec names use suffixes where clarity matters (`PlotSpec`, `ScaleSpec`, `AnnotationSpec`).

## 3.2 Import Ambiguity Policy

Documented and validated options:

- Alias imports:
  - `import se.alipsa.matrix.charm.Chart as CharmChart`
  - `import se.alipsa.matrix.charts.Chart as LegacyChart`
- Static entry points:
  - `import static se.alipsa.matrix.charm.Charts.plot`
  - `import static se.alipsa.matrix.charm.Charts.chart`
- Fully qualified names remain fallback.

Validation test:

- `matrix-charts/src/test/groovy/charm/api/CharmApiDesignTest.groovy` (`testImportAliasStrategyCompilesAndRuns`)

## 3.3 Render/Output Contract

`se.alipsa.matrix.charm.Chart.render()` returns `se.alipsa.groovy.svg.Svg` objects.

No SVG-string rendering API is used as the core output contract.

## 3.4 Groovy API Conventions

Charm DSL uses:

- camelCase names
- closure configuration (`aes {}`, `points {}`, `theme {}`)
- map/named-argument forms (`aes(x: 'cty', y: 'hwy')`, `layer(Geom.POINT, [size: 2])`)

Charm does not introduce R-style underscore factory naming.

## 3.5 Package Parity vs gg Structure

Section 3 establishes the concept parity model around:

- aesthetics (`Aes`)
- stats (`Stat`)
- positions (`Position`)
- coordinates (`Coord`)
- facets (`Facet`)
- geoms/layers (`Geom`, `Layer`)
- scales (`Scale`, `ScaleSpec`)
- themes (`Theme`)
- annotations (`AnnotationSpec`)

This matches the core GoG concepts required for migration from `gg`.

## 3.6 Stable Public Package Layout (v1)

Current Section 3 implementation introduces the canonical entrypoint package:

- `se.alipsa.matrix.charm`

Planned subpackage split for implementation sections 4-8 remains:

- `aes`, `coord`, `facet`, `geom`, `layer`, `position`, `render`, `scale`, `sf`, `stat`, `theme`, `dsl`, `util`

## 3.7 gg -> Charm Conversion Contract (Design Baseline)

Section 3 keeps `gg` as the user-facing compatibility surface and establishes Charm as the canonical model target.

Adapter details are implemented in Section 6, but Section 3 locks the rule:

- gg syntax compiles to Charm canonical model semantics (not separate engine logic).

## 3.8 Charm MVP Scope Baseline

MVP scope defined for first cutover:

- point
- line
- bar/col
- histogram
- boxplot
- smooth
- facets (grid default, wrap opt-in)
- major scales
- theme basics
- legend basics

## 3.9 API Usage Examples

Primary closure DSL:

```groovy
import static se.alipsa.matrix.charm.Charts.plot

def spec = plot(mpg) {
  aes {
    x = col.cty
    y = col.hwy
    color = col['class']
  }
  points {
    size = 2
    alpha = 0.7
  }
  smooth {
    method = 'lm'
  }
  labels {
    title = 'City vs Highway MPG'
  }
}

def chart = spec.build()
def svg = chart.render() // Svg
```

gg compatibility syntax (unchanged entrypoint):

```groovy
import static se.alipsa.matrix.gg.GgPlot.*

def gg = ggplot(mpg, aes(x: 'cty', y: 'hwy', colour: 'class')) +
    geom_point(size: 2, alpha: 0.7) +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG')

def svg = gg.render() // Svg
```

## 3.10 API Design Tests

Added:

- `matrix-charts/src/test/groovy/charm/api/CharmApiDesignTest.groovy`

Coverage:

- closure DSL example build/render
- scale/theme/coord closure examples
- static-safe `col['name']` usage under `@CompileStatic`
- import aliasing strategy (`CharmChart` vs legacy `Chart`)
- gg secondary syntax still rendering

## 3.11 Charm Aes Model

Section 3 uses Charm-native typed aesthetic mapping model:

- `Aes`
- `ColumnExpr`
- `ColumnRef`
- `Cols`

`gg.aes.Aes` is not reused as Charm core type.

## 3.12 Thread-Safety Default

Section 3 implementation uses explicit per-chart state with immutable compiled chart objects:

- mutable `PlotSpec` -> immutable `Chart`
- no global mutable theme state introduced

## 3.13 Error Handling Strategy

Added Charm domain exceptions:

- `CharmException`
- `CharmValidationException`
- `CharmMappingException`
- `CharmRenderException`

## 3.14 Spec Lock

Section 3 implementation follows `matrix-charts/charm-specification.md` as v1 source of truth.
Any behavior not yet implemented in Section 3 is deferred to subsequent sections, not redefined.

## 3.15 gg Facade Requirement

Validation included in API tests that `se.alipsa.matrix.gg.GgPlot` syntax continues to render.
Section 6 will route that syntax through Charm internals while preserving the public API.

## Command Log

- `./gradlew :matrix-charts:test --tests "charm.api.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 6 tests, 6 passed, 0 failed, 0 skipped
- `./gradlew :matrix-charts:test -Pheadless=true`
  - Result: SUCCESS
  - Summary: 1703 tests, 1703 passed, 0 failed, 0 skipped
