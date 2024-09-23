import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.*
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.stats.Student

import static org.junit.jupiter.api.Assertions.*

class StudentTest {

  /**
   * Equivalent code in R
   * <code><pre>
   * flowers <- iris[iris$Species == 'setosa' | iris$Species == 'virginica',]
   * print(t.test(Petal.Length ~ Species, data = flowers))
   * </pre></code>
   * Another way to express the same thing is:
   * <code><pre>
   * setosa <- iris[iris$Species == 'setosa',]
   * virginica <- iris[iris$Species == 'virginica',]
   * print(t.test(setosa[, 'Petal.Length'], virginica[, 'Petal.Length']))
   * </pre></code>
   */
  @Test
  void testTwoSample() {
    def iris = Dataset.iris()
    def speciesIdx = iris.columnIndex("Species")
    def setosa = iris.subset {
      it[speciesIdx] == 'setosa'
    }
    def virginica = iris.subset {
      it[speciesIdx] == 'virginica'
    }
    Student.Result result = Student.tTest(setosa['Petal Length'], virginica['Petal Length'], false)
    println(result)
    assertEquals(-49.98618626, result.getT(8), "t value")
    println("var1 = ${result.var1}, var2 = ${result.var2}")
    assertEquals(58.60939455, result.getDf(8), "Degrees of freedom")
    assertEquals(0.17366400, result.getSd1(8), "sd1")
    assertEquals(0.55189470, result.getSd2(8), "sd2")
    assertEquals(0.55189470, result.getSd2(8), "sd2")
    assertEquals(9.269628E-50, result.p, 0.0000001)
  }

  /**
   * Equivalent in R
   * <code><pre>
   * plantHeights <- c(14, 14, 16, 13, 12, 17, 15, 14, 15, 13, 15, 14)
   * print(t.test(plantHeights, mu=15))
   *
   * > One Sample t-test
   * >
   * > data:  plantHeights
   * > t = -1.6848, df = 11, p-value = 0.1201
   * > alternative hypothesis: true mean is not equal to 15
   * > 95 percent confidence interval:
   * > 13.46244 15.20423
   * > sample estimates:
   * > mean of x
   * > 14.33333
   * </pre></code>
   */
  @Test
  void testOneSample() {
    def plantHeights = [14, 14, 16, 13, 12, 17, 15, 14, 15, 13, 15, 14]
    def t = Student.tTest(plantHeights, 15)
    println(t)
    assertEquals(-1.68484708, t.getT(8))
    assertEquals(11, t.getDf())
    assertEquals(0.12014461, t.getP(8))
    assertEquals(14.33333333, t.getMean(8))
    assertEquals(1.87878788, t.getVar(8))
  }

  /**
   * Equivalent code in R:
   * <code><pre>
   * data <- data.frame(score = c(85 ,85, 78, 78, 92, 94, 91, 85, 72, 97,
   * 84, 95, 99, 80, 90, 88, 95, 90, 96, 89,
   * 84, 88, 88, 90, 92, 93, 91, 85, 80, 93,
   * 97, 100, 93, 91, 90, 87, 94, 83, 92, 95),
   * group = c(rep('pre', 20), rep('post', 20)))
   * t.test(score ~ group, data = data, paired = TRUE)
   *
   * Paired t-test
   *
   * data:  score by group
   * t = 1.588, df = 19, p-value = 0.1288
   * alternative hypothesis: true difference in means is not equal to 0
   * 95 percent confidence interval:
   * -0.6837307  4.9837307
   * sample estimates:
   * mean of the differences
   * 2.15
   * </pre>
   * </code>
   */
  @Test
  void testPaired() {
    def data = Matrix.builder().data(
        score: [85, 85, 78, 78, 92, 94, 91, 85, 72, 97,
                84, 95, 99, 80, 90, 88, 95, 90, 96, 89,
                84, 88, 88, 90, 92, 93, 91, 85, 80, 93,
                97, 100, 93, 91, 90, 87, 94, 83, 92, 95],
        group: ['pre'] * 20 + ['post'] * 20)
        .types(Integer, String)
        .build()
    def pre = data.subset('group', { it == 'pre' })
    def post = data.subset('group', { it == 'post' })
    def result = Student.pairedTTest(post['score'], pre['score'])
    println(result)
    assertEquals(1.58801321, result.getT(8), 't statistic')
    assertEquals(19, result.getDf() as Integer, 'Degrees of freedom')
    assertEquals(0.128785661, result.getP(9), 'P value')
    assertEquals(2.15, result.mean1 - result.mean2, 'mean of the differences')
  }
}
