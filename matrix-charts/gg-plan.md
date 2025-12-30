# Implementation Plan: Complete the se.alipsa.matrix.gg Grammar of Graphics API

## Executive Summary

This plan outlines the completion of the `se.alipsa.matrix.gg` package to create a fully functional Grammar of Graphics charting library backed by gsvg for SVG rendering. The design allows users to work at the high-level ggplot2-like API while enabling drop-down access to the underlying SVG object model.

---

## Current State Analysis

### What Exists
- **Package structure**: Complete GoG hierarchy (aes, geom, stat, scale, coord, facet, theme)
- **API facade**: `GgPlot.groovy` with static factory methods mimicking ggplot2 syntax
- **Composition**: `GgChart.groovy` with `plus()` operators for fluent composition
- **Skeleton classes**: 18 geoms, 7 stats, 2 coords, 1 scale - all empty implementations
- **gsvg dependency**: Available (`se.alipsa.groovy:gsvg:0.2.0`) but not integrated

### What's Missing
1. [x] No data storage in GgChart
2. [x] No layer management system
3. [x] No rendering pipeline
4. [x] No aesthetic encoding logic - Implemented in GgRenderer with auto-detection
5. [x] No scale computation - Implemented ScaleDiscrete, ScaleContinuous, color scales
6. [x] No coordinate transformation
7. [x] No theme system
8. [x] No faceting - Implemented FacetWrap and FacetGrid with full renderer support

---

## Architecture Design

### Core Principles

1. **Layered Architecture**: Build the chart through composition of layers
2. **Deferred Rendering**: Collect all specifications first, render on demand
3. **SVG Access**: Expose the underlying `Svg` object for customization
4. **Mapping vs Setting**: Distinguish aesthetic mappings (`aes(x: 'col')`) from fixed values (`geom_point(size: 3)`)
5. **Separation of Concerns**:
   - Data + Aesthetics = What to plot
   - Geom + Stat = How to visualize
   - Scale = How to map values
   - Coord = How to position
   - Theme = How to style

### Data Flow

```
Matrix (data) + Aes (mappings)
       |
   Layer params (fixed aesthetics)
       |
   GgChart (collects layers)
       |
   Stat transformations
       |
   Position adjustments
       |
   Scale computations (data -> aesthetic space)
       |
   Coord transformations (aesthetic -> pixel space)
       |
   Geom rendering (to SVG elements)
       |
   Theme styling
       |
   Svg (gsvg object - accessible to user)
```

---

## Design Decisions

1. **Scope Priority**: API-first approach - define complete API surface for all existing geom skeletons, implement rendering incrementally
2. Output formats: SVG only initially, later it will also have png, swing, and javafx as output formats
3. **SVG Access**: `renderSvg()` returns the `Svg` object; users can modify it before export
4. **Statistics**: Basic set - count, bin, boxplot, identity - leverage matrix-stats for computation
5. **Fresh Rendering**: `render()` always produces a new `Svg`. `write(svg, ...)` writes the user-modified SVG, while `write(chart, ...)` renders a new one

---

## API Design Details

### ggplot2 Compatibility Semantics
The goal is to allow ggplot2 chart code to be ported with minimal changes (after data.frame -> Matrix).
To preserve ggplot2 behavior, the following rules apply:

1. `aes(...)` is *mapping only*. All entries are treated as column mappings.
2. Fixed aesthetics are set on the geom (e.g., `geom_point(color: 'red')`) and do not create scales/legends.
3. Identity constants inside `aes` are expressed with `I()` wrapper (matching ggplot2):
   `aes(color: I('red'))`.

This avoids ambiguous auto-detection and matches ggplot2 expectations for scales and legends.

### Default Scale Resolution
To match ggplot2 behavior, scales are chosen automatically from the mapped column type unless explicitly overridden:

- Numeric -> continuous scales
- String/enum -> discrete scales
- Date/time -> time scales (if supported)

Explicit `scale_*` calls always override defaults, and `scale_*_identity` is used for identity mappings.
**Mixed/unknown types**: Log a warning and default to discrete scale.

### Text Measurement Strategy
SVG rendering depends on external font metrics, so exact text sizes vary by environment.
Use heuristic text measurement by default to avoid extra dependencies, and provide an optional
`TextMeasurer` interface (e.g., AWT `FontMetrics`) for users who want more accurate axis/legend layout.
Expose theme hooks (`plot.margin`, `axis.text.margin`, `legend.key.width`) to let users override layout.

### Primary User Flow
```groovy
import static se.alipsa.matrix.gg.GgPlot.*

// Create chart specification
def chart = ggplot(myData, aes(x: 'column1', y: 'column2', color: 'category')) +
    geom_point(size: 3) +
    geom_smooth(method: 'lm') +
    labs(title: 'My Chart', x: 'X Axis', y: 'Y Axis') +
    theme_minimal()

// Render to SVG
Svg svg = chart.render()

// Optional: customize the SVG directly
svg.addText('Custom annotation').x(100).y(50).fill('red')

// Export
GgPlot.write(svg, new File('chart.svg'))
// or
GgPlot.write(chart, new File('chart.svg'))
```

---

## Implementation Plan

### Phase 1: Core Infrastructure

#### 1.1 Layer System
**File**: `se/alipsa/matrix/gg/layer/Layer.groovy` (new) [x]

```groovy
class Layer {
    Matrix data           // Optional per-layer data
    Aes aes              // Per-layer aesthetic mappings
    Map params           // Fixed aesthetics (e.g., size: 3)
    Geom geom            // Geometric representation
    StatType stat        // Statistical transformation (enum)
    PositionType position // Position adjustment (enum)
    boolean inheritAes = true
    Map statParams       // e.g., bins, binwidth, method
}

enum StatType {
    IDENTITY, COUNT, BIN, BOXPLOT, SMOOTH, SUMMARY, DENSITY, BIN2D, CONTOUR
}

enum PositionType {
    IDENTITY, DODGE, STACK, FILL, JITTER
}
```

#### 1.2 Enhanced GgChart
**File**: `GgChart.groovy` (modify)

Add properties:
- [x] `Matrix data` - Base dataset
- [x] `Aes globalAes` - Global aesthetic mappings
- [x] `List<Layer> layers` - Composition layers
- [x] `Coord coord` - Coordinate system (default: CoordCartesian)
- [x] `List<Scale> scales` - Scale specifications
- [x] `Theme theme` - Theme configuration
- [x] `Facet facet` - Faceting specification
- [x] `Label labels` - Title, axis labels, etc.
- [x] `int width`, `int height` - Chart dimensions

Add methods:
- [x] `Svg render()` - Execute the rendering pipeline
- [] `static void write(svg|chart, File|OutputStream)` - Write to file/stream

#### 1.3 Enhanced Aes
**File**: `aes/Aes.groovy` (modify)

Add mappings for:
- [x] `x`, `y` - Position
- [x] `color`, `fill` - Colors
- [x] `size`, `shape` - Point attributes
- [x] `alpha` - Transparency
- [x] `linetype`, `linewidth` - Line attributes
- [x] `group` - Grouping variable
- [x] `label` - Text labels
- [x] `weight` - Statistical weights

Note: `Aes` represents column mappings only; fixed values belong in `Layer.params` or geom arguments.

---

### Phase 2: Scale System

#### 2.1 Scale Base and Types
**Files** (new):
- [x] `scale/Scale.groovy` - Abstract base with `train(data)`, `map(values)`, `transform(value)`, `inverse(value)`, `breaks()`, `labels()`, `limits`, `expand`
- [x] `scale/ScaleContinuous.groovy` - For numeric data
- [x] `scale/ScaleDiscrete.groovy` - For categorical data
- [] `scale/ScaleIdentity.groovy` - Pass-through scale

#### 2.2 Position Scales
**Files** (new):
- [x] `scale/ScaleXContinuous.groovy`, `scale/ScaleYContinuous.groovy`
- [x] `scale/ScaleXDiscrete.groovy`, `scale/ScaleYDiscrete.groovy`
- [] `scale/ScaleXLog10.groovy`, `scale/ScaleYLog10.groovy`

#### 2.3 Color Scales
**Files** (modify/new):
- [x] `scale/ScaleColorManual.groovy` - Implement properly
- [x] `scale/ScaleColorGradient.groovy` - Continuous color gradient
- [] `scale/ScaleColorBrewer.groovy` - ColorBrewer palettes
- [x] `scale/ScaleFillManual.groovy`, `scale/ScaleFillGradient.groovy` - Implemented via factory methods with aesthetic='fill'

#### 2.4 Other Scales
**Files** (new):
- [] `scale/ScaleSize.groovy` - Map values to sizes
- [] `scale/ScaleShape.groovy` - Map to point shapes
- [] `scale/ScaleAlpha.groovy` - Map to transparency
- [] `scale/ScaleLinetype.groovy` - Map to line patterns

---

### Phase 3: Coordinate Systems

#### 3.1 Cartesian Coordinates
**File**: `coord/CoordCartesian.groovy` (new - default) [x]

Methods:
- [x] `transform(x, y)` - Data to pixel coordinates
- [x] `inverse(px, py)` - Pixel to data coordinates
- [] `setupAxes(Range xRange, Range yRange)` - Configure axis ranges

#### 3.2 Implement Existing Coords
**Files** (modify):
- [x] `coord/CoordFlip.groovy` - Swap x/y axes, integrates with renderer for flipped grid/axis rendering
- [x] `coord/CoordPolar.groovy` - Polar transformation with theta/r mapping, arc path generation

#### 3.3 Additional Coords (optional)
- [] `coord/CoordFixed.groovy` - Fixed aspect ratio
- [] `coord/CoordTrans.groovy` - Transformed coordinates (log, sqrt)

---

### Phase 4: Statistical Transformations

#### 4.1 Stat Dispatch [x]
The renderer dispatches to `GgStat` methods based on the `StatType` enum stored in the layer:

```groovy
// In GgRenderer
Matrix computeStat(Layer layer, Matrix data) {
    switch (layer.stat) {
        case StatType.IDENTITY: return GgStat.identity(data, layer.aes)
        case StatType.COUNT:    return GgStat.count(data, layer.aes)
        case StatType.BIN:      return GgStat.bin(data, layer.aes, layer.statParams)
        case StatType.BOXPLOT:  return GgStat.boxplot(data, layer.aes)
        case StatType.SMOOTH:   return GgStat.smooth(data, layer.aes, layer.statParams)
        // ... etc
    }
}
```

#### 4.2 Implement Stats (Leveraging Existing Infrastructure)

Since stat classes are thin wrappers delegating to existing matrix statistics, consolidate them into a single utility class. Users interact via factory methods (`stat_bin()`, `stat_count()`) or implicitly through geoms, not directly with stat classes.

**File**: `stat/GgStat.groovy` (new) - Consolidated stat implementations [x]

```groovy
class GgStat {
    /** Pass-through, returns data unchanged (default for most geoms) */
    static Matrix identity(Matrix data, Aes aes) { return data }

    /** Count occurrences - delegates to Stat.frequency() */
    static Matrix count(Matrix data, Aes aes) { ... }

    /** Histogram binning with bins/binwidth params */
    static Matrix bin(Matrix data, Aes aes, Map params) { ... }

    /** Boxplot stats - delegates to Stat.quartiles(), iqr(), median() */
    static Matrix boxplot(Matrix data, Aes aes) { ... }

    /** Smoothing/regression - delegates to LinearRegression */
    static Matrix smooth(Matrix data, Aes aes, Map params) { ... }

    /** Summary stats - delegates to Stat.meanBy(), medianBy() */
    static Matrix summary(Matrix data, Aes aes, Map params) { ... }
}
```

**Reusable Components from matrix-core (`se.alipsa.matrix.core.Stat`):**

| Stat Class | Delegates To | Method |
|------------|--------------|--------|
| `Count` | `Stat.frequency(column)` | Returns Matrix with Value, Frequency, Percent |
| `Count` | `Stat.countBy(matrix, groupBy)` | Grouped counts |
| `Boxplot` | `Stat.quartiles(values)` | Returns [Q1, Q3] |
| `Boxplot` | `Stat.iqr(values)` | Inter-quartile range (Q3 - Q1) |
| `Boxplot` | `Stat.median(values)` | Median for center line |
| `Boxplot` | `Stat.min/max(values)` | Whisker endpoints |
| `Summary` | `Stat.meanBy(matrix, col, groupBy)` | Group means |
| `Summary` | `Stat.medianBy(matrix, col, groupBy)` | Group medians |
| `Summary` | `Stat.groupBy(matrix, colNames)` | Multi-level grouping |
| `Bin` | `Stat.quartiles()` | For quartile-based breaks |

**Reusable Components from matrix-stats:**

| Stat Class | Delegates To | Purpose |
|------------|--------------|---------|
| `Smooth` | `LinearRegression(x, y)` | Linear trend line (slope, intercept, r2) |
| `Smooth` | `LinearRegression.predict(xs)` | Fitted values for line |
| `Normalize`* | `Normalize.minMaxNorm()` | Scale data to [0,1] |
| `Normalize`* | `Normalize.stdScaleNorm()` | Z-score normalization |
| `Correlation`* | `Correlation.cor(x, y, method)` | Pearson/Spearman/Kendall |

*Optional stat layers for specialized visualizations

**What Needs New Implementation (in GgStat):**
- [x] `GgStat.bin()` - Bin width calculation, break point generation
- [x] `GgStat.density()` - Kernel density estimation (Gaussian KDE with Silverman's rule)
- [x] `GgStat.bin2d()` - 2D binning grid computation - implemented in GeomBin2d
- [x] `GgStat.contour()` - Contour level computation (marching squares algorithm) - implemented in GeomContour

**Example Implementation Pattern:**
```groovy
// In stat/GgStat.groovy
class GgStat {
    static Matrix boxplot(Matrix data, Aes aes) {
        // Group by x if present, otherwise compute for all data
        def groups = aes.xColName ? Stat.groupBy(data, aes.xColName) : ['all': data]

        def results = groups.collect { groupKey, groupData ->
            def values = groupData[aes.yColName] as List<Number>
            def quartiles = Stat.quartiles(values)
            def median = Stat.median(values)
            def iqr = Stat.iqr(values)
            def whiskerLow = Math.max(Stat.min(values), quartiles[0] - 1.5 * iqr)
            def whiskerHigh = Math.min(Stat.max(values), quartiles[1] + 1.5 * iqr)
            def outliers = values.findAll { it < whiskerLow || it > whiskerHigh }
            [x: groupKey, ymin: whiskerLow, lower: quartiles[0], middle: median,
             upper: quartiles[1], ymax: whiskerHigh, outliers: outliers]
        }
        Matrix.builder().mapList(results).build()
    }
}
```

---

### Phase 5: Geom Implementations (Core Set)

Each geom needs:
1. Default stat assignment
2. Required aesthetics declaration
3. `render(G group, Matrix computed, Scales scales, Coord coord)` method

#### 5.1 Priority 1 - Essential Geoms
**Files** (modify):
- [x] `geom/GeomPoint.groovy` - Scatter plots (stat: identity)
- [x] `geom/GeomLine.groovy` (new) - Line charts with grouping support
- [x] `geom/GeomBar.groovy` - Bar charts (stat: count)
- [x] `geom/GeomCol.groovy` - Column charts (stat: identity)
- [x] `geom/GeomHistogram.groovy` - Histograms (stat: bin)
- [x] `geom/GeomBoxplot.groovy` - Box plots (stat: boxplot)

#### 5.2 Priority 2 - Common Geoms
- [x] `geom/GeomSmooth.groovy` - Trend lines (stat: smooth)
- [x] `geom/GeomArea.groovy` (new) - Area charts with fill, supports grouping
- [x] `geom/GeomText.groovy` (new) - Text labels with hjust/vjust, angle, nudge
- [x] `geom/GeomLabel.groovy` (new) - Labeled text with background rectangle

#### 5.3 Priority 3 - Reference Lines
- [x] `geom/GeomAbline.groovy` - Arbitrary lines (slope/intercept)
- [x] `geom/GeomHline.groovy` - Horizontal reference lines
- [x] `geom/GeomVline.groovy` - Vertical reference lines
- [x] `geom/GeomSegment.groovy` - Line segments

#### 5.4 Priority 4 - Advanced Geoms
- [x] `geom/GeomViolin.groovy` - Violin plots with kernel density, quantile lines
- [x] `geom/GeomRug.groovy` - Rug plots for marginal distributions (sides: tblr)
- [x] `geom/GeomDensity.groovy` (new) - Density plots with bandwidth adjustment
- [x] `geom/GeomContour.groovy` - Contour lines using marching squares algorithm
- [x] `geom/GeomContourFilled.groovy` - Filled contour regions with viridis-like color palette
- [x] `geom/GeomBin2d.groovy` - 2D binning heatmaps with configurable bins/binwidth
- [x] `geom/GeomCount.groovy` - Sized points by observation count at each location

---

### Phase 6: Theme System

#### 6.1 Theme Base
**File**: `theme/Theme.groovy` (modify) [x]

Properties covering all visual elements:
- Plot background, panel background, grid lines
- Axis lines, ticks, labels, titles
- Legend position, styling
- Text fonts, sizes, colors
- Margins, padding

#### 6.2 Built-in Themes
**Files** (new):
- [x] `theme/ThemeGray.groovy` - Default ggplot2-like gray theme
- [x] `theme/ThemeMinimal.groovy` - Minimal styling
- [x] `theme/ThemeBW.groovy` - Black and white
- [x] `theme/ThemeClassic.groovy` - Classic look
Note: Implemented as factory methods in `GgPlot.groovy` (`theme_gray()`, `theme_minimal()`, `theme_bw()`, `theme_classic()`), not separate theme classes.

#### 6.3 Theme Elements
**Files** (new):
- [x] `theme/ElementText.groovy` - Text styling
- [x] `theme/ElementLine.groovy` - Line styling
- [x] `theme/ElementRect.groovy` - Rectangle styling
- [x] `theme/ElementBlank.groovy` - Remove element
Note: Implemented as inner classes in `theme/Theme.groovy`, not separate files.

---

### Phase 7: Rendering Pipeline

#### 7.1 Renderer
**File**: `render/GgRenderer.groovy` (new) [x]

Main rendering orchestrator:
```groovy
class GgRenderer {
    Svg render(GgChart chart) {
        Svg svg = new Svg()
        svg.width(chart.width).height(chart.height)

        // 1. Setup plot area
        G plotArea = setupPlotArea(svg, chart.theme)

        // 2. Train scales across layers
        Map<String, Scale> computedScales = trainScales(chart)

        // 3. Draw each layer
        chart.layers.each { layer ->
            Matrix data = layer.data ?: chart.data
            Matrix computed = computeStat(layer, data)              // Dispatch via StatType enum
            Matrix positioned = computePosition(layer, computed)    // Dispatch via PositionType enum
            Matrix mapped = mapAesthetics(positioned, layer.aes, layer.params, computedScales)
            Matrix coords = chart.coord.transform(mapped, chart)
            layer.geom.render(plotArea, coords, computedScales, chart.coord)
        }

        // 4. Draw axes
        renderAxes(plotArea, computedScales, chart.coord, chart.theme)

        // 5. Draw legend
        renderLegend(svg, computedScales, chart.theme)

        // 6. Apply theme styling
        applyTheme(svg, chart.theme)

        return svg
    }
}
```

#### 7.2 Axis Rendering
**File**: `render/AxisRenderer.groovy` (new) [x]

- [x] X-axis and Y-axis rendering
- [x] Tick mark computation and placement
- [x] Axis labels and titles
- [x] Grid lines
- [] Layout strategy: compute margins with simple text size heuristics to avoid overlaps
Note: Axis rendering is implemented directly in `render/GgRenderer.groovy`.

#### 7.3 Legend Rendering
**File**: `render/GgRenderer.groovy` (implemented as renderLegend method) [x]

- [x] Automatic legend generation from scales (color, fill)
- [x] Position control (top, bottom, left, right, none, custom [x,y])
- [x] Discrete legends with colored keys and labels
- [x] Continuous legends with gradient color bars

---

### Phase 8: Position Adjustments

**File**: `position/GgPosition.groovy` (new) - Consolidated position implementations [x]

Similar to stats, position adjustments are consolidated into a utility class:

```groovy
class GgPosition {
    static Matrix identity(Matrix data, Aes aes, Map params) { return data }
    static Matrix dodge(Matrix data, Aes aes, Map params) { ... }
    static Matrix stack(Matrix data, Aes aes, Map params) { ... }
    static Matrix fill(Matrix data, Aes aes, Map params) { ... }
    static Matrix jitter(Matrix data, Aes aes, Map params) { ... }
}
```

The `PositionType` enum is already defined in `Layer.groovy`.

---

### Phase 9: Faceting

**Files**:
- [x] `facet/Facet.groovy` - Abstract base class with filterDataForPanel, getPanelLabel, isFreeX/Y
- [x] `facet/FacetWrap.groovy` - Wrap a 1D ribbon of panels into 2D grid (ncol, nrow, dir, drop, scales)
- [x] `facet/FacetGrid.groovy` - 2D matrix of panels by row/column variables (rows, cols, margins, scales)

**Renderer Support**:
- [x] `GgRenderer.renderFaceted()` - Multi-panel rendering with layout computation
- [x] Panel strip labels with configurable styling
- [x] Fixed/free scales per panel
- [x] Axis labels only on bottom row and leftmost column

---

### Phase 10: API Completeness

#### 10.1 GgPlot Factory Methods
**File**: `GgPlot.groovy` (modify)

Add missing factory methods:
- [x] `labs(title:, subtitle:, x:, y:, caption:)` - Label specification
- [x] `theme_gray()`, `theme_minimal()`, etc. - Theme shortcuts
- [x] `xlim()`, `ylim()` - Axis limits
- [x] `ggsave()` - Export function
- [] Additional geom/stat/scale constructors

#### 10.2 Convenience Methods
- [] `GgChart.toBase64()` - Embed in HTML
- [x] `GgChart + Label` - Add labels via plus operator

---

## File Summary

### New Files (22+)
```
se/alipsa/matrix/gg/
├── layer/
│   └── [x] Layer.groovy
├── render/
│   ├── [x] GgRenderer.groovy   # Includes axis and legend rendering
│   ├── AxisRenderer.groovy     # Implemented inside render/GgRenderer.groovy
│   └── LegendRenderer.groovy   # Implemented inside render/GgRenderer.groovy
├── position/
│   └── [x] GgPosition.groovy   # Consolidated position implementations
├── scale/
│   ├── [x] Scale.groovy
│   ├── [x] ScaleContinuous.groovy
│   ├── [x] ScaleDiscrete.groovy
│   ├── [x] ScaleXContinuous.groovy
│   ├── [x] ScaleYContinuous.groovy
│   ├── [x] ScaleXDiscrete.groovy
│   ├── [x] ScaleYDiscrete.groovy
│   ├── [x] ScaleColorManual.groovy
│   ├── [x] ScaleColorGradient.groovy
│   └── ScaleSize.groovy
├── coord/
│   └── [x] CoordCartesian.groovy
├── stat/
│   └── [x] GgStat.groovy       # Consolidated stat implementations
├── geom/
│   ├── [x] GeomLine.groovy - Line charts with grouping support
│   └── GeomArea.groovy
└── theme/
    ├── ThemeGray.groovy        # Implemented in GgPlot.groovy
    ├── ThemeMinimal.groovy     # Implemented in GgPlot.groovy
    ├── ThemeBW.groovy          # Implemented in GgPlot.groovy
    ├── ThemeClassic.groovy     # Implemented in GgPlot.groovy
    ├── ElementText.groovy      # Implemented in theme/Theme.groovy
    ├── ElementLine.groovy      # Implemented in theme/Theme.groovy
    ├── ElementRect.groovy      # Implemented in theme/Theme.groovy
    └── ElementBlank.groovy     # Implemented in theme/Theme.groovy
```

### Modified Files (20+)
- `GgChart.groovy` - Major enhancement
- `GgPlot.groovy` - Add factory methods
- `aes/Aes.groovy` - Extended mappings
- All existing geom/*.groovy files
- All existing stat/*.groovy files
- `scale/Scale.groovy`, `scale/ScaleColorManual.groovy`
- `coord/Coord.groovy`, `coord/CoordFlip.groovy`, `coord/CoordPolar.groovy`
- `theme/Theme.groovy`

---

## Implementation Order (API-First Approach)

### Sprint 1: API Surface Definition
1. [x] Define `Layer` class with complete interface
2. [x] Expand `GgChart` with all properties and method signatures
3. [x] Expand `Aes` with all aesthetic mappings + `I()` wrapper for identity constants
4. [x] Define `Geom` base class with abstract `render()` method
5. [x] Create `GgStat` utility class with method stubs for all stat types
6. [x] Define `Scale` base class with `train()`, `map()`, `transform()`, `breaks()`, `labels()`
7. [x] Define `Coord` base class with abstract `transform()` method
8. [x] Define `Theme` base class with all styling properties
9. [] Add all missing factory methods to `GgPlot`

### Sprint 2: Core Rendering (Point Chart End-to-End)
1. [x] Implement `GgRenderer` orchestrator
2. [x] Implement `CoordCartesian` (default coordinate system)
3. [x] Implement basic `ScaleXContinuous` and `ScaleYContinuous`
4. [x] Implement `GgStat.identity()` (pass-through)
5. [x] Implement `GeomPoint.render()` - first working chart
6. [x] Implement basic axis rendering
7. [] Write tests for point chart

### Sprint 3: Bar and Line Charts
1. [x] Implement `GgStat.count()` for bar charts
2. [x] Implement `GeomBar.render()` and `GeomCol.render()` - bar/column charts with stat_count and stat_identity
3. [x] Implement `GeomLine.render()` (new class) - with grouping, sorting, color scales
4. [x] Implement `ScaleXDiscrete` for categorical axes
5. [x] Implement `PositionDodge` and `PositionStack`
6. [x] Write tests for bar/line charts (GeomBarColTest, GeomLineTest)

### Sprint 4: Histogram and Boxplot
1. [x] Implement `GgStat.bin()` for histograms
2. [x] Implement `GeomHistogram.render()` - renders bins as rectangles using stat_bin output
3. [x] Implement `GgStat.boxplot()` (leveraging matrix-core Stat)
4. [x] Implement `GeomBoxplot.render()` - renders box, whiskers, caps, median line, outliers
5. [x] Write tests for histogram (16 tests in GeomHistogramTest)
6. [x] Write tests for boxplot (17 tests in GeomBoxplotTest)

### Sprint 5: Color Scales and Legend
1. [x] Implement `ScaleColorManual` properly
2. [x] Implement `ScaleColorGradient`
3. [x] Implement `LegendRenderer` - supports discrete and continuous legends, configurable position
4. [x] Implement color mapping in geoms - auto-detection in GgRenderer
5. [x] Write tests for legend (13 tests in LegendTest)

### Sprint 6: Theme System
1. [x] Implement `ThemeGray` (default)
2. [x] Implement `ThemeMinimal`
3. [x] Implement theme element classes
4. [x] Apply themes in renderer

### Sprint 7: Remaining Geoms
1. [x] Implement reference line geoms (hline, vline, abline, segment) - Full implementations with linetype, color, alpha, arrow support
2. [x] Implement `GeomSmooth` (basic linear)
3. [x] Implement `GeomArea` - Area charts with fill, alpha, grouping support
4. [x] Implement `GeomText` - Text labels with positioning, rotation, styling
5. [x] Implement `GeomLabel` - Text with background rectangles
6. [x] Implement `GeomRug` - Marginal distribution tick marks (sides: tblr)
7. [x] Implement `GeomViolin` - Violin plots with kernel density estimation
8. [x] Implement `GeomDensity` - Density curves with bandwidth adjustment
9. [x] Implement `GeomContour` - Contour lines using marching squares algorithm
10. [x] Implement `GeomContourFilled` - Filled contour regions between levels
11. [x] Implement `GeomBin2d` - 2D binning heatmaps
12. [x] Implement `GeomCount` - Sized points by observation count

### Sprint 8: Special Coordinates
1. [x] Implement `CoordFlip` - Swaps x/y axes for horizontal bar charts, boxplots, etc.
2. [x] Implement `CoordPolar` - Polar coordinates with theta/r mapping, arc path generation for pie charts

### Sprint 9: Faceting
1. [x] Implement `Facet` base class with abstract methods and common functionality
2. [x] Implement `FacetWrap` - Wraps panels into 2D grid (ncol, nrow, dir, scales)
3. [x] Implement `FacetGrid` - Matrix of panels by row/column variables
4. [x] Update `GgRenderer` with `renderFaceted()` method for multi-panel rendering
5. [x] Implement panel strip labels with theme-based styling
6. [x] Support fixed and free scales across panels
7. [x] Add factory methods `facet_wrap()` and `facet_grid()` to GgPlot
8. [x] Write tests for faceting (28 tests in FacetTest)

### Future: Advanced Features
- [] Additional position adjustments
- [] More statistical transformations
- [] ScaleColorBrewer palettes
- [] CoordFixed (fixed aspect ratio)
- [] CoordTrans (log, sqrt transformations)
