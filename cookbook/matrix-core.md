# Matrix Core

The matrix-core is the heart of the matrix project. It
contains the Matrix and Grid classes as well as several utility classes to
do basic statistics (sum, mean, median, sd, variance, counts, frequency etc.) and to
convert data into various shapes and formats.

## Matrix creation

### Creating from code
There are two kinds of creation methods:
1. The constructors. They assume that the data you supply is in columnar format
2. The static create methods. They assume that the data you supply is in row based format

#### Constructor methods:
- Matrix(String name, List<String> headerList, List<List<?>> columns, List<Class<?>>... dataTypesOpt). Note: if no data types are given, they will be set to Object
- Matrix(String name, Map<String, List<String>> columns, List<Class<?>>... dataTypesOpt)
- Matrix(Map<String, List<?>> columns, List<Class<?>>... dataTypesOpt)
- Matrix(List<List<?>> columns). Note, columns will be named v1, v2 etc.


##### Parameters
- Name is the name of the Matrix e.g. if you have a matrix of cars, the matrix might be named 'car'
- HeaderList are the names of the columns
- columns are lists of each the actual columns
- dataTypesOpt are a List of the classes for the data in each column

Examples:
```groovy
import se.alipsa.groovy.matrix.Matrix
import static se.alipsa.groovy.matrix.ListConverter.*

// Creating based on lists of data
Matrix ed = new Matrix("ed",
                ['id', 'name', 'salary', 'start'], [
                    1..5,
                    ["Rick","Dan","Michelle","Ryan","Gary"],
                    [623.3,515.2,611.0,729.0,843.25],
                    toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
                ]
        )

// Creating based on a map of data, with types
Matrix empData = new Matrix('empData',
    [
        emp_id: 1..5,
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ],
    [int, String, Number, LocalDate]
)

// Creating based on a list of columns
def e = new Matrix([
    1..5,
    ["Rick","Dan","Michelle","Ryan","Gary"],
    [623.3,515.2,611.0,729.0,843.25],
    toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
])
```      

#### Static create methods
Creating based on a collection (List) of data
- static Matrix create(String name, List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt)
- static Matrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt)
- static Matrix create(String name, List<List<?>> rowList)
- static Matrix create(List<List<?>> rowList)

Creating based on Grid
- static Matrix create(String name, List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt)
- static Matrix create(List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt)
- static Matrix create(String name, Grid grid)
- static Matrix create(Grid grid)

Creating based on a CSV (note: for more advanced text file import, use the matrix-csv package)
- static Matrix create(String name, File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)

Creating based on a jdbc ResultSet
- static Matrix create(String name, ResultSet rs)
- static Matrix create(ResultSet rs)

##### Parameters
- Name is the name of the Matrix e.g. if you have a matrix of cars, the matrix might be named 'car'
- HeaderList are the names of the columns
- rowList are a list of lists of the observations
- dataTypesOpt are a List of the classes for the data in each column
- grid is a monotyped two dimensional list structure
- file is a pointer to a text file containing rows of observations
- delimiter is what separates variables in an observation
- stringQuote is the string surrounding each string type variable (value)
- firstRowAsHeader is boolean where true means 'use the first row to assign column names'
- rs is the jdbc resultset to use to create the Matrix

Examples:
```groovy
import se.alipsa.groovy.matrix.*
import static se.alipsa.groovy.matrix.ListConverter.*

// Create based on column names and rows
def table = Matrix.create(["v0", "v1", "v2"], [
    [1, 2, 3],
    [1.1, 1, 0.9],
    [null, 7, "2"]
])

// Create based on a list of rows
def employeeList = []
employees << ['John Doe', 21000, asLocalDate('2013-11-01'), asLocalDate('2020-01-10')]
employees << ['Peter Smith', 23400,	'2018-03-25',	'2020-04-12']
employees << ['Jane Doe', 26800, asLocalDate('2017-03-14'), asLocalDate('2020-10-02')]
def employees = Matrix.create(employeeList)

// Created based on a CSV:
def data = [
    ['place', 'firstname', 'lastname', 'team'],
    ['1', 'Lorena', 'Wiebes', 'Team DSM'],
    ['2', 'Marianne', 'Vos', 'Team Jumbo Visma'],
    ['3', 'Lotte', 'Kopecky', 'Team SD Worx']
]
def file = File.createTempFile('FemmesStage1Podium', '.csv')
file.text = data*.join(',').join('\n') // Write the CSV file
def femmesStage1Podium = Matrix.create(file)

// Create based on an URL
def plantGrowth = Matrix.create(
    getClass().getResource('/PlantGrowth.csv'),
    ',',
    '"',
)

import groovy.sql.Sql
// Create based on a resultSet
def project = null
Sql.withInstance(dbUrl, dbUser, dbPasswd, dbDriver) { sql ->
  sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.create(rs) }
}
```

## Getting data
A Matrix can be referenced using the shorthand square bracket syntax. If you use Java instead of groovy,
you can use the getAt() methods instead.

```groovy
import se.alipsa.groovy.matrix.Matrix

import javax.swing.JTable

// Given the following matrix
def myMatrix = new Matrix([
    id    : [1, 2, 3, 4],
    weight: [23.12, 19.98, 20.21, 24.10],
    height: [123, 128, 99, 113]],
    [int, BigDecimal, int]
)
// Getting an individual value by matrix[row, column]
assert 123 == myMatrix[0, 2]
assert 123 == myMatrix.get(0, 2)
assert 123 == myMatrix[0, 'height']


//Getting an entire column
assert [1, 2, 3, 4] == myMatrix[0]
assert [1, 2, 3, 4] == myMatrix.column(0)
assert [1, 2, 3, 4] == myMatrix['id']
assert [1, 2, 3, 4] == myMatrix.column('id')

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
  def weight = it[1]
  def height = it[2]
  return weight < 24 && height < 125
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
  singleVector.add(String.valueOf(row[1]))
  singleVector.add(String.valueOf(row[2]))
  doubleVector.add(singlevector)
}
swingTable = new JTable(doubleVector, ['weight', 'height'] as Vector)

// Search for the first row where the value equals that in the column
assert [2, 19.98, 128] == findFirstRow('id', 2)

// The Matrix selectColumns(String... columnNames) return a new matrix with only the selected columns
def hightAndWeight = myMatrix.selectColumns('height', 'weight')
// The hightAndWeight Matrix will look like this:
// height	weight
// 123	  23.12
// 128	  19.98
//  99    20.21
// 113	  24.10

List<Row> rows(List<Integer> index)
List<List<?>> columns(Integer[] indices)
List<List<?>> columns(List<String> columnNames)
List<List<?>> columns()
int columnIndex(String columnName)
List<Integer> columnIndexes(List<String> columnNames)
```

## Displaying data
String content(boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n')
String toMarkdown(Map<String, String> attr = [:])
String toMarkdown(List<String> alignment, Map<String, String> attr = [:])
String toString()

## Matrix meta data
int rowCount()
int columnCount()
Stat.str()
Stat.summary()
String getName()
void setName(String name)
Matrix withName(String name)
List<String> columnTypeNames()
Class<?> columnType(String columnName)
Class<?> columnType(int i)
List<Class<?>> columnTypes(List<String> columnNames)
List<Class<?>> columnTypes()
Matrix renameColumn(int columnIndex, String after)
Matrix renameColumn(String before, String after)
String columnName(int index)
void columnNames(List<String> names)
List<String> columnNames()

## Adding Data
Matrix addRow(List<?> row)
Matrix addRows(List<List<?>> rows)
Matrix addColumn(String name, type = Object, List<?> column)
Matrix addColumn(String name, type = Object, Integer index, List<?> column)
Matrix addColumns(Matrix table, String... columns)
Matrix addColumns(List<String> names, List<List<?>> columns, List<Class<?>> types)

## Removing data
Matrix dropColumns(String... columnNames)
Matrix dropColumnsExcept(String... columnNames)
Matrix removeEmptyRows()

## updating data
void putAt(Integer column, List<?> values)
void putAt(String columnName, List<?> values)
void putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
void putAt(List where, List<?> column)
void putAt(Number rowIndex, Number colIndex, Object value)
void putAt(List<Number> where, Object value)

## Manipulating data
Matrix convert(int columnNumber, Class<?> type, Closure converter)
Matrix convert(Converter[] converters)
Matrix convert(String columnName, Class<?> type, Closure converter)
Matrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)
Matrix convert(List<Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)
void replaceColumn(String columnName, Class<?> type = Object, List<?> values)
Matrix apply(int columnNumber, Closure criteria, Closure function)
Matrix apply(String columnName, Closure criteria, Closure function)
Matrix apply(int columnNumber, List<Integer> rows, Closure function)
Matrix apply(String columnName, List<Integer> rows, Closure function)
Matrix apply(int columnNumber, Closure function)
Matrix apply(String columnName, Closure function)

## Using data in calculations
List<?> withColumns(List<String> colNames, Closure operation)
List<?> withColumns(Number[] colIndices, Closure operation)
List<?> withColumns(int[] colIndices, Closure operation)

## Restructuring data
Matrix orderBy(List<String> columnNames)
Matrix orderBy(String columnName, Boolean descending = Boolean.FALSE)
Matrix orderBy(LinkedHashMap<String, Boolean> columnsAndDirection)
Matrix orderBy(Comparator comparator)

## Transforming data
Map<?, Matrix> split(String columnName)
Grid<Object> grid()
Matrix clone()
Matrix transpose(List<String> header, List<Class> types, boolean includeHeaderAsRow = false)
Matrix transpose(boolean includeHeaderAsRow = false)
Matrix transpose(List<String> header, boolean includeHeaderAsRow = false)
Matrix transpose(String columnNameAsHeader, List<Class> types,  boolean includeHeaderAsRow = false)
Matrix transpose(String columnNameAsHeader,  boolean includeHeaderAsRow = false)

## Comparing data
boolean equals(Object o, boolean ignoreColumnNames = false, boolean ignoreName = false, boolean ignoreTypes = true)
String diff(Matrix other, boolean forceRowComparing = false)

---
[Back to index](cookbook.md)  |  [Next (Matrix Stats)](matrix-stats.md)