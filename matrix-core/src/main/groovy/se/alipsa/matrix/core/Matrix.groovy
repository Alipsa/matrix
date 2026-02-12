package se.alipsa.matrix.core

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import se.alipsa.matrix.core.util.ClipboardUtil
import se.alipsa.matrix.core.util.MatrixPrinter
import se.alipsa.matrix.core.util.RowComparator

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.core.util.ClassUtils.*

/**
 * A Matrix is a 2 dimensional data structure.
 * It is essentially a Grid i.e. [][] (List<List>) but also has a header and a name
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
 * Similarly, you use the same notation to assign / change values e.g.
 * <code>myMatrix[0,1] = 23</code> to assign the value 23 to the second variable of the first observation
 * or to assign / create a column <code> myMatrix['bar'] = [1..12]</code> to assign the range 1 to 12 to the column bar
 *
 */
@CompileStatic
class Matrix implements Iterable<Row>, Cloneable {

  private List<Column> mColumns
  private String mName
  public static final Boolean ASC = Boolean.FALSE
  public static final Boolean DESC = Boolean.TRUE
  private Map<String, ?> metaData = [:]

  static {
    // Splits "5.0.1" into [5, 0, 1]
    def versionParts = GroovySystem.version.tokenize('.')*.toInteger()
    def majorVersion = versionParts[0]

    if (majorVersion < 5) {
      throw new IllegalStateException(
          "Unsupported Groovy version! This library requires Groovy 5.0.0 or higher. " +
              "Current version: ${GroovySystem.version}"
      )
    }
  }

  static MatrixBuilder builder() {
    new MatrixBuilder()
  }

  static MatrixBuilder builder(String matrixName) {
    new MatrixBuilder(matrixName)
  }

  static MatrixBuilder builder(Map<String, List> columnData, List<Class> types, String name = null) {
    new MatrixBuilder(name).columns(columnData).types(types)
  }

  static List<String> anonymousHeader(List row) {
    (1..row.size()).collect { 'c' + it }
  }

  static List<String> anonymousHeader(int ncols) {
    (1..ncols).collect { 'c' + it }
  }

  /**
   * Creates a new Matrix
   *
   * @param name the name of the Matrix
   * @param headerList the variable (column) names
   * @param columns the variable list (the data in columnar format)
   * @param dataTypes the data types (classes)
   */
  Matrix(String name, List<String> headerList, List<List> columns, List<Class> dataTypes) {
    if (dataTypes != null && headerList == null) {
      headerList = anonymousHeader(dataTypes)
    }

    if (columns == null) {
      int ncol = headerList == null ? (dataTypes == null ? 0 : dataTypes.size()) : headerList.size()
      columns = []
      for (int i = 0; i < ncol; i++) {
        columns << new Column()
      }
    }

    if (columns != null && headerList == null) {
      if (columns.isEmpty()) {
        headerList = []
      } else {
        headerList = anonymousHeader(columns)
      }
    }

    dataTypes = dataTypes ?: []

    if (dataTypes.size() > columns.size()) {
      for (int i = 0; i < dataTypes.size() - columns.size(); i++) {
        columns.add([])
      }
    }

    mName = name
    def mTypes = sanitizeTypes(headerList, dataTypes)
    def mHeaders = headerList.collect { String.valueOf(it) }
    if (mHeaders.size() != mTypes.size()) {
      throw new IllegalArgumentException("Number of elements in the headerList (${headerList}) differs from number of datatypes (${dataTypes})")
    }
    mColumns = []
    columns.eachWithIndex { List column, int idx ->
      mColumns.add(new Column(mHeaders[idx], column, mTypes[idx]))
    }
  }

  /**
   * Add a column of the specified type, initialized with null values.
   *
   * @param name the name of the column
   * @param type the type (class) of the data
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix addColumn(String name, Class type) {
    addColumn(name, type, [null]*rowCount())
  }

  /**
   * Add a column to the end of the matrix.
   *
   * @param name the name of the column
   * @param type the type (class) of the data
   * @param column the list of column values
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix addColumn(String name, Class type = Object, List column) {
    if (columnNames().contains(name)) {
      throw new IllegalArgumentException("Column names must be unique, $name already exists at index ${columnIndex(name)}")
    }
    if (rowCount() > 0 && column.size() != rowCount()) {
      throw new IllegalArgumentException("Column size (${column.size()}) does not match matrix row count (${rowCount()})")
    }
    mColumns << new Column(name, column, type)
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
  Matrix addColumn(String name, Class type = Object, Integer index, List column) {
    mColumns.add(index, new Column(name, column, type))
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
    List<List> cols = table.columns(names) as List<List>
    List<Class> types = table.types(names)
    return addColumns(names, cols, types)
  }

  /**
   * Add the provided column data, names and types to this matrix.
   *
   * @param names the column names to add in the order the columns appear
   * @param columns the column data lists to add
   * @param types the column types corresponding to the supplied columns
   * @return the mutated Matrix
   * @throws IllegalArgumentException if the number of supplied names, columns and types are not identical
   */
  Matrix addColumns(List<String> names, List<List> columns, List<Class> types) {
    int columnSize = columns.size()
    if (columnSize != names.size() || columnSize != types.size()) {
      throw new IllegalArgumentException("List sizes of columns, names and types must match")
    }
    if (rowCount() > 0) {
      columns.eachWithIndex { List col, int i ->
        if (col.size() != rowCount()) {
          throw new IllegalArgumentException("Column '${names[i]}' size (${col.size()}) does not match matrix row count (${rowCount()})")
        }
      }
    }
    names.eachWithIndex { String name, int i ->
      addColumn(name, types[i], columns[i])
    }
    return this
  }

  /**
   * Add the specified columns from another matrix by index.
   *
   * @param table the matrix to add columns from
   * @param columns the column indices to add
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix addColumns(Matrix table, List<Integer> columns) {
    List<String> names
    columns.each {
      addColumn(
          table.columnName(it),
          table.type(it),
          table[it]
      )
    }
    this
  }

  /**
   * Appends a row to the current Matrix
   *
   * @param row a List containing the row to add
   * @return a reference to the this Matrix to allow method chaining
   */
  Matrix addRow(List row) {
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
  Matrix addRow(int position, List row) {
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
   * @param rows a List<List containing the rows to add
   * @return a reference to the this Matrix to allow method chaining
   */
  Matrix addRows(List<List> rows) {
    if (rows == null) {
      throw new IllegalArgumentException("Row cannot be null")
    }
    rows.each {
      addRow(it)
    }
    return this
  }

  /**
   * Mutable operation to add a row.
   *
   * This differs from {@code plus(List)} which returns a new matrix without mutating this instance.
   *
   * @param row the row list to add
   * @return this matrix (altered)
   *
   * Note: In v3.7.0, the `&` operator is planned to append columns instead of rows.
   * Use addRow/addRows for forward-compatible code.
   */
  Matrix and(List row) {
    addRow(row)
  }

  /**
   * Mutable operation to add all rows from another matrix.
   *
   * This differs from {@code plus(Matrix)} which returns a new matrix without mutating this instance.
   *
   * @param row the row list to add
   * @return this matrix (altered)
   *
   * Note: In v3.7.0, the `&` operator is planned to append columns instead of rows.
   * Use addRow/addRows for forward-compatible code.
   */
  Matrix and(Matrix matrix) {
    addRows(matrix.rowList())
  }

  /**
   * Apply executes a Closure on each value of the specified column
   *
   * @param columnName the column to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(String columnName, Closure function) {
    if (!columnNames().contains(columnName)) {
      throw new IllegalArgumentException("There is no column called $columnName in this matrix")
    }
    return apply(columnIndex(columnName), function)
  }

  /**
   * Apply executes a Closure on each value of the specified column
   *
   * @param columnNumber the column to apply to
   * @param function the closure to apply
   * @return this Matrix (mutated)
   */
  Matrix apply(int columnNumber, Closure function) {
    int lastColIdx = mColumns.size() -1
    if (columnNumber < 0 || columnNumber > lastColIdx) {
      throw new IndexOutOfBoundsException("The column number must be within the available columns (0-${lastColIdx}) but was $columnNumber")
    }
    def col = new Column()
    Class updatedClass = null
    column(columnNumber).each { v ->
      def val = function.call(v)
      if (updatedClass == null && val != null) {
        updatedClass = val.class
      }
      col.add(val)
    }
    col.type = updatedClass
    col.name = columnName(columnNumber)
    mColumns[columnNumber] = col
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
    int lastColIdx = mColumns.size() -1
    if (columnNumber < 0 || columnNumber > lastColIdx) {
      throw new IndexOutOfBoundsException("The column number must be within the available columns (0-${lastColIdx}) but was $columnNumber")
    }
    def col = new Column()
    Class updatedClass = null
    column(columnNumber).eachWithIndex { it, idx ->
      if (idx in rows) {
        def val = function(it)
        if (updatedClass == null && val != null) {
          updatedClass = val.class
        }
        col.add(val)
      } else {
        col.add(it)
      }
    }
    col.type = updatedClass
    col.name = columnName(columnNumber)
    mColumns[columnNumber] = col
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
  Matrix apply(int columnNumber, Closure<Boolean> criteria, Closure function) {
    List<List> updatedRows = []
    Class updatedClass = null
    def orgVal
    rows().each { it ->
      def row = it as List
      if (criteria.call(row)) {
        def r = []
        for (int i = 0; i < columnCount(); i++) {
          orgVal = row[i]
          if (columnNumber == i) {
            def val = function.call(orgVal)
            if (updatedClass == null && val != null) {
              updatedClass = val.class
            }
            r.add(val)
          } else {
            r.add(orgVal)
          }
        }
        updatedRows.add(r)
      } else {
        updatedRows.add(row)
      }
    }

    List<Class> dataTypes = updatedClass != type(columnNumber)
        ? createTypeListWithNewValue(columnNumber, updatedClass, true)
        : types()
    List<Column> cols = new ArrayList<>()
    Grid.transpose(updatedRows).eachWithIndex { it, idx ->
      cols << new Column(columnName(idx), it as List, dataTypes[idx])
    }
    mColumns = cols
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

  /**
   * Return the name of the column at the specified index.
   *
   * @param index the column index
   * @return the column name
   */
  String columnName(int index) {
    return columnNames()[index]
  }

  /**
   * Return all column names in order.
   *
   * @return the list of column names
   */
  List<String> columnNames() {
    return mColumns.collect{it.name}
  }

  /**
   * Cast this matrix to other common types.
   *
   * Supported targets:
   * - Matrix (returns this)
   * - List/ArrayList (rows as List<List>)
   * - Map/LinkedHashMap (column name -> column values)
   * - Grid (matrix values only)
   *
   * @param type the target class
   * @return the matrix converted to the requested type
   */
  def asType(Class type) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null")
    }
    if (type == Matrix) {
      return this
    }
    if (type == List || type == ArrayList) {
      return rowList()
    }
    if (type == Map || type == LinkedHashMap) {
      Map map = [:]
      columns().each {
        map[it.name] = it.values
      }
      return map
    }
    if (type == Grid) {
      return grid()
    }
    throw new IllegalArgumentException("Cannot convert Matrix to type $type")
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
    mColumns[index].name = String.valueOf(name)
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
    if (columnCount() != names.size()) {
      throw new IllegalArgumentException("Number of column names (${names.size()}) does not match number of columns (${columnCount()}) in this matrix")
    }
    mColumns.eachWithIndex { Column col, int i ->
      col.setName(names[i])
    }
    return this
  }

  /**
   *
   * @param range
   * @return the column names corresponding to each index in the range
   */
  List<String> columnNames(IntRange range) {
    columns(range).collect {it.name}
  }

  /**
   * get all column names matching the type specified
   *
   * @param c the Class to look for, subclasses will also match
   * @return a list of all column names matching the type specified
   */
  List<String> columnNames(Class c) {
    List<String> names = []
    mColumns.eachWithIndex { Column col, int idx ->
      Class cls = col.type
      if (c.isAssignableFrom(cls)) {
        names << columnName(idx)
      }
    }
    names
  }

  /**
   * Convert all columns to the type specified
   *
   * @param type the class to convert all values to
   * @param dateTimeFormatter an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(Class<T> type, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    List<Class<T>> types = [type] * columnCount()
    convert(types, null, dateTimeFormat, numberFormat)
  }

  /**
   * Convert all columns to the type specified, substituting a value for nulls.
   *
   * @param type the class to convert all values to
   * @param valueIfNull the value to use when an element is null
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(Class<T> type, T valueIfNull, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    List<Class<T>> types = [type] * columnCount()
    convert(types, valueIfNull, dateTimeFormat, numberFormat)
  }


  /**
   * Convert columns in order to the classes specified.
   *
   * @param types a list of column types (classes) in column order
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  Matrix convert(List<Class> types, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    convert(types, null, dateTimeFormat, numberFormat)
  }

  /**
   * Convert the columns in the order of a list to the classes specified
   * @param types a list of column types (classes)
   * @param dateTimeFormatter an optional DateTimeFormatter
   * @param numberFormat an optional NumberFormat
   * @return this Matrix converted as specified
   */
  Matrix convert(List<Class> types, Object valueIfNull, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    if (types.size() > columnCount()) {
      throw new IllegalArgumentException("There are more column types specified (${types.size()}) than there are columns in this matrix (${columnCount()})")
    }
    Map<String, Class> columnTypeMap = [:]
    for (int i = 0; i < types.size(); i++) {
      columnTypeMap[columnName(i)] = convertPrimitiveToWrapper(types[i])
    }
    return convert(columnTypeMap, valueIfNull, dateTimeFormat, numberFormat)
  }

  /**
   * Convert named columns to the classes specified in the map.
   *
   * @param types a map of column name to target class
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  Matrix convert(Map<String, Class> types, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    convert(types, null, dateTimeFormat, numberFormat)
  }

  /**
   * Convert named columns to the classes specified in the map, substituting a value for nulls.
   *
   * @param types a map of column name to target class
   * @param valueIfNull the value to use when an element is null
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  Matrix convert(Map<String, Class> types, Object valueIfNull, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    List<Column> convertedColumns = []
    for (int i = 0; i < columnCount(); i++) {
      String colName = columnName(i)
      if (types[colName]) {
        convertedColumns.add(new Column(colName, ListConverter.convert(
            column(i),
            types[colName],
            valueIfNull,
            dateTimeFormat,
            numberFormat
        ), types[colName] as Class
        ))
      } else {
        convertedColumns.add(column(i))
      }
    }
    mColumns = convertedColumns
    this
  }

  /**
   * Convert a single column to the specified type.
   *
   * @param columnName the column name to convert
   * @param type the target class
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(String columnName, Class<T> type, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    return convert([(columnName): type], null, dateTimeFormat, numberFormat)
  }

  /**
   * Convert a single column to the specified type, substituting a value for nulls.
   *
   * @param columnName the column name to convert
   * @param type the target class
   * @param valueIfNull the value to use when an element is null
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(String columnName, Class<T> type, T valueIfNull, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    return convert([(columnName): type], valueIfNull, dateTimeFormat, numberFormat)
  }

  /**
   * Convert a single column by index to the specified type.
   *
   * @param columnIndex the column index to convert
   * @param type the target class
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(int columnIndex, Class<T> type, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    convert(columnIndex, type, null, dateTimeFormat, numberFormat)
  }

  /**
   * Convert a single column by index to the specified type, substituting a value for nulls.
   *
   * @param columnIndex the column index to convert
   * @param type the target class
   * @param valueIfNull the value to use when an element is null
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(int columnIndex, Class<T> type, T valueIfNull, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    convert(columnName(columnIndex), type, valueIfNull, dateTimeFormat, numberFormat)
  }

  /**
   * Convert a column using a custom converter closure.
   *
   * @param columnName the column name to convert
   * @param type the target class
   * @param converter a closure that converts each value
   * @return this Matrix converted as specified
   */
  Matrix convert(String columnName, Class type, Closure converter) {
    return convert(columnNames().indexOf(columnName), type, converter)
  }

  /**
   * As this clashes with convert(List<Class>) we require an array as parameter. i.e.
   * You need to cast it with as Converter[] e.g.
   * <code>
   * convert([new Converter('id', Integer, {Integer.parseInt(it)})] as Converter[])
   * <code>
   *
   * @param converters an array of se.alipsa.matrix.core.Converter
   * @return this Matrix with the types and column values converted
   */
  Matrix convert(Converter... converters) {
    List<Column> convertedColumns = []
    List<Class> convertedTypes = []

    List<String> columnNameList = []
    List<Class> columnTypeList = []
    List<Closure> converterList = []
    converters.each {
      columnNameList.add(it.columnName)
      columnTypeList.add(it.type)
      converterList.add(it.converter)
    }

    for (int i = 0; i < columnCount(); i++) {
      String colName = columnName(i)
      if (columnNameList.contains(colName)) {
        int index = columnNameList.indexOf(colName)
        def columnVals = new Column()
        def converter = converterList[index]
        for (def val in column(colName)) {
          columnVals.add(converter.call(val))
        }
        convertedTypes.add(columnTypeList[index])
        columnVals.name = colName
        columnVals.type = columnTypeList[index]
        convertedColumns.add(columnVals)
      } else {
        convertedColumns.add(column(i))
        convertedTypes.add(type(i))
      }
    }
    mColumns.clear()
    mColumns.addAll(convertedColumns)
    this
  }

  /**
   *
   * @param columnNumber the column index for the column to convert
   * @param columnType the class the column will be converted to
   * @param converter as closure converting each value in the designated column
   * @return a new Matrix
   */
  <T> Matrix convert(int columnNumber, Class<T> columnType, Closure<T> converter) {

    def col = new Column(columnName(columnNumber),columnType)
    Column column = mColumns[columnNumber]
    column.each {
       col << converter(it)
    }
    column.clear()
    column.addAll(col)
    column.type = columnType
    this
  }

  /**
   * Convert a range of columns to the specified type.
   *
   * @param columns the column index range to convert
   * @param type the target class
   * @param valueIfNull the value to use when an element is null
   * @param dateTimeFormat an optional formatter if you are converting to a date object (LocalDate, LocalDateTime etc)
   * @param numberFormat an optional number format if you are converting Strings to numbers
   * @return this Matrix converted as specified
   */
  <T> Matrix convert(IntRange columns, Class<T> type, T valueIfNull = null, String dateTimeFormat = null, NumberFormat numberFormat = null) {
    Map<String, Class> map = [:]
    columns.each {
      map.put((columnName(it)), type)
    }
    convert(map, valueIfNull, dateTimeFormat, numberFormat)
  }

  int columnCount() {
    return mColumns.size()
  }

  /**
   * @return the index of the last column (columnCount - 1)
   */
  int lastColumnIndex() {
    columnCount() - 1
  }

  /**
   *
   * @param columnName the column name to match
   * @return the index (position) of the columnName
   */
  int columnIndex(String columnName) {
    for (int i = 0; i < mColumns.size(); i++) {
      if (mColumns[i].name == columnName) {
        return i
      }
    }
    return -1
  }

  /**
   * Finds the index of a partial column name. It
   * 1. ignores leading and trailing whitespace
   * 2. matches the first column that either starts with or endswith the columName supplied
   *
   * @param columnName a partial column name
   * @return the index of the first match or -1 if not found
   */
  int columnIndexFuzzy(String columnName) {
    String colName = columnName.trim()
    int i = 0
    for (String name in columnNames()) {
      String n = name.trim()
      if (n.startsWith(colName) || n.endsWith(colName)) {
        return i
      }
      i++
    }
    return -1
  }

  List<Integer> columnIndices(List<String> columnNames) {
    List<Integer> colNums = []
    columnNames.each {
      colNums.add(columnIndex(it))
    }
    return colNums
  }

  /**
   * Return the column with the specified name.
   *
   * @param columnName the column name to retrieve
   * @return the matching Column
   */
  Column column(String columnName) {
    try {
      return mColumns.get(columnIndex(columnName))
    } catch (IndexOutOfBoundsException e) {
      println ("Attempting to find index for '$columnName' but got ${columnIndex(columnName)}")
      throw new IndexOutOfBoundsException("The column '$columnName' does not exist in this matrix (${this.mName}): " + e.getMessage())
    }
  }

  /**
   * Return the column at the specified index.
   *
   * @param index the column index
   * @return the column at that index
   */
  Column column(int index) {
    return mColumns.get(index)
  }

  /**
   * Return all columns in order.
   *
   * @return the list of columns
   */
  List<Column> columns() {
    return mColumns
  }

  /**
   * Return the columns matching the supplied names.
   *
   * @param columnNames the column names to retrieve
   * @return the matching columns in the order supplied
   */
  List<Column> columns(String[] columnNames) {
    return columns(columnNames as List<String>)
  }

  /**
   * Return the columns matching the supplied names.
   *
   * @param columnNames the column names to retrieve
   * @return the matching columns in the order supplied
   */
  List<Column> columns(List<String> columnNames) {
    def cols = new ArrayList<Column>()
    for (String colName in columnNames) {
      cols.add(column(colName))
    }
    return cols
  }

  /**
   * Return the columns matching the supplied indices.
   *
   * @param indices the column indices to retrieve
   * @return the matching columns in the order supplied
   */
  List<Column> columns(Integer[] indices) {
    List<Column> cols = new ArrayList<>()
    for (int index in indices) {
      cols.add(column(index))
    }
    return cols
  }

  /**
   * Return the columns within the specified index range.
   *
   * @param range the column index range to retrieve
   * @return the matching columns in range order
   */
  List<Column> columns(IntRange range) {
    Integer[] arr = new Integer[range.size()]
    range.eachWithIndex { int entry, int i -> arr[i] = entry }
    columns(arr)
  }


  /**
   * Return the type for the column at the specified index.
   *
   * @param i the column index
   * @return the column type
   */
  Class type(int i) {
    return mColumns[i].type
  }


  /**
   * Return the type for the column with the specified name.
   *
   * @param columnName the column name
   * @return the column type
   */
  Class type(String columnName) {
    return mColumns[columnIndex(columnName)].type
  }


  /**
   * Return the types for all columns in order.
   *
   * @return the list of column types
   */
  List<Class> types() {
    return mColumns.collect{it.type}
  }


  /**
   * Return the types for the specified column names.
   *
   * @param columnNames the column names
   * @return the matching column types in the order supplied
   */
  List<Class> types(List<String> columnNames) {
    List<Class> types = []
    for (name in columnNames) {
      types.add(type(name))
    }
    return types
  }


  /**
   * Return the types for columns in the specified index range.
   *
   * @param range the column index range
   * @return the matching column types in range order
   */
  List<Class> types(IntRange range) {
    return mColumns[range].collect {it.type}
  }


  /**
   * Return the type names for all columns in order.
   *
   * @return the list of column type names
   */

  List<String> typeNames() {
    return mColumns.collect {it.type.simpleName}
  }


  /**
   * Return the type name for the column at the specified index.
   *
   * @param idx the column index
   * @return the column type name
   */

  String typeName(Integer idx) {
    type(idx).simpleName
  }


  /**
   * Render the matrix as a delimited text table.
   *
   * @param includeHeader whether to include a header row with column names
   * @param includeTitle whether to include the matrix name as a title line
   * @param delimiter the column delimiter
   * @param lineEnding the line ending to use between rows
   * @param maxColumnLength the maximum width for each column in the output
   * @return the formatted text table
   */

  String content(boolean includeHeader = true, boolean includeTitle = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    String title = ''
    if (includeTitle) {
      title = toString() + lineEnding
    }
    return title + head(rowCount(), includeHeader, delimiter, lineEnding, maxColumnLength)
  }


  /**
   * Render the matrix as a delimited text table using the supplied parameters.
   *
   * Supported params: includeHeader, includeTitle, delimiter, lineEnding, maxColumnLength.
   *
   * @param params the rendering parameters
   * @return the formatted text table
   */

  String content(Map params) {
    boolean includeTitle = params.getOrDefault('includeTitle', true)
    boolean includeHeader = params.getOrDefault('includeHeader', true)
    String delimiter = params.getOrDefault('delimiter', '\t')
    String lineEnding = params.getOrDefault('lineEnding', '\n')
    int maxColumnLength = (int) params.getOrDefault('maxColumnLength', 50)
    return content(includeHeader, includeTitle, delimiter, lineEnding, maxColumnLength)
  }


  /**
   * Determine whether the supplied row contains any non-null values.
   *
   * Note: this helper is internal-only and will become private in v3.7.0.
   *
   * @param row the row values to inspect
   * @return true if at least one value is non-null, otherwise false
   */
  static boolean containsValues(Iterable row) {
    def strVal
    for (def element in row) {
      strVal = String.valueOf(element)
      if (element != null && strVal != 'null' && !strVal.isBlank()) return true
    }
    return false
  }

  /**
   * Return a copy of this matrix.
   *
   * The clone contains copies of the columns and rows so changes to the clone
   * do not affect the original matrix.
   *
   * @return a deep copy of this matrix
   */
  @Override
  Matrix clone() {
    List<String> headers = []
    List<Class> types = []
    mColumns.each {
      headers << it.name
      types << it.type
    }
    return new Matrix(mName, headers, mColumns as List<List>, types)
  }

  /**
   * Makes a copy of the matrix structure only (name, column names, types) but not the data.
   *
   * @return a new Matrix identical to this except for the content.
   */
  Matrix cloneEmpty() {
    List<String> headers = []
    List<Class> types = []
    mColumns.each {
      headers << it.name
      types << it.type
    }
    builder(mName).columnNames(headers).types(types).build()
  }

  private List<Class> createTypeListWithNewValue(int columnNumber, Class updatedClass, boolean findCommonGround) {
    List<Class> types = []
    for (int i = 0; i < mColumns.size(); i++) {
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


  /**
   * Compare this matrix with another and return a human-readable diff.
   *
   * @param other the matrix to compare with
   * @param forceRowComparing whether to compare row-by-row even if column names differ
   * @param allowedDiff the numeric tolerance when comparing numeric values
   * @return a diff string describing any differences, or an empty string if equal
   */
  String diff(Matrix other, boolean forceRowComparing = false, double allowedDiff = 0.0001) {
    StringBuilder sb = new StringBuilder()
    if (this.matrixName != other.matrixName) {
      sb.append("Names differ: \n\tthis: ${matrixName} \n\tthat: ${other.matrixName}\n")
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
            def thatVal = (thatRow[c] != null ? thatRow[c] : Double.NaN) as double
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

  /**
   *
   * @param columnNames
   * @return this modified matrix
   * @deprecated use dropColumns(String...) instead. Will be removed in 3.6.0
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix dropColumnsExcept(String... columnNames) {
    dropExcept(columnNames)
  }



  /**
   * Drop all columns except the specified ones.
   *
   * @param columnNames the column names to keep
   * @return a matrix with only the specified columns
   */
  Matrix dropExcept(String... columnNames) {
    def retainColNames = columnNames.length > 0 ? columnNames as List : []
    for (String columnName : this.columnNames()) {
      if (retainColNames.contains(columnName)) {
        continue
      }
      def colIdx = columnIndex(columnName)
      mColumns.remove(colIdx)
    }
    return this
  }

  /**
   *
   * @param columnIndices
   * @return
   * @deprecated use dropColumns(int...) instead. Will be removed in 3.6.0
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix dropColumnsExcept(int ... columnIndices) {
    dropExcept(columnIndices)
  }


  /**
   * Drop all columns except the specified indices.
   *
   * @param columnIndices the column indices to keep
   * @return a matrix with only the specified columns
   */
  Matrix dropExcept(int ... columnIndices) {
    if (columnIndices.length == 0) {
      mColumns.clear()
      return this
    }
    def retainColIndices = columnIndices as Set
    List<Column> columnsToKeep = []
    for (int i = 0; i < columnCount(); i++) {
      if (retainColIndices.contains(i)) {
        columnsToKeep.add(mColumns[i])
      }
    }
    mColumns.clear()
    mColumns.addAll(columnsToKeep)
    return this
  }

  /**
   *
   * @param columnNames
   * @return
   * @deprecated use drop(String...) instead. Will be removed in 3.6.0
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix dropColumns(String... columnNames) {
    drop(columnNames)
  }


  /**
   * Drop the specified columns by name.
   *
   * @param columnNames the column names to remove
   * @return a matrix without the specified columns
   */
  Matrix drop(String... columnNames) {
    if (columnNames.length == 0) {
      println "no variables to drop specified, nothing to do"
      return this
    }
    List<Integer> idxs = columnIndices(columnNames as List<String>)
    if (idxs.contains(-1)) {
      def colNames = this.columnNames()
      throw new IllegalArgumentException("Variables ${String.join(',', columnNames)} does not match actual column names: ${String.join(',', colNames)}")
    }
    drop(idxs)
  }

  /**
   *
   * @param columnIndices
   * @return
   * @deprecated use drop(int...) instead. Will be removed in 3.6.0
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix dropColumns(IntRange columnIndices) {
    drop(columnIndices)
  }

  /**
   * Drop the range of columns specified.
   *
   * @param columnIndices the range of columns to drop
   * @return this modified matrix
   */
  Matrix drop(IntRange columnIndices) {
    drop(columnIndices.toList())
  }

  /**
   *
   * @param columnIndices
   * @return
   * @deprecated use drop(int...) instead. Will be removed in 3.6.0
   */
  @Deprecated
  Matrix dropColumns(List<Integer> columnIndices) {
    drop(columnIndices)
  }

  /**
   * Drop the columns specified in the list of column indices.
   *
   * @param columnIndices the list of column indices to drop
   * @return this modified matrix
   */
  Matrix drop(List<Integer> columnIndices) {
    Collections.sort(columnIndices)
    columnIndices.eachWithIndex { colIdx, idx ->
      // Each time we iterate and remove, all of the below will have one less item
      // so we need to adjust the colIdx to still match
      mColumns.remove(colIdx - idx)
    }
    return this
  }

  /**
   *
   * @param columnIndices
   * @return
   * @deprecated use drop(int...) instead. Will be removed in 3.6.0
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix dropColumns(int ... columnIndices) {
    drop(columnIndices)
  }

  /**
   * Drop the columns for the indices specified.
   *
   * @param columnIndices the columns indices to drop
   * @return this modified matrix
   */
  Matrix drop(int ... columnIndices) {
    def columnsToDrop = columnIndices.length > 0 ? columnIndices as List<Integer> : []
    drop(columnsToDrop)
  }

  @Override
  boolean equals(Object o) {
    equals(o, true, true, false, 0.0001d, false)
  }


  /**
   * Compare this matrix to another with configurable comparison rules.
   *
   * @param o the matrix to compare against
   * @param ignoreColumnNames whether to ignore column names when comparing
   * @param ignoreMatrixName whether to ignore the matrix name when comparing
   * @param ignoreTypes whether to ignore column types when comparing
   * @param allowedDiff numeric tolerance for numeric values
   * @param throwException whether to throw on mismatch instead of returning false
   * @param message message prefix used when throwException is true
   * @return true if the matrices are considered equal, otherwise false
   */
  boolean equals(Object o, boolean ignoreColumnNames, boolean ignoreMatrixName, boolean ignoreTypes = true,
                 Double allowedDiff = 0.0001, boolean throwException = false, String message = '') {
    if (this.is(o)) return true
    if (!(o instanceof Matrix)) return false

    Matrix matrix = (Matrix) o
    if (!ignoreMatrixName && mName != matrix.mName) {
      handleError("$message - Matrix.equals: names do not match", throwException)
      return false
    }
    if (mColumns.size() != matrix.columnCount()) {
      handleError("$message - Matrix.equals: number of columns differ", throwException)
      return false
    }
    if (!ignoreColumnNames && columnNames() != matrix.columnNames()) {
      handleError("$message - Matrix.equals: column names differ", throwException)
      return false
    }
    if (!ignoreTypes && types() != matrix.types()) {
      handleError("$message - Matrix.equals: column types differ", throwException)
      return false
    }
    //if (mColumns != matrix.mColumns) return false
    List valueDiff = checkValues(matrix, allowedDiff, ignoreTypes)
    if (valueDiff.size() > 0) {
      handleError("$message - Matrix.equals: value(s) differ: row ${valueDiff[0]}, column ${valueDiff[1]} expected ${valueDiff[2]} but was ${valueDiff[3]}", throwException)
      return false
    }

    return true
  }


  /**
   * Handle comparison errors for matrix equality checks.
   *
   * Note: this helper is internal-only and will become private in v3.7.0.
   *
   * @param msg the error message
   * @param throwException whether to throw an IllegalArgumentException instead of logging
   */
  static void handleError(String msg, boolean throwException) {
    if (throwException) {
      throw new IllegalArgumentException(msg)
    } else {
      println(msg)
    }
  }

  private List checkValues(Matrix matrix, Double allowedDiff, boolean ignoreTypes = false) {
    List valueDiff = []
    int i = 0
    for (List column in mColumns) {
      def thatCol = matrix.column(i)
      int r = 0
      for (entry in column) {
        def thatVal = thatCol[r]
        if (entry instanceof Number) {
          def diff = Math.abs((entry as double) - ((thatVal != null ? thatVal : Double.NaN) as double))
          if (diff > allowedDiff) {
            return [r, i, entry, thatVal]
          }
        } else {
          if (ignoreTypes) {
            if (String.valueOf(entry) != String.valueOf(thatVal)) {
              return [r, i, entry, thatVal]
            }
          } else {
            if (entry != thatVal) {
              return [r, i, entry, thatVal]
            }
          }
        }
        r++
      }
      i++
    }
    return valueDiff
  }

  /**
   * Searches for the first occurrence of the value in the column specified
   *
   * @param columnName the name of the column to search
   * @param value the value to search for
   * @return the first row where the column matches the value or null of no match found
   */
  Row findFirstRow(String columnName, Object value) {
    int rowIndex = column(columnName).findIndexOf { it == value }
    return rowIndex >= 0 ? row(rowIndex) : null
  }

  /**
   * Searches for the first occurrence of the value in the column specified
   *
   * @param columnIndex the index of the column to search
   * @param value the value to search for
   * @return the first row where the column matches the value or null of no match found
   */
  Row findFirstRow(Integer columnIndex, Object value) {
    int rowIndex = column(columnIndex).findIndexOf { it == value }
    return rowIndex >= 0 ? row(rowIndex) : null
  }

  /**
   * Get a raw value from the matrix at the specified row/column.
   *
   * @param row the row index
   * @param column the column index
   * @return the value at the specified coordinates
   */
  Object get(int row, int column) {
    return mColumns[column][row]
  }

  /**
   * @return the name of the matrix, or null if unnamed
   */
  String getMatrixName() {
    return mName
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table["salary"] for the salary column
   *
   * @return the column corresponding to the column name supplied
   */
  Column getAt(String columnName) {
    return column(columnName)
  }

  /**
   * Enable the use of square bracket to reference a range of columns, e.g. table[0..2] for the first 3 columns
   *
   * @return the column corresponding to the column name supplied
   */
  List<Column> getAt(IntRange range) {
    mColumns[range]
  }

  /**
   * Enable the use of square bracket to reference a column, e.g. table[2] for the 3:rd column
   *
   * @return the column corresponding to the column index supplied
   */
  Column getAt(int columnIndex) {
    return column(columnIndex)
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
   *
   * @return the value corresponding to the row and column name supplied
   */
  <T> T getAt(Integer row, String columnName) {
    Class<T> type = type(columnName) as Class<T>
    Integer columnIdx = columnIndex(columnName)
    def val = get(row, columnIdx)
    val == null ? null : val.asType(type)
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
  List getAt(int rowIndex, IntRange columns) {
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
  List getAt(IntRange rows, int colIndex) {
    column(colIndex)[rows]
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
   * @param type the class that the value should be converted to
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
   * @param type the class that the value should be converted to
   * @return a value of the type specified
   */
  <T> T getAt(int row, String columnName, Class<T> type) {
    return ValueConverter.convert(get(row, columnIndex(columnName)), type)
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
   * @param type the class that the value should be converted to
   * @param valIfNull value to replace a missing (null) value
   * @return a value of the type specified
   */
  <T> T getAt(int row, int column, Class<T> type, T valIfNull) {
    getAt(row, column, type) ?: valIfNull
  }

  /**
   *
   * @param row the observation to get
   * @param columnName the variable to get
   * @param type the class that the value should be converted to
   * @param valIfNull value to replace a missing (null) value
   * @return
   */
  <T> T getAt(int row, String columnName, Class<T> type, T valIfNull) {
    getAt(row, columnName, type) ?: valIfNull
  }

  /**
   * Generic override for getAt to enable access from statically compiled groovy.
   * Without this putAt expressions such as <code>matris[0,1] = null</code> would fail
   * with a compilation error.
   *
   * @param args matching any of the individual getAt methods
   * @return
   */
  def getAt(List args) {
    if (args == null || args.size() == 0) {
      throw new IllegalArgumentException("No arguments supplied")
    }
    if (args.size() == 1) {
      def arg = args[0]
      if (arg instanceof String) return getAt(arg as String)
      if (arg instanceof IntRange) return getAt(arg as IntRange)
      if (arg instanceof Integer) return getAt(arg as Integer)
    }
    if (args.size() == 2) {
      def arg0 = args[0]
      def arg1 = args[1]
      if (arg0 instanceof Integer && arg1 instanceof Integer) return getAt(arg0 as Integer, arg1 as Integer)
      if (arg0 instanceof Integer && arg1 instanceof String) return getAt(arg0 as Integer, arg1 as String)
      if (arg0 instanceof Integer && arg1 instanceof IntRange) return getAt(arg0 as Integer, arg1 as IntRange)
      if (arg0 instanceof IntRange && arg1 instanceof Integer) return getAt(arg0 as IntRange, arg1 as Integer)
      if (arg0 instanceof String && arg1 instanceof Class) return getAt(arg0 as String, arg1 as Class)
    }
    if (args.size() == 3) {
      def arg0 = args[0]
      def arg1 = args[1]
      def arg2 = args[2]
      if (arg0 instanceof Integer && arg1 instanceof Integer && arg2 instanceof Class)
        return getAt(arg0 as Integer, arg1 as Integer, arg2 as Class)
      if (arg0 instanceof Integer && arg1 instanceof String && arg2 instanceof Class)
        return getAt(arg0 as Integer, arg1 as String, arg2 as Class)
    }
    if (args.size() == 4) {
      def arg0 = args[0]
      def arg1 = args[1]
      def arg2 = args[2]
      def arg3 = args[3]
      if (arg0 instanceof Integer && arg1 instanceof Integer && arg2 instanceof Class)
        return getAt(arg0 as Integer, arg1 as Integer, arg2 as Class, arg3)
      if (arg0 instanceof Integer && arg1 instanceof String && arg2 instanceof Class)
        return getAt(arg0 as Integer, arg1 as String, arg2 as Class, arg3)
    }
    throw new IllegalArgumentException("Unable to understand arguments supplied: $args")
  }


  /**
   * Intercept property access to return a column when the name matches.
   *
   * @param name the property name
   * @return the matching Column, or the default property value if not a column name
   */
  Object getProperty(String name) {
    if (columnNames().contains(name)) {
      return column(name)
    }
    return getProperties().get(name)
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
    MatrixPrinter.head(this, rows, includeHeader, delimiter, lineEnding, maxColumnLength)
  }

  @Override
  int hashCode() {
    int result
    result = (mColumns != null ? mColumns.size() : 0)
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
    int currentIndex = columnIndex(columnName)
    Column col = mColumns.remove(currentIndex)
    mColumns.add(index, col)
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
    //return create(mName, mHeaders, rows as List<List>, mTypes)
  }


  /**
   * Sort rows using the supplied column ordering.
   *
   * The map key is the column name and the value indicates ascending (true)
   * or descending (false) order. The map iteration order is respected.
   *
   * @param columnsAndDirection ordered map of column name to ascending flag
   * @return a matrix sorted according to the supplied columns
   */
  Matrix orderBy(LinkedHashMap<String, Boolean> columnsAndDirection) {
    def colNames = columnsAndDirection.keySet() as List<String>
    def headers = columnNames()
    for (String columnName in colNames) {
      if (columnName !in headers) {
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


  /**
   * Sort rows using the supplied comparator.
   *
   * The comparator receives row lists for comparison.
   *
   * @param comparator the comparator used to order rows
   * @return a matrix sorted according to the comparator
   */
  Matrix orderBy(Comparator comparator) {
    // copy all the rows
    List<Row> rows = this.rows()
    Collections.sort(rows, comparator)
    updateValues(rows)
    return this
  }

  /**
   * Append a row to a clone of this matrix.
   *
   * This differs from {@code and(List)} which mutates this instance.
   *
   * @param row the row to append
   * @return a new matrix with the row appended to the end
   */
  Matrix plus(List row) {
    if (row == null) {
      return clone()
    }
    clone().addRow(row)
  }

  /**
   * Append all rows from another matrix to a clone of this one.
   *
   * This differs from {@code and(Matrix)} which mutates this instance.
   *
   * @param table the matrix to append from
   * @return a new matrix containing the rows of the former and the new rows added
   */
  Matrix plus(Matrix table) {
    if (table == null) {
      return clone()
    }
    clone().addRows(table.rowList())
  }

  /**
   * Allows for "short form" manipulation of values e.g:
   * myMatrix[1,2] = 42
   * myMatrix[1,"Foo"] = 42
   */
  void putAt(List where, Object value) {
    def row = where[0]
    def col = where[1]
    if (!row instanceof Number) {
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


  /**
   * Set a value by row and column indices.
   *
   * This is the backing method for the square-bracket assignment syntax.
   *
   * Example:
   * <pre>
   * matrix.putAt(0, 1, 42)
   * matrix[0, 1] = 42
   * </pre>
   *
   * @param rowIndex the row index
   * @param colIndex the column index
   * @param value the value to set
   */
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


  /**
   * Set a value by row index and column name.
   *
   * This is the backing method for the square-bracket assignment syntax.
   *
   * Example:
   * <pre>
   * matrix.putAt(0, 'age', 42)
   * matrix[0, 'age'] = 42
   * </pre>
   *
   * @param rowIndex the row index
   * @param columnName the column name
   * @param value the value to set
   */
  void putAt(Number rowIndex, String columnName, Object value) {
    putAt(rowIndex, columnIndex(columnName), value)
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
  void putAt(List where, List column) {
    if (where.size() < 2) {
      throw new IllegalArgumentException("Insufficient number of arguments, specify at least columnName, type and the list of values")
    }
    if (where.size() == 2) {
      // special cases, when null is passed, the closest match is actually here so we need to redirect properly
      if (where[0] instanceof Number && column == null) {
        if (where[1] instanceof String) {
          putAt(where[0] as Number, where[1] as String, null as Object)
        } else if (where[1] instanceof Number) {
          putAt(where[0] as Number, where[1] as Number, null as Object)
        }
      } else {
        putAt(where[0] as String, where[1] as Class, column)
      }
    } else if (where.size() == 3) {
      putAt(where[0] as String, where[1] as Class, where[2] as Integer, column)
    } else {
      throw new IllegalArgumentException("Too many arguments ${where.size()}, putAt understands maximim 4")
    }
  }



  /**
   * Add the column to the end or at the index if specified
   * <code><pre>
   *   tbl["yearMonth", YearMonth] = toYearMonths(tbl.start_date)
   * </code>
   * Groovy semantics requires this to mutate, i.e. this does not work
   * <code>
   *   Matrix t2 = (table["yearMonth", YearMonth, 0] = toYearMonth(table["start_date"]))
   * </code>
   * thus, we return void to make that fact obvious.
   *
   * @param columnName the column name to add
   * @param type the class of the contents in the column list
   * @param index the position to insert it or null if append to the end
   * @param column the list of values to add
   */
  void putAt(String columnName, Class type, Integer index = null, List column) {
    if (rowCount() != column.size()) {
      throw new IllegalArgumentException("Number of column values (${column.size()}) does not match number of rows (${rowCount()}) in this matrix")
    }
    if (columnNames().contains(columnName)) {
      replace(columnName, type, column)
    } else {
      if (index == null) {
        mColumns << new Column(columnName, column, type)
      } else {
        mColumns.add(index, new Column(columnName, column, type))
      }
    }
  }

  /**
   * Assign or replace a column by name using the provided values.
   *
   * @param columnName the column name to add or replace
   * @param values the column values
   */
  void putAt(String columnName, List values) {
    upsertColumn(columnName, values)
  }

  /**
   * Assign or replace a column by index using the provided values.
   *
   * @param column the column index to add or replace
   * @param values the column values
   */
  void putAt(Integer column, List values) {
    upsertColumn(columnName(column), values)
  }

  /**
   * myMatrix[0..2] = otherMatrix[1..3]
   *
   * @param range
   * @param List
   */
  void putAt(IntRange range, List<List> columns) {
    if (range.size() != columns.size()) {
      throw new IllegalArgumentException("Size of range (${range.size()}) must be equal to the number of columns (${columns.size()})")
    }
    range.eachWithIndex { int it, int idx ->
      putAt(it, columns[idx])
    }
  }

  /**
   *
   * @return the index value of the last observation
   */
  int lastRowIndex() {
    rowCount() - 1
  }

  /**
   * Append a column to this matrix.
   *
   * Note: In v3.7.0, the `<<` operator is planned to append rows instead of columns.
   * Use addColumn for forward-compatible code.
   */
  def leftShift(Column column) {
    addColumn(column.name, column.type, column)
  }

  /**
   * Append multiple columns to this matrix.
   *
   * Note: In v3.7.0, the `<<` operator is planned to append rows instead of columns.
   * Use addColumn for forward-compatible code.
   */
  def leftShift(Collection<Column> columns) {
    columns.each { column ->
      if (!column instanceof Column) {
        throw new IllegalArgumentException("leftShift to add columns should be specified with a list of columns, a Map or another Matrix")
      }
      addColumn(column.name, column.type, column)
    }
  }

  /**
   * Adds the column from the specified matrix to this one. Note that this is not a join on some matching criteria
   * but just a "raw" add. If the specified Matrix has more rows than this, only a subset will be added. If it is smaller
   * nulls will be used to fill the extra space.
   *
   * @param m the matrix containing the columns to add
   * @return this Matrix (mutated)
   *
   * Note: In v3.7.0, the `<<` operator is planned to append rows instead of columns.
   * Use addColumn for forward-compatible code.
   */
  def leftShift(Matrix m) {
    int nrow = rowCount()
    m.columns().eachWithIndex {c, idx ->
      def col
      if (c.size() < nrow) {
        col = c + [null]*(nrow-c.size())
      } else if (c.size() > nrow) {
        col = c.subList(0, nrow)
      } else {
        col = c
      }
      addColumn(m.columnName(idx), m.type(idx), col)
    }
  }

  /**
   * Append columns to this matrix from a map of column name to values.
   *
   * Note: In v3.7.0, the `<<` operator is planned to append rows instead of columns.
   * Use addColumn for forward-compatible code.
   */
  def leftShift(Map<String, List> columns) {
    columns.each { name, list ->
      def type = list.isEmpty() ? Object : list[0].class
      addColumn(name, type, list)
    }
  }

  /**
   * Pivot is a "partial transpose" i.e. it turns rows into columns for a particular column.
   * <code><pre>
   * Matrix orgMatrix = Matrix.builder('deposits').data(
   *   customerId: [1,1,2,2,2,3],
   *   amount: [100, 110, 100, 110, 120, 100],
   *   currency: ['SEK', 'DKK', 'SEK', 'USD', 'EUR', 'SEK'],
   *   name: ['Per', 'Per', 'Ian', 'Ian', 'Ian', 'John']
   * ).types(int, int, String, String)
   * .build()
   * Matrix pivotedMatrix = orgMatrix.pivot('customerId', 'currency', 'amount')
   * </pre></code>
   * The orgMatrix which looks like this
   * <pre>
   *   customerId	amount	currency	name
   *           1	   100	SEK     	Per
   *           1	   110	DKK     	Per
   *           2	   100	SEK     	Ian
   *           2	   110	USD     	Ian
   *           2	   120	EUR     	Ian
   *           3	   100	SEK     	John
   * </pre>
   * will in the pivotedMatrix be changed to the following:
   * <pre>
   *   customerId	name	SEK	 DKK	 USD	 EUR
   *           1	Per 	100	 110	null	null
   *           2	Ian 	100	null	 110	 120
   *           3	John	100	null	null	null
   * </pre>
   *
   * @param idColumn the first occurrence of the each value in the Id column will be used as a new row in the new matrix
   * @param columnNameColumn the column with the column names to create
   * @param valueColumn the value column to use to populate the new columns
   * @return a new Matrix with the additional columns for the values in the columnNameColumn
   */
  Matrix pivot(String idColumn, String columnNameColumn, String valueColumn) {
    def d = [:]
    List<Row> rows = []
    Set newColumns = new LinkedHashSet()
    this.each {
      def id = it[idColumn]
      def m = d[id]
      if (m == null) {
        d[id] = m = [:]
        rows.add(it)
      }
      def colName = String.valueOf(it[columnNameColumn])
      newColumns.add(colName)
      m[colName] = it[valueColumn]
    }

    Matrix pm = builder(matrixName).rowList(rows).build()
    def colType = type(valueColumn)
    newColumns.each {
      pm.addColumn(String.valueOf(it), colType)
    }
    d.eachWithIndex { Map.Entry it, int idx ->
      Map values = it.value as Map
      values.each { k, v ->
        pm.putAt(idx, String.valueOf(k), v)
      }
    }
    pm.drop(columnNameColumn, valueColumn)
  }

  /**
   * Renames the column and returns the table to allow for chaining.
   *
   * @param before the existing column name
   * @param after the new column name
   * @return the (mutated) table to allow for chaining
   * @deprecated Use rename(before, after) instead
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix renameColumn(String before, String after) {
    rename(before, after)
  }

  /**
   * renames the column and returns the table to allow for chaining
   *
   * @param before the existing column name
   * @param after the new column name
   * @return the (mutated) table to allow for chaining
   */
  Matrix rename(String before, String after) {
    rename(columnIndex(before), after)
    this
  }

  /**
   * Renames the column and returns the table to allow for chaining.
   *
   * @deprecated use rename(int, String) instead
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix renameColumn(int columnIndex, String after) {
    rename(columnIndex, after)
  }

  /**
   * renames the column and returns the table to allow for chaining
   *
   * @param columnIndex the index of the column to rename
   * @param after the new column name
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix rename(int columnIndex, String after) {
    mColumns[columnIndex].name = after
    this
  }

  /**
   * Replace all values in the entire matrix that matches the value
   *
   * @param from the value to search for
   * @param to the value to replace with
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix replaceAll(Object from, Object to) {
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
  Matrix replace(String columnName, Class type = Object, List values) {
    def col = column(columnName)
    if (values == null) {
      throw new IllegalArgumentException('The list of values cannot be null')
    }
    if (col.size() != values.size()) {
      throw new IllegalArgumentException("The size of the column is ${col.size()} but values only contains ${values.size()} elements")
    }
    col.clear()
    col.addAll(values)
    col.type = type
    this
  }

  /**
   * Remove the row indexes specified from this Matrix.
   * This is the mutating equivalent to subset.
   *
   * @param indexes the row indexes to remove
   * @return this matrix (mutated) to allow for method chaining
   */
  Matrix removeRows(int ... indexes) {
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
    indexes.sort()
    mColumns.each { col ->
      indexes.eachWithIndex { Number idx, int count ->
        col.remove((int) idx - count)
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
    removeRows(rowIndices { !containsValues(it as Iterable) })
  }

  /**
   * Remove all columns with only null values and/or blank strings
   *
   * @return this matrix with the empty columns removed
   */
  Matrix dropEmptyColumns() {
    int i = 0
    List<Integer> columnsToRemove = []
    for (List col in mColumns) {
      if (!containsValues(col)) {
        columnsToRemove.add(i as Integer)
      }
      i++
    }
    drop(columnsToRemove)
  }

  /**
   *
   * @return this matrix without the empty columns
   * @deprecated Use dropEmptyColumns() instead
   */
  @Deprecated(forRemoval = true, since = "3.7.0")
  Matrix removeEmptyColumns() {
    dropEmptyColumns()
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
  }

  /**
   *
   * @return a detached list of rows where each row is a list of objects
   */
  List<List<Object>> rowList() {
    return mColumns.transpose()
  }


  /**
   * Return the rows for the specified indices.
   *
   * @param index the row indices
   * @return the matching rows in the order supplied
   */
  List<Row> rows(List<Integer> index) {
    List<Row> rows = []
    index.each {
      rows.add(row((int) it))
    }
    return rows
  }

  /**
   * Finding rows based on some criteria e.g:
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
    this.each { row ->
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
    mColumns.eachWithIndex { List column, int col ->
      for (Integer row = 0; row < nRows; row++) {
        r.get(row).addElement(column[row])
      }
    }
    return r.values() as List
  }

  private static List<Class> sanitizeTypes(Collection<String> headerList, List<Class>... dataTypesOpt) {
    List<Class> types = []
    if (dataTypesOpt.length > 0) {
      types = convertPrimitivesToWrapper(dataTypesOpt[0])
      if (types.isEmpty()) {
        types = createObjectTypes(headerList)
      }
      if (headerList.size() != types.size()) {
        println "Headers (${headerList.size()} elements): $headerList"
        println "Types:  (${types.size()} elements): ${types.collect { it.simpleName }}"
        throw new IllegalArgumentException("Number of columns (${headerList.size()}) differs from number of datatypes provided (${types.size()})")
      }
    }
    if (types.isEmpty()) {
      types = createObjectTypes(headerList)
    }
    return types
  }

  /**
   * Select columns by name and return a new Matrix containing only those columns.
   *
   * @param columnNames the column names to include
   * @return a new Matrix with the selected columns
   */
  Matrix selectColumns(List<String> columnNames) {
    selectColumns(columnNames as String[])
  }


  /**
   * Return a new matrix containing only the specified columns.
   *
   * @param columnNames the column names to include
   * @return a matrix with the selected columns
   */
  Matrix selectColumns(String... columnNames) {
    List<List> cols = []
    List<String> colNames = []
    List<Class> dataTypes = []
    for (name in columnNames) {
      cols.add(column(name))
      colNames.add(name)
      dataTypes.add(type(name))
    }
    return builder()
        .matrixName(mName)
        .columnNames(colNames)
        .columns(cols)
        .types(dataTypes)
        .build()
  }


  /**
   * Return a new matrix containing the columns in the specified index range.
   *
   * @param range the column index range to include
   * @return a matrix with the selected columns
   */
  Matrix selectColumns(IntRange range) {
    selectColumns(columnNames(range) as String[])
  }

  /**
   *
   * @param criteria takes a row (List) as parameter and returns true if the row should be included
   * @return a List of row indexes matching the criteria supplied
   */
  List<Integer> rowIndices(Closure criteria) {
    def r = [] as List<Integer>
    rows().eachWithIndex { row, idx ->
      if (criteria(row)) {
        r.add(idx)
      }
    }
    return r
  }


  /**
   * Set the matrix name.
   *
   * @param name the new matrix name
   */
  void setMatrixName(String name) {
    mName = name
  }

  /**
   * Allow property-style assignment to add or replace a column when the value is a List.
   * For non-list values, this delegates to normal property assignment (e.g. matrixName).
   *
   * @param propertyName the column name to add or replace when the value is a List
   * @param newValue the column values (List) or a regular property value
   */
  @Override
  void setProperty(String propertyName, Object newValue) {
    if (newValue instanceof List) {
      upsertColumn(propertyName, newValue)
    } else {
      GroovyObject.super.setProperty(propertyName, newValue)
    }
  }

  /**
   * unPivot is a "partial transpose" i.e. it turns columns into rows for a set of columns.
   * <code><pre>
   * Matrix orgMatrix = Matrix.builder('deposits').data(
   *   customerId: [1,2,3],
   *   name: ['Per', 'Ian', 'John'],
   *   SEK: [100, 100, 100],
   *   DKK: [110, null, null],
   *   USD: [null, 110, null],
   *   EUR: [null, 120, null]
   * ).types(int, String, int, int, int, int)
   * .build()
   * Matrix unPivotedMatrix = orgMatrix.unPivot('amount', 'currency', ['SEK', 'DKK', 'USD', 'EUR'])
   * </pre></code>
   * The orgMatrix which looks like this
   * <pre>
   *   customerId	name	SEK	 DKK	 USD	 EUR
   *           1	Per 	100	 110	null	null
   *           2	Ian 	100	null	 110	 120
   *           3	John	100	null	null	null
   * </pre>
   * will in the unPivotedMatrix be changed to the following:
   * <pre>
   *   customerId	name	amount	currency
   *           1	Per 	   100	SEK
   *           1	Per 	   110	DKK
   *           2	Ian 	   100	SEK
   *           2	Ian 	   110	USD
   *           2	Ian 	   120	EUR
   *           3	John	   100	SEK
   * </pre>
   *
   * @param valueColumnName the name of the column where the values will be
   * @param categoryColumnName the column with name of each category (column name) from the list of column names
   * @param columnNames the column names to unPivot
   * @return a new Matrix with the additional rows for the values in the columnNames
   */
  Matrix unPivot(String valueColumnName, String categoryColumnName, List<String> columnNames) {
    def m = this.cloneEmpty()
    def colType = type(columnNames[0])
    columnNames.each { String it ->
      colType = findClosestCommonSuper(type(it), colType)
    }

    m.addColumn(valueColumnName, colType)
    m.addColumn(categoryColumnName, String)
    this.each { row ->
      columnNames.each {
        def value = row[it]
        if (value != null) {
          m.addRow(row + [value, it])
        }
      }
    }
    m.drop(columnNames as String[])
    m
  }

  private void upsertColumn(String propertyName, List values) {
    if (columnNames().contains(propertyName)) {
      def col = column(propertyName)
      if (values.size() != col.size() && !col.isEmpty()) {
        // We only allow update when the row is empty or number of rows equals number of values
        throw new IllegalArgumentException("The list of values is not the same as the number of rows")
      }
      col.clear()
      col.addAll(values)
    } else {
      if (values.isEmpty()) {
        addColumn(propertyName, Object, values)
      } else {
        addColumn(propertyName, values[0].class, values)
      }
    }
  }

  /**
   * Splits this matrix into chunks of approximately chunkSize rows.
   * The last chunk may contain fewer rows.
   *
   * @param chunkSize the approximate number of rows per chunk
   * @return a list of Matrix objects
   */
  List<Matrix> split(int chunkSize) {
    def rowChunks = this.collate( rowCount().intdiv( chunkSize ) )
    List<Matrix> chunks = []
    rowChunks.eachWithIndex { it, idx ->
      chunks << builder(matrixName + "_" + idx).rowList(it).build()
    }
    chunks
  }

  /**
   * Splits this matrix into a specified number of chunks.
   * Rows will be distributed as evenly as possible among the chunks.
   *
   * @param numChunks the number of chunks to split the matrix into (must be > 0 and <= rowCount())
   * @return a list of exactly numChunks Matrix objects
   * @throws IllegalArgumentException if numChunks is less than 1 or greater than rowCount()
   * @example if there are 30 rows and numChunks=4, you get 4 matrices:
   *          the first 2 have 8 rows each, the last 2 have 7 rows each (8+8+7+7=30)
   */
  List<Matrix> splitInto(int numChunks) {
    if (numChunks < 1) {
      throw new IllegalArgumentException("numChunks must be at least 1, got: $numChunks")
    }
    int totalRows = rowCount()
    if (numChunks > totalRows) {
      throw new IllegalArgumentException("numChunks ($numChunks) cannot be greater than rowCount() ($totalRows)")
    }

    // Calculate base size and remainder to distribute rows evenly
    int baseSize = totalRows.intdiv(numChunks)
    int remainder = totalRows % numChunks

    List<Matrix> chunks = []
    int startRow = 0

    for (int i = 0; i < numChunks; i++) {
      // First 'remainder' chunks get an extra row
      int chunkSize = (i < remainder) ? baseSize + 1 : baseSize
      int endRow = startRow + chunkSize

      def rowsForChunk = rows().subList(startRow, endRow)
      chunks << builder(matrixName + "_" + i).rowList(rowsForChunk).build()

      startRow = endRow
    }

    return chunks
  }

  /**
   * split is used to create a map of matrices for each unique value in the column.
   * This is useful for e.g. countBy or sumBy (see Stat.countBy() and Stat.countBy())
   *
   * @param columnName
   * @return a map of matrices for each unique value in the column where the key is the value
   */
  Map<?, Matrix> split(String columnName) {
    List col = column(columnName)
    Map<Object, List<Integer>> groups = new HashMap<>()
    for (int i = 0; i < col.size(); i++) {
      groups.computeIfAbsent(col[i], k -> []).add(i)
    }
    Map<Object, Matrix> tables = [:]
    for (entry in groups) {
      tables.put(entry.key,
          builder()
              .matrixName(String.valueOf(entry.key))
              .columnNames(columnNames())
              .rows(rows(entry.value) as List<List>)
              .types(types())
              .build()
      )
    }
    return tables
  }

  /**
   * <code><pre>
   * def table = Matrix.builder().data(
   *      'place': [1, 2, 3],
   *      'firstname': ['Lorena', 'Marianne', 'Lotte'],
   *      'start': ['2021-12-01', '2022-07-10', '2023-05-27'])
   *      .types(int, String, String)
   *      .build()
   *
   *  // This will select the second and third row and return it in a new Matrix
   *  Matrix subSet = table.subset('place', { it > 1 })
   *  </pre></code>
   *  The mutable version of this is removeRows.
   *
   * @param columnName the name of the column to use to select matches
   * @param condition a closure containing the condition for what to retain
   * @return the subset of the matrix (a new Matrix, the original one is unaffected)
   */
  Matrix subset(@NotNull String columnName, @NotNull Closure<Boolean> condition) {
    List<Integer> r = column(columnName).findIndexValues(condition) as List<Integer>
    builder()
        .rows(this.rows(r) as List<List>)
        .matrixName(this.matrixName)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   * Return a new Matrix containing only rows that match the supplied criteria.
   *
   * The criteria receives each row as a List-like Row and should return true to keep the row.
   * Example:
   * <code><pre>
   * def subset = table.subset { it[0] > 10 && it['status'] == 'ACTIVE' }
   * </pre></code>
   *
   * The mutable counterpart is {@code removeRows} or {@code dropRows}.
   *
   * @param criteria takes a row (List) as parameter and returns true if the row should be included
   * @return a new Matrix with the rows matching the criteria supplied retained
   */
  Matrix subset(Closure<Boolean> criteria) {
    builder()
        .rows(rows(criteria) as List<List>)
        .matrixName(this.matrixName)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   * Return a new Matrix containing only rows within the specified index range.
   *
   * @param rows the row index range to include
   * @return a new Matrix with the selected rows
   */
  Matrix subset(IntRange rows) {
    builder()
        .rows(this.rows(rows) as List<List>)
        .matrixName(this.matrixName)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   * Return a new Matrix containing only the rows with the specified indices.
   *
   * @param rows the row indices to include
   * @return a new Matrix with the selected rows
   */
  Matrix subset(List<Integer> rows) {
    builder()
        .rows(this.rows(rows) as List<List>)
        .matrixName(this.matrixName)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   * Return a new Matrix containing only the rows with the specified indices.
   *
   * @param rows the row indices to include
   * @return a new Matrix with the selected rows (or a clone if none supplied)
   */
  Matrix subset(Integer... rows) {
    if (rows == null || rows.length == 0) {
      return this.clone()
    }
    builder()
        .rows(this.rows(rows as List) as List<List>)
        .matrixName(this.matrixName)
        .columnNames(this.columnNames())
        .types(this.types())
        .build()
  }

  /**
   * Filters the matrix to only include rows where the column has the specified value.
   *
   * @param columnName the column to match against
   * @param value the value to keep
   * @return a new matrix with the rows matching the value
   */
  Matrix subset(String columnName, Object value) {
    subset(columnName) {
      it == value
    }
  }

  /**
   * Return a textual representation of the last N rows in the matrix.
   *
   * @param rows number of rows to include from the end
   * @param includeHeader if true, include the header row
   * @param delimiter column delimiter
   * @param lineEnding line ending to use
   * @param maxColumnLength maximum column width for formatting
   * @return a formatted string containing the tail of the matrix
   */
  String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    MatrixPrinter.tail(this, rows, includeHeader, delimiter, lineEnding, maxColumnLength)
  }

  /**
   * Convert this matrix to a CSV formatted string.
   *
   * Defaults: quoteString = '\"', delimiter = ', ', rowDelimiter = '\\n', includeHeader = true
   *
   * @return the matrix as a CSV string
   */
  String toCsvString() {
    toCsvString(includeHeader: true, includeTypes: true)
  }

  /**
   * Convert this matrix to a CSV formatted string.
   *
   * @param includeHeader if true, include the header row
   * @return the matrix as a CSV string
   */
  String toCsvString(boolean includeHeader) {
    toCsvString([includeHeader: includeHeader])
  }

  /**
   * Convert this matrix to a CSV formatted string.
   * If the matrix has a name, it will be included as a comment header.
   *
   * @param includeHeader if true, include the header row
   * @param includeTypes if true, include a types comment header
   * @return the matrix as a CSV string
   */
  String toCsvString(boolean includeHeader, boolean includeTypes) {
    toCsvString([includeHeader: includeHeader, includeTypes: includeTypes])
  }

  /**
   * Convert this matrix to a CSV formatted string.
   * If the matrix has a name, it will be included as a comment header (#name: matrixName).
   * If includeTypes is true, type information will be included as a comment header (#types: Integer, String, ...).
   * Metadata will be included as comment headers (#metadata.key: value).
   * These comment headers can be read back using Matrix.builder().csvString() to fully restore the matrix.
   *
   * <p><b>Metadata Constraints:</b> Only metadata values of the following types can be serialized and round-tripped:
   * <ul>
   *   <li>String</li>
   *   <li>Number (Integer, Long, BigDecimal, etc.)</li>
   *   <li>Boolean</li>
   *   <li>LocalDate</li>
   *   <li>LocalDateTime</li>
   * </ul>
   * Other types will be converted to String via toString() and will be read back as String values.
   *
   * @param options optional parameters:
   *   - quoteString (String) string quote, default '\"'
   *   - delimiter (String) column delimiter, default ', '
   *   - rowDelimiter (String) row delimiter, default '\\n'
   *   - lineComment (String) comment prefix, default '#'
   *   - includeHeader (boolean) include header row, default true
   *   - includeTypes (boolean) include a types comment header, default false
   * @return the matrix as a CSV string
   */
  String toCsvString(Map options) {
    String quoteString = options.containsKey('quoteString') ? options.quoteString as String : '"'
    String delimiter = options.containsKey('delimiter') ? options.delimiter as String : ','
    String rowDelimiter = options.containsKey('rowDelimiter')
      ? options.rowDelimiter as String
      : (options.rowdelimiter as String ?: '\n')
    String lineComment = options.containsKey('lineComment')
      ? options.lineComment as String
      : (options.linecomment as String ?: '#')
    boolean includeHeader = options.containsKey('includeHeader') ? options.includeHeader as boolean : true
    boolean includeTypes = options.containsKey('includeTypes') ? options.includeTypes as boolean : false
    boolean customQuoteString = options.containsKey('quoteString')

    StringBuilder sb = new StringBuilder()
    if (includeTypes || matrixName || !metaData.isEmpty()) {
      if (lineComment == null || lineComment.isEmpty()) {
        lineComment = '#'
      }
      if (matrixName) {
        sb.append(lineComment)
          .append('name: ')
          .append(matrixName)
          .append(rowDelimiter)
      }
      if (!metaData.isEmpty()) {
        metaData.each { key, value ->
          sb.append(lineComment)
            .append('metadata.')
            .append(key)
            .append(': ')
            .append(serializeMetadataValue(value))
            .append(rowDelimiter)
        }
      }
      if (includeTypes) {
        sb.append(lineComment)
          .append('types: ')
          .append(types().collect { typeLabel(it) }.join(','))
          .append(rowDelimiter)
      }
    }

    boolean wroteHeader = false
    if (includeHeader) {
      sb.append(buildCsvRow(columnNames(), quoteString, delimiter, rowDelimiter, null, customQuoteString))
      wroteHeader = true
    }

    int rowCount = rowCount()
    List<Class> columnTypes = types()
    for (int i = 0; i < rowCount; i++) {
      if (wroteHeader || i > 0 || includeHeader) {
        sb.append(rowDelimiter)
      }
      sb.append(buildCsvRow(row(i), quoteString, delimiter, rowDelimiter, columnTypes, customQuoteString))
      wroteHeader = true
    }
    sb.toString()
  }

  /**
   * Copy this matrix to the system clipboard in CSV format.
   *
   * Defaults: quoteString = '\"', delimiter = ', ', rowDelimiter = '\\n', includeHeader = true, includeTypes = true
   *
   * @param options optional parameters:
   *   - quoteString (String) string quote, default '\"'
   *   - delimiter (String) column delimiter, default ', '
   *   - rowDelimiter (String) row delimiter, default '\\n'
   *   - lineComment (String) comment prefix, default '#'
   *   - includeHeader (boolean) include header row, default true
   *   - includeTypes (boolean) include a types comment header, default true
   */
  void toClipboard(Map options = [:]) {
    if (!options.containsKey('includeTypes')) {
      options.includeTypes = true
    }
    if (!options.containsKey('includeHeader')) {
      options.includeHeader = true
    }
    ClipboardUtil.writeText(toCsvString(options))
  }


  @Override
  String toString() {
    return "${matrixName ?: 'a matrix with '}: ${rowCount()} obs * ${columnCount()} variables "
  }

  /**
   * Return the dimensions of the matrix as a map.
   *
   * @return a map with keys 'observations' (row count) and 'variables' (column count)
   */
  Map<String, Integer> dimensions() {
    ['observations': rowCount(), 'variables': columnCount()]
  }


  /**
   * Render a subset of rows as an HTML table.
   *
   * @param attr table attributes (e.g. id, class, align)
   * @param numRows number of rows to render
   * @param fromHead if true, render from the start; otherwise from the end
   * @param autoAlign if true, right-align numeric columns unless overridden by align attribute
   * @return HTML table string
   */
  String toHtml(Map<String, String> attr = [:], Integer numRows, boolean fromHead = true, boolean autoAlign = true) {
    List<?> r
    if (fromHead) {
      r = rows(0..numRows - 1)
    } else {
      r = rows(rowCount() - numRows..rowCount() - 1)
    }
    toHtml(attr, r, autoAlign)
  }

  /**
   * Render all rows as an HTML table.
   *
   * @param attr table attributes (e.g. id, class, align)
   * @param autoAlign if true, right-align numeric columns unless overridden by align attribute
   * @return HTML table string
   */
  String toHtml(Map<String, String> attr = [:], boolean autoAlign=true) {
    toHtml(attr, rows(), autoAlign)
  }

  /**
   * There are some additional attributes added to the table: each th and td element will have the column name and
   * the simple type name added as classes. All attributes added a parameter becomes a table attribute except the
   * align attribute which is used to set alignment on the columns. Here is an example:
   * <code><pre>
   * def empData = Matrix.builder()
   *   .columns(
   *     emp_id: 1..3,
   *     emp_name: ["Rick", "Dan", "Michelle"],
   *     salary: [623.3, 515.2, 611.0, 729.0, 843.25],
   *     start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23"))
   *   .types(int, String, Number, LocalDate)
   *   .build()
   * println empData.toHtml(id: 'mytable', class: 'table', align: 'emp_id: center, salary: right, start_date: center')
   * </pre></code>
   * This will print
   * <pre>
   * <table id="mytable" class="table">
   * <thead>
   * <tr>
   * <th class='emp_id Integer' style='text-align: left'>emp_id</th>
   * <th class='emp_name String'>emp_name</th>
   * <th class='salary Number' style='text-align: right'>salary</th>
   * <th class='start_date LocalDate' style='text-align: center'>start_date</th>
   * </tr>
   * </thead>
   * <tbody>
   * <tr>
   * <td class='emp_id Integer' style='text-align: left'>1</td>
   * <td class='emp_name String'>Rick</td>
   * <td class='salary Number' style='text-align: right'>623.3</td>
   * <td class='start_date LocalDate' style='text-align: center'>2013-01-01</td>
   * </tr>
   * <tr>
   * <td class='emp_id Integer' style='text-align: left'>2</td>
   * <td class='emp_name String'>Dan</td>
   * <td class='salary Number' style='text-align: right'>515.2</td>
   * <td class='start_date LocalDate' style='text-align: center'>2012-03-27</td>
   * </tr>
   * <tr>
   * <td class='emp_id Integer' style='text-align: left'>3</td>
   * <td class='emp_name String'>Michelle</td>
   * <td class='salary Number' style='text-align: right'>611.0</td>
   * <td class='start_date LocalDate' style='text-align: center'>2013-09-23</td>
   * </tr>
   * <tbody>
   * </table>
   * </pre>
   *
   * @param attr attributes to add to the table element except the align attributes which will result in a
   * style attribute added to the th and td elements matching the column name (see example above)
   * @param autoAlign if true all numbers will be right aligned. If the align key is set in the map this setting has no effect
   * @return a xhtml representation of the table
   */
  String toHtml(Map<String, String> attr = [:], List<?> rows, boolean autoAlign=true) {
    Map alignment = [:]
    StringBuilder sb = new StringBuilder()
    sb.append('<table')
    if (attr.size() > 0) {
      attr.each {k,v ->
        if (k == 'align') {
          v.split(',').each { s ->
            String a = s as String
            def key = a.substring(0, a.indexOf(':')).trim()
            def value = a.substring(a.indexOf(':')+1).trim()
            alignment.put( key, value)
          }
        } else {
          sb.append(' ').append(k).append('="').append(v).append('"')
        }
      }
    }
    if (alignment.isEmpty() && autoAlign) {
      columnNames().eachWithIndex { String colName, int idx ->
        if (Number.isAssignableFrom(type(idx))) {
          alignment.put(colName, 'right')
        }
      }
    }

    sb.append('>\n')
    if (mColumns.size() > 0) {
      sb.append('  <thead>\n')
      sb.append('    <tr>\n')

      columnNames().eachWithIndex { it, idx ->
        String colName = columnName(idx)
        sb.append("      <th class='$colName ${typeName(idx)}'")
        if (alignment.containsKey(colName)) {
          sb.append(" style='text-align: ${alignment[colName]}'")
        }
        sb.append('>').append(it).append('</th>\n')
      }
      sb.append('    </tr>\n  </thead>\n')
    }
    StringBuilder rowBuilder = new StringBuilder()
    sb.append('  <tbody>\n')
    for (row in rows) {
      rowBuilder.setLength(0)
      sb.append('    <tr>\n')
      row.eachWithIndex { Object val, int i ->
        rowBuilder.append("      <td class='${columnName(i)} ${typeName(i)}'")
        String colName = columnName(i)
        if (alignment.containsKey(colName)) {
          rowBuilder.append(" style='text-align: ${alignment[colName]}'")
        }
        rowBuilder.append('>').append(ValueConverter.asString(val)).append('</td>\n')
      }
      sb.append(rowBuilder).append('    </tr>\n')
    }
    sb.append('  <tbody>\n</table>\n')
    sb.toString()
  }

  /**
   * Render the matrix as a Markdown table.
   *
   * Column alignment is derived from column types (numbers right-aligned, others left).
   *
   * @param attr table attributes (e.g. id, class) appended for Markdown/HTML renderers
   * @return the markdown formatted table
   */
  String toMarkdown(Map<String, String> attr = [:]) {
    return toMarkdown(attr, calculateAlignment())
  }


  /**
   * Render a subset of rows as a Markdown table.
   *
   * @param numRows the number of rows to include
   * @param fromHead true to include rows from the start, false to include from the end
   * @return the markdown formatted table
   */
  String toMarkdown(int numRows, boolean fromHead = true) {
    toMarkdown([:], numRows, fromHead)
  }


  /**
   * Render a subset of rows as a Markdown table with attributes.
   *
   * @param attr table attributes (e.g. id, class) appended for Markdown/HTML renderers
   * @param numRows the number of rows to include
   * @param fromHead true to include rows from the start, false to include from the end
   * @return the markdown formatted table
   */
  String toMarkdown(Map<String, String> attr, int numRows, boolean fromHead = true) {
    List<?> r
    if (fromHead) {
      r = rows(0..numRows - 1)
    } else {
      r = rows(rowCount() - numRows..rowCount() - 1)
    }
    return toMarkdown(attr, calculateAlignment(), r)
  }


  /**
   * Render the matrix as a Markdown table using explicit alignment markers.
   *
   * @param attr table attributes (e.g. id, class) appended for Markdown/HTML renderers
   * @param alignment alignment markers per column (e.g. :---, :---:, ---:)
   * @return the markdown formatted table
   */
  String toMarkdown(Map<String, String> attr = [:], List<String> alignment) {
    toMarkdown(attr, alignment, rows())
  }

  /**
   * Render the provided rows as a Markdown table with explicit alignment.
   *
   * @param attr table attributes (e.g. id, class) appended for Markdown/HTML renderers
   * @param alignment alignment markers per column (e.g. :---, :---:, ---:)
   * @param rows the rows to render (Row or List instances)
   * @return the markdown formatted table
   */
  String toMarkdown(Map<String, String> attr = [:], List<String> alignment, List<?> rows) {
    if (alignment.size() != columnCount()) {
      throw new IllegalArgumentException("number of alignment markers (${alignment.size()}) differs from number of columns (${columnCount()})")
    }
    StringBuilder sb = new StringBuilder()
    sb.append('| ')
    sb.append(String.join(' | ', columnNames())).append(' |\n')
    sb.append('| ').append(String.join(' | ', alignment)).append(' |\n')
    StringBuilder rowBuilder = new StringBuilder()
    for (row in rows) {
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

  private List<String> calculateAlignment() {
    List<String> alignment = []
    for (type in types()) {
      if (Number.isAssignableFrom(type)) {
        alignment.add('---:')
      } else {
        alignment.add('---')
      }
    }
    alignment
  }

  private static String buildCsvRow(List<?> values, String quoteString, String delimiter, String rowDelimiter, List<Class> types, boolean customQuoteString) {
    StringBuilder rowBuilder = new StringBuilder()
    int max = values.size()
    for (int i = 0; i < max; i++) {
      Class type = (types != null && i < types.size()) ? types[i] : null
      rowBuilder.append(escapeCsvValue(values[i], quoteString, delimiter, rowDelimiter, type, customQuoteString))
      if (i < max - 1) {
        rowBuilder.append(delimiter)
      }
    }
    rowBuilder.toString()
  }

  private static String escapeCsvValue(Object value, String quoteString, String delimiter, String rowDelimiter, Class type, boolean customQuoteString) {
    String stringValue = ValueConverter.asString(value) ?: ''
    if (quoteString == null || quoteString.isEmpty()) {
      return stringValue
    }

    // Determine if we should quote based on type or special characters
    boolean shouldQuote = false

    // Quote string-typed values only when a custom quote string was specified
    if (customQuoteString && type == String && stringValue && !stringValue.isBlank()) {
      shouldQuote = true
    }

    // Always quote if the value contains special characters
    if (stringValue.contains(quoteString) ||
        stringValue.contains(delimiter) ||
        stringValue.contains(rowDelimiter) ||
        stringValue.contains('\n') ||
        stringValue.contains('\r')) {
      shouldQuote = true
    }

    if (!shouldQuote) {
      return stringValue
    }
    String escaped = stringValue.replace(quoteString, quoteString + quoteString)
    "${quoteString}${escaped}${quoteString}"
  }

  private static String typeLabel(Class type) {
    if (type != null && type.isPrimitive()) {
      return primitiveWrapper(type)
    }
    if (type == String || type == BigDecimal || type == BigInteger || type == Number || type == Object) {
      return type.simpleName
    }
    String pkg = type.package?.name ?: ''
    if (pkg.startsWith('java.lang') || pkg.startsWith('java.math') || pkg.startsWith('java.time') || pkg.startsWith('java.util') || pkg.startsWith('java.io') || pkg.startsWith('java.net')) {
      return type.simpleName
    }
    type.name
  }

  private static String serializeMetadataValue(Object value) {
    if (value == null) {
      return ''
    }
    if (value instanceof LocalDate) {
      return value.toString()
    }
    if (value instanceof LocalDateTime) {
      return value.toString()
    }
    return value.toString()
  }

  /**
   * Transpose the matrix using a column as the new header row.
   *
   * The values in {@code columnNameAsHeader} become the column names of the transposed matrix.
   * If {@code includeHeaderAsRow} is true, the original column names are inserted as the first data row.
   *
   * @param columnNameAsHeader the column whose values should become the new column names
   * @param includeHeaderAsRow if true, include the original header as the first row
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(String columnNameAsHeader, boolean includeHeaderAsRow = false) {
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
   * Transpose the matrix using a column as the new header row with explicit data types.
   *
   * @param columnNameAsHeader the column whose values should become the new column names
   * @param dataTypes the data types for the transposed columns
   * @param includeHeaderAsRow if true, include the original header as the first row
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(String columnNameAsHeader, List<Class> dataTypes, boolean includeHeaderAsRow = false) {
    List<String> header
    List<List> r = new ArrayList<>(rowCount() + 1)
    if (includeHeaderAsRow) {
      header = ['']
      header.addAll(ListConverter.toStrings(column(columnNameAsHeader)))
      r.add(columnNames())
    } else {
      header = ListConverter.toStrings(column(columnNameAsHeader))
    }
    r.addAll(rows())
    builder()
        .matrixName(mName)
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

    return transpose((1..numCols).collect { 'c' + it }, includeHeaderAsRow)
  }

  /**
   *
   * @param header
   * @param dataTypes
   * @param includeHeaderAsRow
   * @return a new Matrix turned 90 degrees
   */
  Matrix transpose(List<String> header, List<Class> dataTypes, boolean includeHeaderAsRow = false) {
    List<List> rows = new ArrayList<>(rowCount() + 1)
    if (includeHeaderAsRow) {
      rows.add(columnNames())
    }
    rows.addAll(mColumns.transpose() as List<List>)
    builder()
        .matrixName(mName)
        .columnNames(header)
        .columns(rows)
        .types(dataTypes)
        .build()
  }

  private void updateValues(List<Row> rows) {
    rows.eachWithIndex { Row row, int r ->
      row.eachWithIndex { Object value, int c ->
        mColumns[c].set(r, value)
      }
    }
  }

  /**
   * Apply a closure to each value in the specified column and return the resulting list.
   *
   * @param columnName the column to apply the operation to
   * @param operation the closure to apply to each value
   * @return a list containing the transformed values
   */
  List withColumn(String columnName, Closure operation) {
    def result = []
    column(columnName).each {
      result << operation.call(it)
    }
    result
  }

  /**
   * Apply a closure to each value in the specified column and return the resulting list.
   *
   * @param columnIndex the column to apply the operation to
   * @param operation the closure to apply to each value
   * @return a list containing the transformed values
   */
  List withColumn(int columnIndex, Closure operation) {
    withColumn(columnName(columnIndex), operation)
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
   * @param columnNames the names of the columns to include
   * @param operation the closure operation doing the calculation
   * @return a list with the result of the operations
   */
  List withColumns(List<String> columnNames, Closure operation) {
    def result = []
    columns(columnNames).transpose().each {
      result << operation.call(it)
    }
    return result
  }

  /**
   * @see #withColumns(List < String >, Closure)
   * @param colIndices the column indices to include
   * @param operation the closure operation doing the calculation
   * @return a list with the result of the operations
   */
  List withColumns(Number[] colIndices, Closure operation) {
    return withColumns(colIndices as int[], operation)
  }

  /**
   * @see #withColumns(List < String >, Closure)
   * @param colIndices the column indices to include
   * @param operation the closure operation doing the calculation
   * @return a list (a new column) with the result of the operations
   */
  List withColumns(int[] colIndices, Closure operation) {
    def result = []
    columns(colIndices as Integer[]).transpose().each {
      result << operation.call(it)
    }
    return result
  }

  /**
   * @see #withColumns(List < String >, Closure)
   * @param colIndices the column index range to include
   * @param operation the closure operation doing the calculation
   * @return a list (a new column) with the result of the operations
   */
  List withColumns(IntRange colIndices, Closure operation) {
    withColumns(colIndices as int[], operation)
  }

  /**
   * Sets the name of the table and return the table
   * @param name the name for the table
   * @return the table itself
   */
  Matrix withMatrixName(String name) {
    setMatrixName(name)
    return this
  }

  /**
   * metaData gives the ability to add useful meta data content to the matrix
   * e.g. creationDate, author etc.
   *
   * @return the metaData Map&lt;String,?&gt;
   */
  Map<String, ?> getMetaData() {
    return metaData
  }

  /**
   * Create a list of Object types sized to the supplied template.
   *
   * Note: This method is internal and will be made private in v3.7.0.
   *
   * @param template a collection used only for sizing
   * @return a list of Object classes matching the template size
   */
  static List<Class> createObjectTypes(Collection template) {
    [Object] * template.size() as List<Class>
  }

  /**
   * Move a value within a row from one column to another.
   *
   * @param rowIndex the row index to update
   * @param fromColumn the source column index
   * @param toColumn the destination column index
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix moveValue(int rowIndex, int fromColumn, int toColumn) {
    Row row = row(rowIndex)
    row.move(fromColumn, toColumn)
    this
  }

  /**
   * Move a value within a row from one column to another using column names.
   *
   * @param rowIndex the row index to update
   * @param fromColumn the source column name
   * @param toColumn the destination column name
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix moveValue(int rowIndex, String fromColumn, String toColumn) {
    moveValue(rowIndex, columnIndex(fromColumn), columnIndex(toColumn))
  }

  /**
   * Move a value within a row from one column to another using a name/index mix.
   *
   * @param rowIndex the row index to update
   * @param fromColumn the source column index
   * @param toColumn the destination column name
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix moveValue(int rowIndex, int fromColumn, String toColumn) {
    moveValue(rowIndex, fromColumn, columnIndex(toColumn))
  }

  /**
   * Move a value within a row from one column to another using a name/index mix.
   *
   * @param rowIndex the row index to update
   * @param fromColumn the source column name
   * @param toColumn the destination column index
   * @return this matrix (mutated) to allow method chaining
   */
  Matrix moveValue(int rowIndex, String fromColumn, int toColumn) {
    moveValue(rowIndex, columnIndex(fromColumn), toColumn)
  }
}
