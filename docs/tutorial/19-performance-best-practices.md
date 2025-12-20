# Performance Best Practices

This chapter provides guidance on optimizing Matrix operations for large datasets and production use. Understanding how Matrix works internally helps you write efficient code and avoid common performance pitfalls.

## Installation

Performance optimization applies to all Matrix modules. The core module is:

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.2'
implementation "se.alipsa.matrix:matrix-core:3.2.0"
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.2</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>
```

## Understanding Matrix Memory Model

### Column-Based Storage

Matrix stores data **column-wise**, not row-wise. Each column is stored as a `Column` object that extends `ArrayList`:

```groovy
// Internally, a Matrix with 3 columns stores:
// mColumns = [Column(name), Column(age), Column(salary)]
// Each Column is an ArrayList of values for that column

Matrix employees = Matrix.builder('employees')
    .data(
        name: ['Alice', 'Bob', 'Charlie'],
        age: [25, 30, 35],
        salary: [50000.0, 60000.0, 75000.0]
    )
    .types([String, Integer, BigDecimal])
    .build()

// Column access is O(1) - direct array lookup
def ages = employees['age']  // Returns the Column object

// Row access requires gathering values from each column
def firstRow = employees.row(0)  // Gathers [name[0], age[0], salary[0]]
```

### Memory Layout Implications

This column-based storage has important performance implications:

| Operation | Performance | Reason |
|-----------|-------------|--------|
| Access column | O(1) | Direct reference to Column object |
| Access cell by column name | O(n) | Lookup column index, then O(1) access |
| Access cell by column index | O(1) | Direct array access |
| Access row | O(c) | Must gather c values from each column |
| Iterate columns | Fast | Sequential memory access |
| Iterate rows | Slower | Random memory access across columns |

### When to Use Grid vs Matrix

Matrix includes a `Grid` class that stores data **row-wise** as `List<List<T>>`:

```groovy
import se.alipsa.matrix.core.Grid

// Grid stores data row-wise
Grid<Object> grid = new Grid([
    ['Alice', 25, 50000.0],
    ['Bob', 30, 60000.0],
    ['Charlie', 35, 75000.0]
])

// Row access is O(1)
def firstRow = grid[0]  // Direct array access

// But no column types, no column names, no statistical operations
```

**Use Grid when:**
- You primarily access data by rows
- You don't need column types or names
- You're building intermediate data structures
- Memory efficiency for simple data is priority

**Use Matrix when:**
- You need column-based operations (statistics, transformations)
- You need typed columns
- You're doing data analysis
- You need column names for clarity

### Data Type Impact on Memory

Choose appropriate data types to minimize memory usage:

| Type | Size (approx) | Use Case |
|------|---------------|----------|
| `Integer` | 16 bytes | Whole numbers -2B to 2B |
| `Long` | 24 bytes | Large whole numbers |
| `BigDecimal` | 40+ bytes | Precise decimals, financial data |
| `Double` | 24 bytes | Scientific calculations, less precision |
| `Float` | 16 bytes | Lower precision, memory-constrained |
| `String` | 40+ bytes | Text (varies with length) |

```groovy
// For large datasets, consider using Double instead of BigDecimal
// if you don't need exact decimal precision
Matrix data = Matrix.builder()
    .data(value: [1.5, 2.3, 3.7])
    .types([Double])  // 24 bytes per value
    .build()

// vs BigDecimal
Matrix precise = Matrix.builder()
    .data(value: [1.5, 2.3, 3.7])
    .types([BigDecimal])  // 40+ bytes per value, but exact
    .build()
```

## Efficient Data Loading

### Streaming CSV Files

For very large CSV files, consider processing in chunks rather than loading everything at once:

```groovy
import se.alipsa.matrix.csv.CsvImporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

// For extremely large files, process line by line
File largeFile = new File("huge_dataset.csv")
CSVFormat format = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setSkipHeaderRecord(true)
    .build()

largeFile.withReader { reader ->
    CSVParser parser = format.parse(reader)
    parser.each { record ->
        // Process one record at a time
        String name = record.get("name")
        int age = record.get("age") as int
        processRecord(name, age)
    }
}
```

### Chunked Reading for Analysis

When you need Matrix features but have large data:

```groovy
import se.alipsa.matrix.csv.CsvImporter

// Read and process in chunks
def processLargeFile(File file, int chunkSize = 10000) {
    def allLines = file.readLines()
    def header = allLines[0]
    def dataLines = allLines.drop(1)

    def results = []
    dataLines.collate(chunkSize).each { chunk ->
        // Create temporary file with header + chunk
        def tempContent = [header] + chunk
        def tempFile = File.createTempFile("chunk", ".csv")
        tempFile.text = tempContent.join("\n")

        // Process chunk as Matrix
        Matrix chunkMatrix = CsvImporter.importCsv(tempFile)
        results << processChunk(chunkMatrix)

        tempFile.delete()
    }
    return results
}
```

### Pre-allocating Column Types

When you know the data types in advance, specifying them improves performance:

```groovy
// Slower: Type inference at runtime
Matrix auto = CsvImporter.importCsv(file)

// Faster: Explicit types, no inference needed
Matrix typed = Matrix.builder()
    .data(
        id: ids,
        name: names,
        value: values
    )
    .types([Integer, String, BigDecimal])
    .build()
```

### Database Loading Best Practices

When loading from databases, minimize data transfer:

```groovy
import se.alipsa.matrix.sql.SqlQuery

// Efficient: Filter and select at database level
Matrix result = SqlQuery.fromSql(connection, """
    SELECT id, name, value
    FROM large_table
    WHERE status = 'active'
    AND created_date > ?
    LIMIT 10000
""", [startDate])

// Inefficient: Loading everything then filtering
// Matrix all = SqlQuery.fromSql(connection, "SELECT * FROM large_table")
// Matrix filtered = all.subset { it['status'] == 'active' }
```

## Optimizing Transformations

### Prefer Batch Operations

The `apply()` method processes entire columns efficiently:

```groovy
// Efficient: Single apply call processes all rows
matrix = matrix.apply('salary', BigDecimal) { row, val ->
    val * 1.1
}

// Less efficient: Individual cell updates
// for (int i = 0; i < matrix.rowCount(); i++) {
//     matrix[i, 'salary'] = matrix[i, 'salary'] * 1.1
// }
```

### Understanding apply() Behavior

The `apply()` method creates a new column and returns a mutated Matrix:

```groovy
// apply() creates a new Column, replaces the old one
Matrix result = matrix.apply('value', BigDecimal) { row, val ->
    val * 2
}

// The original column is replaced with a new one
// No in-place mutation of individual cells
```

### Chain Operations Wisely

Each operation may create intermediate objects. Chain when beneficial:

```groovy
// Each subset creates a new Matrix
Matrix step1 = data.subset { it['age'] > 25 }
Matrix step2 = step1.subset { it['salary'] > 50000 }
Matrix step3 = step2.subset { it['active'] == true }

// Better: Single subset with combined conditions
Matrix result = data.subset { row ->
    row['age'] > 25 && row['salary'] > 50000 && row['active'] == true
}
```

### Use Column Arithmetic for Numeric Operations

The Column class supports element-wise arithmetic which is optimized:

```groovy
// Efficient: Column arithmetic
Column prices = matrix['price']
Column quantities = matrix['quantity']
Column totals = prices * quantities  // Element-wise multiplication

// Add the result back
matrix = matrix.addColumn('total', BigDecimal, totals)

// Also efficient: Direct column operations
Column doubled = matrix['value'] * 2
Column normalized = (matrix['value'] - minVal) / (maxVal - minVal)
```

### Avoid Repeated Column Lookups

Cache column references when accessing multiple times:

```groovy
// Inefficient: Repeated column lookup by name
for (int i = 0; i < matrix.rowCount(); i++) {
    total += matrix[i, 'value']  // Looks up 'value' column index each time
}

// Efficient: Cache the column reference
Column valueColumn = matrix['value']
for (int i = 0; i < valueColumn.size(); i++) {
    total += valueColumn[i]
}

// Even better: Use Column's built-in methods
def total = matrix['value'].sum()
```

## Working with Large Datasets

### Chunked Processing

Process large datasets in manageable chunks:

```groovy
def processLargeMatrix(Matrix matrix, int chunkSize = 10000) {
    def results = []
    def rowCount = matrix.rowCount()

    for (int start = 0; start < rowCount; start += chunkSize) {
        int end = Math.min(start + chunkSize, rowCount)
        IntRange range = start..<end

        // Get chunk as a new Matrix
        Matrix chunk = matrix.subset(range)

        // Process the chunk
        def chunkResult = processChunk(chunk)
        results << chunkResult
    }

    return results
}
```

### Parallel Processing with GPars

For CPU-intensive operations, use parallel processing:

```groovy
@Grab('org.codehaus.gpars:gpars:1.2.1')
import groovyx.gpars.GParsPool

Matrix data = // ... large dataset

// Parallel processing of rows
GParsPool.withPool {
    def results = data.rows().collectParallel { row ->
        // CPU-intensive calculation
        expensiveCalculation(row)
    }
}

// Parallel processing of column chunks
def columns = data.columnNames()
GParsPool.withPool {
    columns.eachParallel { colName ->
        processColumn(data[colName])
    }
}
```

### When to Use External Tools

Consider using specialized tools for very large datasets:

| Data Size | Recommendation |
|-----------|----------------|
| < 100K rows | Matrix handles well |
| 100K - 1M rows | Use chunked processing, consider memory |
| 1M - 10M rows | Database or Apache Spark |
| > 10M rows | Big data tools (Spark, Flink) |

```groovy
// For truly large data, use Matrix for prototyping
// then move to appropriate tools

// Prototype with Matrix
Matrix sample = data.head(1000)
def analysisResults = analyzeWithMatrix(sample)

// Production with database
def fullResults = runSqlAnalysis(connection, query)
```

### Sampling for Exploratory Analysis

Work with samples during development:

```groovy
import se.alipsa.matrix.smile.Gsmile

// Random sample for exploration
Matrix sample = Gsmile.smileSample(largeMatrix, 1000)

// Stratified sampling by category
def categories = largeMatrix['category'].unique()
Matrix stratified = Matrix.builder().build()
categories.each { cat ->
    Matrix catData = largeMatrix.subset('category', cat)
    Matrix catSample = catData.head(100)
    stratified = stratified.rowCount() == 0 ? catSample :
        stratified.addRows(catSample.rows() as List<List>)
}
```

## Memory Management

### Releasing Unused References

Explicitly null out large objects when done:

```groovy
Matrix largeData = loadLargeDataset()

// Process the data
def results = processData(largeData)

// Release memory
largeData = null

// Suggest garbage collection (not guaranteed)
System.gc()

// Continue with results only
saveResults(results)
```

### Avoid Holding Row References

Row objects hold references back to the Matrix:

```groovy
// This holds references to the original Matrix
List<Row> allRows = matrix.rows() as List

// Better: Extract just the data you need
List<List> rowData = matrix.rows().collect { it as List }

// Or work with columns directly
def names = matrix['name'] as List
def ages = matrix['age'] as List
```

### Clone vs Reference

Understand when you're getting a copy vs a reference:

```groovy
// Clone creates an independent copy
Matrix copy = matrix.clone()
copy[0, 'name'] = 'Modified'  // Original is unchanged

// Column access returns a reference
Column col = matrix['name']
col[0] = 'Modified'  // THIS MODIFIES THE ORIGINAL MATRIX!

// To get a copy of column data
List<String> names = matrix['name'] as List  // Copies to new list
```

### JVM Tuning for Large Datasets

Configure JVM for large datasets:

```bash
# Increase heap size
java -Xmx4g -Xms4g -jar myapp.jar

# Use G1 garbage collector for large heaps
java -Xmx8g -XX:+UseG1GC -jar myapp.jar

# Monitor memory usage
java -Xmx4g -XX:+PrintGCDetails -jar myapp.jar
```

For Groovy scripts:

```bash
# Set JAVA_OPTS before running Groovy
export JAVA_OPTS="-Xmx4g -Xms2g"
groovy myscript.groovy
```

## Profiling and Benchmarking

### Timing Operations

Measure performance of your operations:

```groovy
def timeIt(String label, Closure operation) {
    long start = System.nanoTime()
    def result = operation()
    long end = System.nanoTime()
    println "$label: ${(end - start) / 1_000_000} ms"
    return result
}

// Usage
Matrix result = timeIt("Loading CSV") {
    CsvImporter.importCsv(file)
}

timeIt("Filtering") {
    result.subset { it['value'] > 100 }
}

timeIt("Aggregation") {
    Stat.sumBy(result, 'value', 'category')
}
```

### Comparing Approaches

Benchmark different approaches to find the fastest:

```groovy
def benchmark(String label, int iterations, Closure operation) {
    // Warmup
    3.times { operation() }

    // Measure
    long total = 0
    iterations.times {
        long start = System.nanoTime()
        operation()
        total += System.nanoTime() - start
    }

    println "$label: ${total / iterations / 1_000_000} ms average"
}

// Compare approaches
benchmark("Subset with closure", 10) {
    matrix.subset { it['value'] > 100 }
}

benchmark("Subset with column name", 10) {
    matrix.subset('value') { it > 100 }
}

benchmark("GINQ query", 10) {
    GQ { from r in matrix where r['value'] > 100 select r }
}
```

### Using @CompileStatic

For performance-critical code, use static compilation:

```groovy
import groovy.transform.CompileStatic

@CompileStatic
class DataProcessor {

    static double calculateSum(Matrix matrix, String column) {
        Column col = matrix.column(column)
        double sum = 0
        for (int i = 0; i < col.size(); i++) {
            Number val = col.get(i) as Number
            if (val != null) {
                sum += val.doubleValue()
            }
        }
        return sum
    }

    static Matrix filterFast(Matrix matrix, String column, double threshold) {
        List<Integer> indices = []
        Column col = matrix.column(column)
        for (int i = 0; i < col.size(); i++) {
            Number val = col.get(i) as Number
            if (val != null && val.doubleValue() > threshold) {
                indices << i
            }
        }
        return matrix.subset(indices)
    }
}
```

### Identifying Bottlenecks

Common bottlenecks and solutions:

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Slow iteration | Row-by-row access | Use column operations |
| Memory errors | Large intermediate objects | Process in chunks |
| Slow filtering | Complex closures | Simplify conditions, use indexes |
| Slow type conversion | Repeated parsing | Pre-convert types once |
| Slow joins | Large cartesian products | Filter before joining |

## Integration Performance

### Database Query Optimization

Optimize database interactions:

```groovy
// Use prepared statements for repeated queries
def preparedQuery = connection.prepareStatement("""
    SELECT * FROM data WHERE category = ? AND value > ?
""")

categories.each { cat ->
    preparedQuery.setString(1, cat)
    preparedQuery.setDouble(2, threshold)
    ResultSet rs = preparedQuery.executeQuery()
    Matrix result = Matrix.builder().resultSet(rs).build()
    processCategory(result)
}

// Use batch inserts
connection.autoCommit = false
def insertStmt = connection.prepareStatement(
    "INSERT INTO results (id, value) VALUES (?, ?)"
)

matrix.rows().each { row ->
    insertStmt.setInt(1, row['id'] as int)
    insertStmt.setDouble(2, row['value'] as double)
    insertStmt.addBatch()
}
insertStmt.executeBatch()
connection.commit()
```

### Efficient JSON Processing

Optimize JSON operations:

```groovy
import se.alipsa.matrix.json.JsonImporter
import se.alipsa.matrix.json.JsonExporter

// For large JSON arrays, stream if possible
// JsonImporter loads entire JSON into memory

// For export, write directly to file
JsonExporter.exportToJson(matrix, new File("output.json"))

// Avoid: Creating string then writing
// String json = JsonExporter.toJson(matrix)
// new File("output.json").text = json
```

### Caching Strategies

Cache expensive computations:

```groovy
class MatrixCache {
    private Map<String, Matrix> cache = [:]
    private Map<String, Long> timestamps = [:]
    private long ttlMillis = 60000  // 1 minute default

    Matrix getOrCompute(String key, Closure<Matrix> computation) {
        if (cache.containsKey(key)) {
            if (System.currentTimeMillis() - timestamps[key] < ttlMillis) {
                return cache[key]
            }
        }

        Matrix result = computation()
        cache[key] = result
        timestamps[key] = System.currentTimeMillis()
        return result
    }

    void invalidate(String key) {
        cache.remove(key)
        timestamps.remove(key)
    }

    void clear() {
        cache.clear()
        timestamps.clear()
    }
}

// Usage
def cache = new MatrixCache()

Matrix expensiveResult = cache.getOrCompute("daily_summary") {
    // Expensive computation
    loadAndProcess()
}
```

## Common Pitfalls

### Pitfall 1: Unnecessary Type Conversions

```groovy
// Bad: Repeated conversions
matrix.rows().each { row ->
    BigDecimal val = new BigDecimal(row['value'].toString())
    // ...
}

// Good: Convert once when loading
matrix = matrix.convert('value', BigDecimal)
// Then access directly
matrix.rows().each { row ->
    BigDecimal val = row['value']
    // ...
}
```

### Pitfall 2: Inefficient Filtering

```groovy
// Bad: Multiple passes through data
Matrix filtered = matrix
    .subset { it['a'] > 10 }
    .subset { it['b'] < 100 }
    .subset { it['c'] == 'active' }

// Good: Single pass
Matrix filtered = matrix.subset { row ->
    row['a'] > 10 && row['b'] < 100 && row['c'] == 'active'
}
```

### Pitfall 3: Not Using Column Operations

```groovy
// Bad: Row iteration for column statistics
double sum = 0
matrix.rows().each { row ->
    sum += row['value'] as double
}

// Good: Column method
double sum = matrix['value'].sum()

// Even better: Stat class
import se.alipsa.matrix.core.Stat
double mean = Stat.mean(matrix['value'])
```

### Pitfall 4: Memory Leaks in Closures

```groovy
// Bad: Closure captures entire matrix
def largeMatrix = loadHugeData()
def processor = { value ->
    // This closure captures largeMatrix even if not used
    value * 2
}

// Good: Extract only what's needed
def largeMatrix = loadHugeData()
def multiplier = largeMatrix['factor'].mean()
largeMatrix = null  // Release early

def processor = { value ->
    value * multiplier  // Only captures the multiplier
}
```

### Pitfall 5: Creating Matrices in Loops

```groovy
// Bad: Creates many Matrix objects
List<Matrix> results = []
categories.each { cat ->
    Matrix filtered = matrix.subset('category', cat)
    Matrix transformed = filtered.apply('value') { it * 2 }
    results << transformed
}

// Better: Process without creating many matrices
Map<String, List> resultsByCategory = [:]
matrix.rows().each { row ->
    String cat = row['category']
    if (!resultsByCategory.containsKey(cat)) {
        resultsByCategory[cat] = []
    }
    resultsByCategory[cat] << row['value'] * 2
}
```

### Pitfall 6: Ignoring Column Index

```groovy
// Slow: Column name lookup on every access
for (int i = 0; i < matrix.rowCount(); i++) {
    values << matrix[i, 'expensiveColumnName']
}

// Faster: Use column index
int colIdx = matrix.columnIndex('expensiveColumnName')
for (int i = 0; i < matrix.rowCount(); i++) {
    values << matrix[i, colIdx]
}

// Fastest: Access column directly
Column col = matrix.column('expensiveColumnName')
values.addAll(col)
```

## Complete Performance Example

Here's a complete example demonstrating performance best practices:

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.CsvImporter
import groovy.transform.CompileStatic

@CompileStatic
class OptimizedAnalysis {

    static Map<String, Object> analyze(File dataFile) {
        long start = System.currentTimeMillis()
        Map<String, Object> results = [:]

        // 1. Load with explicit types (faster than inference)
        println "Loading data..."
        Matrix data = CsvImporter.importCsv(dataFile)
        results['loadTime'] = System.currentTimeMillis() - start
        results['rowCount'] = data.rowCount()

        // 2. Convert types once
        start = System.currentTimeMillis()
        data = data.convert([
            'date': java.time.LocalDate,
            'amount': BigDecimal,
            'quantity': Integer
        ])
        results['convertTime'] = System.currentTimeMillis() - start

        // 3. Use column arithmetic (optimized)
        start = System.currentTimeMillis()
        Column amounts = data['amount'] as Column
        Column quantities = data['quantity'] as Column
        Column totals = amounts * quantities
        data = data.addColumn('total', BigDecimal, totals)
        results['arithmeticTime'] = System.currentTimeMillis() - start

        // 4. Single-pass filtering
        start = System.currentTimeMillis()
        Matrix filtered = data.subset { row ->
            (row['amount'] as BigDecimal) > 100 &&
            (row['quantity'] as Integer) > 0
        }
        results['filterTime'] = System.currentTimeMillis() - start
        results['filteredRows'] = filtered.rowCount()

        // 5. Efficient aggregation
        start = System.currentTimeMillis()
        Column totalCol = filtered['total'] as Column
        results['sum'] = totalCol.sum()
        results['mean'] = Stat.mean(totalCol)
        results['aggregateTime'] = System.currentTimeMillis() - start

        // 6. Group by with minimal overhead
        start = System.currentTimeMillis()
        Matrix summary = Stat.sumBy(filtered, 'total', 'category')
        results['groupByTime'] = System.currentTimeMillis() - start
        results['categoryCount'] = summary.rowCount()

        return results
    }

    static void main(String[] args) {
        if (args.length == 0) {
            println "Usage: OptimizedAnalysis <data.csv>"
            return
        }

        File dataFile = new File(args[0])
        if (!dataFile.exists()) {
            println "File not found: ${dataFile.absolutePath}"
            return
        }

        println "Running optimized analysis..."
        Map results = analyze(dataFile)

        println "\n=== Performance Results ==="
        println "Rows processed: ${results['rowCount']}"
        println "Rows after filter: ${results['filteredRows']}"
        println "Categories: ${results['categoryCount']}"
        println ""
        println "Timing:"
        println "  Load:      ${results['loadTime']} ms"
        println "  Convert:   ${results['convertTime']} ms"
        println "  Arithmetic: ${results['arithmeticTime']} ms"
        println "  Filter:    ${results['filterTime']} ms"
        println "  Aggregate: ${results['aggregateTime']} ms"
        println "  Group By:  ${results['groupByTime']} ms"
        println ""
        println "Statistics:"
        println "  Sum:  ${results['sum']}"
        println "  Mean: ${results['mean']}"
    }
}
```

## Summary

Key performance best practices:

1. **Understand the memory model**: Matrix is column-based; optimize for column access
2. **Choose appropriate types**: Use `Double` instead of `BigDecimal` when precision isn't critical
3. **Batch operations**: Use `apply()` instead of cell-by-cell updates
4. **Single-pass filtering**: Combine filter conditions into one `subset()` call
5. **Use column methods**: Leverage built-in Column arithmetic and statistics
6. **Process in chunks**: Break large datasets into manageable pieces
7. **Release memory**: Null out large objects when done
8. **Cache column references**: Avoid repeated column lookups by name
9. **Profile your code**: Measure before and after optimizations
10. **Use @CompileStatic**: For performance-critical sections

By following these practices, you can work efficiently with datasets containing hundreds of thousands of rows while maintaining the convenience and expressiveness of the Matrix API.

Go to [previous section](18-advanced-operations.md) | Back to [outline](outline.md)
