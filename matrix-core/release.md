# Matrix Core Release History

### 3.4.1, 2025-07-18
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-core/3.4.1/matrix-core-3.4.1.jar)
- ValueConverter
  - return null value when converting a blank string to LocalDate or LocalDateTime
  - Return null value if converting an empty string to a BigInteger
- Overload ListConverter.convert with an option for passing a Locale 

### 3.4.0, 2025-07-09
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-core/3.4.0/matrix-core-3.4.0.jar)
- Add Stat.groupBy() to group by column(s)
- Improve robustness of mapList in Builder (default to Object for columnType if value is null)
- Override asType for Row so it supports casting to Map, List, Set and String
- Override asType for Matrix so it supports casting to Map, List and Grid
- Add support for setting the locale for ValueConverter date methods.
- In Stat.str scan for datatype if type is null
- Enhance robustness of ValueConverter -> toLocalDate

### 3.3.0, 2025-05-25
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-core/3.3.0/matrix-core-3.3.0.jar)
- Add Matrix.getAt(List) to get statically compiled groovy to work using short notations
- Add Matrix.selectColumns(List<String> columnNames) behaving like the other selectColumns methods
- Add MatrixBuilder.data() method that takes a url in String format
- Add versions of toHtml and toMarkdown that takes the number of rows to show
- Add implement Cloneable to Matrix
- Add MatrixBuilder.addRow() to add a row to the matrix

### 3.2.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-core/3.2.0/matrix-core-3.2.0.jar)
- Add a static builder create methods to Matrix for name, columns and types
- Change the Matrix constructor to use a list instead of Columns 
- Make MatrixBuilder constructors package scoped
- Override unique() in Column to not mutate it. 
- Add common stats methods to Column as short hands for using Stat

### 3.1.0, 2025-03-26
- Add support for arbitrary metaData to a Matrix
- add conversion from minus and hyphen to the correct negation symbol when supplying a formatter to the conversion
- -add List<LocalDateTime> toLocalDateTimes(List<String> dates) to ListConverter
- fix LocalDateTime asLocalDateTime when given a LocalDate

### 3.0.0, 2025-03-12
- Require JDK 21
- override getAt for Column to allow type conversion
- override minus for row to mean remove a column and return a new list
- add Stat.iqr: inter quartile range
- add ValueConverter for Number
- add Number -> YearMonth conversion to ValueConverter
- ValueConverter.asString() now returns null when converting a null instead of 'null'
- add null value handling/assignment to all ValueConverter methods

### 2.3.1, 2025-02-21
- Don't strip out non-numeric characters when converting from String to BigDecimal, Double and Float to support scientific notation e.g. '7.594000000032963e-05'

### 2.3.0, 2025-02-16
- Add pivot (rows to columns) and unPivot (columns to rows) methods to a Matrix
- Add Matrix.cloneEmpty() to make a copy without data content.
- Remove the duplicate list for rows and types and rely solely on the column attributes (which fixed a bug in renameColumn in the process)
- Add subList(IntRange) to Column

### 2.2.0, 2025-01-06
- Change group name and package name from se.alipsa.groovy to se.alipsa.matrix

### 2.1.1, 2024-Dec-03
- Add Row.detach() to allow data manipulations independently of the Matrix parent.

### 2.1.0, 2024-Nov-26
- add MatrixBuilder.ginqResult() to create a Matrix from the result of a ginq query
- add Stat.summary(Column) to make it possible to get the summary of a single column
- Change signature of ListConverter to be a List instead of a List<Number> so it also applies to a list of Strings
- Add columnIndexFuzzy() to search for a column name that starts or ends with the name supplied
- Add Matrix.leftshift to support adding a List of columns
- Add Matrix.addColumns() for a List of indices or IntRange of a Matrix
- Deprecate Matrix removeEmptyColumns in favor of dropEmptyColumns() (we drop columns and remove rows)
- Fix bug in MatrixBuilder.rowList that had a circular reference
- add Matrix.typeName(Integer) to get the type simpleName
- add toHtml() to create a xhtml representation of the Matrix
- improve ValueConverter.asInteger to handle String decimals better and also handle boolean conversions
- add ValueConverter.asIntegerRound for nearest int value
- add Matrix findFirstRow(Integer columnIndex, Object value) overload
- Add MatrixBuilder.mapList to build from a list of Maps

### 2.0.0, 2024-10-31
note there are several (minor) api breaking changes due to extensive cleanup and consistency fixes
This release was mainly guided by a big port of an R based budget planning and reporting application 
to Groovy powered by Matrix (resulting in a code reduction with about 20% and an increased performance by
more than 300%).

- add constructor to create an empty Matrix with only name and the column names defined
- *Breaking change:* change Grid semantics so that getAt and putAt mean the same thing
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
- Overload ListConverter methods, align naming and parameter order. 
- Add Matrix.getAt for column with type conversion. 
- Add NumberFormat as optional argument to OdsImporter.importOdsSheets and ValueConverter.isNumeric()
- Add a Grid.add(int, List) and Matrix.addRow(int, List) to allow for inserting a row at the designated place. 
- Add ValueTwoArgClosure to enable more elegant Java code.
- Fix Matrix.content() and head() to pretty print the column names 
- add Matrix.removeRows()
- change columnCount to count number of columns instead of number of columnNames
- add size check to Matrix.create()
- add check for missing column in Matrix.dropColumns()
- Modify Matrix.addColumns to include all columns from the supplied matrix if no columns are specified
- Fix bug in Matrix.diff() and add check for number of columns
- *Breaking change:* Add a builder to Matrix and remove all static create methods and constructors as a result
- change Matrix.columnTypes() to Matrix.types() 
- add putAt for ranges (e.g. myMatrix[0..2] = otherMatrix[1..3])
- change putAt to allow for add operations i.e. it is now possible to add an element using e.g. myMatrix[1,2] = 'Foo'
  note that the element to add must be at the size of the column i.e. it is not possible to do `myMatrix[2,2] = 'Foo'`
  if there is only 1 row in the matrix.
- Add Stat.apply() for various column operations (i.e. do something with values from two columns and assign the result to a third column)
- Add Matrix.dropColumns(IntRange columnIndices)
- Add Matrix.convert() for IntRange and individual column index, add formatters to the convert method taking a column name
- Change Matrix.diff() formatting for easier reading
- Add Matrix.columns(IntRange range) to get the specified range of columns
- Change equals to compare values and add a parameter for allowed diff used when comparing numbers
- add lastRowIndex and LastColumnIndex as convenience methods for rowCount -1 and ColumnCount -1 respectively
- add getAt for a range of rows or a range of columns
- add a MatrixBuilder.data() method for when the data is a list of custom objects
- *Breaking change:* changed some Matrix methods (e.g. the convert, apply, removeRows and orderBy methods) to mutate to make things more consistent
- Add Matrix.moveRow()
- Add Matrix.subset() for an IntRange of rows
- Enable using ginq by overriding get and set Property on a Row
- add support in MatrixBuilder for building from an existing Matrix
- add option to set default value if null to matrix getAt methods
- add support for null substitution to Matrix.convert() methods by overloading them.
- add MatrixBuilder.rowList to create a Matrix from a List of Rows
- Matrix.findFirstRow() now returns a Row instead of a List<?>
- Add Stat sumRows, meanRows, medianRows and ensure that median calculations no longer depends on a 
  sorted list by always sorting it internally
- Override getProperty for Matrix allowing the column to be accessed by dot notation
- *Breaking change:* Rename Matrix.getName() to Matrix.getMatrixName, setName() -> setMatrixName, withName() -> withMatrixName()
  to not collide with the common column name "name". Hence, `myMatrix.name` now refers to the column 'name' (if any) and
  `myMatrix.matrixName` refers to the name ot the Matrix.
- add Matrix withColumn(String, Closure), withColumn(int columnIndex, Closure operation), withColumns(IntRange, Closure)
- add Matrix.selectColumns(IntRange range), Matrix.columnNames(IntRange)
- enable short notation for adding a row. Both myMatrix.columnName = [1,2,3] and myMatrix['columnName'] = [1,2,3] works
- *Breaking change:* change sublist from min, max of range to the entire range (max excludes the last value) to be 
  consistent with the way IntRanges are handled w.r.t. collections in Groovy
- bugfix for putAt when supplying null as value e.g. `myMatrix[0,2] = null` or `myMatrix[0,'columnName'] = null` 
- *Breaking change:* leftShift (short notation <<) now only refers to column operations so removed the 
  "add row" since that is already covered with plus and added a leftshift for a Matrix. Plus is no longer mutating as 
  this `m + [1,2,3]` does not look intuitive, instead you need to do `m = m + [1,2,3]` which is much easier to read. 
  Plus (+) operations pertains to rows, left shift (<<) refers to columns.
- Add a column type that extends ArrayList that can do arithmetic operations on individual elements
- Removed Generics <?> constraints for List and Class as it was not adding value, just made the code harder to read
- *Breaking change:* change methodName Matrix.selectRowIndices to Matrix.rowIndices
- add leftShift short notation to add a column to a Matrix
- cleanup Row by removing the getString, getInt and getObject methods
- add support for converting nulls to specified value in Row.getAt
- add Row.toMap() to get a Map<String, ?> representation of the row
- Matrix.columnNames(Class type) to get all column names matching the type (including subclasses)

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