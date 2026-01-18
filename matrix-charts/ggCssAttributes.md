# Adding CSS Classes and IDs to gg Package SVG Output
The gg package, which is an implementation of ggplot2 renders to Svg. In order to add styles to the Svg it would be easier if the Svg had classes and id's to facilitate a high level of detailed        
customisation beyond what can be done with Aes definition. The following is a plan to add that without braking any of the ggplot2 compatibility.

## Executive Summary

Adding CSS classes and IDs to individual SVG elements in the gg package is **fully feasible** and **would not break ggplot2 compatibility**. The gsvg library already supports these features, and the codebase already uses them for structural elements. R's ggplot2 does not define CSS conventions for SVG output, so there are no compatibility constraints.

---

## 1. Current State of CSS Support

### Already Implemented (Structural Level)

The gg package already uses `id()` and `styleClass()` for major structural groups:

| Element | ID | Location |
|---------|-----|----------|
| Plot area | `plot-area` | GgRenderer.groovy:175 |
| Data layer | `data-layer` | GgRenderer.groovy:186 |
| Axes container | `axes` | AxisRenderer.groovy:45 |
| X-axis | `x-axis` | AxisRenderer.groovy:558 |
| Y-axis | `y-axis` | AxisRenderer.groovy:649 |
| Grid | `grid` | GridRenderer.groovy:44 |
| Legend | `legend` | LegendRenderer.groovy:138 |
| Facet panels | `panel-{row}-{col}` | GgRenderer.groovy:299 |
| Facet data layers | `data-layer-{row}-{col}` | GgRenderer.groovy:344 |
| Clip path | `plot-clip` | GgRenderer.groovy:189 |

**CSS Classes already applied:**
- Layer groups: `geompoint`, `geombar`, `geomline`, etc. (based on geom type)
- Chart title: `chart-title`
- Boxplot groups: `geom-boxplot`

### Not Currently Implemented (Element Level)

Individual data elements (circles, rects, lines, paths) receive **direct style attributes** but no CSS classes or IDs:

```groovy
// Current pattern in GeomBar.groovy:135-150
def rect = group.addRect(barWidth, heightPx)
    .x(xPx)
    .y(yPx)
    .fill(barFill)
// No .id() or .styleClass() call
```

---

## 2. gsvg Library Capabilities

The gsvg library (version 1.0.0) fully supports CSS classes and IDs on all SVG elements:

```groovy
// Available methods on all SVG elements:
element.id('unique-id')           // Sets id attribute
element.styleClass('css-class')   // Sets class attribute
element.addAttribute('key', val)  // Sets any attribute
```

**Evidence from SvgBarChart.groovy** (lines 178-180) showing the pattern already works:
```groovy
graph.addRect(barWidth, val)
    .styleClass("bar")
    .id("bar-$seriesIdx-$valueIdx")
    .fill('navy')
```

---

## 3. ggplot2 R Compatibility Analysis

### R ggplot2 SVG Output
- R's ggplot2 outputs SVG via `ggsave()` or graphics devices like `svglite`
- **No standardized CSS class/ID conventions exist** in ggplot2's SVG output
- SVG is typically treated as a static image format in R

### ggiraph Extension
The [ggiraph package](https://davidgohel.github.io/ggiraph/index.html) adds interactivity to ggplot2 using custom SVG attributes:
- `data_id` - Element identification for hover/click
- `tooltip` - Hover text
- `onclick` - JavaScript callbacks

This approach uses **data attributes** rather than CSS classes, but both approaches are valid.

### Compatibility Verdict
**No compatibility concerns.** Since R ggplot2 doesn't define CSS conventions, the matrix gg package is free to define its own naming scheme.

---

## 4. Proposed Element-Level Naming Scheme

### CSS Classes (for styling by type)

| Element Type | Suggested Class | Usage |
|--------------|-----------------|-------|
| Point | `gg-point` | All scatter plot points |
| Bar | `gg-bar` | Bar chart rectangles |
| Line segment | `gg-line` | Line chart segments |
| Area | `gg-area` | Area chart paths |
| Text label | `gg-label` | Data labels |
| Axis tick | `gg-tick` | Axis tick marks |
| Axis label | `gg-axis-label` | Axis text labels |
| Grid line | `gg-grid-major` / `gg-grid-minor` | Grid lines |

### IDs (for specific element targeting)

**CSS-selector-friendly, always starts with a letter.**
Format (default):
`gg-panel-{row}-{col}-layer-{layer}-{geom}-{element}`

Examples:
- `gg-panel-0-0-layer-0-point-42` - 43rd point in first layer, single panel
- `gg-panel-1-2-layer-0-bar-5` - 6th bar in first layer of panel (1,2)

**Multi-chart pages (optional prefix):**
Format:
`chart-{chart-prefix}-panel-{row}-{col}-layer-{layer}-{geom}-{element}`

Examples:
- `chart-iris-panel-0-1-layer-0-line-12` - With `chartIdPrefix: 'iris'`

**Prefix normalization:**
- Lowercase
- Replace whitespace with `-`
- Strip characters outside `[a-z0-9-]`

### Data Attributes (for interactivity, stat-output oriented)

```groovy
element.addAttribute('data-x', xValue)        // Stat output x used for rendering
element.addAttribute('data-y', yValue)        // Stat output y used for rendering
element.addAttribute('data-row', rowIndex)    // Stat output row index (0-based)
element.addAttribute('data-panel', 'r-c')     // Panel coordinates as "row-col"
element.addAttribute('data-layer', layerIdx)  // Layer index (0-based)
element.addAttribute('data-group', group)     // Optional: group value if present
```

---

## 5. Example of Enhanced SVG Output

### Current output:
```xml
<g id="data-layer">
  <g class="geompoint">
    <circle cx="100" cy="200" r="3" fill="#F8766D"/>
    <circle cx="150" cy="180" r="3" fill="#F8766D"/>
  </g>
</g>
```

### Enhanced output (single panel):
```xml
<g id="data-layer">
  <g class="geompoint">
    <circle class="gg-point" id="point-0-0" data-x="4" data-y="21"
            cx="100" cy="200" r="3" fill="#F8766D"/>
    <circle class="gg-point" id="point-0-1" data-x="6" data-y="18"
            cx="150" cy="180" r="3" fill="#F8766D"/>
  </g>
</g>
```

### Enhanced output (faceted chart, panel 0-1):
```xml
<g id="data-layer-0-1">
  <g class="geompoint">
    <circle class="gg-point" id="0-1-point-0-0" data-x="4" data-y="21"
            cx="100" cy="200" r="3" fill="#F8766D"/>
    <circle class="gg-point" id="0-1-point-0-1" data-x="6" data-y="18"
            cx="150" cy="180" r="3" fill="#F8766D"/>
  </g>
</g>
```

---

## 6. Benefits of Implementation

1. **CSS Stylesheet Support** - External stylesheets can override inline styles
2. **JavaScript Interactivity** - Easy element selection via `document.querySelectorAll('.gg-point')`
3. **Accessibility** - Screen readers can better interpret chart structure
4. **Testing** - Simpler assertions on specific elements
5. **Theming** - CSS-based themes separate from Groovy code
6. **Animation** - CSS animations and transitions become possible

---

## 7. Implementation Plan

### 7.1 Infrastructure Setup

- [ ] 7.1.1 Create `CssAttributeConfig` class in `se.alipsa.matrix.gg` package to hold configuration options:
  - `enabled` (boolean) - Master toggle for CSS attributes
  - `includeClasses` (boolean) - Whether to add CSS classes
  - `includeIds` (boolean) - Whether to add element IDs
  - `includeDataAttributes` (boolean) - Whether to add data-* attributes
  - `chartIdPrefix` (String) - Optional prefix for multi-chart pages (null by default)
  - `idPrefix` (String) - Default prefix when `chartIdPrefix` is null (`'gg'`)
- [ ] 7.1.2 Add `cssAttributes` property to `GgChart` to allow per-chart configuration
- [ ] 7.1.3 Create `RenderContext` extension to carry CSS config and panel info through the rendering pipeline:
  - Add `cssConfig` property to existing `RenderContext` class
  - Add `panelRow` and `panelCol` properties (null for single-panel charts)
  - Add `layerIndex` property (set by GgRenderer before each layer render)
  - Ensure `GgRenderer` resets panel/layer state per render to avoid stale context
- [ ] 7.1.4 Create utility methods in `GeomUtils` for CSS attribute handling:
  - `generateElementId(RenderContext ctx, String geomType, int elementIndex)` - generates CSS-safe ID
  - `normalizeIdPrefix(String prefix)` - normalize prefix per naming scheme
  - `normalizeIdToken(String token)` - normalize geom/type tokens for IDs
  - `applyCssAttributes(SvgElement element, RenderContext ctx, String geomType, String cssClass, int elementIndex)` - applies class and ID
  - `applyDataAttributes(SvgElement element, RenderContext ctx, Map rowData, String xCol, String yCol, String groupCol, int statRowIndex)` - applies data-* attributes when enabled
  - `applyDataAttributes(SvgElement element, RenderContext ctx, Aes aes, Map rowData, int statRowIndex)` - overload for convenience
- [ ] 7.1.5 Modernize switch statements in `GeomUtils.groovy` to use arrow syntax (per AGENTS.md guidelines)

### 7.2 GeomUtils.drawPoint() Refactoring

The current `drawPoint()` method returns `void` and creates multiple elements for compound shapes (plus, cross, x). This needs refactoring:

- [ ] 7.2.1 Refactor `GeomUtils.drawPoint()` to return the created element(s):
  - For simple shapes (circle, square, triangle, diamond): return the single element
  - For compound shapes (plus, cross, x): wrap in a `<g>` group and return the group
  - Add `RenderContext` parameter for CSS config access
  - Add `elementIndex` parameter for ID generation
- [ ] 7.2.2 Add overloaded `drawPoint()` that accepts `RenderContext` and `elementIndex` for CSS support
- [ ] 7.2.3 Keep backward-compatible `drawPoint()` signature that delegates to new method with null context

### 7.3 Core Geom Updates

- [ ] 7.3.1 Update `GeomPoint.render()` to track element index and pass `RenderContext` to `GeomUtils.drawPoint()`
- [ ] 7.3.2 Update `GeomBar.render()` to add `.styleClass('gg-bar')` and `.id()` to rect elements
- [ ] 7.3.3 Ensure `GeomCol` inherits CSS changes from `GeomBar` (only update if it overrides render)
- [ ] 7.3.4 Update `GeomLine.render()` to add `.styleClass('gg-line')` and `.id()` to line/path elements
- [ ] 7.3.5 Update `GeomPath.render()` to add `.styleClass('gg-path')` and `.id()` to path elements
- [ ] 7.3.6 Update `GeomArea.render()` to add `.styleClass('gg-area')` and `.id()` to area paths
- [ ] 7.3.7 Update `GeomPolygon.render()` to add `.styleClass('gg-polygon')` and `.id()` to polygon paths
- [ ] 7.3.8 Update `GeomRect.render()` to add `.styleClass('gg-rect')` and `.id()` to rect elements
- [ ] 7.3.9 Update `GeomTile.render()` to add `.styleClass('gg-tile')` and `.id()` to tile elements
- [ ] 7.3.10 Update `GeomSegment.render()` to add `.styleClass('gg-segment')` and `.id()` to segment lines
- [ ] 7.3.11 Update `GeomStep.render()` to add `.styleClass('gg-step')` and `.id()` to step lines

### 7.4 Statistical Geom Updates

- [ ] 7.4.1 Update `GeomHistogram.render()` to add `.styleClass('gg-histogram-bar')` and `.id()` to histogram bars
- [ ] 7.4.2 Update `GeomBoxplot.render()` to add CSS classes for box (`gg-boxplot-box`), whiskers (`gg-boxplot-whisker`), median (`gg-boxplot-median`), outliers (`gg-boxplot-outlier`)
- [ ] 7.4.3 Update `GeomViolin.render()` to add `.styleClass('gg-violin')` and `.id()` to violin paths
- [ ] 7.4.4 Update `GeomDensity.render()` to add `.styleClass('gg-density')` and `.id()` to density paths
- [ ] 7.4.5 Update `GeomDensity2d.render()` to add `.styleClass('gg-density2d')` and `.id()` to contour paths
- [ ] 7.4.6 Update `GeomDensity2dFilled.render()` to add `.styleClass('gg-density2d-filled')` and `.id()` to filled contour paths
- [ ] 7.4.7 Update `GeomSmooth.render()` to add `.styleClass('gg-smooth')` and `.id()` to smooth lines and ribbons
- [ ] 7.4.8 Update `GeomRibbon.render()` to add `.styleClass('gg-ribbon')` and `.id()` to ribbon paths
- [ ] 7.4.9 Update `GeomErrorbar.render()` to add `.styleClass('gg-errorbar')` and `.id()` to errorbar elements
- [ ] 7.4.10 Update `GeomErrorbarh.render()` to add `.styleClass('gg-errorbarh')` and `.id()` to horizontal errorbar elements
- [ ] 7.4.11 Update `GeomCrossbar.render()` to add `.styleClass('gg-crossbar')` and `.id()` to crossbar elements
- [ ] 7.4.12 Update `GeomPointrange.render()` to add `.styleClass('gg-pointrange')` and `.id()` to pointrange elements
- [ ] 7.4.13 Update `GeomLinerange.render()` to add `.styleClass('gg-linerange')` and `.id()` to linerange elements

### 7.5 Text and Label Geom Updates

- [ ] 7.5.1 Update `GeomText.render()` to add `.styleClass('gg-text')` and `.id()` to text elements
- [ ] 7.5.2 Update `GeomLabel.render()` to add `.styleClass('gg-label')` and `.id()` to label elements

### 7.6 Reference Line Geom Updates

- [ ] 7.6.1 Update `GeomAbline.render()` to add `.styleClass('gg-abline')` and `.id()` to abline elements
- [ ] 7.6.2 Update `GeomHline.render()` to add `.styleClass('gg-hline')` and `.id()` to horizontal line elements
- [ ] 7.6.3 Update `GeomVline.render()` to add `.styleClass('gg-vline')` and `.id()` to vertical line elements

### 7.7 Specialized Geom Updates

- [ ] 7.7.1 Update `GeomContour.render()` to add `.styleClass('gg-contour')` and `.id()` to contour paths
- [ ] 7.7.2 Update `GeomContourFilled.render()` to add `.styleClass('gg-contour-filled')` and `.id()` to filled contour paths
- [ ] 7.7.3 Update `GeomBin2d.render()` to add `.styleClass('gg-bin2d')` and `.id()` to bin rectangles
- [ ] 7.7.4 Update `GeomHex.render()` to add `.styleClass('gg-hex')` and `.id()` to hexagon paths
- [ ] 7.7.5 Update `GeomDotplot.render()` to add `.styleClass('gg-dot')` and `.id()` to dot elements
- [ ] 7.7.6 Ensure `GeomJitter` inherits CSS changes from `GeomPoint` (only update if it overrides render)
- [ ] 7.7.7 Update `GeomRug.render()` to add `.styleClass('gg-rug')` and `.id()` to rug lines
- [ ] 7.7.8 Update `GeomQuantile.render()` to add `.styleClass('gg-quantile')` and `.id()` to quantile lines
- [ ] 7.7.9 Update `GeomFreqpoly.render()` to add `.styleClass('gg-freqpoly')` and `.id()` to frequency polygon lines
- [ ] 7.7.10 Update `GeomSpoke.render()` to add `.styleClass('gg-spoke')` and `.id()` to spoke lines
- [ ] 7.7.11 Update `GeomCurve.render()` to add `.styleClass('gg-curve')` and `.id()` to curve paths
- [ ] 7.7.12 Update `GeomCount.render()` to add `.styleClass('gg-count')` and `.id()` to count elements

### 7.8 Q-Q Plot Geom Updates

- [ ] 7.8.1 Update `GeomQq.render()` to add `.styleClass('gg-qq')` and `.id()` to Q-Q plot points
- [ ] 7.8.2 Update `GeomQqLine.render()` to add `.styleClass('gg-qq-line')` and `.id()` to Q-Q reference lines

### 7.9 Spatial and Map Geom Updates

- [ ] 7.9.1 Update `GeomMap.render()` to add `.styleClass('gg-map')` and `.id()` to map paths
- [ ] 7.9.2 Update `GeomSf.render()` to add `.styleClass('gg-sf')` and `.id()` to spatial feature paths
- [ ] 7.9.3 Ensure `GeomSfText` inherits CSS changes from `GeomText` (only update if it overrides render)
- [ ] 7.9.4 Ensure `GeomSfLabel` inherits CSS changes from `GeomLabel` (only update if it overrides render)

### 7.10 Other Geom Updates

- [ ] 7.10.1 Update `GeomFunction.render()` to add `.styleClass('gg-function')` and `.id()` to function curve paths
- [ ] 7.10.2 Update `GeomLogticks.render()` to add `.styleClass('gg-logtick')` and `.id()` to log tick marks
- [ ] 7.10.3 Update `GeomMag.render()` to add `.styleClass('gg-mag')` and `.id()` to magnitude elements
- [ ] 7.10.4 Update `GeomParallel.render()` to add `.styleClass('gg-parallel')` and `.id()` to parallel coordinate lines
- [ ] 7.10.5 Update `GeomRaster.render()` to add `.styleClass('gg-raster')` and `.id()` to raster elements
- [ ] 7.10.6 Update `GeomRasterAnn.render()` to add `.styleClass('gg-raster-ann')` and `.id()` to raster annotation elements
- [ ] 7.10.7 Update `GeomCustom.render()` to support optional CSS class/ID parameters
- [ ] 7.10.8 `GeomBlank` - No changes needed (renders nothing)

### 7.11 Renderer Updates

- [ ] 7.11.1 Update `AxisRenderer` to add `.styleClass('gg-tick')` to tick marks
- [ ] 7.11.2 Update `AxisRenderer` to add `.styleClass('gg-axis-label')` to axis labels
- [ ] 7.11.3 Update `AxisRenderer` to add `.styleClass('gg-axis-title')` to axis titles
- [ ] 7.11.4 Update `GridRenderer` to add `.styleClass('gg-grid-major')` to major grid lines
- [ ] 7.11.5 Update `GridRenderer` to add `.styleClass('gg-grid-minor')` to minor grid lines
- [ ] 7.11.6 Update `LegendRenderer` to add CSS classes to legend keys (`gg-legend-key`) and labels (`gg-legend-label`)

### 7.12 GgRenderer Integration

- [ ] 7.12.1 Update `GgRenderer.renderLayer()` to set `layerIndex` on `RenderContext` before calling geom render
- [ ] 7.12.2 Update `GgRenderer.renderFaceted()` to set `panelRow` and `panelCol` on `RenderContext` for each panel
- [ ] 7.12.3 Update `GgRenderer` to initialize and propagate `CssAttributeConfig` from `GgChart` to `RenderContext`
- [ ] 7.12.4 Update `Geom.render()` signature to accept `RenderContext` parameter (or add overloaded method)

### 7.13 Data Attributes (Optional Enhancement)

- [ ] 7.13.1 Update geom render methods to add `data-x`, `data-y` attributes from stat output when `includeDataAttributes` is enabled
- [ ] 7.13.2 Update geom render methods to add `data-row` using stat output row index
- [ ] 7.13.3 Update geom render methods to add `data-panel` and `data-layer`
- [ ] 7.13.4 Update geom render methods to add optional `data-group` when group is present

### 7.14 Testing

- [ ] 7.14.1 Create `CssAttributeTest.groovy` in `src/test/groovy/gg/` to verify classes are applied to point elements
- [ ] 7.14.2 Add tests to verify classes are applied to bar elements
- [ ] 7.14.3 Add tests to verify classes are applied to line elements
- [ ] 7.14.4 Add tests to verify IDs are unique within a single-panel chart
- [ ] 7.14.5 Add tests to verify IDs are unique within a faceted chart (across all panels)
- [ ] 7.14.6 Add tests to verify IDs follow the naming convention (with `gg-panel` prefix)
- [ ] 7.14.7 Add tests to verify IDs follow the naming convention with `chartIdPrefix`
- [ ] 7.14.8 Add tests to verify axis and grid CSS classes
- [ ] 7.14.9 Add tests to verify data attributes when enabled
- [ ] 7.14.10 Add tests to verify CSS attributes can be disabled via config
- [ ] 7.14.11 Add tests to verify `chartIdPrefix` option works correctly
- [ ] 7.14.12 Add tests for compound shapes (plus, cross, x) to verify group wrapping and ID assignment
- [ ] 7.14.13 Run module test suite: `./gradlew :matrix-charts:test`
- [ ] 7.14.14 Run full test suite to check for regressions: `./gradlew test`

### 7.15 Documentation

- [ ] 7.15.1 Add GroovyDoc to `CssAttributeConfig` class documenting all configuration options
- [ ] 7.15.2 Update package documentation with CSS class reference table
- [ ] 7.15.3 Add usage examples showing CSS styling of charts
- [ ] 7.15.4 Document ID naming conventions for single-panel and faceted charts

---

## 8. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Performance overhead from extra attributes | Low | Make CSS attributes optional via config (disabled by default); benchmark before/after |
| SVG file size increase | Low | Minimal overhead (~20-50 bytes per element); optional feature |
| Breaking existing tests | Medium | Run full test suite after each geom update; use direct SVG object assertions per AGENTS.md |
| ID collisions in multi-chart pages | Medium | Include `chartIdPrefix` option and enforce normalized prefix |
| ID collisions in faceted charts | Medium | Include `panel-{row}-{col}` in all IDs |
| Render method signature changes | Medium | Add overloaded methods rather than changing existing signatures; maintain backward compatibility |
| Data attribute confusion for stats | Low | Define data-* to reflect stat output only; document clearly |

---

## 9. Technical Notes

### Compound Shapes in GeomUtils.drawPoint()

The `drawPoint()` method creates multiple SVG elements for certain shapes:
- **plus/cross**: Two perpendicular lines
- **x**: Two diagonal lines

For these compound shapes, the implementation should:
1. Create a `<g>` (group) element
2. Add the individual line elements to the group
3. Apply the CSS class and ID to the group element
4. Return the group element

Example output for a "plus" shape:
```xml
<g class="gg-point" id="point-0-5">
  <line x1="95" y1="100" x2="105" y2="100" stroke="#F8766D"/>
  <line x1="100" y1="95" x2="100" y2="105" stroke="#F8766D"/>
</g>
```

### RenderContext Usage

The existing `RenderContext` class should be extended to carry CSS configuration:

```groovy
class RenderContext {
  // Existing properties...

  // New CSS-related properties
  CssAttributeConfig cssConfig
  Integer panelRow      // null for single-panel charts
  Integer panelCol      // null for single-panel charts
  int layerIndex = 0
}
```

Geoms can then use this context to generate properly namespaced IDs:
```groovy
String elementId = GeomUtils.generateElementId(context, 'point', elementIndex)
// Returns: "gg-panel-0-0-layer-0-point-5" or "chart-iris-panel-0-1-layer-0-point-5"
```

---

## 10. References

- [ggiraph - Make ggplot2 Graphics Interactive](https://davidgohel.github.io/ggiraph/index.html)
- [svglite R Package](https://github.com/r-lib/svglite)
- [gsvg Library](https://github.com/Alipsa/gsvg)
