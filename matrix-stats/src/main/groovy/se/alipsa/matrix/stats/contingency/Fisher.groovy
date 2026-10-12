package se.alipsa.matrix.stats.contingency

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.distribution.HypergeometricDistribution
import se.alipsa.matrix.stats.distribution.SpecialFunctions
import se.alipsa.matrix.stats.solver.BrentSolver
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Fisher's exact test is a statistical significance test used in the analysis of contingency tables.
 * Although in practice it is employed when sample sizes are small, it is valid for all sample sizes.
 * It is named after its inventor, Ronald Fisher, and is one of a class of exact tests,
 * so called because the significance of the deviation from a null hypothesis (e.g., p-value) can be calculated exactly,
 * rather than relying on an approximation that becomes exact in the limit as the sample size grows to infinity,
 * as with many other statistical tests.
 *
 * <p>This implementation uses the hypergeometric distribution to calculate exact p-values for 2×2 contingency tables.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test data: [[12, 5], [9, 11]]
 * def result = Fisher.test([[12, 5], [9, 11]])
 * println "p-value: ${result.pValue}"
 * println "odds ratio: ${result.oddsRatio}"
 * </pre>
 */
@SuppressWarnings('DuplicateNumberLiteral')
class Fisher {

  private static final String ALTERNATIVE_NULL_MESSAGE = 'Alternative must not be null'

  /**
   * Alternative hypothesis options for Fisher's exact test.
   */
  enum Alternative {
    TWO_SIDED('two.sided'),
    GREATER('greater'),
    LESS('less')

    final String label

    private Alternative(String label) {
      this.label = label
    }
  }

  /**
   * Performs Fisher's exact test on a 2×2 contingency table.
   *
   * @param table A 2×2 contingency table as a List of Lists: [[a, b], [c, d]]
   * @param confidenceLevel confidence level (default 0.95), strictly between zero and one
   * @param alternative The alternative hypothesis: "two.sided" (default), "greater", or "less"
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(List<List<Number>> table, String alternative = 'two.sided', Number confidenceLevel = 0.95) {
    validateTable(table)

    int a = table[0][0].intValue()
    int b = table[0][1].intValue()
    int c = table[1][0].intValue()
    int d = table[1][1].intValue()

    calculateFisherTest(a, b, c, d, alternative, confidenceLevel.doubleValue())
  }

  /**
   * Performs Fisher's exact test on a 2×2 contingency table.
   *
   * @param table A 2×2 contingency table as a List of Lists: [[a, b], [c, d]]
   * @param confidenceLevel confidence level (default 0.95), strictly between zero and one
   * @param alternative The alternative hypothesis
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(List<List<Number>> table, Alternative alternative, Number confidenceLevel = 0.95) {
    if (alternative == null) {
      throw new IllegalArgumentException(ALTERNATIVE_NULL_MESSAGE)
    }
    test(table, alternative.label, confidenceLevel)
  }

  /**
   * Performs Fisher's exact test on a Matrix (2×2 contingency table).
   *
   * @param table A 2×2 Matrix
   * @param confidenceLevel confidence level (default 0.95), strictly between zero and one
   * @param alternative The alternative hypothesis: "two.sided" (default), "greater", or "less"
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(Matrix table, String alternative = 'two.sided', Number confidenceLevel = 0.95) {
    if (table.rowCount() != 2 || table.columnCount() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table.rowCount()}×${table.columnCount()})")
    }

    test([[table.get(0, 0) as Number, table.get(0, 1) as Number],
          [table.get(1, 0) as Number, table.get(1, 1) as Number]], alternative, confidenceLevel)
  }

  /**
   * Performs Fisher's exact test on a Matrix (2×2 contingency table).
   *
   * @param table A 2×2 Matrix
   * @param confidenceLevel confidence level (default 0.95), strictly between zero and one
   * @param alternative The alternative hypothesis
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(Matrix table, Alternative alternative, Number confidenceLevel = 0.95) {
    if (alternative == null) {
      throw new IllegalArgumentException(ALTERNATIVE_NULL_MESSAGE)
    }
    test(table, alternative.label, confidenceLevel)
  }

  private static void validateTable(List<List<Number>> table) {
    if (table == null || table.size() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table?.size() ?: 0} rows)")
    }
    if (table[0].size() != 2 || table[1].size() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table[0]?.size()}×${table[1]?.size()} columns)")
    }
    for (List<Number> row : table) {
      for (Number val : row) {
        if (val == null || val < 0 || !Double.isFinite(val.doubleValue()) ||
            val.doubleValue() != val.intValue()) {
          throw new IllegalArgumentException('Table values must be non-negative integers')
        }
      }
    }
  }

  private static FisherResult calculateFisherTest(int a, int b, int c, int d, String alternative, double confidenceLevel) {
    if (!Double.isFinite(confidenceLevel) || confidenceLevel <= 0 || confidenceLevel >= 1) {
      throw new IllegalArgumentException('confidenceLevel must be between 0 and 1')
    }
    int n = a + b + c + d
    int rowSum1 = a + b
    int colSum1 = a + c

    // Calculate odds ratio
    double sampleOddsRatio = calculateOddsRatio(a, b, c, d)

    // Calculate p-value using hypergeometric distribution
    // The hypergeometric distribution models the probability of 'a' successes
    // in a sample of size rowSum1, drawn from a population of size n
    // with colSum1 successes
    HypergeometricDistribution dist = new HypergeometricDistribution(n, colSum1, rowSum1)

    String normalizedAlternative = alternative == null ? '' : alternative.toLowerCase(Locale.ROOT)
    double pValue = switch (normalizedAlternative) {
      case 'greater' -> dist.upperCumulativeProbability(a)       // P(X >= a)
      case 'less' -> dist.cumulativeProbability(a)               // P(X <= a)
      case 'two.sided' -> calculateTwoSidedPValue(dist, a)       // two-sided
      default -> throw new IllegalArgumentException("Alternative must be 'two.sided', 'greater', or 'less', got: ${alternative}")
    }

    // Calculate confidence interval for odds ratio (95% by default)
    int lo = Math.max(0, rowSum1 - (n - colSum1))
    int hi = Math.min(rowSum1, colSum1)
    double[] logWeights = (lo..hi).collect { int k ->
      logChoose(colSum1, k) + logChoose(n - colSum1, rowSum1 - k)
    } as double[]
    double oddsRatio = a == lo ? 0.0d : a == hi ? Double.POSITIVE_INFINITY :
        solveOdds { double u -> noncentralMoment(logWeights, lo, a, u, 0) - a }
    double alpha = (1 - confidenceLevel) / (normalizedAlternative == 'two.sided' ? 2 : 1)
    double lower = a == lo || normalizedAlternative == 'less' ? 0.0d :
        solveOdds { double u -> noncentralMoment(logWeights, lo, a, u, 1) - alpha }
    double upper = a == hi || normalizedAlternative == 'greater' ? Double.POSITIVE_INFINITY :
        solveOdds { double u -> noncentralMoment(logWeights, lo, a, u, -1) - alpha }
    double[] confInt = [lower, upper] as double[]

    new FisherResult(
      pValue: BigDecimal.valueOf(pValue),
      oddsRatio: finiteDecimal(oddsRatio),
      sampleOddsRatio: BigDecimal.valueOf(sampleOddsRatio),
      confidenceLevel: confidenceLevel,
      confidenceInterval: confInt.collect { double value -> finiteDecimal(value) },
      alternative: alternative
    )
  }

  private static double calculateOddsRatio(int a, int b, int c, int d) {
    if (b == 0 || c == 0) {
      // Avoid division by zero - add 0.5 to all cells (Haldane-Anscombe correction)
      return ((a + 0.5) * (d + 0.5)) / ((b + 0.5) * (c + 0.5))
    }
    productRatio(a, d, b, c)
  }

  private static double productRatio(int numeratorLeft, int numeratorRight, int denominatorLeft, int denominatorRight) {
    long numerator = (numeratorLeft as long) * (numeratorRight as long)
    long denominator = (denominatorLeft as long) * (denominatorRight as long)
    (numerator as double) / (denominator as double)
  }

  private static double calculateTwoSidedPValue(HypergeometricDistribution dist, int observed) {
    double observedProb = dist.probability(observed)
    double pValue = 0.0

    // Sum probabilities for all tables with probability <= observed probability
    for (int k = dist.getSupportLowerBound(); k <= dist.getSupportUpperBound(); k++) {
      double prob = dist.probability(k)
      if (prob <= observedProb * (1 + 1e-7)) {  // Small epsilon for floating point comparison
        pValue += prob
      }
    }

    Math.min(pValue, 1.0)  // Ensure p-value doesn't exceed 1 due to rounding
  }

  private static Number finiteDecimal(double value) {
    Double.isFinite(value) ? BigDecimal.valueOf(value) : value
  }

  private static double logChoose(int n, int k) {
    SpecialFunctions.logGamma(n + 1.0d) - SpecialFunctions.logGamma(k + 1.0d) - SpecialFunctions.logGamma(n - k + 1.0d)
  }

  private static double solveOdds(Closure<Double> equation) {
    Math.exp(BrentSolver.solve({ double u -> equation.call(u) }, -700.0d, 700.0d, 1e-12, 1e-12, 200).root)
  }

  // Normalise in log space to avoid overflow at the endpoints of the root bracket.
  private static double noncentralMoment(double[] weights, int lo, int observed, double logOdds, int tail) {
    double maximum = Double.NEGATIVE_INFINITY
    for (int i = 0; i < weights.length; i++) {
      maximum = Math.max(maximum, weights[i] + (lo + i) * logOdds)
    }
    double total = 0
    double selected = 0
    for (int i = 0; i < weights.length; i++) {
      int k = lo + i
      double probability = Math.exp(weights[i] + k * logOdds - maximum)
      total += probability
      if (tail == 0) {
        selected += k * probability
      } else if (tail > 0 ? k >= observed : k <= observed) {
        selected += probability
      }
    }
    selected / total
  }

  /**
   * Result class for Fisher's exact test.
   */
  static class FisherResult {
    /** The p-value of the test */
    BigDecimal pValue

    /** The estimated odds ratio */
    Number oddsRatio

    /** Exact confidence interval for the odds ratio [lower, upper]. */
    List<Number> confidenceInterval

    /** Sample cross-product odds ratio, with a half-cell correction for zero denominators. */
    BigDecimal sampleOddsRatio

    /** Confidence level, default 0.95. Infinite endpoints are Double.POSITIVE_INFINITY. */
    Number confidenceLevel

    /** The alternative hypothesis used */
    String alternative

    /**
     * Evaluates whether to reject the null hypothesis at the given significance level.
     *
     * @param alpha Significance level (default 0.05)
     * @return true if null hypothesis should be rejected (p-value < alpha)
     */
    boolean evaluate(Number alpha = 0.05) {
      pValue < NumericConversion.toAlpha(alpha)
    }

    @Override
    String toString() {
      BigDecimal percent = (confidenceLevel as BigDecimal) * 100G
      """Fisher's Exact Test Result:
  p-value: ${pValue}
  odds ratio: ${oddsRatio}
  ${confidenceInterval ? sprintf('%s%% CI: [%.4f, %.4f]', percent.stripTrailingZeros().toPlainString(), confidenceInterval[0], confidenceInterval[1]) : 'CI: N/A'}
  alternative: ${alternative}"""
    }
  }
}
