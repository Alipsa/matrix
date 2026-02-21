package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.StatEngine
import se.alipsa.matrix.charm.render.stat.StatUtils

import static org.junit.jupiter.api.Assertions.*

class P1StatTest {

  @Test
  void testSummaryStat() {
    LayerSpec layer = makeLayer(CharmStatType.SUMMARY, [fun: 'mean'])
    List<LayerData> data = [
        new LayerData(x: 1, y: 2, group: 'g1', rowIndex: 0),
        new LayerData(x: 1, y: 4, group: 'g1', rowIndex: 1),
        new LayerData(x: 2, y: 6, group: 'g1', rowIndex: 2),
        new LayerData(x: 2, y: 8, group: 'g1', rowIndex: 3),
        new LayerData(x: 1, y: 1, group: 'g2', rowIndex: 4),
        new LayerData(x: 1, y: 3, group: 'g2', rowIndex: 5),
        new LayerData(x: 2, y: 5, group: 'g2', rowIndex: 6),
        new LayerData(x: 2, y: 7, group: 'g2', rowIndex: 7)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(4, result.size())
    BigDecimal g1x1 = result.find { it.group == 'g1' && (it.x as BigDecimal).compareTo(1.0) == 0 }?.y as BigDecimal
    BigDecimal g1x2 = result.find { it.group == 'g1' && (it.x as BigDecimal).compareTo(2.0) == 0 }?.y as BigDecimal
    BigDecimal g2x1 = result.find { it.group == 'g2' && (it.x as BigDecimal).compareTo(1.0) == 0 }?.y as BigDecimal
    BigDecimal g2x2 = result.find { it.group == 'g2' && (it.x as BigDecimal).compareTo(2.0) == 0 }?.y as BigDecimal
    assertEquals(0, g1x1.compareTo(3.0))
    assertEquals(0, g1x2.compareTo(7.0))
    assertEquals(0, g2x1.compareTo(2.0))
    assertEquals(0, g2x2.compareTo(6.0))
  }

  @Test
  void testBin2DStat() {
    LayerSpec layer = makeLayer(CharmStatType.BIN2D, [bins: 4])
    List<LayerData> data = (1..16).collect { int i ->
      new LayerData(x: (i % 4) + 1, y: ((i / 4) as int) + 1, rowIndex: i - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result.every { it.meta.count != null })
  }

  @Test
  void testContourStat() {
    LayerSpec layer = makeLayer(CharmStatType.CONTOUR, [bins: 3])
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, label: 0.2, rowIndex: 0),
        new LayerData(x: 2, y: 1.5, label: 0.6, rowIndex: 1),
        new LayerData(x: 3, y: 2, label: 0.9, rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(3, result.size())
    assertTrue(result.every { it.meta.level != null })
    assertTrue(result.every { it.group?.toString()?.startsWith('level-') })
  }

  @Test
  void testContourStatKeepsZeroZValues() {
    LayerSpec layer = makeLayer(CharmStatType.CONTOUR, [bins: 2])
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, label: 0, rowIndex: 0),
        new LayerData(x: 2, y: 2, label: 1, rowIndex: 1)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(2, result.size())
    assertTrue(result.any { (it.meta.z as BigDecimal).compareTo(0.0) == 0 })
  }

  @Test
  void testQqStat() {
    LayerSpec layer = makeLayer(CharmStatType.QQ)
    List<LayerData> data = (1..8).collect { int i ->
      new LayerData(x: i, rowIndex: i - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(8, result.size())
    assertNotNull(result[0].x)
    assertNotNull(result[0].y)
  }

  @Test
  void testQqStatUsesZeroXValues() {
    LayerSpec layer = makeLayer(CharmStatType.QQ)
    List<LayerData> data = [
        new LayerData(x: 0, y: 100, rowIndex: 0),
        new LayerData(x: 1, y: 101, rowIndex: 1),
        new LayerData(x: 2, y: 102, rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    List<BigDecimal> observed = result.collect { it.y as BigDecimal }
    assertTrue(observed.any { it.compareTo(0.0) == 0 })
    assertFalse(observed.any { it.compareTo(100.0) == 0 })
  }

  @Test
  void testQqLineStat() {
    LayerSpec layer = makeLayer(CharmStatType.QQ_LINE)
    List<LayerData> data = (1..8).collect { int i ->
      new LayerData(x: i, rowIndex: i - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(2, result.size())
    assertNotNull(result[0].meta.slope)
    assertNotNull(result[0].meta.intercept)
  }

  @Test
  void testQqLineStatUsesZeroXValues() {
    LayerSpec layer = makeLayer(CharmStatType.QQ_LINE)
    List<LayerData> data = [
        new LayerData(x: 0, y: 100, rowIndex: 0),
        new LayerData(x: 1, y: 101, rowIndex: 1),
        new LayerData(x: 2, y: 102, rowIndex: 2),
        new LayerData(x: 3, y: 103, rowIndex: 3)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    BigDecimal slope = result[0].meta.slope as BigDecimal
    assertTrue(slope > 0)
    assertTrue(slope < 5)
  }

  @Test
  void testFunctionStat() {
    LayerSpec layer = makeLayer(CharmStatType.FUNCTION, [fun: ({ Number x -> x * 2 } as Closure<Number>), xlim: [0, 4], n: 5])
    List<LayerData> result = StatEngine.apply(layer, [])
    assertEquals(5, result.size())
    assertEquals(0, (result[0].x as BigDecimal).compareTo(0.0))
    assertEquals(0, (result[0].y as BigDecimal).compareTo(0.0))
  }

  @Test
  void testSummaryBinStat() {
    LayerSpec layer = makeLayer(CharmStatType.SUMMARY_BIN, [bins: 3, fun: 'median'])
    List<LayerData> data = (1..12).collect { int i ->
      new LayerData(x: i, y: i % 5, rowIndex: i - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertFalse(result.isEmpty())
    assertTrue(result.every { it.meta.xmin != null && it.meta.xmax != null })
  }

  @Test
  void testUniqueStat() {
    LayerSpec layer = makeLayer(CharmStatType.UNIQUE)
    List<LayerData> data = [
        new LayerData(x: 1, y: 2, group: 'g1', rowIndex: 0),
        new LayerData(x: 1, y: 2, group: 'g1', rowIndex: 1),
        new LayerData(x: 2, y: 3, group: 'g1', rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(2, result.size())
  }

  @Test
  void testUniqueStatRetainsDistinctColorAndFill() {
    LayerSpec layer = makeLayer(CharmStatType.UNIQUE)
    List<LayerData> data = [
        new LayerData(x: 1, y: 2, group: 'g1', color: 'red', fill: 'pink', rowIndex: 0),
        new LayerData(x: 1, y: 2, group: 'g1', color: 'blue', fill: 'pink', rowIndex: 1),
        new LayerData(x: 1, y: 2, group: 'g1', color: 'red', fill: 'pink', rowIndex: 2)
    ]
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(2, result.size())
  }

  @Test
  void testNormalQuantileClampsOutOfRangeProbabilities() {
    BigDecimal lower = StatUtils.normalQuantile(0.0)
    BigDecimal upper = StatUtils.normalQuantile(1.0)
    assertNotNull(lower)
    assertNotNull(upper)
    assertTrue(Double.isFinite(lower as double))
    assertTrue(Double.isFinite(upper as double))
    assertTrue(lower < 0)
    assertTrue(upper > 0)
  }

  @Test
  void testGroupBySeriesKeepsFalsyKeys() {
    List<LayerData> data = [
        new LayerData(x: 1, y: 1, group: 0, rowIndex: 0),
        new LayerData(x: 2, y: 2, group: false, rowIndex: 1),
        new LayerData(x: 3, y: 3, group: '', rowIndex: 2),
        new LayerData(x: 4, y: 4, rowIndex: 3)
    ]
    Map<Object, List<LayerData>> grouped = StatUtils.groupBySeries(data)
    assertEquals(4, grouped.size())
    assertTrue(grouped.containsKey(0))
    assertTrue(grouped.containsKey(false))
    assertTrue(grouped.containsKey(''))
    assertTrue(grouped.containsKey('__all__'))
  }

  @Test
  void testQuantileStat() {
    LayerSpec layer = makeLayer(CharmStatType.QUANTILE, [quantiles: [0.5], n: 6])
    List<LayerData> data = (1..20).collect { int i ->
      new LayerData(x: i, y: i * 2 + (i % 3), rowIndex: i - 1)
    }
    List<LayerData> result = StatEngine.apply(layer, data)
    assertEquals(6, result.size())
    assertTrue(result.every { it.meta.quantile != null })
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
