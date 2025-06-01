import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.cluster.KMeansPlusPlus
import smile.clustering.KMeans

import static KMeansPlusPlusTest.gaussianClusters
import static java.lang.Math.pow
import static java.lang.Math.sqrt
import static org.junit.jupiter.api.Assertions.assertEquals

class KMeansComparisonTest {

  @Test
  void compareMatrixAndSmileKMeans() {
    int k = 4
    double[][] points = gaussianClusters(750, 0.05)

    def matrixKMeans = new KMeansPlusPlus.Builder(k, points)
        .iterations(20)
        .epsilon(0.003)
        .useEpsilon(true)
        .pp(true)
        .useL1norm(false) // Use L2 norm for consistency with SMILE
        .build()

    def matrixCentroids = matrixKMeans.centroids
    def matrixGroups = matrixKMeans.assignment.toList().countBy { it.clusterId }
    def matrixWCSS = matrixKMeans.WCSS

    println "\nMatrix KMeans:"
    println "Group sizes: $matrixGroups"
    println "WCSS: $matrixWCSS"
    println "Centroids: ${matrixCentroids.collect { it.toList() }}"

    // ✅ Use Smile's built-in fit method for KMeans
    def smileKMeans = KMeans.fit(points, k, 100)
    def smileCentroids = smileKMeans.centers()
    def smileLabels = smileKMeans.group().toList()
    def smileGroups = smileLabels.countBy { it }
    def smileWCSS = smileKMeans.distortion()

    println "\nSMILE KMeans:"
    println "Group sizes: $smileGroups"
    println "WCSS: $smileWCSS"
    println "Centroids: ${smileCentroids.collect { it.toList() }}"

    List<double[]> matchedSmileCentroids = []

    matrixCentroids.each { m ->
      def (closest, _) = smileCentroids.collect { s ->
        [s, sqrt(pow(m[0] - s[0], 2) + pow(m[1] - s[1], 2))]
      }.min { it[1] } // Find closest Smile centroid
      matchedSmileCentroids << closest
    }

    // Now do assertions
    matrixCentroids.eachWithIndex { m, i ->
      def s = matchedSmileCentroids[i]
      assertEquals(m[0], s[0], 1e-5d)
      assertEquals(m[1], s[1], 1e-5d)
    }
    // WCSS calculation differs between Smile and Matrix, so we adjust and allow a small tolerance
    def n = points.length
    def d = points[0].length
    def smileTotalWCSS = smileWCSS * n * d / 2
    assertEquals(matrixWCSS, smileTotalWCSS, 0.05d)
  }
}
