# Matrix Charts

Matrix Charts provides various predefined charts that can easily be created based om
Matrix data. There are two different Plot classes that can either product Java FX (Plot) or Swing based
charts (SwingPlot) respectively.

## Fluent Builder API

In addition to the static `create()` factory methods, each chart type provides a fluent builder API
that starts from `builder(data)`, chains configuration methods, and terminates with `build()`.
All existing `create()` factories remain unchanged — the builder is a purely additive alternative.

### AreaChart

```groovy
def chart = AreaChart.builder(data)
    .title('Boston Robberies')
    .x('Record')
    .y('Robberies')
    .build()
```

### BarChart

```groovy
def chart = BarChart.builder(data)
    .title('Sales by Region')
    .x('region')
    .y('q1', 'q2', 'q3')
    .horizontal()
    .stacked()
    .build()
```

Additional methods: `chartType(ChartType)`, `direction(ChartDirection)`, `horizontal()`, `vertical()`, `stacked()`.

### LineChart

```groovy
def chart = LineChart.builder(data)
    .title('Trends')
    .x('year')
    .y('sales', 'profit')
    .build()
```

### ScatterChart

```groovy
def chart = ScatterChart.builder(data)
    .title('MPG vs Weight')
    .x('wt')
    .y('mpg')
    .build()
```

### PieChart

```groovy
def chart = PieChart.builder(data)
    .title('Market Share')
    .x('company')
    .y('revenue')
    .build()
```

### BoxChart

With category and value columns:

```groovy
def chart = BoxChart.builder(data)
    .title('Salary Distribution')
    .x('department')
    .y('salary')
    .build()
```

With multiple columns (each column becomes a box):

```groovy
def chart = BoxChart.builder(data)
    .title('Feature Comparison')
    .columns(['mpg', 'hp', 'wt'])
    .build()
```

### Histogram

```groovy
def chart = Histogram.builder(data)
    .title('MPG Distribution')
    .x('mpg')
    .bins(5)
    .binDecimals(2)
    .build()
```

### Common builder methods

All builders share these methods inherited from `Chart.ChartBuilder`:

- `title(String)` — chart title
- `x(String)` — x-axis (category) column name
- `y(String)` — single y-axis (value) column name
- `y(String...)` — multiple y-axis column names
- `xAxisTitle(String)` / `yAxisTitle(String)` — axis labels
- `xAxisScale(AxisScale)` / `yAxisScale(AxisScale)` — axis scales
- `xAxisScale(start, end, step)` / `yAxisScale(start, end, step)` — axis scales from values
- `legend(Legend)` — legend configuration
- `style(Style)` — style configuration

## GGPlot-style Charting

For ggplot2-style charting (e.g. `ggplot()`, `geom_point()`, `geom_smooth()`, etc.),
see the separate [matrix-ggcharts](../../matrix-ggcharts/README.md) module and its
[ggPlot documentation](../../matrix-ggcharts/docs/ggPlot.md).

---
[Back to index](cookbook.md)