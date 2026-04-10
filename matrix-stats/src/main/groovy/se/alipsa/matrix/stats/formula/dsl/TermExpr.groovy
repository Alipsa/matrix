package se.alipsa.matrix.stats.formula.dsl

/**
 * Base type for Groovy-native formula DSL term expressions.
 */
abstract class TermExpr {

  /**
   * Adds a term to this expression.
   *
   * @param other the term to add
   * @return a combined term expression
   */
  TermExpr plus(TermExpr other) {
    new BinaryTermExpr(this, '+', requireTerm(other, 'other'))
  }

  /**
   * Removes a term from this expression.
   *
   * @param other the term to remove
   * @return a subtraction term expression
   */
  TermExpr minus(TermExpr other) {
    new BinaryTermExpr(this, '-', requireTerm(other, 'other'))
  }

  /**
   * Creates an interaction with another term via the {@code %} operator.
   * Groovy 5 dispatches {@code %} to {@code remainder()}.
   *
   * @param other the term to interact with
   * @return an interaction term expression
   */
  TermExpr remainder(TermExpr other) {
    new InteractionExpr([this, requireTerm(other, 'other')])
  }

  /**
   * Expands shorthand interactions up to the supplied degree.
   *
   * @param degree the positive integer interaction degree
   * @return an expansion expression
   */
  TermExpr power(Number degree) {
    new ExpansionExpr(this, degree)
  }

  /**
   * Render the term using R-style formula syntax.
   *
   * @return the rendered formula fragment
   */
  abstract String render()

  protected static TermExpr requireTerm(TermExpr term, String label) {
    if (term == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    term
  }
}
