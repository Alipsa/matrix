package charm.render.coord

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.coord.CartesianCoord

import static org.junit.jupiter.api.Assertions.*

class CartesianCoordTest {

  @Test
  void testPassThrough() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 2, y: 20, rowIndex: 1)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    assertEquals(1, result[0].x)
    assertEquals(10, result[0].y)
    assertEquals(2, result[1].x)
    assertEquals(20, result[1].y)
  }

  @Test
  void testEmptyData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> data = []
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testNullData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> result = CartesianCoord.compute(coord, null)
    assertNull(result)
  }

  @Test
  void testXlimClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [2, 8]])
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 5, y: 20, rowIndex: 1),
        new LayerData(x: 10, y: 30, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())
    // x=1 clamped to 2
    assertBigDecimalEquals(2, result[0].x)
    // x=5 within range, unchanged
    assertBigDecimalEquals(5, result[1].x)
    // x=10 clamped to 8
    assertBigDecimalEquals(8, result[2].x)
  }

  @Test
  void testYlimClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [ylim: [5, 25]])
    List<LayerData> data = [
        new LayerData(x: 1, y: 3, rowIndex: 0),
        new LayerData(x: 2, y: 15, rowIndex: 1),
        new LayerData(x: 3, y: 30, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())
    // y=3 clamped to 5
    assertBigDecimalEquals(5, result[0].y)
    // y=15 within range, unchanged
    assertBigDecimalEquals(15, result[1].y)
    // y=30 clamped to 25
    assertBigDecimalEquals(25, result[2].y)
  }

  @Test
  void testBothLimsClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [0, 10], ylim: [0, 100]])
    List<LayerData> data = [
        new LayerData(x: -5, y: -10, rowIndex: 0),
        new LayerData(x: 5, y: 50, rowIndex: 1),
        new LayerData(x: 15, y: 150, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())

    assertBigDecimalEquals(0, result[0].x)
    assertBigDecimalEquals(0, result[0].y)

    assertBigDecimalEquals(5, result[1].x)
    assertBigDecimalEquals(50, result[1].y)

    assertBigDecimalEquals(10, result[2].x)
    assertBigDecimalEquals(100, result[2].y)
  }

  @Test
  void testNoLimsPassesThrough() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [:])
    List<LayerData> data = [
        new LayerData(x: 100, y: 200, rowIndex: 0)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    // No limits: pass through (same reference since no copy needed)
    assertSame(data, result)
  }

  @Test
  void testPreservesNonNumericXValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [0, 10]])
    List<LayerData> data = [
        new LayerData(x: 'category', y: 10, rowIndex: 0)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(1, result.size())
    // Non-numeric x is not clamped
    assertEquals('category', result[0].x)
  }

  @Test
  void testPreservesAllFields() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [0, 10], ylim: [0, 20]])
    LayerData datum = new LayerData(
        x: 5, y: 10, color: 'red', fill: 'blue',
        xend: 15, yend: 25,
        xmin: 3, xmax: 7, ymin: 8, ymax: 12,
        size: 4, shape: 'circle', alpha: 0.5, linetype: 'dashed',
        group: 'G1', label: 'lbl', weight: 1.5, rowIndex: 3
    )
    datum.meta.custom = 'val'
    List<LayerData> result = CartesianCoord.compute(coord, [datum])
    LayerData updated = result[0]

    assertNotSame(datum, updated)
    assertBigDecimalEquals(5, updated.x)
    assertBigDecimalEquals(10, updated.y)

    assertEquals('red', updated.color)
    assertEquals('blue', updated.fill)
    assertEquals(4, updated.size)
    assertEquals('circle', updated.shape)
    assertBigDecimalEquals(0.5, updated.alpha)
    assertEquals('dashed', updated.linetype)
    assertEquals('G1', updated.group)
    assertEquals('lbl', updated.label)
    assertBigDecimalEquals(1.5, updated.weight)
    assertEquals(3, updated.rowIndex)
    assertEquals([custom: 'val'], updated.meta)
    assertNotSame(datum.meta, updated.meta)
  }

  private static void assertBigDecimalEquals(Number expected, Object actual) {
    assertEquals(0, (expected as BigDecimal).compareTo(actual as BigDecimal))
  }
}
