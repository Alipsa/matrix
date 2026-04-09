package se.alipsa.matrix.stats.distribution

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * F-distribution (Fisher-Snedecor distribution) implementation.
 * Provides CDF and p-value calculations for ANOVA and variance ratio tests.
 *
 * <p>Uses custom high-precision implementation with no external dependencies.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
class FDistribution implements ContinuousDistribution {

  private final BigDecimal dfNumerator
  private final BigDecimal dfDenominator
  private final double dfNumeratorValue
  private final double dfDenominatorValue

  /**
   * Creates an F-distribution with the specified degrees of freedom.
   *
   * @param dfNumerator degrees of freedom for the numerator (between groups)
   * @param dfDenominator degrees of freedom for the denominator (within groups)
   */
  FDistribution(Number dfNumerator, Number dfDenominator) {
    BigDecimal normalizedDfNumerator = NumericConversion.toBigDecimal(dfNumerator, 'dfNumerator')
    BigDecimal normalizedDfDenominator = NumericConversion.toBigDecimal(dfDenominator, 'dfDenominator')
    if (normalizedDfNumerator <= 0 || normalizedDfDenominator <= 0) {
      throw new IllegalArgumentException(
          "Degrees of freedom must be positive, got: dfNumerator=$dfNumerator, dfDenominator=$dfDenominator"
      )
    }
    this.dfNumerator = normalizedDfNumerator
    this.dfDenominator = normalizedDfDenominator
    this.dfNumeratorValue = normalizedDfNumerator as double
    this.dfDenominatorValue = normalizedDfDenominator as double
  }

  BigDecimal getDfNumerator() { dfNumerator }

  BigDecimal getDfDenominator() { dfDenominator }

  /**
   * Computes the cumulative distribution function (CDF) of the F-distribution.
   * P(F <= f) where F follows an F-distribution.
   *
   * @param f the F-value (must be >= 0)
   * @return probability P(F <= f)
   */
  BigDecimal cdf(Number f) {
    BigDecimal.valueOf(cdfValue(NumericConversion.toFiniteDouble(f, 'f')))
  }

  @Deprecated
  double cdf(double f) {
    cdfValue(f)
  }

  private double cdfValue(double f) {
    if (f < 0) {
      throw new IllegalArgumentException("f must be non-negative, got: $f")
    }
    if (f == 0) {
      return 0.0
    }

    // F-distribution CDF in terms of incomplete beta function
    // F ~ (dfNum/dfDen) * (B/A) where A,B are chi-squared
    // CDF = I_x(dfNum/2, dfDen/2) where x = (dfNum*f)/(dfNum*f + dfDen)
    double x = (dfNumeratorValue * f) / (dfNumeratorValue * f + dfDenominatorValue)
    SpecialFunctions.regularizedIncompleteBeta(x, dfNumeratorValue / 2.0d, dfDenominatorValue / 2.0d)
  }

  /**
   * Alias matching the commons-math3 distribution API.
   *
   * @param f the F-value (must be >= 0)
   * @return probability P(F <= f)
   */
  BigDecimal cumulativeProbability(Number f) {
    cdf(f)
  }

  @Override
  @Deprecated
  double cumulativeProbability(double f) {
    cdfValue(f)
  }

  /**
   * Computes the p-value (upper tail probability) for an F-statistic.
   * This is P(F > f) = 1 - CDF(f)
   *
   * @param f the F-statistic
   * @return p-value (probability of observing a value at least as extreme)
   */
  BigDecimal pValue(Number f) {
    1.0 - cdf(f)
  }

  @Deprecated
  double pValue(double f) {
    1.0d - cdfValue(f)
  }

  /**
   * Static convenience method for computing F-test p-value.
   *
   * @param f the F-statistic
   * @param dfNumerator degrees of freedom for the numerator
   * @param dfDenominator degrees of freedom for the denominator
   * @return p-value
   */
  static BigDecimal pValue(Number f, Number dfNumerator, Number dfDenominator) {
    new FDistribution(dfNumerator, dfDenominator).pValue(f)
  }

  @Deprecated
  static double pValue(double f, double dfNumerator, double dfDenominator) {
    new FDistribution(dfNumerator, dfDenominator).pValue(f) as double
  }

  /**
   * Computes the one-way ANOVA F-statistic.
   *
   * @param groups list of double arrays, each representing a group
   * @return the F-statistic
   */
  /** @deprecated Use {@link #oneWayAnovaFValueArrays} or {@link #oneWayAnovaFValueFromLists} instead. */
  @Deprecated
  static BigDecimal oneWayAnovaFValue(List<?> groups) {
    NumericConversion.<BigDecimal> dispatchPrimitiveOrList(
      groups,
      { List<?> g -> oneWayAnovaFValueArrays(g as List<double[]>) },
      { List<?> g -> oneWayAnovaFValueFromLists(g as Collection<? extends List<? extends Number>>) },
      'group'
    )
  }

  /**
   * Computes the one-way ANOVA F-statistic from primitive group arrays.
   *
   * @param groups list of primitive group arrays
   * @return the F-statistic
   */
  static BigDecimal oneWayAnovaFValueArrays(List<double[]> groups) {
    BigDecimal.valueOf(oneWayAnovaFValueInternal(copyPrimitiveGroups(groups)))
  }

  /**
   * Computes the one-way ANOVA F-statistic from idiomatic Groovy numeric lists.
   *
   * @param groups collection of numeric group values
   * @return the F-statistic
   */
  static BigDecimal oneWayAnovaFValueFromLists(Collection<? extends List<? extends Number>> groups) {
    BigDecimal.valueOf(oneWayAnovaFValueInternal(normalizeListGroups(groups)))
  }

  private static double oneWayAnovaFValueInternal(List<double[]> groups) {
    int k = groups.size()  // number of groups
    double grandSum = 0.0
    List<Integer> groupSizes = groups.collect { double[] group -> group.length }
    int n = groupSizes.sum() as int  // total observations
    double[] groupMeans = new double[k]
    groups.eachWithIndex { double[] group, int i ->
      double sum = 0.0
      group.each { double v ->
        sum += v
        grandSum += v
      }
      groupMeans[i] = sum / group.length
    }

    double grandMean = grandSum / n

    // Sum of squares between groups (SSB)
    double ssb = (0..<k).sum { int i ->
      double diff = groupMeans[i] - grandMean
      (groupSizes[i] * diff * diff) as double
    } as double

    // Sum of squares within groups (SSW)
    double ssw = (0..<k).sum { int i ->
      double[] group = groups[i]
      double mean = groupMeans[i]
      group.sum { double v ->
        double diff = v - mean
        (diff * diff) as double
      }
    } as double

    // Degrees of freedom
    double dfBetween = k - 1
    double dfWithin = n - k

    // Mean squares
    double msb = ssb / dfBetween
    double msw = ssw / dfWithin

    // F-statistic
    msb / msw
  }

  /**
   * Computes the one-way ANOVA p-value.
   *
   * @param groups list of double arrays, each representing a group
   * @return the p-value for the ANOVA test
   */
  /** @deprecated Use {@link #oneWayAnovaPValueArrays} or {@link #oneWayAnovaPValueFromLists} instead. */
  @Deprecated
  static BigDecimal oneWayAnovaPValue(List<?> groups) {
    NumericConversion.<BigDecimal> dispatchPrimitiveOrList(
      groups,
      { List<?> g -> oneWayAnovaPValueArrays(g as List<double[]>) },
      { List<?> g -> oneWayAnovaPValueFromLists(g as Collection<? extends List<? extends Number>>) },
      'group'
    )
  }

  /**
   * Computes the one-way ANOVA p-value from primitive group arrays.
   *
   * @param groups list of primitive group arrays
   * @return the p-value for the ANOVA test
   */
  static BigDecimal oneWayAnovaPValueArrays(List<double[]> groups) {
    List<double[]> normalizedGroups = copyPrimitiveGroups(groups)
    int k = normalizedGroups.size()
    int n = normalizedGroups.sum { double[] group -> group.length } as int

    BigDecimal f = oneWayAnovaFValueArrays(normalizedGroups)
    BigDecimal dfBetween = k - 1
    BigDecimal dfWithin = n - k

    pValue(f, dfBetween, dfWithin)
  }

  /**
   * Computes the one-way ANOVA p-value from idiomatic Groovy numeric lists.
   *
   * @param groups collection of numeric group values
   * @return the p-value for the ANOVA test
   */
  static BigDecimal oneWayAnovaPValueFromLists(Collection<? extends List<? extends Number>> groups) {
    List<double[]> normalizedGroups = normalizeListGroups(groups)
    int k = normalizedGroups.size()
    int n = normalizedGroups.sum { double[] group -> group.length } as int

    BigDecimal f = BigDecimal.valueOf(oneWayAnovaFValueInternal(normalizedGroups))
    BigDecimal dfBetween = k - 1
    BigDecimal dfWithin = n - k

    pValue(f, dfBetween, dfWithin)
  }

  private static List<double[]> copyPrimitiveGroups(List<double[]> groups) {
    if (groups == null) {
      return []
    }
    List<double[]> normalized = []
    for (int i = 0; i < groups.size(); i++) {
      double[] group = groups[i]
      if (group == null) {
        throw new IllegalArgumentException("Group at index ${i} cannot be null")
      }
      normalized << (group.clone() as double[])
    }
    normalized
  }

  private static List<double[]> normalizeListGroups(Collection<? extends List<? extends Number>> groups) {
    if (groups == null) {
      return []
    }
    groups.collect { List<? extends Number> group ->
      NumericConversion.toDoubleArray(group, 'group')
    }
  }
}
