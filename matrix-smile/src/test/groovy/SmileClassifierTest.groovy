import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.ml.SmileClassifier

import static org.junit.jupiter.api.Assertions.*

class SmileClassifierTest {

  // Binary classification dataset for RandomForest (avoids Smile multi-class metrics bug)
  private Matrix createBinaryData() {
    return Matrix.builder()
        .data(
            x1: [1.0, 1.2, 1.5, 1.8, 2.0, 2.2, 8.0, 8.2, 8.5, 8.8, 9.0, 9.2],
            x2: [1.1, 0.9, 1.3, 1.7, 2.1, 1.9, 8.1, 7.9, 8.3, 8.7, 9.1, 8.9],
            label: ['A', 'A', 'A', 'A', 'A', 'A', 'B', 'B', 'B', 'B', 'B', 'B']
        )
        .types([Double, Double, String])
        .build()
  }

  private Matrix createBinaryTestData() {
    return Matrix.builder()
        .data(
            x1: [1.3, 8.3],
            x2: [1.4, 8.4],
            label: ['A', 'B']
        )
        .types([Double, Double, String])
        .build()
  }

  // Multi-class iris-like dataset for DecisionTree
  private Matrix createIrisLikeData() {
    return Matrix.builder()
        .data(
            sepal_length: [5.1, 4.9, 4.7, 5.0, 5.4, 7.0, 6.4, 6.9, 5.5, 6.5, 6.3, 5.8, 7.1, 6.3, 6.5],
            sepal_width: [3.5, 3.0, 3.2, 3.6, 3.9, 3.2, 3.2, 3.1, 2.3, 2.8, 3.3, 2.7, 3.0, 2.9, 3.0],
            petal_length: [1.4, 1.4, 1.3, 1.4, 1.7, 4.7, 4.5, 4.9, 4.0, 4.6, 6.0, 5.1, 5.9, 5.6, 5.8],
            petal_width: [0.2, 0.2, 0.2, 0.2, 0.4, 1.4, 1.5, 1.5, 1.3, 1.5, 2.5, 1.9, 2.1, 1.8, 2.2],
            species: ['setosa', 'setosa', 'setosa', 'setosa', 'setosa',
                      'versicolor', 'versicolor', 'versicolor', 'versicolor', 'versicolor',
                      'virginica', 'virginica', 'virginica', 'virginica', 'virginica']
        )
        .types([Double, Double, Double, Double, String])
        .build()
  }

  private Matrix createIrisTestData() {
    return Matrix.builder()
        .data(
            sepal_length: [5.0, 6.5, 6.7],
            sepal_width: [3.4, 3.0, 3.1],
            petal_length: [1.5, 4.5, 5.5],
            petal_width: [0.2, 1.5, 2.0],
            species: ['setosa', 'versicolor', 'virginica']
        )
        .types([Double, Double, Double, Double, String])
        .build()
  }

  @Test
  void testRandomForestClassifier() {
    Matrix trainData = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)

    assertNotNull(classifier)
    assertEquals('label', classifier.targetColumn)
    assertEquals(2, classifier.featureColumns.length)
    assertEquals(2, classifier.classLabels.length)
    assertTrue(classifier.classLabels.contains('A'))
    assertTrue(classifier.classLabels.contains('B'))
  }

  @Test
  void testRandomForestPredict() {
    Matrix trainData = createBinaryData()
    Matrix testData = createBinaryTestData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)
    Matrix predictions = classifier.predict(testData)

    assertEquals(2, predictions.rowCount())
    assertTrue(predictions.columnNames().contains('prediction'))
    assertEquals(4, predictions.columnCount()) // 3 original + 1 prediction
  }

  @Test
  void testRandomForestPredictLabels() {
    Matrix trainData = createBinaryData()
    Matrix testData = createBinaryTestData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)
    List<String> labels = classifier.predictLabels(testData)

    assertEquals(2, labels.size())
    assertEquals('A', labels[0])
    assertEquals('B', labels[1])
  }

  @Test
  void testRandomForestAccuracy() {
    Matrix data = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(data, 'label', 50)
    double accuracy = classifier.accuracy(data)

    // Should have good accuracy on well-separated training data
    assertTrue(accuracy >= 0.8, "Expected accuracy >= 0.8 but got $accuracy")
  }

  @Test
  void testRandomForestConfusionMatrix() {
    Matrix data = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(data, 'label', 50)
    Matrix cm = classifier.confusionMatrix(data)

    assertNotNull(cm)
    assertEquals(2, cm.rowCount()) // 2 classes
    assertTrue(cm.columnNames().contains('actual'))
  }

  @Test
  void testRandomForestEvaluate() {
    Matrix data = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(data, 'label', 50)
    Matrix eval = classifier.evaluate(data)

    assertEquals(2, eval.rowCount()) // 2 classes
    assertTrue(eval.columnNames().contains('class'))
    assertTrue(eval.columnNames().contains('precision'))
    assertTrue(eval.columnNames().contains('recall'))
    assertTrue(eval.columnNames().contains('f1'))
    assertTrue(eval.columnNames().contains('support'))
  }

  @Test
  void testDecisionTreeClassifier() {
    Matrix trainData = createIrisLikeData()

    SmileClassifier classifier = SmileClassifier.decisionTree(trainData, 'species')

    assertNotNull(classifier)
    assertEquals('species', classifier.targetColumn)
    assertEquals(4, classifier.featureColumns.length)
    assertEquals(3, classifier.classLabels.length)
  }

  @Test
  void testDecisionTreePredict() {
    Matrix trainData = createIrisLikeData()
    Matrix testData = createIrisTestData()

    SmileClassifier classifier = SmileClassifier.decisionTree(trainData, 'species')
    List<String> labels = classifier.predictLabels(testData)

    assertEquals(3, labels.size())
    assertEquals('setosa', labels[0])
  }

  @Test
  void testDecisionTreeAccuracy() {
    Matrix data = createIrisLikeData()

    SmileClassifier classifier = SmileClassifier.decisionTree(data, 'species')
    double accuracy = classifier.accuracy(data)

    // Decision tree should have good accuracy on training data
    assertTrue(accuracy >= 0.8, "Expected accuracy >= 0.8 but got $accuracy")
  }

  @Test
  void testDecisionTreeConfusionMatrix() {
    Matrix data = createIrisLikeData()

    SmileClassifier classifier = SmileClassifier.decisionTree(data, 'species')
    Matrix cm = classifier.confusionMatrix(data)

    assertNotNull(cm)
    assertEquals(3, cm.rowCount())
    assertTrue(cm.columnNames().contains('actual'))
  }

  @Test
  void testDecisionTreeEvaluate() {
    Matrix data = createIrisLikeData()

    SmileClassifier classifier = SmileClassifier.decisionTree(data, 'species')
    Matrix eval = classifier.evaluate(data)

    assertEquals(3, eval.rowCount())
    assertTrue(eval.columnNames().contains('class'))
    assertTrue(eval.columnNames().contains('precision'))
    assertTrue(eval.columnNames().contains('recall'))
    assertTrue(eval.columnNames().contains('f1'))
    assertTrue(eval.columnNames().contains('support'))
  }

  @Test
  void testPredictClasses() {
    Matrix trainData = createBinaryData()
    Matrix testData = createBinaryTestData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)
    int[] classes = classifier.predictClasses(testData)

    assertEquals(2, classes.length)
    // Classes should be valid indices
    for (int c : classes) {
      assertTrue(c >= 0 && c < 2)
    }
  }

  @Test
  void testGetClassLabels() {
    Matrix trainData = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)
    String[] labels = classifier.classLabels

    assertEquals(2, labels.length)
  }

  @Test
  void testGetFeatureColumns() {
    Matrix trainData = createBinaryData()

    SmileClassifier classifier = SmileClassifier.randomForest(trainData, 'label', 20)
    String[] features = classifier.featureColumns

    assertEquals(2, features.length)
    assertTrue(features.contains('x1'))
    assertTrue(features.contains('x2'))
  }
}
