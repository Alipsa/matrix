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

    // For closed-right, value 2.5 is included in the bin ending at 2.5 (right boundary included).
    // The first bin uses [a,b] notation (both closed) as a special case to include the minimum.
    assertEquals('[1.5,2.5]', labels[1], '2.5 should be in the first bin [1.5,2.5] (both brackets closed for first bin)')
    // Subsequent bins use (a,b] notation (open left, closed right)
    assertEquals('(2.5,3.5]', labels[2], '3.5 should be in bin (2.5,3.5] (standard closed-right notation)')
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
