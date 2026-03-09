package se.alipsa.matrix.core

import groovy.transform.CompileStatic

/**
 * Represents the result of a {@code groupBy} operation on a {@link Matrix}.
 *
 * <p>Instead of encoding compound keys as underscore-separated strings,
 * GroupedMatrix uses structured {@code List<?>} keys for type-safe access.</p>
 *
 * <pre>{@code
 * def grouped = Stat.groupBy(matrix, 'country', 'quarter')
 * grouped.get('USA', 'Q1')        // sub-Matrix for country=USA, quarter=Q1
 * grouped.keys()                  // [['USA','Q1'], ['USA','Q2'], ...]
 * grouped.level('country')        // ['USA', 'UK', ...] as Set
 * grouped.agg(sales: { Stat.sum(it) })
 * }</pre>
 */
@CompileStatic
class GroupedMatrix {

  private final Matrix source
  private final List<String> groupColumns
  private final Map<List<?>, Matrix> groups

  /**
   * Create a GroupedMatrix from pre-computed groups.
   *
   * @param source the original (ungrouped) matrix
   * @param groupColumns the column names used for grouping
   * @param groups mapping from compound key to sub-matrix
   */
  GroupedMatrix(Matrix source, List<String> groupColumns, Map<List<?>, Matrix> groups) {
    this.source = source
    this.groupColumns = Collections.unmodifiableList(new ArrayList<>(groupColumns))
    this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups))
  }

  /**
   * Return the sub-matrix for the given compound key values.
   *
   * <p>The number of keys must match the number of group columns.</p>
   *
   * @param keys the key values, one per group column
   * @return the sub-matrix, or null if no group matches
   */
  Matrix get(Object... keys) {
    if (keys.length != groupColumns.size()) {
      throw new IllegalArgumentException(
          "Expected ${groupColumns.size()} key value(s) for group columns ${groupColumns} but got ${keys.length}"
      )
    }
    groups[keys as List<?>]
  }

  /**
   * Return all compound keys.
   *
   * @return list of compound keys (each a list of values matching the group columns)
   */
  List<List<?>> keys() {
    groups.keySet().toList()
  }

  /**
   * Return the unique values for one group column.
   *
   * @param columnName the group column name
   * @return the set of unique values for that level
   */
  Set<?> level(String columnName) {
    int idx = groupColumns.indexOf(columnName)
    if (idx < 0) {
      throw new IllegalArgumentException("'${columnName}' is not a group column. Group columns are: ${groupColumns}")
    }
    Set<Object> values = new LinkedHashSet<>()
    for (List<?> key : groups.keySet()) {
      values.add(key[idx])
    }
    values
  }

  /**
   * Return the number of groups.
   *
   * @return the group count
   */
  int groupCount() {
    groups.size()
  }

  /**
   * Return the group column names.
   *
   * @return unmodifiable list of group column names
   */
  List<String> groupColumns() {
    groupColumns
  }

  /**
   * Return the original source matrix.
   *
   * @return the source matrix
   */
  Matrix source() {
    source
  }

  /**
   * Iterate over all groups, passing compound key and sub-matrix to the closure.
   *
   * @param action a closure receiving {@code (List<?> key, Matrix group)}
   * @return this GroupedMatrix for chaining
   */
  GroupedMatrix each(Closure action) {
    for (Map.Entry<List<?>, Matrix> entry : groups.entrySet()) {
      action.call(entry.key, entry.value)
    }
    this
  }

  /**
   * Aggregate each group using named column aggregations.
   *
   * <p>The result is a new Matrix with the group columns followed by one column
   * per aggregation entry. Each aggregation closure receives a {@link Column}.</p>
   *
   * <p>The map keys must be existing source column names in the grouped sub-matrices.
   * Each closure receives the {@link Column} for that column name.</p>
   *
   * <pre>{@code
   * grouped.agg(
   *   sales: { Stat.sum(it) },
   *   profit: { Stat.mean(it) }
   * )
   * }</pre>
   *
   * @param aggregations map of source column name to aggregation closure
   * @return a new Matrix with group columns and aggregated values
   */
  Matrix agg(Map<String, Closure> aggregations) {
    List<String> colNames = new ArrayList<>(groupColumns)
    List<String> aggKeys = aggregations.keySet() as List<String>
    colNames.addAll(aggKeys)

    // Build types: group column types from source, per-slot aggregation types inferred from first non-null
    List<Class> types = new ArrayList<>()
    for (String gc : groupColumns) {
      types.add(source.type(gc))
    }
    int nAgg = aggKeys.size()
    Class[] aggTypes = new Class[nAgg]

    List<List> rows = []
    for (Map.Entry<List<?>, Matrix> entry : groups.entrySet()) {
      List<?> key = entry.key
      Matrix group = entry.value
      List row = new ArrayList(key)
      for (int i = 0; i < nAgg; i++) {
        Closure fn = aggregations[aggKeys[i]]
        Object result = fn.call(group.column(aggKeys[i]))
        row.add(result)
        if (aggTypes[i] == null && result != null) {
          aggTypes[i] = result.class
        }
      }
      rows.add(row)
    }
    for (int i = 0; i < nAgg; i++) {
      types.add(aggTypes[i] != null ? aggTypes[i] : Object)
    }

    Matrix.builder()
        .columnNames(colNames)
        .rows(rows)
        .types(types)
        .build()
  }

  /**
   * Aggregate each group by applying a single closure to every non-group column.
   *
   * @param aggregation a closure receiving a {@link Column} and returning a scalar
   * @return a new Matrix with group columns and aggregated values
   */
  Matrix agg(Closure aggregation) {
    List<String> nonGroupCols = source.columnNames().findAll { !(it in groupColumns) }
    Map<String, Closure> aggregations = [:]
    for (String col : nonGroupCols) {
      aggregations[col] = aggregation
    }
    agg(aggregations)
  }

  /**
   * Convert to a backward-compatible map with compound list keys.
   *
   * @return a map from compound key (as List) to sub-matrix
   */
  Map<List<?>, Matrix> toMap() {
    Collections.unmodifiableMap(groups)
  }

  /**
   * Convert to the legacy string-keyed map format for backward compatibility.
   *
   * <p>Compound keys are joined with underscore, matching the pre-3.7.0 format.</p>
   *
   * @return a map from underscore-joined string key to sub-matrix
   */
  Map<String, Matrix> toStringKeyMap() {
    Map<String, Matrix> result = new LinkedHashMap<>()
    for (Map.Entry<List<?>, Matrix> entry : groups.entrySet()) {
      String key = entry.key.collect { String.valueOf(it) }.join('_')
      result[key] = entry.value
    }
    result
  }

  @Override
  String toString() {
    "GroupedMatrix[columns=${groupColumns}, groups=${groups.size()}]"
  }
}
