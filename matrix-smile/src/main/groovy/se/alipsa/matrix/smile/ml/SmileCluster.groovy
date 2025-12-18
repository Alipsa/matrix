package se.alipsa.matrix.smile.ml

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import smile.clustering.*

/**
 * Wrapper for Smile clustering algorithms providing a Matrix-friendly API.
 * Supports KMeans and DBSCAN clustering.
 */
@CompileStatic
class SmileCluster {

  private final CentroidClustering<double[], double[]> centroidModel
  private final int[] clusterLabels
  private final String[] featureColumns
  private final int numClusters

  private SmileCluster(CentroidClustering<double[], double[]> centroidModel, int[] clusterLabels, String[] featureColumns, int numClusters) {
    this.centroidModel = centroidModel
    this.clusterLabels = clusterLabels
    this.featureColumns = featureColumns
    this.numClusters = numClusters
  }

  /**
   * Perform K-Means clustering.
   *
   * @param matrix the data to cluster
   * @param k the number of clusters
   * @return a SmileCluster with the clustering result
   */
  static SmileCluster kmeans(Matrix matrix, int k) {
    return kmeans(matrix, k, 100)
  }

  /**
   * Perform K-Means clustering with specified max iterations.
   *
   * @param matrix the data to cluster
   * @param k the number of clusters
   * @param maxIter the maximum number of iterations
   * @return a SmileCluster with the clustering result
   */
  static SmileCluster kmeans(Matrix matrix, int k, int maxIter) {
    String[] featureColumns = matrix.columnNames() as String[]
    double[][] data = matrixToArray(matrix)

    CentroidClustering<double[], double[]> model = KMeans.fit(data, k, maxIter)

    return new SmileCluster(model, model.group(), featureColumns, k)
  }

  /**
   * Perform DBSCAN clustering (Density-Based Spatial Clustering).
   *
   * @param matrix the data to cluster
   * @param minPts the minimum number of points required in a neighborhood
   * @param radius the neighborhood radius
   * @return a SmileCluster with the clustering result
   */
  static SmileCluster dbscan(Matrix matrix, int minPts, double radius) {
    String[] featureColumns = matrix.columnNames() as String[]
    double[][] data = matrixToArray(matrix)

    DBSCAN<double[]> model = DBSCAN.fit(data, minPts, radius)
    int[] labels = model.group()

    return new SmileCluster(null, labels, featureColumns, model.k())
  }

  /**
   * Get the cluster labels for the training data.
   *
   * @return array of cluster labels (0-based, outliers may be -1 or MAX_VALUE for DBSCAN)
   */
  int[] getLabels() {
    return clusterLabels
  }

  /**
   * Get the number of clusters.
   *
   * @return the number of clusters
   */
  int getNumClusters() {
    return numClusters
  }

  /**
   * Get cluster labels as a list.
   *
   * @return list of cluster labels
   */
  List<Integer> getLabelsList() {
    List<Integer> result = new ArrayList<>(clusterLabels.length)
    for (int label : clusterLabels) {
      result.add(label)
    }
    return result
  }

  /**
   * Add cluster labels to the original matrix.
   *
   * @param matrix the original data
   * @return a new Matrix with a 'cluster' column added
   */
  Matrix addClusterColumn(Matrix matrix) {
    List<Integer> labels = getLabelsList()

    Map<String, List<?>> data = new LinkedHashMap<>()
    for (String col : matrix.columnNames()) {
      data.put(col, matrix.column(col))
    }
    data.put('cluster', labels)

    List<Class<?>> types = new ArrayList<>(matrix.types())
    types.add(Integer)

    return Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Predict cluster for new data points.
   * Note: Only available for centroid-based clustering (KMeans).
   *
   * @param matrix the data to predict
   * @return array of cluster labels
   */
  int[] predict(Matrix matrix) {
    if (centroidModel == null) {
      throw new UnsupportedOperationException("Prediction is not supported for this clustering algorithm")
    }

    double[][] data = matrixToArray(matrix)
    int[] predictions = new int[data.length]

    for (int i = 0; i < data.length; i++) {
      predictions[i] = centroidModel.predict(data[i])
    }

    return predictions
  }

  /**
   * Get cluster centroids.
   * Note: Only available for centroid-based clustering (KMeans).
   *
   * @return the cluster centroids or null if not available
   */
  double[][] getCentroids() {
    if (centroidModel != null) {
      return centroidModel.centers()
    }
    return null
  }

  /**
   * Get cluster centroids as a Matrix.
   * Note: Only available for centroid-based clustering (KMeans).
   *
   * @return a Matrix containing the cluster centroids
   */
  Matrix getCentroidsMatrix() {
    double[][] centroids = getCentroids()
    if (centroids == null) {
      return null
    }

    Map<String, List<?>> data = new LinkedHashMap<>()
    for (int j = 0; j < featureColumns.length; j++) {
      List<Double> col = new ArrayList<>(centroids.length)
      for (int i = 0; i < centroids.length; i++) {
        col.add(centroids[i][j])
      }
      data.put(featureColumns[j], col)
    }

    List<Class<?>> types = new ArrayList<>()
    for (int i = 0; i < featureColumns.length; i++) {
      types.add(Double)
    }

    return Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Get the count of points in each cluster.
   *
   * @return a Matrix with cluster counts
   */
  Matrix clusterCounts() {
    Map<Integer, Integer> counts = new LinkedHashMap<>()

    for (int label : clusterLabels) {
      counts.merge(label, 1) { old, v -> old + v }
    }

    List<Integer> clusters = new ArrayList<>(counts.keySet())
    clusters.sort()

    List<Integer> countList = new ArrayList<>()
    for (int cluster : clusters) {
      countList.add(counts.get(cluster))
    }

    return Matrix.builder()
        .data(
            cluster: clusters,
            count: countList
        )
        .types([Integer, Integer])
        .build()
  }

  /**
   * Get the feature column names.
   */
  String[] getFeatureColumns() {
    return featureColumns
  }

  /**
   * Get the silhouette coefficient (cluster quality metric).
   * Higher values indicate better-defined clusters.
   *
   * @param matrix the original data
   * @return the silhouette coefficient (-1 to 1)
   */
  double silhouette(Matrix matrix) {
    double[][] data = matrixToArray(matrix)

    // Simple silhouette calculation
    return calculateSilhouette(data, clusterLabels)
  }

  // Helper methods

  private static double[][] matrixToArray(Matrix matrix) {
    int rows = matrix.rowCount()
    int cols = matrix.columnCount()
    double[][] result = new double[rows][cols]

    for (int j = 0; j < cols; j++) {
      List<?> column = matrix.column(j)
      for (int i = 0; i < rows; i++) {
        Object val = column.get(i)
        result[i][j] = val != null ? ((Number) val).doubleValue() : 0.0d
      }
    }

    return result
  }

  private static double calculateSilhouette(double[][] data, int[] labels) {
    int n = data.length
    if (n < 2) return 0.0d

    // Find unique clusters (excluding outliers which may be MAX_VALUE)
    Set<Integer> uniqueClusters = new HashSet<>()
    for (int label : labels) {
      if (label >= 0 && label < Integer.MAX_VALUE) {
        uniqueClusters.add(label)
      }
    }

    if (uniqueClusters.size() < 2) return 0.0d

    double totalSilhouette = 0.0d
    int validPoints = 0

    for (int i = 0; i < n; i++) {
      int clusterI = labels[i]
      // Skip outliers
      if (clusterI < 0 || clusterI >= Integer.MAX_VALUE) continue

      // Calculate mean intra-cluster distance (a)
      double a = meanDistanceToCluster(data, labels, i, clusterI)

      // Calculate minimum mean inter-cluster distance (b)
      double b = Double.MAX_VALUE
      for (int cluster : uniqueClusters) {
        if (cluster != clusterI) {
          double dist = meanDistanceToCluster(data, labels, i, cluster)
          if (dist < b) {
            b = dist
          }
        }
      }

      // Silhouette for this point
      if (b < Double.MAX_VALUE) {
        double s = (b - a) / Math.max(a, b)
        totalSilhouette += s
        validPoints++
      }
    }

    return validPoints > 0 ? totalSilhouette / validPoints : 0.0d
  }

  private static double meanDistanceToCluster(double[][] data, int[] labels, int pointIndex, int cluster) {
    double sumDist = 0.0d
    int count = 0

    for (int i = 0; i < data.length; i++) {
      if (labels[i] == cluster && i != pointIndex) {
        sumDist += euclideanDistance(data[pointIndex], data[i])
        count++
      }
    }

    return count > 0 ? sumDist / count : 0.0d
  }

  private static double euclideanDistance(double[] a, double[] b) {
    double sum = 0.0d
    for (int i = 0; i < a.length; i++) {
      double diff = a[i] - b[i]
      sum += diff * diff
    }
    return Math.sqrt(sum)
  }
}
