Please create a plan to implement the fowlloing 3 ggplot2 functions.

1. geom_mag() - Vector field magnitude visualization (physics/fluid dynamics)
2. stat_align() - Described in detail below
3. Identity scales - scale_*_identity() edge cases

## Stat_align

In `ggplot2`, **`stat_align()`** is a statistical transformation designed to align data points across different groups to a common set of x-coordinates.

It is primarily used as the default statistic for **`geom_area()`** to ensure that stacked area charts render correctly even when the data groups do not share the exact same x-values.

### What Problem Does it Solve?

In a stacked area chart, every layer (group) is stacked on top of the one below it. For this to work mathematically, every group must have a y-value defined for every x-value present in the plot.

If your data is "misaligned"—for example, Group A has a data point at  but Group B does not—`ggplot2` cannot stack them directly because there is no value for Group B to add to Group A at that specific x-coordinate.

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