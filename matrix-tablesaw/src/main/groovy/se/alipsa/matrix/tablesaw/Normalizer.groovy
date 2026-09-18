package se.alipsa.matrix.tablesaw

import tech.tablesaw.api.BigDecimalAggregateFunctions
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.FloatColumn

import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.Normalize.MissingValueType

import java.math.MathContext
import java.math.RoundingMode

/**
 * Column-level normalization for Tablesaw numeric columns.
 *
 * <p>Missing input values always produce missing output values. Results are returned as new
 * columns named {@code norm_<original name>}; the input column is never modified.
 *
 * <p>Double and Float overloads delegate the arithmetic to {@link Normalize} from matrix-stats and
 * use Tablesaw's double-precision aggregates. BigDecimal min-max, mean and standard-score overloads
 * take their statistics from {@link BigDecimalAggregateFunctions} (min and max are exact; the mean is
 * rounded to 16 decimal places by {@code Stat.mean}, and the sample standard deviation is the
 * {@link MathContext#DECIMAL64} square root of {@code Stat.variance}) and perform the per-value
 * arithmetic in BigDecimal (exact subtraction, division in {@link MathContext#DECIMAL64}). Inputs are
 * never converted to {@code double}, so values that only differ beyond double precision are still
 * distinguished. Log normalization of BigDecimal values delegates to
 * {@link Normalize#logNorm(Number, int...)}.
 */
class Normalizer {

  private static final String NORM_PREFIX = 'norm_'
  private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL64

  /**
   * Natural-log normalization of each non-missing value.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static DoubleColumn logNorm(DoubleColumn column, int... decimals) {
    mapDoubles(column) { double x -> Normalize.logNorm(x, decimals) }
  }

  /**
   * Natural-log normalization of each non-missing value.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static FloatColumn logNorm(FloatColumn column, int... decimals) {
    mapFloats(column) { float x -> Normalize.logNorm(x, decimals) }
  }

  /**
   * Natural-log normalization of each non-missing value.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static BigDecimalColumn logNorm(BigDecimalColumn column, int... decimals) {
    mapBigDecimals(column) { BigDecimal x -> Normalize.logNorm(x, decimals) }
  }

  /**
   * Min-max normalization to the [0, 1] range.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static DoubleColumn minMaxNorm(DoubleColumn column, int... decimals) {
    double min = column.min()
    double max = column.max()
    mapDoubles(column) { double x -> Normalize.minMaxNorm(x, min, max, MissingValueType.DOUBLE_NAN, decimals) }
  }

  /**
   * Min-max normalization to the [0, 1] range.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static FloatColumn minMaxNorm(FloatColumn column, int... decimals) {
    float min = column.min() as float
    float max = column.max() as float
    mapFloats(column) { float x -> Normalize.minMaxNorm(x, min, max, MissingValueType.FLOAT_NAN, decimals) }
  }

  /**
   * Min-max normalization to the [0, 1] range in BigDecimal arithmetic (exact subtraction, division
   * in {@link MathContext#DECIMAL64}): {@code (x - min) / (max - min)}.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to (HALF_EVEN)
   * @return a new column named {@code norm_<name>}; all missing if the column has no values or
   *         all values are equal (zero range)
   */
  static BigDecimalColumn minMaxNorm(BigDecimalColumn column, int... decimals) {
    BigDecimal min = BigDecimalAggregateFunctions.min.summarize(column)
    BigDecimal max = BigDecimalAggregateFunctions.max.summarize(column)
    if (min == null || max == null) {
      return allMissing(column)
    }
    BigDecimal range = max - min
    mapBigDecimals(column) { BigDecimal x -> divideRounded(x - min, range, decimals) }
  }

  /**
   * Mean normalization: (x - mean) / (max - min).
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static DoubleColumn meanNorm(DoubleColumn column, int... decimals) {
    double min = column.min()
    double max = column.max()
    double mean = column.mean()
    mapDoubles(column) { double x -> Normalize.meanNorm(x, mean, min, max, MissingValueType.DOUBLE_NAN, decimals) }
  }

  /**
   * Mean normalization: (x - mean) / (max - min).
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static FloatColumn meanNorm(FloatColumn column, int... decimals) {
    float min = column.min() as float
    float max = column.max() as float
    float mean = column.mean() as float
    mapFloats(column) { float x -> Normalize.meanNorm(x, mean, min, max, MissingValueType.FLOAT_NAN, decimals) }
  }

  /**
   * Mean normalization in BigDecimal arithmetic: {@code (x - mean) / (max - min)}, where the mean is
   * rounded to 16 decimal places by {@code Stat.mean} and the division uses {@link MathContext#DECIMAL64}.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to (HALF_EVEN)
   * @return a new column named {@code norm_<name>}; all missing if the column has no values or
   *         all values are equal (zero range)
   */
  static BigDecimalColumn meanNorm(BigDecimalColumn column, int... decimals) {
    BigDecimal min = BigDecimalAggregateFunctions.min.summarize(column)
    BigDecimal max = BigDecimalAggregateFunctions.max.summarize(column)
    BigDecimal mean = BigDecimalAggregateFunctions.mean.summarize(column)
    if (min == null || max == null || mean == null) {
      return allMissing(column)
    }
    BigDecimal range = max - min
    mapBigDecimals(column) { BigDecimal x -> divideRounded(x - mean, range, decimals) }
  }

  /**
   * Standard-score normalization: (x - mean) / sd.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static DoubleColumn stdScaleNorm(DoubleColumn column, int... decimals) {
    double mean = column.mean()
    double stdDev = column.standardDeviation()
    mapDoubles(column) { double x -> Normalize.stdScaleNorm(x, mean, stdDev, decimals) }
  }

  /**
   * Standard-score normalization: (x - mean) / sd.
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to
   * @return a new column named {@code norm_<name>}
   */
  static FloatColumn stdScaleNorm(FloatColumn column, int... decimals) {
    float mean = column.mean() as float
    float stdDev = column.standardDeviation() as float
    mapFloats(column) { float x -> Normalize.stdScaleNorm(x, mean, stdDev, decimals) }
  }

  /**
   * Standard-score normalization in BigDecimal arithmetic: {@code (x - mean) / sd} (mean rounded to
   * 16 decimal places, sample standard deviation from {@link BigDecimalAggregateFunctions#stdDev},
   * division in {@link MathContext#DECIMAL64}).
   *
   * @param column the column to normalize
   * @param decimals optional number of decimals to round to (HALF_EVEN)
   * @return a new column named {@code norm_<name>}; all missing if fewer than two values are
   *         present or the standard deviation is zero
   */
  static BigDecimalColumn stdScaleNorm(BigDecimalColumn column, int... decimals) {
    BigDecimal mean = BigDecimalAggregateFunctions.mean.summarize(column)
    BigDecimal stdDev = BigDecimalAggregateFunctions.stdDev.summarize(column)
    if (mean == null || stdDev == null) {
      return allMissing(column)
    }
    mapBigDecimals(column) { BigDecimal x -> divideRounded(x - mean, stdDev, decimals) }
  }

  private static DoubleColumn mapDoubles(DoubleColumn column, Closure<Double> fn) {
    List<Double> vals = []
    for (int i = 0; i < column.size(); i++) {
      vals.add(column.isMissing(i) ? null : fn.call(column.getDouble(i)))
    }
    DoubleColumn.create(NORM_PREFIX + column.name(), vals as Double[])
  }

  private static FloatColumn mapFloats(FloatColumn column, Closure<Float> fn) {
    List<Float> vals = []
    for (int i = 0; i < column.size(); i++) {
      vals.add(column.isMissing(i) ? null : fn.call(column.getFloat(i)))
    }
    FloatColumn.create(NORM_PREFIX + column.name(), vals as Float[])
  }

  private static BigDecimalColumn mapBigDecimals(BigDecimalColumn column, Closure<BigDecimal> fn) {
    List<BigDecimal> vals = []
    for (int i = 0; i < column.size(); i++) {
      vals.add(column.isMissing(i) ? null : fn.call(column.get(i)))
    }
    BigDecimalColumn.create(NORM_PREFIX + column.name(), vals as BigDecimal[])
  }

  private static BigDecimalColumn allMissing(BigDecimalColumn column) {
    BigDecimalColumn.create(NORM_PREFIX + column.name(), column.size())
  }

  /**
   * {@code numerator / denominator} in {@link #DIVISION_CONTEXT}, {@code null} (missing) when the
   * denominator is zero, rounded to {@code decimals[0]} places (HALF_EVEN) when decimals are given.
   */
  private static BigDecimal divideRounded(BigDecimal numerator, BigDecimal denominator, int... decimals) {
    if (denominator.signum() == 0) {
      return null
    }
    BigDecimal result = numerator.divide(denominator, DIVISION_CONTEXT)
    decimals.length > 0 ? result.setScale(decimals[0], RoundingMode.HALF_EVEN) : result
  }
}
