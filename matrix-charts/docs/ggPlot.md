# GGPlot Guide

A comprehensive guide to using the `se.alipsa.matrix.gg` package for creating ggplot2-style visualizations in Groovy.

## Table of Contents
- [Introduction](#introduction)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [Available Components](#available-components)
- [Examples](#examples)
- [Differences from R's ggplot2](#differences-from-rs-ggplot2)
- [Output Formats](#output-formats)

## Introduction

The `se.alipsa.matrix.gg` package provides a ggplot2-style API for creating data visualizations in Groovy.
The API is designed to be nearly identical to R's ggplot2 library, making it easy to port R plotting code to Groovy with minimal modifications.

> **Implementation note:** The gg API is a thin compatibility wrapper over the
> [Charm](charm.md) engine. All gg functions delegate to Charm, producing the same
> `PlotSpec` model and rendering through the same SVG pipeline. The full gg public API
> is preserved and will continue to work. For new code that does not need ggplot2
> compatibility, consider using the [Charm DSL](charm.md) directly.

### Key Features
- Familiar ggplot2 syntax and semantics
- Works with Matrix data structures
- Extensive geom (geometric object) support
- Complete theming system
- Faceting for multi-panel plots
- Statistical transformations
- Multiple output formats (SVG, PNG, JavaFX)

## Quick Start

### Basic Setup

```groovy
@Grab('se.alipsa.matrix:matrix-core:3.7.0')
@Grab('se.alipsa.matrix:matrix-charts:0.5.0')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1')

import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
```

### Creating Your First Plot

```groovy
def mpg = Dataset.mpg()

def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

write(chart, new File('my_plot.svg'))
```

### Porting from R

The API is nearly identical to R's ggplot2. Here's a comparison:

**R code:**
```R
library(ggplot2)

chart <- ggplot(mpg, aes(cty, hwy)) +
  geom_point() +
  coord_fixed()

ggsave("my_plot.svg", plot = chart)
```

**Groovy equivalent:**
```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy')) +
    geom_point() +
    coord_fixed()

ggsave("my_plot.svg", plot: chart)
```

**Key differences:**
1. Use a Matrix instead of a data.frame
2. Quote column names and constants
3. Use `write()` or `ggsave()` instead of R's `ggsave()`

## Core Concepts

### The Grammar of Graphics

The gg package follows the "grammar of graphics" approach, where plots are built by layering components:

```groovy
ggplot(data, aes(x, y)) +           // Data and aesthetics
    geom_point() +                  // Geometric objects
    scale_x_log10() +               // Scales
    coord_fixed() +                 // Coordinate systems
    facet_wrap('category') +        // Faceting
    theme_minimal() +               // Themes
    labs(title: 'My Plot')          // Labels
```

### Aesthetics (aes)

Aesthetics map data columns to visual properties:

```groovy
// Basic aesthetics
aes('x_column', 'y_column')
aes(x: 'x_column', y: 'y_column')

// Additional aesthetics
aes('x_col', 'y_col',
    color: 'category',
    size: 'value',
    shape: 'group',
    alpha: 'transparency_col',
    fill: 'fill_col')

// Grouping and faceting
aes('x', 'y', group: 'category')
aes('x', 'y', group: cut_width('continuous_var', 1))
```

**Common aesthetics:**
- `x`, `y` - Position
- `color` / `colour` - Color of points, lines, borders
- `fill` - Fill color for areas
- `size` - Size of points or line width
- `shape` - Point shape
- `alpha` - Transparency (0-1)
- `linetype` - Line type (solid, dashed, dotted, etc.)
- `group` - Grouping variable

### Geoms (Geometric Objects)

Geoms determine how data is displayed. You can layer multiple geoms:

```groovy
ggplot(data, aes('x', 'y')) +
    geom_point() +                  // Add points
    geom_smooth(method: 'lm') +     // Add regression line
    geom_line()                     // Add line connecting points
```

### Scales

Scales control how data values map to visual properties:

```groovy
// Transform scales
ggplot(data, aes('x', 'y')) +
    geom_point() +
    scale_x_log10() +              // Log10 transform for x-axis
    scale_y_sqrt()                 // Square root transform for y-axis

// Color scales
ggplot(data, aes('x', 'y', color: 'category')) +
    geom_point() +
    scale_color_viridis_d()        // Discrete viridis colors

// Manual scales
ggplot(data, aes('x', 'y', color: 'group')) +
    geom_point() +
    scale_color_manual(values: ['red', 'blue', 'green'])
```

### Coordinate Systems

Coordinate systems control the plot's coordinate space:

```groovy
coord_cartesian()        // Default Cartesian coordinates
coord_fixed()           // Fixed aspect ratio (1:1)
coord_fixed(ratio: 2)   // Custom aspect ratio
coord_flip()            // Flip x and y axes
coord_polar()           // Polar coordinates
```

### Faceting

Faceting creates multi-panel plots based on categorical variables:

```groovy
// Wrap facets (single variable)
ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    facet_wrap('class')

// Grid facets (two variables)
ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    facet_grid('year ~ drv')       // rows ~ columns
```

### Themes

Themes control the overall appearance:

```groovy
// Built-in themes
theme_grey()            // Default theme
theme_minimal()         // Minimal theme
theme_bw()              // Black and white theme
theme_classic()         // Classic theme
theme_dark()            // Dark theme
theme_light()           // Light theme
theme_void()            // Empty theme

// Customize theme elements
ggplot(data, aes('x', 'y')) +
    geom_point() +
    theme_minimal() +
    theme(
        'legend.position': 'top',
        'axis.line': element_line(linewidth: 0.75),
        'axis.line.x.bottom': element_line(colour: 'blue')
    )
```

## Available Components

### Geoms (54 types)

#### Basic Geoms
- `geom_point()` - Scatter plots
- `geom_line()` - Line plots
- `geom_path()` - Connected paths (ordered by data)
- `geom_step()` - Step plots
- `geom_bar()` - Bar charts (counts)
- `geom_col()` - Column charts (values)
- `geom_area()` - Area plots
- `geom_polygon()` - Polygons
- `geom_rect()` - Rectangles
- `geom_tile()` - Tiles/heatmaps
- `geom_text()` - Text labels
- `geom_label()` - Text labels with background

#### Statistical Geoms
- `geom_histogram()` - Histograms
- `geom_freqpoly()` - Frequency polygons
- `geom_density()` - Density plots
- `geom_boxplot()` - Box plots
- `geom_violin()` - Violin plots
- `geom_dotplot()` - Dot plots
- `geom_count()` - Count overlapping points
- `geom_bin2d()` - 2D binned heatmap
- `geom_hex()` - Hexagonal binning
- `geom_smooth()` - Smoothed conditional means
- `geom_quantile()` - Quantile regression

#### Range/Error Geoms
- `geom_ribbon()` - Ribbons (confidence bands)
- `geom_pointrange()` - Points with error bars
- `geom_linerange()` - Line ranges
- `geom_errorbar()` - Error bars
- `geom_errorbarh()` - Horizontal error bars
- `geom_crossbar()` - Crossbars

#### Contour/Density Geoms
- `geom_contour()` - Contour lines
- `geom_contour_filled()` - Filled contours
- `geom_density2d()` - 2D density contours
- `geom_density2d_filled()` - Filled 2D density

#### Reference Line Geoms
- `geom_abline()` - Diagonal reference lines
- `geom_hline()` - Horizontal reference lines
- `geom_vline()` - Vertical reference lines
- `geom_segment()` - Line segments
- `geom_curve()` - Curved lines
- `geom_spoke()` - Spokes (angle + radius)

#### Statistical Display Geoms
- `geom_qq()` - Q-Q plots
- `geom_qq_line()` - Q-Q reference line
- `geom_rug()` - Rug plots (marginal ticks)
- `geom_jitter()` - Jittered points

#### Specialized Geoms
- `geom_raster()` - Raster images
- `geom_map()` - Map data
- `geom_sf()` - Simple features (spatial)
- `geom_sf_label()` - SF labels
- `geom_sf_text()` - SF text
- `geom_parallel()` - Parallel coordinates
- `geom_function()` - Function plots
- `geom_mag()` - Magnitude plots
- `geom_logticks()` - Logarithmic tick marks
- `geom_blank()` - Blank layer (for limits)
- `geom_custom()` - Custom geoms

### Scales

#### Position Scales
- `scale_x_continuous()` / `scale_y_continuous()`
- `scale_x_discrete()` / `scale_y_discrete()`
- `scale_x_log10()` / `scale_y_log10()`
- `scale_x_sqrt()` / `scale_y_sqrt()`
- `scale_x_reverse()` / `scale_y_reverse()`
- `scale_x_date()` / `scale_y_date()`
- `scale_x_datetime()` / `scale_y_datetime()`
- `scale_x_time()` / `scale_y_time()`
- `scale_x_binned()` / `scale_y_binned()`

#### Color Scales
- `scale_color_continuous()` / `scale_colour_continuous()`
- `scale_color_discrete()` / `scale_colour_discrete()`
- `scale_color_gradient()` / `scale_colour_gradient()`
- `scale_color_gradient2()` / `scale_colour_gradient2()`
- `scale_color_gradientn()` / `scale_colour_gradientn()`
- `scale_color_hue()` / `scale_colour_hue()`
- `scale_color_grey()` / `scale_colour_grey()`
- `scale_color_brewer()` / `scale_colour_brewer()`
- `scale_color_distiller()` / `scale_colour_distiller()`
- `scale_color_fermenter()` / `scale_colour_fermenter()`
- `scale_color_viridis_d()` / `scale_colour_viridis_d()`
- `scale_color_viridis_c()` / `scale_colour_viridis_c()`
- `scale_color_steps()` / `scale_colour_steps()`
- `scale_color_identity()` / `scale_colour_identity()`
- `scale_color_manual()` / `scale_colour_manual()`

#### Fill Scales
- `scale_fill_continuous()`, `scale_fill_discrete()`
- `scale_fill_gradient()`, `scale_fill_gradient2()`, `scale_fill_gradientn()`
- `scale_fill_hue()`, `scale_fill_grey()`
- `scale_fill_brewer()`, `scale_fill_distiller()`, `scale_fill_fermenter()`
- `scale_fill_viridis_d()`, `scale_fill_viridis_c()`
- `scale_fill_steps()`, `scale_fill_identity()`, `scale_fill_manual()`

#### Other Aesthetic Scales
- `scale_size()`, `scale_size_binned()`, `scale_size_area()`
- `scale_shape()`, `scale_shape_binned()`, `scale_shape_manual()`, `scale_shape_identity()`
- `scale_linetype()`, `scale_linetype_manual()`, `scale_linetype_identity()`
- `scale_alpha()`, `scale_alpha_binned()`, `scale_alpha_manual()`, `scale_alpha_identity()`
- `scale_radius()`

### Coordinate Systems
- `coord_cartesian()` - Standard Cartesian (default)
- `coord_fixed()` - Fixed aspect ratio
- `coord_flip()` - Flip x/y axes
- `coord_polar()` - Polar coordinates
- `coord_map()` - Map projections
- `coord_sf()` - Simple features (spatial)
- `coord_trans()` - Transformed coordinates

### Faceting
- `facet_wrap()` - Wrap panels into rows/columns
- `facet_grid()` - Grid of panels by variables

### Themes
- `theme_grey()` - Default grey theme
- `theme_bw()` - Black and white theme
- `theme_minimal()` - Minimal theme
- `theme_classic()` - Classic theme (no gridlines)
- `theme_dark()` - Dark theme
- `theme_light()` - Light theme
- `theme_void()` - Empty theme
- `theme()` - Custom theme modifications

### Labels and Annotations
- `labs()` - Set labels for title, subtitle, axes, legends
- `xlab()` / `ylab()` - X/Y axis labels
- `ggtitle()` - Plot title and subtitle
- `annotate()` - Add annotations
- `annotation_logticks()` - Log scale tick marks
- `annotation_custom()` - Custom annotations
- `annotation_raster()` - Raster annotations
- `annotation_map()` - Map annotations

### Guides
- `guides()` - Set guide (legend) properties
- `guide_legend()` - Legend guide
- `guide_colorbar()` / `guide_colourbar()` - Continuous color bar
- `guide_bins()` - Binned guide
- `guide_colorsteps()` / `guide_coloursteps()` - Color steps
- `guide_axis()` - Axis guide
- `guide_none()` - Remove guide

### Limits and Expansions
- `lims()` - Set limits for multiple axes
- `xlim()` / `ylim()` - Set x/y limits
- `expand_limits()` - Expand limits to include values
- `expansion()` - Control axis expansion

### Utilities
- `vars()` - Quote variables for faceting
- `after_stat()` - Reference computed statistics
- `after_scale()` - Reference scaled values
- `cut_width()` - Bin continuous variable by width
- `position_nudge()` - Nudge position adjustment
- `expr()` - Define expressions
- `I()` - Use value "as is" (identity)

## Examples

### Basic Scatter Plot

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def mpg = Dataset.mpg()
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

write(chart, new File('scatter.svg'))
```

### Colored by Category

```groovy
def chart = ggplot(mpg, aes('cty', 'hwy', colour: 'class')) +
    geom_point() +
    scale_colour_viridis_d() +
    labs(title: 'MPG by Vehicle Class')

write(chart, new File('colored_scatter.svg'))
```

### Layered Plot with Regression Line

```groovy
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG with Trend Line')

write(chart, new File('regression.svg'))
```

### Box Plot with Grouping

```groovy
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_boxplot(aes(group: cut_width('displ', 1))) +
    labs(title: 'Highway MPG by Engine Displacement')

write(chart, new File('boxplot.svg'))
```

### Faceted Plot

```groovy
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    facet_grid('year ~ drv') +
    labs(title: 'MPG by Year and Drive Type')

write(chart, new File('facets.svg'))
```

### Custom Theme

```groovy
def chart = ggplot(mpg, aes('cty', 'hwy', colour: 'class')) +
    geom_point() +
    theme_minimal() +
    theme(
        'legend.position': 'top',
        'axis.line': element_line(linewidth: 0.75),
        'axis.line.x.bottom': element_line(colour: 'blue')
    ) +
    labs(title: 'Customized Theme Example')

write(chart, new File('custom_theme.svg'))
```

### Annotations

```groovy
import se.alipsa.matrix.core.Matrix

def data = Matrix.builder().data(
    x: [1, 2, 3, 4],
    y: [10, 12, 9, 14]
).types(Integer, Integer).build()

def chart = ggplot(data, aes('x', 'y')) +
    geom_point() +
    annotate('text', x: 2, y: 12, label: 'Peak') +
    annotate('rect', xmin: 1, xmax: 3, ymin: 9, ymax: 13, alpha: 0.15) +
    annotate('segment', x: 1, xend: 3, y: 10, yend: 10, color: 'red')

write(chart, new File('annotations.svg'))
```

### Scale Transformations

```groovy
def data = Matrix.builder().data(
    x: [1, 10, 100, 1000],
    y: [1, 4, 9, 16]
).types(Integer, Integer).build()

def chart = ggplot(data, aes('x', 'y')) +
    geom_point() +
    scale_x_log10() +
    scale_y_sqrt() +
    labs(title: 'Log-sqrt Transformed Scales')

write(chart, new File('transforms.svg'))
```

### Date/Time Scales

```groovy
import java.time.LocalDate

def dates = Matrix.builder().data(
    date: [LocalDate.parse('2024-01-01'), LocalDate.parse('2024-02-01'), LocalDate.parse('2024-03-01')],
    value: [10, 12, 15]
).types(LocalDate, Integer).build()

def chart = ggplot(dates, aes('date', 'value')) +
    geom_line() +
    geom_point() +
    scale_x_date() +
    labs(title: 'Time Series Data')

write(chart, new File('timeseries.svg'))
```

### Heatmap

```groovy
def grid = Matrix.builder().data(
    x: [1, 2, 3, 1, 2, 3, 1, 2, 3],
    y: [1, 1, 1, 2, 2, 2, 3, 3, 3],
    value: [5, 10, 15, 8, 12, 18, 3, 7, 20]
).types(Integer, Integer, Integer).build()

def chart = ggplot(grid, aes('x', 'y', fill: 'value')) +
    geom_tile() +
    scale_fill_viridis_c() +
    labs(title: 'Heatmap Example')

write(chart, new File('heatmap.svg'))
```

### Complex Example with Multiple Aesthetics

```groovy
import se.alipsa.matrix.datasets.Dataset
import static se.alipsa.matrix.gg.GgPlot.*

def penguins = Dataset.penguins()

def chart = ggplot(
    data: penguins,
    mapping: aes(x: 'flipper_length_mm', y: 'body_mass_g')
) +
    geom_point(aes(color: 'species', shape: 'species')) +
    geom_smooth(method: 'lm') +
    labs(
        title: 'Body mass and flipper length',
        subtitle: 'Dimensions for Adelie, Chinstrap, and Gentoo Penguins',
        x: 'Flipper length (mm)',
        y: 'Body mass (g)',
        color: 'Species',
        shape: 'Species'
    ) +
    theme_minimal()

write(chart, new File('penguins.svg'))
```

## Differences from R's ggplot2

While the API is designed to be as close as possible to R's ggplot2, there are some differences due to language constraints:

### 1. Quoting
Column names and string constants must be quoted:

```R
# R
aes(x, y, color = class)
```
Must be written as:
```groovy
// Groovy
aes('x', 'y', color: 'class')
```

### 2. Named Arguments
Use Groovy's Map syntax for named arguments:

```R
# R
geom_point(size = 3, alpha = 0.5)
```
```groovy
// Groovy
geom_point(size: 3, alpha: 0.5)
```

### 3. Data Structures
Use Matrix instead of data.frame:

```R
# R
df <- data.frame(x = 1:10, y = rnorm(10))
```
```groovy
// Groovy
import se.alipsa.matrix.core.Matrix
def data = Matrix.builder().data(
    x: 1..10,
    y: [/* values */]
).build()
```

### 4. Rendering and Saving
Use `write()` or `ggsave()`:

```groovy
// R
ggsave("plot.svg", plot = chart)

// Groovy
write(chart, new File('plot.svg'))
// or
ggsave('plot.svg', chart)
```

### 5. Static Imports
Import the GgPlot class statically:

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
```

### 6. Boolean Constants
Use TRUE/FALSE or T/F constants from GgPlot:

```groovy
import static se.alipsa.matrix.gg.GgPlot.*

facet_wrap('var', scales: 'free')  // Use string 'free'
// TRUE, FALSE, T, F are available as constants
```

## Output Formats

### SVG (Default)

```groovy
import static se.alipsa.matrix.gg.GgPlot.*

def chart = ggplot(data, aes('x', 'y')) + geom_point()

// Write directly to file
write(chart, new File('plot.svg'))

// Or use ggsave
ggsave(chart, 'plot.svg')
```

### PNG

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.chartexport.ChartToPng

def chart = ggplot(data, aes('x', 'y')) + geom_point()
ggsave(chart, 'plot.png')
```

### JPEG

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.chartexport.ChartToJpeg

def chart = ggplot(data, aes('x', 'y')) + geom_point()
ggsave(chart, 'plot.jpeg', quality: 0.95) // quality: 0.0-1.0
```

### JavaFX

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.chartexport.ChartToJfx
import org.girod.javafx.svgimage.SVGImage

def chart = ggplot(data, aes('x', 'y')) + geom_point()
SVGImage svgImage = ChartToJfx.export(chart)
// Use svgImage in your JavaFX application
```

### Swing

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel

def chart = ggplot(data, aes('x', 'y')) + geom_point()
SvgPanel panel = ChartToSwing.export(chart)
// Add panel to your Swing container
```

### Multiple Charts in One File

```groovy
// Save multiple charts to one SVG file
ggsave('combined.svg', chart1, chart2, chart3)
```

## Additional Resources

- **Examples**: See the `matrix-charts/examples/gg/` directory for more examples
- **API Documentation**: [JavaDoc](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
- **Matrix Core**: See matrix-core documentation for working with Matrix data structures
- **Matrix Stats**: Statistical functions for use with `geom_smooth()` and other stat geoms

## Tips and Best Practices

### 1. Build Plots Incrementally
```groovy
def base = ggplot(data, aes('x', 'y'))
def withPoints = base + geom_point()
def final = withPoints + theme_minimal()
```

### 2. Reuse Aesthetics
```groovy
def commonAes = aes('flipper_length', 'body_mass', color: 'species')
def plot1 = ggplot(penguins, commonAes) + geom_point()
def plot2 = ggplot(penguins, commonAes) + geom_boxplot()
```

### 3. Use Helper Functions
```groovy
// For limits
+ lims(x: [0, 100], y: [0, 50])

// For expansion
+ expand_limits(x: 0, y: [0, 100])

// For custom CSS
+ css_attributes(stroke: 'blue', 'stroke-width': 2)
```

### 4. Layer Order Matters
```groovy
// This puts smooth line on top
ggplot(data, aes('x', 'y')) +
    geom_point() +
    geom_smooth()

// This puts points on top (usually preferred)
ggplot(data, aes('x', 'y')) +
    geom_smooth() +
    geom_point()
```

### 5. Use Matrix Operations
```groovy
// Prepare data with Matrix operations
def data = Matrix.builder().data(csvFile).build()
    .replaceAll('NA', null)
    .convert(age: Integer, salary: Double)
    .subset { row -> row.salary > 50000 }

def chart = ggplot(data, aes('age', 'salary')) + geom_point()
```
