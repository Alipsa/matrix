package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.DensityStat

import static org.junit.jupiter.api.Assertions.*

class DensityStatTest {

  @Test
  void testBasicDensity() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..100).collect {
      new LayerData(x: rng.nextGaussian() * 10 + 50, y: 0, rowIndex: it - 1)
    }
    List<LayerData> result = DensityStat.compute(layer, data)
    assertFalse(result.isEmpty())
    // Default n=512 evaluation points
    assertEquals(512, result.size())
  }

  @Test
  void testDensityPositiveValues() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..50).collect {
      new LayerData(x: rng.nextGaussian() * 5 + 25, y: 0, rowIndex: it - 1)
    }
    List<LayerData> result = DensityStat.compute(layer, data)
    result.each { LayerData d ->
      BigDecimal density = d.y as BigDecimal
      assertTrue(density >= 0, "Density values should be non-negative")
    }
  }

  @Test
  void testDensityIntegratesToApproximatelyOne() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..200).collect {
      new LayerData(x: rng.nextGaussian() * 10, y: 0, rowIndex: it - 1)
    }
    List<LayerData> result = DensityStat.compute(layer, data)
    // Approximate integral using trapezoidal rule
    double integral = 0.0
    for (int i = 1; i < result.size(); i++) {
      double dx = (result[i].x as BigDecimal).doubleValue() - (result[i - 1].x as BigDecimal).doubleValue()
      double avgY = ((result[i].y as BigDecimal).doubleValue() + (result[i - 1].y as BigDecimal).doubleValue()) / 2.0
      integral += dx * avgY
    }
    assertEquals(1.0, integral, 0.15, "Density should integrate to approximately 1")
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> result = DensityStat.compute(layer, [])
    assertTrue(result.isEmpty())
  }

  @Test
  void testInsufficientData() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = [new LayerData(x: 5, y: 0, rowIndex: 0)]
    List<LayerData> result = DensityStat.compute(layer, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testExtractNumericX() {
    List<LayerData> data = [
        new LayerData(x: 1, y: 0, rowIndex: 0),
        new LayerData(x: null, y: 0, rowIndex: 1),
        new LayerData(x: 3.5, y: 0, rowIndex: 2),
        new LayerData(x: 'abc', y: 0, rowIndex: 3)
    ]
    List<Number> values = DensityStat.extractNumericX(data)
    assertEquals(2, values.size())
  }

  @Test
  void testBuildKdeParams() {
    Map<String, Object> params = [kernel: 'gaussian', bw: 2.0, adjust: 1.5, n: 256]
    Map<String, Object> kdeParams = DensityStat.buildKdeParams(params)
    assertEquals('gaussian', kdeParams.kernel)
    assertEquals(2.0, kdeParams.bandwidth)
    assertEquals(1.5, kdeParams.adjust)
    assertEquals(256, kdeParams.n)
  }

  @Test
  void testBuildKdeParamsIgnoresNulls() {
    Map<String, Object> params = [kernel: null, bw: null]
    Map<String, Object> kdeParams = DensityStat.buildKdeParams(params)
    assertFalse(kdeParams.containsKey('kernel'))
    assertFalse(kdeParams.containsKey('bandwidth'))
  }

  private static LayerSpec makeLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.LINE),
        StatSpec.of(CharmStatType.DENSITY, statParams)
    )
  }
}
