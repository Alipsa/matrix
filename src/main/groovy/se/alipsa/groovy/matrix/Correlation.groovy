package se.alipsa.groovy.matrix

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
   * @param numbers1 the first list of Numbers
   * @param numbers2 the second list of Numbers
   * @return a value between -1 to +1 where -1 represents X and Y are negatively correlated
   * and +1 represents X and Y are positively correlated
   */
  static BigDecimal corPearson(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    def sumX = 0.0
    def sumY = 0.0
    def sumXY = 0.0
    def sumX2 = 0.0
    def sumY2 = 0.0
    def size = numbersX.size()

    def final itX = numbersX.iterator()
    def final itY = numbersY.iterator()
    for (int i = 0; i < size; i++) {
      def x = itX.next()
      def y = itY.next()

      sumX += x
      sumY += y
      sumXY += x * y
      sumX2 += x * x
      sumY2 += y * y
    }

    def final bottom = Math.sqrt((size * sumX2 - sumX * sumX) * (size * sumY2 - sumY * sumY))
    if (bottom == 0) return 0
    def final top = size * sumXY - sumX * sumY
    return top / bottom
  }

  static BigDecimal corSpearman(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    int n = numbersX.size()
    def sum_X = 0.0, sum_Y = 0.0, sum_XY = 0.0, squareSum_X = 0.0, squareSum_Y = 0.0
    Number x, y
    for (int i = 0; i < n; i++) {
      x = numbersX.get(i)
      y = numbersY.get(i)

      sum_X = sum_X + x
      sum_Y = sum_Y + y
      sum_XY = sum_XY + x * y

      // sum of square of elements.
      squareSum_X = squareSum_X + x**2
      squareSum_Y = squareSum_Y + y**2
    }

    return (n * sum_XY - sum_X * sum_Y) / Math.sqrt(
        (n * squareSum_X - sum_X * sum_X)
            * (n * squareSum_Y - sum_Y * sum_Y))
  }

  /**
   * Kendall Tau can be used as a metric to compare similarities between search results.
   */
  static BigDecimal corKendall(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    if (numbersX.size() != numbersY.size()) {
      throw new IllegalArgumentException("Lists must be of equal size!")
    }

    final int n = numbersX.size()
    final long numPairs = sumN(n - 1)

    BigDecimalPair[] pairs = new BigDecimalPair[n]
    for (int i = 0; i < n; i++) {
      pairs[i] = new BigDecimalPair(numbersX[i], numbersY[i])
    }

    Arrays.sort(pairs, (p1, p2) -> {
      int compareKey = compare(p1.getFirst(), p2.getFirst())
      return compareKey != 0 ? compareKey : compare(p1.getSecond(), p2.getSecond())
    });

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
    final double nonTiedPairsMultiplied = (numPairs - tiedXPairs) * (double) (numPairs - tiedYPairs)
    return concordantMinusDiscordant / Math.sqrt(nonTiedPairsMultiplied)
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
    return n * (n + 1) / 2l;
  }

  /**
   * Helper data structure holding a (Number, Number) pair.
   */
  private static class BigDecimalPair {
    /** The first value */
    private final BigDecimal first;
    /** The second value */
    private final BigDecimal second;

    /**
     * @param first first value.
     * @param second second value.
     */
    BigDecimalPair(BigDecimal first, BigDecimal second) {
      this.first = first;
      this.second = second;
    }

    /** @return the first value.  */
    BigDecimal getFirst() {
      return first;
    }

    /** @return the second value.  */
    BigDecimal getSecond() {
      return second;
    }

  }

}
