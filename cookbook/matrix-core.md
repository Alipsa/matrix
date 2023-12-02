# Matrix Core

The matrix-core is the heart of the matrix project. It
contains the Matrix and Grid classes as well as several utility classes to
do basic statistics (sum, mean, median, sd, variance, counts, frequency etc.) and to
convert data into various shapes and formats.

## Matrix creation

### Creating from code
There are two kinds of creation methods:
1. The constructors. They assume that the data you supply is in columnar format
2. THe static create methods. They assume that the data you supply is in row based format

#### Constructor methods:
- Matrix(String name, List<String> headerList, List<List<?>> columns, List<Class<?>>... dataTypesOpt). Note: if no data types are given, they will be set to Object
- Matrix(String name, Map<String, List<String>> columns, List<Class<?>>... dataTypesOpt)
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
Creating based on a collection (List or map) of data
- static Matrix create(String name, List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt)
- static Matrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt)
- static Matrix create(String name, List<List<?>> rowList)
- static Matrix create(List<List<?>> rowList)
- static Matrix create(String name, Map<String, List<?>> map, List<Class<?>>... dataTypesOpt)
- static Matrix create(Map<String, List<?>> map, List<Class<?>>... dataTypesOpt)
Creating based on Grid
- static Matrix create(String name, List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt)
- static Matrix create(List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt)
- static Matrix create(String name, Grid grid)
- static Matrix create(Grid grid)
Creating based on a CSV
- static Matrix create(String name, File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
- static Matrix create(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true)
Creating based on a jdbc ResultSet
- static Matrix create(String name, ResultSet rs)
- static Matrix create(ResultSet rs)


---
[Back to index](cookbook.md)  |  [Next (Matrix Stats)](matrix-stats.md)