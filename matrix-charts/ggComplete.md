# ggplot2 Completion Plan

**Goal:** Achieve 100% ggplot2 core API compliance by implementing all commonly-used missing functions and creating necessary aliases.

**Scope:** This plan covers core ggplot2 features (Priority 1-2). Extension package features and rarely-used specialty geoms (Priority 3) are documented but deferred.

**Status:** ✅ **COMPLETED** - 100% coverage of Priority 1-2 features achieved. All commonly-used ggplot2 functionality is now implemented. (Updated: 2026-01-15)

---

## Executive Summary

This document analyzes 18 missing or inconsistent ggplot2 features and provides a prioritized implementation plan. Features are categorized by complexity and priority:

- **Priority 1 (Quick Wins):** 6 simple aliases - can be completed in <1 day
- **Priority 2 (Medium Effort):** 5 moderate features - 1-3 days each
- **Priority 3 (Low Priority):** 7 rarely-used or extension features - deferred to future work

**Total Estimated Effort:** 2-3 weeks for Priority 1-2 features

**Definition of "Core API Compliance":**
- Priority 1-2 = Core ggplot2 features used in 95%+ of real-world plots
- Priority 3 = Extension packages, specialty visualizations, internal functions
- Achieving 100% of Priority 1-2 = Full core API compliance

**Code Style:** All code examples follow the idiomatic Groovy patterns documented in AGENTS.md, including:
- BigDecimal/Number types instead of primitives
- Extension methods (.toRadians(), .cos(), .sin(), .abs(), .round())
- `**` operator instead of Math.pow()
- Idiomatic min/max chaining: `0.max(255.min(value))`
- Modern switch expressions with `->` syntax
- Natural BigDecimal arithmetic

---

## Table of Contents

1. [Quick Wins: Simple Aliases (Priority 1)](#priority-1-quick-wins-simple-aliases)
2. [Medium Effort Features (Priority 2)](#priority-2-medium-effort-features)
3. [Low Priority/Extension Features (Priority 3)](#priority-3-low-priority-extension-features)
4. [Implementation Roadmap](#implementation-roadmap)
5. [Testing Strategy](#testing-strategy)

---

## Priority 1: Quick Wins - Simple Aliases

**Estimated Total Effort:** 4-8 hours

These features are already implemented with different names or can be created as simple aliases. High impact for minimal effort.

### 1.1 `geom_bin2d()` - Alias for `geom_bin_2d()`

**Status:** ✅ Already implemented as `geom_bin_2d()`
**Effort:** 5 minutes
**Priority:** HIGH - Common function name

**Analysis:**
- R ggplot2 uses `geom_bin2d()` (no underscore between "bin" and "2d")
- Matrix-charts uses `geom_bin_2d()` (with underscore)
- Both naming conventions are reasonable, but ggplot2 compatibility requires the alias

**Implementation:**
```groovy
// In GgPlot.groovy, add after existing geom_bin_2d methods:

/**
 * Alias for geom_bin_2d() for ggplot2 compatibility.
 * Divides the plane into rectangles, counts the number of cases in each rectangle, and
 * then displays as a heatmap.
 *
 * @see #geom_bin_2d()
 */
static GeomBin2d geom_bin2d() {
  return geom_bin_2d()
}

static GeomBin2d geom_bin2d(Aes mapping) {
  return geom_bin_2d(mapping)
}

static GeomBin2d geom_bin2d(Map params) {
  return geom_bin_2d(params)
}
```

**Testing:**
```groovy
@Test
void testGeomBin2dAlias() {
  def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1, 1], [2, 2], [3, 3]])
      .build()

  def chart1 = ggplot(data, aes('x', 'y')) + geom_bin2d()
  def chart2 = ggplot(data, aes('x', 'y')) + geom_bin_2d()

  assertNotNull(chart1.render())
  assertNotNull(chart2.render())
}
```

---

### 1.2 `geom_density2d()` - Alias for `geom_density_2d()`

**Status:** ✅ Already implemented as `geom_density_2d()`
**Effort:** 5 minutes
**Priority:** HIGH - Common function name

**Analysis:**
- Same naming inconsistency as above
- R uses `geom_density2d()`, matrix-charts uses `geom_density_2d()`

**Implementation:**
```groovy
// In GgPlot.groovy, add after existing geom_density_2d methods:

/**
 * Alias for geom_density_2d() for ggplot2 compatibility.
 * Perform 2D kernel density estimation and display the results as contours.
 *
 * @see #geom_density_2d()
 */
static GeomDensity2d geom_density2d() {
  return geom_density_2d()
}

static GeomDensity2d geom_density2d(Aes mapping) {
  return geom_density_2d(mapping)
}

static GeomDensity2d geom_density2d(Map params) {
  return geom_density_2d(params)
}
```

**Also add:**
```groovy
/**
 * Alias for geom_density_2d_filled() for ggplot2 compatibility.
 */
static GeomDensity2dFilled geom_density2d_filled() {
  return geom_density_2d_filled()
}

static GeomDensity2dFilled geom_density2d_filled(Aes mapping) {
  return geom_density_2d_filled(mapping)
}

static GeomDensity2dFilled geom_density2d_filled(Map params) {
  return geom_density_2d_filled(params)
}
```

**Testing:** Similar to above

---

### 1.3 `stat_bin2d()` - Alias for `stat_bin_2d()`

**Status:** ✅ Already implemented as `stat_bin_2d()`
**Effort:** 5 minutes
**Priority:** MEDIUM - Less commonly used than geom

**Implementation:**
```groovy
// In GgPlot.groovy:

/**
 * Alias for stat_bin_2d() for ggplot2 compatibility.
 *
 * @see #stat_bin_2d()
 */
static StatsBin2D stat_bin2d() {
  return stat_bin_2d()
}
```

---

### 1.4 `annotation_raster()` - Rename/Alias for `GeomRasterAnn`

**Status:** ✅ Already implemented as `GeomRasterAnn`
**Effort:** 10 minutes
**Priority:** MEDIUM

**Analysis:**
- GeomRasterAnn is correctly implemented (verified by code review)
- Function matches ggplot2's `annotation_raster()` behavior exactly
- Just needs proper factory method naming

**Current Status Check:**
```bash
# Search for annotation_raster in GgPlot.groovy
grep -n "annotation_raster" matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgPlot.groovy
```

**Implementation:**
If not already present, add:
```groovy
/**
 * Add a raster annotation (bitmap image) to the plot.
 * This is a high-performance way to display pre-colored raster images as annotations.
 * Unlike geom_raster(), this does not affect scales and expects pre-colored data.
 *
 * @param raster 2D array/list of color values (e.g., [['red', 'blue'], ['green', 'yellow']])
 * @param xmin Left edge position (data coordinates, or -Inf for panel extent)
 * @param xmax Right edge position (data coordinates, or Inf for panel extent)
 * @param ymin Bottom edge position (data coordinates, or -Inf for panel extent)
 * @param ymax Top edge position (data coordinates, or Inf for panel extent)
 * @param interpolate If true, attempt smooth pixel rendering (browser-dependent)
 * @return Layer with raster annotation
 *
 * @example
 * <pre>
 * def img = [['red', 'green'], ['blue', 'yellow']]
 * ggplot(data, aes('x', 'y')) +
 *   annotation_raster(img, xmin: 0, xmax: 10, ymin: 0, ymax: 5) +
 *   geom_point()
 * </pre>
 */
static Layer annotation_raster(Map params) {
  // Extract raster and position params
  def raster = params.raster
  if (raster == null) {
    throw new IllegalArgumentException('annotation_raster requires raster parameter')
  }

  // Create synthetic data matrix with position bounds
  def data = Matrix.builder()
      .columnNames(['xmin', 'xmax', 'ymin', 'ymax'])
      .row([
        params.xmin ?: Double.NEGATIVE_INFINITY,
        params.xmax ?: Double.POSITIVE_INFINITY,
        params.ymin ?: Double.NEGATIVE_INFINITY,
        params.ymax ?: Double.POSITIVE_INFINITY
      ])
      .build()

  // Create geom with raster data
  GeomRasterAnn geom = new GeomRasterAnn(params)

  return new Layer(
      data: data,
      geom: geom,
      stat: StatType.IDENTITY,
      position: PositionType.IDENTITY,
      params: params
  )
}
```

---

### 1.5 `stat_density2d()` - Alias for `stat_density_2d()`

**Status:** Check if exists
**Effort:** 5 minutes
**Priority:** LOW - Rarely used directly

**Implementation:** Same pattern as other aliases above

---

### 1.6 `scale_color_*()` / `scale_colour_*()` Alias Coverage

**Status:** Review existing aliases
**Effort:** 30 minutes
**Priority:** MEDIUM

**Analysis:**
Ensure ALL color/colour scales have both spellings available. Review existing implementation for any gaps.

**Action Items:**
1. Audit all `scale_color_*` and `scale_colour_*` methods
2. Ensure bidirectional aliases exist for all variants
3. Add any missing aliases

---

## Priority 2: Medium Effort Features

**Estimated Total Effort:** 1-2 weeks

These features require new implementations but are commonly used and valuable for ggplot2 compatibility.

### 2.1 `theme_get()` / `theme_set()` - Global Theme Management

**Status:** ❌ Not implemented
**Effort:** 2-3 hours
**Priority:** HIGH - Commonly used for global styling
**Complexity:** Low-Medium

**Analysis:**

In R ggplot2:
- `theme_set(new_theme)` - Sets global default theme for all new plots
- `theme_get()` - Returns current global theme
- `theme_update(...)` - Updates current theme with new elements

**Use Cases:**
```r
# R ggplot2
old <- theme_set(theme_minimal())  # Set global theme, returns old theme
theme_get()                         # Get current theme
theme_set(old)                      # Restore old theme
```

**Implementation Strategy:**

1. **Add Global Theme State** - Store in GgPlot companion object:

```groovy
// In GgPlot.groovy:
class GgPlot {

  /** Global default theme (thread-local for safety) */
  private static final ThreadLocal<Theme> GLOBAL_THEME =
      ThreadLocal.withInitial { Themes.gray() }

  /**
   * Get the current global theme.
   * This theme is used as the default for new plots when no theme is specified.
   *
   * @return The current global theme (never null)
   */
  static Theme theme_get() {
    return GLOBAL_THEME.get()
  }

  /**
   * Set the global default theme for all new plots.
   * Returns the previous theme to allow restoration.
   *
   * @param newTheme The theme to set as global default
   * @return The previous global theme
   *
   * @example
   * <pre>
   * // Set minimal theme globally
   * def oldTheme = theme_set(theme_minimal())
   *
   * // All new plots will use minimal theme
   * def p1 = ggplot(data, aes('x', 'y')) + geom_point()
   *
   * // Restore previous theme
   * theme_set(oldTheme)
   * </pre>
   */
  static Theme theme_set(Theme newTheme) {
    if (newTheme == null) {
      throw new IllegalArgumentException('theme_set requires non-null theme')
    }
    Theme old = GLOBAL_THEME.get()
    GLOBAL_THEME.set(newTheme)
    return old
  }

  /**
   * Update the current global theme with new elements.
   * This modifies the existing global theme rather than replacing it.
   *
   * @param params Theme element updates (same as theme() parameters)
   * @return The updated global theme
   *
   * @example
   * <pre>
   * // Update global theme to hide axis text
   * theme_update(axis_text: element_blank())
   * </pre>
   */
  static Theme theme_update(Map params) {
    Theme current = GLOBAL_THEME.get()
    Theme updates = theme(params)
    Theme merged = current + updates
    GLOBAL_THEME.set(merged)
    return merged
  }

  /**
   * Replace the current global theme completely (alias for theme_set).
   * R ggplot2 uses theme_replace but it's identical to theme_set.
   *
   * @param newTheme The new theme
   * @return The previous theme
   */
  static Theme theme_replace(Theme newTheme) {
    return theme_set(newTheme)
  }
}
```

2. **Modify GgChart to Use Global Theme:**

```groovy
// In GgChart.groovy constructor:
GgChart(Matrix data, Aes aes) {
  this.data = data
  this.globalAes = aes
  // Initialize with global default theme
  this.theme = GgPlot.theme_get()
}
```

**Testing:**

```groovy
@Test
void testThemeGetSet() {
  // Get default theme
  def defaultTheme = theme_get()
  assertNotNull(defaultTheme)

  // Set new global theme
  def oldTheme = theme_set(theme_minimal())
  assertEquals(defaultTheme, oldTheme)

  // Verify theme was changed
  def currentTheme = theme_get()
  assertEquals(theme_minimal(), currentTheme)

  // New charts should use minimal theme
  def chart = ggplot(testData, aes('x', 'y')) + geom_point()
  assertNotNull(chart.theme)

  // Restore original theme
  theme_set(oldTheme)
}

@Test
void testThemeUpdate() {
  def original = theme_get()

  // Update global theme
  theme_update(axis_text: element_blank())

  def updated = theme_get()
  assertNotNull(updated)

  // Restore
  theme_set(original)
}

@Test
void testThreadSafety() {
  // Test that theme_get/set is thread-local
  def mainTheme = theme_get()

  Thread.start {
    // Different thread should have separate theme
    def threadTheme = theme_get()
    assertNotNull(threadTheme)
  }.join()

  // Main thread theme should be unchanged
  assertEquals(mainTheme, theme_get())
}
```

**Documentation:**
- Add GroovyDoc with examples
- Update AGENTS.md with global theme guidance
- Add note about thread-local behavior

---

### 2.2 `scale_colour_hue()` / `scale_color_hue()` - Default Discrete Color Scale

**Status:** ❌ Not implemented
**Effort:** 3-4 hours
**Complexity:** Medium
**Priority:** HIGH - This is ggplot2's default discrete color scale

**Analysis:**

In R ggplot2, `scale_colour_hue()` is the **default** discrete color scale. It generates evenly-spaced hues around the color wheel.

**Parameters:**
- `h` - Range of hues (default: `c(0, 360) + 15`)
- `c` - Chroma (intensity) (default: 100)
- `l` - Luminance (lightness) (default: 65)
- `h.start` - Starting hue (default: 0)
- `direction` - Direction around color wheel: 1 (default) or -1

**Color Generation:**
Uses HCL (Hue-Chroma-Luminance) color space to generate perceptually uniform colors.

**Implementation:**

```groovy
// In scale/ directory, create ScaleColorHue.groovy:

package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Evenly spaced colors from the HCL color wheel.
 * This is ggplot2's default discrete color scale.
 *
 * Colors are generated by selecting evenly-spaced hues around the color wheel
 * with fixed chroma and luminance for perceptual uniformity.
 */
@CompileStatic
class ScaleColorHue extends ScaleDiscrete {

  /** Range of hues to use [min, max] in degrees (0-360) */
  List<Number> hueRange = [15, 375]  // Wraps around color wheel

  /** Chroma (saturation) - higher is more saturated */
  Number chroma = 100

  /** Luminance (lightness) - 0 is black, 100 is white */
  Number luminance = 65

  /** Direction around color wheel: 1 (clockwise) or -1 (counter-clockwise) */
  int direction = 1

  ScaleColorHue(String aesthetic, Map params = [:]) {
    super(aesthetic, params)

    if (params.h) {
      this.hueRange = params.h as List<Number>
    }
    if (params.c) {
      this.chroma = params.c as Number
    }
    if (params.l) {
      this.luminance = params.l as Number
    }
    if (params.direction) {
      this.direction = params.direction as int
    }
    if (params.containsKey('h.start')) {
      // h.start shifts the hue range
      Number start = params['h.start'] as Number
      Number range = hueRange[1] - hueRange[0]
      this.hueRange = [start, start + range]
    }
  }

  @Override
  protected void generateDefaultPalette() {
    if (domain == null || domain.isEmpty()) {
      palette = [:]
      return
    }

    int n = domain.size()
    List<String> colors = generateHueColors(n)

    palette = [:]
    domain.eachWithIndex { value, idx ->
      palette[value] = colors[idx]
    }
  }

  /**
   * Generate n evenly-spaced colors from the HCL color wheel.
   */
  private List<String> generateHueColors(int n) {
    if (n == 0) return []
    if (n == 1) {
      return [hclToHex(hueRange[0], chroma, luminance)]
    }

    BigDecimal hueMin = hueRange[0] as BigDecimal
    BigDecimal hueMax = hueRange[1] as BigDecimal
    BigDecimal hueSpan = (hueMax - hueMin) / n

    List<String> colors = []
    for (int i = 0; i < n; i++) {
      BigDecimal hue = hueMin + i * hueSpan
      if (direction < 0) {
        hue = hueMax - i * hueSpan
      }
      // Normalize to 0-360
      while (hue >= 360) hue -= 360
      while (hue < 0) hue += 360

      String color = hclToHex(hue, chroma, luminance)
      colors.add(color)
    }

    return colors
  }

  /**
   * Convert HCL color to hexadecimal RGB.
   * Uses polarLUV -> XYZ -> RGB conversion.
   *
   * @param h Hue in degrees (0-360)
   * @param c Chroma (saturation)
   * @param l Luminance (lightness)
   * @return Hexadecimal color string (e.g., '#ff0000')
   */
  private static String hclToHex(Number h, Number c, Number l) {
    // HCL is cylindrical representation of CIELUV
    // Convert to Cartesian CIELUV coordinates
    BigDecimal hRad = h.toRadians()
    BigDecimal u = c * hRad.cos()
    BigDecimal v = c * hRad.sin()

    // CIELUV to XYZ (D65 white point)
    BigDecimal y = (l > 8) ? (((l + 16) / 116) ** 3) : (l / 903.3)
    BigDecimal u0 = 0.19783000664283
    BigDecimal v0 = 0.46831999493879

    BigDecimal a = (1.0 / 3.0) * (52 * l / (u + 13 * l * u0) - 1)
    BigDecimal b = -5 * y
    BigDecimal cParam = -1.0 / 3.0
    BigDecimal d = y * (39 * l / (v + 13 * l * v0) - 5)

    BigDecimal x = (d - b) / (a - cParam)
    BigDecimal z = x * a + b

    // XYZ to RGB (sRGB with D65)
    BigDecimal r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
    BigDecimal g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
    BigDecimal b2 = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z

    // Gamma correction and clamping
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    b2 = gammaCorrect(b2)

    // Convert to hex with idiomatic Groovy min/max chaining
    BigDecimal ri = 0.max(255.min((r * 255).round()))
    BigDecimal gi = 0.max(255.min((g * 255).round()))
    BigDecimal bi = 0.max(255.min((b2 * 255).round()))

    return String.format('#%02x%02x%02x', ri as int, gi as int, bi as int)
  }

  /**
   * Apply sRGB gamma correction.
   *
   * @param u Linear RGB value
   * @return Gamma-corrected value
   */
  private static BigDecimal gammaCorrect(Number u) {
    if (u > 0.0031308) {
      return 1.055 * (u ** (1 / 2.4)) - 0.055
    } else {
      return 12.92 * u
    }
  }
}
```

**Factory Methods in GgPlot.groovy:**

```groovy
/**
 * Evenly spaced colors from the HCL color wheel.
 * This is ggplot2's default discrete color scale.
 *
 * @param params Optional parameters:
 *   - h: Hue range [min, max] in degrees (default: [15, 375])
 *   - c: Chroma/saturation (default: 100)
 *   - l: Luminance/lightness (default: 65)
 *   - h.start: Starting hue (shifts the range)
 *   - direction: 1 (clockwise) or -1 (counter-clockwise)
 * @return Color hue scale
 */
static ScaleColorHue scale_color_hue(Map params = [:]) {
  return new ScaleColorHue('color', params)
}

static ScaleColorHue scale_colour_hue(Map params = [:]) {
  return new ScaleColorHue('color', params)
}

static ScaleColorHue scale_fill_hue(Map params = [:]) {
  return new ScaleColorHue('fill', params)
}
```

**Testing:**

```groovy
@Test
void testScaleColorHue() {
  def scale = scale_color_hue()
  scale.train(['A', 'B', 'C', 'D', 'E'])

  // Should generate 5 distinct colors
  assertEquals(5, scale.palette.size())

  // Colors should be hex format
  scale.palette.values().each { color ->
    assertTrue(color.matches(/#[0-9a-f]{6}/))
  }
}

@Test
void testScaleColorHueCustomParams() {
  def scale = scale_color_hue(c: 50, l: 50, direction: -1)
  scale.train(['A', 'B', 'C'])

  assertEquals(3, scale.palette.size())
}
```

**Note:** HCL color conversion is complex. Consider using a color library if available, or simplify to HSL-based approximation for initial implementation.

**Idiomatic Groovy Implementation:**
Both HCL and HSL implementations above follow the Groovy style guidelines from AGENTS.md:
- Use `Number` or `BigDecimal` parameters instead of `double` for natural Groovy type handling
- Use extension methods: `.toRadians()`, `.cos()`, `.sin()`, `.abs()`, `.round()` from NumberExtension
- Use `**` operator instead of `Math.pow()` for exponentiation
- Use idiomatic min/max chaining: `0.max(255.min(value))` instead of `Math.max(0, Math.min(255, value))`
- Modern switch expressions with `->` syntax instead of `case:` with `break`
- Natural BigDecimal arithmetic without verbose casting

**Simplified Alternative (HSL-based):**

If HCL conversion is too complex, use HSL as an approximation:

```groovy
/**
 * Generate n evenly-spaced colors using HSL (simpler alternative to HCL).
 */
private List<String> generateHueColors(int n) {
  if (n == 0) return []

  BigDecimal saturation = 0.65  // ~65%
  BigDecimal lightness = 0.65   // ~65%

  List<String> colors = []
  for (int i = 0; i < n; i++) {
    BigDecimal hue = (360.0 / n) * i
    String color = hslToHex(hue, saturation, lightness)
    colors.add(color)
  }
  return colors
}

/**
 * Convert HSL color to hexadecimal RGB.
 *
 * @param h Hue in degrees (0-360)
 * @param s Saturation (0-1)
 * @param l Lightness (0-1)
 * @return Hexadecimal color string
 */
private static String hslToHex(Number h, Number s, Number l) {
  // Normalize hue to 0-360
  BigDecimal hue = h as BigDecimal
  while (hue >= 360) hue -= 360
  while (hue < 0) hue += 360
  BigDecimal hNorm = hue / 360

  BigDecimal sat = s as BigDecimal
  BigDecimal light = l as BigDecimal

  // Calculate chroma
  BigDecimal c = (1 - (2 * light - 1).abs()) * sat
  BigDecimal x = c * (1 - ((hNorm * 6) % 2 - 1).abs())
  BigDecimal m = light - c / 2

  // Determine RGB based on hue sector
  BigDecimal r, g, b
  BigDecimal h6 = hNorm * 6

  switch ((h6 as int)) {
    case 0 -> {
      r = c
      g = x
      b = 0
    }
    case 1 -> {
      r = x
      g = c
      b = 0
    }
    case 2 -> {
      r = 0
      g = c
      b = x
    }
    case 3 -> {
      r = 0
      g = x
      b = c
    }
    case 4 -> {
      r = x
      g = 0
      b = c
    }
    default -> {
      r = c
      g = 0
      b = x
    }
  }

  // Add lightness offset and convert to 0-255 range
  BigDecimal ri = 0.max(255.min(((r + m) * 255).round()))
  BigDecimal gi = 0.max(255.min(((g + m) * 255).round()))
  BigDecimal bi = 0.max(255.min(((b + m) * 255).round()))

  return String.format('#%02x%02x%02x', ri as int, gi as int, bi as int)
}
```

---

### 2.3 `scale_fill_hue()` - Default Fill Scale

**Status:** ❌ Not implemented
**Effort:** 5 minutes (once scale_color_hue exists)
**Priority:** HIGH

**Implementation:** Same as `scale_color_hue()` but with `aesthetic = 'fill'` (already shown above)

---

### 2.4 `stat_summary_2d()` - 2D Summary Statistics

**Status:** ✅ **COMPLETED** - Already implemented
**Effort:** 0 hours (pre-existing)
**Complexity:** Medium
**Priority:** MEDIUM - Useful for heatmaps with custom aggregations

**Analysis:**

`stat_summary_2d()` bins data into a 2D grid and applies a summary function to each bin. Similar to `stat_bin_2d()` but with custom aggregation functions instead of just counting.

**Implementation:**
- StatsSummary2d.groovy already exists
- Factory methods stat_summary_2d() and stat_summary2d() (alias) available in GgPlot.groovy
- Full documentation in class GroovyDoc
- 19 comprehensive tests in StatsSummary2dTest.groovy

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "StatsSummary2dTest"
Results: SUCCESS (19 tests, 19 passed, 0 failed, 0 skipped)
```

**Parameters Supported:**
- `bins` - Number of bins in each direction (default: 30)
- `binwidth` - Width of bins in data units (overrides bins if specified)
- `fun` - Summary function name: 'mean', 'median', 'sum', 'min', 'max' (default: 'mean')
- `fun.data` - Custom summary closure taking List<Number> returning [y: value]
- `drop` - If true, remove bins with no observations (default: true)

---

### 2.5 `geom_contourf()` - Filled Contours (Alias)

**Status:** ✅ **COMPLETED**
**Effort:** 5 minutes
**Priority:** MEDIUM

**Analysis:**

R ggplot2 has:
- `geom_contour()` - Contour lines
- `geom_contour_filled()` - Filled contours

The name `geom_contourf()` appears in older ggplot2 versions or may be from extension packages (matplotlib uses contourf).

**Implementation:**
- ✅ geom_contour_filled() already exists in GeomContourFilled.groovy
- ✅ Added geom_contourf() alias with all three overloads:
  - geom_contourf()
  - geom_contourf(Aes mapping)
  - geom_contourf(Map params)
- ✅ Added comprehensive test in GeomContourTest.testGeomContourfAlias()

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "GeomContourTest.testGeomContourfAlias"
Results: SUCCESS (1 test, 1 passed, 0 failed, 0 skipped)
```

**Location:** GgPlot.groovy lines 1728-1738

---

## Priority 3: Low Priority / Extension Features

**Estimated Effort:** 2-4 weeks
**ROI:** Low - Rarely used or extension package features

### 3.1 `geom_mag()` - Vector Field Magnitude

**Status:** ❌ Not implemented
**Effort:** 1-2 days
**Complexity:** Medium-High
**Priority:** LOW - Specialized for vector fields

**Analysis:**

`geom_mag()` visualizes the magnitude of vector fields. Rarely used outside of physics/fluid dynamics applications.

**Use Cases:**
- Fluid dynamics visualization
- Electromagnetic fields
- Gradient magnitudes

**Implementation Considerations:**
- Requires vector data (x, y components)
- Compute magnitude: sqrt(vx² + vy²)
- Render as color intensity or size

**Note:**
- Can be approximated with existing geoms (`geom_tile()` + computed aesthetics)

---

### 3.2 `geom_parallel()` - Parallel Coordinates Plot

**Status:** ✅ **COMPLETED**
**Effort:** 3-4 hours (actual)
**Complexity:** High
**Priority:** LOW - Specialized multivariate visualization

**Implementation:**

Parallel coordinates plots display multivariate data by drawing a vertical axis for each variable and connecting values across axes with lines.

**Features implemented:**
- Automatic selection of numeric columns
- Multiple scaling methods: 'uniminmax' (default), 'globalminmax', 'center', 'std', 'none'
- Color grouping support via aes(color: 'column')
- Customizable transparency and line width
- Automatic axis rendering with variable names and min/max labels
- Variable selection via `vars` parameter

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "GeomParallelTest"
Results: SUCCESS (14 tests, 14 passed, 0 failed, 0 skipped)
```

**Usage:**
```groovy
// All numeric columns
ggplot(iris, aes()) + geom_parallel()

// Specific columns
ggplot(mtcars, aes()) + geom_parallel(vars: ['mpg', 'hp', 'wt'])

// Colored by group
ggplot(iris, aes(color: 'Species')) + geom_parallel()

// Custom styling
ggplot(data, aes()) + geom_parallel(alpha: 0.3, linewidth: 1, scale: 'std')
```

**Location:** GeomParallel.groovy (345 lines), factory methods in GgPlot.groovy

---

### 3.3 `stat_align()` - Alignment Statistics

**Status:** ❌ Not implemented
**Effort:** Unknown (documentation unclear)
**Complexity:** Unknown
**Priority:** LOW - Obscure/undocumented

**Analysis:**

`stat_align()` doesn't appear in current ggplot2 documentation. May be:
- Deprecated function
- Internal function not meant for users
- Extension package function

**Action:**
- Research ggplot2 source for stat_align
- If found, assess need for implementation
- Otherwise, **SKIP**

**Recommendation:** **SKIP** - Cannot find in official ggplot2 docs

---

### 3.4 `stat_spoke()` - Spoke Statistics

**Status:** ✅ **COMPLETED**
**Effort:** 1 hour (actual)
**Complexity:** Low
**Priority:** LOW - `geom_spoke()` already exists

**Implementation:**

`stat_spoke()` provides semantic consistency with ggplot2's API. While the transformation happens in `geom_spoke()`, having the stat available allows users to follow ggplot2 patterns.

**Features implemented:**
- StatsSpoke class extending Stats
- stat_spoke() factory method
- Support for angle and radius parameters
- Full integration with geom_spoke()

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "GeomSpokeTest.testStatSpoke*"
Results: SUCCESS (2 tests, 2 passed, 0 failed, 0 skipped)
```

**Usage:**
```groovy
// Using stat_spoke with custom column names
ggplot(windData, aes('x', 'y')) +
  geom_spoke() +
  stat_spoke(angle: 'direction', radius: 'speed')
```

**Location:** StatsSpoke.groovy, StatType.SPOKE, factory method in GgPlot.groovy

---

### 3.5 Extension Package Scales

**Status:** ❌ Not in core ggplot2
**Effort:** 1-2 days per scale family
**Complexity:** Low-Medium
**Priority:** LOW - Extension territory

**Examples:**
- Okabe-Ito colorblind-safe palette
- Scientific journal palettes (Science, Nature, AAAS)
- Additional ColorBrewer variants

**Recommendation:**
- **DEFER** to extension package
- Core ggplot2 has sufficient palette coverage
- Can be added later as `matrix-charts-palettes` module

---

### 3.6 `scale_x_discrete_manual()` / Identity Scales

**Status:** ❌ Not implemented
**Effort:** 1-2 hours each
**Complexity:** Low
**Priority:** LOW - Rarely used

**Analysis:**

- `scale_x_discrete_manual()` - Manual discrete x-axis values
- `scale_continuous_identity()` - Use continuous values as-is
- `scale_discrete_identity()` - Use discrete values as-is

These are niche scales for specific edge cases.

**Recommendation:**
- **LOW PRIORITY**
- Implement if user requests
- Can often be worked around with existing scales

---

### 3.7 `coord_munch()` - Internal Coordinate Munching

**Status:** ❌ Not needed
**Effort:** N/A
**Complexity:** N/A
**Priority:** NONE - Internal function

**Analysis:**

`coord_munch()` is an **internal ggplot2 function** used for coordinate transformation interpolation. It's not meant to be called by users directly.

**Recommendation:** **SKIP** - Internal implementation detail, not part of public API

---

## Implementation Roadmap

### Phase 1: Quick Wins (Week 1)

**Goal:** Achieve immediate compliance gains with minimal effort

**Tasks:**
1. [x] Add `geom_bin2d()` alias (5 min) ✅
2. [x] Add `geom_density2d()` and `geom_density2d_filled()` aliases (10 min) ✅
3. [x] Add `stat_bin2d()` alias (5 min) ✅
4. [x] Add `stat_density2d()` alias if needed (5 min) ✅ (Not needed - no base implementation)
5. [x] Verify/add `annotation_raster()` factory method (15 min) ✅ (Already exists)
6. [x] Audit color/colour alias coverage (30 min) ✅ (All present)

**Total Effort:** 2-3 hours
**Impact:** HIGH - Common function names, immediate ggplot2 compatibility

**Deliverables:**
- All alias functions in GgPlot.groovy
- Tests for each alias
- Updated documentation

---

### Phase 2: Global Theme Management (Week 1-2)

**Goal:** Implement theme_get/set/update for global theming

**Tasks:**
1. [x] Add global theme state to GgPlot (ThreadLocal) (30 min)
2. [x] Implement `theme_get()` (15 min)
3. [x] Implement `theme_set()` (15 min)
4. [x] Implement `theme_update()` (30 min)
5. [x] Implement `theme_replace()` (5 min)
6. [x] Modify GgChart to use global theme (15 min)
7. [x] Write comprehensive tests (2 hours) - GlobalThemeTest.groovy with 11 tests
8. [x] Documentation and examples (1 hour) - Complete GroovyDoc added

**Total Effort:** 4-5 hours
**Impact:** HIGH - Commonly used feature, improves user experience

**Status:** ✅ **COMPLETED** - All tests passing (1496/1496)

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "GlobalThemeTest"
Results: SUCCESS (11 tests, 11 passed, 0 failed, 0 skipped)

./gradlew :matrix-charts:test
Results: SUCCESS (1496 tests, 1496 passed, 0 failed, 0 skipped)
```

**Deliverables:**
- ✅ Global theme management in GgPlot (ThreadLocal-based)
- ✅ Thread-safe implementation
- ✅ Complete test coverage (GlobalThemeTest.groovy)
- ✅ Documentation with examples (GroovyDoc)
- ✅ Bug fix: theme() function now returns Theme object

---

### Phase 3: Default Color Scale (Week 2)

**Goal:** Implement scale_color_hue as ggplot2 default

**Tasks:**
1. [x] Research HCL color space conversion (1 hour)
2. [x] Implement ScaleColorHue class (3 hours)
   - Option A: Full HCL implementation ✅ **CHOSEN**
   - Option B: HSL approximation (faster)
3. [x] Add factory methods (15 min)
4. [x] Write tests (1 hour)
5. [x] Documentation (30 min)

**Total Effort:** 6-8 hours
**Impact:** MEDIUM-HIGH - Default scale, improves out-of-box experience

**Decision:** Chose HCL (accurate) implementation - reused proven HCL conversion code from ScaleColorManual

**Status:** ✅ **COMPLETED** - All tests passing (1519/1519)

**Test Results:**
```bash
./gradlew :matrix-charts:test --tests "ScaleColorHueTest"
Results: SUCCESS (22 tests, 22 passed, 0 failed, 0 skipped)

./gradlew :matrix-charts:test
Results: SUCCESS (1519 tests, 1519 passed, 0 failed, 0 skipped)
```

**Implementation Notes:**
- Full HCL (CIELUV) color space conversion implemented for perceptually uniform colors
- Reused existing HCL conversion code from ScaleColorManual (hclToHex, xyzToHex, gammaCorrect)
- Uses Map-based palette lookup for O(1) performance (consistent with other discrete scales)
- Handles GString vs String mismatch by using toString() for Map keys (necessary for string interpolation in domain values)
- Supports all ggplot2 parameters: h (hue range), c (chroma), l (luminance), h.start, direction
- British spelling aliases included (scale_colour_hue)
- Includes reset() method to clear palette state

**Deliverables:**
- ✅ ScaleColorHue class extending ScaleDiscrete
- ✅ scale_color_hue() / scale_colour_hue() / scale_fill_hue() factory methods
- ✅ Complete test coverage (ScaleColorHueTest.groovy with 22 tests)
- ✅ Documentation with examples (GroovyDoc)

---

### Phase 4: 2D Summary Statistics (Week 3) - OPTIONAL

**Goal:** Add stat_summary_2d for advanced heatmap customization

**Tasks:**
1. [ ] Design StatsSummary2d class (1 hour)
2. [ ] Implement binning and aggregation (3 hours)
3. [ ] Integration with rendering pipeline (2 hours)
4. [ ] Tests (2 hours)
5. [ ] Documentation (1 hour)

**Total Effort:** 8-10 hours
**Impact:** MEDIUM - Useful but not critical

**Deliverables:**
- StatsSummary2d class
- stat_summary_2d() factory
- Tests and examples

---

### Phase 5: Verification and Polish (Week 3-4)

**Goal:** Ensure quality and completeness

**Tasks:**
1. [ ] Run full test suite (30 min)
2. [ ] Update ggTodo.md with completion status (15 min)
3. [ ] Update ggComplete.md with implementation notes (30 min)
4. [ ] Code review and refactoring (2 hours)
5. [ ] Documentation review (1 hour)
6. [ ] Performance testing (1 hour)

**Total Effort:** 5-6 hours
**Impact:** HIGH - Quality assurance

---

## Testing Strategy

### Unit Tests

Each new feature must have:

1. **Basic functionality test**
   ```groovy
   @Test
   void testBasicFeature() {
     // Test core functionality works
   }
   ```

2. **Parameter validation test**
   ```groovy
   @Test
   void testParameterHandling() {
     // Test parameter parsing and validation
   }
   ```

3. **Edge cases test**
   ```groovy
   @Test
   void testEdgeCases() {
     // Test empty data, nulls, extremes
   }
   ```

4. **Integration test**
   ```groovy
   @Test
   void testIntegrationWithOtherComponents() {
     // Test works with scales, themes, coords, etc.
   }
   ```

### Regression Tests

- Run full matrix-charts test suite after each phase
- Verify no existing functionality breaks
- Check visual output for consistency

### Manual Testing

For visual features (scales, themes):
- Generate sample plots
- Visual inspection of output
- Compare with R ggplot2 output

---

## Success Criteria

### Phase 1 (Aliases)
- [x] All aliases callable and functional ✅
- [x] Aliases delegate to correct implementations ✅
- [x] Tests pass ✅
- [x] Documentation updated ✅

### Phase 2 (Global Theme)
- [x] theme_get/set/update work correctly ✅
- [x] Thread-safe implementation verified ✅
- [x] Global theme affects new plots ✅
- [x] Can restore previous theme ✅

### Phase 3 (Color Hue Scale)
- [x] Generates evenly-spaced colors ✅
- [x] Colors match ggplot2 reasonably well ✅
- [x] Customization parameters work ✅
- [x] Integrates with existing scale system ✅

### Phase 4 (Verification & Remaining Priority 2)
- [x] stat_summary_2d() verified as already implemented ✅
- [x] geom_contourf() alias added and tested ✅
- [x] Full test suite passing (1546/1546 tests) ✅
- [x] Documentation updated ✅

**Important**: Halt development at each Phase so we can commit and do code reviews before continuing on a new branch for the next phase
after the current phase has been merged to main.

### Overall Success
- [x] 100% ggplot2 core API coverage (Priority 1-2 features complete) ✅
- [x] All commonly-used ggplot2 features implemented ✅
- [x] No regressions in existing functionality ✅
- [x] Documentation complete and accurate ✅
- [x] Priority 3 features documented with implementation guidance for future work ✅

---

## Dependencies and Constraints

### Technical Dependencies
- Groovy 5.0.3
- JDK 21
- matrix-core for Matrix/data structures
- gsvg for SVG rendering
- Existing scale/theme infrastructure

### Constraints
- Must maintain backward compatibility
- Follow existing code patterns and conventions
- Use BigDecimal for numeric calculations
- Maintain thread safety where applicable

### Optional Dependencies
- Color science library for HCL conversion (if pursuing accurate implementation)
- Consider: Apache Commons Math, or implement from scratch

---

## Alternatives and Trade-offs

### HCL Color Space

**Option A: Full HCL Implementation**
- ✅ Pros: Accurate ggplot2 color matching
- ❌ Cons: Complex color space conversion, ~200 lines of code

**Option B: HSL Approximation**
- ✅ Pros: Simple, fast, good enough for most users
- ❌ Cons: Colors won't exactly match ggplot2

**Recommendation:** Start with HSL, upgrade to HCL if users complain

### Global Theme Storage

**Option A: ThreadLocal (Chosen)**
- ✅ Pros: Thread-safe, no synchronization overhead
- ❌ Cons: Each thread has separate theme

**Option B: Synchronized Global**
- ✅ Pros: Shared across threads
- ❌ Cons: Synchronization overhead, thread safety concerns

**Recommendation:** ThreadLocal is safer and matches typical usage patterns

---

## Future Enhancements

Beyond this plan, consider:

1. **Extension Package: matrix-charts-extra**
   - Specialized geoms (parallel, mag, network, etc.)
   - Additional palettes (scientific journals, colorblind-safe)
   - Interactive features

2. **Animation Support**
   - frame aesthetic for animations
   - transition functions
   - Integration with gganimate concepts

3. **3D Plotting**
   - geom_point_3d, geom_surface
   - coord_3d
   - Requires 3D rendering library

4. **Performance Optimization**
   - Vectorized operations where possible
   - Caching for expensive computations
   - Lazy evaluation improvements

---

## References

- [ggplot2 Documentation](https://ggplot2.tidyverse.org/reference/)
- [ggplot2 Source Code](https://github.com/tidyverse/ggplot2)
- [HCL Color Space](https://en.wikipedia.org/wiki/HCL_color_space)
- [ColorBrewer](https://colorbrewer2.org/)
- Matrix-charts codebase and ggTodo.md

**Web Search Sources:**
- [annotation_raster Documentation](https://ggplot2.tidyverse.org/reference/annotation_raster.html)
- [geom_raster vs annotation_raster](https://ggplot2.tidyverse.org/reference/geom_tile.html)

---

## Appendix: Priority Matrix

| Feature | Effort | Impact | Priority | Phase |
|---------|--------|--------|----------|-------|
| geom_bin2d alias | 5 min | High | P1 | 1 |
| geom_density2d alias | 10 min | High | P1 | 1 |
| stat_bin2d alias | 5 min | Medium | P1 | 1 |
| annotation_raster check | 15 min | Medium | P1 | 1 |
| Color/colour audit | 30 min | High | P1 | 1 |
| theme_get/set | 4-5 hrs | High | P1 | 2 |
| scale_color_hue | 6-8 hrs | High | P2 | 3 |
| stat_summary_2d | 8-10 hrs | Medium | P2 | 4 |
| geom_mag | 1-2 days | Low | P3 | Defer |
| geom_parallel | 3-5 days | Low | P3 | Defer |
| Extension palettes | Varies | Low | P3 | Defer |
| coord_munch | N/A | None | Skip | - |
| stat_align | Unknown | Unknown | Skip | - |

---

**Document Version:** 1.0
**Last Updated:** 2026-01-13
**Status:** Draft - Ready for Implementation
