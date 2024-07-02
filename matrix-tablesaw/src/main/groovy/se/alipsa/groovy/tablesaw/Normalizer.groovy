package se.alipsa.groovy.tablesaw

import se.alipsa.groovy.stats.Normalize
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.FloatColumn

/**
 * This class provides various ways to normalize a Tablesaw column.
 */
class Normalizer {

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the double column to normalize
   * @return a double column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static DoubleColumn logNorm(DoubleColumn column, int... decimals) {
    def vals = []
    for (Double x : column) {
      vals.add(Normalize.logNorm(x, decimals))
    }
    return DoubleColumn.create("norm_" + column.name(), vals);
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the Float column to normalize
   * @return a Float column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static FloatColumn logNorm(FloatColumn column, int... decimals) {
    def vals = []
    for (Float x : column) {
      vals.add(Normalize.logNorm(x, decimals))
    }
    return FloatColumn.create("norm_" + column.name(), vals as Float[]);
  }

  /**
   * Logarithmic transformations are used to normalize skewed distributions of continuous variables.
   * Taking the natural log of each observation in the distribution, forces the observations closer to the mean and
   * will thus generate a more normal distribution.
   *
   * @param column the BigDecimal column to normalize
   * @return a BigDecimal column where all values are transformed with the natural logarithm (base e) of the observations
   */
  static BigDecimalColumn logNorm(BigDecimalColumn column, int... decimals) {
    def vals = []
    for (BigDecimal x : column) {
      vals.add(Normalize.logNorm(x, decimals))
    }
    return BigDecimalColumn.create("norm_" + column.name(), vals as BigDecimal[]);
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the double column to scale
   * @return a new double column of scaled values between 0 and 1
   */
  static DoubleColumn minMaxNorm(DoubleColumn column, int... decimals) {
    def min = column.min()
    def max = column.max()

    def vals = []
    for (def x : column) {
      vals.add(Normalize.minMaxNorm(x, min, max, decimals))
    }
    return DoubleColumn.create("norm_" + column.name(), vals as Double[]);
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new float column of scaled values between 0 and 1
   */
  static FloatColumn minMaxNorm(FloatColumn column, int... decimals) {
    def min = column.min() as float
    def max = column.max() as float

    def vals = []
    for (def x : column) {
      vals.add(Normalize.minMaxNorm(x, min, max, decimals))
    }
    return FloatColumn.create("norm_" + column.name(), vals as Float[]);
  }

  /**
   * Ranges the data values to be between 0 and 1, the formula is:
   * Z<sub>i</sub> = ( X<sub>i</sub> - min(X) ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new float column of scaled values between 0 and 1
   */
  static BigDecimalColumn minMaxNorm(BigDecimalColumn column, int... decimals) {
    def min = column.min() as BigDecimal
    def max = column.max() as BigDecimal

    def vals = []
    for (def x : column) {
      vals.add(Normalize.minMaxNorm(x, min, max, decimals))
    }
    return BigDecimalColumn.create("norm_" + column.name(), vals as BigDecimal[]);
  }

 /**
  * Scales the data values to be between (–1, 1), the formula is
  * X´ = ( X - μ ) / ( max(X) - min(X) )
  *
  * @param column the double column to scale
  * @return a new, normalized double column
  */
  static DoubleColumn meanNorm(DoubleColumn column, int... decimals) {
    def min = column.min()
    def max = column.max()
    def mean = column.mean()

    def vals = []
    for (def x : column) {
      vals.add(Normalize.meanNorm(x, mean, min, max, decimals))
    }
    return DoubleColumn.create("norm_" + column.name(), vals as Double[])
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new, normalized float column
   */
  static FloatColumn meanNorm(FloatColumn column, int... decimals) {
    def min = column.min() as float
    def max = column.max() as float
    def mean = column.mean() as float

    def vals = []
    for (def x : column) {
      vals.add(Normalize.meanNorm(x, mean, min, max, decimals))
    }
    return FloatColumn.create("norm_" + column.name(), vals as Float[])
  }

  /**
   * Scales the data values to be between (–1, 1), the formula is
   * X´ = ( X - μ ) / ( max(X) - min(X) )
   *
   * @param column the float column to scale
   * @return a new, normalized float column
   */
  static BigDecimalColumn meanNorm(BigDecimalColumn column, int... decimals) {
    def min = column.min() as BigDecimal
    def max = column.max() as BigDecimal
    def mean = column.mean() as BigDecimal

    def vals = []
    for (def x : column) {
      vals.add(Normalize.meanNorm(x, mean, min, max, decimals))
    }
    return BigDecimalColumn.create("norm_" + column.name(), vals);
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
  static DoubleColumn stdScaleNorm(DoubleColumn column, int... decimals) {
    def mean = column.mean()
    def stdDev = column.standardDeviation()

    def vals = []
    for (def x : column) {
      vals.add(Normalize.stdScaleNorm(x, mean, stdDev, decimals))
    }
    return DoubleColumn.create("norm_" + column.name(), vals as Double[])
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
  static FloatColumn stdScaleNorm(FloatColumn column, int... decimals) {
    def stdDev = column.standardDeviation() as float
    def mean = column.mean() as float

    def vals = []
    for (def x : column) {
      vals.add(Normalize.stdScaleNorm(x, mean, stdDev, decimals))
    }
    return FloatColumn.create("norm_" + column.name(), vals as Float[])
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
  static BigDecimalColumn stdScaleNorm(BigDecimalColumn column, int... decimals) {
    def stdDev = column.standardDeviation() as BigDecimal
    def mean = column.mean() as BigDecimal

    def vals = []
    for (def x : column) {
      vals.add(Normalize.stdScaleNorm(x, mean, stdDev, decimals))
    }
    return BigDecimalColumn.create("norm_" + column.name(), vals as BigDecimal[])
  }
}
