package charm.render.position

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.position.PositionEngine

import static org.junit.jupiter.api.Assertions.*

class PositionEngineTest {

  @Test
  void testDispatchIdentity() {
    LayerSpec layer = makeLayer(CharmPositionType.IDENTITY)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertSame(data, result)
  }

  @Test
  void testDispatchDodge() {
    LayerSpec layer = makeLayer(CharmPositionType.DODGE)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'B', rowIndex: 1)
    ]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    // Dodged data should have different x positions
    assertNotEquals(result[0].x, result[1].x)
  }

  @Test
  void testDispatchStack() {
    LayerSpec layer = makeLayer(CharmPositionType.STACK)
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, rowIndex: 0),
        new LayerData(x: 'A', y: 20, rowIndex: 1)
    ]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    assertNotNull(result[0].ymin)
    assertNotNull(result[0].ymax)
  }

  @Test
  void testDispatchFill() {
    LayerSpec layer = makeLayer(CharmPositionType.FILL)
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, rowIndex: 0),
        new LayerData(x: 'A', y: 20, rowIndex: 1)
    ]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    // Fill normalizes to [0, 1]
    BigDecimal lastYmax = result.last().ymax as BigDecimal
    assertEquals(0, BigDecimal.ONE.compareTo(lastYmax))
  }

  @Test
  void testDispatchJitter() {
    LayerSpec layer = makeLayer(CharmPositionType.JITTER, [width: 1, height: 1, seed: 42])
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertEquals(1, result.size())
    assertNotEquals(data[0].x, result[0].x)
    assertNotEquals(data[0].y, result[0].y)
  }

  @Test
  void testDispatchDodge2() {
    LayerSpec layer = makeLayer(CharmPositionType.DODGE2, [padding: 0.2, width: 0.8])
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'B', rowIndex: 1)
    ]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertEquals(2, result.size())
    assertNotEquals(result[0].x, result[1].x)
  }

  @Test
  void testDodge2PaddingExpandsAroundOriginalBucketCenter() {
    LayerSpec noPaddingLayer = makeLayer(CharmPositionType.DODGE2, [padding: 0.0, width: 0.8])
    LayerSpec paddedLayer = makeLayer(CharmPositionType.DODGE2, [padding: 0.2, width: 0.8])
    List<LayerData> data = [
        new LayerData(x: -1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: -1, y: 20, fill: 'B', rowIndex: 1)
    ]

    List<BigDecimal> noPaddingX = PositionEngine.apply(noPaddingLayer, data)
        .collect { LayerData datum -> datum.x as BigDecimal }
        .sort()
    List<BigDecimal> paddedX = PositionEngine.apply(paddedLayer, data)
        .collect { LayerData datum -> datum.x as BigDecimal }
        .sort()

    assertEquals(2, noPaddingX.size())
    assertEquals(2, paddedX.size())
    assertTrue(paddedX[0] < noPaddingX[0])
    assertTrue(paddedX[1] > noPaddingX[1])
  }

  @Test
  void testDispatchNudge() {
    LayerSpec layer = makeLayer(CharmPositionType.NUDGE, [x: 0.5, y: -1])
    List<LayerData> data = [new LayerData(x: 2, y: 3, rowIndex: 0)]
    List<LayerData> result = PositionEngine.apply(layer, data)
    assertEquals(1, result.size())
    assertEquals(0, (result[0].x as BigDecimal).compareTo(2.5))
    assertEquals(0, (result[0].y as BigDecimal).compareTo(2.0))
  }

  @Test
  void testEffectiveParamsMergesLayerAndPositionParams() {
    PositionSpec pos = PositionSpec.of(CharmPositionType.DODGE, [width: 0.8])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        pos,
        [color: 'red']
    )
    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    // positionSpec.params takes priority
    assertEquals(0.8, params.width)
    // layer params also available
    assertEquals('red', params.color)
  }

  @Test
  void testEffectiveParamsPositionSpecOverridesLayerParams() {
    PositionSpec pos = PositionSpec.of(CharmPositionType.DODGE, [width: 0.8])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        pos,
        [width: 0.5]
    )
    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    assertEquals(0.8, params.width)
  }

  private static LayerSpec makeLayer(CharmPositionType positionType, Map<String, Object> params = [:]) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.POINT),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        PositionSpec.of(positionType),
        params
    )
  }
}
