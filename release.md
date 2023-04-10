# Release history

1.1.0, In progress
- Add null handling
- Stat changes
  - Add sumBy
  - Add countBy
- ListConverter changes
  - Add toLocalDateTimes
  - Add toYearMonth
- TableMatrix changes
  - add columnIndex(columnName)
  - add rows() to replace matrix()
  - add columns()
  - add addColumn(s) and addRows(s)
  - detect and modify datatype change to something appropriate in apply() methods 
  (nearest common if not all rows are affected, the new datatype if all rows are affected) 
  - add sort
  - add split (eg to use in Stat.sumBy and Stat.countBy)
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