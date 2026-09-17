package dimred

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import smile.feature.extraction.PCA

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.dimred.Pca

class PcaTest {

  private static final double TOLERANCE = 1e-8

  /**
   * Deterministic data with two correlated factors and one independent column,
   * so PC1 and PC2 should dominate.
   */
  private static Matrix correlatedData() {
    Random random = new Random(42)
    List<List<Object>> rows = (1..60).collect {
      double factor = random.nextGaussian()
      List<Object> row = [
        factor + random.nextGaussian() * 0.1,
        2 * factor + random.nextGaussian() * 0.1,
        -factor + random.nextGaussian() * 0.1,
        random.nextGaussian(),
      ]
      row
    }
    Matrix.builder()
        .columnNames(['a', 'b', 'c', 'noise'])
        .rows(rows)
        .types([Double, Double, Double, Double])
        .build()
  }

  private static double[][] asDoubleArray(Matrix matrix) {
    double[][] values = new double[matrix.rowCount()][matrix.columnCount()]
    matrix.eachWithIndex { List<Object> row, int r ->
      values[r] = row.collect { Object value -> (value as Number).doubleValue() } as double[]
    }
    values
  }

  @Test
  void testProjectReturnsNamedComponents() {
    Pca pca = Pca.fit(correlatedData())
    Matrix projected = pca.project(2)

    assertEquals(60, projected.rowCount())
    assertEquals(['PC1', 'PC2'], projected.columnNames())
    assertEquals(BigDecimal, projected.type('PC1'))
  }

  @Test
  void testExplainedVarianceMatchesSmile() {
    Matrix data = correlatedData()
    Pca pca = Pca.fit(data)
    double[] smileVariance = PCA.fit(asDoubleArray(data)).varianceProportion()

    pca.explainedVariance().eachWithIndex { BigDecimal actual, int i ->
      assertEquals(smileVariance[i], actual.doubleValue(), TOLERANCE, "Component $i")
      assertEquals(actual, pca.explainedVariance(i))
    }
    // ratios are ordered and sum to 1
    assertTrue(pca.explainedVariance()[0] >= pca.explainedVariance()[1])
    assertEquals(1.0d, pca.explainedVariance().sum { it.doubleValue() }, TOLERANCE)
    assertEquals(1.0d, pca.cumulativeExplainedVariance().last().doubleValue(), TOLERANCE)
  }

  @Test
  void testScoresMatchSmileUpToSign() {
    Matrix data = correlatedData()
    Pca pca = Pca.fit(data)
    double[][] smileProjected = PCA.fit(asDoubleArray(data)).getProjection(2).apply(asDoubleArray(data))

    [0, 1].each { int component ->
      List<BigDecimal> scores = pca.scores(component)
      double sign = scores[0].doubleValue() * smileProjected[0][component] < 0 ? -1 : 1
      scores.eachWithIndex { BigDecimal score, int row ->
        assertEquals(smileProjected[row][component], sign * score.doubleValue(), 1e-6, "Row $row")
      }
    }
  }

  @Test
  void testCenteringMakesScoresZeroMean() {
    Pca pca = Pca.fit(correlatedData())
    [0, 1, 2].each { int component ->
      double mean = pca.scores(component).sum { it.doubleValue() } / pca.scores(component).size()
      assertEquals(0.0d, mean, 1e-8)
    }
  }

  @Test
  void testUncenteredScoresAgreeWithProjection() {
    Pca pca = Pca.fit(correlatedData(), false, false)
    Matrix projected = pca.project(2)

    [0, 1].each { int component ->
      pca.scores(component).eachWithIndex { BigDecimal score, int row ->
        assertEquals(score.doubleValue(), (projected.get(row, component) as BigDecimal).doubleValue(), TOLERANCE)
      }
    }
    assertTrue(pca.scores(0).sum().abs() > 0.1)
  }

  @Test
  void testScalingGivesUnitColumnVariance() {
    Random random = new Random(7)
    Matrix data = Matrix.builder()
        .columnNames(['small', 'large'])
        .rows((1..50).collect { [random.nextGaussian(), random.nextGaussian() * 10000] })
        .types([Double, Double])
        .build()

    Pca unscaled = Pca.fit(data)
    assertTrue(unscaled.explainedVariance(0).doubleValue() > 0.99,
        'Without scaling the large column should dominate PC1')

    Pca scaled = Pca.fit(data, true, true)
    assertEquals(0.5d, scaled.explainedVariance(0).doubleValue(), 0.1,
        'With scaling both columns contribute equally')
  }

  @Test
  void testProjectNewDataMatchesRefitScores() {
    Matrix data = correlatedData()
    Pca pca = Pca.fit(data)
    Matrix projectedTraining = pca.project(1)

    Matrix subset = data.subset(0..<30)
    Matrix projectedSubset = pca.project(1, subset)

    assertEquals(30, projectedSubset.rowCount())
    projectedSubset['PC1'].eachWithIndex { Object value, int row ->
      assertEquals((projectedTraining.get(row, 0) as BigDecimal).doubleValue(),
          (value as BigDecimal).doubleValue(), 1e-8)
    }
  }

  @Test
  void testLoadingsAreUnitVectorsAndOrthogonal() {
    Pca pca = Pca.fit(correlatedData())
    Matrix loadings = pca.loadings()

    assertEquals(['Feature', 'PC1', 'PC2', 'PC3', 'PC4'], loadings.columnNames())
    assertEquals(4, loadings.rowCount())

    double pc1Norm = Math.sqrt(loadings['PC1'].sum { Object v -> (v as BigDecimal).doubleValue() ** 2 })
    assertEquals(1.0d, pc1Norm, TOLERANCE)

    double dot = 0
    loadings['PC1'].eachWithIndex { Object v1, int i ->
      dot += (v1 as BigDecimal).doubleValue() * (loadings['PC2'][i] as BigDecimal).doubleValue()
    }
    assertEquals(0.0d, dot, TOLERANCE)
  }

  @Test
  void testColumnSubsetSelection() {
    Matrix data = correlatedData()
    Pca pca = Pca.fit(data, ['a', 'b', 'c'])
    assertEquals(3, pca.componentCount())
    assertEquals(['a', 'b', 'c'], pca.loadings()['Feature'])
  }

  @Test
  void testDegenerateDataHasZeroExplainedVariance() {
    Matrix constant = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1.0, 2.0], [1.0, 2.0]])
        .types([Double, Double])
        .build()

    Pca pca = Pca.fit(constant)

    assertEquals([BigDecimal.ZERO, BigDecimal.ZERO], pca.explainedVariance())
    assertEquals(BigDecimal.ZERO, pca.explainedVariance(0))
    assertEquals([BigDecimal.ZERO, BigDecimal.ZERO], pca.cumulativeExplainedVariance())
  }

  @Test
  void testRejectsNullAndNonNumericValuesWithCoordinates() {
    Matrix nullValue = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1.0, 2.0], [null, 3.0]])
        .types([Double, Double])
        .build()
    Matrix textValue = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1.0, 2.0], [3.0, 'not a number']])
        .types([Double, String])
        .build()

    IllegalArgumentException nullException = assertThrows(IllegalArgumentException) { Pca.fit(nullValue) }
    IllegalArgumentException textException = assertThrows(IllegalArgumentException) { Pca.fit(textValue) }

    assertEquals("Column 'x' contains a non-numeric value at row 1", nullException.message)
    assertEquals("Column 'y' contains a non-numeric value at row 1", textException.message)
  }

  @Test
  void testRejectsDuplicateAndEmptyColumnSelections() {
    Matrix data = correlatedData()

    IllegalArgumentException duplicates = assertThrows(IllegalArgumentException) { Pca.fit(data, ['a', 'a']) }
    assertEquals('Column selection contains duplicate columns: a', duplicates.message)
    assertThrows(IllegalArgumentException) { Pca.fit(data, []) }
  }

  @Test
  void testProjectNewDataValidatesCellValues() {
    Pca pca = Pca.fit(correlatedData())
    Matrix nullValue = correlatedData()
    nullValue.putAt(1, 'a', null)
    Matrix textValue = correlatedData()
    textValue.putAt(1, 'b', 'abc')

    IllegalArgumentException nullException = assertThrows(IllegalArgumentException) { pca.project(1, nullValue) }
    IllegalArgumentException textException = assertThrows(IllegalArgumentException) { pca.project(1, textValue) }

    assertEquals("Column 'a' contains a non-numeric value at row 1", nullException.message)
    assertEquals("Column 'b' contains a non-numeric value at row 1", textException.message)
    assertThrows(IllegalArgumentException) { pca.project(1, null) }
  }

  @Test
  void testValidation() {
    Matrix data = correlatedData()
    assertThrows(IllegalArgumentException) { Pca.fit(null) }
    assertThrows(IllegalArgumentException) { Pca.fit(data, ['missing']) }

    Pca pca = Pca.fit(data)
    assertThrows(IndexOutOfBoundsException) { pca.scores(-1) }
    assertThrows(IndexOutOfBoundsException) { pca.scores(4) }
    assertThrows(IndexOutOfBoundsException) { pca.project(0) }
    assertThrows(IndexOutOfBoundsException) { pca.project(5) }
    assertThrows(IllegalArgumentException) { pca.project(1, data.clone().drop('a')) }

    Matrix dataWithExtraColumn = data.clone().addColumn('extra', Double, [1.0d] * data.rowCount())
    assertEquals(data.rowCount(), pca.project(1, dataWithExtraColumn).rowCount())

    Matrix constant = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1.0, 2.0], [1.0, 3.0]])
        .types([Double, Double])
        .build()
    assertThrows(IllegalArgumentException) { Pca.fit(constant, true, true) }
  }
}
