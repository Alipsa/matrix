package se.alipsa.matrix.smile

import groovy.transform.CompileStatic

import smile.data.DataFrame

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * Utility class for working with Smile DataFrames and Matrix objects.
 * Provides convenience methods for conversion, statistical summaries, and sampling.
 */
@CompileStatic
class SmileUtil {

  private static final String STATISTIC = 'statistic'
  // Internal: shared by SmileRegression and SmileClassifier for consistent error messages
  static final String DROPNA_HINT = 'Use SmileFeatures.dropna() or fillna() before calling ML algorithms.'
  static final String DROPNA_HINT_TRAINING = 'Use SmileFeatures.dropna() or fillna() before training.'
  private static final int ROUND_DECIMALS = 4
  private static final double PERCENT = 100.0

  /**
   * Convert a Smile DataFrame to a Matrix.
   *
   * @param dataFrame the Smile DataFrame to convert
   * @return a Matrix containing the same data
   */
  static Matrix toMatrix(DataFrame dataFrame) {
    DataframeConverter.convert(dataFrame)
  }

  /**
   * Convert a Matrix to a Smile DataFrame.
   *
   * @param matrix the Matrix to convert
   * @return a Smile DataFrame containing the same data
   */
  static DataFrame toDataFrame(Matrix matrix) {
    DataframeConverter.convert(matrix)
  }

  /**
   * Generate a statistical summary of a Matrix, similar to pandas describe().
   * Returns statistics for numeric columns: count, mean, std, min, 25%, 50%, 75%, max.
   *
   * @param matrix the Matrix to describe
   * @return a Matrix with statistical summary
   */
  static Matrix describe(Matrix matrix) {
    List<String> numericColumns = getNumericColumnNames(matrix)

    if (numericColumns.isEmpty()) {
      return Matrix.builder()
          .columnNames([STATISTIC])
          .types([String])
          .build()
    }

    List<String> stats = ['count', 'mean', 'std', 'min', '25%', '50%', '75%', 'max']
    Map<String, List<Object>> data = [:]
    data.put(STATISTIC, stats as List<Object>)

    for (String colName : numericColumns) {
      List<?> column = matrix[colName] as List<?>
      List<Double> numericValues = column.findAll { it != null }
          .collect { it as double }

      if (numericValues.isEmpty()) {
        data.put(colName, [0, null, null, null, null, null, null, null])
        continue
      }

      numericValues.sort()
      int count = numericValues.size()
      BigDecimal meanVal = Stat.mean(numericValues as List<BigDecimal>)
      BigDecimal stdVal = Stat.sd(numericValues as List<Number>) as BigDecimal
      double mean = meanVal != null ? meanVal as double : 0.0d
      double std = stdVal != null ? stdVal as double : 0.0d
      double min = numericValues.first()
      double max = numericValues.last()
      double q25 = percentile(numericValues, 25)
      double q50 = percentile(numericValues, 50)
      double q75 = percentile(numericValues, 75)

      data.put(colName, [
          count,
          round(mean),
          round(std),
          round(min),
          round(q25),
          round(q50),
          round(q75),
          round(max)
      ])
    }

    List<Class<?>> types = [String]
    types.addAll(Collections.nCopies(numericColumns.size(), Object))

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Calculate percentile value from a sorted list.
   */
  private static double percentile(List<Double> sortedValues, double percentile) {
    if (sortedValues.isEmpty()) { return Double.NaN }
    if (sortedValues.size() == 1) { return sortedValues[0] }

    double index = (percentile / PERCENT) * (sortedValues.size() - 1)
    int lower = (int) Math.floor(index)
    int upper = (int) Math.ceil(index)

    if (lower == upper) {
      return sortedValues[lower]
    }

    double fraction = index - lower
    sortedValues[lower] * (1 - fraction) + sortedValues[upper] * fraction
  }

  /**
   * Get the names of numeric columns in a Matrix.
   *
   * @param matrix the Matrix to analyze
   * @return list of column names that contain numeric data
   */
  static List<String> getNumericColumnNames(Matrix matrix) {
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
    result
  }

  /**
   * Convert a Matrix to a 2D double array.
   * Null values are not permitted; throws {@code IllegalArgumentException} directing
   * the caller to {@code SmileFeatures.dropna()} or {@code fillna()}.
   *
   * @param matrix the Matrix to convert
   * @return 2D array of double values
   * @throws IllegalArgumentException if any column contains a null value
   */
  @SuppressWarnings('NestedForLoop')
  static double[][] matrixToArray(Matrix matrix) {
    int rows = matrix.rowCount()
    int cols = matrix.columnCount()
    double[][] result = new double[rows][cols]

    for (int j = 0; j < cols; j++) {
      List<?> column = matrix.column(j)
      for (int i = 0; i < rows; i++) {
        Object val = column.get(i)
        if (val == null) {
          throw new IllegalArgumentException(
              "Column '${matrix.columnName(j)}' contains a null at row ${i}. " +
              DROPNA_HINT)
        }
        result[i][j] = val as double
      }
    }

    result
  }

  /**
   * Round a double value to the default number of decimal places (4).
   * Returns NaN and Infinite values unchanged.
   *
   * @param value the value to round
   * @return the rounded value, or the original if NaN/Infinite
   */
  static double round(double value) {
    round(value, ROUND_DECIMALS)
  }

  /**
   * Round a double value to specified decimal places using HALF_UP rounding.
   * Returns NaN and Infinite values unchanged. Returns {@code double} rather
   * than {@code BigDecimal} because callers store results in double-typed columns.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the rounded value, or the original if NaN/Infinite
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static double round(double value, int numDecimals) {
    if (Double.isNaN(value) || Double.isInfinite(value)) { return value }
    if (numDecimals < 0) { throw new IllegalArgumentException("numDecimals cannot be negative: was $numDecimals") }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    bd as double
  }

  /**
   * @deprecated Use {@link Matrix#sample(int, Random)} instead.
   */
  @Deprecated
  static Matrix sample(Matrix matrix, int n) {
    matrix.sample(n)
  }

  /**
   * @deprecated Use {@link Matrix#sample(int, Random)} instead.
   */
  @Deprecated
  static Matrix sample(Matrix matrix, int n, Random random) {
    matrix.sample(n, random)
  }

  /**
   * @deprecated Use {@link Matrix#sampleFraction(Number, Random)} instead.
   */
  @Deprecated
  static Matrix sample(Matrix matrix, double fraction) {
    matrix.sampleFraction(fraction)
  }

  /**
   * @deprecated Use {@link Matrix#sampleFraction(Number, Random)} instead.
   */
  @Deprecated
  static Matrix sample(Matrix matrix, double fraction, Random random) {
    matrix.sampleFraction(fraction, random)
  }

  /**
   * @deprecated Use {@link Matrix#top(Number)} instead.
   */
  @Deprecated
  static Matrix head(Matrix matrix, int n = 5) {
    matrix.top(n)
  }

  /**
   * @deprecated Use {@link Matrix#bottom(Number)} instead.
   */
  @Deprecated
  static Matrix tail(Matrix matrix, int n = 5) {
    matrix.bottom(n)
  }

  /**
   * Check if a column contains any null values.
   *
   * @param values the list of values to check
   * @return true if any value is null
   */
  static boolean hasNulls(List<?> values) {
    values.any { it == null }
  }

  /**
   * Count the number of null values in a column.
   *
   * @param values the list of values to check
   * @return the count of null values
   */
  static int countNulls(List<?> values) {
    values.count { it == null } as int
  }

  /**
   * @deprecated Use {@link Matrix#info()} instead. Note: the matrix-core version uses
   * column names {@code nonNullCount} and {@code nullCount} instead of {@code non-null}
   * and {@code null}.
   */
  @Deprecated
  static Matrix info(Matrix matrix) {
    Matrix result = matrix.info()
    Matrix.builder()
        .data(
            column: result.column('column') as List<Object>,
            type: result.column('type') as List<Object>,
            'non-null': result.column('nonNullCount') as List<Object>,
            'null': result.column('nullCount') as List<Object>,
            unique: result.column('unique') as List<Object>
        )
        .types([String, String, Integer, Integer, Integer])
        .build()
  }

  /**
   * Validate that all expected feature columns exist in the matrix.
   *
   * @param matrix the Matrix to check
   * @param expected the expected feature column names
   * @throws IllegalArgumentException if any expected column is missing
   */
  static void validateFeatureColumns(Matrix matrix, String[] expected) {
    List<String> columns = matrix.columnNames()
    for (String col : expected) {
      if (!columns.contains(col)) {
        throw new IllegalArgumentException(
            "Missing feature column '${col}'. " +
            "Expected: ${expected.toList()}")
      }
    }
  }

  /**
   * Extract feature column names by excluding the target column.
   *
   * @param matrix the Matrix to extract from
   * @param targetColumn the target column to exclude
   * @return the feature column names
   * @throws IllegalArgumentException if no feature columns remain
   */
  static String[] extractFeatureColumns(Matrix matrix, String targetColumn) {
    String[] featureColumns = matrix.columnNames().findAll { it != targetColumn } as String[]
    if (featureColumns.length == 0) {
      throw new IllegalArgumentException(
          "No feature columns remain after excluding target '${targetColumn}'")
    }
    featureColumns
  }

  /**
   * Validate that none of the expected feature columns contain null values.
   *
   * @param matrix the Matrix to check
   * @param expected the feature column names to check for nulls
   * @throws IllegalArgumentException if any feature column contains null values
   */
  static void validateNoNullFeatureColumns(Matrix matrix, String[] expected) {
    for (String col : expected) {
      if (hasNulls(matrix.column(col))) {
        throw new IllegalArgumentException(
            "Feature column '${col}' contains null values. " +
            DROPNA_HINT)
      }
    }
  }

  /**
   * Validate that the target column exists and the matrix is non-empty.
   *
   * @param matrix the Matrix to validate
   * @param targetColumn the expected target column name
   * @throws IllegalArgumentException if the target is missing or the matrix is empty
   */
  static void validateTestMatrix(Matrix matrix, String targetColumn) {
    if (!matrix.columnNames().contains(targetColumn)) {
      throw new IllegalArgumentException(
          "Target column '${targetColumn}' not found in test matrix. Available: ${matrix.columnNames()}")
    }
    if (matrix.rowCount() == 0) {
      throw new IllegalArgumentException(
          'Test matrix is empty (0 rows)')
    }
  }

  /**
   * @deprecated Use {@link Stat#frequency(Matrix, String)} instead. Note: the matrix-core
   * version uses uppercase column names ({@code Value}, {@code Frequency}, {@code Percent})
   * and {@code BigDecimal} percent. This wrapper preserves the v0.1.0 lowercase names and
   * {@code Double} percent type.
   */
  @Deprecated
  static Matrix frequency(Matrix matrix, String columnName) {
    Matrix result = Stat.frequency(matrix, columnName)
    List<Double> percentAsDouble = result[Stat.FREQUENCY_PERCENT].collect { it as double }
    Matrix.builder()
        .data(
            value: result[Stat.FREQUENCY_VALUE] as List<Object>,
            frequency: result[Stat.FREQUENCY_FREQUENCY] as List<Object>,
            percent: percentAsDouble as List<Object>
        )
        .types([String, Integer, Double])
        .build()
  }

}
