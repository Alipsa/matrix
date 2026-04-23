package cluster

import static KMeansTestData.RANDOM_SEED
import static KMeansTestData.gaussianClusters
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.cluster.GroupEstimator

class GroupEstimatorTest {

  // Two clearly separated clusters
  private static final List<List<Double>> TWO_CLUSTER_POINTS = [
      [0.0, 0.0], [0.1, 0.05], [0.05, 0.1],
      [10.0, 10.0], [10.1, 9.95], [9.95, 10.1]
  ]

  @Test
  void testEstimateNumberOfGroupsElbow() {
    double[][] data = gaussianClusters(750, 0.05, RANDOM_SEED) as double[][]
    GroupEstimator estimator = new GroupEstimator(GroupEstimator.CalculationMethod.ELBOW)
    int k = estimator.estimateNumberOfGroups(data)
    assertTrue(k in 3..5, "Elbow method should estimate 3–5 clusters for a 4-cluster dataset, got $k")
  }

  @Test
  void testEstimateNumberOfGroupsRuleOfThumb() {
    double[][] data = gaussianClusters(750, 0.05, RANDOM_SEED) as double[][]
    GroupEstimator estimator = new GroupEstimator(GroupEstimator.CalculationMethod.RULE_OF_THUMB)
    int k = estimator.estimateNumberOfGroups(data)
    assertTrue(k >= 2, "Rule-of-thumb estimate should be at least 2, got $k")
  }

  @Test
  void testEstimateNumberOfGroupsGroovyFacing() {
    GroupEstimator estimator = new GroupEstimator(GroupEstimator.CalculationMethod.ELBOW)
    int k = estimator.estimateNumberOfGroups(TWO_CLUSTER_POINTS)
    assertEquals(2, k, "Elbow method should estimate 2 clusters for two clearly separated clusters, got $k")
  }

  @Test
  void testEstimateKByElbowDirect() {
    double[][] data = TWO_CLUSTER_POINTS as double[][]
    int k = GroupEstimator.estimateKByElbow(data)
    assertEquals(2, k, "Elbow method should find 2 clusters for clearly separated data, got $k")
  }

  @Test
  void testEstimateKByElbowGroovyFacing() {
    int k = GroupEstimator.estimateKByElbow(TWO_CLUSTER_POINTS)
    assertEquals(2, k, "Groovy-facing elbow method should find 2 clusters, got $k")
  }

  @Test
  void testEstimateKByElbowCustomMaxK() {
    double[][] data = gaussianClusters(100, 0.05, RANDOM_SEED) as double[][]
    int k = GroupEstimator.estimateKByElbow(data, 6, 5)
    assertTrue(k in 2..6, "Elbow method with maxK=6 should return a value in 2..6, got $k")
  }

  @Test
  void testEstimateNumberOfGroupsTooFewPoints() {
    double[][] onePoint = [[1.0, 2.0]] as double[][]
    GroupEstimator estimator = new GroupEstimator()
    assertThrows(IllegalArgumentException) { estimator.estimateNumberOfGroups(onePoint) }
  }

  @Test
  void testEstimateKByElbowTooFewDistinctPoints() {
    // All identical points → only 1 distinct point
    double[][] data = [[1.0, 1.0], [1.0, 1.0], [1.0, 1.0]] as double[][]
    assertThrows(IllegalArgumentException) { GroupEstimator.estimateKByElbow(data) }
  }
}
