# Matrix Core

The matrix-core is the heart of the matrix project. It
contains the Matrix and Grid classes as well as several utility classes to
do basic statistics (sum, mean, median, sd, variance, counts, frequency etc.) and to
convert data into various shapes and formats.

## Matrix creation

There recommended way to create a Matrix is to use the MatrixBuilder created from the static Matrix.builder() method
The builder can create a Matrix based on data in the form of a Map<String, List> representing the column name and the column data,
or from a csv file, a url, a ResultSet, some list structure (either row using the rows() method orcolumns using the columns() method)
etc.

Examples:
```groovy
import se.alipsa.groovy.matrix.Matrix
import java.time.LocalDate
import static se.alipsa.groovy.matrix.ListConverter.*
import static se.alipsa.groovy.matrix.ValueConverter.*

// Creating based on lists of data
Matrix ed = Matrix.builder("ed")
        .columnNames('id', 'name', 'salary', 'start')
        .columns([
                    1..5,
                    ["Rick","Dan","Michelle","Ryan","Gary"],
                    [623.3,515.2,611.0,729.0,843.25],
                    toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
                ])
        .build()

// Creating based on a map of data, with types
Matrix empData = Matrix.builder('empData').data(
    [
        emp_id: 1..5,
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ])
    .types([int, String, Number, LocalDate])
    .build()

// Creating based on a list of columns
def e = Matrix.builder().columns([
    1..5,
    ["Rick","Dan","Michelle","Ryan","Gary"],
    [623.3,515.2,611.0,729.0,843.25],
    toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
  ])
  .build()

// Create based on column names and rows
def table = Matrix.builder()
        .columnNames("v0", "v1", "v2")
        .columns(
                [1, 2, 3], 
                [1.1, 1, 0.9], 
                [null, 7, "2"]
        )
        .build()

// Create based on a list of rows
def employeeList = []
employeeList << ['John Doe', 21000, asLocalDate('2013-11-01'), asLocalDate('2020-01-10')]
employeeList << ['Peter Smith', 23400,	'2018-03-25',	'2020-04-12']
employeeList << ['Jane Doe', 26800, asLocalDate('2017-03-14'), asLocalDate('2020-10-02')]
def employees = Matrix.builder().rows(employeeList).build()

// Created based on a CSV:
def data = [
    ['place', 'firstname', 'lastname', 'team'],
    ['1', 'Lorena', 'Wiebes', 'Team DSM'],
    ['2', 'Marianne', 'Vos', 'Team Jumbo Visma'],
    ['3', 'Lotte', 'Kopecky', 'Team SD Worx']
]
def file = File.createTempFile('FemmesStage1Podium', '.csv')
file.text = data*.join(',').join('\n') // Write the CSV file
def femmesStage1Podium = Matrix.builder().data(file).build()

// Create based on an URL
def plantGrowth = Matrix.builder().data(
    getClass().getResource('/PlantGrowth.csv'),
    ',',
    '"',
).build()

import groovy.sql.Sql
// Create based on a resultSet
def project = null
Sql.withInstance(dbUrl, dbUser, dbPasswd, dbDriver) { sql ->
  sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.builder().data(rs).build() }
}
```

## Getting data
A Matrix can be referenced using the shorthand square bracket syntax. If you use Java instead of groovy,
you can use the getAt() methods instead.

```groovy
import se.alipsa.groovy.matrix.Matrix

import javax.swing.JTable

// Given the following matrix
def myMatrix = Matrix.builder().data(
    id    : [1, 2, 3, 4],
    weight: [23.12, 19.98, 20.21, 24.10],
    height: [123, 128, 99, 113]
  ).types(int, BigDecimal, int)
  .build()
// Getting an individual value by matrix[row, column]
assert 123 == myMatrix[0, 2]
assert 123 == myMatrix.get(0, 2)
assert 123 == myMatrix[0, 'height']


//Getting an entire column
assert [1, 2, 3, 4] == myMatrix[0]
assert [1, 2, 3, 4] == myMatrix.column(0)
assert [1, 2, 3, 4] == myMatrix['id']
assert [1, 2, 3, 4] == myMatrix.column('id')
assert [1, 2, 3, 4] == myMatrix.id

// Getting an observation (row)
assert [2, 19.98, 128] == myMatrix.row(1)

// Getting all observations as a list of lists (List<List<Object>>)
assert 4 == myMatrix.rowList().size()

// Getting them as a List of Rows enables you to make changes to values that are reflected back
assert 4 == myMatrix.rows().size()

// Getting subset of rows in a new matrix, all observations where weight is less than 23
Matrix lightWeight = myMatrix.subset('weight', { it < 23 })
assert 2 == lightWeight.rowCount()

// if you have more complex criteria you can omit the column and deal with the entire row
Matrix lightAndShort = myMatrix.subset {
  BigDecimal weight = it[1]
  Integer height = it[2]
  return weight < 24 && height < 125
}

// Similarly to get just the list of rows matching the criteria, use the rows(Closure criteria) method
List<Row> lightAndShortRows = myMatrix.rows {
  return it.weight < 24 && it.height < 125
}

// Iterating through the data
def doubleVector = new Vector<Vector<String>>()
myMatrix.each { row ->
  def singleVector = new Vector<String>()
  singleVector.add(String.valueOf(row[1]))
  singleVector.add(String.valueOf(row[2]))
  doubleVector.add(singlevector)
}
JTable swingTable = new JTable(doubleVector, ['weight', 'height'] as Vector)

// Identical operation using the for..in syntax:
doubleVector = new Vector<Vector<String>>()
for (row in myMatrix) {
  def singleVector = new Vector<String>()
  singleVector.add(String.valueOf(row.weight))
  singleVector.add(String.valueOf(row.height))
  doubleVector.add(singlevector)
}
swingTable = new JTable(doubleVector, ['weight', 'height'] as Vector)

// Search for the first row where the value equals that in the column
assert [2, 19.98, 128] == myMatrix.findFirstRow('id', 2)

// The Matrix selectColumns(String... columnNames) return a new matrix with only the selected columns
def hightAndWeight = myMatrix.selectColumns('height', 'weight')
// The hightAndWeight Matrix will look like this:
// height	weight
// 123	  23.12
// 128	  19.98
//  99    20.21
// 113	  24.10

// Selecting the 2nd and 3rd row
List<Row> r = myMatrix.rows(1..2)

// Getting the weight and height columns
List<List<?>> weightHight = myMatrix.columns(['weight', 'height'])
// Other variation of this are 
// List<List<?>> columns(Integer[] indices) which selects rows based on the indices
// List<List<?>> columns() which returns all columns in a list format
```

## Displaying data
- String content(boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
- String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
- String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
- String toMarkdown(Map<String, String> attr = [:])
- String toMarkdown(List<String> alignment, Map<String, String> attr = [:])
- String toString()

### Examples:
```groovy
import se.alipsa.groovy.matrix.Matrix
// Given the following Matrix
def myMatrix = Matrix.builder().data(
    id    : [1, 2, 3, 4],
    weight: [23.12, 19.98, 20.21, 24.10],
    height: [123, 128, 99, 113])
    .types(int, BigDecimal, int)
    .build()
println myMatrix.content()
```
will output
```
id	weight	height
1	23.12	123
2	19.98	128
3	20.21	99
4	24.10	113
```
```groovy
println myMatrix.toMarkdown(Map.of("class", "table"))
```
will output
```
| id | weight | height |
| ---: | ---: | ---: |
| 1 | 23.12 | 123 |
| 2 | 19.98 | 128 |
| 3 | 20.21 | 99 |
| 4 | 24.10 | 113 |
{class="table" }
```

## Matrix meta data
- int rowCount(): Gives you the number of observations in the Matrix
- int columnCount(): GIves you the number of variables in the Matrix
- Stat.str(): Provides information about the structure of the Matrix
- Stat.summary(): Provides information about the data in the Matrix
- String getName(): the name of the Matrix
- void setName(String name)
- Matrix withName(String name): sets the name and returns itself allowing for method chaining 
- List<String> columnTypeNames(): gives you the simple names of the column types
- Class<?> columnType(String columnName): gives you the type class of the column  
- Class<?> columnType(int i): as above
- List<Class<?>> columnTypes(List<String> columnNames): as above but for a list of columns
- List<Class<?>> columnTypes(): as above but for all variables 
- Matrix renameColumn(int columnIndex, String after): Rename the specified column
- Matrix renameColumn(String before, String after): as above
- String columnName(int index): the variable name for the specified column
- void columnNames(List<String> names): set all variable names
- List<String> columnNames(): gives you all variable names
- int columnIndex(String columnName): the position for the variable specified
- List<Integer> columnIndices(List<String> columnNames): the positions for the variables specified

```groovy 
import se.alipsa.groovy.matrix.Matrix
import static se.alipsa.groovy.matrix.Stat.*

table = Matrix.builder('Test')
          .data(
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: [null, 'Foo', "Foo"]
          )
        .types(Number, double, String)
        .build()
println table.types()
println str(table)
```
Which will print
```
[class java.lang.Number, class java.lang.Double, class java.lang.String]
Matrix (Test, 3 observations of 3 variables)
--------------------------------------------
v0: [Number, 0.3, 2, 3]
v1: [Double, 1.1, 1, 0.9]
v2: [String, null, Foo, Foo]
```
Notice that the for the line that prints the column types, the second column contains Double and not double which was given.
This is because a List cannot contain primitives and also because we prefer to work with the wrapper over the 
raw primitive in Groovy.

```groovy
summary(table)
```
will print
```
v0
--
Type:	Number
Min:	0.3
1st Q:	2
Median:	2
Mean:	1.766666667
3rd Q:	3
Max:	3

v1
--
Type:	Double
Min:	0.9
1st Q:	1
Median:	1
Mean:	1.000000000
3rd Q:	1.1
Max:	1.1

v2
--
Type:	String
Number of unique values:	2
Most frequent:	Foo occurs 2 times (66.67%)
```

## Adding Data
- Matrix addRow(List<?> row)
- Matrix addRows(List<List<?>> rows)
- Matrix addColumn(String name, type = Object, List<?> column)
- Matrix addColumn(String name, type = Object, Integer index, List<?> column)
- Matrix addColumns(Matrix table, String... columns)
- Matrix addColumns(List<String> names, List<List<?>> columns, List<Class<?>> types)

```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder('Test')
        .data(
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: [null, 'Foo', "Foo"])
        .types(Number, double, String)
        .build()
table.addRow([0.9, 1.2, 'Bar'])
table.addColumn('v3', Integer, 1..4)
```
The table will now contain the following (one added row and one added column):

| v0 | v1 | v2 | v3 |
| ---: | ---: | --- | ---: |
| 0.3 | 1.1 | null | 1 |
| 2 | 1 | Foo | 2 |
| 3 | 0.9 | Foo | 3 |
| 0.9 | 1.2 | Bar | 4 |

## Removing data
- Matrix dropColumns(String... columnNames)
- Matrix dropColumnsExcept(String... columnNames)
- Matrix removeEmptyRows()

```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder('Test')
            .data(
                [
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: [null, 'Foo', "Foo"]
        ])
  .types(Number, double, String)
  .build()
table.dropColumns('v1')
```
Table will now contain:

| v0 | v2 |
| ---: | --- |
| 0.3 | null |
| 2 | Foo |
| 3 | Foo |

```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder('Test').data([
        v0: [null, 2, 3],
        v1: [null, 1, 0.9],
        v2: [null, 'Foo', "Bar"]
  ])
        .types([Number, double, String])
        .build()
table.removeEmptyRows()
```
Table will now contain

| v0 | v1 | v2  |
| ---: | ---: |-----|
| 2 | 1 | Foo |
| 3 | 0.9 | Bar |

```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder('Test').data(
                [
                v0: [null, 2, 3],
                v1: [null, 1, 0.9],
                v2: [null, 'Foo', "Bar"]
        ]).types([Number, double, String]).build()
println table.dropColumnsExcept('v0', 'v2').content()
```

Table will now contain:

| v0 | v2 |
| ---: | --- |
| null | null |
| 2 | Foo |
| 3 | Bar |


## Modifying data
- void putAt(Integer column, List<?> values)
  e.g. `myMatrix[1] = [42, 12, 10]`
- void putAt(String columnName, List<?> values)
  e.g. `myMatrix['temperature'] = [42, 12, 10]`
- void putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
```groovy
import se.alipsa.groovy.matrix.Matrix
import java.time.*
import static se.alipsa.groovy.matrix.ListConverter.*

// note that this version of put at will add a new column
 def table = Matrix.builder().data(
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo': [1, 2, 3]
 ).types(String, LocalDate, int).build()
        table["yearMonth", YearMonth, 0] = toYearMonths(table["start"])
```

| yearMonth | firstname | start | foo |
| --- | --- | --- | ---: |
| 2021-12 | Lorena | 2021-12-01 | 1 |
| 2022-07 | Marianne | 2022-07-10 | 2 |
| 2023-05 | Lotte | 2023-05-27 | 3 |

- void putAt(List where, List<?> column)
```groovy
// this will (can only) update an existing column
def table = Matrix.builder().data(
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
        'foo': [1, 2, 3]
).types(String, LocalDate, int).build
table["foo"] = [2,5,3]
table.content()
```

| firstname | start | foo |
| --- | --- | ---: |
| Lorena | 2021-12-01 | 2 |
| Marianne | 2022-07-10 | 5 |
| Lotte | 2023-05-27 | 3 |

- void putAt(Number rowIndex, Number colIndex, Object value)
```groovy
myMatrix.putAt(1,2,42)
```
- void putAt(List<Number> where, Object value)
```groovy 
myMatrix[1,2] = 42
// or in java
myMatrix.putAt(List.of(1,2),42)
```
- Matrix convert(int columnNumber, Class<?> type, Closure converter)
- Matrix convert(Converter[] converters)
- Matrix convert(String columnName, Class<?> type, Closure converter)
- Matrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)
- Matrix convert(List<Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)
- void replaceColumn(String columnName, Class<?> type = Object, List<?> values)
- Matrix apply(int columnNumber, Closure criteria, Closure function)
- Matrix apply(String columnName, Closure criteria, Closure function)
- Matrix apply(int columnNumber, List<Integer> rows, Closure function)
- Matrix apply(String columnName, List<Integer> rows, Closure function)
- Matrix apply(int columnNumber, Closure function)
- Matrix apply(String columnName, Closure function)

The various putAt methods can be used with the shorthand square bracket notation in Groovy.


## Using data in calculations
- List<?> withColumns(List<String> colNames, Closure operation)
- List<?> withColumns(Number[] colIndices, Closure operation)
- List<?> withColumns(int[] colIndices, Closure operation)

Stat methods
- static Matrix countBy(Matrix table, String groupBy)
- static List<Number> sum(Matrix matrix, String... columnNames)
- static List<Number> sum(Matrix matrix, IntRange columnIndices)
- static List<Number> sum(Matrix matrix, List<Integer> columnIndices)
- static Matrix sumBy(Matrix table, String sumColumn, String groupBy)
- static Matrix meanBy(Matrix table, String meanColumn, String groupBy, int scale = 9)
- static Matrix medianBy(Matrix table, String medianColumn, String groupBy)
- static Matrix funBy(Matrix table, String columnName, String groupBy, Closure fun, Class<?> columnType)
- static List<BigDecimal> means(Matrix table, List<String> colNames)
- static BigDecimal mean(Matrix table, String colName)
- static List<BigDecimal> medians(Matrix table, String colName)
- static List<BigDecimal> medians(Matrix table, List<String> colNames)
- static <T extends Comparable> List<T> min(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false)
- static <T extends Comparable> List<T> max(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false)
- static <T extends Number> List<T> sd(Matrix table, List<String> columnNames, boolean isBiasCorrected = true)
- static <T extends Number> T sd(Matrix table, String columnName, boolean isBiasCorrected = true)
- static Matrix frequency(Matrix table, String columnName)
- static Matrix frequency(Matrix table, int columnIndex)
- static Matrix frequency(Matrix table, String groupName, String columnName, boolean includeColumnNameCategory = true)

## Restructuring data
- Matrix orderBy(List<String> columnNames)
- Matrix orderBy(String columnName, Boolean descending = Boolean.FALSE)
- Matrix orderBy(LinkedHashMap<String, Boolean> columnsAndDirection)
- Matrix orderBy(Comparator comparator)
- Matrix moveColumn(String columnName, int index)
- Matrix transpose(List<String> header, List<Class> types, boolean includeHeaderAsRow = false)
- Matrix transpose(boolean includeHeaderAsRow = false)
- Matrix transpose(List<String> header, boolean includeHeaderAsRow = false)
- Matrix transpose(String columnNameAsHeader, List<Class> types,  boolean includeHeaderAsRow = false)
- Matrix transpose(String columnNameAsHeader,  boolean includeHeaderAsRow = false)

## Transforming data
- Map<?, Matrix> split(String columnName)
- Grid<Object> grid()
- Matrix clone()

## Joining (merging) Matrices 
- static Matrix merge(Matrix x, Matrix y, Map<String, String> by, boolean all = false)

## Comparing data
- boolean equals(Object o, boolean ignoreColumnNames = false, boolean ignoreName = false, boolean ignoreTypes = true)
- String diff(Matrix other, boolean forceRowComparing = false)

---
[Back to index](cookbook.md)  |  [Next (Matrix Stats)](matrix-stats.md)