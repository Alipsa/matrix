# Matrix Library vs NumPy + Pandas: A Comprehensive Comparison

## Executive Summary

The Matrix library is a **mature, feature-rich** data manipulation and visualization toolkit for the JVM/Groovy ecosystem. It combines functionality that in Python requires **three separate libraries** (pandas for tabular data, numpy for numerical arrays, matplotlib/seaborn for visualization).

---

## 1. Core Data Structures Comparison

| Feature               | Matrix Library               | NumPy                   | Pandas                      |
|-----------------------|------------------------------|-------------------------|-----------------------------|
| **Primary Structure** | `Matrix` (typed columns)     | `ndarray` (homogeneous) | `DataFrame` (typed columns) |
| **Homogeneous Array** | `Grid<T>`                    | `ndarray`               | -                           |
| **1D Data**           | `Column` (extends ArrayList) | `ndarray`               | `Series`                    |
| **Type System**       | Per-column types             | Single dtype            | Per-column dtypes           |
| **Memory Layout**     | Columnar (List-based)        | Contiguous C/Fortran    | Columnar (block-based)      |

### Strengths of Matrix

- **Clean Groovy syntax** - Property access (`matrix.salary`), closures, operator overloading
- **Type flexibility** - Any Java/Groovy object type supported per column
- **Unified library** - Data manipulation + statistics + visualization in one package
- **GINQ integration** - SQL-like query syntax directly in Groovy (more powerful than pandas' `query()` method)

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

### Weaknesses vs NumPy/Pandas

- **No true N-dimensional arrays** - Grid is 2D only; NumPy supports arbitrary dimensions
- **No native vectorized operations** - Python's numpy is compiled C with SIMD; Matrix uses JVM loops
- **Memory efficiency** - Lists of boxed objects vs NumPy's contiguous primitive arrays (but see Tablesaw integration below)

---

## 2. Data Manipulation Comparison

| Operation     | Matrix                            | Pandas                                | Notes                                              |
|---------------|-----------------------------------|---------------------------------------|----------------------------------------------------|
| **Selection** | `table[0, 'col']`, `table.col`    | `df.loc[0, 'col']`, `df['col']`       | Similar expressiveness                             |
| **Filtering** | `subset { it.x > 5 }`             | `df[df.x > 5]`                        | Matrix uses closures, pandas uses boolean indexing |
| **Sorting**   | `orderBy('col', DESC)`            | `sort_values('col', ascending=False)` | Similar                                            |
| **Grouping**  | `Stat.sumBy(t, 'val', 'grp')`     | `df.groupby('grp')['val'].sum()`      | Pandas more flexible with agg()                    |
| **Joins**     | `Joiner.merge(a, b, 'key')`       | `pd.merge(a, b, on='key')`            | Similar; both O(n+m) hash joins                    |
| **Pivot**     | `table.pivot('id', 'var', 'val')` | `df.pivot(index, columns, values)`    | Similar                                            |
| **Apply**     | `table.apply('col') { it * 2 }`   | `df['col'].apply(lambda x: x * 2)`    | Similar                                            |

### Pandas Advantages

1. **Rolling/expanding windows** - `df.rolling(7).mean()`
2. **Multi-index support** - Hierarchical row/column indices

### Comparable Features

**Vectorized column arithmetic:** Both Matrix and pandas support element-wise column operations:

```groovy
// Matrix - column arithmetic with operator overloading
table.salary * 1.2                      // Multiply each value by scalar
table.salary * table.inflationRate      // Multiply two columns element-wise
table.salary + table.bonus              // Add columns
table.revenue - table.costs             // Subtract columns
table.total / table.count               // Divide columns
```

```python
# Pandas equivalent
df['salary'] * 1.2
df['salary'] * df['inflation_rate']
df['salary'] + df['bonus']
```

**GroupBy operations:** Matrix provides `Stat.groupBy()` which returns a `Map<String, Matrix>` allowing flexible aggregations:

```groovy
// Group by one or more columns
Map<String, Matrix> groups = Stat.groupBy(table, 'department', 'region')

// Apply any aggregation to groups
groups.collectEntries { key, group ->
    [key, [sum: Stat.sum(group['salary']), mean: Stat.mean(group['salary'])]]
}

// Convenience methods for common aggregations
Matrix sums = Stat.sumBy(table, 'salary', 'department')
Matrix means = Stat.meanBy(table, 'salary', 'department')

// Custom aggregation with funBy
Matrix maxValues = Stat.funBy(table, 'salary', 'department', { it.max() }, BigDecimal)
```

**String and DateTime operations:** Matrix achieves these via `apply()` with Groovy's native methods:

```groovy
// String operations (equivalent to pandas .str accessor)
table.apply('name') { it.toUpperCase() }
table.apply('name') { it.contains('Smith') }
table.apply('name') { it.split(' ').first() }

// DateTime operations (equivalent to pandas .dt accessor)
table.apply('date') { it.year }
table.apply('date') { it.monthValue }
table.apply('date') { it.dayOfWeek }
```

### Potential Improvements for Matrix

**Easy to add:**

```groovy
// Rolling window operations (HIGH VALUE)
Matrix.rolling(window: 7).mean()  // Moving average
Matrix.rolling(window: 30).apply { it.max() - it.min() }  // Custom rolling
```

---

## 3. Statistical Functions Comparison

| Feature              | Matrix                                                  | NumPy/Pandas/SciPy              |
|----------------------|---------------------------------------------------------|---------------------------------|
| **Basic stats**      | sum, mean, median, sd, variance                         | All present                     |
| **Correlation**      | Pearson, Spearman, Kendall + significance testing       | All present                     |
| **Regression**       | Linear, Polynomial, Quantile, Logistic, Ridge, LASSO, ElasticNet | via statsmodels/sklearn  |
| **T-tests**          | Welch, Student, paired, one-sample                      | scipy.stats                     |
| **ANOVA**            | One-way                                                 | scipy.stats                     |
| **Clustering**       | K-means, K-means++, DBSCAN (via Smile)                  | sklearn comparable              |
| **Distributions**    | Normal, Exponential, Gamma, Beta, Poisson, etc. + fitting | scipy.stats comparable        |
| **Classification**   | Random Forest, Decision Trees (via Smile)               | sklearn comparable              |
| **Dimensionality**   | PCA with variance analysis (via Smile)                  | sklearn comparable              |
| **Normality tests**  | Shapiro-Wilk, Anderson-Darling, Jarque-Bera, etc.       | scipy.stats comparable          |
| **Time series**      | ADF, KPSS, Granger causality, Durbin-Watson, etc.       | statsmodels comparable          |

### Matrix Strengths

- **Integrated statistics** - No need for separate library imports
- **Regression models** are first-class citizens with prediction methods
- **BigDecimal precision** - Better for financial/scientific accuracy
- **Smile ML integration** - Full-featured ML library via matrix-smile module

### Smile Integration (matrix-smile)

The matrix-smile module provides comprehensive ML capabilities via integration with the [Smile](https://haifengl.github.io/) library:

**Clustering:** K-Means, DBSCAN with silhouette coefficient analysis

**Classification:** Random Forest, Decision Trees with confusion matrix, precision/recall/F1

**Regression:** OLS, Ridge, LASSO, ElasticNet with R², MSE, RMSE, MAE metrics

**Dimensionality Reduction:** PCA with variance analysis and loadings extraction

**Distributions:** Normal, Exponential, Gamma, Beta, LogNormal, Weibull, Poisson, Binomial, etc. with fitting from data

**Hypothesis Tests:** T-tests, F-tests, Chi-square, Kolmogorov-Smirnov

### NumPy/SciPy Advantages

1. **Broadcasting** - Automatic shape expansion for operations
2. **Linear algebra** - `np.linalg` (eigenvalues, SVD, matrix inverse, determinant)
3. **FFT** - Fast Fourier Transform
4. **Signal processing** - scipy.signal
5. **Optimization** - scipy.optimize (minimize, curve fitting)
6. **Interpolation** - scipy.interpolate

### Potential Improvements for Matrix

**Medium effort:**

```groovy
// 1. Linear algebra module (HIGH VALUE for scientific computing)
import se.alipsa.matrix.linalg.Linalg

Linalg.inverse(matrix)           // Matrix inverse
Linalg.det(matrix)               // Determinant
Linalg.eigenvalues(matrix)       // Eigenvalue decomposition
Linalg.svd(matrix)               // Singular value decomposition
Linalg.solve(A, b)               // Solve Ax = b

// 2. Cumulative operations (EASY)
table['value'].cumsum()          // Cumulative sum
table['value'].cumprod()         // Cumulative product
table['value'].cummax()          // Running maximum
table['value'].cummin()          // Running minimum

// 3. Diff/shift operations (EASY)
table['value'].diff()            // First difference
table['value'].diff(2)           // Second difference
table['value'].shift(1)          // Lag by 1
table['value'].shift(-1)         // Lead by 1
```

---

## 4. I/O Capabilities Comparison

| Format            | Matrix                                              | Pandas                |
|-------------------|-----------------------------------------------------|-----------------------|
| **CSV**           | matrix-csv (seamless API)                           | Built-in, very fast   |
| **Excel**         | matrix-spreadsheet: FastExcel (xlsx), FastOds (ods) | openpyxl/xlrd         |
| **JSON**          | matrix-json                                         | Built-in              |
| **SQL**           | matrix-sql + JDBC (any database)                    | SQLAlchemy            |
| **Parquet**       | matrix-parquet (seamless API)                       | pyarrow/fastparquet   |
| **Avro**          | matrix-avro                                         | fastavro              |
| **ARFF**          | matrix-arff                                         | scipy.io.arff         |
| **HDF5**          | jHDF                                                | h5py/tables           |
| **Feather**       | Missing                                             | pyarrow               |
| **Pickle**        | N/A (Java serialization)                            | Built-in              |
| **XML**           | Groovy native (XmlSlurper, XmlParser)               | lxml                  |
| **HTML**          | Jsoup                                               | read_html()           |
| **Clipboard**     | java.awt + matrix-csv                               | read_clipboard()      |
| **Google Sheets** | matrix-gsheets                                      | gspread               |
| **BigQuery**      | matrix-bigquery                                     | google-cloud-bigquery |

### Matrix Strengths

- **JDBC support** - Works with any database with a JDBC driver
- **BigQuery/Google Sheets** - Native integration
- **Parquet** - Good support with BigDecimal precision options

### Potential Improvements

**Possible additions:**

```groovy
// Convenience clipboard methods (currently possible via java.awt + matrix-csv)
Matrix table = Matrix.readClipboard()
table.toClipboard()
```

---

## 5. Visualization Comparison

| Feature                 | Matrix (ggplot API)             | Matplotlib/Seaborn          |
|-------------------------|---------------------------------|-----------------------------|
| **Geoms**               | 57 geom types                   | Extensive via both libs     |
| **Grammar of Graphics** | Full implementation             | Different paradigm          |
| **Statistical plots**   | geom_smooth, geom_density, etc. | seaborn specializes in this |
| **Faceting**            | facet_wrap, facet_grid          | seaborn FacetGrid           |
| **Themes**              | 7 built-in themes               | Many matplotlib styles      |
| **Interactive**         | Limited (JavaFX)                | Plotly, Bokeh, Altair       |
| **Output**              | SVG, PNG, JPG                   | PNG, PDF, SVG, interactive  |

### Matrix Strengths

- **True ggplot2 API** - Familiar to R users
- **Integrated** - No separate library needed
- **SVG output** - Clean vector graphics

### Weaknesses

- **No interactive charts** - Matplotlib has Plotly, Bokeh integration
- **Limited animation** - Matplotlib can animate
- **No Jupyter integration** - Python notebooks auto-display charts

---

## 6. Performance Comparison

| Aspect            | Matrix                                  | NumPy/Pandas                      |
|-------------------|-----------------------------------------|-----------------------------------|
| **Vectorization** | JVM loops (Tablesaw for primitives)     | Native C/SIMD                     |
| **Memory**        | Boxed objects (Tablesaw for primitives) | Contiguous primitives             |
| **Large data**    | JVM heap limited                        | Out-of-core (Dask)                |
| **Parallelism**   | Manual (GPars)                          | Built-in (numba, multiprocessing) |

### NumPy/Pandas Advantages

1. **10-100x faster** for **large** numerical operations (gap narrows significantly with Tablesaw)
2. **Out-of-core processing** with Dask/Vaex
3. **GPU acceleration** with CuPy/RAPIDS

### Matrix Mitigations

- Good for moderate datasets (millions of rows)
- JVM JIT compilation helps with repeated operations
- Can integrate with Smile ML and/or tablesaw for high-performance algorithms

### Tablesaw Integration (matrix-tablesaw module)

For performance-critical workloads with large datasets, Matrix provides integration with [Tablesaw](https://github.com/jtablesaw/tablesaw) via the `matrix-tablesaw` module. Tablesaw uses **contiguous primitive arrays** (similar to NumPy's approach) rather than boxed objects, providing:

- **Memory efficiency** - Primitive arrays use significantly less memory than boxed objects
- **Cache-friendly operations** - Contiguous memory layout improves CPU cache utilization
- **Faster numerical operations** - Direct primitive operations without boxing/unboxing overhead

```groovy
import se.alipsa.matrix.tablesaw.TableSaw

// Convert Matrix to Tablesaw Table for performance-critical operations
Table table = TableSaw.toTable(matrix)

// Perform high-performance operations with Tablesaw
// ...

// Convert back to Matrix when needed
Matrix result = TableSaw.toMatrix(table)
```

This allows users to choose the appropriate data structure based on their needs: Matrix for flexibility and Groovy idioms, Tablesaw for raw performance with large numerical datasets.

---

## 7. Summary: Strengths and Weaknesses

### Matrix Library Strengths

1. **JVM Integration** - Works seamlessly with Java/Groovy ecosystem
2. **Clean Groovy Syntax** - Property access, closures, operator overloading
3. **Unified Package** - Data manipulation + statistics + visualization
4. **Type System** - Flexible types with BigDecimal precision
5. **Grammar of Graphics** - Full ggplot2-style API (57 geoms)
6. **Comprehensive I/O** - CSV, JSON, SQL, Excel, Parquet, BigQuery, Google Sheets
7. **Statistical Functions** - Regression, hypothesis tests, correlation, normality tests, time series
8. **Smile ML Integration** - Classification, clustering, PCA, distributions via matrix-smile
9. **Compile-time Safety** - `@CompileStatic` for type checking
10. **Tablesaw Integration** - Primitive array performance when needed for large datasets

### Matrix Library Weaknesses

1. **No N-dimensional Arrays** - Only 2D structures
2. **No Rolling/Window Operations** - Missing time-series analysis
3. **No Multi-Index** - Hierarchical indexing not supported
4. **Limited Linear Algebra** - No eigenvalues, SVD, matrix inverse
5. **No Interactive Visualization** - Only static SVG/PNG
6. **Performance Gap** - JVM vs native C for heavy computation (mitigated by Tablesaw integration)
7. **No Cumulative Operations** - cumsum, cumprod, etc.

---

## 8. Recommended Enhancements (Prioritized)

### High Priority (High Value, Moderate Effort)

| Enhancement                   | Value | Effort | Notes                           |
|-------------------------------|-------|--------|---------------------------------|
| **Rolling/Window Operations** | High  | Medium | Essential for time-series       |
| **Cumulative Operations**     | High  | Easy   | cumsum, cumprod, cummax, cummin |
| **Diff/Shift Operations**     | High  | Easy   | diff(), shift(), lag/lead       |

### Medium Priority (Good Value)

| Enhancement               | Value  | Effort | Notes                |
|---------------------------|--------|--------|----------------------|
| **Linear Algebra Module** | Medium | High   | Wrap EJML or similar |

### Lower Priority (Nice to Have)

| Enhancement            | Value | Effort | Notes                 |
|------------------------|-------|--------|-----------------------|
| **HTML Table Reader**  | Low   | Medium | Web scraping use case |
| **Clipboard Support**  | Low   | Easy   | Desktop convenience   |
| **More Interpolation** | Low   | Medium | Linear, spline, etc.  |

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

## Conclusion

The Matrix library is a **well-designed, comprehensive toolkit** that provides pandas-like functionality for the JVM ecosystem. Its main differentiators are:

1. **Unified solution** (data + stats + viz in one library)
2. **Idiomatic Groovy** with excellent syntax
3. **Strong ggplot2 implementation**

The primary gaps versus numpy+pandas are in **time-series operations** (rolling, cumulative, diff/shift) and **numerical computing** (linear algebra, N-dimensional arrays). Adding rolling window operations would significantly enhance the library for time-series analysis use cases.
