# Advanced Matrix Operations

This chapter covers advanced data manipulation techniques that go beyond the basics covered in the matrix-core tutorial. You'll learn how to perform complex filtering, advanced transformations, joining matrices, grouping and aggregation, time series operations, matrix reshaping, and advanced GINQ queries.

## Prerequisites

Before proceeding, ensure you're familiar with:
- Matrix basics (Chapter 2: Matrix Core)
- Statistical operations (Chapter 3: Matrix Stats)
- GINQ syntax (basic usage)

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.datasets.Dataset
import java.time.*
```

## Complex Subsetting and Filtering

While basic subsetting with `subset()` is covered in the core tutorial, advanced scenarios often require more sophisticated filtering.

### Multi-Condition Filtering

```groovy
Matrix employees = Matrix.builder('employees')
    .data(
        name: ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve', 'Frank'],
        department: ['Engineering', 'Sales', 'Engineering', 'HR', 'Engineering', 'Sales'],
        salary: [75000, 55000, 82000, 48000, 95000, 62000],
        yearsExp: [5, 3, 8, 2, 12, 4],
        active: [true, true, true, false, true, true]
    )
    .types([String, String, Integer, Integer, Boolean])
    .build()

// Filter with multiple conditions using AND logic
Matrix seniorEngineers = employees.subset { row ->
    row['department'] == 'Engineering' &&
    row['salary'] > 70000 &&
    row['yearsExp'] >= 5 &&
    row['active'] == true
}
println "Senior Engineers:"
println seniorEngineers
```

Output:
```
Matrix (employees, 3 x 5)
name   	department 	salary	yearsExp	active
Alice  	Engineering	75000 	5       	true
Charlie	Engineering	82000 	8       	true
Eve    	Engineering	95000 	12      	true
```

### OR Conditions and Complex Logic

```groovy
// Filter with OR conditions
Matrix engineersOrHighSalary = employees.subset { row ->
    row['department'] == 'Engineering' || row['salary'] > 60000
}

// Complex nested conditions
Matrix complexFilter = employees.subset { row ->
    (row['department'] == 'Engineering' && row['yearsExp'] >= 5) ||
    (row['department'] == 'Sales' && row['salary'] > 60000)
}
```

### Filtering with Lists (IN clause equivalent)

```groovy
// Filter where value is in a list
List<String> targetDepts = ['Engineering', 'HR']
Matrix filtered = employees.subset { row ->
    row['department'] in targetDepts
}

// Exclude values in a list (NOT IN)
Matrix excluded = employees.subset { row ->
    !(row['department'] in ['Sales'])
}
```

### Null-Safe Filtering

```groovy
Matrix dataWithNulls = Matrix.builder('data')
    .data(
        name: ['Alice', 'Bob', null, 'Diana'],
        score: [85, null, 72, 90]
    )
    .types([String, Integer])
    .build()

// Filter handling nulls explicitly
Matrix nonNull = dataWithNulls.subset { row ->
    row['name'] != null && row['score'] != null
}

// Filter with null coalescing
Matrix withDefaults = dataWithNulls.subset { row ->
    (row['score'] ?: 0) > 70
}
```

### Dynamic Column Filtering

```groovy
// Filter based on column name provided as variable
String filterColumn = 'salary'
Object filterValue = 60000

Matrix dynamicFilter = employees.subset { row ->
    (row[filterColumn] as Number) > (filterValue as Number)
}
```

### Index-Based Row Selection

```groovy
// Select specific rows by index
Matrix selected = employees.subset(0..2)  // First 3 rows

// Select non-contiguous rows
List<Integer> indices = [0, 2, 4]
List<List> selectedRows = employees.rows(indices)
Matrix nonContiguous = Matrix.builder()
    .rows(selectedRows as List<List>)
    .columnNames(employees.columnNames())
    .types(employees.types())
    .build()
```

## Advanced Transformations

### Column-wise Transformations with apply()

The `apply()` method transforms values in a column:

```groovy
Matrix sales = Matrix.builder('sales')
    .data(
        product: ['A', 'B', 'C', 'D'],
        price: [100.0, 250.0, 75.0, 180.0],
        quantity: [10, 5, 20, 8]
    )
    .types([String, BigDecimal, Integer])
    .build()

// Simple transformation: apply 10% discount
sales.apply('price') { price ->
    price * 0.9
}

// Transform with type conversion
sales.apply('price') { price ->
    Math.round(price) as Integer
}

println sales
```

### Conditional Transformations

```groovy
// Apply transformation only to rows meeting criteria
Matrix data = Matrix.builder('data')
    .data(
        category: ['A', 'B', 'A', 'C', 'B'],
        value: [100, 200, 150, 300, 250]
    )
    .types([String, Integer])
    .build()

// Double values only for category 'A'
data.apply('value', { row -> row['category'] == 'A' }) { val ->
    val * 2
}

// Apply to specific row indices
data.apply('value', [0, 2, 4]) { val ->
    val + 50
}
```

### Row-wise Transformations with applyRows()

When you need access to the entire row during transformation:

```groovy
Matrix orders = Matrix.builder('orders')
    .data(
        product: ['Widget', 'Gadget', 'Gizmo'],
        price: [25.0, 45.0, 15.0],
        quantity: [10, 5, 20],
        discount: [0.1, 0.0, 0.15]
    )
    .types([String, BigDecimal, Integer, BigDecimal])
    .build()

// Add calculated column using row values
orders.addColumn('total', BigDecimal, [null] * orders.rowCount())
orders.applyRows('total') { row ->
    def price = row['price'] as BigDecimal
    def qty = row['quantity'] as Integer
    def disc = row['discount'] as BigDecimal
    price * qty * (1 - disc)
}

println orders
```

### Creating Derived Columns

```groovy
Matrix employees = Matrix.builder('emp')
    .data(
        firstName: ['John', 'Jane', 'Bob'],
        lastName: ['Doe', 'Smith', 'Johnson'],
        salary: [50000, 65000, 55000]
    )
    .types([String, String, Integer])
    .build()

// Create full name column
List<String> fullNames = []
employees.each { row ->
    fullNames << "${row['firstName']} ${row['lastName']}"
}
employees.addColumn('fullName', String, fullNames)

// Create bonus column based on salary
List<Integer> bonuses = employees['salary'].collect { sal ->
    (sal * 0.1) as Integer
}
employees.addColumn('bonus', Integer, bonuses)

println employees
```

### Replacing Values

```groovy
Matrix data = Matrix.builder('data')
    .data(
        status: ['active', 'inactive', 'pending', 'active'],
        code: [1, 2, 3, 1]
    )
    .types([String, Integer])
    .build()

// Replace specific value in all columns
data.replace('active', 'ACTIVE')

// Replace in specific column
data.replace('status', 'inactive', 'INACTIVE')

// Replace with pattern matching using apply
data.apply('status') { status ->
    status?.toUpperCase()
}
```

## Joining and Merging Matrices

The `Joiner` class provides SQL-like join operations.

### Inner Join

```groovy
import se.alipsa.matrix.core.Joiner

Matrix employees = Matrix.builder('employees')
    .data(
        empId: [1, 2, 3, 4],
        name: ['Alice', 'Bob', 'Charlie', 'Diana'],
        deptId: [101, 102, 101, 103]
    )
    .types([Integer, String, Integer])
    .build()

Matrix departments = Matrix.builder('departments')
    .data(
        deptId: [101, 102, 104],
        deptName: ['Engineering', 'Sales', 'Marketing']
    )
    .types([Integer, String])
    .build()

// Inner join - only matching rows
Matrix joined = Joiner.merge(employees, departments, 'deptId')
println "Inner Join:"
println joined
```

Output:
```
Inner Join:
Matrix (employees, 3 x 4)
empId	name   	deptId	deptName
1    	Alice  	101   	Engineering
2    	Bob    	102   	Sales
3    	Charlie	101   	Engineering
```

### Left Join

```groovy
// Left join - all rows from left table, nulls for non-matching
Matrix leftJoined = Joiner.merge(employees, departments, 'deptId', true)
println "Left Join:"
println leftJoined
```

Output:
```
Left Join:
Matrix (employees, 4 x 4)
empId	name   	deptId	deptName
1    	Alice  	101   	Engineering
2    	Bob    	102   	Sales
3    	Charlie	101   	Engineering
4    	Diana  	103   	null
```

### Join on Different Column Names

```groovy
Matrix orders = Matrix.builder('orders')
    .data(
        orderId: [1, 2, 3],
        customerId: [100, 101, 100]
    )
    .types([Integer, Integer])
    .build()

Matrix customers = Matrix.builder('customers')
    .data(
        id: [100, 101, 102],
        customerName: ['Acme Corp', 'Widget Inc', 'Tech Ltd']
    )
    .types([Integer, String])
    .build()

// Join on columns with different names
Matrix ordersWithCustomers = Joiner.merge(
    orders,
    customers,
    [x: 'customerId', y: 'id']
)
println ordersWithCustomers
```

### Combining Multiple Matrices Vertically

```groovy
Matrix q1Sales = Matrix.builder('Q1')
    .data(
        product: ['A', 'B'],
        sales: [1000, 1500]
    )
    .types([String, Integer])
    .build()

Matrix q2Sales = Matrix.builder('Q2')
    .data(
        product: ['A', 'B'],
        sales: [1200, 1400]
    )
    .types([String, Integer])
    .build()

// Append rows from another matrix
Matrix combined = q1Sales + q2Sales
// Or using addRows
q1Sales.addRows(q2Sales.rowList())
```

### Adding Columns from Another Matrix

```groovy
Matrix base = Matrix.builder('base')
    .data(id: [1, 2, 3], name: ['A', 'B', 'C'])
    .types([Integer, String])
    .build()

Matrix additional = Matrix.builder('extra')
    .data(id: [1, 2, 3], score: [85, 92, 78], grade: ['B', 'A', 'C'])
    .types([Integer, Integer, String])
    .build()

// Add specific columns from another matrix
base.addColumns(additional, 'score', 'grade')
println base
```

## Grouping and Aggregation

Grouping and aggregation can be achieved using Groovy's collection methods and the Stat class.

### Manual Grouping

```groovy
Matrix sales = Matrix.builder('sales')
    .data(
        region: ['North', 'South', 'North', 'South', 'East', 'East'],
        product: ['A', 'A', 'B', 'B', 'A', 'B'],
        amount: [100, 150, 200, 175, 125, 300]
    )
    .types([String, String, Integer])
    .build()

// Group by region and calculate sum
Map<String, List<Row>> byRegion = sales.rows().groupBy { row ->
    row['region']
}

// Calculate aggregates for each group
Map<String, Integer> regionTotals = [:]
byRegion.each { region, rows ->
    regionTotals[region] = rows.collect { it['amount'] as Integer }.sum()
}
println "Region Totals: $regionTotals"

// Create summary matrix
Matrix summary = Matrix.builder('summary')
    .data(
        region: regionTotals.keySet().toList(),
        totalSales: regionTotals.values().toList()
    )
    .types([String, Integer])
    .build()
println summary
```

### Multiple Aggregations

```groovy
// Calculate multiple aggregates per group
Map<String, Map<String, Object>> regionStats = [:]
byRegion.each { region, rows ->
    def amounts = rows.collect { it['amount'] as Integer }
    regionStats[region] = [
        count: amounts.size(),
        sum: amounts.sum(),
        avg: amounts.sum() / amounts.size(),
        min: amounts.min(),
        max: amounts.max()
    ]
}

// Convert to Matrix
List<String> regions = regionStats.keySet().toList()
Matrix statsMatrix = Matrix.builder('regionStats')
    .data(
        region: regions,
        count: regions.collect { regionStats[it].count },
        sum: regions.collect { regionStats[it].sum },
        avg: regions.collect { regionStats[it].avg },
        min: regions.collect { regionStats[it].min },
        max: regions.collect { regionStats[it].max }
    )
    .types([String, Integer, Integer, BigDecimal, Integer, Integer])
    .build()

println statsMatrix
```

### Group By Multiple Columns

```groovy
// Group by multiple columns
Map<List, List<Row>> byRegionProduct = sales.rows().groupBy { row ->
    [row['region'], row['product']]
}

// Calculate aggregates
List<List> summaryRows = []
byRegionProduct.each { key, rows ->
    def amounts = rows.collect { it['amount'] as Integer }
    summaryRows << [key[0], key[1], amounts.sum(), amounts.size()]
}

Matrix multiGroupSummary = Matrix.builder('multiGroupSummary')
    .rows(summaryRows)
    .columnNames(['region', 'product', 'totalAmount', 'count'])
    .types([String, String, Integer, Integer])
    .build()

println multiGroupSummary
```

### Using Stat Functions

```groovy
import se.alipsa.matrix.core.Stat

Matrix data = Dataset.mtcars()

// Get frequency table
Matrix frequency = Stat.frequency(data['cyl'])
println "Cylinder Frequency:"
println frequency

// Get summary statistics
def summary = Stat.summary(data)
println "\nData Summary:"
println summary

// Calculate statistics on specific columns
println "\nMPG Statistics:"
println "Sum: ${Stat.sum(data['mpg'])}"
println "Mean: ${Stat.mean(data['mpg'])}"
println "Median: ${Stat.median(data['mpg'])}"
println "Std Dev: ${Stat.sd(data['mpg'])}"
println "Variance: ${Stat.variance(data['mpg'])}"
println "Min: ${Stat.min(data['mpg'])}"
println "Max: ${Stat.max(data['mpg'])}"
```

## Working with Time Series Data

### Date/Time Column Operations

```groovy
import java.time.*
import se.alipsa.matrix.core.ListConverter

Matrix events = Matrix.builder('events')
    .data(
        date: ListConverter.toLocalDates([
            '2024-01-15', '2024-01-20', '2024-02-10',
            '2024-02-25', '2024-03-05', '2024-03-18'
        ]),
        value: [100, 150, 120, 180, 200, 175]
    )
    .types([LocalDate, Integer])
    .build()

// Extract date components
List<Integer> years = events['date'].collect { it.year }
List<Integer> months = events['date'].collect { it.monthValue }
List<Integer> days = events['date'].collect { it.dayOfMonth }
List<String> dayOfWeek = events['date'].collect { it.dayOfWeek.toString() }

events.addColumn('year', Integer, years)
events.addColumn('month', Integer, months)
events.addColumn('dayOfWeek', String, dayOfWeek)

println events
```

### Filtering by Date Range

```groovy
LocalDate startDate = LocalDate.of(2024, 2, 1)
LocalDate endDate = LocalDate.of(2024, 2, 28)

Matrix februaryData = events.subset { row ->
    def date = row['date'] as LocalDate
    date >= startDate && date <= endDate
}
println "February Data:"
println februaryData
```

### Monthly Aggregation

```groovy
// Group by month and aggregate
Map<Integer, List<Row>> byMonth = events.rows().groupBy { row ->
    (row['date'] as LocalDate).monthValue
}

List<List> monthlyData = []
byMonth.toSorted { a, b -> a.key <=> b.key }.each { month, rows ->
    def values = rows.collect { it['value'] as Integer }
    monthlyData << [month, values.sum(), values.size(), values.sum() / values.size()]
}

Matrix monthlySummary = Matrix.builder('monthlySummary')
    .rows(monthlyData)
    .columnNames(['month', 'total', 'count', 'average'])
    .types([Integer, Integer, Integer, BigDecimal])
    .build()

println monthlySummary
```

### Lag and Lead Operations

```groovy
// Create lag column (previous value)
Matrix timeSeries = Matrix.builder('series')
    .data(
        period: [1, 2, 3, 4, 5],
        value: [100, 110, 105, 115, 120]
    )
    .types([Integer, Integer])
    .build()

List<Integer> values = timeSeries['value'] as List<Integer>

// Lag (previous value)
List<Integer> lagValues = [null] + values[0..-2]
timeSeries.addColumn('prevValue', Integer, lagValues)

// Lead (next value)
List<Integer> leadValues = values[1..-1] + [null]
timeSeries.addColumn('nextValue', Integer, leadValues)

// Calculate period-over-period change
List<Integer> changes = []
for (int i = 0; i < values.size(); i++) {
    if (i == 0) {
        changes << null
    } else {
        changes << (values[i] - values[i-1])
    }
}
timeSeries.addColumn('change', Integer, changes)

println timeSeries
```

### Rolling/Moving Averages

```groovy
// Calculate 3-period moving average
int windowSize = 3
List<BigDecimal> movingAvg = []

for (int i = 0; i < values.size(); i++) {
    if (i < windowSize - 1) {
        movingAvg << null
    } else {
        def window = values[(i - windowSize + 1)..i]
        movingAvg << (window.sum() / windowSize) as BigDecimal
    }
}

timeSeries.addColumn('ma3', BigDecimal, movingAvg)
println timeSeries
```

## Matrix Reshaping

### Transpose (Rows to Columns)

```groovy
Matrix wide = Matrix.builder('wide')
    .data(
        metric: ['Revenue', 'Expenses', 'Profit'],
        Q1: [1000, 800, 200],
        Q2: [1200, 850, 350],
        Q3: [1100, 900, 200]
    )
    .types([String, Integer, Integer, Integer])
    .build()

// Manual transpose
List<String> newColumns = ['quarter'] + (wide['metric'] as List<String>)
List<String> quarters = ['Q1', 'Q2', 'Q3']

List<List> transposedRows = []
quarters.eachWithIndex { quarter, idx ->
    def row = [quarter]
    for (int i = 0; i < wide.rowCount(); i++) {
        row << wide[i, quarter]
    }
    transposedRows << row
}

Matrix transposed = Matrix.builder('transposed')
    .rows(transposedRows)
    .columnNames(newColumns)
    .build()

println "Original:"
println wide
println "\nTransposed:"
println transposed
```

### Pivot Table

The Matrix class includes a `pivot()` method for reshaping:

```groovy
Matrix longFormat = Matrix.builder('sales')
    .data(
        region: ['North', 'North', 'South', 'South', 'North', 'South'],
        quarter: ['Q1', 'Q2', 'Q1', 'Q2', 'Q3', 'Q3'],
        sales: [100, 150, 200, 175, 125, 225]
    )
    .types([String, String, Integer])
    .build()

// Pivot: region as rows, quarter as columns, sales as values
Matrix pivoted = longFormat.pivot('region', 'quarter', 'sales')
println "Pivoted:"
println pivoted
```

### Melt (Wide to Long)

```groovy
Matrix wideData = Matrix.builder('wide')
    .data(
        id: [1, 2, 3],
        name: ['A', 'B', 'C'],
        jan: [100, 200, 150],
        feb: [110, 190, 160],
        mar: [120, 210, 155]
    )
    .types([Integer, String, Integer, Integer, Integer])
    .build()

// Melt to long format
List<String> valueColumns = ['jan', 'feb', 'mar']
List<List> longRows = []

wideData.each { row ->
    valueColumns.each { col ->
        longRows << [row['id'], row['name'], col, row[col]]
    }
}

Matrix longData = Matrix.builder('long')
    .rows(longRows)
    .columnNames(['id', 'name', 'month', 'value'])
    .types([Integer, String, String, Integer])
    .build()

println "Wide Format:"
println wideData
println "\nLong Format:"
println longData
```

## Advanced GINQ Queries

Groovy's GINQ (Groovy INtegrated Query) provides SQL-like syntax for querying collections.

### Basic GINQ with Matrix

```groovy
Matrix employees = Matrix.builder('employees')
    .data(
        name: ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve'],
        department: ['Engineering', 'Sales', 'Engineering', 'HR', 'Engineering'],
        salary: [75000, 55000, 82000, 48000, 95000]
    )
    .types([String, String, Integer])
    .build()

// Query using GINQ
def result = GQ {
    from row in employees.rows()
    where row['department'] == 'Engineering'
    orderby row['salary'] desc
    select row['name'], row['salary']
}

println "Engineers by salary (desc):"
result.each { println it }
```

### Aggregation with GINQ

```groovy
// Group by and aggregate
def deptStats = GQ {
    from row in employees.rows()
    groupby row['department']
    select row['department'] as dept,
           count() as empCount,
           sum(row['salary'] as Integer) as totalSalary
}

println "\nDepartment Statistics:"
deptStats.each { println "${it.dept}: ${it.empCount} employees, \$${it.totalSalary}" }
```

### Joins with GINQ

```groovy
Matrix departments = Matrix.builder('departments')
    .data(
        deptName: ['Engineering', 'Sales', 'HR'],
        budget: [500000, 300000, 200000]
    )
    .types([String, Integer])
    .build()

// Join employees with departments
def joined = GQ {
    from e in employees.rows()
    join d in departments.rows() on e['department'] == d['deptName']
    select e['name'], e['department'], e['salary'], d['budget']
}

println "\nJoined Data:"
joined.each { println it }
```

### Subqueries and Complex Conditions

```groovy
// Find employees earning above average
def avgSalary = Stat.mean(employees['salary'])

def aboveAverage = GQ {
    from row in employees.rows()
    where (row['salary'] as Integer) > avgSalary
    orderby row['salary'] desc
    select row['name'], row['salary']
}

println "\nEmployees earning above average (\$${avgSalary.round(0)}):"
aboveAverage.each { println "${it.v1}: \$${it.v2}" }
```

### Window-like Functions with GINQ

```groovy
Matrix sales = Matrix.builder('sales')
    .data(
        salesperson: ['Alice', 'Bob', 'Charlie', 'Alice', 'Bob'],
        quarter: ['Q1', 'Q1', 'Q1', 'Q2', 'Q2'],
        amount: [10000, 8000, 12000, 11000, 9500]
    )
    .types([String, String, Integer])
    .build()

// Running total by salesperson
def runningTotals = GQ {
    from row in sales.rows()
    groupby row['salesperson']
    select row['salesperson'] as person,
           sum(row['amount'] as Integer) as totalSales,
           count() as numQuarters
}

println "\nSalesperson Totals:"
runningTotals.each {
    println "${it.person}: \$${it.totalSales} over ${it.numQuarters} quarter(s)"
}
```

### Converting GINQ Results to Matrix

```groovy
// Query and convert to Matrix
def queryResult = GQ {
    from e in employees.rows()
    where (e['salary'] as Integer) > 60000
    select e['name'] as name, e['department'] as dept, e['salary'] as salary
}

// Collect results and build Matrix
List<List> resultRows = queryResult.collect { [it.name, it.dept, it.salary] }

Matrix resultMatrix = Matrix.builder('highEarners')
    .rows(resultRows)
    .columnNames(['name', 'department', 'salary'])
    .types([String, String, Integer])
    .build()

println "\nHigh Earners Matrix:"
println resultMatrix
```

## Complete Example: Sales Analysis Pipeline

Here's a comprehensive example combining multiple advanced techniques:

```groovy
import se.alipsa.matrix.core.*
import java.time.LocalDate

// Create sample sales data
Matrix sales = Matrix.builder('sales')
    .data(
        date: ListConverter.toLocalDates([
            '2024-01-15', '2024-01-22', '2024-02-05', '2024-02-18',
            '2024-03-01', '2024-03-15', '2024-01-10', '2024-02-20',
            '2024-03-25', '2024-01-28'
        ]),
        region: ['North', 'South', 'North', 'South', 'East',
                 'North', 'East', 'East', 'South', 'North'],
        product: ['A', 'B', 'A', 'A', 'B', 'B', 'A', 'B', 'A', 'A'],
        quantity: [10, 15, 8, 12, 20, 18, 14, 9, 16, 11],
        unitPrice: [25.0, 35.0, 25.0, 25.0, 35.0, 35.0, 25.0, 35.0, 25.0, 25.0]
    )
    .types([LocalDate, String, String, Integer, BigDecimal])
    .build()

println "=== Original Sales Data ==="
println sales

// Step 1: Add calculated column
List<BigDecimal> revenues = []
sales.each { row ->
    revenues << (row['quantity'] as Integer) * (row['unitPrice'] as BigDecimal)
}
sales.addColumn('revenue', BigDecimal, revenues)

// Step 2: Add date components
List<Integer> months = sales['date'].collect { (it as LocalDate).monthValue }
sales.addColumn('month', Integer, months)

// Step 3: Filter to Q1 only
Matrix q1Sales = sales.subset { row ->
    (row['month'] as Integer) <= 3
}

// Step 4: Group by region and calculate totals
Map<String, List<Row>> byRegion = q1Sales.rows().groupBy { it['region'] }
List<List> summaryRows = []

byRegion.each { region, rows ->
    def totalQty = rows.collect { it['quantity'] as Integer }.sum()
    def totalRev = rows.collect { it['revenue'] as BigDecimal }.sum()
    def avgPrice = totalRev / totalQty
    summaryRows << [region, rows.size(), totalQty, totalRev, avgPrice.round(2)]
}

Matrix regionSummary = Matrix.builder('regionSummary')
    .rows(summaryRows)
    .columnNames(['region', 'transactions', 'totalQty', 'totalRevenue', 'avgUnitPrice'])
    .types([String, Integer, Integer, BigDecimal, BigDecimal])
    .build()
    .orderBy('totalRevenue', Matrix.DESC)

println "\n=== Q1 Regional Summary ==="
println regionSummary

// Step 5: Monthly trend analysis
Map<Integer, List<Row>> byMonth = q1Sales.rows().groupBy { it['month'] }
List<List> monthlyTrend = []

byMonth.toSorted { a, b -> a.key <=> b.key }.each { month, rows ->
    def revenue = rows.collect { it['revenue'] as BigDecimal }.sum()
    monthlyTrend << [month, revenue]
}

Matrix monthlyRevenue = Matrix.builder('monthlyRevenue')
    .rows(monthlyTrend)
    .columnNames(['month', 'revenue'])
    .types([Integer, BigDecimal])
    .build()

println "\n=== Monthly Revenue Trend ==="
println monthlyRevenue

// Step 6: Product performance pivot
Matrix productPivot = q1Sales.pivot('product', 'region', 'revenue')
println "\n=== Product Revenue by Region ==="
println productPivot

println "\n=== Analysis Complete ==="
```

## Conclusion

This chapter covered advanced Matrix operations including:

- **Complex filtering** with multiple conditions, null handling, and dynamic columns
- **Advanced transformations** using apply(), applyRows(), and derived columns
- **Joining matrices** with inner and left joins
- **Grouping and aggregation** for summary statistics
- **Time series operations** including date extraction, filtering, and rolling calculations
- **Matrix reshaping** with pivot and melt operations
- **GINQ queries** for SQL-like data manipulation

These techniques enable sophisticated data analysis workflows directly in Groovy using the Matrix library. Combined with the machine learning capabilities from matrix-smile, you have a complete toolkit for data science tasks.

Go to [previous section](17-matrix-smile.md) | Go to [next section](19-performance-best-practices.md) | Back to [outline](outline.md)
