# Matrix Core Module

The matrix-core module is the foundation of the Alipsa Matrix library. It provides the essential classes and functionality for working with tabular data. In this section, we'll explore the key components of the matrix-core module and how to use them effectively.

## Understanding the Matrix Class

### What is a Matrix?

A Matrix in the Alipsa library is a grid with a header and defined column types. You can think of it as an in-memory ResultSet or a data frame similar to those in R or pandas in Python. It provides a structured way to work with tabular data, making operations like filtering, transformation, and analysis more intuitive.

### Matrix vs. Grid

The library provides two main classes for working with tabular data:

1. **Matrix**: A typed table with headers and defined column types
2. **Grid**: A uniformly typed table without headers

The Matrix class is more feature-rich and is the primary focus of this tutorial, while Grid provides a simpler structure for working with uniform data types.

## Creating a Matrix

There are several ways to create a Matrix object, depending on your data source and requirements.

### Creating from Groovy Code

The most common way to create a Matrix is using the builder pattern:

```groovy
import se.alipsa.matrix.core.*

// Create a Matrix with data defined inline
def employees = [
    "employee": ['John Doe', 'Peter Smith', 'Jane Doe'],
    "salary": [21000, 23400, 26800],
    "startDate": ListConverter.toLocalDates(['2013-11-01', '2018-03-25', '2017-03-14']),
    "reviewPeriod": ListConverter.toYearMonths(['2020-01', '2019-04', '2018-10'])
]

def table = Matrix.builder().data(employees).build()

// Verify the data
assert table.salary[0] == 21000 // get index 0 from the column "salary"
assert table[0, 1] == 21000 // get row 0, column 1
assert table[0, 'salary'] == 21000 // get row index 0 from the column name salary
assert table['salary'][0] == 21000 // get the column and the first index
assert table.row(0).salary == 21000 // get the first row and then the salary column
assert table.row(0)[1] == 21000 // get the first row and then the second column
```

You can also create a Matrix by specifying columns and their types:

```groovy
import se.alipsa.matrix.core.*

// Create a Matrix with column definitions
def table = Matrix.builder().columns([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
])
.types([String, String, String]*3)
.build()

// Convert column types after creation
def table2 = table.convert([place: Integer, start: LocalDate])
println(table2.typeNames())  // Prints: [Integer, String, LocalDate]
```

### Creating from a Result Set

When working with databases, you can create a Matrix directly from a JDBC ResultSet:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.groovy.datautil.SqlUtil

// Connect to a database and execute a query
SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
    sql.query('SELECT * FROM PROJECT') { rs ->
        project = Matrix.builder().data(rs).build()
    }
}

// Now you can work with the project Matrix
println(project.content())
```

### Creating from a CSV File

You can easily create a Matrix from a CSV file:

```groovy
import se.alipsa.matrix.core.Matrix

// Create a Matrix from a CSV file
def table = Matrix.builder().data(new File('/some/path/foo.csv'), ';').build()
```

## Accessing Data in a Matrix

The Matrix class provides multiple ways to access and manipulate data.

### Using [] Notation

Data can be referenced using [] notation. For example, to get the content of the 3rd row and 2nd column:

```groovy
def value = table[3, 2]  // Row 3, Column 2
```

You can also access data by column name:

```groovy
def priceColumn = table["price"]  // Get the entire price column
def price = table[3, "price"]     // Get the price from row 3
```

If you pass only one argument, you get the column:

```groovy
def priceColumn = table["price"]
// or alternatively
def priceColumn = table.price
```

### Using Column Names

Matrix provides property-like access to columns:

```groovy
// These are equivalent
def priceColumn = table.price
def priceColumn = table["price"]
```

### Using Row Indices

You can access rows by index:

```groovy
def row = table.row(3)  // Get the 4th row (0-based indexing)
def value = row.price   // Get the price value from that row
```

## Data Manipulation

### Converting Data Types

The Matrix class provides several methods to convert column types:

1. **Using a map of column names and types**:

```groovy
def convertedTable = table.convert([
    place: Integer,
    start: LocalDate
])
```

2. **Using a list of column types**:

```groovy
def convertedTable = table.convert([Integer, String, LocalDate])
```

3. **Using a closure for complex conversions**:

```groovy
def convertedTable = table.convert('place', Integer, { 
    String val = String.valueOf(it).trim()
    if (val == 'null' || val == ',' || val.isBlank()) return null
    return Integer.valueOf(val)
})
```

### Transforming Data

You can transform data using the apply method:

```groovy
import se.alipsa.matrix.core.*
import java.time.*

def data = [
    'place': ['1', '2', '3', ','],
    'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10']
]

def table = Matrix.builder('emp')
    .columns(data)
    .build()
    .convert(place: Integer, start: LocalDate)

// Add 10 days to the start dates
def table2 = table.apply("start", { startDate ->
    startDate.plusDays(10)
})

println(table2.content())
```

### Subsetting Data

You can create subsets of your data using various methods:

```groovy
// Using findIndexValues to select rows
def rows = table.rows(table['place'].findIndexValues { it > 1 })
assertEquals(2, rows.size())

// Using subset method
def subSet = table.subset('place', { it > 1 })
assertIterableEquals(table.rows(1..2), subSet.grid())
```

## Performing Calculations

### Using the Apply Method

The apply method allows you to perform calculations on columns:

```groovy
import se.alipsa.matrix.core.*

def account = Matrix.builder('account')
    .columns([
        'id': ['1', '2', '3', '4'],
        'balance': [12323, 23400, 45932, 77200]
    ])
    .build()

def ir = Matrix.builder('interest rates')
    .columns([
        id: [1, 2, 3, 4],
        interestRate: [0.034, 0.022, 0.019, 0.028]
    ])
    .build()

def accountAndInterest = account.clone().withMatrixName('accountAndInterest')
accountAndInterest['interestAmount', Double] = Stat.apply(account['balance'], ir['interestRate']) { b * r }

// Result will have interest amounts calculated for each account
println(accountAndInterest.content())
```

### Column Arithmetics

The Matrix class overrides basic mathematical operations to work on columns:

```groovy
import se.alipsa.matrix.core.*

def m = Matrix.builder()
    .columns([
        id: [1, 2, 3, 4],
        balance: [10000, 1000.23, 20122, 12.1]
    ])
    .types(int, double)
    .build()

def r = Matrix.builder()
    .columns([
        id: [1, 2, 3, 4],
        ir: [0.041, 0.020, 0.035, 0.5]
    ])
    .types(int, double)
    .build()

def interest = [10000*0.041, 1000.23*0.020, 20122*0.035, 12.1*0.5]
assert interest == m.balance * r.ir

// You can also multiply all items in a column
assert [10000*0.05, 1000.23*0.05, 20122*0.05, 12.1*0.05] == m.balance * 0.05
```

### Statistical Operations with Stat Class

The Stat class provides various statistical functions:

```groovy
import se.alipsa.matrix.core.*
import static se.alipsa.matrix.core.Stat.*

def table = Matrix.builder().data([
    v0: [0.3, 2, 3],
    v1: [1.1, 1, 0.9],
    v2: [null, 'Foo', 'Foo']
])
.types(Number, Double, String)
.build()

def summary = summary(table)
println(summary)

// Calculate mean, median, standard deviation, etc.
def mean = mean(table.v0)
def median = median(table.v0)
def sd = sd(table.v0)
def variance = variance(table.v0)
```

## Using Groovy Integrated Queries (Ginq)

Groovy Integrated queries can be used on Matrix rows for powerful data manipulation:

```groovy
import se.alipsa.matrix.core.Matrix

def stock = Matrix.builder().data(
    name: ['Orange', 'Apple', 'Banana', 'Mango', 'Durian'],
    price: [11, 6, 4, 29, 32],
    stock: [2, 3, 1, 10, 9]
)
.types(String, int, int)
.build()

def result = GQ {
    from f in stock
    where f.price < 32
    orderby f.price in desc
    select f.name, f.price, f.stock
}

// You can also use Matrix's built-in query capabilities
def exp = stock.subset(it.price < 32).orderBy('price', true)

// Create a new Matrix from the query result
def matrix2 = Matrix.builder().ginqResult(result).build()
```

## The Grid Class

A Grid is a uniformly typed table. The grid class contains static functions to operate on a 2D list structure.

### Creating a Grid

```groovy
import se.alipsa.matrix.core.*

def foo = [
    [12.0, 3.0, Math.PI],
    ["1.9", 2, 3],
    ["4.3", 2, 3]
] as Grid

// A typed way is usually clearer
def bar = new Grid<Number>([
    [12.0, 3.0, Math.PI],
    [1.9, 2, 3],
    [4.3, 2, 3]
])

// Calculate mean
Stat.means(bar)
```

### Grid Operations

The Grid class provides several operations:

- **convert**: Converts one column type to another
- **clone**: Creates a deep copy of the grid
- **transpose**: "Rotates" the grid 90 degrees
- **isValid**: Checks if it is a proper grid or not

Elements can be accessed using the simple square bracket notation:

```groovy
foo[0, 1] = 3.23
assert 3.23 == foo[0, 1]
```

## Using Matrix from Other JVM Languages

If you're using the Matrix library from Java, you'll need to use the underlying methods instead of the shorthand notation:

```java
// Instead of
myMatrix[2, 'id'] = 34;

// You must use
myMatrix.putAt(2, "id", 34);

// Instead of
def myGroovyVar = myMatrix[1, 2];

// You must use
var myJavaVar = myMatrix.getAt(1, 2);
```

There are some utility classes making Matrix creation less painful in Java:

```java
// In Groovy
def gEmpData = Matrix.builder()
    .data(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    )
    .types(int, String, Number, LocalDate)
    .build();

// In Java
import se.alipsa.matrix.core.util.Columns;
import static se.alipsa.matrix.core.util.CollectionUtils.*;

var jEmpData = Matrix.builder().data( new Columns()
    .add("emp_id", r(1, 5))
    .add("emp_name", "Rick", "Dan", "Michelle", "Ryan", "Gary")
    .add("salary", 623.3, 515.2, 611.0, 729.0, 843.25)
    .add("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
)
.types(int.class, String.class, Number.class, LocalDate.class)
.build();
```

## Conclusion

The matrix-core module provides a solid foundation for working with tabular data in Groovy and Java. Its intuitive API, powerful data manipulation capabilities, and statistical functions make it an excellent choice for data analysis and processing tasks.

In the next sections, we'll explore the additional modules that build upon this foundation to provide specialized functionality for various data formats and operations.
