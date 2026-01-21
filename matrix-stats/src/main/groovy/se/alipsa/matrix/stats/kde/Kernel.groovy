package se.alipsa.matrix.stats.kde


/**
 * Kernel functions for kernel density estimation.
 *
 * Each kernel is a symmetric probability density function that integrates to 1.
 * The kernel function K(u) is evaluated at the standardized distance u = (x - xi) / h
 * where h is the bandwidth.
 *
 * Equivalent to R's density() kernel parameter.
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
