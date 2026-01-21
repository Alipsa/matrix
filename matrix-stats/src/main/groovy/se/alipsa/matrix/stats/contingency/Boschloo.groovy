package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic

/**
 * Boschloo's test is a uniformly more powerful alternative to Fisher's exact test for 2×2 contingency tables.
 * Like Barnard's test, it only conditions on the total sample size rather than both margins,
 * making it more powerful when margins are not fixed by design.
 *
 * Boschloo's test uses Fisher's exact test p-value as the ordering statistic. For each possible value
 * of the nuisance parameter π (the common success probability under the null hypothesis), it computes
 * the probability of obtaining a table with a Fisher p-value as extreme or more extreme than the observed
 * table. The final p-value is the maximum over all values of π.
 *
 * Example:
 * <pre>
 * // Test association in a 2×2 table
 * int[][] table = [[12, 8],    // Row 1: Success, Failure
 *                  [6, 14]]    // Row 2: Success, Failure
 *
 * def result = Boschloo.test(table)
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Boschloo, R. D. (1970). "Raised conditional level of significance for the 2 × 2-table when testing the equality of two probabilities"
 * - Lydersen, S., Fagerland, M. W., & Laake, P. (2009). "Recommended tests for association in 2×2 tables"
 *
 * Note: Boschloo's test is computationally intensive for large sample sizes.
 */
@CompileStatic
class Boschloo {

  /**
   * Performs Boschloo's exact test for a 2×2 contingency table.
   *
   * @param table A 2×2 table represented as int[2][2]: [[a, b], [c, d]]
   * @return BoschlooResult containing p-value and nuisance parameter
   */
  static BoschlooResult test(int[][] table) {
    validateTable(table)

    int a = table[0][0]
    int b = table[0][1]
    int c = table[1][0]
    int d = table[1][1]

    int n1 = a + b  // Row 1 total
    int n2 = c + d  // Row 2 total
    int n = n1 + n2  // Grand total

    if (n == 0) {
      throw new IllegalArgumentException("Table cannot have zero total count")
    }

    // Calculate Fisher's exact p-value for the observed table
    double observedFisherP = calculateFisherPValue(a, b, c, d)

    // Find the nuisance parameter π that maximizes the p-value
    int gridSize = Math.min(100, n + 1)
    double maxPValue = 0.0
    double optimalPi = 0.5

    for (int i = 0; i <= gridSize; i++) {
      double pi = i / (double) gridSize

      // Calculate Boschloo p-value for this π
      double pValue = calculateBoschlooForPi(n1, n2, observedFisherP, pi)

      if (pValue > maxPValue) {
        maxPValue = pValue
        optimalPi = pi
      }
    }

    // Clamp p-value to [0, 1] to handle numerical precision issues
    maxPValue = Math.max(0.0, Math.min(1.0, maxPValue))

    return new BoschlooResult(
      pValue: maxPValue,
      fisherPValue: observedFisherP,
      nuisanceParameter: optimalPi,
      sampleSize: n
    )
  }

  /**
   * Calculate Boschloo's p-value for a given nuisance parameter π.
   * This is the probability of obtaining a table with Fisher p-value ≤ observed Fisher p-value.
   */
  private static double calculateBoschlooForPi(int n1, int n2, double observedFisherP, double pi) {
    double pValue = 0.0

    // Enumerate all possible values of x1 and x2
    for (int x1 = 0; x1 <= n1; x1++) {
      int y1 = n1 - x1

      for (int x2 = 0; x2 <= n2; x2++) {
        int y2 = n2 - x2

        // Calculate Fisher's exact p-value for this table
        double fisherP = calculateFisherPValue(x1, y1, x2, y2)

        // If this table is as or more extreme (by Fisher's criterion), include its probability
        if (fisherP <= observedFisherP + 1e-10) {
          double prob = binomialProbability(n1, x1, pi) * binomialProbability(n2, x2, pi)
          pValue += prob
        }
      }
    }

    return pValue
  }

  /**
   * Calculate Fisher's exact test p-value for a 2×2 table using the hypergeometric distribution.
   * This is a two-sided p-value.
   */
  private static double calculateFisherPValue(int a, int b, int c, int d) {
    int n1 = a + b
    int n2 = c + d
    int k1 = a + c
    int k2 = b + d
    int n = n1 + n2

    if (n == 0) {
      return 1.0
    }

    // Calculate probability of the observed table
    double observedProb = hypergeometricProbability(a, n1, n2, k1)

    // Sum probabilities of all tables as or more extreme
    double pValue = 0.0

    for (int x = 0; x <= Math.min(n1, k1); x++) {
      int y = n1 - x
      int z = k1 - x
      int w = n2 - z

      // Check if this table is valid
      if (z >= 0 && z <= n2 && w >= 0 && w <= k2) {
        double prob = hypergeometricProbability(x, n1, n2, k1)

        // Include if probability is less than or equal to observed (two-sided test)
        if (prob <= observedProb + 1e-10) {
          pValue += prob
        }
      }
    }

    // Clamp p-value to [0, 1] to handle numerical precision issues
    return Math.max(0.0, Math.min(1.0, pValue))
  }

  /**
   * Calculate hypergeometric probability: P(X = k) where X ~ Hypergeometric(N, K, n)
   * This is the probability of drawing k successes in n draws from a population of size N
   * containing K successes.
   */
  private static double hypergeometricProbability(int k, int n, int m, int t) {
    // P(X = k) = C(t, k) * C(N-t, n-k) / C(N, n)
    // where N = n + m (total), t = column total, n = row 1 total
    int N = n + m

    if (k < 0 || k > n || k > t || (n - k) > (N - t)) {
      return 0.0
    }

    // Use log probabilities for numerical stability
    double logProb = logBinomialCoefficient(t, k) +
                     logBinomialCoefficient(N - t, n - k) -
                     logBinomialCoefficient(N, n)

    return Math.exp(logProb)
  }

  /**
   * Calculate binomial probability: C(n,k) * p^k * (1-p)^(n-k)
   */
  private static double binomialProbability(int n, int k, double p) {
    if (k < 0 || k > n) {
      return 0.0
    }

    if (p <= 0.0) {
      return k == 0 ? 1.0 : 0.0
    }

    if (p >= 1.0) {
      return k == n ? 1.0 : 0.0
    }

    // Use log probabilities for numerical stability
    double logProb = logBinomialCoefficient(n, k) + k * Math.log(p) + (n - k) * Math.log(1 - p)

    return Math.exp(logProb)
  }

  /**
   * Calculate log of binomial coefficient: log(C(n,k))
   */
  private static double logBinomialCoefficient(int n, int k) {
    if (k < 0 || k > n) {
      return Double.NEGATIVE_INFINITY
    }

    if (k == 0 || k == n) {
      return 0.0
    }

    // Use symmetry: C(n,k) = C(n, n-k)
    if (k > n - k) {
      k = n - k
    }

    double result = 0.0
    for (int i = 1; i <= k; i++) {
      result += Math.log(n - k + i) - Math.log(i)
    }

    return result
  }

  private static void validateTable(int[][] table) {
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null")
    }

    if (table.length != 2) {
      throw new IllegalArgumentException("Table must be 2×2 (got ${table.length} rows)")
    }

    for (int row = 0; row < 2; row++) {
      if (table[row] == null) {
        throw new IllegalArgumentException("Row ${row} is null")
      }
      if (table[row].length != 2) {
        throw new IllegalArgumentException("Row ${row} must have 2 columns (got ${table[row].length})")
      }

      for (int col = 0; col < 2; col++) {
        if (table[row][col] < 0) {
          throw new IllegalArgumentException("Cell [${row}][${col}] has negative value: ${table[row][col]}")
        }
      }
    }
  }

  /**
   * Result class for Boschloo's test.
   */
  @CompileStatic
  static class BoschlooResult {
    /** The p-value (two-sided) */
    double pValue

    /** Fisher's exact p-value for the observed table */
    double fisherPValue

    /** The nuisance parameter that maximizes p-value */
    double nuisanceParameter

    /** The total sample size */
    int sampleSize

    /**
     * Interprets the Boschloo test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant association
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Significant association detected (p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No significant association detected (p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String significance = pValue < alpha ? "significant" : "not significant"

      return String.format(
        "Boschloo's exact test:\\n" +
        "p-value: %.4f\\n" +
        "Fisher's p-value: %.4f\\n" +
        "Nuisance parameter (π): %.4f\\n" +
        "Sample size: %d\\n" +
        "Conclusion: Association is %s at %.0f%% significance level",
        pValue, fisherPValue, nuisanceParameter, sampleSize, significance, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Boschloo's Exact Test
  Sample size: ${sampleSize}
  p-value: ${String.format('%.4f', pValue)}
  Fisher's p-value: ${String.format('%.4f', fisherPValue)}
  Nuisance parameter (π): ${String.format('%.4f', nuisanceParameter)}

  ${interpret()}"""
    }
  }
}
