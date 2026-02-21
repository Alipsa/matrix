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

  @Test
  void testDispatchDensity2D() {
    LayerSpec layer = makeLayer(CharmStatType.DENSITY_2D, [bins: 5])
    List<LayerData> data = (1..30).collect {
      new LayerData(x: it % 6, y: (it / 6) as BigDecimal, rowIndex: it)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result.every { it.group != null })
  }

  @Test
  void testDispatchDensity2DSortsByNumericLevel() {
    LayerSpec layer = makeLayer(CharmStatType.DENSITY_2D, [bins: 12])
    List<LayerData> data = []
    int rowIndex = 0
    12.times {
      data << new LayerData(x: 0.1, y: 0.1, rowIndex: rowIndex++)
    }
    11.times {
      data << new LayerData(x: 1.1, y: 0.1, rowIndex: rowIndex++)
    }
    3.times {
      data << new LayerData(x: 2.1, y: 0.1, rowIndex: rowIndex++)
    }

    List<LayerData> result = StatEngine.apply(layer, data)

    assertEquals(3, result.size())
    List<Integer> levels = result.collect { (it.meta.level as Number).intValue() }
    assertEquals([2, 10, 11], levels)
  }

  @Test
  void testDispatchBinHex() {
    LayerSpec layer = makeLayer(CharmStatType.BIN_HEX, [bins: 5])
    List<LayerData> data = (1..30).collect {
      new LayerData(x: it % 6, y: (it / 6) as BigDecimal, rowIndex: it)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result.every { it.meta.hex == true })
  }

  @Test
  void testDispatchSummaryHexAndSummary2D() {
    List<LayerData> data = (1..20).collect {
      new LayerData(
          x: it % 5,
          y: (it / 5) as BigDecimal,
          fill: (it * 2) as BigDecimal,
          rowIndex: it
      )
    }
    List<LayerData> summary2d = StatEngine.apply(makeLayer(CharmStatType.SUMMARY_2D, [bins: 4, fun: 'mean']), data)
    assertFalse(summary2d.isEmpty())
    assertTrue(summary2d.every { it.fill instanceof BigDecimal })

    List<LayerData> summaryHex = StatEngine.apply(makeLayer(CharmStatType.SUMMARY_HEX, [bins: 4, fun: 'sum']), data)
    assertFalse(summaryHex.isEmpty())
    assertTrue(summaryHex.every { it.meta.hex == true })
  }

  @Test
  void testDispatchEllipse() {
    LayerSpec layer = makeLayer(CharmStatType.ELLIPSE, [segments: 20, level: 0.95])
    List<LayerData> data = (1..20).collect {
      new LayerData(x: it as BigDecimal, y: (it * 2) as BigDecimal, group: 'g1', rowIndex: it)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result.every { it.group == 'g1' })
  }

  @Test
  void testDispatchEllipseUsesBiasCorrectedSpread() {
    LayerSpec layer = makeLayer(CharmStatType.ELLIPSE, [segments: 8, level: 0.95])
    List<LayerData> data = [
        new LayerData(x: 0, y: 0, group: 'g1', rowIndex: 0),
        new LayerData(x: 2, y: 0, group: 'g1', rowIndex: 1)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)

    assertEquals(9, result.size())
    BigDecimal maxX = result.collect { it.x as BigDecimal }.max()
    assertTrue(maxX > 3.5, 'Expected sample sd based ellipse extent')
  }

  @Test
  void testDispatchSfAndSfCoordinates() {
    String polygon = 'POLYGON((0 0, 1 0, 1 1, 0 0))'
    List<LayerData> sfInput = [
        new LayerData(label: polygon, rowIndex: 0, meta: [__row: [geometry: polygon]])
    ]
    List<LayerData> sfResult = StatEngine.apply(makeLayer(CharmStatType.SF, [geometry: 'geometry']), sfInput)
    assertFalse(sfResult.isEmpty())
    assertTrue(sfResult.every { it.meta.__sf_type == 'POLYGON' })

    List<LayerData> sfCoordResult = StatEngine.apply(makeLayer(CharmStatType.SF_COORDINATES, [geometry: 'geometry']), sfInput)
    assertEquals(1, sfCoordResult.size())
    assertNotNull(sfCoordResult[0].x)
    assertNotNull(sfCoordResult[0].y)
  }

  @Test
  void testDispatchSpoke() {
    LayerSpec layer = makeLayer(CharmStatType.SPOKE, [angle: 'angle', radius: 'radius'])
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, rowIndex: 0, meta: [__row: [angle: 0.0, radius: 2.0]])
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(1, result.size())
    assertNotNull(result[0].xend)
    assertNotNull(result[0].yend)
  }

  @Test
  void testDispatchSpokeUsesNumericRadiusParamAsDefault() {
    LayerSpec layer = makeLayer(CharmStatType.SPOKE, [angle: 'angle', radius: 2.5])
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, rowIndex: 0, meta: [__row: [angle: 0.0, radius: 99.0]])
    ]
    List<LayerData> result = StatEngine.apply(layer, data)

    assertEquals(1, result.size())
    assertEquals(3.5, result[0].xend)
    assertEquals(1.0, result[0].yend)
  }

  @Test
  void testDispatchSpokeFallsBackToDefaultWhenRadiusParamIsColumnName() {
    LayerSpec layer = makeLayer(CharmStatType.SPOKE, [angle: 'angle', radius: 'missing_radius'])
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, rowIndex: 0, meta: [__row: [angle: 0.0, radius: 7.0]])
    ]
    List<LayerData> result = StatEngine.apply(layer, data)

    assertEquals(1, result.size())
    assertEquals(2.0, result[0].xend)
    assertEquals(1.0, result[0].yend)
  }

  @Test
  void testDispatchAlign() {
    LayerSpec layer = makeLayer(CharmStatType.ALIGN)
    List<LayerData> data = [
        new LayerData(x: 0, y: 1, group: 'A', rowIndex: 0),
        new LayerData(x: 2, y: 3, group: 'A', rowIndex: 1),
        new LayerData(x: 1, y: 4, group: 'B', rowIndex: 2),
        new LayerData(x: 3, y: 8, group: 'B', rowIndex: 3)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result*.group.toSet().containsAll(['A', 'B']))
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
