package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

/**
 * Convergent Cross Mapping (CCM) is a statistical test for detecting causal relationships between variables
 * in dynamical systems. Unlike Granger causality which assumes separable influences, CCM is designed for
 * systems with synergistic, nonlinear interactions where traditional correlation-based methods fail.
 *
 * <p>While Granger causality is best suited for purely stochastic systems where the influences
 * of the causal variables are separable (independent of each other), CCM is based on the theory of dynamical
 * systems and can be applied to systems where causal variables have synergistic effects, such as
 * ecological systems, climate dynamics, and physiological networks.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>When analyzing nonlinear dynamical systems (ecosystems, climate, physiology)</li>
 * <li>When variables appear uncorrelated but may have causal relationships</li>
 * <li>When causality is mediated through state space rather than direct linear effects</li>
 * <li>When Granger causality gives inconclusive results due to nonlinearity</li>
 * <li>For detecting weak coupling or indirect causal influences</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> Variable X does not causally influence variable Y</li>
 * <li><strong>H1 (alternative):</strong> Variable X causally influences Y (evidenced by convergent cross-map skill)</li>
 * </ul>
 *
 * <h3>Theory</h3>
 * <p>Based on Takens' embedding theorem, if X causally drives Y, then the reconstructed state space (shadow manifold)
 * of Y contains information about X. CCM tests this by attempting to predict X from Y's manifold.
 * Convergence (improving prediction with more data) indicates causality.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test for causality between two nonlinear time series
 * def x = [1.0, 1.2, 1.5, 1.3, 1.6, 1.4, 1.8, ...] as double[]
 * def y = [2.0, 2.3, 2.8, 2.5, 3.0, 2.7, 3.2, ...] as double[]
 *
 * // E = embedding dimension, tau = time delay, librarySizes = library sizes to test
 * def result = Ccm.test(x, y, 3, 1, [10, 20, 30, 50])
 * println result.interpret()
 *
 * // Check specific directional causality
 * if (result.xCausesY()) {
 *   println "X causally drives Y"
 * }
 * if (result.yCausesX()) {
 *   println "Y causally drives X"
 * }
 * </pre>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Sugihara, G., May, R., Ye, H., et al. (2012). "Detecting Causality in Complex Ecosystems",
 * Science, 338(6106), 496-500.</li>
 * <li>Takens, F. (1981). "Detecting strange attractors in turbulence", in Dynamical Systems and Turbulence,
 * Lecture Notes in Mathematics, 898, 366-381.</li>
 * <li>Ye, H., et al. (2015). "Distinguishing time-delayed causal interactions using convergent cross mapping",
 * Scientific Reports, 5, 14750.</li>
 * </ul>
 */
@CompileStatic
class Ccm {

  /**
   * Performs convergent cross mapping test.
   *
   * @param x First time series (potential cause)
   * @param y Second time series (potential effect)
   * @param E Embedding dimension (typically 2-5)
   * @param tau Time delay for embedding (typically 1)
   * @param librarySizes Library sizes to test for convergence (e.g., [10, 20, 50, 100])
   * @return CcmResult containing cross-map skills and convergence information
   */
  static CcmResult test(double[] x, double[] y, int E = 3, int tau = 1, List<Integer> librarySizes = null) {
    if (x == null || y == null) {
      throw new IllegalArgumentException("Time series cannot be null")
    }

    if (x.length != y.length) {
      throw new IllegalArgumentException("Time series must have the same length (x: ${x.length}, y: ${y.length})")
    }

    int n = x.length

    if (E < 1) {
      throw new IllegalArgumentException("Embedding dimension E must be at least 1 (got ${E})")
    }

    if (tau < 1) {
      throw new IllegalArgumentException("Time delay tau must be at least 1 (got ${tau})")
    }

    int minObservations = E * tau + 1
    if (n < minObservations + 10) {
      throw new IllegalArgumentException("Need at least ${minObservations + 10} observations for E=${E}, tau=${tau} (got ${n})")
    }

    // Default library sizes if not specified
    if (librarySizes == null || librarySizes.isEmpty()) {
      librarySizes = generateDefaultLibrarySizes(n, E, tau)
    }

    // Compute valid range for library sizes
    int maxValidLib = n - (E - 1) * tau

    // Validate library sizes
    for (Integer lib : librarySizes) {
      if (lib < E + 1) {
        throw new IllegalArgumentException("Library size must be at least E+1 = ${E + 1} (got ${lib})")
      }
      if (lib > maxValidLib) {
        throw new IllegalArgumentException("Library size ${lib} too large (max: ${maxValidLib} for n=${n}, E=${E}, tau=${tau})")
      }
    }

    // Perform cross-mapping in both directions
    // X causes Y: cross-map X from Y's manifold (xmap_y)
    // Y causes X: cross-map Y from X's manifold (ymap_x)
    double[] xmapY = new double[librarySizes.size()]
    double[] ymapX = new double[librarySizes.size()]

    for (int i = 0; i < librarySizes.size(); i++) {
      int L = librarySizes[i]

      // Cross-map X from Y's shadow manifold
      xmapY[i] = crossMap(y, x, E, tau, L)

      // Cross-map Y from X's shadow manifold
      ymapX[i] = crossMap(x, y, E, tau, L)
    }

    return new CcmResult(
      xmapY: xmapY,
      ymapX: ymapX,
      librarySizes: librarySizes.toArray(new Integer[0]),
      embeddingDim: E,
      timeLag: tau,
      seriesLength: n
    )
  }

  /**
   * Cross-maps target from source's shadow manifold.
   * If source causes target, we should be able to predict target from source's reconstructed state space.
   *
   * @param source Time series to build shadow manifold from
   * @param target Time series to predict
   * @param E Embedding dimension
   * @param tau Time delay
   * @param L Library size (number of points to use)
   * @return Cross-map correlation (Pearson's r between actual and predicted target)
   */
  private static double crossMap(double[] source, double[] target, int E, int tau, int L) {
    int n = source.length
    int maxDelay = (E - 1) * tau

    // Build shadow manifold from source
    int numVectors = n - maxDelay
    double[][] shadowManifold = new double[numVectors][E]

    for (int t = 0; t < numVectors; t++) {
      for (int e = 0; e < E; e++) {
        shadowManifold[t][e] = source[t + e * tau]
      }
    }

    // Randomly select L points for library
    List<Integer> libraryIndices = selectRandomIndices(numVectors, L)

    // Predict target for each library point using nearest neighbors
    List<Double> predictions = []
    List<Double> actuals = []

    for (Integer idx : libraryIndices) {
      // Find E+1 nearest neighbors (excluding the point itself)
      List<NeighborDistance> neighbors = findNearestNeighbors(shadowManifold, idx, E + 1, libraryIndices)

      // Make prediction using simplex projection (weighted average)
      double prediction = simplexProjection(neighbors, target, maxDelay)
      double actual = target[idx + maxDelay]

      predictions.add(prediction)
      actuals.add(actual)
    }

    // Compute Pearson correlation between predictions and actuals
    return pearsonCorrelation(predictions as double[], actuals as double[])
  }

  /**
   * Finds k nearest neighbors in the shadow manifold.
   */
  private static List<NeighborDistance> findNearestNeighbors(double[][] manifold, int queryIdx,
                                                               int k, List<Integer> validIndices) {
    double[] query = manifold[queryIdx]
    List<NeighborDistance> neighbors = []

    for (Integer idx : validIndices) {
      if (idx == queryIdx) continue  // Exclude self

      double dist = euclideanDistance(query, manifold[idx])
      neighbors.add(new NeighborDistance(index: idx, distance: dist))
    }

    // Sort by distance and take k nearest
    neighbors.sort { it.distance }
    return neighbors.take(Math.min(k, neighbors.size()))
  }

  /**
   * Makes prediction using simplex projection (distance-weighted average).
   */
  private static double simplexProjection(List<NeighborDistance> neighbors, double[] target, int offset) {
    if (neighbors.isEmpty()) {
      return Double.NaN
    }

    // Compute weights (inverse distance, with small epsilon to avoid division by zero)
    double epsilon = 1e-6
    double[] weights = new double[neighbors.size()]
    double weightSum = 0.0

    for (int i = 0; i < neighbors.size(); i++) {
      double dist = neighbors[i].distance
      weights[i] = 1.0 / (dist + epsilon)
      weightSum += weights[i]
    }

    // Normalize weights and compute weighted average
    double prediction = 0.0
    for (int i = 0; i < neighbors.size(); i++) {
      double w = weights[i] / weightSum
      double targetVal = target[neighbors[i].index + offset]
      prediction += w * targetVal
    }

    return prediction
  }

  /**
   * Euclidean distance between two vectors.
   */
  private static double euclideanDistance(double[] a, double[] b) {
    double sum = 0.0
    for (int i = 0; i < a.length; i++) {
      double diff = a[i] - b[i]
      sum += diff * diff
    }
    return Math.sqrt(sum)
  }

  /**
   * Selects n random indices from range [0, max).
   */
  private static List<Integer> selectRandomIndices(int max, int n) {
    if (n >= max) {
      return (0..<max).toList()
    }

    Random rnd = new Random()
    Set<Integer> selected = new HashSet<>()
    while (selected.size() < n) {
      selected.add(rnd.nextInt(max))
    }
    return selected.toList()
  }

  /**
   * Computes Pearson correlation coefficient.
   */
  private static double pearsonCorrelation(double[] x, double[] y) {
    if (x.length != y.length || x.length == 0) {
      return Double.NaN
    }

    int n = x.length
    double sumX = 0.0, sumY = 0.0, sumXY = 0.0
    double sumX2 = 0.0, sumY2 = 0.0

    for (int i = 0; i < n; i++) {
      sumX += x[i]
      sumY += y[i]
      sumXY += x[i] * y[i]
      sumX2 += x[i] * x[i]
      sumY2 += y[i] * y[i]
    }

    double numerator = n * sumXY - sumX * sumY
    double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

    if (Math.abs(denominator) < 1e-10) {
      return 0.0
    }

    return numerator / denominator
  }

  /**
   * Generates default library sizes based on series length.
   */
  private static List<Integer> generateDefaultLibrarySizes(int n, int E, int tau) {
    int maxDelay = (E - 1) * tau
    int maxLib = n - maxDelay
    int minLib = E + 1

    List<Integer> sizes = []
    int step = Math.max(1, (int) ((maxLib - minLib) / 5))

    for (int lib = minLib; lib <= maxLib; lib += step) {
      sizes.add(lib)
    }

    // Always include max
    if (sizes[-1] != maxLib) {
      sizes.add(maxLib)
    }

    return sizes
  }

  /**
   * Helper class for nearest neighbor with distance.
   */
  @CompileStatic
  private static class NeighborDistance {
    int index
    double distance
  }

  /**
   * Result class for CCM test.
   */
  @CompileStatic
  static class CcmResult {
    /** Cross-map skill: X cross-mapped from Y (if positive and increasing, Y causes X) */
    double[] xmapY

    /** Cross-map skill: Y cross-mapped from X (if positive and increasing, X causes Y) */
    double[] ymapX

    /** Library sizes used for testing */
    Integer[] librarySizes

    /** Embedding dimension */
    int embeddingDim

    /** Time lag */
    int timeLag

    /** Original series length */
    int seriesLength

    /**
     * Checks if X causes Y (based on increasing cross-map skill from X's manifold).
     *
     * @param threshold Minimum correlation for last library size
     * @return true if evidence suggests X causes Y
     */
    boolean xCausesY(double threshold = 0.3) {
      if (ymapX.length < 2) return false
      // Check if correlation is positive and increasing
      return ymapX[-1] > threshold && ymapX[-1] > ymapX[0]
    }

    /**
     * Checks if Y causes X (based on increasing cross-map skill from Y's manifold).
     *
     * @param threshold Minimum correlation for last library size
     * @return true if evidence suggests Y causes X
     */
    boolean yCausesX(double threshold = 0.3) {
      if (xmapY.length < 2) return false
      // Check if correlation is positive and increasing
      return xmapY[-1] > threshold && xmapY[-1] > xmapY[0]
    }

    /**
     * Interprets the CCM results.
     */
    String interpret() {
      StringBuilder sb = new StringBuilder()
      sb.append("Convergent Cross Mapping Results:\n")
      sb.append("=" * 60).append("\n\n")

      sb.append("Configuration:\n")
      sb.append("  Embedding dimension (E): ${embeddingDim}\n")
      sb.append("  Time lag (tau): ${timeLag}\n")
      sb.append("  Series length: ${seriesLength}\n")
      sb.append("  Library sizes: ${librarySizes.join(', ')}\n\n")

      sb.append("Cross-map Skills:\n")
      sb.append("-" * 60).append("\n")
      sb.append(String.format("%-15s %-20s %-20s\n", "Library Size", "X|M(Y) (Y→X)", "Y|M(X) (X→Y)"))
      sb.append("-" * 60).append("\n")

      for (int i = 0; i < librarySizes.length; i++) {
        sb.append(String.format("%-15d %-20.4f %-20.4f\n",
                                librarySizes[i], xmapY[i], ymapX[i]))
      }

      sb.append("\n")
      sb.append("Interpretation:\n")
      sb.append("-" * 60).append("\n")

      boolean xToY = xCausesY()
      boolean yToX = yCausesX()

      if (xToY && yToX) {
        sb.append("Bidirectional causality: Both X and Y appear to causally influence each other\n")
      } else if (xToY) {
        sb.append("Unidirectional causality: X appears to causally drive Y\n")
      } else if (yToX) {
        sb.append("Unidirectional causality: Y appears to causally drive X\n")
      } else {
        sb.append("No clear causal relationship detected\n")
      }

      sb.append("\nNote: CCM detects causality via state space reconstruction.\n")
      sb.append("Positive, increasing cross-map skill indicates causal influence.\n")

      return sb.toString()
    }

    @Override
    String toString() {
      return interpret()
    }
  }
}
