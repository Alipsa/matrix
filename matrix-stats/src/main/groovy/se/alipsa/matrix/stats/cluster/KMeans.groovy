package se.alipsa.matrix.stats.cluster


import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

/**
 * K-Means clustering is an unsupervised machine learning algorithm that partitions n observations
 * into k clusters by minimizing the Within-Cluster Sum of Squares (WCSS). Each observation is
 * assigned to the cluster with the nearest centroid (cluster center).
 *
 * <p>This class provides a high-level API for performing K-Means clustering on Matrix data,
 * with support for automatic cluster number estimation and KMeans++ initialization for improved results.</p>
 *
 * <h3>What is K-Means?</h3>
 * <p>K-Means is a centroid-based clustering algorithm that:</p>
 * <ul>
 *   <li>Groups data points into k clusters based on feature similarity</li>
 *   <li>Minimizes the variance within each cluster (WCSS)</li>
 *   <li>Iteratively refines cluster assignments until convergence</li>
 *   <li>Works best with spherical, similarly-sized clusters</li>
 * </ul>
 *
 * <h3>When to Use K-Means</h3>
 * <ul>
 *   <li><strong>Customer Segmentation:</strong> Group customers by purchasing behavior</li>
 *   <li><strong>Image Compression:</strong> Reduce color palette by clustering similar colors</li>
 *   <li><strong>Anomaly Detection:</strong> Identify outliers as points far from cluster centroids</li>
 *   <li><strong>Feature Engineering:</strong> Create cluster-based features for other ML models</li>
 *   <li><strong>Document Clustering:</strong> Group similar documents or topics</li>
 * </ul>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>Centroids</strong></dt>
 *   <dd>The center point of each cluster, calculated as the mean of all assigned points.
 *   The algorithm iteratively updates centroids until they stabilize.</dd>
 *
 *   <dt><strong>WCSS (Within-Cluster Sum of Squares)</strong></dt>
 *   <dd>The sum of squared distances between each point and its cluster centroid.
 *   Lower WCSS indicates tighter, more cohesive clusters. Formula: WCSS = Σᵢ Σⱼ ||Cᵢ - Xⱼ||²</dd>
 *
 *   <dt><strong>Convergence</strong></dt>
 *   <dd>The algorithm stops when cluster assignments no longer change or when WCSS improvement
 *   falls below epsilon (typically 0.001). Multiple iterations are run to find the best clustering.</dd>
 *
 *   <dt><strong>KMeans++ Initialization</strong></dt>
 *   <dd>An improved initialization strategy that selects initial centroids using weighted probabilities
 *   proportional to distance from existing centroids. This produces better results than random initialization.</dd>
 * </dl>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.core.Matrix
 * import se.alipsa.matrix.stats.cluster.KMeans
 * import se.alipsa.matrix.stats.Normalize
 *
 * // Load and prepare data
 * Matrix m = Matrix.builder('Whiskey data')
 *   .data('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
 *   .build()
 *
 * // Select numeric features for clustering
 * List&lt;String&gt; features = m.columnNames() - 'Distillery'
 * features.each { m.convert(it, Double) }
 *
 * // Normalize features (recommended for K-Means)
 * m = Normalize.minMaxNorm(m)
 *
 * // Create KMeans instance
 * KMeans kmeans = new KMeans(m)
 *
 * // Option 1: Specify k explicitly
 * Matrix result = kmeans.fit(features, 3, 20)  // 3 clusters, 20 iterations
 * println "Execution time: ${kmeans.reportTime()}"
 *
 * // Option 2: Automatic k estimation using Elbow method
 * Matrix result = kmeans.fit(features, 20)  // Auto-estimate k, 20 iterations
 *
 * // Option 3: Specify estimation method
 * Matrix result = kmeans.fit(features, 20, GroupEstimator.CalculationMethod.ELBOW)
 * </pre>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Normalize features:</strong> Use Normalize.minMaxNorm() or Normalize.zScore() before clustering</li>
 *   <li><strong>Choose k wisely:</strong> Use the Elbow method or domain knowledge to select cluster count</li>
 *   <li><strong>Run multiple iterations:</strong> Use 20-50 iterations to avoid local optima</li>
 *   <li><strong>Check execution time:</strong> Use reportTime() to monitor performance</li>
 *   <li><strong>Validate results:</strong> Examine cluster sizes and characteristics for meaningful groupings</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 *   <li>MacQueen, J. (1967). "Some methods for classification and analysis of multivariate observations."
 *   Proceedings of the Fifth Berkeley Symposium on Mathematical Statistics and Probability, 1:281–297.</li>
 *   <li>Arthur, D. and Vassilvitskii, S. (2007). "k-means++: the advantages of careful seeding."
 *   SODA '07: Proceedings of the eighteenth annual ACM-SIAM symposium on Discrete algorithms, 1027–1035.</li>
 *   <li>Hartigan, J.A. and Wong, M.A. (1979). "Algorithm AS 136: A K-Means Clustering Algorithm."
 *   Journal of the Royal Statistical Society, Series C, 28(1): 100–108.</li>
 * </ul>
 *
 * @see KMeansPlusPlus
 * @see GroupEstimator
 * @see ClusteredPoint
 */

class KMeans {

  private Matrix matrix
  private KMeansPlusPlus clustering

  KMeans(Matrix matrix) {
    if (matrix == null || matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must contain data")
    }
    this.matrix = matrix
  }

  private double[][] extractPoints(List<String> columnNames) {
    List missingItems = columnNames - matrix.columnNames()

    if (missingItems) {
      throw new IllegalArgumentException("The following columns does not exist in the matrix: ${missingItems.join(', ')}")
    }

    Matrix m = matrix.selectColumns(columnNames)
    double[][] points = new double[m.rowCount()][m.columnCount()]
    m.eachWithIndex { row, i ->
      points[i] = ListConverter.toDoubleArray(row as List<? extends Number>)
    }
    return points
  }

  Matrix fit(List<String> columnNames, int k, int iterations, String columnName = "Group", boolean mutate = true) {
    double[][] points = extractPoints(columnNames)
    clustering = new KMeansPlusPlus.Builder(k, points)
        .iterations(iterations)
        .pp(true)
        .useEpsilon(true)
        .build()
    return addClusterColumn(clustering, columnName, mutate)
  }

  Matrix fit(List<String> columnNames, int iterations = 30, GroupEstimator.CalculationMethod method = GroupEstimator.CalculationMethod.ELBOW, String columnName = "Group", boolean mutate = true) {
    double[][] points = extractPoints(columnNames)
    clustering = new KMeansPlusPlus.Builder(points, method)
        .iterations(iterations)
        .pp(true)
        .useEpsilon(true)
        .build()
    return addClusterColumn(clustering, columnName, mutate)
  }

  private Matrix addClusterColumn(KMeansPlusPlus clustering, String columnName, boolean mutate) {
    List<Integer> clusterIds = clustering.assignment*.clusterId
    if (mutate) {
      return matrix.addColumn(columnName, Integer, clusterIds)
    } else {
      return matrix.clone().addColumn(columnName, Integer, clusterIds)
    }
  }

  String reportTime() {
    return clustering?.timing ?: "No clustering performed yet, call fit() first."
  }

  int getExecutionTimeMillis() {
    return clustering?.executionTimeMillis ?: -1
  }

}
