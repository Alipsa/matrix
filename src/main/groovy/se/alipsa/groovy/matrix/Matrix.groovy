package se.alipsa.groovy.matrix

import groovyjarjarantlr4.v4.runtime.misc.NotNull
import se.alipsa.groovy.matrix.util.RowComparator

import java.nio.file.Files
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import static se.alipsa.groovy.matrix.util.ClassUtils.*

/**
 * A Matrix is a 2 dimensional data structure.
 * It is essentially a Grid i.e. [][] (List<List<?>>) but also has a header and a name
 * and several convenience methods to work with it.
 *
 * The table name and column names are mutable and can be set with setName() and renameColumn() respectively.
 * The data and the data types are immutable however.
 * Any manipulation on the content on those results in a new Matrix where the changes are represented.
 */
class Matrix {

  private List<String> mHeaders
  private List<List<?>> mRows
  private List<Class<?>> mTypes
  // Copy of the data i column format
  private List<List<Object>> mColumns
  private String mName
  static final Boolean ASC = Boolean.FALSE
  static final Boolean DESC = Boolean.TRUE

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

  static Matrix create(String name, File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    def table = create(file, delimiter, stringQuote, firstRowAsHeader)
    table.mName = name
    return table
  }

  static Matrix create(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    Matrix table = create(Files.newInputStream(file.toPath()), delimiter, stringQuote, firstRowAsHeader)
    def fileName = file.name
    table.setName(fileName.substring(0, fileName.lastIndexOf('.')))
    return table
  }

  static Matrix create(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try(InputStream inputStream = url.openStream()) {
      def name = url.getFile() == null ? url.getPath() : url.getFile()
      if (name.contains('/')) {
        name = name.substring(name.lastIndexOf('/') + 1, name.length())
      }
      if (name.contains('.')) {
        name = name.substring(0, name.lastIndexOf('.'))
      }
      return create(inputStream, delimiter, stringQuote, firstRowAsHeader).withName(name)
    }
  }

  static Matrix create(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try(InputStreamReader reader = new InputStreamReader(inputStream)) {
      List<List<String>> data = []
      final boolean stripQuotes = stringQuote != ''
      for (line in reader.readLines()) {
        List<String> row = []
        for (val in line.split(delimiter)) {
          if (stripQuotes) {
            row.add(val.replaceAll(~/^$stringQuote|$stringQuote$/, '').trim())
          } else {
            row.add(val.trim())
          }
        }
        data.add(row)
      }
      List<String> headerNames

      if (firstRowAsHeader) {
        headerNames = data[0] as List<String>
        data = data[1..data.size() - 1]
      } else {
        headerNames = []
      }
      create(headerNames, data, [String] * headerNames.size())
    }
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

  void columnNames(List<String> names) {
    if(columnCount() != names.size()) {
      throw new IllegalArgumentException("Number of column names (${names.size()}) does not match number of columns (${columnCount()}) in this table")
    }
    mHeaders.clear()
    mHeaders.addAll(names)
  }

  void renameColumn(String before, String after) {
    List<String> temp = []
    temp.addAll(mHeaders)
    temp[temp.indexOf(before)] = after
    mHeaders =  Collections.unmodifiableList(temp)
  }

  void renameColumn(int columnIndex, String after) {
    List<String> temp = []
    temp.addAll(mHeaders)
    temp[columnIndex] = after
    mHeaders =  Collections.unmodifiableList(temp)
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

  Matrix selectColumns(String... columnNames) {
    List<List<?>> columns = []
    List<String> colNames = []
    List<Class<?>> types = []
    for (name in columnNames) {
      columns.add(column(name))
      colNames.add(name)
      types.add(columnType(name))
    }
    return create(name, colNames, columns.transpose(), types)
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
    int rowCount = 0;
    for (List<Object> row : mRows) {
      if (columns.size() == 0) {
        for (int i = 0; i < row.size(); i++) {
          columns.add(new ArrayList<>())
        }
      }
      for (int j = 0; j < row.size(); j++) {
        try {
          columns.get(j).add(row.get(j))
        } catch(IndexOutOfBoundsException e) {
          throw new ConversionException("Wrong number of columns found on row $rowCount", e)
        }
      }
      rowCount++
    }
    mColumns = Collections.unmodifiableList(columns)
  }

  @Override
  String toString() {
    return "$name: ${rowCount()} obs * ${columnCount()} variables "
  }

  /**
   * @param attr table attributes e.g. id, class etc. that is added immediately after the table and become table
   *  attributes when rendered as html
   * @return a markdown formatted table where all the values have been converted to strings, numbers are right aligned
   * and everything else default (left) aligned
   */
  String toMarkdown(Map<String, String> attr = [:]) {
    def alignment = []
    for (type in columnTypes()) {
      if (Number.isAssignableFrom(type)) {
        alignment.add('---:')
      } else {
        alignment.add('---')
      }
    }
    //mHeaders.collect(w -> w.replaceAll(".", "-"))
    return toMarkdown(alignment)
  }

  /**
   *
   * @param alignment use :--- for left align, :----: for centered, and ---: for right alignment
   * @return a markdown formatted table where all the values have been converted to strings
   */
  String toMarkdown(List<String> alignment, Map<String, String> attr = [:]) {
    if (alignment.size() != columnCount()) {
      throw new IllegalArgumentException("number of alignment markers (${alignment.length}) differs from number of columns (${columnCount()})")
    }
    StringBuilder sb = new StringBuilder()
    sb.append('| ')
    sb.append(String.join(' | ', mHeaders)).append(' |\n')
    sb.append('| ').append(String.join(' | ', alignment)).append(' |\n')
    StringBuilder rowBuilder = new StringBuilder()
    for (row in rows()) {
      rowBuilder.setLength(0)

      for (val in row) {
        rowBuilder.append(ValueConverter.asString(val)).append(' | ')
      }
      sb.append('| ')
          .append(rowBuilder.toString().trim()).append('\n')
    }
    if (attr.size() > 0) {
      sb.append("{")
      attr.each {
        sb.append(it.key).append('="').append(it.value).append('" ')
      }
      sb.append("}\n")
    }
    return sb.toString()
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
    return create(mName, mHeaders, convertedRows, convertedTypes)
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
    return create(mName, mHeaders, convertedRows, convertedTypes)
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
  Matrix subset(@NotNull String columnName, @NotNull Closure<Boolean> condition) {
    def rows = rows(column(columnName).findIndexValues(condition) as List<Integer>)
    return create(mName, mHeaders, rows, mTypes)
  }

  /**
   *
   * @param criteria takes a row (List<?>) as parameter and returns true if the row should be included
   * @return a new Matrix containing the List of rows matching the criteria supplied
   */
  Matrix subset(Closure<Boolean> criteria) {
    def r = [] as List<List<?>>
    mRows.each { row -> {
        if (criteria(row)) {
          r.add(row)
        }
      }
    }
    return create(mName, mHeaders, r, mTypes)
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
    return create(mName, mHeaders, convertedRows, types)
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
    return create(mName, mHeaders, convertedRows, types)
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
    return create(mName, mHeaders, updatedRows, types)
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
    return create(mName, headers,  Grid.transpose(columns), types)
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
    return create(mName, headers,  Grid.transpose(c), t)
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
    return create(name, mHeaders, rows, mTypes)
  }

  Matrix addRows(List<List<?>> rows) {
    if (rows == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    def r = []
    r.addAll(mRows)
    r.addAll(rows)
    return create(mName, mHeaders, r, mTypes)
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

  /**
   * @deprecated Use orderBy instead
   */
  @Deprecated
  Matrix sort(String columnName, Boolean descending = Boolean.FALSE) {
    return sortBy(columnName, descending)
  }

  /**
   * @deprecated Use orderBy instead
   */
  @Deprecated
  Matrix sortBy(String columnName, Boolean descending = Boolean.FALSE) {
    orderBy(columnName, descending)
  }

  /**
   * @deprecated Use orderBy instead
   */
  @Deprecated
  Matrix sortBy(LinkedHashMap<String, Boolean> columnsAndDirection) {
    orderBy(columnsAndDirection)
  }

  /**
   * @deprecated Use orderBy instead
   */
  @Deprecated
  Matrix sortBy(Comparator comparator) {
    orderBy(comparator)
  }

  /**
   * Sort this table in ascending  order by the columns specified
   * @param columnNames the columns to sort by
   * @return a copy of this table, sorted in ascending order by the columns specified
   */
  Matrix orderBy(List<String> columnNames) {
    LinkedHashMap<String, Boolean> columnsAndDirection = [:]
    for (colName in columnNames) {
      columnsAndDirection[colName] = ASC
    }
    return orderBy(columnsAndDirection)
  }

  Matrix orderBy(String columnName, Boolean descending = Boolean.FALSE) {
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

  Matrix orderBy(LinkedHashMap<String, Boolean> columnsAndDirection) {
    def columnNames = columnsAndDirection.keySet() as List<String>
    for (String columnName in columnNames) {
      if (columnName !in mHeaders) {
        throw new IllegalArgumentException("The column name ${columnName} does not exist is this table (${mName})")
      }
    }
    LinkedHashMap<Integer, Boolean> sortCriteria = [:]
    columnsAndDirection.each {
      sortCriteria[columnIndex(it.key)] = it.value
    }
    def comparator = new RowComparator(sortCriteria)
    return orderBy(comparator)
  }

  Matrix orderBy(Comparator comparator) {
    // copy all the rows
    List<List<?>> rows = []
    for (row in mRows) {
      def vals = []
      vals.addAll(row)
      rows.add(vals)
    }
    Collections.sort(rows, comparator)
    return create(mName, mHeaders, rows, mTypes)
  }

  List<?> findFirstRow(String columnName, Object value) {
    def table = subset(columnName, { it == value })
    if (table.rowCount() > 0) {
      return table.row(0)
    }
    return null
  }

  String getName(){
    return mName
  }

  void setName(String name) {
    mName = name
  }

  /**
   * Sets the name of the table and return the table
   * @param name the name for the table
   * @return the table itself
   */
  Matrix withName(String name) {
    setName(name)
    return this
  }

  /**
   * Allows you to iterate over the rows in this table
   * <pre><code>
   *   // a for loop usage example
   *   for (row in table) {
   *     //do stuff with the row
   *   }
   *
   *   // using each
   *   table.each {
   *     // "it" contains the row
   *   }
   * @return an Iterator iterating over the rows (observations) in this table
   */
  Iterator<List<?>> iterator() {
    return mRows.iterator()
  }

  /**f
   * Convert this table into a Grid
   * @return a Grid corresponding to the data content of this table
   */
  Grid grid() {
    return new Grid(rows())
  }
}
