# matrix-ggplot Release Notes

## 0.5.0

### New features

- `aes()` now supports positional range aesthetics: `xend`, `yend`, `xmin`, `xmax`, `ymin`,
  `ymax`. Both the map form (`aes(xend: 'col')`) and the closure form
  (`aes { xend = col }`) work correctly and are passed through to the renderer.
- `labs()` now accepts independent legend titles per aesthetic:
  `labs(color: 'Speed', fill: 'Count')` sets separate titles for each guide.
- `stat_summary(geom: 'line')` and all other stat-compatible named geoms in `GEOM_REGISTRY`
  now work as the `geom:` argument to `stat_*` functions. The stat-compatible names are:
  `area`, `bar`, `bin2d`, `bin_2d`, `boxplot`, `col`, `contour`, `contour_filled`,
  `contourf`, `count`, `crossbar`, `density`, `density_2d`, `density2d`,
  `density_2d_filled`, `density2d_filled`, `dotplot`, `errorbar`, `errorbarh`,
  `freqpoly`, `function`, `hex`, `histogram`, `jitter`, `label`, `line`, `linerange`,
  `path`, `point`, `pointrange`, `polygon`, `qq`, `qq_line`, `quantile`, `raster`,
  `rect`, `ribbon`, `rug`, `segment`, `smooth`, `spoke`, `step`, `text`, `tile`,
  and `violin`.
- `guide_axis_stack()` now has typed overloads for `Guide` and `String` first arguments,
  plus typed vararg overloads for additional guides.
- R-to-Groovy conversion now emits BigDecimal-friendly extension-method calls for math
  functions, including nested calls and two-argument `log(x, base)`.
- `acos()` is available as a `Number` and `BigDecimal` extension method through
  `matrix-groovy-ext`, enabling generated `acos()` expressions to run without a missing
  method error.

### Bug fixes

- `labs(color: ..., fill: ...)` no longer silently lets the fill title overwrite the color
  legend title.
- `stat_*` calls with `geom: '<name>'` for unsupported names now throw
  `IllegalArgumentException` immediately instead of silently rendering a blank chart.
- `GgChart.render()` no longer mutates the chart's `coord` field as a side effect. Charts
  with no explicit coordinate system are still rendered as Cartesian; the default is now
  applied inside the compiler instead of on the chart object.
- R-to-Groovy conversion now emits `se.alipsa.matrix.ext.NumberExtension.PI` for `pi` instead
  of `Math.PI`, preserving the project's BigDecimal-first numeric behavior.
- Multi-argument R math calls without a supported extension-method equivalent now fail with
  a descriptive `UnsupportedOperationException` instead of generating invalid Groovy code.

### Improvements

- `ScaleColorFermenter` palette type lookup uses a Map instead of a switch statement for
  cleaner extensibility.
- `Rconverter` now generates idiomatic Groovy GDK extension method calls, such as
  `(it.x).log()`, instead of Java-style `Math.log(it.x)` calls.
- `qplot` and `stat_*` named-geom resolution now share the same lowercase, immutable
  `GEOM_REGISTRY`, keeping aliases and error messages consistent.
- Unsupported dedicated-factory-only geoms remain intentionally outside `GEOM_REGISTRY`:
  `abline`, `hline`, `vline`, `curve`, `sf`, `sf_text`, `sf_label`, `map`, `mag`,
  `parallel`, `lm`, `blank`, and `point_sampled`.
