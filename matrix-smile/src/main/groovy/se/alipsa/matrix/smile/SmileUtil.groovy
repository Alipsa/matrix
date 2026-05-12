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
   * Null values are converted to 0.0.
   *
   * @param matrix the Matrix to convert
   * @return 2D array of double values
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
        result[i][j] = val != null ? val as double : 0.0d
      }
    }

    result
  }

  /**
   * Round a double value to the default number of decimal places (4).
   * Low-level utility method; prefer calling higher-level SmileStats/SmileFeatures methods.
   *
   * @param value the value to round
   * @return the rounded value
   */
  static double round(double value) {
    round(value, ROUND_DECIMALS)
  }

  /**
   * Round a double value to specified decimal places.
   * Low-level utility method; prefer calling higher-level SmileStats/SmileFeatures methods.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places
   * @return the rounded value
   */
  static double round(double value, int numDecimals) {
    if (Double.isNaN(value) || Double.isInfinite(value)) { return value }
    if (numDecimals < 0) { throw new IllegalArgumentException("numDecimals cannot be negative: was $numDecimals") }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    bd as double
  }

  /**
   * Take a random sample of rows from a Matrix.
   *
   * @param matrix the Matrix to sample from
   * @param n the number of rows to sample
   * @return a new Matrix containing the sampled rows
   */
  static Matrix sample(Matrix matrix, int n) {
    sample(matrix, n, new Random())
  }

  /**
   * Take a random sample of rows from a Matrix with a specific random seed.
   *
   * @param matrix the Matrix to sample from
   * @param n the number of rows to sample
   * @param random the Random instance to use
   * @return a new Matrix containing the sampled rows
   */
  static Matrix sample(Matrix matrix, int n, Random random) {
    if (n <= 0) {
      throw new IllegalArgumentException("Sample size must be positive: was $n")
    }
    if (n > matrix.rowCount()) {
      throw new IllegalArgumentException("Sample size ($n) cannot exceed matrix row count (${matrix.rowCount()})")
    }

    List<Integer> indices = (0..<matrix.rowCount()).toList()
    Collections.shuffle(indices, random)
    List<Integer> sampleIndices = indices.take(n).sort()

    Matrix.builder()
        .rows(matrix.rows(sampleIndices) as List<List>)
        .matrixName(matrix.matrixName)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .build()
  }

  /**
   * Take a random sample of a fraction of rows from a Matrix.
   *
   * @param matrix the Matrix to sample from
   * @param fraction the fraction of rows to sample (0.0 to 1.0)
   * @return a new Matrix containing the sampled rows
   */
  static Matrix sample(Matrix matrix, double fraction) {
    sample(matrix, fraction, new Random())
  }

  /**
   * Take a random sample of a fraction of rows from a Matrix with a specific random seed.
   *
   * @param matrix the Matrix to sample from
   * @param fraction the fraction of rows to sample (0.0 to 1.0)
   * @param random the Random instance to use
   * @return a new Matrix containing the sampled rows
   */
  static Matrix sample(Matrix matrix, double fraction, Random random) {
    if (fraction <= 0.0 || fraction > 1.0) {
      throw new IllegalArgumentException("Fraction must be between 0 and 1 (exclusive of 0): was $fraction")
    }
    int n = Math.max(1, (int) Math.round(matrix.rowCount() * fraction))
    sample(matrix, n, random)
  }

  /**
   * Get the first n rows of a Matrix (convenience method).
   *
   * @param matrix the Matrix
   * @param n the number of rows
   * @return a new Matrix with the first n rows
   */
  static Matrix head(Matrix matrix, int n = 5) {
    int take = Math.min(n, matrix.rowCount())
    List<Integer> indices = (0..<take).toList()
    Matrix.builder()
        .rows(matrix.rows(indices) as List<List>)
        .matrixName(matrix.matrixName)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .build()
  }

  /**
   * Get the last n rows of a Matrix (convenience method).
   *
   * @param matrix the Matrix
   * @param n the number of rows
   * @return a new Matrix with the last n rows
   */
  static Matrix tail(Matrix matrix, int n = 5) {
    int rowCount = matrix.rowCount()
    int start = Math.max(0, rowCount - n)
    List<Integer> indices = (start..<rowCount).toList()
    Matrix.builder()
        .rows(matrix.rows(indices) as List<List>)
        .matrixName(matrix.matrixName)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .build()
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
   * Get column information including type, null count, and unique count.
   *
   * @param matrix the Matrix to analyze
   * @return a Matrix with column information
   */
  static Matrix info(Matrix matrix) {
    List<String> columnNames = []
    List<String> types = []
    List<Integer> nonNullCounts = []
    List<Integer> nullCounts = []
    List<Integer> uniqueCounts = []

    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      List<?> column = matrix.column(i)

      columnNames << colName
      types << matrix.type(i).simpleName
      int nullCount = countNulls(column)
      nullCounts << nullCount
      nonNullCounts << (column.size() - nullCount)
      uniqueCounts << column.findAll { it != null }.toSet().size()
    }

    Matrix.builder()
        .data(
            column: columnNames,
            type: types,
            'non-null': nonNullCounts,
            'null': nullCounts,
            unique: uniqueCounts
        )
        .types([String, String, Integer, Integer, Integer])
        .build()
  }

  /**
   * Create a frequency table for a column.
   *
   * @param matrix the Matrix containing the column
   * @param columnName the name of the column to analyze
   * @return a Matrix with value, frequency, and percentage
   */
  static Matrix frequency(Matrix matrix, String columnName) {
    List<?> column = matrix[columnName] as List<?>
    Map<Object, Integer> freq = [:]

    for (Object val : column) {
      freq.merge(val, 1) { old, v -> old + v }
    }

    int total = column.size()
    List<Object> values = []
    List<Integer> frequencies = []
    List<Double> percentages = []

    // Sort by frequency descending
    freq.entrySet()
        .sort { -it.value }
        .each { entry ->
          values << (entry.key == null ? 'null' : entry.key.toString())
          frequencies << entry.value
          percentages << round(entry.value * PERCENT / total, 2)
        }

    Matrix.builder()
        .data(
            value: values,
            frequency: frequencies,
            percent: percentages
        )
        .types([String, Integer, Double])
        .build()
  }

}
