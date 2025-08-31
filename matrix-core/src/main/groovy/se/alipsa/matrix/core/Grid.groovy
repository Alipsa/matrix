package se.alipsa.matrix.core

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.text.NumberFormat

/**
 * A Grid is thin layer on top of a List<List<T>>.
 * Note than unlike a Matrix, the getAt and putAt methods with
 * a single argument refers to a row and not (as with a Matrix) to a column.
 *
 *
 * @param <T>
 */
@CompileStatic
class Grid<T> implements Iterable<List<T>> {

  List<List<T>> data

  Grid() {
    data = []
  }

  Grid(List<List<T>> data) {
    if (isValid(data)) {
      this.data = data
    } else if (data instanceof List) {
      this.data = [data] as List<List<T>>
    } else {
      throw new IllegalArgumentException("data is invalid")
    }
  }

  Grid(int nrow) {
    data = new ArrayList<List<T>>(nrow)
  }

  Grid(int nrow, int ncol) {
    data = new ArrayList<List<T>>(nrow)
    for (row in 1..nrow) {
      data << new ArrayList<T>(ncol)
    }
  }


  Grid(T value, int nrow, int ncol) {
    data = new ArrayList<List<T>>(nrow)
    for (row in 1..nrow) {
      data << ([value] * ncol) as List<T>
    }
  }

  Grid(Map<String, Object> params) {
    this((T) params.value, params.nrow as int, params.ncol as int)
  }

  List<T> getAt(int row) {
    return data[row]
  }

  T getAt(int row, int column) {
    return data[row][column]
  }

  def leftShift(List<T> row) {
    data << row
  }

  boolean add(List<T> row) {
    data.add(row)
  }

  /**
   * Insert a row i the specified position.
   *
   * @param position the row index to insert the row at
   * @param row the row data
   * @return the mutated Grid
   */
  boolean add(int position, List<T> row) {
    data.add(position, row)
  }

  boolean addAll(List<List<T>> grid) {
    data.addAll(grid)
  }

  Grid plus(List<T> row) {
    def grid = new Grid()
    grid.addAll(data)
    grid.add(row)
    return grid
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
      throw new IllegalArgumentException("Observation index cannot be null")
    }
    if (rowIdx < 0) {
      throw new IllegalArgumentException("Observation index cannot be less than zero")
    }
    if (rowIdx < data.size()) {
      replaceRow(rowIdx, values.collect())
    } else if (rowIdx == data.size()) {
      data << values.collect()
    } else {
      throw new IndexOutOfBoundsException("Index $rowIdx cannot be greater than ${data.size()}")
    }
  }

  /**
   *
   * @return a Map<String, Integer> of the number of observations (rows) and the number of
   * variables (columns) in the Grid with the keys 'observations' and 'variables'
   */
  Map<String, Integer> dimensions() {
    ['observations': data.size(), 'variables': data.collect() { it.size() }.max()]
  }

  String toString() {
    StringBuilder sb = new StringBuilder('[\n')
    data.each { sb.append('  ').append(String.valueOf(it)).append('\n') }
    sb.append(']\n')
  }

  /**
   *
   * @return the list of rows in the grid.
   * Note that this enables mutability, i.e. changes to the result is reflected in the Grid
   */
  List<List<T>> getData() {
    return data
  }

  /**
   *
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
    def r = data.get(index)
    r.clear()
    r.addAll(row)
    this
  }

  Grid replaceColumn(int column, List<T> values) {
    data.eachWithIndex { List row, int i ->
      row[column] = values[i]
    }
    this
  }

  Iterator<List<T>> iterator() {
    return data.iterator()
  }

  Grid<T> transpose() {
    new Grid<T>(transpose(this.data))
  }

  static <N> Grid<N> convert(Grid grid, Integer colNum, Class<N> type, NumberFormat format = null) {
    return new Grid(convert(grid.data, [colNum], type, format))
  }

  static <N> List<List<N>> convert(List<List<?>> rowList, Integer colNum, Class<N> type, NumberFormat format = null) {
    if (colNum == null) return null
    def m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      value = m[r][colNum]
      if (value == null) continue
      m[r].set(colNum, ValueConverter.convert(value, type, null, format))
    }
    return m as List<List<N>>
  }

  static <N> Grid<N> convert(Grid grid, List<Integer> colNums, Class<N> type, NumberFormat format = null) {
    return new Grid(convert(grid.data, colNums, type, format))
  }

  @CompileDynamic
  static List<List<?>> convert(List<List<?>> rowList, List<Integer> colNums, Class<?> type, NumberFormat format = null) {
    def m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      for (int c in colNums) {
        value = m[r][c]
        if (value == null) continue
        if (format == null) {
          m[r].set(c, ValueConverter.convert(value, type))
        } else {
          m[r].set(c, ValueConverter.convert(value, type, null, format))
        }
      }
    }
    return m
  }

  static Grid convert(Grid grid, int colNum, Closure converter) {
    return new Grid(convert(grid.data, colNum, converter))
  }

  static List<List<?>> convert(List<List<?>> rowList, int colNum, Closure converter) {
    List<List> m = clone(rowList)
    def value
    for (int r = 0; r < m.size(); r++) {
      value = m[r][colNum]
      if (value == null) continue
      m[r].set(colNum, converter.call(value))
    }
    return m as List<List<?>>
  }

  static List<List> clone(List<List> rowList) {
    List<List> copy = new ArrayList<>(rowList.size())
    for (row in rowList) {
      copy.add(row.collect())
    }
    return copy
  }

  static Grid transpose(Grid grid) {
    return new Grid(transpose(grid.data))
  }

  static List<List<?>> transpose(List<List<?>> rowList) {
    if (rowList == null) return null
    return rowList.transpose()
  }

  static boolean isValid(Grid grid) {
    return isValid(grid.data)
  }

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
    return true
  }


}
