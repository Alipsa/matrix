# Matrix
This is a Groovy library (that also works in Java) to make it easy to work with
a matrix (tabular) data. Whenever you ḧave a structure like this
List<List<?>> (typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`) a Matrix or
a Grid can greatly enhance the experience of working with that data structure.

The Matrix project consist of the following modules:
1. _[matrix-core](https://github.com/Alipsa/matrix/blob/main/matrix-core/readme.md)_ The matrix-core is the heart of the matrix project. It
   contains the Matrix and Grid classes as well as several utility classes to
   do basic statistics (sum, mean, median, sd, variance, counts, frequency etc.) and to
   convert data into various shapes and formats
   See [tests](https://github.com/Alipsa/matrix/blob/main/matrix-core/src/test/groovy/MatrixTest.groovy) for more usage examples or
   the [javadocs](https://javadoc.io/doc/se.alipsa.groovy/matrix-core/latest/index.html) for more info.
2. _[matrix-stats](https://github.com/Alipsa/matrix/blob/main/matrix-stats/README.md)_ The stats library contains various statistical methods and tests
   (correlations, normalization, linear regression, t-test, etc.)
3. _[matrix-datasets](https://github.com/Alipsa/matrix/blob/main/matrix-datasets/README.md)_ contains some common datasets used in R and Python such as mtcars, iris, diamonds, plantgrowth, toothgrowth etc.
4. _[matrix-spreadsheet](https://github.com/Alipsa/matrix/blob/main/matrix-spreadsheet/README.md)_ provides ways to import and export between a Matrix and an Excel or OpenOffice Calc spreadsheet
5. _[matrix-csv](https://github.com/Alipsa/matrix/blob/main/matrix-csv/README.md)_ provides a more advanced way to import and export between a Matrix and a CSV file (matrix-core has basic support
   for doing this built in)
6. _[matrix-json](https://github.com/Alipsa/matrix/blob/main/matrix-json/README.md)_ provides ways to import and export between a Matrix and Json 
7. _[matrix-charts](https://github.com/Alipsa/matrix/blob/main/matrix-charts/README.md)_ allows you to create charts in various formats (file, javafx) based on Matrix data.
8. _[matrix-sql](https://github.com/Alipsa/matrix/blob/main/matrix-sql/readme.md)_ relational database interaction
9. _[matrix-tablesaw](https://github.com/Alipsa/matrix/blob/main/matrix-tablesaw/readme.md)_ interoperability between Matrix and the Tablesaw library

## Setup
Matrix should work with any 4.x version of groovy. Binary builds can be downloaded 
from the [Matrix project release page](https://github.com/Alipsa/matrix/releases) but if you use a build system that 
handles dependencies via maven central (gradle, maven ivy etc.) you can add your dependencies from there
. The group name is se.alipsa.groovy. An example for matrix-core is as follows for Gradle
```groovy
implementation 'se.alipsa.groovy:matrix-core:1.2.4'
```
...and the following for maven
```xml
<dependency>
    <groupId>se.alipsa.groovy</groupId>
    <artifactId>matrix-core</artifactId>
    <version>1.2.4</version>
</dependency>
```

The jvm should be JDK 17 or higher.

## Matrix
A Matrix is a grid with a header and where each column type is defined.
In some ways you can think of it as an in-memory ResultSet.

A Matrix is created using the builder. 

### Creating from groovy code:
```groovy
import se.alipsa.groovy.matrix.*

def employees = [
        "employee": ['John Doe','Peter Smith','Jane Doe'],
        "salary": [21000, 23400, 26800],
        "startDate": ListConverter.toLocalDates(['2013-11-01','2018-03-25','2017-03-14']),
        "reviewPeriod": ListConverter.toYearMonth(['2020-01', '2019-04', '2018-10'])
]
def table = Matrix.builder.data(employees).build()
```        
### Creating from a result set:

```groovy
@Grab('com.h2database:h2:2.3.232')

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
  sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.builder().data(rs).build() }
}
// Now we can do stuff with the project TMatrix, e.g.
println(project.content())
```

### Creating from a csv file:
```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder().data(new File('/some/path/foo.csv'), ';').build()
```

Data can be referenced using square bracket notation [] 
e.g. to get the content of the 3:rd row and 2:nd column you do table[3,2] or table[3, 'price']. 
If you pass only one argument, you get the column e.g. List<?> priceColumn = table['price']

### General inspection

#### head and tail - a short snippet of a Matrix
```groovy
import se.alipsa.groovy.matrix.*

def table = Matrix.builder().data(
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
  )
  .types(Integer, String, String)
  .build()
println("Head\n${table.head(2, false)}")
println("Tail\n${table.tail(2, false)}")
```
Will print
```
Head
1	Lorena	   2021-12-01
2	Marianne   2022-07-10

Tail
2	Marianne   2022-07-10
3	Lotte	   2023-05-27
```

#### str - structure
```groovy
import se.alipsa.groovy.matrix.*
import java.time.*

Matrix empData = Matrix.builder().data(
        emp_id: 1..5, 
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"], 
        salary: [623.3,515.2,611.0,729.0,843.25], 
        start_date: ListConverter.toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types(int, String, Number, LocalDate)
        .build()
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

def table = Matrix.builder().data(
    v0: [0.3, 2, 3],
    v1: [1.1, 1, 0.9],
    v2: [null, 'Foo', "Foo"])
    .types(Number, Double, String)
    .build()
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

there are several convert methods that can be used to transform data.

The basic idea is like this:

```groovy
import se.alipsa.groovy.matrix.Matrix
import java.time.LocalDate

// Given a table of strings
def table = Matrix.builder().data(
        'place': ['1', '2', '3'],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ['2021-12-01', '2022-07-10', '2023-05-27'])
        .types([String]*3)
        .build()
println(table.typeNames())
// Convert the place column to int and the start column to localdates
table.convert([place: Integer, start: LocalDate])
println(table.typeNames())
```
which will print
```
[String, String, String]
[Integer, String, LocalDate]
```

The convert methods are:
#### convert using a list of column types
`Matrix convert(List<Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)`
e.g:
```groovy
table.convert([Integer, String, LocalDate], DateTimeFormatter.ofPattern('yyyy-MM-dd'))
```
#### Convert using a map of column names and their type
`Matrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)`
This is the example shown in the basic idea above

#### Convert a specified column into the type using a closure to perform the conversion
`Matrix convert(String columnName, Class<?> type, Closure converter)`
This is used for more complex conversions where the data is more dirty. e.g:
```groovy
table.convert('place', Integer, {
            String val = String.valueOf(it).trim()
            if (val == 'null' || val == ',' || val.isBlank()) return null
            return Integer.valueOf(val)
        })
```
#### Convert using an array of Converters for each column you want to convert
`Matrix convert(Converter[] converters)`
a converter is a simple Groovy class containing the column name, 
the type (class) and a closure to convert each value, e.g:
```groovy
import se.alipsa.groovy.matrix.*
import java.time.LocalDate

table.convert([
    new Converter('place', Integer, {
      try {Integer.parseInt(it)} catch (NumberFormatException e) {null}
    }),
    new Converter('start', LocalDate, {
      LocalDate.parse(it)
    })
] as Converter[])
```
Note that the cast to an Array of Converter at the end which is needed as otherwise the 
compiler will think you want to call the convert(List<Class<?>>) method instead.

### Getting a subset of the table
```groovy
import se.alipsa.groovy.matrix.*
def table = Matrix.builder().data(
        'place': [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ['2021-12-01', '2022-07-10', '2023-05-27']
)
        .types(int, String, String)
        .build()
// We can use the groovy method findIndexValues on a column to select the rows we want
def rows = table.rows(table['place'].findIndexValues { it > 1 })
assert 2 == rows.size()

// ...But the same thing can be done using the subset method
def subSet = table.subset('place', { it > 1 })
// grid() returns the data content (no header) of the Matrix
assert table.rows(1..2)[0] == subSet.grid()[0]
assert table.rows(1..2)[1] == subSet.row(1) // or we specify the row directly from the subset 
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
def table = Matrix.builder()
        .data(data)
        .build()
        .convert(place: Integer, start: LocalDate)

// Add 10 days to the start dates
table.apply("start", { startDate ->
   startDate.plusDays(10)
})
println(table.content())
```
Will print:
```
null: 4 obs * 3 variables 
place	firstname	start     
    1	Lorena   	2021-12-01
    2	Marianne 	2022-07-10
    3	Lotte    	2023-05-27
 null	Chris    	2023-01-10
```
Note that it is possible to change the datatype of the column into something else when doing apply. The apply method
will detect this change and change the datatype to the new one (if all columns are affected) or the nearest common
one if only a subset of rows is affected.

### Combining selectRows with apply
```groovy
import java.time.LocalDate
import se.alipsa.groovy.matrix.*
import static se.alipsa.groovy.matrix.ListConverter.*

def data = [
        'foo': [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]
def table = Matrix.builder().data(data).types(Integer, String, LocalDate).build()
// select the observations where start is later than the jan 1 2022
def selection = table.selectRowIndices {
   def date = it['start']
   return date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
}

// Index values 1,2 will match (row with index 0 is before jan 1 2022 so is not included)
assert [1,2] == selection
// Double each value in the foo column that matches the selection
// as apply mutates the matrix, we clone it so the table is kept intact
def foo = table.clone().apply("foo", selection, { it * 2})
assert 4 == foo[1, 0]
assert 6 == foo[2, 0]
assert 2 == table[1, 0]
assert 3 == table[2, 0]

// The same thing can be done in one go which is a bit more efficient
table.apply("foo", {
   def date = it[2] as LocalDate
   date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
}, {
   it * 2
})
assert 4 == table[1, 0]
assert 6 == table[2, 0]
```

For more information see the [Cookbook](cookbook/cookbook.md)



