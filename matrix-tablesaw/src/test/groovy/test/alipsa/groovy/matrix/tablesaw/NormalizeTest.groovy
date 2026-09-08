package test.alipsa.groovy.matrix.tablesaw

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.FloatColumn

import se.alipsa.matrix.tablesaw.Normalizer

class NormalizeTest {

  @Test
  void testLogNorm() {
    def obs = DoubleColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.logNorm(obs, 6)
    def exp = [7.090077, 10.450655, 8.147867, 2.484907, 8.147867, 6.892642, 7.099202] as double[]
    Assertions.assertArrayEquals(exp, norm.asDoubleArray())

    obs = FloatColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as float[])
    norm = Normalizer.logNorm(obs, 6)
    exp = [7.090077, 10.450655, 8.147867, 2.484907, 8.147867, 6.892642, 7.099202] as float[]
    Assertions.assertArrayEquals(exp, norm.asFloatArray())

    obs = BigDecimalColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as BigDecimal[])
    norm = Normalizer.logNorm(obs, 6)
    exp = [7.090077, 10.450655, 8.147867, 2.484907, 8.147867, 6.892642, 7.099202] as BigDecimal[]
    Assertions.assertArrayEquals(exp, norm.asBigDecimalArray())
  }

  @Test
  void testLogNormPreservesFloatingMissingRows() {
    def doubles = Normalizer.logNorm(DoubleColumn.create('d', [1d, null, Math.E] as Double[]), 6)
    def floats = Normalizer.logNorm(FloatColumn.create('f', [1f, null, Math.E as float] as Float[]), 6)

    Assertions.assertEquals(3, doubles.size())
    Assertions.assertTrue(doubles.isMissing(1))
    Assertions.assertEquals(0d, doubles.getDouble(0))
    Assertions.assertEquals(1d, doubles.getDouble(2), 1e-6)
    floats.with {
      Assertions.assertEquals(3, size())
      Assertions.assertTrue(isMissing(1))
      Assertions.assertEquals(0f, getFloat(0))
      Assertions.assertEquals(1f, getFloat(2), 1e-6f)
    }
  }

  @Test
  void testMinMaxNormDouble() {
    def obs = DoubleColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.minMaxNorm(obs, 8)
    def exp = [0.03437997, 1.00000000, 0.09966720, 0.00000000, 0.09966720, 0.02815801, 0.03469831] as double[]
    Assertions.assertArrayEquals(exp, norm.asDoubleArray())
  }

  @Test
  void testMinMaxNormFloat() {
    def obs = FloatColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as float[])
    def norm = Normalizer.minMaxNorm(obs, 8)
    def exp = [0.03437997, 1.00000000, 0.09966720, 0.00000000, 0.09966720, 0.02815801, 0.03469831] as float[]
    Assertions.assertArrayEquals(exp, norm.asFloatArray())
  }

  @Test
  void testMinMaxNormZeroRangeFloatColumnsUseMissingValues() {
    def doubleNorm = Normalizer.minMaxNorm(DoubleColumn.create('doubleValues', [7d, 7d, 7d] as double[]))
    def floatNorm = Normalizer.minMaxNorm(FloatColumn.create('floatValues', [7f, 7f, 7f] as float[]))

    (0..<3).each { int row ->
      Assertions.assertTrue(doubleNorm.isMissing(row))
      Assertions.assertTrue(floatNorm.isMissing(row))
    }
  }

  @Test
  void testMinMaxNormBigDecimal() {
    def obs = BigDecimalColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as BigDecimal[])
    def norm = Normalizer.minMaxNorm(obs, 8)
    def exp = [0.03437997, 1.00000000, 0.09966720, 0.00000000, 0.09966720, 0.02815801, 0.03469831] as BigDecimal[]
    Assertions.assertArrayEquals(exp, norm.asBigDecimalArray())
  }

  @Test
  void testStdScaleNormDouble() {
    def obs = DoubleColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.stdScaleNorm(obs, 7)
    def exp = [-0.4175944, 2.2556070, -0.2368546, -0.5127711, -0.2368546, -0.4348191, -0.4167131] as double[]
    Assertions.assertArrayEquals(exp, norm.asDoubleArray())
  }

  @Test
  void testStdScaleNormFloat() {
    def obs = FloatColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as Float[])
    def norm = Normalizer.stdScaleNorm(obs, 6)
    def exp = [-0.417594, 2.255607, -0.236855, -0.512771, -0.236855, -0.434819, -0.416713] as Float[]
    Assertions.assertArrayEquals(exp, norm.asFloatArray())
  }

  @Test
  void testStdScaleNormBigDecimal() {
    def obs = BigDecimalColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.stdScaleNorm(obs, 7)
    def exp = [-0.4175944, 2.2556070, -0.2368546, -0.5127711, -0.2368546, -0.4348191, -0.4167131] as BigDecimal[]
    Assertions.assertArrayEquals(exp, norm.asBigDecimalArray())
  }

  @Test
  void testMeanNormDouble() {
    def obs = DoubleColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.meanNorm(obs, 7)
    def exp = [-0.1508444, 0.8147756,  -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261] as double[]
    Assertions.assertArrayEquals(exp, norm.asDoubleArray())
  }

  @Test
  void testMeanNormFloat() {
    def obs = FloatColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211] as Float[])
    def norm = Normalizer.meanNorm(obs, 7)
    def exp = [ -0.1508444, 0.8147756, -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261 ] as Float[]
    Assertions.assertArrayEquals(exp, norm.asFloatArray())
  }

  @Test
  void testMeanNormZeroRangeFloatColumnsUseMissingValues() {
    def doubleNorm = Normalizer.meanNorm(DoubleColumn.create('doubleValues', [7d, 7d, 7d] as double[]))
    def floatNorm = Normalizer.meanNorm(FloatColumn.create('floatValues', [7f, 7f, 7f] as float[]))

    (0..<3).each { int row ->
      Assertions.assertTrue(doubleNorm.isMissing(row))
      Assertions.assertTrue(floatNorm.isMissing(row))
    }
  }

  @Test
  void testMeanNormBigDecimal() {
    def obs = BigDecimalColumn.create('values', [1200, 34567, 3456, 12, 3456, 985, 1211])
    def norm = Normalizer.meanNorm(obs, 7)
    def exp = [-0.1508444, 0.8147756,  -0.0855572, -0.1852244, -0.0855572, -0.1570664, -0.1505261] as BigDecimal[]
    Assertions.assertArrayEquals(exp, norm.asBigDecimalArray())
  }

}
