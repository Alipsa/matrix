package se.alipsa.matrix.stats.cluster

import se.alipsa.matrix.core.Matrix

/**
 * KMeans++ is an enhanced K-Means clustering algorithm that uses an improved initialization
 * strategy to produce better clustering results. It partitions n observations into k clusters
 * by iteratively minimizing the Within-Cluster Sum of Squares (WCSS).
 *
 * <p>This implementation is based on <a href='https://github.com/JasonAltschuler/KMeansPlusPlus'>Jason Altschuler's
 * implementation</a> (used with explicit permission). It provides a low-level API for clustering
 * double[][] arrays, with optional parameters for fine-tuning convergence and distance metrics.</p>
 *
 * <h3>What is KMeans++?</h3>
 * <p>KMeans++ improves upon standard K-Means by using a smarter initialization strategy:</p>
 * <ul>
 *   <li><strong>Standard K-Means:</strong> Randomly selects k initial centroids, often leading to poor local optima</li>
 *   <li><strong>KMeans++:</strong> Selects initial centroids probabilistically, favoring points far from existing centroids</li>
 *   <li><strong>Result:</strong> Faster convergence and better clustering quality (proven O(log k) competitive ratio)</li>
 * </ul>
 *
 * <h3>When to Use KMeansPlusPlus</h3>
 * <ul>
 *   <li><strong>Low-Level Control:</strong> When you need direct access to centroids, WCSS, or distance metrics</li>
 *   <li><strong>Performance Tuning:</strong> When you want to customize epsilon, iterations, or distance norms</li>
 *   <li><strong>Non-Matrix Data:</strong> When working with double[][] arrays instead of Matrix objects</li>
 *   <li><strong>Algorithmic Research:</strong> When studying clustering behavior or implementing variants</li>
 * </ul>
 *
 * <p><strong>Note:</strong> For most use cases, prefer the higher-level {@link KMeans} class which provides
 * a simpler API and integrates directly with Matrix objects.</p>
 *
 * <h3>KMeans++ Initialization Strategy</h3>
 * <p>The initialization algorithm works as follows:</p>
 * <ol>
 *   <li>Choose the first centroid uniformly at random from the data points</li>
 *   <li>For each subsequent centroid:
 *     <ul>
 *       <li>Compute D(x)² for each point x, where D(x) = distance to nearest existing centroid</li>
 *       <li>Select the next centroid with probability proportional to D(x)²</li>
 *       <li>This favors points far from existing centroids, spreading them out</li>
 *     </ul>
 *   </li>
 *   <li>Repeat until k centroids are selected</li>
 * </ol>
 *
 * <p><strong>Why it works:</strong> This strategy ensures initial centroids are well-distributed across
 * the data space, avoiding poor initializations that trap the algorithm in bad local optima.</p>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>Centroids</strong></dt>
 *   <dd>The k cluster centers, each represented as an n-dimensional point. Updated iteratively
 *   as the mean of all points assigned to the cluster. Access via getCentroids().</dd>
 *
 *   <dt><strong>WCSS (Within-Cluster Sum of Squares)</strong></dt>
 *   <dd>The objective function being minimized: WCSS = Σᵢ Σⱼ ||Cᵢ - Xⱼ||², where Cᵢ is the
 *   i-th centroid and Xⱼ is a point assigned to cluster i. Lower WCSS = better clustering.</dd>
 *
 *   <dt><strong>Convergence</strong></dt>
 *   <dd>The algorithm stops when WCSS improvement falls below epsilon (default: 0.001) or when
 *   no improvement occurs. Runs multiple iterations (default: 50) and keeps the best result.</dd>
 *
 *   <dt><strong>Distance Metrics</strong></dt>
 *   <dd>Supports L1 (Manhattan) and L2 (Euclidean squared) distance. KMeans++ initialization
 *   always uses L2 for theoretical guarantees, but the main algorithm can use either.</dd>
 * </dl>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.stats.cluster.KMeansPlusPlus
 * import se.alipsa.matrix.stats.cluster.ClusteredPoint
 *
 * // Prepare data as double[][]
 * double[][] points = [
 *     [1.0, 2.0],
 *     [1.5, 1.8],
 *     [5.0, 8.0],
 *     [8.0, 8.0],
 *     [1.0, 0.6],
 *     [9.0, 11.0]
 * ]
 *
 * // Method 1: Fixed k with custom parameters
 * KMeansPlusPlus clustering = new KMeansPlusPlus.Builder(2, points)
 *     .iterations(50)           // Run 50 times, keep best result
 *     .pp(true)                 // Use KMeans++ initialization (recommended)
 *     .epsilon(0.001)           // Stop when WCSS improvement &lt; 0.001
 *     .useEpsilon(true)         // Enable epsilon-based stopping
 *     .useL1norm(false)         // Use L2 (Euclidean) distance
 *     .build()
 *
 * // Method 2: Auto-estimate k using GroupEstimator
 * clustering = new KMeansPlusPlus.Builder(points, GroupEstimator.CalculationMethod.ELBOW)
 *     .iterations(30)
 *     .build()
 *
 * // Access results
 * double[][] centroids = clustering.getCentroids()
 * ClusteredPoint[] assignments = clustering.getAssignment()
 * double wcss = clustering.getWCSS()
 *
 * println "WCSS: $wcss"
 * println "Execution time: ${clustering.getTiming()}"
 *
 * // Examine cluster assignments
 * assignments.each { cp ->
 *     println "Point ${Arrays.toString(cp.point)} → Cluster ${cp.clusterId}"
 * }
 *
 * // Get clusters as Matrix objects
 * Map&lt;Integer, Matrix&gt; clusterMatrices = clustering.getClustersById()
 * clusterMatrices.each { clusterId, matrix ->
 *     println "Cluster $clusterId: ${matrix.rowCount()} points"
 * }
 * </pre>
 *
 * <h3>Advanced Configuration</h3>
 * <pre>
 * // Fine-tune for large datasets (prioritize speed)
 * clustering = new KMeansPlusPlus.Builder(10, points)
 *     .iterations(20)           // Fewer iterations
 *     .epsilon(0.01)            // Looser convergence threshold
 *     .useL1norm(true)          // Faster distance calculation
 *     .build()
 *
 * // Fine-tune for precision (prioritize accuracy)
 * clustering = new KMeansPlusPlus.Builder(5, points)
 *     .iterations(100)          // More iterations
 *     .epsilon(0.0001)          // Tighter convergence threshold
 *     .useL1norm(false)         // More accurate L2 distance
 *     .build()
 * </pre>
 *
 * <h3>Algorithm Steps</h3>
 * <ol>
 *   <li><strong>Initialization:</strong> Select k initial centroids using KMeans++ or random sampling</li>
 *   <li><strong>Assignment Step:</strong> Assign each point to its nearest centroid (using L1 or L2 distance)</li>
 *   <li><strong>Update Step:</strong> Recalculate each centroid as the mean of its assigned points</li>
 *   <li><strong>Convergence Check:</strong> If WCSS improvement &lt; epsilon (or 0), stop; otherwise repeat from step 2</li>
 *   <li><strong>Multi-Run:</strong> Repeat the entire process 'iterations' times and keep the best result (lowest WCSS)</li>
 * </ol>
 *
 * <h3>Parameters</h3>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>k</td><td>int</td><td>required</td><td>Number of clusters (must be ≤ number of distinct points)</td></tr>
 *   <tr><td>points</td><td>double[][]</td><td>required</td><td>Input data (m × n array: m points, n dimensions)</td></tr>
 *   <tr><td>iterations</td><td>int</td><td>50</td><td>Number of independent runs (keeps best WCSS)</td></tr>
 *   <tr><td>pp</td><td>boolean</td><td>true</td><td>Use KMeans++ initialization (vs. random)</td></tr>
 *   <tr><td>epsilon</td><td>double</td><td>0.001</td><td>Convergence threshold for WCSS improvement</td></tr>
 *   <tr><td>useEpsilon</td><td>boolean</td><td>true</td><td>Enable epsilon-based stopping (vs. exact convergence)</td></tr>
 *   <tr><td>L1norm</td><td>boolean</td><td>true</td><td>Use L1 (Manhattan) vs. L2 (Euclidean) distance</td></tr>
 * </table>
 *
 * <h3>Output Methods</h3>
 * <ul>
 *   <li><strong>getCentroids():</strong> Returns double[][] of final cluster centers (k × n)</li>
 *   <li><strong>getAssignment():</strong> Returns ClusteredPoint[] array with cluster IDs for each point</li>
 *   <li><strong>getWCSS():</strong> Returns final Within-Cluster Sum of Squares (double)</li>
 *   <li><strong>getClustersById():</strong> Returns Map&lt;Integer, Matrix&gt; of points grouped by cluster</li>
 *   <li><strong>getTiming():</strong> Returns execution time as formatted string</li>
 *   <li><strong>getExecutionTimeMillis():</strong> Returns execution time in milliseconds</li>
 * </ul>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Always use pp(true):</strong> KMeans++ initialization consistently outperforms random initialization</li>
 *   <li><strong>Normalize data:</strong> Use Normalize.minMaxNorm() before clustering for better results</li>
 *   <li><strong>Check for empty clusters:</strong> The algorithm automatically reinitializes empty clusters to random points</li>
 *   <li><strong>Validate k:</strong> Ensure k ≤ number of distinct points, or construction will fail</li>
 *   <li><strong>Monitor WCSS:</strong> Compare WCSS values to assess clustering quality</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 *   <li>Arthur, D. and Vassilvitskii, S. (2007). "k-means++: the advantages of careful seeding."
 *   SODA '07: Proceedings of the eighteenth annual ACM-SIAM symposium on Discrete algorithms, 1027–1035.
 *   (Proves O(log k) competitive ratio for KMeans++ initialization)</li>
 *   <li>MacQueen, J. (1967). "Some methods for classification and analysis of multivariate observations."
 *   Proceedings of the Fifth Berkeley Symposium on Mathematical Statistics and Probability, 1:281–297.
 *   (Original K-Means algorithm description)</li>
 *   <li>Hartigan, J.A. and Wong, M.A. (1979). "Algorithm AS 136: A K-Means Clustering Algorithm."
 *   Journal of the Royal Statistical Society, Series C, 28(1): 100–108.</li>
 *   <li>Altschuler, J. (2016). KMeansPlusPlus GitHub Repository.
 *   <a href="https://github.com/JasonAltschuler/KMeansPlusPlus">https://github.com/JasonAltschuler/KMeansPlusPlus</a></li>
 * </ul>
 *
 * @author Jason Altschuler
 * @author Per Nyfelt
 * @see KMeans
 * @see GroupEstimator
 * @see ClusteredPoint
 */
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

        // choose the next centroid using binary search for O(log m) complexity
        double totalWeight = weightedDistribution[m - 1]
        double target = gen.nextDouble() * totalWeight
        // Binary search to find smallest index j where weightedDistribution[j] >= target
        int lo = 0
        int hi = m - 1
        while (lo < hi) {
          int mid = (lo + hi) >>> 1
          if (weightedDistribution[mid] < target) {
            lo = mid + 1
          } else {
            hi = mid
          }
        }
        choose = lo
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
