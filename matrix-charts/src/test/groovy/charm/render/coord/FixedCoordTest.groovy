package charm.render.coord

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.coord.FixedCoord

import static org.junit.jupiter.api.Assertions.*

class FixedCoordTest {

  @Test
  void testPassThrough() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 1.0])
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 2, y: 20, rowIndex: 1)
    ]
    List<LayerData> result = FixedCoord.compute(coord, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    assertEquals(1, result[0].x)
    assertEquals(10, result[0].y)
    assertEquals(2, result[1].x)
    assertEquals(20, result[1].y)
  }

  @Test
  void testRatioIsMetadata() {
    // Fixed coord at data level behaves like Cartesian
    // The ratio is metadata consumed by the renderer
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 2.0])
    List<LayerData> data = [
        new LayerData(x: 5, y: 10, rowIndex: 0)
    ]
    List<LayerData> result = FixedCoord.compute(coord, data)
    assertEquals(1, result.size())
    assertEquals(5, result[0].x)
    assertEquals(10, result[0].y)
    // Ratio is accessible from coord spec params
    assertEquals(0, new BigDecimal('2.0').compareTo(coord.ratio))
  }

  @Test
  void testFixedWithXlim() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 1.0, xlim: [0, 10]])
    List<LayerData> data = [
        new LayerData(x: -5, y: 10, rowIndex: 0),
        new LayerData(x: 5, y: 20, rowIndex: 1),
        new LayerData(x: 15, y: 30, rowIndex: 2)
    ]
    List<LayerData> result = FixedCoord.compute(coord, data)
    assertEquals(3, result.size())

    // x=-5 clamped to 0
    assertEquals(0, new BigDecimal('0').compareTo(result[0].x as BigDecimal))
    // x=5 within range
    assertEquals(5, result[1].x)
    // x=15 clamped to 10
    assertEquals(0, new BigDecimal('10').compareTo(result[2].x as BigDecimal))
  }

  @Test
  void testEmptyData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 1.0])
    List<LayerData> data = []
    List<LayerData> result = FixedCoord.compute(coord, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testNullData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 1.0])
    List<LayerData> result = FixedCoord.compute(coord, null)
    assertNull(result)
  }

  @Test
  void testPreservesAllFields() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 1.0])
    LayerData datum = new LayerData(
        x: 5, y: 10, color: 'red', fill: 'blue',
        xmin: 3, xmax: 7, ymin: 8, ymax: 12,
        group: 'G1', rowIndex: 3
    )
    datum.meta.custom = 'val'
    List<LayerData> result = FixedCoord.compute(coord, [datum])
    // No limits: same reference via pass-through
    assertSame(datum, result[0])
  }
}
