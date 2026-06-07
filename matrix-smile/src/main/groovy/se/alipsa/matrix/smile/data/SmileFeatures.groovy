package se.alipsa.matrix.smile.data

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.SmileUtil

/**
 * Feature engineering utilities for machine learning preprocessing.
 * Provides standardization, normalization, and encoding transformations.
 */
@CompileStatic
@SuppressWarnings('ClassSize')
class SmileFeatures {

  private static final int HALF_DIVISOR = 2
  private static final double ZERO = 0.0d

  /**
   * Standardize columns to have zero mean and unit variance (z-score normalization).
   * Formula: z = (x - mean) / std
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to standardize (all numeric columns if not specified)
   * @return a new Matrix with standardized columns
   */
  static Matrix standardize(Matrix matrix, List<String> columns = null) {
    List<String> targetColumns = columns ?: SmileUtil.getNumericColumnNames(matrix)
    transformColumns(matrix, targetColumns) { List<Double> values ->
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
    standardize(matrix, [column])
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
    List<String> targetColumns = columns ?: SmileUtil.getNumericColumnNames(matrix)
    transformColumns(matrix, targetColumns) { List<Double> values ->
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
    normalize(matrix, [column])
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
    if (min >= max) {
      throw new IllegalArgumentException("min must be less than max: min=${min}, max=${max}")
    }
    transformColumns(matrix, columns) { List<Double> values ->
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
    oneHotEncode(matrix, column, true)
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
    oneHotEncoder().fitTransform(matrix, column, dropOriginal)
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
    result
  }

  /**
   * Label encode a categorical column (convert to integer labels).
   *
   * @param matrix the Matrix to transform
   * @param column the categorical column to encode
   * @return a new Matrix with the column replaced by integer labels
   */
  static Matrix labelEncode(Matrix matrix, String column) {
    labelEncoder().fitTransform(matrix, column)
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
    result
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
    Map<String, List<?>> newData = [:]
    List<Class<?>> newTypes = []
    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (columns.contains(colName)) {
        List<?> col = matrix.column(i)
        List<Double> numericCol = col.collect { it != null ? it as double : null }
        List<Double> transformed = []
        for (int j = 0; j < numericCol.size(); j++) {
          Double v = numericCol.get(j)
          if (v == null) {
            transformed.add(null)
          } else if (v <= -1.0d) {
            throw new IllegalArgumentException(
                "logTransform: value ${v} at row ${j} in column '${colName}' is <= -1 (log1p undefined)")
          } else {
            transformed.add(v.log1p() as double)
          }
        }
        newData.put(colName, transformed)
        newTypes.add(Double)
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }
    Matrix.builder().data(newData).types(newTypes).matrixName(matrix.matrixName).build()
  }

  /**
   * Apply log transformation to a single column.
   *
   * @param matrix the Matrix to transform
   * @param column the column name to transform
   * @return a new Matrix with the log-transformed column
   */
  static Matrix logTransform(Matrix matrix, String column) {
    logTransform(matrix, [column])
  }

  /**
   * Apply square root transformation to columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to transform
   * @return a new Matrix with sqrt-transformed columns
   */
  static Matrix sqrtTransform(Matrix matrix, List<String> columns) {
    Map<String, List<?>> newData = [:]
    List<Class<?>> newTypes = []
    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (columns.contains(colName)) {
        List<?> col = matrix.column(i)
        List<Double> numericCol = col.collect { it != null ? it as double : null }
        List<Double> transformed = []
        for (int j = 0; j < numericCol.size(); j++) {
          Double v = numericCol.get(j)
          if (v == null) {
            transformed.add(null)
          } else if (v < ZERO) {
            throw new IllegalArgumentException(
                "sqrtTransform: negative value ${v} at row ${j} in column '${colName}'")
          } else {
            transformed.add(v.sqrt() as double)
          }
        }
        newData.put(colName, transformed)
        newTypes.add(Double)
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }
    Matrix.builder().data(newData).types(newTypes).matrixName(matrix.matrixName).build()
  }

  /**
   * Apply power transformation (Box-Cox like) to columns.
   *
   * @param matrix the Matrix to transform
   * @param columns the column names to transform
   * @param power the power to apply
   * @return a new Matrix with power-transformed columns
   */
  static Matrix powerTransform(Matrix matrix, List<String> columns, Number power) {
    if (power == null) {
      throw new IllegalArgumentException('power cannot be null')
    }
    double p = power as double
    transformColumns(matrix, columns) { List<Double> values ->
      values.collect { v -> v != null ? v ** p : null }
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
    List<Double> numericCol = col.collect { it != null ? it as double : null }

    double min = numericCol.findAll { it != null }.min() as double
    double max = numericCol.findAll { it != null }.max() as double
    double binWidth = (max - min) / bins

    List<Integer> binLabels = numericCol.collect { v ->
      if (v == null) { return null }
      int binNum = (int) ((v - min) / binWidth)
      return Math.min(binNum, bins - 1) // Handle edge case where v == max
    }

    replaceColumn(matrix, column, binLabels, Integer)
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
      throw new IllegalArgumentException('Number of labels must be one less than number of edges')
    }

    List<?> col = matrix.column(column)
    List<Double> numericCol = col.collect { it != null ? it as double : null }

    List<?> binLabels = numericCol.collect { v ->
      if (v == null) { return null }
      for (int i = 0; i < edges.size() - 1; i++) {
        if (v >= edges[i] && v < edges[i + 1]) {
          return labels != null ? labels[i] : i
        }
      }
      // Handle values at the maximum edge
      if (v == edges.last()) {
        return labels != null ? labels.last() : edges.size() - HALF_DIVISOR
      }
      throw new IllegalArgumentException(
          "Value ${v} in column '${column}' is outside the edge range [${edges[0]}, ${edges.last()}]")
    }

    Class<?> newType = labels != null ? String : Integer
    replaceColumn(matrix, column, binLabels, newType)
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
    replaceColumn(matrix, column, filledCol, matrix.type(matrix.columnNames().indexOf(column)))
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
    List<Double> numericCol = col.findAll { it != null }.collect { it as double }
    if (numericCol.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot compute mean for column '${column}': no non-null values found")
    }
    double mean = sumDoubles(numericCol) / numericCol.size()
    fillna(matrix, column, mean)
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
    List<Double> sortedCol = col.findAll { it != null }.collect { it as double }.sort() as List<Double>
    double median
    int size = sortedCol.size()
    if (size % HALF_DIVISOR == 0) {
      int midIndex = size.intdiv(HALF_DIVISOR) as int
      median = (sortedCol[midIndex - 1] + sortedCol[midIndex]) / 2.0d
    } else {
      median = sortedCol[size.intdiv(HALF_DIVISOR) as int]
    }
    fillna(matrix, column, median)
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

    Matrix.builder()
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
    List<String> knownColumns = matrix.columnNames()
    for (String col : columns) {
      if (!knownColumns.contains(col)) {
        throw new IllegalArgumentException(
            "Column '${col}' not found in matrix. Available: ${knownColumns}")
      }
    }
    List<Integer> colIndices = columns.collect { knownColumns.indexOf(it) }

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

    Matrix.builder()
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
    new StandardScaler()
  }

  /**
   * Create a MinMaxScaler that can be fitted and used to transform data.
   *
   * @return a new MinMaxScaler instance
   */
  static MinMaxScaler minMaxScaler() {
    new MinMaxScaler()
  }

  /**
   * Create a LabelEncoder that can be fitted on training data and applied to new data.
   *
   * @return a new LabelEncoder instance
   */
  static LabelEncoder labelEncoder() {
    new LabelEncoder()
  }

  /**
   * Create a OneHotEncoder that can be fitted on training data and applied to new data.
   *
   * @return a new OneHotEncoder instance
   */
  static OneHotEncoder oneHotEncoder() {
    new OneHotEncoder()
  }

  // ============ Helper Methods ============

  private static Matrix transformColumns(Matrix matrix, List<String> columns, Closure<List<?>> transformer) {
    Map<String, List<?>> newData = [:]
    List<Class<?>> newTypes = []

    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (columns.contains(colName)) {
        List<?> col = matrix.column(i)
        List<Double> numericCol = col.collect { it != null ? it as double : null }
        List<?> transformed = transformer.call(numericCol)
        newData.put(colName, transformed)
        newTypes.add(Double)
      } else {
        newData.put(colName, matrix.column(i))
        newTypes.add(matrix.type(i))
      }
    }

    Matrix.builder()
        .data(newData)
        .types(newTypes)
        .matrixName(matrix.matrixName)
        .build()
  }

  private static List<Object> extractSortedUniqueValues(List<?> col, String column, String entityName) {
    Set<Object> unique = col.findAll { it != null } as Set
    if (unique.isEmpty()) {
      throw new IllegalArgumentException("No non-null ${entityName} found for column '${column}'")
    }
    unique.toList().sort { it.toString() }
  }

  private static Matrix replaceColumn(Matrix matrix, String column, List<?> newValues, Class<?> newType) {
    Map<String, List<?>> newData = [:]
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

    Matrix.builder()
        .data(newData)
        .types(newTypes)
        .matrixName(matrix.matrixName)
        .build()
  }

  private static List<Double> standardizeValues(List<Double> values) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) { return values }

    double mean = sumDoubles(nonNull) / nonNull.size()
    double variance = sumDoubles(nonNull.collect { Double v -> (v - mean) * (v - mean) }) / nonNull.size()
    double std = Math.sqrt(variance)

    if (std == ZERO) {
      return values.collect { Double v -> v != null ? ZERO : (Double) null } as List<Double>
    }

    values.collect { Double v -> v != null ? (v - mean) / std : (Double) null } as List<Double>
  }

  private static List<Double> normalizeMinMax(List<Double> values) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) { return values }

    double min = nonNull.min()
    double max = nonNull.max()
    double range = max - min

    if (range == ZERO) {
      return values.collect { Double v -> v != null ? 0.0d : (Double) null } as List<Double>
    }

    values.collect { Double v -> v != null ? (v - min) / range : (Double) null } as List<Double>
  }

  private static List<Double> normalizeToRange(List<Double> values, double targetMin, double targetMax) {
    List<Double> nonNull = values.findAll { it != null } as List<Double>
    if (nonNull.isEmpty()) { return values }

    double min = nonNull.min()
    double max = nonNull.max()
    double range = max - min
    double targetRange = targetMax - targetMin

    if (range == ZERO) {
      return values.collect { Double v -> v != null ? targetMin : (Double) null } as List<Double>
    }

    values.collect { Double v -> v != null ? ((v - min) / range) * targetRange + targetMin : (Double) null } as List<Double>
  }

  private static double sumDoubles(List<Double> values) {
    double sum = ZERO
    for (Double v : values) {
      if (v != null) {
        sum += v
      }
    }
    sum
  }

  // ============ Scaler Classes ============

  /**
   * StandardScaler that can be fitted on training data and applied to new data.
   */
  @CompileStatic
  static class StandardScaler {

    private final Map<String, Double> means = [:]
    private final Map<String, Double> stds = [:]
    private boolean fitted = false

    /**
     * Fit the scaler on training data.
     *
     * @param matrix the training data
     * @param columns the columns to fit (all numeric if not specified)
     * @return this scaler
     */
    StandardScaler fit(Matrix matrix, List<String> columns = null) {
      List<String> targetColumns = columns ?: SmileUtil.getNumericColumnNames(matrix)

      for (String col : targetColumns) {
        List<?> colData = matrix.column(col)
        List<Double> numericCol = colData.findAll { it != null }.collect { it as double } as List<Double>

        if (numericCol.isEmpty()) {
          means[col] = 0.0d
          stds[col] = 1.0d
          continue
        }

        double mean = sumDoubles(numericCol) / numericCol.size()
        double variance = sumDoubles(numericCol.collect { Double v -> (v - mean) * (v - mean) }) / numericCol.size()
        double std = Math.sqrt(variance)

        means[col] = mean
        stds[col] = std == ZERO ? 1.0d : std
      }

      fitted = true
      this
    }

    /**
     * Transform data using the fitted parameters.
     * The scaler must be fitted (using fit()) before calling this method.
     *
     * @param matrix the data to transform
     * @return a new Matrix with standardized columns
     * @throws IllegalStateException if the scaler has not been fitted
     */
    Matrix transform(Matrix matrix) {
      if (!fitted) {
        throw new IllegalStateException('Scaler must be fitted before transforming')
      }

      Map<String, List<?>> newData = [:]
      List<Class<?>> newTypes = []

      for (int i = 0; i < matrix.columnCount(); i++) {
        String colName = matrix.columnName(i)
        if (means.containsKey(colName)) {
          List<?> col = matrix.column(i)
          double mean = means[colName]
          double std = stds[colName]
          List<Double> transformed = col.collect { v ->
            v != null ? ((v as double) - mean) / std : null
          }
          newData.put(colName, transformed)
          newTypes.add(Double)
        } else {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
      }

      Matrix.builder()
          .data(newData)
          .types(newTypes)
          .matrixName(matrix.matrixName)
          .build()
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
      transform(matrix)
    }

    /**
     * Get the fitted means.
     */
    Map<String, Double> getMeans() {
      Collections.unmodifiableMap(means)
    }

    /**
     * Get the fitted standard deviations.
     */
    Map<String, Double> getStds() {
      Collections.unmodifiableMap(stds)
    }

  }

  /**
   * MinMaxScaler that can be fitted on training data and applied to new data.
   */
  @CompileStatic
  static class MinMaxScaler {

    private final Map<String, Double> mins = [:]
    private final Map<String, Double> maxs = [:]
    private boolean fitted = false

    /**
     * Fit the scaler on training data.
     *
     * @param matrix the training data
     * @param columns the columns to fit (all numeric if not specified)
     * @return this scaler
     */
    MinMaxScaler fit(Matrix matrix, List<String> columns = null) {
      List<String> targetColumns = columns ?: SmileUtil.getNumericColumnNames(matrix)

      for (String col : targetColumns) {
        List<?> colData = matrix.column(col)
        List<Double> numericCol = colData.findAll { it != null }.collect { it as double }

        if (numericCol.isEmpty()) {
          mins[col] = 0.0d
          maxs[col] = 1.0d
          continue
        }

        mins[col] = numericCol.min()
        maxs[col] = numericCol.max()
      }

      fitted = true
      this
    }

    /**
     * Transform data using the fitted parameters.
     * The scaler must be fitted (using fit()) before calling this method.
     *
     * @param matrix the data to transform
     * @return a new Matrix with normalized columns
     * @throws IllegalStateException if the scaler has not been fitted
     */
    Matrix transform(Matrix matrix) {
      if (!fitted) {
        throw new IllegalStateException('Scaler must be fitted before transforming')
      }

      Map<String, List<?>> newData = [:]
      List<Class<?>> newTypes = []

      for (int i = 0; i < matrix.columnCount(); i++) {
        String colName = matrix.columnName(i)
        if (mins.containsKey(colName)) {
          List<?> col = matrix.column(i)
          double min = mins[colName]
          double max = maxs[colName]
          double range = max - min

          List<Double> transformed = col.collect { v ->
            if (v == null) { return null }
            if (range == ZERO) { return ZERO }
            ((v as double) - min) / range
          }
          newData.put(colName, transformed)
          newTypes.add(Double)
        } else {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
      }

      Matrix.builder()
          .data(newData)
          .types(newTypes)
          .matrixName(matrix.matrixName)
          .build()
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
      transform(matrix)
    }

    /**
     * Get the fitted minimums.
     */
    Map<String, Double> getMins() {
      Collections.unmodifiableMap(mins)
    }

    /**
     * Get the fitted maximums.
     */
    Map<String, Double> getMaxs() {
      Collections.unmodifiableMap(maxs)
    }

  }

  // ============ Encoder Classes ============

  /**
   * Stateful label encoder that maps categorical values to integer indices.
   * Fit on training data, then apply the same mapping to test/production data.
   *
   * <p>For one-shot encoding without a reusable mapping, use the stateless
   * {@link #labelEncode(Matrix, String)} method instead.
   *
   * <p><b>Identity model:</b> This encoder uses {@code Object.equals()} for label identity.
   * Note that {@code SmileClassifier} uses {@code toString()} identity — {@code 1} (Integer)
   * and {@code "1"} (String) are the same class in {@code SmileClassifier} but distinct labels
   * in this encoder. Ensure your column types are consistent if using both APIs on the same data.
   */
  @CompileStatic
  static class LabelEncoder {

    private List<Object> labels
    private Map<Object, Integer> labelMap
    private boolean fitted = false

    /**
     * Fit the encoder on training data.
     * Collects unique non-null values and sorts them by {@code toString()} for deterministic ordering.
     *
     * @param matrix the training data
     * @param column the column to encode
     * @return this encoder
     * @throws IllegalArgumentException if no non-null labels are found
     */
    LabelEncoder fit(Matrix matrix, String column) {
      labels = extractSortedUniqueValues(matrix.column(column), column, 'labels')
      labelMap = [:]
      labels.eachWithIndex { Object val, int idx -> labelMap[val] = idx }
      fitted = true
      this
    }

    /**
     * Transform data using the fitted mapping.
     *
     * @param matrix the data to transform
     * @param column the column to encode
     * @return a new Matrix with the column replaced by integer labels
     * @throws IllegalStateException if the encoder has not been fitted
     * @throws IllegalArgumentException if a value is null or was not seen during fitting
     */
    Matrix transform(Matrix matrix, String column) {
      if (!fitted) {
        throw new IllegalStateException('Encoder must be fitted before transforming')
      }
      List<?> col = matrix.column(column)
      List<Integer> encoded = []
      for (int i = 0; i < col.size(); i++) {
        Object val = col.get(i)
        if (val == null) {
          throw new IllegalArgumentException("Null value at row ${i} in column '${column}'")
        }
        Integer idx = labelMap.get(val)
        if (idx == null) {
          throw new IllegalArgumentException(
              "Unseen label '${val}' at row ${i} in column '${column}'. Known labels: ${labels}")
        }
        encoded.add(idx)
      }
      replaceColumn(matrix, column, encoded, Integer)
    }

    /**
     * Fit and transform in one step.
     *
     * @param matrix the data to fit and transform
     * @param column the column to encode
     * @return a new Matrix with the column replaced by integer labels
     */
    Matrix fitTransform(Matrix matrix, String column) {
      fit(matrix, column)
      transform(matrix, column)
    }

    /**
     * Get the ordered list of labels.
     *
     * @return an unmodifiable list of labels in index order
     * @throws IllegalStateException if the encoder has not been fitted
     */
    List<Object> getLabels() {
      if (!fitted) {
        throw new IllegalStateException('Encoder must be fitted before accessing labels')
      }
      Collections.unmodifiableList(labels)
    }

    /**
     * Get the original label for a given integer index.
     *
     * @param index the integer index
     * @return the original label value
     * @throws IllegalStateException if the encoder has not been fitted
     */
    Object inverse(int index) {
      if (!fitted) {
        throw new IllegalStateException('Encoder must be fitted before inverse lookup')
      }
      labels.get(index)
    }

  }

  /**
   * Stateful one-hot encoder that creates binary columns for each category.
   * Fit on training data, then apply the same mapping to test/production data.
   *
   * <p>For one-shot encoding without a reusable mapping, use the stateless
   * {@link #oneHotEncode(Matrix, String)} method instead.
   *
   * <p><b>Identity model:</b> This encoder uses {@code Object.equals()} for category identity.
   * Note that {@code SmileClassifier} uses {@code toString()} identity — {@code 1} (Integer)
   * and {@code "1"} (String) are the same class in {@code SmileClassifier} but distinct
   * categories in this encoder. Ensure your column types are consistent if using both APIs
   * on the same data.
   */
  @CompileStatic
  static class OneHotEncoder {

    private List<Object> categories
    private Set<Object> categorySet
    private boolean fitted = false

    /**
     * Fit the encoder on training data.
     * Collects unique non-null values and sorts them by {@code toString()} for deterministic ordering.
     *
     * @param matrix the training data
     * @param column the column to encode
     * @return this encoder
     * @throws IllegalArgumentException if no non-null categories are found
     */
    OneHotEncoder fit(Matrix matrix, String column) {
      categories = extractSortedUniqueValues(matrix.column(column), column, 'categories')
      categorySet = categories.toSet()
      fitted = true
      this
    }

    /**
     * Transform data using the fitted categories.
     * Produces one binary Integer column per category named {@code "${column}_${category}"}.
     *
     * @param matrix the data to transform
     * @param column the column to encode
     * @param dropOriginal whether to drop the original column (default true)
     * @return a new Matrix with one-hot encoded columns
     * @throws IllegalStateException if the encoder has not been fitted
     * @throws IllegalArgumentException if a value is unseen or null, or if generated column names collide
     */
    Matrix transform(Matrix matrix, String column, boolean dropOriginal = true) {
      if (!fitted) {
        throw new IllegalStateException('Encoder must be fitted before transforming')
      }

      List<String> generatedNames = categories.collect { "${column}_${it}" as String }

      // Check for duplicate generated names
      Set<String> seen = [] as Set
      for (String name : generatedNames) {
        if (!seen.add(name)) {
          throw new IllegalArgumentException(
              "Duplicate generated column name '${name}'. Distinct categories produce the same " +
              'string representation. Pre-convert the column to a consistent type.')
        }
      }

      // Check for collisions with existing retained columns
      Set<String> retained = [] as Set
      for (String col : matrix.columnNames()) {
        if (col == column && dropOriginal) { continue }
        retained.add(col)
      }
      for (String name : generatedNames) {
        if (retained.contains(name)) {
          throw new IllegalArgumentException(
              "Generated column name '${name}' collides with an existing column. " +
              'Rename the existing column before encoding.')
        }
      }

      // Validate all values exist in categories
      List<?> colData = matrix.column(column)
      for (int i = 0; i < colData.size(); i++) {
        Object val = colData.get(i)
        if (val == null) {
          throw new IllegalArgumentException("Null value at row ${i} in column '${column}'")
        }
        if (!categorySet.contains(val)) {
          throw new IllegalArgumentException(
              "Unseen category '${val}' at row ${i} in column '${column}'. " +
              "Known categories: ${categories}")
        }
      }

      // Build output
      Map<String, List<?>> newData = [:]
      List<Class<?>> newTypes = []
      int colIndex = matrix.columnNames().indexOf(column)

      for (int i = 0; i < matrix.columnCount(); i++) {
        String colName = matrix.columnName(i)
        if (i == colIndex) {
          if (!dropOriginal) {
            newData.put(colName, matrix.column(i))
            newTypes.add(matrix.type(i))
          }
          for (int c = 0; c < categories.size(); c++) {
            Object cat = categories.get(c)
            List<Integer> binaryCol = colData.collect { it == cat ? 1 : 0 }
            newData.put(generatedNames.get(c), binaryCol)
            newTypes.add(Integer)
          }
        } else {
          newData.put(colName, matrix.column(i))
          newTypes.add(matrix.type(i))
        }
      }

      Matrix.builder()
          .data(newData)
          .types(newTypes)
          .matrixName(matrix.matrixName)
          .build()
    }

    /**
     * Fit and transform in one step.
     *
     * @param matrix the data to fit and transform
     * @param column the column to encode
     * @param dropOriginal whether to drop the original column (default true)
     * @return a new Matrix with one-hot encoded columns
     */
    Matrix fitTransform(Matrix matrix, String column, boolean dropOriginal = true) {
      fit(matrix, column)
      transform(matrix, column, dropOriginal)
    }

    /**
     * Get the ordered list of categories.
     *
     * @return an unmodifiable list of categories in index order
     * @throws IllegalStateException if the encoder has not been fitted
     */
    List<Object> getCategories() {
      if (!fitted) {
        throw new IllegalStateException('Encoder must be fitted before accessing categories')
      }
      Collections.unmodifiableList(categories)
    }

  }

}
