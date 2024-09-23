package se.alipsa.groovy.stats.solver

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.AllowedSolution
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
import org.apache.commons.math3.analysis.solvers.UnivariateSolver


/**
Similar to the excel goal seek function.
 */
class GoalSeek {

  /**
   * Provides similar functionality to the excel/calc goal seek function.
   * It uses an extension of the Brent-Dekker algorithm which uses inverse nth order polynomial
   * interpolation instead of inverse quadratic interpolation, and which allows selection
   * of the side of the convergence interval for result bracketing.
   * Example usage
   * <pre><code>
   * def val = GoalSeek.solve(27000, 0, 100) {
   *   it * 100_000
   * }
   * println "Found the value ${val.value} after ${val.iterations} iterations"
   * assert 0.270 == val.value
   * </code></pre>
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param threshold the allowed diff to still be considered as "no difference"
   * @param maxIterations the maximum number of iterations allowed
   * @param algorithm a closure containing the algoritm to apply
   * @return a Map of the results with the following keys:
   * <ul>
   *  <li>value: the actual value needed to produce the result</li>
   *  <li>result: the result of the value put in the algorithm</li>
   *  <li>diff: the difference from the target value</li>
   *  <li>iterations: the number of iterations it took to get to the conclusion</li>
   * </ul>
   * @throws RuntimeException if the goal cannot be found within the maxIterations
   */
  static Map solve(final double targetValue, double minValue, double maxValue, double threshold = 1.0e-8, int maxIterations = 1000, Closure<Double> algorithm) {
    if (minValue >= maxValue) {
      throw new IllegalArgumentException("minValue ($minValue) must be smaller than maxValue ($maxValue)")
    }
    UnivariateFunction function = new UnivariateFunction() {
      @Override
      double value(double v) {
        targetValue - algorithm.call(v)
      }
    }
    // Recommended 1.0e-12 when a an absolute accuracy is 1.0e-8 so the divide by 10 000 to get a similar ratio
    final double relativeAccuracy = threshold / 10_000
    final double absoluteAccuracy = threshold
    final int maxOrder = 5
    UnivariateSolver solver = new BracketingNthOrderBrentSolver(relativeAccuracy, absoluteAccuracy, maxOrder);
    double val = solver.solve(maxIterations, function, minValue, maxValue, AllowedSolution.LEFT_SIDE);
    //[value: val, result: algorithm.call(val), diff: seeker.getDiff(currentValue), iterations: i]
    double result = algorithm.call(val)
    return [value: val, result: result, diff: (targetValue-result), iterations: solver.getEvaluations()]
  }
}
