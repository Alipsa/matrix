import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
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
        "y": [0.1, 0.9, 0.9, 0.1]
    ]).build()

    KMeans kmeans = new KMeans(m)
    List<String> features = ["x", "y"]
    Matrix result = kmeans.fit(features, 2, 10, "clusterGroup", false)

    assertNotSame(m, result, "Should return a new Matrix when mutate = false")
    assertEquals(m.rowCount(), result.rowCount())
    assertTrue(result.columnNames().contains("clusterGroup"))
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
    assertTrue(thrown.message.contains("Matrix does not contain column: c"))
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
    KMeans kmeans = new KMeans(m)
    Matrix clustered = kmeans.fit(features, 3, 50, "Group", false)

    //println clustered.content()
    assertEquals(3, clustered.column("Group").toSet().size(), "Should have three clusters")
    assertEquals(6, clustered.rowCount(), "Should have same row count after clustering")
    assertTrue(clustered.columnNames().contains("Group"), "Matrix should contain 'Group' column")
  }
}
