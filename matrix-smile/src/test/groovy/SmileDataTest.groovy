import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.data.SmileData

import static org.junit.jupiter.api.Assertions.*

class SmileDataTest {

  private Matrix createTestMatrix() {
    return Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0],
            y: [10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
            label: ['A', 'A', 'A', 'A', 'A', 'B', 'B', 'B', 'B', 'B']
        )
        .types([Double, Double, String])
        .build()
  }

  // ============ trainTestSplit Tests ============

  @Test
  void testTrainTestSplitDefaultRatio() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.trainTestSplit(data)

    assertEquals(8, train.rowCount())
    assertEquals(2, test.rowCount())
    assertEquals(10, train.rowCount() + test.rowCount())
  }

  @Test
  void testTrainTestSplitCustomRatio() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.trainTestSplit(data, 0.3, false)

    assertEquals(7, train.rowCount())
    assertEquals(3, test.rowCount())
  }

  @Test
  void testTrainTestSplitNoShuffle() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.trainTestSplit(data, 0.2, false)

    // Without shuffle, first 8 rows should be train, last 2 should be test
    assertEquals(8, train.rowCount())
    assertEquals(2, test.rowCount())

    // First row of train should be row 0
    assertEquals(1.0d, train.row(0)[0])
    // First row of test should be row 8
    assertEquals(9.0d, test.row(0)[0])
  }

  @Test
  void testTrainTestSplitWithSeed() {
    Matrix data = createTestMatrix()

    def (train1, test1) = SmileData.trainTestSplit(data, 0.2, true, 42L)
    def (train2, test2) = SmileData.trainTestSplit(data, 0.2, true, 42L)

    // Same seed should produce same split
    assertEquals(train1.rowCount(), train2.rowCount())
    for (int i = 0; i < train1.rowCount(); i++) {
      assertEquals(new ArrayList(train1.row(i)), new ArrayList(train2.row(i)))
    }
  }

  @Test
  void testTrainTestSplitWithNamedParams() {
    Matrix data = createTestMatrix()

    def result = SmileData.trainTestSplit(testRatio: 0.3, shuffle: true, seed: 42L, data)

    assertEquals(7, result[0].rowCount())
    assertEquals(3, result[1].rowCount())
  }

  @Test
  void testTrainTestSplitPreservesColumnNames() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.trainTestSplit(data)

    assertEquals(['x', 'y', 'label'], train.columnNames())
    assertEquals(['x', 'y', 'label'], test.columnNames())
  }

  @Test
  void testTrainTestSplitPreservesTypes() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.trainTestSplit(data)

    assertEquals([Double, Double, String], train.types())
    assertEquals([Double, Double, String], test.types())
  }

  @Test
  void testTrainTestSplitInvalidRatio() {
    Matrix data = createTestMatrix()

    assertThrows(IllegalArgumentException) {
      SmileData.trainTestSplit(data, 0.0)
    }
    assertThrows(IllegalArgumentException) {
      SmileData.trainTestSplit(data, 1.0)
    }
    assertThrows(IllegalArgumentException) {
      SmileData.trainTestSplit(data, -0.5)
    }
  }

  @Test
  void testTrainTestSplitTooFewRows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0])
        .types([Double])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileData.trainTestSplit(data)
    }
  }

  // ============ kFold Tests ============

  @Test
  void testKFoldBasic() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(data, 5)

    assertEquals(5, folds.size())
    for (SmileData.Fold fold : folds) {
      assertEquals(8, fold.train.rowCount())
      assertEquals(2, fold.validation.rowCount())
    }
  }

  @Test
  void testKFoldIndices() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(data, 5)

    // Each fold should have unique index
    Set<Integer> indices = folds.collect { it.index }.toSet()
    assertEquals([0, 1, 2, 3, 4] as Set, indices)
  }

  @Test
  void testKFoldNoShuffle() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(data, 5, false)

    assertEquals(5, folds.size())
    // First fold should have first 2 rows as validation
    SmileData.Fold firstFold = folds[0]
    assertEquals(2, firstFold.validation.rowCount())
    assertEquals(1.0d, firstFold.validation.row(0)[0])
    assertEquals(2.0d, firstFold.validation.row(1)[0])
  }

  @Test
  void testKFoldWithSeed() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds1 = SmileData.kFold(data, 5, true, 42L)
    List<SmileData.Fold> folds2 = SmileData.kFold(data, 5, true, 42L)

    // Same seed should produce same folds
    for (int i = 0; i < 5; i++) {
      assertEquals(folds1[i].train.rowCount(), folds2[i].train.rowCount())
      for (int j = 0; j < folds1[i].train.rowCount(); j++) {
        assertEquals(new ArrayList(folds1[i].train.row(j)), new ArrayList(folds2[i].train.row(j)))
      }
    }
  }

  @Test
  void testKFoldWithNamedParams() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(k: 3, shuffle: true, seed: 42L, data)

    assertEquals(3, folds.size())
  }

  @Test
  void testKFoldInvalidK() {
    Matrix data = createTestMatrix()

    assertThrows(IllegalArgumentException) {
      SmileData.kFold(data, 1)
    }
    assertThrows(IllegalArgumentException) {
      SmileData.kFold(data, 0)
    }
  }

  @Test
  void testKFoldTooFewRows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0])
        .types([Double])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileData.kFold(data, 5)
    }
  }

  @Test
  void testKFoldCoverageComplete() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(data, 5, false)

    // Each row should appear exactly once across all validation sets
    Set<Double> allValidationX = []
    for (SmileData.Fold fold : folds) {
      for (int i = 0; i < fold.validation.rowCount(); i++) {
        allValidationX << fold.validation.row(i)[0]
      }
    }

    assertEquals(10, allValidationX.size())
  }

  @Test
  void testFoldToString() {
    Matrix data = createTestMatrix()

    List<SmileData.Fold> folds = SmileData.kFold(data, 5)
    SmileData.Fold fold = folds[0]

    String str = fold.toString()
    assertTrue(str.contains('index=0'))
    assertTrue(str.contains('trainSize=8'))
    assertTrue(str.contains('validationSize=2'))
  }

  // ============ stratifiedSplit Tests ============

  @Test
  void testStratifiedSplit() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.stratifiedSplit(data, 'label', 0.2)

    // Both classes should be represented in both sets
    List<String> trainLabels = train.column('label')
    List<String> testLabels = test.column('label')

    assertTrue(trainLabels.contains('A'))
    assertTrue(trainLabels.contains('B'))
    assertTrue(testLabels.contains('A'))
    assertTrue(testLabels.contains('B'))
  }

  @Test
  void testStratifiedSplitProportional() {
    Matrix data = createTestMatrix()

    def (train, test) = SmileData.stratifiedSplit(data, 'label', 0.2)

    // Count A and B in train
    int trainA = train.column('label').count { it == 'A' }
    int trainB = train.column('label').count { it == 'B' }
    int testA = test.column('label').count { it == 'A' }
    int testB = test.column('label').count { it == 'B' }

    // Original has 5 A and 5 B, so roughly proportional split expected
    assertEquals(5, trainA + testA)
    assertEquals(5, trainB + testB)
  }

  @Test
  void testStratifiedSplitWithSeed() {
    Matrix data = createTestMatrix()

    def (train1, test1) = SmileData.stratifiedSplit(data, 'label', 0.2, 42L)
    def (train2, test2) = SmileData.stratifiedSplit(data, 'label', 0.2, 42L)

    assertEquals(train1.rowCount(), train2.rowCount())
    assertEquals(test1.rowCount(), test2.rowCount())
  }

  // ============ bootstrap Tests ============

  @Test
  void testBootstrapBasic() {
    Matrix data = createTestMatrix()

    List<Matrix> samples = SmileData.bootstrap(data, 3)

    assertEquals(3, samples.size())
    for (Matrix sample : samples) {
      assertEquals(10, sample.rowCount()) // Same size as original by default
      assertEquals(3, sample.columnCount())
    }
  }

  @Test
  void testBootstrapCustomSize() {
    Matrix data = createTestMatrix()

    List<Matrix> samples = SmileData.bootstrap(data, 5, 5)

    assertEquals(5, samples.size())
    for (Matrix sample : samples) {
      assertEquals(5, sample.rowCount())
    }
  }

  @Test
  void testBootstrapWithSeed() {
    Matrix data = createTestMatrix()

    List<Matrix> samples1 = SmileData.bootstrap(data, 3, 10, 42L)
    List<Matrix> samples2 = SmileData.bootstrap(data, 3, 10, 42L)

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < samples1[i].rowCount(); j++) {
        assertEquals(new ArrayList(samples1[i].row(j)), new ArrayList(samples2[i].row(j)))
      }
    }
  }

  @Test
  void testBootstrapInvalidN() {
    Matrix data = createTestMatrix()

    assertThrows(IllegalArgumentException) {
      SmileData.bootstrap(data, 0)
    }
  }

  @Test
  void testBootstrapAllowsDuplicates() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0])
        .types([Double])
        .build()

    // With only 2 rows but 10 sample size, there must be duplicates
    List<Matrix> samples = SmileData.bootstrap(data, 1, 10, 42L)
    Matrix sample = samples[0]

    assertEquals(10, sample.rowCount())
    // Values should only be 1.0 or 2.0
    for (int i = 0; i < sample.rowCount(); i++) {
      double val = sample.row(i)[0] as double
      assertTrue(val == 1.0d || val == 2.0d)
    }
  }
}
