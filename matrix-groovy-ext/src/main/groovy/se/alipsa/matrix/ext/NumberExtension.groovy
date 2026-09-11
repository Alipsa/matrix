package se.alipsa.matrix.ext

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
 *   <li><b>Logarithm:</b> log() - Natural logarithm (ln), log(base) - Custom-base logarithm, log10() - Base-10 logarithm, log1p() - Natural logarithm of (1 + x)</li>
 *   <li><b>Exponential:</b> exp() - Natural exponential function (e^x)</li>
 *   <li><b>Square Root:</b> sqrt() - Square root with default DECIMAL64 precision</li>
 *   <li><b>Cube Root:</b> cbrt() - Cube root with default DECIMAL64 precision</li>
 *   <li><b>Hypotenuse:</b> hypot(Number) - sqrt(x² + y²) without overflow/underflow</li>
 *   <li><b>Trigonometry:</b> sin(), cos(), tan() - Trigonometric functions for angles in radians</li>
 *   <li><b>Inverse Trigonometry:</b> asin(), acos(), atan(), atan2() - Inverse trig functions returning radians</li>
 *   <li><b>Angle Conversion:</b> toDegrees(), toRadians() - Convert between radians and degrees</li>
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
 * BigDecimal doubleEpsilon = (1000.0d).ulp()  // → 1.1368683772161603E-13 (IEEE 754)
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
 * <h3>Non-finite input</h3>
 * <p>{@code BigDecimal} cannot represent NaN or infinity. Number overloads therefore
 * reject non-finite {@code Double} and {@code Float} inputs with an
 * {@link IllegalArgumentException} whose message identifies the operation and value.
 *
 * @since 0.1.0
 */
// Repeated small numeric literals are part of the published formulas here; extracting them
// into artificial constants would hurt readability more than it would help.
@SuppressWarnings(['ClassSize', 'DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class NumberExtension {

  /** π to 30 digits is 3.141592653589793238462643383279, 16 is precise enough for most scientific calculations */
  static final BigDecimal PI = 3.1415926535897932
  /** Higher-precision π constant used for internal range reduction in transcendental functions. */
  static final BigDecimal PI32 = 3.141592653589793238462643383279
  /** e (eulers number) to 30 digits is 2.718281828459045235360287471352, 16 is enough for practical use */
  static final BigDecimal E = 2.7182818284590452
  /** Higher-precision e constant used for internal computation where extra precision matters. */
  static final BigDecimal E32 = 2.718281828459045235360287471352

  /** Natural logarithm of 2 to 32 significant digits */
  private static final BigDecimal LN2 = 0.69314718055994530941723212145818
  /** Natural logarithm of 10 to 32 significant digits */
  private static final BigDecimal LN10 = 2.30258509299404568401799145468436
  /** Threshold for terminating the {@code log1p} Taylor series; conservatively below DECIMAL64 precision for |x| < 1e-10. */
  private static final BigDecimal LOG1P_THRESHOLD = 1e-34
  private static final int LOG1P_MAX_ITERATIONS = 40
  private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128
  private static final MathContext RESULT_CONTEXT = MathContext.DECIMAL64
  private static final MathContext HALF_PI_CONTEXT = new MathContext(17, RoundingMode.HALF_EVEN)
  /** Shared work and cache ceiling for adaptive-precision trigonometric range reduction. */
  private static final int MAX_TRIGONOMETRIC_PRECISION = 512
  /** Maximum absolute exponent accepted by BigDecimal.pow(int, MathContext). */
  private static final int MAX_EXP_POWER = 999_999_999
  private static final BigDecimal ONE_EIGHTY = BigDecimal.valueOf(180)
  private static final BigDecimal TWO = BigDecimal.valueOf(2)
  private static final BigDecimal THREE = BigDecimal.valueOf(3)
  private static final int CBRT_ITERATIONS = 3
  private static final BigDecimal TWO_PI = PI32 * TWO
  private static final int STATIC_PI_LIMIT = PI32.precision() - RESULT_CONTEXT.precision - 2
  private static final BigDecimal RESULT_PI = PI32.round(RESULT_CONTEXT)
  private static final BigDecimal RESULT_HALF_PI = PI32.divide(BigDecimal.valueOf(2), HALF_PI_CONTEXT)
  private static volatile BigDecimal cachedPi
  private static final BigDecimal CALCULATION_PI = calculatePi(CALCULATION_CONTEXT)

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
   * Returns the largest integer value less than or equal to this Number.
   *
   * @param self the Number value
   * @return a BigDecimal representing the floor of this value
   * @see #floor(BigDecimal)
   */
  static BigDecimal floor(Number self) {
    floor(toBigDecimal(self, 'floor'))
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
   * Returns the smallest integer value greater than or equal to this Number.
   *
   * @param self the Number value
   * @return a BigDecimal representing the ceiling of this value
   * @see #ceil(BigDecimal)
   */
  static BigDecimal ceil(Number self) {
    ceil(toBigDecimal(self, 'ceil'))
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
   * @param self the BigDecimal value (must be positive)
   * @return a BigDecimal representing the natural logarithm of this value
   * @throws IllegalArgumentException if self is not positive (value <= 0)
   */
  static BigDecimal log(BigDecimal self) {
    if (self <= 0) {
      throw new IllegalArgumentException("Logarithm is undefined for non-positive values: ${self}")
    }
    if (self == BigDecimal.ONE) {
      return BigDecimal.ZERO
    }
    lnSeries(self).round(MathContext.DECIMAL64)
  }

  /**
   * Returns the natural logarithm (ln) of this number as a BigDecimal.
   *
   * @param self the Number value (must be positive)
   * @return a BigDecimal representing the natural logarithm of this value
   * @see #log(BigDecimal)
   */
  static BigDecimal log(Number self) {
    log(toBigDecimal(self, 'log'))
  }

  /**
   * Internal natural logarithm computation with DECIMAL128 precision.
   * Uses argument reduction and the series ln(x) = 2(y + y³/3 + y⁵/5 + ...)
   * where y = (x-1)/(x+1).
   */
  private static BigDecimal lnSeries(BigDecimal value) {
    MathContext mc = CALCULATION_CONTEXT
    BigDecimal two = 2
    // Argument reduction: scale to [1, 10) using BigDecimal magnitude, then to [0.5, 2.0] with powers of 2
    int tenPower = value.precision() - value.scale() - 1
    BigDecimal x = tenPower != 0 ? value.scaleByPowerOfTen(-tenPower) : value
    int twoPower = 0
    while (x > two) {
      x = x.divide(two, mc)
      twoPower++
    }
    while (x < 0.5) {
      x = x.multiply(two, mc)
      twoPower--
    }

    BigDecimal y = (x - 1).divide(x + 1, mc)
    BigDecimal ySquared = y.multiply(y, mc)
    BigDecimal term = y
    BigDecimal result = y
    int iteration = 1
    BigDecimal threshold = new BigDecimal("1e-${mc.precision}")

    while (true) {
      term = term.multiply(ySquared, mc)
      BigDecimal step = term.divide(new BigDecimal(2 * iteration + 1), mc)
      if (step.abs() < threshold) {
        break
      }
      result = result.add(step)
      iteration++
    }

    result.multiply(two, mc)
        .add(LN10.multiply(BigDecimal.valueOf(tenPower), mc), mc)
        .add(LN2.multiply(BigDecimal.valueOf(twoPower), mc), mc)
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
   * @param self the BigDecimal value (must be positive)
   * @param base the logarithm base (must be positive and not equal to 1)
   * @return a BigDecimal representing the logarithm of this value to the specified base
   * @throws IllegalArgumentException if self <= 0, base <= 0, or base == 1
   */
  static BigDecimal log(BigDecimal self, Number base) {
    if (self <= 0) {
      throw new IllegalArgumentException("Logarithm is undefined for non-positive values: ${self}")
    }
    BigDecimal baseValue = toBigDecimal(base, 'log')

    if (baseValue <= 0) {
      throw new IllegalArgumentException("Logarithm base must be positive: ${base}")
    }
    if (baseValue == BigDecimal.ONE) {
      throw new IllegalArgumentException('Logarithm base cannot be 1: log base 1 is undefined')
    }

    BigDecimal lnBase = switch (baseValue) {
      case BigDecimal.TEN -> LN10
      case TWO -> LN2
      default -> lnSeries(baseValue)
    }
    lnSeries(self).divide(lnBase, MathContext.DECIMAL64)
  }

  /**
   * Returns the logarithm of this number to the specified base as a BigDecimal.
   *
   * @param self the Number value (must be positive)
   * @param base the logarithm base (must be positive and not equal to 1)
   * @return a BigDecimal representing the logarithm of this value to the specified base
   * @see #log(BigDecimal, Number)
   */
  static BigDecimal log(Number self, Number base) {
    log(toBigDecimal(self, 'log'), base)
  }

  /**
   * Returns the natural logarithm of (1 + x), i.e. ln(1 + self).
   *
   * <p>For values where {@code abs &lt; 1e-10} (strictly less than), this method uses the Taylor series to avoid
   * catastrophic cancellation. The series terminates when a term's magnitude drops below 1e-34, so the omitted
   * term is smaller than that threshold before DECIMAL64 rounding.
   * For {@code abs &gt;= 1e-10} it computes {@code log(self + 1)} using the BigDecimal series.
   * Both paths return values rounded to {@link MathContext#DECIMAL64}.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal zero = 0.0
   * zero.log1p()  // → 0.0
   *
   * BigDecimal eMinusOne = NumberExtension.E - 1
   * eMinusOne.log1p()  // → 1.0
   * }</pre>
   *
   * @param self the BigDecimal value (must be greater than -1)
   * @return a BigDecimal representing ln(1 + self), rounded to DECIMAL64 precision
   * @throws IllegalArgumentException if self &lt;= -1 (ln(1+x) requires x &gt; -1)
   */
  static BigDecimal log1p(BigDecimal self) {
    if (self <= -1) {
      throw new IllegalArgumentException("log1p is undefined for values <= -1 (ln(1+x) requires x > -1): ${self}")
    }
    if (self.abs() < 1e-10) {
      log1pSmall(self).round(MathContext.DECIMAL64)
    } else {
      log(self + 1)
    }
  }

  /* Computes ln(1+x) for very small x using the Taylor series x - x^2/2 + x^3/3 - ...
   * For |x| < 1e-10 the series converges in ≤4 iterations (term 4 ≈ x^4/4 ≈ 2.5e-41 < LOG1P_THRESHOLD);
   * the 40-iteration cap is a safety net that is effectively unreachable at this threshold. */
  private static BigDecimal log1pSmall(BigDecimal value) {
    MathContext mc = MathContext.DECIMAL128
    BigDecimal term = value
    BigDecimal result = value
    int n = 2
    boolean subtractTerm = true  // first loop term (n=2) subtracts its step from result
    while (n <= LOG1P_MAX_ITERATIONS + 1) {
      term = term.multiply(value, mc)
      BigDecimal step = term.divide(BigDecimal.valueOf(n), mc)
      if (step.abs() < LOG1P_THRESHOLD) {
        // Remaining terms form a geometric series with ratio |value| < 1e-10;
        // their sum is bounded by step / (1 - |value|) ≈ step < LOG1P_THRESHOLD.
        // The current term is omitted because it is already inside that bound.
        break
      }
      result = subtractTerm ? result.subtract(step, mc) : result.add(step, mc)
      subtractTerm = !subtractTerm
      n++
    }
    // Safety net for future threshold changes that no longer guarantee early termination.
    if (n > LOG1P_MAX_ITERATIONS + 1) {
      throw new IllegalStateException("log1pSmall did not converge within $LOG1P_MAX_ITERATIONS iterations for input: $value")
    }
    result
  }

  /**
   * Returns the natural logarithm of (1 + x), i.e. ln(1 + self).
   *
   * @param self the Number value (must be greater than -1)
   * @return a BigDecimal representing ln(1 + self)
   * @see #log1p(BigDecimal)
   */
  static BigDecimal log1p(Number self) {
    log1p(toBigDecimal(self, 'log1p'))
  }

  /**
   * Returns the base-10 logarithm (log10) of this number as a BigDecimal.
   *
   * @param self the BigDecimal value (must be positive)
   * @return a BigDecimal representing the base-10 logarithm (log10) of this value
   * @throws IllegalArgumentException if self is not positive (value <= 0)
   */
  static BigDecimal log10(BigDecimal self) {
    if (self <= 0) {
      throw new IllegalArgumentException("Logarithm is undefined for non-positive values: ${self}")
    }
    if (self == BigDecimal.ONE) {
      return BigDecimal.ZERO
    }
    lnSeries(self).divide(LN10, MathContext.DECIMAL64)
  }

  /**
   * Returns the base-10 logarithm (log10) of this number as a BigDecimal.
   *
   * @param self the Number value (must be positive)
   * @return a BigDecimal representing the base-10 logarithm (log10) of this value
   * @see #log10(BigDecimal)
   */
  static BigDecimal log10(Number self) {
    log10(toBigDecimal(self, 'log10'))
  }

  /**
   * Returns Euler's number e raised to the power of this value.
   * <p>
   * This method provides the natural exponential function (exp), which is the inverse
   * of the natural logarithm. Uses argument reduction and Taylor series for pure BigDecimal
   * precision: e<sup>x</sup> = e<sup>k</sup> &middot; e<sup>r</sup> where k = round(x) and r = x - k.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal x = 1.0G
   * x.exp()  // → 2.718281828... (e)
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
   * @throws ArithmeticException if |round(self)| exceeds the BigDecimal.pow limit of 999999999
   */
  static BigDecimal exp(BigDecimal self) {
    if (self == 0) {
      return BigDecimal.ONE
    }

    // Argument reduction: e^x = e^k * e^r where k = round(x), r = x - k, |r| <= 0.5
    // e^k uses BigDecimal.pow(int) for full precision; e^r uses Taylor series (fast convergence)
    long roundedExponent
    try {
      roundedExponent = self.setScale(0, RoundingMode.HALF_EVEN).longValueExact()
    } catch (ArithmeticException ignored) {
      throw new ArithmeticException("Exponent too large for exp(): $self")
    }
    if (roundedExponent > MAX_EXP_POWER || roundedExponent < -MAX_EXP_POWER) {
      throw new ArithmeticException("Exponent too large for exp(): $self")
    }

    int k = roundedExponent as int
    BigDecimal r = self - k

    // Taylor series: e^r = 1 + r + r²/2! + r³/3! + ...
    BigDecimal term = BigDecimal.ONE
    BigDecimal result = BigDecimal.ONE
    int iteration = 1
    BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-CALCULATION_CONTEXT.precision)

    while (true) {
      term = term.multiply(r, CALCULATION_CONTEXT)
          .divide(BigDecimal.valueOf(iteration), CALCULATION_CONTEXT)
      if (term.abs() < threshold) {
        break
      }
      result = result.add(term, CALCULATION_CONTEXT)
      iteration++
    }

    // Combine: e^k * e^r
    BigDecimal eToK = k >= 0
        ? E32.pow(k, CALCULATION_CONTEXT)
        : BigDecimal.ONE.divide(E32.pow(-k, CALCULATION_CONTEXT), CALCULATION_CONTEXT)

    (eToK * result).round(RESULT_CONTEXT)
  }

  /**
   * Returns Euler's number e raised to the power of this value.
   *
   * @param self the exponent value (any Number type)
   * @return e raised to the power of this value, as a BigDecimal
   * @see #exp(BigDecimal)
   */
  static BigDecimal exp(Number self) {
    exp(toBigDecimal(self, 'exp'))
  }

  /**
   * Returns the size of an ulp (unit in the last place) of this Number value.
   * <p>
   * Dispatch is by runtime type: {@code Double} and {@code Float} return their IEEE 754
   * ulp; every other Number type is treated as a decimal and returns 10<sup>-scale</sup>,
   * the positional value of its least significant digit.
   * <p>
   * This is deliberately the only public {@code ulp} overload. Additional typed overloads
   * make direct static invocation ambiguous under dynamic Groovy, which silently selected
   * the {@code Float} overload for {@code Integer} and {@code Long} arguments.
   *
   * @param self the Number value
   * @return a BigDecimal representing the size of an ulp
   */
  static BigDecimal ulp(Number self) {
    switch (self) {
      case Double -> {
        requireFinite(self, 'ulp')
        floatingPointUlp((Double) self)
      }
      case Float -> {
        requireFinite(self, 'ulp')
        floatingPointUlp((Float) self)
      }
      default -> decimalUlp(toBigDecimal(self, 'ulp'))
    }
  }

  /** Decimal ulp: 10<sup>-scale</sup>, the positional value of the least significant digit. */
  private static BigDecimal decimalUlp(BigDecimal self) {
    BigDecimal.ONE.scaleByPowerOfTen(-self.scale())
  }

  /** IEEE 754 ulp of a double value. */
  private static BigDecimal floatingPointUlp(Double self) {
    BigDecimal.valueOf(Math.ulp(self))
  }

  /** IEEE 754 ulp of a float value, widened to double. */
  private static BigDecimal floatingPointUlp(Float self) {
    BigDecimal.valueOf(Math.ulp(self) as double)
  }

  /**
   * Returns the smaller of this BigDecimal and the given Number.
   * <p>On a numeric tie this overload returns {@code other}; this differs from
   * {@link BigDecimal#min(BigDecimal)}, which returns its receiver. The difference is
   * observable when the equal values have different scales.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the smaller value as a BigDecimal
   */
  static BigDecimal min(BigDecimal self, Number other) {
    BigDecimal otherBD = toBigDecimal(other, 'min')
    return self < otherBD ? self : otherBD
  }

  /**
   * Returns the larger of this BigDecimal and the given Number.
   * <p>On a numeric tie this overload returns {@code other}; this differs from
   * {@link BigDecimal#max(BigDecimal)}, which returns its receiver. The difference is
   * observable when the equal values have different scales.
   *
   * @param self the BigDecimal value
   * @param other the Number to compare with
   * @return the larger value as a BigDecimal
   */
  static BigDecimal max(BigDecimal self, Number other) {
    BigDecimal otherBD = toBigDecimal(other, 'max')
    return self > otherBD ? self : otherBD
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
    BigDecimal selfBD = toBigDecimal(self, 'min')
    BigDecimal otherBD = toBigDecimal(other, 'min')
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
    BigDecimal selfBD = toBigDecimal(self, 'max')
    BigDecimal otherBD = toBigDecimal(other, 'max')
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
   * @throws IllegalArgumentException if self is negative
   * @see MathContext#DECIMAL64
   */
  static BigDecimal sqrt(BigDecimal self) {
    if (self < 0) {
      throw new IllegalArgumentException("sqrt undefined for negative value: ${self}")
    }
    return self.sqrt(MathContext.DECIMAL64)
  }

  /**
   * Returns the square root of this Number using DECIMAL64 precision.
   * <p>
   * This overload allows Integer, Long, Double, and other Number receivers to use the
   * same extension syntax as BigDecimal values.
   *
   * @param self the Number value to take the square root of
   * @return the square root as a BigDecimal with DECIMAL64 precision
   * @throws IllegalArgumentException if self is negative
   * @see #sqrt(BigDecimal)
   */
  static BigDecimal sqrt(Number self) {
    return sqrt(toBigDecimal(self, 'sqrt'))
  }

  /**
   * Returns the cube root of this BigDecimal value using DECIMAL64 precision.
   * <p>
   * Computes the real cube root, including negative inputs
   * (e.g. {@code (-8).cbrt() == -2}).
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal volume = 27.0G
   * BigDecimal side = volume.cbrt()  // → 3.0
   *
   * // Negative values are supported
   * BigDecimal negative = -8.0G
   * BigDecimal root = negative.cbrt()  // → -2.0
   * }</pre>
   *
   * @param self the BigDecimal value to take the cube root of
   * @return the cube root as a BigDecimal with DECIMAL64 precision
   * @see MathContext#DECIMAL64
   */
  static BigDecimal cbrt(BigDecimal self) {
    if (self == BigDecimal.ZERO) {
      return BigDecimal.ZERO
    }
    BigDecimal absValue = self.abs()
    // Scale the value by powers of 1000 so its magnitude falls in double range,
    // compute the cube root, then scale back. This handles values far beyond
    // Double.MAX_VALUE or smaller than Double.MIN_NORMAL.
    int exponent = absValue.precision() - absValue.scale() - 1
    int k = Math.floorDiv(exponent, 3)
    BigDecimal scaled = absValue.movePointLeft(3 * k)
    // Seed from double-precision Math.cbrt (~16 correct digits), then refine with
    // Newton-Raphson: x_{n+1} = (2*x_n + a/x_n^2) / 3. Each step roughly doubles the
    // correct digit count, so three steps saturate DECIMAL128 guard precision.
    BigDecimal x = BigDecimal.valueOf(Math.cbrt(scaled.doubleValue()))
    for (int i = 0; i < CBRT_ITERATIONS; i++) {
      BigDecimal xSquared = x.multiply(x, CALCULATION_CONTEXT)
      x = x.multiply(TWO, CALCULATION_CONTEXT)
          .add(scaled.divide(xSquared, CALCULATION_CONTEXT), CALCULATION_CONTEXT)
          .divide(THREE, CALCULATION_CONTEXT)
    }
    BigDecimal root = x.movePointRight(k).round(RESULT_CONTEXT)
    self.signum() >= 0 ? root : root.negate()
  }

  /**
   * Returns the cube root of this Number using DECIMAL64 precision.
   *
   * @param self the Number value to take the cube root of
   * @return the cube root as a BigDecimal with DECIMAL64 precision
   * @see #cbrt(BigDecimal)
   */
  static BigDecimal cbrt(Number self) {
    cbrt(toBigDecimal(self, 'cbrt'))
  }

  /**
   * Returns the hypotenuse of a right-angled triangle with sides {@code x} and {@code y}
   * without undue overflow or underflow.
   * <p>
   * Equivalent to {@code sqrt(x² + y²)} but scales the calculation to keep intermediate
   * values within range. The computation uses DECIMAL128 guard precision and rounds
   * once to DECIMAL64 when returning the result.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal dx = 3.0G
   * BigDecimal dy = 4.0G
   * BigDecimal distance = dx.hypot(dy)  // → 5.0
   * }</pre>
   *
   * @param x the first side
   * @param y the second side
   * @return sqrt(x² + y²) as a BigDecimal with DECIMAL64 precision
   * @see MathContext#DECIMAL64
   */
  static BigDecimal hypot(BigDecimal x, BigDecimal y) {
    BigDecimal ax = x.abs()
    BigDecimal ay = y.abs()
    if (ax == BigDecimal.ZERO && ay == BigDecimal.ZERO) {
      return BigDecimal.ZERO
    }
    if (ax == BigDecimal.ZERO) {
      return ay.round(RESULT_CONTEXT)
    }
    if (ay == BigDecimal.ZERO) {
      return ax.round(RESULT_CONTEXT)
    }
    BigDecimal larger = ax.max(ay)
    BigDecimal smaller = ax.min(ay)
    BigDecimal ratio = smaller.divide(larger, CALCULATION_CONTEXT)
    BigDecimal factor = BigDecimal.ONE
        .add(ratio.multiply(ratio, CALCULATION_CONTEXT), CALCULATION_CONTEXT)
        .sqrt(CALCULATION_CONTEXT)
    larger.multiply(factor, CALCULATION_CONTEXT).round(RESULT_CONTEXT)
  }

  /**
   * Returns the hypotenuse of a right-angled triangle with sides {@code x} and {@code y}.
   *
   * @param x the first side
   * @param y the second side
   * @return sqrt(x² + y²) as a BigDecimal with DECIMAL64 precision
   * @see #hypot(BigDecimal, BigDecimal)
   */
  static BigDecimal hypot(Number x, Number y) {
    hypot(toBigDecimal(x, 'hypot'), toBigDecimal(y, 'hypot'))
  }

  /**
   * Reduces an angle modulo 2π. The π precision grows with the integer part of the angle,
   * preserving enough fractional digits for a DECIMAL64 trigonometric result.
   */
  private static BigDecimal reduceAngle(BigDecimal angle) {
    if (angle.abs() <= TWO_PI) {
      return angle
    }

    long integerDigits = (angle.precision() as long) - angle.scale()
    if (integerDigits <= STATIC_PI_LIMIT) {
      return angle.remainder(TWO_PI)
    }

    long requestedPrecision = integerDigits + CALCULATION_CONTEXT.precision + 8
    if (requestedPrecision > MAX_TRIGONOMETRIC_PRECISION) {
      throw new ArithmeticException("Angle magnitude is too large for trigonometric range reduction: ${angle}")
    }

    int precision = Math.max(CALCULATION_CONTEXT.precision, requestedPrecision as int)
    MathContext reductionContext = new MathContext(precision, RoundingMode.HALF_EVEN)
    BigDecimal preciseTwoPi = calculatePi(reductionContext).multiply(BigDecimal.valueOf(2), reductionContext)
    angle.remainder(preciseTwoPi)
  }

  /** Computes π using Machin's formula at the requested precision. */
  private static BigDecimal calculatePi(MathContext context) {
    BigDecimal cached = cachedPi
    if (cached != null && cached.precision() >= context.precision) {
      return cached.round(context)
    }
    calculateAndCachePi(context)
  }

  /** Computes and caches π after rechecking the cache while holding the class monitor. */
  private static synchronized BigDecimal calculateAndCachePi(MathContext context) {
    BigDecimal cached = cachedPi
    if (cached != null && cached.precision() >= context.precision) {
      return cached.round(context)
    }

    int workPrecision
    try {
      workPrecision = Math.addExact(context.precision, 8)
    } catch (ArithmeticException ignored) {
      throw new ArithmeticException("Requested precision is too large for π calculation: ${context.precision}")
    }
    MathContext workContext = new MathContext(workPrecision, context.roundingMode)
    BigDecimal firstTerm = arctanInverse(5, workContext).multiply(BigDecimal.valueOf(16), workContext)
    BigDecimal secondTerm = arctanInverse(239, workContext).multiply(BigDecimal.valueOf(4), workContext)
    BigDecimal calculated = firstTerm.subtract(secondTerm, workContext).round(context)
    cachePi(calculated, context)
    calculated
  }

  /** Retains calculated π only within the bounded cache precision. */
  private static void cachePi(BigDecimal calculated, MathContext context) {
    int cachedPrecision = cachedPi?.precision() ?: 0
    if (context.precision <= MAX_TRIGONOMETRIC_PRECISION && calculated.precision() > cachedPrecision) {
      cachedPi = calculated
    }
  }

  /** Computes arctan(1 / inverse) using its alternating Taylor series. */
  private static BigDecimal arctanInverse(int inverse, MathContext context) {
    BigDecimal inverseValue = BigDecimal.valueOf(inverse)
    BigDecimal inverseSquared = inverseValue * inverseValue
    BigDecimal term = BigDecimal.ONE.divide(inverseValue, context)
    BigDecimal result = term
    BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-context.precision)
    int iteration = 1

    while (true) {
      term = term.divide(inverseSquared, context).negate()
      BigDecimal step = term.divide(BigDecimal.valueOf(2L * iteration + 1), context)
      if (step.abs() < threshold) {
        break
      }
      result = result.add(step, context)
      iteration++
    }
    result
  }

  /**
   * Returns the sine of this BigDecimal value (in radians).
   * <p>
   * This method uses higher-precision range reduction followed by a Taylor series expansion
   * to retain accuracy for large angles while returning a BigDecimal result.
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
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   */
  static BigDecimal sin(BigDecimal self) {
    sinInternal(self).round(RESULT_CONTEXT)
  }

  /** Computes sine with guard precision for use by derived functions. */
  private static BigDecimal sinInternal(BigDecimal self) {
    BigDecimal reducedAngle = reduceAngle(self).round(CALCULATION_CONTEXT)

    BigDecimal result = reducedAngle
    BigDecimal term = reducedAngle
    BigDecimal xSquared = reducedAngle.multiply(reducedAngle, CALCULATION_CONTEXT)
    int iteration = 1
    BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-CALCULATION_CONTEXT.precision)

    while (true) {
      // term = term * (-x^2) / ((2n)(2n+1))
      term = term.multiply(xSquared, CALCULATION_CONTEXT).negate()
      BigDecimal divisor = BigDecimal.valueOf((2L * iteration) * (2L * iteration + 1))
      term = term.divide(divisor, CALCULATION_CONTEXT)

      if (term.abs() < threshold) {
        break
      }

      result = result.add(term, CALCULATION_CONTEXT)
      iteration++
    }
    result
  }

  /**
   * Returns the sine of this Number value (in radians).
   *
   * @param self the angle in radians
   * @return the sine of the angle as a BigDecimal
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   * @see #sin(BigDecimal)
   */
  static BigDecimal sin(Number self) {
    sin(toBigDecimal(self, 'sin'))
  }

  /**
   * Returns the cosine of this BigDecimal value (in radians).
   * <p>
   * This method uses higher-precision range reduction followed by a Taylor series expansion
   * to retain accuracy for large angles while returning a BigDecimal result.
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
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   */
  static BigDecimal cos(BigDecimal self) {
    cosInternal(self).round(RESULT_CONTEXT)
  }

  /** Computes cosine with guard precision for use by derived functions. */
  private static BigDecimal cosInternal(BigDecimal self) {
    BigDecimal reducedAngle = reduceAngle(self).round(CALCULATION_CONTEXT)

    BigDecimal result = BigDecimal.ONE
    BigDecimal term = BigDecimal.ONE
    BigDecimal xSquared = reducedAngle.multiply(reducedAngle, CALCULATION_CONTEXT)
    int iteration = 1
    BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-CALCULATION_CONTEXT.precision)

    while (true) {
      // term = term * (-x^2) / ((2n-1)(2n))
      term = term.multiply(xSquared, CALCULATION_CONTEXT).negate()
      BigDecimal divisor = BigDecimal.valueOf((2L * iteration - 1) * (2L * iteration))
      term = term.divide(divisor, CALCULATION_CONTEXT)

      if (term.abs() < threshold) {
        break
      }

      result = result.add(term, CALCULATION_CONTEXT)
      iteration++
    }
    result
  }

  /**
   * Returns the cosine of this Number value (in radians).
   *
   * @param self the angle in radians
   * @return the cosine of the angle as a BigDecimal
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   * @see #cos(BigDecimal)
   */
  static BigDecimal cos(Number self) {
    cos(toBigDecimal(self, 'cos'))
  }

  /**
   * Converts this BigDecimal value from radians to degrees.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * NumberExtension.PI32.toDegrees()  // → 180.0000000000000
   *
   * BigDecimal radians = 1G
   * radians.toDegrees()  // → 57.29577951308232
   * }</pre>
   * <p>The conversion uses the internal high-precision π value and returns a
   * DECIMAL64-rounded result; zero is returned in canonical form. It does not use the
   * lower-precision public {@link #PI} constant.
   *
   * @param self the angle in radians
   * @return the angle in degrees as a BigDecimal
   */
  static BigDecimal toDegrees(BigDecimal self) {
    if (self.signum() == 0) {
      return BigDecimal.ZERO
    }
    self.multiply(ONE_EIGHTY, CALCULATION_CONTEXT)
        .divide(CALCULATION_PI, CALCULATION_CONTEXT)
        .round(RESULT_CONTEXT)
  }

  /**
   * Converts this Number value from radians to degrees.
   *
   * @param self the angle in radians
   * @return the angle in degrees as a BigDecimal
   * @see #toDegrees(BigDecimal)
   */
  static BigDecimal toDegrees(Number self) {
    toDegrees(toBigDecimal(self, 'toDegrees'))
  }

  /**
   * Converts this BigDecimal value from degrees to radians.
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal degrees = 180G
   * degrees.toRadians()  // → 3.141592653589793
   *
   * BigDecimal angle = 1G
   * angle.toRadians()  // → 0.01745329251994330
   * }</pre>
   * <p>The conversion uses the internal high-precision π value and returns a
   * DECIMAL64-rounded result; zero is returned in canonical form. It does not use the
   * lower-precision public {@link #PI} constant.
   *
   * @param self the angle in degrees
   * @return the angle in radians as a BigDecimal
   */
  static BigDecimal toRadians(BigDecimal self) {
    if (self.signum() == 0) {
      return BigDecimal.ZERO
    }
    self.multiply(CALCULATION_PI, CALCULATION_CONTEXT)
        .divide(ONE_EIGHTY, CALCULATION_CONTEXT)
        .round(RESULT_CONTEXT)
  }

  /**
   * Converts this Number value from degrees to radians.
   *
   * @param self the angle in degrees
   * @return the angle in radians as a BigDecimal
   * @see #toRadians(BigDecimal)
   */
  static BigDecimal toRadians(Number self) {
    toRadians(toBigDecimal(self, 'toRadians'))
  }

  /**
   * Returns the tangent of this Number value (in radians).
   *
   * @param self the angle in radians
   * @return the tangent of the angle as a BigDecimal
   * @see #tan(BigDecimal)
   * @throws ArithmeticException if the internal cosine approximation is exactly zero
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   */
  static BigDecimal tan(Number self) {
    tan(toBigDecimal(self, 'tan'))
  }

  /**
   * Returns the tangent of this BigDecimal value (in radians).
   * <p>
   * Tangent is computed as {@code sin(x) / cos(x)} using the corresponding BigDecimal
   * implementations. A BigDecimal cannot represent an exact irrational tangent pole,
   * and an exact-zero cosine approximation is not expected, so values near odd
   * multiples of π/2 produce a large finite result. Its magnitude is limited by how
   * precisely the caller represented the angle.
   *
   * @param self the angle in radians
   * @return the tangent of the angle as a BigDecimal
   * @throws ArithmeticException if the internal cosine approximation is exactly zero
   * @throws ArithmeticException if the angle magnitude requires more than 512 digits of π for range reduction (roughly 1E+470 and above)
   */
  static BigDecimal tan(BigDecimal self) {
    BigDecimal sinVal = sinInternal(self)
    BigDecimal cosVal = cosInternal(self)

    if (cosVal == 0) {
      // Defensive only: an exact zero from the DECIMAL128 approximation is not
      // expected and was not observed. Guards against future precision changes.
      throw new ArithmeticException('Tangent undefined (cos is 0)')
    }

    sinVal.divide(cosVal, CALCULATION_CONTEXT).round(RESULT_CONTEXT)
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
    return atan(toBigDecimal(self, 'atan'))
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
   * @return the arctangent of the value
   */
  static BigDecimal atan(BigDecimal self) {
    atanInternal(self).round(RESULT_CONTEXT)
  }

  /** Computes arctangent with guard precision for use by derived functions. */
  private static BigDecimal atanInternal(BigDecimal self) {
    if (self == 0) {
      return BigDecimal.ZERO
    }

    // Handle negative input: atan(-x) = -atan(x)
    if (self < 0) {
      return atanInternal(self.negate()).negate()
    }

    BigDecimal x = self
    BigDecimal multiplier = BigDecimal.ONE

    // Reduce until x <= 0.5 so the Taylor series below converges quickly.
    while (x > 0.5) {
      // Identity: newX = x / (1 + sqrt(1 + x^2))
      BigDecimal xSquared = x.multiply(x, CALCULATION_CONTEXT)
      BigDecimal root = BigDecimal.ONE.add(xSquared, CALCULATION_CONTEXT).sqrt(CALCULATION_CONTEXT)
      BigDecimal denominator = BigDecimal.ONE.add(root, CALCULATION_CONTEXT)

      x = x.divide(denominator, CALCULATION_CONTEXT)
      multiplier *= 2
    }

    // 2. Taylor Series: x - x^3/3 + x^5/5 - x^7/7 ...
    BigDecimal result = x
    BigDecimal xSquared = x.multiply(x, CALCULATION_CONTEXT)
    BigDecimal term = x
    int iteration = 1

    // Threshold: stop when changes are smaller than the precision we care about
    BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-CALCULATION_CONTEXT.precision)

    while (true) {
      // Calculate next numerator term: term * -x^2
      term = term.multiply(xSquared, CALCULATION_CONTEXT).negate()

      // Calculate divisor: 2k + 1 (3, 5, 7...)
      BigDecimal divisor = BigDecimal.valueOf(2L * iteration + 1)
      BigDecimal step = term.divide(divisor, CALCULATION_CONTEXT)

      if (step.abs() < threshold) {
        break
      }
      result = result.add(step, CALCULATION_CONTEXT)
      iteration++
    }
    result.multiply(multiplier, CALCULATION_CONTEXT)
  }

  /**
   * Returns the arcsine (inverse sine) of this Number as a BigDecimal.
   * <p>
   * This is a convenience wrapper that converts the Number to BigDecimal.
   *
   * @param self the Number value (must be in [-1, 1])
   * @return the arcsine of the value in radians
   * @throws IllegalArgumentException if the value is outside [-1, 1]
   */
  static BigDecimal asin(Number self) {
    asin(toBigDecimal(self, 'asin'))
  }

  /**
   * Returns the arcsine (inverse sine) of this BigDecimal value.
   * <p>
   * Uses the identity: asin(x) = atan(x / sqrt(1 - x²))
   * The result is in the range [-π/2, π/2].
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal val = 0.5
   * val.asin()  // → π/6 (approximately 0.5236)
   *
   * BigDecimal one = 1.0
   * one.asin()  // → π/2
   * }</pre>
   *
   * @param self the BigDecimal value (must be in [-1, 1])
   * @return the arcsine of the value in radians as a BigDecimal
   * @throws IllegalArgumentException if the value is outside [-1, 1]
   */
  static BigDecimal asin(BigDecimal self) {
    if (self < -1 || self > 1) {
      throw new IllegalArgumentException("asin undefined for value outside [-1, 1]: ${self}")
    }
    if (self == 0) {
      return BigDecimal.ZERO
    }
    if (self == 1) {
      return RESULT_HALF_PI
    }
    if (self == -1) {
      return RESULT_HALF_PI.negate()
    }
    // asin(x) = atan(x / sqrt(1 - x²))
    BigDecimal xSquared = self.multiply(self, CALCULATION_CONTEXT)
    BigDecimal denominator = BigDecimal.ONE.subtract(xSquared, CALCULATION_CONTEXT).sqrt(CALCULATION_CONTEXT)
    atanInternal(self.divide(denominator, CALCULATION_CONTEXT)).round(RESULT_CONTEXT)
  }

  /**
   * Returns the arccosine (inverse cosine) of this BigDecimal value.
   * <p>
   * Uses the identity: acos(x) = π/2 - asin(x)
   * The result is in the range [0, π].
   *
   * <h3>Usage Example</h3>
   * <pre>{@code
   * BigDecimal val = 0.5
   * val.acos()  // → π/3 (approximately 1.0472)
   *
   * BigDecimal one = 1.0
   * one.acos()  // → 0
   * }</pre>
   *
   * @param self the BigDecimal value (must be in [-1, 1])
   * @return the arccosine of the value in radians as a BigDecimal
   * @throws IllegalArgumentException if the value is outside [-1, 1]
   */
  static BigDecimal acos(BigDecimal self) {
    if (self < -1 || self > 1) {
      throw new IllegalArgumentException("acos undefined for value outside [-1, 1]: ${self}")
    }
    if (self == 1) {
      return BigDecimal.ZERO
    }
    if (self == -1) {
      return RESULT_PI
    }
    if (self == 0) {
      return RESULT_HALF_PI
    }
    // This form avoids subtractive cancellation near 1:
    // acos(x) = 2 * atan(sqrt((1 - x) / (1 + x)))
    BigDecimal numerator = BigDecimal.ONE.subtract(self, CALCULATION_CONTEXT)
    BigDecimal denominator = BigDecimal.ONE.add(self, CALCULATION_CONTEXT)
    BigDecimal ratio = numerator.divide(denominator, CALCULATION_CONTEXT)
    BigDecimal root = ratio.sqrt(CALCULATION_CONTEXT)
    atanInternal(root).multiply(BigDecimal.valueOf(2), CALCULATION_CONTEXT).round(RESULT_CONTEXT)
  }

  /**
   * Returns the arccosine (inverse cosine) of this Number value.
   * <p>
   * Convenience wrapper that converts the Number to BigDecimal.
   *
   * @param self the Number value (must be in [-1, 1])
   * @return the arccosine of the value in radians
   * @throws IllegalArgumentException if the value is outside [-1, 1]
   */
  static BigDecimal acos(Number self) {
    acos(toBigDecimal(self, 'acos'))
  }

  /**
   * Returns the angle theta (in radians) from the conversion of rectangular
   * coordinates (x, y) to polar coordinates (r, theta).
   *
   * @param y the ordinate coordinate
   * @param x the abscissa coordinate
   * @return the angle theta in radians, in the range -π to π, as a BigDecimal
   * @see #atan2(BigDecimal, BigDecimal)
   */
  static BigDecimal atan2(Number y, Number x) {
    atan2(toBigDecimal(y, 'atan2'), toBigDecimal(x, 'atan2'))
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
   * @param y the ordinate coordinate
   * @param x the abscissa coordinate (x)
   * @return the angle theta from polar coordinate (r, theta) in radians, as a BigDecimal
   */
  static BigDecimal atan2(BigDecimal y, BigDecimal x) {
    // 1. Handle special cases (x=0, y=0) to avoid division by zero
    if (x == 0) {
      if (y > 0) {
        return RESULT_HALF_PI
      }
      if (y < 0) {
        return RESULT_HALF_PI.negate()
      }
      return BigDecimal.ZERO
    }

    // 2. Calculate the ratio z = y/x
    BigDecimal z = y.divide(x, CALCULATION_CONTEXT)

    // 3. Calculate raw atan(z)
    BigDecimal result = atanInternal(z)

    // 4. Adjust for Quadrants
    if (x < 0) {
      if (y >= 0) {
        result = result.add(CALCULATION_PI, CALCULATION_CONTEXT)
      } else {
        result = result.subtract(CALCULATION_PI, CALCULATION_CONTEXT)
      }
    }
    result.round(RESULT_CONTEXT)
  }

  /**
   * Converts a Number to BigDecimal, rejecting non-finite floating point values with a
   * message that names the operation. BigDecimal has no NaN or Infinity representation,
   * so these inputs cannot be propagated.
   */
  private static BigDecimal toBigDecimal(Number value, String operation) {
    requireFinite(value, operation)
    value as BigDecimal
  }

  /** Rejects floating point values that BigDecimal cannot represent. */
  private static void requireFinite(Number value, String operation) {
    if (value instanceof Double && !Double.isFinite((Double) value)) {
      throw new IllegalArgumentException("${operation} is undefined for non-finite input: ${value}")
    }
    if (value instanceof Float && !Float.isFinite((Float) value)) {
      throw new IllegalArgumentException("${operation} is undefined for non-finite input: ${value}")
    }
  }

}
