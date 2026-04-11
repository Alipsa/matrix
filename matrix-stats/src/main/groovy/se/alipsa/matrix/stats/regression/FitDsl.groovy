package se.alipsa.matrix.stats.regression

import groovy.transform.CompileDynamic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.dsl.GroovyFormulaDsl
import se.alipsa.matrix.stats.formula.dsl.GroovyFormulaSpec

/**
 * Groovy-native convenience entry points for fitting built-in regression models.
 *
 * <p>These helpers evaluate a {@link ModelFrame} from the formula DSL and then delegate
 * to the registered fit method in {@link FitRegistry}. They are intended for static-import
 * usage such as {@code lm(data) { y | x + group }}.</p>
 */
final class FitDsl {

  private FitDsl() {
  }

  /**
   * Fits a linear model using the Groovy formula DSL.
   *
   * @param data the input dataset
   * @param formula the Groovy formula closure
   * @return the fitted result
   */
  static FitResult lm(
    Matrix data,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    fit('lm', data, formula)
  }

  /**
   * Fits a loess model using the Groovy formula DSL and default options.
   *
   * @param data the input dataset
   * @param formula the Groovy formula closure
   * @return the fitted result
   */
  static FitResult loess(
    Matrix data,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    fit('loess', data, formula)
  }

  /**
   * Fits a loess model using explicit loess options.
   *
   * @param data the input dataset
   * @param options the loess options
   * @param formula the Groovy formula closure
   * @return the fitted result
   */
  static FitResult loess(
    Matrix data,
    LoessOptions options,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    fit('loess', data, formula, options)
  }

  /**
   * Fits a generalized additive model using the Groovy formula DSL and default options.
   *
   * @param data the input dataset
   * @param formula the Groovy formula closure
   * @return the fitted result
   */
  static FitResult gam(
    Matrix data,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    fit('gam', data, formula)
  }

  /**
   * Fits a generalized additive model using explicit GAM options.
   *
   * @param data the input dataset
   * @param options the GAM options
   * @param formula the Groovy formula closure
   * @return the fitted result
   */
  static FitResult gam(
    Matrix data,
    GamOptions options,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    fit('gam', data, formula, options)
  }

  @CompileDynamic
  private static FitResult fit(
    String methodName,
    Matrix data,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula
  ) {
    FitRegistry.instance().get(methodName).fit(ModelFrame.of(data, formula).evaluate())
  }

  @CompileDynamic
  private static FitResult fit(
    String methodName,
    Matrix data,
    @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<GroovyFormulaSpec> formula,
    FitOptions options
  ) {
    FitRegistry.instance().get(methodName).fit(ModelFrame.of(data, formula).evaluate(), options)
  }
}
