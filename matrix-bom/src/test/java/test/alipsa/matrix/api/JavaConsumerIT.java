package test.alipsa.matrix.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import se.alipsa.matrix.charm.Charts;
import se.alipsa.matrix.charm.PlotSpec;
import se.alipsa.matrix.core.Matrix;
import se.alipsa.matrix.core.MatrixBuilder;
import se.alipsa.matrix.core.util.CollectionUtils;
import se.alipsa.matrix.core.util.Columns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Compiles the documented Java consumer shapes against the resolved BOM. */
class JavaConsumerIT {

  @Test
  void javaMapApiRemainsSourceCompatible() {
    Columns columns = new Columns()
        .add("x", Arrays.asList(1, 2, 3))
        .add("y", Arrays.asList(4, 5, 6));
    Map<String, List> convenience = CollectionUtils.m("z", Arrays.asList(7, 8, 9));
    MatrixBuilder builder = Matrix.builder(columns, Arrays.asList(Integer.class, Integer.class), "java");
    Matrix matrix = builder.data(columns).build();
    Matrix withZ = matrix.and(convenience);
    assertEquals(3, withZ.rowCount());
    assertEquals(3, withZ.columnCount());

    Matrix typed = Matrix.builder(convenience, Arrays.asList(Integer.class), "typed").build();
    assertEquals(3, typed.rowCount());

    PlotSpec plotted = Charts.plot(withZ);
    assertNotNull(plotted);
    assertNotNull(Charts.chart(withZ));
  }
}
