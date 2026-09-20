package charm.render

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil

class LayerDataUtilTest {

  private static LayerSpec layerOf(CharmGeomType type) {
    new LayerSpec(GeomSpec.of(type))
  }

  @Test
  void trainingValuesIncludeGeometryAndStatBounds() {
    LayerData datum = new LayerData(x: 2, xend: 9, xmin: 0, xmax: 12, y: 5, ymin: 1, ymax: 10, rowIndex: 0)
    datum.meta.binStart = -1
    datum.meta.binEnd = 13
    datum.meta.whiskerLow = -2
    datum.meta.whiskerHigh = 15
    datum.meta.outliers = [-3, 20]
    assertTrue(LayerDataUtil.xTrainingValues(layerOf(CharmGeomType.HISTOGRAM), [datum]).containsAll([2, 9, 0, 12, -1, 13]))
    assertTrue(LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.BOXPLOT), [datum]).containsAll([5, 1, 10, -2, 15, -3, 20]))
  }

  @Test
  void barGeomsContributeZeroOnlyWhenTheyHaveData() {
    List<LayerData> data = [new LayerData(x: 'A', y: 3, rowIndex: 0)]
    assertTrue(LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.COL), data).contains(BigDecimal.ZERO))
    assertFalse(LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.ERRORBAR), data).contains(BigDecimal.ZERO))
    assertEquals([], LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.COL), []))
  }

  @Test
  void flippedBarGeomsContributeZeroToTheFlippedValueAxis() {
    List<LayerData> flippedData = [new LayerData(x: 3, y: 'A', rowIndex: 0)]

    assertTrue(LayerDataUtil.xTrainingValues(layerOf(CharmGeomType.COL), flippedData, true).contains(BigDecimal.ZERO))
    assertFalse(LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.COL), flippedData, false).contains(BigDecimal.ZERO))
  }

  @Test
  void flippedBoxplotTrainsWhiskersAndOutliersOnTheHorizontalAxis() {
    LayerData datum = new LayerData(x: 25, y: 'A', rowIndex: 0)
    datum.meta.whiskerLow = 10
    datum.meta.whiskerHigh = 40
    datum.meta.outliers = [5, 50]

    List<Object> values = LayerDataUtil.xTrainingValues(layerOf(CharmGeomType.BOXPLOT), [datum], false, true)

    assertTrue(values.containsAll([25, 10, 40, 5, 50]))
  }

  @Test
  void flippedHistogramTrainsBinEdgesOnTheVerticalAxis() {
    LayerData datum = new LayerData(x: 3, y: 5, rowIndex: 0)
    datum.meta.binStart = 1
    datum.meta.binEnd = 9

    List<Object> values = LayerDataUtil.yTrainingValues(layerOf(CharmGeomType.HISTOGRAM), [datum], false, true)

    assertTrue(values.containsAll([5, 1, 9]))
  }
}
