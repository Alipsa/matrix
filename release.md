# Release history

1.1.0, 2023-04-20
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

  
1.0.1, 2023-mar-19
- Upgrade to groovy 4.0.10
- Add apply methods to Table matrix
- Enhance docs

1.0, 2023-mar-18
- Initial release