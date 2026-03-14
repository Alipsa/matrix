import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.cluster.GroupEstimator
import se.alipsa.matrix.stats.cluster.KMeansPlusPlus

import static KMeansTestData.RANDOM_SEED
import static KMeansTestData.gaussianClusters
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Tests the KMeans clustering algorithm using a synthetic dataset.
 * @author Jason Altschuler
 * @author Per Nyfelt
 */
class KMeansPlusPlusTest {

  @Test
  void testKMeansPlusPlus() {
    // the test data is four 750-point Gaussian clusters (3000 points in all)
    // created around the vertices of the unit square
    int k = 4
    double[][] points = gaussianClusters(750, 0.05) as double[][]

    // run K-means
    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(k, points)
        .iterations(20)
        .pp(true)
        .epsilon(.003)
        .useEpsilon(true)
        .randomSeed(RANDOM_SEED)
        .build()

    // print timing information
    println clustering.getTiming()
    // get output
    double[][] centroids = clustering.getCentroids()
    double WCSS          = clustering.getWCSS()

    def groupCounts = clustering.getAssignment().toList().countBy { it.clusterId }
    //println("Assignments: " + groupCounts)
    assertEquals(750, groupCounts[0], "Cluster 0 should have 750 points")
    assertEquals(750, groupCounts[1], "Cluster 1 should have 750 points")
    assertEquals(750, groupCounts[2], "Cluster 2 should have 750 points")
    assertEquals(750, groupCounts[3], "Cluster 3 should have 750 points")

    Map<Integer, Matrix> clusters = clustering.getClustersById()
    clusters.each { id, matrix ->
      //println "Cluster $id has ${matrix.rowCount()} rows and ${matrix.columnCount()} columns"
      assertEquals(750, matrix.rowCount(), "Cluster $id should have 750 points")
    }

    // print centeroids
    // for (int i = 0; i < k; i++) println("(" + centroids[i][0] + ", " + centroids[i][1] + ")")
    assertEquals(4, centroids.length, "Centroids should be 4")

    println("The within-cluster sum-of-squares (WCSS) = " + WCSS)
    assertTrue(WCSS < 250 && WCSS > 230, "WCSS should be approximately 240")
  }

  @Test
  void testBuilderWithElbowEstimation() {
    double[][] data = gaussianClusters(750, 0.05) as double[][]
    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(data, GroupEstimator.CalculationMethod.ELBOW)
        .randomSeed(RANDOM_SEED)
        .build()
    println "Estimated number of clusters for elbow method: ${clustering.getCentroids().length}"
    println clustering.getTiming()
    assert clustering.getCentroids().length in 3..5
  }

  @Test
  void testBuilderWithRuleOfThumbEstimation() {
    double[][] data = gaussianClusters(750, 0.05) as double[][]
    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(data, GroupEstimator.CalculationMethod.RULE_OF_THUMB)
        .randomSeed(RANDOM_SEED)
        .build()
    println "Estimated number of clusters for Rule of thumb: ${clustering.getCentroids().length}"
    println clustering.getTiming()
    assert clustering.getCentroids().length <= (int)Math.max(2, Math.sqrt(data.length / 2.0d))
    assert clustering.getCentroids().length >= Math.max(2, (int) Math.round(Math.cbrt(data.length)))
  }
}
