package se.alipsa.matrix.stats

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

import static se.alipsa.matrix.core.ValueConverter.convert

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
 *
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
    if (x == null || x == 0) {
      return null  // logNorm always returns null for zero/invalid
    }

    double logResult = Math.log(x.doubleValue())

    if (decimals.length > 0 && !Double.isInfinite(logResult)) {
      BigDecimal bd = new BigDecimal(logResult).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    return convertToType(logResult, x)
  }

  /**
   * Apply logarithmic normalization to an array of values.
   */
  static <T extends Number> List<T> logNorm(T[] values, int... decimals) {
    return values.collect { logNorm(it, decimals) }
  }

  /**
   * Apply logarithmic normalization to a list of values.
   */
  static List logNorm(List values, int... decimals) {
    List result = values.collect { val ->
      if (val instanceof String) {
        return val
      }
      return logNorm(val as Number, decimals)
    }
    // Force BigDecimal preservation - if any input was BigDecimal, ensure outputs stay BigDecimal
    if (values.any { it instanceof BigDecimal }) {
      result = result.collect { val ->
        if (val == null) return null
        if (val instanceof String) return val
        if (val instanceof BigInteger) {
          // Convert BigInteger back to BigDecimal (Groovy may have auto-converted)
          return new BigDecimal(val).setScale(decimals.length > 0 ? decimals[0] : 6, RoundingMode.HALF_EVEN)
        }
        if (val instanceof Number && !(val instanceof BigDecimal)) {
          // Safety: convert any other numeric type to BigDecimal
          return new BigDecimal(val.toString()).setScale(decimals.length > 0 ? decimals[0] : 6, RoundingMode.HALF_EVEN)
        }
        return val
      }
    }
    return result
  }

  /**
   * Apply logarithmic normalization to a specific column in a Matrix.
   */
  static List<? extends Number> logNorm(Matrix table, String columnName, int... decimals) {
    return logNorm(table.column(columnName).data as List, decimals)
  }

  /**
   * Apply logarithmic normalization to all numeric columns in a Matrix.
   */
  static Matrix logNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      columns << logNorm(col as List, decimals)
    }
    return Matrix.builder(table.matrixName)
        .columnNames(table.columnNames())
        .columns(columns)
        .types(table.types())
        .build()
  }

  // ===== MIN-MAX NORMALIZATION =====

  /**
   * Min-Max scaling normalizes values to a [0, 1] range.
   * Formula: Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the value to normalize
   * @param min the minimum value in the dataset
   * @param max the maximum value in the dataset
   * @param decimals optional number of decimal places to round to
   * @return the normalized value in [0, 1] range, or null/NaN for invalid input
   */
  static <T extends Number> T minMaxNorm(T x, Number min, Number max, int... decimals) {
    if (x == null) {
      return minMaxNormNullValue(x)
    }

    double xVal = x.doubleValue()
    double minVal = min.doubleValue()
    double maxVal = max.doubleValue()
    double range = maxVal - minVal

    if (range == 0) {
      return minMaxNormNullValue(x)
    }

    double result = (xVal - minVal) / range

    if (decimals.length > 0) {
      BigDecimal bd = new BigDecimal(result).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    return convertToType(result, x)
  }

  /**
   * Apply min-max normalization to an array of values.
   */
  static <T extends Number> List<T> minMaxNorm(T[] values, int... decimals) {
    if (values.length == 0) return []

    T min = values.min()
    T max = values.max()

    return values.collect { minMaxNorm(it, min, max, decimals) }
  }

  /**
   * Apply min-max normalization to a list of values.
   */
  static List minMaxNorm(List values, int... decimals) {
    if (values.isEmpty()) return []
    if (values[0] instanceof String) return values  // Return strings as-is

    def min = values.min()
    def max = values.max()

    return values.collect { minMaxNorm(it as Number, min, max, decimals) }
  }

  /**
   * Apply min-max normalization to a specific column in a Matrix.
   */
  static List<? extends Number> minMaxNorm(Matrix table, String columnName, int... decimals) {
    return minMaxNorm(table.column(columnName).data as List, decimals)
  }

  /**
   * Apply min-max normalization to all numeric columns in a Matrix.
   */
  static Matrix minMaxNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      columns << minMaxNorm(col as List, decimals)
    }
    return Matrix.builder(table.matrixName)
        .columnNames(table.columnNames())
        .columns(columns)
        .types(table.types())
        .build()
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
   * @param decimals optional number of decimal places to round to
   * @return the normalized value, or null/NaN for invalid input
   */
  static <T extends Number> T meanNorm(T x, Number mean, Number min, Number max, int... decimals) {
    if (x == null) {
      return meanNormNullValue(x)
    }

    double xVal = x.doubleValue()
    double meanVal = mean.doubleValue()
    double minVal = min.doubleValue()
    double maxVal = max.doubleValue()
    double range = maxVal - minVal

    if (range == 0) {
      return meanNormNullValue(x)
    }

    double result = (xVal - meanVal) / range

    if (decimals.length > 0) {
      BigDecimal bd = new BigDecimal(result).setScale(decimals[0], RoundingMode.HALF_EVEN)
      return convertToType(bd, x)
    }

    return convertToType(result, x)
  }

  /**
   * Apply mean normalization to an array of values.
   */
  static <T extends Number> List<T> meanNorm(T[] values, int... decimals) {
    if (values.length == 0) return []

    List<T> list = Arrays.asList(values)
    BigDecimal mean = Stat.mean(list)
    T min = list.min()
    T max = list.max()

    return values.collect { meanNorm(it, mean, min, max, decimals) }
  }

  /**
   * Apply mean normalization to a list of values.
   */
  static List meanNorm(List values, int... decimals) {
    if (values.isEmpty()) return []
    if (values[0] instanceof String) return values  // Return strings as-is

    BigDecimal mean = Stat.mean(values)
    def min = values.min()
    def max = values.max()

    return values.collect { meanNorm(it as Number, mean, min, max, decimals) }
  }

  /**
   * Apply mean normalization to a specific column in a Matrix.
   */
  static List<? extends Number> meanNorm(Matrix table, String columnName, int... decimals) {
    return meanNorm(table.column(columnName).data as List, decimals)
  }

  /**
   * Apply mean normalization to all numeric columns in a Matrix.
   */
  static Matrix meanNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      columns << meanNorm(col as List, decimals)
    }
    return Matrix.builder(table.matrixName)
        .columnNames(table.columnNames())
        .columns(columns)
        .types(table.types())
        .build()
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

    return convertToType(result, x)
  }

  /**
   * Apply standard deviation normalization to an array of values.
   */
  static <T extends Number> List<T> stdScaleNorm(T[] values, int... decimals) {
    if (values.length == 0) return []

    List<T> list = Arrays.asList(values)
    BigDecimal mean = Stat.mean(list)
    BigDecimal stdDev = Stat.sd(list)

    return values.collect { stdScaleNorm(it, mean, stdDev, decimals) }
  }

  /**
   * Apply standard deviation normalization to a list of values.
   */
  static List stdScaleNorm(List values, int... decimals) {
    if (values.isEmpty()) return []
    if (values[0] instanceof String) return values  // Return strings as-is

    BigDecimal mean = Stat.mean(values)
    BigDecimal stdDev = Stat.sd(values)

    return values.collect { stdScaleNorm(it as Number, mean, stdDev, decimals) }
  }

  /**
   * Apply standard deviation normalization to a specific column in a Matrix.
   */
  static List<? extends Number> stdScaleNorm(Matrix table, String columnName, int... decimals) {
    return stdScaleNorm(table.column(columnName).data as List, decimals)
  }

  /**
   * Apply standard deviation normalization to all numeric columns in a Matrix.
   */
  static Matrix stdScaleNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      columns << stdScaleNorm(col as List, decimals)
    }
    return Matrix.builder(table.matrixName)
        .columnNames(table.columnNames())
        .columns(columns)
        .types(table.types())
        .build()
  }

  // ===== HELPER METHODS =====

  /**
   * Returns null/NaN for minMaxNorm: Float.NaN for Byte/Short/Float, Double.NaN for Integer/Long/Double, null for BigInteger/BigDecimal
   */
  private static <T extends Number> T minMaxNormNullValue(T sample) {
    if (sample instanceof BigDecimal || sample instanceof BigInteger) {
      return null
    }
    if (sample instanceof Double || sample instanceof Integer || sample instanceof Long) {
      return Double.NaN as T
    }
    // Byte, Short, Float
    return Float.NaN as T
  }

  /**
   * Returns null/NaN for meanNorm: Float.NaN for Byte/Short/Integer/Long/Float, Double.NaN for Double, null for BigInteger/BigDecimal
   */
  private static <T extends Number> T meanNormNullValue(T sample) {
    if (sample instanceof BigDecimal || sample instanceof BigInteger) {
      return null
    }
    if (sample instanceof Double) {
      return Double.NaN as T
    }
    // Byte, Short, Integer, Long, Float
    return Float.NaN as T
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
    if (sample instanceof BigInteger) {
      return (value instanceof BigInteger ? value : BigInteger.valueOf(value.longValue())) as T
    }
    if (sample instanceof Double) {
      return value.doubleValue() as T
    }
    if (sample instanceof Float) {
      return value.floatValue() as T
    }
    // Integer types (Long, Integer, Short, Byte) are upgraded to Float
    // because normalization operations produce non-integer results
    if (sample instanceof Long || sample instanceof Integer ||
        sample instanceof Short || sample instanceof Byte) {
      return value.floatValue() as T
    }
    // Default to the value's natural type
    return value as T
  }
}
