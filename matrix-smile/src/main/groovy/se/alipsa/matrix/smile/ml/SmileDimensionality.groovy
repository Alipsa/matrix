package se.alipsa.matrix.smile.ml

import groovy.transform.CompileStatic

import smile.feature.extraction.PCA

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.SmileUtil

/**
 * Wrapper for Smile dimensionality reduction algorithms providing a Matrix-friendly API.
 * Supports PCA (Principal Component Analysis).
 */
@CompileStatic
class SmileDimensionality {

  private final PCA fullPca
  private final PCA projectionPca
  private final String[] featureColumns
  private final int numComponents

  private SmileDimensionality(PCA fullPca, PCA projectionPca, String[] featureColumns, int numComponents) {
    this.fullPca = fullPca
    this.projectionPca = projectionPca
    this.featureColumns = featureColumns
    this.numComponents = numComponents
  }

  /**
   * Perform PCA (Principal Component Analysis) and reduce to k dimensions.
   *
   * @param matrix the data to transform
   * @param k the number of principal components to retain
   * @return a SmileDimensionality with the PCA result
   */
  static SmileDimensionality pca(Matrix matrix, int k) {
    String[] featureColumns = matrix.columnNames() as String[]
    double[][] data = SmileUtil.matrixToArray(matrix)

    PCA pcaModel = PCA.fit(data)
    PCA proj = pcaModel.getProjection(k)

    new SmileDimensionality(pcaModel, proj, featureColumns, k)
  }

  /**
   * Perform PCA retaining a fraction of the total variance.
   *
   * @param matrix the data to transform
   * @param varianceFraction the fraction of variance to retain (0.0 to 1.0)
   * @return a SmileDimensionality with the PCA result
   */
  static SmileDimensionality pcaByVariance(Matrix matrix, double varianceFraction) {
    if (varianceFraction <= 0.0 || varianceFraction > 1.0) {
      throw new IllegalArgumentException("Variance fraction must be between 0 and 1 (exclusive of 0): was $varianceFraction")
    }

    String[] featureColumns = matrix.columnNames() as String[]
    double[][] data = SmileUtil.matrixToArray(matrix)

    PCA pcaModel = PCA.fit(data)
    PCA proj = pcaModel.getProjection(varianceFraction)

    // Determine actual number of components from projection
    double[] transformed = proj.apply(data[0])
    int k = transformed.length

    new SmileDimensionality(pcaModel, proj, featureColumns, k)
  }

  /**
   * Perform PCA using correlation matrix (standardized data).
   * Use this when features have different scales.
   *
   * @param matrix the data to transform
   * @param k the number of principal components to retain
   * @return a SmileDimensionality with the PCA result
   */
  static SmileDimensionality pcaCorrelation(Matrix matrix, int k) {
    String[] featureColumns = matrix.columnNames() as String[]
    double[][] data = SmileUtil.matrixToArray(matrix)

    PCA pcaModel = PCA.cor(data)
    PCA proj = pcaModel.getProjection(k)

    new SmileDimensionality(pcaModel, proj, featureColumns, k)
  }

  /**
   * Transform data using the fitted PCA model.
   *
   * @param matrix the data to transform
   * @return a Matrix with transformed data (k columns for k components)
   */
  Matrix transform(Matrix matrix) {
    double[][] data = SmileUtil.matrixToArray(matrix)
    double[][] transformed = projectionPca.apply(data)

    arrayToMatrix(transformed, numComponents)
  }

  /**
   * Get the transformed data as a double array.
   *
   * @param matrix the data to transform
   * @return 2D array of transformed values
   */
  double[][] transformValues(Matrix matrix) {
    double[][] data = SmileUtil.matrixToArray(matrix)
    projectionPca.apply(data)
  }

  /**
   * Get the variance explained by each principal component.
   *
   * @return array of variances (eigenvalues)
   */
  double[] getVariance() {
    fullPca.variance()
  }

  /**
   * Get the proportion of variance explained by each component.
   *
   * @return array of variance proportions
   */
  double[] getVarianceProportion() {
    fullPca.varianceProportion()
  }

  /**
   * Get the cumulative variance proportion.
   *
   * @return array of cumulative variance proportions
   */
  double[] getCumulativeVarianceProportion() {
    fullPca.cumulativeVarianceProportion()
  }

  /**
   * Get the loadings (eigenvectors) matrix.
   *
   * @return 2D array of loadings
   */
  double[][] getLoadings() {
    fullPca.loadings().toArray()
  }

  /**
   * Get the loadings as a Matrix.
   *
   * @return a Matrix with loadings (features x components)
   */
  @SuppressWarnings('NestedForLoop')
  Matrix getLoadingsMatrix() {
    double[][] loadings = getLoadings()

    Map<String, List<?>> data = [:]
    data.put('feature', featureColumns.toList())

    for (int j = 0; j < loadings[0].length; j++) {
      List<Double> col = new ArrayList<>(loadings.length)
      for (int i = 0; i < loadings.length; i++) {
        col.add(loadings[i][j])
      }
      data.put("PC${j + 1}" as String, col)
    }

    List<Class<?>> types = []
    types.add(String)
    for (int i = 0; i < loadings[0].length; i++) {
      types.add(Double)
    }

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Get variance explained summary as a Matrix.
   *
   * @return a Matrix with component, variance, proportion, and cumulative proportion
   */
  Matrix varianceSummary() {
    double[] variance = getVariance()
    double[] proportion = getVarianceProportion()
    double[] cumulative = getCumulativeVarianceProportion()

    List<String> components = []
    List<Double> variances = []
    List<Double> proportions = []
    List<Double> cumulatives = []

    for (int i = 0; i < variance.length; i++) {
      components.add("PC${i + 1}" as String)
      variances.add(SmileUtil.round(variance[i]))
      proportions.add(SmileUtil.round(proportion[i]))
      cumulatives.add(SmileUtil.round(cumulative[i]))
    }

    Matrix.builder()
        .data(
            component: components,
            variance: variances,
            proportion: proportions,
            cumulative: cumulatives
        )
        .types([String, Double, Double, Double])
        .build()
  }

  /**
   * Get the number of principal components.
   */
  int getNumComponents() {
    numComponents
  }

  /**
   * Get the original feature column names.
   */
  String[] getFeatureColumns() {
    featureColumns
  }

  /**
   * Get the center (mean) of the original data.
   *
   * @return array of means for each feature
   */
  double[] getCenter() {
    fullPca.center()
  }

  // Helper methods

  @SuppressWarnings('NestedForLoop')
  private static Matrix arrayToMatrix(double[][] data, int numComponents) {
    Map<String, List<?>> result = [:]
    List<Class<?>> types = []

    for (int j = 0; j < numComponents; j++) {
      List<Double> col = new ArrayList<>(data.length)
      for (int i = 0; i < data.length; i++) {
        col.add(data[i][j])
      }
      result.put("PC${j + 1}" as String, col)
      types.add(Double)
    }

    Matrix.builder()
        .data(result)
        .types(types)
        .build()
  }

}
