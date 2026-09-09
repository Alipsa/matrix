package se.alipsa.matrix.core

import static se.alipsa.matrix.core.util.ClassUtils.primitiveWrapper

import se.alipsa.matrix.core.util.ValueComparison

/**
 * Join operations for combining matrices on one or more key columns.
 *
 * <p>Supports inner, left, right, full outer, and cross joins with single or
 * composite (multi-column) keys. When both matrices contain columns with the same
 * name, the result columns are suffixed {@code _x} and {@code _y}.</p>
 *
 * <p>One-to-many joins are fully supported: if a key in y has multiple matching
 * rows, each match produces a separate result row (cross product).</p>
 *
 * <p>Finite numeric keys compare by mathematical value across numeric runtime types.
 * String keys do not match numeric keys. Null, NaN, and infinite key values never
 * match, following SQL-style null semantics.</p>
 *
 * <p><b>Column name collisions:</b> if x already contains a column whose name ends
 * with {@code _y} (or {@code _x}) and a suffix is needed, the result may contain
 * duplicate column names. This matches pandas behavior.</p>
 */
@SuppressWarnings('JavadocEmptyFirstLine')
class Joiner {

  /**
   * Merges two matrices on specified columns.
   *
   * @param x the left Matrix
   * @param y the right Matrix
   * @param by a map with keys 'x' and 'y' whose values are column name(s).
   *           Values are typed as Object because they can be either String (single key)
   *           or List&lt;String&gt; (composite key); Groovy type erasure prevents separate
   *           Map&lt;String, String&gt; and Map&lt;String, List&lt;String&gt;&gt; overloads.
   *           Single key: {@code [x: 'id', y: 'guid']}.
   *           Multi-key: {@code [x: ['dept', 'id'], y: ['department', 'empId']]}
   * @param all false for inner join, true for left join (backward compatible)
   * @return a new Matrix with the joined rows
   */
  static Matrix merge(Matrix x, Matrix y, Map<String, Object> by, boolean all = false) {
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
  static Matrix merge(Matrix x, Matrix y, Map<String, Object> by, JoinType joinType) {
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
    ResultColumns rc = computeResultColumns(x, y, [], [], allYIndices, JoinType.CROSS)

    int xColCount = x.columnCount()
    int yColCount = y.columnCount()
    int xRowCount = x.rowCount()
    int yRowCount = y.rowCount()
    long resultSize = (long) xRowCount * yRowCount
    if (resultSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "Cross join would produce ${resultSize} rows, exceeding maximum list size")
    }
    List<List<Object>> resultRows = new ArrayList<>((int) resultSize)

    List<List<Object>> xCols = (0..<xColCount).collect { x.column(it) as List<Object> }
    List<List<Object>> yCols = (0..<yColCount).collect { y.column(it) as List<Object> }

    (0..<xRowCount).each { int xr ->
      List<Object> xRow = extractFromCols(xCols, xr)
      (0..<yRowCount).each { int yr ->
        resultRows << xRow + extractFromCols(yCols, yr)
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
    if (joinType == JoinType.CROSS) {
      return crossJoin(x, y)
    }
    List<Integer> xKeyIndices = resolveIndices(x, xKeyNames)
    List<Integer> yKeyIndices = resolveIndices(y, yKeyNames)

    boolean filterOnly = joinType == JoinType.SEMI || joinType == JoinType.ANTI

    Set<Integer> yKeyIndexSet = yKeyIndices as Set<Integer>
    List<Integer> yNonKeyIndices = filterOnly
        ? []
        : (0..<y.columnCount()).findAll { int i -> !yKeyIndexSet.contains(i) }

    ResultColumns rc = filterOnly
        ? new ResultColumns(names: x.columnNames(), types: x.types())
        : computeResultColumns(x, y, xKeyNames, yKeyNames, yNonKeyIndices, joinType)

    JoinIndex yJoinIndex = buildIndex(y, yKeyIndices, yNonKeyIndices)
    Map<List<Object>, List<List<Object>>> yIndex = yJoinIndex.rows

    List<List<Object>> resultRows = []
    boolean needsMatchTracking = joinType == JoinType.RIGHT || joinType == JoinType.FULL
    Set<List<Object>> matchedYKeys = needsMatchTracking ? ([] as Set<List<Object>>) : null
    int xColCount = x.columnCount()
    int yNonKeyCount = yNonKeyIndices.size()
    List<Object> nullYRow = Collections.nCopies(yNonKeyCount, null)
    int xRowCount = x.rowCount()

    List<List<Object>> xKeyCols = xKeyIndices.collect { x.column(it) as List<Object> }
    List<List<Object>> xAllCols = (0..<xColCount).collect { x.column(it) as List<Object> }

    (0..<xRowCount).each { int r ->
      List<Object> key = normalizeJoinKey(extractFromCols(xKeyCols, r))
      List<Object> xRow = extractFromCols(xAllCols, r)

      List<List<Object>> yMatches = isMatchableKey(key) ? yIndex.get(key) : null
      if (yMatches != null) {
        if (joinType == JoinType.SEMI) {
          resultRows.add(xRow)
        } else if (joinType != JoinType.ANTI) {
          if (needsMatchTracking) {
            matchedYKeys.add(key)
          }
          yMatches.each { List<Object> yVals ->
            resultRows << xRow + yVals
          }
        }
      } else if (joinType == JoinType.ANTI) {
        resultRows.add(xRow)
      } else if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
        resultRows.add(xRow + nullYRow)
      }
    }

    if (joinType == JoinType.RIGHT || joinType == JoinType.FULL) {
      List<Class> xKeyTypes = xKeyIndices.collect { x.type(it) }
      appendUnmatchedYRows(resultRows, yIndex, yJoinIndex.rawKeys, matchedYKeys,
          xColCount, xKeyIndices, xKeyTypes, yKeyNames.size())
    }

    Matrix.builder()
        .matrixName(x.matrixName)
        .columnNames(rc.names)
        .rows(resultRows)
        .types(rc.types)
        .build()
  }

  @SuppressWarnings('ParameterCount')
  private static void appendUnmatchedYRows(List<List<Object>> resultRows,
                                           Map<List<Object>, List<List<Object>>> yIndex,
                                           Map<List<Object>, List<Object>> rawKeys,
                                           Set<List<Object>> matchedYKeys,
                                           int xColCount, List<Integer> xKeyIndices,
                                           List<Class> xKeyTypes, int keyCount) {
    yIndex.each { List<Object> yKey, List<List<Object>> yRows ->
      if (matchedYKeys.contains(yKey)) {
        return
      }
      yRows.each { List<Object> yVals ->
        List<Object> xRow = ([null] * xColCount) as List<Object>
        (0..<keyCount).each { int k ->
          xRow.set(xKeyIndices[k], convertUnmatchedKey(rawKeys[yKey][k], xKeyTypes[k]))
        }
        resultRows << xRow + yVals
      }
    }
  }

  /**
   * Converts an unmatched y key to the x key column's declared type, but only
   * when the conversion is lossless. Narrowing conversions that would change
   * the numeric value (4.5 to 4), or that collapse non-finite values (NaN,
   * infinity), keep the raw y key so the emitted value stays truthful.
   */
  private static Object convertUnmatchedKey(Object rawKey, Class xKeyType) {
    if (rawKey == null) {
      return null
    }
    Object converted = ValueConverter.convert(rawKey, xKeyType)
    if (converted == null) {
      return rawKey
    }
    if (rawKey instanceof Number && converted instanceof Number) {
      BigDecimal rawDecimal = ValueConverter.asBigDecimal(rawKey as Number)
      BigDecimal convertedDecimal = ValueConverter.asBigDecimal(converted as Number)
      if (rawDecimal == null || convertedDecimal == null || rawDecimal != convertedDecimal) {
        return rawKey
      }
    }
    converted
  }

  private static List<Integer> resolveIndices(Matrix m, List<String> colNames) {
    List<Integer> indices = []
    colNames.each { String name ->
      int idx = m.columnIndex(name)
      if (idx < 0) {
        throw new IllegalArgumentException(
            "Join column '${name}' does not exist in matrix '${m.matrixName}'")
      }
      indices << idx
    }
    indices
  }

  @SuppressWarnings('DuplicateStringLiteral')
  private static List<List<String>> normalizeKeys(Map<String, Object> by) {
    if (!by.containsKey('x') || !by.containsKey('y')) {
      throw new IllegalArgumentException("Join key map must contain both 'x' and 'y' entries")
    }
    Object xVal = by.get('x')
    Object yVal = by.get('y')
    if (xVal == null || yVal == null) {
      throw new IllegalArgumentException("Join key values for 'x' and 'y' must not be null")
    }
    List<String> xKeys = xVal instanceof List ? (xVal as List<String>) : [xVal as String]
    List<String> yKeys = yVal instanceof List ? (yVal as List<String>) : [yVal as String]
    if (xKeys.size() != yKeys.size()) {
      throw new IllegalArgumentException(
          "Join key lists must have the same size: x has ${xKeys.size()}, y has ${yKeys.size()}")
    }
    [xKeys, yKeys]
  }

  private static JoinIndex buildIndex(
      Matrix m, List<Integer> keyIndices, List<Integer> valueIndices) {
    Map<List<Object>, List<List<Object>>> index = [:]
    Map<List<Object>, List<Object>> rawKeys = [:]
    int rowCount = m.rowCount()
    List<List<Object>> keyCols = keyIndices.collect { m.column(it) as List<Object> }
    List<List<Object>> valCols = valueIndices.collect { m.column(it) as List<Object> }
    (0..<rowCount).each { int r ->
      List<Object> rawKey = extractFromCols(keyCols, r)
      List<Object> key = normalizeJoinKey(rawKey)
      rawKeys.putIfAbsent(key, rawKey)
      index.computeIfAbsent(key) { [] } << extractFromCols(valCols, r)
    }
    new JoinIndex(rows: index, rawKeys: rawKeys)
  }

  private static List<Object> extractFromCols(List<List<Object>> cols, int row) {
    List<Object> result = new ArrayList<>(cols.size())
    cols.each { List<Object> col -> result.add(col[row]) }
    result
  }

  private static List<Object> normalizeJoinKey(List<Object> key) {
    key.collect { ValueComparison.normalizeKey(it) }
  }

  private static boolean isMatchableKey(List<Object> key) {
    key.every { Object value ->
      if (value == null) {
        return false
      }
      if (value instanceof Double || value instanceof Float) {
        double number = value.doubleValue()
        return !Double.isNaN(number) && !Double.isInfinite(number)
      }
      true
    }
  }

  @SuppressWarnings('ParameterCount')
  private static ResultColumns computeResultColumns(Matrix x, Matrix y,
                                                    List<String> xKeyNames,
                                                    List<String> yKeyNames,
                                                    List<Integer> yNonKeyIndices,
                                                    JoinType joinType) {
    Set<String> xKeyNameSet = xKeyNames as Set<String>
    List<String> xColNames = x.columnNames()
    List<Class> xColTypes = x.types()

    List<String> yNonKeyNames = []
    List<Class> yNonKeyTypes = []
    List<String> yColNames = y.columnNames()
    List<Class> yColTypes = y.types()
    yNonKeyIndices.each { int i ->
      yNonKeyNames << yColNames[i]
      yNonKeyTypes << yColTypes[i]
    }

    Set<String> yNonKeyNameSet = yNonKeyNames as Set<String>
    Set<String> duplicates = xColNames as Set<String>
    duplicates.retainAll(yNonKeyNameSet)

    List<String> resultNames = xColNames.collect { String n ->
      duplicates.contains(n) && !xKeyNameSet.contains(n) ? "${n}_x" as String : n
    }
    resultNames.addAll(yNonKeyNames.collect { String n ->
      duplicates.contains(n) ? "${n}_y" as String : n
    })

    List<Class> resultTypes = [] + xColTypes
    if (joinType == JoinType.RIGHT || joinType == JoinType.FULL) {
      xKeyNames.eachWithIndex { String xKeyName, int keyIndex ->
        int xKeyIndex = x.columnIndex(xKeyName)
        Class xKeyType = xColTypes[xKeyIndex]
        Class yKeyType = yColTypes[y.columnIndex(yKeyNames[keyIndex])]
        resultTypes[xKeyIndex] = commonDeclaredType(xKeyType, yKeyType)
      }
    }
    resultTypes.addAll(yNonKeyTypes)

    new ResultColumns(names: resultNames, types: resultTypes)
  }

  private static Class commonDeclaredType(Class left, Class right) {
    Class leftType = left.isPrimitive() ? primitiveWrapper(left) : left
    Class rightType = right.isPrimitive() ? primitiveWrapper(right) : right
    if (leftType == rightType || leftType.isAssignableFrom(rightType)) {
      return leftType
    }
    if (rightType.isAssignableFrom(leftType)) {
      return rightType
    }
    if (Number.isAssignableFrom(leftType) && Number.isAssignableFrom(rightType)) {
      return Number
    }
    Object
  }

  private static class ResultColumns {

    List<String> names
    List<Class> types

  }

  private static class JoinIndex {

    Map<List<Object>, List<List<Object>>> rows
    Map<List<Object>, List<Object>> rawKeys

  }

}
