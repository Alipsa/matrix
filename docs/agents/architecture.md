# Architecture and Modules

Companion to [AGENTS.md](../../AGENTS.md). Matrix is a Groovy library for working with tabular
(2D) data. It provides Matrix and Grid classes along with specialized modules for statistics,
visualization, and data I/O formats.

## Module Structure

`settings.gradle` is the authoritative module list. Current published and example modules include:

| Module                 | Purpose                                                                  |
|------------------------|--------------------------------------------------------------------------|
| **matrix-core**        | Core Matrix/Grid classes, basic statistics, data conversion              |
| **matrix-stats**       | Statistical tests, regression, clustering, time series, and related math |
| **matrix-datasets**    | Common datasets (mtcars, iris, diamonds, etc.)                           |
| **matrix-charts**      | Charm rendering engine and export utilities with SVG output              |
| **matrix-ggplot**      | GGPlot2-style charting API, delegates to Charm in matrix-charts          |
| **matrix-pict**        | Chart-type-first API (BarChart, LineChart, etc.), delegates to Charm in matrix-charts |
| **matrix-xchart**      | XChart integration                                                       |
| **matrix-csv**         | Advanced CSV import/export via commons-csv                               |
| **matrix-json**        | JSON import/export via Jackson                                           |
| **matrix-spreadsheet** | Excel/OpenOffice import/export via Apache POI and SODS                   |
| **matrix-sql**         | Database interaction via JDBC                                            |
| **matrix-gsheets**     | Google Sheets import/export                                              |
| **matrix-bigquery**    | Google BigQuery integration                                              |
| **matrix-parquet**     | Parquet format support                                                   |
| **matrix-avro**        | Avro format support                                                      |
| **matrix-tablesaw**    | Tablesaw interoperability                                                |
| **matrix-arff**        | ARFF import/export                                                       |
| **matrix-smile**       | Smile ML library integration                                             |
| **matrix-groovy-ext**  | Groovy extensions to Number and BigDecimal enabling more idiomatic Groovy |
| **matrix-logging**     | Logging integration helpers                                              |
| **matrix-bom**         | Bill of Materials for dependency management                              |
| **matrix-examples**    | Runnable example subprojects                                             |

## Architecture

### matrix-core
- `Matrix`: Primary tabular data structure with typed columns. Includes `top(n)`/`bottom(n)` (return Matrix slices), `info()` (column metadata Matrix), `sample(n)`/`sampleFraction(fraction)` (random sampling without replacement)
- `Column`: Typed list with `hasNulls()`, `countNulls()`, rolling/cumulative/shift helpers
- `Grid`: 2D array-like structure for homogeneous data
- `Stat`: Basic statistics (sum, mean, median, sd, frequency, groupBy)
- `ListConverter`: Type conversion utilities

### matrix-charts (Charm Rendering Engine)
The charting module provides the Charm rendering engine and a chart-type-first API:

```
Charts.groovy     -> Charm DSL entry point (plot, aes, geoms, scales, themes)
CharmRenderer     -> Core rendering pipeline
chartexport/      -> Export to PNG, JPEG, Swing, JavaFX, BufferedImage
charts/           -> Chart-type-first API (AreaChart, BarChart, PieChart, etc.)
```

### matrix-ggplot (GGPlot2-style API)
Provides a ggplot2-compatible API that delegates to Charm in matrix-charts:

```
GgPlot.groovy     -> Static factory methods (ggplot, aes, geom_*, scale_*, theme_*)
GgChart.groovy    -> Chart specification container with plus() operators
gg/bridge/        -> Bridge converting gg specs to Charm model
gg/export/        -> GgExport convenience wrapper for chart export
```

**Data flow** (spans both modules): Data + Mapping -> GgChart (matrix-ggplot) -> GgCharmCompiler bridge -> Charm model (matrix-charts) -> Stat transformation -> Position adjustment -> Scale computation -> Coord transformation -> Geom rendering -> Theme styling -> SVG output

**Key patterns**:
- Deferred rendering: collect specifications, render on `chart.render()`
- Static compilation enabled globally (see Coding Style section in AGENTS.md)
- Scale auto-detection: numeric data -> continuous scales, string data -> discrete scales

### Extension Modules
Modules like matrix-smile use Groovy extension methods registered via `META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule` to add methods like `matrix.toSmileDataFrame()`.

### matrix-smile
Integration with the Smile ML library. Key classes:
- `SmileUtil` — Conversions (`toDataFrame`, `toMatrix`, `describe`). Methods `head`/`tail`/`info`/`frequency`/`sample` are deprecated; use `Matrix.top`/`bottom`/`info`, `Stat.frequency`, `Matrix.sample` instead
- `Gsmile` — Groovy extensions for Matrix (`toSmileDataFrame`, `smileDescribe`, `smileSample`, `smileHead`, `smileTail`, `smileInfo`, `smileFrequency`) and DataFrame (`toMatrix`, `getAt`, `head`, `tail`, `filter`, `eachRow`, `collectRows`, `rowCount`, `columnCount`, `columnNames`, `structure`)
- `SmileStats` — Distribution constructors and fitting (`normalFit`, `exponentialFit`, `gammaFit`, `betaFit`, `logNormalFit` with `double[]`, `List<Number>`, and `Matrix+column` overloads), hypothesis tests, correlation with significance
- `SmileFeatures` — Feature engineering: `standardize`, `normalize`, `oneHotEncode`, `labelEncode`, `logTransform`, `sqrtTransform`, `powerTransform`, `binning`, `fillna`/`fillnaMean`/`fillnaMedian`, `dropna`. Stateful scalers/encoders: `StandardScaler`, `MinMaxScaler`, `LabelEncoder`, `OneHotEncoder` (all follow fit/transform/fitTransform pattern)
- `SmileClassifier` — Random forest, decision tree, SVM, logistic regression, etc.
- `SmileRegression` — OLS, ridge, LASSO, elastic net
- `SmileCluster` — K-means, DBSCAN, hierarchical clustering
- `SmileDimensionality` — PCA with variance analysis
- `SmileData` — Train/test split, stratified split, k-fold cross-validation, bootstrap sampling
