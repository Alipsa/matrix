package se.alipsa.matrix.stats.cluster

/**
 * ClusteredPoint is a simple data structure that represents a data point assigned to a cluster
 * in K-Means clustering. It pairs a cluster identifier with the point's feature vector.
 *
 * <p>This class is an immutable value object used internally by {@link KMeansPlusPlus} to store
 * cluster assignment results. Each ClusteredPoint contains the cluster ID (0 to k-1) and the
 * original n-dimensional point coordinates.</p>
 *
 * <h3>What is a ClusteredPoint?</h3>
 * <p>In K-Means clustering, every data point is assigned to exactly one cluster. ClusteredPoint
 * captures this assignment by storing:</p>
 * <ul>
 *   <li><strong>clusterId:</strong> The cluster this point belongs to (integer from 0 to k-1)</li>
 *   <li><strong>point:</strong> The original feature vector (n-dimensional coordinates as double[])</li>
 * </ul>
 *
 * <h3>When to Use ClusteredPoint</h3>
 * <ul>
 *   <li><strong>Post-Processing:</strong> Iterate through clustering results to analyze individual assignments</li>
 *   <li><strong>Custom Analysis:</strong> Calculate cluster-specific statistics or metrics</li>
 *   <li><strong>Visualization:</strong> Color or label points by their cluster ID</li>
 *   <li><strong>Validation:</strong> Examine specific points and their cluster assignments</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.stats.cluster.KMeansPlusPlus
 * import se.alipsa.matrix.stats.cluster.ClusteredPoint
 *
 * // Perform clustering
 * double[][] points = [[1.0, 2.0], [1.5, 1.8], [5.0, 8.0], [8.0, 8.0]]
 * KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(2, points)
 *     .iterations(20)
 *     .pp(true)
 *     .build()
 *
 * // Access cluster assignments
 * ClusteredPoint[] assignments = clustering.getAssignment()
 *
 * // Examine each point's cluster
 * assignments.each { cp ->
 *     println "Point ${Arrays.toString(cp.point)} → Cluster ${cp.clusterId}"
 * }
 *
 * // Find all points in cluster 0
 * List&lt;double[]&gt; cluster0Points = assignments.findAll { it.clusterId == 0 }
 *                                              .collect { it.point }
 * println "Cluster 0 has ${cluster0Points.size()} points"
 *
 * // Calculate cluster-specific statistics
 * Map&lt;Integer, Double&gt; clusterMeans = assignments.groupBy { it.clusterId }
 *     .collectEntries { clusterId, points ->
 *         double avgX = points.collect { it.point[0] }.sum() / points.size()
 *         [clusterId, avgX]
 *     }
 * </pre>
 *
 * <h3>Integration with KMeansPlusPlus</h3>
 * <pre>
 * import se.alipsa.matrix.core.Matrix
 *
 * // Get assignments as ClusteredPoint array
 * ClusteredPoint[] assignments = clustering.getAssignment()
 *
 * // Or get clusters grouped by ID as Matrix objects
 * Map&lt;Integer, Matrix&gt; clusterMatrices = clustering.getClustersById()
 * clusterMatrices.each { clusterId, matrix ->
 *     println "Cluster $clusterId size: ${matrix.rowCount()}"
 * }
 * </pre>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>Cluster Assignment</strong></dt>
 *   <dd>Each point is assigned to the cluster whose centroid is nearest (using L1 or L2 distance).
 *   The clusterId field stores this assignment (0-indexed).</dd>
 *
 *   <dt><strong>Feature Vector</strong></dt>
 *   <dd>The point field contains the original coordinates in feature space. For example,
 *   a point [3.5, 2.1, 4.8] represents a 3-dimensional data point.</dd>
 *
 *   <dt><strong>Immutability</strong></dt>
 *   <dd>ClusteredPoint is immutable by design (final fields). Once created, the cluster
 *   assignment and point coordinates cannot be changed, ensuring thread safety.</dd>
 * </dl>
 *
 * <h3>Implementation Details</h3>
 * <ul>
 *   <li><strong>Memory Efficient:</strong> Stores only clusterId (int) and point reference (double[])</li>
 *   <li><strong>Thread Safe:</strong> Immutable design allows safe concurrent access</li>
 *   <li><strong>Simple Accessors:</strong> Provides getClusterId() and getPoint() methods</li>
 *   <li><strong>No Validation:</strong> Assumes valid inputs from KMeansPlusPlus algorithm</li>
 * </ul>
 *
 * <h3>Common Patterns</h3>
 * <pre>
 * // Count points per cluster
 * Map&lt;Integer, Integer&gt; clusterSizes = assignments.groupBy { it.clusterId }
 *                                                 .collectEntries { k, v -> [k, v.size()] }
 *
 * // Find points closest to centroids (representative points)
 * double[][] centroids = clustering.getCentroids()
 * assignments.groupBy { it.clusterId }.each { clusterId, points ->
 *     ClusteredPoint closest = points.min { cp ->
 *         distance(cp.point, centroids[clusterId])
 *     }
 *     println "Representative of cluster $clusterId: ${Arrays.toString(closest.point)}"
 * }
 *
 * // Export to CSV format
 * assignments.each { cp ->
 *     String coords = cp.point.join(',')
 *     println "${cp.clusterId},$coords"
 * }
 * </pre>
 *
 * @see KMeansPlusPlus
 * @see KMeans
 */

class ClusteredPoint {
  final int clusterId
  final double[] point

  ClusteredPoint(int clusterId, double[] point) {
    this.clusterId = clusterId
    this.point = point
  }

  int getClusterId() { clusterId }
  double[] getPoint() { point }
}
