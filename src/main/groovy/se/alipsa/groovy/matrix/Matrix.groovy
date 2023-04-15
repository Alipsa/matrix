package se.alipsa.groovy.matrix

import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import static se.alipsa.groovy.matrix.util.ClassUtils.*

/**
 * This is essentially a Grid i.e. [][] (List<List<?>>) but also has a header
 * and several convenience methods to work with it.
 */
class Matrix {

  private List<String> mHeaders
  private List<List<?>> mRows
  private List<Class<?>> mTypes
  // Copy of the data i column format
  private List<List<Object>> mColumns
  private String mName

  static Matrix create(String name, List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    def table = create(headerList, rowList, dataTypesOpt)
    table.mName = name
    return table
  }

  static Matrix create(String name, List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt) {
    def table = create(headerList, grid, dataTypesOpt)
    table.mName = name
    return table
  }

  static Matrix create(List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt) {
    create(headerList, grid.data, dataTypesOpt)
  }

  static Matrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    Matrix table = new Matrix()
    table.mHeaders = Collections.unmodifiableList(headerList.collect())
    table.mRows = Collections.unmodifiableList(rowList.collect())
    if (dataTypesOpt.length > 0) {
      table.mTypes = Collections.unmodifiableList(convertPrimitivesToWrapper(dataTypesOpt[0]))
      if (headerList.size() != table.mTypes.size()) {
        throw new IllegalArgumentException("Number of columns (${headerList.size()}) differs from number of datatypes provided (${table.mTypes.size()})")
      }
    } else {
      table.mTypes = Collections.unmodifiableList([Object.class] * headerList.size())
    }
    table.createColumnList()
    return table
  }

  static Matrix create(String name, File file, String delimiter = ',') {
    def table = create(file, delimiter)
    table.mName = name
    return table
  }

  static Matrix create(File file, String delimiter = ',') {
    def data = file.readLines()*.split(delimiter) as List<List<?>>
    def headerNames = data[0] as List<String>
    def matrix = data[1..data.size() - 1]
    create(headerNames, matrix, [String] * headerNames.size())
  }

  static Matrix create(String name, Grid grid) {
    create(name, grid.data)
  }

  static Matrix create(String name, List<List<?>> rowList) {
    def table = create(rowList)
    table.mName = name
    return table
  }

  static Matrix create(Grid grid) {
    create(grid.data)
  }

  static Matrix create(List<List<?>> rowList) {
    def headerList = []
    for (int i = 0; i < rowList[0].size(); i++) {
      headerList.add("v$i")
    }
    return create(headerList, rowList)
  }

  static Matrix create(String name, Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
    def table = create(map, dataTypesOpt)
    table.mName = name
    return table
  }

  static Matrix create(Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
    List<String> header = map.keySet().collect()
    List<?> columns = map.values().collect()
    return create(header, columns.transpose(), dataTypesOpt)
  }

  static Matrix create(String name, ResultSet rs) {
    def table = create(rs)
    table.mName = name
    return table
  }

  static Matrix create(ResultSet rs) {
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
    Matrix table = new Matrix()
    table.mHeaders = Collections.unmodifiableList(headers)
    table.mTypes = Collections.unmodifiableList(convertPrimitivesToWrapper(columnTypes))
    List<List<Object>> rows = []
    while (rs.next()) {
      List<?> row = []
      for (int i = 1; i <= ncols; i++) {
        row.add(rs.getObject(i))
      }
      rows.add(row)
    }
    table.mRows = Collections.unmodifiableList(rows)
    table.createColumnList()
    return table
  }

  private Matrix() {}

  List<String> columnNames() {
    return mHeaders
  }

  int columnIndex(String columnName) {
    return mHeaders.indexOf(columnName)
  }

  List<Integer> columnIndexes(List<String> columnNames) {
    List<Integer> colNums = []
    columnNames.each {
      colNums.add(columnIndex(it))
    }
    return colNums
  }

  List<?> column(String columnName) {
    try {
      return mColumns.get(columnIndex(columnName))
    } catch (IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException("The column '$columnName' does not exist in this table: " + e.getMessage())
    }
  }

  List<?> column(int index) {
    return mColumns.get(index)
  }

  List<?> row(int index) {
    return mRows.get(index)
  }

  List<List<?>> columns() {
    return mColumns
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
    return mRows
  }

  /**
   *
   * @param criteria takes a row (List<?>) as parameter and returns true if the row should be included
   * @return a List of row indexes matching the criteria supplied
   */
  List<Integer> selectRows(Closure criteria) {
    def r = [] as List<Integer>
    mRows.eachWithIndex { row, idx ->
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
    return mRows
  }

  Class<?> columnType(int i) {
    return mTypes[i]
  }

  Class<?> columnType(String columnName) {
    return mTypes[columnIndex(columnName)]
  }

  List<Class<?>> columnTypes() {
    return mTypes
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
    mTypes.each { types.add(it.getSimpleName()) }
    return types
  }


  Matrix transpose(List<String> header = mHeaders) {
    return create(header, mColumns, mTypes)
  }

  int columnCount() {
    return mHeaders.size()
  }

  int rowCount() {
    return mRows.size()
  }

  Object get(int row, int column) {
    //return rowList.get(row).get(column)
    return mRows[row][column]
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[0, 1] for the 2:nd column of the first observation
   * @return the value corresponding to the row and column indexes supplied
   */
  Object getAt(Integer row, Integer column) {
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
    for (List<Object> row : mRows) {
      if (columns.size() == 0) {
        for (int i = 0; i < row.size(); i++) {
          columns.add(new ArrayList<>())
        }
      }
      for (int j = 0; j < row.size(); j++) {
        columns.get(j).add(row.get(j))
      }
    }
    mColumns = Collections.unmodifiableList(columns)
  }

  @Override
  String toString() {
    return "${rowCount()} obs * ${columnCount()} vars; " + String.join(", ", mHeaders)
  }

  String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, rowCount())
    if (includeHeader) {
      sb.append(String.join(delimiter, mHeaders)).append(lineEnding)
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
      sb.append(String.join(delimiter, mHeaders)).append(lineEnding)
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

  Matrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    def convertedColumns = []
    def convertedTypes = []
    for (int i = 0; i < columnCount(); i++) {
      String colName = mHeaders[i]
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
    def convertedRows = Grid.transpose(convertedColumns)
    return create(mHeaders, convertedRows, convertedTypes)
  }

  Matrix convert(String columnName, Class<?> type, Closure converter) {
    return convert(columnNames().indexOf(columnName), type, converter)
  }

  /**
   *
   * @param columnNumber the column index for the column to convert
   * @param type the class the column will be converted to
   * @param converter as closure converting each value in the designated column
   * @return a new Matrix
   */
  Matrix convert(int columnNumber, Class<?> type, Closure converter) {
    def convertedColumns = []
    def convertedTypes = []
    def col = []
    for (int i = 0; i < columnCount(); i++) {
      if (columnNumber == i) {
        column(i).each { col.add(converter.call(it)) }
        convertedColumns.add(col)
        convertedTypes.add(type)
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(columnType(i))
      }
    }
    def convertedRows = Grid.transpose(convertedColumns)
    return create(mHeaders, convertedRows, convertedTypes)
  }

  /**
   * def table = Matrix.create([
   *      'place': [1, 2, 3],
   *      'firstname': ['Lorena', 'Marianne', 'Lotte'],
   *      'start': ['2021-12-01', '2022-07-10', '2023-05-27']
   *  ],
   *      [int, String, String]
   *  )
   *  // This will select the second and third row and return it in a new Matrix
   *  Matrix subSet = table.subset('place', { it > 1 })
   * @param columnName the name of the column to use to select matches
   * @param condition a closure containing the condition for what to retain
   * @return new Matrix with only the rows that matched the criteria
   */
  Matrix subset(@NotNull String columnName, @NotNull Closure condition) {
    def rows = rows(column(columnName).findIndexValues(condition) as List<Integer>)
    return create(mHeaders, rows, mTypes)
  }

  Matrix apply(String columnName, Closure function) {
    return apply(columnNames().indexOf(columnName), function)
  }

  Matrix apply(int columnNumber, Closure function) {
    def converted = []
    def col = []
    Class<?> updatedClass = null
    for (int i = 0; i < columnCount(); i++) {
      if (columnNumber == i) {
        column(i).each {
          def val = function.call(it)
          if (updatedClass == null && val != null) {
            updatedClass = val.class
          }
          col.add(val)
        }
        converted.add(col)
      } else {
        converted.add(column(i))
      }
    }
    List<Class<?>> types = updatedClass != columnType(columnNumber)
        ? createTypeListWithNewValue(columnNumber, updatedClass, false)
        : mTypes
    def convertedRows = Grid.transpose(converted)
    return create(mHeaders, convertedRows, types)
  }

  Matrix apply(String columnName, List<Integer> rows, Closure function) {
    apply(columnIndex(columnName), rows, function)
  }

  Matrix apply(int columnNumber, List<Integer> rows, Closure function) {
    def converted = []
    def col = []
    Class<?> updatedClass = null
    for (int i = 0; i < columnCount(); i++) {
      if (columnNumber == i) {
        column(i).eachWithIndex { it, idx ->
          if (idx in rows) {
            def val = function.call(it)
            if (updatedClass == null && val != null) {
              updatedClass = val.class
            }
            col.add(val)
          } else {
            col.add(it)
          }
        }
        converted.add(col)
      } else {
        converted.add(column(i))
      }
    }
    List<Class<?>> types = updatedClass != columnType(columnNumber)
        ? createTypeListWithNewValue(columnNumber, updatedClass, true)
        : mTypes
    def convertedRows = Grid.transpose(converted)
    return create(mHeaders, convertedRows, types)
  }

  Matrix apply(String columnName, Closure criteria, Closure function) {
    apply(columnNames().indexOf(columnName), criteria, function)
  }

  Matrix apply(int columnNumber, Closure criteria, Closure function) {
    def updatedRows = []
    Class<?> updatedClass = null
    mRows.each { row -> {
        if (criteria(row)) {
          def r = []
          for (int i = 0; i < columnCount(); i++) {
            if (columnNumber == i) {
              def val = function(row[i])
              if (updatedClass == null && val != null) {
                updatedClass = val.class
              }
              r.add(val)
            } else {
              r.add(row[i])
            }
          }
          updatedRows.add(r)
        } else {
          updatedRows.add(row)
        }
    }}
    List<Class<?>> types = updatedClass != columnType(columnNumber)
        ? createTypeListWithNewValue(columnNumber, updatedClass, true)
        : mTypes
    return create(mHeaders, updatedRows, types)
  }

  private List<Class<?>> createTypeListWithNewValue(int columnNumber, Class<?> updatedClass, boolean findCommonGround) {
    List<Class<?>> types = []
    for (int i = 0; i < mHeaders.size(); i++) {
      if (i == columnNumber) {
        if (findCommonGround) {
          types.add(findClosestCommonSuper(updatedClass, columnType(columnNumber)))
        } else {
          types.add(updatedClass)
        }
      } else {
        types.add(columnType(i))
      }
    }
    return types
  }


  Matrix addColumn(String name, List<?> column, type = Object) {
    List<List<?>> columns = []
    columns.addAll(mColumns)
    columns.add(column)
    List<String> headers = []
    headers.addAll(mHeaders)
    headers.add(name)
    List<Class<?>> types = []
    types.addAll(mTypes)
    types.add(type)
    return create(headers,  Grid.transpose(columns), types)
  }

  Matrix addColumns(Matrix table, String... columns) {
    List<String> names = columns.toList()
    List<List<?>> cols = table.columns(names)
    List<Class<?>> types = table.columnTypes(names)
    return addColumns(names, cols, types)
  }

  Matrix addColumns(List<String> names, List<List<?>> columns, List<Class<?>> types) {
    if (columns.size() != names.size() && columns.size() != types.size()) {
      throw new IllegalArgumentException("List sizes of columns, names and types does not match")
    }
    List<List<?>> c = []
    c.addAll(this.mColumns)
    c.addAll(columns)
    List<String> headers = []
    headers.addAll(this.mHeaders)
    headers.addAll(names)
    List<Class<?>> t = []
    t.addAll(this.mTypes)
    t.addAll(types)
    return create(headers,  Grid.transpose(c), t)
  }

  Matrix addRow(List<?> row) {
    if (row == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    if (row.size() != columnCount()) {
      throw new IllegalArgumentException("The number of elements in the row (${row.size()}) does not match the number of columns (${columnCount()})")
    }
    def rows = []
    rows.addAll(mRows)
    rows.add(row)
    return create(mHeaders, rows, mTypes)
  }

  Matrix addRows(List<List<?>> rows) {
    if (rows == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    def r = []
    r.addAll(mRows)
    r.addAll(rows)
    return create(mHeaders, r, mTypes)
  }

  Matrix dropColumnsExcept(String... columnNames) {
    List<List<?>> cols = []
    List<Class<?>> types = []
    List<String> heads = []
    for (int i = 0; i < columnNames.length; i++) {
      def colName = columnNames[i]
      cols.add(column(colName))
      types.add(columnType(colName))
      heads.add(colName)
    }
    return create(mName, heads, Grid.transpose(cols), types)
  }

  Matrix dropColumns(String... columnNames) {
    def cols = []
    def types = []
    def heads = []
    for (colName in this.columnNames()) {
      if (colName in columnNames) {
        continue
      }
      cols.add(column(colName))
      types.add(columnType(colName))
      heads.add(colName)
    }
    return create(mName, heads, Grid.transpose(cols), types)
  }

  Map<?, Matrix> split(String columnName) {
    List<?> col = column(columnName)
    Map<Object, List<Integer>> groups = new HashMap<>()
    for (int i = 0; i < col.size(); i++) {
      groups.computeIfAbsent(col[i], k -> []).add(i)
    }
    Map<?, Matrix> tables = [:]
    for (entry in groups) {
      tables.put(entry.key, create(String.valueOf(entry.key), mHeaders, rows(entry.value), mTypes))
    }
    return tables
  }

  Matrix sort(String columnName, descending = false) {
    if (columnName !in columnNames()) {
      throw new IllegalArgumentException("The column name ${columnName} does not exist is this table (${mName})")
    }
    def comparator = new RowComparator(columnIndex(columnName))
    // copy all the rows
    List<List<?>> rows = []
    for (row in mRows) {
      def vals = []
      vals.addAll(row)
      rows.add(vals)
    }
    Collections.sort(rows, comparator)
    if (descending) {
      Collections.reverse(rows)
    }
    return create(mName, mHeaders, rows, mTypes)
  }

  List<?> findFirstRow(String columnName, Object value) {
    def table = subset(columnName, { it == value })
    if (table.rowCount() > 0) {
      return table.row(0)
    }
    return null
  }

  class RowComparator<T> implements Comparator<List<T>> {

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

  String getName(){
    return mName
  }

  void setName(String name) {
    mName = name
  }

  Iterator<List<?>> iterator() {
    return mRows.iterator()
  }

  Grid grid() {
    return new Grid(rows())
  }
}
