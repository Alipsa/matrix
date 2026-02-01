package test.alipsa.matrix.ext

import org.junit.jupiter.api.Test
import se.alipsa.matrix.ext.NumberExtension

import java.math.MathContext

import static org.junit.jupiter.api.Assertions.*

class NumberExtensionTest {

  @Test
  void testConstants() {
    assertEquals(NumberExtension.E.doubleValue(), Math.E, 1e-16)
    assertEquals(NumberExtension.PI.doubleValue(), Math.PI, 1e-16)
  }

  @Test
  void testFloor() {
    BigDecimal value = 3.7G
    BigDecimal floored = NumberExtension.floor(value)
    assert floored == 3G

    // Test negative values
    assert NumberExtension.floor(-3.7G) == -4G

    // Test already integer
    assert NumberExtension.floor(5G) == 5G
  }

  @Test
  void testCeil() {
    BigDecimal value = 3.2G
    BigDecimal ceiled = NumberExtension.ceil(value)
    assert ceiled == 4G

    // Test negative values
    assert NumberExtension.ceil(-3.2G) == -3G

    // Test already integer
    assert NumberExtension.ceil(5G) == 5G
  }

  @Test
  void testLog() {
    // Test log(e) = 1
    BigDecimal e = Math.E as BigDecimal
    BigDecimal logE = e.log()
    assertEquals(1.0, logE.doubleValue(), 1e-10)

    // Test log(1) = 0
    BigDecimal one = 1.0
    BigDecimal logOne = one.log()
    assertEquals(0.0, logOne.doubleValue(), 1e-10)

    // Test log(e^2) = 2
    BigDecimal eSquared = (Math.E * Math.E) as BigDecimal
    BigDecimal logESquared = eSquared.log()
    assertEquals(2.0, logESquared.doubleValue(), 1e-10)

    // Test log is inverse of exp
    BigDecimal value = 5.0
    BigDecimal logged = value.log()
    BigDecimal recovered = logged.exp()
    assertEquals(value.doubleValue(), recovered.doubleValue(), 1e-10)

    // Test with extension syntax
    assert 1.0.log() == 0.0
  }

  @Test
  void testLog10() {
    BigDecimal value = 1000G
    BigDecimal logValue = NumberExtension.log10(value)
    assert logValue == 3G

    // Test with different values
    assert NumberExtension.log10(100) == 2G
    assert NumberExtension.log10(10.0) == 1G
  }

  @Test
  void testLogWithBase() {
    // Test logarithm with custom base
    BigDecimal value = 8.0
    BigDecimal log2Result = value.log(2)
    assertEquals(3.0, log2Result.doubleValue(), 0.00001, "log base 2 of 8 should be 3")

    // Test base 3
    BigDecimal value2 = 27.0
    BigDecimal log3Result = value2.log(3)
    assertEquals(3.0, log3Result.doubleValue(), 0.00001, "log base 3 of 27 should be 3")

    // Test base 10 (should match log10())
    BigDecimal value3 = 1000.0
    BigDecimal logBase10 = value3.log(10)
    BigDecimal log10Direct = value3.log10()
    assertEquals(log10Direct.doubleValue(), logBase10.doubleValue(), 0.00001,
        "log(value, 10) should equal log10(value)")

    // Test with natural base e
    BigDecimal valueE = Math.E as BigDecimal
    BigDecimal logE = valueE.log(Math.E)
    assertEquals(1.0, logE.doubleValue(), 0.00001, "log base e of e should be 1")

    // Test with extension syntax
    assert 8.0.log(2) == 3.0
  }

  @Test
  void testLogNegativeValueThrows() {
    // Test that log throws for negative values
    def exception = assertThrows(IllegalArgumentException) {
      (-5.0).log()
    }
    assertTrue(exception.message.contains('non-positive'))

    // Test log with base
    exception = assertThrows(IllegalArgumentException) {
      (-10.0).log(2)
    }
    assertTrue(exception.message.contains('non-positive'))
  }

  @Test
  void testLogZeroValueThrows() {
    // Test that log throws for zero
    def exception = assertThrows(IllegalArgumentException) {
      (0.0).log()
    }
    assertTrue(exception.message.contains('non-positive'))

    // Test log with base
    exception = assertThrows(IllegalArgumentException) {
      (0.0).log(10)
    }
    assertTrue(exception.message.contains('non-positive'))
  }

  @Test
  void testLogInvalidBaseThrows() {
    // Test base of 1 (undefined)
    def exception = assertThrows(IllegalArgumentException) {
      (10.0).log(1)
    }
    assertTrue(exception.message.contains('cannot be 1') || exception.message.contains('undefined'))

    // Test negative base
    exception = assertThrows(IllegalArgumentException) {
      (10.0).log(-2)
    }
    assertTrue(exception.message.contains('must be positive'))

    // Test base of 0
    exception = assertThrows(IllegalArgumentException) {
      (10.0).log(0)
    }
    assertTrue(exception.message.contains('must be positive'))
  }

  @Test
  void testExp() {
    // Test exp(0) = 1
    BigDecimal zero = 0.0
    BigDecimal expZero = zero.exp()
    assertEquals(1.0, expZero.doubleValue(), 1e-10)

    // Test exp(1) = e
    BigDecimal one = 1.0
    BigDecimal expOne = one.exp()
    assertEquals(Math.E, expOne.doubleValue(), 1e-10)

    // Test exp(2)
    BigDecimal two = 2.0
    BigDecimal expTwo = two.exp()
    assertEquals(Math.E * Math.E, expTwo.doubleValue(), 1e-10)

    // Test exp is inverse of log
    BigDecimal value = 5.0
    BigDecimal logged = value.log()
    BigDecimal recovered = logged.exp()
    assertEquals(value.doubleValue(), recovered.doubleValue(), 1e-10)

    // Test with extension syntax
    BigDecimal testVal = 0.0
    assert testVal.exp() == 1.0

    // Test with different Number types (consistency with log/log10)
    Integer intVal = 1
    assertEquals(Math.E, intVal.exp().doubleValue(), 1e-10)

    Long longVal = 2L
    assertEquals(Math.E * Math.E, longVal.exp().doubleValue(), 1e-10)

    Double doubleVal = 0.0
    assertEquals(1.0, doubleVal.exp().doubleValue(), 1e-10)
  }

  @Test
  void testUlpBigDecimal() {
    BigDecimal value = 1.0G
    BigDecimal ulpValue = NumberExtension.ulp(value)

    // ulp should return a positive value
    assert ulpValue > 0

    // Test with extension syntax
    assert value.ulp() > 0

    // Test with different magnitudes
    BigDecimal large = 1000.0G
    BigDecimal small = 0.001G
    assert large.ulp() > small.ulp()
  }

  @Test
  void testUlpNumber() {
    // Test with Integer
    Integer intValue = 42
    BigDecimal ulpInt = NumberExtension.ulp(intValue)
    assert ulpInt > 0

    // Test with Double
    Double doubleValue = 3.14
    BigDecimal ulpDouble = NumberExtension.ulp(doubleValue)
    assert ulpDouble > 0

    // Test with Long
    Long longValue = 100L
    BigDecimal ulpLong = NumberExtension.ulp(longValue)
    assert ulpLong > 0
  }

  @Test
  void testMinBigDecimalWithNumber() {
    BigDecimal bd = 5.0G

    // Test with smaller Integer
    assert bd.min(3) == 3G

    // Test with larger Integer
    assert bd.min(10) == 5G

    // Test with Double
    assert bd.min(4.5) == 4.5G

    // Test with equal value
    assert bd.min(5.0) == 5.0G
  }

  @Test
  void testMaxBigDecimalWithNumber() {
    BigDecimal bd = 5.0G

    // Test with larger Integer
    assert bd.max(10) == 10G

    // Test with smaller Integer
    assert bd.max(3) == 5G

    // Test with Double
    assert bd.max(6.5) == 6.5G

    // Test with equal value
    assert bd.max(5.0) == 5.0G
  }

  @Test
  void testMinNumberWithBigDecimal() {
    BigDecimal bd = 5.0G

    // Test Integer with BigDecimal
    assert 3.min(bd) == 3G
    assert 10.min(bd) == 5G

    // Test Long with BigDecimal
    assert 3L.min(bd) == 3G
    assert 10L.min(bd) == 5G
  }

  @Test
  void testMaxNumberWithBigDecimal() {
    BigDecimal bd = 5.0G

    // Test Integer with BigDecimal
    assert 3.max(bd) == 5G
    assert 10.max(bd) == 10G

    // Test Long with BigDecimal
    assert 3L.max(bd) == 5G
    assert 10L.max(bd) == 10G
  }

  @Test
  void testChainedMinMax() {
    // Test clamping value to range [0, 100]
    BigDecimal value = 150G
    BigDecimal clamped = 0.max(value.min(100))
    assert clamped == 100G

    // Test value below minimum
    value = -10G
    clamped = 0.max(value.min(100))
    assert clamped == 0G

    // Test value in range
    value = 50G
    clamped = 0.max(value.min(100))
    assert clamped == 50G
  }

  @Test
  void testMixedTypeComparisons() {
    // Test BigDecimal with various Number types
    BigDecimal bd = 5.5G

    // With Integer (size() - 2 pattern)
    Integer listSize = 10
    BigDecimal result = bd.min(listSize - 2)
    assert result == 5.5G

    // With calculation result
    result = bd.max(3 * 2)
    assert result == 6G
  }

  @Test
  void testNegativeValues() {
    BigDecimal negative = -5.0G

    // min/max with negative values
    assert negative.min(-10) == -10G
    assert negative.max(-10) == -5G
    assert negative.min(0) == -5G
    assert negative.max(0) == 0G
  }

  @Test
  void testSqrt() {
    // Test perfect squares
    BigDecimal four = 4.0G
    assert four.sqrt() == 2.0G

    BigDecimal nine = 9.0G
    assert nine.sqrt() == 3.0G

    BigDecimal twentyFive = 25.0G
    assert twentyFive.sqrt() == 5.0G

    // Test non-perfect square
    BigDecimal two = 2.0G
    BigDecimal sqrtTwo = two.sqrt()
    assert sqrtTwo > 1.4G
    assert sqrtTwo < 1.5G

    // Test with area calculation pattern (from ScaleSizeArea)
    BigDecimal area = 16.0G
    BigDecimal side = area.sqrt()
    assert side == 4.0G

    // Test that it's equivalent to sqrt(MathContext.DECIMAL64)
    BigDecimal value = 100.0G
    assert value.sqrt() == value.sqrt(MathContext.DECIMAL64)

    // Test with explicit DECIMAL128 precision for higher precision needs
    BigDecimal val128 = 2.0G
    BigDecimal sqrt128 = val128.sqrt(MathContext.DECIMAL128)
    assert sqrt128 != null
    // DECIMAL128 has more precision than DECIMAL64
    assert sqrt128.precision() >= val128.sqrt().precision()
  }

  @Test
  void testSqrtWithExtensionSyntax() {
    // Verify sqrt works with extension syntax (not just static call)
    assert 4G.sqrt() == 2G
    assert 9G.sqrt() == 3G

    // Test in expression
    BigDecimal rMin = 2.0G
    BigDecimal rMax = 4.0G
    BigDecimal midArea = (rMin * rMin + rMax * rMax) / 2
    BigDecimal result = midArea.sqrt()
    assert result > 0
  }

  @Test
  void testNumberMinWithNumber() {
    // Test Integer with Integer
    Integer a = 100
    Integer b = 50
    assert a.min(b) == 50G
    assert b.min(a) == 50G

    // Test Long with Long
    Long x = 100L
    Long y = 50L
    assert x.min(y) == 50G

    // Test Double with Integer
    Double d = 5.5
    Integer i = 10
    assert d.min(i) == 5.5G
    assert i.min(d) == 5.5G

    // Test the CoordPolar pattern
    int plotWidth = 640
    int plotHeight = 480
    BigDecimal radius = plotWidth.min(plotHeight) / 2
    assert radius == 240G
  }

  @Test
  void testNumberMaxWithNumber() {
    // Test Integer with Integer
    Integer a = 100
    Integer b = 50
    assert a.max(b) == 100G
    assert b.max(a) == 100G

    // Test Long with Long
    Long x = 100L
    Long y = 50L
    assert x.max(y) == 100G

    // Test Double with Integer
    Double d = 5.5
    Integer i = 10
    assert d.max(i) == 10G
    assert i.max(d) == 10G

    // Test with negative values
    int neg = -10
    int pos = 5
    assert neg.max(pos) == 5G
    assert pos.max(neg) == 5G
  }

  @Test
  void testNumberMinMaxChaining() {
    // Test that Number.min/max can be chained
    int a = 150
    int b = 50
    int c = 100

    // Find minimum of three values
    BigDecimal min = a.min(b).min(c)
    assert min == 50G

    // Find maximum of three values
    BigDecimal max = a.max(b).max(c)
    assert max == 150G

    // Clamping pattern with Number types
    int value = 200
    int minBound = 0
    int maxBound = 100
    BigDecimal clamped = minBound.max(value.min(maxBound))
    assert clamped == 100G
  }

  @Test
  void testSin() {
    // Test sin(0) = 0
    BigDecimal zero = 0G
    BigDecimal sinZero = zero.sin()
    assertEquals(0.0, sinZero.doubleValue(), 1e-10)

    // Test sin(π/2) = 1
    BigDecimal piOver2 = Math.PI / 2 as BigDecimal
    BigDecimal sinPiOver2 = piOver2.sin()
    assertEquals(1.0, sinPiOver2.doubleValue(), 1e-10)

    // Test sin(π) = 0
    BigDecimal pi = Math.PI as BigDecimal
    BigDecimal sinPi = pi.sin()
    assertEquals(0.0, sinPi.doubleValue(), 1e-10)

    // Test sin(3π/2) = -1
    BigDecimal threePiOver2 = (3 * Math.PI / 2) as BigDecimal
    BigDecimal sinThreePiOver2 = threePiOver2.sin()
    assertEquals(-1.0, sinThreePiOver2.doubleValue(), 1e-10)

    // Test sin(π/6) = 0.5
    BigDecimal piOver6 = (Math.PI / 6) as BigDecimal
    BigDecimal sinPiOver6 = piOver6.sin()
    assertEquals(0.5, sinPiOver6.doubleValue(), 1e-10)

    assertEquals(Math.sin(12.2), NumberExtension.sin(12.2).doubleValue(), 1e-12)
  }

  @Test
  void testCos() {
    // Test cos(0) = 1
    BigDecimal zero = 0G
    BigDecimal cosZero = zero.cos()
    assertEquals(1.0, cosZero.doubleValue(), 1e-10)

    // Test cos(π/2) = 0
    BigDecimal piOver2 = Math.PI / 2 as BigDecimal
    BigDecimal cosPiOver2 = piOver2.cos()
    assertEquals(0.0, cosPiOver2.doubleValue(), 1e-10)

    // Test cos(π) = -1
    BigDecimal pi = Math.PI as BigDecimal
    BigDecimal cosPi = pi.cos()
    assertEquals(-1.0, cosPi.doubleValue(), 1e-10)

    // Test cos(2π) = 1
    BigDecimal twoPi = (2 * Math.PI) as BigDecimal
    BigDecimal cosTwoPi = twoPi.cos()
    assertEquals(1.0, cosTwoPi.doubleValue(), 1e-10)

    // Test cos(π/3) = 0.5
    BigDecimal piOver3 = (Math.PI / 3) as BigDecimal
    BigDecimal cosPiOver3 = piOver3.cos()
    assertEquals(0.5, cosPiOver3.doubleValue(), 1e-10)
  }

  @Test
  void testSinCosWithExtensionSyntax() {
    // Verify sin/cos work with extension syntax
    BigDecimal zero = 0.0G
    assert zero.sin().doubleValue() == 0.0

    // Test calculating points on unit circle
    BigDecimal angle = Math.PI / 4 as BigDecimal  // 45 degrees
    BigDecimal x = angle.cos()
    BigDecimal y = angle.sin()

    // At 45 degrees, sin and cos should be equal
    assertEquals(x.doubleValue(), y.doubleValue(), 1e-10)

    // And they should be approximately √2/2
    assertEquals(Math.sqrt(2) / 2, x.doubleValue(), 1e-10)
  }

  @Test
  void testSinCosPolarCoordinates() {
    // Test a practical use case: converting polar to cartesian coordinates
    BigDecimal radius = 10.0G
    BigDecimal angle = Math.PI / 3 as BigDecimal  // 60 degrees

    BigDecimal x = radius * angle.cos()
    BigDecimal y = radius * angle.sin()

    // At 60 degrees: cos(60°) = 0.5, sin(60°) = √3/2
    assertEquals(5.0, x.doubleValue(), 1e-10)
    assertEquals(10 * Math.sqrt(3) / 2, y.doubleValue(), 1e-10)
  }

  @Test
  void testExtensionSyntax() {
    // Verify all methods work with extension syntax (not just static calls)
    BigDecimal value = 3.7G

    assert value.floor() == 3G
    assert value.ceil() == 4G
    assert value.ulp() > 0
    assert value.min(5) == 3.7G
    assert value.max(2) == 3.7G
    assert value.sqrt() > 0
    assert value.sin() != null
    assert value.cos() != null
    assert value.exp() != null
    assert value.log() != null
    assert value.atan2(1.0) != null

    // Verify Number types work with extension syntax
    BigDecimal e = Math.E as BigDecimal
    assert e.log() == 1.0
    assert 100.log10() == 2G
    assert 42.ulp() > 0
    assert 5.min(10.0G) == 5G
    assert 5.max(10.0G) == 10G

    // Verify Number.min/max(Number)
    assert 100.min(50) == 50G
    assert 100.max(50) == 100G
  }

  @Test
  void testTan() {
    assertEquals(Math.tan(12.2), NumberExtension.tan(12.2).doubleValue(), 1e-10)
    assertEquals(Math.tan(11), NumberExtension.tan(11).doubleValue(), 1e-10)
  }

  @Test
  void testAtan() {
    assertEquals(Math.atan(12.2), NumberExtension.atan(12.2).doubleValue(), 1e-12)
    assertEquals(Math.atan(11), NumberExtension.atan(11).doubleValue(), 1e-12)
  }

  @Test
  void testAtan2() {
    // Test atan2(1, 1) = π/4 (45 degrees)
    BigDecimal y = 1.0
    BigDecimal x = 1.0
    BigDecimal angle = y.atan2(x)
    assertEquals(Math.PI / 4, angle.doubleValue(), 1e-10)

    // Test atan2(1, 0) = π/2 (90 degrees - pointing up)
    angle = (1.0).atan2(0.0)
    assertEquals(Math.PI / 2, angle.doubleValue(), 1e-10)

    // Test atan2(0, 1) = 0 (0 degrees - pointing right)
    angle = (0.0).atan2(1.0)
    assertEquals(0.0, angle.doubleValue(), 1e-10)

    // Test atan2(-1, -1) = -3π/4 (pointing to third quadrant)
    angle = (-1.0).atan2(-1.0)
    assertEquals(-3 * Math.PI / 4, angle.doubleValue(), 1e-10)

    // Test practical use case: angle of line from (1, 1) to (5, 4)
    BigDecimal dy = 4 - 1  // 3
    BigDecimal dx = 5 - 1  // 4
    angle = dy.atan2(dx)
    // This should be atan(3/4) = atan(0.75)
    assertEquals(Math.atan2(3.0, 4.0), angle.doubleValue(), 1e-10)

    assertEquals(Math.atan2(12.2, 6.4), NumberExtension.atan2(12.2, 6.4).doubleValue(), 1e-12)
    assertEquals(Math.atan2(15, 6), NumberExtension.atan2(15, 6).doubleValue(), 1e-12)

    // Test with extension syntax
    assert (1.0).atan2(1.0).doubleValue() == Math.atan2(1.0, 1.0)
  }

  @Test
  void testAtan2WithMixedTypes() {
    // Test with Integer
    Integer yi = 3
    Integer xi = 4
    BigDecimal angle = yi.atan2(xi)
    assertEquals(Math.atan2(3.0, 4.0), angle.doubleValue(), 1e-10)

    // Test with Long
    Long yL = 1L
    Long xL = 1L
    angle = yL.atan2(xL)
    assertEquals(Math.PI / 4, angle.doubleValue(), 1e-10)

    // Test mixed types
    Double yD = 1.0
    Integer xI = 1
    angle = yD.atan2(xI)
    assertEquals(Math.PI / 4, angle.doubleValue(), 1e-10)
  }

  @Test
  void testToDegrees() {
    BigDecimal radians = Math.PI as BigDecimal
    BigDecimal degrees = NumberExtension.toDegrees(radians)
    assertEquals(180.0, degrees.doubleValue(), 1e-10)

    radians = (Math.PI / 2) as BigDecimal
    degrees = NumberExtension.toDegrees(radians)
    assertEquals(90.0, degrees.doubleValue(), 1e-10)

    // Test with extension syntax
    radians = (Math.PI / 4) as BigDecimal
    degrees = radians.toDegrees()
    assertEquals(45.0, degrees.doubleValue(), 1e-10)
  }

  @Test
  void testToRadians() {
    BigDecimal degrees = 180.0G
    BigDecimal radians = NumberExtension.toRadians(degrees)
    assertEquals(Math.PI, radians.doubleValue(), 1e-10)
    degrees = 90.0G
    radians = NumberExtension.toRadians(degrees)
    assertEquals(Math.PI / 2, radians.doubleValue(), 1e-10)
    // Test with extension syntax
    degrees = 45.0G
    radians = degrees.toRadians()
    assertEquals(Math.PI / 4, radians.doubleValue(), 1e-10)
  }
}
