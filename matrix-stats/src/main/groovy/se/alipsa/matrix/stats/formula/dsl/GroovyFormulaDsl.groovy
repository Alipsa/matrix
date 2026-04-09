package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic

/**
 * Closure delegate for Groovy-native formula expressions.
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
