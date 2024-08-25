# Matrix core Release history

### 1.2.5, In progress
- add constructor to create an empty Matrix with only name and the column names defined
- change Grid semantics so that getAt and putAt mean the same thing
  it was so that getAt (X = grid[0]) gets the row and putAt (grid[0] = X) puts the column
  but now both refers to the row.
- Added replaceColumn method to compensate for the change in putAt semantics for a Grid
- add dimensions() method to Grid and Matrix returning a Map of number of observations and variables
- add typed get at (e.g: myMatrix[0, 1, String]) using the ValueConverter to convert
- add ValueConverter for Long
- add Matrix.removeEmptyColumns
- add type conversion to Row (using the GroovyExtension mechanism to get the short notation to work as well)
- Fix Grid declaration (should implement Iterable<List<T>> not Iterable<T>)
- Changed Matrix.apply() and Matrix.subset() to apply to Row instead of a generic list
- Fix index adjustment for Matrix.dropColumns()
- safeguard Matrix maxContentLength from NPE when the column name is null
- Add several Java classes to make is much easier (and more Groovy like) to use a
  Matrix from Java code. See MatrixJavaTest for details

### 1.2.4, 2024-07-04
- add plus override to Matrix allowing for easy ways to append a row or append all rows from another matrix
- add constructor for an empty Matrix

### 1.2.3, 2024-03-17
- Add padding to head, tail, content to make the output much more readable
- add a column max length limit to head, tail and content
- add replace methods for simple data cleaning
- change to typed getAt methods in Row and Matrix
- add a getAt using the column name

## 1.2.2, 2023-12-22
- add putAt for a whole column for both Grid and Matrix enabling expressions like `myMatrix['id'] = [10, 11, 12, 13, 14]`
- Remove the create methods using a map as it assumes columnar data which goes against the basic idea of
using constructors for column data and create methods for row based data.
- Rename Matrix.selectRows() to selectRowIndices()
- add some convenience methods to Row, make it clear that iterations is over Row objects
- add a convert method to easily convert the entire matrix to a singular type
- add moveColumn to Matrix

## 1.2.1, 2023-11-19
- No changes

## 1.2.0, 2023-11-11
- add additional attributes parameter to Matrix.toMarkdown
- add a group frequency method to Stat similar to the R table function
- Remove rows from the matrix and use only a list of columns. 
- Regard a Matrix to be mutable to allow for native Groovy syntax constructs. 
- Add compact form syntax to a Matrix allowing easy adding and altering of data. 
- Add withColumns, replaceColumn and clone() methods
- Make Grid mono typed
- MapList enhancements

## 1.1.2, 2023-08-05
- allow min and max to work on all comparables
- add ignoreNonNumerics param to min functions
- add List<Double> toDoubles(...) and double[] toDoubleArray(...) to list converter

## 1.1.1, 2023-05-18
- add selectColumns
- Fix csv reading to make the rows List of String instead of String[]
- Add short syntax notation to grid
- add toMarkdown() to Matrix 
- add withName (setName + return this) for chained calls
- trim values in csv file import
- rename sort to orderBy and deprecate sort methods
- propagate table name to various methods returning a new matrix
- add some groovy docs

## 1.1.0, 2023-04-20
- Add null handling
- Stat changes
  - Add sumBy, countBy, medianBy, and meanBy
- ListConverter changes
  - Add toLocalDateTimes
  - Add toYearMonth
- Matrix changes
  - renamed it to Grid
- TableMatrix changes
  - Renamed it to Matrix
  - add columnIndex(columnName)
  - add rows() to replace matrix()
  - add columns()
  - add addColumn(s) and addRows(s)
  - detect and modify datatype change to something appropriate in apply() methods 
  (nearest common if not all rows are affected, the new datatype if all rows are affected) 
  - add sort
  - add split (eg to use in Stat.sumBy and Stat.countBy)
  - improve file import adding params for string quote and first row as header 
- ValueConverter additions
  - add toBoolean 
  - improve toLocalDate
  - improve toLocalDateTime
  - rename all 'to' methods to 'as'

  
## 1.0.1, 2023-mar-19
- Upgrade to groovy 4.0.10
- Add apply methods to Table matrix
- Enhance docs

## 1.0, 2023-mar-18
- Initial release