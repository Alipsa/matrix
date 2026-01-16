Add support for the following 3 ggplot2 functions to the se.alipsa.matrix.gg package.

1. geom_mag() - Vector field magnitude visualization (physics/fluid dynamics)
2. stat_align() - Alignment transformation for stacked area charts
3. Identity scales - scale_*_identity() edge cases and validation

## Stat_align

In `ggplot2`, **`stat_align()`** is a statistical transformation designed to align data points across different groups to a common set of x-coordinates.

It is primarily used as the default statistic for **`geom_area()`** to ensure that stacked area charts render correctly even when the data groups do not share the exact same x-values.

### What Problem Does it Solve?

In a stacked area chart, every layer (group) is stacked on top of the one below it. For this to work mathematically, every group must have a y-value defined for every x-value present in the plot.

If your data is "misaligned"—for example, Group A has a data point at x = 2 but Group B does not—`ggplot2` cannot stack them directly because there is no value for Group B to add to Group A at that specific x-coordinate.

### How `stat_align()` Works

1. **Interpolation:** It inspects all x-values across all groups in your dataset.
2. **Union of X:** It creates a master list of unique x-coordinates found in *any* of the groups.
3. **Alignment:** It interpolates the y-values for every group at these new x-coordinates. If a group didn't originally have a point at a specific x, `stat_align()` calculates what the value *would* be based on the line connecting its existing points.

### Practical Example

Imagine you are plotting server load over time for two servers:

* **Server A** reports data at: 1:00 PM and 1:10 PM.
* **Server B** reports data at: 1:05 PM and 1:15 PM.

If you try to `geom_area()` this without alignment, the stacking would fail or look jagged because the x-values don't match. `stat_align()` automatically generates interpolated values so that:

* **Server A** gets a calculated value at 1:05 PM and 1:15 PM.
* **Server B** gets a calculated value at 1:00 PM and 1:10 PM.
* Result: Both servers have data at 1:00, 1:05, 1:10, and 1:15, allowing them to stack perfectly.

### How to Use (or Disable) It

Because it is the default for `geom_area`, you are usually using it without knowing it:

```r
# stat_align is active by default here
ggplot(df, aes(x = time, y = value, fill = group)) +
  geom_area()

```

If you want to **disable** this behavior (e.g., you want to plot the raw polygons exactly as defined in your data without interpolation), you can switch the stat to "identity":

```r
# Turns off alignment/interpolation
ggplot(df, aes(x = time, y = value, fill = group)) +
  geom_area(stat = "identity")

```

There isn't a standalone documentation page dedicated solely to `stat_align()` because it is primarily an internal default for `geom_area()`. Its behavior is documented within the **Ribbons and area plots** reference page.

Here is the official documentation link:
**[https://ggplot2.tidyverse.org/reference/geom_ribbon.html](https://ggplot2.tidyverse.org/reference/geom_ribbon.html)**

### Where to find it on that page?

Look for the **Details** section or the **stat** argument description. It states:

> "To facilitate stacking, the default `stat = "align"` interpolates groups to a common set of x-coordinates. To turn off this interpolation, `stat = "identity"` can be used instead."

---

## geom_mag()

In `ggplot2`, **`geom_mag()`** is a specialized geometric layer for visualizing **vector field magnitudes** in physics, fluid dynamics, and other scientific contexts. It is primarily used to represent the **strength or intensity** of vectors at specific points in 2D space.

### What is it used for?

Vector fields are common in scientific visualization:
- **Physics**: Electric fields, magnetic fields, force fields
- **Fluid dynamics**: Velocity fields, pressure gradients, flow patterns
- **Meteorology**: Wind speed and direction
- **Engineering**: Stress fields, displacement fields

While `geom_spoke()` visualizes both magnitude and direction of vectors, `geom_mag()` focuses specifically on **magnitude representation**, typically using size, color, or intensity to encode the strength of vectors.

### How `geom_mag()` works

`geom_mag()` renders vector magnitudes at specified (x, y) locations using visual encodings such as:
1. **Circle/point size** proportional to magnitude
2. **Color intensity** mapped to magnitude
3. **Symbol size** scaled by magnitude value

The typical aesthetic mappings include:
- `x` - X-coordinate of vector location
- `y` - Y-coordinate of vector location
- `mag` or `magnitude` - The magnitude value to visualize
- Optional: `color`, `fill`, `alpha`, `size` for additional encoding

### Practical Example

Imagine you are visualizing a wind field where you have measurements at various points:

```r
# Wind field data: location (x, y) and wind speed magnitude
wind_data <- data.frame(
  x = c(1, 2, 3, 1, 2, 3, 1, 2, 3),
  y = c(1, 1, 1, 2, 2, 2, 3, 3, 3),
  magnitude = c(2.5, 5.1, 3.2, 4.8, 6.2, 5.5, 3.1, 4.3, 2.9)
)

# Visualize wind speed magnitude at each point
ggplot(wind_data, aes(x = x, y = y)) +
  geom_mag(aes(mag = magnitude, size = magnitude, color = magnitude)) +
  scale_size_continuous(range = c(1, 10)) +
  scale_color_gradient(low = "blue", high = "red")
```

This creates a plot where:
- Each point shows the location of a measurement
- Point size represents wind speed magnitude
- Point color represents intensity (blue = low, red = high)

### Differences from Related Geoms

**`geom_mag()` vs `geom_spoke()`:**
- `geom_spoke()` requires both `angle` and `radius` (direction + magnitude) and draws line segments
- `geom_mag()` only requires magnitude and typically renders as points/circles
- Use `geom_spoke()` when both direction and magnitude matter
- Use `geom_mag()` when only magnitude matters or when you want a cleaner heat-map style visualization

**`geom_mag()` vs `geom_point()`:**
- `geom_point()` is a general-purpose scatter plot layer
- `geom_mag()` is specialized for vector magnitudes with specific aesthetic defaults
- `geom_mag()` may include additional validation for magnitude values (non-negative)

### Aesthetic Requirements

**Required:**
- `x` - X position
- `y` - Y position

**Optional:**
- `mag` or `magnitude` - Magnitude value (typically non-negative); when omitted, points are drawn with default size
- `size` - Point size (often mapped to magnitude)
- `color` - Point color (often mapped to magnitude for heat-map effect)
- `fill` - Fill color for filled shapes
- `alpha` - Transparency
- `shape` - Point shape

### Implementation Notes

When implementing `geom_mag()` in the matrix-charts package:

1. **Create `GeomMag` class** extending `Geom`
2. **Default aesthetic mapping**: Map `magnitude` to both `size` and `color` by default
3. **Validation**: Ensure magnitude values are non-negative (or handle negative as zero/warning)
4. **Stat**: Default to `stat_identity` (no statistical transformation needed)
5. **Rendering**: Use circles or other symbols scaled by magnitude
6. **Legend**: Automatically create combined size+color legend when both are mapped to magnitude

### Official Documentation

ggplot2 documentation for vector field visualization:
**[https://ggplot2.tidyverse.org/reference/geom_spoke.html](https://ggplot2.tidyverse.org/reference/geom_spoke.html)**

Note: `geom_mag()` is a specialized extension and may be documented as part of extension packages or scientific visualization libraries.

---

## Identity Scales - Edge Cases and Validation

Identity scales (`scale_*_identity()`) are unique in ggplot2 because they use **data values directly** without any transformation or mapping. Instead of mapping data to a palette or range, the data itself contains the final aesthetic values.

### Available Identity Scales

The matrix-charts package already implements:
- `scale_color_identity()` / `scale_colour_identity()` - Use color names or hex values from data
- `scale_fill_identity()` - Use fill colors directly from data
- `scale_alpha_identity()` - Use alpha/transparency values directly (clamped to [0, 1])
- `scale_size_identity()` - Use size values directly from data
- `scale_shape_identity()` - Use shape names/codes directly from data
- `scale_linetype_identity()` - Use line type specifications directly from data

### When to Use Identity Scales

Use identity scales when:
1. **Data pre-encoded**: Your data already contains the aesthetic values you want to display
2. **External styling**: Colors, sizes, or shapes are determined by external systems
3. **Conditional formatting**: Different rows have explicitly assigned visual properties
4. **Direct control**: You want complete control over aesthetic values without mapping

### How Identity Scales Work

**Example with color:**
```r
# Data has a 'point_color' column with actual color values
data <- data.frame(
  x = c(1, 2, 3),
  y = c(2, 4, 6),
  point_color = c('red', 'blue', '#FF00FF')
)

ggplot(data, aes(x = x, y = y, color = point_color)) +
  geom_point() +
  scale_color_identity()  # Use the color values as-is
```

Without `scale_color_identity()`, ggplot2 would treat 'red', 'blue', '#FF00FF' as categorical levels and map them to a default palette. With identity scale, it uses those exact color values.

### Critical Edge Cases

#### 1. Invalid Color Values

**Problem:** What happens when color/fill identity scales receive invalid color values?

**Current behavior (from tests):**
- Unknown color names are **passed through as-is** (e.g., 'unknowncolor123' → 'unknowncolor123')
- This may cause rendering issues in SVG if the color is not recognized

**Recommendation:**
- Add validation to detect invalid colors
- Provide clear warning messages
- Fall back to `naValue` for invalid colors (not just `null` values)
- Consider using ColorUtil.normalizeColor() more aggressively

**Example edge case:**
```r
data <- data.frame(x = 1:3, y = 1:3, col = c('red', 'invalid_color', '#GGG'))
ggplot(data, aes(x, y, color = col)) + geom_point() + scale_color_identity()
# What happens to 'invalid_color' and '#GGG'?
```

#### 2. Out-of-Range Numeric Values

**Problem:** Alpha, size, and other numeric identity scales may receive out-of-range values.

**Current behavior:**
- **Alpha**: Values are clamped to [0, 1] range ✓
- **Size**: No explicit clamping (could receive negative values)

**Recommendation:**
- **Size**: Add validation to ensure non-negative values (or clamp to minimum)
- **Alpha**: Current clamping is correct ✓
- Document the valid ranges for each scale type

**Example edge case:**
```r
data <- data.frame(x = 1:4, y = 1:4,
                   sizes = c(5, -2, 100, 0),
                   alpha = c(0.5, 1.5, -0.2, 0.8))
ggplot(data, aes(x, y, size = sizes, alpha = alpha)) +
  geom_point() +
  scale_size_identity() +    # Should -2 be allowed?
  scale_alpha_identity()     # 1.5 and -0.2 are clamped ✓
```

#### 3. Null and Missing Values

**Problem:** How should identity scales handle `null`, `NA`, or missing values?

**Current behavior:**
- All identity scales define an `naValue` (default varies by scale)
- Color: `naValue = 'grey50'`
- Alpha: `naValue = 1.0`
- Null values are replaced with naValue ✓

**Recommendation:**
- Ensure consistent handling across all identity scales
- Document the default `naValue` for each scale type
- Allow users to customize `naValue` via parameters ✓ (already implemented)

**Example:**
```r
data <- data.frame(x = 1:3, y = 1:3, col = c('red', NA, 'blue'))
ggplot(data, aes(x, y, color = col)) +
  geom_point() +
  scale_color_identity(naValue = 'purple')  # NA → 'purple'
```

#### 4. Type Coercion Issues

**Problem:** What happens when non-string values are passed to color scales, or non-numeric to alpha/size?

**Current behavior:**
- Color scales: `value.toString()` converts to string
- Numeric scales: `ScaleUtils.coerceToNumber()` attempts conversion
- Non-coercible values → return naValue ✓

**Recommendation:**
- Document type expectations clearly
- Consider warning users when type coercion fails
- Test edge cases like boolean values, complex objects, etc.

**Example edge case:**
```r
data <- data.frame(x = 1:3, y = 1:3,
                   col = c(123, 456, 789))  # Numbers instead of colors
ggplot(data, aes(x, y, color = col)) +
  geom_point() +
  scale_color_identity()  # '123', '456', '789' as colors?
```

#### 5. Guide/Legend Behavior

**Problem:** Should identity scales display legends by default?

**Context:**
- When using identity scales, the data values are the visual values
- A legend often doesn't make sense (e.g., showing 'red' → red, 'blue' → blue)
- However, users may still want legends for documentation purposes

**Recommendation:**
- Default: `guide = 'none'` for identity scales (suppress legend)
- Allow users to override: `scale_color_identity(guide = 'legend')`
- Document this behavior clearly

**Example:**
```r
# No legend by default (makes sense - colors are self-documenting)
ggplot(data, aes(x, y, color = color_col)) +
  geom_point() +
  scale_color_identity()

# Force legend if needed for documentation
ggplot(data, aes(x, y, color = color_col)) +
  geom_point() +
  scale_color_identity(guide = 'legend')
```

#### 6. Shape Identity with Invalid Shapes

**Problem:** Shape identity scale may receive invalid shape specifications.

**Recommendation:**
- Validate shape names against known SVG shapes (circle, square, triangle, etc.)
- Validate shape codes if using numeric codes
- Fall back to default shape (e.g., circle) for invalid values
- Provide warning when invalid shapes are encountered

**Example edge case:**
```r
data <- data.frame(x = 1:3, y = 1:3,
                   shp = c('circle', 'invalid_shape', 'square'))
ggplot(data, aes(x, y, shape = shp)) +
  geom_point() +
  scale_shape_identity()  # What renders for 'invalid_shape'?
```

#### 7. Linetype Identity with Invalid Patterns

**Problem:** Linetype identity may receive invalid dash patterns.

**Recommendation:**
- Validate linetype names ('solid', 'dashed', 'dotted', etc.)
- Validate custom dash arrays (e.g., '5,5' format)
- Fall back to 'solid' for invalid patterns
- Document supported linetype values

**Example edge case:**
```r
data <- data.frame(x = 1:3, y = 1:3,
                   lt = c('dashed', 'invalid_type', '10,5,2'))
ggplot(data, aes(x, y, linetype = lt, group = 1)) +
  geom_line() +
  scale_linetype_identity()  # Validate patterns
```

### Implementation Checklist for Edge Cases

When implementing or improving identity scales:

- [ ] **Validation**: Add input validation for all aesthetic values
- [ ] **Clamping**: Clamp numeric values to valid ranges (alpha [0,1], size >= 0)
- [ ] **naValue handling**: Ensure consistent null/NA handling across all scales
- [ ] **Type coercion**: Document and test type coercion behavior
- [ ] **Error messages**: Provide clear warnings for invalid values
- [ ] **Fallback behavior**: Define fallback values for invalid inputs
- [ ] **Guide defaults**: Set appropriate guide defaults (typically 'none')
- [ ] **Documentation**: Document valid value ranges and formats
- [ ] **Testing**: Add comprehensive tests for edge cases (see existing tests as examples)
- [ ] **Color validation**: Use ColorUtil.normalizeColor() consistently
- [ ] **String conversion**: Test string-to-numeric conversions for alpha/size
- [ ] **Boundary testing**: Test min/max values, zero, negative values

### Reference Implementation

The existing identity scale implementations in matrix-charts provide good patterns:
- **ScaleColorIdentity**: Uses ColorUtil.normalizeColor() for validation
- **ScaleAlphaIdentity**: Implements proper clamping to [0, 1] range
- Both handle naValue correctly
- Both support customization via constructor parameters

**Files to reference:**
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleColorIdentity.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleAlphaIdentity.groovy`
- `matrix-charts/src/test/groovy/gg/scale/ScaleColorIdentityTest.groovy`
- `matrix-charts/src/test/groovy/gg/scale/ScaleAlphaIdentityTest.groovy`

### Official Documentation

ggplot2 identity scales documentation:
**[https://ggplot2.tidyverse.org/reference/scale_identity.html](https://ggplot2.tidyverse.org/reference/scale_identity.html)**