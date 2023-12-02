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
import se.alipsa.groovy.matrix.Matrix

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
---
[Back to index](cookbook.md)  |  [Next (Matrix Stats)](matrix-stats.md)