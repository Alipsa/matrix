package se.alipsa.matrix.core

/**
 * Join operations for combining matrices on one or more key columns.
 *
 * <p>Supports inner, left, right, full outer, and cross joins with single or
 * composite (multi-column) keys. When both matrices contain columns with the same
 * name, the result columns are suffixed {@code _x} and {@code _y}.</p>
 *
 * <p>One-to-many joins are fully supported: if a key in y has multiple matching
 * rows, each match produces a separate result row (cross product).</p>
 */
@SuppressWarnings(['JavadocEmptyFirstLine', 'Instanceof', 'NestedForLoop'])
class Joiner {

  /**
   * Merges two matrices on specified columns.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by a map with keys 'x' and 'y' whose values are column name(s).
   *           Single key: {@code [x: 'id', y: 'guid']}.
   *           Multi-key: {@code [x: ['dept', 'id'], y: ['department', 'empId']]}
   * @param all false for inner join, true for left join (backward compatible)
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, Map by, boolean all = false) {
    merge(x, y, by, all ? JoinType.LEFT : JoinType.INNER)
  }

  /**
   * Merges two matrices on a column with the same name in both.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by the column name present in both matrices
   * @param all false for inner join, true for left join (backward compatible)
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, String by, boolean all = false) {
    doMerge(x, y, [by], [by], all ? JoinType.LEFT : JoinType.INNER)
  }

  /**
   * Merges two matrices using an explicit {@link JoinType}.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by a map with keys 'x' and 'y' (see {@link #merge(Matrix, Matrix, Map, boolean)})
   * @param joinType the type of join to perform
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, Map by, JoinType joinType) {
    List<List<String>> keys = normalizeKeys(by)
    doMerge(x, y, keys[0], keys[1], joinType)
  }

  /**
   * Merges two matrices on a single column with the same name using an explicit {@link JoinType}.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by the column name present in both matrices
   * @param joinType the type of join to perform
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, String by, JoinType joinType) {
    doMerge(x, y, [by], [by], joinType)
  }

  /**
   * Merges two matrices on multiple columns with the same names using an explicit {@link JoinType}.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by the column names present in both matrices
   * @param joinType the type of join to perform
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, List<String> by, JoinType joinType) {
    doMerge(x, y, by, by, joinType)
  }

  /**
   * Produces the Cartesian product of two matrices (cross join). Every row from x
   * is paired with every row from y, producing {@code x.rowCount() * y.rowCount()} rows.
   * No key column is required. Columns with the same name are suffixed {@code _x} / {@code _y}.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @return a new Matrix with all column combinations and {@code x.rowCount() * y.rowCount()} rows
   */
  static Matrix crossJoin(Matrix x, Matrix y) {
    List<Integer> allYIndices = (0..<y.columnCount()) as List<Integer>
    ResultColumns rc = computeResultColumns(x, y, [], allYIndices)

    int xColCount = x.columnCount()
    int yColCount = y.columnCount()
    int xRowCount = x.rowCount()
    int yRowCount = y.rowCount()
    List<List<Object>> resultRows = new ArrayList<>(xRowCount * yRowCount)

    for (int xr = 0; xr < xRowCount; xr++) {
      List<Object> xRow = extractRow(x, xr, xColCount)
      for (int yr = 0; yr < yRowCount; yr++) {
        resultRows.add(xRow + extractRow(y, yr, yColCount))
      }
    }

    Matrix.builder()
        .matrixName(x.matrixName)
        .columnNames(rc.names)
        .rows(resultRows)
        .types(rc.types)
        .build()
  }

  @SuppressWarnings('ParameterCount')
  private static Matrix doMerge(Matrix x, Matrix y,
                                List<String> xKeyNames, List<String> yKeyNames,
                                JoinType joinType) {
    List<Integer> xKeyIndices = resolveIndices(x, xKeyNames)
    List<Integer> yKeyIndices = resolveIndices(y, yKeyNames)

    boolean filterOnly = joinType == JoinType.SEMI || joinType == JoinType.ANTI

    Set<Integer> yKeyIndexSet = yKeyIndices as Set<Integer>
    List<Integer> yNonKeyIndices = filterOnly
        ? []
        : (0..<y.columnCount()).findAll { int i -> !yKeyIndexSet.contains(i) }

    ResultColumns rc = filterOnly
        ? new ResultColumns(names: x.columnNames(), types: x.types())
        : computeResultColumns(x, y, xKeyNames, yNonKeyIndices)

    Map<List<Object>, List<List<Object>>> yIndex = buildIndex(y, yKeyIndices, yNonKeyIndices)

    List<List<Object>> resultRows = []
    Set<List<Object>> matchedYKeys = [] as Set<List<Object>>
    int xColCount = x.columnCount()
    int yNonKeyCount = yNonKeyIndices.size()
    List<Object> nullYRow = Collections.nCopies(yNonKeyCount, null)

    for (int r = 0; r < x.rowCount(); r++) {
      List<Object> key = extractKey(x, r, xKeyIndices)
      List<Object> xRow = extractRow(x, r, xColCount)

      List<List<Object>> yMatches = yIndex.get(key)
      if (yMatches != null) {
        if (joinType == JoinType.SEMI) {
          resultRows.add(xRow)
        } else if (joinType != JoinType.ANTI) {
          matchedYKeys.add(key)
          for (List<Object> yVals : yMatches) {
            resultRows.add(xRow + yVals)
          }
        }
      } else if (joinType == JoinType.ANTI) {
        resultRows.add(xRow)
      } else if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
        resultRows.add(xRow + nullYRow)
      }
    }

    if (joinType == JoinType.RIGHT || joinType == JoinType.FULL) {
      appendUnmatchedYRows(resultRows, yIndex, matchedYKeys,
          xColCount, xKeyIndices, yKeyNames.size(), joinType)
    }

    Matrix.builder()
        .matrixName(x.matrixName)
        .columnNames(rc.names)
        .rows(resultRows)
        .types(rc.types)
        .build()
  }

  private static void appendUnmatchedYRows(List<List<Object>> resultRows,
                                           Map<List<Object>, List<List<Object>>> yIndex,
                                           Set<List<Object>> matchedYKeys,
                                           int xColCount, List<Integer> xKeyIndices,
                                           int keyCount, JoinType joinType) {
    for (Map.Entry<List<Object>, List<List<Object>>> entry : yIndex.entrySet()) {
      List<Object> yKey = entry.key
      if (joinType == JoinType.FULL && matchedYKeys.contains(yKey)) {
        continue
      }
      if (joinType == JoinType.RIGHT && matchedYKeys.contains(yKey)) {
        continue
      }
      for (List<Object> yVals : entry.value) {
        List<Object> xRow = new ArrayList<Object>(Collections.nCopies(xColCount, null))
        for (int k = 0; k < keyCount; k++) {
          xRow.set(xKeyIndices[k], yKey[k])
        }
        resultRows.add(xRow + yVals)
      }
    }
  }

  private static List<Integer> resolveIndices(Matrix m, List<String> colNames) {
    List<Integer> indices = new ArrayList<>(colNames.size())
    for (String name : colNames) {
      int idx = m.columnIndex(name)
      if (idx < 0) {
        throw new IllegalArgumentException(
            "Join column '${name}' does not exist in matrix '${m.matrixName}'")
      }
      indices.add(idx)
    }
    indices
  }

  private static List<List<String>> normalizeKeys(Map by) {
    Object xVal = by.get('x')
    Object yVal = by.get('y')
    if (xVal == null || yVal == null) {
      throw new IllegalArgumentException("Join key map must contain both 'x' and 'y' entries")
    }
    List<String> xKeys = xVal instanceof List ? (xVal as List<String>) : [xVal as String]
    List<String> yKeys = yVal instanceof List ? (yVal as List<String>) : [yVal as String]
    if (xKeys.size() != yKeys.size()) {
      throw new IllegalArgumentException(
          "Join key lists must have the same size: x has ${xKeys.size()}, y has ${yKeys.size()}")
    }
    [xKeys, yKeys]
  }

  private static Map<List<Object>, List<List<Object>>> buildIndex(
      Matrix m, List<Integer> keyIndices, List<Integer> valueIndices) {
    Map<List<Object>, List<List<Object>>> index = [:]
    int rowCount = m.rowCount()
    for (int r = 0; r < rowCount; r++) {
      List<Object> key = extractKey(m, r, keyIndices)
      List<List<Object>> bucket = index.get(key)
      if (bucket == null) {
        bucket = []
        index.put(key, bucket)
      }
      List<Object> vals = new ArrayList<>(valueIndices.size())
      for (int vi : valueIndices) {
        vals.add(m.column(vi)[r])
      }
      bucket.add(vals)
    }
    index
  }

  private static List<Object> extractKey(Matrix m, int row, List<Integer> keyIndices) {
    List<Object> key = new ArrayList<>(keyIndices.size())
    for (int ki : keyIndices) {
      key.add(m.column(ki)[row])
    }
    key
  }

  private static List<Object> extractRow(Matrix m, int row, int colCount) {
    List<Object> vals = new ArrayList<>(colCount)
    for (int c = 0; c < colCount; c++) {
      vals.add(m.column(c)[row])
    }
    vals
  }

  private static ResultColumns computeResultColumns(Matrix x, Matrix y,
                                                    List<String> xKeyNames,
                                                    List<Integer> yNonKeyIndices) {
    Set<String> xKeyNameSet = xKeyNames as Set<String>
    List<String> xColNames = x.columnNames()
    List<Class> xColTypes = x.types()

    List<String> yNonKeyNames = new ArrayList<>(yNonKeyIndices.size())
    List<Class> yNonKeyTypes = new ArrayList<>(yNonKeyIndices.size())
    List<String> yColNames = y.columnNames()
    List<Class> yColTypes = y.types()
    for (int i : yNonKeyIndices) {
      yNonKeyNames.add(yColNames[i])
      yNonKeyTypes.add(yColTypes[i])
    }

    Set<String> xNonKeyNames = new LinkedHashSet<>(xColNames)
    xNonKeyNames.removeAll(xKeyNameSet)
    Set<String> yNonKeyNameSet = new LinkedHashSet<>(yNonKeyNames)
    Set<String> duplicates = new LinkedHashSet<>(xNonKeyNames)
    duplicates.retainAll(yNonKeyNameSet)

    List<String> resultNames = new ArrayList<>(xColNames.size() + yNonKeyNames.size())
    for (String n : xColNames) {
      resultNames.add(duplicates.contains(n) && !xKeyNameSet.contains(n) ? "${n}_x" as String : n)
    }
    for (String n : yNonKeyNames) {
      resultNames.add(duplicates.contains(n) ? "${n}_y" as String : n)
    }

    List<Class> resultTypes = new ArrayList<>(xColTypes)
    resultTypes.addAll(yNonKeyTypes)

    new ResultColumns(names: resultNames, types: resultTypes)
  }

  private static class ResultColumns {

    List<String> names
    List<Class> types

  }

}
