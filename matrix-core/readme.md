[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.groovy/matrix-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.groovy/matrix-core)
[![javadoc](https://javadoc.io/badge2/se.alipsa.groovy/matrix-core/javadoc.svg)](https://javadoc.io/doc/se.alipsa.groovy/matrix-core)
# Matrix
This is a Groovy library to make it easy to work with
a matrix i.e. a List<List<?>> typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`

The matrix core is focused on providing the data structure and data manipulation functionality (through the Matrix and 
Grid classes) that all the other matrix libraries use.

## Setup
Matrix should work with any 4.x version of groovy, and probably older versions as well. Binary builds can be downloaded 
from the [Matrix project release page](https://github.com/Alipsa/matrix/releases) but if you use a build system that 
handles dependencies via maven central (gradle, maven ivy etc.) you can do the following for Gradle
```groovy
implementation 'se.alipsa.groovy:matrix-core:2.1.0'
```
...and the following for maven
```xml
<dependencies>
    <dependency>
        <groupId>se.alipsa.groovy</groupId>
        <artifactId>matrix-core</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

The jvm should be JDK 17 or higher. If using the matrix library from Java, you need to add a dependency for 
the groovy core library as well e.g:

```xml
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>4.0.24</version>
</dependency>
```

The only difference when using it from Java is that some of the shorthand methods does not work e.g.
instead of doing `myMatrix[2, 'id']` you will need to use the underlying method instead i.e. `myMatrix.getAt(2, 'id')`

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
        "reviewPeriod": ListConverter.toYearMonths(['2020-01', '2019-04', '2018-10'])
]
def table = Matrix.builder().data(employees).build()
// There are several ways to access the data, below are some examples how to get single values:
assert table.salary[0] == 21000 // get index 0 from the column "salary"
assert table[0, 1] == 21000 // get row 0, column 1
assert table[0, 'salary'] == 21000 // get row index 0 from the column name salary
assert table['salary'][0] == 21000 // get the column and the first index
assert table.row(0).salary == 21000 // get the first row and then the salary column
assert table.row(0)[1] == 21000 // get the first row and then the second column
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
// Now we can do stuff with the project Matrix, e.g. pretty print the data:
println(project.content())
```

### Creating from a csv file:
```groovy
import se.alipsa.groovy.matrix.Matrix
def table = Matrix.builder().data(new File('/some/path/foo.csv'), ';').build()
```

Data can be referenced using [] notation e.g. to get the content of the 
3:rd row and 2:nd column you do table[3,2] or table[3,'price'], or even table.price[3]. 
If you pass only one argument, you get the column e.g. List<?> priceColumn = table["price"],
or if you prefer: List<?> priceColumn = table.price

### General inspection

#### head and tail - a short snippet of a Matrix
```groovy
import se.alipsa.groovy.matrix.*

def table = Matrix.builder().columns([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
    ])
    .types(Integer, String, String)
    .build()
println("Head\n${table.head(2, false)}")
println("Tail\n${table.tail(2, false)}")
```
Will print
```
Head
1	Lorena	  2021-12-01
2	Marianne  2022-07-10

Tail
2	Marianne  2022-07-10
3	Lotte	  2023-05-27
```

#### str - structure
```groovy
import se.alipsa.groovy.matrix.*
import java.time.*

def empData = Matrix.builder().data(
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
Matrix (5 observations of 4 variables)
--------------------------------------
emp_id: [Integer, 1, 2, 3, 4]
emp_name: [String, Rick, Dan, Michelle, Ryan]
salary: [Number, 623.3, 515.2, 611.0, 729.0]
start_date: [LocalDate, 2012-01-01, 2013-09-23, 2014-11-15, 2014-05-11]
```
Note that even though it is _possible_ to define a column as a primitive data type (int in this example), all 
primitives will be converted to their wrapper type (Integer in this example). This happens on Matrix creation 
and has nothing to do with the str() method.

#### Summary

```groovy
import se.alipsa.groovy.matrix.*

import static se.alipsa.groovy.matrix.Stat.*

def table = Matrix.builder().data([
    v0: [0.3, 2, 3],
    v1: [1.1, 1, 0.9],
    v2: [null, 'Foo', "Foo"]
  ])
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
def table = Matrix.builder()
    .data([
        'place': ['1', '2', '3'], 
        'firstname': ['Lorena', 'Marianne', 'Lotte'], 
        'start': ['2021-12-01', '2022-07-10', '2023-05-27']
    ])
    .types([String]*3)
    .build()
println(table.typeNames())

// Convert the place column to Integer and the start column to LocalDate
def table2 = table.convert([place: Integer, start: LocalDate])
println(table2.typeNames())
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
` Matrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null)`
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
import se.alipsa.groovy.matrix.Matrix
import static org.junit.jupiter.api.Assertions.*

def table = Matrix.builder().data([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']])
  .types([int, String, String])
  .build()
// We can use the groovy method findIndexValues on a column to select the rows we want
def rows = table.rows(table['place'].findIndexValues { it > 1 })
assertEquals(2, rows.size())

// ...But the same thing can be done using the subset method
def subSet = table.subset('place', { it > 1 })
// grid() returns the data content (no header) of the Matrix
assertIterableEquals(table.rows(1..2), subSet.grid())
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
Will print:
```
emp: 4 obs * 3 variables 
place	firstname	start     
    1	Lorena   	2021-12-11
    2	Marianne 	2022-07-20
    3	Lotte    	2023-06-06
 null	Chris    	2023-01-20
```
Note that it is possible to change the datatype of the column into something else when doing apply. The apply method
will detect this change and change the datatype to the new one (if all columns are affected) or the nearest common
one if only a subset of rows is affected.

Stat also have an apply method that does not mutate any data. It is useful if you want to do 
something with two columns. e.g:

```groovy
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Stat
import static se.alipsa.groovy.matrix.ListConverter.*

def account = Matrix.builder('account')
    .columns([
        'id': ['1', '2', '3', '4'],
        'balance': [12323, 23400, 45932, 77200],
    ])
    .build()

def ir = Matrix.builder('interest rates')
    .columns([
        id: [1,2,3,4],
        interestRate: [0.034, 0.022, 0.019, 0.028]
    ])
    .build()

def accountAndInterest = account.clone().withMatrixName('accountAndInterest')
accountAndInterest['interestAmount', Double] = Stat.apply(account['balance'], ir['interestRate']) { b, r ->
  b * r
}
accountAndInterest.content()
```
which will print
```
accountAndInterest: 4 obs * 3 variables 
id	balance	interestAmount
1 	  12323	       418.982
2 	  23400	       514.800
3 	  45932	       872.708
4 	  77200	      2161.600
```

### Combining selectRows with apply
```groovy
import java.time.*
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
List<Integer> selection = table.rowIndices {
  // We use the column index to refer to a specific variable, 2 will be the 'start' column
  def date = it[2] as LocalDate
  return date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
}
// Index values 1,2 will match (row with index 0 is before jan 1 2022 so is not included)
assert [1,2] == selection

// Double each value in the foo column that matches the selection
def f = table.clone().apply("foo", selection, { it * 2})
assert 4 == f[1, 0]
assert 6 == f[2, 0]

// The same thing can be done in one go which is a bit more efficient
table.apply("foo", {
  def date = it[2] as LocalDate
  date == null ? false : date.isAfter(LocalDate.of(2022,1, 1))
}, {
  it * 2
}
)
assert 4 == table[1, 0]
assert 6 == table[2, 0]
```   

### Using Ginq
Groovy Integrated queries can be used on Matrix rows. 
You needs to add groovy-ginq as a dependency, and then you can do things like:

```groovy
import se.alipsa.groovy.matrix.Matrix

Matrix stock = Matrix.builder().data(
    name: ['Orange', 'Apple', 'Banana', 'Mango', 'Durian'],
    price: [11,6,4,29,32],
    stock: [2,3,1,10,9])

    .types(String, int, int)
    .build()

def expected = [['Mango', 29, 10], ['Orange', 11, 2], ['Apple', 6, 3], ['Banana', 4, 1]]

def result = GQ {
  from f in stock
  where f.price < 32
  orderby f.price in desc
  select f.name, f.price, f.stock
}

assert expected == result.toList()

// The same thing using matrix built in query capabilities:
def exp = stock.subset{it.price < 32}.orderBy('price', true)

// We can construct a Matrix from the query result
Matrix matrix2 = Matrix.builder().ginqResult(result).build()
assert exp == matrix2

// or if we want a bit more control of column names and types:
Matrix m2 = Matrix.builder()
    .rows(result.toList())
    .columnNames(stock) // copy the column names from the original matrix
    .types(stock) // copy the types from the original matrix
    .build()

assert exp == m2

```
See [using ginq](https://groovy-lang.org/using-ginq.html) for more info about ginq.

See [tests](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/MatrixTest.groovy) for more usage examples or
the [javadocs](https://javadoc.io/doc/se.alipsa.groovy/matrix/latest/index.html) for more info.

### Column Arithmetics
The columns in a matrix overrides the following basic mathematical operations to be on each element in the column
which differs from standard Groovy behavior on lists: plus (+), minus (-), multiply (*), 
divide (/), power (**). As a consequence you can do stuff like this:
```groovy
import se.alipsa.groovy.matrix.Matrix
Matrix m = Matrix.builder().columns(
    [id: [1,2,3,4],
     balance: [10000, 1000.23, 20122, 12.1]
]).types(int, double)
.build()

Matrix r = Matrix.builder().columns(
  [id: [1,2,3,4],
   ir: [0.041, 0.020, 0.035, 0.5]
])
.types(int, double)
.build()


def interest = [10000*0.041, 1000.23*0.020, 20122*0.035, 12.1*0.5]
assert interest == m.balance * r.ir

assert 0.0366259560 == (m.balance * r.ir).sum() / m.balance.sum()

// or we can multiply all items in a column like this
assert [10000*0.05, 1000.23*0.05, 20122*0.05, 12.1*0.05] == m.balance * 0.05
```

### Using Matrix from another JVM language
If you are using the Matrix library from another JVM language, you cannot use the 
short notation for creating and referring to lists and maps:

```groovy
// Instead of
myMatrix[1,2] = 34
// you must use the following in java
myMatrix.putAt(1,2, 34);

// Instead of
def myGroovyVar = myMatrix[1,2]
// you must use the following in java
var myJavaVar = myMatrix.getAt(1,2);
```
There are some utility classes making Matrix creation much less painful in java:

```groovy
// In groovy you can do
def gEmpData = Matrix.builder().data(
      emp_id: 1..5,
      emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
      salary: [623.3,515.2,611.0,729.0,843.25],
      start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ).types(int, String, Number, LocalDate)
    .build()

// ... in java you can take advantage of the Column and CollectionUtils  
// to do something almost as simple:
import se.alipsa.groovy.matrix.util.Columns;
import static se.alipsa.groovy.matrix.util.CollectionUtils.*;

var jEmpData = Matrix.builder().data( new Columns()
    .add("emp_id", r(1,5))
    .add("emp_name", "Rick","Dan","Michelle","Ryan","Gary")
    .add("salary", 623.3,515.2,611.0,729.0,843.25)
    .add("start_date", toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")))
    .types(int.class, String.class, Number.class, LocalDate.class)
    .build();
```
Note that some methods require a closure as a parameter. You need to rewrite that somewhat in java. E.g:
```groovy
// The following groovy code
import se.alipsa.groovy.matrix.Matrix
import java.time.LocalDate
import static se.alipsa.groovy.matrix.ListConverter.*

def data = [
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]
def table = Matrix.builder().data(data).types(int, String, LocalDate).build()
def selection = table.rowIndices {
  it[2].isAfter(LocalDate.of(2022,1, 1))
}
assert [1,2] == selection

// ...will looks like this in Java:
import se.alipsa.groovy.matrix.Matrix;
import java.time.LocalDate;
import se.alipsa.groovy.matrix.util.*;
import static se.alipsa.groovy.matrix.util.CollectionUtils.*;
import static se.alipsa.groovy.matrix.ListConverter.*;
import static org.junit.jupiter.api.Assertions.*

var dat = new Columns(
    m("place", 1, 2, 3),
    m("firstname", "Lorena", "Marianne", "Lotte"),
    m("start", toLocalDates("2021-12-01", "2022-07-10", "2023-05-27"))
);
var tabl = Matrix.builder().data(dat).types(int.class, String.class, LocalDate.class).build();
var select = tabl.rowIndices(new RowCriteriaClosure(it -> 
    it.getAt(2, LocalDate.class).isAfter(LocalDate.of(2022,1, 1))
)
);
assertIterableEquals(c(1,2), select);
```
See test.alipsa.matrix.MatrixJavaTest for more examples (the logic is identical to the
groovy version MatrixTest).

## Grid
A Grid is a uniformly typed table
The grid class contains some static function to operate on a 2d list (a [][] structure or List<List<T>>).
- _convert_ converts one column type to another type
- _clone_ creates a deep copy of the matrix
- _transpose_ "rotates" the matrix 90 degrees
- _isValid_ checks if it is a proper matrix or not

a Grid can be created by supplying a list of rows to the constructor e.g.
```groovy
import se.alipsa.groovy.matrix.*
Grid foo = [
    [12.0, 3.0, Math.PI],
    ["1.9", 2, 3],
    ["4.3", 2, 3]
] as Grid // the as Grid is technically not needed but makes some IDE's happy (e.g. Intellij)

// But a Typed way is usually clearer
Grid<Number> bar = new Grid<>([
    [12.0, 3.0, Math.PI],
    [1.9, 2, 3],
    [4.3, 2, 3]
])

Stat.means(bar)
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
