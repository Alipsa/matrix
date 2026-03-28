package se.alipsa.matrix.stats

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

/**
 * Utility class for sampling and splitting matrices.
 *
 * <p>This class provides methods for random train/test splitting of matrices,
 * which is useful for machine learning workflows. This is distinct from
 * Matrix.split() and Matrix.splitInto() which provide deterministic chunking.</p>
 */
@CompileStatic
class Sampler {

  /**
   * Randomly splits a matrix into train and test sets.
   *
   * <p>This method shuffles the rows and splits them according to the specified ratio.
   * This is useful for creating train/test splits for machine learning models.</p>
   *
   * @param data the matrix to split
   * @param ratio the fraction of data to use for training (0 < ratio <= 1)
   * @return a list containing [trainMatrix, testMatrix]
   * @throws IllegalArgumentException if ratio is not between 0 and 1
   */
  static List<Matrix> split(Matrix data, BigDecimal ratio) {
    if (ratio > 1.0 || ratio <= 0) {
      throw new IllegalArgumentException("Ratio must be greater than 0 and at most 1")
    }
    int size = (int) (data.rowCount() * ratio)
    if (size == 0) {
      throw new IllegalArgumentException("Ratio ${ratio} produces an empty training set for ${data.rowCount()} rows")
    }
    List<Integer> samples = (0..data.rowCount() - 1).collect() as List<Integer>
    samples.shuffle()
    List<Integer> train = samples.take(size) as List<Integer>
    List<Integer> test = samples.takeRight(data.rowCount() - size) as List<Integer>
    List<Row> trainRows = data.rows(train)
    List<Row> testRows = data.rows(test)

    Matrix trainMatrix = Matrix.builder()
        .matrixName(data.matrixName + '-train')
        .columnNames(data.columnNames())
        .rowList(trainRows)
        .types(data.types())
        .build()
    Matrix testMatrix = Matrix.builder()
        .matrixName(data.matrixName + '-test')
        .columnNames(data.columnNames())
        .rowList(testRows)
        .types(data.types())
        .build()
    return [trainMatrix, testMatrix]
  }
}
