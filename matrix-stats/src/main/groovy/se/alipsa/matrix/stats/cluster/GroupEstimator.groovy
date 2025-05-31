package se.alipsa.matrix.stats.cluster

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
