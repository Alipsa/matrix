package se.alipsa.matrix.stats

import groovy.transform.CompileStatic

@CompileStatic
class Correlation {

  static final String PEARSON = "pearson"
  static final String SPEARMAN = "spearman"
  static final String KENDALL = "kendall"

  static BigDecimal cor(List<? extends Number> valuesX, List<? extends Number> valuesY, String method = PEARSON) {
    if (method == PEARSON) return corPearson(valuesX, valuesY)
    if (method == SPEARMAN) return corSpearman(valuesX, valuesY)
    if (method == KENDALL) return corKendall(valuesX, valuesY)
    throw new IllegalArgumentException("Unknown method: ${method}")
  }

  /**
   * measures a linear dependence between two variables (x and y) i.e. a parametric correlation test
   * as it depends on the distribution of the data.
   * @param numbersX the first list of Numbers
   * @param numbersY the second list of Numbers
   * @return a value between -1 to +1 where -1 represents X and Y are negatively correlated
   * and +1 represents X and Y are positively correlated
   * @throws IllegalArgumentException if inputs are null, empty, of different sizes, or have insufficient observations
   */
  static BigDecimal corPearson(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    validateCorrelationInputs(numbersX, numbersY, "Pearson correlation")

    BigDecimal sumX = 0
    BigDecimal sumY = 0
    BigDecimal sumXY = 0
    BigDecimal sumX2 = 0
    BigDecimal sumY2 = 0
    int size = numbersX.size()

    def final itX = numbersX.iterator()
    def final itY = numbersY.iterator()
    for (int i = 0; i < size; i++) {
      BigDecimal x = itX.next() as BigDecimal
      BigDecimal y = itY.next() as BigDecimal

      sumX += x
      sumY += y
      sumXY += x * y
      sumX2 += x * x
      sumY2 += y * y
    }

    BigDecimal bottomSquared = (size * sumX2 - sumX * sumX) * (size * sumY2 - sumY * sumY)
    if (bottomSquared == 0) return 0
    BigDecimal bottom = bottomSquared.sqrt()
    BigDecimal top = size * sumXY - sumX * sumY
    return top / bottom
  }

  /**
   * Spearman's rank correlation coefficient measures the monotonic relationship between two variables.
   * It is the Pearson correlation coefficient applied to the ranks of the data.
   * @param numbersX the first list of Numbers
   * @param numbersY the second list of Numbers
   * @return a value between -1 to +1 where -1 represents X and Y are negatively correlated
   * and +1 represents X and Y are positively correlated
   * @throws IllegalArgumentException if inputs are null, empty, of different sizes, or have insufficient observations
   */
  static BigDecimal corSpearman(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    validateCorrelationInputs(numbersX, numbersY, "Spearman correlation")

    List<BigDecimal> ranksX = rank(numbersX)
    List<BigDecimal> ranksY = rank(numbersY)
    return corPearson(ranksX, ranksY)
  }

  /**
   * Kendall Tau can be used as a metric to compare similarities between search results.
   * @param numbersX the first list of Numbers
   * @param numbersY the second list of Numbers
   * @return a value between -1 to +1 representing the correlation
   * @throws IllegalArgumentException if inputs are null, empty, of different sizes, or have insufficient observations
   */
  static BigDecimal corKendall(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    validateCorrelationInputs(numbersX, numbersY, "Kendall correlation")

    final int n = numbersX.size()
    final long numPairs = sumN(n - 1)

    BigDecimalPair[] pairs = new BigDecimalPair[n]
    for (int i = 0; i < n; i++) {
      pairs[i] = new BigDecimalPair(numbersX[i] as BigDecimal, numbersY[i] as BigDecimal)
    }

    Arrays.sort(pairs, (p1, p2) -> {
      int compareKey = compare(p1.getFirst(), p2.getFirst())
      return compareKey != 0 ? compareKey : compare(p1.getSecond(), p2.getSecond())
    })

    long tiedXPairs = 0
    long tiedXYPairs = 0
    long consecutiveXTies = 1
    long consecutiveXYTies = 1
    BigDecimalPair prev = pairs[0]
    for (int i = 1; i < n; i++) {
      final BigDecimalPair curr = pairs[i]
      if (compare(curr.getFirst(), prev.getFirst()) == 0) {
        consecutiveXTies++
        if (compare(curr.getSecond(), prev.getSecond()) == 0) {
          consecutiveXYTies++
        } else {
          tiedXYPairs += sumN(consecutiveXYTies - 1)
          consecutiveXYTies = 1
        }
      } else {
        tiedXPairs += sumN(consecutiveXTies - 1)
        consecutiveXTies = 1
        tiedXYPairs += sumN(consecutiveXYTies - 1)
        consecutiveXYTies = 1
      }
      prev = curr
    }
    tiedXPairs += sumN(consecutiveXTies - 1)
    tiedXYPairs += sumN(consecutiveXYTies - 1)

    long swaps = 0
    BigDecimalPair[] pairsDestination = new BigDecimalPair[n]
    for (int segmentSize = 1; segmentSize < n; segmentSize <<= 1) {
      for (int offset = 0; offset < n; offset += 2 * segmentSize) {
        int i = offset
        final int iEnd = Math.min(i + segmentSize, n)
        int j = iEnd
        final int jEnd = Math.min(j + segmentSize, n)

        int copyLocation = offset
        while (i < iEnd || j < jEnd) {
          if (i < iEnd) {
            if (j < jEnd) {
              if (compare(pairs[i].getSecond(), pairs[j].getSecond()) <= 0) {
                pairsDestination[copyLocation] = pairs[i]
                i++
              } else {
                pairsDestination[copyLocation] = pairs[j]
                j++
                swaps += iEnd - i
              }
            } else {
              pairsDestination[copyLocation] = pairs[i]
              i++
            }
          } else {
            pairsDestination[copyLocation] = pairs[j]
            j++
          }
          copyLocation++
        }
      }
      final BigDecimalPair[] pairsTemp = pairs
      pairs = pairsDestination
      pairsDestination = pairsTemp
    }

    long tiedYPairs = 0
    long consecutiveYTies = 1
    prev = pairs[0]
    for (int i = 1; i < n; i++) {
      final BigDecimalPair curr = pairs[i]
      if (compare(curr.getSecond(), prev.getSecond()) == 0) {
        consecutiveYTies++
      } else {
        tiedYPairs += sumN(consecutiveYTies - 1)
        consecutiveYTies = 1
      }
      prev = curr
    }
    tiedYPairs += sumN(consecutiveYTies - 1)

    final long concordantMinusDiscordant = numPairs - tiedXPairs - tiedYPairs + tiedXYPairs - 2 * swaps
    final BigDecimal nonTiedPairsMultiplied = (numPairs - tiedXPairs) * (numPairs - tiedYPairs)
    return concordantMinusDiscordant / nonTiedPairsMultiplied.sqrt()
  }

  /**
   * Ranks the values in the list using average rank for ties.
   * @param values the list of values to rank
   * @return a list of ranks (1-based)
   */
  private static List<BigDecimal> rank(List<? extends Number> values) {
    int n = values.size()

    // Create indexed pairs (value, original index)
    def indexed = values.withIndex().collect { val, idx ->
      [value: val as BigDecimal, index: idx]
    }

    // Sort by value
    indexed.sort { a, b -> a.value <=> b.value }

    // Assign ranks, handling ties with average rank
    BigDecimal[] ranks = new BigDecimal[n]
    int i = 0
    while (i < n) {
      int j = i
      // Find all tied values
      while (j < n - 1 && indexed[j].value == indexed[j + 1].value) {
        j++
      }

      // Average rank for tied values (1-based ranking)
      BigDecimal avgRank = (i + 1 + j + 1) / 2

      // Assign average rank to all tied positions
      for (int k = i; k <= j; k++) {
        ranks[indexed[k].index as int] = avgRank
      }

      i = j + 1
    }

    return ranks.toList()
  }

  private static int compare(Number x, Number y) {
    if (x == y) {
      return 0
    }
    if (x < y) {
      return -1
    }
    return 1
  }

  /**
   * Returns the sum of the number from 1 .. n according to Gauss' summation formula:
   * \[ \sum\limits_{k=1}^n k = \frac{n(n + 1)}{2} \]
   *
   * @param n the summation end
   * @return the sum of the number from 1 to n
   */
  private static long sumN(long n) {
    return (long)(n * (n + 1) / 2)
  }

  /**
   * Validates correlation inputs for null, empty, size mismatch, and insufficient observations.
   * @param x the first list
   * @param y the second list
   * @param testName the name of the test for error messages
   * @throws IllegalArgumentException if validation fails
   */
  private static void validateCorrelationInputs(List<? extends Number> x, List<? extends Number> y, String testName) {
    if (x == null || y == null) {
      throw new IllegalArgumentException("${testName} requires non-null input lists")
    }
    if (x.isEmpty() || y.isEmpty()) {
      throw new IllegalArgumentException("${testName} requires non-empty input lists")
    }
    if (x.size() != y.size()) {
      throw new IllegalArgumentException("${testName} requires lists of equal size, got: x=${x.size()}, y=${y.size()}")
    }
    if (x.size() < 2) {
      throw new IllegalArgumentException("${testName} requires at least 2 paired observations, got: ${x.size()}")
    }
  }

  /**
   * Helper data structure holding a (Number, Number) pair.
   */
  private static class BigDecimalPair {
    /** The first value */
    private final BigDecimal first
    /** The second value */
    private final BigDecimal second

    /**
     * @param first first value.
     * @param second second value.
     */
    BigDecimalPair(BigDecimal first, BigDecimal second) {
      this.first = first
      this.second = second
    }

    /** @return the first value.  */
    BigDecimal getFirst() {
      return first
    }

    /** @return the second value.  */
    BigDecimal getSecond() {
      return second
    }

  }

}
