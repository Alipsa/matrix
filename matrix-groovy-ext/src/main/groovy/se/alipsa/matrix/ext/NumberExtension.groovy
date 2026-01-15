package se.alipsa.matrix.ext

import groovy.transform.CompileStatic

import java.math.MathContext
import java.math.RoundingMode

/**
 * Extension methods for Number types to support idiomatic Groovy numeric operations.
 *
 * <p>This class provides extension methods that enable natural, chainable syntax for common
 * numeric operations in Groovy, particularly useful for data processing and statistical computations.
 * All methods work seamlessly with any Number type (Integer, Long, Double, BigDecimal, etc.),
 * automatically handling type conversions and returning BigDecimal for precision.
 *
 * <h3>Available Operations</h3>
 * <ul>
 *   <li><b>Rounding:</b> floor(), ceil() - Round to integer values as BigDecimal</li>
 *   <li><b>Logarithm:</b> log() - Natural logarithm (ln), log10() - Base-10 logarithm</li>
 *   <li><b>Exponential:</b> exp() - Natural exponential function (e^x)</li>
 *   <li><b>Square Root:</b> sqrt() - Square root with default DECIMAL64 precision</li>
 *   <li><b>Trigonometry:</b> sin(), cos() - Sine and cosine for angles in radians</li>
 *   <li><b>Precision:</b> ulp() - Unit in the last place for epsilon calculations</li>
 *   <li><b>Comparison:</b> min(), max() - Chainable min/max operations supporting mixed types</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Works with any Number type
 * Integer i = 100
 * i.log10()  // → 2.0
 *
 * Double d = 3.7
 * d.floor()  // → 3.0
 * d.ceil()   // → 4.0
 *
 * // BigDecimal for precision
 * BigDecimal value = 100G
 * value.log10()  // → 2.0
 *
 * // Square root
 * BigDecimal area = 25.0G
 * area.sqrt()  // → 5.0
 *
 * // Trigonometric functions
 * BigDecimal angle = Math.PI / 2 as BigDecimal
 * angle.sin()  // → 1.0
 * angle.cos()  // → 0.0
 *
 * // Unit in last place (for epsilon calculations)
 * BigDecimal epsilon = value.ulp() * 10
 *
 * // Chainable min/max with mixed types
 * BigDecimal binIndex = 0.max(value.min(100))  // Clamp to [0, 100]
 * BigDecimal result = someValue.min(breaks.size() - 2)  // Works with Integer
 * }</pre>
 *
 * <h3>Design Philosophy</h3>
 * <p>These extensions prioritize readability and idiomatic Groovy syntax. They enable
 * natural method chaining and work seamlessly with mixed numeric types (BigDecimal, Integer,
 * Long, Double, etc.), automatically handling type conversions.
 *
 * @since 1.0
 */
@CompileStatic
class NumberExtension {

  /** π to 16 digits is 3.1415926535897932, is precise enough for most scientific calculations */
  static final BigDecimal PI = 3.1415926535897932
  /** e (eulers number) to 30 digits is 2.718281828459045235360287471352, 16 is enough for practical use */
  static final BigDecimal E = 2.7182818284590452

  /**
   * Returns the largest integer value less than or equal to this BigDecimal.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the floor of this value
   */
  static BigDecimal floor(BigDecimal self) {
    return self.setScale(0, RoundingMode.FLOOR)
  }

  /**
   * Returns the smallest integer value greater than or equal to this BigDecimal.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the ceiling of this value
   */
  static BigDecimal ceil(BigDecimal self) {
    return self.setScale(0, RoundingMode.CEILING)
  }

  /**
   * Returns the natural logarithm (ln) of this number as a BigDecimal.
   * <p>
   * This method computes the natural logarithm (base e) of the given value.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal e = Math.E as BigDecimal
   * e.log()  // → 1.0
   *
   * BigDecimal value = 10.0
   * value.log()  // → 2.302585...
   *
   * // log is inverse of exp
   * BigDecimal x = 5.0
   * x.log().exp()  // → 5.0
   * }</pre>
   *
   * @param self the Number value (must be positive)
   * @return a BigDecimal representing the natural logarithm of this value
   * @throws IllegalArgumentException if self is not positive (value <= 0)
   */
  static BigDecimal log(Number self) {
    double value = self.doubleValue()
    if (value <= 0) {
      throw new IllegalArgumentException("Logarithm is undefined for non-positive values: ${self}")
    }
    return Math.log(value) as BigDecimal
  }

  /**
   * Returns the logarithm of this number to the specified base as a BigDecimal.
   * <p>
   * This method computes log_base(value) using the change of base formula:
   * log_base(value) = ln(value) / ln(base)
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal value = 8.0
   * value.log(2)  // → 3.0 (log base 2 of 8)
   *
   * BigDecimal value2 = 1000.0
   * value2.log(10)  // → 3.0 (log base 10 of 1000)
   *
   * BigDecimal value3 = 27.0
   * value3.log(3)  // → 3.0 (log base 3 of 27)
   * }</pre>
   *
   * @param self the Number value (must be positive)
   * @param base the logarithm base (must be positive and not equal to 1)
   * @return a BigDecimal representing the logarithm of this value to the specified base
   * @throws IllegalArgumentException if self <= 0, base <= 0, or base == 1
   */
  static BigDecimal log(Number self, Number base) {
    double value = self.doubleValue()
    double baseValue = base.doubleValue()

    if (value <= 0) {
      throw new IllegalArgumentException("Logarithm is undefined for non-positive values: ${self}")
    }
    if (baseValue <= 0) {
      throw new IllegalArgumentException("Logarithm base must be positive: ${base}")
    }
    if (baseValue == 1.0) {
      throw new IllegalArgumentException("Logarithm base cannot be 1: log base 1 is undefined")
    }

    double valueLog = Math.log(value)
    double baseLog = Math.log(baseValue)
    return (valueLog / baseLog) as BigDecimal
  }

  /**
   * Returns the base-10 logarithm (log10) of this number as a BigDecimal.
   *
   * @param self the Number value
   * @return  a BigDecimal representing the base-10 logarithm (log10) of this value
   */
  static BigDecimal log10(Number self) {
    return Math.log10(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns Euler's number e raised to the power of this value.
   * <p>
   * This method provides the natural exponential function (exp), which is the inverse
   * of the natural logarithm. It uses the power operator with Math.E as the base.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal x = 1.0G
   * x.exp()  // → 2.718281828... (Math.E)
   *
   * BigDecimal x2 = 0G
   * x2.exp()  // → 1.0
   *
   * BigDecimal x3 = 2.0G
   * x3.exp()  // → 7.389056099...
   *
   * // Works with any Number type
   * Integer i = 2
   * i.exp()  // → 7.389056099...
   * }</pre>
   *
   * @param self the exponent value (any Number type)
   * @return e raised to the power of this value, as a BigDecimal
   */
  static BigDecimal exp(Number self) {
    BigDecimal val = self as BigDecimal
    return E ** val
  }

  /**
   * Returns the size of an ulp (unit in the last place) of this BigDecimal value.
   * An ulp is the positive distance between this floating-point value and the next larger magnitude value.
   *
   * @param self the BigDecimal value
   * @return a BigDecimal representing the size of an ulp
   */
  static BigDecimal ulp(BigDecimal self) {
    return Math.ulp(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns the size of an ulp (unit in the last place) of this Number value.
   * An ulp is the positive distance between this floating-point value and the next larger magnitude value.
   *
   * @param self the Number value
   * @return a BigDecimal representing the size of an ulp
   */
  static BigDecimal ulp(Number self) {
    return Math.ulp(self.doubleValue()) as BigDecimal
  }

  /**
   * Returns the smaller of this BigDecimal and the given Number.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(BigDecimal self, Number other) {
    BigDecimal otherBD = other as BigDecimal
    return self < otherBD ? self : otherBD
  }

  /**
   * Returns the larger of this BigDecimal and the given Number.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(BigDecimal self, Number other) {
    BigDecimal otherBD = other as BigDecimal
    return self > otherBD ? self : otherBD
  }

  /**
   * Returns the smaller of this Number and the given BigDecimal.
   *
   * @param self the Number value
   * @param other the BigDecimal to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(Number self, BigDecimal other) {
    BigDecimal selfBD = self as BigDecimal
    return selfBD < other ? selfBD : other
  }

  /**
   * Returns the larger of this Number and the given BigDecimal.
   *
   * @param self the Number value
   * @param other the BigDecimal to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(Number self, BigDecimal other) {
    BigDecimal selfBD = self as BigDecimal
    return selfBD > other ? selfBD : other
  }

  /**
   * Returns the smaller of this Number and the given Number.
   * <p>
   * This method enables natural comparison syntax for any Number types,
   * including primitives like int, long, double, etc.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * int plotWidth = 640
   * int plotHeight = 480
   * BigDecimal radius = plotWidth.min(plotHeight) / 2  // → 240
   * }</pre>
   *
   * @param self the Number value
   * @param other the Number to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(Number self, Number other) {
    BigDecimal selfBD = self as BigDecimal
    BigDecimal otherBD = other as BigDecimal
    return selfBD < otherBD ? selfBD : otherBD
  }

  /**
   * Returns the larger of this Number and the given Number.
   * <p>
   * This method enables natural comparison syntax for any Number types,
   * including primitives like int, long, double, etc.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * int width = 100
   * int minWidth = 50
   * BigDecimal result = width.max(minWidth)  // → 100
   * }</pre>
   *
   * @param self the Number value
   * @param other the Number to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(Number self, Number other) {
    BigDecimal selfBD = self as BigDecimal
    BigDecimal otherBD = other as BigDecimal
    return selfBD > otherBD ? selfBD : otherBD
  }

  /**
   * Returns the square root of this BigDecimal value using DECIMAL64 precision.
   * <p>
   * This is a convenience method that provides a default MathContext for square root
   * operations, making code more readable and concise.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal area = 25.0G
   * BigDecimal side = area.sqrt()  // → 5.0
   *
   * // Instead of the more verbose:
   * BigDecimal side = area.sqrt(MathContext.DECIMAL64)
   *
   * // For higher precision (e.g., in matrix-stats):
   * BigDecimal precise = area.sqrt(MathContext.DECIMAL128)
   * }</pre>
   *
   * @param self the BigDecimal value to take the square root of
   * @return the square root as a BigDecimal with DECIMAL64 precision
   * @see MathContext#DECIMAL64
   */
  static BigDecimal sqrt(BigDecimal self) {
    return self.sqrt(MathContext.DECIMAL64)
  }

  static BigDecimal sqrt(Number self) {
    return sqrt(self as BigDecimal)
  }

  /**
   * Returns the sine of this BigDecimal value (in radians).
   * <p>
   * This method wraps {@link Math#sin(double)} and returns the result as a BigDecimal
   * for consistent type handling in Groovy numeric operations.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal angle = Math.PI / 2  // 90 degrees in radians
   * BigDecimal result = angle.sin()  // → 1.0
   *
   * BigDecimal angle2 = 0G
   * angle2.sin()  // → 0.0
   * }</pre>
   *
   * @param self the angle in radians
   * @return the sine of the angle as a BigDecimal
   */
  static BigDecimal sin(BigDecimal self) {
    //return Math.sin(self.doubleValue()) as BigDecimal
    // Normalize x to range [-2PI, 2PI] to keep series fast
    // (Optional but recommended for large angles)
    BigDecimal twoPi = PI * 2
    if (self > twoPi || self < -twoPi) {
      self = self % twoPi
    }

    BigDecimal result = self
    BigDecimal term = self
    BigDecimal xSquared = self ** 2
    int iteration = 1
    BigDecimal threshold = new BigDecimal("1e-" + MathContext.DECIMAL64.getPrecision())

    while (true) {
      // term = term * (-x^2) / ((2n)(2n+1))
      term = (term * xSquared).negate()
      BigDecimal divisor = (2 * iteration) * (2 * iteration + 1)
      term = term / divisor

      if (term.abs() < threshold) break

      result = result + term
      iteration++
    }
    return result
  }

  /**
   * Returns the cosine of this BigDecimal value (in radians).
   * <p>
   * This method wraps {@link Math#cos(double)} and returns the result as a BigDecimal
   * for consistent type handling in Groovy numeric operations.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal angle = 0G
   * BigDecimal result = angle.cos()  // → 1.0
   *
   * BigDecimal angle2 = Math.PI
   * angle2.cos()  // → -1.0
   * }</pre>
   *
   * @param self the angle in radians
   * @return the cosine of the angle as a BigDecimal
   */
  static BigDecimal cos(BigDecimal self) {
    BigDecimal twoPi = PI * 2
    if (self > twoPi || self < -twoPi) {
      self = self % twoPi
    }

    BigDecimal result = BigDecimal.ONE
    BigDecimal term = BigDecimal.ONE
    BigDecimal xSquared = self ** 2
    int iteration = 1
    BigDecimal threshold = new BigDecimal("1e-" + MathContext.DECIMAL64.getPrecision())

    while (true) {
      // term = term * (-x^2) / ((2n-1)(2n))
      term = (term * xSquared).negate()
      BigDecimal divisor = (2 * iteration - 1) * (2 * iteration)
      term = term / divisor

      if (term.abs() < threshold) break

      result = result + term
      iteration++
    }
    return result
  }


  /**
   * Converts this BigDecimal value from radians to degrees.
   *
   * @param self the angle in radians
   * @return the angle in degrees as a BigDecimal
   */
  static BigDecimal toDegrees(BigDecimal self) {
    return self * 180.0 / PI
  }

  /**
   * Converts this BigDecimal value from degrees to radians.
   *
   * @param self the angle in degrees
   * @return the angle in radians as a BigDecimal
   */
  static BigDecimal toRadians(BigDecimal self) {
    return self * PI / 180.0
  }

  static BigDecimal tan(Number self) {
    tan(self as BigDecimal)
  }

  static BigDecimal tan(BigDecimal self) {
    BigDecimal sinVal = sin(self)
    BigDecimal cosVal = cos(self)

    if (cosVal == 0) {
      throw new ArithmeticException("Tangent undefined (cos is 0)")
    }

    return sinVal / cosVal
  }


  /**
   * Returns the arctangent (inverse tangent) of this Number as a BigDecimal.
   * <p>
   * This is a convenience wrapper that converts the Number to BigDecimal.
   *
   * @param self the Number value
   * @return the arctangent of the value
   */
  static BigDecimal atan(Number self) {
    return atan(self as BigDecimal)
  }

  /**
   * Returns the arctangent of this BigDecimal with the DECIMAL64 precision.
   * <p>
   * Implements the arctangent using the argument reduction identity:
   * arctan(x) = 2 * arctan( x / (1 + sqrt(1 + x^2)) )
   * followed by a Taylor series expansion once x is sufficiently small.
   * This avoids converting to double and maintains high precision.
   *
   * @param self the BigDecimal value
   * @param mc the MathContext to use for precision and rounding
   * @return the arctangent of the value
   */
  static BigDecimal atan(BigDecimal self) {
    if (self == 0) return BigDecimal.ZERO

    // Handle negative input: atan(-x) = -atan(x)
    if (self < 0) {
      return atan(self.negate()).negate()
    }

    BigDecimal x = self
    BigDecimal multiplier = BigDecimal.ONE

    // 1. Argument Reduction
    // We shrink 'x' until it is small enough (< 0.5) for the Taylor series.
    // BUG FIX: changed condition from (x > 0) to (x > 0.5) to prevent infinite loop
    while (x > 0.5) {
      // Identity: newX = x / (1 + sqrt(1 + x^2))
      BigDecimal xSquared = x ** 2
      BigDecimal root = sqrt((1 + xSquared))
      BigDecimal denominator = 1 + root

      x = x / denominator
      multiplier = multiplier * 2
    }

    // 2. Taylor Series: x - x^3/3 + x^5/5 - x^7/7 ...
    BigDecimal result = x
    BigDecimal xSquared = x ** 2
    BigDecimal term = x
    int iteration = 1

    // Threshold: stop when changes are smaller than the precision we care about
    BigDecimal threshold = new BigDecimal("1e-" + MathContext.DECIMAL64.getPrecision())

    while (true) {
      // Calculate next numerator term: term * -x^2
      term = (term * xSquared).negate()

      // Calculate divisor: 2k + 1 (3, 5, 7...)
      BigDecimal divisor = 2 * iteration + 1
      BigDecimal step = term / divisor

      if (step.abs() < threshold) {
        break
      }
      result = result + step
      iteration++
    }
    return result * multiplier
  }

  static BigDecimal atan2(Number y, Number x) {
    atan2(y as BigDecimal, x as BigDecimal)
  }

  /**
   * Returns the angle theta (in radians) from the conversion of rectangular coordinates (x, y) to polar coordinates (r, theta).
   * This method computes the angle in radians between the positive x-axis and the point (x, y).
   * <p>
   * This is the two-argument arctangent function, which handles all four quadrants correctly.
   * The result is in the range -π to π.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * // Calculate angle of line from (1, 1) to (5, 4)
   * BigDecimal dy = 4 - 1  // 3
   * BigDecimal dx = 5 - 1  // 4
   * BigDecimal angle = dy.atan2(dx)  // Angle in radians
   *
   * // Instead of:
   * // double angle = Math.atan2(dy, dx)
   *
   * // Works with any Number types
   * Number y = 1.0
   * Number x = 1.0
   * y.atan2(x)  // → π/4 (45 degrees)
   * }</pre>
   *
   * @param self the ordinate coordinate (y)
   * @param x the abscissa coordinate (x)
   * @return the angle theta from polar coordinate (r, theta) in radians, as a BigDecimal
   */
  static BigDecimal atan2(BigDecimal y, BigDecimal x) {
    // 1. Handle special cases (x=0, y=0) to avoid division by zero
    if (x == 0) {
      if (y > 0) return PI / 2
      if (y < 0) return (PI / 2).negate()
      throw new ArithmeticException("atan2 undefined for x=0, y=0");
    }

    // 2. Calculate the ratio z = y/x
    BigDecimal z = y / x

    // 3. Calculate raw atan(z)
    BigDecimal result = atan(z)

    // 4. Adjust for Quadrants
    if (x  < 0) {
      if (y >= 0) {
        result = result + PI
      } else {
        result = result - PI
      }
    }
    return result
  }
}
