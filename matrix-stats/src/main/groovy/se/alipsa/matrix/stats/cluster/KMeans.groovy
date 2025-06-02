package se.alipsa.matrix.stats.cluster

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

/**
 * Run KMeans clustering on selected columns.
 * Example usage:
 * <pre><code>
 * Matrix m = Matrix.builder('Whiskey data')
 *   .data('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
 *   .build()
 * KMeans kmeans = new KMeans(m)
 * List<String> features = m.columnNames() - 'Distillery'
 *
 * // Option 1: explicitly specify k (number of groups) and iterations
 * Matrix mWithGroup = kmeans.fit(features, 3, 20)
 *
 * // Option 2: estimate k automatically
 * Matrix mWithGroupAuto = kmeans.fit(features, 20)
 * // If you want, you can specify which method to use for automatic estimation (ELBOW or RULE_OF_THUMB)
 * Matrix mWithGroupAuto = kmeans.fit(features, 20, GroupEstimator.CalculationMethod.ELBOW)
 *
 * </pre></code>
 *
 * @param columnNames the column names to use as feature vectors
 * @param k number of clusters
 * @param iterations number of iterations to try, default is 50
 * @param columnName name of the added cluster column (default "Group")
 * @param mutate whether to mutate the original Matrix (true) or return a copy (false), default is true
 */
class KMeans {

  private Matrix matrix
  private KMeansPlusPlus clustering

  KMeans(Matrix matrix) {
    if (matrix == null || matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must contain data")
    }
    this.matrix = matrix
  }

  private double[][] extractPoints(List<String> columnNames) {
    List missingItems = columnNames - matrix.columnNames()

    if (missingItems) {
      throw new IllegalArgumentException("The following columns does not exist in the matrix: ${missingItems.join(', ')}")
    }

    Matrix m = matrix.selectColumns(columnNames)
    double[][] points = new double[m.rowCount()][m.columnCount()]
    m.eachWithIndex { row, i ->
      points[i] = ListConverter.toDoubleArray(row as List<? extends Number>)
    }
    return points
  }

  Matrix fit(List<String> columnNames, int k, int iterations, String columnName = "Group", boolean mutate = true) {
    double[][] points = extractPoints(columnNames)
    clustering = new KMeansPlusPlus.Builder(k, points)
        .iterations(iterations)
        .pp(true)
        .useEpsilon(true)
        .build()
    return addClusterColumn(clustering, columnName, mutate)
  }

  Matrix fit(List<String> columnNames, int iterations = 30, GroupEstimator.CalculationMethod method = GroupEstimator.CalculationMethod.ELBOW, String columnName = "Group", boolean mutate = true) {
    double[][] points = extractPoints(columnNames)
    clustering = new KMeansPlusPlus.Builder(points, method)
        .iterations(iterations)
        .pp(true)
        .useEpsilon(true)
        .build()
    return addClusterColumn(clustering, columnName, mutate)
  }

  private Matrix addClusterColumn(KMeansPlusPlus clustering, String columnName, boolean mutate) {
    List<Integer> clusterIds = clustering.assignment*.clusterId
    if (mutate) {
      return matrix.addColumn(columnName, Integer, clusterIds)
    } else {
      return matrix.clone().addColumn(columnName, Integer, clusterIds)
    }
  }

  String reportTime() {
    return clustering?.timing ?: "No clustering performed yet, call fit() first."
  }

  int getExecutionTimeMillis() {
    return clustering?.executionTimeMillis ?: -1
  }

}
