# Matrix
[![javadoc](https://javadoc.io/badge2/se.alipsa.groovy/matrix/javadoc.svg)](https://javadoc.io/doc/se.alipsa.groovy/matrix)
This is a Groovy library to make it easy to work with
a matrix i.e. a List<List<?>> typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`

Methods are static making is simple to use in Groovy scripts

## Setup
Matrix should work with any 4.x version of groovy, and probably older versions as well. Binary builds can be downloaded 
from the [Matrix project release page](https://github.com/Alipsa/matrix/releases) but if you use a build system that 
handles dependencies via maven central (gradle, maven ivy etc.) you can do the following for Gradle
```groovy
implementation 'se.alipsa.groovy:matrix:1.1.0'
```
...and the following for maven
```xml
<dependency>
    <groupId>se.alipsa.groovy</groupId>
    <artifactId>matrix</artifactId>
    <version>1.1.0</version>
</dependency>
```

The jvm should be JDK 17 or higher.

## Matrix
A Matrix is an immutable Grid with a header and where each column type is defined.
In some ways you can think of it as an in memory ResultSet.

A Matrix is created using one of the static create methods in Matrix. 

### Creating from groovy code:
```groovy
import se.alipsa.groovy.matrix.*

def employees = [
        "employee": ['John Doe','Peter Smith','Jane Doe'],
        "salary": [21000, 23400, 26800],
        "startDate": ListConverter.toLocalDates(['2013-11-01','2018-03-25','2017-03-14']),
        "reviewPeriod": ListConverter.toYearMonth(['2020-01', '2019-04', '2018-10'])
]
def table = Matrix.create(employees)
```        
### Creating from a result set:

```groovy
@Grab('com.h2database:h2:2.1.214')

import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.datautil.SqlUtil

dbDriver = "org.h2.Driver"
dbUrl = "jdbc:h2:file:" + System.getProperty("java.io.tmpdir") + "/testdb"
dbUser = "sa"
dbPasswd = "123"

// Create the table
SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
    sql.execute '''
        create table IF NOT EXISTS PROJECT  (
            id integer not null primary key,
            name varchar(50),
            url varchar(100)
        )
    '''
    // insert som data  
    sql.execute('delete from PROJECT')
    sql.execute 'insert into PROJECT (id, name, url) values (?, ?, ?)', [10, 'Groovy', 'http://groovy.codehaus.org']
    sql.execute 'insert into PROJECT (id, name, url) values (?, ?, ?)', [20, 'Alipsa', 'http://www.alipsa.se']

}

// Create a Matrix from the PROJECT table
SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
  sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.create(rs) }
}
// Now we can do stuff with the project TMatrix, e.g.
println(project.content())
```

### Creating from a csv file:
```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.create(new File('/some/path/foo.csv'), ';')
```

Data can be reference using []
notation e.g. to get the content of the 3:rd row and 2:nd column you do table[3,2]. If you pass only one argument,
you get the column e.g. List<?> priceColumn = table["price"]

### General inspection

#### head and tail - a short snippet of a Matrix
```groovy
import se.alipsa.groovy.matrix.*

def table = Matrix.create([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
],
    [Integer, String, String]
)
println("Head\n${table.head(2, false)}")
println("Tail\n${table.tail(2, false)}")
```
Will print
```
Head
1	Lorena	2021-12-01
2	Marianne	2022-07-10

Tail
2	Marianne	2022-07-10
3	Lotte	2023-05-27
```

#### str - structure
```groovy
import se.alipsa.groovy.matrix.*
import java.time.*

def empData = Matrix.create(
    emp_id: 1..5,
    emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
    salary: [623.3,515.2,611.0,729.0,843.25],
    start_date: ListConverter.toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
    [int, String, Number, LocalDate]
)
struct = Stat.str(empData)
struct.each {
  println it
}
```
will print 
```
Matrix=[5 observations of 4 variables]
emp_id=[Integer, 1, 2, 3, 4]
emp_name=[String, Rick, Dan, Michelle, Ryan]
salary=[Number, 623.3, 515.2, 611.0, 729.0]
start_date=[LocalDate, 2012-01-01, 2013-09-23, 2014-11-15, 2014-05-11]
```
Note that even though it is _possible_ to define a column as a primitive data type (int in this example), all 
primitives will be converted to their wrapper type (Integer in this example). This happens on Matrix creation 
and has nothing to do with the str() method.

#### Summary

```groovy
import se.alipsa.groovy.matrix.*

import static se.alipsa.groovy.matrix.Stat.*

def table = Matrix.create([
    v0: [0.3, 2, 3],
    v1: [1.1, 1, 0.9],
    v2: [null, 'Foo', "Foo"]
], [Number, Double, String])
def summary = summary(table)
println(summary)
```
Will print:
```
v0
--
Type:	Number
Min:	0.3
1st Q:	2
Median:	2
Mean:	1.7666666667
3rd Q:	3
Max:	3

v1
--
Type:	Double
Min:	0.9
1st Q:	1
Median:	1
Mean:	1.0
3rd Q:	1.1
Max:	1.1

v2
--
Type:	String
Number of unique values:	2
Most frequent:	Foo occurs 2 times (66.67%)
```

### Transforming data
```groovy
import se.alipsa.groovy.matrix.Matrix
import java.time.LocalDate

// Given a table of strings
def table = Matrix.create([
    'place': ['1', '2', '3'],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
], [String]*3
)
println(table.columnTypeNames())
// Convert the place column to int and the start column to localdates
def table2 = table.convert([place: Integer, start: LocalDate])
println(table2.columnTypeNames())
```
which will print
```
[String, String, String]
[Integer, String, LocalDate]
```

### Getting a subset of the table
```groovy
def table = create([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
], [int, String, String])
// We can use the groovy method findIndexValues on a column to select the rows we want
def rows = table.getRows(table['place'].findIndexValues { it > 1 })
assertEquals(2, rows.size())

// ...But the same thing can be done using the subset method
def subSet = table.subset('place', { it > 1 })
// Get matrix returns the data content (no header) of the Matrix
assertArrayEquals(table.getRows(1..2).toArray(), subSet.getMatrix().toArray())
```

## Performing calculations with apply
```groovy
import java.time.LocalDate
import se.alipsa.groovy.matrix.*

def data = [
    'place': ['1', '2', '3', ','],
    'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
]
def table = Matrix
    .create(data)
    .convert(place: Integer, start: LocalDate)

// Add 10 days to the start dates
def table2 = table.apply("start", { startDate ->
    startDate.plusDays(10)
})
println(table2.content())
```
Will print:
```
place	firstname	start
1	Lorena	2021-12-11
2	Marianne	2022-07-20
3	Lotte	2023-06-06
null	Chris	2023-01-20
```
Note that it is possible to change the datatype of the column into something else when doing apply. The apply method
will detect this change and change the datatype to the new one (if all columns are affected) or the nearest common
one if only a subset of rows is affected.

### Combining selectRows with apply
```groovy
def data = [
    'foo': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]
def table = Matrix.create(data, [Integer, String, LocalDate])
// select the observations where start is later than the jan 1 2022
def selection = table.selectRows {
  // We use the column index to refer to a specific variable, 2 will be the 'start' column
  def date = it[2] as LocalDate
  return date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
}
// Index values 1,2 will match (row with index 0 is before jan 1 2022 so is not included)
assertArrayEquals([1,2].toArray(), selection.toArray())
// Double each value in the foo column that matches the selection
def foo = table.apply("foo", selection, { it * 2})
assertEquals(4, foo[1, 0])
assertEquals(6, foo[2, 0])

// The same thing can be done in one go which is a bit more efficient
def bar = table.apply("foo", {
    def date = it[2] as LocalDate
    date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
  }, {
    it * 2
  }
)
assertEquals(4, bar[1, 0])
assertEquals(6, bar[2, 0])
```   

See [tests](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/MatrixTest.groovy) for more usage examples or
the [javadocs](https://javadoc.io/doc/se.alipsa.groovy/matrix/latest/index.html) for more info.



## Grid
The grid class contains some static function to operate on a 2d list (a [][] structure or List<List<?>>).
- _convert_ converts one column type to another numeric type
- _clone_ creates a deep copy of the matrix
- _transpose_ "rotates" the matrix 90 degrees
- _isValid_ checks if it is a proper matrix or not

a Grid can be created by supplying a list of rows to the constructor e.g.
```groovy
import se.alipsa.groovy.matrix.Grid
Grid foo = [
    [12.0, 3.0, Math.PI],
    ["1.9", 2, 3],
    ["4.3", 2, 3]
] as Grid // the as Grid is technically not needed but makes some IDE's happy (e.g. Intellij)
```
elements can be accessed using the simple square bracket notation grid[rowindex, columnIndex], e.g:
```groovy
foo[0, 1] = 3.23

assert 3.23 == foo[0,1]
```

## Stat
Stat contains basic statistical operations such as sum, mean, median, frequency, sd (standard deviation), variance, 
quartiles. See [StatTest](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/StatTest.groovy)
for some examples.

## Correlation
Correlation can do the most common types of correlation calculations (Pearson, Spearman, and Kendall). See
[CorrelationTest](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/CorrelationTest.groovy) for some examples.
