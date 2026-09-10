package se.alipsa.matrix.stats.cluster

import groovy.transform.PackageScope

import se.alipsa.matrix.stats.util.DoubleArrayUtils
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * ClusteredPoint is a simple data structure that represents a data point assigned to a cluster
 * in K-Means clustering. It pairs a cluster identifier with the point's feature vector.
 *
 * <p>This class is an immutable value object used internally by {@link KMeansPlusPlus} to store
 * cluster assignment results. Each ClusteredPoint contains the cluster ID (0 to k-1) and an
 * immutable snapshot of the original n-dimensional point coordinates.</p>
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
 *     double[] coordinates = cp.point // one defensive copy for this operation
 *     println "Point ${Arrays.toString(coordinates)} → Cluster ${cp.clusterId}"
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
 *         double avgX = points.collect { it.coordinate(0) }.sum() / points.size()
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
 *   <dd>ClusteredPoint is immutable by design. It copies input coordinates and returns a copy
 *   from getPoint(), so neither the cluster assignment nor stored coordinates can be changed.</dd>
 * </dl>
 *
 * <h3>Implementation Details</h3>
 * <ul>
 *   <li><strong>Defensive:</strong> Copies coordinates at public construction and access boundaries</li>
 *   <li><strong>Thread Safe:</strong> Immutable design allows safe concurrent access</li>
 *   <li><strong>Simple Accessors:</strong> Provides cluster, dimension, scalar, and snapshot accessors</li>
 *   <li><strong>Validated:</strong> Public constructors reject null, empty, or non-finite coordinates;
 *   the package-scoped {@code internal} factory skips copying for the clustering hot path</li>
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
 *         double squaredDistance = 0.0
 *         for (int i = 0; i < cp.dimensions; i++) {
 *             double delta = cp.coordinate(i) - centroids[clusterId][i]
 *             squaredDistance += delta * delta
 *         }
 *         squaredDistance
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
  private static final String POINT_LABEL = 'point'

  final int clusterId
  private final double[] point

  /**
   * Creates a cluster assignment with a defensive copy of its coordinates.
   *
   * @param clusterId the zero-based cluster identifier
   * @param point the point coordinates
   * @throws IllegalArgumentException if point is null
   */
  ClusteredPoint(int clusterId, double[] point) {
    this(clusterId, point, true)
  }

  /**
   * Creates a cluster assignment from numeric coordinates.
   *
   * @param clusterId the zero-based cluster identifier
   * @param point the point coordinates
   * @throws IllegalArgumentException if point is null, empty, or contains an invalid value
   */
  ClusteredPoint(int clusterId, List<? extends Number> point) {
    this(clusterId, NumericConversion.toDoubleArray(point, POINT_LABEL), false)
  }

  private ClusteredPoint(int clusterId, double[] point, boolean defensiveCopy) {
    if (point == null) {
      throw new IllegalArgumentException('Point cannot be null')
    }
    this.clusterId = clusterId
    this.point = defensiveCopy ? DoubleArrayUtils.copy(point) : point
  }

  @PackageScope
  static ClusteredPoint internal(int clusterId, double[] point) {
    new ClusteredPoint(clusterId, point, false)
  }

  /**
   * Returns the zero-based cluster identifier.
   *
   * @return the cluster identifier
   */
  int getClusterId() { clusterId }

  /**
   * Returns a defensive copy of the point coordinates. Cache the returned array when reading
   * several coordinates in one operation to avoid repeated allocations.
   *
   * @return an independent copy of the point coordinates
   */
  double[] getPoint() { DoubleArrayUtils.copy(point) }

  /**
   * Returns one coordinate without allocating a defensive array copy.
   *
   * @param index the zero-based coordinate index
   * @return the coordinate value
   * @throws ArrayIndexOutOfBoundsException if index is outside {@code 0 <= index < dimensions}
   */
  double coordinate(int index) { point[index] }

  /**
   * Returns the number of coordinates without allocating a defensive array copy.
   *
   * @return the point dimensionality
   */
  int getDimensions() { point.length }

  @PackageScope
  double[] internalPoint() { point }

  /**
   * Returns the coordinates as immutable decimal values.
   *
   * @return immutable decimal coordinates
   */
  List<BigDecimal> getPointValues() {
    NumericConversion.toBigDecimalList(point, POINT_LABEL)
  }
}
