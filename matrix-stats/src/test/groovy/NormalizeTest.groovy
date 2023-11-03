import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.stats.Normalize

class NormalizeTest {

  @Test
  void testLogNormFromArray() {
    Double[] obs =  [1200,34567,3456,12,3456,985,1211] as Double[]
    List<Double> norm = Normalize.logNorm(obs, 6)
    def exp = [7.090077d, 10.450655d, 8.147867d, 2.484907d, 8.147867d, 6.892642d, 7.099202d]
    Assertions.assertIterableEquals(exp, norm)

    Float[] obs2 = [1200,34567,3456,12,3456,985,1211] as Float[]
    norm = Normalize.logNorm(obs2, 6)
    List<Float> exp2 = [7.090077f, 10.450655f, 8.147867f, 2.484907f, 8.147867f, 6.892642f, 7.099202f]
    Assertions.assertIterableEquals(exp2, norm)

    BigDecimal[] obs3 =  [1200,34567,3456,12,3456,985,1211] as BigDecimal[]
    norm = Normalize.logNorm(obs3, 6)
    List<BigDecimal> exp3 = [7.090077g, 10.450655g, 8.147867g, 2.484907g, 8.147867g, 6.892642g, 7.099202g]
    Assertions.assertIterableEquals(exp3, norm)
  }

  @Test
  void testLogNormFromList() {
    List<Double> obs =  [1200d,34567d,3456d,12d,3456d,985d,1211d]
    def norm = Normalize.logNorm(obs)
    def exp = [7.090077d, 10.450655d, 8.147867d, 2.484907d, 8.147867d, 6.892642d, 7.099202d]
    int idx = 0
    exp.each {
      def val = norm[idx++]
      Assertions.assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Double, 6, "index $idx")
    }

    def obs2 = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    norm = Normalize.logNorm(obs2, 6)
    List<Float> exp2 = [7.090077f, 10.450655f, 8.147867f, 2.484907f, 8.147867f, 6.892642f, 7.099202f]
    idx = 0
    exp2.each {
      def val = norm[idx++]
      Assertions.assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Float, 6, "index $idx")
    }

    def obs3 =  [1200g,34567g,3456g,12g,3456g,985g,1211.0g]
    norm = Normalize.logNorm(obs3, 6)
    List<BigDecimal> exp3 = [7.090077g, 10.450655g, 8.147867g, 2.484907g, 8.147867g, 6.892642g, 7.099202g]
    idx = 0
    exp3.each {
      def val = norm[idx++]
      Assertions.assertTrue(val instanceof BigDecimal, "value is not BigDecimal but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as BigDecimal, 6, "index $idx")
    }
  }

  @Test
  void testMinMaxNormDouble() {

    Assertions.assertEquals(0.03437997d, Normalize.minMaxNorm(1200.0d, 12d, 34567d, 8))

    def obs = [1200d,34567d,3456d,12d,3456d,985d,1211d] as Double[]
    def norm = Normalize.minMaxNorm(obs, 8) as List<Double>
    List<Double> exp = [0.03437997d, 1.00000000d, 0.09966720d, 0.00000000d, 0.09966720d, 0.02815801d, 0.03469831d]
    Assertions.assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      Assertions.assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Double, 8, "index $idx")
    }
  }

  @Test
  void testMinMaxNormFloat() {

    Assertions.assertEquals(0.03437997f, Normalize.minMaxNorm(1200f, 12f, 34567f, 8))

    def obs = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    def norm = Normalize.minMaxNorm(obs as Float[], 8)
    List<Float> exp = [0.03437997f, 1.00000000f, 0.09966720f, 0.00000000f, 0.09966720f, 0.02815801f, 0.03469831f]
    Assertions.assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      Assertions.assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Float, 8, "index $idx")
    }
  }

  @Test
  void testMinMaxNormBigDecimal() {

    Assertions.assertEquals(0.03437997g, Normalize.minMaxNorm(1200.0g, 12.0g, 34567.0g, 8))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200,34567,3456,12,3456,985,1211])
    List<BigDecimal> norm = Normalize.minMaxNorm(obs as BigDecimal[], 8)
    def exp = [0.03437997g, 1.00000000g, 0.09966720g, 0.00000000g, 0.09966720g, 0.02815801g, 0.03469831g]
    Assertions.assertIterableEquals(exp, norm)

    def norm2 = Normalize.minMaxNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      Assertions.assertTrue(val instanceof BigDecimal, "value is not BigDecimal but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as BigDecimal, 8, "index $idx")
    }
  }

  @Test
  void testStdScaleNormDouble() {
    Assertions.assertEquals(-0.4175944d, Normalize.stdScaleNorm(1200d, 6412.428571428572d, 12482.037558790136d, 7 ))

    List<Double> obs = [1200d,34567d,3456d,12d,3456d,985d,1211d]
    List<Double> norm = Normalize.stdScaleNorm(obs as Double[], 7)
    List<Double> exp = [-0.4175944d, 2.2556070d, -0.2368546d, -0.5127711d, -0.2368546d, -0.4348191d, -0.4167131d]
    Assertions.assertIterableEquals(exp, norm)

    def norm2 = Normalize.stdScaleNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      Assertions.assertTrue(val instanceof Double, "value is not Double but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Double, 7, "index $idx")
    }
  }

  @Test
  void testStdScaleNormFloat() {
    Assertions.assertEquals(2.25560701f, Normalize.stdScaleNorm(34567f, 6412.428571428571f, 12482.037558790136f, 8))

    List<Float> obs = [1200f,34567f,3456f,12f,3456f,985f,1211f]
    def norm = Normalize.stdScaleNorm(obs as Float[], 6) as List<Float>
    List<Float> exp = [-0.417594f, 2.255607f, -0.236855f, -0.512771f, -0.236855f, -0.434819f, -0.416713f]
    Assertions.assertIterableEquals(exp, norm)

    def norm2 = Normalize.stdScaleNorm(obs)
    int idx = 0
    exp.each {
      def val = norm2[idx++]
      Assertions.assertTrue(val instanceof Float, "value is not Float but ${val.getClass().simpleName}")
      Assertions.assertEquals(it, val as Float, 6, "index $idx")
    }
  }

  @Test
  void testStdScaleNormBigDecimal() {
    Assertions.assertEquals(-0.41759437g, Normalize.stdScaleNorm(1200.0g, 6412.428571428572g, 12482.037558790136g, 8 ))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200,34567,3456,12,3456,985,1211])
    def norm = Normalize.stdScaleNorm(obs as BigDecimal[], 7) as List<BigDecimal>
    List<BigDecimal> exp = [-0.4175944g, 2.2556070g, -0.2368546g, -0.5127711g, -0.2368546g, -0.4348191g, -0.4167131g]
    Assertions.assertIterableEquals(exp, norm)

    norm = Normalize.stdScaleNorm(obs, 7)
    Assertions.assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormDouble() {
    Assertions.assertEquals(-0.150526076d, Normalize.meanNorm(1211d, 6412.428571428572d, 12d, 34567d, 9))

    List<Double> obs = [1200d,34567d,3456d,12d,3456d,985d,1211d]
    def norm = Normalize.meanNorm(obs as Double[], 7)
    List<Double>  exp = [-0.1508444d, 0.8147756d,  -0.0855572d, -0.1852244d, -0.0855572d, -0.1570664d, -0.1505261d]
    Assertions.assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    Assertions.assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormFloat() {
    Assertions.assertEquals(-0.150526076f, Normalize.meanNorm(1211f, 6412.428571428572f, 12f, 34567f, 9 ))

    List<Float> obs = ListConverter.toFloats([1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalize.meanNorm(obs as Float[], 7)
    List<Float> exp = [ -0.1508444f, 0.8147756f, -0.0855572f, -0.1852244f, -0.0855572f, -0.1570664f, -0.1505261f ]
    Assertions.assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    Assertions.assertIterableEquals(exp, norm)
  }

  @Test
  void testMeanNormBigDecimal() {
    Assertions.assertEquals(-0.150526076g, Normalize.meanNorm(1211.0g, 6412.428571428572g, 12.0g, 34567.0g,  9 ))

    List<BigDecimal> obs = ListConverter.toBigDecimals([1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalize.meanNorm(obs as BigDecimal[], 7) as List<BigDecimal>
    List<BigDecimal> exp = [-0.1508444, 0.8147756,  -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261]
    Assertions.assertIterableEquals(exp, norm)

    norm = Normalize.meanNorm(obs, 7)
    Assertions.assertIterableEquals(exp, norm)
  }

}
