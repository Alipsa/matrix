# Matrix

This is a Groovy library to make it easy to work with
a matrix i.e. a List<List<?>> typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`

Methods are static making is simple to use in Groovy scripts

## TableMatrix
A TableMatrix is an immutable Matrix with a header and where each column type is defined.
In some ways you can think of it as an in memory ResultSet.

A table Matrix is created using one of the static create methods in TableMatrix. 

### Creating from groovy code:
```groovy
import static se.alipsa.groovy.matrix.ListConverter.*

def employees = [
        "employee": ['John Doe','Peter Smith','Jane Doe'],
        "salary": [21000, 23400, 26800],
        "startDate": toLocalDates('2013-11-01','2018-03-25','2017-03-14'),
        "endDate": toLocalDates('2020-01-10', '2020-04-12', '2020-10-06')
]
def table = TableMatrix.create(employees)
```        
### Creating from a result set:

```groovy
@Grab('com.h2database:h2:2.1.214')

import se.alipsa.groovy.matrix.TableMatrix
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

// Create a TableMatrix from the PROJECT table
SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
  sql.query('SELECT * FROM PROJECT') { rs -> project = TableMatrix.create(rs) }
}
// Now we can do stuff with the project TableMatrix, e.g.
println(project.content())
```

### Creating from a csv file:
```groovy
import se.alipsa.groovy.matrix.TableMatrix
def table = TableMatrix.create(new File('/some/path/foo.csv'), ';')
```

Data can be reference using []
notation e.g. to get the content of the 3:rd row and 2:nd column you do table[3,2]. If you pass is only one argument,
you get the column e.g. List<?> priceColumn = table["price"]

### General inspection

#### head and tail - a short snippet of a TableMatrix
```groovy
import se.alipsa.groovy.matrix.*

def table = TableMatrix.create([
    'place': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
],
    [int, String, String]
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
import java.time.LocalDate
import se.alipsa.groovy.matrix.*

import static se.alipsa.groovy.matrix.ListConverter.*


def empData = TableMatrix.create(
    emp_id: 1..5,
    emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
    salary: [623.3,515.2,611.0,729.0,843.25],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
    [int, String, Number, LocalDate]
)
struct = Stat.str(empData)
struct.each {
  println it
}
```
will print 
```
TableMatrix=[5 observations of 4 variables]
emp_id=[int, 1, 2, 3, 4]
emp_name=[String, Rick, Dan, Michelle, Ryan]
salary=[Number, 623.3, 515.2, 611.0, 729.0]
start_date=[LocalDate, 2012-01-01, 2013-09-23, 2014-11-15, 2014-05-11]
```

#### Summary

```groovy
import se.alipsa.groovy.matrix.*

import static se.alipsa.groovy.matrix.Stat.*

def table = TableMatrix.create([
    v0: [0.3, 2, 3],
    v1: [1.1, 1, 0.9],
    v2: [null, 'Foo', "Foo"]
], [Number, double, String])
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
Type:	double
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
import se.alipsa.groovy.matrix.TableMatrix
import java.time.LocalDate

// Given a table of strings
def table = TableMatrix.create([
    'place': ['1', '2', '3'],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ['2021-12-01', '2022-07-10', '2023-05-27']
], [String]*3
)
println(table.getColumnTypeNames())
// Convert the place column to int and the start column to localdates
def table2 = table.convert([place: int, start: LocalDate])
println(table2.getColumnTypeNames())
```
which will print
```
[String, String, String]
[int, String, LocalDate]
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
// Get matrix returns the data content (no header) of the TableMatrix
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
def table = TableMatrix
    .create(data)
    .convert(place: int, start: LocalDate)

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

### Combining selectRows with apply
```groovy
def data = [
    'foo': [1, 2, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]
def table = TableMatrix.create(data, [int, String, LocalDate])
// select the observations where start is later than the jan 1 2022
def selection = table.selectRows {
  // We use the column index to refer to a specific variable, 2 will be the start column
  def date = it[2] as LocalDate
  return date.isAfter(LocalDate.of(2022,1, 1))
}
// Index values 1,2 will match (row with index is before jan 1 2022 so is not included)
assertArrayEquals([1,2].toArray(), selection.toArray())
// Double each value in the foo column that matches the selection
def foo = table.apply("foo", selection, { it * 2})
assertEquals(4, foo[0, 0])
assertEquals(6, foo[1, 0])
```        

## Matrix
The matrix class contains some static function to operate on a numerical Matrix (a [][] structure or List<List<?>>).
- _convert_ converts one column type to another numeric type
- _clone_ creates a deep copy of the matrix
- _transpose_ "rotates" the matrix 90 degrees
- _isValid_ checks if it is a proper matrix or not

## Stat
Stat contains basic statistical operations such as sum, mean, median, frequency, sd (standard deviation), variance, 
quartiles. See [StatTest](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/StatTest.groovy)
for some examples.

## Correlation
Correlation can do the most common types of correlation calculations (Pearson, Spearman, and Kendall). See
[CorrelationTest](https://github.com/Alipsa/matrix/blob/main/src/test/groovy/CorrelationTest.groovy) for some examples.
