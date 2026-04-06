package se.alipsa.matrix.stats.distribution

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Minimal interface for continuous distributions that expose a cumulative distribution function.
 */
interface ContinuousDistribution {

  /**
   * Computes the cumulative distribution function P(X <= x).
   *
   * @param x the evaluation point
   * @return cumulative probability at {@code x}
   */
  BigDecimal cumulativeProbability(Number x)

  /**
   * Primitive compatibility bridge retained during the idiomatic API transition.
   *
   * @param x the evaluation point
   * @return cumulative probability at {@code x}
   * @deprecated Prefer {@link #cumulativeProbability(Number)}
   */
  @Deprecated
  default double cumulativeProbability(double x) {
    cumulativeProbability(NumericConversion.toBigDecimal(x, 'x')) as double
  }
}
