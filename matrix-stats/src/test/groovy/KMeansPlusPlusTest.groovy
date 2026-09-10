import static KMeansTestData.RANDOM_SEED
import static KMeansTestData.gaussianClusters
import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.cluster.ClusteredPoint
import se.alipsa.matrix.stats.cluster.GroupEstimator
import se.alipsa.matrix.stats.cluster.KMeansPlusPlus

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
    double wcss          = clustering.getWCSS()

    def groupCounts = clustering.getAssignment().toList().countBy { it.clusterId }
    //println("Assignments: " + groupCounts)
    assertEquals(750, groupCounts[0], 'Cluster 0 should have 750 points')
    assertEquals(750, groupCounts[1], 'Cluster 1 should have 750 points')
    assertEquals(750, groupCounts[2], 'Cluster 2 should have 750 points')
    assertEquals(750, groupCounts[3], 'Cluster 3 should have 750 points')

    Map<Integer, Matrix> clusters = clustering.getClustersById()
    clusters.each { id, matrix ->
      //println "Cluster $id has ${matrix.rowCount()} rows and ${matrix.columnCount()} columns"
      assertEquals(750, matrix.rowCount(), "Cluster $id should have 750 points")
    }

    // print centeroids
    // for (int i = 0; i < k; i++) println("(" + centroids[i][0] + ", " + centroids[i][1] + ")")
    assertEquals(4, centroids.length, 'Centroids should be 4')

    println('The within-cluster sum-of-squares (WCSS) = ' + wcss)
    assertTrue(wcss < 250 && wcss > 230, 'WCSS should be approximately 240')
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

  @Test
  void testGroovyFacingBuilderAndValueAccessors() {
    List<List<BigDecimal>> points = gaussianClusters(25, 0.05).collectNested { Double value ->
      BigDecimal.valueOf(value)
    } as List<List<BigDecimal>>

    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(4, points)
        .iterations(10)
        .pp(true)
        .randomSeed(RANDOM_SEED)
        .build()

    assertEquals(points.size(), clustering.assignments.size())
    assertEquals(4, clustering.centroidValues.size())
    assertEquals(2, clustering.centroidValues[0].size())
    assertEquals(2, clustering.assignments[0].pointValues.size())
    assertTrue(clustering.wcssValue > 0)
    assertTrue(clustering.executionTimeValue >= 0)
  }

  @Test
  void testBuilderAcceptsNumberEpsilon() {
    List<List<BigDecimal>> points = gaussianClusters(20, 0.05).collectNested { Double value ->
      BigDecimal.valueOf(value)
    } as List<List<BigDecimal>>

    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(4, points)
        .iterations(5)
        .epsilon(0.003G)
        .useEpsilon(true)
        .randomSeed(RANDOM_SEED)
        .build()

    assertEquals(points.size(), clustering.assignments.size())
  }

  @Test
  void testBasicInitializationDoesNotMutateInput() {
    double[][] points = [[0.0], [1.0], [2.0], [3.0]] as double[][]
    double[][] original = points.collect { double[] row -> row.clone() as double[] } as double[][]

    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(2, points)
      .iterations(1)
      .pp(false)
      .randomSeed(7)
      .build()

    for (int i = 0; i < points.length; i++) {
      assertArrayEquals(original[i], points[i])
    }
    assertEquals(
      original*.toList().countBy { it },
      clustering.assignment.collect { it.point.toList() }.countBy { it }
    )
  }

  @Test
  void testSeededPlusPlusResultsRemainReproducible() {
    double[][] points = [[0.0], [1.0], [4.0], [5.0]] as double[][]

    KMeansPlusPlus first = new KMeansPlusPlus.Builder(2, points)
      .iterations(2)
      .pp(true)
      .randomSeed(19)
      .build()
    KMeansPlusPlus second = new KMeansPlusPlus.Builder(2, points)
      .iterations(2)
      .pp(true)
      .randomSeed(19)
      .build()

    assertEquals(first.wcss, second.wcss)
    for (int i = 0; i < first.centroids.length; i++) {
      assertArrayEquals(first.centroids[i], second.centroids[i])
    }
    assertEquals(
      first.assignment.collect { [it.clusterId, it.point.toList()] },
      second.assignment.collect { [it.clusterId, it.point.toList()] }
    )
  }

  @Test
  void testReturnedResultsDoNotExposeInputOrModelArrays() {
    double[][] points = [[0.0, 1.0], [1.0, 2.0], [8.0, 9.0], [9.0, 10.0]] as double[][]
    double[][] original = points.collect { double[] row -> row.clone() as double[] } as double[][]
    KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(2, points)
      .iterations(1)
      .pp(true)
      .randomSeed(23)
      .build()

    def assignment = clustering.assignment
    double assignedValue = assignment[0].point[0]
    assignment[0].point[0] = 999.0

    assertEquals(assignedValue, clustering.assignment[0].point[0])
    int assignedCluster = assignment[0].clusterId
    assignment[0] = new ClusteredPoint(999, [999.0d, 999.0d] as double[])
    assertEquals(assignedCluster, clustering.assignment[0].clusterId)
    assertSame(clustering.assignments, clustering.assignments)
    for (int i = 0; i < points.length; i++) {
      assertArrayEquals(original[i], points[i])
    }

    double[][] centroids = clustering.centroids
    double[][] expectedCentroids = centroids.collect { double[] row -> row.clone() as double[] } as double[][]
    centroids[0][0] = 999.0
    centroids[1] = [999.0, 999.0] as double[]
    double[][] centroidsAgain = clustering.centroids

    assertNotSame(centroids, centroidsAgain)
    for (int i = 0; i < expectedCentroids.length; i++) {
      assertNotSame(centroids[i], centroidsAgain[i])
      assertArrayEquals(expectedCentroids[i], centroidsAgain[i])
    }
  }

  @Test
  void testClusteredPointIsImmutableAtPublicBoundaries() {
    double[] source = [1.0d, 2.0d] as double[]
    ClusteredPoint point = new ClusteredPoint(3, source)

    source[0] = 99.0d
    double[] returned = point.point
    returned[1] = 99.0d

    assertArrayEquals([1.0d, 2.0d] as double[], point.point)
    assertEquals(1.0d, point.coordinate(0))
    assertEquals(2, point.dimensions)
    assertThrows(ArrayIndexOutOfBoundsException) {
      point.coordinate(point.dimensions)
    }
  }

  @Test
  void testClusteredPointRejectsNullCoordinates() {
    IllegalArgumentException arrayException = assertThrows(IllegalArgumentException) {
      new ClusteredPoint(3, null as double[])
    }
    assertEquals('Point cannot be null', arrayException.message)

    IllegalArgumentException listException = assertThrows(IllegalArgumentException) {
      new ClusteredPoint(3, null as List<Number>)
    }
    assertEquals('Point cannot be null', listException.message)
  }
}
