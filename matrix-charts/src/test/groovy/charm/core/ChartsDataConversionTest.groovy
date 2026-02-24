package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotSpec

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.charm.Charts.chart
import static se.alipsa.matrix.charm.Charts.plot

class ChartsDataConversionTest {

  @Test
  void testPlotAcceptsListMapInput() {
    List<Map> rows = [
        [x: 1, y: 2],
        [x: 3, y: 4]
    ]

    PlotSpec spec = plot(rows)

    assertEquals(['x', 'y'], spec.data.columnNames())
    assertEquals([1, 3], spec.data['x'])
    assertEquals([2, 4], spec.data['y'])
  }

  @Test
  void testPlotAcceptsColumnMapInput() {
    Map<String, List> columns = [
        x: [1, 2, 3],
        y: [4, 5, 6]
    ]

    PlotSpec spec = plot(columns)

    assertEquals(['x', 'y'], spec.data.columnNames())
    assertEquals([1, 2, 3], spec.data['x'])
    assertEquals([4, 5, 6], spec.data['y'])
  }

  @Test
  void testPlotAcceptsIterablePojoInput() {
    Iterable<SampleObs> observations = [
        new SampleObs(group: 'A', value: 10),
        new SampleObs(group: 'B', value: 20)
    ]

    PlotSpec spec = plot(observations)

    assertEquals(['group', 'value'], spec.data.columnNames())
    assertEquals(['A', 'B'], spec.data['group'])
    assertEquals([10, 20], spec.data['value'])
  }

  @Test
  void testConvenienceInputSupportsConfigureClosure() {
    Chart chart = plot([[x: 1, y: 2], [x: 2, y: 3]]) {
      mapping {
        x = col.x
        y = col.y
      }
      points {
        size = 2
      }
    }.build()

    assertEquals(1, chart.layers.size())
    assertEquals('x', chart.mapping.x.columnName())
    assertEquals('y', chart.mapping.y.columnName())
  }

  @Test
  void testChartAliasesAcceptConvenienceInput() {
    PlotSpec spec = chart([x: [1, 2], y: [3, 4]])
    assertEquals(['x', 'y'], spec.data.columnNames())
  }

  @Test
  void testConvenienceInputRejectsEmptyCollections() {
    assertThrows(IllegalArgumentException.class) {
      plot([] as List<Map>)
    }
    assertThrows(IllegalArgumentException.class) {
      plot([:] as Map<String, List>)
    }
    assertThrows(IllegalArgumentException.class) {
      plot([] as Iterable)
    }
  }

  static class SampleObs {
    String group
    Integer value
  }
}
