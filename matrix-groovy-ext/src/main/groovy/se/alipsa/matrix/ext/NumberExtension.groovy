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
   * @param self the Number value
   * @return a BigDecimal representing the natural logarithm of this value
   */
  static BigDecimal log(Number self) {
    return Math.log(self.doubleValue()) as BigDecimal
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
    return Math.E ** val
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
    return Math.sin(self.doubleValue()) as BigDecimal
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
    return Math.cos(self.doubleValue()) as BigDecimal
  }
}
