package se.alipsa.matrix.smile.ml

import groovy.transform.CompileStatic

import smile.data.DataFrame
import smile.data.formula.Formula
import smile.regression.ElasticNet
import smile.regression.LASSO
import smile.regression.LinearModel
import smile.regression.OLS
import smile.regression.RidgeRegression

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.DataframeConverter
import se.alipsa.matrix.smile.SmileUtil

/**
 * Wrapper for Smile regression algorithms providing a Matrix-friendly API.
 * Supports OLS, Ridge, Lasso, and ElasticNet regression.
 */
@CompileStatic
class SmileRegression {

  private static final double ZERO = 0.0d

  private final LinearModel model
  private final Formula formula
  private final String targetColumn
  private final String[] featureColumns

  private SmileRegression(LinearModel model, Formula formula, String targetColumn, String[] featureColumns) {
    this.model = model
    this.formula = formula
    this.targetColumn = targetColumn
    this.featureColumns = featureColumns
  }

  /**
   * Train an Ordinary Least Squares (OLS) regression model.
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @return a trained SmileRegression
   */
  static SmileRegression ols(Matrix matrix, String targetColumn) {
    String[] featureColumns = matrix.columnNames().findAll { it != targetColumn } as String[]
    DataFrame df = DataframeConverter.convert(matrix)
    Formula formula = Formula.lhs(targetColumn)

    LinearModel model = OLS.fit(formula, df)

    new SmileRegression(model, formula, targetColumn, featureColumns)
  }

  /**
   * Train a Ridge regression model (L2 regularization).
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @param lambda the regularization parameter (default 1.0)
   * @return a trained SmileRegression
   */
  static SmileRegression ridge(Matrix matrix, String targetColumn, double lambda = 1.0) {
    String[] featureColumns = matrix.columnNames().findAll { it != targetColumn } as String[]
    DataFrame df = DataframeConverter.convert(matrix)
    Formula formula = Formula.lhs(targetColumn)

    LinearModel model = RidgeRegression.fit(formula, df, lambda)

    new SmileRegression(model, formula, targetColumn, featureColumns)
  }

  /**
   * Train a LASSO regression model (L1 regularization).
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @param lambda the regularization parameter (default 1.0)
   * @return a trained SmileRegression
   */
  static SmileRegression lasso(Matrix matrix, String targetColumn, double lambda = 1.0) {
    String[] featureColumns = matrix.columnNames().findAll { it != targetColumn } as String[]
    DataFrame df = DataframeConverter.convert(matrix)
    Formula formula = Formula.lhs(targetColumn)

    LASSO.Options options = new LASSO.Options(lambda)
    LinearModel model = LASSO.fit(formula, df, options)

    new SmileRegression(model, formula, targetColumn, featureColumns)
  }

  /**
   * Train an ElasticNet regression model (L1 + L2 regularization).
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @param lambda1 the L1 regularization parameter (default 0.5)
   * @param lambda2 the L2 regularization parameter (default 0.5)
   * @return a trained SmileRegression
   */
  static SmileRegression elasticNet(Matrix matrix, String targetColumn, double lambda1 = 0.5, double lambda2 = 0.5) {
    String[] featureColumns = matrix.columnNames().findAll { it != targetColumn } as String[]
    DataFrame df = DataframeConverter.convert(matrix)
    Formula formula = Formula.lhs(targetColumn)

    LinearModel model = ElasticNet.fit(formula, df, lambda1, lambda2)

    new SmileRegression(model, formula, targetColumn, featureColumns)
  }

  /**
   * Predict target values for new data.
   *
   * @param matrix the data to predict (must have the same feature columns as training data)
   * @return a Matrix with predictions added as a new column 'prediction'
   */
  Matrix predict(Matrix matrix) {
    double[] predictions = predictValues(matrix)

    // Create result matrix with predictions
    Map<String, List<?>> data = [:]
    for (String col : matrix.columnNames()) {
      data.put(col, matrix.column(col))
    }

    List<Double> predList = new ArrayList<>(predictions.length)
    for (double pred : predictions) {
      predList.add(pred)
    }
    data.put('prediction', predList)

    List<Class<?>> types = new ArrayList<>(matrix.types())
    types.add(Double)

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Get predictions as an array of double values.
   *
   * @param matrix the data to predict
   * @return array of predicted values
   */
  double[] predictValues(Matrix matrix) {
    DataFrame df = DataframeConverter.convert(matrix)
    int nrows = df.nrow()
    double[] predictions = new double[nrows]
    for (int i = 0; i < nrows; i++) {
      predictions[i] = model.predict(df.get(i))
    }
    predictions
  }

  /**
   * Calculate the R-squared (coefficient of determination) for test data.
   *
   * @param testMatrix the test data with actual target values
   * @return R-squared value between 0 and 1 (can be negative for poor fits)
   */
  double rSquared(Matrix testMatrix) {
    double[] actual = extractTarget(testMatrix, targetColumn)
    double[] predicted = predictValues(testMatrix)

    calculateRSquared(actual, predicted)
  }

  /**
   * Calculate the Mean Squared Error (MSE) for test data.
   *
   * @param testMatrix the test data with actual target values
   * @return the mean squared error
   */
  double mse(Matrix testMatrix) {
    double[] actual = extractTarget(testMatrix, targetColumn)
    double[] predicted = predictValues(testMatrix)

    calculateMSE(actual, predicted)
  }

  /**
   * Calculate the Root Mean Squared Error (RMSE) for test data.
   *
   * @param testMatrix the test data with actual target values
   * @return the root mean squared error
   */
  double rmse(Matrix testMatrix) {
    Math.sqrt(mse(testMatrix))
  }

  /**
   * Calculate the Mean Absolute Error (MAE) for test data.
   *
   * @param testMatrix the test data with actual target values
   * @return the mean absolute error
   */
  double mae(Matrix testMatrix) {
    double[] actual = extractTarget(testMatrix, targetColumn)
    double[] predicted = predictValues(testMatrix)

    calculateMAE(actual, predicted)
  }

  /**
   * Get evaluation metrics as a Matrix.
   *
   * @param testMatrix the test data with actual target values
   * @return a Matrix with R², MSE, RMSE, MAE metrics
   */
  Matrix evaluate(Matrix testMatrix) {
    double[] actual = extractTarget(testMatrix, targetColumn)
    double[] predicted = predictValues(testMatrix)

    double r2 = calculateRSquared(actual, predicted)
    double mseVal = calculateMSE(actual, predicted)
    double rmseVal = Math.sqrt(mseVal)
    double maeVal = calculateMAE(actual, predicted)

    Matrix.builder()
        .data(
            metric: ['R²', 'MSE', 'RMSE', 'MAE'],
            value: [
                SmileUtil.round(r2),
                SmileUtil.round(mseVal),
                SmileUtil.round(rmseVal),
                SmileUtil.round(maeVal)
            ]
        )
        .types([String, Double])
        .build()
  }

  /**
   * Get the coefficients of the linear model.
   *
   * @return array of coefficients
   */
  double[] getCoefficients() {
    model.coefficients()
  }

  /**
   * Get the intercept of the linear model.
   *
   * @return the intercept value
   */
  double getIntercept() {
    model.intercept()
  }

  /**
   * Get the feature column names.
   */
  String[] getFeatureColumns() {
    featureColumns
  }

  /**
   * Get the target column name.
   */
  String getTargetColumn() {
    targetColumn
  }

  // Helper methods

  private static double[] extractTarget(Matrix matrix, String targetColumn) {
    List<?> values = matrix.column(targetColumn)
    double[] result = new double[values.size()]

    for (int i = 0; i < values.size(); i++) {
      Object val = values.get(i)
      if (val == null) {
        throw new IllegalArgumentException(
            "Target column '${targetColumn}' contains a null at row ${i}. " +
            "Use SmileFeatures.dropna() or fillna() to handle missing values before training.")
      }
      result[i] = val as double
    }

    result
  }

  private static double calculateRSquared(double[] actual, double[] predicted) {
    double meanActual = ZERO
    for (double a : actual) {
      meanActual += a
    }
    meanActual /= actual.length

    double ssTot = ZERO
    double ssRes = ZERO

    for (int i = 0; i < actual.length; i++) {
      ssTot += (actual[i] - meanActual) * (actual[i] - meanActual)
      ssRes += (actual[i] - predicted[i]) * (actual[i] - predicted[i])
    }

    ssTot > 0 ? 1.0d - (ssRes / ssTot) : 0.0d
  }

  private static double calculateMSE(double[] actual, double[] predicted) {
    double sum = ZERO
    for (int i = 0; i < actual.length; i++) {
      double diff = actual[i] - predicted[i]
      sum += diff * diff
    }
    sum / actual.length
  }

  private static double calculateMAE(double[] actual, double[] predicted) {
    double sum = ZERO
    for (int i = 0; i < actual.length; i++) {
      sum += Math.abs(actual[i] - predicted[i])
    }
    sum / actual.length
  }

}
