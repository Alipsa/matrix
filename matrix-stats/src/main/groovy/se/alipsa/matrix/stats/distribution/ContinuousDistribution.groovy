package se.alipsa.matrix.stats.distribution

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Minimal interface for continuous distributions that expose a cumulative distribution function.
 */
@SuppressWarnings('DuplicateStringLiteral')
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
  /**
   * Computes the upper-tail probability P(X > x).
   * @param x the evaluation point
   * @return upper-tail probability
   */
  default BigDecimal survivalProbability(Number x) {
    1 - cumulativeProbability(x)
  }

  /** Primitive compatibility bridge for the upper-tail probability. */
  @Deprecated
  default double survivalProbability(double x) {
    survivalProbability(NumericConversion.toBigDecimal(x, 'x')) as double
  }
}
