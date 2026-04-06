package se.alipsa.matrix.stats.solver

/**
 * Objective function for one-dimensional root-finding.
 *
 * <p>This low-level solver boundary intentionally uses primitive doubles to avoid
 * boxing and to match the native numeric kernels used by the concrete solvers.</p>
 */
interface UnivariateObjective {

  /**
   * Evaluates the function at the given point.
   *
   * @param x point to evaluate
   * @return objective value at {@code x}
   */
  double value(double x)
}
