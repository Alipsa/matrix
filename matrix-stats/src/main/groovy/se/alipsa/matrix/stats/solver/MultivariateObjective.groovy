package se.alipsa.matrix.stats.solver

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
}
