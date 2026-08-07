package se.alipsa.matrix.core

import groovy.transform.CompileDynamic

import se.alipsa.matrix.core.util.ClassUtils

import java.text.NumberFormat

/**
 * A Grid is a rectangular layer on top of a List<List<T>>.
 * Note than unlike a Matrix, the getAt and putAt methods with
 * a single argument refers to a row and not (as with a Matrix) to a column.
 *
 * <p>Grid enforces a uniform row width: rows added after the first row must match
 * the established width. The constructors copy the supplied outer list and every
 * row, so later mutations of the source lists do not bypass Grid validation.</p>
 *
 * <p>Indexed value assignment (e.g. {@code grid[0][1] = value}) remains write-through,
 * but structural mutations of the public {@code data} view or its rows are rejected.</p>
 *
 * @param <T> the grid element type
 */
class Grid<T> implements Iterable<List<T>> {

  private static final String UNSUPPORTED_STRUCTURE_MUTATION = 'Structural mutation of Grid rows is not supported; use validated Grid methods to change the row structure.'

  private List<List<T>> data
  private final Class<?> elementType

  Grid() {
    data = []
    elementType = Object
  }

  Grid(List<List<T>> data) {
    elementType = Object
    if (isValid(data)) {
      this.data = copyRows(data)
    } else if (data instanceof List && isValid([data])) {
      this.data = [new ArrayList<>(data as List<T>)]
    } else {
      throw new IllegalArgumentException('data is invalid')
    }
  }

  /**
   * Create a Grid from row data and validate that all non-null values are
   * assignable to the supplied element type.
   *
   * @param data the row data
   * @param elementType the target element type
   * @throws IllegalArgumentException if the row structure is invalid or contains incompatible values
   */
  Grid(List<List<T>> data, Class<T> elementType) {
    this.elementType = safeElementType(elementType)
    if (isValid(data, this.elementType)) {
      this.data = copyRows(data)
    } else if (data instanceof List && isValid([data], this.elementType)) {
      this.data = [new ArrayList<>(data as List<T>)]
    } else {
      throw new IllegalArgumentException("data is invalid for element type ${this.elementType.simpleName}")
    }
  }

  Grid(int nrow) {
    data = new ArrayList<List<T>>(nrow)
    elementType = Object
  }

  Grid(int nrow, int ncol) {
    data = new ArrayList<List<T>>(nrow)
    elementType = Object
    nrow.times {
      data << ([null] * ncol) as List<T>
    }
  }

  Grid(T value, int nrow, int ncol) {
    data = new ArrayList<List<T>>(nrow)
    elementType = value == null ? Object : safeElementType(value.class)
    nrow.times {
      data << ([value] * ncol) as List<T>
    }
  }

  Grid(Map<String, Object> params) {
    this((T) params.value, params.nrow as int, params.ncol as int)
  }

  List<T> getAt(int row) {
    new CheckedGridRowView<>(data.get(row))
  }

  T getAt(int row, int column) {
    data.get(row).get(column)
  }

  Grid<T> leftShift(List<T> row) {
    add(row)
    this
  }

  boolean add(List<T> row) {
    validateNewRow(row)
    data.add(new ArrayList<>(row))
  }

  /**
   * Insert a row at the specified position.
   *
   * @param position the row index to insert the row at
   * @param row the row data
   */
  void add(int position, List<T> row) {
    validateNewRow(row)
    data.add(position, new ArrayList<>(row))
  }

  boolean addAll(List<List<T>> grid) {
    if (grid == null) {
      throw new IllegalArgumentException('Grid rows cannot be null')
    }
    if (grid.isEmpty()) {
      return false
    }
    int expectedWidth = establishedWidth()
    if (expectedWidth < 0) {
      if (grid[0] == null) {
        throw new IllegalArgumentException('Row 0 cannot be null')
      }
      expectedWidth = grid[0].size()
    }
    grid.eachWithIndex { List<T> row, int i ->
      validateRow(row, expectedWidth, "Row $i")
    }
    grid.each { List<T> row ->
      data.add(new ArrayList<>(row))
    }
    true
  }

  Grid plus(List<T> row) {
    def grid = new Grid<T>(copyRows(), elementType as Class<T>)
    grid.add(row)
    grid
  }

  String plus(String str) {
    data.toString() + str
  }

  void putAt(List<Integer> rowColumn, T value) {
    def row = data.get(rowColumn[0])
    Integer column = rowColumn[1]
    row.set(column, value)
  }

  /**
   * provides short notation for updating or adding a
   * Observation. Given a grid as follows
   * <code>
   * <pre>
   * Grid foo = [
   *     [12.0, 3.0, Math.PI],
   *     [1.9, 2, 3],
   *     [4.3, 2, 3]
   * ] as Grid
   * </pre>
   * </code>
   * The following will replace the second observation
   * <code>
   * <pre>
   * foo[1] = [1.7, 1, 5]
   * </pre>
   * </code>
   * and the following will append a new row
   * <code><pre>
   * foo[3] = [1.7, 1, 5]
   * </pre></code>
   *
   * @param rowIdx the row index to update when less than the number of rows or
   * append when equal the number of rows
   * @param values a list of variables representing the observation to update
   * @throws IllegalArgumentException when the index is null or negative
   * @throws IndexOutOfBoundsException when the index is larger than the size of the
   * number of observation in the Grid
   */
  void putAt(Integer rowIdx, List<T> values) {
    if (rowIdx == null) {
      throw new IllegalArgumentException('Observation index cannot be null')
    }
    if (rowIdx < 0) {
      throw new IllegalArgumentException('Observation index cannot be less than zero')
    }
    List<T> row = values.collect() as List<T>
    if (rowIdx < data.size()) {
      replaceRow(rowIdx, row)
    } else if (rowIdx == data.size()) {
      validateNewRow(row)
      data << new ArrayList<>(row)
    } else {
      throw new IndexOutOfBoundsException("Index $rowIdx cannot be greater than ${data.size()}")
    }
  }

  /**
   * @return a Map<String, Integer> of the number of observations (rows) and the number of
   * variables (columns) in the Grid with the keys 'observations' and 'variables'
   */
  Map<String, Integer> dimensions() {
    ['observations': data.size(), 'variables': data.isEmpty() ? 0 : data.max { it.size() }.size()]
  }

  @Override
  String toString() {
    StringBuilder sb = new StringBuilder('[\n')
    data.each { sb.append('  ').append(String.valueOf(it)).append('\n') }
    sb.append(']\n')
    sb.toString()
  }

  /**
   * @return a read-only outer view of the grid rows.
   * Changes to values through {@link List#set(int, Object)} on a row are reflected in the Grid,
   * but structural mutations of the outer list or any row are rejected.
   */
  List<List<T>> getData() {
    new CheckedGridDataView<>(data)
  }

  /**
   * Direct reassignment of the backing data list is not supported.
   */
  void setData(List<List<T>> data) {
    throw new UnsupportedOperationException("${UNSUPPORTED_STRUCTURE_MUTATION}: attempted to assign ${data}")
  }

  /**
   * @return a copy of the list of rows in the grid
   * Immutable, i.e. changes to the result is not reflected in the Grid unless
   * the grid contains mutable objects that are changed.
   */
  List<List<T>> getRowList() {
    def copy = new ArrayList(data.size())
    data.each {
      copy << it.collect()
    }
    copy
  }

  Grid replaceRow(int index, List<T> row) {
    validateNewRow(row)
    List<T> replacement = new ArrayList<>(row)
    def r = data.get(index)
    r.clear()
    r.addAll(replacement)
    this
  }

  Grid replaceColumn(int column, List<T> values) {
    if (values == null) {
      throw new IllegalArgumentException('Column values cannot be null')
    }
    int valSize = values.size()
    int rowCount = data.size()
    if (valSize != rowCount) {
      throw new IllegalArgumentException("Column values size ($valSize) must match row count ($rowCount)")
    }
    values.eachWithIndex { T value, int i ->
      validateValue(value, "Value at row $i")
    }
    data.eachWithIndex { List row, int i ->
      row[column] = values[i]
    }
    this
  }

  Iterator<List<T>> iterator() {
    getData().iterator()
  }

  Grid<T> transpose() {
    new Grid<T>(transpose(this.data), elementType as Class<T>)
  }

  static <N> Grid<N> convert(Grid grid, Integer colNum, Class<N> type, NumberFormat format = null) {
    List<List<N>> converted = convert(grid.data, colNum, type, format)
    coversAllColumns(grid.data, [colNum]) ? new Grid<N>(converted, type) : new Grid<N>(converted)
  }

  // Null in means "no single target column was supplied", so preserve the legacy null result.
  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  static <N> List<List<N>> convert(List<List<?>> rowList, Integer colNum, Class<N> type, NumberFormat format = null) {
    if (colNum == null) {
      return null
    }
    def m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      value = m[r][colNum]
      if (value == null) {
        continue
      }
      m[r].set(colNum, ValueConverter.convert(value, type, null, format))
    }
    m as List<List<N>>
  }

  static <N> Grid<N> convert(Grid grid, List<Integer> colNums, Class<N> type, NumberFormat format = null) {
    List<List<?>> converted = convert(grid.data, colNums, type, format)
    coversAllColumns(grid.data, colNums) ? new Grid<N>(converted as List<List<N>>, type) : new Grid<N>(converted as List<List<N>>)
  }

  @CompileDynamic
  static List<List<?>> convert(List<List<?>> rowList, List<Integer> colNums, Class<?> type, NumberFormat format = null) {
    def m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      for (int c in colNums) {
        value = m[r][c]
        if (value == null) {
          continue
        }
        if (format == null) {
          m[r].set(c, ValueConverter.convert(value, type))
        } else {
          m[r].set(c, ValueConverter.convert(value, type, null, format))
        }
      }
    }
    m
  }

  static Grid convert(Grid grid, int colNum, Closure converter) {
    new Grid(convert(grid.data, colNum, converter))
  }

  static List<List<?>> convert(List<List<?>> rowList, int colNum, Closure converter) {
    List<List> m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      value = m[r][colNum]
      if (value == null) {
        continue
      }
      m[r].set(colNum, converter.call(value))
    }
    m as List<List<?>>
  }

  static List<List> clone(List<List> rowList) {
    List<List> copy = new ArrayList<>(rowList.size())
    for (row in rowList) {
      copy.add(row.collect())
    }
    copy
  }

  static Grid transpose(Grid grid) {
    new Grid(transpose(grid.data))
  }

  // Preserve the historical null-in/null-out transpose contract.
  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  static List<List<?>> transpose(List<List<?>> rowList) {
    if (rowList == null) {
      return null
    }
    rowList.transpose()
  }

  /**
   * Validates that a Grid has a valid structure.
   *
   * @param grid the Grid to validate
   * @return true if the grid is valid, false otherwise
   */
  static boolean isValid(Grid grid) {
    isValid(grid.data)
  }

  /**
   * Validates that a Grid has a valid structure and that all non-null values are
   * compatible with the supplied element type.
   *
   * @param grid the Grid to validate
   * @param elementType the target element type to validate against
   * @return true if the grid is valid for the supplied type
   */
  static boolean isValid(Grid grid, Class<?> elementType) {
    isValid(grid?.data, elementType)
  }

  /**
   * Validates that a row list has a valid Grid structure.
   * A valid Grid must be:
   * - A non-null List of Lists
   * - All rows must have the same number of columns
   *
   * @param rowList the row list to validate
   * @return true if the row list forms a valid grid structure, false otherwise
   */
  @SuppressWarnings('DuplicateNumberLiteral')
  static boolean isValid(Object rowList) {
    if (rowList == null) {
      return false
    }
    if (!List.isInstance(rowList)) {
      return false
    }
    int numCols
    int prevNumCols = -1
    List list = rowList as List
    for (row in list) {
      if (!List.isInstance(row)) {
        return false
      }
      numCols = (row as List).size()
      if (prevNumCols == -1) {
        prevNumCols = numCols
      }
      if (numCols != prevNumCols) {
        return false
      }
      prevNumCols = numCols
    }
    true
  }

  /**
   * Validates that a row list has a valid Grid structure and that all non-null
   * values are assignable to the supplied element type.
   *
   * @param rowList the row list to validate
   * @param elementType the target element type to validate against
   * @return true if the row list forms a valid grid structure for the target type
   */
  static boolean isValid(Object rowList, Class<?> elementType) {
    if (!isValid(rowList)) {
      return false
    }
    Class<?> safeType = safeElementType(elementType)
    List list = rowList as List
    for (row in list) {
      for (value in (row as List)) {
        if (value != null && !safeType.isInstance(value)) {
          return false
        }
      }
    }
    true
  }

  private static Class<?> safeElementType(Class<?> elementType) {
    ClassUtils.convertPrimitiveToWrapper(elementType) ?: Object
  }

  private static boolean coversAllColumns(List<List<?>> rowList, List<Integer> colNums) {
    if (rowList == null || rowList.isEmpty()) {
      return true
    }
    Set<Integer> columns = colNums as Set<Integer>
    columns == (0..<rowList[0].size()) as Set<Integer>
  }

  private List<List<T>> copyRows() {
    data.collect { List<T> row -> row.collect() as List<T> }
  }

  private static <T> List<List<T>> copyRows(List<List<T>> rows) {
    rows.collect { List<T> row -> new ArrayList<>(row) }
  }

  private int establishedWidth() {
    data.isEmpty() ? -1 : data[0].size()
  }

  private void validateNewRow(List<T> row) {
    if (row == null) {
      throw new IllegalArgumentException('Row cannot be null')
    }
    int expectedWidth = establishedWidth()
    if (expectedWidth >= 0 && row.size() != expectedWidth) {
      throw new IllegalArgumentException("Row width (${row.size()}) does not match grid width ($expectedWidth)")
    }
    row.eachWithIndex { T value, int i ->
      validateValue(value, "Value at column $i")
    }
  }

  private void validateRow(List<T> row, int expectedWidth, String context) {
    if (row == null) {
      throw new IllegalArgumentException("$context cannot be null")
    }
    if (row.size() != expectedWidth) {
      throw new IllegalArgumentException("$context width (${row.size()}) does not match grid width ($expectedWidth)")
    }
    row.eachWithIndex { T value, int i ->
      validateValue(value, "$context value at column $i")
    }
  }

  private void validateValue(T value, String context) {
    if (value != null && elementType != Object && !elementType.isInstance(value)) {
      throw new IllegalArgumentException("${context} is ${value.class.simpleName} but expected ${elementType.simpleName}")
    }
  }

  /**
   * A live view of a single Grid row. Value assignment writes through to the backing row;
   * structural operations are rejected.
   */
  private static class CheckedGridRowView<T> extends AbstractList<T> {

    private final List<T> backingRow

    CheckedGridRowView(List<T> backingRow) {
      this.backingRow = backingRow
    }

    @Override
    T get(int index) {
      backingRow.get(index)
    }

    @Override
    int size() {
      backingRow.size()
    }

    @Override
    T set(int index, T element) {
      backingRow.set(index, element)
    }

    @Override
    void add(int index, T element) {
      throw new UnsupportedOperationException(UNSUPPORTED_STRUCTURE_MUTATION)
    }

    @Override
    T remove(int index) {
      throw new UnsupportedOperationException(UNSUPPORTED_STRUCTURE_MUTATION)
    }
  }

  /**
   * A read-only outer view of a Grid. It returns checked row views that allow
   * write-through value assignment but rejects structural mutation of the outer list
   * and direct row reassignment.
   */
  private static class CheckedGridDataView<T> extends AbstractList<List<T>> {

    private final List<List<T>> backingData

    CheckedGridDataView(List<List<T>> backingData) {
      this.backingData = backingData
    }

    @Override
    List<T> get(int index) {
      new CheckedGridRowView<>(backingData.get(index))
    }

    @Override
    int size() {
      backingData.size()
    }

    @Override
    List<T> set(int index, List<T> element) {
      throw new UnsupportedOperationException(UNSUPPORTED_STRUCTURE_MUTATION)
    }

    @Override
    void add(int index, List<T> element) {
      throw new UnsupportedOperationException(UNSUPPORTED_STRUCTURE_MUTATION)
    }

    @Override
    List<T> remove(int index) {
      throw new UnsupportedOperationException(UNSUPPORTED_STRUCTURE_MUTATION)
    }
  }

}
