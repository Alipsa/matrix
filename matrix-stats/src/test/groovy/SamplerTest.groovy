import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Sampler

class SamplerTest {

  @Test
  void testSampleMatrix() {
    def (train, test) = Sampler.split(Dataset.cars(), 0.5)
    assertEquals(25, train.rowCount(), 'train size')
    assertEquals(25, test.rowCount(), 'test size')

    Matrix ids = Matrix.builder()
        .matrixName('ids')
        .data(
            id: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
            letter: ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J']
        )
        .types(Integer, String)
        .build()
    (train, test) = Sampler.split(ids, 0.1)
    assertEquals(1, train.rowCount())
    assertEquals(9, test.rowCount())
    assertIterableEquals(ids.columnNames(), train.columnNames())
    assertIterableEquals(ids.columnNames(), test.columnNames())
  }

  @Test
  void testRejectZeroRatio() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Sampler.split(Dataset.cars(), 0.0)
    }

    assertEquals('Ratio must be greater than 0 and at most 1', exception.message)
  }

  @Test
  void testRejectRatioThatProducesEmptyTrainingSet() {
    Matrix tiny = Matrix.builder()
        .matrixName('tiny')
        .data(id: [1, 2, 3, 4, 5])
        .types(Integer)
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Sampler.split(tiny, 0.1)
    }

    assertEquals('Ratio 0.1 produces an empty training set for 5 rows', exception.message)
  }
}
