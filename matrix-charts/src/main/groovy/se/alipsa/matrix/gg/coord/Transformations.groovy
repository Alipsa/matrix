package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * Common coordinate transformations.
 */
@CompileStatic
class Transformations {

  /**
   * Identity transformation (no change).
   */
  static class IdentityTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      return x as BigDecimal
    }

    @Override
    BigDecimal inverse(Number x) {
      return x as BigDecimal
    }

    @Override
    String getName() {
      return "identity"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Log10 transformation.
   */
  static class Log10Trans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val <= 0) return null
      return (val as double).log10() as BigDecimal
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      return 10 ** (x as BigDecimal)
    }

    @Override
    String getName() {
      return "log10"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      if (limits == null || limits.size() < 2) return null

      BigDecimal min = ((limits[0] as double).max(1e-10)).log10() as BigDecimal
      BigDecimal max = (limits[1] as double).log10() as BigDecimal

      // Generate breaks at powers of 10
      int minExp = min.floor() as int
      int maxExp = max.ceil() as int

      List<BigDecimal> result = []
      for (int i = minExp; i <= maxExp; i++) {
        BigDecimal value = (10 ** i) as BigDecimal
        result << value
      }
      return result
    }
  }

  /**
   * Natural log transformation.
   */
  static class LogTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val <= 0) return null
      return val.log()
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      return val.exp()
    }

    @Override
    String getName() {
      return "log"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Square root transformation.
   */
  static class SqrtTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val < 0) return null
      return val ** 0.5
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      return val ** 2
    }

    @Override
    String getName() {
      return "sqrt"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Reverse transformation (flips the axis).
   */
  static class ReverseTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      return -(x as BigDecimal)
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      return -(x as BigDecimal)
    }

    @Override
    String getName() {
      return "reverse"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Power transformation (x^n).
   *
   * For odd integer exponents (1, 3, 5, etc.), both forward and inverse transformations
   * correctly handle negative values. For example:
   * - Forward: (-2)^3 = -8
   * - Inverse: cbrt(-8) = -2
   *
   * For non-integer or even exponents, negative input values return null since the
   * result would be complex (not a real number).
   */
  static class PowerTrans implements Trans {
    final BigDecimal exponent
    final boolean isOddInteger

    PowerTrans(Number exponent) {
      this.exponent = exponent as BigDecimal
      // Check if exponent is an odd integer
      this.isOddInteger = isOddIntegerExponent(this.exponent)
    }

    private static boolean isOddIntegerExponent(BigDecimal exp) {
      try {
        int intValue = exp.intValueExact()
        return intValue % 2 != 0
      } catch (ArithmeticException ignored) {
        return false  // Not an integer
      }
    }

    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      // For odd integer exponents, negative values are allowed
      if (val < 0 && !isOddInteger) {
        return null  // Negative base with non-odd-integer exponent is undefined for reals
      }
      return val ** exponent
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal

      // For odd integer exponents, handle negative values specially
      // since x ** (1/n) doesn't work for negative x in Java
      if (val < 0) {
        if (isOddInteger) {
          // For odd roots: nth-root(-x) = -nth-root(x)
          BigDecimal absResult = (-val) ** (1 / exponent)
          return -absResult
        } else {
          return null  // Negative with non-odd-integer exponent is undefined
        }
      }
      return val ** (1 / exponent)
    }

    @Override
    String getName() {
      return "power"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Arcsine square root transformation.
   * Commonly used for proportions/percentages (values between 0 and 1).
   * Forward: asin(sqrt(x))
   * Inverse: sin(x)^2
   */
  static class AsnTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val < 0 || val > 1) return null
      return Math.asin(Math.sqrt(val as double)) as BigDecimal
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      double sinVal = Math.sin(x as double)
      return (sinVal * sinVal) as BigDecimal
    }

    @Override
    String getName() {
      return "asn"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Reciprocal transformation (1/x).
   */
  static class ReciprocalTrans implements Trans {
    @Override
    BigDecimal transform(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val == 0) return null
      return 1 / val
    }

    @Override
    BigDecimal inverse(Number x) {
      if (x == null) return null
      BigDecimal val = x as BigDecimal
      if (val == 0) return null
      return 1 / val
    }

    @Override
    String getName() {
      return "reciprocal"
    }

    @Override
    List<BigDecimal> breaks(List<Number> limits, int n) {
      return null // Use default breaks
    }
  }

  /**
   * Get a transformation by name.
   * @param name transformation name: 'identity', 'log', 'log10', 'sqrt', 'reverse', 'reciprocal', 'asn'
   * @param params optional parameters (e.g., for power: [exponent: 2])
   * @return Trans instance
   */
  static Trans getTrans(String name, Map params = [:]) {
    if (name == null) return new IdentityTrans()

    switch (name.toLowerCase()) {
      case 'identity':
        return new IdentityTrans()
      case 'log':
        return new LogTrans()
      case 'log10':
        return new Log10Trans()
      case 'sqrt':
        return new SqrtTrans()
      case 'reverse':
        return new ReverseTrans()
      case 'reciprocal':
      case 'inverse':
        return new ReciprocalTrans()
      case 'power':
        Number exp
        if (params.exponent) {
          exp = params.exponent as Number
        } else {
          exp = 2
        }
        return new PowerTrans(exp)
      case 'asn':
      case 'asin':
        return new AsnTrans()
      default:
        throw new IllegalArgumentException("Unknown transformation: $name")
    }
  }

  /**
   * Create transformation from closure.
   * @param forward forward transformation closure
   * @param inverse inverse transformation closure
   * @return Trans instance
   */
  static Trans fromClosures(Closure<Number> forward, Closure<Number> inverse) {
    return new Trans() {
      @Override
      BigDecimal transform(Number x) {
        if (x == null) return null
        Number result = forward.call(x)
        return result as BigDecimal
      }

      @Override
      BigDecimal inverse(Number x) {
        if (x == null) return null
        Number result = inverse.call(x)
        return result as BigDecimal
      }

      @Override
      String getName() {
        return "custom"
      }

      @Override
      List<BigDecimal> breaks(List<Number> limits, int n) {
        return null
      }
    }
  }
}
