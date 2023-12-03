# Release history

## 1.2.2, in progress
- add putAt for a whole column for both Grid and Matrix enabling expressions like `myMatrix['id'] = [10, 11, 12, 13, 14]`
- Remove the create methods using a map as it assumes columnar data which goes against the basic idea of
using contructors for column data and create methods for row based data.
- Rename Matrix.selectRows() to selectRowIndices()

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