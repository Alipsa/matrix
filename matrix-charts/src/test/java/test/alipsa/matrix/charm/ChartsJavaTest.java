package test.alipsa.matrix.charm;

import groovy.lang.Closure;
import org.junit.jupiter.api.Test;
import se.alipsa.matrix.charm.Charts;
import se.alipsa.matrix.charm.PlotSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartsJavaTest {

  @Test
  @SuppressWarnings("rawtypes")
  void testJavaCanPassMapOfRawListsToCharts() {
    Map<String, List> columns = new LinkedHashMap<>();
    columns.put("x", List.of(1, 2));
    columns.put("y", List.of(3, 4));

    PlotSpec plot = Charts.plot(columns);
    PlotSpec chart = Charts.chart(columns);
    Closure<Object> configure = new Closure<Object>(null) {
      public Object doCall() {
        return null;
      }
    };
    PlotSpec configuredPlot = Charts.plot(columns, configure);
    PlotSpec configuredChart = Charts.chart(columns, configure);

    assertEquals(List.of("x", "y"), plot.getData().columnNames());
    assertEquals(List.of("x", "y"), chart.getData().columnNames());
    assertEquals(List.of("x", "y"), configuredPlot.getData().columnNames());
    assertEquals(List.of("x", "y"), configuredChart.getData().columnNames());
  }
}
