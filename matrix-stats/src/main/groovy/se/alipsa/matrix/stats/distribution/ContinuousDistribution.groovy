package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic

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
  double cumulativeProbability(double x)
}
