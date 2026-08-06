package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

/** Covers ARFF relation names, numeric/string/nominal values, missing values, and malformed input. */
@Tag('arff')
class ArffApiIT implements ApiItSupport {

  @Test
  void arffRoundTripAndValidation() {
    Matrix data = Matrix.builder([id: [1, 2], category: ['a,b', null], score: [1.5G, 2.5G]],
        [Integer, String, BigDecimal], 'arff_relation').build()
    File file = tempFile('.arff').toFile()
    MatrixArffWriter.write(data, file)
    def restored = MatrixArffReader.read(file)
    assertEquals(data.columnNames(), restored.columnNames())
    MatrixAssertions.assertContentMatches(data, restored, data.diff(restored))
    assertThrows(IllegalArgumentException) {
      MatrixArffReader.read((InputStream) null)
    }
  }
}
