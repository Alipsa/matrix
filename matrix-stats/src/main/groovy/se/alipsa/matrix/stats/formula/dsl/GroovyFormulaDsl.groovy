package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic

/**
 * Closure delegate for Groovy-native formula expressions.
 *
 * <p>This class is intentionally dynamic: it relies on {@code propertyMissing} and
 * operator dispatch for concise column references, so full {@code @CompileStatic}
 * support is not required for the operator DSL.</p>
 */
class GroovyFormulaDsl {

  /**
   * Evaluates a Groovy formula closure.
   *
   * @param closure the formula closure
   * @return the formula specification produced by the closure
   */
  @CompileDynamic
  static GroovyFormulaSpec evaluate(
      @DelegatesTo(value = GroovyFormulaDsl, strategy = Closure.DELEGATE_FIRST)
      Closure<GroovyFormulaSpec> closure
  ) {
    if (closure == null) {
      throw new IllegalArgumentException('Formula closure cannot be null')
    }
    GroovyFormulaDsl dsl = new GroovyFormulaDsl()
    Closure cloned = closure.rehydrate(dsl, closure.owner, closure.thisObject)
    cloned.resolveStrategy = Closure.DELEGATE_FIRST
    def result = cloned.call()
    if (!(result instanceof GroovyFormulaSpec)) {
      throw new IllegalArgumentException('Formula closure must return a Groovy formula expression such as y | x + group')
    }
    result as GroovyFormulaSpec
  }

  /**
   * Resolves bare column references as term references.
   *
   * @param name the unresolved property name
   * @return a term reference with the same name
   */
  TermRef propertyMissing(String name) {
    new TermRef(name)
  }

  /**
   * Returns the dot placeholder for later model-frame expansion.
   *
   * @return the dot term expression
   */
  AllTermsExpr getAll() {
    new AllTermsExpr()
  }

  /**
   * Returns an intercept-removal expression that can participate in RHS term chaining.
   *
   * @return the no-intercept term expression
   */
  NoInterceptExpr getNoIntercept() {
    new NoInterceptExpr()
  }

  /**
   * Creates a term reference for a column name that is not a legal Groovy identifier.
   *
   * @param name the column name
   * @return a quoted term reference
   */
  TermRef col(String name) {
    new TermRef(name, true)
  }

  /**
   * Creates a logarithm transform term.
   *
   * @param expression the transformed input
   * @return the transform term
   */
  TermExpr log(TermExpr expression) {
    new FunctionTermExpr('log', [expression])
  }

  /**
   * Creates a square-root transform term.
   *
   * @param expression the transformed input
   * @return the transform term
   */
  TermExpr sqrt(TermExpr expression) {
    new FunctionTermExpr('sqrt', [expression])
  }

  /**
   * Creates an exponential transform term.
   *
   * @param expression the transformed input
   * @return the transform term
   */
  TermExpr exp(TermExpr expression) {
    new FunctionTermExpr('exp', [expression])
  }

  /**
   * Creates a polynomial expansion term.
   *
   * @param expression the base term
   * @param degree the polynomial degree
   * @return the polynomial term
   */
  TermExpr poly(TermExpr expression, Number degree) {
    if (degree == null) {
      throw new IllegalArgumentException('degree cannot be null')
    }
    new FunctionTermExpr('poly', [expression, degree])
  }

  /**
   * Creates a smooth term using the default basis size.
   *
   * @param expression the smooth input
   * @return the smooth term
   */
  TermExpr smooth(TermExpr expression) {
    new FunctionTermExpr('s', [expression])
  }

  /**
   * Creates a smooth term with an explicit basis size.
   *
   * @param expression the smooth input
   * @param df the requested degrees of freedom
   * @return the smooth term
   */
  TermExpr smooth(TermExpr expression, Number df) {
    if (df == null) {
      throw new IllegalArgumentException('df cannot be null')
    }
    new FunctionTermExpr('s', [expression, df])
  }

  /**
   * Alias for {@link #smooth(TermExpr)}.
   *
   * @param expression the smooth input
   * @return the smooth term
   */
  TermExpr s(TermExpr expression) {
    smooth(expression)
  }

  /**
   * Alias for {@link #smooth(TermExpr, Number)}.
   *
   * @param expression the smooth input
   * @param df the requested degrees of freedom
   * @return the smooth term
   */
  TermExpr s(TermExpr expression, Number df) {
    smooth(expression, df)
  }

  /**
   * Creates an identity expression term from an arithmetic-expression closure.
   *
   * <p>The closure is evaluated against {@link ExpressionDsl}, not the outer formula DSL, so
   * operators such as {@code +} and {@code *} build arithmetic expressions instead of formula
   * terms. For example: {@code y | I { (x + 1) * z }}.</p>
   *
   * @param expression the arithmetic-expression closure
   * @return the identity term
   */
  @CompileDynamic
  @SuppressWarnings('MethodName')
  TermExpr I(
    @DelegatesTo(value = ExpressionDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<?> expression
  ) {
    new FunctionTermExpr('I', [ExpressionDsl.evaluate(expression)])
  }

  /**
   * Creates an interaction expression from two or more terms.
   *
   * @param first the first term
   * @param second the second term
   * @param rest additional interaction terms
   * @return an interaction expression
   */
  TermExpr interaction(TermExpr first, TermExpr second, TermExpr... rest) {
    List<TermExpr> terms = [first, second]
    terms.addAll(rest.toList())
    new InteractionExpr(terms)
  }

  /**
   * Rejects one-argument interactions with a clear message.
   *
   * @param first the only term
   * @return never returns normally
   */
  @SuppressWarnings('UnusedMethodParameter')
  TermExpr interaction(TermExpr first) {
    throw new IllegalArgumentException('interaction(...) requires at least two terms')
  }

  /**
   * Rejects empty interactions with a clear message.
   *
   * @return never returns normally
   */
  TermExpr interaction() {
    throw new IllegalArgumentException('interaction(...) requires at least two terms')
  }
}
