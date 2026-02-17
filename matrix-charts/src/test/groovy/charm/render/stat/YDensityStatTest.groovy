package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.YDensityStat

import static org.junit.jupiter.api.Assertions.*

class YDensityStatTest {

  @Test
  void testBasicYDensity() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..50).collect {
      new LayerData(x: 'G', y: rng.nextGaussian() * 10 + 50, rowIndex: it - 1)
    }
    List<LayerData> result = YDensityStat.compute(layer, data)
    assertFalse(result.isEmpty())
  }

  @Test
  void testDensityInXField() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..50).collect {
      new LayerData(x: 'G', y: rng.nextGaussian() * 5 + 25, rowIndex: it - 1)
    }
    List<LayerData> result = YDensityStat.compute(layer, data)
    // For y-density, density goes in x field
    result.each { LayerData d ->
      BigDecimal density = d.x as BigDecimal
      assertTrue(density >= 0, "Density values in x should be non-negative")
    }
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> result = YDensityStat.compute(layer, [])
    assertTrue(result.isEmpty())
  }

  @Test
  void testInsufficientData() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = [new LayerData(x: 'G', y: 5, rowIndex: 0)]
    List<LayerData> result = YDensityStat.compute(layer, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testPreservesTemplateColors() {
    LayerSpec layer = makeLayer([:])
    Random rng = new Random(42)
    List<LayerData> data = (1..20).collect {
      new LayerData(x: 'G', y: rng.nextGaussian() * 5 + 25, color: 'red', fill: 'blue', rowIndex: it - 1)
    }
    List<LayerData> result = YDensityStat.compute(layer, data)
    assertFalse(result.isEmpty())
    result.each { LayerData d ->
      assertEquals('red', d.color)
      assertEquals('blue', d.fill)
    }
  }

  private static LayerSpec makeLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.POINT),
        StatSpec.of(CharmStatType.YDENSITY, statParams)
    )
  }
}
