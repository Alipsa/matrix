import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class NormalizeTest {

  @Test
  void testLogNormFromArray() {
    Double[] obs =  [1200,34567,3456,12,3456,985,1211] as Double[]
    List<Double> norm = Normalize.logNorm(obs, 6)
    def exp = [7.090077d, 10.450655d, 8.147867d, 2.484907d, 8.147867d, 6.892642d, 7.099202d]
    assertIterableEquals(exp, norm)

    Float[] obs2 = [1200,34567,3456,12,3456,985,1211] as Float[]
    norm = Normalize.logNorm(obs2, 6)
    List<Float> exp2 = [7.090077f, 10.450655f, 8.147867f, 2.484907f, 8.147867f, 6.892642f, 7.099202f]
    assertIterableEquals(exp2, norm)

    BigDecimal[] obs3 =  [1200,34567,3456,12,3456,985,1211] as BigDecimal[]
    norm = Normalize.logNorm(obs3, 6)
    List<BigDecimal> exp3 = [7.090077g, 10.450655g, 8.147867g, 2.484907g, 8.147867g, 6.892642g, 7.099202g]
    assertIterableEquals(exp3, norm)
  }

  @Test
  void testLogNormFromList() {
    List<Double> obs =  [1200d,34567d,3456d,12d,3456d,985d,1211d]
    def norm = Normalize.logNorm(obs)
    def exp = [7.090077d, 10.450655d, 8.147867d, 2.484907d, 8.147867d, 6.892642d, 7.099202d]
    int idx = 0
    exp.each {
      def val = norm[idx++]
      assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      assertEquals(it, val as Double, 6, "index $idx")
    }

    def obs2 = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    norm = Normalize.logNorm(obs2, 6)
    List<Float> exp2 = [7.090077f, 10.450655f, 8.147867f, 2.484907f, 8.147867f, 6.892642f, 7.099202f]
    idx = 0
    exp2.each {
      def val = norm[idx++]
      assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      assertEquals(it, val as Float, 6, "index $idx")
    }

    def obs3 =  [1200g,34567g,3456g,12g,3456g,985g,1211.0g]
    norm = Normalize.logNorm(obs3, 6)
    List<BigDecimal> exp3 = [7.090077g, 10.450655g, 8.147867g, 2.484907g, 8.147867g, 6.892642g, 7.099202g]
    idx = 0
    exp3.each {
      def val = norm[idx++]
      assertTrue(val instanceof BigDecimal, "value is not BigDecimal but ${val.getClass().simpleName}")
      assertEquals(it, val as BigDecimal, 6, "index $idx")
    }
    assert [2.079f, 1.946f, 2.197f, 1.792f, 2.079f] == Normalize.logNorm([8, 7, 9, 6, 8], 3)
  }

  @Test
  void testLogNormZeroes() {
    assertIterableEquals([null]*3, Normalize.logNorm(ListConverter.convert([0, 0, 0], Byte)), "Byte should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm(ListConverter.convert([0, 0, 0], Short)), "Short should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm(ListConverter.toIntegers([0, 0, 0])), "Integer should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm([0f, 0f, 0f]), "Float should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm([0l, 0l, 0l]), "Long should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm([0.0d, 0d, 0d]), "Double should be -Infinity")
    assertIterableEquals([null]*3, Normalize.logNorm(ListConverter.convert([0, 0, 0], BigInteger)), "BigInteger should be null")
    assertIterableEquals([null]*3, Normalize.logNorm([0.0g, 0.0g, 0.0g]), "BigDecimal should be null")
  }

  @Test
  void testMinMaxNormDouble() {

    assertEquals(0.03437997d, Normalize.minMaxNorm(1200.0d, 12d, 34567d, 8))

    def obs = [1200d,34567d,3456d,12d,3456d,985d,1211d] as Double[]
    def norm = Normalize.minMaxNorm(obs, 8) as List<Double>
    List<Double> exp = [0.03437997d, 1.00000000d, 0.09966720d, 0.00000000d, 0.09966720d, 0.02815801d, 0.03469831d]
    assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      assertEquals(it, val as Double, 8, "index $idx")
    }
  }

  @Test
  void testMinMaxNormFloat() {

    assertEquals(0.03437997f, Normalize.minMaxNorm(1200f, 12f, 34567f, 8))

    def obs = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    def norm = Normalize.minMaxNorm(obs as Float[], 8)
    List<Float> exp = [0.03437997f, 1.00000000f, 0.09966720f, 0.00000000f, 0.09966720f, 0.02815801f, 0.03469831f]
    assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      assertEquals(it, val as Float, 8, "index $idx")
    }
  }

  @Test
  void testMinMaxNormZeroes() {
    assert [Float.NaN]*3 == Normalize.minMaxNorm(ListConverter.convert([0, 0, 0], Byte)) : "Byte should be NaN"
    assert [Float.NaN]*3 == Normalize.minMaxNorm(ListConverter.convert([0, 0, 0], Short)) : "Short should be NaN"
    assert [Double.NaN]*3 == Normalize.minMaxNorm([0i, 0i, 0i]) : "Integer should be NaN"
    assert [Float.NaN]*3 == Normalize.minMaxNorm([0f, 0f, 0f]) : "Float should be NaN"
    assert [Double.NaN]*3 == Normalize.minMaxNorm([0d, 0d, 0d]) : "Double should be NaN"
    assert [Double.NaN]*3 == Normalize.minMaxNorm([0l, 0l, 0l]) : "Long should be NaN"
    assert [null]*3 == Normalize.minMaxNorm([0G, 0G, 0G]) : "BigInteger should be null"
    assert [null]*3 == Normalize.minMaxNorm([0.0g, 0.0g, 0.0g]) : "BigDecimal should be null"
  }

  @Test
  void testMinMaxNormBigDecimal() {

    assertEquals(0.03437997g, Normalize.minMaxNorm(1200.0g, 12.0g, 34567.0g, 8))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200,34567,3456,12,3456,985,1211])
    List<BigDecimal> norm = Normalize.minMaxNorm(obs as BigDecimal[], 8)
    def exp = [0.03437997g, 1.00000000g, 0.09966720g, 0.00000000g, 0.09966720g, 0.02815801g, 0.03469831g]
    assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      assertTrue(val instanceof BigDecimal, "value is not BigDecimal but ${val.getClass().simpleName}")
      assertEquals(it, val as BigDecimal, 8, "index $idx")
    }
  }

  @Test
  void testStdScaleNormDouble() {
    assertEquals(-0.4175944d, Normalize.stdScaleNorm(1200d, 6412.428571428572d, 12482.037558790136d, 7 ))

    List<Double> obs = [1200d,34567d,3456d,12d,3456d,985d,1211d]
    List<Double> norm = Normalize.stdScaleNorm(obs as Double[], 7)
    List<Double> exp = [-0.4175944d, 2.2556070d, -0.2368546d, -0.5127711d, -0.2368546d, -0.4348191d, -0.4167131d]
    assertIterableEquals(exp, norm)

    def norm2 = Normalize.stdScaleNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      assertEquals(it, val as Double, 7, "index $idx")
    }
  }

  @Test
  void testStdScaleNormFloat() {
    assertEquals(2.25560701f, Normalize.stdScaleNorm(34567f, 6412.428571428571f, 12482.037558790136f, 8))

    List<Float> obs = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    def norm = Normalize.stdScaleNorm(obs as Float[], 6) as List<Float>
    List<Float> exp = [-0.417594f, 2.255607f, -0.236855f, -0.512771f, -0.236855f, -0.434819f, -0.416713f]
    assertIterableEquals(exp, norm)

    def norm2 = Normalize.stdScaleNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      assertEquals(it, val as Float, 6, "index $idx")
    }
  }

  @Test
  void testStdScaleNormBigDecimal() {
    assertEquals(-0.41759437g, Normalize.stdScaleNorm(1200.0g, 6412.428571428572g, 12482.037558790136g, 8 ))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200,34567,3456,12,3456,985,1211])
    def norm = Normalize.stdScaleNorm(obs as BigDecimal[], 7) as List<BigDecimal>
    List<BigDecimal> exp = [-0.4175944g, 2.2556070g, -0.2368546g, -0.5127711g, -0.2368546g, -0.4348191g, -0.4167131g]
    assertIterableEquals(exp, norm)

    norm = Normalize.stdScaleNorm(obs, 7)
    assertIterableEquals(exp, norm)
  }

  @Test
  void testStdScaleNormZeroes() {
    assertIterableEquals([null]*3, Normalize.stdScaleNorm(ListConverter.convert([0, 0, 0], Byte)), "Byte should be -Infinity")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm(ListConverter.convert([0, 0, 0], Short)), "Short should be -Infinity")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm(ListConverter.toIntegers([0, 0, 0])), "Integer should be -Infinity")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm([0f, 0f, 0f]), "Float should be null")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm([0.0d, 0d, 0d]), "Double should be null")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm([0l, 0l, 0l]), "Long should be -Infinity")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm(ListConverter.convert([0, 0, 0], BigInteger)), "BigInteger should be null")
    assertIterableEquals([null]*3, Normalize.stdScaleNorm([0.0g, 0.0g, 0.0g]), "BigDecimal should be null")
  }

  @Test
  void testMeanNormDouble() {
    assertEquals(-0.150526076d, Normalize.meanNorm(1211d, 6412.428571428572d, 12d, 34567d, 9))

    List<Double> obs = [1200d,34567d,3456d,12d,3456d,985d,1211d]
    def norm = Normalize.meanNorm(obs as Double[], 7)
    List<Double>  exp = [-0.1508444d, 0.8147756d,  -0.0855572d, -0.1852244d, -0.0855572d, -0.1570664d, -0.1505261d]
    assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormFloat() {
    assertEquals(-0.150526076f, Normalize.meanNorm(1211f, 6412.428571428572f, 12f, 34567f, 9 ))

    List<Float> obs = ListConverter.toFloats([1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalize.meanNorm(obs as Float[], 7)
    List<Float> exp = [ -0.1508444f, 0.8147756f, -0.0855572f, -0.1852244f, -0.0855572f, -0.1570664f, -0.1505261f ]
    assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormBigDecimal() {
    assertEquals(-0.150526076g, Normalize.meanNorm(1211.0g, 6412.428571428572g, 12.0g, 34567.0g,  9 ))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalize.meanNorm(obs as BigDecimal[], 7) as List<BigDecimal>
    List<BigDecimal> exp = [-0.1508444, 0.8147756,  -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261]
    assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormZeroes() {
    assertIterableEquals([Float.NaN]*3, Normalize.meanNorm(ListConverter.convert([0, 0, 0], Byte)), "Byte should be -Infinity")
    assertIterableEquals([Float.NaN]*3, Normalize.meanNorm(ListConverter.convert([0, 0, 0], Short)), "Short should be -Infinity")
    assertIterableEquals([Float.NaN]*3, Normalize.meanNorm(ListConverter.toIntegers([0, 0, 0])), "Integer should be -Infinity")
    assertIterableEquals([Float.NaN]*3, Normalize.meanNorm([0f, 0f, 0f]), "Float should be null")
    assertIterableEquals([Double.NaN]*3, Normalize.meanNorm([0.0d, 0d, 0d]), "Double should be null")
    assertIterableEquals([Float.NaN]*3, Normalize.meanNorm([0l, 0l, 0l]), "Long should be -Infinity")
    assertIterableEquals([null]*3, Normalize.meanNorm(ListConverter.convert([0, 0, 0], BigInteger)), "BigInteger should be null")
    assertIterableEquals([null]*3, Normalize.meanNorm([0.0g, 0.0g, 0.0g]), "BigDecimal should be null")
  }

  @Test
  void testNormalizeMatrix() {
    def mtcars = Dataset.mtcars()
    def m = Normalize.stdScaleNorm(mtcars)
    assert mtcars['model'] == m['model']
  }
}
