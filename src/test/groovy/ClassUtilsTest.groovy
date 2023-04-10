import static org.junit.jupiter.api.Assertions.*

import static se.alipsa.groovy.matrix.util.ClassUtils.*

import org.junit.jupiter.api.Test

class ClassUtilsTest {

  @Test
  void testConvertToWrapper() {
    assertEquals(Integer, primitiveWrapper(int))
    assertEquals(Boolean, primitiveWrapper(boolean))

    assertArrayEquals([Short, Long, Double, Float].toArray(),
        convertPrimitivesToWrapper([short, long, double, float]).toArray())
  }

  @Test
  void testFindClosestCommon() {
    assertEquals(Integer, findClosestCommonSuper(short, Integer), "short and Integer")
    assertEquals(Integer, findClosestCommonSuper(Integer, Short), "Integer and Short")
    assertEquals(Double, findClosestCommonSuper(byte, Double), "byte and double")
    assertEquals(Double, findClosestCommonSuper(Long, Float), "Double and Float")
    assertEquals(Double, findClosestCommonSuper(Double, Integer), "Double and Integer")
    assertEquals(Double, findClosestCommonSuper(Integer, Double), "Integer and Double")
    assertEquals(Float, findClosestCommonSuper(Float, Integer), "Float and Integer")
    assertEquals(BigDecimal, findClosestCommonSuper(Double, BigInteger), "Double and BigInteger")
    assertEquals(BigDecimal, findClosestCommonSuper(BigDecimal, BigInteger), "BigDecimal and BigInteger")
    assertEquals(BigDecimal, findClosestCommonSuper(Float, BigInteger), "Float and BigInteger")
    assertEquals(BigDecimal, findClosestCommonSuper(Double, BigInteger), "Double and BigInteger")
    assertEquals(BigDecimal, findClosestCommonSuper(byte, BigDecimal), "byte and BigDecimal")
  }
}
