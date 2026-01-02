package se.alipsa.matrix.stats.kde

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode

/**
 * Kernel Density Estimation (KDE) for univariate data.
 *
 * KDE is a non-parametric method to estimate the probability density function
 * of a random variable. This implementation supports multiple kernel functions
 * and automatic bandwidth selection.
 *
 * <p>Equivalent to R's density() function:</p>
 * <pre>
 * # R equivalent
 * x <- c(1.2, 2.3, 1.8, 2.1, 3.5, 2.9)
 * d <- density(x, kernel = "gaussian")
 * d$bw      # bandwidth
 * d$x       # evaluation points
 * d$y       # density values
 * </pre>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Basic usage with defaults (Gaussian kernel, Silverman bandwidth)
 * def kde = new KernelDensity([1.2, 2.3, 1.8, 2.1, 3.5, 2.9])
 *
 * // With configuration
 * def kde = new KernelDensity(data, [kernel: Kernel.EPANECHNIKOV, adjust: 0.5])
 *
 * // From Matrix column
 * def kde = new KernelDensity(mtcars, 'mpg')
 *
 * // Query density at specific point
 * double d = kde.density(2.5)
 *
 * // Get results as Matrix
 * Matrix result = kde.toMatrix()
 * </pre>
 */
@CompileStatic
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
