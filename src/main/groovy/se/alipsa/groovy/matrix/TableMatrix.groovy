package se.alipsa.groovy.matrix

import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

/**
 * This is essentially a [][] (List<List<?>>) but also has a header
 * and several convenience methods to work with it.
 */
class TableMatrix {

  private List<String> headerList
  private List<List<?>> rowList
  private List<Class<?>> columnTypes
  // Copy of the data i column format
  private List<List<Object>> columnList
  String name

  static TableMatrix create(String name, List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    def table = create(headerList, rowList, dataTypesOpt)
    table.name = name
    return table
  }

  static TableMatrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    TableMatrix table = new TableMatrix()
    table.headerList = Collections.unmodifiableList(headerList.collect())
    table.rowList = Collections.unmodifiableList(rowList.collect())
    if (dataTypesOpt.length > 0) {
      table.columnTypes = Collections.unmodifiableList(dataTypesOpt[0])
      if (headerList.size() != table.columnTypes.size()) {
        throw new IllegalArgumentException("Number of columns (${headerList.size()}) differs from number of datatypes provided (${table.columnTypes.size()})")
      }
    } else {
      table.columnTypes = Collections.unmodifiableList([Object.class] * headerList.size())
    }
    table.createColumnList()
    return table
  }

  static TableMatrix create(String name, File file, String delimiter = ',') {
    def table = create(file, delimiter)
    table.name = name
    return table
  }

  static TableMatrix create(File file, String delimiter = ',') {
    def data = file.readLines()*.split(delimiter) as List<List<?>>
    def headerNames = data[0] as List<String>
    def matrix = data[1..data.size() - 1]
    create(headerNames, matrix, [String] * headerNames.size())
  }

  static TableMatrix create(String name, List<List<?>> rowList) {
    def table = create(rowList)
    table.name = name
    return table
  }

  static TableMatrix create(List<List<?>> rowList) {
    def headerList = []
    for (int i = 0; i < rowList[0].size(); i++) {
      headerList.add("v$i")
    }
    return create(headerList, rowList)
  }

  static TableMatrix create(String name, Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
    def table = create(map, dataTypesOpt)
    table.name = name
    return table
  }

  static TableMatrix create(Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
    List<String> header = map.keySet().collect()
    List<?> columns = map.values().collect()
    return create(header, columns.transpose(), dataTypesOpt)
  }

  static TableMatrix create(String name, ResultSet rs) {
    def table = create(rs)
    table.name = name
    return table
  }

  static TableMatrix create(ResultSet rs) {
    ResultSetMetaData rsmd = rs.getMetaData()
    int ncols = rsmd.getColumnCount()
    List<String> headers = []
    List<Class<?>> columnTypes = []
    for (int i = 1; i <= ncols; i++) {
      headers.add(rsmd.getColumnName(i))
      try {
        columnTypes.add(Class.forName(rsmd.getColumnClassName(i)))
      } catch (ClassNotFoundException e) {
        System.err.println "Failed to load class ${rsmd.getColumnClassName(i)}, setting type to Object; ${e.toString()}"
        columnTypes.add(Object.class)
      }
    }
    TableMatrix table = new TableMatrix()
    table.headerList = Collections.unmodifiableList(headers)
    table.columnTypes = Collections.unmodifiableList(columnTypes)
    List<List<Object>> rows = []
    while (rs.next()) {
      List<?> row = []
      for (int i = 1; i <= ncols; i++) {
        row.add(rs.getObject(i))
      }
      rows.add(row)
    }
    table.rowList = Collections.unmodifiableList(rows)
    table.createColumnList()
    return table
  }

  private TableMatrix() {}

  List<String> columnNames() {
    return headerList
  }

  int columnIndex(String columnName) {
    return headerList.indexOf(columnName)
  }

  List<Integer> columnIndexes(List<String> colNames) {
    List<Integer> colNums = []
    colNames.each {
      colNums.add(columnIndex(it))
    }
    return colNums
  }

  List<?> column(String columnName) {
    try {
      return columnList.get(columnIndex(columnName))
    } catch (IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException("The column '$columnName' does not exist in this table: " + e.getMessage())
    }
  }

  List<?> column(int index) {
    return columnList.get(index)
  }

  List<?> row(int index) {
    return rowList.get(index)
  }

  List<List<?>> columns() {
    return columnList
  }

  List<List<?>> columns(List<String> columnNames) {
    def cols = []
    for (String colName in columnNames) {
      cols.add(column(colName))
    }
    return cols
  }

  List<List<?>> rows(List<Integer> index) {
    def rows = []
    index.each {
      rows.add(row((int) it))
    }
    return rows
  }

  List<List<?>> rows() {
    return rowList
  }

  /**
   *
   * @param criteria takes a row (List<?>) as parameter and returns true if the row should be included
   * @return a List of row indexes matching the criteria supplied
   */
  List<Integer> selectRows(Closure criteria) {
    def r = [] as List<Integer>
    rowList.eachWithIndex { row, idx ->
      {
        if (criteria(row)) {
          r.add(idx)
        }
      }
    }
    return r
  }

  /**
   * @deprecated Use rows() instead
   * @return a list of rows (List<?>)
   */
  @Deprecated
  List<List<?>> matrix() {
    return rowList
  }

  Class<?> columnType(int i) {
    return columnTypes[i]
  }

  Class<?> columnType(String columnName) {
    return columnTypes[columnIndex(columnName)]
  }

  List<Class<?>> columnTypes() {
    return columnTypes
  }

  List<Class<?>> columnTypes(List<String> columnNames) {
    List<Class<?>> types = []
    for (name in columnNames) {
      types.add(columnType(name))
    }
    return types
  }

  List<String> columnTypeNames() {
    List<String> types = new ArrayList<>()
    columnTypes.each { types.add(it.getSimpleName()) }
    return types
  }


  TableMatrix transpose(List<String> header = headerList) {
    return create(header, columnList, columnTypes)
  }

  int columnCount() {
    return headerList.size()
  }

  int rowCount() {
    return rowList.size()
  }

  Object get(int row, int column) {
    //return rowList.get(row).get(column)
    return rowList[row][column]
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[0, 1] for the 2:nd column of the first observation
   * @return the value corresponding to the row and column indexes supplied
   */
  Object getAt(int row, int column) {
    return get(row, column)
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[0, "salary"] for the salary for the first observation
   * @return the value corresponding to the row and column name supplied
   */
  Object getAt(int row, String columnName) {
    return get(row, columnIndex(columnName))
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table["salary"] for the salary column
   * @return the column corresponding to the column name supplied
   */
  List<?> getAt(String columnName) {
    return column(columnName)
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[2] for the 3:rd column
   * @return the column corresponding to the column index supplied
   */
  List<?> getAt(int columnIndex) {
    return column(columnIndex)
  }

  private void createColumnList() {
    List<List<Object>> columns = new ArrayList<>()
    for (List<Object> row : rowList) {
      if (columns.size() == 0) {
        for (int i = 0; i < row.size(); i++) {
          columns.add(new ArrayList<>())
        }
      }
      for (int j = 0; j < row.size(); j++) {
        columns.get(j).add(row.get(j))
      }
    }
    columnList = Collections.unmodifiableList(columns)
  }

  @Override
  String toString() {
    return "${rowCount()} obs * ${columnCount()} vars; " + String.join(", ", headerList)
  }

  String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, rowCount())
    if (includeHeader) {
      sb.append(String.join(delimiter, headerList)).append(lineEnding)
    }
    for (int i = 0; i < nRows; i++) {
      def row = ListConverter.convert(row(i), String.class)
      sb.append(String.join(delimiter, row)).append(lineEnding)
    }
    return sb.toString()
  }

  String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, rowCount())
    if (includeHeader) {
      sb.append(String.join(delimiter, headerList)).append(lineEnding)
    }
    for (int i = rowCount() - nRows; i < rowCount(); i++) {
      def row = ListConverter.convert(row(i), String.class)
      sb.append(String.join(delimiter, row)).append(lineEnding)
    }
    return sb.toString()
  }

  String content(boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
    return head(rowCount(), includeHeader, delimiter, lineEnding)
  }

  TableMatrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    def convertedColumns = []
    def convertedTypes = []
    for (int i = 0; i < columnCount(); i++) {
      String colName = headerList[i]
      if (columnTypes[colName]) {
        convertedColumns.add(ListConverter.convert(
            column(i),
            columnTypes[colName] as Class<Object>,
            dateTimeFormatter,
            numberFormat)
        )
        convertedTypes.add(columnTypes[colName])
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(columnType(i))
      }
    }
    def convertedRows = Matrix.transpose(convertedColumns)
    return create(headerList, convertedRows, convertedTypes)
  }

  TableMatrix convert(String colName, Class<?> type, Closure converter) {
    return convert(columnNames().indexOf(colName), type, converter)
  }

  /**
   *
   * @param colNum the column index for the column to convert
   * @param type the class the column will be converted to
   * @param converter as closure converting each value in the designated column
   * @return a new TableMatrix
   */
  TableMatrix convert(int colNum, Class<?> type, Closure converter) {
    def convertedColumns = []
    def convertedTypes = []
    def col = []
    for (int i = 0; i < columnCount(); i++) {
      if (colNum == i) {
        column(i).each { col.add(converter.call(it)) }
        convertedColumns.add(col)
        convertedTypes.add(type)
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(columnType(i))
      }
    }
    def convertedRows = Matrix.transpose(convertedColumns)
    return create(headerList, convertedRows, convertedTypes)
  }

  /**
   * def table = TableMatrix.create([
   *      'place': [1, 2, 3],
   *      'firstname': ['Lorena', 'Marianne', 'Lotte'],
   *      'start': ['2021-12-01', '2022-07-10', '2023-05-27']
   *  ],
   *      [int, String, String]
   *  )
   *  // This will select the second and third row and return it in a new TableMatrix
   *  TableMatrix subSet = table.subset('place', { it > 1 })
   * @param columnName the name of the column to use to select matches
   * @param condition a closure containing the condition for what to retain
   * @return new TableMatrix with only the rows that matched the criteria
   */
  TableMatrix subset(@NotNull String columnName, @NotNull Closure condition) {
    def rows = rows(column(columnName).findIndexValues(condition) as List<Integer>)
    return create(headerList, rows, columnTypes)
  }

  TableMatrix apply(String colName, Closure function) {
    return apply(columnNames().indexOf(colName), function)
  }

  TableMatrix apply(int colNum, Closure function) {
    def convertedColumns = []
    def col = []
    for (int i = 0; i < columnCount(); i++) {
      if (colNum == i) {
        column(i).each { col.add(function.call(it)) }
        convertedColumns.add(col)
      } else {
        convertedColumns.add(column(i))
      }
    }
    def convertedRows = Matrix.transpose(convertedColumns)
    return create(headerList, convertedRows, columnTypes)
  }

  TableMatrix apply(String colName, List<Integer> rows, Closure function) {
    apply(columnIndex(colName), rows, function)
  }

  TableMatrix apply(int colNum, List<Integer> rows, Closure function) {
    def convertedColumns = []
    def col = []
    for (int i = 0; i < columnCount(); i++) {
      if (colNum == i) {
        column(i).eachWithIndex { it, idx ->
          if (idx in rows) {
            col.add(function.call(it))
          } else {
            col.add(it)
          }
        }
        convertedColumns.add(col)
      } else {
        convertedColumns.add(column(i))
      }
    }
    def convertedRows = Matrix.transpose(convertedColumns)
    return create(headerList, convertedRows, columnTypes)
  }

  TableMatrix apply(String colName, Closure criteria, Closure function) {
    apply(columnNames().indexOf(colName), criteria, function)
  }

  TableMatrix apply(int colNum, Closure criteria, Closure function) {
    def updatedRows = []
    rowList.each { row -> {
        if (criteria(row)) {
          def r = []
          for (int i = 0; i < columnCount(); i++) {
            if (colNum == i) {
              r.add(function(row[i]))
            } else {
              r.add(row[i])
            }
          }
          updatedRows.add(r)
        } else {
          updatedRows.add(row)
        }
    }}
    return create(headerList, updatedRows, columnTypes)
  }

  String name() {
    return name
  }


  TableMatrix addColumn(String name, List<?> column, type = Object) {
    List<List<?>> columns = []
    columns.addAll(columnList)
    columns.add(column)
    List<String> headers = []
    headers.addAll(headerList)
    headers.add(name)
    List<Class<?>> types = []
    types.addAll(columnTypes)
    types.add(type)
    return create(headers,  Matrix.transpose(columns), types)
  }

  TableMatrix addColumns(TableMatrix table, String... columns) {
    List<String> names = columns.toList()
    List<List<?>> cols = table.columns(names)
    List<Class<?>> types = table.columnTypes(names)
    return addColumns(names, cols, types)
  }

  TableMatrix addColumns(List<String> names, List<List<?>> columns, List<Class<?>> types) {
    if (columns.size() != names.size() && columns.size() != types.size()) {
      throw new IllegalArgumentException("List sizes of columns, names and types does not match")
    }
    List<List<?>> c = []
    c.addAll(this.columnList)
    c.addAll(columns)
    List<String> headers = []
    headers.addAll(this.headerList)
    headers.addAll(names)
    List<Class<?>> t = []
    t.addAll(this.columnTypes)
    t.addAll(types)
    return create(headers,  Matrix.transpose(c), t)
  }

  TableMatrix addRow(List<?> row) {
    if (row == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    if (row.size() != columnCount()) {
      throw new IllegalArgumentException("The number of elements in the row (${row.size()}) does not match the number of columns (${columnCount()})")
    }
    def rows = []
    rows.addAll(rowList)
    rows.add(row)
    return create(headerList, rows, columnTypes)
  }

  TableMatrix addRows(List<List<?>> rows) {
    if (rows == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    def r = []
    r.addAll(rowList)
    r.addAll(rows)
    return create(headerList, r, columnTypes)
  }

  TableMatrix dropColumnsExcept(String... colNames) {
    List<List<?>> cols = []
    List<Class<?>> types = []
    List<String> heads = []
    for (int i = 0; i < colNames.length; i++) {
      def colName = colNames[i]
      cols.add(column(colName))
      types.add(columnType(colName))
      heads.add(colName)
    }
    return create(name, heads, Matrix.transpose(cols), types)
  }

  TableMatrix dropColumns(String... colNames) {
    def cols = []
    def types = []
    def heads = []
    for (colName in columnNames()) {
      if (colName in colNames) {
        continue
      }
      cols.add(column(colName))
      types.add(columnType(colName))
      heads.add(colName)
    }
    return create(name, heads, Matrix.transpose(cols), types)
  }

  Map<?, TableMatrix> split(String columnName) {
    List<?> col = column(columnName)
    Map<Object, List<Integer>> groups = new HashMap<>()
    for (int i = 0; i < col.size(); i++) {
      groups.computeIfAbsent(col[i], k -> []).add(i)
    }
    //println("groups is $groups")
    Map<?, TableMatrix> tables = [:]
    for (entry in groups) {
      tables.put(entry.key, create(String.valueOf(entry.key), headerList, rows(entry.value), columnTypes))
    }
    return tables
  }

  TableMatrix sort(String columnName, descending = false) {
    if (columnName !in columnNames()) {
      throw new IllegalArgumentException("The column name ${columnName} does not exist is this table (${name})")
    }
    def comparator = new RowComparator(columnIndex(columnName))
    // copy all the rows
    List<List<?>> rows = []
    for (row in rowList) {
      def vals = []
      vals.addAll(row)
      rows.add(vals)
    }
    Collections.sort(rows, comparator)
    if (descending) {
      Collections.reverse(rows)
    }
    return create(name, headerList, rows, columnTypes)
  }

  List<?> findFirstRow(String columnName, Object value) {
    def table = subset(columnName, { it == value })
    if (table.rowCount() > 0) {
      return table.row(0)
    }
    return null
  }

  class RowComparator<T extends Comparable<T>> implements Comparator<List<T>> {

    int columnIdx

    RowComparator(int columnIdx) {
      this.columnIdx = columnIdx
    }

    @Override
    int compare(List<T> r1, List<T> r2) {
      def v1 = r1[columnIdx]
      def v2 = r2[columnIdx]
      if (v1 instanceof Comparable) {
        return v1 <=> v2
      } else {
        return String.valueOf(v1) <=> String.valueOf(v2)
      }
    }

  }
}
