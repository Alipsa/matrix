package se.alipsa.matrix.stats

import se.alipsa.matrix.core.Matrix

/**
 * Utility class for sampling and splitting matrices.
 *
 * <p>This class provides methods for random train/test splitting of matrices,
 * which is useful for machine learning workflows. This is distinct from
 * Matrix.split() and Matrix.splitInto() which provide deterministic chunking.</p>
 */
class Sampler {

  /**
   * Randomly splits a matrix into train and test sets.
   *
   * <p>This method shuffles the rows and splits them according to the specified ratio.
   * This is useful for creating train/test splits for machine learning models.</p>
   *
   * @param data the matrix to split
   * @param ratio the fraction of data to use for training (0 < ratio < 1)
   * @return a list containing [trainMatrix, testMatrix]
   * @throws IllegalArgumentException if ratio is not between 0 and 1
   */
  static List<Matrix> split(Matrix data, BigDecimal ratio) {
    if (ratio > 1.0 || ratio < 0) {
      throw new IllegalArgumentException("Ratio must be a number between 0 and 1")
    }
    int size = (int) (data.rowCount() * ratio)
    def samples = (0..data.rowCount()-1).collect()
    samples.shuffle()
    def train = samples.take(size)
    def test = samples.takeRight(data.rowCount()-size)

    Matrix trainMatrix = Matrix.builder()
    .matrixName(data.matrixName + '-train')
    .columnNames(data.columnNames())
    .rows(data.rows(train))
    .types(data.types())
    .build()
    Matrix testMatrix =
        Matrix.builder()
            .matrixName(data.matrixName + '-test')
            .columnNames(data.columnNames())
            .rows(data.rows(test))
            .types(data.types())
            .build()
    return [trainMatrix, testMatrix]
  }
}
