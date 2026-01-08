# ggplot2 Implementation Gap Analysis

This document tracks the implementation status of ggplot2 functions in the matrix-charts `se.alipsa.matrix.gg` package.

## Philosophy

**Primary Goal:** Write beautiful, easy-to-read, idiomatic Groovy code.

- Leverage Groovy's default numeric type (BigDecimal) where it naturally fits.
- Prefer readability over precision, and simplicity over micro-optimizations.
- Make the code maintainable and Groovy-like
- Add extensions to BigDecimalExtensions in the matrix-groovy-ext module when needed to allow writing the code in a more ideomatic groovy way

## Current Implementation Coverage

The matrix-charts gg implementation covers approximately 40-50% of ggplot2's functionality.

### What's Already Implemented

| Category      | Implemented   | Status             |
|---------------|---------------|--------------------|
| **Geoms**     | 31            | Good core coverage |
| **Scales**    | 18 core types | Good coverage      |
| **Stats**     | 6             | Limited            |
| **Coords**    | 4             | Good               |
| **Facets**    | 2             | Complete           |
| **Positions** | 6             | Good               |
| **Themes**    | 5 base themes | Good               |

---

## Phase 1: Core Enhancements (High Priority)

### 1.1 Essential Geoms

- 1.1.1 [x] `geom_ribbon()` - Ribbon/band area for confidence intervals
- 1.1.2 [x] `geom_tile()` - Rectangular heatmap tiles
- 1.1.3 [x] `geom_rect()` - Rectangles
- 1.1.4 [x] `geom_path()` - Connect observations in data order
- 1.1.5 [x] `geom_step()` - Step plot for time series
- 1.1.6 [x] `geom_pointrange()` - Point with vertical line range
- 1.1.7 [x] `geom_linerange()` - Vertical line ranges
- 1.1.8 [x] `geom_crossbar()` - Crossbar for error bars

### 1.2 Transform Scales

- 1.2.1 [x] `scale_x_log10()` - Log10 transformed x-axis
- 1.2.2 [x] `scale_y_log10()` - Log10 transformed y-axis
- 1.2.3 [x] `scale_x_sqrt()` - Square root transformed x-axis
- 1.2.4 [x] `scale_y_sqrt()` - Square root transformed y-axis
- 1.2.5 [x] `scale_x_reverse()` - Reversed x-axis
- 1.2.6 [x] `scale_y_reverse()` - Reversed y-axis

### 1.3 Date/Time Scales

- 1.3.1 [x] `scale_x_date()` - Date scale for x-axis
- 1.3.2 [x] `scale_y_date()` - Date scale for y-axis
- 1.3.3 [x] `scale_x_datetime()` - DateTime scale for x-axis
- 1.3.4 [x] `scale_y_datetime()` - DateTime scale for y-axis

### 1.4 Annotations

- 1.4.1 [x] `annotate()` - Add annotation layer (text, shapes, etc.)

### 1.5 Guide System

- 1.5.1 [x] `guides()` - Control which guides appear for each scale
- 1.5.2 [x] `guide_legend()` - Legend customization
- 1.5.3 [x] `guide_colourbar()` / `guide_colorbar()` - Continuous color guide

### 1.6 Limit Functions

- 1.6.1 [x] `lims()` - Set multiple limits at once
- 1.6.2 [x] `expand_limits()` - Expand limits to include specific values

### Phase 1 Test Coverage

- [x] `geom_ribbon()`
- [x] `geom_tile()`
- [x] `geom_rect()`
- [x] `geom_path()`
- [x] `geom_step()`
- [x] `geom_pointrange()`
- [x] `geom_linerange()`
- [x] `geom_crossbar()`
- [x] `scale_x_log10()`
- [x] `scale_y_log10()`
- [x] `scale_x_sqrt()`
- [x] `scale_y_sqrt()`
- [x] `scale_x_reverse()`
- [x] `scale_y_reverse()`
- [x] `scale_x_date()`
- [x] `scale_y_date()`
- [x] `scale_x_datetime()`
- [x] `scale_y_datetime()`
- [x] `annotate()`
- [x] `guides()`
- [x] `guide_legend()`
- [x] `guide_colourbar()` / `guide_colorbar()`
- [x] `lims()`
- [x] `expand_limits()`

---

## Phase 2: Statistical Features (Medium Priority)

Leverage matrix-stats or Stat from matrix core when possible. Avoid complex statistics calculations in matrix-stats, focusing instead on visualisation aspects; instead, add missing statistics to matrix stats.

### 2.1 Stats for Existing Geoms

- 2.1.1 [x] `stat_bin()` - Binning statistics for histograms
- 2.1.2 [x] `stat_density()` - 1D kernel density estimation
- 2.1.3 [x] `stat_smooth()` - Smoothed conditional means
- 2.1.4 [x] `stat_ydensity()` - Density for violin plots
- 2.1.5 [x] `stat_ecdf()` - Empirical cumulative distribution function

### 2.2 New Statistical Geoms

- 2.2.1 [x] `geom_qq()` - Q-Q plot points
- 2.2.2 [x] `geom_qq_line()` - Q-Q plot reference line
- 2.2.3 [x] `stat_qq()` - Q-Q statistics
- 2.2.4 [x] `stat_qq_line()` - Q-Q line statistics
- 2.2.5 [x] `geom_freqpoly()` - Frequency polygon
- 2.2.6 [x] `geom_jitter()` - Convenience wrapper for jittered points

### 2.3 Color Palette Scales

- 2.3.1 [x] `scale_colour_brewer()` / `scale_color_brewer()` - ColorBrewer palettes
- 2.3.2 [x] `scale_fill_brewer()` - ColorBrewer fill palettes
- 2.3.3 [x] `scale_colour_distiller()` / `scale_color_distiller()` - Continuous ColorBrewer
- 2.3.4 [x] `scale_fill_distiller()` - Continuous ColorBrewer fill
- 2.3.5 [x] `scale_colour_grey()` / `scale_color_grey()` - Greyscale palette
- 2.3.6 [x] `scale_fill_grey()` - Greyscale fill palette
- 2.3.7 [x] `scale_colour_gradientn()` / `scale_color_gradientn()` - N-color gradient
- 2.3.8 [x] `scale_fill_gradientn()` - N-color gradient fill

### 2.4 Alpha Scales

- 2.4.1 [x] `scale_alpha()` - Alpha transparency scale
- 2.4.2 [x] `scale_alpha_continuous()` - Continuous alpha scale
- 2.4.3 [x] `scale_alpha_discrete()` - Discrete alpha scale
- 2.4.4 [x] `scale_alpha_binned()` - Binned alpha scale

### 2.5 Size Scales

- 2.5.1 [x] `scale_size()` - Size scale
- 2.5.2 [x] `scale_size_continuous()` - Continuous size scale
- 2.5.3 [x] `scale_size_discrete()` - Discrete size scale
- 2.5.4 [x] `scale_size_binned()` - Binned size scale
- 2.5.5 [x] `scale_size_area()` - Size by area (proportional)
- 2.5.6 [x] `scale_radius()` - Size by radius

### 2.6 Position Adjustments

- 2.6.1 [x] `position_nudge()` - Nudge by fixed amount

### 2.7 Other Functions

- 2.7.1 [x] `expansion()` - Control axis expansion
- 2.7.2 [x] `vars()` - Quote facet variables
- 2.7.3 [x] `after_scale()` - Reference scaled aesthetics

### Phase 2 Test Coverage

- [x] `stat_bin()`
- [x] `stat_density()`
- [x] `stat_smooth()`
- [x] `stat_ydensity()`
- [x] `stat_ecdf()`
- [x] `stat_qq()`
- [x] `stat_qq_line()`
- [x] `geom_qq()`
- [x] `geom_qq_line()`
- [x] `geom_freqpoly()`
- [x] `geom_jitter()`
- [x] `scale_colour_brewer()` / `scale_color_brewer()`
- [x] `scale_fill_brewer()`
- [x] `scale_colour_distiller()` / `scale_color_distiller()`
- [x] `scale_fill_distiller()`
- [x] `scale_colour_grey()` / `scale_color_grey()`
- [x] `scale_fill_grey()`
- [x] `scale_colour_gradientn()` / `scale_color_gradientn()`
- [x] `scale_fill_gradientn()`
- [x] `scale_alpha()`
- [x] `scale_alpha_continuous()`
- [x] `scale_alpha_discrete()`
- [x] `scale_alpha_binned()`
- [x] `scale_size()`
- [x] `scale_size_continuous()`
- [x] `scale_size_discrete()`
- [x] `scale_size_binned()`
- [x] `scale_size_area()`
- [x] `scale_radius()`
- [x] `position_nudge()`
- [x] `expansion()`
- [x] `vars()`
- [x] `after_scale()`

---

## Phase 3: Advanced Features

### 3.1 Additional Geoms

- 3.1.1 [ ] `geom_polygon()` - Filled polygons
- 3.1.2 [ ] `geom_hex()` - Hexagonal binning
- 3.1.3 [ ] `geom_dotplot()` - Dot plots
- 3.1.4 [ ] `geom_curve()` - Curved line segments
- 3.1.5 [ ] `geom_spoke()` - Radial line segments from point
- 3.1.6 [ ] `geom_density_2d()` - 2D density contour lines
- 3.1.7 [ ] `geom_density_2d_filled()` - Filled 2D density contours
- 3.1.8 [ ] `geom_errorbarh()` - Horizontal error bars
- 3.1.9 [ ] `geom_raster()` - Fast rectangle rendering

### 3.2 Additional Stats

- 3.2.1 [ ] `stat_ellipse()` - Confidence ellipses for normal data
- 3.2.2 [ ] `stat_summary_bin()` - Binned summaries
- 3.2.3 [ ] `stat_unique()` - Remove duplicate observations
- 3.2.4 [ ] `stat_function()` - Compute y from function of x

### 3.3 Coordinate Transforms

- 3.3.1 [ ] `coord_trans()` - Arbitrary coordinate transformations

### 3.4 Additional Themes

- 3.4.1 [ ] `theme_void()` - Completely blank canvas
- 3.4.2 [ ] `theme_light()` - Light background theme
- 3.4.3 [ ] `theme_dark()` - Dark background theme
- 3.4.4 [ ] `theme_linedraw()` - Black lines, no grey

### 3.5 Secondary Axes

- 3.5.1 [ ] `sec_axis()` - Specify secondary axis
- 3.5.2 [ ] `dup_axis()` - Duplicate primary axis

### 3.6 Guide Enhancements

- 3.6.1 [ ] `guide_axis()` - Axis guide customization
- 3.6.2 [ ] `guide_none()` - Suppress guide display
- 3.6.3 [ ] `guide_bins()` - Binned legend

### 3.7 Facet Label Functions

- 3.7.1 [ ] `labeller()` - Construct labelling function
- 3.7.2 [ ] `label_value()` - Label with variable value only
- 3.7.3 [ ] `label_both()` - Label with variable name and value
- 3.7.4 [ ] `label_context()` - Context-aware labels
- 3.7.5 [ ] `label_parsed()` - Parse labels as expressions
- 3.7.6 [ ] `label_wrap_gen()` - Wrap long labels

### 3.8 Shape Scales

- 3.8.1 [ ] `scale_shape()` - Shape scale
- 3.8.2 [ ] `scale_shape_discrete()` - Discrete shape scale
- 3.8.3 [ ] `scale_shape_binned()` - Binned shape scale
- 3.8.4 [ ] `scale_shape_manual()` - Manual shape mapping

### 3.9 Linetype Scales

- 3.9.1 [ ] `scale_linetype()` - Linetype scale
- 3.9.2 [ ] `scale_linetype_discrete()` - Discrete linetype scale
- 3.9.3 [ ] `scale_linetype_manual()` - Manual linetype mapping

### 3.10 Annotation Enhancements

- 3.10.1 [ ] `annotation_logticks()` - Log tick mark annotations
- 3.10.2 [ ] `annotation_custom()` - Custom grob annotations

---

## Phase 4: Specialized Features

### 4.1 Spatial/Map Support

- 4.1.1 [ ] `geom_sf()` - Simple Features geometry
- 4.1.2 [ ] `geom_sf_text()` - Text labels for SF
- 4.1.3 [ ] `geom_sf_label()` - Labels with background for SF
- 4.1.4 [ ] `stat_sf()` - SF statistics
- 4.1.5 [ ] `stat_sf_coordinates()` - Extract SF coordinates
- 4.1.6 [ ] `coord_sf()` - Coordinate system for SF
- 4.1.7 [ ] `coord_map()` - Map projections
- 4.1.8 [ ] `coord_quickmap()` - Quick map approximation
- 4.1.9 [ ] `geom_map()` - Polygon maps
- 4.1.10 [ ] `annotation_map()` - Map annotation
- 4.1.11 [ ] `borders()` - Map borders

### 4.2 Hexagonal Binning

- 4.2.1 [ ] `stat_bin_hex()` - Hexagonal binning statistics
- 4.2.2 [ ] `stat_summary_hex()` - Hexagonal summary statistics

### 4.3 Quantile Regression

- 4.3.1 [ ] `geom_quantile()` - Quantile regression lines
- 4.3.2 [ ] `stat_quantile()` - Quantile regression statistics

### 4.4 Identity Scales

- 4.4.1 [ ] `scale_colour_identity()` / `scale_color_identity()` - Use color values directly
- 4.4.2 [ ] `scale_fill_identity()` - Use fill values directly
- 4.4.3 [ ] `scale_size_identity()` - Use size values directly
- 4.4.4 [ ] `scale_shape_identity()` - Use shape values directly
- 4.4.5 [ ] `scale_linetype_identity()` - Use linetype values directly
- 4.4.6 [ ] `scale_alpha_identity()` - Use alpha values directly

### 4.5 Binned Color Scales

- 4.5.1 [ ] `scale_colour_steps()` / `scale_color_steps()` - Binned sequential colors
- 4.5.2 [ ] `scale_fill_steps()` - Binned sequential fill
- 4.5.3 [ ] `scale_colour_steps2()` / `scale_color_steps2()` - Binned diverging colors
- 4.5.4 [ ] `scale_fill_steps2()` - Binned diverging fill
- 4.5.5 [ ] `scale_colour_stepsn()` / `scale_color_stepsn()` - Binned n-color
- 4.5.6 [ ] `scale_fill_stepsn()` - Binned n-color fill

### 4.6 Binned Position Scales

- 4.6.1 [ ] `scale_x_binned()` - Binned x-axis
- 4.6.2 [ ] `scale_y_binned()` - Binned y-axis

### 4.7 Time Scales

- 4.7.1 [ ] `scale_x_time()` - Time-of-day scale for x-axis
- 4.7.2 [ ] `scale_y_time()` - Time-of-day scale for y-axis

### 4.8 Advanced Guides

- 4.8.1 [ ] `guide_coloursteps()` / `guide_colorsteps()` - Stepped color guide
- 4.8.2 [ ] `guide_axis_logticks()` - Log-scale axis with ticks
- 4.8.3 [ ] `guide_axis_stack()` - Stacked axes
- 4.8.4 [ ] `guide_axis_theta()` - Angular axis for polar coords
- 4.8.5 [ ] `guide_custom()` - Custom guide implementation

### 4.9 Miscellaneous

- 4.9.1 [ ] `qplot()` / `quickplot()` - Quick plotting function
- 4.9.2 [ ] `annotation_raster()` - Raster image annotations
- 4.9.3 [ ] `coord_radial()` - Modern polar alternative
- 4.9.4 [ ] `geom_function()` - Draw arbitrary function
- 4.9.5 [ ] `theme_test()` - Testing theme
- 4.9.6 [ ] `scale_colour_fermenter()` / `scale_fill_fermenter()` - Binned ColorBrewer

---

## Summary

| Phase     | Items         | Focus                                                |
|-----------|---------------|------------------------------------------------------|
| Phase 1   | 22 items      | Core functionality, transforms, dates, annotations   |
| Phase 2   | 28 items      | Statistics, color palettes, size/alpha scales        |
| Phase 3   | 33 items      | Advanced geoms, themes, secondary axes, facet labels |
| Phase 4   | 42 items      | Spatial, specialized binning, identity scales        |
| **Total** | **125 items** |                                                      |

---

## References

- [ggplot2 Function Reference](https://ggplot2.tidyverse.org/reference/index.html)
- [ggplot2 Cheat Sheet](https://rstudio.github.io/cheatsheets/html/data-visualization.html)
