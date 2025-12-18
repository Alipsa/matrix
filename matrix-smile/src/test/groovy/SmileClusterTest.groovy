import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.ml.SmileCluster

import static org.junit.jupiter.api.Assertions.*

class SmileClusterTest {

  // Create clustered data with 3 clear groups
  private Matrix createClusteredData() {
    return Matrix.builder()
        .data(
            x: [1.0, 1.2, 1.5, 1.8,   // Cluster 0
                5.0, 5.2, 5.5, 5.8,   // Cluster 1
                9.0, 9.2, 9.5, 9.8],  // Cluster 2
            y: [1.1, 0.9, 1.3, 1.7,
                5.1, 4.9, 5.3, 5.7,
                9.1, 8.9, 9.3, 9.7]
        )
        .types([Double, Double])
        .build()
  }

  // Create data for DBSCAN with noise
  private Matrix createDBSCANData() {
    return Matrix.builder()
        .data(
            x: [1.0, 1.1, 1.2, 1.0,   // Dense cluster
                5.0, 5.1, 5.2, 5.0,   // Dense cluster
                10.0],                 // Noise point
            y: [1.0, 1.1, 1.0, 1.2,
                5.0, 5.1, 5.0, 5.2,
                10.0]
        )
        .types([Double, Double])
        .build()
  }

  // KMeans Tests

  @Test
  void testKMeansClustering() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)

    assertNotNull(cluster)
    assertEquals(3, cluster.numClusters)
    assertEquals(2, cluster.featureColumns.length)
  }

  @Test
  void testKMeansLabels() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    int[] labels = cluster.labels

    assertEquals(12, labels.length)
    // All labels should be valid cluster indices
    for (int label : labels) {
      assertTrue(label >= 0 && label < 3)
    }
  }

  @Test
  void testKMeansLabelsList() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    List<Integer> labels = cluster.labelsList

    assertEquals(12, labels.size())
  }

  @Test
  void testKMeansAddClusterColumn() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    Matrix withCluster = cluster.addClusterColumn(data)

    assertEquals(12, withCluster.rowCount())
    assertEquals(3, withCluster.columnCount())
    assertTrue(withCluster.columnNames().contains('cluster'))
  }

  @Test
  void testKMeansPredict() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)

    // Predict for new points
    Matrix newData = Matrix.builder()
        .data(
            x: [1.3, 5.3, 9.3],
            y: [1.3, 5.3, 9.3]
        )
        .types([Double, Double])
        .build()

    int[] predictions = cluster.predict(newData)

    assertEquals(3, predictions.length)
    // All predictions should be valid cluster indices
    for (int pred : predictions) {
      assertTrue(pred >= 0 && pred < 3)
    }
  }

  @Test
  void testKMeansCentroids() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    double[][] centroids = cluster.centroids

    assertNotNull(centroids)
    assertEquals(3, centroids.length) // 3 clusters
    assertEquals(2, centroids[0].length) // 2 features
  }

  @Test
  void testKMeansCentroidsMatrix() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    Matrix centroidsMatrix = cluster.centroidsMatrix

    assertNotNull(centroidsMatrix)
    assertEquals(3, centroidsMatrix.rowCount()) // 3 clusters
    assertEquals(2, centroidsMatrix.columnCount()) // 2 features
    assertTrue(centroidsMatrix.columnNames().contains('x'))
    assertTrue(centroidsMatrix.columnNames().contains('y'))
  }

  @Test
  void testKMeansClusterCounts() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    Matrix counts = cluster.clusterCounts()

    assertNotNull(counts)
    assertTrue(counts.columnNames().contains('cluster'))
    assertTrue(counts.columnNames().contains('count'))

    // Total should equal original data size
    int total = 0
    for (Object count : counts.column('count')) {
      total += (Integer) count
    }
    assertEquals(12, total)
  }

  @Test
  void testKMeansWithMaxIter() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3, 100)

    assertNotNull(cluster)
    assertEquals(3, cluster.numClusters)
  }

  @Test
  void testKMeansSilhouette() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    double silhouette = cluster.silhouette(data)

    // Well-separated clusters should have positive silhouette
    assertTrue(silhouette > 0, "Expected positive silhouette but got $silhouette")
  }

  // DBSCAN Tests

  @Test
  void testDBSCANClustering() {
    Matrix data = createDBSCANData()

    SmileCluster cluster = SmileCluster.dbscan(data, 2, 0.5)

    assertNotNull(cluster)
    assertTrue(cluster.numClusters >= 1)
  }

  @Test
  void testDBSCANLabels() {
    Matrix data = createDBSCANData()

    SmileCluster cluster = SmileCluster.dbscan(data, 2, 0.5)
    int[] labels = cluster.labels

    assertEquals(9, labels.length)
    // Some points may be noise (label Integer.MAX_VALUE) or in clusters
  }

  @Test
  void testDBSCANAddClusterColumn() {
    Matrix data = createDBSCANData()

    SmileCluster cluster = SmileCluster.dbscan(data, 2, 0.5)
    Matrix withCluster = cluster.addClusterColumn(data)

    assertEquals(9, withCluster.rowCount())
    assertEquals(3, withCluster.columnCount())
    assertTrue(withCluster.columnNames().contains('cluster'))
  }

  @Test
  void testDBSCANPredictThrowsException() {
    Matrix data = createDBSCANData()

    SmileCluster cluster = SmileCluster.dbscan(data, 2, 0.5)

    // DBSCAN doesn't support prediction
    assertThrows(UnsupportedOperationException) {
      cluster.predict(data)
    }
  }

  @Test
  void testDBSCANClusterCounts() {
    Matrix data = createDBSCANData()

    SmileCluster cluster = SmileCluster.dbscan(data, 2, 0.5)
    Matrix counts = cluster.clusterCounts()

    assertNotNull(counts)
    assertTrue(counts.columnNames().contains('cluster'))
    assertTrue(counts.columnNames().contains('count'))
  }

  // Edge Cases

  @Test
  void testKMeansTwoClusters() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, 1.1, 1.2, 5.0, 5.1, 5.2],
            y: [1.0, 1.1, 1.2, 5.0, 5.1, 5.2]
        )
        .types([Double, Double])
        .build()

    SmileCluster cluster = SmileCluster.kmeans(data, 2)

    assertEquals(2, cluster.numClusters)
    assertEquals(6, cluster.labels.length)
  }

  @Test
  void testGetFeatureColumns() {
    Matrix data = createClusteredData()

    SmileCluster cluster = SmileCluster.kmeans(data, 3)
    String[] features = cluster.featureColumns

    assertEquals(2, features.length)
    assertTrue(features.contains('x'))
    assertTrue(features.contains('y'))
  }
}
