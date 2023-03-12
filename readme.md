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
import static se.alipsa.matrix.ListConverter.*

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

import se.alipsa.matrix.TableMatrix
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
import se.alipsa.matrix.TableMatrix
def table = TableMatrix.create(new File('/some/path/foo.csv'), ';')
```

Data can be reference using []
notation e.g. to get the content of the 3:rd row and 2:nd column you do table[3,2]. If you pass is only one argument,
you get the column e.g. List<?> priceColumn = table["price"]

### Transforming data
```groovy
import se.alipsa.matrix.TableMatrix
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
// We ca use the groovy method findIndexValues on a column to select the rows we want
def rows = table.getRows(table['place'].findIndexValues { it > 1 })
assertEquals(2, rows.size())

// ...But the same thing can be done using the subset method
def subSet = table.subset('place', { it > 1 })
// Get matrix returns the data content (no header) of the TableMatrix
assertArrayEquals(table.getRows(1..2).toArray(), subSet.getMatrix().toArray())
```
## Matrix

## Stat
Stat contains basic statistical operations such as sum, mean, median, sd (standard deviation), variance
