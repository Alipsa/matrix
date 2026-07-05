package se.alipsa.matrix.stats

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * Implements various ways of normalizing (scaling) data e.g.
 * scale the values so that the they have a mean of 0 and a standard deviation of 1
 * It implements 4 different approaches
 * <ol>
 *   <li>logarithmic normalization</li>
 *   <li>Min-Max scaling, Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )</li>
 *   <li>Mean normalization, X´ = ( X - μ ) / ( max(X) - min(X) )</li>
 *   <li>Standard deviation normalization (Z score), Z = ( X<sub>i</sub> - μ ) / σ</li>
 * </ol>
 */
class Normalize {

  // ===== LOG NORMALIZATION =====

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param x the value to normalize
   * @param decimals optional number of decimal places to round to
   * @return the natural logarithm (base e) of the x value, or null/NaN for invalid input
   */
  static <T extends Number> T logNorm(T x, int... decimals) {
    if (x == null || x <= 0) {
      return null  // logNorm always returns null for zero/negative/invalid
    }

    if (x instanceof Float && (x as Float).isInfinite()) {
      return x as T
    }

    if (x instanceof Double && (x as Double).isInfinite()) {
      return x as T
    }

    BigDecimal logResult = x.log()

    if (decimals.length > 0) {
      BigDecimal bd = logResult.setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }
    convertToType(logResult, x)
  }

  /**
   * Apply logarithmic normalization to an array of values.
   */
  static <T extends Number> List<T> logNorm(T[] values, int... decimals) {
    values.collect { logNorm(it, decimals) }
  }

  /**
   * Apply logarithmic normalization to a list of values.
   */
  static List logNorm(List values, int... decimals) {
    if (!containsOnlyNumbers(values)) {
      return values
    }
    List result = values.collect { val -> logNorm(val as Number, decimals) }
    // Force BigDecimal preservation - if any input was BigDecimal, ensure outputs stay BigDecimal
    if (values.any { it instanceof BigDecimal }) {
      result = result.collect { val ->
        if (val == null) {
          return null
        }
        if (val instanceof BigInteger) {
          // Convert BigInteger back to BigDecimal (Groovy may have auto-converted)
          return new BigDecimal(val).setScale(decimals.length > 0 ? decimals[0] : 6, RoundingMode.HALF_EVEN)
        }
        if (val instanceof Number && !(val instanceof BigDecimal)) {
          // Safety: convert any other numeric type to BigDecimal
          return new BigDecimal(val.toString()).setScale(decimals.length > 0 ? decimals[0] : 6, RoundingMode.HALF_EVEN)
        }
        val
      }
    }
    result
  }

  /**
   * Apply logarithmic normalization to a specific column in a Matrix.
   */
  static List<? extends Number> logNorm(Matrix table, String columnName, int... decimals) {
    logNorm(table.column(columnName) as List, decimals)
  }

  /**
   * Apply logarithmic normalization to all numeric columns in a Matrix.
   */
  static Matrix logNorm(Matrix table, int... decimals) {
    normalizeMatrix(table, Normalization.LOG, decimals)
  }

  // ===== MIN-MAX NORMALIZATION =====

  /**
   * Min-Max scaling normalizes values to a [0, 1] range.
   * Formula: Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the value to normalize
   * @param min the minimum value in the dataset
   * @param max the maximum value in the dataset
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized value in [0, 1] range, or the requested missing-value sentinel for invalid input
   */
  static <T extends Number> T minMaxNorm(T x, Number min, Number max,
                                         MissingValueType missingValueType = MissingValueType.NULL,
                                         int... decimals) {
    if (x == null) {
      return nullSentinel(missingValueType) as T
    }

    double xVal = x.doubleValue()
    double minVal = min.doubleValue()
    double maxVal = max.doubleValue()
    double range = maxVal - minVal

    if (range == 0) {
      return nullSentinel(missingValueType) as T
    }

    double result = (xVal - minVal) / range

    if (decimals.length > 0) {
      BigDecimal bd = new BigDecimal(result).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    convertToType(result, x)
  }

  /**
   * Apply min-max normalization to an array of values.
   *
   * @param values the values to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized values
   */
  static <T extends Number> List<T> minMaxNorm(T[] values,
                                               MissingValueType missingValueType = MissingValueType.NULL,
                                               int... decimals) {
    if (values.length == 0) {
      return []
    }

    List<T> numbers = values.findAll { it != null } as List<T>
    if (numbers.isEmpty()) {
      return values.collect { nullSentinel(missingValueType) as T }
    }
    T min = numbers.min()
    T max = numbers.max()

    values.collect { minMaxNorm(it, min, max, missingValueType, decimals) }
  }

  /**
   * Apply min-max normalization to a list of values.
   *
   * @param values the values to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized values, or the original list if it contains non-numeric values
   */
  static List minMaxNorm(List values, MissingValueType missingValueType = MissingValueType.NULL, int... decimals) {
    if (values.isEmpty()) {
      return []
    }
    if (!containsOnlyNumbers(values)) {
      return values
    }

    List<Number> numbers = numericValues(values)
    Number min = numbers.min()
    Number max = numbers.max()

    values.collect { minMaxNorm(it as Number, min, max, missingValueType, decimals) }
  }

  /**
   * Apply min-max normalization to a specific column in a Matrix.
   *
   * @param table the Matrix containing the column
   * @param columnName the column to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized column values
   */
  static List<? extends Number> minMaxNorm(Matrix table, String columnName,
                                           MissingValueType missingValueType = MissingValueType.NULL,
                                           int... decimals) {
    minMaxNorm(table.column(columnName) as List, missingValueType, decimals)
  }

  /**
   * Apply min-max normalization to all numeric columns in a Matrix.
   *
   * @param table the Matrix to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return a new Matrix with normalized numeric columns
   */
  static Matrix minMaxNorm(Matrix table, MissingValueType missingValueType = MissingValueType.NULL, int... decimals) {
    normalizeMatrix(table, Normalization.MIN_MAX, missingValueType, decimals)
  }

  // ===== MEAN NORMALIZATION =====

  /**
   * Mean normalization scales values relative to the mean and range.
   * Formula: X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param x the value to normalize
   * @param mean the mean of the dataset
   * @param min the minimum value in the dataset
   * @param max the maximum value in the dataset
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized value, or the requested missing-value sentinel for invalid input
   */
  static <T extends Number> T meanNorm(T x, Number mean, Number min, Number max,
                                       MissingValueType missingValueType = MissingValueType.NULL,
                                       int... decimals) {
    if (x == null) {
      return nullSentinel(missingValueType) as T
    }

    double xVal = x.doubleValue()
    double meanVal = mean.doubleValue()
    double minVal = min.doubleValue()
    double maxVal = max.doubleValue()
    double range = maxVal - minVal

    if (range == 0) {
      return nullSentinel(missingValueType) as T
    }

    double result = (xVal - meanVal) / range

    if (decimals.length > 0) {
      BigDecimal bd = new BigDecimal(result).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    convertToType(result, x)
  }

  /**
   * Apply mean normalization to an array of values.
   *
   * @param values the values to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized values
   */
  static <T extends Number> List<T> meanNorm(T[] values,
                                             MissingValueType missingValueType = MissingValueType.NULL,
                                             int... decimals) {
    if (values.length == 0) {
      return []
    }

    List<T> numbers = values.findAll { it != null } as List<T>
    if (numbers.isEmpty()) {
      return values.collect { nullSentinel(missingValueType) as T }
    }
    BigDecimal mean = Stat.mean(numbers)
    T min = numbers.min()
    T max = numbers.max()

    values.collect { meanNorm(it, mean, min, max, missingValueType, decimals) }
  }

  /**
   * Apply mean normalization to a list of values.
   *
   * @param values the values to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized values, or the original list if it contains non-numeric values
   */
  static List meanNorm(List values, MissingValueType missingValueType = MissingValueType.NULL, int... decimals) {
    if (values.isEmpty()) {
      return []
    }
    if (!containsOnlyNumbers(values)) {
      return values
    }

    List<Number> numbers = numericValues(values)
    BigDecimal mean = Stat.mean(numbers)
    Number min = numbers.min()
    Number max = numbers.max()

    values.collect { meanNorm(it as Number, mean, min, max, missingValueType, decimals) }
  }

  /**
   * Apply mean normalization to a specific column in a Matrix.
   *
   * @param table the Matrix containing the column
   * @param columnName the column to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return the normalized column values
   */
  static List<? extends Number> meanNorm(Matrix table, String columnName,
                                         MissingValueType missingValueType = MissingValueType.NULL,
                                         int... decimals) {
    meanNorm(table.column(columnName) as List, missingValueType, decimals)
  }

  /**
   * Apply mean normalization to all numeric columns in a Matrix.
   *
   * @param table the Matrix to normalize
   * @param missingValueType sentinel to return for null or undefined results; defaults to {@link MissingValueType#NULL}
   * @param decimals optional number of decimal places to round to
   * @return a new Matrix with normalized numeric columns
   */
  static Matrix meanNorm(Matrix table, MissingValueType missingValueType = MissingValueType.NULL, int... decimals) {
    normalizeMatrix(table, Normalization.MEAN, missingValueType, decimals)
  }

  // ===== STANDARD DEVIATION NORMALIZATION (Z-SCORE) =====

  /**
   * Standard deviation normalization (Z-score) scales values by standard deviations from the mean.
   * Formula: Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param x the value to normalize
   * @param mean the mean of the dataset
   * @param stdDev the standard deviation of the dataset
   * @param decimals optional number of decimal places to round to
   * @return the z-score, or null/NaN for invalid input
   */
  static <T extends Number> T stdScaleNorm(T x, Number mean, Number stdDev, int... decimals) {
    if (x == null) {
      return null  // stdScaleNorm always returns null for invalid
    }

    double xVal = x.doubleValue()
    double meanVal = mean.doubleValue()
    double stdDevVal = stdDev.doubleValue()

    if (stdDevVal == 0) {
      return null  // stdScaleNorm always returns null for zero stddev
    }

    double result = (xVal - meanVal) / stdDevVal

    if (decimals.length > 0) {
      BigDecimal bd = new BigDecimal(result).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    convertToType(result, x)
  }

  /**
   * Apply standard deviation normalization to an array of values.
   */
  static <T extends Number> List<T> stdScaleNorm(T[] values, int... decimals) {
    if (values.length == 0) {
      return []
    }

    List<T> list = values.toList()
    BigDecimal mean = Stat.mean(list)
    BigDecimal stdDev = Stat.sd(list)

    values.collect { stdScaleNorm(it, mean, stdDev, decimals) }
  }

  /**
   * Apply standard deviation normalization to a list of values.
   */
  static List stdScaleNorm(List values, int... decimals) {
    if (values.isEmpty()) {
      return []
    }
    if (!containsOnlyNumbers(values)) {
      return values
    }

    // nulls are excluded from stats but preserved in output via scalar null-handling
    List<Number> numbers = numericValues(values)
    BigDecimal mean = Stat.mean(numbers)
    BigDecimal stdDev = Stat.sd(numbers)

    values.collect { stdScaleNorm(it as Number, mean, stdDev, decimals) }
  }

  /**
   * Apply standard deviation normalization to a specific column in a Matrix.
   */
  static List<? extends Number> stdScaleNorm(Matrix table, String columnName, int... decimals) {
    stdScaleNorm(table.column(columnName) as List, decimals)
  }

  /**
   * Apply standard deviation normalization to all numeric columns in a Matrix.
   */
  static Matrix stdScaleNorm(Matrix table, int... decimals) {
    normalizeMatrix(table, Normalization.STD_SCALE, decimals)
  }

  // ===== HELPER METHODS =====

  private enum Normalization {
    LOG,
    MIN_MAX,
    MEAN,
    STD_SCALE
  }

  /**
   * Missing-value sentinel options for normalization methods that can produce undefined results.
   */
  enum MissingValueType {
    NULL(null),
    FLOAT_NAN(Float.NaN),
    DOUBLE_NAN(Double.NaN)

    final Number sentinel

    private MissingValueType(Number sentinel) {
      this.sentinel = sentinel
    }
  }

  private static Matrix normalizeMatrix(Matrix table, Normalization normalization, int... decimals) {
    normalizeMatrix(table, normalization, MissingValueType.NULL, decimals)
  }

  private static Matrix normalizeMatrix(Matrix table, Normalization normalization,
                                        MissingValueType missingValueType, int... decimals) {
    List<List> columns = []
    List<Class> types = []
    for (Column col in table.columns()) {
      if (isNumericColumn(col)) {
        List normalized = normalizeList(col as List, normalization, missingValueType, decimals)
        columns << normalized
        types << transformedType(normalized, col.type)
      } else {
        columns << col.collect()
        types << col.type
      }
    }
    Matrix.builder(table.matrixName)
        .columnNames(table.columnNames())
        .columns(columns)
        .types(types)
        .build()
  }

  private static List normalizeList(List values, Normalization normalization, int... decimals) {
    normalizeList(values, normalization, MissingValueType.NULL, decimals)
  }

  private static List normalizeList(List values, Normalization normalization,
                                    MissingValueType missingValueType, int... decimals) {
    switch (normalization) {
      case LOG -> logNorm(values, decimals)
      case MIN_MAX -> minMaxNorm(values, missingValueType, decimals)
      case MEAN -> meanNorm(values, missingValueType, decimals)
      case STD_SCALE -> stdScaleNorm(values, decimals)
      default -> throw new IllegalArgumentException("Unsupported normalization: $normalization")
    }
  }

  private static boolean isNumericColumn(Column col) {
    boolean hasNonNullValue = false
    boolean allValuesAreNumeric = col.every { value ->
      if (value == null) {
        true
      } else {
        hasNonNullValue = true
        value instanceof Number
      }
    }
    if (!hasNonNullValue || !allValuesAreNumeric) {
      return false
    }
    if (col.type != null && Number.isAssignableFrom(col.type)) {
      return true
    }
    col.type == null || col.type == Object
  }

  private static boolean containsOnlyNumbers(List values) {
    boolean hasNonNullValue = false
    boolean allValuesAreNumeric = values.every { value ->
      if (value == null) {
        true
      } else {
        hasNonNullValue = true
        value instanceof Number
      }
    }
    hasNonNullValue && allValuesAreNumeric
  }

  private static List<Number> numericValues(List values) {
    values.findAll { it instanceof Number } as List<Number>
  }

  // Scalar normalization functions preserve per-element type, so the first non-null value reliably
  // represents the output type for the whole column.
  private static Class transformedType(List values, Class fallbackType) {
    def transformedValue = values.find { it != null }
    transformedValue == null ? fallbackType : transformedValue.class
  }

  private static Number nullSentinel(MissingValueType missingValueType) {
    missingValueType.sentinel
  }

  /**
   * Converts a numeric value to the same type as the sample.
   * For normalization operations, integer types are upgraded to Float since
   * operations like log, normalization produce non-integer results.
   */
  private static <T extends Number> T convertToType(Number value, T sample) {
    if (sample instanceof BigDecimal) {
      // Always ensure we return BigDecimal, not BigInteger
      BigDecimal result
      if (value instanceof BigDecimal) {
        result = (BigDecimal) value
      } else {
        result = new BigDecimal(value.toString())
      }
      // CRITICAL: Ensure scale > 0 to prevent Groovy from converting to BigInteger
      // Groovy may auto-convert BigDecimal to BigInteger for whole numbers with scale 0
      // We ensure minimum scale of 1 for all BigDecimal results
      int targetScale = Math.max(result.scale(), 1)
      if (result.scale() < targetScale) {
        result = result.setScale(targetScale, RoundingMode.HALF_EVEN)
      }
      return result as T
    }
    if (sample instanceof Double) {
      return value.doubleValue() as T
    }
    if (sample instanceof Float) {
      return value.floatValue() as T
    }
    // Integer types (BigInteger, Long, Integer, Short, Byte) are upgraded to Float
    // because normalization operations produce non-integer results
    if (sample instanceof BigInteger || sample instanceof Long || sample instanceof Integer ||
        sample instanceof Short || sample instanceof Byte) {
      return value.floatValue() as T
    }
    // Default to the value's natural type
    value as T
  }
}
