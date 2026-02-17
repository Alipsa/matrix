package charm.render.coord

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.coord.FlipCoord

import static org.junit.jupiter.api.Assertions.*

class FlipCoordTest {

  @Test
  void testFlipSwapsXAndY() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 2, y: 20, rowIndex: 1)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(2, result.size())

    assertEquals(10, result[0].x)
    assertEquals(1, result[0].y)
    assertEquals(20, result[1].x)
    assertEquals(2, result[1].y)
  }

  @Test
  void testFlipSwapsMinMax() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, xmin: 0, xmax: 2, ymin: 5, ymax: 15, rowIndex: 0)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(1, result.size())

    // xmin/xmax get original ymin/ymax values
    assertEquals(5, result[0].xmin)
    assertEquals(15, result[0].xmax)
    // ymin/ymax get original xmin/xmax values
    assertEquals(0, result[0].ymin)
    assertEquals(2, result[0].ymax)
  }

  @Test
  void testFlipSwapsEnd() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, xend: 3, yend: 30, rowIndex: 0)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(1, result.size())

    assertEquals(30, result[0].xend)
    assertEquals(3, result[0].yend)
  }

  @Test
  void testFlipPreservesNonCoordFields() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, color: 'red', fill: 'blue',
            size: 3, shape: 'circle', alpha: 0.5, linetype: 'solid',
            group: 'G1', label: 'lbl', weight: 2, rowIndex: 7)
    ]
    data[0].meta.custom = 'value'
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(1, result.size())

    assertEquals('red', result[0].color)
    assertEquals('blue', result[0].fill)
    assertEquals(3, result[0].size)
    assertEquals('circle', result[0].shape)
    assertEquals(0.5, result[0].alpha)
    assertEquals('solid', result[0].linetype)
    assertEquals('G1', result[0].group)
    assertEquals('lbl', result[0].label)
    assertEquals(2, result[0].weight)
    assertEquals(7, result[0].rowIndex)
    assertEquals('value', result[0].meta.custom)
  }

  @Test
  void testFlipWithCategoricalValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 'Category A', y: 42, rowIndex: 0)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(1, result.size())

    assertEquals(42, result[0].x)
    assertEquals('Category A', result[0].y)
  }

  @Test
  void testFlipEmptyData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = []
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testFlipNullData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> result = FlipCoord.compute(coord, null)
    assertNull(result)
  }

  @Test
  void testFlipNullXAndY() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: null, y: null, rowIndex: 0)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)
    assertEquals(1, result.size())
    assertNull(result[0].x)
    assertNull(result[0].y)
  }

  @Test
  void testDoubleFlipRestoresOriginal() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, xmin: 0, xmax: 2, ymin: 5, ymax: 15, rowIndex: 0)
    ]
    List<LayerData> once = FlipCoord.compute(coord, data)
    List<LayerData> twice = FlipCoord.compute(coord, once)

    assertEquals(1, twice[0].x)
    assertEquals(10, twice[0].y)
    assertEquals(0, twice[0].xmin)
    assertEquals(2, twice[0].xmax)
    assertEquals(5, twice[0].ymin)
    assertEquals(15, twice[0].ymax)
  }

  @Test
  void testFlipAppliesLimitsInFlippedSpace() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP, params: [xlim: [0, 20], ylim: [0, 5]])
    List<LayerData> data = [
        new LayerData(x: -10, y: 30, rowIndex: 0),
        new LayerData(x: 3, y: 4, rowIndex: 1)
    ]
    List<LayerData> result = FlipCoord.compute(coord, data)

    assertEquals(2, result.size())
    assertBigDecimalEquals(20, result[0].x)
    assertBigDecimalEquals(0, result[0].y)
    assertBigDecimalEquals(4, result[1].x)
    assertBigDecimalEquals(3, result[1].y)
  }

  private static void assertBigDecimalEquals(Number expected, Object actual) {
    assertEquals(0, (expected as BigDecimal).compareTo(actual as BigDecimal))
  }
}
