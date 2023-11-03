import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.stats.regression.LinearRegression

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.*

class LinearRegressionTest {

  /**
   * similar code in R
   * library("se.alipsa:rideutils")
   * x <- c(2.7, 3, 5, 7, 9, 11, 14)
   * y <- c(4, 5, 7, 10.8, 15, 20, 40)
   *
   * model <- lm(y ~ x)
   * print(coef(model))
   * print(predict(model, newdata = data.frame(x = 13)))
   * viewPlot({
   *   plot(x,y)
   *   abline(model)
   * })
   */
  @Test
  void testLinearRegression() {
    def x = [2.7, 3, 5, 7, 9, 11, 14]
    def y = [4, 5, 7, 10.8, 15, 20, 40]
    def model = new LinearRegression(x, y)

    assertEquals(-6.23971066, model.intercept.setScale(8, RoundingMode.HALF_EVEN))
    assertEquals(2.81388732, model.slope.setScale(8, RoundingMode.HALF_EVEN))
    assertEquals(30.34082454, model.predict(13).setScale(8, RoundingMode.HALF_EVEN))
  }

  /**
   * similar code in R
   * <code><pre>
   *   library("se.alipsa:rideutils")
   *   x <- c(2, 3, 5, 7, 9, 11, 14)
   *   y <- c(4.02, 5.44, 7.12, 10.88, 15.10, 20.91, 26.02)
   *   model <- lm(y ~ x)
   *   print(model)
   *   cf <- coef(model)
   *   print(cf)
   *   print(paste("R2 =", summary(model)$r.squared))
   *   print(paste("intercept std err =", sqrt(diag(vcov(model)))[1]))
   *   print(paste("slope std err =", sqrt(diag(vcov(model)))[2]))
   *   viewPlot({
   *     plot(x,y)
   *     abline(model)
   *   })
   *   print(predict(model, newdata = data.frame(x = c(13, 15))))
   * </pre></code>
   */
  @Test
  void testLinearRegressionFromMatrix() {
    def table = Matrix.create(
      x: [2, 3, 5, 7, 9, 11, 14],
      y: [4.02, 5.44, 7.12, 10.88, 15.10, 20.91, 26.02]
    )
    def model = new LinearRegression(table, 'x', 'y')

    assertEquals(-0.98130982, model.getIntercept(8))
    assertEquals(1.88939547, model.getSlope(8))
    assertEquals("Y = 1.89X - 0.98", model.toString())
    assertEquals(0.98034908, model.getRsquared(8))
    assertEquals(0.99577407, model.getInterceptStdErr(8))
    assertEquals(0.11962969, model.getSlopeStdErr(8))

    def predictions = model.predict([13, 15], 8)
    assertEquals(23.58083123, predictions[0])
    assertEquals(27.35962217, predictions[1])
    assertEquals(23.58083123, model.predict(13, 8))
    assertEquals(model.predict([-1, 15])[0], model.predict(-1))
  }
}
