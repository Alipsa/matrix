package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.math.MathContext

/**
 * Internal parsing and normalization support for formula handling.
 */
@CompileStatic
@PackageScope
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
final class FormulaSupport {

  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64

  private FormulaSupport() {
  }

  static ParsedFormula parse(String formula, String defaultResponse) {
    if (formula == null || formula.isBlank()) {
      throw new FormulaParseException('Formula cannot be null or blank', 0)
    }
    new Parser(formula).parseFormula(defaultResponse)
  }

  static ParsedFormula update(String baseFormula, String updateFormula) {
    ParsedFormula base = parse(baseFormula, null)
    String normalizedUpdate = updateFormula.contains('~') ? updateFormula : ". ~ ${updateFormula}"
    ParsedFormula updater = parse(normalizedUpdate, null)
    FormulaExpression response = replaceDots(updater.response, base.response)
    FormulaExpression predictors = replaceDots(updater.predictors, base.predictors)
    new ParsedFormula("${baseFormula} | update ${updateFormula}", response, predictors)
  }

  static NormalizedFormula normalize(ParsedFormula parsedFormula) {
    FormulaExpression normalizedResponse = normalizeResponse(parsedFormula.response)
    boolean includeIntercept = true
    Set<FormulaTerm> terms = [] as LinkedHashSet<FormulaTerm>

    for (SignedExpression component : flattenSignedExpressions(parsedFormula.predictors, 1)) {
      Boolean interceptDirective = interceptDirective(component.expression)
      if (interceptDirective != null) {
        includeIntercept = component.sign > 0 ? interceptDirective : !interceptDirective
        continue
      }

      Set<FormulaTerm> componentTerms = expandTerms(component.expression)
      if (component.sign > 0) {
        terms.addAll(componentTerms)
      } else {
        terms.removeAll(componentTerms)
      }
    }

    new NormalizedFormula(normalizedResponse, includeIntercept, sortTerms(terms))
  }

  static int operatorPrecedence(String operator) {
    switch (operator) {
      case ':':
        return 4
      case '^':
        return 3
      case '*':
      case '/':
        return 2
      case '+':
      case '-':
        return 1
      default:
        return 0
    }
  }

  private static FormulaExpression normalizeResponse(FormulaExpression expression) {
    Set<FormulaTerm> responseTerms = expandTerms(expression)
    if (responseTerms.size() != 1) {
      throw new FormulaParseException('Multiple responses are not supported; use a single response expression', expression.start)
    }
    FormulaTerm term = responseTerms.first()
    if (term.factors.size() != 1 || term.factors[0] instanceof FormulaExpression.Dot) {
      throw new FormulaParseException('Response must be a single variable or transform', expression.start)
    }
    term.factors[0]
  }

  private static FormulaExpression replaceDots(FormulaExpression expression, FormulaExpression replacement) {
    if (expression instanceof FormulaExpression.Dot) {
      return replacement
    }
    if (expression instanceof FormulaExpression.Grouping) {
      FormulaExpression.Grouping grouping = expression as FormulaExpression.Grouping
      return new FormulaExpression.Grouping(replaceDots(grouping.expression, replacement), grouping.start, grouping.end)
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      return new FormulaExpression.Unary(unary.operator, replaceDots(unary.expression, replacement), unary.start, unary.end)
    }
    if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      return new FormulaExpression.Binary(
        binary.operator,
        replaceDots(binary.left, replacement),
        replaceDots(binary.right, replacement),
        binary.start,
        binary.end
      )
    }
    if (expression instanceof FormulaExpression.FunctionCall) {
      FormulaExpression.FunctionCall functionCall = expression as FormulaExpression.FunctionCall
      return new FormulaExpression.FunctionCall(
        functionCall.name,
        functionCall.arguments.collect { FormulaExpression argument -> replaceDots(argument, replacement) },
        functionCall.start,
        functionCall.end
      )
    }
    expression
  }

  private static List<SignedExpression> flattenSignedExpressions(FormulaExpression expression, int sign) {
    if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      if (binary.operator == '+') {
        return flattenSignedExpressions(binary.left, sign) + flattenSignedExpressions(binary.right, sign)
      }
      if (binary.operator == '-') {
        return flattenSignedExpressions(binary.left, sign) + flattenSignedExpressions(binary.right, -sign)
      }
    }
    [new SignedExpression(sign, expression)]
  }

  private static Boolean interceptDirective(FormulaExpression expression) {
    if (expression instanceof FormulaExpression.NumberLiteral) {
      BigDecimal value = (expression as FormulaExpression.NumberLiteral).value
      if (value == BigDecimal.ZERO) {
        return false
      }
      if (value == BigDecimal.ONE) {
        return true
      }
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      if (unary.operator == '-' && unary.expression instanceof FormulaExpression.NumberLiteral) {
        BigDecimal value = (unary.expression as FormulaExpression.NumberLiteral).value
        if (value == BigDecimal.ONE) {
          return false
        }
      }
    }
    null
  }

  private static Set<FormulaTerm> expandTerms(FormulaExpression expression) {
    if (expression instanceof FormulaExpression.Grouping) {
      return expandTerms((expression as FormulaExpression.Grouping).expression)
    }
    if (expression instanceof FormulaExpression.Variable ||
      expression instanceof FormulaExpression.Dot ||
      expression instanceof FormulaExpression.FunctionCall) {
      return [FormulaTerm.of(expression)] as LinkedHashSet<FormulaTerm>
    }
    if (expression instanceof FormulaExpression.NumberLiteral) {
      return [] as LinkedHashSet<FormulaTerm>
    }
    if (expression instanceof FormulaExpression.Unary) {
      FormulaExpression.Unary unary = expression as FormulaExpression.Unary
      if (unary.operator == '+') {
        return expandTerms(unary.expression)
      }
      if (unary.operator == '-') {
        if (interceptDirective(unary) != null) {
          return [] as LinkedHashSet<FormulaTerm>
        }
        throw new FormulaParseException("Unary '${unary.operator}' is only supported for intercept control", unary.start)
      }
    }
    if (!(expression instanceof FormulaExpression.Binary)) {
      throw new FormulaParseException("Unsupported formula expression: ${expression.class.simpleName}", expression.start)
    }

    FormulaExpression.Binary binary = expression as FormulaExpression.Binary
    Set<FormulaTerm> leftTerms = expandTerms(binary.left)
    Set<FormulaTerm> rightTerms = expandTerms(binary.right)

    switch (binary.operator) {
      case '+':
        return combineSets(leftTerms, rightTerms)
      case '-':
        return subtractSets(leftTerms, rightTerms)
      case ':':
        return interactions(leftTerms, rightTerms)
      case '*':
        return combineSets(combineSets(leftTerms, rightTerms), interactions(leftTerms, rightTerms))
      case '/':
        return combineSets(leftTerms, interactions(leftTerms, rightTerms))
      case '^':
        return powerTerms(binary.left, binary.right)
      default:
        throw new FormulaParseException("Unsupported formula operator '${binary.operator}'", binary.start)
    }
  }

  private static Set<FormulaTerm> powerTerms(FormulaExpression left, FormulaExpression right) {
    int exponent = positiveIntegerExponent(right)
    Set<FormulaTerm> baseTerms = expandTerms(left)
    if (baseTerms.isEmpty()) {
      return [] as LinkedHashSet<FormulaTerm>
    }

    List<FormulaTerm> orderedTerms = sortTerms(baseTerms)
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    int maxDegree = Math.min(exponent, orderedTerms.size())
    for (int degree = 1; degree <= maxDegree; degree++) {
      appendCombinations(orderedTerms, degree, 0, [] as List<FormulaTerm>, result)
    }
    result
  }

  private static int positiveIntegerExponent(FormulaExpression expression) {
    if (!(expression instanceof FormulaExpression.NumberLiteral)) {
      throw new FormulaParseException('Interaction expansion (^) requires a positive integer exponent', expression.start)
    }
    FormulaExpression.NumberLiteral numberLiteral = expression as FormulaExpression.NumberLiteral
    try {
      BigDecimal integerValue = numberLiteral.value.round(MATH_CONTEXT)
      if (integerValue.scale() > 0 && integerValue.stripTrailingZeros().scale() > 0) {
        throw new NumberFormatException(numberLiteral.sourceText)
      }
      int exponent = integerValue.intValueExact()
      if (exponent < 1) {
        throw new NumberFormatException(numberLiteral.sourceText)
      }
      exponent
    } catch (ArithmeticException | NumberFormatException ignored) {
      throw new FormulaParseException('Interaction expansion (^) requires a positive integer exponent', expression.start)
    }
  }

  private static void appendCombinations(
    List<FormulaTerm> terms,
    int targetSize,
    int startIndex,
    List<FormulaTerm> current,
    Set<FormulaTerm> result
  ) {
    if (current.size() == targetSize) {
      result << FormulaTerm.combine(current)
      return
    }
    for (int i = startIndex; i <= terms.size() - (targetSize - current.size()); i++) {
      List<FormulaTerm> next = new ArrayList<>(current)
      next << terms[i]
      appendCombinations(terms, targetSize, i + 1, next, result)
    }
  }

  private static Set<FormulaTerm> interactions(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    for (FormulaTerm left : leftTerms) {
      for (FormulaTerm right : rightTerms) {
        result << FormulaTerm.combine([left, right])
      }
    }
    result
  }

  private static Set<FormulaTerm> combineSets(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    result.addAll(leftTerms)
    result.addAll(rightTerms)
    result
  }

  private static Set<FormulaTerm> subtractSets(Set<FormulaTerm> leftTerms, Set<FormulaTerm> rightTerms) {
    Set<FormulaTerm> result = [] as LinkedHashSet<FormulaTerm>
    result.addAll(leftTerms)
    result.removeAll(rightTerms)
    result
  }

  private static List<FormulaTerm> sortTerms(Collection<FormulaTerm> terms) {
    terms.toList().sort { FormulaTerm a, FormulaTerm b ->
      int factorOrder = a.factors.size() <=> b.factors.size()
      factorOrder != 0 ? factorOrder : a.asFormulaString() <=> b.asFormulaString()
    } as List<FormulaTerm>
  }

  @CompileStatic
  private static final class SignedExpression {
    final int sign
    final FormulaExpression expression

    SignedExpression(int sign, FormulaExpression expression) {
      this.sign = sign
      this.expression = expression
    }
  }

  @CompileStatic
  private enum TokenType {
    IDENTIFIER,
    BACKTICK_IDENTIFIER,
    NUMBER,
    TILDE,
    PLUS,
    MINUS,
    COLON,
    STAR,
    SLASH,
    CARET,
    LPAREN,
    RPAREN,
    COMMA,
    DOT,
    EOF
  }

  @CompileStatic
  private static final class FormulaToken {
    final TokenType type
    final String text
    final int start
    final int end

    FormulaToken(TokenType type, String text, int start, int end) {
      this.type = type
      this.text = text
      this.start = start
      this.end = end
    }
  }

  @CompileStatic
  private static final class Tokenizer {
    private final String source
    private int index = 0

    Tokenizer(String source) {
      this.source = source
    }

    List<FormulaToken> tokenize() {
      List<FormulaToken> tokens = []
      while (index < source.length()) {
        char current = source.charAt(index)
        if (Character.isWhitespace(current)) {
          index++
          continue
        }

        int start = index
        switch (current) {
          case '~':
            tokens << new FormulaToken(TokenType.TILDE, '~', start, ++index)
            continue
          case '+':
            tokens << new FormulaToken(TokenType.PLUS, '+', start, ++index)
            continue
          case '-':
            tokens << new FormulaToken(TokenType.MINUS, '-', start, ++index)
            continue
          case ':':
            tokens << new FormulaToken(TokenType.COLON, ':', start, ++index)
            continue
          case '*':
            tokens << new FormulaToken(TokenType.STAR, '*', start, ++index)
            continue
          case '/':
            tokens << new FormulaToken(TokenType.SLASH, '/', start, ++index)
            continue
          case '^':
            tokens << new FormulaToken(TokenType.CARET, '^', start, ++index)
            continue
          case '(':
            tokens << new FormulaToken(TokenType.LPAREN, '(', start, ++index)
            continue
          case ')':
            tokens << new FormulaToken(TokenType.RPAREN, ')', start, ++index)
            continue
          case ',':
            tokens << new FormulaToken(TokenType.COMMA, ',', start, ++index)
            continue
          case '.':
            tokens << new FormulaToken(TokenType.DOT, '.', start, ++index)
            continue
          case '`':
            tokens << backtickIdentifier(start)
            continue
          default:
            break
        }

        if (Character.isDigit(current)) {
          tokens << numberLiteral(start)
          continue
        }
        if (Character.isLetter(current) || current == '_') {
          tokens << identifier(start)
          continue
        }

        throw parseError("Unexpected character '${current}'", start)
      }
      tokens << new FormulaToken(TokenType.EOF, '', source.length(), source.length())
      tokens
    }

    private FormulaToken identifier(int start) {
      while (index < source.length()) {
        char value = source.charAt(index)
        if (!(Character.isLetterOrDigit(value) || value == '_')) {
          break
        }
        index++
      }
      new FormulaToken(TokenType.IDENTIFIER, source.substring(start, index), start, index)
    }

    private FormulaToken numberLiteral(int start) {
      while (index < source.length() && Character.isDigit(source.charAt(index))) {
        index++
      }
      if (index < source.length() && source.charAt(index) == '.') {
        index++
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
          index++
        }
      }
      new FormulaToken(TokenType.NUMBER, source.substring(start, index), start, index)
    }

    private FormulaToken backtickIdentifier(int start) {
      index++ // opening backtick
      StringBuilder builder = new StringBuilder()
      while (index < source.length() && source.charAt(index) != '`') {
        builder.append(source.charAt(index))
        index++
      }
      if (index >= source.length()) {
        throw parseError('Unterminated backtick identifier', start)
      }
      index++ // closing backtick
      new FormulaToken(TokenType.BACKTICK_IDENTIFIER, builder.toString(), start, index)
    }

    private FormulaParseException parseError(String message, int position) {
      FormulaSupport.parseError(source, position, message)
    }
  }

  @CompileStatic
  private static final class Parser {
    private final String source
    private final List<FormulaToken> tokens
    private int index = 0

    Parser(String source) {
      this.source = source
      this.tokens = new Tokenizer(source).tokenize()
    }

    ParsedFormula parseFormula(String defaultResponse) {
      FormulaExpression firstSide = parseExpression()
      if (match(TokenType.TILDE)) {
        if (peek().type == TokenType.EOF) {
          throw parseError('Formula is missing a right-hand side', peek().start)
        }
        FormulaExpression predictors = parseExpression()
        expect(TokenType.EOF, 'Unexpected trailing input after formula')
        return new ParsedFormula(source, firstSide, predictors)
      }

      if (defaultResponse == null || defaultResponse.isBlank()) {
        throw parseError("Formula must contain '~' unless a default response is provided", peek().start)
      }

      FormulaExpression response = new Parser(defaultResponse).parseSideOnly()
      expect(TokenType.EOF, 'Unexpected trailing input after predictor expression')
      new ParsedFormula("${defaultResponse} ~ ${source}", response, firstSide)
    }

    private FormulaExpression parseSideOnly() {
      FormulaExpression expression = parseExpression()
      expect(TokenType.EOF, 'Unexpected trailing input in formula side')
      expression
    }

    private FormulaExpression parseExpression() {
      parseAdditive()
    }

    private FormulaExpression parseAdditive() {
      FormulaExpression expression = parseMultiplicative()
      while (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
        FormulaToken operator = advance()
        FormulaExpression right = parseMultiplicative()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseMultiplicative() {
      FormulaExpression expression = parsePower()
      while (peek().type == TokenType.STAR || peek().type == TokenType.SLASH) {
        FormulaToken operator = advance()
        FormulaExpression right = parsePower()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parsePower() {
      FormulaExpression expression = parseInteraction()
      while (peek().type == TokenType.CARET) {
        FormulaToken operator = advance()
        FormulaExpression right = parseInteraction()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseInteraction() {
      FormulaExpression expression = parseUnary()
      while (peek().type == TokenType.COLON) {
        FormulaToken operator = advance()
        FormulaExpression right = parseUnary()
        expression = new FormulaExpression.Binary(operator.text, expression, right, expression.start, right.end)
      }
      expression
    }

    private FormulaExpression parseUnary() {
      if (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
        FormulaToken operator = advance()
        FormulaExpression right = parseUnary()
        return new FormulaExpression.Unary(operator.text, right, operator.start, right.end)
      }
      parsePrimary()
    }

    private FormulaExpression parsePrimary() {
      FormulaToken token = advance()
      switch (token.type) {
        case TokenType.IDENTIFIER:
          if (match(TokenType.LPAREN)) {
            return parseFunctionCall(token)
          }
          return new FormulaExpression.Variable(token.text, false, token.start, token.end)
        case TokenType.BACKTICK_IDENTIFIER:
          return new FormulaExpression.Variable(token.text, true, token.start, token.end)
        case TokenType.NUMBER:
          return new FormulaExpression.NumberLiteral(new BigDecimal(token.text), token.text, token.start, token.end)
        case TokenType.DOT:
          return new FormulaExpression.Dot(token.start, token.end)
        case TokenType.LPAREN:
          FormulaExpression inner = parseExpression()
          FormulaToken closing = expect(TokenType.RPAREN, "Expected ')' to close grouped expression")
          return new FormulaExpression.Grouping(inner, token.start, closing.end)
        default:
          throw parseError("Unexpected token '${token.text ?: token.type.name()}'", token.start)
      }
    }

    private FormulaExpression parseFunctionCall(FormulaToken nameToken) {
      List<FormulaExpression> arguments = []
      if (!match(TokenType.RPAREN)) {
        do {
          arguments << parseExpression()
        } while (match(TokenType.COMMA))
        FormulaToken closing = expect(TokenType.RPAREN, "Expected ')' after function arguments")
        return new FormulaExpression.FunctionCall(nameToken.text, arguments, nameToken.start, closing.end)
      }
      new FormulaExpression.FunctionCall(nameToken.text, arguments, nameToken.start, previous().end)
    }

    private boolean match(TokenType type) {
      if (peek().type != type) {
        return false
      }
      advance()
      true
    }

    private FormulaToken expect(TokenType type, String message) {
      if (peek().type != type) {
        throw parseError(message, peek().start)
      }
      advance()
    }

    private FormulaToken advance() {
      FormulaToken token = tokens[index]
      index++
      token
    }

    private FormulaToken previous() {
      tokens[index - 1]
    }

    private FormulaToken peek() {
      tokens[index]
    }

    private FormulaParseException parseError(String message, int position) {
      FormulaSupport.parseError(source, position, message)
    }
  }

  private static FormulaParseException parseError(String source, int position, String message) {
    int safePosition = Math.max(0, Math.min(position, source.length()))
    String pointer = "${' ' * safePosition}^"
    new FormulaParseException("${message} at position ${safePosition}\n${source}\n${pointer}", safePosition)
  }
}
