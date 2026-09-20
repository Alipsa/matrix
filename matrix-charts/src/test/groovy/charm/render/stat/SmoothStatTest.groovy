package charm.render.stat

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.SmoothStat

@SuppressWarnings('UnnecessaryGString')
class SmoothStatTest {

  @Test
  void testLinearRegression() {
    LayerSpec layer = makeLayer([n: 10])
    // Perfect linear: y = 2x
    List<LayerData> data = (1..10).collect {
      new LayerData(x: it, y: it * 2, rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    assertEquals(10, result.size())

    // Check fitted values follow y = 2x
    result.each { LayerData d ->
      double fitted = (d.y as BigDecimal).doubleValue()
      double expected = (d.x as BigDecimal).doubleValue() * 2
      assertEquals(expected, fitted, 0.1, 'Fitted value should match y=2x')
    }
  }

  @Test
  void testConfidenceIntervals() {
    LayerSpec layer = makeLayer([se: true, n: 10])
    List<LayerData> data = (1..20).collect {
      new LayerData(x: it, y: it * 2 + (it % 3), rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    assertFalse(result.isEmpty())
    result.each { LayerData d ->
      assertNotNull(d.meta.ymin, 'ymin should be set when se=true')
      assertNotNull(d.meta.ymax, 'ymax should be set when se=true')
      BigDecimal ymin = d.meta.ymin as BigDecimal
      BigDecimal ymax = d.meta.ymax as BigDecimal
      BigDecimal yFit = d.y as BigDecimal
      assertTrue(ymin <= yFit, "ymin should be <= fitted value")
      assertTrue(ymax >= yFit, "ymax should be >= fitted value")
    }
  }

  @Test
  void testNoConfidenceIntervals() {
    LayerSpec layer = makeLayer([se: false, n: 10])
    List<LayerData> data = (1..10).collect {
      new LayerData(x: it, y: it * 2, rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    result.each { LayerData d ->
      assertNull(d.meta.ymin, 'ymin should not be set when se=false')
      assertNull(d.meta.ymax, 'ymax should not be set when se=false')
    }
  }

  @Test
  void testPolynomialRegression() {
    LayerSpec layer = makeLayer([degree: 2, se: false, n: 10])
    // Quadratic data: y = x^2
    List<LayerData> data = (1..10).collect {
      new LayerData(x: it, y: it * it, rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    assertEquals(10, result.size())

    // Fitted values should be close to x^2
    result.each { LayerData d ->
      double x = (d.x as BigDecimal).doubleValue()
      double fitted = (d.y as BigDecimal).doubleValue()
      double expected = x * x
      assertEquals(expected, fitted, 1.0, 'Polynomial fit should approximate x^2')
    }
  }

  @Test
  void testFormulaParsing() {
    LayerSpec layer = makeLayer([formula: 'y ~ poly(x, 3)', se: false, n: 10])
    List<LayerData> data = (1..20).collect {
      new LayerData(x: it, y: it * it * it, rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    assertEquals(10, result.size())
  }

  @Test
  void testInsufficientData() {
    LayerSpec layer = makeLayer([n: 10])
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = SmoothStat.compute(layer, data)
    // With < 2 data points, should return original data
    assertSame(data, result)
  }

  @Test
  void testDefaultNPoints() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = (1..10).collect {
      new LayerData(x: it, y: it * 2, rowIndex: it - 1)
    }
    List<LayerData> result = SmoothStat.compute(layer, data)
    assertEquals(80, result.size())
  }

  @Test
  void smoothFitsOneLinePerColorGroupAndSupportsOnePoint() {
    List<LayerData> data = []
    (1..10).each { int i ->
      data << new LayerData(x: i, y: i * 2, color: 'up', rowIndex: data.size())
      data << new LayerData(x: i, y: 40 - i * 2, color: 'down', rowIndex: data.size())
    }
    List<LayerData> result = SmoothStat.compute(makeLayer([se: false, n: 5]), data)
    assertEquals(10, result.size())
    assertEquals(5, result.count { it.color == 'up' })
    assertEquals(5, result.count { it.color == 'down' })
    assertTrue((result.findAll { it.color == 'up' }.last().y as BigDecimal) >
        (result.findAll { it.color == 'up' }.first().y as BigDecimal))
    assertEquals(1, SmoothStat.compute(makeLayer([se: false, n: 1]), data.findAll { it.color == 'up' }).size())
  }

  private static LayerSpec makeLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.SMOOTH),
        StatSpec.of(CharmStatType.SMOOTH, statParams)
    )
  }

}
