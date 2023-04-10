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
}
