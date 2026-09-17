package se.alipsa.matrix.stats.dimred

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.linalg.Linalg
import se.alipsa.matrix.stats.linalg.SvdResult

/**
 * Principal Component Analysis (PCA) for dimensionality reduction of Matrix data.
 *
 * <p>PCA transforms a set of possibly correlated numeric columns into a smaller set of
 * uncorrelated columns (principal components) ordered by the amount of variance they
 * explain. The implementation is based on the singular value decomposition (SVD) of the
 * (optionally centered and/or scaled) data matrix, which is numerically stable and
 * equivalent to the eigendecomposition of the covariance matrix.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * import se.alipsa.matrix.core.Matrix
 * import se.alipsa.matrix.stats.dimred.Pca
 *
 * Matrix data = Matrix.builder()
 *   .columnNames(['height', 'weight', 'age'])
 *   .rows([[170, 65, 30], [180, 80, 45], [160, 55, 25]])
 *   .types([Double, Double, Double])
 *   .build()
 *
 * Pca pca = Pca.fit(data)
 * println pca.explainedVariance()          // variance ratio per component
 * Matrix projected = pca.project(2)        // scores with columns PC1, PC2
 * </pre>
 *
 * <h3>Centering and Scaling</h3>
 * <p>By default columns are centered (mean subtracted) before the decomposition, which is
 * the standard PCA behavior. Set {@code scale} to {@code true} to also divide each column
 * by its standard deviation (z-score), which is recommended when columns are measured on
 * different scales. Scaling requires non-constant columns.</p>
 *
 * @see Linalg
 * @see SvdResult
 */
class Pca {

  private static final String PROJECTION_NAME = 'PCA projection'
  private static final String JOIN_SEPARATOR = ', '

  private final List<String> columnNames
  private final List<BigDecimal> means
  private final List<BigDecimal> scales
  private final SvdResult svd

  private Pca(List<String> columnNames, List<BigDecimal> means, List<BigDecimal> scales, SvdResult svd) {
    this.columnNames = columnNames.asImmutable()
    this.means = means.asImmutable()
    this.scales = scales.asImmutable()
    this.svd = svd
  }

  /**
   * Fit a PCA model on the numeric columns of a matrix.
   *
   * @param matrix the source data matrix
   * @param columns the columns to use for the decomposition; defaults to all columns
   * @param center whether to subtract the column mean before decomposition (default true)
   * @param scale whether to divide by the column standard deviation after centering (default false)
   * @return a fitted Pca instance
   * @throws IllegalArgumentException if the matrix is null or empty, a column is missing,
   *         or scaling is requested for a constant column
   */
  static Pca fit(Matrix matrix, List<String> columns = null, boolean center = true, boolean scale = false) {
    if (matrix == null || matrix.rowCount() == 0 || matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must contain data')
    }
    List<String> selected = columns ?: matrix.columnNames()
    List<String> missing = selected - matrix.columnNames()
    if (missing) {
      throw new IllegalArgumentException(missingColumns(missing, 'matrix'))
    }

    List<BigDecimal> means = center ? Stat.means(matrix, selected) : [0.0G] * selected.size()
    List<BigDecimal> scales = [1.0G] * selected.size()
    if (scale) {
      List<BigDecimal> sds = Stat.sd(matrix, selected)
      sds.eachWithIndex { BigDecimal sd, int idx ->
        if (sd.signum() == 0) {
          throw new IllegalArgumentException(
              "Cannot scale column '${selected[idx]}': it is constant (standard deviation is zero)")
        }
        scales[idx] = sd
      }
    }

    Matrix prepared = prepare(matrix, selected, means, scales)
    new Pca(selected, means, scales, Linalg.svd(prepared))
  }

  /**
   * Fit a PCA model on all columns of a matrix, specifying only centering and scaling.
   *
   * @param matrix the source data matrix
   * @param center whether to subtract the column mean before decomposition
   * @param scale whether to divide by the column standard deviation after centering
   * @return a fitted Pca instance
   */
  static Pca fit(Matrix matrix, boolean center, boolean scale) {
    fit(matrix, null, center, scale)
  }

  /**
   * The number of principal components, equal to the number of fitted columns.
   *
   * @return the number of principal components
   */
  int componentCount() {
    singularValues.size()
  }

  /**
   * The singular values of the (prepared) data matrix in decreasing order.
   * The variance explained by component {@code i} is proportional to the square of
   * singular value {@code i}.
   *
   * @return the singular values as BigDecimal values
   */
  List<BigDecimal> getSingularValues() {
    svd.singularValues
  }

  /**
   * The fraction of total variance explained by each principal component, in [0, 1].
   * The values are ordered by component and sum to 1.
   *
   * @return the variance ratio per component
   */
  List<BigDecimal> explainedVariance() {
    List<Double> squared = singularValues.collect { BigDecimal value -> Math.pow(value.doubleValue(), 2) }
    double total = squared.sum() as double
    squared.collect { Double value -> BigDecimal.valueOf(value / total) }
  }

  /**
   * The fraction of total variance explained by a single principal component.
   *
   * @param component the zero-based component index (0 is PC1)
   * @return the variance ratio for the component, in [0, 1]
   * @throws IndexOutOfBoundsException if the component index is out of range
   */
  BigDecimal explainedVariance(int component) {
    validateComponent(component)
    explainedVariance()[component]
  }

  /**
   * The cumulative fraction of variance explained by the first {@code n} components.
   * The last value is always 1.
   *
   * @return the cumulative variance ratios, one per component
   */
  List<BigDecimal> cumulativeExplainedVariance() {
    List<BigDecimal> cumulative = []
    BigDecimal sum = BigDecimal.ZERO
    explainedVariance().each { BigDecimal ratio ->
      sum += ratio
      cumulative << sum
    }
    cumulative
  }

  /**
   * The scores (coordinates) of the fitted data on a single principal component.
   *
   * @param component the zero-based component index (0 is PC1)
   * @return the score for each row of the fitted data
   * @throws IndexOutOfBoundsException if the component index is out of range
   */
  List<BigDecimal> scores(int component) {
    validateComponent(component)
    (0..<svd.u.rowCount()).collect { int row ->
      (svd.u.get(row, component) as BigDecimal) * singularValues[component]
    }
  }

  /**
   * Project the fitted data onto the first {@code k} principal components.
   *
   * @param k the number of components to project onto, between 1 and the component count
   * @return a matrix with one row per source row and columns {@code PC1..PCk}
   * @throws IndexOutOfBoundsException if k is out of range
   */
  Matrix project(int k) {
    validateComponent(k - 1)
    List<List<BigDecimal>> rows = (0..<svd.u.rowCount()).collect { int row ->
      (0..<k).collect { int component ->
        (svd.u.get(row, component) as BigDecimal) * singularValues[component]
      }
    }
    Matrix.builder()
        .matrixName(PROJECTION_NAME)
        .columnNames(componentNames(k))
        .rows(rows)
        .types(([BigDecimal] * k) as List<Class>)
        .build()
  }

  /**
   * Project new data onto the first {@code k} principal components using this fitted model.
   * The new data must contain the same columns that the model was fitted on, prepared with
   * the same centering and scaling.
   *
   * @param k the number of components to project onto, between 1 and the component count
   * @param data the data to project; must contain the fitted columns
   * @return a matrix with one row per data row and columns {@code PC1..PCk}
   * @throws IllegalArgumentException if a fitted column is missing in the data
   * @throws IndexOutOfBoundsException if k is out of range
   */
  Matrix project(int k, Matrix data) {
    validateComponent(k - 1)
    List<String> missing = columnNames - data.columnNames()
    if (missing) {
      throw new IllegalArgumentException(missingColumns(missing, 'data'))
    }
    Matrix prepared = prepare(data, columnNames, means, scales)
    List<List<BigDecimal>> rows = (0..<prepared.rowCount()).collect { int row ->
      (0..<k).collect { int component ->
        BigDecimal score = BigDecimal.ZERO
        columnNames.eachWithIndex { String name, int idx ->
          score += (prepared.get(row, idx) as BigDecimal) * (svd.vt.get(component, idx) as BigDecimal)
        }
        score
      }
    }
    Matrix.builder()
        .matrixName(PROJECTION_NAME)
        .columnNames(componentNames(k))
        .rows(rows)
        .types(([BigDecimal] * k) as List<Class>)
        .build()
  }

  /**
   * The loadings matrix: one row per fitted column (feature) and one column per principal
   * component ({@code PC1..PCn}), with the feature name in the leading {@code Feature}
   * column. Each component column is a unit eigenvector of the covariance matrix; the
   * value in a cell is the correlation weight between the feature and the component.
   *
   * @return the loadings matrix with a Feature column and one column per component
   */
  Matrix loadings() {
    int components = componentCount()
    List<List<?>> rows = []
    for (int feature = 0; feature < columnNames.size(); feature++) {
      List<Object> row = [columnNames[feature]]
      for (int component = 0; component < components; component++) {
        row << (svd.vt.get(component, feature) as BigDecimal)
      }
      rows << row
    }
    List<Class> types = [String]
    types.addAll(([BigDecimal] * components) as List<Class>)
    Matrix.builder()
        .matrixName('PCA loadings')
        .columnNames(['Feature'] + componentNames(components))
        .rows(rows)
        .types(types)
        .build()
  }

  private void validateComponent(int component) {
    if (component < 0 || component >= componentCount()) {
      throw new IndexOutOfBoundsException(
          "Component index must be between 0 and ${componentCount() - 1} but was $component")
    }
  }

  private static String missingColumns(List<String> missing, String label) {
    "The following columns does not exist in the ${label}: ${missing.join(JOIN_SEPARATOR)}"
  }

  private static List<String> componentNames(int k) {
    (1..k).collect { int i -> 'PC' + i }
  }

  private static Matrix prepare(Matrix source, List<String> columns, List<BigDecimal> means, List<BigDecimal> scales) {
    Matrix prepared = source.select(columns)
    columns.eachWithIndex { String name, int idx ->
      BigDecimal mean = means[idx]
      BigDecimal scaleFactor = scales[idx]
      prepared.apply(name) { Object value ->
        (BigDecimal.valueOf((value as Number).doubleValue()) - mean) / scaleFactor
      }
    }
    prepared
  }
}
