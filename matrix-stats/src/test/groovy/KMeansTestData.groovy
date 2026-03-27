/**
 * Shared synthetic datasets for k-means tests.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class KMeansTestData {

  static final long RANDOM_SEED = 42L

  static List<List<Double>> gaussianClusters(int pointsPerCluster = 750, double stdDev = 0.05, long randomSeed = RANDOM_SEED) {
    def centers = [
        [0.0, 0.0],
        [1.0, 0.0],
        [0.0, 1.0],
        [1.0, 1.0]
    ]

    Random rand = new Random(randomSeed)
    List<List<Double>> allPoints = []

    centers.each { center ->
      (1..pointsPerCluster).each {
        double x = center[0] + rand.nextGaussian() * stdDev
        double y = center[1] + rand.nextGaussian() * stdDev
        allPoints << [x, y]
      }
    }
    allPoints
  }
}
