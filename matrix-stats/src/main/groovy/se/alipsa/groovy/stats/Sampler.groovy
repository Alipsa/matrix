package se.alipsa.groovy.stats

import se.alipsa.groovy.matrix.Matrix

class Sampler {

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
    .name(data.name + '-train')
    .columnNames(data.columnNames())
    .rows(data.rows(train))
    .types(data.types())
    .build()
    Matrix testMatrix =
        Matrix.builder()
            .name(data.name + '-test')
            .columnNames(data.columnNames())
            .rows(data.rows(test))
            .types(data.types())
            .build()
    return [trainMatrix, testMatrix]
  }
}
