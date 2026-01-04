package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.CutWidth

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * Tests for CutWidth bin boundary handling.
 */
class CutWidthTest {

  /**
   * Values equal to the bin start should fall into the previous bin when closed right.
   */
  @Test
  void testClosedRightAssignsBoundaryToPreviousBin() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1.5],
            [2.5],
            [3.5]
        ])
        .types(BigDecimal)
        .build()

    def cutWidth = new CutWidth('x', 1)
    String colName = cutWidth.addToMatrix(data)
    def labels = data[colName] as List<String>

    assertEquals('[1.5,2.5]', labels[1], '2.5 should be in the lower bin')
    assertEquals('(2.5,3.5]', labels[2], '3.5 should be in the lower bin')
  }

  /**
   * Values equal to the bin start are included in that bin when closed left.
   */
  @Test
  void testClosedLeftAssignsBoundaryToNextBin() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1.5],
            [2.5],
            [3.5]
        ])
        .types(BigDecimal)
        .build()

    def cutWidth = new CutWidth('x', 1, null, null, false)
    String colName = cutWidth.addToMatrix(data)
    def labels = data[colName] as List<String>

    assertEquals('[2.5,3.5)', labels[1], '2.5 should be in the upper bin')
    assertEquals('[3.5,4.5)', labels[2], '3.5 should be in the upper bin')
  }
}
