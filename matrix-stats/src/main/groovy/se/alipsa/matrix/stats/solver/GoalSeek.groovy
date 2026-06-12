package se.alipsa.matrix.stats.solver

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Root-finding utility that mirrors spreadsheet "goal seek" behavior for one-dimensional functions.
 */
class GoalSeek {
  private static final double MIDPOINT_DIVISOR = 2.0d
  private static final double ROOT_TARGET = 0.0d
  private static final int DEFAULT_MAX_ITERATIONS = 1000

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
   * assert (val.computedValue - 0.270G).abs() < 1e-8G
   * </code></pre>
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param threshold the allowed diff to still be considered as "no difference"
   * @param maxIterations the maximum number of iterations allowed
   * @param algorithm a closure containing the algoritm to apply
   * @return a typed result with the following properties:
   * <ul>
   *  <li>value: the actual value needed to produce the result</li>
   *  <li>result: the result of the value put in the algorithm</li>
   *  <li>diff: the difference from the target value</li>
   *  <li>iterations: the number of iterations it took to get to the conclusion</li>
   * </ul>
   * @throws RuntimeException if the goal cannot be found within the maxIterations
   */
  static Result solve(
      final double targetValue,
      double minValue,
      double maxValue,
      double threshold = 1.0e-8,
      int maxIterations = DEFAULT_MAX_ITERATIONS,
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
    new Result(value: val, result: result, diff: (targetValue - result), iterations: solverResult.evaluations)
  }

  /**
   * Solves a goal seek problem from Groovy-facing numeric inputs.
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param algorithm a closure containing the algorithm to apply
   * @return the typed goal seek result
   */
  static Result solve(
      final Number targetValue,
      Number minValue,
      Number maxValue,
      Closure<? extends Number> algorithm
  ) {
    solve(targetValue, minValue, maxValue, 1.0e-8G, DEFAULT_MAX_ITERATIONS, algorithm)
  }

  /**
   * Solves a goal seek problem from Groovy-facing numeric inputs.
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param threshold the allowed diff to still be considered as "no difference"
   * @param algorithm a closure containing the algorithm to apply
   * @return the typed goal seek result
   */
  static Result solve(
      final Number targetValue,
      Number minValue,
      Number maxValue,
      Number threshold,
      Closure<? extends Number> algorithm
  ) {
    solve(targetValue, minValue, maxValue, threshold, DEFAULT_MAX_ITERATIONS, algorithm)
  }

  /**
   * Solves a goal seek problem from Groovy-facing numeric inputs.
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param threshold the allowed diff to still be considered as "no difference"
   * @param maxIterations the maximum number of iterations allowed
   * @param algorithm a closure containing the algorithm to apply
   * @return the typed goal seek result
   */
  static Result solve(
      final Number targetValue,
      Number minValue,
      Number maxValue,
      Number threshold,
      int maxIterations,
      Closure<? extends Number> algorithm
  ) {
    solve(
        NumericConversion.toFiniteDouble(targetValue, 'targetValue'),
        NumericConversion.toFiniteDouble(minValue, 'minValue'),
        NumericConversion.toFiniteDouble(maxValue, 'maxValue'),
        NumericConversion.toFiniteDouble(threshold, 'threshold'),
        maxIterations,
        algorithm
    )
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

    if (lowValue == ROOT_TARGET) {
      return low
    }
    if (highValue == ROOT_TARGET) {
      return high
    }

    for (int iteration = 0; iteration < 128 && (high - low) > absoluteAccuracy; iteration++) {
      double candidate = low - lowValue * (high - low) / (highValue - lowValue)
      if (!Double.isFinite(candidate) || candidate <= low || candidate >= high) {
        candidate = low + (high - low) / MIDPOINT_DIVISOR
      }
      double candidateValue = function.value(candidate)
      if (candidateValue == ROOT_TARGET) {
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
    low + (high - low) / MIDPOINT_DIVISOR
  }

  /**
   * Typed result of a goal seek solve.
   */
  static class Result {
    /** Actual value needed to produce the requested result. */
    double value
    /** Result of applying the algorithm to {@link #value}. */
    double result
    /** Difference between target value and {@link #result}. */
    double diff
    /** Number of solver evaluations performed. */
    int iterations

    /**
     * Returns the computed input value as {@code BigDecimal}.
     *
     * @return the computed input value
     */
    BigDecimal getComputedValue() {
      NumericConversion.toBigDecimal(value, 'value')
    }

    /**
     * Returns the algorithm result as {@code BigDecimal}.
     *
     * @return the algorithm result
     */
    BigDecimal getResultValue() {
      NumericConversion.toBigDecimal(result, 'result')
    }

    /**
     * Returns the difference from the target value as {@code BigDecimal}.
     *
     * @return the target difference
     */
    BigDecimal getDiffValue() {
      NumericConversion.toBigDecimal(diff, 'diff')
    }

    /**
     * Supports explicit compatibility coercion to the previous map-shaped result.
     *
     * @param targetType the requested coercion type
     * @return a map when {@code targetType} is {@code Map}; otherwise Groovy's default coercion result
     */
    Object asType(Class targetType) {
      if (targetType == Map) {
        return [value: value, result: result, diff: diff, iterations: iterations]
      }
      super.asType(targetType)
    }
  }
}
