package se.alipsa.matrix.stats.kde

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode

/**
 * Kernel Density Estimation (KDE) is a non-parametric technique for estimating the probability
 * density function of a continuous random variable. Unlike parametric methods (e.g., assuming
 * normal distribution), KDE makes no assumptions about the underlying distribution shape.
 *
 * <p>KDE creates a smooth density curve by placing a kernel function (typically a bump or bell curve)
 * at each data point and summing them. The bandwidth parameter controls the width of each kernel,
 * determining the smoothness of the final estimate.</p>
 *
 * <h3>What is Kernel Density Estimation?</h3>
 * <p>KDE estimates the probability density at any point x using:</p>
 * <pre>
 * f̂(x) = (1/nh) Σᵢ K((x - xᵢ)/h)
 * </pre>
 * <p>where:</p>
 * <ul>
 *   <li><strong>n:</strong> Number of data points</li>
 *   <li><strong>h:</strong> Bandwidth (smoothing parameter)</li>
 *   <li><strong>K:</strong> Kernel function (e.g., Gaussian, Epanechnikov)</li>
 *   <li><strong>xᵢ:</strong> Individual data points</li>
 * </ul>
 *
 * <h3>When to Use KDE</h3>
 * <ul>
 *   <li><strong>Exploratory Data Analysis:</strong> Visualize distribution shape without assuming normality</li>
 *   <li><strong>Multimodal Distributions:</strong> Detect multiple peaks in data that histogram might miss</li>
 *   <li><strong>Smooth Probability Estimates:</strong> Get continuous density estimates for any point</li>
 *   <li><strong>Comparing Distributions:</strong> Overlay density curves for different groups</li>
 *   <li><strong>Anomaly Detection:</strong> Identify points in low-density regions</li>
 *   <li><strong>Sampling:</strong> Generate synthetic data following the estimated distribution</li>
 * </ul>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>Bandwidth (h)</strong></dt>
 *   <dd>The most critical parameter controlling smoothness. Larger bandwidth creates smoother estimates
 *   but may obscure features; smaller bandwidth shows more detail but may be too rough. This implementation
 *   uses Silverman's rule of thumb by default: h = 0.9 × min(σ, IQR/1.34) × n^(-1/5), where σ is standard
 *   deviation and IQR is interquartile range. The adjust parameter allows scaling this bandwidth.</dd>
 *
 *   <dt><strong>Kernel Function</strong></dt>
 *   <dd>The shape of the "bump" placed at each data point. Common choices include Gaussian (smooth, unbounded),
 *   Epanechnikov (optimal MISE, compact support), Uniform (simple, discontinuous), and Triangular (balanced).
 *   The choice of kernel has less impact than bandwidth selection in practice.</dd>
 *
 *   <dt><strong>Bias-Variance Tradeoff</strong></dt>
 *   <dd>Small bandwidth (undersmoothing) has low bias but high variance; large bandwidth (oversmoothing)
 *   has high bias but low variance. The optimal bandwidth minimizes Mean Integrated Squared Error (MISE),
 *   balancing these competing factors.</dd>
 *
 *   <dt><strong>Boundary Effects</strong></dt>
 *   <dd>KDE can produce non-zero density estimates outside the data range due to kernel support extending
 *   beyond data boundaries. Use trim=true to restrict density to the observed data range, or use boundary
 *   correction methods for data with natural bounds (e.g., non-negative values).</dd>
 *
 *   <dt><strong>Evaluation Grid</strong></dt>
 *   <dd>The density is computed on a grid of n equally-spaced points (default: 512). This grid determines
 *   the resolution of the density curve. More points provide smoother visualization but increase computation time.</dd>
 * </dl>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.core.Matrix
 * import se.alipsa.matrix.stats.kde.KernelDensity
 * import se.alipsa.matrix.stats.kde.Kernel
 *
 * // Basic usage with defaults (Gaussian kernel, Silverman bandwidth)
 * def data = [1.2, 2.3, 1.8, 2.1, 3.5, 2.9]
 * def kde = new KernelDensity(data)
 * println kde.summary()  // Print summary statistics
 *
 * // Query density at specific point
 * double density = kde.density(2.5)
 * println "Density at 2.5: ${density}"
 *
 * // Get results as Matrix for plotting
 * Matrix result = kde.toMatrix()
 * // result has 'x' and 'density' columns ready for visualization
 *
 * // From Matrix column
 * Matrix mtcars = Matrix.builder().data('/path/to/mtcars.csv').build()
 * def mpgDensity = new KernelDensity(mtcars, 'mpg')
 * </pre>
 *
 * <h3>Advanced Configuration</h3>
 * <pre>
 * // Use Epanechnikov kernel (optimal MISE)
 * def kde = new KernelDensity(data, [
 *   kernel: Kernel.EPANECHNIKOV
 * ])
 *
 * // Adjust bandwidth (multiply Silverman's rule by factor)
 * def kde = new KernelDensity(data, [
 *   adjust: 0.5  // Narrower bandwidth, less smooth
 * ])
 *
 * // Specify custom bandwidth directly
 * def kde = new KernelDensity(data, [
 *   bandwidth: 0.3  // Override automatic selection
 * ])
 *
 * // Trim density to data range
 * def kde = new KernelDensity(data, [
 *   trim: true  // No density estimates outside [min, max]
 * ])
 *
 * // Custom evaluation range and resolution
 * def kde = new KernelDensity(data, [
 *   from: 0,    // Start of evaluation grid
 *   to: 10,     // End of evaluation grid
 *   n: 1024     // Number of evaluation points
 * ])
 *
 * // Combine multiple options
 * def kde = new KernelDensity(data, [
 *   kernel: Kernel.GAUSSIAN,
 *   adjust: 1.5,
 *   trim: false,
 *   n: 512
 * ])
 * </pre>
 *
 * <h3>Comparison with R</h3>
 * <p>This implementation is equivalent to R's density() function:</p>
 * <pre>
 * # R code
 * x <- c(1.2, 2.3, 1.8, 2.1, 3.5, 2.9)
 * d <- density(x, kernel = "gaussian", adjust = 1.0, n = 512)
 * d$bw      # bandwidth used
 * d$x       # evaluation points
 * d$y       # density values
 *
 * # Groovy equivalent
 * def data = [1.2, 2.3, 1.8, 2.1, 3.5, 2.9]
 * def kde = new KernelDensity(data, [kernel: Kernel.GAUSSIAN, adjust: 1.0, n: 512])
 * kde.bandwidth  // bandwidth used
 * kde.x          // evaluation points
 * kde.density    // density values
 * </pre>
 *
 * <h3>Best Practices</h3>
 * <ul>
 *   <li><strong>Choose bandwidth carefully:</strong> Start with default Silverman's rule, adjust if needed</li>
 *   <li><strong>Visualize results:</strong> Always plot the density to assess if smoothing is appropriate</li>
 *   <li><strong>Compare kernels:</strong> Gaussian is safe default; try Epanechnikov for efficiency</li>
 *   <li><strong>Handle boundaries:</strong> Use trim=true for bounded data or apply transformations</li>
 *   <li><strong>Consider sample size:</strong> KDE requires sufficient data (n > 30 recommended)</li>
 *   <li><strong>Avoid for discrete data:</strong> KDE is for continuous distributions; use histograms for discrete</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 *   <li>Silverman, B.W. (1986). "Density Estimation for Statistics and Data Analysis."
 *   Chapman & Hall/CRC. ISBN 978-0412246203. [Classic reference on KDE theory and practice]</li>
 *   <li>Scott, D.W. (2015). "Multivariate Density Estimation: Theory, Practice, and Visualization."
 *   2nd Edition, Wiley. ISBN 978-0471697558. [Comprehensive coverage including bandwidth selection]</li>
 *   <li>Wand, M.P. and Jones, M.C. (1995). "Kernel Smoothing."
 *   Chapman & Hall/CRC. ISBN 978-0412552700. [Technical details on kernel methods]</li>
 *   <li>Sheather, S.J. and Jones, M.C. (1991). "A Reliable Data-Based Bandwidth Selection Method for Kernel Density Estimation."
 *   Journal of the Royal Statistical Society, Series B, 53(3): 683-690. [Advanced bandwidth selection]</li>
 *   <li>R Documentation: stats::density() function.
 *   https://stat.ethz.ch/R-manual/R-devel/library/stats/html/density.html</li>
 * </ul>
 *
 * @see Kernel
 * @see BandwidthSelector
 */
class KernelDensity {

  /** The kernel function used for density estimation */
  final Kernel kernel

  /** Number of evaluation points */
  final int n

  /** Bandwidth adjustment multiplier */
  final double adjust

  /** Whether to trim density to the data range */
  final boolean trim

  /** The computed bandwidth */
  final double bandwidth

  /** The evaluation points (x values) */
  final double[] x

  /** The density values at each evaluation point */
  final double[] density

  /** The original data (sorted) */
  private final double[] data

  /** Minimum data value */
  final double dataMin

  /** Maximum data value */
  final double dataMax

  /**
   * Create a kernel density estimate from a list of numbers using default settings.
   *
   * @param values the data values (nulls are filtered out)
   */
  KernelDensity(List<? extends Number> values) {
    this(values, [:])
  }

  /**
   * Create a kernel density estimate from a list of numbers with custom parameters.
   *
   * @param values the data values (nulls are filtered out)
   * @param params configuration parameters:
   *        - kernel: Kernel enum or String ('gaussian', 'epanechnikov', 'uniform', 'triangular')
   *        - bandwidth: custom bandwidth (overrides automatic selection)
   *        - adjust: bandwidth adjustment multiplier (default: 1.0)
   *        - n: number of evaluation points (default: 512)
   *        - trim: trim density to data range (default: false)
   *        - from: custom range start
   *        - to: custom range end
   */
  KernelDensity(List<? extends Number> values, Map params) {
    // Filter nulls and convert to double array
    List<Double> filtered = []
    for (Number v : values) {
      if (v != null) {
        filtered.add(v.doubleValue())
      }
    }

    if (filtered.size() < 2) {
      throw new IllegalArgumentException("KernelDensity requires at least 2 non-null data points")
    }

    this.data = ListConverter.toDoubleArray(filtered)
    Arrays.sort(this.data)

    this.dataMin = this.data[0]
    this.dataMax = this.data[this.data.length - 1]

    // Parse kernel parameter
    Object kernelParam = params['kernel']
    if (kernelParam instanceof Kernel) {
      this.kernel = (Kernel) kernelParam
    } else if (kernelParam instanceof String) {
      this.kernel = Kernel.fromString((String) kernelParam)
    } else {
      this.kernel = Kernel.GAUSSIAN
    }

    // Parse other parameters with defaults
    this.n = params['n'] != null ? ((Number) params['n']).intValue() : 512
    this.adjust = params['adjust'] != null ? ((Number) params['adjust']).doubleValue() : 1.0d
    this.trim = params['trim'] != null ? (Boolean) params['trim'] : false

    // Compute bandwidth
    if (params['bandwidth'] != null) {
      this.bandwidth = ((Number) params['bandwidth']).doubleValue() * this.adjust
    } else {
      this.bandwidth = BandwidthSelector.silverman(this.data) * this.adjust
    }

    // Determine evaluation range
    double range = this.dataMax - this.dataMin
    double extension = this.trim ? 0.0 : range * 0.1
    double evalMin = params['from'] != null ? ((Number) params['from']).doubleValue() : (this.dataMin - extension)
    double evalMax = params['to'] != null ? ((Number) params['to']).doubleValue() : (this.dataMax + extension)

    // Compute density on the grid
    this.x = new double[this.n]
    this.density = new double[this.n]

    computeDensity(evalMin, evalMax)
  }

  /**
   * Create a kernel density estimate from a Matrix column using default settings.
   *
   * @param table the Matrix containing the data
   * @param column the name of the column to use
   */
  KernelDensity(Matrix table, String column) {
    this(table[column] as List<? extends Number>, [:])
  }

  /**
   * Create a kernel density estimate from a Matrix column with custom parameters.
   *
   * @param table the Matrix containing the data
   * @param column the name of the column to use
   * @param params configuration parameters (see other constructor)
   */
  KernelDensity(Matrix table, String column, Map params) {
    this(table[column] as List<? extends Number>, params)
  }

  /**
   * Compute the density at each evaluation point.
   */
  private void computeDensity(double evalMin, double evalMax) {
    double step = (evalMax - evalMin) / (this.n - 1)

    for (int i = 0; i < this.n; i++) {
      this.x[i] = evalMin + i * step
      this.density[i] = computeDensityAt(this.x[i])
    }
  }

  /**
   * Compute the density at a specific point.
   */
  private double computeDensityAt(double point) {
    double sum = 0.0
    for (double xi : this.data) {
      double u = (point - xi) / this.bandwidth
      sum += this.kernel.evaluate(u)
    }
    return sum / (this.data.length * this.bandwidth)
  }

  /**
   * Get the density estimate at a specific point.
   *
   * @param point the point at which to estimate density
   * @return the estimated density at that point
   */
  double density(Number point) {
    return computeDensityAt(point.doubleValue())
  }

  /**
   * Get density estimates at multiple points.
   *
   * @param points the points at which to estimate density
   * @return list of density estimates
   */
  List<Double> density(List<? extends Number> points) {
    List<Double> result = new ArrayList<>(points.size())
    for (Number p : points) {
      result.add(computeDensityAt(p.doubleValue()))
    }
    return result
  }

  /**
   * Get the bandwidth.
   *
   * @return the bandwidth used for density estimation
   */
  double getBandwidth() {
    return bandwidth
  }

  /**
   * Get the bandwidth rounded to the specified number of decimal places.
   *
   * @param decimals the number of decimal places
   * @return the rounded bandwidth
   */
  BigDecimal getBandwidth(int decimals) {
    return BigDecimal.valueOf(bandwidth).setScale(decimals, RoundingMode.HALF_EVEN)
  }

  /**
   * Get the evaluation points (x values).
   *
   * @return copy of the evaluation points array
   */
  double[] getX() {
    return Arrays.copyOf(x, x.length)
  }

  /**
   * Get the density values.
   *
   * @return copy of the density values array
   */
  double[] getDensity() {
    return Arrays.copyOf(density, density.length)
  }

  /**
   * Get the number of data points used for estimation.
   *
   * @return the number of data points
   */
  int getDataCount() {
    return data.length
  }

  /**
   * Convert the density estimate to a Matrix with 'x' and 'density' columns.
   *
   * @return a Matrix containing the density estimate
   */
  Matrix toMatrix() {
    List<Double> xList = new ArrayList<>(n)
    List<Double> densityList = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      xList.add(x[i])
      densityList.add(density[i])
    }

    return Matrix.builder()
        .matrixName('KernelDensity')
        .columns([
            'x': xList,
            'density': densityList
        ])
        .types(Double, Double)
        .build()
  }

  /**
   * Convert the density estimate to a list of [x, density] arrays.
   * This format is compatible with plotting functions.
   *
   * @return list of [x, density] pairs
   */
  List<double[]> toPointList() {
    List<double[]> result = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      result.add([x[i], density[i]] as double[])
    }
    return result
  }

  @Override
  String toString() {
    return "KernelDensity(kernel=${kernel}, n=${n}, bandwidth=${getBandwidth(6)})"
  }

  /**
   * Get a summary of the density estimation.
   *
   * @return formatted summary string
   */
  String summary() {
    double maxDensity = 0.0
    double maxX = 0.0
    for (int i = 0; i < n; i++) {
      if (density[i] > maxDensity) {
        maxDensity = density[i]
        maxX = x[i]
      }
    }

    return """
Kernel Density Estimation
=========================
Kernel:      ${kernel}
Bandwidth:   ${getBandwidth(6)}
Data points: ${data.length}
Eval points: ${n}
Data range:  [${BigDecimal.valueOf(dataMin).setScale(4, RoundingMode.HALF_EVEN)}, ${BigDecimal.valueOf(dataMax).setScale(4, RoundingMode.HALF_EVEN)}]
Peak:        x = ${BigDecimal.valueOf(maxX).setScale(4, RoundingMode.HALF_EVEN)}, density = ${BigDecimal.valueOf(maxDensity).setScale(6, RoundingMode.HALF_EVEN)}
""".stripIndent()
  }
}
