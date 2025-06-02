import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.cluster.GroupEstimator
import se.alipsa.matrix.stats.cluster.KMeans

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class KMeansTest {

  @Test
  void testKMeansWithFixedK() {
    Matrix m = Matrix.builder("test").columns([
        "x": (0..<3000).collect { it % 2 == 0 ? 0.1 : 0.9 },
        "y": (0..<3000).collect { it % 3 == 0 ? 0.1 : 0.9 }
    ]).build()

    KMeans kmeans = new KMeans(m)
    List<String> features = ["x", "y"]
    Matrix clustered = kmeans.fit(features, 2)

    assertEquals(2, clustered.column("Group").toSet().size(), "Should have two clusters")
    assertEquals(3000, clustered.rowCount(), "Should have same row count after clustering")
    assertTrue(clustered.columnNames().contains("Group"), "Matrix should contain 'Group' column")
  }

  @Test
  void testKMeansWithAutoEstimation() {
    Matrix m = Matrix.builder("test").columns([
        "x": (0..<3000).collect { it % 4 == 0 ? 0.1 : 0.9 },
        "y": (0..<3000).collect { it % 5 == 0 ? 0.1 : 0.9 }
    ]).build()

    KMeans kmeans = new KMeans(m)
    List<String> features = ["x", "y"]
    Matrix clustered = kmeans.fit(features, 10, GroupEstimator.CalculationMethod.ELBOW)

    assertTrue(clustered.column("Group").toSet().size() >= 2, "Should have at least 2 clusters")
    assertEquals(3000, clustered.rowCount(), "Should have same row count after clustering")
  }

  @Test
  void testNoMutationReturnsNewMatrix() {
    Matrix m = Matrix.builder("test").columns([
        "x": [0.1, 0.9, 0.1, 0.9],
        "y": [0.1, 0.9, 0.9, 0.1],
        "z": [0.42, 0.73, 0.64, 0.45] // added column
    ]).types([double]*3)
        .build()

    KMeans kmeans = new KMeans(m)
    List<String> features = ["x", "y", "z"]
    Matrix result = kmeans.fit(features, 2, 20, "clusterGroup", false)

    println result.content()
    // Assertions
    assertNotSame(m, result, "Should return a new Matrix when mutate = false")
    assertEquals(m.rowCount(), result.rowCount())
    assertTrue(result.columnNames().contains("clusterGroup"))
    assertEquals(m.column("z"), result.column("z"), "z column should be preserved in result")
    assertTrue(result.column("clusterGroup").every { it instanceof Integer }, "Group values should be integers")
  }

  @Test
  void testInvalidFeatureThrows() {
    Matrix m = Matrix.builder("test").columns([
        "a": [1, 2, 3],
        "b": [4, 5, 6]
    ]).build()

    KMeans kmeans = new KMeans(m)

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException) {
      kmeans.fit(["c"], 2)
    }
    assertTrue(thrown.message.contains("The following columns does not exist in the matrix: c"))
  }

  @Test
  void testWhiskeyDataClustering() {
    def m = Matrix.builder()
        .data('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
        .build()
        .dropColumns('RowID')

    List<String> features = m.columnNames() - 'Distillery'
    features.each {
      m.convert(it, Double)
    }
    m = Normalize.minMaxNorm(m)
    KMeans kmeans = new KMeans(m)
    Matrix clustered = kmeans.fit(features, 3, 50, "Group", false)
    println "testWhiskeyDataClustering: " + kmeans.reportTime()
    println Stat.countBy(clustered, 'Group').content()
    assertEquals(3, clustered.column("Group").toSet().size(), "Should have three clusters")
    assertEquals(m.rowCount(), clustered.rowCount(), "Should have same row count after clustering")
    assertTrue(clustered.columnNames().contains("Group"), "Matrix should contain 'Group' column")
  }
}
