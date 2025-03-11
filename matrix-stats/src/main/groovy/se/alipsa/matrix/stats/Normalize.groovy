package se.alipsa.matrix.stats

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
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


  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param x
   * @return the natural logarithm (base e) of the x value
   */
  static BigDecimal logNorm(BigDecimal x, int... decimals) {
    if (x == null) return null
    def result = Math.log(x) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN)
    }
    return result
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param x
   * @return the natural logarithm (base e) of the x value
   */
  static <T extends Number> T logNorm(T x, int... decimals) {
    if (x == null) return null
    def result = Math.log(x as BigDecimal) as T
    if (decimals.length > 0) {
      return (result as BigDecimal).setScale(decimals[0], RoundingMode.HALF_EVEN) as T
    }
    return result
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param x
   * @return the natural logarithm (base e) of the x value
   */
  static Double logNorm(Double x, int... decimals) {
    if (x == null) return Double.NaN
    def result = Math.log(x) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).doubleValue()
    }
    return result.doubleValue()
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param x
   * @return the natural logarithm (base e) of the x value
   */
  static Float logNorm(Float x, int... decimals) {
    if (x == null) return Float.NaN
    def result = Math.log(x) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).floatValue()
    }
    return result.floatValue()
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the double column to normalize
   * @return a double column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static List<Double> logNorm(Double[] column, int... decimals) {
    def vals = []
    for (Double x : column) {
      vals.add(logNorm(x, decimals))
    }
    return vals
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the Float column to normalize
   * @return a Float column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static List<Float> logNorm(Float[] column, int... decimals) {
    def vals = []
    for (Float x : column) {
      vals.add(logNorm(x, decimals))
    }
    return vals
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the BigDecimal column to normalize
   * @return a BigDecimal column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static List<BigDecimal> logNorm(BigDecimal[] column, int... decimals) {
    def vals = []
    for (BigDecimal x : column) {
      vals.add(logNorm(x, decimals))
    }
    return vals
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the BigDecimal column to normalize
   * @return a BigDecimal column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static List<? extends Number> logNorm(List<? extends Number> column, int... decimals) {
    def vals = []
    for (def x : column) {
      vals.add(logNorm(x, decimals))
    }
    return vals
  }

  static List<? extends Number> logNorm(Matrix table, String columnName, int... decimals) {
    if (Number.isAssignableFrom(table.type(columnName))) {
      return logNorm(table[columnName] as List<? extends Number>, decimals)
    } else {
      throw new IllegalArgumentException("$columnName is not a numeric column")
    }
  }

  static Matrix logNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      columns << logNorm(col, decimals)
    }
    new MatrixBuilder(table.matrixName)
        .columns(columns)
        .types(table.types())
        .build()
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between 0 and 1
   */
  static BigDecimal minMaxNorm(BigDecimal x, BigDecimal minX, BigDecimal maxX, int... decimals) {
    if (x == null || minX == null || maxX == null) {
      return null
    }
    def result = (x - minX) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN)
    }
    return result
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between 0 and 1
   */
  static <T extends Number> T minMaxNorm(T x, T minX, T maxX, int... decimals) {
    if (x == null || minX == null || maxX == null) {
      return null
    }
    def result = ((x - minX) / (maxX - minX)) as T
    if (decimals.length > 0) {
      return (result as BigDecimal).setScale(decimals[0], RoundingMode.HALF_EVEN) as T
    }
    return result
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between 0 and 1
   */
  static Double minMaxNorm(Double x, Double minX, Double maxX, int... decimals) {
    if (x == null || Double.isNaN(x) || minX == null || Double.isNaN(minX) || maxX == null || Double.isNaN(maxX)) {
      return Double.NaN
    }
    def result = (x - minX) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).doubleValue()
    }
    return result.doubleValue()
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between 0 and 1
   */
  static Float minMaxNorm(Float x, Float minX, Float maxX, int... decimals) {
    if (x == null || Float.isNaN(x) || minX == null || Float.isNaN(minX) || maxX == null || Float.isNaN(maxX)) {
      return Float.NaN
    }
    def result = (x - minX) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).floatValue()
    }
    return result.floatValue()
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the double column to scale
   * @return a new double column of scaled values between 0 and 1
   */
  static List<Double> minMaxNorm(Double[] column, int... decimals) {
    def min = column.min()
    def max = column.max()

    List<Double> vals = []
    for (def x : column) {
      vals.add(minMaxNorm(x, min, max, decimals))
    }
    return vals
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new float column of scaled values between 0 and 1
   */
  static List<Float> minMaxNorm(Float[] column, int... decimals) {
    List<Float> col = column as List<Float>
    def min = col.min() as float
    def max = col.max() as float

    List<Float> vals = []
    for (def x : col) {
      vals.add(minMaxNorm(x, min, max, decimals))
    }
    return vals
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new float column of scaled values between 0 and 1
   */
  static List<BigDecimal> minMaxNorm(BigDecimal[] column, int... decimals) {
    List<BigDecimal> col = column as List<BigDecimal>
    def min = col.min() as BigDecimal
    def max = col.max() as BigDecimal

    List<BigDecimal> vals = []
    for (def x : col) {
      vals.add(minMaxNorm(x, min, max, decimals))
    }
    return vals
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new float column of scaled values between 0 and 1
   */
  static List<? extends Number> minMaxNorm(List<? extends Number> column, int... decimals) {
    def min = column.min()
    def max = column.max()

    List<? extends Number> vals = []
    for (def x : column) {
      def type = x.getClass()
      vals.add(minMaxNorm(x, convert(min, type), convert(max, type), decimals))
    }
    return vals
  }

  static List<? extends Number> minMaxNorm(Matrix table, String columnName, int... decimals) {
    if (Number.isAssignableFrom(table.type(columnName))) {
      return minMaxNorm(table[columnName] as List<? extends Number>, decimals)
    } else {
      throw new IllegalArgumentException("$columnName is not a numeric column")
    }
  }

  static Matrix minMaxNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      if (! Number.isAssignableFrom(col.type)) {
        columns << col
      } else {
        columns << minMaxNorm(col, decimals)
      }
    }
    new MatrixBuilder(table.matrixName)
        .columns(columns)
        .columnNames(table.columnNames())
        .types(table.types())
        .build()
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between -1 and 1
   */
  static BigDecimal meanNorm(BigDecimal x, BigDecimal sampleMean, BigDecimal minX, BigDecimal maxX, int... decimals) {
    if (x == null || sampleMean == null || minX == null || maxX == null) {
      return null
    }
    def result = (x - sampleMean) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN)
    }
    return result
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between -1 and 1
   */
  static <T extends Number> T meanNorm(T x, T sampleMean, T minX, T maxX, int... decimals) {
    if (x == null || sampleMean == null || minX == null || maxX == null) {
      return null
    }
    def result = ((x - sampleMean) / (maxX - minX)) as T
    if (decimals.length > 0) {
      return (result as BigDecimal).setScale(decimals[0], RoundingMode.HALF_EVEN) as T
    }
    return result
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between -1 and 1
   */
  static Double meanNorm(Double x, Double sampleMean, Double minX, Double maxX, int... decimals) {
    if (x == null || Double.isNaN(x) || sampleMean == null || Double.isNaN(sampleMean) || minX == null || Double.isNaN(minX) || maxX == null || Double.isNaN(maxX)) {
      return Double.NaN
    }
    def result = (x - sampleMean) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).doubleValue()
    }
    return result.doubleValue()
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param minX the lowest value in the distribution
   * @param maxX the highest value in the distribution
   * @return a scaled value between -1 and 1
   */
  static Float meanNorm(Float x, Float sampleMean, Float minX, Float maxX, int... decimals) {
    if (x == null || Float.isNaN(x) || sampleMean == null || Float.isNaN(sampleMean) || minX == null || Float.isNaN(minX) || maxX == null || Float.isNaN(maxX)) {
      return Float.NaN
    }
    def result = (x - sampleMean) / (maxX - minX) as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).floatValue()
    }
    return result.floatValue()
  }

 /**
  * Scales the data values to be between (–1, 1), the formula is
  * X´ = ( X - μ ) / ( max(X) - min(X) )
  *
  * @param column the double column to scale
  * @return a new, normalized double column
  */
  static List<Double> meanNorm(Double[] column, int... decimals) {
    List<Double> col = column as List<Double>
    def min = col.min()
    def max = col.max()
    def mean = Stat.mean(col)

    List<Double> vals = []
    for (def x : col) {
      vals.add(meanNorm(x, mean, min, max, decimals))
    }
    return vals
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new, normalized float column
   */
  static List<Float> meanNorm(Float[] column, int... decimals) {
    List<Float> col = column as List<Float>
    def min = col.min() as float
    def max = col.max() as float
    def mean = Stat.mean(col) as float

    List<Float> vals = []
    for (def x : col) {
      vals.add(meanNorm(x, mean, min, max, decimals))
    }
    return vals
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new, normalized float column
   */
  static List<BigDecimal> meanNorm(BigDecimal[] column, int... decimals) {
    List<BigDecimal> col = column as List<BigDecimal>
    def min = col.min() as BigDecimal
    def max = col.max() as BigDecimal
    def mean = Stat.mean(col) as BigDecimal

    List<BigDecimal> vals = []
    for (def x : col) {
      vals.add(meanNorm(x, mean, min, max, decimals))
    }
    return vals
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new, normalized float column
   */
  static List<Number> meanNorm(List<? extends Number> column, int... decimals) {
    def min = column.min()
    def max = column.max()
    def mean = Stat.mean(column)

    List<? extends Number> vals = []
    for (def x : column) {
      def type = x.getClass()
      vals.add(meanNorm(x, convert(mean, type), convert(min, type), convert(max, type), decimals))
    }
    return vals
  }

  static List<? extends Number> meanNorm(Matrix table, String columnName, int... decimals) {
    if (Number.isAssignableFrom(table.type(columnName))) {
      return meanNorm(table[columnName] as List<? extends Number>, decimals)
    } else {
      throw new IllegalArgumentException("$columnName is not a numeric column")
    }
  }

  static Matrix meanNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      if (! Number.isAssignableFrom(col.type)) {
        columns << col
      } else {
        columns << meanNorm(col, decimals)
      }
    }
    new MatrixBuilder(table.matrixName)
        .columns(columns)
        .columnNames(table.columnNames())
        .types(table.types())
        .build()
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param stdDeviation the standard deviation (σ)
   * @return a scaled value so that the mean of the observed values will be 0 and standard deviation will be 1
   */
  static BigDecimal stdScaleNorm(BigDecimal x, BigDecimal sampleMean, BigDecimal stdDeviation, int... decimals) {
    if (x == null || sampleMean == null || stdDeviation == null) {
      return null
    }
    def result = (x - sampleMean) / stdDeviation as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN)
    }
    return result
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param stdDeviation the standard deviation (σ)
   * @return a scaled value so that the mean of the observed values will be 0 and standard deviation will be 1
   */
  static <T extends Number> T stdScaleNorm(T x, T sampleMean, T stdDeviation, int... decimals) {
    if (x == null || sampleMean == null || stdDeviation == null) {
      return null
    }
    def result = ((x - sampleMean) / stdDeviation) as T
    if (decimals.length > 0) {
      return (result as BigDecimal).setScale(decimals[0], RoundingMode.HALF_EVEN) as T
    }
    return result
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param stdDeviation the standard deviation (σ)
   * @return a scaled value so that the mean of the observed values will be 0 and standard deviation will be 1
   */
  static Double stdScaleNorm(Double x, Double sampleMean, Double stdDeviation, int... decimals) {
    if (x == null || Double.isNaN(x) || sampleMean == null || Double.isNaN(sampleMean) || stdDeviation == null || Double.isNaN(stdDeviation)) {
      return Double.NaN
    }
    def result = (x - sampleMean) / stdDeviation as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN)
    }
    return result.doubleValue()
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param x the observed value
   * @param sampleMean the sample mean (μ)
   * @param stdDeviation the standard deviation (σ)
   * @return a scaled value so that the mean of the observed values will be 0 and standard deviation will be 1
   */
  static Float stdScaleNorm(Float x, Float sampleMean, Float stdDeviation, int... decimals) {
    if (x == null || Float.isNaN(x) || sampleMean == null || Float.isNaN(sampleMean) || stdDeviation == null || Float.isNaN(stdDeviation)) {
      return Float.NaN
    }
    println ("(x - sampleMean) / stdDeviation = ${(x - sampleMean) / stdDeviation}")
    def result = (x - sampleMean) / stdDeviation as BigDecimal
    if (decimals.length > 0) {
      return result.setScale(decimals[0], RoundingMode.HALF_EVEN).floatValue()
    }
    return result.floatValue()
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param column the double column to scale
   * @return a scaled double column where the values are scaled so that the mean of the observed values
   * will be 0 and standard deviation will be 1
   */
  static List<Double> stdScaleNorm(Double[] column, int... decimals) {
    List<Double> col = column as List<Double>
    def mean = Stat.mean(col) as Double
    def stdDev = Stat.sd(col) as Double

    List<Double> vals = []
    for (def x : column) {
      vals.add(stdScaleNorm(x, mean, stdDev, decimals))
    }
    return vals
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param column the float column to scale
   * @return a scaled float column where the values are scaled so that the mean of the observed values
   * will be 0 and standard deviation will be 1
   */
  static List<Float> stdScaleNorm(Float[] column, int... decimals) {
    List<Float> col = column as List<Float>
    def stdDev = Stat.sd(col) as Float
    def mean = Stat.mean(col) as Float

    List<Float> vals = []
    for (Float x : col) {
      vals.add(stdScaleNorm(x, mean, stdDev, decimals))
    }
    return vals
  }

  /**
   * scales the distribution of data values so that the mean of the observed values
   * will be 0 and standard deviation will be 1 (a.k.a Z-score).
   * Z = ( X<sub>i</sub> - μ ) / σ
   *
   * @param column the float column to scale
   * @return a scaled float column where the values are scaled so that the mean of the observed values
   * will be 0 and standard deviation will be 1
   */
  static List<BigDecimal> stdScaleNorm(BigDecimal[] column, int... decimals) {
    List<BigDecimal> col = column as List<BigDecimal>
    def stdDev = Stat.sd(col) as BigDecimal
    def mean = Stat.mean(col) as BigDecimal

    List<BigDecimal> vals = []
    for (def x : col) {
      vals.add(stdScaleNorm(x, mean, stdDev, decimals))
    }
    return vals
  }

  /**
  * scales the distribution of data values so that the mean of the observed values
  * will be 0 and standard deviation will be 1 (a.k.a Z-score).
  * Z = ( X<sub>i</sub> - μ ) / σ
  *
  * @param column the float column to scale
  * @return a scaled float column where the values are scaled so that the mean of the observed values
  * will be 0 and standard deviation will be 1
  */
  static List<? extends Number> stdScaleNorm(List<? extends Number> column, int... decimals) {
    def stdDev = Stat.sd(column)
    def mean = Stat.mean(column)

    List<? extends Number> vals = []
    for (def x : column) {
      Class<? extends Number> cls = x.getClass()
      vals.add(stdScaleNorm(x as Number, mean, stdDev, decimals).asType(cls))
    }
    return vals
  }

  static List<? extends Number> stdScaleNorm(Matrix table, String columnName, int... decimals) {
    if (Number.isAssignableFrom(table.type(columnName))) {
      return stdScaleNorm(table[columnName] as List<? extends Number>, decimals)
    } else {
      throw new IllegalArgumentException("$columnName is not a numeric column")
    }
  }

  static Matrix stdScaleNorm(Matrix table, int... decimals) {
    List<List> columns = []
    for (Column col in table.columns()) {
      if (! Number.isAssignableFrom(col.type)) {
        columns << col
      } else {
        columns << stdScaleNorm(col, decimals)
      }
    }
    new MatrixBuilder(table.matrixName)
        .columns(columns)
        .columnNames(table.columnNames())
        .types(table.types())
        .build()
  }

}
