package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import se.alipsa.groovy.matrix.util.RowComparator

import java.sql.ResultSet
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import static se.alipsa.groovy.matrix.util.ClassUtils.*

/**
 * A Matrix is a 2 dimensional data structure.
 * It is essentially a Grid i.e. [][] (List<List<?>>) but also has a header and a name
 * and several convenience methods to work with it.
 *
 * Use the Matrix.builder() to create a Matrix.
 * The builder takes either data in the form of a list of rows, a list of columns, a map of the columns with their
 * column name, a resultset etc.
 *
 * Matrix data can be accessed using the matrix[row, column] notation e.g. <code>myMatrix[1,3]</code>
 * for the 4:th variable of the second observation
 * or matrix[column] for the whole column e.g.
 * <code>myMatrix[1]</code> for the second column, or <code>myMatrix['foo']</code> for the column named foo.
 *
 * Similarly, you use the the same notation to assign / change values e.g.
 * <code>myMatrix[0,1] = 23</code> to assign the value 23 to the second variable of the first observation
 * or to assign / create a column <code> myMatrix['bar'] = [1..12]</code> to assign the range 1 to 12 to the column bar
 *
 */
@CompileStatic
class Matrix implements Iterable<Row> {

  // TODO: consider using Collections.checkedList to create a type checked version of each column

  private List<String> mHeaders
  private List<Class<?>> mTypes
  // Copy of the data i column format
  private List<List<?>> mColumns
  private String mName
  public static final Boolean ASC = Boolean.FALSE
  public static final Boolean DESC = Boolean.TRUE

  static MatrixBuilder builder() {
    new MatrixBuilder()
  }

  /**
   * @deprecated use builder()
   *  .name(name)
   *  .columnNames(headerList)
   *  .rows(rowList)
   *  .dataTypes(classList)
   *  .build() instead
   * @param name the name of the Matrix
   * @param headerList the names of the variables (columns)
   * @param rowList the observations
   * @param dataTypesOpt the data types (classes) for the columns
   * @return a new Matrix
   */
  @Deprecated
  static Matrix create(String name, List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    builder()
        .name(name)
        .columnNames(headerList)
        .rows(rowList)
        .types(dataTypesOpt)
        .build()
  }

  /**
   * @deprecated use builder()
   *  .columnNames(headerList)
   *  .rows(rowList)
   *  .dataTypes(classList)
   *  .build() instead
   * @param headerList the names of the variables (columns)
   * @param rowList the observations
   * @param dataTypesOpt the data types (classes) for the columns
   * @return a new Matrix
   */
  @Deprecated
  static Matrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
    builder()
        .columnNames(headerList)
        .rows(rowList)
        .types(dataTypesOpt)
        .build()
  }

  /**
   *
   * Deprecated, use <code><pre>
   *   Matrix.builder()
   *   .name(theName)
   *   .rows(rowList)
   *   .build()
   * </pre></code> instead
   *
   * @param name the name of the Matrix
   * @param rowList the observations
   * @return a new Matrix
   * @deprecated use Matrix.builder() instead
   */
  @Deprecated
  static Matrix create(String name, List<List<?>> rowList) {
    builder()
        .name(name)
        .rows(rowList)
        .build()
  }

  /**
   * @deprecated use Matrix.builder().rows(rowList).build() instead
   * @param rowList the observations
   * @return a new Matrix
   */
  @Deprecated
  static Matrix create(List<List<?>> rowList) {
    builder().rows(rowList).build()
  }

  /**
   *
   * @param name
   * @param headerList
   * @param grid
   * @param dataTypesOpt
   * @return
   * @deprecated use Matrix.builder().name(name).columnNames(headerList).data(grid).dataTypes(classList).build() instead
   */
  static Matrix create(String name, List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt) {
    builder().name(name).columnNames(headerList).data(grid).types(dataTypesOpt).build()
  }

  /**
   *
   * @param headerList the variable names
   * @param grid the data
   * @param dataTypesOpt the data types
   * @return a new Matrix
   * @deprecated use Matrix.builder().columnNames(headerList).data(grid).dataTypes(classList).build() instead
   */
  @Deprecated
  static Matrix create(List<String> headerList, Grid grid, List<Class<?>>... dataTypesOpt) {
    builder()
        .columnNames(headerList)
        .rows(grid.data)
        .types(dataTypesOpt)
        .build()
  }

  /**
   *
   * @param name the name of the Matrix
   * @param grid the data
   * @return a new Matrix
   * @deprecated use builder().name(name).rows(grid.data).build() instead
   */
  @Deprecated
  static Matrix create(String name, Grid grid) {
    builder().name(name).rows(grid.data).build()
  }

  /**
   * @deprecated use builder().rows(grid.data).build() instead
   * @param grid
   * @return
   */
  @Deprecated
  static Matrix create(Grid grid) {
    builder().rows(grid.data).build()
  }

  /**
   * @deprecated use builder().name(name).data(file, delimiter, stringQuote, firstRowAsHeader).build() instead
   * @param name
   * @param file
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  @Deprecated
  static Matrix create(String name, File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    builder()
        .name(name)
        .data(file, delimiter, stringQuote, firstRowAsHeader)
        .build()
  }

  /**
   *
   * @param file
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   * @deprecated use builder().data(file, delimiter, stringQuote).build() instead
   */
  @Deprecated
  static Matrix create(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    builder()
    .data(file, delimiter, stringQuote)
    .build()
  }

  /**
   * @deprecated use builder().data(url, delimiter, stringQuote, firstRowAsHeader).build() instead
   * @param url
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  @Deprecated
  static Matrix create(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    builder().data(url, delimiter, stringQuote, firstRowAsHeader).build()
  }

  /**
   * @deprecated use builder().data(inputStream, delimiter, stringQuote, firstRowAsHeader).build() instead
   * @param inputStream
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  @Deprecated
  static Matrix create(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    builder().data(inputStream, delimiter, stringQuote, firstRowAsHeader).build()
  }

  /**
   * @deprecated use builder().name(name).data(rs).build() instead
   * @param name
   * @param rs
   * @return
   */
  @Deprecated
  static Matrix create(String name, ResultSet rs) {
    builder().name(name).data(rs).build()
  }

  /**
   * @deprecated use builder().data(rs).build() instead
   * @param rs
   * @return
   */
  @Deprecated
  static Matrix create(ResultSet rs) {
    builder().data(rs).build()
  }

  /**
   * Create an empty matrix
   * Note the difference between Matrix.create(String, Map) which creates and empty matrix
   * (the map contains column names and types) and this new Matrix(String, Map) which creates a matrix
   * with data (the map contains column name and values)
   *
   * @param name
   * @param columnNameAndType a map containing thd column name and its type
   * @return a new Matrix with empty rows
   */
  static Matrix create(String name, Map<String, Class<?>> columnNameAndType) {
    Matrix m = new Matrix()
    m.mName = name
    List<String> headers = new ArrayList<>(columnNameAndType.keySet())
    List types = new ArrayList(columnNameAndType.values())
    m.mTypes = sanitizeTypes(headers, types)
    m.mHeaders = headers
    List<List<?>> columns = []
    headers.each {
      columns << new ArrayList()
    }
    m.mColumns = columns
    m
  }

  private Matrix() {}

  /**
   * Create an empty matrix with the column names defined
   *
   * @deprecated use builder().name(name).columnNames(headerList).build() instead
   * @param name the name of the matrix
   * @param headerList the list of column names
   */
  @Deprecated
  Matrix(String name, List<String> headerList) {
    this(name, headerList, createEmptyColumns(headerList), createObjectTypes(headerList))
  }

  /**
   * @deprecated use builder().name(name).columnNames(headerList).columns(columns).build() instead
   * @param name
   * @param headerList
   * @param columns
   */
  @Deprecated
  Matrix(String name, List<String> headerList, List<List<?>> columns) {
    this(name, headerList, columns, createObjectTypes(columns))
  }

  /**
   * Creates a new Matrix
   *
   * @param name the name of the Matrix
   * @param headerList the variable (column) names
   * @param columns the variable list (the data in columnar format)
   * @param dataTypes the data types (classes)
   */
  Matrix(String name, List<String> headerList, List<List<?>> columns, List<Class<?>> dataTypes) {
    if (dataTypes != null && headerList == null) {
      headerList = (1..dataTypes.size()).collect{'c' + it}
    }

    if (columns == null) {
      int ncol = headerList == null ? (dataTypes == null ? 0 : dataTypes.size()): headerList.size()
      columns = []
      for(int i = 0; i < ncol; i++) {
        columns << []
      }
    }

    if (columns != null && headerList == null) {
      if (columns.isEmpty()) {
        headerList = []
      } else {
        headerList = (1..columns.size()).collect { 'c' + it }
      }
    }

    dataTypes = dataTypes ?: []

    if (dataTypes.size() > columns.size()) {
      for (int i = 0; i < dataTypes.size() - columns.size(); i++) {
        columns << []
      }
    }

    mName = name
    mTypes = sanitizeTypes(headerList, dataTypes)
    mHeaders = headerList.collect{ String.valueOf(it) }
    if (mHeaders.size() != mTypes.size()) {
      throw new IllegalArgumentException("Number of elements in the headerList (${headerList}) differs from number of datatypes (${dataTypes})")
    }
    mColumns = []
    columns.each { List<?> column ->
      //mColumns.add(Collections.checkedList(column, mTypes[i]))
      mColumns.add(column.collect())
    }
    //println "Creating a matrix with name: '$mName', ${mHeaders.size()} headers, ${mColumns.size()} columns, ${mTypes.size()} types"
  }

  /**
   * Constructor to create a matrix with the map of columnar data supplied
   * Note the difference between Matrix.create(String, Map) which creates and empty matrix
   * (the map contains column names and types) and this new Matrix(String, Map) which creates a matrix
   * with data (the map contains column name and values)
   *
   * @param name the name of the matrix
   * @param columns a map with the column name as the key and a lost of values for that column
   * @param dataTypesOpt an optional list of dataTypes
   * @deprecated use builder().name(theName).data(columns).dataTypes(dataTypesOpt).build() instead
   */
  @Deprecated
  Matrix(String name, Map<String, List<?>> columns, List<Class<?>>... dataTypesOpt) {
    mName = name
    mHeaders = []
    mColumns = []
    columns.each {k, v ->
      mHeaders << String.valueOf(k)
      mColumns << v.collect()
    }
    mTypes = sanitizeTypes(mHeaders, dataTypesOpt)
  }

  /**
   *
   * @deprecated use builder().columns(theColumns).build() instead
   */
  @Deprecated
  Matrix(Map<String, List<?>> columns) {
    this((String)null, columns, [])
  }

  /**
   * @deprecated use builder().columns(columnList).dataTypes(classList).build() instead
   * @param columns the data in columnar format
   * @param dataTypes the classes for each column
   */
  @Deprecated
  Matrix(Map<String, List<?>> columns, List<Class<?>> dataTypes) {
    this((String)null, columns, dataTypes)
  }

  /**
   * @deprecated use builder().columns(columnList).build() instead
   * @param columns
   */
  @Deprecated
  Matrix(List<List<?>> columns) {
    mHeaders = []
    mColumns = []
    columns.eachWithIndex { List<?> column, int i ->
      mHeaders << ("c$i" as String)
      mColumns << column.collect()
    }
    mTypes = createObjectTypes(columns)
  }

  Matrix addColumn(String name, type = Object, List<?> column) {
    if (mHeaders.contains(name)) {
      throw new IllegalArgumentException("Column names must be unique, $name already exists at index ${columnIndex(name)}")
    }
    mHeaders << name
    mTypes << (type as Class<?>)
    mColumns << column
    return this
  }

  /**
   * add (insert) a column into the specified location of the Matrix
   *
   * @param name the name of the column
   * @param column the list of column values
   * @param type the type (class) of the data
   * @param index where to put the column
   * @return a new Matrix with the column inserted
   */
  Matrix addColumn(String name, type = Object, Integer index, List<?> column) {
    mHeaders.add(index, name)
    mTypes.add(index, type as Class<?>)
    mColumns.add(index, column)
    return this
  }

  /**
   * Add the columns of the matrix supplied to this matrix. Note that the number of rows
   * must be the same.
   *
   * @param table the matrix to add
   * @param columns the columns of the table to add. If none supplied all columns will be added
   * @return mutates this matrix and returns it
   */
  Matrix addColumns(Matrix table, String... columns) {
    List<String> names
    if (columns.length > 0) {
      names = columns.toList()
    } else {
      names = table.columnNames()
    }
    List<List<?>> cols = table.columns(names)
    List<Class<?>> types = table.types(names)
    return addColumns(names, cols, types)
  }

  Matrix addColumns(List<String> names, List<List<?>> columns, List<Class<?>> types) {
    if (columns.size() != names.size() && columns.size() != types.size()) {
      throw new IllegalArgumentException("List sizes of columns, names and types does not match")
    }
    names.eachWithIndex { String name, int i ->
      addColumn(name, types[i], columns[i])
    }
    return this
  }

  /**
   * Appends a row to the current Matrix
   *
   * @param row a List<?> containing the row to add
   * @return a reference to the this Matrix to allow method chaining
   */
  Matrix addRow(List<?> row) {
    if (row == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    if (row.size() != columnCount()) {
      throw new IllegalArgumentException("The number of elements in the row (${row.size()}) does not match the number of columns (${columnCount()})")
    }
    for (int c = 0; c < row.size(); c++) {
      mColumns[c].add(row[c])
    }
    return this
  }

  /**
   * Insert a row i the specified position.
   *
   * @param position the row index to insert the row at
   * @param row the row data
   * @return the mutated Matrix
   */
  Matrix addRow(int position, List<?> row) {
    if (row == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    if (row.size() != columnCount()) {
      throw new IllegalArgumentException("The number of elements in the row (${row.size()}) does not match the number of columns (${columnCount()})")
    }
    for (int c = 0; c < row.size(); c++) {
      mColumns[c].add(position, row[c])
    }
    return this
  }

  /**
   * Adds several rows to the current Matrix
   *
   * @param rows a List<List<?> containing the rows to add
   * @return a reference to the this Matrix to allow method chaining
   */
  Matrix addRows(List<List<?>> rows) {
    if (rows == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    rows.each {
      addRow(it)
    }
    return this
  }

  /**
   * Apply executes a Closure on each value of the specified column
   *
   * @param columnName the column to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(String columnName, Closure function) {
    return apply(columnNames().indexOf(columnName), function)
  }

  /**
   * Apply executes a Closure on each value of the specified column
   *
   * @param columnNumber the column to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(int columnNumber, Closure function) {
    List<List<?>> converted = []
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
    List<Class<?>> types = updatedClass != type(columnNumber)
            ? createTypeListWithNewValue(columnNumber, updatedClass, false)
            : mTypes

    mColumns = new ArrayList<>(converted)
    mTypes = new ArrayList<>(types)
    /*
    def convertedRows = Grid.transpose(converted)
    return builder()
        .name(mName)
        .columnNames(mHeaders)
        .rows(convertedRows)
        .types(types)
        .build()

     */
    this
  }

  /**
   * Apply executes a Closure on each value of the specified column for the rows specified
   *
   * @param columnName the column to apply to
   * @param rows the rows to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(String columnName, List<Integer> rows, Closure function) {
    apply(columnIndex(columnName), rows, function)
  }

  /**
   * Apply executes a Closure on each value of the specified column for the rows specified
   *
   * @param columnNumber the column to apply to
   * @param rows the rows to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(int columnNumber, List<Integer> rows, Closure function) {
    List<List<?>> converted = []
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
    List<Class<?>> dataTypes = updatedClass != type(columnNumber)
            ? createTypeListWithNewValue(columnNumber, updatedClass, true)
            : mTypes
    mColumns = new ArrayList<>(converted)
    mTypes = new ArrayList<>(dataTypes)
    /*
    def convertedRows = Grid.transpose(converted)
    return builder()
      .name(mName)
      .columnNames(mHeaders)
      .rows(convertedRows)
      .types(dataTypes)
      .build()*/
    this
  }

  /**
   * Apply executes a Closure on each value of the specified column if the criteria closure is true
   *
   * @param columnName the column to apply to
   * @param criteria the Closure determining the rows to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(String columnName, Closure<Boolean> criteria, Closure function) {
    apply(columnNames().indexOf(columnName), criteria, function)
  }

  /**
   * Apply executes a Closure on each value of the specified column if the criteria closure is true
   *
   * @param columnNumber the column to apply to
   * @param criteria the Closure determining the rows to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(int columnNumber, Closure criteria, Closure function) {
    List<List<?>> updatedRows = []
    Class<?> updatedClass = null
    rows().each { it -> {
      def row = it as List<?>
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
    List<Class<?>> dataTypes = updatedClass != type(columnNumber)
            ? createTypeListWithNewValue(columnNumber, updatedClass, true)
            : mTypes
    mColumns = new ArrayList<>(Grid.transpose(updatedRows))
    mTypes = new ArrayList<>(dataTypes)
    /*
    return builder()
      .name(mName)
      .columnNames(mHeaders)
      .rows(updatedRows)
      .types(dataTypes)
      .build()*/
    this
  }

  /**
   * Apply executes a Closure on each value of the specified column
   * but takes the entire row as the parameter which is different from the apply(columnName, function)
   * which only takes the column value as parameter
   *
   * @param columnName the column to apply to
   * @param function the closure taking the entire row as parameter
   * @return this Matrix (mutated)
   */
  Matrix applyRows(String columnName, Closure function) {
    this.rows().each { row ->
      row[columnName] = function.call(row)
    }
    this
  }

  String columnName(int index) {
    return columnNames()[index]
  }

  List<String> columnNames() {
    return mHeaders
  }

  /**
   * Set the column name at the index specified
   * The column name will be converted to a String.
   *
   * @param index the position to rename
   * @param name the value to rename the column to
   * @return a reference to this matrix
   */
  Matrix columnName(int index, Object name) {
    mHeaders[index] = String.valueOf(name)
    this
  }

  /**
   * Due to type erasure, we cannot guard against a list of something else
   * being passed as column names so we convert all items in the list to a String
   *
   * @param names
   * @return
   */
  Matrix columnNames(List<String> names) {
    if(columnCount() != names.size()) {
      throw new IllegalArgumentException("Number of column names (${names.size()}) does not match number of columns (${columnCount()}) in this matrix")
    }
    mHeaders.clear()
    mHeaders.addAll(ListConverter.toStrings(names))
    return this
  }

  /**
   * Convert all columns to the type specified
   *
   * @param type the class to convert all values to
   * @param dateTimeFormatter an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(Class<T> type, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<Class<T>> types = [type]*columnCount()
    convert(types, dateTimeFormatter, numberFormat)
  }

  /**
   * Convert the columns in the order of a list to the classes specified
   * @param types a list of column types (classes)
   * @param dateTimeFormatter an optional DateTimeFormatter
   * @param numberFormat an optional NumberFormat
   * @return this Matrix converted as specified
   */
  Matrix convert(List<Class<?>> types, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    if (types.size() > columnCount()) {
      throw new IllegalArgumentException("There are more column types specified (${types.size()}) than there are columns in this matrix (${columnCount()})")
    }
    Map<String, Class<?>> columnTypeMap = [:]
    for (int i = 0; i < types.size(); i++) {
      columnTypeMap[columnName(i)] =types[i]
    }
    return convert(columnTypeMap, dateTimeFormatter, numberFormat)
  }

  Matrix convert(Map<String, Class<?>> types, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<List<?>> convertedColumns = []
    List<Class<?>> convertedTypes = []
    for (int i = 0; i < columnCount(); i++) {
      String colName = mHeaders[i]
      if (types[colName]) {
        convertedColumns.add(ListConverter.convert(
                column(i),
                types[colName] as Class<Object>,
                dateTimeFormatter,
                numberFormat)
        )
        convertedTypes.add(types[colName])
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(type(i))
      }
    }
    mColumns = new ArrayList<>(convertedColumns)
    mTypes = new ArrayList<>(convertedTypes)
    //def convertedRows = Grid.transpose(convertedColumns) as List<List<?>>
    //return new Matrix(mName, mHeaders, convertedColumns, convertedTypes)
    this
  }

  Matrix convert(String columnName, Class<?> type, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    return convert([(columnName): type], dateTimeFormatter, numberFormat)
  }

  Matrix convert(int columnIndex, Class<?> type, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    convert(columnName(columnIndex), type, dateTimeFormatter, numberFormat)
  }

  Matrix convert(String columnName, Class<?> type, Closure converter) {
    return convert(columnNames().indexOf(columnName), type, converter)
  }

  /**
   * As this clashes with convert(List<Class<?>>) we require an array as parameter. i.e.
   * You need to cast it with as Converter[] e.g.
   * <code>
   * convert([new Converter('id', Integer, {Integer.parseInt(it)})] as Converter[])
   * <code>
   *
   * @param converters an array of se.alipsa.groovy.matrix.Converter
   * @return this Matrix with the types and column values converted
   */
  Matrix convert(Converter[] converters) {
    List<List<?>> convertedColumns = []
    List<Class<?>> convertedTypes = []

    List<String> columnNameList = []
    List<Class<?>> columnTypeList = []
    List<Closure> converterList = []
    converters.each {
      columnNameList.add(it.columnName)
      columnTypeList.add(it.type)
      converterList.add(it.converter)
    }

    for (int i = 0; i < columnCount(); i++) {
      String colName = mHeaders[i]
      if (columnNameList.contains(colName)) {
        int index = columnNameList.indexOf(colName)
        def columnVals = []
        def converter = converterList[index]
        for (def val in column(colName)) {
          columnVals.add(converter.call(val))
        }
        convertedTypes.add(columnTypeList[index])
        convertedColumns.add(columnVals)
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(type(i))
      }
    }
    //def convertedRows = Grid.transpose(convertedColumns)
    //return builder().name(mName).columnNames(mHeaders).rows(convertedRows).types(convertedTypes).build()
    mColumns.clear()
    mColumns.addAll(convertedColumns)
    mTypes.clear()
    mTypes.addAll(convertedTypes)
    this
  }

  /**
   *
   * @param columnNumber the column index for the column to convert
   * @param columnType the class the column will be converted to
   * @param converter as closure converting each value in the designated column
   * @return a new Matrix
   */
  Matrix convert(int columnNumber, Class<?> columnType, Closure converter) {
    List<List<?>> convertedColumns = []
    List<Class<?>> convertedTypes = []
    def col = []
    for (int i = 0; i < columnCount(); i++) {
      if (columnNumber == i) {
        column(i).each { col.add(converter.call(it)) }
        convertedColumns.add(col)
        convertedTypes.add(columnType)
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(type(i))
      }
    }
    def convertedRows = Grid.transpose(convertedColumns)
    return builder().name(mName).columnNames(mHeaders).rows(convertedRows).types(convertedTypes).build()
  }

  Matrix convert(IntRange columns, Class<?> type, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    Map<String, Class<?>> map = [:]
    columns.each {
      map.put((columnName(it)), type)
    }
    convert(map, dateTimeFormatter, numberFormat)
  }

  int columnCount() {
    return mColumns.size()
  }

  int lastColumnIndex() {
    columnCount() -1
  }

  /**
   *
   * @param columnName the column name to match
   * @return the index (position) of the columnName
   */
  int columnIndex(String columnName) {
    return mHeaders.indexOf(columnName)
  }

  List<Integer> columnIndices(List<String> columnNames) {
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
      throw new IndexOutOfBoundsException("The column '$columnName' does not exist in this matrix (${this.mName}): " + e.getMessage())
    }
  }

  List<?> column(int index) {
    return mColumns.get(index)
  }

  List<List<?>> columns() {
    return mColumns
  }

  List<List<?>> columns(String[] columnNames) {
    return columns(columnNames as List<String>)
  }

  List<List<?>> columns(List<String> columnNames) {
    def cols = []
    for (String colName in columnNames) {
      cols.add(column(colName))
    }
    return cols as List<List<?>>
  }

  List<List<?>> columns(Integer[] indices) {
    def cols = []
    for (int index in indices) {
      cols.add(column(index))
    }
    return cols as List<List<?>>
  }

  List<List<?>> columns(IntRange range) {
    Integer[] arr = new Integer[range.size()]
    range.eachWithIndex { int entry, int i -> arr[i] = entry }
    columns(arr)
  }

  Class<?> type(int i) {
    return mTypes[i]
  }

  Class<?> type(String columnName) {
    return mTypes[columnIndex(columnName)]
  }

  List<Class<?>> types() {
    return mTypes
  }

  List<Class<?>> types(List<String> columnNames) {
    List<Class<?>> types = []
    for (name in columnNames) {
      types.add(type(name))
    }
    return types
  }

  List<Class<?>> types(IntRange range) {
    return mTypes[range]
  }

  List<String> typeNames() {
    List<String> types = new ArrayList<>()
    mTypes.each { types.add(it.getSimpleName()) }
    return types
  }

  String content(boolean includeHeader = true, boolean includeTitle = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    String title = ''
    if (includeTitle) {
      title = toString() + lineEnding
    }
    return title + head(rowCount(), includeHeader, delimiter, lineEnding, maxColumnLength)
  }

  String content(Map params) {
    boolean includeTitle = params.getOrDefault('includeTitle', true)
    boolean includeHeader = params.getOrDefault('includeHeader', true)
    String delimiter = params.getOrDefault('delimiter' , '\t')
    String lineEnding = params.getOrDefault('lineEnding', '\n')
    int maxColumnLength = (int)params.getOrDefault('maxColumnLength', 50)
    return content(includeHeader, includeTitle, delimiter, lineEnding, maxColumnLength)
  }

  static boolean containsValues(Iterable<?> row) {
    def strVal
    for (def element in row) {
      strVal = String.valueOf(element)
      if (element != null && strVal != 'null' && !strVal.isBlank() ) return true
    }
    return false
  }

  @Override
  Matrix clone() {
    return new Matrix(mName, mHeaders, mColumns, mTypes)
  }

  private List<Class<?>> createTypeListWithNewValue(int columnNumber, Class<?> updatedClass, boolean findCommonGround) {
    List<Class<?>> types = []
    for (int i = 0; i < mHeaders.size(); i++) {
      if (i == columnNumber) {
        if (findCommonGround) {
          types.add(findClosestCommonSuper(updatedClass, type(columnNumber)))
        } else {
          types.add(updatedClass)
        }
      } else {
        types.add(type(i))
      }
    }
    return types
  }

  String diff(Matrix other, boolean forceRowComparing = false, double allowedDiff = 0.0001) {
    StringBuilder sb = new StringBuilder()
    if (this.name != other.name) {
      sb.append("Names differ: \n\tthis: ${name} \n\tthat: ${other.name}\n")
    }
    if (this.columnNames() != other.columnNames()) {
      sb.append("Column Names differ: \n\tthis: ${columnNames().join(', ')} \n\tthat: ${other.columnNames().join(', ')}\n")
    }
    if (this.columnCount() != other.columnCount()) {
      sb.append("Number of columns differ: this: ${this.columnCount()}; that: ${other.columnCount()}\n")
    }
    if (this.rowCount() != other.rowCount()) {
      sb.append("Number of rows differ: this: ${this.rowCount()}; that: ${other.rowCount()}\n")
    }
    if (this.types() != other.types()) {
      sb.append("Column types differ: \n\tthis: ${typeNames().join(', ')} \n\tthat: ${other.typeNames().join(', ')}")
    }
    if (sb.length() == 0 || forceRowComparing) {
      def thisRow, thatRow
      for (int i = 0; i < rowCount(); i++) {
        thisRow = row(i)
        thatRow = other.row(i)
        boolean valueDiff = false
        thisRow.eachWithIndex { Object entry, int c ->
          if (entry instanceof Number) {
            def thatVal = (thatRow[c] ?: Double.NaN) as double
            double thisVal = entry as double
            if (Math.abs(thisVal - thatVal as double) > allowedDiff) {
              valueDiff = true
            }
          } else if (entry != thatRow[c]) {
            valueDiff = true
          }
        }
        if (valueDiff) {
          sb.append("Row ${i} differs: \n\tthis: ${thisRow.join(', ')} \n\tthat: ${thatRow.join(', ')}\n")
        }
      }
    }
    if (sb.length() > 0) {
      return sb.toString()
    } else {
      return 'No differences between the two matrices detected!'
    }
  }

  Matrix dropColumnsExcept(String... columnNames) {
    def retainColNames = columnNames.length > 0 ? columnNames as List : []
    for (String columnName : mHeaders.collect()) {
      if (retainColNames.contains(columnName)) {
        continue
      }
      def colIdx = columnIndex(columnName)
      mColumns.remove(colIdx)
      mTypes.remove(colIdx)
      mHeaders.remove(colIdx)
    }
    return this
  }

  Matrix dropColumnsExcept(int... columnIndices) {
    def retainColIndices = columnIndices.length > 0 ? columnIndices as List : []
    for (int i = 0; i < columnCount(); i++) {
      if (retainColIndices.contains(i)) {
        continue
      }
      mColumns.remove(i)
      mTypes.remove(i)
      mHeaders.remove(i)
    }
    return this
  }

  Matrix dropColumns(String... columnNames) {
    if (columnNames.length == 0) {
      println "no variables to drop specified, nothing to do"
      return this
    }
    List<Integer> idxs = columnIndices(columnNames as List<String>)
    if (idxs.contains(-1)) {
      throw new IllegalArgumentException("Variables ${String.join(',', columnNames)} does not match actual column names: ${String.join(',', mHeaders)}")
    }
    dropColumns(idxs)
  }

  Matrix dropColumns(IntRange columnIndices) {
    dropColumns(columnIndices.toList())
  }

  Matrix dropColumns(List<Integer> columnIndices) {
    Collections.sort(columnIndices)
    columnIndices.eachWithIndex { colIdx, idx ->
      // Each time we iterate and remove, all of the below will have one less item
      // so we need to adjust the colIdx to still match
      mColumns.remove(colIdx - idx)
      mTypes.remove(colIdx - idx)
      mHeaders.remove(colIdx - idx)
    }
    return this
  }

  Matrix dropColumns(int... columnIndices) {
    def columnsToDrop = columnIndices.length > 0 ? columnIndices as List<Integer> : []
    dropColumns(columnsToDrop)
  }

  boolean equals(Object o, boolean ignoreColumnNames = false, boolean ignoreName = false, boolean ignoreTypes = true, Double allowedDiff = 0.0001) {
    if (this.is(o)) return true
    if (!(o instanceof Matrix)) return false

    Matrix matrix = (Matrix) o

    if (!ignoreColumnNames && mHeaders != matrix.mHeaders) return false
    if (!ignoreName && mName != matrix.mName) return false
    if (!ignoreTypes && mTypes != matrix.mTypes) return false
    //if (mColumns != matrix.mColumns) return false
    boolean valueDiff = false
    mColumns.eachWithIndex { List<?> column, int i ->
      def thatCol = matrix.column(i)
      column.eachWithIndex { Object entry, int r ->
        def thatVal = thatCol[r]
        if (entry instanceof Number) {
          def diff = Math.abs((entry as double) - ((thatVal ?: Double.NaN) as double))
          if (diff > allowedDiff) {
            valueDiff = true
          }
        } else {
          if (entry != thatVal) {
            valueDiff = true
          }
        }
      }
    }
    if (valueDiff) return false

    return true
  }

  /**
   * Searches for the first occurance of the value in the column specified
   *
   * @param columnName the name of the column to search
   * @param value the value to search for
   * @return the first row where the column matches the value or null of no match found
   */
  List<?> findFirstRow(String columnName, Object value) {
    int rowIndex = column(columnName).findIndexOf {it == value}
    return rowIndex >= 0 ? row(rowIndex) : null
  }

  Object get(int row, int column) {
    return mColumns[column][row]
  }

  String getName(){
    return mName
  }

  <T> T getAt(Integer row, String columnName) {
    Class<T> type = type(columnName) as Class<T>
    Integer columnIdx = columnIndex(columnName)
    return get(row, columnIdx).asType(type)
  }

  /**
   * Convenience method with built in type conversion
   * A LocalDate variable with the value LocalDate.of(2021, 12, 1)
   * in the first row, second column will have
   * assert '2021-12-01' == table[0, 1, String]. The ValueConverter is used
   * to convert the type.
   *
   * @param row the observation to get
   * @param column the variable to get
   * @param type the class that the the value should be converted to
   * @return a value of the type specified
   */
  <T> T getAt(int row, int column, Class<T> type) {
    return ValueConverter.convert(get(row, column), type)
  }

  /**
   * Convenience method with built in type conversion
   * A LocalDate variable with the value LocalDate.of(2021, 12, 1)
   * in the first row, second column will have
   * assert '2021-12-01' == table[0, 1, String]. The ValueConverter is used
   * to convert the type.
   *
   * @param row the observation to get
   * @param columnName the variable to get
   * @param type the class that the the value should be converted to
   * @return a value of the type specified
   */
  <T> T getAt(int row, String columnName, Class<T> type) {
    return ValueConverter.convert(get(row, columnIndex(columnName)), type)
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[0, 1] for the 2:nd column of the first observation
   * @return the value corresponding to the row and column indexes supplied
   */
  <T> T getAt(Integer row, Integer column) {
    Class<T> type = type(column) as Class<T>
    if (type != null) {
      return get(row, column).asType(type)
    }
    return get(row, column) as T
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[0, "salary"] for the salary for the first observation
   * @return the value corresponding to the row and column name supplied
   */
  <T> T getAt(int row, String columnName) {
    Class<T> type = type(columnName) as Class<T>
    return get(row, columnIndex(columnName)).asType(type)
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table["salary"] for the salary column
   *
   * @return the column corresponding to the column name supplied
   */
  List<?> getAt(String columnName) {
    return column(columnName)
  }

  /**
   * Enable the use of square bracket to reference a range of columns, e.g. table[0..2] for the first 3 columns
   *
   * @return the column corresponding to the column name supplied
   */
  List<List<?>> getAt(IntRange range) {
    mColumns[range]
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table["salary", BigDecimal] for the salary column
   * converted to the type specified (BigDecimal in this case)
   *
   * @return the column corresponding to the column name supplied converted to the type (using the ListConverter)
   */
  <T> List<T> getAt(String columnName, Class<T> type) {
    ListConverter.convert(column(columnName), type)
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[2] for the 3:rd column
   *
   * @return the column corresponding to the column index supplied
   */
  List<?> getAt(int columnIndex) {
    return column(columnIndex)
  }

  /**
   * get the variables in the range specified for a specific row
   * <code><pre>
   * def table = Matrix.builder()
   *  .columns(
   *    'firstname': ['Lorena', 'Marianne', 'Lotte'],
   *    'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
   *    'foo': [1, 2, 3])
   *  .types(String, LocalDate, int)
   *  .build()
   *
   * assert [asLocalDate('2021-12-01'), 1] == table[0, 1..2]
   * </pre></code>
   * @param rowIndex the observation to get
   * @param columns the variables to from the observation to include in the result
   * @return the variables (columns) in the range specified for a specific row
   */
  List<?> getAt(int rowIndex, IntRange columns) {
    row(rowIndex)[columns]
  }

  /**
   * Get the observations in the range specified for a specific column
   * <code><pre>
   * def table = Matrix.builder()
   *  .columns(
   *    'firstname': ['Lorena', 'Marianne', 'Lotte'],
   *    'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
   *    'foo': [1, 2, 3])
   *  .types(String, LocalDate, int)
   *  .build()
   *
   * assert ['Marianne', 'Lotte'] == table[1..2, 0]
   * </pre></code>
   *
   * @param rows
   * @param colIndex
   * @return a number of observations (rows) of the variable (column) specified
   */
  List<?> getAt(IntRange rows, int colIndex) {
    column(colIndex)[rows]
  }

  /**
   * This method is primarily meant for human viewíng of the content of the matrix
   *
   * @param rows the number of observations to include in the result
   * @param includeHeader optional, defaults to false
   * @param delimiter optional defaults to tab (\t)
   * @param lineEnding optional defaults to newline (\n)
   * @param maxColumnLength optional defaults to 50
   * @return a string representation of each observation up to the rows specified
   */
  String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, rowCount())
    List<Integer> columnLengths = mHeaders.collect { colName -> maxContentLength(colName, includeHeader, maxColumnLength)}

    if (includeHeader) {
      List<String> headerRow = padRow(mHeaders, columnLengths)
      sb.append(String.join(delimiter, headerRow)).append(lineEnding)
    }

    for (int i = 0; i < nRows; i++) {
      List<String> stringRow = padRow(row(i), columnLengths)
      sb.append(String.join(delimiter, stringRow)).append(lineEnding)
    }
    return sb.toString()
  }

  @Override
  int hashCode() {
    int result
    result = (mHeaders != null ? mHeaders.hashCode() : 0)
    result = 31 * result + (mTypes != null ? mTypes.hashCode() : 0)
    result = 31 * result + (mName != null ? mName.hashCode() : 0)
    return result
  }

  /**f
   * Convert this table into a Grid
   * @return a Grid corresponding to the data content of this table
   */
  Grid<Object> grid() {
    return new Grid<Object>(rows() as List<List<Object>>)
  }

  /**f
   * Convert this table into a Grid
   * @return a Grid corresponding to the data content of this table
   */
  <T> Grid<T> grid(Class<T> type, boolean convertValues = false) {
    List<List<T>> r
    if (convertValues) {
      r = convert(type).rows() as List<List<T>>
    } else {
      r = rows() as List<List<T>>
    }
    return new Grid<T>(r)
  }

  /**
   * Allows you to iterate over the rows in this Matrix
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
   * @return an Iterator iterating over the rows (observations) in this matrix
   */
  @Override
  Iterator<Row> iterator() {
    return rows().iterator()
    //return new RowIterator(this)
  }

  /**
   * returns the longest content (number of characters) of the column specified
   *
   * @param columnName
   * @param includeHeader
   * @param maxColumnLength
   * @return
   */
  int maxContentLength(String columnName, boolean includeHeader, int maxColumnLength = 50) {
    int columnNameLength = columnName == null ? 0 : columnName.length()
    Integer maxLength = Math.min(includeHeader ? columnNameLength : 0, maxColumnLength)
    Integer length
    column(columnName).each {
      length = String.valueOf(it).length()
      if (length > maxLength) {
        if (length >= maxColumnLength) {
          maxLength = maxColumnLength
        } else {
          maxLength = length
        }
      }
    }
    maxLength
  }

  /**
   * move a row from one position to another
   *
   * @param from the index of the row to move
   * @param to the index where the rwo should be inserted
   * @return this Matrix reorganized as specified
   */
  Matrix moveRow(int from, int to) {
    mColumns.each {
      it.add(to, it.remove(from))
    }
    this
  }

  /**
   * Move the named column to the index specified
   *
   * @param columnName the name of the volumn to move
   * @param index the index to move it to
   * @return this (mutated) matrix
   */
  Matrix moveColumn(String columnName, int index) {
    Class<?> type = type(columnName)
    int currentIndex = columnIndex(columnName)
    List<?> col = mColumns[currentIndex]
    mColumns.remove(currentIndex)
    mColumns.add(index, col)
    mHeaders.remove(currentIndex)
    mHeaders.add(index, columnName)
    mTypes.remove(currentIndex)
    mTypes.add(index, type)
    return this
  }

  /**
   * Sort this table in ascending order by the columns specified
   *
   * @param columnNames the columns to sort by
   * @return this table (mutated), sorted in ascending order by the columns specified
   */
  Matrix orderBy(List<String> columnNames) {
    LinkedHashMap<String, Boolean> columnsAndDirection = [:]
    for (colName in columnNames) {
      columnsAndDirection[colName] = ASC
    }
    return orderBy(columnsAndDirection)
  }

  /**
   * Sort this table in the order specified by the columns specified
   *
   * @param columnNames the columns to sort by
   * @return this table (mutated), sorted in the order specified by the columns specified
   */
  Matrix orderBy(String columnName, Boolean descending = Boolean.FALSE) {
    if (columnName !in columnNames()) {
      throw new IllegalArgumentException("The column name ${columnName} does not exist is this table (${mName})")
    }
    def comparator = new RowComparator(columnIndex(columnName))
    // copy all the rows
    List<Row> rows = this.rows()
    Collections.sort(rows, comparator)
    if (descending) {
      Collections.reverse(rows)
    }
    updateValues(rows)
    return this
    //return create(mName, mHeaders, rows as List<List<?>>, mTypes)
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
    List<Row> rows = this.rows()
    Collections.sort(rows, comparator)
    updateValues(rows)
    return this
    //return create(mName, mHeaders, rows as List<List<?>>, mTypes)
  }

  /**
   * Used to pretty print (e.g. to console) a row
   */
  List<String> padRow(List row, List<Integer> columnLengths) {
    List<String> stringRow = []
    for (int c = 0; c < mHeaders.size(); c++) {
      def val = row[c]
      def strVal = String.valueOf(val)
      int columnLength = columnLengths[c]
      if (strVal.length() > columnLength) {
        strVal = strVal.substring(0, columnLength)
      }
      def valType = type(c)
      if (val instanceof Number || (valType != null && Number.isAssignableFrom(valType))) {
        strVal = strVal.padLeft(columnLength)
      } else {
        strVal = strVal.padRight(columnLength)
      }
      stringRow << strVal
    }
    stringRow
  }

  /**
   * Append a row to this matrix
   *
   * @param row
   * @return this matrix
   */
  Matrix plus(List row) {
    addRow(row)
  }

  /**
   * Append all rows from the matrix to this one
   *
   * @param table the matrix to append from
   * @return this matrix
   */
  Matrix plus(Matrix table) {
    addRows(table.rowList())
  }

  Matrix appendValues(String columnName, List<?> values) {
    column(columnName).addAll(values)
    this
  }

  /**
   * Allows for "short form" manipulation of values e.g:
   * myMatrix[1,2] = 42
   * myMatrix[1,"Foo"] = 42
   */
  void putAt(List<?> where, Object value) {
    def row = where[0]
    def col = where[1]
    if (! row instanceof Number) {
      throw new IllegalArgumentException("Unexpected row index type: ${row.class}, dont know what to do with that")
    }
    if (col instanceof String) {
      putAt(row as Number, col as String, value)
    } else if (col instanceof Number) {
      putAt(row as Number, col as Number, value)
    } else {
      throw new IllegalArgumentException("Unexpected column type: ${col.class}, dont know what to do with that")
    }
  }

  void putAt(Number rowIndex, Number colIndex, Object value) {
    def column = mColumns[colIndex.intValue()]
    if (rowIndex < column.size()) {
      column.set(rowIndex.intValue(), value)
    } else if (rowIndex == column.size()) {
      column.add(value)
    } else {
      throw new IndexOutOfBoundsException("Row index ($rowIndex) is outside the size of the column ${column.size()}")
    }
  }

  void putAt(Number rowIndex, String colName, Object value) {
    putAt(rowIndex, columnIndex(colName), value)
  }

  /**
   * Allows for "short form" manipulation of columns e.g:
   * myMatrix["yearMonth", YearMonth] = ListConverter.toYearMonth(myMatrix["start_date"])
   * If the column exists, the data will be replaced, otherwise a new column will be appended
   *
   * @param where a List of columnName (the name of the column to replace or add) and
   * type the class of the column data
   * @param column a list of the column values
   */
  void putAt(List where, List<?> column) {
    if (where.size() < 2) {
      throw new IllegalArgumentException("Insufficient number of arguments, specify at least columnName, type and the list of values")
    }
    if (where.size() == 2) {
      putAt(where[0] as String, where[1] as Class<?>, column)
    } else if (where.size() == 3) {
      putAt(where[0] as String, where[1] as Class<?>, where[2] as Integer, column)
    } else {
      throw new IllegalArgumentException("Too many arguments ${where.size()}, putAt understands maximim 4")
    }
  }


  // Groovy semantics requires this to mutate, i.e. this does not work
  // Matrix t2 = (table["yearMonth", YearMonth, 0] = toYearMonth(table["start_date"]))
  // thus, we return void to make that fact obvious
  void putAt(String columnName, Class<?> type, Integer index = null, List<?> column) {
    if(rowCount() != column.size()) {
      throw new IllegalArgumentException("Number of column values (${column.size()}) does not match number of rows (${rowCount()}) in this matrix")
    }
    if (columnNames().contains(columnName)) {
      replace(columnName, type, column)
    } else {
      if (index == null) {
        mColumns << column
        mHeaders << columnName
        mTypes << type
      } else {
        mColumns.add(index, column)
        mHeaders.add(index, columnName)
        mTypes.add(index, type)
      }
    }
  }

  void putAt(String columnName, List<?> values) {
    putAt(columnIndex(columnName), values)
  }

  void putAt(Integer column, List<?> values) {
    def col = this.column(column)
    if (values.size() != col.size() && !col.isEmpty()) {
      // We only allow putAt when the row is empty or number of rows equals number of values
      throw new IllegalArgumentException("The list of values is not the same as the number of rows")
    }
    col.clear()
    col.addAll(values)
  }

  /**
   * myMatrix[0..2] = otherMatrix[1..3]
   *
   * @param range
   * @param List
   */
  void putAt(IntRange range, List<List<?>> columns) {
    if (range.size() != columns.size()) {
      throw new IllegalArgumentException("Size of range (${range.size()}) must be equal to the number of columns (${columns.size()})")
    }
    range.eachWithIndex { int it, int idx ->
      putAt(it, columns[idx])
    }
  }

  def leftShift(List row) {
    addRow(row)
  }

  def leftShift(List column, Object value) {
    column.add(value)
  }

  /**
   * renames the column and returns the table to allow for chaining
   *
   * @param before the existing column name
   * @param after the new column name
   * @return the (mutated) table to allow for chaining
   */
  Matrix renameColumn(String before, String after) {
    renameColumn(columnIndex(before), after)
    this
  }

  /**
   * renames the column and returns the table to allow for chaining
   *
   * @param columnIndex the index of the column to rename
   * @param after the new column name
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix renameColumn(int columnIndex, String after) {
    mHeaders.set(columnIndex, after)
    this
  }

  /**
   * Replace all values in the entire matrix that matches the value
   *
   * @param from the value to search for
   * @param to the value to replace with
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix replace(Object from, Object to) {
    columns().each {
      Collections.replaceAll(it as List<Object>, from, to)
    }
    this
  }

  /**
   * Replace all values in the specified column that matches the value
   *
   * @param columnName the name of the column to replace values in
   * @param from the value to search for
   * @param to the value to replace with
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix replace(String columnName, Object from, Object to) {
    Collections.replaceAll(column(columnName) as List<Object>, from, to)
    this
  }

  /**
   * Replace all values in the specified column that matches the value
   *
   * @param columnIndex the index of the column to replace values in
   * @param from the value to search for
   * @param to the value to replace with
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix replace(int columnIndex, Object from, Object to) {
    Collections.replaceAll(column(columnIndex) as List<Object>, from, to)
    this
  }


  /**
   * Replace the values in the column with the specified ones
   *
   * @param columnName the name of the column to replace values in
   * @param type the type of the new column
   * @param values the values to replace the old one with
   * @return this matrix (mutated) to allow for method chaining
   * @throws IllegalArgumentException if the column provided is null or of different size than the previous one
   */
  Matrix replace(String columnName, Class<?> type = Object, List<?> values) {
    def col = column(columnName)
    if (values == null) {
      throw new IllegalArgumentException('The list of values cannot be null')
    }
    if (col.size() != values.size()) {
      throw new IllegalArgumentException("The size of the column is ${col.size()} but values only contains ${values.size()} elements")
    }
    col.clear()
    col.addAll(values)
    mTypes.set(columnIndex(columnName), type)
    this
  }

  /**
   * Remove the row indexes specified from this Matrix.
   * This is the mutating equivalent to subset.
   *
   * @param indexes the row indexes to remove
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix removeRows(int... indexes) {
    removeRows(indexes as List<Integer>)
  }

  /**
   * Remove the row indexes specified from this Matrix.
   * This is the mutating equivalent to subset.
   *
   * @param indexes the row indexes to remove
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix removeRows(List<Integer> indexes) {
    Arrays.sort(indexes)
    mColumns.each { col ->
      indexes.eachWithIndex { Number idx, int count ->
        col.remove((int)idx - count)
      }
    }
    this
  }

  /**
   * Remove all rows with only null values and/or blank strings
   *
   * @return this matrix with the empty rows removed
   */
  Matrix removeEmptyRows() {
    removeRows(selectRowIndices { !containsValues(it as Iterable<?>) })
  }

  /**
   * Remove all columns with only null values and/or blank strings
   *
   * @return this matrix with the empty columns removed
   */
  Matrix removeEmptyColumns() {
    int i = 0
    List<Integer> columnsToRemove = []
    for (List<?> col in mColumns) {
      boolean hasVal = false
      for (def v in col) {
        if (v != null) {
          hasVal = true
          break
        }
      }
      if (!hasVal) {
        columnsToRemove.add( i as Integer)
      }
      i++
    }
    //println "Removing columns $columnsToRemove"
    dropColumns(columnsToRemove)
  }

  /**
   * Get the row at the specified index
   *
   * @param index the index of the row
   * @return the row (@see Row) corresponding to the row index (starting with 0)
   */
  Row row(int index) {
    def data = []
    mColumns.each { col ->
      data << col[index]
    }
    return new Row(index, data, this)
  }

  /**
   * The Matrix should be uniform for this to work properly since
   * it returns the number of elements in the first column as the
   * row count.
   *
   * @return the number of rows in this matrix
   */
  int rowCount() {
    if (mColumns == null || mColumns.isEmpty()) {
      return 0
    }
    return mColumns.get(0).size()
    //return rows().size()
  }

  /**
   *
   * @return the index value of the last observation
   */
  int lastRowIndex() {
    rowCount() -1
  }

  /**
   *
   * @return a detached list of rows where each row is a list of objects
   */
  List<List<Object>> rowList() {
    return mColumns.transpose()
  }

  List<Row> rows(List<Integer> index) {
    List<Row> rows = []
    index.each {
      rows.add(row((int) it))
    }
    return rows
  }

  /**
   * Finding rows based on some criterie e.g:
   * <code>
   * def r = table.rows { row ->
   *   row['place'] == 2
   * }
   * </code>
   *
   * @param criteria a Closure returning true if the row should be included in the result or not.
   * @return a List of Rows that matched the criteria
   */
  List<Row> rows(Closure criteria) {
    List<Row> r = []
    this.each { row->
      if (criteria(row)) {
        r.add(row as Row)
      }
    }
    return r
  }

  /**
   *
   * @return a list of all the rows in this Matrix.
   * Changes to values in a row is reflected back to the Matrix.
   */
  List<Row> rows() {
    if (mColumns.isEmpty()) {
      return Collections.emptyList()
    }
    int nRows = mColumns[0].size()
    Map<Integer, Row> r = [:]
    for (Integer i = 0; i < nRows; i++) {
      r[i] = new Row(i, this)
    }
    mColumns.eachWithIndex { List<?> column, int col ->
      for (Integer row = 0; row < nRows; row++) {
        r.get(row).addElement(column[row])
      }
    }
    return r.values() as List
  }

  private static List<Class<?>> sanitizeTypes(Collection<String> headerList, List<Class<?>>... dataTypesOpt) {
    List<Class<?>> types = []
    if (dataTypesOpt.length > 0) {
      types = convertPrimitivesToWrapper(dataTypesOpt[0])
      if (types.isEmpty()) {
        types = createObjectTypes(headerList)
      }
      if (headerList.size() != types.size()) {
        println "Headers (${headerList.size()} elements): $headerList"
        println "Types:  (${types.size()} elements): ${types.collect{ it.simpleName}}"
        throw new IllegalArgumentException("Number of columns (${headerList.size()}) differs from number of datatypes provided (${types.size()})")
      }
    }
    if (types.isEmpty()) {
      types = createObjectTypes(headerList)
    }
    return types
  }

  Matrix selectColumns(String... columnNames) {
    List<List<?>> cols = []
    List<String> colNames = []
    List<Class<?>> dataTypes = []
    for (name in columnNames) {
      cols.add(column(name))
      colNames.add(name)
      dataTypes.add(type(name))
    }
    return builder()
        .name(mName)
        .columnNames(colNames)
        .columns(cols)
        .types(dataTypes)
        .build()
  }

  /**
   *
   * @param criteria takes a row (List<?>) as parameter and returns true if the row should be included
   * @return a List of row indexes matching the criteria supplied
   */
  List<Integer> selectRowIndices(Closure criteria) {
    def r = [] as List<Integer>
    rows().eachWithIndex { row, idx -> {
        if (criteria(row)) {
          r.add(idx)
        }
      }
    }
    return r
  }

  void setName(String name) {
    mName = name
  }

  /**
   * split is used t create a map of matrices for each unique value in the column.
   * This is useful for e.g. countBy or sumBy (see Stat.countBy() and Stat.countBy())
   *
   * @param columnName
   * @return a map of matrices for each unique value in the column where the key is the value
   */
  Map<?, Matrix> split(String columnName) {
    List<?> col = column(columnName)
    Map<Object, List<Integer>> groups = new HashMap<>()
    for (int i = 0; i < col.size(); i++) {
      groups.computeIfAbsent(col[i], k -> []).add(i)
    }
    Map<?, Matrix> tables = [:]
    for (entry in groups) {
      tables.put(entry.key,
          builder()
              .name(String.valueOf(entry.key))
              .columnNames(mHeaders)
              .rows(rows(entry.value) as List<List<?>>)
              .types(mTypes)
              .build()
          )
    }
    return tables
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
   *
   *  The mutable version of this is removeRows.
   *
   * @param columnName the name of the column to use to select matches
   * @param condition a closure containing the condition for what to retain
   * @return the subset of the matrix (a new Matrix, the original one is unaffected)
   */
  Matrix subset(@NotNull String columnName, @NotNull Closure<Boolean> condition) {
    List<Integer> r = column(columnName).findIndexValues(condition) as List<Integer>
    builder()
        .rows(this.rows(r) as List<List<?>>)
        .name(this.name)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   *
   * @param criteria takes a row (List<?>) as parameter and returns true if the row should be included
   * @return this Matrix with the rows matching the criteria supplied retained
   */
  Matrix subset(Closure<Boolean> criteria) {
    builder()
        .rows(rows(criteria) as List<List<?>>)
        .name(this.name)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  Matrix subset(IntRange rows) {
    builder()
        .rows(this.rows(rows) as List<List<?>>)
        .name(this.name)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, rowCount())
    if (includeHeader) {
      sb.append(String.join(delimiter, mHeaders)).append(lineEnding)
    }
    List<Integer> columnLengths = mHeaders.collect { colName -> maxContentLength(colName, includeHeader, maxColumnLength)}
    for (int i = rowCount() - nRows; i < rowCount(); i++) {
      //def row = ListConverter.convert(row(i), String.class)
      List<String> stringRow = padRow(row(i), columnLengths)
      sb.append(String.join(delimiter, stringRow)).append(lineEnding)
    }
    return sb.toString()
  }


  @Override
  String toString() {
    return "$name: ${rowCount()} obs * ${columnCount()} variables "
  }

  Map<String, Integer> dimensions() {
    ['observations': rowCount(), 'variables': columnCount()]
  }

  /**
   * @param attr table attributes e.g. id, class etc. that is added immediately after the table and become table
   *  attributes when rendered as html
   * @return a markdown formatted table where all the values have been converted to strings, numbers are right aligned
   * and everything else default (left) aligned
   */
  String toMarkdown(Map<String, String> attr = [:]) {
    List<String> alignment = []
    for (type in types()) {
      if (Number.isAssignableFrom(type)) {
        alignment.add('---:')
      } else {
        alignment.add('---')
      }
    }
    //mHeaders.collect(w -> w.replaceAll(".", "-"))
    return toMarkdown(alignment, attr)
  }

  /**
   *
   * @param alignment use :--- for left align, :----: for centered, and ---: for right alignment
   * @return a markdown formatted table where all the values have been converted to strings
   */
  String toMarkdown(List<String> alignment, Map<String, String> attr = [:]) {
    if (alignment.size() != columnCount()) {
      throw new IllegalArgumentException("number of alignment markers (${alignment.size()}) differs from number of columns (${columnCount()})")
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

  /**
   *
   * @param columnNameAsHeader
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(String columnNameAsHeader,  boolean includeHeaderAsRow = false) {
    List<String> header
    if (includeHeaderAsRow) {
      header = ['']
      header.addAll(ListConverter.toStrings(column(columnNameAsHeader)))
    } else {
      header = ListConverter.toStrings(column(columnNameAsHeader))
    }
    return transpose(header, createObjectTypes(header), includeHeaderAsRow)
  }

  /**
   *
   * @param columnNameAsHeader
   * @param dataTypes
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(String columnNameAsHeader, List<Class> dataTypes,  boolean includeHeaderAsRow = false) {
    List<String> header
    List<List<?>> r = new ArrayList<>(rowCount()+1)
    if (includeHeaderAsRow) {
      header = ['']
      header.addAll(ListConverter.toStrings(column(columnNameAsHeader)))
      r.add(mHeaders)
    } else {
      header = ListConverter.toStrings(column(columnNameAsHeader))
    }
    r.addAll(rows())
    builder()
        .name(mName)
        .columnNames(header)
        .columns(r)
        .types(dataTypes)
        .build()
  }

  /**
   *
   * @param header
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(List<String> header, boolean includeHeaderAsRow = false) {
    return transpose(header, createObjectTypes(header), includeHeaderAsRow)
  }

  /**
   *
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(boolean includeHeaderAsRow = false) {
    def numCols = includeHeaderAsRow ? rowCount() + 1 : rowCount()

    return transpose((1..numCols).collect{'c' + it}, includeHeaderAsRow)
  }

  /**
   *
   * @param header
   * @param dataTypes
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(List<String> header, List<Class> dataTypes, boolean includeHeaderAsRow = false) {
    List<List<?>> rows = new ArrayList<>(rowCount()+1)
    if (includeHeaderAsRow) {
      rows.add(mHeaders)
    }
    rows.addAll(mColumns.transpose() as List<List<?>>)
    builder()
      .name(mName)
      .columnNames(header)
      .columns(rows)
      .types(dataTypes)
      .build()
  }

  private void updateValues(List<Row> rows) {
    rows.eachWithIndex{ Row row, int r ->
      row.eachWithIndex { Object value, int c ->
        mColumns[c].set(r, value)
      }
    }
  }

  /**
   * Easy way to calculate and create a new column based on existing data.
   *
   * assuming two columns as follows:
   *  a: [1,2,3,4,5],
   *  b: [1.2,2.3,0.7,1.3,1.9]
   * The equivalent to
   * <code>
   * def m = []
   * columns(['a','b'].transpose().each {
   *  x, y -> m << x - y
   * }
   * </code>
   * ... can be done as follows:
   * <code>
   * def m = withColumns(['a', 'b']) { x, y -> x - y }
   * </code>
   * which will result in
   * <code>
   * [-0.2, -0.3, 2.3, 2.7, 3.1]
   * </code>
   *
   * @param colNames the names of the columns to include
   * @param operation the closure operation doing the calculation
   * @return a list with the result of the operations
   */
  List<?> withColumns(List<String> colNames, Closure operation) {
    def result = []
    columns(colNames).transpose().each {
      result << operation.call(it)
    }
    return result
  }

  /**
   * @see #withColumns(List<String>, Closure)
   * @param colIndices the column indices to include
   * @param operation the closure operation doing the calculation
   * @return a list with the result of the operations
   */
  List<?> withColumns(Number[] colIndices, Closure operation) {
    return withColumns(colIndices as int[], operation)
  }

  /**
   * @see #withColumns(List<String>, Closure)
   * @param colIndices the column indices to include
   * @param operation the closure operation doing the calculation
   * @return a list (a new column) with the result of the operations
   */
  List<?> withColumns(int[] colIndices, Closure operation) {
    def result = []
    columns(colIndices as Integer[]).transpose().each {
      result << operation.call(it)
    }
    return result
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

  static List<Class<?>> createObjectTypes(Collection template) {
    [Object]*template.size() as List<Class<?>>
  }

  static List<List<?>> createEmptyColumns(Collection template) {
    List<List<?>> columns = []
    for (int i = 0; i < template.size(); i++) {
      columns << []
    }
    columns
  }
}
