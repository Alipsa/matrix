package se.alipsa.matrix.stats.kde


/**
 * Kernel functions for Kernel Density Estimation (KDE), providing different weighting
 * schemes for smoothing data when estimating probability density functions.
 *
 * <p>A kernel function is a symmetric, non-negative probability density function that integrates to 1.
 * In KDE, the kernel is centered at each data point and weighted by the bandwidth to create a smooth
 * density estimate. Different kernels provide different smoothness properties and computational efficiency.</p>
 *
 * <h3>What are Kernel Functions?</h3>
 * <p>Kernel functions K(u) define how each data point contributes to the density estimate at a given location.
 * The function is evaluated at the standardized distance u = (x - xi) / h, where:</p>
 * <ul>
 *   <li><strong>x:</strong> The point where we estimate density</li>
 *   <li><strong>xi:</strong> A data point in the sample</li>
 *   <li><strong>h:</strong> The bandwidth (smoothing parameter)</li>
 * </ul>
 *
 * <h3>When to Use Different Kernels</h3>
 * <dl>
 *   <dt><strong>GAUSSIAN (Recommended default)</strong></dt>
 *   <dd>Smooth, infinitely differentiable kernel with unbounded support. Best for general-purpose density estimation.
 *   This is the default in R's density() function and most statistical software.</dd>
 *
 *   <dt><strong>EPANECHNIKOV (Optimal efficiency)</strong></dt>
 *   <dd>Theoretically optimal in terms of Mean Integrated Squared Error (MISE). Computationally efficient with
 *   compact support [-1, 1]. Good choice when efficiency matters and edge effects are acceptable.</dd>
 *
 *   <dt><strong>UNIFORM (Simplest)</strong></dt>
 *   <dd>Rectangular kernel giving equal weight to all points within bandwidth. Produces discontinuous density
 *   estimates. Rarely used except for educational purposes or when discontinuity is desired.</dd>
 *
 *   <dt><strong>TRIANGULAR (Balance)</strong></dt>
 *   <dd>Linear decay from center, providing smoother results than UNIFORM but less smooth than GAUSSIAN.
 *   Compact support [-1, 1]. Good middle ground between efficiency and smoothness.</dd>
 * </dl>
 *
 * <h3>Key Concepts</h3>
 * <dl>
 *   <dt><strong>Kernel Smoothness</strong></dt>
 *   <dd>Gaussian kernel is infinitely smooth (C∞), while Epanechnikov and Triangular have discontinuous
 *   derivatives at the boundaries. Smoother kernels produce smoother density estimates but may be slower
 *   to compute.</dd>
 *
 *   <dt><strong>Kernel Support</strong></dt>
 *   <dd>Support is the range where K(u) > 0. Gaussian has unbounded support (-∞, ∞), while Epanechnikov,
 *   Uniform, and Triangular have compact support [-1, 1]. Compact support means faster computation as
 *   distant points contribute zero weight.</dd>
 *
 *   <dt><strong>Efficiency</strong></dt>
 *   <dd>Epanechnikov kernel minimizes the asymptotic Mean Integrated Squared Error (MISE), making it
 *   theoretically optimal. However, in practice, the choice of bandwidth (h) is more important than
 *   the choice of kernel function.</dd>
 *
 *   <dt><strong>Standardized Distance</strong></dt>
 *   <dd>The value u = (x - xi) / h represents how many bandwidths the point x is from the data point xi.
 *   This standardization allows the same kernel function to be used regardless of the scale of the data.</dd>
 * </dl>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * import se.alipsa.matrix.stats.kde.Kernel
 * import se.alipsa.matrix.stats.kde.KernelDensity
 *
 * // Use Gaussian kernel (default)
 * def kde = new KernelDensity(data, [kernel: Kernel.GAUSSIAN])
 *
 * // Use optimal Epanechnikov kernel
 * def kde = new KernelDensity(data, [kernel: Kernel.EPANECHNIKOV])
 *
 * // Parse kernel from string (case-insensitive)
 * Kernel k = Kernel.fromString('gaussian')  // GAUSSIAN
 *
 * // Evaluate kernel function directly
 * double weight = Kernel.GAUSSIAN.evaluate(0.5)  // Weight at 0.5 bandwidths from center
 * </pre>
 *
 * <h3>Mathematical Definitions</h3>
 * <ul>
 *   <li><strong>Gaussian:</strong> K(u) = (1/√2π) exp(-u²/2), support: (-∞, ∞)</li>
 *   <li><strong>Epanechnikov:</strong> K(u) = (3/4)(1 - u²) if |u| ≤ 1, else 0</li>
 *   <li><strong>Uniform:</strong> K(u) = 1/2 if |u| ≤ 1, else 0</li>
 *   <li><strong>Triangular:</strong> K(u) = 1 - |u| if |u| ≤ 1, else 0</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 *   <li>Silverman, B.W. (1986). "Density Estimation for Statistics and Data Analysis."
 *   Chapman & Hall/CRC. ISBN 978-0412246203.</li>
 *   <li>Epanechnikov, V.A. (1969). "Non-Parametric Estimation of a Multivariate Probability Density."
 *   Theory of Probability and Its Applications, 14(1): 153–158.</li>
 *   <li>Wand, M.P. and Jones, M.C. (1995). "Kernel Smoothing."
 *   Chapman & Hall/CRC. ISBN 978-0412552700.</li>
 *   <li>R Documentation: density() function kernel parameter.
 *   https://stat.ethz.ch/R-manual/R-devel/library/stats/html/density.html</li>
 * </ul>
 *
 * @see KernelDensity
 * @see BandwidthSelector
 */
enum Kernel {

  /**
   * Gaussian (normal) kernel: K(u) = (1/√2π) * exp(-u²/2)
   * Support: (-∞, ∞)
   * This is the default kernel in R's density() function.
   */
  GAUSSIAN {
    @Override
    double evaluate(double u) {
      return Math.exp(-0.5 * u * u) / Math.sqrt(2 * Math.PI)
    }
  },

  /**
   * Epanechnikov kernel: K(u) = (3/4)(1 - u²) for |u| ≤ 1, 0 otherwise
   * Support: [-1, 1]
   * Optimal in the sense of minimizing mean integrated squared error.
   */
  EPANECHNIKOV {
    @Override
    double evaluate(double u) {
      if (Math.abs(u) > 1) return 0.0
      return 0.75 * (1.0 - u * u)
    }
  },

  /**
   * Uniform (rectangular) kernel: K(u) = 1/2 for |u| ≤ 1, 0 otherwise
   * Support: [-1, 1]
   */
  UNIFORM {
    @Override
    double evaluate(double u) {
      if (Math.abs(u) > 1) return 0.0
      return 0.5
    }
  },

  /**
   * Triangular kernel: K(u) = 1 - |u| for |u| ≤ 1, 0 otherwise
   * Support: [-1, 1]
   */
  TRIANGULAR {
    @Override
    double evaluate(double u) {
      double absU = Math.abs(u)
      if (absU > 1) return 0.0
      return 1.0 - absU
    }
  }

  /**
   * Evaluate the kernel function at the standardized distance u.
   *
   * @param u the standardized distance (x - xi) / h
   * @return the kernel density contribution
   */
  abstract double evaluate(double u)

  /**
   * Parse a kernel from a string name (case-insensitive).
   *
   * @param name the kernel name ('gaussian', 'epanechnikov', 'uniform', 'triangular')
   * @return the corresponding Kernel enum value
   * @throws IllegalArgumentException if the kernel name is not recognized
   */
  static Kernel fromString(String name) {
    if (name == null) return GAUSSIAN
    switch (name.toLowerCase()) {
      case 'gaussian':
      case 'normal':
        return GAUSSIAN
      case 'epanechnikov':
      case 'epan':
        return EPANECHNIKOV
      case 'uniform':
      case 'rectangular':
      case 'rect':
        return UNIFORM
      case 'triangular':
      case 'triangle':
        return TRIANGULAR
      default:
        throw new IllegalArgumentException("Unknown kernel type: ${name}. " +
            "Supported kernels: gaussian, epanechnikov, uniform, triangular")
    }
  }
}
