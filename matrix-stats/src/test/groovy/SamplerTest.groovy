import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.stats.Sampler

import static org.junit.jupiter.api.Assertions.*

class SamplerTest {

  @Test
  void testSampleMatrix() {
    def (train, test) = Sampler.split(Dataset.cars(), 0.5)
    assertEquals(25, train.rowCount(), 'train size')
    assertEquals(25, train.rowCount(), 'test size')

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
}
