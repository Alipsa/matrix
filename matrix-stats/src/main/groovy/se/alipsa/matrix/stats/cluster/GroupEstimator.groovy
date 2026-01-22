package se.alipsa.matrix.stats.cluster

/**
 * GroupEstimator determines the optimal number of clusters (k) for K-Means clustering
 * using heuristic and analytical methods. Choosing the right k is critical for meaningful
 * clustering results, and this class automates that decision.
 *
 * <p>The estimator supports two methods: the Elbow method (recommended) and a simple
 * rule-of-thumb heuristic. Both methods analyze the data structure to suggest an
 * appropriate number of clusters.</p>
 *
 * <h3>What is the Optimal k Problem?</h3>
 * <p>K-Means requires specifying k (number of clusters) upfront, but the ideal k is often
 * unknown. Too few clusters lose important patterns; too many create artificial divisions.
 * GroupEstimator solves this by analyzing how clustering quality changes with different k values.</p>
 *
 * <h3>When to Use GroupEstimator</h3>
 * <ul>
 *   <li><strong>Exploratory Analysis:</strong> When you don't know how many natural groups exist</li>
 *   <li><strong>Automated Pipelines:</strong> When k must be determined programmatically</li>
 *   <li><strong>Data-Driven Segmentation:</strong> When business requirements don't dictate cluster count</li>
 *   <li><strong>Validation:</strong> To confirm domain expert suggestions about cluster count</li>
 * </ul>
 *
 * <h3>Estimation Methods</h3>
 *
 * <h4>1. ELBOW Method (Recommended)</h4>
 * <p>The Elbow method plots WCSS (Within-Cluster Sum of Squares) against k and identifies
 * the "elbow point" where adding more clusters yields diminishing returns.</p>
 *
 * <p><strong>How it works:</strong></p>
 * <ol>
 *   <li>Run K-Means for k = 1 to maxK (default: 10)</li>
 *   <li>Record WCSS for each k value</li>
 *   <li>Find the point of maximum perpendicular distance from the line connecting first and last points</li>
 *   <li>This "elbow" represents the optimal k where marginal improvement drops significantly</li>
 * </ol>
 *
 * <p><strong>Advantages:</strong> Data-driven, visually interpretable, works well for distinct clusters</p>
 * <p><strong>Limitations:</strong> Computationally expensive, may not find clear elbow in noisy data</p>
 *
 * <h4>2. RULE_OF_THUMB Method</h4>
 * <p>A simple heuristic that estimates k using mathematical formulas based on dataset size n:</p>
 * <ul>
 *   <li>Conservative estimate: ∛n (cube root, tends to underestimate)</li>
 *   <li>Liberal estimate: √(n/2) (tends to overestimate)</li>
 *   <li>Final k: average of both estimates</li>
 * </ul>
 *
 * <p><strong>Advantages:</strong> Fast, no clustering required, good starting point</p>
 * <p><strong>Limitations:</strong> Ignores data structure, less accurate than Elbow method</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.stats.cluster.GroupEstimator
 * import se.alipsa.matrix.stats.cluster.GroupEstimator.CalculationMethod
 *
 * // Prepare data as double[][]
 * double[][] points = [[1.0, 2.0], [1.5, 1.8], [5.0, 8.0], [8.0, 8.0], [1.0, 0.6]]
 *
 * // Method 1: Using Elbow method (recommended)
 * GroupEstimator estimator = new GroupEstimator(CalculationMethod.ELBOW)
 * int k = estimator.estimateNumberOfGroups(points)
 * println "Optimal k (Elbow): $k"
 *
 * // Method 2: Using rule of thumb (faster)
 * estimator = new GroupEstimator(CalculationMethod.RULE_OF_THUMB)
 * k = estimator.estimateNumberOfGroups(points)
 * println "Estimated k (Rule): $k"
 *
 * // Method 3: Static method with custom parameters
 * k = GroupEstimator.estimateKByElbow(points, 15, 20)  // maxK=15, iterations=20
 * println "Optimal k (custom): $k"
 * </pre>
 *
 * <h3>Integration with KMeans</h3>
 * <pre>
 * import se.alipsa.matrix.stats.cluster.KMeans
 * import se.alipsa.matrix.core.Matrix
 *
 * Matrix data = Matrix.builder().data(points).build()
 * KMeans kmeans = new KMeans(data)
 *
 * // KMeans automatically uses GroupEstimator when k is not specified
 * Matrix result = kmeans.fit(features, 30)  // Auto-estimates k with 30 iterations
 *
 * // Or specify the estimation method
 * result = kmeans.fit(features, 30, CalculationMethod.ELBOW)
 * </pre>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>WCSS (Within-Cluster Sum of Squares)</strong></dt>
 *   <dd>Measures cluster compactness. As k increases, WCSS decreases. The elbow is where
 *   the rate of decrease sharply slows, indicating optimal k.</dd>
 *
 *   <dt><strong>Elbow Point</strong></dt>
 *   <dd>The point on the WCSS curve where the angle is sharpest, representing the best
 *   trade-off between cluster count and quality. Computed using perpendicular distance
 *   from the line connecting first and last points.</dd>
 *
 *   <dt><strong>maxK Parameter</strong></dt>
 *   <dd>Maximum number of clusters to test (default: 10). Higher values are more thorough
 *   but slower. Should be less than the number of distinct data points.</dd>
 * </dl>
 *
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li><strong>ELBOW method:</strong> Runs K-Means maxK times, expensive for large datasets</li>
 *   <li><strong>RULE_OF_THUMB:</strong> Instant, suitable for real-time applications</li>
 *   <li><strong>Cache results:</strong> Estimation is deterministic for a given dataset</li>
 *   <li><strong>Adjust maxK:</strong> Lower maxK for faster results, higher for more accuracy</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 *   <li>Thorndike, R.L. (1953). "Who belongs in the family?" Psychometrika, 18(4): 267–276.
 *   (Original description of the elbow method)</li>
 *   <li>Ketchen, D.J. and Shook, C.L. (1996). "The application of cluster analysis in strategic
 *   management research: an analysis and critique." Strategic Management Journal, 17(6): 441–458.</li>
 *   <li>Kodinariya, T.M. and Makwana, P.R. (2013). "Review on determining number of Cluster in
 *   K-Means Clustering." International Journal of Advance Research in Computer Science and
 *   Management Studies, 1(6): 90–95.</li>
 * </ul>
 *
 * @see KMeans
 * @see KMeansPlusPlus
 */

class GroupEstimator {

  private final CalculationMethod method

  enum CalculationMethod {
    RULE_OF_THUMB, ELBOW
  }

  GroupEstimator(CalculationMethod method = CalculationMethod.ELBOW) {
    this.method = method
  }

  int estimateNumberOfGroups(double[][] points) {
    if (points.length < 2) {
      throw new IllegalArgumentException("Number of points must be at least 2, but was $numPoints")
    }

    switch (method) {
      case CalculationMethod.RULE_OF_THUMB:
        return ruleOfThumb(points)
      case CalculationMethod.ELBOW:
        return estimateKByElbow(points)
      default:
        throw new IllegalArgumentException("Unknown calculation method: $method")
    }
  }

  static int estimateKByElbow(double[][] points, int maxK = 10, int iterations = 10) {
    Set<String> uniquePoints = points.collect { it.toList().toString() }.toSet()
    int maxClusters = Math.min(maxK, uniquePoints.size())

    if (maxClusters < 2) {
      throw new IllegalArgumentException("Not enough distinct points to estimate clusters (found ${uniquePoints.size()})")
    }

    List<Double> wcssList = []
    List<Integer> ks = (1..maxClusters).toList()

    for (int k : ks) {
      KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(k, points)
          .iterations(iterations)
          .pp(true)
          .useEpsilon(true)
          .build()
      wcssList << clustering.getWCSS()
    }

    return findElbow(ks, wcssList)
  }

  private static int ruleOfThumb(double[][] points) {
    //println "Estimated number of clusters (k) = $estimated"
    int n = points.length
    // Simple heuristic: sqrt(n / 2), tends to overestimate
    int estimatedSqrt = (int) Math.round(Math.sqrt(n / 2.0d))
    // More conservative heuristic: cbrt(n), tends to underestimate
    int estimatedQbrt = Math.max(2, (int) Math.round(Math.cbrt(points.length)))
    return (estimatedSqrt + estimatedQbrt)/2
  }

  /**
   * Finds the elbow point in a WCSS vs. k curve using the triangle method.
   * This method computes the point with the maximum distance from the line between first and last.
   */
  private static int findElbow(List<Integer> ks, List<Double> wcssList) {
    double x1 = ks.first() as double
    double y1 = wcssList.first()
    double x2 = ks.last() as double
    double y2 = wcssList.last()

    double maxDist = -1
    int bestK = ks.first()

    for (int i = 1; i < ks.size() - 1; i++) {
      double x0 = ks[i] as double
      double y0 = wcssList[i]

      // Compute distance from point to line
      double numerator = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2*y1 - y2*x1)
      double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2))
      double dist = numerator / denominator

      if (dist > maxDist) {
        maxDist = dist
        bestK = ks[i]
      }
    }

    return bestK
  }
}
