package gg

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.ColumnRef

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.cols
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.ggplot

class ColumnRefTest {

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames(['x', 'y', 'group'])
        .rows([
            [1, 2, 'a'],
            [2, 4, 'b'],
            [3, 6, 'a']
        ])
        .build()
  }

  @Test
  void testValidColumnReferenceReturnsColumnName() {
    Matrix data = sampleData()
    ColumnRef c = cols(data)

    assertEquals('x', c.x)
    assertEquals('y', c.y)
    assertEquals('group', c.group)
  }

  @Test
  void testInvalidColumnReferenceThrowsHelpfulMessage() {
    Matrix data = sampleData()
    ColumnRef c = cols(data)

    IllegalArgumentException e = assertThrows(IllegalArgumentException) {
      c.nonexistent
    }
    assertTrue(e.message.contains("Column 'nonexistent' not found in matrix"))
    assertTrue(e.message.contains('x'))
    assertTrue(e.message.contains('y'))
    assertTrue(e.message.contains('group'))
  }

  @Test
  void testColsWorksInAesIntegration() {
    Matrix data = sampleData()
    ColumnRef c = cols(data)

    Svg svg = (ggplot(data, aes(x: c.x, y: c.y)) + geom_point()).render()

    assertNotNull(svg)
    String xml = SvgWriter.toXml(svg)
    assertTrue(xml.contains('<svg'))
    assertTrue(xml.contains('<circle'))
  }

  @Test
  void testReservedPropertyNameClassResolvesAsColumn() {
    Matrix data = Matrix.builder()
        .columnNames(['class', 'y'])
        .rows([
            ['a', 1],
            ['b', 2]
        ])
        .build()
    ColumnRef c = cols(data)

    assertEquals('class', c.class)
    assertEquals('y', c.y)
  }

  @Test
  void testColsDoesNotMutateMatrixColumnOrder() {
    Matrix data = Matrix.builder()
        .columnNames(['z', 'a', 'm'])
        .rows([
            [1, 2, 3]
        ])
        .build()

    cols(data)

    assertEquals(['z', 'a', 'm'], data.columnNames())
  }

  @Test
  void testColsWithNullMatrixThrows() {
    assertThrows(IllegalArgumentException) {
      cols(null)
    }
  }
}
