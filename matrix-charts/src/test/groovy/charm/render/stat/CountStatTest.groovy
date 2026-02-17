package charm.render.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.stat.CountStat

import static org.junit.jupiter.api.Assertions.*

class CountStatTest {

  @Test
  void testCountGroupsByX() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 1, rowIndex: 0),
        new LayerData(x: 'B', y: 2, rowIndex: 1),
        new LayerData(x: 'A', y: 3, rowIndex: 2),
        new LayerData(x: 'A', y: 4, rowIndex: 3),
        new LayerData(x: 'B', y: 5, rowIndex: 4)
    ]
    List<LayerData> result = CountStat.compute(layer, data)
    assertEquals(2, result.size())

    LayerData groupA = result.find { it.x == 'A' }
    LayerData groupB = result.find { it.x == 'B' }
    assertNotNull(groupA)
    assertNotNull(groupB)
    assertEquals(3, groupA.y)
    assertEquals(2, groupB.y)
  }

  @Test
  void testCountPercent() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 1, rowIndex: 0),
        new LayerData(x: 'B', y: 2, rowIndex: 1),
        new LayerData(x: 'A', y: 3, rowIndex: 2),
        new LayerData(x: 'A', y: 4, rowIndex: 3)
    ]
    List<LayerData> result = CountStat.compute(layer, data)
    LayerData groupA = result.find { it.x == 'A' }
    // 3 out of 4 = 75%
    assertEquals(75.0, (groupA.meta.percent as BigDecimal).doubleValue(), 0.01)
  }

  @Test
  void testEmptyData() {
    LayerSpec layer = makeLayer()
    List<LayerData> result = CountStat.compute(layer, [])
    assertTrue(result.isEmpty())
  }

  @Test
  void testSingleGroup() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'X', y: 1, rowIndex: 0),
        new LayerData(x: 'X', y: 2, rowIndex: 1)
    ]
    List<LayerData> result = CountStat.compute(layer, data)
    assertEquals(1, result.size())
    assertEquals(2, result[0].y)
    assertEquals(100.0, (result[0].meta.percent as BigDecimal).doubleValue(), 0.01)
  }

  @Test
  void testPreservesTemplateColor() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 1, color: 'red', fill: 'blue', rowIndex: 0),
        new LayerData(x: 'A', y: 2, color: 'red', fill: 'blue', rowIndex: 1)
    ]
    List<LayerData> result = CountStat.compute(layer, data)
    assertEquals('red', result[0].color)
    assertEquals('blue', result[0].fill)
  }

  private static LayerSpec makeLayer() {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.COUNT)
    )
  }
}
