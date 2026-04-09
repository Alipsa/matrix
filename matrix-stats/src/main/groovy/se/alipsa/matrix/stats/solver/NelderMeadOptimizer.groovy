package se.alipsa.matrix.stats.solver

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Native Nelder-Mead simplex optimizer for low-dimensional derivative-free minimization.
 *
 * <p>This optimizer intentionally keeps its simplex state in primitive double arrays.
 * That keeps the implementation close to the standard algorithm and avoids boxing or
 * BigDecimal overhead inside hot optimization loops.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
final class NelderMeadOptimizer {

  private static final double REFLECTION = 1.0d
  private static final double EXPANSION = 2.0d
  private static final double CONTRACTION = 0.5d
  private static final double SHRINK = 0.5d

  private NelderMeadOptimizer() {
  }

  /**
   * Minimizes a multivariate objective using a Nelder-Mead simplex.
   *
   * @param objective objective function to minimize
   * @param initialGuess initial simplex anchor point
   * @param stepSizes axis-aligned simplex edge sizes
   * @param maxEvaluations maximum objective evaluations
   * @param relativeThreshold relative convergence threshold
   * @param absoluteThreshold absolute convergence threshold
   * @return best point and value found by the optimizer
   */
  static OptimizationResult minimize(
      MultivariateObjective objective,
      double[] initialGuess,
      double[] stepSizes,
      int maxEvaluations,
      double relativeThreshold,
      double absoluteThreshold
  ) {
    if (objective == null) {
      throw new IllegalArgumentException("Objective function cannot be null")
    }
    if (initialGuess == null || initialGuess.length == 0) {
      throw new IllegalArgumentException("Initial guess must contain at least one dimension")
    }
    if (stepSizes == null || stepSizes.length != initialGuess.length) {
      throw new IllegalArgumentException("Step sizes must match the initial guess length")
    }
    if (maxEvaluations < initialGuess.length + 1) {
      throw new IllegalArgumentException("maxEvaluations is too small for the simplex dimension")
    }

    int dimension = initialGuess.length
    double[][] simplex = new double[dimension + 1][dimension]
    double[] values = new double[dimension + 1]

    simplex[0] = copyOf(initialGuess)
    values[0] = objective.value(simplex[0])
    int evaluations = 1

    for (int i = 0; i < dimension; i++) {
      simplex[i + 1] = copyOf(initialGuess)
      simplex[i + 1][i] += stepSizes[i]
      values[i + 1] = objective.value(simplex[i + 1])
      evaluations++
    }

    int iterations = 0
    while (evaluations < maxEvaluations) {
      sortSimplex(simplex, values)
      if (hasConverged(simplex, values, relativeThreshold, absoluteThreshold)) {
        break
      }
      iterations++

      double[] centroid = centroid(simplex, dimension)
      double[] reflected = transform(centroid, simplex[dimension], REFLECTION)
      double reflectedValue = objective.value(reflected)
      evaluations++

      if (reflectedValue < values[0]) {
        double[] expanded = transform(centroid, simplex[dimension], EXPANSION)
        double expandedValue = objective.value(expanded)
        evaluations++
        if (expandedValue < reflectedValue) {
          simplex[dimension] = expanded
          values[dimension] = expandedValue
        } else {
          simplex[dimension] = reflected
          values[dimension] = reflectedValue
        }
        continue
      }

      if (reflectedValue < values[dimension - 1]) {
        simplex[dimension] = reflected
        values[dimension] = reflectedValue
        continue
      }

      boolean outside = reflectedValue < values[dimension]
      double[] contracted = outside ?
          contract(centroid, reflected, CONTRACTION) :
          contract(centroid, simplex[dimension], CONTRACTION)
      double contractedValue = objective.value(contracted)
      evaluations++

      if ((outside && contractedValue <= reflectedValue) || (!outside && contractedValue < values[dimension])) {
        simplex[dimension] = contracted
        values[dimension] = contractedValue
        continue
      }

      for (int i = 1; i <= dimension; i++) {
        simplex[i] = shrink(simplex[0], simplex[i], SHRINK)
        values[i] = objective.value(simplex[i])
        evaluations++
        if (evaluations >= maxEvaluations) {
          break
        }
      }
    }

    sortSimplex(simplex, values)
    new OptimizationResult(point: simplex[0], value: values[0], evaluations: evaluations, iterations: iterations)
  }

  /**
   * Minimizes an objective from idiomatic numeric vectors.
   *
   * @param objective objective function to minimize
   * @param initialGuess initial simplex anchor point
   * @param stepSizes axis-aligned simplex edge sizes
   * @param maxEvaluations maximum objective evaluations
   * @param relativeThreshold relative convergence threshold
   * @param absoluteThreshold absolute convergence threshold
   * @return best point and value found by the optimizer
   */
  static OptimizationResult minimize(
      MultivariateObjective objective,
      List<? extends Number> initialGuess,
      List<? extends Number> stepSizes,
      int maxEvaluations,
      Number relativeThreshold,
      Number absoluteThreshold
  ) {
    minimize(
      objective,
      NumericConversion.toDoubleArray(initialGuess, 'initialGuess'),
      NumericConversion.toDoubleArray(stepSizes, 'stepSizes'),
      maxEvaluations,
      NumericConversion.toFiniteDouble(relativeThreshold, 'relativeThreshold'),
      NumericConversion.toFiniteDouble(absoluteThreshold, 'absoluteThreshold')
    )
  }

  private static double[] centroid(double[][] simplex, int dimension) {
    double[] centroid = new double[dimension]
    for (int i = 0; i < dimension; i++) {
      for (int j = 0; j < dimension; j++) {
        centroid[j] += simplex[i][j]
      }
    }
    for (int j = 0; j < dimension; j++) {
      centroid[j] /= dimension
    }
    centroid
  }

  private static double[] transform(double[] centroid, double[] worst, double factor) {
    double[] transformed = new double[centroid.length]
    for (int i = 0; i < centroid.length; i++) {
      transformed[i] = centroid[i] + factor * (centroid[i] - worst[i])
    }
    transformed
  }

  private static double[] contract(double[] centroid, double[] point, double factor) {
    double[] contracted = new double[centroid.length]
    for (int i = 0; i < centroid.length; i++) {
      contracted[i] = centroid[i] + factor * (point[i] - centroid[i])
    }
    contracted
  }

  private static double[] shrink(double[] best, double[] point, double factor) {
    double[] shrunk = new double[best.length]
    for (int i = 0; i < best.length; i++) {
      shrunk[i] = best[i] + factor * (point[i] - best[i])
    }
    shrunk
  }

  private static boolean hasConverged(double[][] simplex, double[] values, double relativeThreshold, double absoluteThreshold) {
    double bestValue = values[0]
    double maxValueSpread = 0.0d
    for (int i = 1; i < values.length; i++) {
      maxValueSpread = Math.max(maxValueSpread, Math.abs(values[i] - bestValue))
    }
    double valueTolerance = absoluteThreshold + relativeThreshold * Math.max(1.0d, Math.abs(bestValue))
    if (maxValueSpread > valueTolerance) {
      return false
    }

    for (int i = 1; i < simplex.length; i++) {
      for (int j = 0; j < simplex[i].length; j++) {
        double coordinateTolerance = absoluteThreshold + relativeThreshold * Math.max(1.0d, Math.abs(simplex[0][j]))
        if (Math.abs(simplex[i][j] - simplex[0][j]) > coordinateTolerance) {
          return false
        }
      }
    }
    true
  }

  private static void sortSimplex(double[][] simplex, double[] values) {
    for (int i = 0; i < values.length - 1; i++) {
      int minIndex = i
      for (int j = i + 1; j < values.length; j++) {
        if (values[j] < values[minIndex]) {
          minIndex = j
        }
      }
      if (minIndex != i) {
        double[] point = simplex[i]
        simplex[i] = simplex[minIndex]
        simplex[minIndex] = point
        double value = values[i]
        values[i] = values[minIndex]
        values[minIndex] = value
      }
    }
  }

  private static double[] copyOf(double[] values) {
    double[] copy = new double[values.length]
    System.arraycopy(values, 0, copy, 0, values.length)
    copy
  }

  static class OptimizationResult {
    /** Best parameter vector found. */
    double[] point
    /** Objective value at {@link #point}. */
    double value
    /** Number of objective evaluations performed. */
    int evaluations
    /** Number of simplex iterations performed. */
    int iterations

    List<BigDecimal> getPointValues() {
      NumericConversion.toBigDecimalList(point, 'point')
    }

    BigDecimal getObjectiveValue() {
      BigDecimal.valueOf(value)
    }
  }
}
