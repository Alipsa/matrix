package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.BinStat

import static org.junit.jupiter.api.Assertions.*

class BinStatTest {

  @Test
  void testDefaultBins() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = (1..100).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    assertEquals(30, result.size())
  }

  @Test
  void testCustomBinCount() {
    LayerSpec layer = makeLayer([bins: 5])
    List<LayerData> data = (1..20).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    assertEquals(5, result.size())
  }

  @Test
  void testBinwidth() {
    LayerSpec layer = makeLayer([binwidth: 10])
    List<LayerData> data = (1..100).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    // Range 1-100 = 99, binwidth 10 -> ~10 bins
    assertTrue(result.size() >= 9 && result.size() <= 10,
        "Expected 9-10 bins, got ${result.size()}")
  }

  @Test
  void testBinCountsSum() {
    LayerSpec layer = makeLayer([bins: 5])
    List<LayerData> data = (1..20).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    int totalCount = result.collect { (it.y as Number).intValue() }.sum() as int
    assertEquals(20, totalCount)
  }

  @Test
  void testBinMetadata() {
    LayerSpec layer = makeLayer([bins: 2])
    List<LayerData> data = (1..10).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    assertEquals(2, result.size())
    result.each { LayerData datum ->
      assertNotNull(datum.meta.binStart, "binStart should be set")
      assertNotNull(datum.meta.binEnd, "binEnd should be set")
      assertNotNull(datum.meta.density, "density should be set")
      assertNotNull(datum.meta.xmin, "xmin should be set")
      assertNotNull(datum.meta.xmax, "xmax should be set")
    }
  }

  @Test
  void testBinCentersAreCorrect() {
    LayerSpec layer = makeLayer([bins: 2])
    List<LayerData> data = [
        new LayerData(x: 0, y: 0, rowIndex: 0),
        new LayerData(x: 10, y: 0, rowIndex: 1)
    ]
    List<LayerData> result = BinStat.compute(layer, data)
    assertEquals(2, result.size())
    // x values should be bin centers
    BigDecimal center1 = result[0].x as BigDecimal
    BigDecimal center2 = result[1].x as BigDecimal
    assertTrue(center1 < center2, "First bin center should be less than second")
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer([bins: 5])
    List<LayerData> result = BinStat.compute(layer, [])
    assertTrue(result.isEmpty())
  }

  @Test
  void testAllSameValues() {
    LayerSpec layer = makeLayer([bins: 3])
    List<LayerData> data = [5, 5, 5, 5].collect { new LayerData(x: it, y: 0, rowIndex: 0) }
    List<LayerData> result = BinStat.compute(layer, data)
    // When all values equal, range expands by 1
    assertFalse(result.isEmpty())
    int totalCount = result.collect { (it.y as Number).intValue() }.sum() as int
    assertEquals(4, totalCount)
  }

  @Test
  void testParamsFromLayerParams() {
    // Simulate how PlotSpec passes params via layer.params (not statSpec.params)
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.HISTOGRAM),
        StatSpec.of(CharmStatType.BIN),
        null, true,
        PositionSpec.of(CharmPositionType.IDENTITY),
        [bins: 5]
    )
    List<LayerData> data = (1..20).collect { new LayerData(x: it, y: 0, rowIndex: it - 1) }
    List<LayerData> result = BinStat.compute(layer, data)
    assertEquals(5, result.size())
  }

  private static LayerSpec makeLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.HISTOGRAM),
        StatSpec.of(CharmStatType.BIN, statParams)
    )
  }
}
