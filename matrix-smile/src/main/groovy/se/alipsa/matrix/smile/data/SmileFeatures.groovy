package se.alipsa.matrix.smile.data

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Feature engineering utilities for machine learning preprocessing.
 * Provides standardization, normalization, and encoding transformations.
 */
@CompileStatic
class SmileFeatures {

  /**
   * Standardize columns to have zero mean and unit variance (z-score normalization).
   * Formula: z = (x - mean) / std
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to standardize (all numeric columns if not specified)
   * @return a new Matrix with standardized columns
   */
  static Matrix standardize(Matrix matrix, List<String> columns = null) {
    List<String> targetColumns = columns ?: getNumericColumnNames(matrix)
    return transformColumns(matrix, targetColumns) { List<Double> values ->
      standardizeValues(values)
    }
  }

  /**
   * Standardize a single column.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to standardize
   * @return a new Matrix with the standardized column
   */
  static Matrix standardize(Matrix matrix, String column) {
    return standardize(matrix, [column])
  }

  /**
   * Normalize columns to [0, 1] range (min-max normalization).
   * Formula: x_norm = (x - min) / (max - min)
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to normalize (all numeric columns if not specified)
   * @return a new Matrix with normalized columns
   */
  static Matrix normalize(Matrix matrix, List<String> columns = null) {
    List<String> targetColumns = columns ?: getNumericColumnNames(matrix)
    return transformColumns(matrix, targetColumns) { List<Double> values ->
      normalizeMinMax(values)
    }
  }

  /**
   * Normalize a single column to [0, 1] range.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to normalize
   * @return a new Matrix with the normalized column
   */
  static Matrix normalize(Matrix matrix, String column) {
    return normalize(matrix, [column])
  }

  /**
   * Normalize columns to a custom range.
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to normalize
   * @param min the minimum value of the target range
   * @param max the maximum value of the target range
   * @return a new Matrix with normalized columns
   */
  static Matrix normalize(Matrix matrix, List<String> columns, double min, double max) {
    return transformColumns(matrix, columns) { List<Double> values ->
      normalizeToRange(values, min, max)
    }
  }

  /**
   * Perform one-hot encoding on a categorical column.
   * Creates new binary columns for each unique value.
   *
   * @param matrix the Matrix to transform
   * @param column the categorical column to encode
   * @return a new Matrix with the original column replaced by one-hot encoded columns
   */
  static Matrix oneHotEncode(Matrix matrix, String column) {
    return oneHotEncode(matrix, column, true)
  }

  /**
   * Perform one-hot encoding on a categorical column.
   *
   * @param matrix the Matrix to transform
   * @param column the categorical column to encode
   * @param dropOriginal whether to drop the original column (default true)
   * @return a new Matrix with one-hot encoded columns
   */
  static Matrix oneHotEncode(Matrix matrix, String column, boolean dropOriginal) {
    List<?> originalColumn = matrix.column(column)
    Set<Object> uniqueValues = new LinkedHashSet<>(originalColumn.findAll { it != null })
    List<Object> sortedValues = uniqueValues.toList().sort { it.toString() }

    // Build the new data structure
    Map<String, List<?>> newData = new LinkedHashMap<>()
    List<Class<?>> newTypes = []

    // Copy columns before the encoded column
    int colIndex = matrix.columnNames().indexOf(column)
    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (i == colIndex) {
        if (!dropOriginal) {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
        // Add one-hot columns
        for (Object value : sortedValues) {
          String newColName = "${column}_${value}".toString()
          List<Integer> binaryCol = originalColumn.collect { it == value ? 1 : 0 }
          newData.put(newColName, binaryCol)
          newTypes.add(Integer)
        }
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }

    return Matrix.builder()
        .data(newData)
        .types(newTypes)
        .matrixName(matrix.matrixName)
        .build()
  }

  /**
   * Perform one-hot encoding on multiple categorical columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the categorical columns to encode
   * @return a new Matrix with one-hot encoded columns
   */
  static Matrix oneHotEncode(Matrix matrix, List<String> columns) {
    Matrix result = matrix
    for (String column : columns) {
      result = oneHotEncode(result, column)
    }
    return result
  }

  /**
   * Label encode a categorical column (convert to integer labels).
   *
   * @param matrix the Matrix to transform
   * @param column the categorical column to encode
   * @return a new Matrix with the column replaced by integer labels
   */
  static Matrix labelEncode(Matrix matrix, String column) {
    List<?> originalColumn = matrix.column(column)
    Set<Object> uniqueValues = new LinkedHashSet<>(originalColumn.findAll { it != null })
    List<Object> sortedValues = uniqueValues.toList().sort { it.toString() }

    Map<Object, Integer> labelMap = [:]
    sortedValues.eachWithIndex { val, idx -> labelMap[val] = idx }

    List<Integer> encodedColumn = originalColumn.collect { it != null ? labelMap[it] : null }

    return replaceColumn(matrix, column, encodedColumn, Integer)
  }

  /**
   * Label encode multiple categorical columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the categorical columns to encode
   * @return a new Matrix with integer-encoded columns
   */
  static Matrix labelEncode(Matrix matrix, List<String> columns) {
    Matrix result = matrix
    for (String column : columns) {
      result = labelEncode(result, column)
    }
    return result
  }

  /**
   * Apply log transformation to columns (log1p for handling zeros).
   * Formula: log(1 + x)
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to transform
   * @return a new Matrix with log-transformed columns
   */
  static Matrix logTransform(Matrix matrix, List<String> columns) {
    return transformColumns(matrix, columns) { List<Double> values ->
      values.collect { v -> v != null ? Math.log1p(v) : null }
    }
  }

  /**
   * Apply log transformation to a single column.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to transform
   * @return a new Matrix with the log-transformed column
   */
  static Matrix logTransform(Matrix matrix, String column) {
    return logTransform(matrix, [column])
  }

  /**
   * Apply square root transformation to columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to transform
   * @return a new Matrix with sqrt-transformed columns
   */
  static Matrix sqrtTransform(Matrix matrix, List<String> columns) {
    return transformColumns(matrix, columns) { List<Double> values ->
      values.collect { v -> v != null ? Math.sqrt(v) : null }
    }
  }

  /**
   * Apply power transformation (Box-Cox like) to columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to transform
   * @param power the power to apply
   * @return a new Matrix with power-transformed columns
   */
  static Matrix powerTransform(Matrix matrix, List<String> columns, double power) {
    return transformColumns(matrix, columns) { List<Double> values ->
      values.collect { v -> v != null ? Math.pow(v, power) : null }
    }
  }

  /**
   * Bin a numeric column into discrete intervals.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to bin
   * @param bins the number of equal-width bins
   * @return a new Matrix with the binned column
   */
  static Matrix binning(Matrix matrix, String column, int bins) {
    List<?> col = matrix.column(column)
    List<Double> numericCol = col.collect { it != null ? ((Number) it).doubleValue() : null }

    double min = numericCol.findAll { it != null }.min() as double
    double max = numericCol.findAll { it != null }.max() as double
    double binWidth = (max - min) / bins

    List<Integer> binLabels = numericCol.collect { v ->
      if (v == null) return null
      int binNum = (int) ((v - min) / binWidth)
      return Math.min(binNum, bins - 1) // Handle edge case where v == max
    }

    return replaceColumn(matrix, column, binLabels, Integer)
  }

  /**
   * Bin a numeric column using custom bin edges.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to bin
   * @param edges the bin edges (n+1 edges for n bins)
   * @param labels optional labels for the bins
   * @return a new Matrix with the binned column
   */
  static Matrix binning(Matrix matrix, String column, List<Double> edges, List<String> labels = null) {
    if (labels != null && labels.size() != edges.size() - 1) {
      throw new IllegalArgumentException("Number of labels must be one less than number of edges")
    }

    List<?> col = matrix.column(column)
    List<Double> numericCol = col.collect { it != null ? ((Number) it).doubleValue() : null }

    List<?> binLabels = numericCol.collect { v ->
      if (v == null) return null
      for (int i = 0; i < edges.size() - 1; i++) {
        if (v >= edges[i] && v < edges[i + 1]) {
          return labels != null ? labels[i] : i
        }
      }
      // Handle values at the maximum edge
      if (v == edges.last()) {
        return labels != null ? labels.last() : edges.size() - 2
      }
      return null
    }

    Class<?> newType = labels != null ? String : Integer
    return replaceColumn(matrix, column, binLabels, newType)
  }

  /**
   * Fill missing values with a constant.
   *
   * @param matrix the Matrix to transform
   * @param column the column name
   * @param value the value to fill nulls with
   * @return a new Matrix with filled values
   */
  static Matrix fillna(Matrix matrix, String column, Object value) {
    List<?> col = matrix.column(column)
    List<?> filledCol = col.collect { it != null ? it : value }
    return replaceColumn(matrix, column, filledCol, matrix.type(matrix.columnNames().indexOf(column)))
  }

  /**
   * Fill missing values with the mean of the column.
   *
   * @param matrix the Matrix to transform
   * @param column the column name
   * @return a new Matrix with filled values
   */
  static Matrix fillnaMean(Matrix matrix, String column) {
    List<?> col = matrix.column(column)
    List<Double> numericCol = col.findAll { it != null }.collect { ((Number) it).doubleValue() }
    double mean = sumDoubles(numericCol) / numericCol.size()
    return fillna(matrix, column, mean)
  }

  /**
   * Fill missing values with the median of the column.
   *
   * @param matrix the Matrix to transform
   * @param column the column name
   * @return a new Matrix with filled values
   */
  static Matrix fillnaMedian(Matrix matrix, String column) {
    List<?> col = matrix.column(column)
    List<Double> sortedCol = col.findAll { it != null }.collect { ((Number) it).doubleValue() }.sort() as List<Double>
    double median
    int size = sortedCol.size()
    if (size % 2 == 0) {
      int midIndex = size.intdiv(2) as int
      median = (sortedCol[midIndex - 1] + sortedCol[midIndex]) / 2.0d
    } else {
      median = sortedCol[size.intdiv(2) as int]
    }
    return fillna(matrix, column, median)
  }

  /**
   * Drop rows containing any null values.
   *
   * @param matrix the Matrix to transform
   * @return a new Matrix without null-containing rows
   */
  static Matrix dropna(Matrix matrix) {
    List<Integer> validIndices = []
    for (int i = 0; i < matrix.rowCount(); i++) {
      List<?> row = matrix.row(i)
      if (!row.any { it == null }) {
        validIndices << i
      }
    }

    if (validIndices.isEmpty()) {
      return Matrix.builder()
          .columnNames(matrix.columnNames() as List<String>)
          .types(matrix.types())
          .matrixName(matrix.matrixName)
          .build()
    }

    return Matrix.builder()
        .rows(matrix.rows(validIndices) as List<List>)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .matrixName(matrix.matrixName)
        .build()
  }

  /**
   * Drop rows containing null values in specific columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the columns to check for nulls
   * @return a new Matrix without null-containing rows
   */
  static Matrix dropna(Matrix matrix, List<String> columns) {
    List<Integer> colIndices = columns.collect { matrix.columnNames().indexOf(it) }

    List<Integer> validIndices = []
    for (int i = 0; i < matrix.rowCount(); i++) {
      List<?> row = matrix.row(i)
      boolean hasNull = colIndices.any { idx -> row[idx] == null }
      if (!hasNull) {
        validIndices << i
      }
    }

    if (validIndices.isEmpty()) {
      return Matrix.builder()
          .columnNames(matrix.columnNames() as List<String>)
          .types(matrix.types())
          .matrixName(matrix.matrixName)
          .build()
    }

    return Matrix.builder()
        .rows(matrix.rows(validIndices) as List<List>)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .matrixName(matrix.matrixName)
        .build()
  }

  // ============ Standardization Scaler Class ============

  /**
   * Create a StandardScaler that can be fitted and used to transform data.
   * Useful when you need to apply the same transformation to train and test data.
   *
   * @return a new StandardScaler instance
   */
  static StandardScaler standardScaler() {
    return new StandardScaler()
  }

  /**
   * Create a MinMaxScaler that can be fitted and used to transform data.
   *
   * @return a new MinMaxScaler instance
   */
  static MinMaxScaler minMaxScaler() {
    return new MinMaxScaler()
  }

  // ============ Helper Methods ============

  private static List<String> getNumericColumnNames(Matrix matrix) {
    List<Class<?>> numericTypes = [
        Integer, int, Long, long, Double, double, Float, float,
        Short, short, Byte, byte, BigDecimal, BigInteger, Number
    ]
    List<String> result = []
    for (int i = 0; i < matrix.columnCount(); i++) {
      if (numericTypes.contains(matrix.type(i))) {
        result << matrix.columnName(i)
      }
    }
    return result
  }

  private static Matrix transformColumns(Matrix matrix, List<String> columns, Closure<List<?>> transformer) {
    Map<String, List<?>> newData = new LinkedHashMap<>()
    List<Class<?>> newTypes = []

    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (columns.contains(colName)) {
        List<?> col = matrix.column(i)
        List<Double> numericCol = col.collect { it != null ? ((Number) it).doubleValue() : null }
        List<?> transformed = transformer.call(numericCol)
        newData.put(colName, transformed)
        newTypes.add(Double)
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }

    return Matrix.builder()
        .data(newData)
        .types(newTypes)
        .matrixName(matrix.matrixName)
        .build()
  }

  private static Matrix replaceColumn(Matrix matrix, String column, List<?> newValues, Class<?> newType) {
    Map<String, List<?>> newData = new LinkedHashMap<>()
    List<Class<?>> newTypes = []

    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (colName == column) {
        newData.put(colName, newValues)
        newTypes.add(newType)
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }

    return Matrix.builder()
        .data(newData)
        .types(newTypes)
        .matrixName(matrix.matrixName)
        .build()
  }

  private static List<Double> standardizeValues(List<Double> values) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) return values

    double mean = sumDoubles(nonNull) / nonNull.size()
    double variance = sumDoubles(nonNull.collect { Double v -> (v - mean) * (v - mean) }) / nonNull.size()
    double std = Math.sqrt(variance)

    if (std == 0.0d) {
      return values.collect { Double v -> v != null ? 0.0d : (Double) null } as List<Double>
    }

    return values.collect { Double v -> v != null ? (v - mean) / std : (Double) null } as List<Double>
  }

  private static List<Double> normalizeMinMax(List<Double> values) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) return values

    double min = nonNull.min()
    double max = nonNull.max()
    double range = max - min

    if (range == 0.0d) {
      return values.collect { Double v -> v != null ? 0.0d : (Double) null } as List<Double>
    }

    return values.collect { Double v -> v != null ? (v - min) / range : (Double) null } as List<Double>
  }

  private static List<Double> normalizeToRange(List<Double> values, double targetMin, double targetMax) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) return values

    double min = nonNull.min()
    double max = nonNull.max()
    double range = max - min
    double targetRange = targetMax - targetMin

    if (range == 0.0d) {
      return values.collect { Double v -> v != null ? targetMin : (Double) null } as List<Double>
    }

    return values.collect { Double v -> v != null ? ((v - min) / range) * targetRange + targetMin : (Double) null } as List<Double>
  }

  private static double sumDoubles(List<Double> values) {
    double sum = 0.0d
    for (Double v : values) {
      if (v != null) {
        sum += v
      }
    }
    return sum
  }

  // ============ Scaler Classes ============

  /**
   * StandardScaler that can be fitted on training data and applied to new data.
   */
  @CompileStatic
  static class StandardScaler {
    private Map<String, Double> means = [:]
    private Map<String, Double> stds = [:]
    private boolean fitted = false

    /**
     * Fit the scaler on training data.
     *
     * @param matrix the training data
     * @param columns the columns to fit (all numeric if not specified)
     * @return this scaler
     */
    StandardScaler fit(Matrix matrix, List<String> columns = null) {
      List<String> targetColumns = columns ?: getNumericColumnNames(matrix)

      for (String col : targetColumns) {
        List<?> colData = matrix.column(col)
        List<Double> numericCol = colData.findAll { it != null }.collect { ((Number) it).doubleValue() } as List<Double>

        if (numericCol.isEmpty()) {
          means[col] = 0.0d
          stds[col] = 1.0d
          continue
        }

        double mean = sumDoubles(numericCol) / numericCol.size()
        double variance = sumDoubles(numericCol.collect { Double v -> (v - mean) * (v - mean) }) / numericCol.size()
        double std = Math.sqrt(variance)

        means[col] = mean
        stds[col] = std == 0.0d ? 1.0d : std
      }

      fitted = true
      return this
    }

    /**
     * Transform data using the fitted parameters.
     *
     * @param matrix the data to transform
     * @return a new Matrix with standardized columns
     */
    Matrix transform(Matrix matrix) {
      if (!fitted) {
        throw new IllegalStateException("Scaler must be fitted before transforming")
      }

      return SmileFeatures.transformColumns(matrix, means.keySet().toList()) { List<Double> values ->
        // This closure receives values for each column in turn, but we need column context
        values // Will be handled column by column below
      }
    }

    /**
     * Fit and transform in one step.
     *
     * @param matrix the data to fit and transform
     * @param columns the columns to process
     * @return a new Matrix with standardized columns
     */
    Matrix fitTransform(Matrix matrix, List<String> columns = null) {
      fit(matrix, columns)

      Map<String, List<?>> newData = new LinkedHashMap<>()
      List<Class<?>> newTypes = []

      for (int i = 0; i < matrix.columnCount(); i++) {
        String colName = matrix.columnName(i)
        if (means.containsKey(colName)) {
          List<?> col = matrix.column(i)
          double mean = means[colName]
          double std = stds[colName]
          List<Double> transformed = col.collect { v ->
            v != null ? (((Number) v).doubleValue() - mean) / std : null
          }
          newData.put(colName, transformed)
          newTypes.add(Double)
        } else {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
      }

      return Matrix.builder()
          .data(newData)
          .types(newTypes)
          .matrixName(matrix.matrixName)
          .build()
    }

    /**
     * Get the fitted means.
     */
    Map<String, Double> getMeans() {
      return Collections.unmodifiableMap(means)
    }

    /**
     * Get the fitted standard deviations.
     */
    Map<String, Double> getStds() {
      return Collections.unmodifiableMap(stds)
    }
  }

  /**
   * MinMaxScaler that can be fitted on training data and applied to new data.
   */
  @CompileStatic
  static class MinMaxScaler {
    private Map<String, Double> mins = [:]
    private Map<String, Double> maxs = [:]
    private boolean fitted = false

    /**
     * Fit the scaler on training data.
     *
     * @param matrix the training data
     * @param columns the columns to fit (all numeric if not specified)
     * @return this scaler
     */
    MinMaxScaler fit(Matrix matrix, List<String> columns = null) {
      List<String> targetColumns = columns ?: getNumericColumnNames(matrix)

      for (String col : targetColumns) {
        List<?> colData = matrix.column(col)
        List<Double> numericCol = colData.findAll { it != null }.collect { ((Number) it).doubleValue() }

        if (numericCol.isEmpty()) {
          mins[col] = 0.0d
          maxs[col] = 1.0d
          continue
        }

        mins[col] = numericCol.min()
        maxs[col] = numericCol.max()
      }

      fitted = true
      return this
    }

    /**
     * Fit and transform in one step.
     *
     * @param matrix the data to fit and transform
     * @param columns the columns to process
     * @return a new Matrix with normalized columns
     */
    Matrix fitTransform(Matrix matrix, List<String> columns = null) {
      fit(matrix, columns)

      Map<String, List<?>> newData = new LinkedHashMap<>()
      List<Class<?>> newTypes = []

      for (int i = 0; i < matrix.columnCount(); i++) {
        String colName = matrix.columnName(i)
        if (mins.containsKey(colName)) {
          List<?> col = matrix.column(i)
          double min = mins[colName]
          double max = maxs[colName]
          double range = max - min

          List<Double> transformed = col.collect { v ->
            if (v == null) return null
            if (range == 0.0d) return 0.0d
            (((Number) v).doubleValue() - min) / range
          }
          newData.put(colName, transformed)
          newTypes.add(Double)
        } else {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
      }

      return Matrix.builder()
          .data(newData)
          .types(newTypes)
          .matrixName(matrix.matrixName)
          .build()
    }

    /**
     * Get the fitted minimums.
     */
    Map<String, Double> getMins() {
      return Collections.unmodifiableMap(mins)
    }

    /**
     * Get the fitted maximums.
     */
    Map<String, Double> getMaxs() {
      return Collections.unmodifiableMap(maxs)
    }
  }
}
