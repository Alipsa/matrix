package se.alipsa.matrix.stats.solver

/**
 * Root-finding utility that mirrors spreadsheet "goal seek" behavior for one-dimensional functions.
 */
class GoalSeek {

  /**
   * Provides similar functionality to the excel/calc goal seek function.
   * It uses a native Brent-Dekker bracketing solver to combine bisection robustness
   * with secant and inverse quadratic interpolation steps when they are safe.
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
  static Map solve(
      final double targetValue,
      double minValue,
      double maxValue,
      double threshold = 1.0e-8,
      int maxIterations = 1000,
      Closure<? extends Number> algorithm
  ) {
    if (minValue >= maxValue) {
      throw new IllegalArgumentException("minValue ($minValue) must be smaller than maxValue ($maxValue)")
    }
    UnivariateObjective function = { double v -> targetValue - (algorithm.call(v) as double) } as UnivariateObjective
    // Recommended 1.0e-12 when a an absolute accuracy is 1.0e-8 so the divide by 10 000 to get a similar ratio
    final double relativeAccuracy = threshold / 10_000
    final double absoluteAccuracy = threshold
    BrentSolver.SolverResult solverResult = BrentSolver.solve(
        function,
        minValue,
        maxValue,
        relativeAccuracy,
        absoluteAccuracy,
        maxIterations
    )
    double val = refineRoot(function, solverResult.root, solverResult.lowerBound, solverResult.upperBound, absoluteAccuracy)
    double result = algorithm.call(val) as double
    return [value: val, result: result, diff: (targetValue-result), iterations: solverResult.evaluations]
  }

  private static double refineRoot(
      UnivariateObjective function,
      double root,
      double lowerBound,
      double upperBound,
      double absoluteAccuracy
  ) {
    if ((upperBound - lowerBound) <= 4.0d * absoluteAccuracy) {
      return root
    }
    if (lowerBound == upperBound) {
      return lowerBound
    }

    double low = lowerBound
    double high = upperBound
    double lowValue = function.value(low)
    double highValue = function.value(high)

    if (lowValue == 0.0d) {
      return low
    }
    if (highValue == 0.0d) {
      return high
    }

    for (int iteration = 0; iteration < 128 && (high - low) > absoluteAccuracy; iteration++) {
      double candidate = low - lowValue * (high - low) / (highValue - lowValue)
      if (!Double.isFinite(candidate) || candidate <= low || candidate >= high) {
        candidate = low + (high - low) / 2.0d
      }
      double candidateValue = function.value(candidate)
      if (candidateValue == 0.0d) {
        return candidate
      }
      if (Math.signum(candidateValue) == Math.signum(lowValue)) {
        low = candidate
        lowValue = candidateValue
      } else {
        high = candidate
        highValue = candidateValue
      }
    }
    low + (high - low) / 2.0d
  }
}
