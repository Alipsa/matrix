package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.util.Rconverter

import static org.junit.jupiter.api.Assertions.assertEquals

class RconverterTest {

  @Test
  void testBasicConversion() {
    String input = 'ggplot(mpg, aes(x = displ, y = hwy)) + geom_point() + labs(title = "My Plot")'
    String expected = "ggplot(mpg, aes(x: 'displ', y: 'hwy')) + geom_point() + labs(title: \"My Plot\")"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testAesConstants() {
    String input = 'ggplot(mpg, aes(x = displ, y = hwy, color = "red", size = 2)) + geom_point()'
    String expected = "ggplot(mpg, aes(x: 'displ', y: 'hwy', color: I(\"red\"), size: I(2))) + geom_point()"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testFormulaConversion() {
    String input = 'ggplot(mpg, aes(displ, hwy)) + geom_smooth(method = "lm", formula = y ~ x)'
    String expected = "ggplot(mpg, aes('displ', 'hwy')) + geom_smooth(method: \"lm\", formula: 'y ~ x')"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testAfterStatConversion() {
    String input = 'ggplot(mpg, aes(x = displ, y = after_stat(count))) + geom_bar()'
    String expected = "ggplot(mpg, aes(x: 'displ', y: after_stat('count'))) + geom_bar()"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testExpressionConversion() {
    String input = 'ggplot(mpg, aes(x = displ, y = log(hwy))) + geom_point()'
    String expected = "ggplot(mpg, aes(x: 'displ', y: expr { Math.log(it.hwy) })) + geom_point()"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testThemeDotKey() {
    String input = 'ggplot(mpg, aes(x = displ, y = hwy)) + theme(axis.text = element_text(size = 12))'
    String expected = "ggplot(mpg, aes(x: 'displ', y: 'hwy')) + theme('axis.text': element_text(size: 12))"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testFunctionListConversion() {
    String input = '''geom_mean <- function() {
  list(
    stat_summary(fun = "mean", geom = "bar", fill = "grey70"),
    stat_summary(fun.data = "mean_cl_normal", geom = "errorbar", width = 0.4)
  )
}
ggplot(mpg, aes(class, cty)) + geom_mean()
ggplot(mpg, aes(drv, cty)) + geom_mean()'''

    String expected = '''def geom_mean() {
  [
    stat_summary(fun: "mean", geom: "bar", fill: "grey70"),
    stat_summary('fun.data': "mean_cl_normal", geom: "errorbar", width: 0.4)
  ]
}
ggplot(mpg, aes('class', 'cty')) + geom_mean()
ggplot(mpg, aes('drv', 'cty')) + geom_mean()'''

    assertEquals(expected, Rconverter.convert(input))
  }
}
