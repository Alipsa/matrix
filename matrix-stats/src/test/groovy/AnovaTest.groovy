import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Anova

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Anova class.
 * Reference values computed using R's aov() function.
 */
class AnovaTest {

  private static final double TOLERANCE = 1e-4

  @Test
  void testAnovaWithIrisDataset() {
    // Test one-way ANOVA on iris dataset
    // R> data(iris)
    // R> summary(aov(Sepal.Length ~ Species, data=iris))
    //             Df Sum Sq Mean Sq F value Pr(>F)
    // Species      2  63.21  31.606   119.3 <2e-16 ***
    // Residuals  147  38.96   0.265

    def iris = Dataset.iris()

    // Group by species
    def setosa = iris.filter { row -> row['Species'] == 'setosa' }['Sepal.Length'] as List<Number>
    def versicolor = iris.filter { row -> row['Species'] == 'versicolor' }['Sepal.Length'] as List<Number>
    def virginica = iris.filter { row -> row['Species'] == 'virginica' }['Sepal.Length'] as List<Number>

    def data = [
      'setosa': setosa,
      'versicolor': versicolor,
      'virginica': virginica
    ]

    def result = Anova.aov(data)

    assertEquals(119.3, result.fValue, 0.1, 'F-value for iris Sepal.Length by Species')
    assertTrue(result.pValue < 1e-15, 'p-value should be very small')
  }

  @Test
  void testAnovaWithIrisSepalWidth() {
    // R> summary(aov(Sepal.Width ~ Species, data=iris))
    //             Df Sum Sq Mean Sq F value Pr(>F)
    // Species      2  11.35   5.672    49.2 <2e-16 ***
    // Residuals  147  16.96   0.115

    def iris = Dataset.iris()

    def setosa = iris.filter { row -> row['Species'] == 'setosa' }['Sepal.Width'] as List<Number>
    def versicolor = iris.filter { row -> row['Species'] == 'versicolor' }['Sepal.Width'] as List<Number>
    def virginica = iris.filter { row -> row['Species'] == 'virginica' }['Sepal.Width'] as List<Number>

    def data = [
      'setosa': setosa,
      'versicolor': versicolor,
      'virginica': virginica
    ]

    def result = Anova.aov(data)

    assertEquals(49.2, result.fValue, 0.5, 'F-value for iris Sepal.Width by Species')
    assertTrue(result.pValue < 1e-10, 'p-value should be very small')
  }

  @Test
  void testAnovaWithMapData() {
    // R> group1 <- c(10, 12, 11, 13, 9)
    // R> group2 <- c(14, 15, 13, 16, 14)
    // R> group3 <- c(8, 9, 7, 10, 8)
    // R> data <- data.frame(value = c(group1, group2, group3),
    //                       group = factor(rep(1:3, each=5)))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value   Pr(>F)
    // group        2  118.0  59.000   17.26 0.000368 ***
    // Residuals   12   41.0   3.417

    def data = [
      'group1': [10, 12, 11, 13, 9],
      'group2': [14, 15, 13, 16, 14],
      'group3': [8, 9, 7, 10, 8]
    ]

    def result = Anova.aov(data)

    assertEquals(17.26, result.fValue, 0.01, 'F-value')
    assertEquals(0.000368, result.pValue, TOLERANCE, 'p-value')
  }

  @Test
  void testAnovaWithMatrixData() {
    // Test using Matrix and column names
    // Each column is a group
    def matrix = Matrix.builder()
      .matrixName('test')
      .columnNames(['group1', 'group2', 'group3'])
      .columns([
        [10, 12, 11, 13, 9],
        [14, 15, 13, 16, 14],
        [8, 9, 7, 10, 8]
      ])
      .build()

    def result = Anova.aov(matrix, ['group1', 'group2', 'group3'])

    assertEquals(17.26, result.fValue, 0.01, 'F-value from Matrix')
    assertEquals(0.000368, result.pValue, TOLERANCE, 'p-value from Matrix')
  }

  @Test
  void testAnovaTwoGroups() {
    // ANOVA with 2 groups should match t-test squared
    // R> group1 <- c(10, 12, 11, 13, 9)
    // R> group2 <- c(14, 15, 13, 16, 14)
    // R> summary(aov(value ~ group, data=data.frame(value=c(group1, group2), group=factor(rep(1:2, each=5)))))
    //             Df Sum Sq Mean Sq F value  Pr(>F)
    // group        1   40.0    40.0      18 0.00289 **
    // Residuals    8   17.8     2.225

    def data = [
      'group1': [10, 12, 11, 13, 9],
      'group2': [14, 15, 13, 16, 14]
    ]

    def result = Anova.aov(data)

    assertEquals(18.0, result.fValue, 0.1, 'F-value for 2-group ANOVA')
    assertEquals(0.00289, result.pValue, TOLERANCE, 'p-value for 2-group ANOVA')
  }

  @Test
  void testAnovaIdenticalGroups() {
    // Identical groups should give F≈0 and p≈1
    def data = [
      'group1': [10, 12, 11, 13, 9],
      'group2': [10, 12, 11, 13, 9],
      'group3': [10, 12, 11, 13, 9]
    ]

    def result = Anova.aov(data)

    assertTrue(result.fValue < 0.0001, 'F-value should be ≈0 for identical groups')
    assertTrue(result.pValue > 0.99, 'p-value should be ≈1 for identical groups')
  }

  @Test
  void testAnovaEvaluateMethod() {
    // Test the evaluate method with different alpha levels
    def data = [
      'group1': [10, 12, 11, 13, 9],
      'group2': [14, 15, 13, 16, 14],
      'group3': [8, 9, 7, 10, 8]
    ]

    def result = Anova.aov(data)

    // p-value = 0.000368, so should reject null at alpha=0.05
    assertTrue(result.evaluate(0.05), 'should reject null at alpha=0.05')
    assertTrue(result.evaluate(0.01), 'should reject null at alpha=0.01')
    assertTrue(result.evaluate(0.001), 'should reject null at alpha=0.001')
    assertFalse(result.evaluate(0.0001), 'should not reject null at alpha=0.0001')
  }

  @Test
  void testAnovaWithDifferentSampleSizes() {
    // Test with unbalanced design (different group sizes)
    // R> g1 <- c(5, 6, 7, 8)
    // R> g2 <- c(10, 11, 12)
    // R> g3 <- c(15, 16)
    // R> data <- data.frame(value = c(g1, g2, g3),
    //                       group = factor(c(rep(1,4), rep(2,3), rep(3,2))))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value  Pr(>F)
    // group        2 130.44  65.222   139.4 8.6e-06 ***
    // Residuals    6   2.81   0.468

    def data = [
      'group1': [5, 6, 7, 8],
      'group2': [10, 11, 12],
      'group3': [15, 16]
    ]

    def result = Anova.aov(data)

    assertEquals(139.4, result.fValue, 1.0, 'F-value for unbalanced design')
    assertTrue(result.pValue < 1e-4, 'p-value should be very small')
  }

  @Test
  void testAnovaInsufficientGroups() {
    // Should throw exception with less than 2 groups
    assertThrows(IllegalArgumentException) {
      Anova.aov(['group1': [1, 2, 3]])
    }
  }

  @Test
  void testAnovaToString() {
    def data = [
      'group1': [10, 12, 11],
      'group2': [14, 15, 13]
    ]

    def result = Anova.aov(data)
    String str = result.toString()

    assertTrue(str.contains('pValue'), 'toString should contain pValue')
    assertTrue(str.contains('fValue'), 'toString should contain fValue')
  }

  @Test
  void testAnovaFourGroups() {
    // Test with 4 groups
    // R> g1 <- c(5, 6, 7)
    // R> g2 <- c(8, 9, 10)
    // R> g3 <- c(11, 12, 13)
    // R> g4 <- c(14, 15, 16)
    // R> data <- data.frame(value = c(g1, g2, g3, g4),
    //                       group = factor(rep(1:4, each=3)))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value Pr(>F)
    // group        3  165.0    55.0      99 1.68e-07 ***
    // Residuals    8    4.4     0.5556

    def data = [
      'group1': [5, 6, 7],
      'group2': [8, 9, 10],
      'group3': [11, 12, 13],
      'group4': [14, 15, 16]
    ]

    def result = Anova.aov(data)

    assertEquals(99.0, result.fValue, 1.0, 'F-value for 4-group ANOVA')
    assertTrue(result.pValue < 1e-6, 'p-value should be very small')
  }
}
