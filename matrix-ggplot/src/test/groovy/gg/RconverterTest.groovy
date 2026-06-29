package gg

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gg.util.Rconverter

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
    String expected = "ggplot(mpg, aes(x: 'displ', y: expr { (it.hwy).log() })) + geom_point()"
    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testMathFunctionsConvertToSuffixMethods() {
    Map<String, String> expectedMethods = [
        log    : 'log',
        log10  : 'log10',
        sqrt   : 'sqrt',
        exp    : 'exp',
        abs    : 'abs',
        sin    : 'sin',
        cos    : 'cos',
        tan    : 'tan',
        asin   : 'asin',
        acos   : 'acos',
        atan   : 'atan',
        floor  : 'floor',
        ceil   : 'ceil',
        ceiling: 'ceil',
        round  : 'round'
    ]

    expectedMethods.each { String rName, String groovyMethod ->
      String input = "ggplot(mpg, aes(y = ${rName}(x)))"
      String expected = "ggplot(mpg, aes(y: expr { (it.x).${groovyMethod}() }))"
      assertEquals(expected, Rconverter.convert(input))
    }
  }

  @Test
  void testPiConvertsToNumberExtensionConstant() {
    String input = 'ggplot(mpg, aes(y = x + pi))'
    String expected = 'ggplot(mpg, aes(y: expr { it.x + se.alipsa.matrix.ext.NumberExtension.PI }))'

    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testNestedMathFunctionsConvertToSuffixMethods() {
    String input = 'ggplot(mpg, aes(y = sqrt(abs(x))))'
    String expected = 'ggplot(mpg, aes(y: expr { ((it.x).abs()).sqrt() }))'

    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testDeeplyNestedMathFunctionArgumentConvertsToSuffixMethod() {
    String input = 'ggplot(mpg, aes(y = log((x + 1) * 2)))'
    String expected = 'ggplot(mpg, aes(y: expr { ((it.x + 1) * 2).log() }))'

    assertEquals(expected, Rconverter.convert(input))
  }

  @Test
  void testTwoArgumentLogConvertsToSuffixMethodWithBase() {
    String literalBaseInput = 'ggplot(mpg, aes(y = log(x, 10)))'
    String literalBaseExpected = 'ggplot(mpg, aes(y: expr { (it.x).log(10) }))'
    assertEquals(literalBaseExpected, Rconverter.convert(literalBaseInput))

    String columnBaseInput = 'ggplot(mpg, aes(y = log(x, base_col)))'
    String columnBaseExpected = 'ggplot(mpg, aes(y: expr { (it.x).log(it.base_col) }))'
    assertEquals(columnBaseExpected, Rconverter.convert(columnBaseInput))
  }

  @Test
  void testUnsupportedMultiArgumentMathFunctionThrows() {
    UnsupportedOperationException exception = assertThrows(UnsupportedOperationException) {
      Rconverter.convert('ggplot(mpg, aes(y = round(x, 2)))')
    }

    assertTrue(exception.message.contains('round'))
    assertTrue(exception.message.contains('not supported'))
  }

  @Test
  void testZeroArgumentMathFunctionThrowsAccurateMessage() {
    UnsupportedOperationException exception = assertThrows(UnsupportedOperationException) {
      Rconverter.convert('ggplot(mpg, aes(y = sin()))')
    }

    assertTrue(exception.message.contains('sin() requires exactly one argument'))
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
