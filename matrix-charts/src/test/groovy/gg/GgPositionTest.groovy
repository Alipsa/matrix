package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.position.GgPosition

import static org.junit.jupiter.api.Assertions.*

class GgPositionTest {

  private static Matrix sampleMatrix() {
    return Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 2, 'A'],
            [1, 3, 'B'],
            [2, 4, 'A'],
            [2, 5, 'B']
        ])
        .types(Integer, Integer, String)
        .build()
  }

  @Test
  void testIdentityReturnsSameInstance() {
    Matrix data = sampleMatrix()
    Aes aes = new Aes(x: 'x', y: 'y', group: 'group')

    Matrix result = GgPosition.identity(data, aes)

    assertSame(data, result, 'Identity should return the input matrix instance')
  }

  @Test
  void testDodgeAdjustsXByGroup() {
    Matrix data = sampleMatrix()
    Aes aes = new Aes(x: 'x', y: 'y', group: 'group')

    Matrix result = GgPosition.dodge(data, aes, [width: 0.9])
    List<Number> xValues = result['x'] as List<Number>

    assertEquals(0.775d, xValues[0] as double, 1e-9)
    assertEquals(1.225d, xValues[1] as double, 1e-9)
    assertEquals(1.775d, xValues[2] as double, 1e-9)
    assertEquals(2.225d, xValues[3] as double, 1e-9)
  }

  @Test
  void testStackAddsYMinMaxAndCenters() {
    Matrix data = sampleMatrix()
    Aes aes = new Aes(x: 'x', y: 'y', group: 'group')

    Matrix result = GgPosition.stack(data, aes)

    Map<String, Map> byKey = [:]
    result.rows().each { row ->
      Map m = row.toMap() as Map
      byKey["${m.x}-${m.group}"] = m
    }

    assertEquals(0.0d, (byKey['1-A'].ymin as double), 1e-9)
    assertEquals(2.0d, (byKey['1-A'].ymax as double), 1e-9)
    assertEquals(1.0d, (byKey['1-A'].y as double), 1e-9)

    assertEquals(2.0d, (byKey['1-B'].ymin as double), 1e-9)
    assertEquals(5.0d, (byKey['1-B'].ymax as double), 1e-9)
    assertEquals(3.5d, (byKey['1-B'].y as double), 1e-9)

    assertEquals(0.0d, (byKey['2-A'].ymin as double), 1e-9)
    assertEquals(4.0d, (byKey['2-A'].ymax as double), 1e-9)
    assertEquals(2.0d, (byKey['2-A'].y as double), 1e-9)

    assertEquals(4.0d, (byKey['2-B'].ymin as double), 1e-9)
    assertEquals(9.0d, (byKey['2-B'].ymax as double), 1e-9)
    assertEquals(6.5d, (byKey['2-B'].y as double), 1e-9)
  }

  @Test
  void testStackReverseOrder() {
    Matrix data = sampleMatrix()
    Aes aes = new Aes(x: 'x', y: 'y', group: 'group')

    Matrix result = GgPosition.stack(data, aes, [reverse: true])

    Map<String, Map> byKey = [:]
    result.rows().each { row ->
      Map m = row.toMap() as Map
      byKey["${m.x}-${m.group}"] = m
    }

    assertEquals(0.0d, (byKey['1-B'].ymin as double), 1e-9)
    assertEquals(3.0d, (byKey['1-B'].ymax as double), 1e-9)
    assertEquals(1.5d, (byKey['1-B'].y as double), 1e-9)

    assertEquals(3.0d, (byKey['1-A'].ymin as double), 1e-9)
    assertEquals(5.0d, (byKey['1-A'].ymax as double), 1e-9)
    assertEquals(4.0d, (byKey['1-A'].y as double), 1e-9)
  }

  @Test
  void testFillNormalizesStacks() {
    Matrix data = sampleMatrix()
    Aes aes = new Aes(x: 'x', y: 'y', group: 'group')

    Matrix result = GgPosition.fill(data, aes)

    Map<String, Map> byKey = [:]
    result.rows().each { row ->
      Map m = row.toMap() as Map
      byKey["${m.x}-${m.group}"] = m
    }

    assertEquals(0.0d, (byKey['1-A'].ymin as double), 1e-9)
    assertEquals(0.4d, (byKey['1-A'].ymax as double), 1e-9)
    assertEquals(0.2d, (byKey['1-A'].y as double), 1e-9)

    assertEquals(0.4d, (byKey['1-B'].ymin as double), 1e-9)
    assertEquals(1.0d, (byKey['1-B'].ymax as double), 1e-9)
    assertEquals(0.7d, (byKey['1-B'].y as double), 1e-9)
  }

  @Test
  void testJitterUsesSeededRandom() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20]
        ])
        .types(Integer, Integer)
        .build()

    Aes aes = new Aes(x: 'x', y: 'y')
    Matrix result = GgPosition.jitter(data, aes, [width: 0.4, height: 0.2, seed: 42L])

    Random random = new Random(42L)
    List<List<Double>> expected = []
    data.each { row ->
      double jitterX = (random.nextDouble() - 0.5d) * 0.4d
      double jitterY = (random.nextDouble() - 0.5d) * 0.2d
      expected << [(row['x'] as double) + jitterX, (row['y'] as double) + jitterY]
    }

    List<Number> xValues = result['x'] as List<Number>
    List<Number> yValues = result['y'] as List<Number>

    assertEquals(expected[0][0], xValues[0] as double, 1e-9)
    assertEquals(expected[0][1], yValues[0] as double, 1e-9)
    assertEquals(expected[1][0], xValues[1] as double, 1e-9)
    assertEquals(expected[1][1], yValues[1] as double, 1e-9)
  }
}
