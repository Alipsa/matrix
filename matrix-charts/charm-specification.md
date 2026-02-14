# Charm v1 API Specification

**Canonical Model Immediately + gg Compatibility Wrapper**

---

## 1. Overview

Charm is a Groovy-native Grammar of Graphics implementation for the Matrix ecosystem.

Charm defines a canonical, typed specification model (`PlotSpec`) that is compiled into an immutable `Chart`, which can then be rendered (primary API) and written to disk (convenience API).

All plotting logic (geoms, stats, scales, themes, facets, coordinate systems, layout, and rendering) lives in `se.alipsa.matrix.charm`.

The existing `se.alipsa.matrix.gg` API remains as a thin compatibility wrapper that delegates entirely to Charm. Users keep their existing entry point:

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
```

There is exactly one engine.

---

## 2. Goals

Charm v1 aims to:

1. Establish a canonical specification model (`PlotSpec`) immediately.
2. Move the entire existing gg engine implementation into the Charm package.
3. Provide a Groovy-idiomatic DSL that feels natural for Groovy analysts.
4. Preserve backward compatibility via a thin `se.alipsa.matrix.gg` wrapper over Charm.
5. Use static compilation internally for correctness and performance.
6. Provide a deterministic lifecycle: spec → build → render → write.
7. Avoid duplicated engine logic across packages.

---

## 3. Non-Goals (v1)

Charm v1 does not aim to:

* Maintain separate engine logic in gg.
* Support multiple competing DSL grammars.
* Allow method-style DSL (`x 2`) alongside property assignment (`x = 2`).
* Introduce dynamic tricks that undermine correctness under static compilation.
* Guarantee ggplot2 parity beyond what the engine supports.

---

## 4. Core Grammar Rules

### Rule 1 — Inside closures → use property assignment

Inside DSL blocks, configuration MUST use `=`:

```groovy
text {
  x = 2
  y = 12
  label = 'Peak'
}
```

Method DSL (`x 2`) is not supported in v1.

---

### Rule 2 — Outside closures → use named arguments

Named argument maps use `:` inside parentheses:

```groovy
text(x: 2, y: 12)
aes(x: 'cty', y: 'hwy')
```

Colon syntax MUST NOT appear inside DSL closures.

---

### Rule 3 — Column references use `col`

Inside `aes {}` and other mapping locations, column references SHOULD use `col`:

```groovy
aes {
  x = col.cty
  y = col.hwy
  color = col['class']
}
```

Strings are allowed but discouraged:

```groovy
x = 'cty'
```

---

## 5. Canonical Object Model

The following model is the authoritative internal representation for the Charm engine.

---

### 5.1 PlotSpec (Root)

Represents the full plot specification.

**Fields:**

* `Matrix data`
* `AesSpec aes`
* `List<LayerSpec> layers`
* `ScaleSpec scale`
* `ThemeSpec theme`
* `FacetSpec facet`
* `CoordSpec coord`
* `LabelsSpec labels`
* `List<AnnotationSpec> annotations`

**Responsibilities:**

* Collect DSL input
* Enforce typed structure
* Validate configuration
* Compile into immutable `Chart`

---

### 5.2 AesSpec

Represents aesthetic mappings.

**Fields:**

* `ColumnExpr x`
* `ColumnExpr y`
* `ColumnExpr color`
* `ColumnExpr fill`
* `ColumnExpr size`
* `ColumnExpr shape`
* `ColumnExpr group`

#### Aesthetic Coercion (explicit and deterministic)

Accepted inputs:

* `ColumnExpr`
* `CharSequence` → coerced to `ColumnName`

Coercion occurs at specification time (via setters).

Invalid inputs MUST throw `IllegalArgumentException`.

Examples:

```groovy
aes { x = col.cty }     // ColumnExpr
aes { x = 'cty' }       // CharSequence → ColumnName('cty')
aes(x: 'cty', y: 'hwy') // map form uses identical coercion logic
```

---

### 5.3 ColumnExpr & Static Compilation

Charm core is implemented with `@CompileStatic`.

Column access rules:

* `col['name']` → canonical and static-safe
* `col.name` → convenience form intended for dynamic DSL usage

Users writing `@CompileStatic` chart-building code MUST use bracket syntax.

Column name collisions MUST use bracket syntax:

```groovy
col['class']
```

`col.name` is permitted as ergonomic sugar in normal Groovy scripts/DSL closures.

---

### 5.4 LayerSpec

Represents a geometry/stat layer.

**Fields:**

* `Geom geom`
* `Stat stat`
* `AesSpec aes` (optional override)
* `boolean inheritAes = true`
* `Position position`
* `Map<String,Object> params`

#### Inheritance rule

Layers inherit plot-level aesthetics unless:

* overridden in layer `aes {}` or `aes(...)`
* `inheritAes = false`

#### smooth {} mapping

```groovy
smooth { method = 'lm' }
```

Compiles to:

* `geom = SMOOTH`
* `params.method = 'lm'`
* appropriate stat selection (implementation-defined but semantically stable)

---

### 5.5 ScaleSpec (Minimal v1)

Represents scale configuration.

**Fields:**

* `Scale x`
* `Scale y`
* `Scale color`
* `Scale fill`

Each `Scale` contains:

* `ScaleType type` (CONTINUOUS, DISCRETE, DATE, TRANSFORM)
* `Transform transform`
* `List<?> breaks`
* `List<String> labels`
* `Map<String,Object> params`

**DSL example:**

```groovy
scale {
  x = log10()
  y = sqrt()
}
```

Implementation details may vary, but the semantic model is fixed.

---

### 5.6 ThemeSpec (Minimal v1)

Represents styling configuration.

**Fields:**

* `LegendTheme legend`
* `AxisTheme axis`
* `TextTheme text`
* `GridTheme grid`
* `Map<String,Object> raw` (compatibility fallback)

**DSL example:**

```groovy
theme {
  legend { position = 'top' }
  axis { lineWidth = 0.75 }
}
```

---

### 5.7 FacetSpec (Updated: GRID default, WRAP opt-in)

Represents faceting configuration.

**Fields:**

* `FacetType type` (NONE, GRID, WRAP)
* `List<ColumnExpr> rows` (GRID)
* `List<ColumnExpr> cols` (GRID)
* `List<ColumnExpr> vars` (WRAP)
* `Integer ncol` (WRAP)
* `Integer nrow` (WRAP)
* `Map<String,Object> params`

#### Facet DSL defaults

Within `facet {}`, specifying `rows` and/or `cols` configures **GRID** faceting by default:

```groovy
facet {
  rows = [col.year, col.cyl]
  cols = [col.drv]
}
```
This is functionally the same as
```groovy
facet {
  grid {
    rows = [col.year, col.cyl]
    cols = [col.drv]
  }
}
```
This is equivalent to “facet_grid” in the gg ggplot2 style API.

#### Explicit WRAP faceting

To request wrap faceting, users MUST call `wrap{...}` inside `facet {}`:

```groovy
facet {
  wrap {
    vars = [col.year]
    ncol = 3
  }
}
```

#### Conflict policy

Combining wrap configuration with grid settings in the same `facet {}` block is invalid and MUST throw an error:

* If `wrap { ... }` is used after `rows/cols` were set → error
* If `rows/cols` are set after `wrap { ... }` was used → error

#### Formula support (gg wrapper parity)

The gg wrapper may accept formula strings such as:

* `'year ~ .'`, `'year ~'`
* `'. ~ drv'`, `'~ drv'`
* `'year + cyl ~ drv'`
* `'a + b ~ c + d'`

During `build()`, these MUST compile into the canonical `rows` / `cols` lists.

`.` and missing sides map to empty lists.

---

### 5.8 CoordSpec

Represents coordinate systems / transformations.

**Fields:**

* `CoordType type` (CARTESIAN, POLAR, …)
* `Map<String,Object> params`

**Charm DSL:**

```groovy
coord {
  type = 'polar'
  theta = 'y'
  start = 0
}
```

---

### 5.9 LabelsSpec

Represents labels and guide titles.

**Fields:**

* `String title`
* `String subtitle`
* `String caption`
* `String x`
* `String y`
* `Map<String,String> guides` (e.g., `[color:'Species']`)

**DSL:**

```groovy
labels {
  title = 'MPG'
  x = 'City MPG'
  y = 'Highway MPG'
}
```

---

### 5.10 AnnotationSpec

Typed annotation hierarchy.

v1 includes:

* `TextAnnotationSpec`
* `RectAnnotationSpec`
* `SegmentAnnotationSpec`

Annotations are compiled into renderable elements during `build()`.

---

## 6. Data Model

Primary input:

* `se.alipsa.matrix.core.Matrix`

Convenience inputs MAY include:

* `List<Map<String, ?>>`
* `Map<String, List<?>>`
* `Iterable<POJO>`

All convenience inputs MUST be converted internally to `Matrix` before compilation.

Column validation SHOULD occur during `build()` and MUST produce actionable errors.

---

## 7. Lifecycle

Charm defines four steps: Specification → Build → Render → Write.

### 7.1 Specification (Mutable)

```groovy
def spec = plot(data) { ... }
```

### 7.2 Compilation (Immutable)

```groovy
Chart chart = spec.build()
```

Build MUST:

* validate mappings and facet variables
* apply defaults
* resolve inheritance
* freeze an immutable chart model

### 7.3 Rendering (Primary API)

```groovy
Svg svg = chart.render()
```

`render()` is the canonical rendering API.

### 7.4 Writing (Convenience API)

```groovy
chart.writeTo('plot.svg')
```

`writeTo()` MUST internally call `render()` and delegate to a writer (e.g., `SvgWriter`).

---

## 8. Example (Charm DSL)

```groovy
plot(mpg) {

  aes {
    x = col.cty
    y = col.hwy
    color = col['class']
  }

  points { size = 2 }

  smooth { method = 'lm' }

  facet {
    rows = [col.year]
    cols = [col.drv]
  }

  coord {
    type = 'polar'
    theta = 'y'
    start = 0
  }

  labels { title = 'MPG' }
}
```

Wrap example:

```groovy
facet {
  wrap {
    vars = [col.year]
    ncol = 3
  }
}
```

---

## 9. Static Compilation & IDE Requirements

Charm core MUST:

* be implemented with `@CompileStatic`
* use `@DelegatesTo` and `DELEGATE_ONLY` for DSL closures
* avoid uncontrolled dynamic dispatch

DSL closures are normally used dynamically by end users; users opting into `@CompileStatic` must use `col['name']` instead of `col.name`.

---

## 10. gg Compatibility Wrapper (Preserve existing entry point)

The `se.alipsa.matrix.gg` module remains and preserves the existing public API surface.

### 10.1 Architecture

* `se.alipsa.matrix.gg.GgPlot` acts as the entry point for `import static ...GgPlot.*`
* gg functions delegate to Charm, producing and modifying `PlotSpec`
* no engine logic (geoms/stats/scales/rendering) remains in gg

### 10.2 Entry point preservation example

User code remains valid:

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def chart = ggplot(Dataset.mtcars, aes(factor(1), fill: 'cyl')) +
            geom_bar(width: 1) +
            coord_polar(theta: "y", start: 0)

ggsave(chart, "barchart.svg")
```

Mapping:

* `ggplot(...)` creates a `PlotSpec`
* `geom_bar(...)` creates a `LayerSpec`
* `coord_polar(...)` modifies `CoordSpec`
* `ggsave(...)` calls `chart.render()` + writer

### 10.3 Scope

The gg wrapper MUST preserve the full existing public API of `se.alipsa.matrix.gg.GgPlot`.

Additional gg functions MAY be added incrementally, but the wrapper must never contain duplicated engine logic.

---

## 11. Extensibility Principles

Future features MUST:

* extend the canonical object model
* respect grammar rules
* preserve a single compilation pipeline
* maintain backward compatibility through the gg wrapper

---

## Final Summary

Charm v1 commits to:

* `PlotSpec` as canonical model immediately
* full engine located in `se.alipsa.matrix.charm`
* immutable `Chart` compiled via `build()`
* `render()` as primary rendering API, `writeTo()` as convenience
* faceting defaults to GRID when `rows/cols` are used, WRAP via `wrap { ... }`
* formulas supported via gg wrapper and compiled into canonical row/col vars
* `se.alipsa.matrix.gg.GgPlot` remains entry point as a thin wrapper over Charm

One engine. One model. Two syntaxes.
