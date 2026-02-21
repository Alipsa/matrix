package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.StatEngine

import static org.junit.jupiter.api.Assertions.*

class StatEngineTest {

  @Test
  void testDispatchIdentity() {
    LayerSpec layer = makeLayer(CharmStatType.IDENTITY)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertSame(data, result)
  }

  @Test
  void testDispatchCount() {
    LayerSpec layer = makeLayer(CharmStatType.COUNT)
    List<LayerData> data = [
        new LayerData(x: 'A', y: 1, rowIndex: 0),
        new LayerData(x: 'A', y: 2, rowIndex: 1),
        new LayerData(x: 'B', y: 3, rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertNotNull(result)
    assertFalse(result.isEmpty())
  }

  @Test
  void testDispatchBin() {
    LayerSpec layer = makeLayer(CharmStatType.BIN, [bins: 5])
    List<LayerData> data = (1..20).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(5, result.size())
  }

  @Test
  void testDispatchBoxplot() {
    LayerSpec layer = makeLayer(CharmStatType.BOXPLOT)
    List<LayerData> data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].collect {
      new LayerData(x: 'G', y: it, rowIndex: it - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(1, result.size())
    assertNotNull(result[0].meta.median)
  }

  @Test
  void testDispatchSmooth() {
    LayerSpec layer = makeLayer(CharmStatType.SMOOTH)
    List<LayerData> data = (1..10).collect {
      new LayerData(x: it, y: it * 2, rowIndex: it - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
  }

  @Test
  void testDispatchEcdf() {
    LayerSpec layer = makeLayer(CharmStatType.ECDF)
    List<LayerData> data = [
        new LayerData(x: 1, y: 2, rowIndex: 0),
        new LayerData(x: 3, y: 5, rowIndex: 1),
        new LayerData(x: 2, y: 4, rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(3, result.size())
    assertEquals(1.0, result[0].x)
    assertEquals(1.0 / 3.0, result[0].y)
  }

  @Test
  void testEffectiveParamsMergesLayerAndStatParams() {
    StatSpec stat = StatSpec.of(CharmStatType.BIN, [bins: 10])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.HISTOGRAM), stat,
        null, true,
        PositionSpec.of(CharmPositionType.IDENTITY),
        [binwidth: 2, color: 'red']
    )
    Map<String, Object> params = StatEngine.effectiveParams(layer)
    // statSpec.params takes priority
    assertEquals(10, params.bins)
    // layer params also available
    assertEquals(2, params.binwidth)
    assertEquals('red', params.color)
  }

  @Test
  void testEffectiveParamsStatSpecOverridesLayerParams() {
    StatSpec stat = StatSpec.of(CharmStatType.BIN, [bins: 10])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.HISTOGRAM), stat,
        null, true,
        PositionSpec.of(CharmPositionType.IDENTITY),
        [bins: 5]
    )
    Map<String, Object> params = StatEngine.effectiveParams(layer)
    assertEquals(10, params.bins)
  }

  private static LayerSpec makeLayer(CharmStatType statType, Map<String, Object> params = [:]) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.POINT),
        StatSpec.of(statType),
        null, true,
        PositionSpec.of(CharmPositionType.IDENTITY),
        params
    )
  }
}
