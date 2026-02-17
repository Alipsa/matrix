package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.BoxplotStat

import static org.junit.jupiter.api.Assertions.*

class BoxplotStatTest {

  @Test
  void testBasicQuartiles() {
    LayerSpec layer = makeLayer([:])
    // Sorted: 1,2,3,4,5,6,7,8,9,10
    List<LayerData> data = (1..10).collect {
      new LayerData(x: 'G', y: it, rowIndex: it - 1)
    }
    List<LayerData> result = BoxplotStat.compute(layer, data)
    assertEquals(1, result.size())
    LayerData r = result[0]
    assertNotNull(r.meta.q1)
    assertNotNull(r.meta.median)
    assertNotNull(r.meta.q3)
    assertNotNull(r.meta.whiskerLow)
    assertNotNull(r.meta.whiskerHigh)
    assertNotNull(r.meta.n)
    assertEquals(10, r.meta.n)

    // Median of 1..10 is 5.5 (type 7)
    BigDecimal median = r.meta.median as BigDecimal
    assertEquals(5.5, median.doubleValue(), 0.01)
  }

  @Test
  void testMultipleGroups() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = []
    ['A', 'B'].each { String group ->
      (1..5).each { int val ->
        data << new LayerData(x: group, y: val, rowIndex: data.size())
      }
    }
    List<LayerData> result = BoxplotStat.compute(layer, data)
    assertEquals(2, result.size())
    assertNotNull(result.find { it.x == 'A' })
    assertNotNull(result.find { it.x == 'B' })
  }

  @Test
  void testOutlierDetection() {
    LayerSpec layer = makeLayer([coef: 1.5])
    // Normal data with an outlier at 100
    List<Number> values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100]
    List<LayerData> data = values.collect {
      new LayerData(x: 'G', y: it, rowIndex: 0)
    }
    List<LayerData> result = BoxplotStat.compute(layer, data)
    assertEquals(1, result.size())
    List<BigDecimal> outliers = result[0].meta.outliers as List<BigDecimal>
    assertNotNull(outliers)
    assertFalse(outliers.isEmpty(), "Should detect outlier at 100")
    assertTrue(outliers.any { (it as BigDecimal).doubleValue() == 100.0 })
  }

  @Test
  void testCoefParameter() {
    // Very large coef -> no outliers
    LayerSpec layerWide = makeLayer([coef: 100])
    List<Number> values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100]
    List<LayerData> data = values.collect {
      new LayerData(x: 'G', y: it, rowIndex: 0)
    }
    List<LayerData> result = BoxplotStat.compute(layerWide, data)
    List<BigDecimal> outliers = result[0].meta.outliers as List<BigDecimal>
    assertTrue(outliers.isEmpty(), "Large coef should produce no outliers")
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> result = BoxplotStat.compute(layer, [])
    assertTrue(result.isEmpty())
  }

  @Test
  void testQuantileType7() {
    // Verify quantile type 7 with known values
    List<BigDecimal> sorted = [1, 2, 3, 4, 5] as List<BigDecimal>
    BigDecimal q25 = BoxplotStat.quantileType7(sorted, 0.25)
    BigDecimal q50 = BoxplotStat.quantileType7(sorted, 0.5)
    BigDecimal q75 = BoxplotStat.quantileType7(sorted, 0.75)

    assertEquals(2.0, q25.doubleValue(), 0.01)
    assertEquals(3.0, q50.doubleValue(), 0.01)
    assertEquals(4.0, q75.doubleValue(), 0.01)
  }

  @Test
  void testQuantileType7SingleValue() {
    List<BigDecimal> sorted = [42] as List<BigDecimal>
    BigDecimal result = BoxplotStat.quantileType7(sorted, 0.5)
    assertEquals(42.0, result.doubleValue(), 0.01)
  }

  @Test
  void testWhiskerBounds() {
    LayerSpec layer = makeLayer([:])
    List<LayerData> data = (1..20).collect {
      new LayerData(x: 'G', y: it, rowIndex: it - 1)
    }
    List<LayerData> result = BoxplotStat.compute(layer, data)
    LayerData r = result[0]
    BigDecimal whiskerLow = r.meta.whiskerLow as BigDecimal
    BigDecimal whiskerHigh = r.meta.whiskerHigh as BigDecimal
    BigDecimal q1 = r.meta.q1 as BigDecimal
    BigDecimal q3 = r.meta.q3 as BigDecimal
    assertTrue(whiskerLow <= q1, "whiskerLow should be <= q1")
    assertTrue(whiskerHigh >= q3, "whiskerHigh should be >= q3")
  }

  private static LayerSpec makeLayer(Map<String, Object> statParams) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.BOXPLOT),
        StatSpec.of(CharmStatType.BOXPLOT, statParams)
    )
  }
}
