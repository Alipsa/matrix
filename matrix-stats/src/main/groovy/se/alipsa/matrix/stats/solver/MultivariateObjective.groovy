package se.alipsa.matrix.stats.solver

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Objective function for multivariate derivative-free optimization.
 *
 * <p>This low-level solver boundary intentionally uses primitive double arrays to avoid
 * boxing and to preserve parity with the native optimization kernels.</p>
 */
interface MultivariateObjective {

  /**
   * Evaluates the objective at the supplied point.
   *
   * @param point current parameter vector
   * @return objective value
   */
  double value(double[] point)

  /**
   * Evaluates the objective at the supplied point using a Groovy-facing numeric vector.
   *
   * @param point current parameter vector
   * @return objective value
   */
  default double value(List<? extends Number> point) {
    value(NumericConversion.toDoubleArray(point, 'point'))
  }
}
