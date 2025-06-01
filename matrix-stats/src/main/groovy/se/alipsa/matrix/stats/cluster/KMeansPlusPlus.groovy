package se.alipsa.matrix.stats.cluster

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * KMeans clustering algorithm. Based on <a href='https://github.com/JasonAltschuler/KMeansPlusPlus'>Jason Altschuler's implementation</a>
 * (with explicit permission).
 * <p>
 * The K-Means algorithm is a method of vector quantization, originally from signal processing,
 * that is popular for cluster analysis in data mining.
 * </p>
 * <p>
 * The algorithm partitions <code>n</code> observations into <code>k</code> clusters,
 * such that each observation belongs to the cluster with the nearest mean (centroid),
 * which serves as a prototype of the cluster.
 * </p>
 *
 * <h3>Example usage:</h3>
 * <pre><code>
 *   import se.alipsa.matrix.stats.cluster.KMeans
 *   import se.alipsa.matrix.core.Matrix
 *
 *   Matrix m = Matrix.builder("Whiskey data")
 *                   .data("https://www.niss.org/sites/default/files/ScotchWhisky01.txt")
 *                   .build()
 *   KMeans kmeans = new KMeans(m)
 *   Matrix clustered = kmeans.fit(features, 3, 20)
 * </code></pre>
 *
 * <h3>Purpose</h3>
 * <ul>
 *   <li><strong>Clustering:</strong> Groups n-dimensional points.</li>
 *   <li><strong>Algorithm:</strong> KMeans++, an enhanced version of classic K-Means.</li>
 *   <li><strong>Objective:</strong> Minimize Within-Cluster Sum of Squares (WCSS).</li>
 * </ul>
 *
 * <h3>Algorithm Steps</h3>
 * <ol>
 *   <li>Select <code>k</code> initial centroids (either randomly or via KMeans++).</li>
 *   <li>Iteratively perform:
 *     <ul>
 *       <li><strong>Assignment step:</strong> assign each point to the nearest centroid.</li>
 *       <li><strong>Update step:</strong> recompute centroids as the mean of assigned points.</li>
 *     </ul>
 *   </li>
 *   <li>Stop when improvement in WCSS is smaller than <code>epsilon</code> (or zero if not using epsilon).</li>
 *   <li>Return the best clustering out of multiple runs (lowest WCSS).</li>
 * </ol>
 *
 * <h3>WCSS (Within-Cluster Sum of Squares)</h3>
 * WCSS = Σᵢ Σⱼ ||Cᵢ - Xⱼ||², where:
 * <ul>
 *   <li>Cᵢ = the i-th centroid</li>
 *   <li>Xⱼ = data point assigned to centroid Cᵢ</li>
 * </ul>
 *
 * <h3>KMeans++ Initialization</h3>
 * Selects the next centroid using weighted probabilities proportional to the squared distance from the nearest existing centroid.
 *
 * <h3>Parameters</h3>
 * <strong>Required:</strong>
 * <ul>
 *   <li><code>int k</code>: number of clusters</li>
 *   <li><code>double[][] points</code>: input data</li>
 * </ul>
 * <strong>Optional:</strong>
 * <ul>
 *   <li><code>int iterations</code> (default 50): number of runs to find best clustering</li>
 *   <li><code>boolean pp</code> (default true): use KMeans++ initialization</li>
 *   <li><code>double epsilon</code> (default .001): WCSS improvement threshold</li>
 *   <li><code>boolean useEpsilon</code> (default true)</li>
 * </ul>
 *
 * <h3>Output</h3>
 * <ul>
 *   <li><code>double[][]</code> centroids</li>
 *   <li><code>ClusteredPoint[]</code> assignment</li>
 *   <li><code>double</code> WCSS</li>
 * </ul>
 *
 * @author Jason Altschuler
 * @author Per Nyfelt
 */
@CompileStatic
class KMeansPlusPlus {
/***********************************************************************
 * Data structures
 **********************************************************************/
  // user-defined parameters
  private int k                // number of centroids
  private double[][] points    // n-dimensional data points.

  // optional parameters
  private int iterations       // number of times to repeat the clustering. Choose run with lowest WCSS
  private boolean pp           // true --> KMeans++. false --> basic random sampling
  private double epsilon       // stops running when improvement in error < epsilon
  private boolean useEpsilon;  // true  --> stop running when marginal improvement in WCSS < epsilon
  // false --> stop running when 0 improvement
  private boolean L1norm       // true --> L1 norm to calculate distance; false --> L2 norm

  // calculated from dimension of points[][]
  private int m                // number of data points   (# of pixels for PhenoRipper)
  private int n                // number of dimensions    (# of channels for PhenoRipper)

  // output
  private double[][] centroids // position vectors of centroids                      dim(2): (k) by (number of channels)
  private ClusteredPoint[] assignment // assigns each point to nearest centroid [0, k-1]    dim(1): (number of pixels)
  private double WCSS          // within-cluster sum-of-squares. Cost function to minimize

  // timing information
  private long start
  private long end

  /***********************************************************************
   * Constructors
   **********************************************************************/

  /**
   * Empty constructor is private to ensure that clients have to use the
   * Builder inner class to create a KMeans object.
   */
  private KMeansPlusPlus() {}

  /**
   * The proper way to construct a KMeans object: from an inner class object.
   * @param builder See inner class named Builder
   */
  private KMeansPlusPlus(Builder builder) {
    // start timing
    start = System.currentTimeMillis()

    // use information from builder
    k = builder.k
    points = builder.points
    iterations = builder.iterations
    pp = builder.pp
    epsilon = builder.epsilon
    useEpsilon = builder.useEpsilon
    L1norm = builder.L1norm

    // get dimensions to set last 2 fields
    m = points.length
    n = points[0].length

    // run KMeans++ clustering algorithm
    run()

    end = System.currentTimeMillis()
  }

  /**
   * Builder class for configuring and constructing a {@link KMeansPlusPlus} instance.
   * Provides a fluent API to customize parameters such as the number of iterations,
   * distance metric, initialization method, and stopping condition.
   */
  static class Builder {
    // required
    private final int k
    private final double[][] points

    // optional (default values given)
    private int iterations     = 50
    private boolean pp         = true
    private double epsilon     = .001
    private boolean useEpsilon = true
    private boolean L1norm = true


    /**
     * Create a Builder that estimates the number of clusters automatically using a specified method.
     *
     * @param points the data points to cluster; must be non-null and contain at least one point
     * @param method the cluster estimation strategy to use (default: ELBOW)
     */
    Builder(double[][] points, GroupEstimator.CalculationMethod method = GroupEstimator.CalculationMethod.ELBOW) {
      if (points == null || points.length == 0 || points[0].length == 0) {
        throw new IllegalArgumentException("Input data must be non-null and contain at least one point with one dimension.")
      }

      this.points = points
      GroupEstimator estimator = new GroupEstimator(method)
      this.k = estimator.estimateNumberOfGroups(points)
      this.pp = true // Force use of KMeans++ since basic random init doesn't make sense when estimating k
    }

    /**
     * Create a Builder with a fixed number of clusters.
     *
     * @param k the number of clusters to find
     * @param points the data points to cluster; must be non-null and at least <code>k</code> distinct points
     * @throws IllegalArgumentException if input is invalid
     */
    Builder(int k, double[][] points) {
      if (points == null || points.length == 0 || points[0].length == 0) {
        throw new IllegalArgumentException("Input data must be non-null and contain at least one point with one dimension.")
      }

      if (k > points.length) {
        throw new IllegalArgumentException("Required: number of points (${points.length}) >= number of clusters ($k)")
      }

      // Check for at least k distinct points by wrapping double[] as List<Double>
      Set<List<Double>> distinctPoints = new HashSet<>()
      for (double[] pt : points) {
        List<Double> list = Arrays.stream(pt).boxed().toList()
        if (distinctPoints.add(list) && distinctPoints.size() >= k) {
          break
        }
      }

      if (distinctPoints.size() < k) {
        throw new IllegalArgumentException("Required: number of distinct data points (${distinctPoints.size()}) < number of clusters ($k)")
      }

      this.k = k
      this.points = points
    }

    /**
     * Set the number of clustering iterations (default is 50).
     * Each iteration runs KMeans independently and retains the best result.
     *
     * @param iterations the number of runs; must be ≥ 1
     * @return the updated builder instance
     */
    Builder iterations(int iterations) {
      if (iterations < 1)
        throw new IllegalArgumentException("Required: non-negative number of iterations. Ex: 50");
      this.iterations = iterations
      return this
    }

    /**
     * Enable or disable KMeans++ initialization (default is true).
     *
     * @param pp true to use KMeans++ (recommended), false for random initialization
     * @return the updated builder instance
     */
    Builder pp(boolean pp) {
      this.pp = pp
      return this
    }

    /**
     * Set the convergence threshold. When the improvement in WCSS is less than epsilon, the algorithm stops.
     * <p>
     * <ul>
     *   <li>Smaller - slower, more precision.</li>
     *   <li>Larger - faster, less precision.</li>
     * </ul>
     * Remember to set 'useEpsilon' to true if using this.
     * </p>
     * <p>
     * Recommended: [.0001 to .01].
     * Default value is .001.
     * </p>
     *
     * @param epsilon a small non-negative number (e.g., 0.001)
     * @return the updated builder instance
     */
    Builder epsilon(double epsilon) {
      if (epsilon < 0.0)
        throw new IllegalArgumentException("Required: non-negative value of epsilon. Ex: .001")

      this.epsilon = epsilon
      return this
    }

    /**
     * Enable or disable stopping based on marginal WCSS improvement. Default value is true.
     *  <ul>
     *   <li>true: stop running when marginal improvement in WCSS &lt; epsilon</li>
     *   <li>false: stop running when 0 improvement</li>
     * </ul>
     * Recommended unless extremely exact.
     *
     * @param useEpsilon true to use epsilon-based stopping; false to stop only on exact convergence
     * @return the updated builder instance
     */
    Builder useEpsilon(boolean useEpsilon) {
      this.useEpsilon = useEpsilon
      return this
    }

    /**
     * Set the distance function to use:
     * <ul>
     *   <li>true for L1 (Manhattan) distance</li>
     *   <li>false for L2 (Euclidean) distance</li>
     * </ul>
     *
     * @param L1norm true to use L1 norm, false for L2 norm
     * @return the updated builder instance
     */
    Builder useL1norm(boolean L1norm) {
      this.L1norm = L1norm
      return this
    }

    /**
     * Construct a {@link KMeansPlusPlus} instance using the current builder configuration.
     *
     * @return a new KMeansPlusPlus object
     */
    KMeansPlusPlus build() {
      //println "Running KMeansPlusPlus clustering..."
      //println "Input points: ${points.length}"
      //println "k = $k, iterations = $iterations"
      return new KMeansPlusPlus(this)
    }
  }


  /***********************************************************************
   * KMeans clustering algorithm
   **********************************************************************/

  /**
   * Run KMeans algorithm
   */
  private void run() {
    //println "Starting clustering with $points.length points and $k clusters"
    // for choosing the best run
    double bestWCSS = Double.POSITIVE_INFINITY
    double[][] bestCentroids = new double[0][0]
    ClusteredPoint[] bestAssignment = new ClusteredPoint[0]

    // run multiple times and then choose the best run
    for (int n = 0; n < iterations; n++) {
      //println "Iteration $n: WCSS = $WCSS"
      cluster()

      // store info if it was the best run so far
      if (WCSS < bestWCSS) {
        //println "$n WCSS < bestWCSS = $WCSS < $bestWCSS"
        //println("$n Centroids: ${centroids} clusters")
        //println("$n Assignment: ${assignment.length} points assigned to clusters")
        bestWCSS = WCSS
        bestCentroids = centroids
        bestAssignment = assignment
      }
    }

    // keep info from best run
    WCSS = bestWCSS
    centroids = bestCentroids
    assignment = bestAssignment
  }


  /**
   * Perform KMeans clustering algorithm once.
   */
  private void cluster() {
    centroids = new double[k][n]
    // continue to re-cluster until marginal gains are small enough
    chooseInitialCentroids()
    WCSS = Double.POSITIVE_INFINITY
    double prevWCSS
    do {
      assignmentStep()   // assign points to the closest centroids
      updateStep()       // update centroids
      prevWCSS = WCSS    // check if cost function meets stopping criteria
      calcWCSS()
    } while (!stop(prevWCSS))
  }


  /**
   * Assigns to each data point the nearest centroid.
   */
  private void assignmentStep() {
    assignment = new ClusteredPoint[m]

    for (int i = 0; i < m; i++) {
      int minLocation = 0
      double minValue = Double.POSITIVE_INFINITY
      for (int j = 0; j < k; j++) {
        double tempDist = distance(points[i], centroids[j])
        if (tempDist < minValue) {
          minValue = tempDist
          minLocation = j
        }
      }

      assignment[i] = new ClusteredPoint(minLocation, points[i])
    }

  }


  /**
   * Updates the centroids.
   */
  private void updateStep() {
    // Zero out all centroids
    for (int i = 0; i < k; i++) {
      if (centroids[i] == null) {
        centroids[i] = new double[n]
      } else {
        Arrays.fill(centroids[i], 0.0d)
      }
    }

    int[] clustSize = new int[k]

    // Sum points assigned to each cluster
    for (ClusteredPoint cp : assignment) {
      int cid = cp.clusterId
      clustSize[cid]++
      for (int j = 0; j < n; j++) {
        centroids[cid][j] += cp.point[j]
      }
    }

    Random rand = new Random()

    // Defensive fix for empty clusters
    for (int i = 0; i < k; i++) {
      if (clustSize[i] == 0) {
        println "⚠️ Cluster $i is empty, reinitializing to a random point"
        int randIndex = rand.nextInt(m)
        centroids[i] = Arrays.copyOf(points[randIndex], n)
        clustSize[i] = 1 // prevent division by zero
      }
    }

    // Normalize to mean (centroid)
    for (int i = 0; i < k; i++) {
      for (int j = 0; j < n; j++) {
        centroids[i][j] /= clustSize[i]
        if (Double.isNaN(centroids[i][j])) {
          throw new IllegalStateException("Centroid $i at dim $j is NaN!")
        }
      }
    }
  }

  /***********************************************************************
   * Choose initial centroids
   **********************************************************************/
  /**
   * Uses either plusplus (KMeans++) or a basic randoms sample to choose initial centroids
   */
  private void chooseInitialCentroids() {
    if (pp)
      plusplus()
    else
      basicRandSample()
  }

  /**
   * Randomly chooses (without replacement) k data points as initial centroids.
   */
  private void basicRandSample() {
    centroids = new double[k][n]
    double[][] copy = points

    Random gen = new Random()

    int rand;
    for (int i = 0; i < k; i++) {
      rand = gen.nextInt(m - i)
      for (int j = 0; j < n; j++) {
        centroids[i][j] = copy[rand][j]       // store chosen centroid
        copy[rand][j] = copy[m - 1 - i][j]    // ensure sampling without replacement
      }
    }
  }

  /**
   * Randomly chooses (without replacement) k data points as initial centroids using a
   * weighted probability distribution (proportional to D(x)^2 where D(x) is the
   * distance from a data point to the nearest, already chosen centroid).
   */
  private void plusplus() {
    centroids = new double[k][n]
    double[] distToClosestCentroid = new double[m]
    double[] weightedDistribution  = new double[m]  // cumulative sum of squared distances

    Random gen = new Random()
    int choose = 0

    for (int c = 0; c < k; c++) {

      // first centroid: choose any data point
      if (c == 0)
        choose = gen.nextInt(m)

      // after first centroid, use a weighted distribution
      else {

        // check if the most recently added centroid is closer to any of the points than previously added ones
        for (int p = 0; p < m; p++) {
          // gives chosen points 0 probability of being chosen again -> sampling without replacement
          double tempDistance = Distance.L2(points[p], centroids[c - 1]); // need L2 norm here, not L1

          // base case: if we have only chosen one centroid so far, nothing to compare to
          if (c == 1)
            distToClosestCentroid[p] = tempDistance

          else { // c != 1
            if (tempDistance < distToClosestCentroid[p])
              distToClosestCentroid[p] = tempDistance
          }

          // no need to square because the distance is the square of the euclidean dist
          if (p == 0)
            weightedDistribution[0] = distToClosestCentroid[0]
          else weightedDistribution[p] = weightedDistribution[p-1] + distToClosestCentroid[p]

        }

        // choose the next centroid
        double rand = gen.nextDouble()
        for (int j = m - 1; j > 0; j--) {
          // TODO: review and try to optimize
          // starts at the largest bin. EDIT: not actually the largest
          if (rand > weightedDistribution[j - 1] / weightedDistribution[m - 1]) {
            choose = j // one bigger than the one above
            break
          }
          else // Because of invalid dimension errors, we can't make the forloop go to j2 > -1 when we have (j2-1) in the loop.
            choose = 0
        }
      }

      // store the chosen centroid
      for (int i = 0; i < n; i++)
        centroids[c][i] = points[choose][i]
    }
  }


  /***********************************************************************
   * Cutoff to stop clustering
   **********************************************************************/

  /**
   * Calculates whether to stop the run
   * @param prevWCSS error from previous step in the run
   * @return
   */
  private boolean stop(double prevWCSS) {
    if (useEpsilon)
      return epsilonTest(prevWCSS)
    else
      return prevWCSS == WCSS
  }

  /**
   * Signals to stop running KMeans when the marginal improvement in WCSS
   * from the last step is small.
   * @param prevWCSS error from previous step in the run
   * @return
   */
  private boolean epsilonTest(double prevWCSS) {
    return epsilon > 1 - (WCSS / prevWCSS)
  }

  /***********************************************************************
   * Utility functions
   **********************************************************************/
  /**
   * Calculates distance between two n-dimensional points.
   * @param x
   * @param y
   * @return
   */
  private double distance(double[] x, double[] y) {
    return L1norm ? Distance.L1(x, y) : Distance.L2(x, y)
  }

  private static class Distance {

    /**
     * L1 norm: distance(X,Y) = sum_i=1:n[|x_i - y_i|]
     * <P> Minkowski distance of order 1.
     * @param x
     * @param y
     * @return
     */
    static double L1(double[] x, double[] y) {
      if (x.length != y.length) throw new IllegalArgumentException("dimension error")
      double dist = 0
      for (int i = 0; i < x.length; i++)
        dist += Math.abs(x[i] - y[i])
      return dist
    }

    /**
     * L2 norm: distance(X,Y) = sqrt(sum<sub>i</sub>=1:n[(x<sub>i</sub>-y<sub>i</sub>)^2])
     * <P> Euclidean distance, or Minkowski distance of order 2.
     * @param x the first n-dimensional point
     * @param y the second n-dimensional point
     * @return the squared distance between the two points
     */
    static double L2(double[] x, double[] y) {
      if (x.length != y.length) throw new IllegalArgumentException("dimension error")
      double dist = 0
      for (int i = 0; i < x.length; i++) {
        double diff = x[i] - y[i]
        dist += diff * diff
      }
      return dist
    }
  }

  /**
   * Calculates WCSS (Within-Cluster-Sum-of-Squares), a measure of the clustering's error.
   */
  private void calcWCSS() {
    double total = 0
    for (ClusteredPoint cp : assignment) {
      total += distance(cp.point, centroids[cp.clusterId])
    }
    this.WCSS = total
  }

  /***********************************************************************
   * Accessors
   ***********************************************************************/

  /**
   * Get the assignment of each data point to a cluster.
   *
   * @return an array where each entry corresponds to a {@link ClusteredPoint} and its assigned cluster
   */
  ClusteredPoint[] getAssignment() {
    return assignment
  }

  /**
   * Returns a map of cluster ID to a Matrix containing the points assigned to that cluster.
   *
   * @return a map where keys are cluster IDs (0 to k-1) and values are a {@link Matrix} containing cluster points
   */
  Map<Integer, Matrix> getClustersById() {
    Map<Integer, List<double[]>> grouped = new HashMap<>()

    // Group points by clusterId
    for (ClusteredPoint cp : assignment) {
      grouped.computeIfAbsent(cp.clusterId) { [] }.add(cp.point)
    }

    // Convert each group to a Matrix
    Map<Integer, Matrix> result = new HashMap<>()
    grouped.each { clusterId, pointList ->
      // Convert List<double[]> to List<List<Double>> for Matrix compatibility
      List<List<Double>> rows = pointList.collect { double[] row ->
        row.collect { it as Double }
      }
      Matrix clusterMatrix = Matrix.builder("Cluster $clusterId").data(rows).build()
      result.put(clusterId, clusterMatrix)
    }

    return result
  }

  /**
   * In k-means clustering, a centroid represents the center of a cluster and is crucial for
   * defining cluster boundaries and assigning data points. It's calculated as the mean of all
   * data points within a cluster. The algorithm iteratively refines these centroids by
   * assigning data points to the nearest centroid and then recalculating the centroid as the
   * mean of the assigned points.
   *
   * @return the centroids of the clusters (the coordinates of the cluster centroids),
   * where each centroid is represented as an array of doubles.
   */
  double[][] getCentroids() {
    return centroids
  }

  /**
   * Returns the WCSS (Within-Cluster-Sum-of-Squares, also known as inertia) of the clustering.
   * WCSS is a metric used to measure the compactness or cohesion of clusters.
   * It represents the sum of squared distances between each data point and the centroid of its assigned cluster.
   * A smaller WCSS value indicates that the data points within a cluster are more tightly grouped around their centroid,
   * resulting in a more compact cluster.
   *
   * @return WCSS the Within-Cluster-Sum-of-Squares
   */
  double getWCSS() {
    return WCSS
  }

  /**
   * Returns the time taken to perform the KMeans++ clustering in seconds as a formatted string.
   * The time is calculated as the difference between the end and start timestamps.
   *
   * @return a string indicating the time taken for KMeans++ clustering
   */
  String getTiming() {
    return "KMeans++ took: " + getExecutionTimeMillis() / 1000.0 + " seconds"
  }

  /**
   * Returns the time taken to perform the KMeans++ clustering in milliseconds.
   * The time is calculated as the difference between the end and start timestamps.
   *
   * @return the execution time in milliseconds
   */
  double getExecutionTimeMillis() {
    return end - start
  }

}
