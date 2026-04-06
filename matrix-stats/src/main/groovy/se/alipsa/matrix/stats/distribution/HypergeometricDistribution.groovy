package se.alipsa.matrix.stats.distribution

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Hypergeometric distribution implementation for exact contingency-table calculations.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class HypergeometricDistribution {

  private final int populationSize
  private final int numberOfSuccesses
  private final int sampleSize
  private final int supportLowerBound
  private final int supportUpperBound

  HypergeometricDistribution(int populationSize, int numberOfSuccesses, int sampleSize) {
    if (populationSize < 0) {
      throw new IllegalArgumentException("Population size must be non-negative, got: $populationSize")
    }
    if (numberOfSuccesses < 0 || numberOfSuccesses > populationSize) {
      throw new IllegalArgumentException(
          "Number of successes must be between 0 and population size, got: $numberOfSuccesses"
      )
    }
    if (sampleSize < 0 || sampleSize > populationSize) {
      throw new IllegalArgumentException("Sample size must be between 0 and population size, got: $sampleSize")
    }

    this.populationSize = populationSize
    this.numberOfSuccesses = numberOfSuccesses
    this.sampleSize = sampleSize
    this.supportLowerBound = Math.max(0, sampleSize - (populationSize - numberOfSuccesses))
    this.supportUpperBound = Math.min(sampleSize, numberOfSuccesses)
  }

  int getSupportLowerBound() {
    supportLowerBound
  }

  int getSupportUpperBound() {
    supportUpperBound
  }

  BigDecimal probability(Number x) {
    BigDecimal.valueOf(probability(NumericConversion.toExactInt(x, 'x')))
  }

  @Deprecated
  double probability(int x) {
    if (x < supportLowerBound || x > supportUpperBound) {
      return 0.0d
    }

    double logProbability = logCombination(numberOfSuccesses, x) +
        logCombination(populationSize - numberOfSuccesses, sampleSize - x) -
        logCombination(populationSize, sampleSize)

    return Math.exp(logProbability)
  }

  BigDecimal cumulativeProbability(Number x) {
    BigDecimal.valueOf(cumulativeProbability(NumericConversion.toExactInt(x, 'x')))
  }

  @Deprecated
  double cumulativeProbability(int x) {
    if (x < supportLowerBound) {
      return 0.0d
    }
    if (x >= supportUpperBound) {
      return 1.0d
    }

    double sum = 0.0d
    for (int k = supportLowerBound; k <= x; k++) {
      sum += probability(k)
    }
    return Math.min(1.0d, sum)
  }

  BigDecimal upperCumulativeProbability(Number x) {
    BigDecimal.valueOf(upperCumulativeProbability(NumericConversion.toExactInt(x, 'x')))
  }

  @Deprecated
  double upperCumulativeProbability(int x) {
    if (x <= supportLowerBound) {
      return 1.0d
    }
    if (x > supportUpperBound) {
      return 0.0d
    }

    double sum = 0.0d
    for (int k = x; k <= supportUpperBound; k++) {
      sum += probability(k)
    }
    return Math.min(1.0d, sum)
  }

  private static double logCombination(int n, int k) {
    if (k < 0 || k > n) {
      return Double.NEGATIVE_INFINITY
    }
    return SpecialFunctions.logGamma(n + 1.0d) -
        SpecialFunctions.logGamma(k + 1.0d) -
        SpecialFunctions.logGamma(n - k + 1.0d)
  }
}
