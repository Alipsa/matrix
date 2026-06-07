package se.alipsa.matrix.smile.ml

import groovy.transform.CompileStatic

import smile.classification.*
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.validation.metric.Accuracy
import smile.validation.metric.ConfusionMatrix

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.DataframeConverter
import se.alipsa.matrix.smile.SmileUtil

/**
 * Wrapper for Smile classification algorithms providing a Matrix-friendly API.
 * Supports RandomForest and DecisionTree classifiers.
 */
@CompileStatic
class SmileClassifier {

  private static final String NULL_LABEL = '<null>'

  private final DataFrameClassifier model
  private final Formula formula
  private final String targetColumn
  private final String[] featureColumns
  private final String[] classLabels

  private SmileClassifier(DataFrameClassifier model, Formula formula, String targetColumn, String[] featureColumns, String[] classLabels) {
    this.model = model
    this.formula = formula
    this.targetColumn = targetColumn
    this.featureColumns = featureColumns
    this.classLabels = classLabels
  }

  /**
   * Train a Random Forest classifier.
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @param ntrees number of trees (default 100)
   * @return a trained SmileClassifier
   */
  static SmileClassifier randomForest(Matrix matrix, String targetColumn, int ntrees = 100) {
    if (ntrees <= 0) {
      throw new IllegalArgumentException("ntrees must be positive: was $ntrees")
    }
    validateTrainingInput(matrix, targetColumn)
    String[] featureColumns = SmileUtil.extractFeatureColumns(matrix, targetColumn)
    SmileUtil.validateNoNullFeatureColumns(matrix, featureColumns)

    // Extract class labels and encode target as integers
    String[] classLabels = extractClassLabels(matrix, targetColumn)
    Matrix encodedMatrix = encodeTarget(matrix, targetColumn, classLabels)

    DataFrame df = DataframeConverter.convert(encodedMatrix)
    Formula formula = Formula.lhs(targetColumn)

    RandomForest rf = RandomForest.fit(formula, df, new RandomForest.Options(ntrees))

    new SmileClassifier(rf, formula, targetColumn, featureColumns, classLabels)
  }

  /**
   * Train a Decision Tree classifier.
   *
   * @param matrix the training data
   * @param targetColumn the name of the target column
   * @return a trained SmileClassifier
   */
  static SmileClassifier decisionTree(Matrix matrix, String targetColumn) {
    validateTrainingInput(matrix, targetColumn)
    String[] featureColumns = SmileUtil.extractFeatureColumns(matrix, targetColumn)
    SmileUtil.validateNoNullFeatureColumns(matrix, featureColumns)

    // Extract class labels and encode target as integers
    String[] classLabels = extractClassLabels(matrix, targetColumn)
    Matrix encodedMatrix = encodeTarget(matrix, targetColumn, classLabels)

    DataFrame df = DataframeConverter.convert(encodedMatrix)
    Formula formula = Formula.lhs(targetColumn)

    DecisionTree tree = DecisionTree.fit(formula, df)

    new SmileClassifier(tree, formula, targetColumn, featureColumns, classLabels)
  }

  /**
   * Predict class labels for new data.
   *
   * @param matrix the data to predict (must have the same feature columns as training data)
   * @return a Matrix with predictions added as a new column 'prediction'
   */
  Matrix predict(Matrix matrix) {
    List<String> predLabels = predictLabels(matrix)

    // Create result matrix with predictions
    Map<String, List<?>> data = [:]
    for (String col : matrix.columnNames()) {
      data.put(col, matrix.column(col))
    }
    data.put('prediction', predLabels)

    List<Class<?>> types = new ArrayList<>(matrix.types())
    types.add(String)

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Get predictions as a list of class labels.
   *
   * @param matrix the data to predict
   * @return list of predicted class labels
   */
  List<String> predictLabels(Matrix matrix) {
    SmileUtil.validateFeatureColumns(matrix, featureColumns)
    SmileUtil.validateNoNullFeatureColumns(matrix, featureColumns)
    DataFrame df = DataframeConverter.convert(matrix)
    int[] predictions = model.predict(df)

    List<String> labels = new ArrayList<>(predictions.length)
    for (int pred : predictions) {
      labels.add(classLabels[pred])
    }

    labels
  }

  /**
   * Get predictions as integer class indices.
   *
   * @param matrix the data to predict
   * @return array of predicted class indices
   */
  int[] predictClasses(Matrix matrix) {
    SmileUtil.validateFeatureColumns(matrix, featureColumns)
    SmileUtil.validateNoNullFeatureColumns(matrix, featureColumns)
    DataFrame df = DataframeConverter.convert(matrix)
    model.predict(df)
  }

  /**
   * Evaluate the classifier on test data.
   *
   * @param testMatrix the test data with actual labels
   * @return accuracy as a value between 0 and 1
   */
  double accuracy(Matrix testMatrix) {
    SmileUtil.validateTestMatrix(testMatrix, targetColumn)
    int[] actual = extractTargetAsInt(testMatrix, targetColumn, classLabels)
    int[] predicted = predictClasses(testMatrix)

    Accuracy.of(actual, predicted)
  }

  /**
   * Get the confusion matrix for test data.
   *
   * @param testMatrix the test data with actual labels
   * @return a Matrix representing the confusion matrix
   */
  Matrix confusionMatrix(Matrix testMatrix) {
    SmileUtil.validateTestMatrix(testMatrix, targetColumn)
    int[] actual = extractTargetAsInt(testMatrix, targetColumn, classLabels)
    int[] predicted = predictClasses(testMatrix)

    ConfusionMatrix cm = ConfusionMatrix.of(actual, predicted)
    int[][] matrix = cm.matrix()

    // Build result matrix
    Map<String, List<?>> data = [:]
    data.put('actual', classLabels.toList())

    for (int j = 0; j < classLabels.length; j++) {
      List<Integer> col = []
      for (int i = 0; i < classLabels.length; i++) {
        col.add(matrix[i][j])
      }
      data.put("pred_${classLabels[j]}" as String, col)
    }

    List<Class<?>> types = []
    types.add(String)
    for (int i = 0; i < classLabels.length; i++) {
      types.add(Integer)
    }

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

  /**
   * Get evaluation metrics as a Matrix.
   *
   * @param testMatrix the test data with actual labels
   * @return a Matrix with precision, recall, f1 for each class
   */
  Matrix evaluate(Matrix testMatrix) {
    SmileUtil.validateTestMatrix(testMatrix, targetColumn)
    int[] actual = extractTargetAsInt(testMatrix, targetColumn, classLabels)
    int[] predicted = predictClasses(testMatrix)

    ConfusionMatrix cm = ConfusionMatrix.of(actual, predicted)
    int[][] matrix = cm.matrix()
    int numClasses = classLabels.length

    List<String> classes = []
    List<Double> precisions = []
    List<Double> recalls = []
    List<Double> f1Scores = []
    List<Integer> supports = []

    for (int i = 0; i < numClasses; i++) {
      classes.add(classLabels[i])

      int tp = matrix[i][i]
      int fp = 0
      int fn = 0

      for (int j = 0; j < numClasses; j++) {
        if (j != i) {
          fp += matrix[j][i]  // predicted as i but actually j
          fn += matrix[i][j]  // actually i but predicted as j
        }
      }

      int support = 0
      for (int j = 0; j < numClasses; j++) {
        support += matrix[i][j]
      }
      supports.add(support)

      double precision = (tp + fp) > 0 ? ((double) tp) / ((double) (tp + fp)) : 0.0d
      double recall = (tp + fn) > 0 ? ((double) tp) / ((double) (tp + fn)) : 0.0d
      double f1 = (precision + recall) > 0 ? 2.0d * precision * recall / (precision + recall) : 0.0d

      precisions.add(SmileUtil.round(precision))
      recalls.add(SmileUtil.round(recall))
      f1Scores.add(SmileUtil.round(f1))
    }

    Matrix.builder()
        .data(
            'class': classes,
            precision: precisions,
            recall: recalls,
            f1: f1Scores,
            support: supports
        )
        .types([String, Double, Double, Double, Integer])
        .build()
  }

  /**
   * Get the class labels used by this classifier.
   */
  String[] getClassLabels() {
    classLabels
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

  /**
   * Get the underlying Smile model.
   */
  DataFrameClassifier getModel() {
    model
  }

  // Helper methods

  private static void validateTrainingInput(Matrix matrix, String targetColumn) {
    if (!matrix.columnNames().contains(targetColumn)) {
      throw new IllegalArgumentException(
          "Target column '${targetColumn}' not found. Available: ${matrix.columnNames()}")
    }
    requireNonNullLabels(matrix, targetColumn)
  }

  private static void requireNonNullLabels(Matrix matrix, String targetColumn) {
    if (SmileUtil.hasNulls(matrix.column(targetColumn))) {
      throw new IllegalArgumentException(
          "Target column '${targetColumn}' contains null labels. " +
          SmileUtil.DROPNA_HINT_TRAINING)
    }
  }

  private static String[] extractClassLabels(Matrix matrix, String targetColumn) {
    List<?> values = matrix.column(targetColumn)
    Set<String> uniqueLabels = new LinkedHashSet<>()
    for (Object val : values) {
      if (val != null) {
        uniqueLabels.add(val.toString())
      }
    }
    uniqueLabels.toArray(new String[0])
  }

  @SuppressWarnings('UnnecessaryToString')
  private static int[] extractTargetAsInt(Matrix matrix, String targetColumn, String[] classLabels) {
    List<?> values = matrix.column(targetColumn)
    int[] result = new int[values.size()]

    Map<String, Integer> labelToIndex = [:]
    for (int i = 0; i < classLabels.length; i++) {
      labelToIndex.put(classLabels[i], Integer.valueOf(i))
    }

    for (int i = 0; i < values.size(); i++) {
      String label = values.get(i)?.toString()
      Integer index = labelToIndex.get(label)
      if (index == null) {
        throw new IllegalArgumentException(
            "Label '${label ?: NULL_LABEL}' at row ${i} was not seen during training. " +
            "Known labels: ${labelToIndex.keySet()}")
      }
      result[i] = index.intValue()
    }

    result
  }

  /**
   * Encode string target column as integers for Smile.
   */
  @SuppressWarnings('UnnecessaryToString')
  private static Matrix encodeTarget(Matrix matrix, String targetColumn, String[] classLabels) {
    Map<String, Integer> labelToIndex = [:]
    for (int i = 0; i < classLabels.length; i++) {
      labelToIndex.put(classLabels[i], Integer.valueOf(i))
    }

    List<?> originalValues = matrix.column(targetColumn)
    List<Integer> encodedValues = new ArrayList<>(originalValues.size())
    for (int i = 0; i < originalValues.size(); i++) {
      Object val = originalValues.get(i)
      String label = val?.toString()
      Integer index = labelToIndex.get(label)
      if (index == null) {
        throw new IllegalArgumentException(
            "Label '${label ?: NULL_LABEL}' at row ${i} was not seen during training. " +
            "Known labels: ${labelToIndex.keySet()}")
      }
      encodedValues.add(index)
    }

    // Create new matrix with encoded target column
    Map<String, List<?>> data = [:]
    List<Class<?>> types = []

    for (int i = 0; i < matrix.columnCount(); i++) {
      String colName = matrix.columnName(i)
      if (colName == targetColumn) {
        data.put(colName, encodedValues)
        types.add(Integer)
      } else {
        data.put(colName, matrix.column(i))
        types.add(matrix.type(i))
      }
    }

    Matrix.builder()
        .data(data)
        .types(types)
        .build()
  }

}
