# Matrix Library vs R + Tidyverse: A Comprehensive Comparison

## Executive Summary

The Matrix library is a **mature, feature-rich** data manipulation and visualization toolkit for the JVM/Groovy ecosystem. It draws significant inspiration from R's tidyverse, particularly in its ggplot2-style charting API. This document compares Matrix to R's base data structures and the tidyverse ecosystem (dplyr, tidyr, ggplot2, readr, purrr, etc.).

---

## 1. Core Data Structures Comparison

| Feature               | Matrix Library               | R (base + tidyverse)              |
|-----------------------|------------------------------|-----------------------------------|
| **Primary Structure** | `Matrix` (typed columns)     | `data.frame` / `tibble`           |
| **Homogeneous Array** | `Grid<T>`                    | `matrix`                          |
| **1D Data**           | `Column` (extends ArrayList) | `vector`                          |
| **Type System**       | Per-column types             | Per-column types                  |
| **Memory Layout**     | Columnar (List-based)        | Columnar (SEXP vectors)           |

### Strengths of Matrix

- **JVM ecosystem** - Integrates seamlessly with Java/Groovy libraries and enterprise systems
- **Static typing option** - `@CompileStatic` for compile-time type checking
- **Familiar ggplot2 API** - R users feel at home with the charting syntax
- **GINQ integration** - SQL-like query syntax directly in Groovy

```groovy
// GINQ example - SQL-like queries in Groovy
def result = GQ {
    from e in employees
    where e.salary > 50000 && e.department == 'Engineering'
    orderby e.salary in desc
    select e.name, e.salary
}
Matrix filtered = Matrix.builder().ginqResult(result).build()
```

- **Idiomatic numeric operations** - matrix-groovy-ext provides clean mathematical syntax

The `matrix-groovy-ext` module extends Number types with mathematical operations, making code more readable than using Java's `Math` class:

```groovy
import static se.alipsa.matrix.ext.NumberExtension.PI
import static se.alipsa.matrix.ext.NumberExtension.E

// Circle calculations - clean and readable
BigDecimal radius = 5.0
BigDecimal circumference = 2 * PI * radius
BigDecimal area = PI * radius ** 2

// Trigonometry - method chaining on numbers
BigDecimal angleDegrees = 45.0
BigDecimal angleRadians = angleDegrees.toRadians()
BigDecimal sine = angleRadians.sin()
BigDecimal cosine = angleRadians.cos()

// Polar to Cartesian conversion
BigDecimal x = radius * angleRadians.cos()
BigDecimal y = radius * angleRadians.sin()

// Cartesian to Polar - atan2 for correct quadrant handling
BigDecimal angle = y.atan2(x)

// Logarithms and exponentials
BigDecimal value = 100.0
BigDecimal naturalLog = value.log()      // ln(100)
BigDecimal log10 = value.log10()         // log₁₀(100) = 2
BigDecimal logBase2 = value.log(2)       // log₂(100)
BigDecimal expValue = 2.0.exp()          // e² ≈ 7.389

// Square root
BigDecimal sqrtValue = 25.0.sqrt()       // 5.0

// Clamping values with chainable min/max
BigDecimal normalized = rawValue.max(0).min(1)  // Clamp to [0, 1]

// Compare with verbose Java Math equivalent:
// double circumference = 2 * Math.PI * radius;
// double sine = Math.sin(Math.toRadians(45.0));
// double angle = Math.atan2(y, x);
// double normalized = Math.max(0, Math.min(1, rawValue));
```

This is similar to R's native vectorized math but with explicit method calls that chain naturally.

### Strengths of R + Tidyverse

- **Purpose-built for statistics** - R was designed from the ground up for data analysis
- **Massive ecosystem** - CRAN has 20,000+ packages for specialized analyses
- **Native vectorization** - All operations are inherently vectorized
- **Interactive exploration** - RStudio provides excellent interactive data exploration

---

## 2. Data Manipulation Comparison

| Operation        | Matrix                            | dplyr/tidyr                               |
|------------------|-----------------------------------|-------------------------------------------|
| **Selection**    | `table[0, 'col']`, `table.col`    | `df$col`, `select(df, col)`               |
| **Filtering**    | `subset { it.x > 5 }`             | `filter(df, x > 5)`                       |
| **Sorting**      | `orderBy('col', DESC)`            | `arrange(df, desc(col))`                  |
| **Grouping**     | `Stat.groupBy(t, 'grp')`          | `group_by(df, grp)`                       |
| **Aggregation**  | `Stat.sumBy(t, 'val', 'grp')`     | `summarise(df, sum(val))`                 |
| **Joins**        | `Joiner.merge(a, b, 'key')`       | `left_join(a, b, by='key')`               |
| **Pivot wider**  | `table.pivot('id', 'var', 'val')` | `pivot_wider(names_from, values_from)`    |
| **Pivot longer** | `table.unPivot(...)`              | `pivot_longer(cols, names_to, values_to)` |
| **Mutate**       | `table.apply('col') { it * 2 }`   | `mutate(df, col = col * 2)`               |
| **Rename**       | `table.rename('old', 'new')`      | `rename(df, new = old)`                   |

### Tidyverse Advantages

1. **Pipe operator** - `%>%` or `|>` enables highly readable data pipelines
2. **Rolling/window functions** - `slider` package, `zoo::rollmean()`
3. **Tidy evaluation** - Non-standard evaluation for expressive code
4. **rowwise operations** - `rowwise()` for row-by-row computation
5. **nest/unnest** - Nested data frames for complex hierarchical data

### Comparable Features

**Vectorized column arithmetic:** Both Matrix and R support element-wise column operations:

```groovy
// Matrix - column arithmetic with operator overloading
table.salary * 1.2                      // Multiply each value by scalar
table.salary * table.inflationRate      // Multiply two columns element-wise
table.salary + table.bonus              // Add columns
```

```r
# R equivalent
df$salary * 1.2
df$salary * df$inflation_rate
df$salary + df$bonus
```

**GroupBy operations:** Matrix provides `Stat.groupBy()` similar to dplyr's `group_by()`:

```groovy
// Matrix groupBy
Map<String, Matrix> groups = Stat.groupBy(table, 'department', 'region')

// Convenience methods
Matrix sums = Stat.sumBy(table, 'salary', 'department')
Matrix means = Stat.meanBy(table, 'salary', 'department')

// Custom aggregation
Matrix maxValues = Stat.funBy(table, 'salary', 'department', { it.max() }, BigDecimal)
```

```r
# R tidyverse equivalent
df %>%
  group_by(department, region) %>%
  summarise(
    sum_salary = sum(salary),
    mean_salary = mean(salary),
    max_salary = max(salary)
  )
```

**String and DateTime operations:** Matrix uses `apply()` with Groovy's native methods:

```groovy
// Matrix string/datetime operations
table.apply('name') { it.toUpperCase() }
table.apply('name') { it.contains('Smith') }
table.apply('date') { it.year }
table.apply('date') { it.monthValue }
```

```r
# R stringr/lubridate equivalent
df %>%
  mutate(
    name = str_to_upper(name),
    contains_smith = str_detect(name, 'Smith'),
    year = year(date),
    month = month(date)
  )
```

### Potential Improvements for Matrix

**Easy to add:**

```groovy
// Rolling window operations (HIGH VALUE)
table.rolling(window: 7).mean()  // Moving average
table.rolling(window: 30).apply { it.max() - it.min() }  // Custom rolling
```

---

## 3. Statistical Functions Comparison

| Feature             | Matrix                                               | R (base + packages)                |
|---------------------|------------------------------------------------------|------------------------------------|
| **Basic stats**     | sum, mean, median, sd, variance                      | All present (base R)               |
| **Correlation**     | Pearson, Spearman, Kendall + significance testing    | cor(), cor.test()                  |
| **Regression**      | Linear, Polynomial, Quantile, Logistic, Ridge, LASSO | lm(), glm(), quantreg, glmnet      |
| **T-tests**         | Welch, Student, paired, one-sample                   | t.test()                           |
| **ANOVA**           | One-way                                              | aov(), anova()                     |
| **Clustering**      | K-means, K-means++, DBSCAN (via Smile)               | kmeans(), dbscan package           |
| **Distributions**   | Normal, Exponential, Gamma, Beta, Poisson + fitting  | Extensive (dnorm, pnorm, qnorm...) |
| **Classification**  | Random Forest, Decision Trees (via Smile)            | randomForest, rpart                |
| **Dimensionality**  | PCA with variance analysis (via Smile)               | prcomp(), princomp()               |
| **Normality tests** | Shapiro-Wilk, Anderson-Darling, Jarque-Bera          | shapiro.test(), nortest package    |
| **Time series**     | ADF, KPSS, Granger causality, Durbin-Watson          | tseries, lmtest packages           |

### Matrix Strengths

- **Integrated statistics** - matrix-stats module provides comprehensive statistical functions
- **Smile ML integration** - Full-featured ML library via matrix-smile module
- **BigDecimal precision** - Better for financial/scientific accuracy than R's double precision

### Smile Integration (matrix-smile)

The matrix-smile module provides comprehensive ML capabilities:

**Clustering:** K-Means, DBSCAN with silhouette coefficient analysis

**Classification:** Random Forest, Decision Trees with confusion matrix, precision/recall/F1

**Regression:** OLS, Ridge, LASSO, ElasticNet with R², MSE, RMSE, MAE metrics

**Dimensionality Reduction:** PCA with variance analysis and loadings extraction

**Distributions:** Normal, Exponential, Gamma, Beta, LogNormal, Weibull, Poisson, Binomial with fitting

**Hypothesis Tests:** T-tests, F-tests, Chi-square, Kolmogorov-Smirnov

### R Advantages

1. **Formula interface** - `lm(y ~ x1 + x2 + x1:x2, data=df)` for complex model specification (Matrix supports formulas in geom_smooth for linear/polynomial models: `geom_smooth(formula: 'y ~ x', method: 'lm')`)
2. **Model diagnostics** - Rich built-in model summary, residual plots, influence measures
3. **Mixed effects models** - lme4, nlme packages
4. **Bayesian statistics** - Stan, brms, rstanarm
5. **Survival analysis** - survival package
6. **Extensive distribution support** - d/p/q/r functions for 20+ distributions

---

## 4. I/O Capabilities Comparison

| Format             | Matrix                                              | R (readr/haven/etc.)    |
|--------------------|-----------------------------------------------------|-------------------------|
| **CSV**            | matrix-csv (seamless API)                           | readr::read_csv()       |
| **Excel**          | matrix-spreadsheet: FastExcel (xlsx), FastOds (ods) | readxl, openxlsx        |
| **JSON**           | matrix-json                                         | jsonlite                |
| **SQL**            | matrix-sql + JDBC (any database)                    | DBI + database drivers  |
| **Parquet**        | matrix-parquet (seamless API)                       | arrow::read_parquet()   |
| **Avro**           | matrix-avro                                         | Limited support         |
| **ARFF**           | matrix-arff                                         | foreign::read.arff()    |
| **HDF5**           | jHDF                                                | rhdf5, hdf5r            |
| **Feather**        | Missing                                             | arrow::read_feather()   |
| **RDS/RData**      | N/A                                                 | Native (readRDS, load)  |
| **SAS/SPSS/Stata** | Missing                                             | haven package           |
| **XML**            | Groovy native (XmlSlurper, XmlParser)               | xml2                    |
| **HTML**           | Jsoup                                               | rvest                   |
| **Clipboard**      | java.awt + matrix-csv                               | read.table("clipboard") |
| **Google Sheets**  | matrix-gsheets                                      | googlesheets4           |
| **BigQuery**       | matrix-bigquery                                     | bigrquery               |

### Matrix Strengths

- **JDBC support** - Works with any database with a JDBC driver
- **Enterprise integration** - BigQuery, Google Sheets native support
- **Parquet** - Good support with BigDecimal precision options

### R Advantages

1. **Native serialization** - RDS/RData for fast R object storage
2. **Statistical software formats** - SAS, SPSS, Stata via haven
3. **Web scraping** - rvest provides tidyverse-integrated web scraping

---

## 5. Visualization Comparison

| Feature                 | Matrix (ggplot API)             | ggplot2                          |
|-------------------------|---------------------------------|----------------------------------|
| **Geoms**               | 57 geom types                   | 50+ geom types                   |
| **Grammar of Graphics** | Full implementation             | Original R implementation        |
| **Statistical plots**   | geom_smooth, geom_density, etc. | Full support                     |
| **Faceting**            | facet_wrap, facet_grid          | facet_wrap, facet_grid           |
| **Themes**              | 7 built-in themes               | Many themes + ggthemes package   |
| **Extensions**          | Limited                         | 100+ extension packages          |
| **Interactive**         | Limited (JavaFX)                | plotly, ggiraph, shiny           |
| **Output**              | SVG, PNG, JPG                   | PNG, PDF, SVG, interactive       |

### Matrix Strengths

- **Familiar API** - R users can transfer ggplot2 knowledge directly
- **Integrated** - No separate library needed
- **SVG output** - Clean vector graphics for web/print

```groovy
// Matrix ggplot-style API
def chart = ggplot(data, aes('cty', 'hwy', color: 'class')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    scale_color_viridis_d() +
    facet_wrap('drv') +
    theme_minimal() +
    labs(title: 'Fuel Economy', x: 'City MPG', y: 'Highway MPG')
```

```r
# Nearly identical R ggplot2 code
ggplot(data, aes(cty, hwy, color = class)) +
    geom_point() +
    geom_smooth(method = 'lm') +
    scale_color_viridis_d() +
    facet_wrap(~drv) +
    theme_minimal() +
    labs(title = 'Fuel Economy', x = 'City MPG', y = 'Highway MPG')
```

### R/ggplot2 Advantages

1. **Extensive extensions** - ggforce, ggridges, ggrepel, patchwork, etc.
2. **Interactive output** - plotly::ggplotly() for instant interactivity
3. **Shiny integration** - Reactive dashboards
4. **Statistical annotations** - ggpubr, ggstatsplot for publication-ready stats
5. **Maps** - sf + ggplot2 for sophisticated geospatial visualization

---

## 6. Performance Comparison

| Aspect            | Matrix                                  | R                                    |
|-------------------|-----------------------------------------|--------------------------------------|
| **Vectorization** | JVM loops (Tablesaw for primitives)     | Native vectorized (C/Fortran)        |
| **Memory**        | Boxed objects (Tablesaw for primitives) | SEXP vectors (some overhead)         |
| **Large data**    | JVM heap limited                        | Memory limited (data.table helps)    |
| **Parallelism**   | Manual (GPars)                          | parallel, future, furrr packages     |

### R Advantages

1. **Native vectorization** - Core operations implemented in C/Fortran
2. **data.table** - Extremely fast for large datasets (often faster than pandas)
3. **Rcpp** - Easy C++ integration for performance-critical code
4. **Arrow** - Out-of-core processing for very large datasets

### Matrix Mitigations

- Good for moderate datasets (millions of rows)
- JVM JIT compilation helps with repeated operations
- Can integrate with Smile ML and/or Tablesaw for high-performance algorithms

### Tablesaw Integration (matrix-tablesaw module)

For performance-critical workloads, Matrix provides integration with [Tablesaw](https://github.com/jtablesaw/tablesaw) via `matrix-tablesaw`. Tablesaw uses **contiguous primitive arrays** providing:

- **Memory efficiency** - Primitive arrays use significantly less memory
- **Cache-friendly operations** - Contiguous memory improves CPU cache utilization
- **Faster numerical operations** - Direct primitive operations without boxing overhead

```groovy
import se.alipsa.matrix.tablesaw.TableSaw

// Convert Matrix to Tablesaw Table for performance-critical operations
Table table = TableSaw.toTable(matrix)

// Perform high-performance operations with Tablesaw
// ...

// Convert back to Matrix when needed
Matrix result = TableSaw.toMatrix(table)
```

---

## 7. Summary: Strengths and Weaknesses

### Matrix Library Strengths

1. **JVM Integration** - Works seamlessly with Java/Groovy ecosystem and enterprise systems
2. **Clean Groovy Syntax** - Property access, closures, operator overloading
3. **Familiar ggplot2 API** - R users can transfer knowledge directly
4. **Type System** - Flexible types with BigDecimal precision
5. **Comprehensive I/O** - CSV, JSON, SQL, Excel, Parquet, BigQuery, Google Sheets
6. **Statistical Functions** - Regression, hypothesis tests, correlation, normality tests
7. **Smile ML Integration** - Classification, clustering, PCA, distributions
8. **Compile-time Safety** - `@CompileStatic` for type checking
9. **Tablesaw Integration** - Primitive array performance when needed
10. **GINQ** - SQL-like query syntax more powerful than dplyr

### Matrix Library Weaknesses

1. **No Rolling/Window Operations** - Missing time-series analysis (R has slider, zoo)
2. **No Pipe Operator** - Less fluent than tidyverse pipelines (though Groovy's tap/with help)
3. **Smaller Ecosystem** - R's CRAN has 20,000+ specialized packages
4. **Limited Formula Interface** - R's `y ~ x1 + x2` is more expressive for models
5. **Limited ggplot Extensions** - R has 100+ ggplot2 extension packages
6. **No Interactive Visualization** - R has plotly, shiny integration
7. **No Cumulative Operations** - cumsum, cumprod, etc.

---

## 8. Recommended Enhancements (Prioritized)

### High Priority (High Value, Moderate Effort)

| Enhancement                   | Value | Effort | Notes                                |
|-------------------------------|-------|--------|--------------------------------------|
| **Rolling/Window Operations** | High  | Medium | Essential for time-series            |
| **Cumulative Operations**     | High  | Easy   | cumsum, cumprod, cummax, cummin      |
| **Diff/Shift Operations**     | High  | Easy   | diff(), shift(), lag/lead            |

### Medium Priority (Good Value)

| Enhancement               | Value  | Effort | Notes                          |
|---------------------------|--------|--------|--------------------------------|
| **Linear Algebra Module** | Medium | High   | Wrap EJML or similar           |

### Lower Priority (Nice to Have)

| Enhancement            | Value | Effort | Notes                          |
|------------------------|-------|--------|--------------------------------|
| **Clipboard Support**  | Low   | Easy   | Convenience methods            |
| **More Interpolation** | Low   | Medium | Linear, spline, etc.           |

---

## 9. Quick Win Implementation Examples

Here are some enhancements that would be relatively easy to add:

### 1. Cumulative Operations (Add to Column class)

```groovy
// In Column.groovy
List<Number> cumsum() {
    Number sum = 0
    this.collect {
        sum = sum + (it as Number)
        sum
    }
}

List<Number> cumprod() {
    Number prod = 1
    this.collect {
        prod = prod * (it as Number)
        prod
    }
}
```

### 2. Diff/Shift Operations

```groovy
// In Column.groovy
List diff(int periods = 1) {
    (0..<size()).collect { i ->
        i < periods ? null : this[i] - this[i - periods]
    }
}

List shift(int periods) {
    if (periods >= 0) {
        [null] * periods + this[0..<(size() - periods)]
    } else {
        this[(-periods)..<size()] + [null] * (-periods)
    }
}
```

### 3. Rolling Window (New class)

```groovy
class Rolling {
    Column column
    int window

    Rolling(Column col, int w) {
        this.column = col
        this.window = w
    }

    List<BigDecimal> mean() {
        (0..<column.size()).collect { i ->
            if (i < window - 1) return null
            def slice = column[(i - window + 1)..i]
            Stat.mean(slice)
        }
    }

    List<BigDecimal> sum() { /* similar */ }
    List<BigDecimal> std() { /* similar */ }
    List apply(Closure fn) { /* apply fn to each window */ }
}

// Usage: table['price'].rolling(7).mean()
```

---

## 10. Migration Guide: R to Matrix

For R users transitioning to Matrix, here's a quick reference:

| R (tidyverse)                           | Matrix (Groovy)                                |
|-----------------------------------------|------------------------------------------------|
| `library(dplyr)`                        | `import se.alipsa.matrix.core.*`               |
| `df <- read_csv("file.csv")`            | `def df = Matrix.builder().data(file).build()` |
| `df %>% filter(x > 5)`                  | `df.subset { it.x > 5 }`                       |
| `df %>% select(a, b, c)`                | `df.selectColumns('a', 'b', 'c')`              |
| `df %>% arrange(desc(x))`               | `df.orderBy('x', Matrix.DESC)`                 |
| `df %>% mutate(y = x * 2)`              | `df.apply('x') { it * 2 }`                     |
| `df %>% group_by(g) %>% summarise(...)` | `Stat.sumBy(df, 'val', 'g')`                   |
| `left_join(a, b, by='key')`             | `Joiner.merge(a, b, 'key', true)`              |
| `ggplot(df, aes(x, y)) + geom_point()`  | `ggplot(df, aes('x', 'y')) + geom_point()`     |

### Key Differences

1. **Column names are strings** - Use `'column'` not bare `column`
2. **Closures instead of expressions** - Use `{ it.x > 5 }` not `x > 5`
3. **Method chaining** - Use `.method()` not `%>%`
4. **No tidy evaluation** - All evaluation is standard Groovy

---

## Conclusion

The Matrix library provides a **compelling alternative to R** for JVM-based data analysis. Its main advantages are:

1. **JVM integration** - Perfect for enterprise environments with Java infrastructure
2. **Familiar ggplot2 API** - Minimal learning curve for R users
3. **Strong typing option** - Compile-time safety when needed
4. **Comprehensive feature set** - Data manipulation, statistics, visualization in one package

The primary gaps versus R + tidyverse are in **time-series operations** (rolling windows, cumulative functions) and **ecosystem breadth** (R's CRAN has more specialized packages). For most data analysis tasks, Matrix provides equivalent functionality with the added benefit of JVM ecosystem integration.
