package charm.render.position

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.position.FillPosition

import static org.junit.jupiter.api.Assertions.*

class FillPositionTest {

  @Test
  void testFillNormalizesToZeroOne() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 30, rowIndex: 0),
        new LayerData(x: 'A', y: 70, rowIndex: 1)
    ]
    List<LayerData> result = FillPosition.compute(layer, data)
    assertEquals(2, result.size())

    // First: ymin=0/100=0, ymax=30/100=0.3
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('0.3').compareTo(result[0].ymax as BigDecimal))

    // Second: ymin=30/100=0.3, ymax=100/100=1.0
    assertEquals(0, new BigDecimal('0.3').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, BigDecimal.ONE.compareTo(result[1].ymax as BigDecimal))
  }

  @Test
  void testFillYIsCentered() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 50, rowIndex: 0),
        new LayerData(x: 'A', y: 50, rowIndex: 1)
    ]
    List<LayerData> result = FillPosition.compute(layer, data)

    // First: y = (0 + 0.5) / 2 = 0.25
    assertEquals(0, new BigDecimal('0.25').compareTo(result[0].y as BigDecimal))

    // Second: y = (0.5 + 1.0) / 2 = 0.75
    assertEquals(0, new BigDecimal('0.75').compareTo(result[1].y as BigDecimal))
  }

  @Test
  void testFillMultipleXGroups() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 20, rowIndex: 0),
        new LayerData(x: 'A', y: 80, rowIndex: 1),
        new LayerData(x: 'B', y: 10, rowIndex: 2),
        new LayerData(x: 'B', y: 30, rowIndex: 3)
    ]
    List<LayerData> result = FillPosition.compute(layer, data)
    assertEquals(4, result.size())

    // A group: total=100
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('0.2').compareTo(result[0].ymax as BigDecimal))
    assertEquals(0, new BigDecimal('0.2').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, BigDecimal.ONE.compareTo(result[1].ymax as BigDecimal))

    // B group: total=40
    assertEquals(0, BigDecimal.ZERO.compareTo(result[2].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('0.25').compareTo(result[2].ymax as BigDecimal))
    assertEquals(0, new BigDecimal('0.25').compareTo(result[3].ymin as BigDecimal))
    assertEquals(0, BigDecimal.ONE.compareTo(result[3].ymax as BigDecimal))
  }

  @Test
  void testFillReverse() {
    PositionSpec pos = PositionSpec.of(CharmPositionType.FILL, [reverse: true])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true, pos, [:]
    )
    List<LayerData> data = [
        new LayerData(x: 'A', y: 30, fill: 'X', rowIndex: 0),
        new LayerData(x: 'A', y: 70, fill: 'Y', rowIndex: 1)
    ]
    List<LayerData> result = FillPosition.compute(layer, data)
    assertEquals(2, result.size())

    // With reverse, processing order is [Y, X]
    // Y (y=70): ymin=0/100=0, ymax=70/100=0.7
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('0.7').compareTo(result[0].ymax as BigDecimal))
    // X (y=30): ymin=70/100=0.7, ymax=100/100=1.0
    assertEquals(0, new BigDecimal('0.7').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, BigDecimal.ONE.compareTo(result[1].ymax as BigDecimal))
  }

  @Test
  void testFillEmptyData() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = []
    List<LayerData> result = FillPosition.compute(layer, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testFillSingleItem() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 42, rowIndex: 0)
    ]
    List<LayerData> result = FillPosition.compute(layer, data)
    assertEquals(1, result.size())

    // Single item: normalized to fill [0, 1]
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, BigDecimal.ONE.compareTo(result[0].ymax as BigDecimal))
    assertEquals(0, new BigDecimal('0.5').compareTo(result[0].y as BigDecimal))
  }

  private static LayerSpec makeLayer(Map<String, Object> params = [:]) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        PositionSpec.of(CharmPositionType.FILL),
        params
    )
  }
}
